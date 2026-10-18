/*
  CubeLutToneMapTest.java

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
package org.dpsoftware.lut;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CubeLutToneMap}.
 * <p>
 * Note: {@link CubeLutToneMap} reads the LUT filename from
 * {@code MainSingleton.getInstance().config.getCubeLut()} at class-load time,
 * so the static must be mocked before the class is first touched.
 */
class CubeLutToneMapTest {

    @TempDir
    Path tempDir;

    private static final MainSingleton mockedInstance;
    private static final MockedStatic<MainSingleton> mockedMainSingleton;

    static {
        mockedInstance = new MainSingleton();
        mockedInstance.config = new Configuration();
        mockedMainSingleton = Mockito.mockStatic(MainSingleton.class);
        mockedMainSingleton.when(MainSingleton::getInstance).thenReturn(mockedInstance);
    }

    @AfterAll
    static void tearDown() {
        if (mockedMainSingleton != null) {
            mockedMainSingleton.close();
        }
    }

    @Test
    void interpolationMatchesReferenceForFractionalAndClampedInputs() throws Exception {
        int size = 5;
        float[] data = new float[size * size * size * 3];
        java.util.Random random = new java.util.Random(17);
        for (int i = 0; i < data.length; i++) {
            data[i] = random.nextFloat() * 1.4f - 0.2f;
        }
        var constructor = CubeLutToneMap.ToneMapper.class.getDeclaredConstructor(float[].class, int.class);
        constructor.setAccessible(true);
        var mapper = constructor.newInstance(data, size);
        float[] actual = new float[3];
        for (int sample = 0; sample < 2000; sample++) {
            float r = sample == 0 ? 0 : random.nextFloat() * 355 - 50;
            float g = sample == 0 ? 255 : random.nextFloat() * 355 - 50;
            float b = random.nextFloat() * 355 - 50;
            mapper.lookup(r, g, b, actual);
            float[] coordinates = {Math.clamp(r, 0, 255) / 255f * (size - 1),
                    Math.clamp(g, 0, 255) / 255f * (size - 1),
                    Math.clamp(b, 0, 255) / 255f * (size - 1)};
            for (int ch = 0; ch < 3; ch++) {
                float expected = 0;
                for (int z = 0; z < 2; z++) {
                    for (int y = 0; y < 2; y++) {
                        for (int x = 0; x < 2; x++) {
                            int ix = Math.min((int) coordinates[0] + x, size - 1);
                            int iy = Math.min((int) coordinates[1] + y, size - 1);
                            int iz = Math.min((int) coordinates[2] + z, size - 1);
                            float tx = coordinates[0] - (int) coordinates[0];
                            float ty = coordinates[1] - (int) coordinates[1];
                            float tz = coordinates[2] - (int) coordinates[2];
                            expected += data[(iz * size * size + iy * size + ix) * 3 + ch]
                                    * (x == 0 ? 1 - tx : tx) * (y == 0 ? 1 - ty : ty)
                                    * (z == 0 ? 1 - tz : tz);
                        }
                    }
                }
                org.junit.jupiter.api.Assertions.assertEquals(Math.clamp(expected, 0, 1) * 255,
                        actual[ch], 0.0001f);
            }
        }
    }

    @Test
    void cachedByteCoordinatesMatchOriginalFloatInterpolationExactly() throws Exception {
        var constructor = CubeLutToneMap.ToneMapper.class.getDeclaredConstructor(float[].class, int.class);
        constructor.setAccessible(true);
        java.util.Random random = new java.util.Random(29);
        float[] actual = new float[3];
        float[] fractional = new float[3];
        for (int size : new int[]{1, 2, 5, 17, 33, 65}) {
            float[] data = new float[size * size * size * 3];
            for (int i = 0; i < data.length; i++) {
                data[i] = random.nextFloat() * 1.4f - 0.2f;
            }
            var mapper = constructor.newInstance(data, size);
            for (int value = 0; value < 256; value++) {
                // Cover every possible channel coordinate, including all combinations of endpoints.
                for (int edge : new int[]{0, 255, value}) {
                    assertOriginalLookup(mapper, data, size, value, edge, edge, actual, fractional);
                    assertOriginalLookup(mapper, data, size, edge, value, edge, actual, fractional);
                    assertOriginalLookup(mapper, data, size, edge, edge, value, actual, fractional);
                }
            }
            for (int sample = 0; sample < 2000; sample++) {
                int r = random.nextInt(355) - 50;
                int g = random.nextInt(355) - 50;
                int b = random.nextInt(355) - 50;
                assertOriginalLookup(mapper, data, size, r, g, b, actual, fractional);
                float rf = r + random.nextFloat();
                float gf = g + random.nextFloat();
                float bf = b + random.nextFloat();
                mapper.lookup(rf, gf, bf, fractional);
                assertArrayEquals(originalLookup(data, size, rf, gf, bf), fractional);
            }
        }
        var disabled = constructor.newInstance(null, 0);
        disabled.lookup(-25, 128, 300, actual);
        assertArrayEquals(new float[]{-25, 128, 300}, actual);
    }

