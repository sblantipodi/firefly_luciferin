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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Generates interpolated frames between captured frames.
 * Inserted frames represent the linear interpolation from the two captured frames.
 * Higher levels will smooth transitions from one color to another but LEDs will be less responsive to quick changes.
 * <p>
 * The GStreamer sink only publishes colors. A dedicated worker owns interpolation
 * and pacing; one replaceable pending frame prevents a backlog when output is slow.
 * <p>
 * The internal state is reallocated on the fly if the number of LED zones
 * changes at runtime (e.g. LED matrix or aspect ratio switch), so a
 * different-length frame never causes an out-of-bounds access.
 */
@Slf4j
public class FrameGenerator {

    private final Object pendingLock = new Object();
    private final Consumer<ColorFloat[]> output;
    private final int initialLedCount;
    private final LongSupplier nanoTime;
    private final NanoSleeper sleeper;
    private CapturedFrame pending;
    private Thread worker;
    private volatile boolean stopRequested;

    /**
     * Create a generator using the system monotonic clock and interruptible sleeps.
     *
     * @param ledCount initial number of configured LED zones
     * @param output   consumer of generated frames; may modify the supplied array
     */
    FrameGenerator(int ledCount, Consumer<ColorFloat[]> output) {
        this(ledCount, output, System::nanoTime, TimeUnit.NANOSECONDS::sleep);
    }

    /**
     * Create a generator with replaceable output and pacing operations.
     *
     * @param ledCount initial number of configured LED zones
     * @param output consumer of generated frames; may modify the supplied array
     * @param nanoTime monotonic clock returning nanoseconds
     * @param sleeper interruptible operation used to wait between generated frames
     */
    FrameGenerator(int ledCount, Consumer<ColorFloat[]> output, LongSupplier nanoTime, NanoSleeper sleeper) {
        this.initialLedCount = ledCount;
        this.output = output;
        this.nanoTime = nanoTime;
        this.sleeper = sleeper;
    }

    /**
     * Publish captured LED colors for asynchronous interpolation using configured frame rates.
     * The capture thread returns without waiting for generated frames to be sent.
     *
     * @param leds array containing color information as ColorFloat (full precision 32 bit)
     */
    public void frameGeneration(ColorFloat[] leds) {
        MainSingleton main = MainSingleton.getInstance();
        frameGeneration(leds, main.getConfig().getSmoothingTargetFramerate(), GStreamerGrabber.getTargetFramerate());
    }

    /**
     * Copy captured colors into the single pending slot and start the worker if needed.
     * A newer publication replaces any snapshot still waiting to be processed.
     *
     * @param leds       captured LED colors, copied before returning to the caller
     * @param outputFps  positive smoothing target frame rate
     * @param captureFps positive captured video frame rate
     * @throws IllegalArgumentException if either frame rate is not positive
     */
    synchronized void frameGeneration(ColorFloat[] leds, int outputFps, int captureFps) {
        if (outputFps <= 0 || captureFps <= 0) {
            throw new IllegalArgumentException("Frame rates must be positive");
        }
        synchronized (pendingLock) {
            // ColorFloat is immutable; copy the array to transfer ownership to the worker.
            pending = new CapturedFrame(leds.clone(), outputFps, captureFps);
            pendingLock.notifyAll();
        }
        if (worker == null || !worker.isAlive()) {
            stopRequested = false;
            // Virtual thread socket I/O is interruptible too: stopping must also
            // release a worker waiting for the multi screen TCP server's reply.
            worker = Thread.ofVirtual().name("led-frame-generator").unstarted(this::generateFrames);
            worker.start();
        }
    }

    /**
     * Interrupt pacing and wait for any in-flight output before switching to direct
     * capture or disposing the pipeline. A subsequent publication starts fresh.
     */
    public synchronized void stop() {
        if (worker != null) {
            stopRequested = true;
            worker.interrupt();
            boolean interrupted = false;
            while (worker.isAlive()) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            worker = null;
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (pendingLock) {
            pending = null;
        }
    }

    /**
     * Consume pending snapshots and interpolate colors on the dedicated worker.
     * Pace output with monotonic deadlines and the original timing headroom,
     * dropping overdue intermediate frames. Interruption terminates the worker.
     */
    private void generateFrames() {
        ColorFloat[] previous = new ColorFloat[initialLedCount];
        Arrays.fill(previous, ColorFloat.BLACK);
        long deadline = nanoTime.getAsLong();
        try {
            while (!stopRequested && !Thread.currentThread().isInterrupted()) {
                CapturedFrame captured;
                synchronized (pendingLock) {
                    while (pending == null) {
                        pendingLock.wait();
                    }
                    captured = pending;
                    pending = null;
                }
                ColorFloat[] leds = captured.leds();
                if (previous.length != leds.length) {
                    previous = new ColorFloat[leds.length];
                    Arrays.fill(previous, ColorFloat.BLACK);
                }
                int frames = Math.max(1, captured.outputFps() / captured.captureFps());
                // Preserve the original pacing headroom and millisecond truncation.
                // Filling the entire capture interval delays the target color and
                // lets scheduling overhead carry unfinished work into the next capture.
                double frameDistanceMs = (double) (1000 / captured.captureFps()) / frames;
                long period = TimeUnit.MILLISECONDS.toNanos((long) Math.max(1,
                        frameDistanceMs - Constants.SMOOTHING_SLOW_FRAME_TOLERANCE));
                deadline = Math.max(deadline, nanoTime.getAsLong());
                for (int i = 0; i < frames; i++) {
                    long remaining = deadline - nanoTime.getAsLong();
                    if (remaining > 0) {
                        sleeper.sleep(remaining);
                    }
                    if (stopRequested || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    // Drop overdue intermediate frames rather than emitting a catch-up burst.
                    if (i < frames - 1 && nanoTime.getAsLong() - deadline >= period) {
                        deadline += period;
                        continue;
                    }
                    float fraction = frames == 1 ? 1f : (float) i / (frames - 1);
                    ColorFloat[] interpolated = new ColorFloat[leds.length];
                    for (int j = 0; j < leds.length; j++) {
                        interpolated[j] = new ColorFloat(
                                previous[j].r() + (leds[j].r() - previous[j].r()) * fraction,
                                previous[j].g() + (leds[j].g() - previous[j].g()) * fraction,
                                previous[j].b() + (leds[j].b() - previous[j].b()) * fraction);
                    }
                    try {
                        // Output applies EMA/white balance in place; never pass the raw endpoints.
                        output.accept(interpolated);
                    } catch (RuntimeException e) {
                        log.warn("Cannot output generated LED frame", e);
                    }
                    deadline = Math.max(deadline + period, nanoTime.getAsLong());
                }
                previous = leds;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Interruptible pacing operation, replaceable for deterministic timing tests.
     */
    @FunctionalInterface
    interface NanoSleeper {
        /**
         * Wait for the requested pacing interval.
         *
         * @param nanos interval in nanoseconds
         * @throws InterruptedException if the worker is interrupted while waiting
         */
        void sleep(long nanos) throws InterruptedException;
    }

    /**
     * Captured colors and the frame rates used to interpolate this snapshot.
     *
     * @param leds       owned copy of the captured LED colors
     * @param outputFps  smoothing target frame rate
     * @param captureFps captured video frame rate
     */
    private record CapturedFrame(ColorFloat[] leds, int outputFps, int captureFps) {
    }
}
