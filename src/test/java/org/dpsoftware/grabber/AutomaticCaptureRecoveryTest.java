/*
  AutomaticCaptureRecoveryTest.java

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

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.PipelineManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class AutomaticCaptureRecoveryTest {
    @Test
    void missingUsbSourceDoesNotConsumeCandidates() throws Exception {
        MainSingleton main = new MainSingleton();
        main.RUNNING = true;
        main.config = new Configuration();
        main.config.setAutomaticCapturePending(true);
        ManagerSingleton manager = new ManagerSingleton();
        try (MockedStatic<MainSingleton> singleton = mockStatic(MainSingleton.class);
             MockedStatic<ManagerSingleton> managers = mockStatic(ManagerSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            managers.when(ManagerSingleton::getInstance).thenReturn(manager);
            GrabberManager grabber = new GrabberManager();
            CaptureMethodSelection selection = new CaptureMethodSelection(false, false, false, true);
            Field field = GrabberManager.class.getDeclaredField("captureSelection");
            field.setAccessible(true);
            field.set(grabber, selection);
            Method probe = GrabberManager.class.getDeclaredMethod("probeCaptureMethod", AtomicInteger.class, MainSingleton.class);
            probe.setAccessible(true);
            for (int i = 0; i < 8; i++) {
                probe.invoke(grabber, new AtomicInteger(), main);
            }
            assertSame(selection, field.get(grabber));
            assertEquals(Configuration.CaptureMethod.USB_VIDEO_NVIDIA, selection.current());
            assertTrue(main.config.isAutomaticCapturePending());
        }
    }

    @Test
    void startupExceptionAllowsLaterWatchdogTicks() throws Exception {
        GrabberSingleton state = new GrabberSingleton();
        try (MockedStatic<GrabberSingleton> singleton = mockStatic(GrabberSingleton.class)) {
            singleton.when(GrabberSingleton::getInstance).thenReturn(state);
            GrabberManager grabber = new GrabberManager();
            assertDoesNotThrow(() -> grabber.runCaptureTask(() -> {
                throw new PipelineManager.MissingCaptureElementException("cudascalea",
                        new IllegalArgumentException("No such GStreamer factory"));
            }));
            AtomicInteger attempts = new AtomicInteger();
            grabber.runCaptureTask(attempts::incrementAndGet);
            assertEquals(0, attempts.get());
            // Advance past the backoff without making the test sleep.
            Field retry = GrabberManager.class.getDeclaredField("captureRetryAfter");
            retry.setAccessible(true);
            retry.setLong(grabber, System.nanoTime() - 1);
            grabber.runCaptureTask(attempts::incrementAndGet);
            assertEquals(1, attempts.get());
        }
    }
}
