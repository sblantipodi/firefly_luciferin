/*
  UpgradeManagerTest.java

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
package org.dpsoftware.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link UpgradeManager#versionNumberToNumber(String)}.
 */
class UpgradeManagerTest {

    @Test
    void versionNumberToNumber_basicVersions() {
        // Formula: Long.parseLong(major + 1_000_000) + Long.parseLong(minor + 1_000) + Long.parseLong(patch)
        assertEquals(21_281_004L, UpgradeManager.versionNumberToNumber("2.28.4"));
        assertEquals(11_001_000L, UpgradeManager.versionNumberToNumber("1.0.0"));
        assertEquals(31_501_010L, UpgradeManager.versionNumberToNumber("3.50.10"));
    }

    @Test
    void versionNumberToNumber_higherVersionYieldsLargerNumber() {
        long v1 = UpgradeManager.versionNumberToNumber("2.28.4");
        long v2 = UpgradeManager.versionNumberToNumber("2.29.0");
        long v3 = UpgradeManager.versionNumberToNumber("3.0.0");
        assertTrue(v1 < v2, "2.29.0 should be greater than 2.28.4");
        assertTrue(v2 < v3, "3.0.0 should be greater than 2.29.0");
    }

    @Test
    void versionNumberToNumber_minorBumpIncreasesValue() {
        long v1 = UpgradeManager.versionNumberToNumber("1.5.0");
        long v2 = UpgradeManager.versionNumberToNumber("1.6.0");
        assertTrue(v2 > v1, "Minor bump should increase the numeric value");
    }

    @Test
    void versionNumberToNumber_patchBumpIncreasesValue() {
        long v1 = UpgradeManager.versionNumberToNumber("1.0.3");
        long v2 = UpgradeManager.versionNumberToNumber("1.0.4");
        assertTrue(v2 > v1, "Patch bump should increase the numeric value");
    }

    @Test
    void versionNumberToNumber_majorBumpDominates() {
        long v1 = UpgradeManager.versionNumberToNumber("1.99.99");
        long v2 = UpgradeManager.versionNumberToNumber("2.0.0");
        assertTrue(v2 > v1, "Major bump should dominate over minor/patch");
    }

    @Test
    void versionNumberToNumber_threeDigitNumbers() {
        long v1 = UpgradeManager.versionNumberToNumber("10.20.30");
        long v2 = UpgradeManager.versionNumberToNumber("10.20.31");
        assertTrue(v2 > v1);
    }

    @Test
    void versionNumberToNumber_zeroPatch() {
        assertEquals(11_051_000L, UpgradeManager.versionNumberToNumber("1.5.0"));
    }

    @Test
    void versionNumberToNumber_sameVersionIsEqual() {
        long v1 = UpgradeManager.versionNumberToNumber("2.15.7");
        long v2 = UpgradeManager.versionNumberToNumber("2.15.7");
        assertEquals(v1, v2);
    }
}
