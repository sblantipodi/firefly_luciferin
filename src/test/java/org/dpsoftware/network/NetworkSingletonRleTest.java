/*
  NetworkSingletonRleTest.java

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

import org.dpsoftware.LEDCoordinate;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NetworkSingleton#computeRleEntries(LinkedHashMap)}.
 * Tests the RLE compression algorithm used for LED group data.
 */
class NetworkSingletonRleTest {

    private static LEDCoordinate leaderLed() {
        LEDCoordinate coord = new LEDCoordinate();
        coord.setGroupedLed(false);
        return coord;
    }

    private static LEDCoordinate followerLed() {
        LEDCoordinate coord = new LEDCoordinate();
        coord.setGroupedLed(true);
        return coord;
    }

    @Test
    void computeRleEntries_nullMatrixReturnsEmpty() {
        List<int[]> result = NetworkSingleton.computeRleEntries(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void computeRleEntries_emptyMatrixReturnsEmpty() {
        LinkedHashMap<Integer, LEDCoordinate> empty = new LinkedHashMap<>();
        List<int[]> result = NetworkSingleton.computeRleEntries(empty);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void computeRleEntries_singleLeader() {
        LinkedHashMap<Integer, LEDCoordinate> matrix = new LinkedHashMap<>();
        matrix.put(1, leaderLed());
        List<int[]> result = NetworkSingleton.computeRleEntries(matrix);

        assertEquals(1, result.size());
        assertArrayEquals(new int[]{1, 1}, result.getFirst()); // 1 group of size 1
    }

    @Test
    void computeRleEntries_allLeaders() {
        LinkedHashMap<Integer, LEDCoordinate> matrix = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            matrix.put(i, leaderLed());
        }
        List<int[]> result = NetworkSingleton.computeRleEntries(matrix);

        assertEquals(1, result.size());
        assertArrayEquals(new int[]{5, 1}, result.getFirst()); // 5 groups of size 1
    }

    @Test
    void computeRleEntries_oneLeaderWithFollowers() {
        // 1 leader + 4 followers = group of size 5
        LinkedHashMap<Integer, LEDCoordinate> matrix = new LinkedHashMap<>();
        matrix.put(1, leaderLed());
        for (int i = 2; i <= 5; i++) {
            matrix.put(i, followerLed());
        }
        List<int[]> result = NetworkSingleton.computeRleEntries(matrix);

        assertEquals(1, result.size());
        assertArrayEquals(new int[]{1, 5}, result.getFirst()); // 1 group of size 5
    }

    @Test
    void computeRleEntries_mixedPattern() {
        // [leader + 2 followers] = group size 3, repeated 3 times → [3x3]
        LinkedHashMap<Integer, LEDCoordinate> matrix = new LinkedHashMap<>();
        for (int g = 0; g < 3; g++) {
            matrix.put(g * 3 + 1, leaderLed());
            matrix.put(g * 3 + 2, followerLed());
            matrix.put(g * 3 + 3, followerLed());
        }
        List<int[]> result = NetworkSingleton.computeRleEntries(matrix);

        assertEquals(1, result.size());
        assertArrayEquals(new int[]{3, 3}, result.getFirst()); // 3 groups of size 3
    }

    @Test
    void computeRleEntries_alternatingSizes() {
        // group sizes: [3, 3, 1, 1, 5, 5, 5] → [3x3], [2x1], [3x5]
        LinkedHashMap<Integer, LEDCoordinate> matrix = new LinkedHashMap<>();
        int idx = 1;

        // Two groups of size 3
        for (int g = 0; g < 2; g++) {
            matrix.put(idx++, leaderLed());
            matrix.put(idx++, followerLed());
            matrix.put(idx++, followerLed());
        }
        // Two groups of size 1
        matrix.put(idx++, leaderLed());
        matrix.put(idx++, leaderLed());
        // Three groups of size 5
        for (int g = 0; g < 3; g++) {
            matrix.put(idx++, leaderLed());
            for (int f = 0; f < 4; f++) matrix.put(idx++, followerLed());
        }

        List<int[]> result = NetworkSingleton.computeRleEntries(matrix);

        assertEquals(3, result.size());
        assertArrayEquals(new int[]{2, 3}, result.get(0));
        assertArrayEquals(new int[]{2, 1}, result.get(1));
        assertArrayEquals(new int[]{3, 5}, result.get(2));
    }

    @Test
    void computeRleEntries_largeGroupExceeds255() {
        // 1 leader + 300 followers = group of size 301 → split into {1, 255} + {1, 46}
        LinkedHashMap<Integer, LEDCoordinate> matrix = new LinkedHashMap<>();
        matrix.put(1, leaderLed());
        for (int i = 2; i <= 301; i++) {
            matrix.put(i, followerLed());
        }
        List<int[]> result = NetworkSingleton.computeRleEntries(matrix);

        assertEquals(2, result.size());
        assertArrayEquals(new int[]{1, 255}, result.get(0));
        assertArrayEquals(new int[]{1, 46}, result.get(1));
    }

    // --- Helpers ---

    @Test
    void computeRleEntries_manyConsecutiveGroupsHit255CountLimit() {
        // 300 groups of size 1 → should split into {255, 1} + {45, 1}
        LinkedHashMap<Integer, LEDCoordinate> matrix = new LinkedHashMap<>();
        for (int i = 1; i <= 300; i++) {
            matrix.put(i, leaderLed());
        }
        List<int[]> result = NetworkSingleton.computeRleEntries(matrix);

        assertEquals(2, result.size());
        assertArrayEquals(new int[]{255, 1}, result.get(0));
        assertArrayEquals(new int[]{45, 1}, result.get(1));
    }

    @Test
    void computeRleEntries_patternFromDocComment() {
        // [3x3],[1x8],[6x3] as described in the Javadoc example
        LinkedHashMap<Integer, LEDCoordinate> matrix = new LinkedHashMap<>();
        int idx = 1;

        // 3 groups of size 3
        for (int g = 0; g < 3; g++) {
            matrix.put(idx++, leaderLed());
            matrix.put(idx++, followerLed());
            matrix.put(idx++, followerLed());
        }
        // 1 group of size 8
        matrix.put(idx++, leaderLed());
        for (int f = 0; f < 7; f++) matrix.put(idx++, followerLed());
        // 6 groups of size 3
        for (int g = 0; g < 6; g++) {
            matrix.put(idx++, leaderLed());
            matrix.put(idx++, followerLed());
            matrix.put(idx++, followerLed());
        }

        List<int[]> result = NetworkSingleton.computeRleEntries(matrix);

        assertEquals(3, result.size());
        assertArrayEquals(new int[]{3, 3}, result.get(0));
        assertArrayEquals(new int[]{1, 8}, result.get(1));
        assertArrayEquals(new int[]{6, 3}, result.get(2));
    }
}
