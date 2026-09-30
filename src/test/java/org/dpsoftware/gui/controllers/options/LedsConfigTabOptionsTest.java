/*
  LedsConfigTabOptionsTest.java

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the bottom LED field layout shared by JavaFX and the web page.
 */
class LedsConfigTabOptionsTest {

    /**
     * Checks zero margin selects the single bottom row and positive margins select the split row.
     */
    @Test
    void selectsBottomLedLayout() {
        var layouts = LedsConfigTabOptions.bottomRowLayouts();
        assertEquals(96, layouts.size());
        assertFalse(LedsConfigTabOptions.isBottomRowSplit("0%"));
        assertFalse(layouts.get("0%"));
        assertTrue(LedsConfigTabOptions.isBottomRowSplit("15%"));
        assertTrue(layouts.get("15%"));
        assertTrue(layouts.get("95%"));
    }
}
