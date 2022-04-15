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
import org.dpsoftware.managers.dto.LedMatrixInfo;
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
public class LEDCoordinate {

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean groupedLed;

    /**
     * Init FullScreen LED Matrix with a default general purpose config
     * @param ledMatrixInfo required infos to create LED Matrix
     * @return LED Matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initFullScreenLedMatrix(LedMatrixInfo ledMatrixInfo) {
        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, Constants.AspectRatio.FULLSCREEN, ledMatrixInfo);
        return defaultLedMatrix;
    }

    /**
     * Init Letterbox LED Matrix with a default general purpose config
     * @param ledMatrixInfo required infos to create LED Matrix
     * @return LED letterbox matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initLetterboxLedMatrix(LedMatrixInfo ledMatrixInfo) {
        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, Constants.AspectRatio.LETTERBOX, ledMatrixInfo);
        return defaultLedMatrix;
    }

    /**
     * Init Pillarbox LED Matrix with a default general purpose config
     * @param ledMatrixInfo required infos to create LED Matrix
     * @return LED letterbox matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initPillarboxMatrix(LedMatrixInfo ledMatrixInfo) {
        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, Constants.AspectRatio.PILLARBOX, ledMatrixInfo);
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

    /**
     * Init LED Matrixes
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo infos used to create the LED matrix
     */
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    void initializeLedMatrix(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, Constants.AspectRatio aspectRatio, LedMatrixInfo ledMatrixInfo) {
        // Store original values before grouping them
        ledMatrixInfo.setBottomRightLedOriginal(ledMatrixInfo.getBottomRightLed());
        ledMatrixInfo.setRightLedOriginal(ledMatrixInfo.getRightLed());
        ledMatrixInfo.setTopLedOriginal(ledMatrixInfo.getTopLed());
        ledMatrixInfo.setLeftLedOriginal(ledMatrixInfo.getLeftLed());
        ledMatrixInfo.setBottomLeftLedOriginal(ledMatrixInfo.getBottomLeftLed());
        ledMatrixInfo.setBottomRowLedOriginal(ledMatrixInfo.getBottomRowLed());
        CommonUtility.groupByCalc(ledMatrixInfo);
        // Group default values
        ledMatrixInfo.setBottomRightLed((int) Math.ceil(ledMatrixInfo.getBottomRightLed() / ledMatrixInfo.getGroupBy()));
        ledMatrixInfo.setRightLed((int) Math.ceil(ledMatrixInfo.getRightLed() / ledMatrixInfo.getGroupBy()));
        ledMatrixInfo.setTopLed((int) Math.ceil(ledMatrixInfo.getTopLed() / ledMatrixInfo.getGroupBy()));
        ledMatrixInfo.setLeftLed((int) Math.ceil(ledMatrixInfo.getLeftLed() / ledMatrixInfo.getGroupBy()));
        ledMatrixInfo.setBottomLeftLed((int) Math.ceil(ledMatrixInfo.getBottomLeftLed() / ledMatrixInfo.getGroupBy()));
        ledMatrixInfo.setBottomRowLed((int) Math.ceil(ledMatrixInfo.getBottomRowLed() / ledMatrixInfo.getGroupBy()));
        // Aspect ratio
        var ledNum = 0;
        if (aspectRatio == Constants.AspectRatio.LETTERBOX) {
            ledMatrixInfo.setLetterboxBorder(ledMatrixInfo.getScreenHeight() / Constants.LETTERBOX_RATIO);
        } else if (aspectRatio == Constants.AspectRatio.PILLARBOX) {
            ledMatrixInfo.setPillarboxBorder(calculateBorders(ledMatrixInfo.getScreenWidth(), ledMatrixInfo.getScreenHeight()));
        }
        ledMatrixInfo.setScreenWidth(ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getPillarboxBorder() * 2));
        ledMatrixInfo.setScreenHeight(ledMatrixInfo.getScreenHeight() - (ledMatrixInfo.getLetterboxBorder() * 2));
        ledMatrixInfo.setTopBottomAreaHeight((ledMatrixInfo.getScreenHeight() * Integer.parseInt(ledMatrixInfo.getGrabberTopBottom().replace(Constants.PERCENT, ""))) / 100);
        ledMatrixInfo.setSideAreaWidth((ledMatrixInfo.getScreenWidth() * Integer.parseInt(ledMatrixInfo.getGrabberSide().replace(Constants.PERCENT, ""))) / 100);
        ledMatrixInfo.setSplitBottomMargin((ledMatrixInfo.getScreenWidth() * Integer.parseInt(ledMatrixInfo.getSplitBottomRow().replace(Constants.PERCENT, ""))) / 100);
        ledMatrixInfo.setCornerGapTopBottom((ledMatrixInfo.getScreenHeight() * Integer.parseInt(ledMatrixInfo.getGapTypeTopBottom().replace(Constants.PERCENT, ""))) / 100);
        ledMatrixInfo.setCornerGapSide((ledMatrixInfo.getScreenWidth() * Integer.parseInt(ledMatrixInfo.getGapTypeSide().replace(Constants.PERCENT, ""))) / 100);
        if (ledMatrixInfo.getBottomRightLed() > 0) {
            ledMatrixInfo.setBottomLedDistance((((ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getCornerGapSide() * 2)) - ledMatrixInfo.getSplitBottomMargin()) / 2) / ledMatrixInfo.getBottomRightLed());
        }
        if (CommonUtility.isSplitBottomRow(ledMatrixInfo.getSplitBottomRow())) {
            ledNum = bottomRightLed(defaultLedMatrix, ledMatrixInfo, ledNum);
        } else {
            ledNum = bottomLed(defaultLedMatrix, ledMatrixInfo, ledNum);
        }
        ledNum = rightLed(defaultLedMatrix, ledMatrixInfo, ledNum);
        ledNum = topLed(defaultLedMatrix, ledMatrixInfo, ledNum);
        ledNum = leftLed(defaultLedMatrix, ledMatrixInfo, ledNum);
        bottomLeft(defaultLedMatrix, ledMatrixInfo, ledNum);
    }

    /**
     * Init LEFT side LEDs
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo infos used to create the LED matrix
     * @param ledNum current LEDs
     * @return next LED to process
     */
    private int leftLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (ledMatrixInfo.getLeftLed() > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            var leftLedDistance = (ledMatrixInfo.getScreenHeight() - (ledMatrixInfo.getCornerGapTopBottom() * 2)) / ledMatrixInfo.getLeftLed();
            for (int i = ledMatrixInfo.getLeftLed(); i >= 1; i--) {
                for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                    x = ledMatrixInfo.getPillarboxBorder();
                    y = Math.max(0, (((ledMatrixInfo.getScreenHeight() - (leftLedDistance * i)) - ledMatrixInfo.getCornerGapTopBottom()) + ledMatrixInfo.getLetterboxBorder()) - calculateTaleBorder(ledMatrixInfo.getScreenWidth() * 2));
                    taleWidth = ledMatrixInfo.getSideAreaWidth();
                    taleHeight = leftLedDistance;
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getLeftLedOriginal()) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * Init BOTTOM LEFT LEDs
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo infos used to create the LED matrix
     * @param ledNum current LEDs
     */
    private void bottomLeft(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (CommonUtility.isSplitBottomRow(ledMatrixInfo.getSplitBottomRow())) {
            if (ledMatrixInfo.getBottomLeftLed() > 0) {
                int ledInsertionNumber = 0;
                int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
                var bottomLedLeftDistance = (((ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getCornerGapSide() * 2)) - ledMatrixInfo.getSplitBottomMargin()) / 2) / ledMatrixInfo.getBottomLeftLed();
                for (int i = 1; i <= ledMatrixInfo.getBottomLeftLed(); i++) {
                    x = (((bottomLedLeftDistance * i) - bottomLedLeftDistance) + ledMatrixInfo.getCornerGapSide()) + ledMatrixInfo.getPillarboxBorder();
                    y = (ledMatrixInfo.getScreenHeight() - ledMatrixInfo.getTopBottomAreaHeight()) + ledMatrixInfo.getLetterboxBorder();
                    taleWidth = bottomLedLeftDistance;
                    taleHeight = ledMatrixInfo.getTopBottomAreaHeight();
                    for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                        defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                        ledInsertionNumber++;
                    }
                }
                while (ledInsertionNumber < ledMatrixInfo.getBottomLeftLedOriginal()) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                    ledInsertionNumber++;
                }
            }
        }
    }

    /**
     * Init TOP LEDs
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo infos used to create the LED matrix
     * @param ledNum current LEDs
     * @return next LED to process
     */
    private int topLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (ledMatrixInfo.getTopLed() > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            var topLedDistance = (ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getCornerGapSide() * 2)) / ledMatrixInfo.getTopLed();
            for (int i = 1; i <= ledMatrixInfo.getTopLed(); i++) {
                x = ((ledMatrixInfo.getScreenWidth() - (topLedDistance * i)) - ledMatrixInfo.getCornerGapSide()) + ledMatrixInfo.getPillarboxBorder();
                y = ledMatrixInfo.getLetterboxBorder();
                taleWidth = topLedDistance;
                taleHeight = ledMatrixInfo.getTopBottomAreaHeight();
                for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getTopLedOriginal()) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * Init RIGHT side LEDs
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo infos used to create the LED matrix
     * @param ledNum current LEDs
     * @return next LED to process
     */
    private int rightLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (ledMatrixInfo.getRightLed() > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            var rightLedDistance = (ledMatrixInfo.getScreenHeight() - (ledMatrixInfo.getCornerGapTopBottom() * 2)) / ledMatrixInfo.getRightLed();
            for (int i = 1; i <= ledMatrixInfo.getRightLed(); i++) {
                x = (ledMatrixInfo.getScreenWidth() - ledMatrixInfo.getSideAreaWidth()) + ledMatrixInfo.getPillarboxBorder();
                y = Math.max(0, (((ledMatrixInfo.getScreenHeight() - (rightLedDistance * i)) - ledMatrixInfo.getCornerGapTopBottom()) + ledMatrixInfo.getLetterboxBorder()) - calculateTaleBorder(width * 2));
                taleWidth = ledMatrixInfo.getSideAreaWidth();
                taleHeight = rightLedDistance;
                for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getRightLedOriginal()) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * Init BOTTOM LEDs
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo infos used to create the LED matrix
     * @param ledNum current LEDs
     * @return next LED to process
     */
    private int bottomLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (ledMatrixInfo.getBottomRowLed() > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            for (int i = 1; i <= ledMatrixInfo.getBottomRowLed(); i++) {
                x = (((ledMatrixInfo.getBottomLedDistance() * i) - ledMatrixInfo.getBottomLedDistance()) + ledMatrixInfo.getCornerGapSide()) + ledMatrixInfo.getPillarboxBorder();
                y = (ledMatrixInfo.getScreenHeight() - ledMatrixInfo.getTopBottomAreaHeight()) + ledMatrixInfo.getLetterboxBorder();
                taleWidth = ledMatrixInfo.getBottomLedDistance();
                taleHeight = ledMatrixInfo.getTopBottomAreaHeight();
                for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getBottomRowLedOriginal()) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * Init BOTTOM RIGHT LEDs
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo infos used to create the LED matrix
     * @param ledNum current LEDs
     * @return next LED to process
     */
    private int bottomRightLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (ledMatrixInfo.getBottomRightLed() > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            for (int i = ledMatrixInfo.getBottomRightLed(); i > 0; i--) {
                x = ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getBottomLedDistance() * i) - ledMatrixInfo.getCornerGapSide() + ledMatrixInfo.getPillarboxBorder();
                y = (ledMatrixInfo.getScreenHeight() - ledMatrixInfo.getTopBottomAreaHeight()) + ledMatrixInfo.getLetterboxBorder();
                taleWidth = ledMatrixInfo.getBottomLedDistance();
                taleHeight = ledMatrixInfo.getTopBottomAreaHeight();
                for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getBottomRightLedOriginal()) {
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