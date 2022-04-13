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
import org.dpsoftware.config.Constants;
import org.dpsoftware.utilities.CommonUtility;

import java.util.LinkedHashMap;


/**
 * X Y coordinate for LEDs
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LEDCoordinate {

    private int x;
    private int y;
    private int width;
    private int height;

    /**
     * Init FullScreen LED Matrix with a default general purpose config
     *
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
     *
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
     *
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
     *
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

    void initializeLedMatrix(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, int width, int height, int bottomRightLed, int rightLed,
                             int topLed, int leftLed, int bottomLeftLed, int bottomRowLed, String splitBottomRow, Constants.AspectRatio aspectRatio,
                             String grabberTopBottom, String grabberSide, String gapTypeTopBottom, String gapTypeSide) {

//        bottomRightLed = (int) Math.ceil(bottomRightLed / 2);
//        rightLed = (int) Math.ceil(rightLed / 2);
//        topLed = (int) Math.ceil(topLed / 2);
//        leftLed = (int) Math.ceil(leftLed / 2);
//        bottomLeftLed = (int) Math.ceil(bottomLeftLed / 2);
//        bottomRowLed = (int) Math.ceil(bottomRowLed / 2);

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
            // bottomRight LED strip
            if (bottomRightLed > 0) {
                for (int i = bottomRightLed; i > 0; i--) {
                    ledNum++;
                    int x = width - (bottomLedDistance * i) - cornerGapSide + pillarboxBorder;
                    defaultLedMatrix.put(ledNum, new LEDCoordinate(x, (height - (topBottomAreaHeight)) + letterboxBorder, bottomLedDistance, topBottomAreaHeight));
                }
            }
        } else {
            // bottom LED strip
            if (bottomRowLed > 0) {
                for (int i = 1; i <= bottomRowLed; i++) {
                    ledNum++;
                    int x = (((bottomLedDistance * i) - bottomLedDistance) + cornerGapSide);
                    defaultLedMatrix.put(ledNum, new LEDCoordinate(x + pillarboxBorder, (height - topBottomAreaHeight) + letterboxBorder,
                            bottomLedDistance, topBottomAreaHeight));
                }
            }
        }
        // right LED strip
        if (rightLed > 0) {
            var rightLedDistance = (height - (cornerGapTopBottom * 2)) / rightLed;
            for (int i = 1; i <= rightLed; i++) {
                ledNum++;
                int y = (((height - (rightLedDistance * i)) - cornerGapTopBottom) + letterboxBorder) - calculateTaleBorder(width * 2);
                defaultLedMatrix.put(ledNum, new LEDCoordinate((width - sideAreaWidth) + pillarboxBorder, Math.max(0, y), sideAreaWidth, rightLedDistance));
            }
        }
        // top LED strip
        if (topLed > 0) {
            var topLedDistance = (width - (cornerGapSide * 2)) / topLed;
            for (int i = 1; i <= topLed; i++) {
                ledNum++;
                int x = ((width - (topLedDistance * i)) - cornerGapSide);
                //noinspection SuspiciousNameCombination
                defaultLedMatrix.put(ledNum, new LEDCoordinate(x + pillarboxBorder, letterboxBorder, topLedDistance, topBottomAreaHeight));
            }
        }
        // left LED strip
        if (leftLed > 0) {
            var leftLedDistance = (height - (cornerGapTopBottom * 2)) / leftLed;
            for (int i = leftLed; i >= 1; i--) {
                ledNum++;
                int y = (((height - (leftLedDistance * i)) - cornerGapTopBottom) + letterboxBorder) - calculateTaleBorder(width * 2);
                defaultLedMatrix.put(ledNum, new LEDCoordinate(pillarboxBorder, Math.max(0, y), sideAreaWidth, leftLedDistance));
            }
        }
        if (CommonUtility.isSplitBottomRow(splitBottomRow)) {
            // bottomLeft LED strip
            if (bottomLeftLed > 0) {
                var bottomLedLeftDistance = (((width - (cornerGapSide * 2)) - splitBottomMargin) / 2) / bottomLeftLed;
                for (int i = 1; i <= bottomLeftLed; i++) {
                    ledNum++;
                    int x = (((bottomLedLeftDistance * i) - bottomLedLeftDistance) + cornerGapSide) + pillarboxBorder;
                    defaultLedMatrix.put(ledNum, new LEDCoordinate(x, (height - topBottomAreaHeight) + letterboxBorder, bottomLedLeftDistance, topBottomAreaHeight));
                }
            }
        }
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