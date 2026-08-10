/*
  NetworkSingletonBuildAllLeadersRleEntriesTest.java

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
package org.dpsoftware.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@code NetworkSingleton.buildAllLeadersRleEntries()} — the closed-form
 * RLE builder for uncompressed mode (every LED is its own group of size 1).
 */
class NetworkSingletonBuildAllLeadersRleEntriesTest {

    @Test
    void buildAllLeadersRleEntries_zeroLeds_returnsEmpty() {
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(0);
        assertTrue(result.isEmpty());
    }

    @Test
    void buildAllLeadersRleEntries_singleLed() {
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(1);

        assertEquals(1, result.size());
        assertArrayEquals(new int[]{1, 1}, result.get(0));
    }

    @Test
    void buildAllLeadersRleEntries_fewLeds_singleEntry() {
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(50);

        assertEquals(1, result.size());
        assertArrayEquals(new int[]{50, 1}, result.get(0));
    }

    @Test
    void buildAllLeadersRleEntries_exactly255_singleEntry() {
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(255);

        assertEquals(1, result.size());
        assertArrayEquals(new int[]{255, 1}, result.get(0));
    }

    @Test
    void buildAllLeadersRleEntries_256_splittedIntoTwo() {
        // 256 = 255 + 1
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(256);

        assertEquals(2, result.size());
        assertArrayEquals(new int[]{255, 1}, result.get(0));
        assertArrayEquals(new int[]{1, 1}, result.get(1));
    }

    @Test
    void buildAllLeadersRleEntries_510_exactlyTwoFullChunks() {
        // 510 = 255 + 255
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(510);

        assertEquals(2, result.size());
        assertArrayEquals(new int[]{255, 1}, result.get(0));
        assertArrayEquals(new int[]{255, 1}, result.get(1));
    }

    @Test
    void buildAllLeadersRleEntries_511_threeChunks() {
        // 511 = 255 + 255 + 1
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(511);

        assertEquals(3, result.size());
        assertArrayEquals(new int[]{255, 1}, result.get(0));
        assertArrayEquals(new int[]{255, 1}, result.get(1));
        assertArrayEquals(new int[]{1, 1}, result.get(2));
    }

    @Test
    void buildAllLeadersRleEntries_1000_leds_correct_chunks() {
        // 1000 = 255 × 3 + 235
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(1000);

        assertEquals(4, result.size());
        assertArrayEquals(new int[]{255, 1}, result.get(0));
        assertArrayEquals(new int[]{255, 1}, result.get(1));
        assertArrayEquals(new int[]{255, 1}, result.get(2));
        assertArrayEquals(new int[]{235, 1}, result.get(3));
    }

    @Test
    void buildAllLeadersRleEntries_allEntriesHaveSizeOne() {
        // Every entry must have size=1 (all leaders mode)
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(730);

        for (int[] entry : result) {
            assertEquals(1, entry[1], "all-leaders mode means every group has size 1");
        }
    }

    @Test
    void buildAllLeadersRleEntries_sumOfCountsEqualsTotalLeds() {
        int totalLeds = 620;
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(totalLeds);

        int sumOfCounts = result.stream().mapToInt(e -> e[0]).sum();
        assertEquals(totalLeds, sumOfCounts, "sum of all counts must equal total LED count");
    }

    @Test
    void buildAllLeadersRleEntries_noEntryExceeds255Count() {
        List<int[]> result = NetworkSingleton.buildAllLeadersRleEntries(2000);

        for (int[] entry : result) {
            assertTrue(entry[0] <= 255, "no count entry may exceed 255 (firmware byte limit)");
        }
    }
}