    private static void assertOriginalLookup(CubeLutToneMap.ToneMapper mapper, float[] data, int size,
                                             int r, int g, int b, float[] actual, float[] fractional) {
        mapper.lookup(r, g, b, actual);
        mapper.lookup((float) r, (float) g, (float) b, fractional);
        float[] expected = originalLookup(data, size, r, g, b);
        assertArrayEquals(expected, actual);
        assertArrayEquals(expected, fractional);
    }

    // Independent copy of the original interpolation and indexing, preserving its operation order.
    private static float[] originalLookup(float[] data, int size, float r, float g, float b) {
        float fx = Math.clamp(r, 0, 255) / 255f * (size - 1);
        float fy = Math.clamp(g, 0, 255) / 255f * (size - 1);
        float fz = Math.clamp(b, 0, 255) / 255f * (size - 1);
        int x0 = Math.min((int) fx, size - 1), y0 = Math.min((int) fy, size - 1), z0 = Math.min((int) fz, size - 1);
        int x1 = Math.min(x0 + 1, size - 1), y1 = Math.min(y0 + 1, size - 1), z1 = Math.min(z0 + 1, size - 1);
        float tx = fx - x0, ty = fy - y0, tz = fz - z0;
        float[] out = new float[3];
        for (int ch = 0; ch < 3; ch++) {
            float c000 = data[(z0 * size * size + y0 * size + x0) * 3 + ch];
            float c100 = data[(z0 * size * size + y0 * size + x1) * 3 + ch];
            float c010 = data[(z0 * size * size + y1 * size + x0) * 3 + ch];
            float c110 = data[(z0 * size * size + y1 * size + x1) * 3 + ch];
            float c001 = data[(z1 * size * size + y0 * size + x0) * 3 + ch];
            float c101 = data[(z1 * size * size + y0 * size + x1) * 3 + ch];
            float c011 = data[(z1 * size * size + y1 * size + x0) * 3 + ch];
            float c111 = data[(z1 * size * size + y1 * size + x1) * 3 + ch];
            float c00 = c000 * (1 - tx) + c100 * tx;
            float c10 = c010 * (1 - tx) + c110 * tx;
            float c01 = c001 * (1 - tx) + c101 * tx;
            float c11 = c011 * (1 - tx) + c111 * tx;
            float c0 = c00 * (1 - ty) + c10 * ty;
            float c1 = c01 * (1 - ty) + c11 * ty;
            out[ch] = Math.clamp(c0 * (1 - tz) + c1 * tz, 0, 1) * 255;
        }
        return out;
    }

    // --- lookup ---

    @Test
    void lookup_blackInputReturnsBlack() {
        int[] result = CubeLutToneMap.lookup(0, 0, 0);
        assertEquals(3, result.length, "Result must have 3 channels");
        assertEquals(0, result[0], "Black input should map to black");
        assertEquals(0, result[1], "Black input should map to black");
        assertEquals(0, result[2], "Black input should map to black");
    }

    @Test
    void lookup_returnsThreeChannels() {
        int[] result = CubeLutToneMap.lookup(128, 64, 200);
        assertNotNull(result, "Result must not be null");
        assertEquals(3, result.length, "Result must have 3 channels");
    }

