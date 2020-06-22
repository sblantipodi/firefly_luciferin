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
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
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

    /**
     * Creates a new instance of GstVideoComponent
     */
    public GStreamerGrabber() {
        this(new AppSink("GstVideoComponent"));
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

    private BufferedImage getBufferedImage(int width, int height) {
        if (currentImage != null && currentImage.getWidth() == width && currentImage.getHeight() == height) {
            return currentImage;
        }
        if (currentImage != null) {
            currentImage.flush();
        }
        currentImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        currentImage.setAccelerationPriority(0.0f);
        return currentImage;
    }


    private class AppSinkListener implements AppSink.NEW_SAMPLE {

        public void rgbFrame(int width, int height, IntBuffer rgbBuffer) {
            // If the EDT is still copying data from the buffer, just drop this frame
            if (!bufferLock.tryLock()) {
                return;
            }
            // Increase the FPS counter
            FastScreenCapture.FPS_PRODUCER++;
            try {

                // TODO not necessary to copy buffer to the JVM, leave it volatile
                // TODO not necessary to copy buffer to the JVM, leave it volatile
                // TODO not necessary to copy buffer to the JVM, leave it volatile

//                int rgb = rgbBuffer.get( x + (y * width));
//                int red = rgb >> 16 & 0xFF;
//                int green = rgb >> 8 & 0xFF;
//                int blue = rgb & 0xFF;

                currentImage = getBufferedImage(width, height);
                pixels = ((DataBufferInt) currentImage.getRaster().getDataBuffer()).getData();
                rgbBuffer.get(pixels, 0, width * height);
                // Put the image in the queue
                FastScreenCapture.sharedQueue.offer(ImageProcessor.getColors(null, currentImage));
            } catch (Exception e) {
                e.printStackTrace();
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