/*
  GStreamerGrabber.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.PipelineManager;
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

    private final Lock bufferLock = new ReentrantLock();
    private final AppSink videosink;
    public static LinkedHashMap<Integer, LEDCoordinate> ledMatrix;

    /**
     * Creates a new instance of GstVideoComponent
     */
    public GStreamerGrabber() {
        this(new AppSink("GstVideoComponent"));
        ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(FireflyLuciferin.config.getDefaultLedMatrix());
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
        // Huge amount of LEDs requires slower framerate

        if (!Constants.Framerate.UNLOCKED.equals(LocalizedEnum.fromBaseStr(Constants.Framerate.class, FireflyLuciferin.config.getDesiredFramerate()))) {
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll("FRAMERATE_PLACEHOLDER", LocalizedEnum.fromStr(Constants.Framerate.class, FireflyLuciferin.config.getDesiredFramerate()).getBaseI18n());
        } else {
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll("FRAMERATE_PLACEHOLDER", "360");
        }
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
     * Return videosink element
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

            int intBufferSize = (width*height)-1;

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
                        // No need for the square root here since we calculate the gamma
                        r = ImageProcessor.gammaCorrection(r / pickNumber);
                        g = ImageProcessor.gammaCorrection(g / pickNumber);
                        b = ImageProcessor.gammaCorrection(b / pickNumber);


                        float[] hsb = Color.RGBtoHSB(r, g, b, null);

                        float hue = hsb[0];
                        float saturation = hsb[1];
                        float brightness = hsb[2];

                        /* then change the saturation... */

//                        log.debug(saturation+"");


                        float[] hsv = new float[3];
                        Color.RGBtoHSB(r, g, b, hsv);
                        int rgb;
                        float adjustedGreenSaturation = (float) FireflyLuciferin.config.getGreenSaturation() + saturation;
                        if ((hsv[0] * 360) > Constants.MIN_GREEN_HUE && (hsv[0] * 360) < Constants.MAX_GREEN_HUE) {
                            log.debug("Green Sat="+saturation);
                            if (adjustedGreenSaturation > 1.0F) adjustedGreenSaturation = 1.0F;
                            else if (adjustedGreenSaturation < 0) adjustedGreenSaturation = 0.0F;
                            rgb = Color.HSBtoRGB(hue, adjustedGreenSaturation, brightness);
                        } else {
                            float adjustedSaturation = (float) FireflyLuciferin.config.getSaturation() + saturation;
                            //TODO CHECK range
                            rgb = Color.HSBtoRGB(hue, (adjustedSaturation < 0 || adjustedSaturation > 1.0) ? saturation : adjustedSaturation, brightness);
                        }



//                        float adjustedYellowSaturation = (float) FireflyLuciferin.config.getYellowSaturation() + saturation;
//                        float adjustedGreenSaturation = (float) FireflyLuciferin.config.getGreenSaturation() + saturation;
//                        float adjustedCyanSaturation = (float) FireflyLuciferin.config.getCyanSaturation() + saturation;
//                        float adjustedBlueSaturation = (float) FireflyLuciferin.config.getBlueSaturation() + saturation;
//                        float adjustedMagentaSaturation = (float) FireflyLuciferin.config.getMagentaSaturation() + saturation;
//
//


//                        float // CYAN
//                                MIN_BLUE_HUE = // CYAN
//                                0.5f * 360;
//                        float // MAGENTA
//                                MAX_BLUE_HUE = // MAGENTA
//                                0.8333333f;
//                        float UNMAPPED_HUE = 0.6666667f;
//
//                        float[] hsv = new float[3];
//                        Color.RGBtoHSB(r, g, b, hsv);
//                        if (hsv[0] > MIN_BLUE_HUE && hsv[0] < MAX_BLUE_HUE) {
//                            r = 0;
//                            g = 0;
//                            b = 0;
//                        } else {
                            r = (rgb>>16)&0xFF;
                            g = (rgb>>8)&0xFF;
                            b = rgb&0xFF;
//                        }


                        if (FireflyLuciferin.config.isEyeCare() && (r+g+b) < 10) r = g = b = (Constants.DEEP_BLACK_CHANNEL_TOLERANCE * 2);
                        leds[key - 1] = new Color(r, g, b);
                    } else {
                        leds[key - 1] = leds[key - 2];
                    }
                });
                // Put the image in the queue or send it via socket to the main instance server
                if (!AudioLoopback.RUNNING_AUDIO || Constants.Effect.MUSIC_MODE_BRIGHT.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))) {
                    // Offer to the queue
                    PipelineManager.offerToTheQueue(leds);
                    // Increase the FPS counter
                    FireflyLuciferin.FPS_PRODUCER_COUNTER++;
                }
            } finally {
                bufferLock.unlock();
            }
        }

        /**
         * New sample triggered every frame
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
