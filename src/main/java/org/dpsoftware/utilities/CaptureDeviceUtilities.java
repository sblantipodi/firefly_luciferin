/*
  CaptureDeviceUtilities.java

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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.grabber.ImageProcessor;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.device.Device;
import org.freedesktop.gstreamer.device.DeviceMonitor;

import java.util.*;

/**
 * Used to discover the available video devices
 */
@Slf4j
public class CaptureDeviceUtilities {

    /**
     * Supported resolution with its available framerates.
     */
    @Setter
    @Getter
    public static class Resolution {
        private int width;
        private int height;
        private List<Integer> fps = new ArrayList<>();

        public Resolution() {
        }

        public Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getMaxFps() {
            return fps.stream().mapToInt(Integer::intValue).max().orElse(0);
        }

        @Override
        public String toString() {
            return width + "x" + height + (fps.isEmpty() ? "" : " @ " + fps + " fps");
        }
    }

    /**
     * A pixel format (e.g. MJPG, YUY2, NV12) and its supported resolutions.
     */
    @Setter
    @Getter
    public static class PixelFormat {
        private String name;
        private List<Resolution> resolutions = new ArrayList<>();

        public PixelFormat() {
        }

        public PixelFormat(String name) {
            this.name = name;
        }

    }

    /**
     * A video capture device with its name, id and supported formats.
     */
    @Setter
    @Getter
    public static class CaptureDevice {
        private String devPath;
        private String friendlyName;
        private int deviceId;
        private List<PixelFormat> formats = new ArrayList<>();

        public CaptureDevice() {
        }

        public CaptureDevice(String devPath, String friendlyName, int deviceId) {
            this.devPath = devPath;
            this.friendlyName = friendlyName;
            this.deviceId = deviceId;
        }

    }

    /**
     * A flattened capture selection containing the device identity, a single format name and a
     * single resolution, with no nested lists.
     */
    @Setter
    @Getter
    public static class BestCaptureFormat {
        private String devPath;
        private String friendlyName;
        private int deviceId;
        private Enums.VideoDeviceFormat bestFormat;
        private int suggestedWidth;
        private int suggestedHeight;
        private int maxFps;

        public BestCaptureFormat() {
        }

        public BestCaptureFormat(CaptureDevice dev, PixelFormat format, Resolution resolution) {
            this.devPath = dev.getDevPath();
            this.friendlyName = dev.getFriendlyName();
            this.deviceId = dev.getDeviceId();
            this.bestFormat = Enums.VideoDeviceFormat.valueOf(format.getName());
            this.suggestedWidth = resolution.getWidth();
            this.suggestedHeight = resolution.getHeight();
            this.maxFps = resolution.getMaxFps();
        }

    }

    /**
     * Initializes GStreamer environment variables and loads native libraries.
     */
    public static void initGStreamer() {
        if (!Gst.isInitialized()) {
            ImageProcessor.initGStreamerLibraryPaths();
            Gst.init(Version.of(1, 14), "CaptureDeviceUtilities");
        }
    }

    /**
     * Discovers all video capture devices and their supported formats/resolutions.
     */
    public static List<CaptureDevice> discover() {
        initGStreamer();
        List<CaptureDevice> devices = new ArrayList<>();
        try {
            devices = discoverViaDeviceMonitor();
            if (!devices.isEmpty()) {
                return devices;
            }
        } catch (Throwable t) {
            log.debug("DeviceMonitor discovery failed: {}", t.getMessage());
        }
        return devices;
    }

    /**
     * Lists capture devices via the GStreamer DeviceMonitor, skipping libcamera ACPI duplicates on Linux.
     */
    private static List<CaptureDevice> discoverViaDeviceMonitor() {
        List<CaptureDevice> list = new ArrayList<>();
        try (DeviceMonitor monitor = new DeviceMonitor()) {
            monitor.addFilter("Video/Source", null);
            if (monitor.start()) {
                List<Device> devList = monitor.getDevices();
                int id = 0;
                for (Device dev : devList) {
                    String name = dev.getDisplayName();
                    if (NativeExecutor.isLinux() && name != null && name.startsWith("\\_SB_")) {
                        continue;
                    }
                    String devPath = extractDevicePath(dev);
                    Caps caps = dev.getCaps();
                    CaptureDevice videoDev = new CaptureDevice(devPath, name, id++);
                    parseCapsIntoDevice(caps, videoDev);
                    if (!videoDev.getFormats().isEmpty()) {
                        list.add(videoDev);
                    }
                }
                monitor.stop();
            }
            return list;
        }
    }

