/*
  FirmwareStateSyncTest.java

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

import com.sun.net.httpserver.HttpServer;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirmwareStateSyncTest {
    private static final String PREFS = """
            {"cp":"10,20,30","toggle":"1","effect":"Solid","brightness":"170","whiteTemp":"45"}
            """;
    private Configuration previousConfig;
    private GuiManager previousGui;
    private boolean previousRunning, previousStarting, previousNight;
    private Configuration config;
    private GlowWormDevice device;
    private MockedStatic<CommonUtility> utility;

    @BeforeEach
    void setup() {
        var main = MainSingleton.getInstance();
        previousConfig = main.config;
        previousGui = main.guiManager;
        previousRunning = main.RUNNING;
        previousNight = main.nightMode;
        previousStarting = ManagerSingleton.getInstance().pipelineStarting;
        main.RUNNING = main.nightMode = false;
        ManagerSingleton.getInstance().pipelineStarting = false;
        main.guiManager = mock(GuiManager.class);
        config = new Configuration();
        config.setColorChooser("255,0,0,123");
        config.setEffect("Solid");
        config.setBrightness(200);
        config.setWhiteTemperature(65);
        main.config = config;
        device = mock(GlowWormDevice.class);
        when(device.getDeviceIP()).thenReturn("192.168.1.20");
        when(device.getMac()).thenReturn("AA:BB");
        utility = mockStatic(CommonUtility.class, CALLS_REAL_METHODS);
        utility.when(CommonUtility::getDeviceToUse).thenReturn(device);
        FirmwareStateSync.invalidate();
    }

    @AfterEach
    void restore() {
        FirmwareStateSync.invalidate();
        utility.close();
        var main = MainSingleton.getInstance();
        main.config = previousConfig;
        main.guiManager = previousGui;
        main.RUNNING = previousRunning;
        main.nightMode = previousNight;
        ManagerSingleton.getInstance().pipelineStarting = previousStarting;
    }

    private FirmwareStateSync.Pending pending() throws Exception {
        var revision = FirmwareStateSync.class.getDeclaredField("revision");
        revision.setAccessible(true);
        return new FirmwareStateSync.Pending(revision.getLong(null), config, device.getDeviceIP(),
                device.getMac(), FirmwareStateSync.snapshot(config));
    }

    @Test
    void adoptsCompleteFirmwareStateWithoutPublishingOldColor() throws Exception {
        try (var network = mockStatic(NetworkManager.class)) {
            assertTrue(FirmwareStateSync.apply(pending(), CommonUtility.JSON_MAPPER.readTree(PREFS)));
            assertEquals("10,20,30,123", config.getColorChooser());
            assertEquals(170, config.getBrightness());
            assertEquals(45, config.getWhiteTemperature());
            assertTrue(config.isToggleLed());
            network.verifyNoInteractions();
            utility.verify(CommonUtility::turnOnLEDs, never());
        }
    }

    @Test
    void offAndFirmwareEffectsAreAdoptedWithoutSendingACommand() throws Exception {
        assertTrue(FirmwareStateSync.apply(pending(), CommonUtility.JSON_MAPPER.readTree(
                PREFS.replace("\"1\"", "\"0\"").replace("Solid", "Fire"))));
        assertFalse(config.isToggleLed());
        assertEquals("Fire", config.getEffect());
        utility.verify(CommonUtility::turnOnLEDs, never());
    }

    @Test
    void discardsReadAfterNewCommandDeviceSwitchOrCaptureStart() throws Exception {
        var prefs = CommonUtility.JSON_MAPPER.readTree(PREFS);
        var old = pending();
        FirmwareStateSync.invalidate();
        assertFalse(FirmwareStateSync.apply(old, prefs));
        old = pending();
        config.setColorChooser("0,255,0,123");
        assertFalse(FirmwareStateSync.apply(old, prefs));
        old = pending();
        when(device.getMac()).thenReturn("CC:DD");
        assertFalse(FirmwareStateSync.apply(old, prefs));
        old = pending();
        MainSingleton.getInstance().RUNNING = true;
        assertFalse(FirmwareStateSync.apply(old, prefs));
        assertEquals("0,255,0,123", config.getColorChooser());
    }

    @Test
    void invalidOrStreamingResponseCannotPartiallyOverwriteConfig() throws Exception {
        String original = FirmwareStateSync.snapshot(config);
        assertThrows(IllegalArgumentException.class, () -> FirmwareStateSync.apply(pending(),
                CommonUtility.JSON_MAPPER.readTree(PREFS.replace("45", "999"))));
        assertEquals(original, FirmwareStateSync.snapshot(config));
        assertFalse(FirmwareStateSync.apply(pending(), CommonUtility.JSON_MAPPER.readTree(
                PREFS.replace("Solid", "GlowWormWifi"))));
        assertEquals(original, FirmwareStateSync.snapshot(config));
    }

    @Test
    void nightModeDoesNotApplyBrightnessReductionTwice() throws Exception {
        MainSingleton.getInstance().nightMode = true;
        FirmwareStateSync.apply(pending(), CommonUtility.JSON_MAPPER.readTree(PREFS));
        assertEquals(200, config.getBrightness());
        assertEquals("10,20,30,123", config.getColorChooser());
    }

    @Test
    void firmwareStopStopsCaptureWithoutPublishingAndSupersedesPendingWork() {
        MainSingleton.getInstance().RUNNING = true;
        synchronized (FirmwareStateSync.class) {
            FirmwareStateSync.request();
            // Cancel the queued read before releasing the worker; HTTP behavior is tested separately.
            FirmwareStateSync.invalidate();
        }
        verify(MainSingleton.getInstance().guiManager).stopCapturingThreads(false);
        utility.verify(CommonUtility::turnOnLEDs, never());
    }

    @Test
    void captureNotificationsKeepExistingEffectCommandPath() {
        try (var network = mockStatic(NetworkManager.class)) {
            FirmwareStateSync.effectChanged("Bias light");
            network.verify(() -> NetworkManager.setEffect("Bias light"));
        }
    }

    @Test
    void readsActualPrefsAndDoesNotSendSetRequestOnSuccessOrFailure() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var status = new java.util.concurrent.atomic.AtomicInteger(200);
        var paths = new java.util.concurrent.CopyOnWriteArrayList<String>();
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            byte[] bytes = PREFS.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length);
            try (var body = exchange.getResponseBody()) {
                body.write(bytes);
            }
        });
        server.start();
        try {
            when(device.getDeviceIP()).thenReturn("127.0.0.1:" + server.getAddress().getPort());
            FirmwareStateSync.read(pending(), false);
            assertEquals("10,20,30,123", config.getColorChooser());
            config.setColorChooser("0,255,0,123");
            status.set(503);
            FirmwareStateSync.read(pending(), false);
            assertEquals("0,255,0,123", config.getColorChooser());
            assertEquals(java.util.List.of("/prefs", "/prefs"), paths);
            utility.verify(CommonUtility::turnOnLEDs, never());
        } finally {
            server.stop(0);
        }
    }
}
