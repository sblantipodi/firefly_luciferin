/*
  LEDCoordinate.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020  Davide Perini

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

import javafx.scene.control.CheckBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    /**
     * Init FullScreen LED Matrix with a default general purpose config
     *
     * @return LED Matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initFullScreenLedMatrix(int screenWidth, int screenHeight, int bottomRightLed,
                                                                         int rightLed, int topLed, int leftLed, int bottomLeftLed, int bottomRowLed, CheckBox splitBottomRow) {

        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, 0.10, screenWidth, screenHeight, bottomRightLed, rightLed, topLed, leftLed, bottomLeftLed, bottomRowLed, splitBottomRow);
        return defaultLedMatrix;

    }

    /**
     * Init Letterbox LED Matrix with a default general purpose config
     *
     * @return LED letterbox matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initLetterboxLedMatrix(int screenWidth, int screenHeight, int bottomRightLed,
                                                              int rightLed, int topLed, int leftLed, int bottomLeftLed, int bottomRowLed, CheckBox splitBottomRow) {

        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, 0.15, screenWidth, screenHeight, bottomRightLed, rightLed, topLed,
                leftLed, bottomLeftLed, bottomRowLed, splitBottomRow);
        return defaultLedMatrix;

    }

    void initializeLedMatrix(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, double borderRatio, int width, int height,
                             int bottomRightLed, int rightLed, int topLed, int leftLed, int bottomLeftLed, int bottomRowLed, CheckBox splitBottomRow) {

        var border = (int) (height * borderRatio);
        var ledNum = 0;

        // bottomRight LED strip
        var bottomSpace = ((width / 2) * 0.15);
        if (splitBottomRow.isSelected()) {
            var bottomLedDistance = ((width / 2) - bottomSpace) / bottomRightLed;
            for (int i = 1; i <= bottomRightLed; i++) {
                ledNum++;
                defaultLedMatrix.put(ledNum, new LEDCoordinate(((int) ((int) (((int) (bottomLedDistance * i)) - bottomLedDistance) + (width/2) + (bottomSpace + 10))) + 15, height - (border)));
            }
        } else {
            // bottomLeft LED strip
            var bottomLedLeftDistance = width / bottomRowLed;
            for (int i = 1; i <= bottomRowLed; i++) {
                ledNum++;
                defaultLedMatrix.put(ledNum, new LEDCoordinate(((bottomLedLeftDistance * i) - bottomLedLeftDistance) + 20, height - (border)));
            }
        }
        // right LED strip
        var rightLedDistance = (height - (border * 2)) / rightLed;
        for (int i = 1; i <= rightLed; i++) {
            ledNum++;
            defaultLedMatrix.put(ledNum, new LEDCoordinate(width - 70, (height - (rightLedDistance * i)) - border));
        }
        // top LED strip
        var topLedDistance = width / topLed;
        for (int i = 1; i <= topLed; i++) {
            ledNum++;
            defaultLedMatrix.put(ledNum, new LEDCoordinate(width - (topLedDistance * i), border - 50));
        }
        // left LED strip
        var leftLedDistance = (height - (border * 2)) / leftLed;
        for (int i = leftLed; i >= 1; i--) {
            ledNum++;
            defaultLedMatrix.put(ledNum, new LEDCoordinate(70, (height - (leftLedDistance * i)) - border));
        }
        if (splitBottomRow.isSelected()) {
            // bottomLeft LED strip
            var bottomLedLeftDistance = ((width / 2) - bottomSpace) / bottomLeftLed;
            for (int i = 1; i <= bottomLeftLed; i++) {
                ledNum++;
                defaultLedMatrix.put(ledNum, new LEDCoordinate((int) (((int) (bottomLedLeftDistance * i)) - bottomLedLeftDistance), height - (border)));
            }
        }

    }

}