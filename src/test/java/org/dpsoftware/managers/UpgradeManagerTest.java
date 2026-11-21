/*
  UpgradeManagerTest.java

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

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for version comparison and update state handling.
 */
class UpgradeManagerTest {

    /**
     * Disabled update checks release the completion callback without contacting Firefly.
     */
    @Test
    void headlessCheckWithUpdatesDisabledCompletes() {
        MainSingleton main = MainSingleton.getInstance();
        Configuration previousConfig = main.config;
        boolean previousHeadlessMode = main.headlessMode;
        int previousInstanceNumber = main.whoAmI;
        try {
            main.config = mock(Configuration.class);
            main.headlessMode = true;
            main.whoAmI = 1;
            UpgradeManager upgradeManager = spy(new UpgradeManager());
            AtomicInteger completed = new AtomicInteger();

            upgradeManager.checkForUpdates(true, completed::incrementAndGet);

            verify(upgradeManager, never()).checkFireflyUpdates(true);
            assertEquals(1, completed.get());
        } finally {
            main.config = previousConfig;
            main.headlessMode = previousHeadlessMode;
            main.whoAmI = previousInstanceNumber;
        }
    }

    /**
     * A headless Firefly update check exposes the available release and skips firmware updates.
     */
    @Test
    void headlessCheckPublishesFireflyUpdateWithoutInstalling() {
        MainSingleton main = MainSingleton.getInstance();
        GuiSingleton gui = GuiSingleton.getInstance();
        Configuration previousConfig = main.config;
        boolean previousHeadlessMode = main.headlessMode;
        boolean previousCommunicationError = main.communicationError;
        boolean previousUpgrade = gui.upgrade;
        int previousInstanceNumber = main.whoAmI;
        String previousVersion = main.version;
        try {
            main.config = mock(Configuration.class);
            when(main.config.isCheckForUpdates()).thenReturn(true);
            main.headlessMode = true;
            main.communicationError = true;
            main.whoAmI = 1;
            main.version = "1.0.0";
            gui.upgrade = false;
            UpgradeManager upgradeManager = spy(new UpgradeManager());
            doReturn(true).when(upgradeManager).checkRemoteUpdateFF("1.0.0");
            AtomicInteger completed = new AtomicInteger();

            upgradeManager.checkForUpdates(false, completed::incrementAndGet);

            assertTrue(gui.isUpgrade());
            verify(upgradeManager, never()).checkFireflyUpdates(false);
            verify(upgradeManager, never()).checkGlowWormUpdates(anyBoolean(), anyBoolean());
            assertEquals(1, completed.get());
        } finally {
            main.config = previousConfig;
            main.headlessMode = previousHeadlessMode;
            main.communicationError = previousCommunicationError;
            main.whoAmI = previousInstanceNumber;
            main.version = previousVersion;
            gui.upgrade = previousUpgrade;
        }
    }

    /**
     * A Firefly update also prevents firmware updates when the desktop interface is active.
     */
    @Test
    void desktopCheckSkipsFirmwareWhenFireflyUpdateIsAvailable() {
        MainSingleton main = MainSingleton.getInstance();
        boolean previousHeadlessMode = main.headlessMode;
        int previousInstanceNumber = main.whoAmI;
        try {
            main.headlessMode = false;
            main.whoAmI = 1;
            UpgradeManager upgradeManager = spy(new UpgradeManager());
            doReturn(true).when(upgradeManager).checkFireflyUpdates(false);
            AtomicInteger completed = new AtomicInteger();

            upgradeManager.checkForUpdates(false, completed::incrementAndGet);

            verify(upgradeManager, never()).checkGlowWormUpdates(anyBoolean(), anyBoolean());
            assertEquals(1, completed.get());
        } finally {
            main.headlessMode = previousHeadlessMode;
            main.whoAmI = previousInstanceNumber;
        }
    }

