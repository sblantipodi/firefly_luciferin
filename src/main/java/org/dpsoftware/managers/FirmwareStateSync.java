/*
  FirmwareStateSync.java

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
package org.dpsoftware.managers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.utilities.CommonUtility;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Adopts firmware-owned state without echoing the desktop's previous color back to the device.
 */
@Slf4j
public final class FirmwareStateSync {
    private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "firmware-state-sync");
        thread.setDaemon(true);
        return thread;
    });
    private static long revision;

    private FirmwareStateSync() {
    }

    /**
     * Supersedes pending reads when a new desktop command or firmware notification arrives.
     */
    public static synchronized void invalidate() {
        revision++;
    }

    static boolean isCaptureEffect(String effect) {
        Enums.Effect value = LocalizedEnum.fromBaseStr(Enums.Effect.class, effect);
        return value == Enums.Effect.BIAS_LIGHT || value == Enums.Effect.MUSIC_MODE_VU_METER
                || value == Enums.Effect.MUSIC_MODE_VU_METER_DUAL || value == Enums.Effect.MUSIC_MODE_BRIGHT
                || value == Enums.Effect.MUSIC_MODE_RAINBOW;
    }

    /**
     * Capture commands keep their existing path; device effects are already applied by Glow Worm.
     */
    public static void effectChanged(String effect) {
        invalidate();
        if (isCaptureEffect(effect)) {
            NetworkManager.setEffect(effect);
        } else if (Constants.OFF.equals(effect) || LocalizedEnum.fromBaseStr(Enums.Effect.class, effect) != null) {
            request();
        }
    }

    /**
     * Stops streaming without publishing a stale Solid command, then reads the selected device.
     */
    public static synchronized void request() {
        invalidate();
        var main = MainSingleton.getInstance();
        var device = CommonUtility.getDeviceToUse();
        if (main.config == null || device == null || !NetworkManager.isValidIp(device.getDeviceIP())) return;
        if (main.RUNNING || ManagerSingleton.getInstance().pipelineStarting) {
            main.guiManager.stopCapturingThreads(false);
        }
        Pending pending = new Pending(revision, main.config, device.getDeviceIP(), device.getMac(), snapshot(main.config));
        WORKER.execute(() -> read(pending, true));
    }

    static String snapshot(Configuration config) {
        return config.getEffect() + ":" + config.getColorChooser() + ":" + config.isToggleLed()
                + ":" + config.getBrightness() + ":" + config.getWhiteTemperature();
    }

    static synchronized boolean current(Pending pending) {
        var main = MainSingleton.getInstance();
        var device = CommonUtility.getDeviceToUse();
        return revision == pending.revision && main.config == pending.config && !main.RUNNING
                && !ManagerSingleton.getInstance().pipelineStarting && device != null
                && Objects.equals(device.getDeviceIP(), pending.ip) && Objects.equals(device.getMac(), pending.mac)
                && snapshot(main.config).equals(pending.snapshot);
    }

    static void read(Pending pending, boolean retry) {
        if (!current(pending)) return;
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://" + pending.ip + "/prefs"))
                    .timeout(Duration.ofSeconds(2)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());
            apply(pending, CommonUtility.JSON_MAPPER.readTree(response.body()));
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.debug("Unable to synchronize firmware state from {}: {}", pending.ip, e.getMessage());
            if (retry && current(pending)) WORKER.schedule(() -> read(pending, false), 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Validates the complete response before changing runtime state. Streaming status is never a picker color.
     */
    static synchronized boolean apply(Pending pending, JsonNode prefs) {
        if (!current(pending)) return false;
        String effect = prefs.path("effect").asText();
        if (isCaptureEffect(effect) || LocalizedEnum.fromBaseStr(Enums.Effect.class, effect) == null) return false;
        String toggle = prefs.path("toggle").asText();
        if (!toggle.equals("0") && !toggle.equals("1")) throw new IllegalArgumentException("Invalid LED state");
        String[] rgb = prefs.path("cp").asText().split(",", -1);
        if (rgb.length != 3) throw new IllegalArgumentException("Invalid color");
        int r = bounded(rgb[0], 0, 255), g = bounded(rgb[1], 0, 255), b = bounded(rgb[2], 0, 255);
        int brightness = bounded(prefs.path("brightness").asText(), 0, 255);
        int temperature = bounded(prefs.path("whiteTemp").asText(), 20, 110);
        Configuration config = pending.config;
        String[] previous = config.getColorChooser().split(",");
        config.setColorChooser(r + "," + g + "," + b + "," + (previous.length > 3 ? previous[3] : "255"));
        // Night mode reports an attenuated device brightness; do not attenuate it again on the next command.
        if (!MainSingleton.getInstance().nightMode) config.setBrightness(brightness);
        config.setWhiteTemperature(temperature);
        config.setToggleLed(toggle.equals("1"));
        config.setEffect(effect);
        if (MainSingleton.getInstance().guiManager != null && MainSingleton.getInstance().guiManager.trayIconManager != null) {
            MainSingleton.getInstance().guiManager.trayIconManager.updateTray();
        }
        return true;
    }

    private static int bounded(String value, int min, int max) {
        int result = Integer.parseInt(value);
        if (result < min || result > max) throw new IllegalArgumentException("Invalid firmware preference");
        return result;
    }

    record Pending(long revision, Configuration config, String ip, String mac, String snapshot) {
    }
}
