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
import org.dpsoftware.utilities.CaptureDeviceUtilities;
import org.freedesktop.gstreamer.Pipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutomaticCaptureRecoveryTest {
    /**
     * Verify that a watchdog restart stops frame generation and reapplies the capture caps
     * before replaying the pipeline with the same grabber.
     *
     * @throws Exception if the watchdog restart method cannot be invoked via reflection
     */
    @Test
    void watchdogStopsFrameGenerationBeforeReplayingTheExistingPipeline() throws Exception {
        MainSingleton main = new MainSingleton();
        main.headlessMode = true;
        GrabberSingleton state = new GrabberSingleton();
        Pipeline pipeline = mock(Pipeline.class);
        state.pipe = pipeline;
        GStreamerGrabber grabber = mock(GStreamerGrabber.class);
        GrabberManager manager = new GrabberManager();
        manager.vc = grabber;
        AtomicInteger restartCounter = new AtomicInteger();

        try (var grabbers = mockStatic(GrabberSingleton.class)) {
            grabbers.when(GrabberSingleton::getInstance).thenReturn(state);
            Method restart = GrabberManager.class.getDeclaredMethod("restartCapturePipeline", MainSingleton.class,
                    AtomicInteger.class, String.class);
            restart.setAccessible(true);
            restart.invoke(manager, main, restartCounter, "tooManyRetries");
        }

        var lifecycle = inOrder(pipeline, grabber);
        lifecycle.verify(pipeline).stop();
        lifecycle.verify(grabber).stopFrameGeneration();
        lifecycle.verify(grabber).configureCaptureCaps();
        lifecycle.verify(pipeline).play();
        assertSame(grabber, manager.vc);
        assertEquals(1, restartCounter.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void missingSourceIsPublishedAndClearedWhenCaptureResumes(boolean headless) throws Exception {
        MainSingleton main = new MainSingleton();
        main.headlessMode = headless;
        main.RUNNING = true;
        main.config = new Configuration();
        main.config.setCaptureMethod(Configuration.CaptureMethod.WIN_USB_VIDEO.name());
        GrabberSingleton state = new GrabberSingleton();
        try (var singleton = mockStatic(MainSingleton.class);
             var managers = mockStatic(ManagerSingleton.class);
             var grabbers = mockStatic(GrabberSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            managers.when(ManagerSingleton::getInstance).thenReturn(new ManagerSingleton());
            grabbers.when(GrabberSingleton::getInstance).thenReturn(state);
            GrabberManager grabber = new GrabberManager();
            Method check = GrabberManager.class.getDeclaredMethod("checkCapturePipeline", MainSingleton.class,
                    AtomicInteger.class, AtomicInteger.class, AtomicReference.class, AtomicInteger.class);
            check.setAccessible(true);
            var missing = new AtomicReference<String>();
            var ticks = new AtomicInteger();
            for (int i = 0; i < 4; i++) {
                check.invoke(grabber, main, new AtomicInteger(), new AtomicInteger(), missing, ticks);
                assertNotNull(state.unavailableCaptureSource);
                assertEquals(missing.get(), state.unavailableCaptureSource);
            }
            main.FPS_PRODUCER_COUNTER = 1;
            check.invoke(grabber, main, new AtomicInteger(), new AtomicInteger(), missing, ticks);
            assertNull(state.unavailableCaptureSource);
            assertNull(missing.get());
        }
    }

    @Test
    void reconnectingUsbSourceClearsPublishedWarning() throws Exception {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.bundle = ResourceBundle.getBundle("messagebundle", Locale.ENGLISH);
        main.config.setCaptureMethod(Configuration.CaptureMethod.WIN_USB_VIDEO.name());
        var device = new CaptureDeviceUtilities.CaptureDevice("/dev/video0", "USB capture", 0);
        main.config.setCaptureDevice(new CaptureDeviceUtilities.BestCaptureFormat(device,
                new CaptureDeviceUtilities.PixelFormat("MJPG"), new CaptureDeviceUtilities.Resolution(1920, 1080)));
        GrabberSingleton state = new GrabberSingleton();
        try (var singleton = mockStatic(MainSingleton.class);
             var grabbers = mockStatic(GrabberSingleton.class);
             var devices = mockStatic(CaptureDeviceUtilities.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            grabbers.when(GrabberSingleton::getInstance).thenReturn(state);
            devices.when(CaptureDeviceUtilities::discover).thenReturn(List.of(), List.of(device));
            GrabberManager grabber = new GrabberManager();
            Method check = GrabberManager.class.getDeclaredMethod("getUnavailableCaptureSource");
            check.setAccessible(true);
            assertNotNull(check.invoke(grabber));
            assertTrue(state.unavailableCaptureSource.startsWith("USB video device: "));
            assertNull(check.invoke(grabber));
            assertNull(state.unavailableCaptureSource);
        }
    }

    @Test
    void missingUsbSourceDoesNotConsumeCandidates() throws Exception {
        MainSingleton main = new MainSingleton();
        main.RUNNING = true;
        main.config = new Configuration();
        main.config.setAutomaticCapturePending(true);
        ManagerSingleton manager = new ManagerSingleton();
        try (MockedStatic<MainSingleton> singleton = mockStatic(MainSingleton.class);
             MockedStatic<ManagerSingleton> managers = mockStatic(ManagerSingleton.class);
             MockedStatic<GrabberSingleton> grabbers = mockStatic(GrabberSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            managers.when(ManagerSingleton::getInstance).thenReturn(manager);
            GrabberSingleton state = new GrabberSingleton();
            grabbers.when(GrabberSingleton::getInstance).thenReturn(state);
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
            assertNotNull(state.unavailableCaptureSource);
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
