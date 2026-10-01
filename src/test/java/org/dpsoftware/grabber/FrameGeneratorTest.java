package org.dpsoftware.grabber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class FrameGeneratorTest {
    private static ColorFloat[] colors(float red) {
        return new ColorFloat[]{new ColorFloat(red, red / 2, red / 4)};
    }

    private static ColorFloat[] take(BlockingQueue<ColorFloat[]> frames) throws InterruptedException {
        ColorFloat[] frame = frames.poll(2, TimeUnit.SECONDS);
        assertNotNull(frame, "Missing generated frame");
        return frame;
    }

    @ParameterizedTest
    @CsvSource({"30,15,30", "60,30,13", "120,60,5", "60,15,13", "60,5,13", "30,2,30", "30,7,32"})
    void preservesOriginalPacingHeadroom(int outputFps, int captureFps, long periodMs) throws Exception {
        AtomicLong clock = new AtomicLong();
        BlockingQueue<TimedFrame> frames = new LinkedBlockingQueue<>();
        FrameGenerator generator = new FrameGenerator(1,
                leds -> frames.add(new TimedFrame(clock.get(), leds)), clock::get, clock::addAndGet);
        try {
            generator.frameGeneration(colors(120), outputFps, captureFps);
            int count = outputFps / captureFps;
            for (int i = 0; i < count; i++) {
                TimedFrame actual = frames.poll(2, TimeUnit.SECONDS);
                assertNotNull(actual);
                assertEquals(TimeUnit.MILLISECONDS.toNanos(i * periodMs), actual.nanos());
                assertEquals(120f * i / (count - 1), actual.leds()[0].r(), 0.001f);
            }
        } finally {
            generator.stop();
        }
    }

    @Test
    void twoXTargetColorRetainsLegacyLatencyWithSchedulingOverhead() throws Exception {
        AtomicLong clock = new AtomicLong();
        BlockingQueue<TimedFrame> frames = new LinkedBlockingQueue<>();
        FrameGenerator generator = new FrameGenerator(1, leds -> {
            frames.add(new TimedFrame(clock.get(), leds));
            // Model three milliseconds of output work per generated frame.
            clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(3));
        }, clock::get, nanos -> clock.addAndGet(nanos + TimeUnit.MILLISECONDS.toNanos(1)));
        try {
            generator.frameGeneration(colors(120), 60, 30);
            TimedFrame previous = frames.poll(2, TimeUnit.SECONDS);
            TimedFrame target = frames.poll(2, TimeUnit.SECONDS);
            assertNotNull(previous);
            assertNotNull(target);
            assertEquals(0, previous.nanos(), "First output must not wait for an extra tick");
            assertEquals(TimeUnit.MILLISECONDS.toNanos(14), target.nanos());
            assertEquals(120, target.leds()[0].r());
        } finally {
            generator.stop();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "30,15", "30,10", "30,7", "30,5", "30,2", "30,1",
            "60,30", "60,20", "60,15", "60,10", "60,5", "60,2",
            "120,60", "120,40", "120,30", "120,20", "120,10", "120,4",
            "60,60"
    })
    void preservesInterpolationAndRawEndpoints(int outputFps, int captureFps) throws Exception {
        BlockingQueue<ColorFloat[]> frames = new LinkedBlockingQueue<>();
        FrameGenerator generator = new FrameGenerator(1, frame -> {
            frames.add(frame.clone());
            // The production output mutates arrays when applying EMA and white balance.
            frame[0] = ColorFloat.BLACK;
        });
        try {
            generator.frameGeneration(colors(120), outputFps, captureFps);
            int count = Math.max(1, outputFps / captureFps);
            for (int i = 0; i < count; i++) {
                float fraction = count == 1 ? 1f : (float) i / (count - 1);
                assertEquals(120 * fraction, take(frames)[0].r(), 0.001f);
            }
            generator.frameGeneration(colors(240), outputFps, captureFps);
            for (int i = 0; i < count; i++) {
                float fraction = count == 1 ? 1f : (float) i / (count - 1);
                ColorFloat actual = take(frames)[0];
                assertEquals(120 + 120 * fraction, actual.r(), 0.001f);
                assertEquals(actual.r() / 2, actual.g(), 0.001f);
                assertEquals(actual.r() / 4, actual.b(), 0.001f);
            }
        } finally {
            generator.stop();
        }
    }

    @Test
    void slowOutputDoesNotBlockCaptureAndOnlyLatestPendingFrameSurvives() throws Exception {
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
            ColorFloat[] latest = colors(30);
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                generator.frameGeneration(colors(20), 60, 30);
                generator.frameGeneration(latest, 60, 30);
            });
            latest[0] = ColorFloat.BLACK;
            release.countDown();
            assertEquals(0, take(frames)[0].r());
            assertEquals(10, take(frames)[0].r());
            assertEquals(10, take(frames)[0].r());
            assertEquals(30, take(frames)[0].r());
            assertNull(frames.poll(100, TimeUnit.MILLISECONDS));
        } finally {
            release.countDown();
            generator.stop();
        }
    }

    @Test
    void matrixSizeCanChangeAndRestartResetsInterpolation() throws Exception {
        BlockingQueue<ColorFloat[]> frames = new LinkedBlockingQueue<>();
        FrameGenerator generator = new FrameGenerator(1, frames::add);
        try {
            generator.frameGeneration(colors(120), 30, 15);
            take(frames);
            take(frames);
            generator.frameGeneration(new ColorFloat[]{colors(60)[0], colors(80)[0]}, 30, 15);
            assertArrayEquals(new ColorFloat[]{ColorFloat.BLACK, ColorFloat.BLACK}, take(frames));
            assertArrayEquals(new ColorFloat[]{colors(60)[0], colors(80)[0]}, take(frames));
            generator.stop();
            generator.frameGeneration(colors(200), 30, 15);
            assertArrayEquals(new ColorFloat[]{ColorFloat.BLACK}, take(frames));
            assertEquals(200, take(frames)[0].r());
        } finally {
            generator.stop();
        }
    }

    @Test
    void stopInterruptsPacingAndDiscardsPendingOutput() throws Exception {
        BlockingQueue<ColorFloat[]> frames = new LinkedBlockingQueue<>();
        FrameGenerator generator = new FrameGenerator(1, frames::add);
        try {
            // Deliberately long interval: stopping must not wait for the next output tick.
            generator.frameGeneration(colors(120), 2, 1);
            take(frames);
            generator.frameGeneration(colors(240), 2, 1);
            assertTimeoutPreemptively(Duration.ofSeconds(1), generator::stop);
            assertNull(frames.poll(600, TimeUnit.MILLISECONDS));
            generator.frameGeneration(colors(60), 60, 60);
            assertEquals(60, take(frames)[0].r());
        } finally {
            generator.stop();
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
}
