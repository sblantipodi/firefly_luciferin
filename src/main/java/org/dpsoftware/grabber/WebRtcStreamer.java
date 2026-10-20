/*
  WebRtcStreamer.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.dpsoftware.grabber;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.lut.CubeLutToneMap;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.network.web.WebRtcSignalingServer;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSrc;
import org.freedesktop.gstreamer.glib.GObject;
import org.freedesktop.gstreamer.glib.Natives;
import org.freedesktop.gstreamer.lowlevel.GstTypes;
import org.freedesktop.gstreamer.webrtc.WebRTCBin;
import org.freedesktop.gstreamer.webrtc.WebRTCBin.ON_ICE_CANDIDATE;
import org.freedesktop.gstreamer.webrtc.WebRTCBin.ON_NEGOTIATION_NEEDED;
import org.freedesktop.gstreamer.webrtc.WebRTCSDPType;
import org.freedesktop.gstreamer.webrtc.WebRTCSessionDescription;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Builds and starts a GStreamer WebRTC pipeline that sends the captured frame
 * (the same scaled frame used by the AppSink) to a browser over WebRTC.
 * The pipeline is:
 * appsrc (BGR x-raw, scaled) - videoconvert - queue - vp8enc - rtpvp8pay queue
 * - application/x-rtp,media=video,encoding-name=VP8,payload=97 - webrtcbin
 * Capture frames are admitted by pushFrame and prepared on a bounded preview worker before feeding appsrc.
 * The webrtcbin handles the SDP offer (sent to the browser via the signaling server) and ICE.
 * One streamer serves one viewer at a time: calling startSession while a session is active tears down the previous pipeline first.
 */
@Slf4j
public class WebRtcStreamer {

    private volatile Pipeline pipeline;
    private volatile PreviewFrameDispatcher<PreviewFrame> frameDispatcher;
    private volatile AppSrc appSrc;
    private volatile Element previewCapsFilter;
    private volatile WebRTCBin webRTCBin;
    private volatile WebRtcSignalingServer signalingServer;
    private volatile Session activeSession;
    private volatile String byteOrder = "";
    private volatile String activeSessionId;
    private volatile int captureWidth;
    private volatile int captureHeight;
    private final AtomicBoolean streaming = new AtomicBoolean(false);
    private final AtomicBoolean unsupportedLogged = new AtomicBoolean(false);
    private volatile Boolean supported;
    private final AtomicLong sessionGeneration = new AtomicLong();
    /**
     * Set to true once the pipeline has been started (after the first pushFrame knows the
     * frame dimensions). Prevents pipeline.play() from being called multiple times.
     */
    private final AtomicBoolean pipelineStarted = new AtomicBoolean(false);
    /**
     * One frame interval in GStreamer clock time (nanoseconds).
     */
    private static final long FRAME_DURATION_NS = 1_000_000_000L / 30;
    /**
     * WebRTC preview bounding box, independent of the capture resampling factor.
     */
    private static final int MAX_PREVIEW_WIDTH = 960;
    private static final int MAX_PREVIEW_HEIGHT = 540;

