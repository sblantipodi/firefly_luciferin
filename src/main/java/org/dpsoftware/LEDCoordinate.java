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

import java.util.HashMap;
import java.util.Map;


/**
 * X Y coordinate for LEDs
 */
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
public class LEDCoordinate {

    private int x;
    private int y;

    /**
     * Init FullScreen LED Matrix with a default general purpose config
     * @return
     */
    public Map initFullScreenLedMatrix() {

        Map defaultLedMatrix = new HashMap<Integer, LEDCoordinate>();

        defaultLedMatrix.put(1, new LEDCoordinate(2596, 1970));
        defaultLedMatrix.put(2, new LEDCoordinate(2694, 1970));
        defaultLedMatrix.put(3, new LEDCoordinate(2792, 1970));
        defaultLedMatrix.put(4, new LEDCoordinate(2890, 1970));
        defaultLedMatrix.put(5, new LEDCoordinate(2988, 1970));
        defaultLedMatrix.put(6, new LEDCoordinate(3086, 1970));
        defaultLedMatrix.put(7, new LEDCoordinate(3184, 1970));
        defaultLedMatrix.put(8, new LEDCoordinate(3282, 1970));
        defaultLedMatrix.put(9, new LEDCoordinate(3380, 1970));
        defaultLedMatrix.put(10, new LEDCoordinate(3478, 1970));
        defaultLedMatrix.put(11, new LEDCoordinate(3576, 1970));
        defaultLedMatrix.put(12, new LEDCoordinate(3674, 1970));
        defaultLedMatrix.put(13, new LEDCoordinate(3772, 1970));
        defaultLedMatrix.put(14, new LEDCoordinate(3294, 2070));
        defaultLedMatrix.put(15, new LEDCoordinate(3294, 1950));
        defaultLedMatrix.put(16, new LEDCoordinate(3294, 1830));
        defaultLedMatrix.put(17, new LEDCoordinate(3294, 1710));
        defaultLedMatrix.put(18, new LEDCoordinate(3294, 1590));
        defaultLedMatrix.put(19, new LEDCoordinate(3294, 1470));
        defaultLedMatrix.put(20, new LEDCoordinate(3294, 1350));
        defaultLedMatrix.put(21, new LEDCoordinate(3294, 1230));
        defaultLedMatrix.put(22, new LEDCoordinate(3294, 1110));
        defaultLedMatrix.put(23, new LEDCoordinate(3294, 990));
        defaultLedMatrix.put(24, new LEDCoordinate(3294, 870));
        defaultLedMatrix.put(25, new LEDCoordinate(3294, 750));
        defaultLedMatrix.put(26, new LEDCoordinate(3294, 630));
        defaultLedMatrix.put(27, new LEDCoordinate(3294, 510));
        defaultLedMatrix.put(28, new LEDCoordinate(3294, 390));
        defaultLedMatrix.put(29, new LEDCoordinate(3294, 270));
        defaultLedMatrix.put(30, new LEDCoordinate(3294, 150));
        defaultLedMatrix.put(31, new LEDCoordinate(3294, 130));
        defaultLedMatrix.put(32, new LEDCoordinate(3754, 130));
        defaultLedMatrix.put(33, new LEDCoordinate(3638, 130));
        defaultLedMatrix.put(34, new LEDCoordinate(3522, 130));
        defaultLedMatrix.put(35, new LEDCoordinate(3406, 130));
        defaultLedMatrix.put(36, new LEDCoordinate(3290, 130));
        defaultLedMatrix.put(37, new LEDCoordinate(3174, 130));
        defaultLedMatrix.put(38, new LEDCoordinate(3058, 130));
        defaultLedMatrix.put(39, new LEDCoordinate(2942, 130));
        defaultLedMatrix.put(40, new LEDCoordinate(2826, 130));
        defaultLedMatrix.put(41, new LEDCoordinate(2710, 130));
        defaultLedMatrix.put(42, new LEDCoordinate(2594, 130));
        defaultLedMatrix.put(43, new LEDCoordinate(2478, 130));
        defaultLedMatrix.put(44, new LEDCoordinate(2362, 130));
        defaultLedMatrix.put(45, new LEDCoordinate(2246, 130));
        defaultLedMatrix.put(46, new LEDCoordinate(2130, 130));
        defaultLedMatrix.put(47, new LEDCoordinate(2014, 130));
        defaultLedMatrix.put(48, new LEDCoordinate(1886, 130));
        defaultLedMatrix.put(49, new LEDCoordinate(1770, 130));
        defaultLedMatrix.put(50, new LEDCoordinate(1654, 130));
        defaultLedMatrix.put(51, new LEDCoordinate(1538, 130));
        defaultLedMatrix.put(52, new LEDCoordinate(1422, 130));
        defaultLedMatrix.put(53, new LEDCoordinate(1306, 130));
        defaultLedMatrix.put(54, new LEDCoordinate(1190, 130));
        defaultLedMatrix.put(55, new LEDCoordinate(1074, 130));
        defaultLedMatrix.put(56, new LEDCoordinate(958, 130));
        defaultLedMatrix.put(57, new LEDCoordinate(842, 130));
        defaultLedMatrix.put(58, new LEDCoordinate(726, 130));
        defaultLedMatrix.put(59, new LEDCoordinate(610, 130));
        defaultLedMatrix.put(60, new LEDCoordinate(494, 130));
        defaultLedMatrix.put(61, new LEDCoordinate(378, 130));
        defaultLedMatrix.put(62, new LEDCoordinate(262, 130));
        defaultLedMatrix.put(63, new LEDCoordinate(146, 130));
        defaultLedMatrix.put(64, new LEDCoordinate(30, 130));
        defaultLedMatrix.put(65, new LEDCoordinate(30, 130));
        defaultLedMatrix.put(66, new LEDCoordinate(30, 150));
        defaultLedMatrix.put(67, new LEDCoordinate(30, 270));
        defaultLedMatrix.put(68, new LEDCoordinate(30, 390));
        defaultLedMatrix.put(69, new LEDCoordinate(30, 510));
        defaultLedMatrix.put(70, new LEDCoordinate(30, 630));
        defaultLedMatrix.put(71, new LEDCoordinate(30, 750));
        defaultLedMatrix.put(72, new LEDCoordinate(30, 870));
        defaultLedMatrix.put(73, new LEDCoordinate(30, 990));
        defaultLedMatrix.put(74, new LEDCoordinate(30, 1110));
        defaultLedMatrix.put(75, new LEDCoordinate(30, 1230));
        defaultLedMatrix.put(76, new LEDCoordinate(30, 1350));
        defaultLedMatrix.put(77, new LEDCoordinate(30, 1470));
        defaultLedMatrix.put(78, new LEDCoordinate(30, 1590));
        defaultLedMatrix.put(79, new LEDCoordinate(30, 1710));
        defaultLedMatrix.put(80, new LEDCoordinate(30, 1830));
        defaultLedMatrix.put(81, new LEDCoordinate(30, 1950));
        defaultLedMatrix.put(82, new LEDCoordinate(30, 2070));
        defaultLedMatrix.put(83, new LEDCoordinate(30, 1970));
        defaultLedMatrix.put(84, new LEDCoordinate(128, 1970));
        defaultLedMatrix.put(85, new LEDCoordinate(226, 1970));
        defaultLedMatrix.put(86, new LEDCoordinate(324, 1970));
        defaultLedMatrix.put(87, new LEDCoordinate(422, 1970));
        defaultLedMatrix.put(88, new LEDCoordinate(520, 1970));
        defaultLedMatrix.put(89, new LEDCoordinate(618, 1970));
        defaultLedMatrix.put(90, new LEDCoordinate(716, 1970));
        defaultLedMatrix.put(91, new LEDCoordinate(814, 1970));
        defaultLedMatrix.put(92, new LEDCoordinate(912, 1970));
        defaultLedMatrix.put(93, new LEDCoordinate(1010, 1970));
        defaultLedMatrix.put(93, new LEDCoordinate(1010, 1970));
        defaultLedMatrix.put(94, new LEDCoordinate(1108, 1970));
        defaultLedMatrix.put(94, new LEDCoordinate(1108, 1970));
        defaultLedMatrix.put(95, new LEDCoordinate(1206, 1970));
        defaultLedMatrix.put(95, new LEDCoordinate(1206, 1970));

        return defaultLedMatrix;

    }

