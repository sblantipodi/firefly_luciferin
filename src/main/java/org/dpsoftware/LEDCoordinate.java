/*
  LEDCoordinate.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/

package org.dpsoftware;

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
                                                               int rightLed, int topLed, int leftLed, int bottomLeftLed) {

        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, 0.10, screenWidth, screenHeight, bottomRightLed, rightLed, topLed, leftLed, bottomLeftLed);
        return defaultLedMatrix;

    }

    /**
     * Init Letterbox LED Matrix with a default general purpose config
     *
     * @return LED letterbox matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initLetterboxLedMatrix(int screenWidth, int screenHeight, int bottomRightLed,
                                                              int rightLed, int topLed, int leftLed, int bottomLeftLed) {

        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, 0.15, screenWidth, screenHeight, bottomRightLed, rightLed, topLed,
                leftLed, bottomLeftLed);
        return defaultLedMatrix;

    }

    void initializeLedMatrix(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, double borderRatio, int width, int height,
                             int bottomRightLed, int rightLed, int topLed, int leftLed, int bottomLeftLed) {

        var border = (int) (height * borderRatio);
        var ledNum = 0;

        // bottomRight LED strip
        var bottomSpace = ((width / 2) * 0.15);
        var bottomLedDistance = ((width / 2) - bottomSpace) / bottomRightLed;
        for (int i = 1; i <= bottomRightLed; i++) {
            ledNum++;
            defaultLedMatrix.put(ledNum, new LEDCoordinate((int) (((int) (((width / 2) + bottomLedDistance) + (bottomLedDistance * i))) + bottomLedDistance), height - (border)));
        }
        // right LED strip
        var rightLedDistance = (height - (border * 2)) / rightLed;
        for (int i = 1; i <= rightLed; i++) {
            ledNum++;
            defaultLedMatrix.put(ledNum, new LEDCoordinate(width - (border), (height - (rightLedDistance * i)) - border));
        }
        // top LED strip
        var topLedDistance = (width) / topLed;
        for (int i = 1; i <= topLed; i++) {
            ledNum++;
            defaultLedMatrix.put(ledNum, new LEDCoordinate(width - (topLedDistance * i), border - 30));
        }
        // left LED strip
        var leftLedDistance = (height - (border * 2)) / leftLed;
        for (int i = leftLed; i >= 1; i--) {
            ledNum++;
            defaultLedMatrix.put(ledNum, new LEDCoordinate(border - 40, (height - (leftLedDistance * i)) - border));
        }
        // bottomLeft LED strip
        var bottomLedLeftDistance = ((width / 2) - bottomSpace) / bottomLeftLed;
        for (int i = 1; i <= bottomLeftLed; i++) {
            ledNum++;
            defaultLedMatrix.put(ledNum, new LEDCoordinate((int) (((int) (bottomLedLeftDistance * i)) - bottomLedLeftDistance), height - (border)));
        }

    }

}