    /**
     * Start (or restart) the WebRTC pipeline for a new viewer.
     *
     * @param server  the signaling server used to relay SDP/ICE to the browser
     * @param session the browser session (kept for diagnostics)
     */
    public synchronized void startSession(WebRtcSignalingServer server, Session session) {
        stop();
        if (!isSupported()) {
            return;
        }
        long generation = sessionGeneration.incrementAndGet();
        this.signalingServer = server;
        this.activeSession = session;
        this.activeSessionId = session.getId();
        // The raw capture frame format (BGRx / xRGB / BGRA / ARGB) depends on the capture method
        // and the native byte order, exactly as the main AppSink caps are built in
        // GStreamerGrabber. videoconvert converts this to the format the VP8 encoder needs.
        boolean isAmdIntel = Configuration.CaptureMethod.PIPEWIREXDG_AMD_INTEL.name()
                .equals(org.dpsoftware.MainSingleton.getInstance().getConfig().getCaptureMethod());
        String byteOrder;
        if (!Configuration.CaptureMethod.DDUPL_DX11.name().equals(
                org.dpsoftware.MainSingleton.getInstance().getConfig().getCaptureMethod())) {
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                byteOrder = PipelineManager.getBo(isAmdIntel ? Constants.BYTE_ORDER_BGRA : Constants.BYTE_ORDER_BGR);
            } else {
                byteOrder = PipelineManager.getBo(isAmdIntel ? Constants.BYTE_ORDER_ARGB : Constants.BYTE_ORDER_RGB);
            }
        } else {
            byteOrder = PipelineManager.getBo(Constants.BYTE_ORDER_BGRA);
        }
        byteOrder = byteOrder.replace("format=", "");
        this.byteOrder = byteOrder;
        try {
            pipeline = createPipeline();
        } catch (Exception e) {
            log.error("Failed to parse WebRTC pipeline: {}", e.getMessage());
            return;
        }
        AppSrc appSource = (AppSrc) pipeline.getElementByName("webrtcsrc");
        Element capsFilter = pipeline.getElementByName("webrtcpreviewcaps");
        WebRTCBin bin = (WebRTCBin) pipeline.getElementByName("webrtcbin");
        appSrc = appSource;
        previewCapsFilter = capsFilter;
        webRTCBin = bin;
        try {
            configureIcePortRange(bin);
        } catch (RuntimeException e) {
            log.error("Unable to restrict WebRTC local ports to UDP {}-{}", Constants.WEBRTC_MIN_RTP_PORT,
                    Constants.WEBRTC_MAX_RTP_PORT, e);
            stop();
            return;
        }

        // Configure the appsrc for push-mode, live video. The width/height are left
        // unset on the caps so the negotiated caps follow the actual frames pushed; the byte
        // order format and the fixed 30/1 framerate are set explicitly.
        appSource.set("caps", Caps.fromString(
                "video/x-raw,format=" + byteOrder + ",framerate=30/1"));

        bin.connect((ON_NEGOTIATION_NEEDED) element -> {
            if (isCurrentSession(generation, bin, server)) {
                log.info("WebRTC onNegotiationNeeded from {}", element.getName());
                bin.createOffer(offer -> onOfferCreated(generation, bin, server, offer));
            }
        });
        bin.connect((ON_ICE_CANDIDATE) (mLineIndex, candidate) -> {
            if (isCurrentSession(generation, bin, server) && candidate != null && !candidate.isBlank()) {
                log.debug("WebRTC local ICE candidate for m-line {}: {}", mLineIndex, candidate);
                server.sendToClient(server.buildIceMessage(candidate, mLineIndex));
            }
        });

        // Handle pipeline errors.
        Bus bus = pipeline.getBus();
        bus.connect((Bus.ERROR) (_, code, message) -> log.error("WebRTC pipeline error: code={}, message={}", code, message));

