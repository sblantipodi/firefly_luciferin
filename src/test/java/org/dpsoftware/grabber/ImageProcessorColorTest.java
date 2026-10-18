/*
  ImageProcessorColorTest.java

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

  You should have received a copy of the GNU Lesser General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware.grabber;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.controllers.ColorCorrectionDialogController;
import org.dpsoftware.lut.CubeLutToneMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure-math color correction methods in {@link ImageProcessor}.
 * Uses MockedStatic to intercept MainSingleton.getInstance() without modifying production code.
 */
class ImageProcessorColorTest {

    private MainSingleton mockedInstance;
    private MockedStatic<MainSingleton> mockedMainSingleton;

    @BeforeEach
    void setUp() {
        mockedInstance = new MainSingleton();
        mockedInstance.config = new Configuration();
        // Default gamma 2.2
        mockedInstance.config.setGamma(2.2);
        mockedInstance.config.setEnableAutomaticGamma(false);
        mockedInstance.config.setLuminosityThreshold(0);
        mockedInstance.config.setNightLight(Constants.DISABLED);
        mockedInstance.config.setBrightnessLimiter(1.0f);
        mockedInstance.config.setSmoothingType(Constants.DISABLED);
        mockedMainSingleton = Mockito.mockStatic(MainSingleton.class);
        mockedMainSingleton.when(MainSingleton::getInstance).thenReturn(mockedInstance);
    }

    @AfterEach
    void tearDown() {
        mockedMainSingleton.close();
    }

    @Test
    void reusableLutBufferPreservesFractionalLedColorsAndPreviousResults() {
        String previousLut = mockedInstance.config.getCubeLut();
        boolean previousNightLight = GrabberSingleton.getInstance().isNightLightAuto();
        mockedInstance.config.setHueMap(ColorCorrectionDialogController.initHSLMap());
        GrabberSingleton.getInstance().setNightLightAuto(false);
        try {
            for (String lut : new String[]{Constants.DISABLED, "HDR to SDR standard.cube", "HDR to SDR high precision.cube"}) {
                mockedInstance.config.setCubeLut(lut);
                CubeLutToneMap.refresh();
                var mapper = CubeLutToneMap.snapshot();
                float[] scratch = new float[3];
                java.util.Random random = new java.util.Random(73);
                ColorFloat first = null;
                ColorFloat expectedFirst = null;
                for (int sample = 0; sample < 500; sample++) {
                    int count = 1 + random.nextInt(500);
                    int r = random.nextInt(255 * count + 1);
                    int g = random.nextInt(255 * count + 1);
                    int b = random.nextInt(255 * count + 1);
                    // Original pipeline: floating-point averages, LUT, HSL, then gamma.
                    float[] mapped = CubeLutToneMap.lookup((float) r / count, (float) g / count, (float) b / count);
                    ColorFloat expected = ImageProcessor.gammaCorrection(ImageProcessor.manageColorsFloat(
                            new ColorFloat(mapped[0], mapped[1], mapped[2])));
                    ColorFloat actual = ImageProcessor.correctColors(r, g, b, count, true, mapper, scratch);
                    assertEquals(expected, actual, lut);
                    assertEquals(expected, ImageProcessor.correctColors(r, g, b, count, true), lut);
                    assertEquals(ColorFloat.BLACK, ImageProcessor.correctColors(r, g, b, count, false, mapper, scratch));
                    if (sample == 0) {
                        first = actual;
                        expectedFirst = expected;
                    }
                }
                assertEquals(expectedFirst, first, "Reusing scratch must not mutate a previously returned color");
                if (!mapper.isAvailable()) {
                    assertEquals(ImageProcessor.correctColors(1, 2, 3, 7, true),
                            ImageProcessor.correctColors(1, 2, 3, 7, true, mapper, null));
                }
            }
        } finally {
            mockedInstance.config.setCubeLut(previousLut);
            CubeLutToneMap.refresh();
            GrabberSingleton.getInstance().setNightLightAuto(previousNightLight);
        }
    }

