/*
  GStreamerGrabber.java

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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * This class needs GStreamer: open source multimedia framework
 * This class uses Windows Desktop Duplication API
 */
@Slf4j
public class GStreamerGrabber extends JComponent {

    public static LinkedHashMap<Integer, LEDCoordinate> ledMatrix;
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
        if (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX11.name())
                || MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX12.name())) {
            // Scale image inside the GPU by RESAMPLING_FACTOR
            String gstPipelineStr;
            if (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX11.name())) {
                gstPipelineStr = Constants.GSTREAMER_PIPELINE_DDUPL_DX11;
            } else {
                gstPipelineStr = Constants.GSTREAMER_PIPELINE_DDUPL_DX12;
            }
            gstreamerPipeline = gstPipelineStr.replace(Constants.INTERNAL_SCALING_X,
                            String.valueOf(MainSingleton.getInstance().config.getScreenResX() / MainSingleton.getInstance().config.getResamplingFactor()))
                    .replace(Constants.INTERNAL_SCALING_Y, String.valueOf(MainSingleton.getInstance().config.getScreenResY() / MainSingleton.getInstance().config.getResamplingFactor()));
        } else {
            gstreamerPipeline = Constants.GSTREAMER_PIPELINE.replace(Constants.INTERNAL_SCALING_X,
                            String.valueOf(MainSingleton.getInstance().config.getScreenResX() / MainSingleton.getInstance().config.getResamplingFactor()))
                    .replace(Constants.INTERNAL_SCALING_Y, String.valueOf(MainSingleton.getInstance().config.getScreenResY() / MainSingleton.getInstance().config.getResamplingFactor()));
        }
        gstreamerPipeline = setFramerate(gstreamerPipeline);
        StringBuilder caps = new StringBuilder(gstreamerPipeline);
        // JNA creates ByteBuffer using native byte order, set masks according to that.
        if (!(MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX11.name()))) {
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
        gstreamerPipeline += Constants.FRAMERATE_PLACEHOLDER.replaceAll(Constants.FPS_PLACEHOLDER, String.valueOf(getTargetFramerate()));
        return gstreamerPipeline;
    }

    /**
     * Implement framerate logic
     * @return target framerate for GStreamer
     */
    public static int getTargetFramerate() {
        String targetFramerate;
        if (!Enums.Framerate.UNLOCKED.equals(LocalizedEnum.fromBaseStr(Enums.Framerate.class, MainSingleton.getInstance().config.getDesiredFramerate()))) {
            Enums.Framerate framerateToSave = LocalizedEnum.fromStr(Enums.Framerate.class, MainSingleton.getInstance().config.getDesiredFramerate());
            targetFramerate = framerateToSave != null ? framerateToSave.getBaseI18n() : MainSingleton.getInstance().config.getDesiredFramerate();
        } else {
            targetFramerate = Constants.FRAMERATE_CAP;
        }
        if (!MainSingleton.getInstance().config.getSmoothingType().equals(Enums.Smoothing.DISABLED.getBaseI18n()) && MainSingleton.getInstance().config.getFrameInsertionTarget() > 0) {
            int target = MainSingleton.getInstance().config.getFrameInsertionTarget();
            if (MainSingleton.getInstance().config.getSmoothingTargetFramerate() == Enums.SmoothingTarget.TARGET_120_FPS.getSmoothingTargetValue()) {
                target *= 2;
            } else if (MainSingleton.getInstance().config.getSmoothingTargetFramerate() == Enums.SmoothingTarget.TARGET_30_FPS.getSmoothingTargetValue()) {
                target /= 2;
            }
            targetFramerate = String.valueOf(target);
        }
        return Integer.parseInt(targetFramerate);
    }

    /**
     * Implement framerate logic
     *
     * @return target framerate for the Glow Worm Device
     */
    public static long getTargetFramerateForDevice() {
        int targetFramerate;
        if (!MainSingleton.getInstance().config.getSmoothingType().equals(Enums.Smoothing.DISABLED.getBaseI18n()) && MainSingleton.getInstance().config.getSmoothingTargetFramerate() > 0) {
            targetFramerate = MainSingleton.getInstance().config.getSmoothingTargetFramerate();
        } else {
            targetFramerate = Integer.parseInt(MainSingleton.getInstance().config.getDesiredFramerate());
        }
        return targetFramerate;
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
        BufferedImage img = new BufferedImage(MainSingleton.getInstance().config.getScreenResX() / MainSingleton.getInstance().config.getResamplingFactor(),
                MainSingleton.getInstance().config.getScreenResY() / MainSingleton.getInstance().config.getResamplingFactor(), 1);
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
         * @param width     captured image width
         * @param height    captured image height
         * @param rgbBuffer the buffer that bake the captured screen image
         * @return an array that contains the average color for each zones
         */
        private static Color[] processBufferUsingCpu(int width, int height, IntBuffer rgbBuffer) {
            Color[] leds = new Color[ledMatrix.size()];
            if (log.isDebugEnabled() || MainSingleton.getInstance().isCpuLatencyBenchRunning()) {
                SimdBenchmark.startSimdTime = System.nanoTime();
            }
            int widthPlusStride = ImageProcessor.getWidthPlusStride(width, height, rgbBuffer);

            jdk.incubator.vector.VectorSpecies<Integer> SPECIES = MainSingleton.getInstance().SPECIES;
            MemorySegment memorySegment = (SPECIES != null) ? MemorySegment.ofBuffer(rgbBuffer) : null;
            int vectorLength = (SPECIES != null) ? SPECIES.length() : 0;

            boolean isBenchmarkingActive = (SPECIES != null && vectorLength > 0 && SimdBenchmark.selectedSimdStrategy == null);
            if (isBenchmarkingActive) {
                synchronized (SimdBenchmark.SIMD_STRATEGY_BENCH_LOCK) {
                    if (SimdBenchmark.simdBenchEndTime == -1) {
                        SimdBenchmark.simdBenchEndTime = System.currentTimeMillis() + (Constants.SIMD_BENCHMARK_DURATION_MS * 2);
                        SimdBenchmark.simdBenchStartTimeDv = System.currentTimeMillis() + Constants.SIMD_BENCHMARK_DURATION_MS;
                        SimdBenchmark.simdBenchStartTimeFv = System.currentTimeMillis() + Constants.SIMD_BENCHMARK_DURATION_MS + (Constants.SIMD_BENCHMARK_DURATION_MS / 2);
                        log.debug("SIMD processing strategy: ({})", SimdBenchmark.describeSimdStrategySelection());
                    }
                }
            }

            ledMatrix.forEach((key, value) -> {
                int r = 0, g = 0, b = 0;
                int pickNumber = 0;
                int xCoordinate = (value.getX() / MainSingleton.getInstance().config.getResamplingFactor());
                int yCoordinate = (value.getY() / MainSingleton.getInstance().config.getResamplingFactor());
                int pixelInUseX = value.getWidth() / MainSingleton.getInstance().config.getResamplingFactor();
                int pixelInUseY = value.getHeight() / MainSingleton.getInstance().config.getResamplingFactor();

                if (SPECIES != null && vectorLength > 0) {
                    if (log.isDebugEnabled() || MainSingleton.getInstance().isCpuLatencyBenchRunning()) {
                        SimdBenchmark.usingSimd = true;
                    }

                    if (!value.isGroupedLed()) {
                        int[] rgbTotals;
                        if (isBenchmarkingActive) {
                            long currentMillis = System.currentTimeMillis();
                            if (currentMillis >= SimdBenchmark.simdBenchStartTimeDv && currentMillis < SimdBenchmark.simdBenchStartTimeFv) {
                                long startDoubleVector = System.nanoTime();
                                rgbTotals = processLedWithDoubleVectorSimd(height, widthPlusStride, memorySegment, SPECIES,
                                        xCoordinate, yCoordinate, pixelInUseX, pixelInUseY);
                                SimdBenchmark.simdStrategyDoubleVectorBenchNanos += (System.nanoTime() - startDoubleVector);
                                SimdBenchmark.totalBenchmarkedFramesDoubleVector++;
                            } else if (currentMillis >= SimdBenchmark.simdBenchStartTimeFv) {
                                long startFullVector = System.nanoTime();
                                rgbTotals = processLedWithFullVectorSimd(height, widthPlusStride, memorySegment, SPECIES, vectorLength,
                                        xCoordinate, yCoordinate, pixelInUseX, pixelInUseY);
                                SimdBenchmark.simdStrategyFullVectorBenchNanos += (System.nanoTime() - startFullVector);
                                SimdBenchmark.totalBenchmarkedFramesFullVector++;
                            } else {
                                rgbTotals = processLedWithSelectedSimdStrategy(height, widthPlusStride, memorySegment, SPECIES, vectorLength,
                                        xCoordinate, yCoordinate, pixelInUseX, pixelInUseY);
                            }
                        } else {
                            // Benchmark is finished, SIMD strategy has been selected
                            rgbTotals = processLedWithSelectedSimdStrategy(height, widthPlusStride, memorySegment, SPECIES, vectorLength,
                                    xCoordinate, yCoordinate, pixelInUseX, pixelInUseY);
                        }
                        r = rgbTotals[0];
                        g = rgbTotals[1];
                        b = rgbTotals[2];
                        pickNumber = rgbTotals[3];
                        leds[key - 1] = ImageProcessor.correctColors(r, g, b, pickNumber, value.isActive());
                    } else {
                        leds[key - 1] = leds[key - 2];
                    }
                } else {
                    // Fallback NO SIMD
                    if (log.isDebugEnabled() || MainSingleton.getInstance().isCpuLatencyBenchRunning()) {
                        SimdBenchmark.usingSimd = false;
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
                        leds[key - 1] = ImageProcessor.correctColors(r, g, b, pickNumber, value.isActive());
                    } else {
                        leds[key - 1] = leds[key - 2];
                    }
                }
                if (log.isTraceEnabled() || MainSingleton.getInstance().isCpuLatencyBenchRunning()) {
                    if (key == 1) SimdBenchmark.benchSimd(pickNumber);
                }
            });
            SimdBenchmark.evaluateSimdPerformance(isBenchmarkingActive);
            return leds;
        }

        /**
         * Processes a specified section of an image using the selected SIMD (Single Instruction, Multiple Data) strategy
         * (either fast SIMD or doubleVector SIMD) to calculate aggregated RGB values and pixel count for the region. The strategy to be used is determined at runtime.
         *
         * @param height          The height of the image being processed.
         * @param widthPlusStride The effective width of the image, including padding (stride).
         * @param memorySegment   The memory segment containing the image data to be processed.
         * @param species         The vector species defining the type and size of SIMD vectors used
         *                        during processing.
         * @param vectorLength    The length of the SIMD vector to be used during operations.
         * @param xCoordinate     The x-coordinate of the top-left corner of the region to process.
         * @param yCoordinate     The y-coordinate of the top-left corner of the region to process.
         * @param pixelInUseX     The number of pixels along the x-axis to process in the specified region.
         * @param pixelInUseY     The number of pixels along the y-axis to process in the specified region.
         * @return An integer array containing four elements:
         *         [0] - Total sum of red values,
         *         [1] - Total sum of green values,
         *         [2] - Total sum of blue values,
         *         [3] - Total number of pixels processed.
         */
        private static int[] processLedWithSelectedSimdStrategy(int height, int widthPlusStride, MemorySegment memorySegment,
                                                                jdk.incubator.vector.VectorSpecies<Integer> species, int vectorLength,
                                                                int xCoordinate, int yCoordinate, int pixelInUseX, int pixelInUseY) {
            if (SimdBenchmark.selectedSimdStrategy == SimdBenchmark.SimdProcessingStrategy.FULL_VECTOR) {
                return processLedWithFullVectorSimd(height, widthPlusStride, memorySegment, species, vectorLength,
                        xCoordinate, yCoordinate, pixelInUseX, pixelInUseY);
            }
            return processLedWithDoubleVectorSimd(height, widthPlusStride, memorySegment, species,
                    xCoordinate, yCoordinate, pixelInUseX, pixelInUseY);
        }

        /**
         * Processes a specific region of an image using fast SIMD (Single Instruction, Multiple Data) techniques to
         * compute the aggregated RGB values and the total pixel count for the defined area. This method
         * leverages vectorized operations for optimized performance on the CPU.
         *
         * @param height          The height of the image to be processed.
         * @param widthPlusStride The effective width of the image, including any additional padding (stride).
         * @param memorySegment   The memory segment containing image data to be processed.
         * @param species         The vector species defining the type and size of SIMD vectors used during processing.
         * @param vectorLength    The length of the SIMD vector to be used during operations.
         * @param xCoordinate     The x-coordinate of the top-left corner of the region to be processed.
         * @param yCoordinate     The y-coordinate of the top-left corner of the region to be processed.
         * @param pixelInUseX     The number of pixels along the x-axis to process in the specified region.
         * @param pixelInUseY     The number of pixels along the y-axis to process in the specified region.
         * @return An integer array containing four elements:
         *         [0] - The total sum of red values,
         *         [1] - The total sum of green values,
         *         [2] - The total sum of blue values,
         *         [3] - The total number of pixels processed.
         */
        private static int[] processLedWithFullVectorSimd(int height, int widthPlusStride, MemorySegment memorySegment,
                                                          jdk.incubator.vector.VectorSpecies<Integer> species, int vectorLength,
                                                          int xCoordinate, int yCoordinate, int pixelInUseX, int pixelInUseY) {
            int r = 0, g = 0, b = 0, pickNumber = 0;
            int maxValidX = Math.min(pixelInUseX, widthPlusStride - xCoordinate);
            for (int y = 0; y < pixelInUseY; y++) {
                int offsetY = yCoordinate + y;
                if (offsetY >= height) continue;
                int baseBufferOffset = offsetY * widthPlusStride;
                int x = 0;
                for (; x + vectorLength <= maxValidX; x += vectorLength) {
                    int offsetX = xCoordinate + x;
                    IntVector rgbVector = IntVector.fromMemorySegment(
                            species, memorySegment,
                            (long) (offsetX + baseBufferOffset) * Integer.BYTES,
                            ByteOrder.nativeOrder());
                    r += rgbVector.and(0xFF0000).lanewise(VectorOperators.LSHR, 16).reduceLanes(VectorOperators.ADD);
                    g += rgbVector.and(0x00FF00).lanewise(VectorOperators.LSHR, 8).reduceLanes(VectorOperators.ADD);
                    b += rgbVector.and(0x0000FF).reduceLanes(VectorOperators.ADD);
                    pickNumber += vectorLength;
                }
                if (x < maxValidX) {
                    VectorMask<Integer> mask = species.indexInRange(x, maxValidX);
                    IntVector rgbVector = IntVector.fromMemorySegment(
                            species, memorySegment,
                            (long) (xCoordinate + x + baseBufferOffset) * Integer.BYTES,
                            ByteOrder.nativeOrder(), mask);
                    r += rgbVector.and(0xFF0000).lanewise(VectorOperators.LSHR, 16).reduceLanes(VectorOperators.ADD, mask);
                    g += rgbVector.and(0x00FF00).lanewise(VectorOperators.LSHR, 8).reduceLanes(VectorOperators.ADD, mask);
                    b += rgbVector.and(0x0000FF).reduceLanes(VectorOperators.ADD, mask);
                    pickNumber += mask.trueCount();
                }
            }
            return new int[]{r, g, b, pickNumber};
        }

        /**
         * Processes a specified section of an image using doubleVector SIMD (Single Instruction, Multiple Data) techniques
         * to calculate the aggregated RGB values and pixel count for the targeted area.
         * This method is designed for performance optimization on the CPU using vectorized operations.
         *
         * @param height          The height of the image being processed.
         * @param widthPlusStride The effective width of the image, including padding (stride).
         * @param memorySegment   The memory segment containing the image data to be processed.
         * @param species         The vector species defining the type and size of SIMD vectors used for processing.
         * @param xCoordinate     The x-coordinate of the top-left corner of the area to be processed.
         * @param yCoordinate     The y-coordinate of the top-left corner of the area to be processed.
         * @param pixelInUseX     The number of pixels in the x direction to process within the specified area.
         * @param pixelInUseY     The number of pixels in the y direction to process within the specified area.
         * @return An integer array containing four elements:
         * [0] - Total sum of red values,
         * [1] - Total sum of green values,
         * [2] - Total sum of blue values,
         * [3] - Total number of pixels processed.
         */
        private static int[] processLedWithDoubleVectorSimd(int height, int widthPlusStride, MemorySegment memorySegment,
                                                            jdk.incubator.vector.VectorSpecies<Integer> species,
                                                            int xCoordinate, int yCoordinate, int pixelInUseX, int pixelInUseY) {
            int r = 0, g = 0, b = 0, pickNumber = 0;
            for (int y = 0; y < pixelInUseY; y++) {
                int offsetY = yCoordinate + y;
                if (offsetY >= height) continue;
                int baseBufferOffset = offsetY * widthPlusStride;
                for (int x = 0; x < pixelInUseX; x += species.length() * 2) {
                    int offsetX = xCoordinate + x;
                    if (offsetX >= widthPlusStride) continue;
                    VectorMask<Integer> mask1 = species.indexInRange(x, pixelInUseX);
                    VectorMask<Integer> mask2 = species.indexInRange(x + species.length(), pixelInUseX);
                    IntVector rgbVector1 = IntVector.fromMemorySegment(species, memorySegment,
                            (long) (offsetX + baseBufferOffset) * Integer.BYTES, ByteOrder.nativeOrder(), mask1);
                    IntVector rgbVector2 = IntVector.fromMemorySegment(species, memorySegment,
                            (long) (Math.min(offsetX + species.length(), widthPlusStride) + baseBufferOffset) * Integer.BYTES,
                            ByteOrder.nativeOrder(), mask2);
                    r += rgbVector1.and(0xFF0000).lanewise(VectorOperators.LSHR, 16)
                            .add(rgbVector2.and(0xFF0000).lanewise(VectorOperators.LSHR, 16))
                            .reduceLanes(VectorOperators.ADD);
                    g += rgbVector1.and(0x00FF00).lanewise(VectorOperators.LSHR, 8)
                            .add(rgbVector2.and(0x00FF00).lanewise(VectorOperators.LSHR, 8))
                            .reduceLanes(VectorOperators.ADD);
                    b += rgbVector1.and(0x0000FF)
                            .add(rgbVector2.and(0x0000FF))
                            .reduceLanes(VectorOperators.ADD);
                    pickNumber += mask1.trueCount() + mask2.trueCount();
                }
            }
            return new int[]{r, g, b, pickNumber};
        }

        /**
         * Processes an RGB frame captured from the screen and calculates the average colors
         * for configured zones using the CPU. Also manages frame generation, smoothing,
         * and queueing the processed data for further use.
         *
         * @param width     The width of the captured image.
         * @param height    The height of the captured image.
         * @param rgbBuffer The buffer containing the captured RGB frame.
         */
        public void rgbFrame(int width, int height, IntBuffer rgbBuffer) {
            if (!bufferLock.tryLock()) {
                return;
            }
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
                    if (!MainSingleton.getInstance().config.getSmoothingType().equals(Enums.Smoothing.DISABLED.getBaseI18n()) && MainSingleton.getInstance().config.getFrameInsertionTarget() > 0) {
                        if (previousFrame != null) {
                            frameGeneration(leds);
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
         * Generate frames between captured frames, inserted frames represents the linear interpolation from the two captured frames.
         * Higher levels will smooth transitions from one color to another but LEDs will be less responsive to quick changes.
         *
         * @param leds array containing color information
         */
        void frameGeneration(Color[] leds) {
            int skipFastFramesMs = 8;
            int targetFramerate = MainSingleton.getInstance().config.getSmoothingTargetFramerate();
            int gpuFramerateFps = MainSingleton.getInstance().config.getFrameInsertionTarget();
            if (targetFramerate == Enums.SmoothingTarget.TARGET_120_FPS.getSmoothingTargetValue()) {
                skipFastFramesMs /= 2;
                gpuFramerateFps *= 2;
            } else if (targetFramerate == Enums.SmoothingTarget.TARGET_30_FPS.getSmoothingTargetValue()) {
                skipFastFramesMs *= 2;
                gpuFramerateFps /= 2;
            }
            Color[] frameGeneration = new Color[ledMatrix.size()];
            int totalElapsed = 0;
            // Framerate we asks to the GPU, less FPS = smoother but less response, more FPS = less smooth but faster to changes.
            // Total number of frames to compute.
            int totalFrameToAdd = targetFramerate - gpuFramerateFps;
            // Number of frames to compute every time a frame is received from the GPU.
            int frameToCompute = (totalFrameToAdd / gpuFramerateFps);
            // Total number of frames to render, contains computed framse + GPU frame.
            int frameToRender = frameToCompute + 1;
            // GPU frame time (milliseconds) between one GPU frame and the other.
            int gpuFrameTimeMs = 1000 / gpuFramerateFps;
            // Milliseconds available to compute and show a frame, remove some milliseconds to the equation for protocol headroom. frameToCompute + 1 frame computed by the GPU.
            double frameDistanceMs = ((double) gpuFrameTimeMs / (frameToCompute + 1));
            // Skip frame if GPU is late and tries to catch up by capturing frames too fast.
            for (int i = 0; i < frameToRender; i++) {
                for (int j = 0; j < leds.length; j++) {
                    final int dRed = leds[j].getRed() - previousFrame[j].getRed();
                    final int dGreen = leds[j].getGreen() - previousFrame[j].getGreen();
                    final int dBlue = leds[j].getBlue() - previousFrame[j].getBlue();
                    Color c = new Color(
                            previousFrame[j].getRed() + (dRed * i) / frameToCompute,
                            previousFrame[j].getGreen() + (dGreen * i) / frameToCompute,
                            previousFrame[j].getBlue() + (dBlue * i) / frameToCompute
                    );
                    frameGeneration[j] = c;
                }
                long finish = System.currentTimeMillis();
                if (frameGeneration.length == leds.length) {
                    long timeElapsed = finish - start;
                    totalElapsed += (int) timeElapsed;
                    if (i != 0 && timeElapsed <= skipFastFramesMs) {
                        log.debug("Frames are coming too fast, GPU is trying to catch up, skipping frame={}, Elapsed={}, TotaleTimeElapsed={}, SkipFastFrames={}",
                                i, timeElapsed, totalElapsed, skipFastFramesMs);
                        CommonUtility.sleepMilliseconds(skipFastFramesMs);
                    }
                    PipelineManager.offerToTheQueue(frameGeneration);
                    start = System.currentTimeMillis();
                    double sleepMs = frameDistanceMs;
                    if (timeElapsed > sleepMs) {
                        sleepMs -= timeElapsed - sleepMs;
                    }
                    sleepMs = Math.max(1, sleepMs - Constants.SMOOTHING_SLOW_FRAME_TOLERANCE);
                    double maxElasped = (frameDistanceMs * frameToRender);
                    if (totalElapsed > maxElasped) {
                        // If GPU is late skip waiting.
                        log.debug("GPU is late, skip wait on frame #{}, Elapsed={}, TotaleTimeElapsed={}, MaxElasped={}, SkipFastFrames={}, FrameDistanceMs={}",
                                i, timeElapsed, totalElapsed, maxElasped, skipFastFramesMs, frameDistanceMs);
                        previousFrame = leds.clone();
                        start = System.currentTimeMillis();
                        break;
                    } else {
                        CommonUtility.sleepMilliseconds((int) sleepMs);
                    }
                }
                if (i == frameToRender - 1) {
                    start = System.currentTimeMillis();
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
                try {
                    rgbFrame(w, h, bb.asIntBuffer());
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    // ignoring the out of bound when changing LED num on the fly
                } finally {
                    buffer.unmap();
                }
            }
            sample.dispose();
            return FlowReturn.OK;
        }
    }

}