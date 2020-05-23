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

import java.util.HashMap;
import java.util.Map;


/**
 * X Y coordinate for LEDs
 */
public class LEDCoordinate {

    private int x;
    private int y;

    public LEDCoordinate() {
    }

    public LEDCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Init LED Matrix with a default general purpose config
     * @return
     */
    public Map initDefaultLedMatrix() {

        Map defaultLedMatrix = new HashMap<Integer, LEDCoordinate>();

        defaultLedMatrix.put(1, new LEDCoordinate(2566, 1836));
        defaultLedMatrix.put(2, new LEDCoordinate(2664, 1836));
        defaultLedMatrix.put(3, new LEDCoordinate(2762, 1836));
        defaultLedMatrix.put(4, new LEDCoordinate(2860, 1836));
        defaultLedMatrix.put(5, new LEDCoordinate(2958, 1836));
        defaultLedMatrix.put(6, new LEDCoordinate(3056, 1836));
        defaultLedMatrix.put(7, new LEDCoordinate(3154, 1836));
        defaultLedMatrix.put(8, new LEDCoordinate(3252, 1836));
        defaultLedMatrix.put(9, new LEDCoordinate(3350, 1836));
        defaultLedMatrix.put(10, new LEDCoordinate(3448, 1836));
        defaultLedMatrix.put(11, new LEDCoordinate(3546, 1836));
        defaultLedMatrix.put(12, new LEDCoordinate(3644, 1836));
        defaultLedMatrix.put(13, new LEDCoordinate(3742, 1836));
        defaultLedMatrix.put(14, new LEDCoordinate(3264, 2040));
        defaultLedMatrix.put(15, new LEDCoordinate(3264, 1920));
        defaultLedMatrix.put(16, new LEDCoordinate(3264, 1800));
        defaultLedMatrix.put(17, new LEDCoordinate(3264, 1680));
        defaultLedMatrix.put(18, new LEDCoordinate(3264, 1560));
        defaultLedMatrix.put(19, new LEDCoordinate(3264, 1440));
        defaultLedMatrix.put(20, new LEDCoordinate(3264, 1320));
        defaultLedMatrix.put(21, new LEDCoordinate(3264, 1200));
        defaultLedMatrix.put(22, new LEDCoordinate(3264, 1080));
        defaultLedMatrix.put(23, new LEDCoordinate(3264, 960));
        defaultLedMatrix.put(24, new LEDCoordinate(3264, 840));
        defaultLedMatrix.put(25, new LEDCoordinate(3264, 720));
        defaultLedMatrix.put(26, new LEDCoordinate(3264, 600));
        defaultLedMatrix.put(27, new LEDCoordinate(3264, 480));
        defaultLedMatrix.put(28, new LEDCoordinate(3264, 360));
        defaultLedMatrix.put(29, new LEDCoordinate(3264, 240));
        defaultLedMatrix.put(30, new LEDCoordinate(3264, 120));
        defaultLedMatrix.put(31, new LEDCoordinate(3264, 0));
        defaultLedMatrix.put(32, new LEDCoordinate(3724, 0));
        defaultLedMatrix.put(33, new LEDCoordinate(3608, 0));
        defaultLedMatrix.put(34, new LEDCoordinate(3492, 0));
        defaultLedMatrix.put(35, new LEDCoordinate(3376, 0));
        defaultLedMatrix.put(36, new LEDCoordinate(3260, 0));
        defaultLedMatrix.put(37, new LEDCoordinate(3144, 0));
        defaultLedMatrix.put(38, new LEDCoordinate(3028, 0));
        defaultLedMatrix.put(39, new LEDCoordinate(2912, 0));
        defaultLedMatrix.put(40, new LEDCoordinate(2796, 0));
        defaultLedMatrix.put(41, new LEDCoordinate(2680, 0));
        defaultLedMatrix.put(42, new LEDCoordinate(2564, 0));
        defaultLedMatrix.put(43, new LEDCoordinate(2448, 0));
        defaultLedMatrix.put(44, new LEDCoordinate(2332, 0));
        defaultLedMatrix.put(45, new LEDCoordinate(2216, 0));
        defaultLedMatrix.put(46, new LEDCoordinate(2100, 0));
        defaultLedMatrix.put(47, new LEDCoordinate(1984, 0));
        defaultLedMatrix.put(48, new LEDCoordinate(1856, 0));
        defaultLedMatrix.put(49, new LEDCoordinate(1740, 0));
        defaultLedMatrix.put(50, new LEDCoordinate(1624, 0));
        defaultLedMatrix.put(51, new LEDCoordinate(1508, 0));
        defaultLedMatrix.put(52, new LEDCoordinate(1392, 0));
        defaultLedMatrix.put(53, new LEDCoordinate(1276, 0));
        defaultLedMatrix.put(54, new LEDCoordinate(1160, 0));
        defaultLedMatrix.put(55, new LEDCoordinate(1044, 0));
        defaultLedMatrix.put(56, new LEDCoordinate(928, 0));
        defaultLedMatrix.put(57, new LEDCoordinate(812, 0));
        defaultLedMatrix.put(58, new LEDCoordinate(696, 0));
        defaultLedMatrix.put(59, new LEDCoordinate(580, 0));
        defaultLedMatrix.put(60, new LEDCoordinate(464, 0));
        defaultLedMatrix.put(61, new LEDCoordinate(348, 0));
        defaultLedMatrix.put(62, new LEDCoordinate(232, 0));
        defaultLedMatrix.put(63, new LEDCoordinate(116, 0));
        defaultLedMatrix.put(64, new LEDCoordinate(0, 0));
        defaultLedMatrix.put(65, new LEDCoordinate(0, 0));
        defaultLedMatrix.put(66, new LEDCoordinate(0, 120));
        defaultLedMatrix.put(67, new LEDCoordinate(0, 240));
        defaultLedMatrix.put(68, new LEDCoordinate(0, 360));
        defaultLedMatrix.put(69, new LEDCoordinate(0, 480));
        defaultLedMatrix.put(70, new LEDCoordinate(0, 600));
        defaultLedMatrix.put(71, new LEDCoordinate(0, 720));
        defaultLedMatrix.put(72, new LEDCoordinate(0, 840));
        defaultLedMatrix.put(73, new LEDCoordinate(0, 960));
        defaultLedMatrix.put(74, new LEDCoordinate(0, 1080));
        defaultLedMatrix.put(75, new LEDCoordinate(0, 1200));
        defaultLedMatrix.put(76, new LEDCoordinate(0, 1320));
        defaultLedMatrix.put(77, new LEDCoordinate(0, 1440));
        defaultLedMatrix.put(78, new LEDCoordinate(0, 1560));
        defaultLedMatrix.put(79, new LEDCoordinate(0, 1680));
        defaultLedMatrix.put(80, new LEDCoordinate(0, 1800));
        defaultLedMatrix.put(81, new LEDCoordinate(0, 1920));
        defaultLedMatrix.put(82, new LEDCoordinate(0, 2040));
        defaultLedMatrix.put(83, new LEDCoordinate(0, 1836));
        defaultLedMatrix.put(84, new LEDCoordinate(98, 1836));
        defaultLedMatrix.put(85, new LEDCoordinate(196, 1836));
        defaultLedMatrix.put(86, new LEDCoordinate(294, 1836));
        defaultLedMatrix.put(87, new LEDCoordinate(392, 1836));
        defaultLedMatrix.put(88, new LEDCoordinate(490, 1836));
        defaultLedMatrix.put(89, new LEDCoordinate(588, 1836));
        defaultLedMatrix.put(90, new LEDCoordinate(686, 1836));
        defaultLedMatrix.put(91, new LEDCoordinate(784, 1836));
        defaultLedMatrix.put(92, new LEDCoordinate(882, 1836));
        defaultLedMatrix.put(93, new LEDCoordinate(980, 1836));
        defaultLedMatrix.put(94, new LEDCoordinate(1078, 1836));
        defaultLedMatrix.put(95, new LEDCoordinate(1176, 1836));

        return defaultLedMatrix;

    }

    // Coordinates is changed due to OS scaling
    public int getX() {
        return x;
    }

    // Coordinates is changed due to OS scaling
    public int getY() {
        return y;
    }

}
