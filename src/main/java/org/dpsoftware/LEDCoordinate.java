/*
  LEDCoordinate.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2021  Davide Perini

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
    private int dimension;

    /**
     * Init FullScreen LED Matrix with a default general purpose config
     *
     * @return LED Matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initFullScreenLedMatrix(int screenWidth, int screenHeight, int bottomRightLed,
                                                                         int rightLed, int topLed, int leftLed, int bottomLeftLed, int bottomRowLed, boolean splitBottomRow) {

        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, 0.10, screenWidth, screenHeight, bottomRightLed, rightLed, topLed, leftLed, bottomLeftLed, bottomRowLed, splitBottomRow, 70);
        return defaultLedMatrix;

    }

    /**
     * Init Letterbox LED Matrix with a default general purpose config
     *
     * @return LED letterbox matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initLetterboxLedMatrix(int screenWidth, int screenHeight, int bottomRightLed,
                                                                        int rightLed, int topLed, int leftLed, int bottomLeftLed, int bottomRowLed, boolean splitBottomRow) {

        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, 0.20, screenWidth, screenHeight, bottomRightLed, rightLed, topLed, leftLed, bottomLeftLed, bottomRowLed, splitBottomRow, 70);
        return defaultLedMatrix;

    }

    /**
     * Init Pillarbox LED Matrix with a default general purpose config
     *
     * @return LED letterbox matrix
     */
    public LinkedHashMap<Integer, LEDCoordinate> initPillarboxMatrix(int screenWidth, int screenHeight, int bottomRightLed,
                                                                     int rightLed, int topLed, int leftLed, int bottomLeftLed, int bottomRowLed, boolean splitBottomRow) {

        LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix = new LinkedHashMap<>();
        initializeLedMatrix(defaultLedMatrix, 0.15, screenWidth, screenHeight, bottomRightLed, rightLed, topLed,
                leftLed, bottomLeftLed, bottomRowLed, splitBottomRow, calculateBorders(screenWidth, screenHeight));
        return defaultLedMatrix;

    }

    /**
     * Calculate borders for fit to screen, 4:3, 16:9, 21:9
     * @param screenWidth screen width
     * @param screenHeight screen height
     */
    public static int calculateBorders(int screenWidth, int screenHeight) {
        double aspectRatio = Math.round(((double) screenWidth / (double) screenHeight) * 10) / 10.00; // Round aspect ratio to 2 decimals
        if (aspectRatio >= 1.2 && aspectRatio <= 1.4) { // standard 4:3
            return 0;
        } else if (aspectRatio >= 1.6 && aspectRatio <= 1.8) { // widescreen 16:9
            return ((screenWidth * 480) / 3840) + 50;
        } else if (aspectRatio >= 2.1 && aspectRatio <= 2.5) {
            return ((screenWidth * 440) / 3440) + 50; // ultra wide screen 21:9
        } else {
            return 0;
        }
    }

    void initializeLedMatrix(LinkedHashMap<Integer, LEDCoordinate> defaultLedMatrix, double borderRatio, int width, int height,
                             int bottomRightLed, int rightLed, int topLed, int leftLed, int bottomLeftLed, int bottomRowLed, boolean splitBottomRow, int fitMargin) {

        var border = (int) (height * borderRatio);
        var ledNum = 0;
        int margin = 70;
        int taleDistance = 10;

        if (fitMargin != margin) {
            width = width - (fitMargin * 2);
        }
        // bottomRight LED strip
        var bottomSpace = ((width / 2) * 0.15);
        if (splitBottomRow) {
            var bottomLedDistance = ((width / 2) - bottomSpace) / bottomRightLed;
            for (int i = 1; i <= bottomRightLed; i++) {
                ledNum++;
                int x = ((int) ((int) (((int) (bottomLedDistance * i)) - bottomLedDistance) + (width/2) + (bottomSpace + taleDistance))) + 15;
                if (fitMargin != margin) {
                    x += fitMargin;
                }
                defaultLedMatrix.put(ledNum, new LEDCoordinate(x, height - (border), (int) bottomLedDistance - taleDistance));
            }
        } else {
            // bottomLeft LED strip
            if (bottomRowLed > 0) {
                var bottomLedLeftDistance = width / bottomRowLed;
                for (int i = 1; i <= bottomRowLed; i++) {
                    ledNum++;
                    int x = ((bottomLedLeftDistance * i) - bottomLedLeftDistance) + 20;
                    if (fitMargin != margin) {
                        x += fitMargin;
                    }
                    defaultLedMatrix.put(ledNum, new LEDCoordinate(x, height - (border), bottomLedLeftDistance - taleDistance));
                }
            }
        }
        // right LED strip
        if (rightLed > 0) {
            var rightLedDistance = (height - (border * 2)) / rightLed;
            for (int i = 1; i <= rightLed; i++) {
                ledNum++;
                int x = 0;
                if (fitMargin != margin) {
                    x += fitMargin;
                }
                defaultLedMatrix.put(ledNum, new LEDCoordinate((width + x) - (margin + 50), (height - (rightLedDistance * i)) - border, rightLedDistance - taleDistance));
            }
        }
        // top LED strip
        if (topLed > 0) {
            var topLedDistance = width / topLed;
            for (int i = 1; i <= topLed; i++) {
                ledNum++;
                int x = (width - (topLedDistance * i));
                if (fitMargin != margin) {
                    x += fitMargin;
                }
                int topBorder = border - 100;
                defaultLedMatrix.put(ledNum, new LEDCoordinate(x, topBorder > 0 ? topBorder : 0, topLedDistance - taleDistance));
            }
        }
        // left LED strip
        if (leftLed > 0) {
            var leftLedDistance = (height - (border * 2)) / leftLed;
            for (int i = leftLed; i >= 1; i--) {
                ledNum++;
                defaultLedMatrix.put(ledNum, new LEDCoordinate(fitMargin, (height - (leftLedDistance * i)) - border, leftLedDistance - taleDistance));
            }
        }
        if (splitBottomRow) {
            // bottomLeft LED strip
            var bottomLedLeftDistance = ((width / 2) - bottomSpace) / bottomLeftLed;
            for (int i = 1; i <= bottomLeftLed; i++) {
                ledNum++;
                int x = (int) (((int) (bottomLedLeftDistance * i)) - bottomLedLeftDistance);
                if (fitMargin != margin) {
                    x += fitMargin;
                }
                defaultLedMatrix.put(ledNum, new LEDCoordinate(x, height - (border), (int) bottomLedLeftDistance - taleDistance));
            }
        }

    }

}