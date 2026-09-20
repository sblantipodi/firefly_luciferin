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
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.network.web.WebRtcSignalingServer;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSrc;
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
 * Builds and feeds a GStreamer WebRTC pipeline that pushes the live capture buffer
 * (the same scaled frame that feeds the AppSink) to a browser over WebRTC.
 * <p>
 * The pipeline is:
 * <pre>
 * appsrc (BGR x-raw, scaled) ! videoconvert ! queue ! vp8enc ! rtpvp8pay
 *   ! queue ! application/x-rtp,media=video,encoding-name=VP8,payload=97 ! webrtcbin
 * </pre>
 * The {@code appsrc} is fed by {@link #pushFrame(ByteBuffer, int, int)} from
 * {@code GStreamerGrabber.handleNewSample}. The {@code webrtcbin} handles the SDP offer
 * (sent to the browser via the signaling server) and ICE.
 * <p>
 * One streamer serves one viewer at a time; calling {@link #startSession} while a session is
 * active tears down the previous pipeline first.
 */
@Slf4j
public class WebRtcStreamer {

    private volatile Pipeline pipeline;
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
    private final AtomicBoolean firstFrameLogged = new AtomicBoolean(false);
    private final AtomicBoolean strideLogged = new AtomicBoolean(false);
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
     * Largest WebRTC preview frame. Smaller capture frames are kept at their native scaled size.
     */
    private static final int MAX_PREVIEW_WIDTH = 1280;
    private static final int MAX_PREVIEW_HEIGHT = 720;

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
            pipeline = (Pipeline) Gst.parseLaunch(
                    "appsrc name=webrtcsrc is-live=true do-timestamp=true format=TIME"
                            + " ! queue leaky=downstream max-size-buffers=1 max-size-bytes=0 max-size-time=0"
                            + " ! videoconvert ! videoscale add-borders=true"
                            + " ! capsfilter name=webrtcpreviewcaps"
                            + " ! vp8enc deadline=1"
                            + " ! rtpvp8pay"
                            + " ! queue leaky=downstream max-size-buffers=1 max-size-bytes=0 max-size-time=0"
                            + " ! application/x-rtp,media=video,encoding-name=VP8,payload=97"
                            + " ! webrtcbin name=webrtcbin stun-server=stun://stun.l.google.com:19302");
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

        // Configure the appsrc for push-mode, live, block format. The width/height are left
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
        bus.connect((Bus.ERROR) (source, code, message) -> {
            log.error("WebRTC pipeline error: code={}, message={}", code, message);
        });

        streaming.set(true);
        // The pipeline is NOT started here; it will be started on the first pushFrame,
        // once the frame dimensions are known and the appsrc caps are set with width/height.
        pipelineStarted.set(false);
        log.info("WebRTC pipeline ready for session {} (will start on first frame)", session.getId());
    }

    /**
     * Push a captured frame into the appsrc. Called from the GStreamer grabber thread on every
     * {@code newSample}. The buffer is the BGR raw frame in the scaled resolution.
     *
     * @param buffer the raw BGR frame buffer (already scaled by the capture pipeline)
     * @param width  the frame width in pixels
     * @param height the frame height in pixels
     */
    public void pushFrame(ByteBuffer buffer, int width, int height) {
        if (!streaming.get() || buffer == null) {
            return;
        }
        restartForCaptureSizeChange(width, height);
        AppSrc sourceElement = appSrc;
        if (sourceElement == null) {
            return;
        }
        // On the first frame, set the input caps and cap the preview only when its actual scaled
        // capture size exceeds 1280x720. This avoids both an expensive 4K VP8 encode and
        // upscaling small LED-oriented capture frames.
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
                    "video/x-raw,width=" + previewSize[0] + ",height=" + previewSize[1]
                            + ",pixel-aspect-ratio=1/1"));
            pipeline.play();
            log.info("WebRTC pipeline started on first frame: {}x{} -> {}x{}", width, height,
                    previewSize[0], previewSize[1]);
        }
        if (firstFrameLogged.compareAndSet(false, true)) {
            log.info("WebRTC pushing first frame to appsrc: {}x{}, bytes={}", width, height, buffer.remaining());
        }
        ByteBuffer source = buffer.duplicate();
        source.rewind();
        // AppSrc raw-video caps do not carry the row stride.  Some hardware paths (notably
        // cudaconvert) retain padding at the end of every row, so sending that mapped buffer as
        // tightly packed video makes each following row start inside the padding.  Compact the
        // rows while copying into the new GstBuffer, as ImageProcessor does for screenshots.
        int widthPlusStride = ImageProcessor.getWidthPlusStride(width, height, source.asIntBuffer());
        int bytesPerPixel = Integer.BYTES;
        int packedRowBytes = width * bytesPerPixel;
        int sourceRowBytes = widthPlusStride * bytesPerPixel;
        int packedFrameBytes = packedRowBytes * height;
        boolean hasStride = widthPlusStride > width;
        if (source.remaining() < packedFrameBytes || (hasStride && source.remaining() < sourceRowBytes * height)) {
            log.warn("Skipping WebRTC frame with invalid buffer size: {} bytes for {}x{} (stride {} pixels)",
                    source.remaining(), width, height, widthPlusStride);
            return;
        }
        if (hasStride && strideLogged.compareAndSet(false, true)) {
            log.info("WebRTC compacting capture rows: {}x{}, stride {} pixels", width, height, widthPlusStride);
        }
        Buffer gstBuffer = new Buffer(packedFrameBytes);
        ByteBuffer mappedBuffer = gstBuffer.map(false);
        if (mappedBuffer == null) {
            log.warn("Unable to map WebRTC frame buffer");
            return;
        }
        if (hasStride) {
            for (int row = 0; row < height; row++) {
                int rowStart = row * sourceRowBytes;
                ByteBuffer sourceRow = source.duplicate();
                sourceRow.position(rowStart);
                sourceRow.limit(rowStart + packedRowBytes);
                mappedBuffer.put(sourceRow);
            }
        } else {
            source.limit(packedFrameBytes);
            mappedBuffer.put(source);
        }
        mappedBuffer.rewind();
        gstBuffer.unmap();

        // do-timestamp=true makes appsrc automatically set the PTS from the system clock,
        // so we no longer set it manually. This avoids the segment format mismatch assertion.
        gstBuffer.setDuration(FRAME_DURATION_NS);

        FlowReturn ret = sourceElement.pushBuffer(gstBuffer);
        if (ret != FlowReturn.OK) {
            log.debug("WebRTC appsrc push returned {}", ret);
        }
    }

    /**
     * Checks the element factories required by {@code webrtcbin}.  Checking the NICE source and
     * sink before calling {@link Gst#parseLaunch(String)} is important: gst1-java 1.4 can crash
     * while converting GStreamer's parse error when the optional gstreamer1.0-nice package is
     * missing.
     *
     * @return {@code true} when the local GStreamer installation can create a WebRTC pipeline
     */
    public boolean isSupported() {
        Boolean available = supported;
        if (available == null) {
            synchronized (this) {
                available = supported;
                if (available == null) {
                    try {
                        available = ElementFactory.find("webrtcbin") != null
                                && ElementFactory.find("nicesrc") != null
                                && ElementFactory.find("nicesink") != null;
                    } catch (RuntimeException e) {
                        // gst1-java can throw while wrapping a missing optional factory.  This is
                        // an expected capability check failure, never a reason to abort the HTTP
                        // request or the capture pipeline.
                        available = false;
                        log.debug("Unable to inspect WebRTC/NICE GStreamer factories: {}", e.toString());
                    }
                    supported = available;
                }
            }
        }
        if (!available && unsupportedLogged.compareAndSet(false, true)) {
            log.info("WebRTC live preview is unavailable because the GStreamer NICE plugin is missing; "
                    + "using the image live preview instead");
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
        sessionGeneration.incrementAndGet();
        if (pipeline != null) {
            pipeline.setState(State.NULL);
            pipeline = null;
        }
        appSrc = null;
        previewCapsFilter = null;
        webRTCBin = null;
        streaming.set(false);
        pipelineStarted.set(false);
        firstFrameLogged.set(false);
        strideLogged.set(false);
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

    private boolean isCurrentSession(long generation, WebRTCBin bin, WebRtcSignalingServer server) {
        return sessionGeneration.get() == generation && webRTCBin == bin && signalingServer == server;
    }

    /**
     * Return an even-sized preview that fits inside the maximum bounding box without enlarging
     * the raw frame. VP8's 4:2:0 input works most reliably with even dimensions.
     */
    private static int[] previewSize(int width, int height) {
        double scale = Math.min(1d, Math.min((double) MAX_PREVIEW_WIDTH / width,
                (double) MAX_PREVIEW_HEIGHT / height));
        int scaledWidth = evenFloor(width * scale);
        int scaledHeight = evenFloor(height * scale);
        return new int[]{scaledWidth, scaledHeight};
    }

    private static int evenFloor(double dimension) {
        return Math.max(2, ((int) Math.floor(dimension)) & ~1);
    }

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