        frameDispatcher = new PreviewFrameDispatcher<>(FRAME_DURATION_NS, frame -> processFrame(frame, generation));
        // The worker starts the pipeline after receiving the first frame and setting its dimensions.
        pipelineStarted.set(false);
        streaming.set(true);
        log.info("WebRTC pipeline ready for session {} (will start on first frame)", session.getId());
    }

    /**
     * Creates the configured WebRTC pipeline without starting playback.
     *
     * @return the parsed preview pipeline
     */
    Pipeline createPipeline() {
        return (Pipeline) Gst.parseLaunch(Constants.GSTREAMER_PIPELINE_WEBRTC);
    }

    /**
     * Restricts local ICE sockets before any SDP negotiation or candidate gathering starts.
     * Remote browser ports are negotiated independently and are not restricted by this range.
     */
    static void configureIcePortRange(WebRTCBin bin) {
        // gst1-java 1.4 has no ICE wrapper and otherwise tries to instantiate abstract GObject.
        GstTypes.register(Natives.registration(WebRtcIceAgent.class, "GstWebRTCICE", WebRtcIceAgent::new));
        GObject iceAgent = (GObject) bin.get("ice-agent");
        iceAgent.set("max-rtp-port", Constants.WEBRTC_MAX_RTP_PORT);
        iceAgent.set("min-rtp-port", Constants.WEBRTC_MIN_RTP_PORT);
    }

    private static final class WebRtcIceAgent extends GstObject {
        /**
         * Wraps the native ICE agent returned by the WebRTC bin.
         *
         * @param initializer the native object initialization data
         */
        private WebRtcIceAgent(Initializer initializer) {
            super(initializer);
        }
    }

    /**
     * Applies the HDR to SDR tone mapping via the configured cube LUT to every pixel of the
     * packed WebRTC copy in place. The capture buffer must never be passed here.
     *
     * @param buffer the writable packed frame buffer
     * @param width  the frame width in pixels
     * @param height the frame height in pixels
     */
    static void applyLutToneMap(ByteBuffer buffer, int width, int height, String format) {
        applyLutToneMap(buffer, width, height, format, CubeLutToneMap.snapshot());
    }

    /**
     * Tone maps a packed preview frame in place using the LUT snapshot associated with that frame.
     * Preserves the alpha or padding byte and the buffer position; unavailable LUTs and unsupported
     * pixel formats leave the frame unchanged.
     *
     * @param buffer the writable packed frame buffer
     * @param width the frame width in pixels
     * @param height the frame height in pixels
     * @param format the GStreamer packed RGB pixel format
     * @param mapper the LUT snapshot to apply consistently to all pixels
     */
    private static void applyLutToneMap(ByteBuffer buffer, int width, int height, String format,
                                        CubeLutToneMap.ToneMapper mapper) {
        if (!mapper.isAvailable()) {
            return;
        }
        int redOffset;
        int greenOffset;
        int blueOffset;
        switch (format) {
            case "BGRx", "BGRA" -> {
                redOffset = 2;
                greenOffset = 1;
                blueOffset = 0;
            }
            case "xRGB", "ARGB" -> {
                redOffset = 1;
                greenOffset = 2;
                blueOffset = 3;
            }
            case "RGBx", "RGBA" -> {
                redOffset = 0;
                greenOffset = 1;
                blueOffset = 2;
            }
            case "xBGR", "ABGR" -> {
                redOffset = 3;
                greenOffset = 2;
                blueOffset = 1;
            }
            default -> {
                return;
            }
        }
        long requiredBytes = (long) width * height * Integer.BYTES;
        if (width <= 0 || height <= 0 || buffer.remaining() < requiredBytes) {
            return;
        }
        float[] mappedColor = new float[3];
        int start = buffer.position();
        for (int y = 0; y < height; y++) {
            int rowStart = start + y * width * Integer.BYTES;
            for (int x = 0; x < width; x++) {
                int offset = rowStart + x * Integer.BYTES;
                int r = buffer.get(offset + redOffset) & 0xFF;
                int g = buffer.get(offset + greenOffset) & 0xFF;
                int b = buffer.get(offset + blueOffset) & 0xFF;
                mapper.lookup(r, g, b, mappedColor);
                buffer.put(offset + redOffset, (byte) Math.round(mappedColor[0]));
                buffer.put(offset + greenOffset, (byte) Math.round(mappedColor[1]));
                buffer.put(offset + blueOffset, (byte) Math.round(mappedColor[2]));
            }
        }
    }

    /**
     * Return an even-sized preview capped at the maximum bounding box without upscaling
     * resampled capture frames, except for the two-pixel minimum. The browser handles display
     * enlargement. VP8's 4:2:0 input works most reliably with even dimensions.
     */
    static int[] previewSize(int width, int height) {
        double scale = Math.min(1.0, Math.min((double) MAX_PREVIEW_WIDTH / width,
                (double) MAX_PREVIEW_HEIGHT / height));
        int scaledWidth = evenFloor(width * scale);
        int scaledHeight = evenFloor(height * scale);
        return new int[]{scaledWidth, scaledHeight};
    }

    /**
     * Checks the element factories required by {@code webrtcbin}.  Checking the NICE source and
     * sink before calling Gst#parseLaunch(String) is important: gst1-java 1.4 can crash
     * while converting GStreamer's parse error when the optional gstreamer1.0-nice package is
     * missing.
     *
     * @return true when the required WebRTC/NICE factories are registered
     */
    public boolean isSupported() {
        Boolean available = supported;
        if (available == null) {
            synchronized (this) {
                available = supported;
                if (available == null) {
                    // Fixed local ICE port ranges require GStreamer 1.20 or newer.
                    available = Gst.getVersion().checkSatisfies(Version.of(1, 20));
                    if (!available) {
                        log.info("WebRTC live preview requires GStreamer 1.20 or newer to restrict local ICE ports");
                    }
                    for (String factoryName : new String[]{"webrtcbin", "nicesrc", "nicesink"}) {
                        try {
                            if (ElementFactory.find(factoryName) == null) {
                                available = false;
                                log.info("Required GStreamer factory '{}' is unavailable", factoryName);
                            }
                        } catch (RuntimeException e) {
                            // gst1-java can throw while wrapping a missing optional factory. Report the failing factory without assuming that the NICE plugin is missing.
                            available = false;
                            log.info("Unable to inspect required GStreamer factory '{}': {}", factoryName, e.toString());
                            log.debug("GStreamer factory inspection failed for '{}'", factoryName, e);
                        }
                    }
                    supported = available;
                }
            }
        }
        if (!available && unsupportedLogged.compareAndSet(false, true)) {
            log.info("WebRTC live preview is unavailable because GStreamer is too old or required factories are unavailable; using the image live preview instead");
        }
        return available;
    }

    /**
     * Receive the SDP answer from the browser and apply it to the webrtcbin.
     *
     * @param sessionId the signaling session that supplied the answer
     * @param sdp       the SDP answer text
     */
    public void onAnswer(String sessionId, String sdp) {
        WebRTCBin bin = webRTCBin;
        if (!sessionId.equals(activeSessionId) || bin == null) {
            return;
        }
        try {
            SDPMessage sdpMessage = new SDPMessage();
            sdpMessage.parseBuffer(sdp);
            WebRTCSessionDescription description =
                    new WebRTCSessionDescription(WebRTCSDPType.ANSWER, sdpMessage);
            bin.setRemoteDescription(description);
            log.info("WebRTC answer applied to webrtcbin");
        } catch (Exception e) {
            log.error("Failed to apply WebRTC answer: {}", e.getMessage());
        }
    }

    /**
     * Receive a remote ICE candidate from the browser and add it to the webrtcbin.
     *
     * @param sessionId  the signaling session that supplied the candidate
     * @param mLineIndex the SDP m-line index
     * @param candidate  the ICE candidate text
     */
    public void onIceCandidate(String sessionId, int mLineIndex, String candidate) {
        WebRTCBin bin = webRTCBin;
        if (!sessionId.equals(activeSessionId) || bin == null || candidate == null || candidate.isBlank()) {
            return;
        }
        try {
            bin.addIceCandidate(mLineIndex, candidate);
        } catch (Exception e) {
            log.warn("Failed to add remote ICE candidate: {}", e.getMessage());
        }
    }

    /**
     * Stop the WebRTC pipeline and release the resources.
     */
    public synchronized void stop() {
        streaming.set(false);
        sessionGeneration.incrementAndGet();
        PreviewFrameDispatcher<PreviewFrame> dispatcher = frameDispatcher;
        frameDispatcher = null;
        if (dispatcher != null) dispatcher.close();
        if (pipeline != null) {
            pipeline.setState(State.NULL);
            pipeline = null;
        }
        appSrc = null;
        previewCapsFilter = null;
        webRTCBin = null;
        pipelineStarted.set(false);
        signalingServer = null;
        activeSession = null;
        activeSessionId = null;
        captureWidth = 0;
        captureHeight = 0;
    }

    /**
     * Recreate webrtcbin if the capture pipeline was restarted with different frame dimensions.
     * Caps cannot be changed safely on the active VP8/WebRTC stream; rebuilding produces a new
     * offer which the existing browser socket answers as a normal WebRTC renegotiation.
     */
    private synchronized void restartForCaptureSizeChange(int width, int height) {
        if (!pipelineStarted.get() || (captureWidth == width && captureHeight == height)) {
            return;
        }
        WebRtcSignalingServer server = signalingServer;
        Session session = activeSession;
        if (server == null || session == null || !session.isOpen()) {
            return;
        }
        log.info("WebRTC capture dimensions changed: {}x{} -> {}x{}; renegotiating preview",
                captureWidth, captureHeight, width, height);
        startSession(server, session);
    }

    /**
     * Checks whether an asynchronous callback still belongs to the active preview session.
     *
     * @param generation the session generation captured when the callback was registered
     * @param bin the WebRTC bin that produced the callback
     * @param server the signaling server associated with the callback
     * @return true when all three values match the current session
     */
    private boolean isCurrentSession(long generation, WebRTCBin bin, WebRtcSignalingServer server) {
        return sessionGeneration.get() == generation && webRTCBin == bin && signalingServer == server;
    }

    /**
     * Offers a capture frame without waiting for LUT processing or the preview pipeline.
     * The dispatcher checks rate and worker availability before making the one required copy:
     * the capture buffer is unmapped as soon as the grabber callback returns.
     */
    public void pushFrame(ByteBuffer buffer, int width, int height) {
        PreviewFrameDispatcher<PreviewFrame> dispatcher = frameDispatcher;
        if (!streaming.get() || dispatcher == null || buffer == null) return;
        dispatcher.submit(() -> copyFrame(buffer, width, height));
    }

    private record PreviewFrame(Buffer buffer, int width, int height, CubeLutToneMap.ToneMapper mapper)
            implements AutoCloseable {
        /**
         * Releases the owned native buffer, or its invalidated Java wrapper after transfer to appsrc.
         */
        @Override
        public void close() {
            // gst1-java invalidates the Java buffer on pushBuffer (ownership transfers to appsrc).
            // dispose is also safe on that invalidated wrapper; unsubmitted buffers are freed here.
            buffer.dispose();
        }
    }

    /**
     * Copies visible capture pixels into an independently owned, tightly packed native buffer
     * and associates the current LUT snapshot with the frame for asynchronous processing.
     *
     * @param buffer the capture buffer, whose frame starts at offset zero
     * @param width the frame width in pixels
     * @param height the frame height in pixels
     * @return the owned preview frame, or null when dimensions are invalid or native mapping fails
     * @throws IllegalArgumentException if the source row stride is invalid
     */
    private PreviewFrame copyFrame(ByteBuffer buffer, int width, int height) {
        long pixels = (long) width * height;
        long packedBytes = pixels * Integer.BYTES;
        if (width <= 0 || height <= 0 || pixels > Integer.MAX_VALUE / Integer.BYTES || buffer.limit() < packedBytes) {
            log.warn("Skipping WebRTC frame with invalid dimensions or buffer size: {}x{}, {} bytes", width, height, buffer.limit());
            return null;
        }
        Buffer copy = new Buffer((int) packedBytes);
        boolean copied = false;
        try {
            ByteBuffer target = copy.map(true);
            if (target == null) return null;
            try {
                copyPackedPixels(buffer, target, width, height);
            } finally {
                copy.unmap();
            }
            PreviewFrame frame = new PreviewFrame(copy, width, height, CubeLutToneMap.snapshot());
            copied = true;
            return frame;
        } finally {
            if (!copied) copy.dispose();
        }
    }

    /**
     * Packs capture rows without changing the source position, limit or bytes. Captured frames start at zero.
     */
    static void copyPackedPixels(ByteBuffer buffer, ByteBuffer target, int width, int height) {
        long pixels = (long) width * height;
        long packedBytes = pixels * Integer.BYTES;
        if (width <= 0 || height <= 0 || pixels > Integer.MAX_VALUE / Integer.BYTES
                || buffer.limit() < packedBytes || target.remaining() < packedBytes) {
            throw new IllegalArgumentException("Invalid WebRTC frame dimensions or buffer size");
        }
        ByteBuffer source = buffer.duplicate();
        source.rewind();
        int widthPlusStride = ImageProcessor.getWidthPlusStride(width, height, source.asIntBuffer());
        int packedRowBytes = width * Integer.BYTES;
        int sourceRowBytes = widthPlusStride * Integer.BYTES;
        long requiredBytes = (long) (height - 1) * sourceRowBytes + packedRowBytes;
        if (widthPlusStride < width || source.remaining() < requiredBytes) {
            throw new IllegalArgumentException("Invalid WebRTC frame stride");
        }
        if (widthPlusStride == width) {
            source.limit((int) packedBytes);
            target.put(source);
        } else {
            for (int row = 0; row < height; row++) {
                int start = row * sourceRowBytes;
                source.limit(start + packedRowBytes).position(start);
                target.put(source);
            }
        }
    }

    /**
     * Only the preview worker enters here. Serialize native pipeline use with session stop/restart;
     * capture admission and copying never acquire this monitor.
     */
    private synchronized void processFrame(PreviewFrame frame, long generation) {
        if (!streaming.get() || sessionGeneration.get() != generation) return;
        int width = frame.width();
        int height = frame.height();
        restartForCaptureSizeChange(width, height);
        // A size change starts a new session/worker. Its next frame initializes the new caps.
        if (!streaming.get() || sessionGeneration.get() != generation) return;
        AppSrc sourceElement = appSrc;
        if (sourceElement == null) return;
        if (pipelineStarted.compareAndSet(false, true)) {
            int[] previewSize = previewSize(width, height);
            captureWidth = width;
            captureHeight = height;
            sourceElement.set("caps", Caps.fromString(
                    "video/x-raw,format=" + byteOrder + ",width=" + width + ",height=" + height + ",framerate=30/1"));
            Element capsFilter = previewCapsFilter;
            if (capsFilter == null) {
                log.error("WebRTC preview caps filter is unavailable");
                stop();
                return;
            }
            capsFilter.set("caps", Caps.fromString(
                    "video/x-raw,width=" + previewSize[0] + ",height=" + previewSize[1] + ",pixel-aspect-ratio=1/1"));
            pipeline.play();
            log.info("WebRTC pipeline started on first frame: {}x{} -> {}x{}", width, height, previewSize[0], previewSize[1]);
        }
        Buffer gstBuffer = frame.buffer();
        ByteBuffer mapped = gstBuffer.map(true);
        if (mapped == null) return;
        try {
            applyLutToneMap(mapped, width, height, byteOrder, frame.mapper());
        } finally {
            gstBuffer.unmap();
        }
        // appsrc timestamps the processed frame. Skipped frames do not build up a synthetic timeline.
        gstBuffer.setDuration(FRAME_DURATION_NS);
        FlowReturn ret = sourceElement.pushBuffer(gstBuffer);
        if (ret != FlowReturn.OK) log.debug("WebRTC appsrc push returned {}", ret);
    }

    /**
     * Rounds a dimension down to an even integer suitable for the preview encoder, with a minimum of two.
     *
     * @param dimension the scaled dimension in pixels
     * @return the even pixel dimension
     */
    private static int evenFloor(double dimension) {
        return Math.max(2, ((int) Math.floor(dimension)) & ~1);
    }

    /**
     * Sets the local SDP offer and sends it to the browser if its preview session is still active.
     *
     * @param generation the session generation that requested the offer
     * @param bin the WebRTC bin that created the offer
     * @param server the signaling server used to deliver the offer
     * @param offer the generated local session description
     */
    private void onOfferCreated(long generation, WebRTCBin bin, WebRtcSignalingServer server,
                                WebRTCSessionDescription offer) {
        if (!isCurrentSession(generation, bin, server)) {
            return;
        }
        try {
            bin.setLocalDescription(offer);
        } catch (Exception e) {
            log.error("Failed to set local description: {}", e.getMessage());
        }
        String offerSdp = offer.getSDPMessage().toString();
        log.debug("WebRTC offer SDP:\n{}", offerSdp);
        server.sendToClient(server.buildOfferMessage(offerSdp));
        log.info("WebRTC offer sent to client");
    }
}
