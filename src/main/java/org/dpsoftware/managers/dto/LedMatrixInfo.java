/*
  LedMatrixInfo.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.managers.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Information used to create LED Matrixes
 */
@Getter
@Setter
@NoArgsConstructor
public class LedMatrixInfo implements Cloneable {

    int bottomRightLedOriginal;
    int rightLedOriginal;
    int topLedOriginal;
    int leftLedOriginal;
    int bottomLeftLedOriginal;
    int bottomRowLedOriginal;
    int screenWidth;
    int screenHeight;
    int bottomRightLed;
    int rightLed;
    int topLed;
    int leftLed;
    int bottomLeftLed;
    int bottomRowLed;
    String splitBottomRow;
    String grabberTopBottom;
    String grabberSide;
    String gapTypeTopBottom;
    String gapTypeSide;
    int topBottomAreaHeight;
    int sideAreaWidth;
    int splitBottomMargin;
    int cornerGapTopBottom;
    int cornerGapSide;
    int bottomLedDistance;
    int groupBy;
    int letterboxBorder;
    int pillarboxBorder;
    int minimumNumberOfLedsInARow;
    int totaleNumOfLeds;

    public LedMatrixInfo(int screenWidth, int screenHeight, int bottomRightLed, int rightLed, int topLed, int leftLed, int bottomLeftLed,
                         int bottomRowLed, String splitBottomRow, String grabberTopBottom, String grabberSide,
                         String gapTypeTopBottom, String gapTypeSide, int groupBy) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.bottomRightLed = bottomRightLed;
        this.rightLed = rightLed;
        this.topLed = topLed;
        this.leftLed = leftLed;
        this.bottomLeftLed = bottomLeftLed;
        this.bottomRowLed = bottomRowLed;
        this.splitBottomRow = splitBottomRow;
        this.grabberTopBottom = grabberTopBottom;
        this.grabberSide = grabberSide;
        this.gapTypeTopBottom = gapTypeTopBottom;
        this.gapTypeSide = gapTypeSide;
        this.groupBy = groupBy;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
