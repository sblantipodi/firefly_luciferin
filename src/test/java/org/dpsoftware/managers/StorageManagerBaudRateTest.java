/*
  StorageManagerBaudRateTest.java

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageManagerBaudRateTest {
    private static final String PROFILE = "Cinema";
    private final MainSingleton main = MainSingleton.getInstance();
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    @TempDir
    Path directory;

    private Configuration previousConfig;
    private String previousProfile;
    private int previousInstance;
    private StorageManager storage;

    /**
     * Configure the current instance to use a named serial profile in the temporary directory.
     */
    @BeforeEach
    void useNamedSerialProfile() {
        previousConfig = main.config;
        previousProfile = main.profileArg;
        previousInstance = main.whoAmI;
        main.config = new Configuration();
        main.config.setOutputDevice("COM14");
        main.config.setBaudRate("230400");
        main.profileArg = PROFILE;
        main.whoAmI = 1;
        storage = new StorageManager(directory.toString());
    }

    /**
     * Restore the instance and profile configuration used before the test.
     */
    @AfterEach
    void restoreProfile() {
        main.config = previousConfig;
        main.profileArg = previousProfile;
        main.whoAmI = previousInstance;
    }

    /**
     * Verify that automatic detection updates the active profile and leaves its other values intact.
     *
     * @throws Exception if the test profiles cannot be written or read
     */
    @Test
    void updatesOnlyTheBaudRateInTheActiveProfile() throws Exception {
        Path active = directory.resolve("1_Cinema.yaml");
        Path other = directory.resolve(Constants.CONFIG_FILENAME);
        Files.writeString(active, "outputDevice: AUTO\nbaudRate: '230400'\nbrightness: 37\nunknownSetting: [keep, 42]\n");
        Files.writeString(other, "outputDevice: COM14\nbaudRate: '500000'\n");
        ObjectNode expected = (ObjectNode) yaml.readTree(active.toFile());
        expected.put("baudRate", "921600");

        assertTrue(storage.writeDetectedBaudRate(PROFILE, "COM14", "230400", "921600"));
        assertEquals(expected, yaml.readTree(active.toFile()));
        assertEquals("921600", main.config.getBaudRate());
        assertEquals("500000", yaml.readTree(other.toFile()).path("baudRate").asText());
        assertFalse(storage.writeDetectedBaudRate(PROFILE, "COM14", "921600", "921600"));
    }

    /**
     * Verify that a newer manual baud rate takes precedence over the result of an older probe.
     *
     * @throws Exception if the test profile cannot be written or read
     */
    @Test
    void leavesANewerManualSettingOnDisk() throws Exception {
        Path active = directory.resolve("1_Cinema.yaml");
        String updated = "outputDevice: COM14\nbaudRate: '115200'\n";
        Files.writeString(active, updated);

        assertFalse(storage.writeDetectedBaudRate(PROFILE, "COM14", "230400", "921600"));
        assertEquals(updated, Files.readString(active));
        assertEquals("230400", main.config.getBaudRate());
    }

    /**
     * Verify that saving unrelated settings preserves the rate saved by automatic detection.
     *
     * @throws Exception if the test profile cannot be written or read
     */
    @Test
    void preservesTheUpdatedBaudRateDuringAnUnrelatedSettingsSave() throws Exception {
        Path active = directory.resolve("1_Cinema.yaml");
        Files.writeString(active, "outputDevice: COM14\nbaudRate: '230400'\n");
        Configuration draft = storage.readConfigFile("1_Cinema.yaml");
        draft.setBrightness(37);
        assertTrue(storage.writeDetectedBaudRate(PROFILE, "COM14", "230400", "921600"));

        storage.writeConfig(draft, null, true);

        var saved = storage.readConfigFile("1_Cinema.yaml");
        assertEquals("921600", saved.getBaudRate());
        assertEquals(37, saved.getBrightness());
    }
}
