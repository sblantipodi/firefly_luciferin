/*
  LEDCoordinateTest.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for pure-math methods in {@link LEDCoordinate}.
 */
class LEDCoordinateTest {

    // --- calculateTaleBorder ---
    // Formula: (TEST_CANVAS_BORDER_RATIO * width) / REFERENCE_RESOLUTION_FOR_SCALING_X
    // = (6 * width) / 3840

    @Test
    void calculateTaleBorder_referenceResolution() {
        assertEquals(6, LEDCoordinate.calculateTaleBorder(3840));
    }

    @Test
    void calculateTaleBorder_fullHD() {
        // (6 * 1920) / 3840 = 3
        assertEquals(3, LEDCoordinate.calculateTaleBorder(1920));
    }

    @Test
    void calculateTaleBorder_halfResolution() {
        // (6 * 960) / 3840 = 1
        assertEquals(1, LEDCoordinate.calculateTaleBorder(960));
    }

    @Test
    void calculateTaleBorder_zeroWidth() {
        assertEquals(0, LEDCoordinate.calculateTaleBorder(0));
    }

    // --- calculateBorders ---
    // Depends on aspect ratio detected from screen dimensions.
    // Uses CommonUtility.checkMonitorAspectRatio internally.

    @Test
    void calculateBorders_4by3ReturnsZero() {
        // 1920x1080 is 16:9, but 800x600 is 4:3 → returns 0
        LEDCoordinate coord = new LEDCoordinate();
        int border = coord.calculateBorders(800, 600);
        assertEquals(0, border);
    }

    @Test
    void calculateBorders_16by9() {
        // Formula: ((screenWidth * 480) / REFERENCE_RESOLUTION_FOR_SCALING_X) + 100
        // = ((1920 * 480) / 3840) + 100 = 240 + 100 = 340
        LEDCoordinate coord = new LEDCoordinate();
        int border = coord.calculateBorders(1920, 1080);
        assertEquals(340, border);
    }

    @Test
    void calculateBorders_16by9_4K() {
        // ((3840 * 480) / 3840) + 100 = 480 + 100 = 580
        LEDCoordinate coord = new LEDCoordinate();
        int border = coord.calculateBorders(3840, 2160);
        assertEquals(580, border);
    }

    @Test
    void calculateBorders_21by9() {
        // Formula: ((screenWidth * 440) / 3440) + 100
        // 2560x1080 → ((2560 * 440) / 3440) + 100 = (1126400/3440) + 100 = 327 + 100 = 427
        LEDCoordinate coord = new LEDCoordinate();
        int border = coord.calculateBorders(2560, 1080);
        assertEquals(427, border);
    }

    @Test
    void calculateBorders_unknownAspectRatioDefaultsTo16by9() {
        // 1000x1000 is unknown → defaults to AR_169 in checkMonitorAspectRatio
        LEDCoordinate coord = new LEDCoordinate();
        int border = coord.calculateBorders(1000, 1000);
        // ((1000 * 480) / 3840) + 100 = 125 + 100 = 225
        assertEquals(225, border);
    }

    // --- Constructor tests ---

    @Test
    void constructor_setsFields() {
        LEDCoordinate coord = new LEDCoordinate(10, 20, 30, 40, true, "TOP");
        assertEquals(10, coord.getX());
        assertEquals(20, coord.getY());
        assertEquals(30, coord.getWidth());
        assertEquals(40, coord.getHeight());
        assertTrue(coord.isGroupedLed());
        assertEquals("TOP", coord.getZone());
    }

    @Test
    void noArgConstructor_defaults() {
        LEDCoordinate coord = new LEDCoordinate();
        assertEquals(0, coord.getX());
        assertEquals(0, coord.getY());
        assertFalse(coord.isGroupedLed());
        assertNull(coord.getZone());
    }
}
