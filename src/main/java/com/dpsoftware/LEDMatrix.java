/*
  LEDMatrix.java

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

package com.dpsoftware;

import java.util.HashMap;
import java.util.Map;


public class LEDMatrix {

    private Map ledMatrix;
    private int x;
    private int y;
    private final int SCREEN_RES_X = 3840;
    private final int SCREEN_RES_Y = 2160;
    private final int OS_SCALING = 150;
    private final int LED_OFFSET = 30;

    /**
     * LED Matrix
     */
    public void initLedMatrix() {

        ledMatrix = new HashMap<Integer, LEDMatrix>();

        ledMatrix.put(1,new LEDMatrix(2560,1440));
        ledMatrix.put(2,new LEDMatrix(2566,1836));
        ledMatrix.put(3,new LEDMatrix(2664,1836));
        ledMatrix.put(4,new LEDMatrix(2762,1836));
        ledMatrix.put(5,new LEDMatrix(2860,1836));
        ledMatrix.put(6,new LEDMatrix(2958,1836));
        ledMatrix.put(7,new LEDMatrix(3056,1836));
        ledMatrix.put(8,new LEDMatrix(3154,1836));
        ledMatrix.put(9,new LEDMatrix(3252,1836));
        ledMatrix.put(10,new LEDMatrix(3350,1836));
        ledMatrix.put(11,new LEDMatrix(3448,1836));
        ledMatrix.put(12,new LEDMatrix(3546,1836));
        ledMatrix.put(13,new LEDMatrix(3644,1836));
        ledMatrix.put(14,new LEDMatrix(3742,1836));
        ledMatrix.put(15,new LEDMatrix(3264,2040));
        ledMatrix.put(16,new LEDMatrix(3264,1920));
        ledMatrix.put(17,new LEDMatrix(3264,1800));
        ledMatrix.put(18,new LEDMatrix(3264,1680));
        ledMatrix.put(19,new LEDMatrix(3264,1560));
        ledMatrix.put(20,new LEDMatrix(3264,1440));
        ledMatrix.put(21,new LEDMatrix(3264,1320));
        ledMatrix.put(22,new LEDMatrix(3264,1200));
        ledMatrix.put(23,new LEDMatrix(3264,1080));
        ledMatrix.put(24,new LEDMatrix(3264,960));
        ledMatrix.put(25,new LEDMatrix(3264,840));
        ledMatrix.put(26,new LEDMatrix(3264,720));
        ledMatrix.put(27,new LEDMatrix(3264,600));
        ledMatrix.put(28,new LEDMatrix(3264,480));
        ledMatrix.put(29,new LEDMatrix(3264,360));
        ledMatrix.put(30,new LEDMatrix(3264,240));
        ledMatrix.put(31,new LEDMatrix(3264,120));
        ledMatrix.put(32,new LEDMatrix(3264,0));
        ledMatrix.put(33,new LEDMatrix(3724,0));
        ledMatrix.put(34,new LEDMatrix(3608,0));
        ledMatrix.put(35,new LEDMatrix(3492,0));
        ledMatrix.put(36,new LEDMatrix(3376,0));
        ledMatrix.put(37,new LEDMatrix(3260,0));
        ledMatrix.put(38,new LEDMatrix(3144,0));
        ledMatrix.put(39,new LEDMatrix(3028,0));
        ledMatrix.put(40,new LEDMatrix(2912,0));
        ledMatrix.put(41,new LEDMatrix(2796,0));
        ledMatrix.put(42,new LEDMatrix(2680,0));
        ledMatrix.put(43,new LEDMatrix(2564,0));
        ledMatrix.put(44,new LEDMatrix(2448,0));
        ledMatrix.put(45,new LEDMatrix(2332,0));
        ledMatrix.put(46,new LEDMatrix(2216,0));
        ledMatrix.put(47,new LEDMatrix(2100,0));
        ledMatrix.put(48,new LEDMatrix(1984,0));
        ledMatrix.put(49,new LEDMatrix(1856,0));
        ledMatrix.put(50,new LEDMatrix(1740,0));
        ledMatrix.put(51,new LEDMatrix(1624,0));
        ledMatrix.put(52,new LEDMatrix(1508,0));
        ledMatrix.put(53,new LEDMatrix(1392,0));
        ledMatrix.put(54,new LEDMatrix(1276,0));
        ledMatrix.put(55,new LEDMatrix(1160,0));
        ledMatrix.put(56,new LEDMatrix(1044,0));
        ledMatrix.put(57,new LEDMatrix(928,0));
        ledMatrix.put(58,new LEDMatrix(812,0));
        ledMatrix.put(59,new LEDMatrix(696,0));
        ledMatrix.put(60,new LEDMatrix(580,0));
        ledMatrix.put(61,new LEDMatrix(464,0));
        ledMatrix.put(62,new LEDMatrix(348,0));
        ledMatrix.put(63,new LEDMatrix(232,0));
        ledMatrix.put(64,new LEDMatrix(116,0));
        ledMatrix.put(65,new LEDMatrix(0,0));
        ledMatrix.put(66,new LEDMatrix(0,0));
        ledMatrix.put(67,new LEDMatrix(0,120));
        ledMatrix.put(68,new LEDMatrix(0,240));
        ledMatrix.put(69,new LEDMatrix(0,360));
        ledMatrix.put(70,new LEDMatrix(0,480));
        ledMatrix.put(71,new LEDMatrix(0,600));
        ledMatrix.put(72,new LEDMatrix(0,720));
        ledMatrix.put(73,new LEDMatrix(0,840));
        ledMatrix.put(74,new LEDMatrix(0,960));
        ledMatrix.put(75,new LEDMatrix(0,1080));
        ledMatrix.put(76,new LEDMatrix(0,1200));
        ledMatrix.put(77,new LEDMatrix(0,1320));
        ledMatrix.put(78,new LEDMatrix(0,1440));
        ledMatrix.put(79,new LEDMatrix(0,1560));
        ledMatrix.put(80,new LEDMatrix(0,1680));
        ledMatrix.put(81,new LEDMatrix(0,1800));
        ledMatrix.put(82,new LEDMatrix(0,1920));
        ledMatrix.put(83,new LEDMatrix(0,2040));
        ledMatrix.put(84,new LEDMatrix(0,1836));
        ledMatrix.put(85,new LEDMatrix(98,1836));
        ledMatrix.put(86,new LEDMatrix(196,1836));
        ledMatrix.put(87,new LEDMatrix(294,1836));
        ledMatrix.put(88,new LEDMatrix(392,1836));
        ledMatrix.put(89,new LEDMatrix(490,1836));
        ledMatrix.put(90,new LEDMatrix(588,1836));
        ledMatrix.put(91,new LEDMatrix(686,1836));
        ledMatrix.put(92,new LEDMatrix(784,1836));
        ledMatrix.put(93,new LEDMatrix(882,1836));
        ledMatrix.put(94,new LEDMatrix(980,1836));
        ledMatrix.put(95,new LEDMatrix(1078,1836));

    }

    public LEDMatrix() {

    }

    public LEDMatrix(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return ((x * 100) / OS_SCALING) + LED_OFFSET;
    }

    public int getY() {
        return ((y * 100) / OS_SCALING) + LED_OFFSET;
    }

    public Map getLedMatrix() {
        return ledMatrix;
    }

    public int getSCREEN_RES_X() {
        return ((SCREEN_RES_X * 100) / OS_SCALING);
    }

    public int getSCREEN_RES_Y() {
        return ((SCREEN_RES_Y * 100) / OS_SCALING);
    }

}
