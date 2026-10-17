/*
  ImageProcessorMathTest.java

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
package org.dpsoftware.grabber;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pure-math methods in {@link ImageProcessor}:
 * black-bar detection and buffer stride calculation.
 * <p>
 * Note: {@link ImageProcessor} has a static field initialized from {@code MainSingleton}
 * at class-load, so the static must be mocked before the class is first touched.
 */
class ImageProcessorMathTest {

    private static final MainSingleton mockedInstance;
    private static final MockedStatic<MainSingleton> mockedMainSingleton;

    /**
     * {@link ImageProcessor} initializes a static field from {@code MainSingleton} at class-load,
     * so the static must be mocked before the class is ever initialized.
     */
    static {
        mockedInstance = new MainSingleton();
        mockedInstance.config = new Configuration();
        mockedInstance.config.setGamma(2.2);
        mockedMainSingleton = Mockito.mockStatic(MainSingleton.class);
        mockedMainSingleton.when(MainSingleton::getInstance).thenReturn(mockedInstance);
    }

    @AfterAll
    static void tearDown() {
        if (mockedMainSingleton != null) {
            mockedMainSingleton.close();
        }
    }

    // --- hasBlackBars ---
    // blackBorderMinimum = (50 * 95) / 100 = 48 (top & bottom)
    // detection requires: top >= 48, bottom >= 48, and center < 50

    @Test
    void hasBlackBars_letterboxDetected() {
        int[][] matrix = new int[3][50];
        for (int i = 0; i < 50; i++) {
            matrix[0][i] = 1;   // top fully black
            matrix[2][i] = 1;   // bottom fully black
            matrix[1][i] = (i < 40) ? 0 : 1; // center mostly content
        }
        assertTrue(ImageProcessor.hasBlackBars(matrix), "Letterbox bars should be detected");
    }

    @Test
    void hasBlackBars_topBorderNotBlackEnough() {
        int[][] matrix = new int[3][50];
        for (int i = 0; i < 50; i++) {
            matrix[0][i] = (i < 46) ? 1 : 0; // top = 46 < 47
            matrix[2][i] = 1;
            matrix[1][i] = 0;
        }
        assertFalse(ImageProcessor.hasBlackBars(matrix), "Top below threshold must not detect");
    }

    @Test
    void hasBlackBars_bottomBorderNotBlackEnough() {
        int[][] matrix = new int[3][50];
        for (int i = 0; i < 50; i++) {
            matrix[0][i] = 1;
            matrix[2][i] = (i < 46) ? 1 : 0; // bottom = 46 < 47
            matrix[1][i] = 0;
        }
        assertFalse(ImageProcessor.hasBlackBars(matrix), "Bottom below threshold must not detect");
    }

    @Test
    void hasBlackBars_fullyBlackScreenIsNotBars() {
        int[][] matrix = new int[3][50];
        for (int i = 0; i < 50; i++) {
            matrix[0][i] = 1;
            matrix[1][i] = 1; // center fully black
            matrix[2][i] = 1;
        }
        assertFalse(ImageProcessor.hasBlackBars(matrix), "A fully black screen is not letterbox/pillarbox");
    }

    @Test
    void hasBlackBars_noBlackAreas() {
        int[][] matrix = new int[3][50]; // all zeros
        assertFalse(ImageProcessor.hasBlackBars(matrix), "No black areas must not detect");
    }

    // --- getWidthPlusStride ---

    @Test
    void getWidthPlusStride_noStrideReturnsWidth() {
        IntBuffer buffer = IntBuffer.allocate(100 * 50);
        assertEquals(100, ImageProcessor.getWidthPlusStride(100, 50, buffer));
    }

    @Test
    void getWidthPlusStride_addsStrideFromCapacity() {
        // width 3440, height 1440, buffer already has a 1-pixel stride: capacity = (3440+1) * 1440
        IntBuffer buffer = IntBuffer.allocate(3441 * 1440);
        int widthPlusStride = ImageProcessor.getWidthPlusStride(3440, 1440, buffer);
        assertEquals(3441, widthPlusStride, "Stride should be added to the width");
    }

    @Test
    void getWidthPlusStride_strideIsIntegerWidthIncrement() {
        // capacity = (width + stride) * height with an integer stride of 4
        int width = 3840;
        int height = 2160;
        int stride = 4;
        IntBuffer buffer = IntBuffer.allocate((width + stride) * height);
        assertEquals(width + stride, ImageProcessor.getWidthPlusStride(width, height, buffer));
    }
}
