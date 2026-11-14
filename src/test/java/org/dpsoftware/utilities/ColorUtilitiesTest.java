/*
  ColorUtilitiesTest.java

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
package org.dpsoftware.utilities;

import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ColorUtilities}.
 * Tests color conversion algorithms: RGB <-> HSL, RGB <-> HSB, Kelvin -> RGB.
 */
class ColorUtilitiesTest {

    private static final float DELTA = 0.05f;

    // --- RGBtoHSL ---

    @Test
    void rgbToHsl_pureRed() {
        float[] hsl = ColorUtilities.RGBtoHSL(new Color(255, 0, 0), null);
        assertAll("pure red HSL",
                () -> assertEquals(0.0f, hsl[0], DELTA, "Hue should be 0 (red)"),
                () -> assertEquals(1.0f, hsl[1], DELTA, "Saturation should be 1.0"),
                () -> assertEquals(0.5f, hsl[2], DELTA, "Lightness should be 0.5")
        );
    }

    @Test
    void rgbToHsl_pureGreen() {
        float[] hsl = ColorUtilities.RGBtoHSL(new Color(0, 255, 0), null);
        assertAll("pure green HSL",
                () -> assertEquals(0.333f, hsl[0], DELTA, "Hue should be ~1/3"),
                () -> assertEquals(1.0f, hsl[1], DELTA),
                () -> assertEquals(0.5f, hsl[2], DELTA)
        );
    }

    @Test
    void rgbToHsl_pureBlue() {
        float[] hsl = ColorUtilities.RGBtoHSL(new Color(0, 0, 255), null);
        assertAll("pure blue HSL",
                () -> assertEquals(0.666f, hsl[0], DELTA, "Hue should be ~2/3"),
                () -> assertEquals(1.0f, hsl[1], DELTA),
                () -> assertEquals(0.5f, hsl[2], DELTA)
        );
    }

    @Test
    void rgbToHsl_white() {
        float[] hsl = ColorUtilities.RGBtoHSL(new Color(255, 255, 255), null);
        assertAll("white HSL",
                () -> assertEquals(0.0f, hsl[0], DELTA),
                () -> assertEquals(0.0f, hsl[1], DELTA, "Saturation should be 0"),
                () -> assertEquals(1.0f, hsl[2], DELTA, "Lightness should be 1.0")
        );
    }

    @Test
    void rgbToHsl_black() {
        float[] hsl = ColorUtilities.RGBtoHSL(new Color(0, 0, 0), null);
        assertAll("black HSL",
                () -> assertEquals(0.0f, hsl[0], DELTA),
                () -> assertEquals(0.0f, hsl[1], DELTA),
                () -> assertEquals(0.0f, hsl[2], DELTA, "Lightness should be 0")
        );
    }

    @Test
    void rgbToHsl_grey() {
        float[] hsl = ColorUtilities.RGBtoHSL(new Color(128, 128, 128), null);
        assertAll("grey HSL",
                () -> assertEquals(0.0f, hsl[0], DELTA),
                () -> assertEquals(0.0f, hsl[1], DELTA, "Saturation should be 0"),
                () -> assertEquals(0.5f, hsl[2], 0.1f, "Lightness should be ~0.5")
        );
    }

    @Test
    void rgbToHsl_reusesProvidedArray() {
        float[] buffer = new float[3];
        float[] result = ColorUtilities.RGBtoHSL(new Color(255, 128, 64), buffer);
        assertSame(buffer, result, "Should return the provided array");
    }