    @Test
    void frameLutSnapshotSurvivesSettingsChange() {
        String previousLut = mockedInstance.config.getCubeLut();
        mockedInstance.config.setHueMap(ColorCorrectionDialogController.initHSLMap());
        try {
            mockedInstance.config.setCubeLut("HDR to SDR standard.cube");
            CubeLutToneMap.refresh();
            var frameMapper = CubeLutToneMap.snapshot();
            float[] scratch = new float[3];
            ColorFloat expected = ImageProcessor.correctColors(1001, 1502, 2003, 11, true, frameMapper, scratch);
            mockedInstance.config.setCubeLut(Constants.DISABLED);
            CubeLutToneMap.refresh();
            assertEquals(expected, ImageProcessor.correctColors(1001, 1502, 2003, 11, true, frameMapper, scratch));
        } finally {
            mockedInstance.config.setCubeLut(previousLut);
            CubeLutToneMap.refresh();
        }
    }

    // --- removeBlueAndMakeItWarm ---

    @Test
    void removeBlueAndMakeItWarm_reducesBlueChannel() {
        ColorFloat input = new ColorFloat(200f, 180f, 160f);
        ColorFloat result = ImageProcessor.removeBlueAndMakeItWarm(input, 0.5f, 0.0f, 0.0f);

        assertTrue(result.b() < input.b(), "Blue should be reduced");
    }

    @Test
    void removeBlueAndMakeItWarm_fullBlueReductionZerosBlue() {
        ColorFloat input = new ColorFloat(128f, 128f, 128f);
        ColorFloat result = ImageProcessor.removeBlueAndMakeItWarm(input, 1.0f, 0.0f, 0.0f);

        assertEquals(0.0f, result.b(), 0.5f, "Blue should be zeroed with full reduction");
    }

    @Test
    void removeBlueAndMakeItWarm_brightColorBoostsRed() {
        // Bright color (avg > 0.5 normalized) → red boosted
        ColorFloat input = new ColorFloat(200f, 200f, 200f);
        ColorFloat result = ImageProcessor.removeBlueAndMakeItWarm(input, 0.5f, 0.2f, 0.1f);

        assertTrue(result.r() >= input.r(), "Red should be boosted for bright colors");
    }

    @Test
    void removeBlueAndMakeItWarm_darkColorDoesNotBoostRed() {
        // Dark color (avg <= 0.5 normalized) → no red boost
        ColorFloat input = new ColorFloat(80f, 80f, 80f);
        ColorFloat result = ImageProcessor.removeBlueAndMakeItWarm(input, 0.3f, 0.2f, 0.1f);

        assertEquals(input.r(), result.r(), 1.0f, "Red should not change for dark colors");
    }

    @Test
    void removeBlueAndMakeItWarm_resultInRange() {
        ColorFloat input = new ColorFloat(255f, 255f, 255f);
        ColorFloat result = ImageProcessor.removeBlueAndMakeItWarm(input, 0.8f, 0.5f, 0.4f);

        assertTrue(result.r() >= 0 && result.r() <= 256, "R should be in range");
        assertTrue(result.g() >= 0 && result.g() <= 256, "G should be in range");
        assertTrue(result.b() >= 0 && result.b() <= 256, "B should be in range");
    }

    // --- adjustLuminosityThreshold ---

    @Test
    void adjustLuminosityThreshold_darkColorBrightnessIncreased() {
        // Very dark input, threshold 0.5 → brightness forced to 0.5
        ColorFloat input = new ColorFloat(10f, 10f, 10f);
        ColorFloat result = ImageProcessor.adjustLuminosityThreshold(input, 0.5f);

        assertTrue(result.r() > input.r(), "Red should increase");
        assertTrue(result.g() > input.g(), "Green should increase");
        assertTrue(result.b() > input.b(), "Blue should increase");
    }

