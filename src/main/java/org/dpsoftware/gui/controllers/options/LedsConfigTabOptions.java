/*
  EyeCareOptions.java

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
import org.dpsoftware.utilities.CommonUtility;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * LED layout choices shared by the JavaFX tab and the web settings page.
 */
public final class LedsConfigTabOptions {

    /**
     * Prevents instantiation.
     */
    private LedsConfigTabOptions() {
    }

    /**
     * Determines whether the bottom row uses separate left and right LED counts.
     *
     * @param splitBottomMargin selected lower margin percentage
     * @return true when the bottom row is split
     */
    public static boolean isBottomRowSplit(String splitBottomMargin) {
        return CommonUtility.isSplitBottomRow(splitBottomMargin);
    }

    /**
     * Provides the bottom-row layout for every lower margin offered by the web select.
     *
     * @return margin-to-layout map for percentages from zero through ninety-five
     */
    public static Map<String, Boolean> bottomRowLayouts() {
        Map<String, Boolean> layouts = new LinkedHashMap<>();
        IntStream.rangeClosed(0, 95).forEach(value -> {
            String margin = value + Constants.PERCENT;
            layouts.put(margin, isBottomRowSplit(margin));
        });
        return layouts;
    }
}
