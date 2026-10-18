/*
  WebRtcToneMapTest.java

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
import org.dpsoftware.config.Constants;
import org.dpsoftware.lut.CubeLutToneMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class WebRtcToneMapTest {
    private Configuration previousConfig;

    @BeforeEach
    void setUp() {
        previousConfig = MainSingleton.getInstance().config;
        MainSingleton.getInstance().config = new Configuration();
    }

    @AfterEach
    void tearDown() {
        MainSingleton.getInstance().config = previousConfig;
        CubeLutToneMap.refresh();
    }

    @Test
    void packsPaddedRowsWithoutChangingCaptureBuffer() {
        ByteBuffer source = ByteBuffer.allocateDirect(3 * 12);
        for (int i = 0; i < source.capacity(); i++) source.put(i, (byte) i);
        source.position(5);
        ByteBuffer target = ByteBuffer.allocate(28).position(4);
        WebRtcStreamer.copyPackedPixels(source, target, 2, 3);
        assertEquals(5, source.position());
        assertEquals(36, source.limit());
        assertEquals(28, target.position());
        for (int row = 0; row < 3; row++) {
            for (int byteInRow = 0; byteInRow < 8; byteInRow++) {
                assertEquals((byte) (row * 12 + byteInRow), target.get(4 + row * 8 + byteInRow));
            }
        }
        for (int i = 0; i < source.capacity(); i++) assertEquals((byte) i, source.get(i));
    }

    @Test
    void packsUnpaddedRowsAndAcceptsMissingFinalPadding() {
        byte[] pixels = new byte[24];
        for (int i = 0; i < pixels.length; i++) pixels[i] = (byte) i;
        ByteBuffer source = ByteBuffer.wrap(pixels).asReadOnlyBuffer().position(5);
        ByteBuffer packed = ByteBuffer.allocate(24);
        WebRtcStreamer.copyPackedPixels(source, packed, 2, 3);
        assertArrayEquals(pixels, packed.array());
        assertEquals(5, source.position());
        // Three rows of two pixels with one padding pixel; last padding pixel is omitted.
        ByteBuffer padded = ByteBuffer.allocate(32);
        for (int i = 0; i < 32; i++) padded.put(i, (byte) i);
        ByteBuffer output = ByteBuffer.allocate(24);
        WebRtcStreamer.copyPackedPixels(padded, output, 2, 3);
        for (int row = 0; row < 3; row++) {
            for (int b = 0; b < 8; b++) assertEquals(padded.get(row * 12 + b), output.get(row * 8 + b));
        }
    }

    @Test
    void rejectsInvalidFrameBeforeWritingAndPreservesPreviewDimensions() {
        ByteBuffer source = ByteBuffer.allocate(20), target = ByteBuffer.allocate(24);
        assertThrows(IllegalArgumentException.class, () -> WebRtcStreamer.copyPackedPixels(source, target, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> WebRtcStreamer.copyPackedPixels(source, target, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> WebRtcStreamer.copyPackedPixels(source, target, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(0, target.position());
        assertArrayEquals(new int[]{960, 540}, WebRtcStreamer.previewSize(1920, 1080));
        assertArrayEquals(new int[]{960, 540}, WebRtcStreamer.previewSize(480, 270));
        assertArrayEquals(new int[]{720, 540}, WebRtcStreamer.previewSize(640, 480));
    }

    @Test
    void mapsAllPackedFormatsWithoutChangingAlphaPositionOrSource() {
        var config = MainSingleton.getInstance().config;
        String previous = config.getCubeLut();
        try {
            config.setCubeLut("HDR to SDR standard.cube");
            CubeLutToneMap.refresh();
            assertTrue(CubeLutToneMap.snapshot().isAvailable());
            int[] expected = CubeLutToneMap.lookup(100, 150, 200);
            for (String format : new String[]{"BGRx", "BGRA", "xRGB", "ARGB", "RGBx", "RGBA", "xBGR", "ABGR"}) {
                int r = format.indexOf('R'), g = format.indexOf('G'), b = format.indexOf('B');
                byte[] source = new byte[12];
                java.util.Arrays.fill(source, (byte) 73);
                for (int offset : new int[]{4, 8}) {
                    source[offset + r] = 100;
                    source[offset + g] = (byte) 150;
                    source[offset + b] = (byte) 200;
                }
                byte[] original = source.clone();
                ByteBuffer copy = ByteBuffer.allocate(12).put(source).position(4);
                WebRtcStreamer.applyLutToneMap(copy, 1, 2, format);
                assertEquals(4, copy.position());
                assertArrayEquals(original, source);
                for (int offset : new int[]{4, 8}) {
                    assertEquals(expected[0], copy.get(offset + r) & 255, format);
                    assertEquals(expected[1], copy.get(offset + g) & 255, format);
                    assertEquals(expected[2], copy.get(offset + b) & 255, format);
                    assertEquals(73, copy.get(offset + 6 - r - g - b), format);
                }
                assertEquals(73, copy.get(0));
            }
        } finally {
            config.setCubeLut(previous);
            CubeLutToneMap.refresh();
        }
    }

    @Test
    void disabledLutSkipsWritesAndExistingSnapshotSurvivesRefresh() {
        var config = MainSingleton.getInstance().config;
        String previous = config.getCubeLut();
        try {
            config.setCubeLut("HDR to SDR standard.cube");
            CubeLutToneMap.refresh();
            var snapshot = CubeLutToneMap.snapshot();
            float[] expected = CubeLutToneMap.lookup(100f, 150f, 200f);
            config.setCubeLut(Constants.DISABLED);
            CubeLutToneMap.refresh();
            assertFalse(CubeLutToneMap.snapshot().isAvailable());
            ByteBuffer readOnly = ByteBuffer.wrap(new byte[]{10, 20, 30, 40}).asReadOnlyBuffer();
            assertDoesNotThrow(() -> WebRtcStreamer.applyLutToneMap(readOnly, 1, 1, "BGRA"));
            float[] actual = new float[3];
            snapshot.lookup(100, 150, 200, actual);
            assertArrayEquals(expected, actual);
        } finally {
            config.setCubeLut(previous);
            CubeLutToneMap.refresh();
        }
    }
}
