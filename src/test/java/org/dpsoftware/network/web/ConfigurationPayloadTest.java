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