    /**
     * The web update state stays active while a headless device update runs and clears afterwards.
     */
    @Test
    void headlessFirmwareUpdatePublishesProgressState() {
        MainSingleton main = MainSingleton.getInstance();
        GuiSingleton gui = GuiSingleton.getInstance();
        Configuration previousConfig = main.config;
        boolean previousHeadlessMode = main.headlessMode;
        boolean previousCommunicationError = main.communicationError;
        boolean previousRunning = main.RUNNING;
        boolean previousUpdateState = gui.glowWormUpdateInProgress;
        int previousInstanceNumber = main.whoAmI;
        List<GlowWormDevice> previousDevices = new ArrayList<>(gui.deviceTableData);
        GlowWormDevice device = mock(GlowWormDevice.class);
        try (MockedStatic<NetworkManager> network = mockStatic(NetworkManager.class)) {
            main.config = mock(Configuration.class);
            when(main.config.isCheckForUpdates()).thenReturn(true);
            when(main.config.isFullFirmware()).thenReturn(true);
            when(main.config.isMqttEnable()).thenReturn(true);
            main.headlessMode = true;
            main.communicationError = false;
            main.RUNNING = false;
            main.whoAmI = 1;
            gui.glowWormUpdateInProgress = false;
            when(device.getDeviceName()).thenReturn("test device");
            when(device.getDeviceVersion()).thenReturn("5.0.0");
            when(device.getDeviceBoard()).thenReturn("ESP32");
            gui.deviceTableData.setAll(device);
            UpgradeManager upgradeManager = spy(new UpgradeManager());
            doReturn(true).when(upgradeManager).checkRemoteUpdateGW(true, "5.0.0");
            doAnswer(_ -> {
                assertTrue(gui.isGlowWormUpdateAvailable());
                assertTrue(gui.isGlowWormUpdateInProgress());
                gui.completeGlowWormUpdate(device);
                return null;
            }).when(upgradeManager).executeUpdate(device, false);

            upgradeManager.checkGlowWormUpdates(false, true);

            verify(upgradeManager).executeUpdate(device, false);
            assertFalse(gui.isGlowWormUpdateAvailable());
            assertFalse(gui.isGlowWormUpdateInProgress());
        } finally {
            main.config = previousConfig;
            main.headlessMode = previousHeadlessMode;
            main.communicationError = previousCommunicationError;
            main.RUNNING = previousRunning;
            main.whoAmI = previousInstanceNumber;
            gui.glowWormUpdateInProgress = previousUpdateState;
            gui.completeGlowWormUpdate(device);
            gui.deviceTableData.setAll(previousDevices);
        }
    }

    /**
     * A device requiring manual firmware installation keeps the web update notice visible.
     */
    @Test
    void manualFirmwareUpdateKeepsAvailabilityNotice() {
        MainSingleton main = MainSingleton.getInstance();
        GuiSingleton gui = GuiSingleton.getInstance();
        Configuration previousConfig = main.config;
        boolean previousHeadlessMode = main.headlessMode;
        boolean previousCommunicationError = main.communicationError;
        boolean previousUpdateState = gui.glowWormUpdateInProgress;
        List<GlowWormDevice> previousDevices = new ArrayList<>(gui.deviceTableData);
        GlowWormDevice device = mock(GlowWormDevice.class);
        try {
            main.config = mock(Configuration.class);
            when(main.config.isCheckForUpdates()).thenReturn(true);
            main.headlessMode = true;
            main.communicationError = false;
            gui.glowWormUpdateInProgress = false;
            when(device.getDeviceName()).thenReturn("manual device");
            when(device.getDeviceVersion()).thenReturn("5.0.0");
            gui.deviceTableData.setAll(device);
            UpgradeManager upgradeManager = spy(new UpgradeManager());
            doReturn(true).when(upgradeManager).checkRemoteUpdateGW(true, "5.0.0");

            upgradeManager.checkGlowWormUpdates(false, true);

            assertTrue(gui.isGlowWormUpdateAvailable());
            assertFalse(gui.isGlowWormUpdateInProgress());
            verify(upgradeManager, never()).executeUpdate(any(), anyBoolean());
        } finally {
            main.config = previousConfig;
            main.headlessMode = previousHeadlessMode;
            main.communicationError = previousCommunicationError;
            gui.glowWormUpdateInProgress = previousUpdateState;
            gui.completeGlowWormUpdate(device);
            gui.deviceTableData.setAll(previousDevices);
        }
    }

