/*
  StorageManagerTest.java

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link StorageManager#checkProfileDifferences(Configuration, Configuration)}.
 * Uses ByteBuddy-powered mockStatic to intercept MainSingleton.getInstance() without modifying the source.
 */
class StorageManagerTest {

    private MainSingleton mockedInstance;
    private MockedStatic<MainSingleton> mockedMainSingleton;

    private static Configuration createConfig() {
        Configuration config = new Configuration();
        config.setLanguage("en");
        config.setTheme("Light");
        config.setBaudRate("8");
        config.setCaptureMethod("default");
        config.setOutputDevice("auto");
        config.setStaticGlowWormIp("");
        config.setNumberOfCPUThreads(4);
        config.setFullFirmware(true);
        config.setWirelessStream(false);
        config.setMqttEnable(false);
        config.setStreamType("udp");
        config.setMqttServer("localhost");
        config.setMqttTopic("topic");
        config.setMqttUsername("");
        config.setMqttPwd("");
        config.setMultiScreenSingleDevice(false);
        config.setMultiMonitor(1);
        config.setSimdAvx(0);
        return config;
    }

    @BeforeEach
    void setUp() {
        mockedInstance = new MainSingleton();
        mockedMainSingleton = Mockito.mockStatic(MainSingleton.class);
        mockedMainSingleton.when(MainSingleton::getInstance).thenReturn(mockedInstance);
    }

    @AfterEach
    void tearDown() {
        mockedMainSingleton.close();
    }

    @Test
    void checkProfileDifferences_identicalConfigsNoRestartNeeded() {
        Configuration defaultConfig = createConfig();
        Configuration profileConfig = createConfig();

        StorageManager sm = new StorageManager();
        sm.checkProfileDifferences(defaultConfig, profileConfig);

        assertFalse(mockedInstance.restartNeeded);
    }

    @Test
    void checkProfileDifferences_differentLanguageTriggersRestart() {
        Configuration defaultConfig = createConfig();
        Configuration profileConfig = createConfig();
        profileConfig.setLanguage("fr");

        StorageManager sm = new StorageManager();
        sm.checkProfileDifferences(defaultConfig, profileConfig);

        assertTrue(mockedInstance.restartNeeded);
    }

    @Test
    void checkProfileDifferences_differentThemeTriggersRestart() {
        Configuration defaultConfig = createConfig();
        Configuration profileConfig = createConfig();
        profileConfig.setTheme("Dark");

        StorageManager sm = new StorageManager();
        sm.checkProfileDifferences(defaultConfig, profileConfig);

        assertTrue(mockedInstance.restartNeeded);
    }

    @Test
    void checkProfileDifferences_differentCpuThreadsTriggersRestart() {
        Configuration defaultConfig = createConfig();
        Configuration profileConfig = createConfig();
        profileConfig.setNumberOfCPUThreads(8);

        StorageManager sm = new StorageManager();
        sm.checkProfileDifferences(defaultConfig, profileConfig);

        assertTrue(mockedInstance.restartNeeded);
    }

    @Test
    void checkProfileDifferences_differentCaptureMethodTriggersRestart() {
        Configuration defaultConfig = createConfig();
        Configuration profileConfig = createConfig();
        profileConfig.setCaptureMethod("different");

        StorageManager sm = new StorageManager();
        sm.checkProfileDifferences(defaultConfig, profileConfig);

        assertTrue(mockedInstance.restartNeeded);
    }

    @Test
    void checkProfileDifferences_differentFirmwareTypeTriggersRestart() {
        Configuration defaultConfig = createConfig();
        defaultConfig.setFullFirmware(true);
        Configuration profileConfig = createConfig();
        profileConfig.setFullFirmware(false);

        StorageManager sm = new StorageManager();
        sm.checkProfileDifferences(defaultConfig, profileConfig);

        assertTrue(mockedInstance.restartNeeded);
    }

    @Test
    void checkProfileDifferences_nullProfileConfigDoesNothing() {
        Configuration defaultConfig = createConfig();
        mockedInstance.setRestartNeeded(true);

        StorageManager sm = new StorageManager();
        sm.checkProfileDifferences(defaultConfig, null);

        // Should remain unchanged — method returns early when profileConfig is null
        assertTrue(mockedInstance.restartNeeded);
    }

    // --- Helper ---

    @Test
    void checkProfileDifferences_nullDefaultConfigDoesNothing() {
        Configuration profileConfig = createConfig();
        mockedInstance.setRestartNeeded(true);

        StorageManager sm = new StorageManager();
        sm.checkProfileDifferences(null, profileConfig);

        // Should remain unchanged — method returns early when defaultConfig is null
        assertTrue(mockedInstance.restartNeeded);
    }
}
