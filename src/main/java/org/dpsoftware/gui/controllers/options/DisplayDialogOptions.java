/*
  DisplayDialogOptions.java

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

import org.dpsoftware.config.Constants;
import org.dpsoftware.lut.CubeLutToneMap;

import java.util.List;

/**
 * Choices and value conversion shared by the JavaFX and web tone mapping controls.
 */
public final class DisplayDialogOptions {

    /**
     * Prevents instantiation.
     */
    private DisplayDialogOptions() {
    }

    /**
     * Returns the built-in and user-provided LUT names shown by the editable control.
     *
     * @return available LUT names
     */
    public static List<String> availableLuts() {
        return CubeLutToneMap.listAvailableLuts();
    }

    /**
     * Converts an empty selection to the stored disabled value.
     *
     * @param selection selected or manually entered LUT name
     * @return trimmed LUT name or the disabled value
     */
    public static String selectedLut(String selection) {
        return selection == null || selection.isBlank() ? Constants.DISABLED : selection.trim();
    }
}
