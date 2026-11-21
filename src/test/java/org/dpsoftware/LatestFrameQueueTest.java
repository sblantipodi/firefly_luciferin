/*
  LatestFrameQueueTest.java

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
package org.dpsoftware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class LatestFrameQueueTest {
    @Test
    void slowConsumerReceivesNewestFrameAndKeepaliveCannotReplaceIt() throws Exception {
        MainSingleton main = new MainSingleton();
        main.sharedQueue = new LinkedBlockingQueue<>(1);
        Color[] old = {Color.RED};
        Color[] newest = {Color.BLUE};
        main.offerLatestFrame(old);
        main.offerLatestFrame(new Color[]{Color.GREEN});
        main.offerLatestFrame(newest);
        assertFalse(main.offerKeepaliveFrame(old));
        assertSame(newest, main.sharedQueue.poll(1, TimeUnit.SECONDS));
        assertTrue(main.sharedQueue.isEmpty());
        assertTrue(main.offerKeepaliveFrame(old));
        main.offerLatestFrame(newest);
        assertSame(newest, main.sharedQueue.poll());
    }

    @Test
    void concurrentProducersStayBoundedAndFinalPublicationWins() throws Exception {
        MainSingleton main = new MainSingleton();
        main.sharedQueue = new LinkedBlockingQueue<>(1);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var producers = new ArrayList<Future<Void>>();
            for (int producer = 0; producer < 4; producer++) {
                producers.add(executor.submit(() -> {
                    start.await();
                    for (int frame = 0; frame < 1000; frame++) {
                        main.offerLatestFrame(new Color[]{Color.RED});
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<Void> producer : producers) producer.get();
        }
        Color[] newest = {Color.BLUE};
        main.offerLatestFrame(newest);
        assertEquals(1, main.sharedQueue.size());
        assertSame(newest, main.sharedQueue.take());
    }
}