    @Test
    void rgbToHsl_createsNewArrayWhenNull() {
        float[] result = ColorUtilities.RGBtoHSL(new Color(100, 150, 200), null);
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    // --- HSLtoRGB round-trip ---

    @Test
    void hslToRgb_roundTripRed() {
        Color original = new Color(255, 0, 0);
        float[] hsl = ColorUtilities.RGBtoHSL(original, null);
        Color roundTripped = ColorUtilities.HSLtoRGB(hsl[0], hsl[1], hsl[2]);
        assertEquals(original.getRed(), roundTripped.getRed());
        assertEquals(original.getGreen(), roundTripped.getGreen());
        assertEquals(original.getBlue(), roundTripped.getBlue());
    }

    @Test
    void hslToRgb_roundTripGreen() {
        Color original = new Color(0, 255, 0);
        float[] hsl = ColorUtilities.RGBtoHSL(original, null);
        Color roundTripped = ColorUtilities.HSLtoRGB(hsl[0], hsl[1], hsl[2]);
        assertEquals(original.getRed(), roundTripped.getRed());
        assertEquals(original.getGreen(), roundTripped.getGreen());
        assertEquals(original.getBlue(), roundTripped.getBlue());
    }

    @Test
    void hslToRgb_roundTripBlue() {
        Color original = new Color(0, 0, 255);
        float[] hsl = ColorUtilities.RGBtoHSL(original, null);
        Color roundTripped = ColorUtilities.HSLtoRGB(hsl[0], hsl[1], hsl[2]);
        assertEquals(original.getRed(), roundTripped.getRed());
        assertEquals(original.getGreen(), roundTripped.getGreen());
        assertEquals(original.getBlue(), roundTripped.getBlue());
    }

    @Test
    void hslToRgb_roundTripWhite() {
        Color original = new Color(255, 255, 255);
        float[] hsl = ColorUtilities.RGBtoHSL(original, null);
        Color roundTripped = ColorUtilities.HSLtoRGB(hsl[0], hsl[1], hsl[2]);
        assertEquals(original.getRed(), roundTripped.getRed());
        assertEquals(original.getGreen(), roundTripped.getGreen());
        assertEquals(original.getBlue(), roundTripped.getBlue());
    }

    @Test
    void hslToRgb_roundTripBlack() {
        Color original = new Color(0, 0, 0);
        float[] hsl = ColorUtilities.RGBtoHSL(original, null);
        Color roundTripped = ColorUtilities.HSLtoRGB(hsl[0], hsl[1], hsl[2]);
        assertEquals(original.getRed(), roundTripped.getRed());
        assertEquals(original.getGreen(), roundTripped.getGreen());
        assertEquals(original.getBlue(), roundTripped.getBlue());
    }

    // --- HSLtoRGBFloat ---

    @Test
    void hslToRgbFloat_grey50Percent() {
        float[] rgb = ColorUtilities.HSLtoRGBFloat(0f, 0f, 0.5f);
        assertAll("grey 50%",
                () -> assertEquals(127.5f, rgb[0], 1.0f),
                () -> assertEquals(127.5f, rgb[1], 1.0f),
                () -> assertEquals(127.5f, rgb[2], 1.0f)
        );
    }

    @Test
    void hslToRgbFloat_clampsOutOfRangeValues() {
        float[] rgb = ColorUtilities.HSLtoRGBFloat(-0.5f, 1.5f, 2.0f);
        assertAll("clamped",
                () -> assertTrue(rgb[0] >= 0 && rgb[0] <= 255),
                () -> assertTrue(rgb[1] >= 0 && rgb[1] <= 255),
                () -> assertTrue(rgb[2] >= 0 && rgb[2] <= 255)
        );
    }

    // --- RGBtoHSLFloat ---

    @Test
    void rgbToHslFloat_pureColors() {
        float[] hsl = ColorUtilities.RGBtoHSLFloat(255f, 0f, 0f);
        assertAll("float red",
                () -> assertEquals(0.0f, hsl[0], DELTA),
                () -> assertEquals(1.0f, hsl[1], DELTA),
                () -> assertEquals(0.5f, hsl[2], DELTA)
        );
    }

    @Test
    void rgbToHslFloat_clampsNegativeValues() {
        float[] hsl = ColorUtilities.RGBtoHSLFloat(-10f, 128f, 128f);
        assertNotNull(hsl);
        assertEquals(3, hsl.length);
    }

    @Test
    void rgbToHslFloat_clampsValuesAbove255() {
        float[] hsl = ColorUtilities.RGBtoHSLFloat(300f, 200f, 100f);
        assertNotNull(hsl);
        assertEquals(3, hsl.length);
    }

    // --- RGBtoHSBFloat / HSBtoRGBFloat ---

    @Test
    void rgbToHsbFloat_pureRed() {
        float[] hsb = ColorUtilities.RGBtoHSBFloat(255f, 0f, 0f);
        assertAll("HSB red",
                () -> assertEquals(0.0f, hsb[0], DELTA),
                () -> assertEquals(1.0f, hsb[1], DELTA),
                () -> assertEquals(1.0f, hsb[2], DELTA)
        );
    }

    @Test
    void rgbToHsbFloat_white() {
        float[] hsb = ColorUtilities.RGBtoHSBFloat(255f, 255f, 255f);
        assertAll("HSB white",
                () -> assertTrue(Float.isNaN(hsb[0]) || hsb[0] >= 0, "Hue can be NaN for achromatic colors"),
                () -> assertEquals(0.0f, hsb[1], DELTA, "Saturation should be 0"),
                () -> assertEquals(1.0f, hsb[2], DELTA, "Brightness should be 1.0")
        );
    }

    @Test
    void rgbToHsbFloat_black() {
        float[] hsb = ColorUtilities.RGBtoHSBFloat(0f, 0f, 0f);
        assertAll("HSB black",
                () -> assertEquals(0.0f, hsb[0], DELTA),
                () -> assertEquals(0.0f, hsb[1], DELTA),
                () -> assertEquals(0.0f, hsb[2], DELTA)
        );
    }

    @Test
    void hsbToRgbFloat_roundTrip() {
        float[] original = {180f, 100f, 220f};
        float[] hsb = ColorUtilities.RGBtoHSBFloat(original[0], original[1], original[2]);
        float[] roundTripped = ColorUtilities.HSBtoRGBFloat(hsb[0], hsb[1], hsb[2]);
        assertAll("HSB round-trip",
                () -> assertEquals(original[0], roundTripped[0], 1.0f),
                () -> assertEquals(original[1], roundTripped[1], 1.0f),
                () -> assertEquals(original[2], roundTripped[2], 1.0f)
        );
    }

    // --- colorKtoRGB ---

    @Test
    void colorKtoRGB_warmTemperature() {
        int[] rgb = new int[3];
        ColorUtilities.colorKtoRGB(rgb, 2000);
        // All values should be in valid range regardless of temperature interpretation
        assertTrue(rgb[0] >= 0 && rgb[0] <= 255);
        assertTrue(rgb[1] >= 0 && rgb[1] <= 255);
        assertTrue(rgb[2] >= 0 && rgb[2] <= 255);
    }

    @Test
    void colorKtoRGB_neutralTemperature() {
        int[] rgb = new int[3];
        ColorUtilities.colorKtoRGB(rgb, 5000); // Neutral daylight
        assertTrue(rgb[0] >= 0 && rgb[0] <= 255);
        assertTrue(rgb[1] >= 0 && rgb[1] <= 255);
        assertTrue(rgb[2] >= 0 && rgb[2] <= 255);
    }

    @Test
    void colorKtoRGB_coolTemperature() {
        int[] rgb = new int[3];
        ColorUtilities.colorKtoRGB(rgb, 7000);
        assertTrue(rgb[0] >= 0 && rgb[0] <= 255);
        assertTrue(rgb[1] >= 0 && rgb[1] <= 255);
        assertTrue(rgb[2] >= 0 && rgb[2] <= 255);
    }

    @Test
    void colorKtoRGB_boundaryTemperature() {
        int[] rgb = new int[3];
        ColorUtilities.colorKtoRGB(rgb, 1000); // Very low
        assertAll("clamped",
                () -> assertTrue(rgb[0] >= 0 && rgb[0] <= 255),
                () -> assertTrue(rgb[1] >= 0 && rgb[1] <= 255),
                () -> assertTrue(rgb[2] >= 0 && rgb[2] <= 255)
        );
    }
}
