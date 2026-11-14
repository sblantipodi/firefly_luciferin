/*
  LivePreviewLedLayoutTest.java

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

import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LivePreviewLedLayoutTest {
    private Configuration configuration() {
        var config = new Configuration();
        config.setScreenResX(3840);
        config.setScreenResY(2160);
        config.setOsScaling(150);
        config.setResamplingFactor(4);
        config.setOrientation(Enums.Orientation.ANTICLOCKWISE.getBaseI18n());
        config.setDefaultLedMatrix("fullscreen");
        var matrix = new LinkedHashMap<Integer, LEDCoordinate>();
        matrix.put(1, new LEDCoordinate(0, 0, 1920, 200, false, "top"));
        matrix.put(2, new LEDCoordinate(0, 0, 1920, 200, true, "top"));
        var disabled = new LEDCoordinate(1920, 0, 1920, 200, false, "top");
        disabled.setActive(false);
        matrix.put(3, disabled);
        matrix.put(4, new LEDCoordinate(1920, 0, 1920, 200, true, "top"));
        var matrices = new LinkedHashMap<String, LinkedHashMap<Integer, LEDCoordinate>>();
        matrices.put("fullscreen", matrix);
        var letterbox = new LinkedHashMap<Integer, LEDCoordinate>();
        letterbox.put(1, new LEDCoordinate(0, 270, 1920, 200, false, "top"));
        matrices.put("letterbox", letterbox);
        config.setLedMatrix(matrices);
        return config;
    }

    @Test
    void preservesSourceCoordinatesAndDisabledZonesWithoutDuplicatingGroupedLeds() throws Exception {
        var layout = LivePreviewWebHandler.ledLayout(configuration());
        assertEquals(3840, layout.width());
        assertEquals(2160, layout.height());
        assertEquals(List.of(1, 3), layout.leds().stream().map(LivePreviewWebHandler.PreviewLed::number).toList());
        assertEquals(1920, layout.leds().getLast().x());
        assertEquals(1920, layout.leds().getFirst().width());
        assertFalse(layout.leds().getLast().active());
        var json = CommonUtility.JSON_MAPPER.readTree(CommonUtility.JSON_MAPPER.writeValueAsBytes(layout));
        assertEquals(3, json.path("leds").get(1).path("number").asInt());
    }

    @Test
    void followsOrientationOffsetAndRuntimeAspectRatio() {
        var config = configuration();
        config.setLedStartOffset(1);
        assertEquals(List.of(4, 2), LivePreviewWebHandler.ledLayout(config).leds().stream()
                .map(LivePreviewWebHandler.PreviewLed::number).toList());
        config.setOrientation(Enums.Orientation.CLOCKWISE.getBaseI18n());
        assertEquals(List.of(3, 1), LivePreviewWebHandler.ledLayout(config).leds().stream()
                .map(LivePreviewWebHandler.PreviewLed::number).toList());
        config.setDefaultLedMatrix("letterbox");
        assertEquals(270, LivePreviewWebHandler.ledLayout(config).leds().getFirst().y());
    }
}
