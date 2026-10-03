/*
  appliesAdaptiveGammaSettings.java

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

import static org.junit.jupiter.api.Assertions.*;

class GammaOptionsTest {

    /**
     * Checks that the dialog and web controls store the same level and checkbox state.
     */
    @Test
    void appliesAdaptiveGammaSettings() {
        Configuration config = new Configuration();
        GammaOptions.apply(config, true, Enums.GammaLevel.HIGH.getI18n());
        assertTrue(config.isEnableAutomaticGamma());
        assertEquals(Enums.GammaLevel.HIGH.getBaseI18n(), config.getGammaLevel());
        assertTrue(GammaOptions.isLevelEditable(config.isEnableAutomaticGamma()));

        GammaOptions.apply(config, false, Enums.GammaLevel.HIGH.getBaseI18n());
        assertFalse(config.isEnableAutomaticGamma());
        assertFalse(GammaOptions.isLevelEditable(config.isEnableAutomaticGamma()));
        assertEquals(Enums.GammaLevel.HIGH.getBaseI18n(), config.getGammaLevel());
        assertThrows(IllegalArgumentException.class, () -> GammaOptions.apply(config, true, "unknown"));
    }
}
