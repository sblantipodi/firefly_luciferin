package org.dpsoftware.grabber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class FrameGeneratorTest {
    private static ColorFloat[] colors(float red) {
        return new ColorFloat[]{new ColorFloat(red, red / 2, red / 4)};
    }

    private static <T> T take(BlockingQueue<T> queue) throws InterruptedException {
        T item = queue.poll(2, TimeUnit.SECONDS);
        assertNotNull(item, "Missing worker output");
        return item;
    }

    @ParameterizedTest
    @CsvSource({"30,15", "60,30", "120,60", "60,15", "60,5", "30,2", "30,7", "60,60"})
    void regularOutputContinuesThroughMissingCaptures(int outputFps, int captureFps) throws Exception {
        try (Harness h = new Harness()) {
            h.generator.frameGeneration(colors(120), outputFps, captureFps);
            long period = 1_000_000_000L / outputFps;
            int ticks = (int) Math.ceil((double) outputFps / captureFps) + 3;
            for (int i = 0; i < ticks; i++) {
                TimedFrame actual = i == 0 ? take(h.frames) : h.tick(0);
                assertEquals(i * period, actual.nanos());
                assertEquals(120 * Math.min(1, (i + 1) * period * captureFps / 1_000_000_000.0),
                        actual.leds()[0].r(), 0.001);
            }
        }
    }

    @Test
    void twoXProducesMidpointAndOutputCannotModifyInterpolationHistory() throws Exception {
        try (Harness h = new Harness(leds -> leds[0] = ColorFloat.BLACK)) {
            h.generator.frameGeneration(colors(120), 60, 30);
            assertEquals(60, take(h.frames).leds()[0].r(), 0.001);
            assertEquals(120, h.tick(0).leds()[0].r(), 0.001);
            assertEquals(120, h.tick(0).leds()[0].r(), 0.001);
            h.generator.frameGeneration(colors(240), 60, 30);
            assertEquals(180, h.tick(0).leds()[0].r(), 0.001);
        }
    }

    @Test
    void newestTargetReplacesUnfinishedRampAtNextTick() throws Exception {
        try (Harness h = new Harness()) {
            h.generator.frameGeneration(colors(120), 60, 5);
            assertEquals(10, take(h.frames).leds()[0].r(), 0.001);
            h.generator.frameGeneration(colors(240), 60, 5);
            ColorFloat[] newest = colors(60);
            h.generator.frameGeneration(newest, 60, 5);
            newest[0] = ColorFloat.BLACK;
            float actual = h.tick(0).leds()[0].r();
            assertEquals(10 + (60 - 10) / 12f, actual, 0.001);
            assertTrue(h.tick(0).leds()[0].r() > actual);
        }
    }

    @Test
    void repeatedCaptureDoesNotRestartTheSameTransition() throws Exception {
        try (Harness h = new Harness()) {
            h.generator.frameGeneration(colors(120), 60, 5);
            take(h.frames);
            for (int i = 1; i <= 12; i++) {
                h.generator.frameGeneration(colors(120), 60, 5);
                assertEquals(Math.min(120, (i + 1) * 10), h.tick(0).leds()[0].r(), 0.001);
            }
        }
    }

    @Test
    void schedulerStallSkipsMissedTicksAndAdvancesUsingElapsedTime() throws Exception {
        try (Harness h = new Harness()) {
            h.generator.frameGeneration(colors(120), 60, 5);
            take(h.frames);
            TimedFrame late = h.tick(TimeUnit.MILLISECONDS.toNanos(210));
            assertEquals(120, late.leds()[0].r(), 0.001);
            assertTrue(h.frames.isEmpty(), "No catch-up burst");
            TimedFrame next = h.tick(0);
            assertTrue(next.nanos() > late.nanos());
            assertTrue(next.nanos() - late.nanos() <= 1_000_000_000L / 60);
        }
    }

    @Test
    void slowOutputSkipsDeadlinesWithoutBlockingCapture() throws Exception {
        BlockingQueue<ColorFloat[]> frames = new LinkedBlockingQueue<>();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        FrameGenerator generator = new FrameGenerator(1, frame -> {
            if (first.getAndSet(false)) {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            frames.add(frame);
        });
        try {
            generator.frameGeneration(colors(10), 60, 30);
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                generator.frameGeneration(colors(20), 60, 30);
                generator.frameGeneration(colors(30), 60, 30);
            });
            release.countDown();
            assertEquals(5, take(frames)[0].r(), 0.001);
            assertEquals(17.5, take(frames)[0].r(), 0.001);
            // Real scheduler jitter can make this interval slightly shorter
            // than nominal. The deterministic harness checks exact endpoints.
            float approaching = take(frames)[0].r();
            assertTrue(approaching > 17.5 && approaching <= 30);
            assertEquals(30, take(frames)[0].r(), 0.001);
        } finally {
            release.countDown();
            generator.stop();
        }
    }

    @Test
    void matrixSizeCanChangeAndRestartResetsInterpolation() throws Exception {
        try (Harness h = new Harness()) {
            h.generator.frameGeneration(colors(120), 30, 15);
            take(h.frames);
            h.tick(0);
            h.generator.frameGeneration(new ColorFloat[]{colors(60)[0], colors(80)[0]}, 30, 15);
            TimedFrame midpoint = h.tick(0);
            assertEquals(2, midpoint.leds().length);
            assertEquals(30, midpoint.leds()[0].r(), 0.001);
            assertEquals(40, midpoint.leds()[1].r(), 0.001);
            h.generator.stop();
            h.sleeps.clear();
            h.generator.frameGeneration(colors(200), 30, 15);
            assertEquals(100, take(h.frames).leds()[0].r(), 0.001);
        }
    }

    @Test
    void outputRateChangesWithoutRestartingWorker() throws Exception {
        try (Harness h = new Harness()) {
            h.generator.frameGeneration(colors(120), 60, 30);
            take(h.frames);
            h.generator.frameGeneration(colors(240), 120, 60);
            TimedFrame changed = h.tick(0);
            TimedFrame next = h.tick(0);
            assertEquals(1_000_000_000L / 120, next.nanos() - changed.nanos());
            assertEquals(240, next.leds()[0].r(), 0.001);
        }
    }

    @Test
    void emaKeepsConvergingWithoutAnotherCapture() throws Exception {
        ColorEma ema = new ColorEma();
        ema.apply(colors(0), .2f);
        BlockingQueue<Float> filtered = new LinkedBlockingQueue<>();
        try (Harness h = new Harness(leds -> {
            ema.apply(leds, .2f);
            filtered.add(leds[0].r());
        })) {
            h.generator.frameGeneration(colors(120), 60, 30);
            take(h.frames);
            float last = take(filtered);
            for (int i = 0; i < 20; i++) {
                h.tick(0);
                float actual = take(filtered);
                assertTrue(actual > last);
                last = actual;
            }
            assertTrue(last > 118);
        }
    }

    @ParameterizedTest
    @CsvSource({"30,0", "60,0", "120,0", "30,100", "60,250"})
    void delayedOutputDoesNotIncreaseEmaWeight(int outputFps, long delayMs) throws Exception {
        ColorEma ema = new ColorEma();
        ema.apply(colors(0), .2f);
        BlockingQueue<Float> filtered = new LinkedBlockingQueue<>();
        try (Harness h = new Harness(leds -> {
            ema.apply(leds, .2f);
            filtered.add(leds[0].r());
        })) {
            // 1:1 isolates EMA from the interpolation curve.
            h.generator.frameGeneration(colors(200), outputFps, outputFps);
            take(h.frames);
            assertEquals(40, take(filtered), .001);
            h.tick(TimeUnit.MILLISECONDS.toNanos(delayMs));
            assertEquals(72, take(filtered), .001);
        }
    }

    @Test
    void stopInterruptsPacingAndDiscardsPendingOutput() throws Exception {
        try (Harness h = new Harness()) {
            h.generator.frameGeneration(colors(120), 2, 1);
            take(h.frames);
            take(h.sleeps);
            h.generator.frameGeneration(colors(240), 2, 1);
            assertTimeoutPreemptively(Duration.ofSeconds(1), h.generator::stop);
            assertTrue(h.frames.isEmpty());
            h.generator.frameGeneration(colors(60), 60, 60);
            assertEquals(60, take(h.frames).leds()[0].r(), 0.001);
        }
    }

    @Test
    void invalidRatesDoNotStartWorker() {
        FrameGenerator generator = new FrameGenerator(1, frame -> fail("Unexpected output"));
        assertThrows(IllegalArgumentException.class, () -> generator.frameGeneration(colors(1), 0, 30));
        assertThrows(IllegalArgumentException.class, () -> generator.frameGeneration(colors(1), 60, 0));
        generator.stop();
    }

    @Test
    void stopInterruptsBlockedSocketOutput() throws Exception {
        CountDownLatch reading = new CountDownLatch(1);
        try (ServerSocket server = new ServerSocket(0)) {
            FrameGenerator generator = new FrameGenerator(1, frame -> {
                try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                    reading.countDown();
                    client.getInputStream().read();
                } catch (java.io.IOException ignored) {
                    // Interrupting a virtual thread closes its blocking socket I/O.
                }
            });
            try {
                generator.frameGeneration(colors(120), 60, 30);
                assertTrue(reading.await(2, TimeUnit.SECONDS));
                try (Socket peer = server.accept()) {
                    assertTimeoutPreemptively(Duration.ofSeconds(1), generator::stop);
                    peer.setSoTimeout(1000);
                    assertEquals(-1, peer.getInputStream().read());
                }
            } finally {
                generator.stop();
            }
        }
    }

    private record TimedFrame(long nanos, ColorFloat[] leds) {
    }

    /**
     * Each sleep waits for the test to advance the monotonic clock explicitly.
     */
    private static final class Harness implements AutoCloseable {
        final AtomicLong clock = new AtomicLong();
        final BlockingQueue<TimedFrame> frames = new LinkedBlockingQueue<>();
        final BlockingQueue<Long> sleeps = new LinkedBlockingQueue<>();
        final Semaphore advance = new Semaphore(0);
        final FrameGenerator generator;

        Harness() {
            this(_ -> {
            });
        }

        Harness(Consumer<ColorFloat[]> output) {
            generator = new FrameGenerator(1, leds -> {
                frames.add(new TimedFrame(clock.get(), leds.clone()));
                output.accept(leds);
            }, clock::get, nanos -> {
                sleeps.add(nanos);
                advance.acquire();
            });
        }

        TimedFrame tick(long oversleep) throws InterruptedException {
            long sleep = take(sleeps);
            assertTrue(sleep > 0);
            clock.addAndGet(sleep + oversleep);
            advance.release();
            return take(frames);
        }

        @Override
        public void close() {
            generator.stop();
        }
    }
}
