/*
  PreviewFrameDispatcher.java

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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Admits preview frames before copying them, with at most one frame being copied, queued or processed.
 * The factory runs on the capture thread while its source buffer is valid; the consumer runs on a worker.
 */
@Slf4j
final class PreviewFrameDispatcher<T extends AutoCloseable> implements AutoCloseable {
    private final ExecutorService executor;
    private final LongSupplier clock;
    private final long intervalNanos;
    private final Consumer<T> consumer;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    // Accessed only by the producer that owns busy.
    private boolean paced;
    private long nextFrameNanos;

    PreviewFrameDispatcher(long intervalNanos, Consumer<T> consumer) {
        this(intervalNanos, consumer, System::nanoTime,
                Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("webrtc-preview").factory()));
    }

    PreviewFrameDispatcher(long intervalNanos, Consumer<T> consumer, LongSupplier clock, ExecutorService executor) {
        if (intervalNanos <= 0) throw new IllegalArgumentException("Frame interval must be positive");
        this.intervalNanos = intervalNanos;
        this.consumer = consumer;
        this.clock = clock;
        this.executor = executor;
    }

    boolean submit(Supplier<T> copyFrame) {
        if (closed.get() || !busy.compareAndSet(false, true)) return false;
        T frame = null;
        boolean submitted = false;
        try {
            long now = clock.getAsLong();
            if (closed.get() || (paced && now - nextFrameNanos < 0)) return false;
            frame = copyFrame.get();
            if (frame == null || closed.get()) return false;
            T ownedFrame = frame;
            // Advance past missed intervals without scheduling a backlog of catch-up frames.
            nextFrameNanos = paced
                    ? nextFrameNanos + ((now - nextFrameNanos) / intervalNanos + 1) * intervalNanos
                    : now + intervalNanos;
            paced = true;
            executor.execute(() -> {
                try (ownedFrame) {
                    if (!closed.get()) consumer.accept(ownedFrame);
                } catch (Exception e) {
                    log.warn("Unable to process WebRTC preview frame", e);
                } finally {
                    busy.set(false);
                }
            });
            submitted = true;
            return true;
        } catch (RejectedExecutionException ignored) {
            // Session stopped while its capture-thread copy was in progress.
            return false;
        } catch (RuntimeException e) {
            log.warn("Unable to copy WebRTC preview frame", e);
            return false;
        } finally {
            if (!submitted) {
                try {
                    if (frame != null) frame.close();
                } catch (Exception e) {
                    log.warn("Unable to release WebRTC preview frame", e);
                } finally {
                    busy.set(false);
                }
            }
        }
    }

    @Override
    public void close() {
        closed.set(true);
        // Queued tasks must run their cleanup; shutdownNow would abandon their native buffers.
        executor.shutdown();
    }
}