    /**
     * Init Letterbox LED Matrix with a default general purpose config
     * @return
     */
    public Map initLetterboxLedMatrix() {

        Map defaultLedMatrix = new HashMap<Integer, LEDCoordinate>();

        defaultLedMatrix.put(1, new LEDCoordinate(2596, 1590));
        defaultLedMatrix.put(2, new LEDCoordinate(2694, 1590));
        defaultLedMatrix.put(3, new LEDCoordinate(2792, 1590));
        defaultLedMatrix.put(4, new LEDCoordinate(2890, 1590));
        defaultLedMatrix.put(5, new LEDCoordinate(2988, 1590));
        defaultLedMatrix.put(6, new LEDCoordinate(3086, 1590));
        defaultLedMatrix.put(7, new LEDCoordinate(3184, 1590));
        defaultLedMatrix.put(8, new LEDCoordinate(3282, 1590));
        defaultLedMatrix.put(9, new LEDCoordinate(3380, 1590));
        defaultLedMatrix.put(10, new LEDCoordinate(3478, 1590));
        defaultLedMatrix.put(11, new LEDCoordinate(3576, 1590));
        defaultLedMatrix.put(12, new LEDCoordinate(3674, 1590));
        defaultLedMatrix.put(13, new LEDCoordinate(3772, 1590));
        defaultLedMatrix.put(14, new LEDCoordinate(3294, 1733));
        defaultLedMatrix.put(15, new LEDCoordinate(3294, 1821));
        defaultLedMatrix.put(16, new LEDCoordinate(3294, 1640));
        defaultLedMatrix.put(17, new LEDCoordinate(3294, 1552));
        defaultLedMatrix.put(18, new LEDCoordinate(3294, 1462));
        defaultLedMatrix.put(19, new LEDCoordinate(3294, 1370));
        defaultLedMatrix.put(20, new LEDCoordinate(3294, 1282));
        defaultLedMatrix.put(21, new LEDCoordinate(3294, 1194));
        defaultLedMatrix.put(22, new LEDCoordinate(3294, 1106));
        defaultLedMatrix.put(23, new LEDCoordinate(3294, 1017));
        defaultLedMatrix.put(24, new LEDCoordinate(3294, 929));
        defaultLedMatrix.put(25, new LEDCoordinate(3294, 839));
        defaultLedMatrix.put(26, new LEDCoordinate(3294, 750));
        defaultLedMatrix.put(27, new LEDCoordinate(3294, 661));
        defaultLedMatrix.put(28, new LEDCoordinate(3294, 572));
        defaultLedMatrix.put(29, new LEDCoordinate(3294, 484));
        defaultLedMatrix.put(30, new LEDCoordinate(3294, 395));
        defaultLedMatrix.put(31, new LEDCoordinate(3294, 306));
        defaultLedMatrix.put(32, new LEDCoordinate(3746, 309));
        defaultLedMatrix.put(33, new LEDCoordinate(3630, 306));
        defaultLedMatrix.put(34, new LEDCoordinate(3514, 306));
        defaultLedMatrix.put(35, new LEDCoordinate(3398, 306));
        defaultLedMatrix.put(36, new LEDCoordinate(3282, 306));
        defaultLedMatrix.put(37, new LEDCoordinate(3174, 306));
        defaultLedMatrix.put(38, new LEDCoordinate(3058, 306));
        defaultLedMatrix.put(39, new LEDCoordinate(2942, 306));
        defaultLedMatrix.put(40, new LEDCoordinate(2826, 306));
        defaultLedMatrix.put(41, new LEDCoordinate(2710, 306));
        defaultLedMatrix.put(42, new LEDCoordinate(2594, 306));
        defaultLedMatrix.put(43, new LEDCoordinate(2478, 306));
        defaultLedMatrix.put(44, new LEDCoordinate(2362, 306));
        defaultLedMatrix.put(45, new LEDCoordinate(2246, 306));
        defaultLedMatrix.put(46, new LEDCoordinate(2130, 306));
        defaultLedMatrix.put(47, new LEDCoordinate(2014, 306));
        defaultLedMatrix.put(48, new LEDCoordinate(1886, 306));
        defaultLedMatrix.put(49, new LEDCoordinate(1770, 306));
        defaultLedMatrix.put(50, new LEDCoordinate(1654, 306));
        defaultLedMatrix.put(51, new LEDCoordinate(1538, 306));
        defaultLedMatrix.put(52, new LEDCoordinate(1422, 306));
        defaultLedMatrix.put(53, new LEDCoordinate(1306, 306));
        defaultLedMatrix.put(54, new LEDCoordinate(1190, 306));
        defaultLedMatrix.put(55, new LEDCoordinate(1074, 306));
        defaultLedMatrix.put(56, new LEDCoordinate(958, 306));
        defaultLedMatrix.put(57, new LEDCoordinate(842, 306));
        defaultLedMatrix.put(58, new LEDCoordinate(726, 306));
        defaultLedMatrix.put(59, new LEDCoordinate(610, 306));
        defaultLedMatrix.put(60, new LEDCoordinate(494, 306));
        defaultLedMatrix.put(61, new LEDCoordinate(378, 306));
        defaultLedMatrix.put(62, new LEDCoordinate(262, 306));
        defaultLedMatrix.put(63, new LEDCoordinate(146, 306));
        defaultLedMatrix.put(64, new LEDCoordinate(30, 306));
        defaultLedMatrix.put(65, new LEDCoordinate(30, 306));
        defaultLedMatrix.put(66, new LEDCoordinate(30, 390));
        defaultLedMatrix.put(67, new LEDCoordinate(30, 479));
        defaultLedMatrix.put(68, new LEDCoordinate(30, 568));
        defaultLedMatrix.put(69, new LEDCoordinate(30, 661));
        defaultLedMatrix.put(70, new LEDCoordinate(30, 750));
        defaultLedMatrix.put(71, new LEDCoordinate(30, 839));
        defaultLedMatrix.put(72, new LEDCoordinate(30, 928));
        defaultLedMatrix.put(73, new LEDCoordinate(30, 1017));
        defaultLedMatrix.put(74, new LEDCoordinate(30, 1106));
        defaultLedMatrix.put(75, new LEDCoordinate(30, 1195));
        defaultLedMatrix.put(76, new LEDCoordinate(30, 1284));
        defaultLedMatrix.put(77, new LEDCoordinate(30, 1373));
        defaultLedMatrix.put(78, new LEDCoordinate(30, 1462));
        defaultLedMatrix.put(79, new LEDCoordinate(30, 1551));
        defaultLedMatrix.put(80, new LEDCoordinate(30, 1622));
        defaultLedMatrix.put(81, new LEDCoordinate(30, 1727));
        defaultLedMatrix.put(82, new LEDCoordinate(30, 1816));
        defaultLedMatrix.put(83, new LEDCoordinate(30, 1590));
        defaultLedMatrix.put(84, new LEDCoordinate(128, 1590));
        defaultLedMatrix.put(85, new LEDCoordinate(226, 1590));
        defaultLedMatrix.put(86, new LEDCoordinate(324, 1590));
        defaultLedMatrix.put(87, new LEDCoordinate(422, 1590));
        defaultLedMatrix.put(88, new LEDCoordinate(520, 1590));
        defaultLedMatrix.put(89, new LEDCoordinate(618, 1590));
        defaultLedMatrix.put(90, new LEDCoordinate(716, 1590));
        defaultLedMatrix.put(91, new LEDCoordinate(814, 1590));
        defaultLedMatrix.put(92, new LEDCoordinate(912, 1590));
        defaultLedMatrix.put(93, new LEDCoordinate(1010, 1590));
        defaultLedMatrix.put(94, new LEDCoordinate(1108, 1590));
        defaultLedMatrix.put(95, new LEDCoordinate(1206, 1590));

        return defaultLedMatrix;

    }

}