    /**
     * The update notice clears only after every registered device succeeds.
     */
    @Test
    void updateNoticeWaitsForAllDeviceSuccesses() {
        GuiSingleton gui = GuiSingleton.getInstance();
        GlowWormDevice first = mock(GlowWormDevice.class);
        GlowWormDevice second = mock(GlowWormDevice.class);
        when(first.getMac()).thenReturn("AA:BB:01");
        when(second.getMac()).thenReturn("AA:BB:02");
        try {
            gui.registerGlowWormUpdate(first);
            gui.registerGlowWormUpdate(second);

            gui.completeGlowWormUpdate(first);
            assertTrue(gui.isGlowWormUpdateAvailable());

            gui.completeGlowWormUpdate(second);
            assertFalse(gui.isGlowWormUpdateAvailable());
        } finally {
            gui.completeGlowWormUpdate(first);
            gui.completeGlowWormUpdate(second);
        }
    }

    @Test
    void versionNumberToNumber_basicVersions() {
        // Formula: Long.parseLong(major + 1_000_000) + Long.parseLong(minor + 1_000) + Long.parseLong(patch)
        assertEquals(21_281_004L, UpgradeManager.versionNumberToNumber("2.28.4"));
        assertEquals(11_001_000L, UpgradeManager.versionNumberToNumber("1.0.0"));
        assertEquals(31_501_010L, UpgradeManager.versionNumberToNumber("3.50.10"));
    }

    @Test
    void versionNumberToNumber_higherVersionYieldsLargerNumber() {
        long v1 = UpgradeManager.versionNumberToNumber("2.28.4");
        long v2 = UpgradeManager.versionNumberToNumber("2.29.0");
        long v3 = UpgradeManager.versionNumberToNumber("3.0.0");
        assertTrue(v1 < v2, "2.29.0 should be greater than 2.28.4");
        assertTrue(v2 < v3, "3.0.0 should be greater than 2.29.0");
    }

    @Test
    void versionNumberToNumber_minorBumpIncreasesValue() {
        long v1 = UpgradeManager.versionNumberToNumber("1.5.0");
        long v2 = UpgradeManager.versionNumberToNumber("1.6.0");
        assertTrue(v2 > v1, "Minor bump should increase the numeric value");
    }

    @Test
    void versionNumberToNumber_patchBumpIncreasesValue() {
        long v1 = UpgradeManager.versionNumberToNumber("1.0.3");
        long v2 = UpgradeManager.versionNumberToNumber("1.0.4");
        assertTrue(v2 > v1, "Patch bump should increase the numeric value");
    }

    @Test
    void versionNumberToNumber_majorBumpDominates() {
        long v1 = UpgradeManager.versionNumberToNumber("1.99.99");
        long v2 = UpgradeManager.versionNumberToNumber("2.0.0");
        assertTrue(v2 > v1, "Major bump should dominate over minor/patch");
    }

    @Test
    void versionNumberToNumber_threeDigitNumbers() {
        long v1 = UpgradeManager.versionNumberToNumber("10.20.30");
        long v2 = UpgradeManager.versionNumberToNumber("10.20.31");
        assertTrue(v2 > v1);
    }

    @Test
    void versionNumberToNumber_zeroPatch() {
        assertEquals(11_051_000L, UpgradeManager.versionNumberToNumber("1.5.0"));
    }

    @Test
    void versionNumberToNumber_sameVersionIsEqual() {
        long v1 = UpgradeManager.versionNumberToNumber("2.15.7");
        long v2 = UpgradeManager.versionNumberToNumber("2.15.7");
        assertEquals(v1, v2);
    }
}
