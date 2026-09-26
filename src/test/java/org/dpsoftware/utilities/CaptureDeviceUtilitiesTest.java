/*
  CaptureDeviceUtilitiesTest.java

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
package org.dpsoftware.utilities;

import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.device.Device;
import org.freedesktop.gstreamer.device.DeviceMonitor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CaptureDeviceUtilities}.
 * <p>
 * Tests cover the data model ({@link CaptureDeviceUtilities.Resolution}, {@link CaptureDeviceUtilities.PixelFormat},
 * {@link CaptureDeviceUtilities.CaptureDevice}) and the private framerates-from-string parser, which is
 * exercised on every Linux system where the GStreamer binding cannot deserialize
 * GstValueList&lt;Fraction&gt; values.
 */
class CaptureDeviceUtilitiesTest {

    @Test
    void print_raw_device_properties() {
        CaptureDeviceUtilities.initGStreamer();
        try (DeviceMonitor monitor = new DeviceMonitor()) {
            monitor.addFilter("Video/Source", null);
            if (!monitor.start()) return;
            for (Device dev : monitor.getDevices()) {
                System.out.println("=== " + dev.getDisplayName() + " ===");
                Structure props = dev.getProperties();
                if (props != null) {
                    System.out.println("  Properties toString: " + props);
                } else {
                    System.out.println("  Properties: null");
                }
            }
            monitor.stop();
        }
    }

    @Test
    void print_devices(){
        List<CaptureDeviceUtilities.CaptureDevice> devices = CaptureDeviceUtilities.discover();
        if (devices.isEmpty()) {
            return;
        }
        for (CaptureDeviceUtilities.CaptureDevice dev : devices) {
            System.out.printf("Device: %s (ID: %d, path: %s)%n", dev.getFriendlyName(), dev.getDeviceId(), dev.getDevPath());
            System.out.println("----------------------------------------");
            for (CaptureDeviceUtilities.PixelFormat fmt : dev.getFormats()) {
                System.out.printf("  Format: %s%n", fmt.getName());
                for (CaptureDeviceUtilities.Resolution res : fmt.getResolutions()) {
                    System.out.printf("    %s%n", res);
                }
            }
            System.out.println();
        }
    }

    // --- Resolution ---

    @Test
    void resolution_holdsDimensions() {
        CaptureDeviceUtilities.Resolution res = new CaptureDeviceUtilities.Resolution(1920, 1080);
        assertAll("1920x1080",
                () -> assertEquals(1920, res.getWidth()),
                () -> assertEquals(1080, res.getHeight()),
                () -> assertTrue(res.getFps().isEmpty())
        );
    }

    @Test
    void resolution_toString_withoutFps() {
        CaptureDeviceUtilities.Resolution res = new CaptureDeviceUtilities.Resolution(640, 480);
        assertEquals("640x480", res.toString());
    }

    @Test
    void resolution_toString_withFps() {
        CaptureDeviceUtilities.Resolution res = new CaptureDeviceUtilities.Resolution(1920, 1080);
        res.getFps().add(30);
        res.getFps().add(60);
        assertEquals("1920x1080 @ [30, 60] fps", res.toString());
    }

    @Test
    void resolution_fpsIsMutableList() {
        CaptureDeviceUtilities.Resolution res = new CaptureDeviceUtilities.Resolution(1280, 720);
        res.getFps().add(25);
        res.getFps().add(50);
        assertTrue(res.getFps().contains(25));
        assertTrue(res.getFps().contains(50));
    }

    // --- PixelFormat ---

    @Test
    void pixelFormat_holdsNameAndEmptyResolutions() {
        CaptureDeviceUtilities.PixelFormat fmt = new CaptureDeviceUtilities.PixelFormat("MJPG");
        assertAll("MJPG",
                () -> assertEquals("MJPG", fmt.getName()),
                () -> assertTrue(fmt.getResolutions().isEmpty())
        );
    }

    // --- CaptureDevice ---

    @Test
    void videoDevice_holdsIdentity() {
        CaptureDeviceUtilities.CaptureDevice dev = new CaptureDeviceUtilities.CaptureDevice("/dev/video0", "Test Cam", 7);
        assertAll("device",
                () -> assertEquals("/dev/video0", dev.getDevPath()),
                () -> assertEquals("Test Cam", dev.getFriendlyName()),
                () -> assertEquals(7, dev.getDeviceId()),
                () -> assertTrue(dev.getFormats().isEmpty())
        );
    }

