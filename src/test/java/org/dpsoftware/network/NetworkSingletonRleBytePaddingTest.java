/*
  NetworkSingletonRleBytePaddingTest.java

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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@code NetworkSingleton.rleBytePadding()} — the firmware protocol
 * byte-limit splitter that breaks RLE entries whose size exceeds 255.
 */
class NetworkSingletonRleBytePaddingTest {

    @Test
    void rleBytePadding_sizeBelowLimit_noSplit() {
        List<int[]> rle = new ArrayList<>();
        NetworkSingleton.rleBytePadding(rle, 3, 120);

        assertEquals(1, rle.size());
        assertArrayEquals(new int[]{3, 120}, rle.getFirst());
    }

    @Test
    void rleBytePadding_sizeExactly255_noSplit() {
        List<int[]> rle = new ArrayList<>();
        NetworkSingleton.rleBytePadding(rle, 1, 255);

        assertEquals(1, rle.size());
        assertArrayEquals(new int[]{1, 255}, rle.getFirst());
    }

    @Test
    void rleBytePadding_sizeJustAbove255_splitsOnce() {
        // 256 = 255 + 1
        List<int[]> rle = new ArrayList<>();
        NetworkSingleton.rleBytePadding(rle, 1, 256);

        assertEquals(2, rle.size());
        assertArrayEquals(new int[]{1, 255}, rle.get(0));
        assertArrayEquals(new int[]{1, 1}, rle.get(1));
    }

    @Test
    void rleBytePadding_size511_splitsTwice() {
        // 511 = 255 + 255 + 1
        List<int[]> rle = new ArrayList<>();
        NetworkSingleton.rleBytePadding(rle, 2, 511);

        assertEquals(3, rle.size());
        assertArrayEquals(new int[]{2, 255}, rle.get(0));
        assertArrayEquals(new int[]{2, 255}, rle.get(1));
        assertArrayEquals(new int[]{2, 1}, rle.get(2));
    }

    @Test
    void rleBytePadding_size510_splitsTwiceExact() {
        // 510 = 255 + 255
        List<int[]> rle = new ArrayList<>();
        NetworkSingleton.rleBytePadding(rle, 1, 510);

        assertEquals(2, rle.size());
        assertArrayEquals(new int[]{1, 255}, rle.get(0));
        assertArrayEquals(new int[]{1, 255}, rle.get(1));
    }

    @Test
    void rleBytePadding_sizeExactlyMultipleOf255() {
        // 765 = 255 × 3
        List<int[]> rle = new ArrayList<>();
        NetworkSingleton.rleBytePadding(rle, 5, 765);

        assertEquals(3, rle.size());
        assertArrayEquals(new int[]{5, 255}, rle.get(0));
        assertArrayEquals(new int[]{5, 255}, rle.get(1));
        assertArrayEquals(new int[]{5, 255}, rle.get(2));
    }

    @Test
    void rleBytePadding_sizeZero_noEntry() {
        List<int[]> rle = new ArrayList<>();
        NetworkSingleton.rleBytePadding(rle, 10, 0);

        assertTrue(rle.isEmpty());
    }

    @Test
    void rleBytePadding_preservesCountAcrossAllEntries() {
        // count must be identical in every split entry
        List<int[]> rle = new ArrayList<>();
        NetworkSingleton.rleBytePadding(rle, 42, 400);

        // 400 = 255 + 145
        assertEquals(2, rle.size());
        for (int[] entry : rle) {
            assertEquals(42, entry[0]); // count preserved
        }
        assertEquals(255, rle.get(0)[1]);
        assertEquals(145, rle.get(1)[1]);
    }

    @Test
    void rleBytePadding_accumulatesOnExistingList() {
        List<int[]> rle = new ArrayList<>();
        rle.add(new int[]{1, 10}); // pre-existing entry

        NetworkSingleton.rleBytePadding(rle, 2, 300);

        // 300 = 255 + 45
        assertEquals(3, rle.size());
        assertArrayEquals(new int[]{1, 10}, rle.get(0)); // original untouched
        assertArrayEquals(new int[]{2, 255}, rle.get(1));
        assertArrayEquals(new int[]{2, 45}, rle.get(2));
    }

    @Test
    void rleBytePadding_largeSize_manySplits() {
        // 1000 = 255 × 3 + 235
        List<int[]> rle = new ArrayList<>();
        NetworkSingleton.rleBytePadding(rle, 7, 1000);

        assertEquals(4, rle.size());
        for (int i = 0; i < 3; i++) {
            assertArrayEquals(new int[]{7, 255}, rle.get(i));
        }
        assertArrayEquals(new int[]{7, 235}, rle.get(3));
    }
}