    @Test
    void adjustLuminosityThreshold_resultInRange() {
        ColorFloat input = new ColorFloat(50f, 60f, 70f);
        ColorFloat result = ImageProcessor.adjustLuminosityThreshold(input, 0.8f);

        assertTrue(result.r() >= 0 && result.r() <= 256, "R in range");
        assertTrue(result.g() >= 0 && result.g() <= 256, "G in range");
        assertTrue(result.b() >= 0 && result.b() <= 256, "B in range");
    }

    @Test
    void adjustLuminosityThreshold_zeroThresholdNoSignificantChange() {
        // Non-gray color with zero threshold — HSB round-trip should be close
        ColorFloat input = new ColorFloat(100f, 150f, 200f);
        ColorFloat result = ImageProcessor.adjustLuminosityThreshold(input, 0.0f);

        // Allow some tolerance for floating-point HSB round-trip
        assertEquals(input.r(), result.r(), 5.0f);
        assertEquals(input.g(), result.g(), 5.0f);
        assertEquals(input.b(), result.b(), 5.0f);
    }

    // --- adjustWhiteBalance ---

    @Test
    void adjustWhiteBalance_warmerTemperatureIncreasesRed() {
        ColorFloat input = new ColorFloat(128f, 128f, 128f);
        ColorFloat result = ImageProcessor.adjustWhiteBalance(input, 50); // positive = warmer

        assertTrue(result.r() >= input.r(), "Warmer should increase or maintain red");
    }

    @Test
    void adjustWhiteBalance_colderTemperatureDecreasesRed() {
        ColorFloat input = new ColorFloat(128f, 128f, 128f);
        ColorFloat result = ImageProcessor.adjustWhiteBalance(input, -30); // negative = colder

        assertTrue(result.r() <= input.r(), "Colder should decrease or maintain red");
    }

    @Test
    void adjustWhiteBalance_preservesLuminance() {
        ColorFloat input = new ColorFloat(200f, 150f, 100f);
        float originalLuminance = (input.r() * 0.299f + input.g() * 0.587f + input.b() * 0.114f) / 255.0f;

        ColorFloat result = ImageProcessor.adjustWhiteBalance(input, 40);
        float newLuminance = (result.r() * 0.299f + result.g() * 0.587f + result.b() * 0.114f) / 255.0f;

        assertEquals(originalLuminance, newLuminance, 0.05f, "Luminance should be approximately preserved");
    }

    @Test
    void adjustWhiteBalance_resultClamped() {
        ColorFloat input = new ColorFloat(255f, 255f, 255f);
        ColorFloat result = ImageProcessor.adjustWhiteBalance(input, 1000); // extreme value

        assertTrue(result.r() >= 0 && result.r() <= 255, "R clamped");
        assertTrue(result.g() >= 0 && result.g() <= 255, "G clamped");
        assertTrue(result.b() >= 0 && result.b() <= 255, "B clamped");
    }

    // --- getAverageForAllZones (Color[] version) ---
    // The formula is: sum(leds[zoneStart] .. leds[zoneEnd-1]) / ((zoneEnd - zoneStart) + 1)

    @Test
    void getAverageForAllZones_uniformColors() {
        Color[] leds = new Color[5];
        for (int i = 0; i < leds.length; i++) {
            leds[i] = new Color(100, 150, 200);
        }

        // zoneStart=0, zoneEnd=4 → sum indices 0..3 (4 colors), divide by 5
        Color avg = ImageProcessor.getAverageForAllZones(leds, 0, 4);

        // (100*4)/5 = 80, (150*4)/5 = 120, (200*4)/5 = 160
        assertEquals(80, avg.getRed());
        assertEquals(120, avg.getGreen());
        assertEquals(160, avg.getBlue());
    }

