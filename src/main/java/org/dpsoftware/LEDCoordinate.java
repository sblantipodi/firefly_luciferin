/*
  LEDCoordinate.java

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
package org.dpsoftware;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.managers.dto.LedMatrixInfo;
import org.dpsoftware.utilities.CommonUtility;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


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
    private boolean active = true;
    private boolean groupedLed;
    private String zone;

    public LEDCoordinate(int x, int y, int width, int height, boolean groupedLed, String zone) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.groupedLed = groupedLed;
        this.zone = zone;
    }

    /**
     * Calculate tale border size
     *
     * @param width screen width
     * @return tale border size
     */
    public static int calculateTaleBorder(int width) {
        return (Constants.TEST_CANVAS_BORDER_RATIO * width) / Constants.REFERENCE_RESOLUTION_FOR_SCALING_X;
    }

    /**
     * Calculated start end of the satellite
     *
     * @param sat satellite where to do the calculation
     * @return record with start and end
     */
    public static getStartEndLeds getGetStartEndLeds(Satellite sat) {
        int start, end;
        LinkedHashMap<Integer, LEDCoordinate> ledMatrix = MainSingleton.getInstance().config.getLedMatrixInUse(MainSingleton.getInstance().config.getDefaultLedMatrix());
        if (sat.getZone().equals(Enums.PossibleZones.ENTIRE_SCREEN.getBaseI18n())) {
            start = 1;
            end = MainSingleton.getInstance().ledNumber;
        } else {
            String correctedColor = sat.getZone();
            if (sat.getZone().equals(Enums.PossibleZones.TOP_RIGHT.getBaseI18n()) || sat.getZone().equals(Enums.PossibleZones.TOP_LEFT.getBaseI18n())) {
                correctedColor = Enums.PossibleZones.TOP.getBaseI18n();
            }
            if (!CommonUtility.isSplitBottomRow(MainSingleton.getInstance().config.getSplitBottomMargin())) {
                if (sat.getZone().equals(Enums.PossibleZones.BOTTOM_RIGHT.getBaseI18n()) || sat.getZone().equals(Enums.PossibleZones.BOTTOM_LEFT.getBaseI18n())) {
                    correctedColor = Enums.PossibleZones.BOTTOM.getBaseI18n();
                }
            }
            String finalCorrectedColor = correctedColor;
            List<Integer> filteredList;
            filteredList = ledMatrix.entrySet().stream()
                    .filter(e -> e.getValue().getZone().equals(finalCorrectedColor))
                    .map(Map.Entry::getKey)
                    .toList();
            start = filteredList
                    .stream()
                    .mapToInt(v -> v)
                    .min().orElse(0);
            end = filteredList
                    .stream()
                    .mapToInt(v -> v)
                    .max().orElse(0);
            if (sat.getZone().equals(Enums.PossibleZones.TOP_RIGHT.getBaseI18n())) {
                end = start + ((end - start) / 3);
            }
            if (sat.getZone().equals(Enums.PossibleZones.TOP_LEFT.getBaseI18n())) {
                int segment = (end - start) / 3;
                start = start + (segment * 2);
            }
            if (!CommonUtility.isSplitBottomRow(MainSingleton.getInstance().config.getSplitBottomMargin())) {
                if (sat.getZone().equals(Enums.PossibleZones.BOTTOM_LEFT.getBaseI18n())) {
                    end = start + ((end - start) / 3);
                }
                if (sat.getZone().equals(Enums.PossibleZones.BOTTOM_RIGHT.getBaseI18n())) {
                    int segment = (end - start) / 3;
                    start = start + (segment * 2);
                }
            }
        }
        return new getStartEndLeds(start, end);
    }

    /**
     * Calculate borders for fit to screen, 4:3, 16:9, 21:9, 32:9
     *
     * @param screenWidth  screen width
     * @param screenHeight screen height
     */
    int calculateBorders(int screenWidth, int screenHeight) {
        var monitorAR = CommonUtility.checkMonitorAspectRatio(screenWidth, screenHeight);
        return switch (monitorAR) {
            case AR_43 -> 0;
            case AR_169 -> ((screenWidth * 480) / Constants.REFERENCE_RESOLUTION_FOR_SCALING_X) + 100;
            case AR_219 -> ((screenWidth * 440) / 3440) + 100;
            case AR_329 -> ((screenWidth * 960) / Constants.REFERENCE_RESOLUTION_FOR_SCALING_X) + 100;
        };
    }

    /**
     * Init LED Matrixes
     *
     * @param aspectRatio    aspect ratio in use
     * @param ledMatrixInfo  infos used to create the LED matrix
     * @param forceNewMatrix use a previously initialized matrix
     * @return
     */
    @SuppressWarnings({"All"})
    public LinkedHashMap<Integer, LEDCoordinate> initializeLedMatrix(Enums.AspectRatio aspectRatio, LedMatrixInfo ledMatrixInfo, boolean forceNewMatrix) {
        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        if (MainSingleton.getInstance().config != null && !forceNewMatrix) {
            if (MainSingleton.getInstance().config.getLedMatrix().get(aspectRatio.getBaseI18n()) != null && !MainSingleton.getInstance().config.getLedMatrix().get(aspectRatio.getBaseI18n()).isEmpty()) {
                defaultLedMatrix = MainSingleton.getInstance().config.getLedMatrix().get(aspectRatio.getBaseI18n());
                return defaultLedMatrix;
            }
        }
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
        if (aspectRatio == Enums.AspectRatio.LETTERBOX) {
            ledMatrixInfo.setLetterboxBorder(ledMatrixInfo.getScreenHeight() / Constants.LETTERBOX_RATIO);
        } else if (aspectRatio == Enums.AspectRatio.PILLARBOX) {
            ledMatrixInfo.setPillarboxBorder(calculateBorders(ledMatrixInfo.getScreenWidth(), ledMatrixInfo.getScreenHeight()));
        }
        ledMatrixInfo.setScreenWidth(ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getPillarboxBorder() * 2));
        ledMatrixInfo.setScreenHeight(ledMatrixInfo.getScreenHeight() - (ledMatrixInfo.getLetterboxBorder() * 2));
        ledMatrixInfo.setTopBottomAreaHeight((ledMatrixInfo.getScreenHeight() * Integer.parseInt(ledMatrixInfo.getGrabberTopBottom().replace(Constants.PERCENT, ""))) / 100);
        ledMatrixInfo.setSideAreaWidth((ledMatrixInfo.getScreenWidth() * Integer.parseInt(ledMatrixInfo.getGrabberSide().replace(Constants.PERCENT, ""))) / 100);
        ledMatrixInfo.setSplitBottomMargin((ledMatrixInfo.getScreenWidth() * Integer.parseInt(ledMatrixInfo.getSplitBottomRow().replace(Constants.PERCENT, ""))) / 100);
        ledMatrixInfo.setCornerGapTopBottom((ledMatrixInfo.getScreenHeight() * Integer.parseInt(ledMatrixInfo.getGapTypeTopBottom().replace(Constants.PERCENT, ""))) / 100);
        ledMatrixInfo.setCornerGapSide((ledMatrixInfo.getScreenWidth() * Integer.parseInt(ledMatrixInfo.getGapTypeSide().replace(Constants.PERCENT, ""))) / 100);
        if (ledMatrixInfo.getBottomRightLed() > 0 && CommonUtility.isSplitBottomRow(ledMatrixInfo.getSplitBottomRow())) {
            ledMatrixInfo.setBottomLedDistance((((ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getCornerGapSide() * 2)) - ledMatrixInfo.getSplitBottomMargin()) / 2) / ledMatrixInfo.getBottomRightLed());
        } else if (ledMatrixInfo.getBottomRowLed() > 0 && !CommonUtility.isSplitBottomRow(ledMatrixInfo.getSplitBottomRow())) {
            ledMatrixInfo.setBottomLedDistance((((ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getCornerGapSide() * 2)) - ledMatrixInfo.getSplitBottomMargin())) / ledMatrixInfo.getBottomRowLed());
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
        return defaultLedMatrix;
    }

    /**
     * Init LEFT side LEDs
     *
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo    infos used to create the LED matrix
     * @param ledNum           current LEDs
     * @return next LED to process
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private int leftLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (ledMatrixInfo.getLeftLed() > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            var leftLedDistance = (ledMatrixInfo.getScreenHeight() - (ledMatrixInfo.getCornerGapTopBottom() * 2)) / ledMatrixInfo.getLeftLed();
            int cornerGapTopBottomAccurate = (ledMatrixInfo.getScreenHeight() - (leftLedDistance * ledMatrixInfo.getLeftLed())) / 2;
            for (int i = ledMatrixInfo.getLeftLed(); i >= 1; i--) {
                for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                    x = ledMatrixInfo.getPillarboxBorder();
                    y = Math.max(0, (((ledMatrixInfo.getScreenHeight() - (leftLedDistance * i)) - cornerGapTopBottomAccurate) + ledMatrixInfo.getLetterboxBorder()) - calculateTaleBorder(ledMatrixInfo.getScreenWidth()));
                    taleWidth = ledMatrixInfo.getSideAreaWidth();
                    taleHeight = leftLedDistance;
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0, Enums.PossibleZones.LEFT.getBaseI18n()));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getLeftLedOriginal()) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true, Enums.PossibleZones.LEFT.getBaseI18n()));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * Init BOTTOM LEFT LEDs
     *
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo    infos used to create the LED matrix
     * @param ledNum           current LEDs
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
                        defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0, Enums.PossibleZones.BOTTOM_LEFT.getBaseI18n()));
                        ledInsertionNumber++;
                    }
                }
                while (ledInsertionNumber < ledMatrixInfo.getBottomLeftLedOriginal()) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true, Enums.PossibleZones.BOTTOM_LEFT.getBaseI18n()));
                    ledInsertionNumber++;
                }
            }


        }
    }

    /**
     * Init TOP LEDs
     *
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo    infos used to create the LED matrix
     * @param ledNum           current LEDs
     * @return next LED to process
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private int topLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (ledMatrixInfo.getTopLed() > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            var topLedDistance = (ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getCornerGapSide() * 2)) / ledMatrixInfo.getTopLed();
            int cornerGapSideAccurate = (ledMatrixInfo.getScreenWidth() - (topLedDistance * ledMatrixInfo.getTopLed())) / 2;
            for (int i = 1; i <= ledMatrixInfo.getTopLed(); i++) {
                x = ((ledMatrixInfo.getScreenWidth() - (topLedDistance * i)) - cornerGapSideAccurate) + ledMatrixInfo.getPillarboxBorder();
                y = ledMatrixInfo.getLetterboxBorder();
                taleWidth = topLedDistance;
                taleHeight = ledMatrixInfo.getTopBottomAreaHeight();
                for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0, Enums.PossibleZones.TOP.getBaseI18n()));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getTopLedOriginal()) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true, Enums.PossibleZones.TOP.getBaseI18n()));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * Init RIGHT side LEDs
     *
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo    infos used to create the LED matrix
     * @param ledNum           current LEDs
     * @return next LED to process
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private int rightLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (ledMatrixInfo.getRightLed() > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            var rightLedDistance = (ledMatrixInfo.getScreenHeight() - (ledMatrixInfo.getCornerGapTopBottom() * 2)) / ledMatrixInfo.getRightLed();
            int cornerGapTopBottomAccurate = (ledMatrixInfo.getScreenHeight() - (rightLedDistance * ledMatrixInfo.getRightLed())) / 2;
            for (int i = 1; i <= ledMatrixInfo.getRightLed(); i++) {
                x = (ledMatrixInfo.getScreenWidth() - ledMatrixInfo.getSideAreaWidth()) + ledMatrixInfo.getPillarboxBorder();
                y = Math.max(0, (((ledMatrixInfo.getScreenHeight() - (rightLedDistance * i)) - cornerGapTopBottomAccurate) + ledMatrixInfo.getLetterboxBorder()) - calculateTaleBorder(ledMatrixInfo.getScreenWidth()));
                taleWidth = ledMatrixInfo.getSideAreaWidth();
                taleHeight = rightLedDistance;
                for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0, Enums.PossibleZones.RIGHT.getBaseI18n()));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getRightLedOriginal()) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true, Enums.PossibleZones.RIGHT.getBaseI18n()));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * Init BOTTOM LEDs
     *
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo    infos used to create the LED matrix
     * @param ledNum           current LEDs
     * @return next LED to process
     */
    private int bottomLed(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, LedMatrixInfo ledMatrixInfo, int ledNum) {
        if (ledMatrixInfo.getBottomRowLed() > 0) {
            int ledInsertionNumber = 0;
            int x = 0, y = 0, taleWidth = 0, taleHeight = 0;
            int cornerGapSideAccurate = (ledMatrixInfo.getScreenWidth() - (ledMatrixInfo.getBottomLedDistance() * ledMatrixInfo.getBottomRowLed())) / 2;
            for (int i = 1; i <= ledMatrixInfo.getBottomRowLed(); i++) {
                x = (((ledMatrixInfo.getBottomLedDistance() * i) - ledMatrixInfo.getBottomLedDistance()) + cornerGapSideAccurate) + ledMatrixInfo.getPillarboxBorder();
                y = (ledMatrixInfo.getScreenHeight() - ledMatrixInfo.getTopBottomAreaHeight()) + ledMatrixInfo.getLetterboxBorder();
                taleWidth = ledMatrixInfo.getBottomLedDistance();
                taleHeight = ledMatrixInfo.getTopBottomAreaHeight();
                for (int groupIndex = 0; groupIndex < ledMatrixInfo.getGroupBy(); groupIndex++) {
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0, Enums.PossibleZones.BOTTOM.getBaseI18n()));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getBottomRowLedOriginal()) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true, Enums.PossibleZones.BOTTOM.getBaseI18n()));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * Init BOTTOM RIGHT LEDs
     *
     * @param defaultLedMatrix matrix to store
     * @param ledMatrixInfo    infos used to create the LED matrix
     * @param ledNum           current LEDs
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
                    defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, groupIndex != 0, Enums.PossibleZones.BOTTOM_RIGHT.getBaseI18n()));
                    ledInsertionNumber++;
                }
            }
            while (ledInsertionNumber < ledMatrixInfo.getBottomRightLedOriginal()) {
                defaultLedMatrix.put(++ledNum, new LEDCoordinate(x, y, taleWidth, taleHeight, true, Enums.PossibleZones.BOTTOM_RIGHT.getBaseI18n()));
                ledInsertionNumber++;
            }
        }
        return ledNum;
    }

    /**
     * +
     * Small record for satellites
     *
     * @param start zone where satellite starts
     * @param end   zone where satellite ends
     */
    public record getStartEndLeds(int start, int end) {
    }

}