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

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmoothingOptionsTest {

    @Test
    void russianStatusIsCompactWithoutChangingPresetOptions() {
        ResourceBundle previousBundle = MainSingleton.getInstance().bundle;
        try {
            MainSingleton.getInstance().bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, Locale.forLanguageTag("ru"));
            Configuration config = new Configuration();
            List<Enums.Smoothing> levels = List.of(Enums.Smoothing.SMOOTHING_LVL_1, Enums.Smoothing.SMOOTHING_LVL_2,
                    Enums.Smoothing.SMOOTHING_LVL_3, Enums.Smoothing.SMOOTHING_LVL_4,
                    Enums.Smoothing.SMOOTHING_LVL_5, Enums.Smoothing.SMOOTHING_LVL_6);
            for (int i = 0; i < levels.size(); i++) {
                Enums.Smoothing level = levels.get(i);
                SmoothingOptions.applyPreset(config, level.getBaseI18n());
                assertEquals("Сглаживание " + (i + 1), SmoothingOptions.statusLabel(config));
                assertEquals("Уровень сглаживания " + (i + 1), level.getI18n());
                assertEquals(level.getBaseI18n(), config.getSmoothingType());
            }
            SmoothingOptions.applyPreset(config, Enums.Smoothing.DISABLED.getBaseI18n());
            assertEquals("", SmoothingOptions.statusLabel(config));
        } finally {
            MainSingleton.getInstance().bundle = previousBundle;
        }
    }

    @Test
    void statusFallsBackToFullLabelWhenNoCompactTranslationExists() {
        ResourceBundle previousBundle = MainSingleton.getInstance().bundle;
        try {
            Configuration config = new Configuration();
            SmoothingOptions.applyPreset(config, Enums.Smoothing.SMOOTHING_LVL_3.getBaseI18n());
            for (String language : List.of("en", "it", "de", "es", "fr", "hu", "pl")) {
                MainSingleton.getInstance().bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, Locale.forLanguageTag(language));
                assertEquals(Enums.Smoothing.SMOOTHING_LVL_3.getI18n(), SmoothingOptions.statusLabel(config));
            }
            MainSingleton.getInstance().bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, Locale.forLanguageTag("ru"));
            SmoothingOptions.applyPreset(config, Enums.Smoothing.CUSTOM.getBaseI18n());
            assertEquals(Enums.Smoothing.CUSTOM.getI18n(), SmoothingOptions.statusLabel(config));
        } finally {
            MainSingleton.getInstance().bundle = previousBundle;
        }
    }

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
