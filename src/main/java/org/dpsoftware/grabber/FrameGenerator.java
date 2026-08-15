/*
  FrameGenerator.java

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

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.utilities.CommonUtility;

import java.util.Arrays;

/**
 * Generates interpolated frames between captured frames.
 * Inserted frames represent the linear interpolation from the two captured frames.
 * Higher levels will smooth transitions from one color to another but LEDs will be less responsive to quick changes.
 * <p>
 * This class owns the smoothing state: the previous frame, the pacing clock,
 * and the reusable output array. It is invoked from the GStreamer sink thread.
 * <p>
 * The internal state is reallocated on the fly if the number of LED zones
 * changes at runtime (e.g. LED matrix or aspect ratio switch), so a
 * different-length frame never causes an out-of-bounds access.
 */
@Slf4j
public class FrameGenerator {

    private int ledCount;
    private ColorFloat[] previousFrame;
    private long start;
    private ColorFloat[] reusableLeds;

    /**
     * Creates a new instance of FrameGenerator.
     *
     * @param ledCount number of configured LED zones
     */
    public FrameGenerator(int ledCount) {
        this.ledCount = ledCount;
        this.previousFrame = new ColorFloat[ledCount];
        Arrays.fill(previousFrame, ColorFloat.BLACK);
        this.reusableLeds = new ColorFloat[ledCount];
    }

    /**
     * Generate frames between captured frames, inserted frames represent the linear interpolation
     * from the two captured frames.
     *
     * @param leds array containing color information as ColorFloat (full precision 32 bit)
     */
    public void frameGeneration(ColorFloat[] leds) {
        MainSingleton main = MainSingleton.getInstance();
        int skipFastFramesMs = 8;
        int targetFramerate = main.config.getSmoothingTargetFramerate();
        int gpuFramerateFps = main.config.getFrameInsertionTarget();
        if (targetFramerate == Enums.SmoothingTarget.TARGET_120_FPS.getSmoothingTargetValue()) {
            skipFastFramesMs /= 2;
            gpuFramerateFps *= 2;
        } else if (targetFramerate == Enums.SmoothingTarget.TARGET_30_FPS.getSmoothingTargetValue()) {
            skipFastFramesMs *= 2;
            gpuFramerateFps /= 2;
        }
        if (previousFrame.length != leds.length) {
            ledCount = leds.length;
            previousFrame = new ColorFloat[ledCount];
            Arrays.fill(previousFrame, ColorFloat.BLACK);
            reusableLeds = new ColorFloat[ledCount];
        }
        ColorFloat[] frameGeneration = (reusableLeds != null && reusableLeds.length == ledCount)
                ? reusableLeds : new ColorFloat[ledCount];
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
                final float dRed = leds[j].r() - previousFrame[j].r();
                final float dGreen = leds[j].g() - previousFrame[j].g();
                final float dBlue = leds[j].b() - previousFrame[j].b();
                frameGeneration[j] = new ColorFloat(
                        previousFrame[j].r() + (dRed * i) / frameToCompute,
                        previousFrame[j].g() + (dGreen * i) / frameToCompute,
                        previousFrame[j].b() + (dBlue * i) / frameToCompute
                );
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
}
