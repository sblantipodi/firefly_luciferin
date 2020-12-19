/*
  GStreamerGrabber.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020  Davide Perini

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

import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
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
public class GStreamerGrabber extends javax.swing.JComponent {

    private final Lock bufferLock = new ReentrantLock();
    private final AppSink videosink;
    static LinkedHashMap<Integer, LEDCoordinate> ledMatrix;

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
        if (!(FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name()))) {
            gstreamerPipeline = Constants.GSTREAMER_PIPELINE_DDUPL;
        } else {
            gstreamerPipeline = Constants.GSTREAMER_PIPELINE;
        }
        if (!Constants.UNLOCKED.equals(FireflyLuciferin.config.getDesiredFramerate())) {
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll("FRAMERATE_PLACEHOLDER", FireflyLuciferin.config.getDesiredFramerate());
        } else {
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll("FRAMERATE_PLACEHOLDER", "144");
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

            try {
                Color[] leds = new Color[ledMatrix.size()];
                // We need an ordered collection so no parallelStream here
                ledMatrix.forEach((key, value) -> {
                    int r = 0, g = 0, b = 0;
                    int skipPixel = 5;
                    // 6 pixel for X axis and 6 pixel for Y axis
                    int pixelToUse = 6;
                    int pickNumber = 0;
                    int xCoordinate = value.getX();
                    int yCoordinate = value.getY();
                    // We start with a negative offset
                    for (int x = 0; x < pixelToUse; x++) {
                        for (int y = 0; y < pixelToUse; y++) {
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
                    r = ImageProcessor.gammaCorrection(r / pickNumber);
                    g = ImageProcessor.gammaCorrection(g / pickNumber);
                    b = ImageProcessor.gammaCorrection(b / pickNumber);
                    if (FireflyLuciferin.config.isEyeCare() && (r+g+b) < 10) r = g = b = 5;
                    leds[key - 1] = new Color(r, g, b);
                });

                // Put the image in the queue
                FireflyLuciferin.sharedQueue.offer(leds);

                // Increase the FPS counter
                FireflyLuciferin.FPS_PRODUCER_COUNTER++;

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
