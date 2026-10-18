/*
  PreviewFrameDispatcherTest.java

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

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PreviewFrameDispatcherTest {
    private static final class Frame implements AutoCloseable {
        final AtomicInteger closed = new AtomicInteger();
        @Override public void close() { closed.incrementAndGet(); }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for worker");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    @Test
    void slowConsumerNeverBlocksCaptureOrCopiesAdditionalFrames() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicReference<Thread> consumerThread = new AtomicReference<>();
        Frame first = new Frame();
        try (var dispatcher = new PreviewFrameDispatcher<Frame>(10, frame -> {
            consumerThread.set(Thread.currentThread());
            entered.countDown();
            await(release);
        }, () -> 0, executor)) {
            Thread captureThread = Thread.currentThread();
            assertTrue(dispatcher.submit(() -> {
                assertSame(captureThread, Thread.currentThread());
                return first;
            }));
            await(entered);
            assertNotSame(captureThread, consumerThread.get());
            for (int i = 0; i < 100; i++) {
                assertFalse(dispatcher.submit(() -> { fail("Busy preview must not copy another frame"); return null; }));
            }
            assertEquals(0, first.closed.get());
        } finally {
            release.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(1, first.closed.get());
    }

    @Test
    void pacingSkipsBeforeCopyAndResumesAfterLongPause() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicLong clock = new AtomicLong(100);
        AtomicInteger processed = new AtomicInteger();
        try (var dispatcher = new PreviewFrameDispatcher<Frame>(10, _ -> processed.incrementAndGet(), clock::get, executor)) {
            assertTrue(dispatcher.submit(Frame::new));
            executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
            clock.set(109);
            assertFalse(dispatcher.submit(() -> { fail("Too early to copy"); return null; }));
            clock.set(110);
            assertTrue(dispatcher.submit(Frame::new));
            executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
            clock.set(1000);
            assertTrue(dispatcher.submit(Frame::new));
            executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
            assertFalse(dispatcher.submit(() -> { fail("No catch-up burst"); return null; }));
            assertEquals(3, processed.get());
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void stoppingBeforeWorkerStartsDiscardsAndClosesQueuedFrame() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch release = new CountDownLatch(1);
        executor.execute(() -> await(release));
        Frame frame = new Frame();
        try (var dispatcher = new PreviewFrameDispatcher<Frame>(10, _ -> fail("Closed session must not consume"), () -> 0, executor)) {
            assertTrue(dispatcher.submit(() -> frame));
            dispatcher.close();
            assertFalse(dispatcher.submit(() -> { fail("Closed session must not copy"); return null; }));
        } finally {
            release.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(1, frame.closed.get());
    }

    @Test
    void stoppingDuringCopyClosesTheCopyWithoutEnqueueingIt() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Frame frame = new Frame();
        try (var dispatcher = new PreviewFrameDispatcher<Frame>(10, _ -> fail("Closed session must not consume"), () -> 0, executor)) {
            assertFalse(dispatcher.submit(() -> {
                dispatcher.close();
                return frame;
            }));
            assertEquals(1, frame.closed.get());
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void failuresReleaseTheSlotAndCloseOwnedFrames() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicLong clock = new AtomicLong();
        Frame bad = new Frame(), good = new Frame();
        try (var dispatcher = new PreviewFrameDispatcher<Frame>(10, frame -> {
            if (frame == bad) throw new IllegalStateException("Simulated consumer failure");
        }, clock::get, executor)) {
            assertFalse(dispatcher.submit(() -> { throw new IllegalArgumentException("Simulated copy failure"); }));
            assertFalse(dispatcher.submit(() -> null));
            assertTrue(dispatcher.submit(() -> bad));
            executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
            assertEquals(1, bad.closed.get());
            clock.set(10);
            assertTrue(dispatcher.submit(() -> good));
            executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
            assertEquals(1, good.closed.get());
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void rejectedTaskClosesItsFrame() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.shutdown();
        Frame frame = new Frame();
        try (var dispatcher = new PreviewFrameDispatcher<Frame>(10, _ -> fail("Rejected task"), () -> 0, executor)) {
            assertFalse(dispatcher.submit(() -> frame));
            assertEquals(1, frame.closed.get());
        }
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
