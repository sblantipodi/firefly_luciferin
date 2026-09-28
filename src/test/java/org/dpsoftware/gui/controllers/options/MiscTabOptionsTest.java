/*
  MiscTabOptionsTest.java

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
package org.dpsoftware.gui.controllers.options;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MiscTabOptionsTest {

    /**
     * Checks the display units and stored units of the main Misc sliders.
     */
    @Test
    void convertsSliderValues() {
        assertEquals(127, MiscTabOptions.storedBrightness(50));
        assertEquals(50, MiscTabOptions.brightnessPercent(127));
        assertEquals(65, MiscTabOptions.storedWhiteTemperature(6500));
        assertEquals(6500, MiscTabOptions.whiteTemperatureKelvin(65));
    }

    /**
     * Checks editable framerates and music effects shared with the web form.
     */
    @Test
    void recognizesFramerateAndAudioEffect() {
        assertEquals("45", MiscTabOptions.storedFramerate("45 FPS"));
        assertEquals(Enums.Framerate.UNLOCKED.getBaseI18n(),
                MiscTabOptions.storedFramerate(Enums.Framerate.UNLOCKED.getI18n()));
        assertTrue(MiscTabOptions.isAudioEffect(Enums.Effect.MUSIC_MODE_VU_METER.getBaseI18n()));
    }

    /**
     * Checks that live Misc changes update the running configuration and reject invalid values.
     */
    @Test
    void appliesLiveMiscValues() {
        Configuration config = new Configuration();
        assertTrue(MiscTabOptions.applyWebChange(config, "gamma", DoubleNode.valueOf(2.2)));
        assertEquals(2.2, config.getGamma());
        assertTrue(MiscTabOptions.applyWebChange(config, "audioLoopbackGain", DoubleNode.valueOf(1.5)));
        assertEquals(1.5f, config.getAudioLoopbackGain());
        assertThrows(IllegalArgumentException.class, () ->
                MiscTabOptions.applyWebChange(config, "desiredFramerate", TextNode.valueOf("invalid")));
    }
}
