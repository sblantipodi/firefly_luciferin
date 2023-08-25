/*
  GStreamerGrabber.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.utilities.CommonUtility;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class needs GStreamer: open source multimedia framework
 * This class uses Windows Desktop Duplication API
 */
@Slf4j
public class GStreamerGrabber extends javax.swing.JComponent {

    public static LinkedHashMap<Integer, LEDCoordinate> ledMatrix;
    final int oneSecondMillis = 1000;
    private final Lock bufferLock = new ReentrantLock();
    public AppSink videosink;
    boolean writeToFile = false;
    int capturedFrames = 0;
    long start;
    private Color[] previousFrame;

    /**
     * Creates a new instance of GstVideoComponent
     */
    public GStreamerGrabber() {
        this(new AppSink("GstVideoComponent"));
        ledMatrix = MainSingleton.getInstance().config.getLedMatrixInUse(MainSingleton.getInstance().config.getDefaultLedMatrix());
        previousFrame = new Color[ledMatrix.size()];
        for (int i = 0; i < previousFrame.length; i++) {
            previousFrame[i] = new Color(0, 0, 0);
        }
    }

    /**
     * Creates a new instance of GstVideoComponent
     */
    public GStreamerGrabber(AppSink appsink) {
        this.videosink = appsink;
        videosink.set(Constants.EMIT_SIGNALS, true);
        AppSinkListener listener = new AppSinkListener();
        videosink.connect(listener);
        String gstreamerPipeline;
        if (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name())) {
            // Scale image inside the GPU by RESAMPLING_FACTOR, Constants.GSTREAMER_MEMORY_DIVIDER tells if resolution is compatible with D3D11Memory with no padding.
            if ((MainSingleton.getInstance().config.getScreenResX() / Constants.GSTREAMER_MEMORY_DIVIDER) % 2 == 0) {
                gstreamerPipeline = Constants.GSTREAMER_PIPELINE_DDUPL
                        .replace(Constants.INTERNAL_SCALING_X, String.valueOf(MainSingleton.getInstance().config.getScreenResX() / Constants.RESAMPLING_FACTOR))
                        .replace(Constants.INTERNAL_SCALING_Y, String.valueOf(MainSingleton.getInstance().config.getScreenResY() / Constants.RESAMPLING_FACTOR));
            } else {
                gstreamerPipeline = Constants.GSTREAMER_PIPELINE_DDUPL_SM
                        .replace(Constants.INTERNAL_SCALING_X, String.valueOf(MainSingleton.getInstance().config.getScreenResX() / Constants.RESAMPLING_FACTOR))
                        .replace(Constants.INTERNAL_SCALING_Y, String.valueOf(MainSingleton.getInstance().config.getScreenResY() / Constants.RESAMPLING_FACTOR));
            }
        } else {
            gstreamerPipeline = Constants.GSTREAMER_PIPELINE
                    .replace(Constants.INTERNAL_SCALING_X, String.valueOf(MainSingleton.getInstance().config.getScreenResX() / Constants.RESAMPLING_FACTOR))
                    .replace(Constants.INTERNAL_SCALING_Y, String.valueOf(MainSingleton.getInstance().config.getScreenResY() / Constants.RESAMPLING_FACTOR));
        }
        gstreamerPipeline = setFramerate(gstreamerPipeline);
        StringBuilder caps = new StringBuilder(gstreamerPipeline);
        // JNA creates ByteBuffer using native byte order, set masks according to that.
        if (!(MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name()))) {
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                caps.append(Constants.BYTE_ORDER_BGR);
            } else {
                caps.append(Constants.BYTE_ORDER_RGB);
            }
        }
        videosink.setCaps(new Caps(caps.toString()));
        setLayout(null);
        setOpaque(true);
        setBackground(Color.BLACK);
    }

    /**
     * Set framerate on the GStreamer pipeling
     *
     * @param gstreamerPipeline pipeline in use
     * @return pipeline str
     */
    private String setFramerate(String gstreamerPipeline) {
        // Huge amount of LEDs requires slower framerate
        if (!Enums.Framerate.UNLOCKED.equals(LocalizedEnum.fromBaseStr(Enums.Framerate.class, MainSingleton.getInstance().config.getDesiredFramerate()))) {
            Enums.Framerate framerateToSave = LocalizedEnum.fromStr(Enums.Framerate.class, MainSingleton.getInstance().config.getDesiredFramerate());
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, framerateToSave != null ? framerateToSave.getBaseI18n() : MainSingleton.getInstance().config.getDesiredFramerate());
        } else {
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, Constants.FRAMERATE_CAP);
        }
        if (!MainSingleton.getInstance().config.getFrameInsertion().equals(Enums.FrameInsertion.NO_SMOOTHING.getBaseI18n())) {
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, String.valueOf(LocalizedEnum.fromBaseStr(Enums.FrameInsertion.class, MainSingleton.getInstance().config.getFrameInsertion()).getFrameInsertionFramerate()));
        }
        return gstreamerPipeline;
    }

    /**
     * Return videosink element
     *
     * @return videosink
     */
    public Element getElement() {
        return videosink;
    }

    /**
     * Write intBuffer (image) to file
     *
     * @param rgbBuffer rgb int buffer
     */
    private void intBufferRgbToImage(IntBuffer rgbBuffer) {
        capturedFrames++;
        BufferedImage img = new BufferedImage(MainSingleton.getInstance().config.getScreenResX() / Constants.RESAMPLING_FACTOR,
                MainSingleton.getInstance().config.getScreenResY() / Constants.RESAMPLING_FACTOR, 1);
        int[] rgbArray = new int[rgbBuffer.capacity()];
        rgbBuffer.rewind();
        rgbBuffer.get(rgbArray);
        img.setRGB(0, 0, img.getWidth(), img.getHeight(), rgbArray, 0, img.getWidth());
        try {
            if (!writeToFile && capturedFrames == 90) {
                writeToFile = true;
                ImageIO.write(img, Constants.GSTREAMER_SCREENSHOT_EXTENSION, new File(Constants.GSTREAMER_SCREENSHOT));
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Listener callback triggered every frame
     */
    private class AppSinkListener implements AppSink.NEW_SAMPLE {

        public void rgbFrame(int width, int height, IntBuffer rgbBuffer) {

            // If the EDT is still copying data from the buffer, just drop this frame
            if (!bufferLock.tryLock()) {
                return;
            }
            int intBufferSize = (width * height) - 1;
            // CHECK_ASPECT_RATIO is true 10 times per second, if true and black bars auto detection is on, auto detect black bars
            if (MainSingleton.getInstance().config.isAutoDetectBlackBars()) {
                if (GrabberSingleton.getInstance().CHECK_ASPECT_RATIO) {
                    GrabberSingleton.getInstance().CHECK_ASPECT_RATIO = false;
                    ImageProcessor.autodetectBlackBars(width, height, rgbBuffer);
                }
            }
            try {
                Color[] leds = new Color[ledMatrix.size()];
                if (MainSingleton.getInstance().config.getRuntimeLogLevel().equals(Level.TRACE.levelStr)) {
                    intBufferRgbToImage(rgbBuffer);
                }
                // We need an ordered collection so no parallelStream here
                ledMatrix.forEach((key, value) -> {
                    int r = 0, g = 0, b = 0;
                    int skipPixel = 1;
                    int pickNumber = 0;
                    // Image grabbed has been scaled by RESAMPLING_FACTOR inside the GPU, convert coordinate to match this scale
                    int xCoordinate = (value.getX() / Constants.RESAMPLING_FACTOR);
                    int yCoordinate = (value.getY() / Constants.RESAMPLING_FACTOR);
                    int pixelInUseX = value.getWidth() / Constants.RESAMPLING_FACTOR;
                    int pixelInUseY = value.getHeight() / Constants.RESAMPLING_FACTOR;
                    if (!value.isGroupedLed()) {
                        // We start with a negative offset
                        for (int y = 0; y < pixelInUseY; y++) {
                            for (int x = 0; x < pixelInUseX; x++) {
                                int offsetX = (xCoordinate + (skipPixel * x));
                                int offsetY = (yCoordinate + (skipPixel * y));
                                int bufferOffset = (Math.min(offsetX, width))
                                        + ((offsetY < height) ? (offsetY * width) : (height * width));
                                int rgb = rgbBuffer.get(Math.min(intBufferSize, bufferOffset));
                                r += rgb >> 16 & 0xFF;
                                g += rgb >> 8 & 0xFF;
                                b += rgb & 0xFF;
                                pickNumber++;
                            }
                        }
                        leds[key - 1] = ImageProcessor.correctColors(r, g, b, pickNumber);
                    } else {
                        leds[key - 1] = leds[key - 2];
                    }
                });
                ImageProcessor.averageOnAllLeds(leds);

                // Put the image in the queue or send it via socket to the main instance server
                if (!MainSingleton.getInstance().exitTriggered && (!AudioSingleton.getInstance().RUNNING_AUDIO
                        || Enums.Effect.MUSIC_MODE_BRIGHT.equals(LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect())))) {
                    if (!MainSingleton.getInstance().config.getFrameInsertion().equals(Enums.FrameInsertion.NO_SMOOTHING.getBaseI18n())) {
                        if (previousFrame != null) {
                            frameInsertion(leds);
                        }
                    } else {
                        PipelineManager.offerToTheQueue(leds);
                    }
                    // Increase the FPS counter
                    MainSingleton.getInstance().FPS_PRODUCER_COUNTER++;
                }
            } finally {
                bufferLock.unlock();
            }
        }

        /**
         * Insert frames between captured frames, inserted frames represents the linear interpolation from the two captured frames.
         * Higher levels will smooth transitions from one color to another but LEDs will be less responsive to quick changes.
         *
         * @param leds array containing color information
         */
        void frameInsertion(Color[] leds) {
            Color[] frameInsertion = new Color[ledMatrix.size()];
            int totalElapsed = 0;
            // Framerate we asks to the GPU, less FPS = smoother but less response, more FPS = less smooth but faster to changes.
            int gpuFramerateFps = LocalizedEnum.fromBaseStr(Enums.FrameInsertion.class, MainSingleton.getInstance().config.getFrameInsertion()).getFrameInsertionFramerate();
            // Total number of frames to compute.
            int totalFrameToAdd = Constants.SMOOTHING_TARGET_FRAMERATE - gpuFramerateFps;
            // Number of frames to compute every time a frame is received from the GPU.
            int frameToCompute = (totalFrameToAdd / gpuFramerateFps);
            // Total number of frames to render, contains computed framse + GPU frame.
            int frameToRender = frameToCompute + 1;
            // GPU frame time (milliseconds) between one GPU frame and the other.
            int gpuFrameTimeMs = oneSecondMillis / gpuFramerateFps;
            // Milliseconds available to compute and show a frame, remove some milliseconds to the equation for protocol headroom. frameToCompute + 1 frame computed by the GPU.
            double frameDistanceMs = ((double) gpuFrameTimeMs / (frameToCompute + 1));
            // Skip frame if GPU is late and tries to catch up by capturing frames too fast.
            for (int i = 0; i < frameToRender; i++) {
                for (int j = 0; j < leds.length; j++) {
                    final int dRed = leds[j].getRed() - previousFrame[j].getRed();
                    final int dGreen = leds[j].getGreen() - previousFrame[j].getGreen();
                    final int dBlue = leds[j].getBlue() - previousFrame[j].getBlue();
                    final Color c = new Color(
                            previousFrame[j].getRed() + ((dRed * i) / frameToCompute),
                            previousFrame[j].getGreen() + ((dGreen * i) / frameToCompute),
                            previousFrame[j].getBlue() + ((dBlue * i) / frameToCompute));
                    frameInsertion[j] = c;
                }
                long finish = System.currentTimeMillis();
                if (frameInsertion.length == leds.length) {
                    long timeElapsed = finish - start;
                    totalElapsed += (int) timeElapsed;
                    if (timeElapsed > Constants.SMOOTHING_SKIP_FAST_FRAMES) {
                        PipelineManager.offerToTheQueue(frameInsertion);
                    } else {
                        log.debug("Frames is coming too fast, GPU is trying to catch up, skipping frame=" + i + ", Elapsed=" + timeElapsed);
                        start = System.currentTimeMillis();
                        previousFrame = leds.clone();
                        break;
                    }
                    start = System.currentTimeMillis();
                    if (i != frameToCompute || totalElapsed >= (frameDistanceMs * frameToRender)) {
                        if (totalElapsed >= (frameDistanceMs * frameToRender)) {
                            // Last frame never sleep, if GPU is late skip waiting.
                            if (i != frameToCompute) {
                                log.debug("GPU is late, skip wait on frame #" + i + ", Elapsed=" + timeElapsed + ", TotaleTimeElapsed=" + totalElapsed);
                            }
                            previousFrame = leds.clone();
                            break;
                        } else {
                            CommonUtility.sleepMilliseconds((int) (frameDistanceMs - Constants.SMOOTHING_SLOW_FRAME_TOLERANCE));
                        }
                    }
                }
            }
            previousFrame = leds.clone();
        }

        /**
         * New sample triggered every frame
         *
         * @param elem appvideosink
         * @return flow
         */
        @Override
        public FlowReturn newSample(AppSink elem) {
            Sample sample = elem.pullSample();
            Structure capsStruct = sample.getCaps().getStructure(0);
            int w = capsStruct.getInteger(Constants.WIDTH);
            int h = capsStruct.getInteger(Constants.HEIGHT);
            Buffer buffer = sample.getBuffer();
            ByteBuffer bb = buffer.map(false);
            if (bb != null) {
                rgbFrame(w, h, bb.asIntBuffer());
                buffer.unmap();
            }
            sample.dispose();
            return FlowReturn.OK;
        }
    }

}
