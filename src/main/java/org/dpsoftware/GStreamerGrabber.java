/*
  GStreamerGrabber.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/
package org.dpsoftware;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class needs GStreamer: open source multimedia framework
 * GStreamer should be installed separately
 * This class uses Windows Desktop Duplication API
 */
class GStreamerGrabber extends javax.swing.JComponent {

    private BufferedImage currentImage = null;
    private final Lock bufferLock = new ReentrantLock();
    private final AppSink videosink;
    private int[] pixels;
    static Configuration config;
    static Map<Integer, LEDCoordinate> ledMatrix;

    /**
     * Creates a new instance of GstVideoComponent
     */
    public GStreamerGrabber(Configuration config) {

        this(new AppSink("GstVideoComponent"));
        this.config = config;
        ledMatrix = config.getLedMatrix();

    }

    /**
     * Creates a new instance of GstVideoComponent
     */
    public GStreamerGrabber(AppSink appsink) {

        this.videosink = appsink;
        videosink.set("emit-signals", true);
        AppSinkListener listener = new AppSinkListener();
        videosink.connect((AppSink.NEW_SAMPLE) listener);
        StringBuilder caps = new StringBuilder("video/x-raw,pixel-aspect-ratio=1/1,");
        // JNA creates ByteBuffer using native byte order, set masks according to that.
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            caps.append("format=BGRx");
        } else {
            caps.append("format=xRGB");
        }
        videosink.setCaps(new Caps(caps.toString()));
        setLayout(null);
        setOpaque(true);
        setBackground(Color.BLACK);

    }

    public Element getElement() {

        return videosink;

    }

    private class AppSinkListener implements AppSink.NEW_SAMPLE {

        public void rgbFrame(int width, int height, IntBuffer rgbBuffer) {

            // If the EDT is still copying data from the buffer, just drop this frame
            if (!bufferLock.tryLock()) {
                return;
            }

            try {
                int ledOffsetX = config.getLedOffsetX();
                int ledOffsetY = config.getLedOffsetY();
                Color[] leds = new Color[ledMatrix.size()];
                // Stream is faster than standards iterations, we need an ordered collection so no parallelStream here
                ledMatrix.entrySet().stream().forEach(entry -> {
                    int r = 0, g = 0, b = 0;
                    int skipPixel = 5;
                    // 6 pixel for X axis and 6 pixel for Y axis
                    int pixelToUse = 6;
                    int pickNumber = 0;
                    int xCoordinate = entry.getValue().getX();
                    int yCoordinate = entry.getValue().getY();
                    // We start with a negative offset
                    for (int x = 0; x < pixelToUse; x++) {
                        for (int y = 0; y < pixelToUse; y++) {
                            int offsetX = (xCoordinate + ledOffsetX + (skipPixel * x));
                            int offsetY = (yCoordinate + ledOffsetY + (skipPixel * y));
                            int rgb = rgbBuffer.get( ((offsetX < width) ? offsetX : width)
                                    + ((offsetY < height) ? offsetY * width : (height-1) * width));
                            r += rgb >> 16 & 0xFF;
                            g += rgb >> 8 & 0xFF;
                            b += rgb & 0xFF;
                            pickNumber++;
                        }
                    }
                    r = ImageProcessor.gammaCorrection(r / pickNumber, config);
                    g = ImageProcessor.gammaCorrection(g / pickNumber, config);
                    b = ImageProcessor.gammaCorrection(b / pickNumber, config);
                    leds[entry.getKey() - 1] = new Color(r, g, b);
                });

                // Put the image in the queue
                FastScreenCapture.sharedQueue.offer(leds);

                // Increase the FPS counter
                FastScreenCapture.FPS_PRODUCER++;

            } finally {
                bufferLock.unlock();
            }
        }

        @Override
        public FlowReturn newSample(AppSink elem) {
            Sample sample = elem.pullSample();
            Structure capsStruct = sample.getCaps().getStructure(0);
            int w = capsStruct.getInteger("width");
            int h = capsStruct.getInteger("height");
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