    @Test
    void lookup_allChannelsInValidRange() {
        int[][] inputs = {
                {0, 0, 0}, {255, 255, 255}, {128, 128, 128},
                {10, 200, 50}, {200, 10, 50}, {50, 200, 10},
                {1, 1, 1}, {254, 254, 254}
        };
        for (int[] input : inputs) {
            int[] result = CubeLutToneMap.lookup(input[0], input[1], input[2]);
            for (int v : result) {
                assertTrue(v >= 0 && v <= 255,
                        "Channel value " + v + " out of range for input "
                                + input[0] + "," + input[1] + "," + input[2]);
            }
        }
    }

    @Test
    void lookup_whiteInputProducesHighChannels() {
        int[] result = CubeLutToneMap.lookup(255, 255, 255);
        assertTrue(result[0] > 200, "White red channel should be high, got " + result[0]);
        assertTrue(result[1] > 200, "White green channel should be high, got " + result[1]);
        assertTrue(result[2] > 200, "White blue channel should be high, got " + result[2]);
    }

    @Test
    void lookup_interpolationIsMonotonic() {
        int[] low = CubeLutToneMap.lookup(30, 30, 30);
        int[] mid = CubeLutToneMap.lookup(128, 128, 128);
        int[] high = CubeLutToneMap.lookup(220, 220, 220);
        assertTrue(low[0] < mid[0], "LUT should be monotonic: low red " + low[0] + " < mid red " + mid[0]);
        assertTrue(mid[0] < high[0], "LUT should be monotonic: mid red " + mid[0] + " < high red " + high[0]);
        assertTrue(low[1] < mid[1], "LUT should be monotonic: low green " + low[1] + " < mid green " + mid[1]);
        assertTrue(mid[1] < high[1], "LUT should be monotonic: mid green " + mid[1] + " < high green " + high[1]);
    }

    @Test
    void lookup_boundaryValue255Handled() {
        int[] result = CubeLutToneMap.lookup(255, 255, 255);
        assertNotNull(result);
        for (int v : result) {
            assertTrue(v >= 0 && v <= 255, "Boundary 255 input must produce valid output");
        }
    }

    @Test
    void lookup_boundaryValue0Handled() {
        int[] result = CubeLutToneMap.lookup(0, 0, 0);
        assertNotNull(result);
        for (int v : result) {
            assertTrue(v >= 0 && v <= 255, "Boundary 0 input must produce valid output");
        }
    }

    @Test
    void lookup_pureRedInput() {
        int[] result = CubeLutToneMap.lookup(255, 0, 0);
        assertEquals(3, result.length);
        for (int v : result) {
            assertTrue(v >= 0 && v <= 255, "Pure red input must produce valid output");
        }
    }

    @Test
    void lookup_pureGreenInput() {
        int[] result = CubeLutToneMap.lookup(0, 255, 0);
        assertEquals(3, result.length);
        for (int v : result) {
            assertTrue(v >= 0 && v <= 255, "Pure green input must produce valid output");
        }
    }

    @Test
    void lookup_pureBlueInput() {
        int[] result = CubeLutToneMap.lookup(0, 0, 255);
        assertEquals(3, result.length);
        for (int v : result) {
            assertTrue(v >= 0 && v <= 255, "Pure blue input must produce valid output");
        }
    }

    @Test
    void lookup_intermediateValuesDiffer() {
        int[] a = CubeLutToneMap.lookup(50, 50, 50);
        int[] b = CubeLutToneMap.lookup(150, 150, 150);
        boolean differs = a[0] != b[0] || a[1] != b[1] || a[2] != b[2];
        assertTrue(differs, "Different grayscale inputs should produce different outputs");
    }

    // --- parseCube (package-private, tested directly) ---

    @Test
    void parseCube_parsesLutSizeAndData() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# comment line\n");
        sb.append("TITLE \"test LUT\"\n");
        sb.append("LUT_3D_SIZE 2\n");
        sb.append("DOMAIN_MIN 0.0 0.0 0.0\n");
        sb.append("DOMAIN_MAX 1.0 1.0 1.0\n");
        // 8 entries (2^3), each R G B in [0,1]
        for (int i = 0; i < 8; i++) {
            float v = i / 7f;
            sb.append(v).append(' ').append(v).append(' ').append(v).append('\n');
        }
        CubeLutToneMap.parseCube(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        // lookup must work after parsing (identity-ish LUT)
        int[] result = CubeLutToneMap.lookup(128, 128, 128);
        assertNotNull(result);
        assertEquals(3, result.length);
        for (int v : result) {
            assertTrue(v >= 0 && v <= 255, "Parsed LUT must produce valid output");
        }
    }