    // --- extractFrameratesFromString (private, via reflection) ---

    @SuppressWarnings("unchecked")
    private static List<Integer> extractFps(String structStr) throws Exception {
        CaptureDeviceUtilities.Resolution res = new CaptureDeviceUtilities.Resolution(0, 0);
        Method m = CaptureDeviceUtilities.class.getDeclaredMethod("extractFrameratesFromString", String.class, CaptureDeviceUtilities.Resolution.class);
        m.setAccessible(true);
        m.invoke(null, structStr, res);
        return res.getFps();
    }

    @Test
    void extractFramerates_singleFraction() throws Exception {
        List<Integer> fps = extractFps("video/x-raw, format=UYVY, width=(int)1280, height=(int)720, framerate=(fraction)30/1");
        assertEquals(List.of(30), fps);
    }

    @Test
    void extractFramerates_singleFractionNonTrivialDenominator() throws Exception {
        List<Integer> fps = extractFps("framerate=(fraction)60/2");
        assertEquals(List.of(30), fps);
    }

    @Test
    void extractFramerates_rangeWithMinAndMax() throws Exception {
        List<Integer> fps = extractFps("framerate=(fraction){ 5/1, 30/1 }");
        assertAll("range 5..30",
                () -> assertTrue(fps.contains(5)),
                () -> assertTrue(fps.contains(30)),
                () -> assertEquals(2, fps.size())
        );
    }

    @Test
    void extractFramerates_multipleListEntries() throws Exception {
        List<Integer> fps = extractFps("framerate=(fraction){ 25/1, 30/1, 60/1 }");
        assertAll("list 25/30/60",
                () -> assertTrue(fps.contains(25)),
                () -> assertTrue(fps.contains(30)),
                () -> assertTrue(fps.contains(60)),
                () -> assertEquals(3, fps.size())
        );
    }

    @Test
    void extractFramerates_deduplicates() throws Exception {
        List<Integer> fps = extractFps("framerate=(fraction){ 30/1, 30/2 }");
        assertAll("dedupe",
                () -> assertTrue(fps.contains(15)),
                () -> assertTrue(fps.contains(30)),
                () -> assertEquals(2, fps.size())
        );
    }

    @Test
    void extractFramerates_roundsFraction() throws Exception {
        List<Integer> fps = extractFps("framerate=(fraction)31/1");
        assertEquals(List.of(31), fps);
    }

    @Test
    void extractFramerates_ignoresZeroNumerator() throws Exception {
        List<Integer> fps = extractFps("framerate=(fraction){ 0/1, 30/1 }");
        assertEquals(List.of(30), fps);
    }

    @Test
    void extractFramerates_ignoresZeroDenominator() throws Exception {
        List<Integer> fps = extractFps("framerate=(fraction){ 30/0, 30/1 }");
        assertEquals(List.of(30), fps);
    }

    @Test
    void extractFramerates_noFramerateField() throws Exception {
        List<Integer> fps = extractFps("video/x-raw, format=UYVY, width=(int)1280, height=(int)720");
        assertTrue(fps.isEmpty());
    }

    @Test
    void extractFramerates_stopsAtSemicolon() throws Exception {
        List<Integer> fps = extractFps("framerate=(fraction)30/1; width=(int)640, height=(int)480, framerate=(fraction)99/1");
        assertEquals(List.of(30), fps);
    }

    // --- closestResolution (package-private) ---

    @Test
    void closestResolution_exactMatch() {
        CaptureDeviceUtilities.Resolution best = CaptureDeviceUtilities.closestResolution(
                List.of(new CaptureDeviceUtilities.Resolution(640, 480), new CaptureDeviceUtilities.Resolution(1920, 1080)), 1920, 1080);
        assertAll("exact 1920x1080",
                () -> assertEquals(1920, best.getWidth()),
                () -> assertEquals(1080, best.getHeight())
        );
    }

