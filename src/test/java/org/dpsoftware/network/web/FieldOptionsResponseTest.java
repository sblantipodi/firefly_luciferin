/*
  FieldOptionsResponseTest.java

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

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.gui.controllers.options.SmoothingOptions;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldOptionsResponseTest {

    /**
     * Checks the complete options response used before the web form is built.
     */
    @Test
    void serializesFieldOptionsResponse() throws Exception {
        MainSingleton.getInstance().config = new Configuration();
        var response = CommonUtility.JSON_MAPPER.createObjectNode();
        response.set("options", CommonUtility.JSON_MAPPER.valueToTree(FieldOptions.getFieldOptions()));
        response.set("smoothingPresets", CommonUtility.JSON_MAPPER.valueToTree(SmoothingOptions.presets()));
        var labels = FieldOptions.getFieldLabels();
        FieldOptions.applyToggleLedLabels(labels);
        response.set("labels", CommonUtility.JSON_MAPPER.valueToTree(labels));
        assertNotNull(response.get("options").get("smoothingType"));
        assertNotNull(response.get("options").get("gammaLevel"));
        assertTrue(response.get("options").get("cubeLut").get("options").size() > 1);
        assertTrue(response.get("options").get("multiMonitor").get("options").size() >= 1);
        assertTrue(response.get("options").get("satelliteZone").get("options").size() > 1);
        assertTrue(response.get("options").get("satelliteOrientation").get("options").size() > 1);
        assertTrue(response.get("options").get("brightnessLimiter").get("options").size() > 1);
        assertTrue(response.get("options").get("minimumBrightness").get("options").size() > 1);
        assertEquals(8, response.get("options").get("ldrInterval").get("options").size());
        assertEquals("0", response.get("options").get("ldrInterval").get("options").get(0).get("value").asText());
        assertEquals(10, response.get("options").get("minimumBrightness").get("options").size());
        assertEquals("1.0", response.get("options").get("brightnessLimiter").get("options").get(0).get("value").asText());
        assertNotNull(response.get("labels").get("ldrLabel"));
        assertNotNull(response.get("labels").get("web.logLevel"));
        assertNotNull(response.get("labels").get("web.device.reachable"));
        assertNotNull(response.get("labels").get("web.device.unreachable"));
        assertNotNull(response.get("labels").get("satelliteAdd"));
        assertNotNull(response.get("labels").get("enableAutomaticGamma"));
        assertNotNull(CommonUtility.JSON_MAPPER.writeValueAsString(response));
    }
}