    @Test
    void getAverageForAllZones_singleElement() {
        Color[] leds = {new Color(255, 128, 64), new Color(0, 0, 0)};

        // zoneStart=0, zoneEnd=1 → sum index 0 only, divide by (1-0)+1 = 2
        Color avg = ImageProcessor.getAverageForAllZones(leds, 0, 1);

        assertEquals(127, avg.getRed());   // 255/2
        assertEquals(64, avg.getGreen());  // 128/2
        assertEquals(32, avg.getBlue());   // 64/2
    }

    @Test
    void getAverageForAllZones_twoElements() {
        Color[] leds = {
                new Color(200, 100, 50),
                new Color(100, 200, 150)
        };

        // zoneStart=0, zoneEnd=1 → sum index 0 only, divide by (1-0)+1 = 2
        Color avg = ImageProcessor.getAverageForAllZones(leds, 0, 1);

        assertEquals(100, avg.getRed());   // 200/2
        assertEquals(50, avg.getGreen());  // 100/2
        assertEquals(25, avg.getBlue());   // 50/2
    }

    // --- getAverageForAllZones (ColorFloat[] version) ---

    @Test
    void getAverageForAllZones_floatVersion() {
        ColorFloat[] leds = {
                new ColorFloat(100f, 150f, 200f),
                new ColorFloat(200f, 100f, 50f)
        };

        ColorFloat avg = ImageProcessor.getAverageForAllZones(leds);

        assertEquals(150f, avg.r(), 1.0f);
        assertEquals(125f, avg.g(), 1.0f);
        assertEquals(125f, avg.b(), 1.0f);
    }

    // --- gammaCorrection ---

    @Test
    void gammaCorrection_identityWhenGammaIs1() {
        mockedInstance.config.setGamma(1.0);
        ColorFloat input = new ColorFloat(128f, 100f, 200f);
        ColorFloat result = ImageProcessor.gammaCorrection(input);

        assertEquals(input.r(), result.r(), 2.0f);
        assertEquals(input.g(), result.g(), 2.0f);
        assertEquals(input.b(), result.b(), 2.0f);
    }

    @Test
    void gammaCorrection_highGammaDarkensColor() {
        mockedInstance.config.setGamma(3.0);
        ColorFloat input = new ColorFloat(200f, 200f, 200f);
        ColorFloat result = ImageProcessor.gammaCorrection(input);

        assertTrue(result.r() < input.r(), "Higher gamma should darken");
    }

    @Test
    void gammaCorrection_lowGammaLightensColor() {
        mockedInstance.config.setGamma(0.5);
        ColorFloat input = new ColorFloat(100f, 100f, 100f);
        ColorFloat result = ImageProcessor.gammaCorrection(input);

        assertTrue(result.r() > input.r(), "Lower gamma should lighten");
    }

    @Test
    void gammaCorrection_blackRemainsBlack() {
        mockedInstance.config.setGamma(2.2);
        ColorFloat input = ColorFloat.BLACK;
        ColorFloat result = ImageProcessor.gammaCorrection(input);

        assertEquals(0f, result.r(), 0.5f);
        assertEquals(0f, result.g(), 0.5f);
        assertEquals(0f, result.b(), 0.5f);
    }

    @Test
    void gammaCorrection_whiteRemainsWhite() {
        mockedInstance.config.setGamma(2.2);
        ColorFloat input = new ColorFloat(255f, 255f, 255f);
        ColorFloat result = ImageProcessor.gammaCorrection(input);

        assertEquals(255f, result.r(), 1.0f);
        assertEquals(255f, result.g(), 1.0f);
        assertEquals(255f, result.b(), 1.0f);
    }

    // --- updateAvgBrightness ---

    @Test
    void updateAvgBrightness_allWhite() {
        ColorFloat[] leds = {
                new ColorFloat(255f, 255f, 255f),
                new ColorFloat(255f, 255f, 255f)
        };
        ImageProcessor.updateAvgBrightness(leds);
        // No exception thrown
    }

    @Test
    void updateAvgBrightness_allBlack() {
        ColorFloat[] leds = {ColorFloat.BLACK};
        ImageProcessor.updateAvgBrightness(leds);
        // No exception thrown
    }
}