    /**
     * Extracts the physical device path (e.g. /dev/video0) from the GStreamer device properties.
     * PipeWire's v4l2 provider stores it under {api.v4l2.path} as a fallback the {object.path}
     * field contains the same value prefixed with {"v4l2:"}.
     * Returns null if neither property is available (e.g. on Windows/macOS or libcamera devices).
     */
    private static String extractDevicePath(Device dev) {
        try {
            org.freedesktop.gstreamer.Structure props = dev.getProperties();
            if (props == null) return null;
            if (props.hasField("api.v4l2.path")) {
                return props.getString("api.v4l2.path");
            }
            if (props.hasField("object.path")) {
                String raw = props.getString("object.path");
                // object.path is in the form "v4l2:/dev/videoN"
                if (raw != null && raw.contains(":")) {
                    return raw.substring(raw.indexOf(':') + 1);
                }
                return raw;
            }
        } catch (Exception e) {
            log.debug("Could not extract device path from GStreamer device properties: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Maps GStreamer caps into formats/resolutions, skipping ranges and sorting by size.
     */
    private static void parseCapsIntoDevice(Caps caps, CaptureDevice dev) {
        if (caps == null) return;
        for (int i = 0; i < caps.size(); i++) {
            Structure s = caps.getStructure(i);
            String mediaType = s.getName();
            String formatName = switch (mediaType) {
                case "image/jpeg" -> "MJPG";
                case "video/x-raw" -> s.hasField("format") ? s.getString("format") : "RAW";
                case "video/x-h264" -> "H264";
                case null, default -> mediaType;
            };
            if (!s.hasField("width") || !s.hasField("height")) {
                continue;
            }
            int width, height;
            try {
                width = s.getInteger("width");
                height = s.getInteger("height");
            } catch (Exception e) {
                // Width/height might be a range if device wasn't queried in READY state
                continue;
            }
            PixelFormat fmt = findOrCreateFormat(dev, formatName);
            Resolution res = findOrCreateResolution(fmt, width, height);
            // Framerate
            extractFramerates(s, res);
        }
        // Sort resolutions descending by width*height
        for (PixelFormat fmt : dev.getFormats()) {
            fmt.getResolutions().sort((a, b) -> Integer.compare(b.getWidth() * b.getHeight(), a.getWidth() * a.getHeight()));
            for (Resolution r : fmt.getResolutions()) {
                Collections.sort(r.getFps());
            }
        }
    }

    /**
     * Reads the framerate field, using the string parser on Linux (binding limitation).
     */
    private static void extractFramerates(Structure s, Resolution res) {
        if (!s.hasField("framerate")) return;
        // On Linux the binding cannot deserialize GstValueList<Fraction> (crashes with g_value_dup_object).
        if (NativeExecutor.isLinux()) {
            extractFrameratesFromString(s.toString(), res);
            return;
        }
        // Windows: framerate is always a single Fraction
        try {
            Fraction frac = s.getFraction("framerate");
            if (frac != null && frac.getNumerator() > 0) {
                int fps = (int) Math.round((double) frac.getNumerator() / frac.getDenominator());
                if (fps > 0 && !res.getFps().contains(fps)) {
                    res.getFps().add(fps);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Extracts framerates from the string representation of a GStreamer Structure.
     */
    private static void extractFrameratesFromString(String structStr, Resolution res) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("framerate=([^;]+)")
                .matcher(structStr);
        if (!m.find()) return;
        String framerateVal = m.group(1);
        // Extract all numerator/denominator pairs (e.g. 30/1, 15/2)
        java.util.regex.Matcher fracMatcher = java.util.regex.Pattern
                .compile("(\\d+)/(\\d+)")
                .matcher(framerateVal);
        while (fracMatcher.find()) {
            int num = Integer.parseInt(fracMatcher.group(1));
            int den = Integer.parseInt(fracMatcher.group(2));
            if (den > 0 && num > 0) {
                int fps = (int) Math.round((double) num / den);
                if (fps > 0 && !res.getFps().contains(fps)) {
                    res.getFps().add(fps);
                }
            }
        }
    }

    /**
     * Returns the existing format with the given name or creates a new one.
     */
    private static PixelFormat findOrCreateFormat(CaptureDevice dev, String formatName) {
        for (PixelFormat f : dev.getFormats()) {
            if (f.getName().equalsIgnoreCase(formatName)) return f;
        }
        PixelFormat f = new PixelFormat(formatName);
        dev.getFormats().add(f);
        return f;
    }

    /**
     * Returns the existing resolution for the given dimensions or creates a new one.
     */
    private static Resolution findOrCreateResolution(PixelFormat fmt, int width, int height) {
        for (Resolution r : fmt.getResolutions()) {
            if (r.getWidth() == width && r.getHeight() == height) return r;
        }
        Resolution r = new Resolution(width, height);
        fmt.getResolutions().add(r);
        return r;
    }

    /**
     * Returns a flattened selection containing the device identity, the first pixel format
     * (in VideoDeviceFormat order) that has resolutions, and the resolution closest to the
     * configured screen resolution (screenResX/screenResY). If the friendly name is null or
     * empty, the first discovered device is used. Returns null if no device is found or no
     * supported format has resolutions.
     */
    public static BestCaptureFormat findPixelFormat(String friendlyName) {
        int width = Constants.DEFAULT_RES_WIDTH / Constants.RESAMPLING_FACTOR;
        int height = Constants.DEFAULT_RES_HEIGHT / Constants.RESAMPLING_FACTOR;
        if (MainSingleton.getInstance().config != null) {
            Configuration main = MainSingleton.getInstance().config;
            width = main.getScreenResX() / main.getResamplingFactor();
            height = main.getScreenResY() / main.getResamplingFactor();
        }
        CaptureDevice dev = (friendlyName == null || friendlyName.isBlank()) ? firstDevice() : findDeviceByName(friendlyName);
        if (dev == null) {
            return null;
        }
        for (Enums.VideoDeviceFormat format : Enums.VideoDeviceFormat.values()) {
            PixelFormat f = findFormatByName(dev, format.name());
            if (f != null && !f.getResolutions().isEmpty()) {
                Resolution closest = closestResolution(f.getResolutions(), width, height);
                if (closest != null) {
                    int maxFps = closest.getMaxFps();
                    closest.getFps().clear();
                    if (maxFps > 0) {
                        closest.getFps().add(maxFps);
                    }
                    return new BestCaptureFormat(dev, f, closest);
                }
            }
        }
        return null;
    }

    /**
     * Returns the resolution closest to the given dimensions by total area, tie broken by the sum
     * of the absolute dimension differences, then by the absolute width difference.
     */
    static Resolution closestResolution(List<Resolution> resolutions, int width, int height) {
        Resolution best = null;
        long bestArea = Long.MAX_VALUE;
        int bestAreaDelta = Integer.MAX_VALUE;
        int bestWidthDelta = Integer.MAX_VALUE;
        for (Resolution r : resolutions) {
            long area = (long) r.getWidth() * r.getHeight();
            long target = (long) width * height;
            long areaDelta = Math.abs(area - target);
            int dimDelta = Math.abs(r.getWidth() - width) + Math.abs(r.getHeight() - height);
            int wDelta = Math.abs(r.getWidth() - width);
            if (areaDelta < bestArea || (areaDelta == bestArea && dimDelta < bestAreaDelta)
                    || (areaDelta == bestArea && dimDelta == bestAreaDelta && wDelta < bestWidthDelta)) {
                best = r;
                bestArea = areaDelta;
                bestAreaDelta = dimDelta;
                bestWidthDelta = wDelta;
            }
        }
        return best;
    }

    /**
     * Returns the first discovered device, or null if none is available.
     */
    private static CaptureDevice firstDevice() {
        for (CaptureDevice dev : discover()) {
            return dev;
        }
        return null;
    }

    private static CaptureDevice findDeviceByName(String friendlyName) {
        if (friendlyName == null) {
            return null;
        }
        for (CaptureDevice dev : discover()) {
            if (friendlyName.equalsIgnoreCase(dev.getFriendlyName())) {
                return dev;
            }
        }
        return null;
    }

    private static PixelFormat findFormatByName(CaptureDevice dev, String name) {
        if (name == null) {
            return null;
        }
        for (PixelFormat f : dev.getFormats()) {
            if (name.equalsIgnoreCase(f.getName())) {
                return f;
            }
        }
        return null;
    }

}
