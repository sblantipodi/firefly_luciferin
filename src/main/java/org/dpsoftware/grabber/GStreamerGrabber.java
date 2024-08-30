/*
  GStreamerGrabber.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
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
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * This class needs GStreamer: open source multimedia framework
 * This class uses Windows Desktop Duplication API
 */
@Slf4j
public class GStreamerGrabber extends JComponent {

    public static LinkedHashMap<Integer, LEDCoordinate> ledMatrix;
    final int oneSecondMillis = 1000;
    private final Lock bufferLock = new ReentrantLock();
    public AppSink videosink;
    boolean writeToFile = false;
    int capturedFrames = 0;
    long start;
    private Color[] previousFrame;
    static long startSimdTime;
    static boolean usingSimd;
    static int lastRgbValue;

    /**
     * Creates a new instance of GstVideoComponent
     */
    public GStreamerGrabber() {
        this(new AppSink("GstVideoComponent"));
        ledMatrix = MainSingleton.getInstance().config.getLedMatrixInUse(MainSingleton.getInstance().config.getDefaultLedMatrix());
        previousFrame = new Color[ledMatrix.size()];
        Arrays.fill(previousFrame, new Color(0, 0, 0));
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
            // Scale image inside the GPU by RESAMPLING_FACTOR
            gstreamerPipeline = Constants.GSTREAMER_PIPELINE_DDUPL.replace(Constants.INTERNAL_SCALING_X,
                            String.valueOf(MainSingleton.getInstance().config.getScreenResX() / Constants.RESAMPLING_FACTOR))
                    .replace(Constants.INTERNAL_SCALING_Y, String.valueOf(MainSingleton.getInstance().config.getScreenResY() / Constants.RESAMPLING_FACTOR));
        } else {
            gstreamerPipeline = Constants.GSTREAMER_PIPELINE.replace(Constants.INTERNAL_SCALING_X,
                            String.valueOf(MainSingleton.getInstance().config.getScreenResX() / Constants.RESAMPLING_FACTOR))
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
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, framerateToSave != null
                    ? framerateToSave.getBaseI18n() : MainSingleton.getInstance().config.getDesiredFramerate());
        } else {
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, Constants.FRAMERATE_CAP);
        }
        if (!MainSingleton.getInstance().config.getFrameInsertion().equals(Enums.FrameInsertion.NO_SMOOTHING.getBaseI18n())) {
            gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER,
                    String.valueOf(LocalizedEnum.fromBaseStr(Enums.FrameInsertion.class, MainSingleton.getInstance().config.getFrameInsertion()).getFrameInsertionFramerate()));
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
     * Bench SIMD vs Scalar CPU computations
     *
     * @param leds array that is offered to the queue
     * @param pickNumber LED to analuze (first one=
     * @param r red channel
     * @param g green channel
     * @param b blu channel
     */
    private static void benchSimd(Color[] leds, int pickNumber, int r, int g, int b) {
        int key = 1;
        long finish = System.nanoTime();
        long timeElapsed = finish - startSimdTime;
        if (pickNumber == 0) {
            if (GrabberSingleton.getInstance().getNanoSimd().size() < Constants.SIMD_SCALAR_BENCH_ITERATIONS) {
                if (usingSimd) GrabberSingleton.getInstance().getNanoSimd().add(timeElapsed);
            } else {
                printSimdBenchResult();
            }
            if (GrabberSingleton.getInstance().getNanoScalar().size() < Constants.SIMD_SCALAR_BENCH_ITERATIONS) {
                if (!usingSimd) GrabberSingleton.getInstance().getNanoScalar().add(timeElapsed);
            } else {
                printSimdBenchResult();
            }
        } else {
            int rgbValueSum = leds[key - 1].getRed() + leds[key - 1].getGreen() + leds[key - 1].getBlue();
            if (lastRgbValue != rgbValueSum) {
                lastRgbValue = leds[key - 1].getRed() + leds[key - 1].getGreen() + leds[key - 1].getBlue();
                if (Enums.SimdAvxOption.findByValue(MainSingleton.getInstance().config.getSimdAvx()).getSimdOptionNumeric() != 0) {
                    log.trace("SIMD: {}, R: {}, G: {}, B: {}, pickNumber: {}, R_AVG: {}, G_AVG: {}, B_AVG: {}",
                            Enums.SimdAvxOption.findByValue(MainSingleton.getInstance().config.getSimdAvx()).getBaseI18n(),
                            r, g, b, pickNumber, leds[key - 1].getRed(), leds[key - 1].getGreen(), leds[key - 1].getBlue());
                }
            }
        }
    }

    /**
     * Print Bench results for SIMD vs Scalar CPU computations
     */
    private static void printSimdBenchResult() {
        long avgSimdTime = 0;
        long avgScalarTime = 0;
        if (!GrabberSingleton.getInstance().getNanoSimd().isEmpty()) {
            avgSimdTime = (long) GrabberSingleton.getInstance().getNanoSimd().stream()
                    .mapToLong(l -> l)
                    .average()
                    .orElse(0.0);
        }
        if (!GrabberSingleton.getInstance().getNanoScalar().isEmpty()) {
            avgScalarTime = (long) GrabberSingleton.getInstance().getNanoScalar().stream()
                    .mapToLong(l -> l)
                    .average()
                    .orElse(0.0);
        }
        List<Long> unifiedList = new ArrayList<>(GrabberSingleton.getInstance().getNanoSimd());
        unifiedList.addAll(GrabberSingleton.getInstance().getNanoScalar());
        long averageTime = (long) unifiedList.stream()
                .mapToLong(l -> l)
                .average()
                .orElse(0.0);
        if (Enums.SimdAvxOption.findByValue(MainSingleton.getInstance().config.getSimdAvx()).getSimdOptionNumeric() != 0) {
            log.debug("AVG TIME FOR ONE FRAME={}ns - AVG SIMD BENCH={}ns - AVG SCALAR BENCH={}ns", averageTime, avgSimdTime, avgScalarTime);
        }
        GrabberSingleton.getInstance().getNanoSimd().clear();
        GrabberSingleton.getInstance().getNanoScalar().clear();
    }

    /**
     * Listener callback triggered every captured frame
     */
    private class AppSinkListener implements AppSink.NEW_SAMPLE {

        /**
         * GPU has captured the screen and now we have an IntBuffer that contains the captured image.
         * This method process that buffer to calculate average colors on the configured zones.
         * This computation is done on the CPU side.
         * The buffer used in this method is not backed by an accessible array, so you can't call asArray() on it,
         * this kind of copy requires a lot of CPU/Memory time but it is required to use the SIMD AVX CPU instructions.
         * The use of AVX512 / AVX256 guarantees a huge increase in performance on very large zones.
         * <p>
         * NOTE: Don't split this method, this code must run inside one method for maximum performance.
         *
         * @param width         captured image width
         * @param height        captured image height
         * @param rgbBuffer     the buffer that bake the captured screen image
         * @return an array that contains the average color for each zones
         */
        private static Color[] processBufferUsingCpu(int width, int height, IntBuffer rgbBuffer) {
            Color[] leds = new Color[ledMatrix.size()];
            if (log.isDebugEnabled()) {
                startSimdTime = System.nanoTime();
            }
            int widthPlusStride = ImageProcessor.getWidthPlusStride(width, height, rgbBuffer);
            // We need an ordered collection, parallelStream does not help here
            var SPECIES = MainSingleton.getInstance().SPECIES;
            MemorySegment memorySegment;
            if (SPECIES != null) {
                memorySegment = MemorySegment.ofBuffer(rgbBuffer);
            } else {
                memorySegment = null;
            }
            ledMatrix.forEach((key, value) -> {
                int r = 0, g = 0, b = 0;
                int pickNumber = 0;
                int xCoordinate = (value.getX() / Constants.RESAMPLING_FACTOR);
                int yCoordinate = (value.getY() / Constants.RESAMPLING_FACTOR);
                int pixelInUseX = value.getWidth() / Constants.RESAMPLING_FACTOR;
                int pixelInUseY = value.getHeight() / Constants.RESAMPLING_FACTOR;
                if (SPECIES != null) {
                    if (log.isDebugEnabled()) {
                        usingSimd = true;
                    }
                    if (!value.isGroupedLed()) {
                        for (int y = 0; y < pixelInUseY; y++) {
                            int offsetY = yCoordinate + y;
                            if (offsetY >= height) continue;
                            int baseBufferOffset = offsetY * widthPlusStride;
                            for (int x = 0; x < pixelInUseX; x += SPECIES.length() * 3) {
                                int offsetX = xCoordinate + x;
                                if (offsetX >= widthPlusStride) continue;
                                VectorMask<Integer> mask1 = SPECIES.indexInRange(x, pixelInUseX);
                                VectorMask<Integer> mask2 = SPECIES.indexInRange(x + SPECIES.length(), pixelInUseX);
                                VectorMask<Integer> mask3 = SPECIES.indexInRange(x + 2 * SPECIES.length(), pixelInUseX);
                                IntVector rgbVector1 = IntVector.fromMemorySegment(SPECIES, memorySegment,
                                        (long) (offsetX + baseBufferOffset) * Integer.BYTES, ByteOrder.nativeOrder(), mask1);
                                IntVector rgbVector2 = IntVector.fromMemorySegment(SPECIES, memorySegment,
                                        (long) (Math.min(offsetX + SPECIES.length(), widthPlusStride) + baseBufferOffset) * Integer.BYTES, ByteOrder.nativeOrder(), mask2);
                                IntVector rgbVector3 = IntVector.fromMemorySegment(SPECIES, memorySegment,
                                        (long) (Math.min(offsetX + 2 * SPECIES.length(), widthPlusStride) + baseBufferOffset) * Integer.BYTES, ByteOrder.nativeOrder(), mask3);
                                IntVector rVector = rgbVector1.and(0xFF0000).lanewise(VectorOperators.LSHR, 16)
                                        .add(rgbVector2.and(0xFF0000).lanewise(VectorOperators.LSHR, 16))
                                        .add(rgbVector3.and(0xFF0000).lanewise(VectorOperators.LSHR, 16));
                                IntVector gVector = rgbVector1.and(0x00FF00).lanewise(VectorOperators.LSHR, 8)
                                        .add(rgbVector2.and(0x00FF00).lanewise(VectorOperators.LSHR, 8))
                                        .add(rgbVector3.and(0x00FF00).lanewise(VectorOperators.LSHR, 8));
                                IntVector bVector = rgbVector1.and(0x0000FF)
                                        .add(rgbVector2.and(0x0000FF))
                                        .add(rgbVector3.and(0x0000FF));
                                r += rVector.reduceLanes(VectorOperators.ADD);
                                g += gVector.reduceLanes(VectorOperators.ADD);
                                b += bVector.reduceLanes(VectorOperators.ADD);
                                pickNumber += mask1.trueCount() + mask2.trueCount() + mask3.trueCount();
                            }
                        }
                        leds[key - 1] = ImageProcessor.correctColors(r, g, b, pickNumber);
                    } else {
                        leds[key - 1] = leds[key - 2];
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        usingSimd = false;
                    }
                    if (!value.isGroupedLed()) {
                        for (int y = 0; y < pixelInUseY; y++) {
                            for (int x = 0; x < pixelInUseX; x++) {
                                int offsetX = (xCoordinate + x);
                                int offsetY = (yCoordinate + y);
                                int bufferOffset = (Math.min(offsetX, widthPlusStride)) + ((offsetY < height) ? (offsetY * widthPlusStride) : (height * widthPlusStride));
                                int rgb = rgbBuffer.get(Math.min(rgbBuffer.capacity() - 1, bufferOffset));
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
                }
                if (log.isTraceEnabled()) {
                    if (key == 1) benchSimd(leds, pickNumber, r, g, b);
                }
            });
            if (log.isDebugEnabled()) {
                benchSimd(leds, 0, 0, 0, 0);
            }
            return leds;
        }

        /**
         * Method that receives the initial buffers and applies all the various corrections on that buffer.
         * After all the computations, the results are offered to the queue that contains the avg colors to be
         * sent to the LED strip.
         *
         * @param width         captured image width
         * @param height        captured image height
         * @param rgbBuffer     the buffer that bake the captured screen image
         */
        public void rgbFrame(int width, int height, IntBuffer rgbBuffer) {
            // If the EDT is still copying data from the buffer, just drop this frame
            if (!bufferLock.tryLock()) {
                return;
            }
            // CHECK_ASPECT_RATIO is true 10 times per second, if true and black bars auto detection is on, auto detect black bars
            if (MainSingleton.getInstance().config.isAutoDetectBlackBars()) {
                if (GrabberSingleton.getInstance().CHECK_ASPECT_RATIO) {
                    GrabberSingleton.getInstance().CHECK_ASPECT_RATIO = false;
                    ImageProcessor.autodetectBlackBars(width, height, rgbBuffer);
                }
            }
            try {
                if (log.isTraceEnabled()) {
                    IntBuffer intBufferClone = rgbBuffer.duplicate();
                    intBufferRgbToImage(intBufferClone);
                }
                // Process zones and calculate avg colors
                Color[] leds = processBufferUsingCpu(width, height, rgbBuffer);
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
            int gpuFramerateFps = LocalizedEnum.fromBaseStr(Enums.FrameInsertion.class,
                    MainSingleton.getInstance().config.getFrameInsertion()).getFrameInsertionFramerate();
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
                    final Color c = new Color(previousFrame[j].getRed() + ((dRed * i) / frameToCompute),
                            previousFrame[j].getGreen() + ((dGreen * i) / frameToCompute), previousFrame[j].getBlue() + ((dBlue * i) / frameToCompute));
                    frameInsertion[j] = c;
                }
                long finish = System.currentTimeMillis();
                if (frameInsertion.length == leds.length) {
                    long timeElapsed = finish - start;
                    totalElapsed += (int) timeElapsed;
                    if (timeElapsed > Constants.SMOOTHING_SKIP_FAST_FRAMES) {
                        PipelineManager.offerToTheQueue(frameInsertion);
                    } else {
                        log.debug("Frames is coming too fast, GPU is trying to catch up, skipping frame={}, Elapsed={}", i, timeElapsed);
                        start = System.currentTimeMillis();
                        previousFrame = leds.clone();
                        break;
                    }
                    start = System.currentTimeMillis();
                    if (i != frameToCompute || totalElapsed >= (frameDistanceMs * frameToRender)) {
                        if (totalElapsed >= (frameDistanceMs * frameToRender)) {
                            // Last frame never sleep, if GPU is late skip waiting.
                            if (i != frameToCompute) {
                                log.debug("GPU is late, skip wait on frame #{}, Elapsed={}, TotaleTimeElapsed={}", i, timeElapsed, totalElapsed);
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
