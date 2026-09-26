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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

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
    void scanJarForCubeLuts_supportsWindowsInstallPathWithSpaces() throws IOException {
        Path installDir = Files.createDirectory(tempDir.resolve("Firefly Luciferin"));
        Path jarPath = installDir.resolve("FireflyLuciferin-jar-with-dependencies.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("org/dpsoftware/lut/1000nits_HDR-to-SDR.cube"));
            jar.closeEntry();
        }

        assertEquals(java.util.List.of("1000nits_HDR-to-SDR.cube"),
                CubeLutToneMap.scanJarForCubeLuts(jarPath.toFile()));
    }

    @Test
    void listAvailableLuts_returnsResourceLuts() {
        java.util.List<String> luts = CubeLutToneMap.listAvailableLuts();
        assertNotNull(luts, "listAvailableLuts must not return null");
        assertFalse(luts.isEmpty(), "At least the bundled LUT resources should be listed");
        // Bundled resources are listed with their full filenames.
        assertTrue(luts.contains("1000nits_HDR-to-SDR.cube"),
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
            mockedInstance.config.setCubeLut("1000nits_HDR-to-SDR.cube");
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
