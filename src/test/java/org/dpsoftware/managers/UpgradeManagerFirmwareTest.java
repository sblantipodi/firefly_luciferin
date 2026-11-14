/*
  UpgradeManagerFirmwareTest.java

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UpgradeManager#versionNumberToNumber(String)}.
 * <p>
 * Algorithm: split on ".", then
 * Long.parseLong(major + "1_000_000") + Long.parseLong(minor + "1_000") + Long.parseLong(patch)
 * <p>
 * Requires exactly 3 parts (major.minor.patch).
 */
class UpgradeManagerFirmwareTest {

    // --- versionNumberToNumber ---

    @Test
    void versionNumberToNumber_baseCase() {
        // "5.12.10" → parseLong("51000000") + parseLong("121000") + parseLong("10")
        // = 51000000 + 121000 + 10 = 51121010
        assertEquals(51_121_010L, UpgradeManager.versionNumberToNumber("5.12.10"));
    }

    @Test
    void versionNumberToNumber_zeroVersion() {
        // "0.0.0" → parseLong("01000000") + parseLong("01000") + parseLong("0")
        // = 1000000 + 1000 + 0 = 1001000
        assertEquals(1_001_000L, UpgradeManager.versionNumberToNumber("0.0.0"));
    }

    @Test
    void versionNumberToNumber_highVersion() {
        // "9.99.99" → parseLong("91000000") + parseLong("991000") + parseLong("99")
        // = 91000000 + 991000 + 99 = 91991099
        assertEquals(91_991_099L, UpgradeManager.versionNumberToNumber("9.99.99"));
    }

    @Test
    void versionNumberToNumber_fourPartsStillWorks() {
        // "5.12.10.3" → only first 3 indices used, same as "5.12.10"
        assertEquals(51_121_010L, UpgradeManager.versionNumberToNumber("5.12.10.3"));
    }

    @Test
    void versionNumberToNumber_tooFewPartsThrows() {
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> UpgradeManager.versionNumberToNumber("5"));
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> UpgradeManager.versionNumberToNumber("5.12"));
    }

    @Test
    void versionNumberToNumber_orderingPreserved() {
        // Higher version → higher number
        long v1 = UpgradeManager.versionNumberToNumber("5.0.0");
        long v2 = UpgradeManager.versionNumberToNumber("5.1.0");
        long v3 = UpgradeManager.versionNumberToNumber("5.1.1");
        long v4 = UpgradeManager.versionNumberToNumber("6.0.0");
        assertTrue(v1 < v2);
        assertTrue(v2 < v3);
        assertTrue(v3 < v4);
    }

    @Test
    void versionNumberToNumber_minorDominatesPatch() {
        // Incrementing minor always increases the number more than patch
        long low = UpgradeManager.versionNumberToNumber("1.0.999");
        long high = UpgradeManager.versionNumberToNumber("1.1.0");
        assertTrue(low < high);
    }
}
