/*
  LEDCoordinate.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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
package org.dpsoftware;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Constants;
import org.dpsoftware.utilities.CommonUtility;

import java.util.LinkedHashMap;


/**
 * X Y coordinate for LEDs, generate fullscreen, letterbox and pillarbox matrix.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Slf4j
@SuppressWarnings("ALL")
public class LEDCoordinate {

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean groupedLed;

    /**
     * Init FullScreen LED Matrix with a default general purpose config
     * @return LED Matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initFullScreenLedMatrix(int screenWidth, int screenHeight, int bottomRightLed, int rightLed, int topLed, int leftLed,
                                                                         int bottomLeftLed, int bottomRowLed, String splitBottomRow, String grabberTopBottom, String grabberSide, String gapTypeTopBottom, String gapTypeSide) {
        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, screenWidth, screenHeight, bottomRightLed, rightLed, topLed, leftLed, bottomLeftLed, bottomRowLed, splitBottomRow, Constants.AspectRatio.FULLSCREEN,
                grabberTopBottom, grabberSide, gapTypeTopBottom, gapTypeSide);
        return defaultLedMatrix;
    }

    /**
     * Init Letterbox LED Matrix with a default general purpose config
     * @return LED letterbox matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initLetterboxLedMatrix(int screenWidth, int screenHeight, int bottomRightLed, int rightLed, int topLed, int leftLed, int bottomLeftLed,
                                                                        int bottomRowLed, String splitBottomRow, String grabberTopBottom, String grabberSide, String gapTypeTopBottom, String gapTypeSide) {
        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, screenWidth, screenHeight, bottomRightLed, rightLed, topLed, leftLed, bottomLeftLed, bottomRowLed, splitBottomRow, Constants.AspectRatio.LETTERBOX,
                grabberTopBottom, grabberSide, gapTypeTopBottom, gapTypeSide);
        return defaultLedMatrix;
    }

    /**
     * Init Pillarbox LED Matrix with a default general purpose config
     * @return LED letterbox matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initPillarboxMatrix(int screenWidth, int screenHeight, int bottomRightLed, int rightLed, int topLed, int leftLed, int bottomLeftLed,
                                                                     int bottomRowLed, String splitBottomRow, String grabberTopBottom, String grabberSide, String gapTypeTopBottom, String gapTypeSide) {
        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, screenWidth, screenHeight, bottomRightLed, rightLed, topLed,
                leftLed, bottomLeftLed, bottomRowLed, splitBottomRow, Constants.AspectRatio.PILLARBOX, grabberTopBottom, grabberSide, gapTypeTopBottom, gapTypeSide);
        return defaultLedMatrix;
    }

    /**
     * Calculate borders for fit to screen, 4:3, 16:9, 21:9, 32:9
     * @param screenWidth  screen width
     * @param screenHeight screen height
     */
    int calculateBorders(int screenWidth, int screenHeight) {
        double aspectRatio = Math.round(((double) screenWidth / (double) screenHeight) * 10) / 10.00; // Round aspect ratio to 2 decimals
        if (aspectRatio >= 1.2 && aspectRatio <= 1.4) { // standard 4:3
            return 0;
        } else if (aspectRatio >= 1.6 && aspectRatio <= 1.8) { // widescreen 16:9
            return ((screenWidth * 480) / 3840) + 100;
        } else if (aspectRatio >= 2.1 && aspectRatio <= 2.5) {
            return ((screenWidth * 440) / 3440) + 100; // ultra wide screen 21:9
        } else if (aspectRatio > 2.5 && aspectRatio <= 3.7) {
            return ((screenWidth * 960) / 3840) + 100; // ultra wide screen 32:9
        } else {
            return 0;
        }
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    void initializeLedMatrix(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, int width, int height, int bottomRightLed, int rightLed,
                             int topLed, int leftLed, int bottomLeftLed, int bottomRowLed, String splitBottomRow, Constants.AspectRatio aspectRatio,
                             String grabberTopBottom, String grabberSide, String gapTypeTopBottom, String gapTypeSide) {

        int bottomRightLedOriginal = bottomRightLed;
        int rightLedOriginal = rightLed;
        int topLedOriginal = topLed;
        int leftLedOriginal = leftLed;
        int bottomLeftLedOriginal = bottomLeftLed;
        int bottomRowLedOriginal = bottomRowLed;
        int groupBy = 3;

        bottomRightLed = (int) Math.ceil(bottomRightLed / groupBy);
        rightLed = (int) Math.ceil(rightLed / groupBy);
        topLed = (int) Math.ceil(topLed / groupBy);
        leftLed = (int) Math.ceil(leftLed / groupBy);
        bottomLeftLed = (int) Math.ceil(bottomLeftLed / groupBy);
        bottomRowLed = (int) Math.ceil(bottomRowLed / groupBy);

        var ledNum = 0;
        int letterboxBorder = 0, pillarboxBorder = 0;
        if (aspectRatio == Constants.AspectRatio.LETTERBOX) {
            letterboxBorder = height / Constants.LETTERBOX_RATIO;
        } else if (aspectRatio == Constants.AspectRatio.PILLARBOX) {
            pillarboxBorder = calculateBorders(width, height);
        }
        width = width - (pillarboxBorder * 2);
        height = height - (letterboxBorder * 2);
        int topBottomAreaHeight = (height * Integer.parseInt(grabberTopBottom.replace(Constants.PERCENT, ""))) / 100;
        int sideAreaWidth = (width * Integer.parseInt(grabberSide.replace(Constants.PERCENT, ""))) / 100;
        var splitBottomMargin = (width * Integer.parseInt(splitBottomRow.replace(Constants.PERCENT, ""))) / 100;
        int cornerGapTopBottom = (height * Integer.parseInt(gapTypeTopBottom.replace(Constants.PERCENT, ""))) / 100;
        int cornerGapSide = (width * Integer.parseInt(gapTypeSide.replace(Constants.PERCENT, ""))) / 100;
        int bottomLedDistance = (((width - (cornerGapSide * 2)) - splitBottomMargin) / 2) / bottomRightLed;
        if (CommonUtility.isSplitBottomRow(splitBottomRow)) {
            ledNum = bottomRightLed(defaultLedMatrix, width, height, bottomRightLed, bottomRightLedOriginal, groupBy, ledNum, letterboxBorder, pillarboxBorder, topBottomAreaHeight, cornerGapSide, bottomLedDistance);
        } else {
            ledNum = bottomLed(defaultLedMatrix, height, bottomRowLed, bottomRowLedOriginal, groupBy, ledNum, letterboxBorder, pillarboxBorder, topBottomAreaHeight, cornerGapSide, bottomLedDistance);
        }
        ledNum = rightLed(defaultLedMatrix, width, height, rightLed, rightLedOriginal, groupBy, ledNum, letterboxBorder, pillarboxBorder, sideAreaWidth, cornerGapTopBottom);
        ledNum = topLed(defaultLedMatrix, width, topLed, topLedOriginal, groupBy, ledNum, letterboxBorder, pillarboxBorder, topBottomAreaHeight, cornerGapSide);
        ledNum = leftLed(defaultLedMatrix, width, height, leftLed, leftLedOriginal, groupBy, ledNum, letterboxBorder, pillarboxBorder, sideAreaWidth, cornerGapTopBottom);
        bottomLeft(defaultLedMatrix, width, height, bottomLeftLed, splitBottomRow, bottomLeftLedOriginal, groupBy, ledNum, letterboxBorder, pillarboxBorder, topBottomAreaHeight, splitBottomMargin, cornerGapSide);
    }

    private void bottomLeft(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, int width, int height, int bottomLeftLed, String splitBottomRow, int bottomLeftLedOriginal, int groupBy, int ledNum, int letterboxBorder, int pillarboxBorder, int topBottomAreaHeight, int splitBottomMargin, int cornerGapSide) {
        if (CommonUtility.isSplitBottomRow(splitBottomRow)) {
            // bottomLeft LED strip
            if (bottomLeftLed > 0) {
                int ledInsertionNumber = 0;
                int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
                var bottomLedLeftDistance = (((width - (cornerGapSide * 2)) - splitBottomMargin) / 2) / bottomLeftLed;
                for (int i = 1; i <= bottomLeftLed; i++) {
                    x = (((bottomLedLeftDistance * i) - bottomLedLeftDistance) + cornerGapSide) + pillarboxBorder;
                    y = (height - topBottomAreaHeight) + letterboxBorder;
                    taleWidth = bottomLedLeftDistance;
                    taleHeight = topBottomAreaHeight;
                    for (int groupIndex = 0; groupIndex < groupBy; groupIndex++) {
                        defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                        ledInsertionNumber++;
                    }
                }
                while (ledInsertionNumber < bottomLeftLedOriginal) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                    ledInsertionNumber++;
                }
            }
        }
    }

    private int leftLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, int width, int height, int leftLed, int leftLedOriginal, int groupBy, int ledNum, int letterboxBorder, int pillarboxBorder, int sideAreaWidth, int cornerGapTopBottom) {
        // left LED strip
        if (leftLed > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            var leftLedDistance = (height - (cornerGapTopBottom * 2)) / leftLed;
            for (int i = leftLed; i >= 1; i--) {
                for (int groupIndex = 0; groupIndex < groupBy; groupIndex++) {
                    x = pillarboxBorder;
                    y = Math.max(0, (((height - (leftLedDistance * i)) - cornerGapTopBottom) + letterboxBorder) - calculateTaleBorder(width * 2));
                    taleWidth = sideAreaWidth;
                    taleHeight = leftLedDistance;
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < leftLedOriginal) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    private int topLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, int width, int topLed, int topLedOriginal, int groupBy, int ledNum, int letterboxBorder, int pillarboxBorder, int topBottomAreaHeight, int cornerGapSide) {
        // top LED strip
        if (topLed > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            var topLedDistance = (width - (cornerGapSide * 2)) / topLed;
            for (int i = 1; i <= topLed; i++) {
                x = ((width - (topLedDistance * i)) - cornerGapSide) + pillarboxBorder;
                y = letterboxBorder;
                taleWidth = topLedDistance;
                taleHeight = topBottomAreaHeight;
                for (int groupIndex = 0; groupIndex < groupBy; groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < topLedOriginal) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    private int rightLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, int width, int height, int rightLed, int rightLedOriginal, int groupBy, int ledNum, int letterboxBorder, int pillarboxBorder, int sideAreaWidth, int cornerGapTopBottom) {
        // right LED strip
        if (rightLed > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            var rightLedDistance = (height - (cornerGapTopBottom * 2)) / rightLed;
            for (int i = 1; i <= rightLed; i++) {
                x = (width - sideAreaWidth) + pillarboxBorder;
                y = Math.max(0, (((height - (rightLedDistance * i)) - cornerGapTopBottom) + letterboxBorder) - calculateTaleBorder(width * 2));
                taleWidth = sideAreaWidth;
                taleHeight = rightLedDistance;
                for (int groupIndex = 0; groupIndex < groupBy; groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < rightLedOriginal) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    private int bottomLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, int height, int bottomRowLed, int bottomRowLedOriginal, int groupBy, int ledNum, int letterboxBorder, int pillarboxBorder, int topBottomAreaHeight, int cornerGapSide, int bottomLedDistance) {
        // bottom LED strip
        if (bottomRowLed > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            for (int i = 1; i <= bottomRowLed; i++) {
                x = (((bottomLedDistance * i) - bottomLedDistance) + cornerGapSide) + pillarboxBorder;
                y = (height - topBottomAreaHeight) + letterboxBorder;
                taleWidth = bottomLedDistance;
                taleHeight = topBottomAreaHeight;
                for (int groupIndex = 0; groupIndex < groupBy; groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < bottomRowLedOriginal) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    private int bottomRightLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, int width, int height, int bottomRightLed, int bottomRightLedOriginal, int groupBy, int ledNum, int letterboxBorder, int pillarboxBorder, int topBottomAreaHeight, int cornerGapSide, int bottomLedDistance) {
        // bottomRight LED strip
        if (bottomRightLed > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            for (int i = bottomRightLed; i > 0; i--) {
                x = width - (bottomLedDistance * i) - cornerGapSide + pillarboxBorder;
                y = (height - topBottomAreaHeight) + letterboxBorder;
                taleWidth = bottomLedDistance;
                taleHeight = topBottomAreaHeight;
                for (int groupIndex = 0; groupIndex < groupBy; groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < bottomRightLedOriginal) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * Calculate tale border size
     *
     * @param width screen width
     * @return tale border size
     */
    public static int calculateTaleBorder(int width) {
        return (Constants.TEST_CANVAS_BORDER_RATIO * width) / 3840;
    }
}