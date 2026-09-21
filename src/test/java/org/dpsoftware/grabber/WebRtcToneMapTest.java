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
    void mapsAllPackedFormatsWithoutChangingAlphaPositionOrSource() {
        var config = MainSingleton.getInstance().config;
        String previous = config.getCubeLut();
        try {
            config.setCubeLut("1000nits_HDR-to-SDR.cube");
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
            config.setCubeLut("1000nits_HDR-to-SDR.cube");
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
