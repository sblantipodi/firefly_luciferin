/*
  TcSnappingHelper.java

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
package org.dpsoftware.gui.tc;

import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

import static org.dpsoftware.utilities.CommonUtility.scaleDownResolution;

/**
 * Pure utility for snap-point calculations when dragging LED tiles.
 * All methods are static and stateless, making them safe to call from any handler.
 */
public final class TcSnappingHelper {

    /**
     * Calculates the snapped position for a tile by comparing against all
     * non-selected LEDs in the current LED matrix.
     *
     * @param conf          current configuration
     * @param x             proposed X of the dragged tile (already scaled down)
     * @param y             proposed Y of the dragged tile (already scaled down)
     * @param w             width of the dragged tile (already scaled down)
     * @param h             height of the dragged tile (already scaled down)
     * @param snapThreshold maximum distance for snapping to occur
     * @param selectedLeds  set of currently selected LEDs (excluded from snap targets)
     * @return snapped point in canvas-coordinates
     */
    public static Point calculateSnappedPosition(Configuration conf, int x, int y, int w, int h,
                                                 int snapThreshold, Set<LEDCoordinate> selectedLeds) {
        int snappedX = x;
        int snappedY = y;
        LinkedHashMap<Integer, LEDCoordinate> ledMatrix = conf.getLedMatrixInUse(
                Objects.requireNonNullElse(MainSingleton.getInstance().config, conf).getDefaultLedMatrix());
        for (LEDCoordinate other : ledMatrix.values()) {
            if (selectedLeds.contains(other)) continue;
            int otherX = scaleDownResolution(other.getX(), conf.getOsScaling());
            int otherY = scaleDownResolution(other.getY(), conf.getOsScaling());
            int otherW = scaleDownResolution(other.getWidth(), conf.getOsScaling());
            int otherH = scaleDownResolution(other.getHeight(), conf.getOsScaling());
            snappedX = snapValue(snappedX, w, otherX, otherW, snapThreshold);
            snappedY = snapValue(snappedY, h, otherY, otherH, snapThreshold);
        }
        return new Point(snappedX, snappedY);
    }

    /**
     * Calculates a snapped position for a UI element based on proximity to another element.
     * Snapping occurs to edges, centers, or aligned positions if within the given threshold.
     *
     * @param pos       The position of the current element.
     * @param size      The size of the current element.
     * @param otherPos  The position of the other element.
     * @param otherSize The size of the other element.
     * @param threshold The maximum distance for snapping to occur.
     * @return The new snapped position if within threshold, otherwise the original position.
     */
    public static int snapValue(int pos, int size, int otherPos, int otherSize, int threshold) {
        // edges
        if (Math.abs(pos - (otherPos + otherSize)) <= threshold) return otherPos + otherSize;
        if (Math.abs(pos + size - otherPos) <= threshold) return otherPos - size;
        // centers
        if (Math.abs(pos + size / 2 - (otherPos + otherSize / 2)) <= threshold)
            return (otherPos + otherSize / 2) - size / 2;
        // same-level snap
        if (Math.abs(pos - otherPos) <= threshold) return otherPos;
        if (Math.abs(pos + size - (otherPos + otherSize)) <= threshold) return otherPos + otherSize - size;
        return pos;
    }
}
