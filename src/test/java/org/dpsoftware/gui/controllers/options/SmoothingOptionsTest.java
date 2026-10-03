/*
  SmoothingOptionsTest.java

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

import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmoothingOptionsTest {

    /**
     * Checks that a preset and the dialog controls produce the same stored settings.
     */
    @Test
    void presetAndControlsStayInSync() {
        Configuration config = new Configuration();
        SmoothingOptions.applyPreset(config, Enums.Smoothing.SMOOTHING_LVL_3.getBaseI18n());
        assertEquals(0.20f, config.getEmaAlpha());
        assertEquals(30, config.getFrameInsertionTarget());
        assertEquals("30 FPS", SmoothingOptions.captureFramerateLabel(config));
        SmoothingOptions.applyControls(config, 0.20f, 30, 60);
        assertEquals(Enums.Smoothing.SMOOTHING_LVL_3.getBaseI18n(), config.getSmoothingType());
        SmoothingOptions.applyControls(config, 0.25f, 30, 120);
        assertEquals(Enums.Smoothing.CUSTOM.getBaseI18n(), config.getSmoothingType());
        assertEquals("60 FPS", SmoothingOptions.captureFramerateLabel(config));
        assertThrows(IllegalArgumentException.class, () -> SmoothingOptions.applyControls(config, 0.99f, 30, 60));
    }
}
