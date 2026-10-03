/*
  GammaOptions.java

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
import org.dpsoftware.config.LocalizedEnum;

import java.util.List;

/**
 * Values and settings shared by the JavaFX and web adaptive gamma controls.
 */
public final class GammaOptions {

    /**
     * Prevents instantiation.
     */
    private GammaOptions() {
    }

    /**
     * Returns the adaptive gamma brightness levels in dialog order.
     *
     * @return available levels
     */
    public static List<Enums.GammaLevel> levels() {
        return List.of(Enums.GammaLevel.values());
    }

    /**
     * Indicates whether the level selector can be edited.
     *
     * @param automaticGamma whether adaptive gamma is enabled
     * @return true when the level selector is enabled
     */
    public static boolean isLevelEditable(boolean automaticGamma) {
        return automaticGamma;
    }

    /**
     * Stores the adaptive gamma state and the selected level using the base language value.
     *
     * @param config         configuration to update
     * @param automaticGamma whether adaptive gamma is enabled
     * @param level          stored or localized brightness level
     */
    public static void apply(Configuration config, boolean automaticGamma, String level) {
        Enums.GammaLevel selected = LocalizedEnum.fromBaseStr(Enums.GammaLevel.class, level);
        if (selected == null) {
            selected = LocalizedEnum.fromStr(Enums.GammaLevel.class, level);
        }
        if (selected == null) {
            throw new IllegalArgumentException("Invalid adaptive gamma level");
        }
        config.setEnableAutomaticGamma(automaticGamma);
        config.setGammaLevel(selected.getBaseI18n());
    }
}
