/*
  ConfigFileUpgraderTest.java

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
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ConfigFileUpgrader} version-migration methods.
 */
class ConfigFileUpgraderTest {

    private ConfigFileUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new ConfigFileUpgrader(new ObjectMapper(), "");
    }

    private Configuration config(String version) {
        Configuration c = new Configuration();
        c.setConfigVersion(version);
        return c;
    }

    // --- updatePrevious217 ---

    @Test
    void updatePrevious217_oldConfigDecrementsMonitorNumber() {
        Configuration c = config("2.1.6");
        c.setMonitorNumber(3);

        upgrader.updatePrevious217(c, false);

        assertEquals(2, c.getMonitorNumber());
        assertEquals(100, c.getTimeout());
    }

    @Test
    void updatePrevious217_newConfigNoChange() {
        Configuration c = config("2.1.8");
        c.setMonitorNumber(1);
        c.setTimeout(5);

        upgrader.updatePrevious217(c, false);

        assertEquals(1, c.getMonitorNumber());
        assertEquals(5, c.getTimeout());
    }

    // --- updatePrevious247 ---

    @Test
    void updatePrevious247_oldConfigSetsWhiteTemperature() {
        Configuration c = config("2.4.7");
        c.setWhiteTemperature(0);

        upgrader.updatePrevious247(c, false);

        assertEquals(Constants.DEFAULT_WHITE_TEMP, c.getWhiteTemperature());
    }

    @Test
    void updatePrevious247_newConfigNoChange() {
        Configuration c = config("2.5.0");
        c.setWhiteTemperature(99);

        upgrader.updatePrevious247(c, false);

        assertEquals(99, c.getWhiteTemperature());
    }

    // --- updatePrevious21010 ---

    @Test
    void updatePrevious21010_trueBecomesTrace() {
        Configuration c = config("2.10.9");
        c.setRuntimeLogLevel(Constants.TRUE);

        upgrader.updatePrevious21010(c, false);

        assertEquals("TRACE", c.getRuntimeLogLevel());
    }

    @Test
    void updatePrevious21010_falseBecomesInfo() {
        Configuration c = config("2.10.9");
        c.setRuntimeLogLevel(Constants.FALSE);

        upgrader.updatePrevious21010(c, false);

        assertEquals("INFO", c.getRuntimeLogLevel());
    }

    @Test
    void updatePrevious21010_newConfigNoChange() {
        Configuration c = config("2.10.11");
        c.setRuntimeLogLevel("DEBUG");

        upgrader.updatePrevious21010(c, false);

        assertEquals("DEBUG", c.getRuntimeLogLevel());
    }

    // --- updatePrevious2187 ---

    @Test
    void updatePrevious2187_migratesGstreamerDdupl() {
        Configuration c = config("2.18.6");
        c.setCaptureMethod(Constants.GSTREAMER_DDUPL);

        upgrader.updatePrevious2187(c, false);

        assertEquals(Configuration.CaptureMethod.DDUPL_DX12.name(), c.getCaptureMethod());
    }

    @Test
    void updatePrevious2187_otherCaptureMethodUnchanged() {
        Configuration c = config("2.18.6");
        c.setCaptureMethod(Configuration.CaptureMethod.CPU.name());

        upgrader.updatePrevious2187(c, false);

        assertEquals(Configuration.CaptureMethod.CPU.name(), c.getCaptureMethod());
    }

    // --- updatePrevious2256 ---

    @Test
    void updatePrevious2256_mapsClassicTheme() {
        Configuration c = config("2.25.5");
        c.setTheme("Classic theme");

        upgrader.updatePrevious2256(c, false);

        assertEquals(Enums.Theme.CLASSIC.getBaseI18n(), c.getTheme());
    }

    @Test
    void updatePrevious2256_mapsDarkTheme() {
        Configuration c = config("2.25.5");
        c.setTheme("Dark theme");

        upgrader.updatePrevious2256(c, false);

        assertEquals(Enums.Theme.DARK_THEME_CYAN.getBaseI18n(), c.getTheme());
    }

    @Test
    void updatePrevious2256_mapsDarkBlueTheme() {
        Configuration c = config("2.25.5");
        c.setTheme("Dark blue theme");

        upgrader.updatePrevious2256(c, false);

        assertEquals(Enums.Theme.DARK_BLUE_THEME.getBaseI18n(), c.getTheme());
    }

    @Test
    void updatePrevious2256_mapsDarkPurpleTheme() {
        Configuration c = config("2.25.5");
        c.setTheme("Dark purple theme");

        upgrader.updatePrevious2256(c, false);

        assertEquals(Enums.Theme.DARK_THEME_PURPLE.getBaseI18n(), c.getTheme());
    }

    @Test
    void updatePrevious2256_mapsLightGrayTheme() {
        Configuration c = config("2.25.5");
        c.setTheme("Light gray theme");

        upgrader.updatePrevious2256(c, false);

        assertEquals(Enums.Theme.DARK_THEME_ORANGE.getBaseI18n(), c.getTheme());
    }

    @Test
    void updatePrevious2256_newConfigNoChange() {
        Configuration c = config("2.25.7");
        c.setTheme("custom-theme");

        upgrader.updatePrevious2256(c, false);

        assertEquals("custom-theme", c.getTheme());
    }

    // --- updatePrevious2284 ---

    @Test
    void updatePrevious2284_resetsUdpTrafficClass() {
        Configuration c = config("2.28.3");
        c.setUdpTrafficClass(999);

        upgrader.updatePrevious2284(c, false);

        assertEquals(Constants.DEFAULT_UDP_TRAFFIC_CLASS, c.getUdpTrafficClass());
    }

    @Test
    void updatePrevious2284_alreadyDefaultNoChange() {
        Configuration c = config("2.28.3");
        c.setUdpTrafficClass(Constants.DEFAULT_UDP_TRAFFIC_CLASS);

        upgrader.updatePrevious2284(c, false);

        assertEquals(Constants.DEFAULT_UDP_TRAFFIC_CLASS, c.getUdpTrafficClass());
    }

    // --- updatePrevious2295 ---

    @Test
    void updatePrevious2295_resetsUdpTrafficClassAndSetsGamma() {
        Configuration c = config("2.29.4");
        c.setUdpTrafficClass(999);
        c.setGamma(1.0);

        upgrader.updatePrevious2295(c, false);

        assertEquals(Constants.DEFAULT_UDP_TRAFFIC_CLASS, c.getUdpTrafficClass());
        assertEquals(Double.parseDouble(Enums.Gamma.GAMMA_22.getGamma()), c.getGamma(), 0.01);
    }
}
