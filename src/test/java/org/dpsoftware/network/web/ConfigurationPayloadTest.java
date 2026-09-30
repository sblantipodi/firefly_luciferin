/*
  ConfigurationPayloadTest.java

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
package org.dpsoftware.network.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationPayloadTest {

    private static Configuration savedConfig() {
        Configuration config = new Configuration();
        config.setTopLed(10);
        config.setLeftLed(8);
        config.setRightLed(9);
        config.setBottomLeftLed(7);
        config.setBottomRightLed(6);
        config.setGroupBy(2);
        return config;
    }

    @Test
    void rejectsInvalidLedCountsAndGrouping() {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("topLed", "12abc");
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, savedConfig()));

        payload.remove("topLed");
        payload.put("groupBy", 7);
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, savedConfig()));
    }

    @Test
    void rejectsPercentagesOutsideEachRange() {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("splitBottomMargin", "96%");
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, savedConfig()));

        payload.remove("splitBottomMargin");
        payload.put("grabberSide", "41%");
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, savedConfig()));
    }

    @Test
    void acceptsExistingGroupingAtMinimum() throws IOException {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("groupBy", 6);
        assertEquals(6, ConfigurationPayload.apply(payload, savedConfig()).getGroupBy());
    }

    @Test
    void preservesFieldsHiddenFromModeAccordion() throws IOException {
        Configuration saved = savedConfig();
        saved.setTheme("Classic");
        saved.setOutputDevice("Existing device");
        saved.setDesiredFramerate("60 FPS");
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("language", "Italiano");

        Configuration updated = ConfigurationPayload.apply(payload, saved);

        assertEquals("Classic", updated.getTheme());
        assertEquals("Existing device", updated.getOutputDevice());
        assertEquals("60 FPS", updated.getDesiredFramerate());
        assertEquals("Italiano", updated.getLanguage());
    }

    @Test
    void preservesNetworkFieldsHiddenFromWebAccordion() throws IOException {
        Configuration saved = savedConfig();
        saved.setStreamType("UDP");
        saved.setMqttServer("tcp://old-host:1883");
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("mqttTopic", "new-topic");

        Configuration updated = ConfigurationPayload.apply(payload, saved);

        assertEquals("UDP", updated.getStreamType());
        assertEquals("tcp://old-host:1883", updated.getMqttServer());
        assertEquals("new-topic", updated.getMqttTopic());
    }

    @Test
    void rejectsInvalidMqttServerPort() {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("mqttServer", "tcp://broker:70000");
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, savedConfig()));
    }

    /**
     * Checks that Misc controls use GUI units while configuration retains stored units.
     */
    @Test
    void convertsMiscTabControlsToStoredValues() throws IOException {
        Configuration saved = savedConfig();
        saved.setColorChooser("10,20,30,128");
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("brightness", 50);
        payload.put("whiteTemperature", 6500);
        payload.put("colorChooserHex", "#ff8040");
        payload.put("desiredFramerate", "60 FPS");

        Configuration updated = ConfigurationPayload.apply(payload, saved);

        assertEquals(127, updated.getBrightness());
        assertEquals(65, updated.getWhiteTemperature());
        assertEquals("255,128,64,128", updated.getColorChooser());
        assertEquals("60", updated.getDesiredFramerate());
    }

    /**
     * Checks that a built-in smoothing level updates its dependent settings.
     */
    @Test
    void appliesMiscSmoothingDependencies() throws IOException {
        Configuration saved = savedConfig();
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("smoothingType", Enums.Smoothing.SMOOTHING_LVL_2.getBaseI18n());

        Configuration updated = ConfigurationPayload.apply(payload, saved);

        assertEquals(30, updated.getFrameInsertionTarget());
        assertEquals(0.30F, updated.getEmaAlpha());
    }

    /**
     * Checks that web dialog controls derive a custom smoothing level on save.
     */
    @Test
    void savesSmoothingDialogControls() throws IOException {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("emaAlpha", 0.25);
        payload.put("frameInsertionTarget", 30);
        payload.put("smoothingTargetFramerate", 120);
        payload.put("smoothingType", Enums.Smoothing.CUSTOM.getBaseI18n());

        Configuration updated = ConfigurationPayload.apply(payload, savedConfig());

        assertEquals(0.25f, updated.getEmaAlpha());
        assertEquals(30, updated.getFrameInsertionTarget());
        assertEquals(120, updated.getSmoothingTargetFramerate());
        assertEquals(Enums.Smoothing.CUSTOM.getBaseI18n(), updated.getSmoothingType());
    }

    /**
     * Checks that removing night controls from the form preserves their saved values.
     */
    @Test
    void preservesHiddenNightModeValues() throws IOException {
        Configuration saved = savedConfig();
        saved.setNightModeFrom("22:00");
        saved.setNightModeTo("06:00");
        saved.setNightModeBrightness("40%");

        Configuration updated = ConfigurationPayload.apply(CommonUtility.JSON_MAPPER.createObjectNode(), saved);

        assertEquals("22:00", updated.getNightModeFrom());
        assertEquals("06:00", updated.getNightModeTo());
        assertEquals("40%", updated.getNightModeBrightness());
    }

    /**
     * Checks that the editable device field stores a selected name or a fixed IP like JavaFX.
     */
    @Test
    void savesEditableOutputDevice() throws IOException {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("serialPort", "Glow Worm");
        Configuration named = ConfigurationPayload.apply(payload, savedConfig());
        assertEquals("Glow Worm", named.getOutputDevice());
        assertEquals("-", named.getStaticGlowWormIp());

        payload.put("serialPort", "192.168.1.10");
        Configuration fixedIp = ConfigurationPayload.apply(payload, savedConfig());
        assertEquals("-", fixedIp.getOutputDevice());
        assertEquals("192.168.1.10", fixedIp.getStaticGlowWormIp());
    }

    /**
     * Checks device field validation and preservation of hidden LDR values.
     */
    @Test
    void validatesDeviceFieldsAndPreservesLdr() throws IOException {
        Configuration saved = savedConfig();
        saved.setLdrInterval(42);
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("multiMonitor", 4);
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, saved));
        payload.remove("multiMonitor");
        payload.put("baudRate", "invalid");
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, saved));
        payload.remove("baudRate");
        payload.put("powerSaving", Enums.PowerSaving.DISABLED.getBaseI18n());
        assertEquals(42, ConfigurationPayload.apply(payload, saved).getLdrInterval());
    }

    /**
     * Checks that tone mapping accepts an editable LUT name and disables an empty selection.
     */
    @Test
    void savesToneMappingSelection() throws IOException {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("cubeLut", " custom.cube ");
        assertEquals("custom.cube", ConfigurationPayload.apply(payload, savedConfig()).getCubeLut());

        payload.put("cubeLut", " ");
        assertEquals(org.dpsoftware.config.Constants.DISABLED,
                ConfigurationPayload.apply(payload, savedConfig()).getCubeLut());
    }

    /**
     * Checks satellite additions, base-value storage and removal through the web payload.
     */
    @Test
    void savesSatelliteRows() throws IOException {
        Configuration saved = savedConfig();
        var original = new org.dpsoftware.gui.elements.Satellite("Top", "Normal", "2",
                "192.168.1.20", "Old satellite", "Average color");
        saved.getSatellites().put(original.getDeviceIp(), original);
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        ObjectNode satellites = payload.putObject("satellites");
        satellites.putObject("192.168.1.21")
                .put("deviceIp", "192.168.1.21")
                .put("zone", Enums.PossibleZones.TOP.getBaseI18n())
                .put("orientation", Enums.Direction.NORMAL.getBaseI18n())
                .put("ledNum", "3")
                .put("algo", Enums.Algo.AVG_COLOR.getBaseI18n());

        Configuration updated = ConfigurationPayload.apply(payload, saved);

        assertEquals(1, updated.getSatellites().size());
        assertEquals("3", updated.getSatellites().get("192.168.1.21").getLedNum());
        assertTrue(ConfigurationPayload.toWebConfig(updated).path("satellites").has("192.168.1.21"));
        satellites.remove("192.168.1.21");
        assertTrue(ConfigurationPayload.apply(payload, saved).getSatellites().isEmpty());
    }

    /**
     * Checks the selected eye care settings are stored in configuration units.
     */
    @Test
    void savesAdvancedEyeCareControls() throws IOException {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("luminosityThreshold", 25);
        payload.put("brightnessLimiter", Enums.BrightnessLimiter.BRIGHTNESS_LIMIT_70.getBrightnessLimitFloat());
        payload.put("enableLDR", true);
        payload.put("ldrTurnOff", true);
        payload.put("ldrInterval", Enums.LdrInterval.MINUTES_20.getLdrIntervalInteger());
        payload.put("ldrMin", 30);

        Configuration updated = ConfigurationPayload.apply(payload, savedConfig());

        assertEquals(25, updated.getLuminosityThreshold());
        assertEquals(0.7f, updated.getBrightnessLimiter());
        assertTrue(updated.isEnableLDR());
        assertTrue(updated.isLdrTurnOff());
        assertEquals(20, updated.getLdrInterval());
        assertEquals(30, updated.getLdrMin());
        ObjectNode webConfig = ConfigurationPayload.toWebConfig(updated);
        assertTrue(webConfig.path("enableLDR").asBoolean());
        assertEquals(20, webConfig.path("ldrInterval").asInt());
        assertEquals(30, webConfig.path("ldrMin").asInt());
        assertEquals(0.7, webConfig.path("brightnessLimiter").asDouble(), 0.0001);
    }

    /**
     * Checks the LDR and threshold controls reject values outside the JavaFX choices.
     */
    @Test
    void rejectsInvalidAdvancedEyeCareControls() {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("luminosityThreshold", 51);
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, savedConfig()));
        payload.remove("luminosityThreshold");
        payload.put("ldrInterval", 15);
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, savedConfig()));
    }

    /**
     * Checks each supported log level is saved and an unsupported level is rejected.
     */
    @Test
    void savesRuntimeLogLevel() throws IOException {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        for (String level : List.of("ERROR", "WARN", "INFO", "DEBUG", "TRACE")) {
            payload.put("runtimeLogLevel", level);
            Configuration updated = ConfigurationPayload.apply(payload, savedConfig());
            assertEquals(level, updated.getRuntimeLogLevel());
            assertEquals(level, ConfigurationPayload.toWebConfig(updated).path("runtimeLogLevel").asText());
        }
        payload.put("runtimeLogLevel", "OFF");
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, savedConfig()));
    }

    /**
     * Checks that web adaptive gamma changes use the dialog's stored level format.
     */
    @Test
    void savesAdaptiveGammaSettings() throws IOException {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("enableAutomaticGamma", false);
        payload.put("gammaLevel", Enums.GammaLevel.MEDIUM.getI18n());

        Configuration updated = ConfigurationPayload.apply(payload, savedConfig());

        assertFalse(updated.isEnableAutomaticGamma());
        assertEquals(Enums.GammaLevel.MEDIUM.getBaseI18n(), updated.getGammaLevel());
    }

    @Test
    void rejectsNonnumericScreenDimensions() {
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("screenResX", "1920px");
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPayload.apply(payload, savedConfig()));
    }

    @Test
    void automaticAspectRatioMatchesModeTabBehavior() throws IOException {
        Configuration saved = savedConfig();
        saved.setDefaultLedMatrix(Enums.AspectRatio.LETTERBOX.getBaseI18n());
        ObjectNode payload = CommonUtility.JSON_MAPPER.createObjectNode();
        payload.put("autoDetectBlackBars", true);

        Configuration updated = ConfigurationPayload.apply(payload, saved);

        assertTrue(updated.isAutoDetectBlackBars());
        assertEquals(Enums.AspectRatio.FULLSCREEN.getBaseI18n(), updated.getDefaultLedMatrix());
    }
}