    @Test
    void parseCube_incompleteDataThrows() {
        String sb = "LUT_3D_SIZE 2\n" +
                // only 4 of the required 8 entries
                "0.1 0.1 0.1\n" +
                "0.2 0.2 0.2\n" +
                "0.3 0.3 0.3\n" +
                "0.4 0.4 0.4\n";
        assertThrows(Exception.class,
                () -> CubeLutToneMap.parseCube(new ByteArrayInputStream(sb.getBytes(StandardCharsets.UTF_8))),
                "Incomplete LUT data should throw");
    }

    @Test
    void parseCube_skipsCommentsAndHeaders() throws Exception {
        String sb = "# a comment\n\n" +
                "TITLE \"ignored\"\n" +
                "DOMAIN_MIN 0 0 0\n" +
                "LUT_3D_SIZE 1\n" +
                "0.5 0.6 0.7\n";
        CubeLutToneMap.parseCube(new ByteArrayInputStream(sb.getBytes(StandardCharsets.UTF_8)));
        int[] result = CubeLutToneMap.lookup(100, 100, 100);
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    // --- listAvailableLuts ---

    @Test
    void listAvailableLuts_returnsResourceLuts() {
        java.util.List<String> luts = CubeLutToneMap.listAvailableLuts();
        assertNotNull(luts, "listAvailableLuts must not return null");
        assertFalse(luts.isEmpty(), "At least the bundled LUT resources should be listed");
        // Bundled resources are listed with their full filenames.
        assertTrue(luts.contains("HDR to SDR standard.cube"),
                "Bundled default LUT should be listed, got: " + luts);
        // Sorted and deduplicated.
        java.util.List<String> sorted = new java.util.ArrayList<>(luts);
        java.util.Collections.sort(sorted);
        assertEquals(new java.util.HashSet<>(luts).size(), luts.size(), "LUT list must have no duplicates");
    }

    // --- refresh ---

    @Test
    void refresh_sameNameIsNoOp() {
        // cubeLut unset -> resolveLutName() returns DEFAULT_CUBE_LUT, which is already loaded at class init.
        // refresh() must not throw and must keep the LUT functional.
        CubeLutToneMap.refresh();
        int[] result = CubeLutToneMap.lookup(128, 128, 128);
        assertNotNull(result);
        assertEquals(3, result.length);
        for (int v : result) {
            assertTrue(v >= 0 && v <= 255, "LUT must remain functional after refresh with unchanged name");
        }
    }

    @Test
    void refresh_changedNameReloadsLut() {
        String original = mockedInstance.config.getCubeLut();
        try {
            mockedInstance.config.setCubeLut("HDR to SDR standard.cube");
            CubeLutToneMap.refresh();
            int[] result = CubeLutToneMap.lookup(255, 255, 255);
            assertNotNull(result);
            assertEquals(3, result.length);
            for (int v : result) {
                assertTrue(v >= 0 && v <= 255, "Reloaded LUT must produce valid output");
            }
        } finally {
            mockedInstance.config.setCubeLut(original);
            CubeLutToneMap.refresh();
        }
    }

    @Test
    void refresh_missingFileFallsBackToConfigPathOrIdentity() {
        String original = mockedInstance.config.getCubeLut();
        try {
            // A name that exists in neither classpath resources nor the config path.
            mockedInstance.config.setCubeLut("does_not_exist_lut.cube");
            CubeLutToneMap.refresh();
            // Must not throw; lookup must still return valid values (identity fallback or previous LUT).
            int[] result = CubeLutToneMap.lookup(100, 150, 200);
            assertNotNull(result);
            assertEquals(3, result.length);
            for (int v : result) {
                assertTrue(v >= 0 && v <= 255, "Missing LUT must still yield valid output");
            }
        } finally {
            mockedInstance.config.setCubeLut(original);
            CubeLutToneMap.refresh();
        }
    }

}
