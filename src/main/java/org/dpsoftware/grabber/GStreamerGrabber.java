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

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.audio.AudioLoopback;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.utilities.CommonUtility;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;

import java.awt.*;
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
    private final Lock bufferLock = new ReentrantLock();
    public AppSink videosink;
    private Color[] previousFrame;
    private Color[] frameInsertion;

    /**
     * Creates a new instance of GstVideoComponent
     */
    public GStreamerGrabber() {
        this(new AppSink("GstVideoComponent"));
        ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(FireflyLuciferin.config.getDefaultLedMatrix());
        frameInsertion = new Color[ledMatrix.size()];
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
        if (FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name())) {
            // Scale image inside the GPU by RESAMPLING_FACTOR
            gstreamerPipeline = Constants.GSTREAMER_PIPELINE_DDUPL
                    .replace(Constants.INTERNAL_SCALING_X, String.valueOf(FireflyLuciferin.config.getScreenResX() / Constants.RESAMPLING_FACTOR))
                    .replace(Constants.INTERNAL_SCALING_Y, String.valueOf(FireflyLuciferin.config.getScreenResY() / Constants.RESAMPLING_FACTOR));
        } else {
            gstreamerPipeline = Constants.GSTREAMER_PIPELINE
                    .replace(Constants.INTERNAL_SCALING_X, String.valueOf(FireflyLuciferin.config.getScreenResX() / Constants.RESAMPLING_FACTOR))
                    .replace(Constants.INTERNAL_SCALING_Y, String.valueOf(FireflyLuciferin.config.getScreenResY() / Constants.RESAMPLING_FACTOR));
        }
        gstreamerPipeline = setFramerate(gstreamerPipeline);
        StringBuilder caps = new StringBuilder(gstreamerPipeline);
        // JNA creates ByteBuffer using native byte order, set masks according to that.
        if (!(FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name()))) {
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
        if (!Enums.Framerate.UNLOCKED.equals(LocalizedEnum.fromBaseStr(Enums.Framerate.class, FireflyLuciferin.config.getDesiredFramerate()))) {
            Enums.Framerate framerateToSave = LocalizedEnum.fromStr(Enums.Framerate.class, FireflyLuciferin.config.getDesiredFramerate());
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, framerateToSave != null ? framerateToSave.getBaseI18n() : FireflyLuciferin.config.getDesiredFramerate());
        } else {
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, Constants.FRAMERATE_CAP);
        }
        switch (LocalizedEnum.fromStr(Enums.FrameInsertion.class, FireflyLuciferin.config.getFrameInsertion())) {
            case SMOOTHING_LVL_1 ->
                    gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, String.valueOf(Enums.FrameInsertion.SMOOTHING_LVL_1.getFrameInsertionFramerate()));
            case SMOOTHING_LVL_2 ->
                    gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, String.valueOf(Enums.FrameInsertion.SMOOTHING_LVL_2.getFrameInsertionFramerate()));
            case SMOOTHING_LVL_3 ->
                    gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, String.valueOf(Enums.FrameInsertion.SMOOTHING_LVL_3.getFrameInsertionFramerate()));
            case SMOOTHING_LVL_4 ->
                    gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, String.valueOf(Enums.FrameInsertion.SMOOTHING_LVL_4.getFrameInsertionFramerate()));
            case SMOOTHING_LVL_5 ->
                    gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, String.valueOf(Enums.FrameInsertion.SMOOTHING_LVL_5.getFrameInsertionFramerate()));
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
            if (FireflyLuciferin.config.isAutoDetectBlackBars()) {
                if (ImageProcessor.CHECK_ASPECT_RATIO) {
                    ImageProcessor.CHECK_ASPECT_RATIO = false;
                    ImageProcessor.autodetectBlackBars(width, height, rgbBuffer);
                }
            }
            try {
                Color[] leds = new Color[ledMatrix.size()];
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
                // Put the image in the queue or send it via socket to the main instance server
                if (!AudioLoopback.RUNNING_AUDIO || Enums.Effect.MUSIC_MODE_BRIGHT.equals(LocalizedEnum.fromBaseStr(Enums.Effect.class, FireflyLuciferin.config.getEffect()))) {
                    if (!FireflyLuciferin.config.getFrameInsertion().equals(Enums.FrameInsertion.NO_SMOOTHING.getBaseI18n())) {
                        if (previousFrame != null) {
                            Gst.invokeLater(() -> frameInsertion(leds));
                        }
                    } else {
                        PipelineManager.offerToTheQueue(leds);
                    }
                    // Increase the FPS counter
                    FireflyLuciferin.FPS_PRODUCER_COUNTER++;
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
            int steps = 0;
            switch (LocalizedEnum.fromStr(Enums.FrameInsertion.class, FireflyLuciferin.config.getFrameInsertion())) {
                case SMOOTHING_LVL_1 -> steps = Enums.FrameInsertion.SMOOTHING_LVL_1.getFrameInsertionSmoothLvl();
                case SMOOTHING_LVL_2 -> steps = Enums.FrameInsertion.SMOOTHING_LVL_2.getFrameInsertionSmoothLvl();
                case SMOOTHING_LVL_3 -> steps = Enums.FrameInsertion.SMOOTHING_LVL_3.getFrameInsertionSmoothLvl();
                case SMOOTHING_LVL_4 -> steps = Enums.FrameInsertion.SMOOTHING_LVL_4.getFrameInsertionSmoothLvl();
                case SMOOTHING_LVL_5 -> steps = Enums.FrameInsertion.SMOOTHING_LVL_5.getFrameInsertionSmoothLvl();
            }
            for (int i = 0; i <= steps; i++) {
                for (int j = 0; j < leds.length; j++) {
                    final int dRed = leds[j].getRed() - previousFrame[j].getRed();
                    final int dGreen = leds[j].getGreen() - previousFrame[j].getGreen();
                    final int dBlue = leds[j].getBlue() - previousFrame[j].getBlue();
                    final Color c = new Color(
                            previousFrame[j].getRed() + ((dRed * i) / steps),
                            previousFrame[j].getGreen() + ((dGreen * i) / steps),
                            previousFrame[j].getBlue() + ((dBlue * i) / steps));
                    frameInsertion[j] = c;
                }
                if (frameInsertion.length == leds.length) {
                    PipelineManager.offerToTheQueue(frameInsertion);
                    CommonUtility.sleepMilliseconds(14);
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
