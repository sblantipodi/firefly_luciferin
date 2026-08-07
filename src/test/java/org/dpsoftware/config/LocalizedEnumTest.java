/*
  LocalizedEnumTest.java

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
package org.dpsoftware.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LocalizedEnum} lookup methods.
 */
class LocalizedEnumTest {

    // --- fromBaseStr ---

    @Test
    void fromBaseStr_findByExactMatch() {
        Enums.Theme result = LocalizedEnum.fromBaseStr(Enums.Theme.class, "Classic");
        assertEquals(Enums.Theme.CLASSIC, result);
    }

    @Test
    void fromBaseStr_caseInsensitive() {
        Enums.Theme result = LocalizedEnum.fromBaseStr(Enums.Theme.class, "classic");
        assertEquals(Enums.Theme.CLASSIC, result);
    }

    @Test
    void fromBaseStr_uppercaseMatch() {
        Enums.Theme result = LocalizedEnum.fromBaseStr(Enums.Theme.class, "CLASSIC");
        assertEquals(Enums.Theme.CLASSIC, result);
    }

    @Test
    void fromBaseStr_nonExistentReturnsNull() {
        Enums.Theme result = LocalizedEnum.fromBaseStr(Enums.Theme.class, "non-existent theme");
        assertNull(result);
    }

    @Test
    void fromBaseStr_emptyStringReturnsNull() {
        Enums.Theme result = LocalizedEnum.fromBaseStr(Enums.Theme.class, "");
        assertNull(result);
    }

    @Test
    void fromBaseStr_nullInputThrowsNpe() {
        // Production code does not guard against null — delegates to String.equalsIgnoreCase
        assertThrows(NullPointerException.class, () ->
                LocalizedEnum.fromBaseStr(Enums.Theme.class, null));
    }

    @Test
    void fromBaseStr_darkThemeCyan() {
        Enums.Theme result = LocalizedEnum.fromBaseStr(Enums.Theme.class, "Dark cyan");
        assertEquals(Enums.Theme.DARK_THEME_CYAN, result);
    }

    // --- fromStr (baseValue = true via fromBaseStr equivalent) ---

    @Test
    void fromStr_baseValueTrue() {
        Enums.Theme result = LocalizedEnum.fromStr(Enums.Theme.class, "Dark blue", true);
        assertEquals(Enums.Theme.DARK_BLUE_THEME, result);
    }

    // --- fromStr (baseValue = false, uses current locale i18n) ---

    @Test
    void fromStr_baseValueFalseUsesCurrentLocale() {
        // getI18n() returns the translation for the current locale
        String localizedLabel = Enums.Theme.CLASSIC.getI18n();
        Enums.Theme result = LocalizedEnum.fromStr(Enums.Theme.class, localizedLabel, false);
        assertEquals(Enums.Theme.CLASSIC, result);
    }

    // --- Gamma enum is NOT a LocalizedEnum — skip gamma lookups here ---

    // --- Effect enum lookups ---

    @Test
    void fromBaseStr_effectSolid() {
        Enums.Effect result = LocalizedEnum.fromBaseStr(Enums.Effect.class, "Solid");
        assertEquals(Enums.Effect.SOLID, result);
    }

    @Test
    void fromBaseStr_effectRainbow() {
        Enums.Effect result = LocalizedEnum.fromBaseStr(Enums.Effect.class, "Rainbow");
        assertEquals(Enums.Effect.RAINBOW, result);
    }

    // --- Theme iteration coverage ---

    @Test
    void fromBaseStr_darkThemeOrange() {
        Enums.Theme result = LocalizedEnum.fromBaseStr(Enums.Theme.class, "Dark orange");
        assertEquals(Enums.Theme.DARK_THEME_ORANGE, result);
    }

    @Test
    void fromBaseStr_darkThemePurple() {
        Enums.Theme result = LocalizedEnum.fromBaseStr(Enums.Theme.class, "Dark purple");
        assertEquals(Enums.Theme.DARK_THEME_PURPLE, result);
    }

    // --- getValue and getBaseI18n contract ---

    @Test
    void getValueReturnsNonEmpty() {
        assertFalse(Enums.Theme.CLASSIC.getValue().isEmpty());
    }

    @Test
    void getBaseI18nReturnsNonEmpty() {
        assertFalse(Enums.Theme.CLASSIC.getBaseI18n().isEmpty());
    }
}