    @Test
    void closestResolution_prefersCloserArea() {
        // target 1280x720 -> area 921600; 1920x1080 area delta 1105920; 640x480 delta 622080
        CaptureDeviceUtilities.Resolution best = CaptureDeviceUtilities.closestResolution(
                List.of(new CaptureDeviceUtilities.Resolution(1920, 1080), new CaptureDeviceUtilities.Resolution(640, 480)), 1280, 720);
        assertAll("closer area wins",
                () -> assertEquals(640, best.getWidth()),
                () -> assertEquals(480, best.getHeight())
        );
    }

    @Test
    void closestResolution_tieBrokenByDimensionDelta() throws Exception {
        Method m = CaptureDeviceUtilities.class.getDeclaredMethod("findOrCreateResolution",
                CaptureDeviceUtilities.PixelFormat.class, int.class, int.class);
        m.setAccessible(true);
        CaptureDeviceUtilities.PixelFormat fmt = new CaptureDeviceUtilities.PixelFormat("MJPG");
        // 1600x900 and 1280x1125 have identical area (1440000); 1280x1125 has smaller dim delta (405 vs 500)
        m.invoke(null, fmt, 1600, 900);
        m.invoke(null, fmt, 1280, 1125);
        CaptureDeviceUtilities.Resolution best = CaptureDeviceUtilities.closestResolution(fmt.getResolutions(), 1280, 720);
        assertAll("tie broken by dim delta",
                () -> assertEquals(1280, best.getWidth()),
                () -> assertEquals(1125, best.getHeight())
        );
    }

    @Test
    void closestResolution_emptyListReturnsNull() {
        assertNull(CaptureDeviceUtilities.closestResolution(List.of(), 1920, 1080));
    }

    // --- findDeviceByName (private, via reflection) ---

    @SuppressWarnings("unchecked")
    private static CaptureDeviceUtilities.CaptureDevice findDeviceByName(String friendlyName) throws Exception {
        Method m = CaptureDeviceUtilities.class.getDeclaredMethod("findDeviceByName", String.class);
        m.setAccessible(true);
        return (CaptureDeviceUtilities.CaptureDevice) m.invoke(null, friendlyName);
    }

    @Test
    void findDeviceByName_nullNameReturnsNull() throws Exception {
        assertNull(findDeviceByName(null));
    }

    // --- findFormatByName (private, via reflection) ---

    @SuppressWarnings("unchecked")
    private static CaptureDeviceUtilities.PixelFormat findFormatByName(CaptureDeviceUtilities.CaptureDevice dev, String name) throws Exception {
        Method m = CaptureDeviceUtilities.class.getDeclaredMethod("findFormatByName",
                CaptureDeviceUtilities.CaptureDevice.class, String.class);
        m.setAccessible(true);
        return (CaptureDeviceUtilities.PixelFormat) m.invoke(null, dev, name);
    }

    @Test
    void findFormatByName_caseInsensitiveMatch() throws Exception {
        CaptureDeviceUtilities.CaptureDevice dev = new CaptureDeviceUtilities.CaptureDevice("/dev/v0", "Cam", 0);
        dev.getFormats().add(new CaptureDeviceUtilities.PixelFormat("MJPG"));
        assertNotNull(findFormatByName(dev, "mjpg"));
    }

    @Test
    void findFormatByName_noMatchReturnsNull() throws Exception {
        CaptureDeviceUtilities.CaptureDevice dev = new CaptureDeviceUtilities.CaptureDevice("/dev/v0", "Cam", 0);
        dev.getFormats().add(new CaptureDeviceUtilities.PixelFormat("MJPG"));
        assertNull(findFormatByName(dev, "NV12"));
        assertNull(findFormatByName(dev, null));
    }

    // --- Resolution.getMaxFps ---

    @Test
    void getMaxFps_returnsHighestValue() {
        CaptureDeviceUtilities.Resolution res = new CaptureDeviceUtilities.Resolution(1920, 1080);
        res.getFps().add(30);
        res.getFps().add(60);
        res.getFps().add(15);
        assertEquals(60, res.getMaxFps());
    }

    @Test
    void getMaxFps_singleValue() {
        CaptureDeviceUtilities.Resolution res = new CaptureDeviceUtilities.Resolution(1280, 720);
        res.getFps().add(25);
        assertEquals(25, res.getMaxFps());
    }

    @Test
    void getMaxFps_emptyListReturnsZero() {
        CaptureDeviceUtilities.Resolution res = new CaptureDeviceUtilities.Resolution(640, 480);
        assertEquals(0, res.getMaxFps());
    }

}
