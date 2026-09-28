package org.dpsoftware.network.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        assertEquals(true, updated.isAutoDetectBlackBars());
        assertEquals(Enums.AspectRatio.FULLSCREEN.getBaseI18n(), updated.getDefaultLedMatrix());
    }
}
