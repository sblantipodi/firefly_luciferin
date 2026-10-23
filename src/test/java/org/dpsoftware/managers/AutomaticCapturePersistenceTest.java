/*
  AutomaticCapturePersistenceTest.java

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
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.InstanceConfigurer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class AutomaticCapturePersistenceTest {
    @Test
    void preservesAutoUntilProbeCompletesAndDoesNotPersistUnverifiedCandidates(@TempDir Path directory) throws Exception {
        MainSingleton main = new MainSingleton();
        main.whoAmI = 1;
        Configuration config = new Configuration();
        try (MockedStatic<MainSingleton> singleton = mockStatic(MainSingleton.class);
             MockedStatic<InstanceConfigurer> paths = mockStatic(InstanceConfigurer.class);
             MockedStatic<NativeExecutor> os = mockStatic(NativeExecutor.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            paths.when(InstanceConfigurer::getConfigPath).thenReturn(directory.toString());
            os.when(NativeExecutor::isWayland).thenReturn(true);
            StorageManager storage = new StorageManager();
            storage.resolveAutomaticCapture(config);
            assertTrue(config.isAutomaticCapturePending());
            assertEquals("PIPEWIREXDG", config.getCaptureMethod());
            assertEquals("AUTO", storage.readConfigFile(Constants.CONFIG_FILENAME).getCaptureMethod());

            config.setCaptureMethod("PIPEWIREXDG_AMD_HIP");
            storage.writeConfig(config, null);
            Configuration nextStartup = storage.readConfigFile(Constants.CONFIG_FILENAME);
            assertEquals("AUTO", nextStartup.getCaptureMethod());
            assertEquals("PIPEWIREXDG_AMD_HIP", config.getCaptureMethod());
            storage.resolveAutomaticCapture(nextStartup);
            assertTrue(nextStartup.isAutomaticCapturePending());

            config.setAutomaticCapturePending(false);
            storage.writeConfig(config, null);
            assertEquals("PIPEWIREXDG_AMD_HIP", storage.readConfigFile(Constants.CONFIG_FILENAME).getCaptureMethod());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void headlessWithoutDevicePreservesAutoAcrossManualSaveAndRestart(boolean windows, @TempDir Path directory) throws Exception {
        MainSingleton main = new MainSingleton();
        main.whoAmI = 1;
        main.setHeadlessMode(true);
        try (MockedStatic<MainSingleton> singleton = mockStatic(MainSingleton.class);
             MockedStatic<InstanceConfigurer> paths = mockStatic(InstanceConfigurer.class);
             MockedStatic<NativeExecutor> os = mockStatic(NativeExecutor.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            paths.when(InstanceConfigurer::getConfigPath).thenReturn(directory.toString());
            os.when(NativeExecutor::isWindows).thenReturn(windows);
            StorageManager storage = new StorageManager();
            Configuration config = new Configuration();
            storage.resolveAutomaticCapture(config);
            assertEquals(windows ? "WIN_USB_VIDEO" : "USB_VIDEO", config.getCaptureMethod());
            assertEquals("AUTO", storage.readConfigFile(Constants.CONFIG_FILENAME).getCaptureMethod());
            assertTrue(config.isAutomaticCapturePending());
            assertNull(config.getCaptureDevice());

            // Web and desktop manual saves read the persisted settings into a separate configuration.
            Configuration edited = storage.readConfigFile(Constants.CONFIG_FILENAME);
            edited.setBrightness(123);
            storage.writeConfig(edited, null);
            Configuration nextStartup = storage.readConfigFile(Constants.CONFIG_FILENAME);
            assertEquals("AUTO", nextStartup.getCaptureMethod());
            assertEquals(123, nextStartup.getBrightness());
            storage.resolveAutomaticCapture(nextStartup);
            assertTrue(nextStartup.isAutomaticCapturePending());

            // An explicit manual method still overrides automatic selection.
            edited.setCaptureMethod(windows ? "WIN_USB_VIDEO" : "USB_VIDEO");
            storage.writeConfig(edited, null);
            Configuration manualStartup = storage.readConfigFile(Constants.CONFIG_FILENAME);
            storage.resolveAutomaticCapture(manualStartup);
            assertFalse(manualStartup.isAutomaticCapturePending());
            assertEquals(edited.getCaptureMethod(), manualStartup.getCaptureMethod());
        }
    }
}
