/*
  ColorUtilities.java

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

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.managers.dto.ColorRGBW;

import java.awt.*;

/**
 * AWT contains the Color class that is able to perform operations on HSB colors but not on HSL ones.
 * This is an helper class for the HSL color management.
 */
@Slf4j
public class ColorUtilities {

    /**
     * Returns the HSL (Hue/Saturation/Luminance) equivalent of a given
     * RGB color. All three HSL components are floats between 0.0 and 1.0.
     *
     * @param color RGB color between 0 and 255
     * @param hsl   a pre-allocated array of floats; can be null
     * @return hsl if non-null, a new array of 3 floats otherwise
     */
    public static float[] RGBtoHSL(Color color, float[] hsl) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        if (hsl == null) {
            hsl = new float[3];
        }
        if (r < 0) r = 0;
        else if (r > 255) r = 255;
        if (g < 0) g = 0;
        else if (g > 255) g = 255;
        if (b < 0) b = 0;
        else if (b > 255) b = 255;
        float var_R = (r / 255f);
        float var_G = (g / 255f);
        float var_B = (b / 255f);
        float var_Min;
        float var_Max;
        float del_Max;
        if (var_R > var_G) {
            var_Min = var_G;
            var_Max = var_R;
        } else {
            var_Min = var_R;
            var_Max = var_G;
        }
        if (var_B > var_Max) {
            var_Max = var_B;
        }
        if (var_B < var_Min) {
            var_Min = var_B;
        }
        del_Max = var_Max - var_Min;
        float H, S, L;
        L = (var_Max + var_Min) / 2f;
        if (del_Max - 0.01f <= 0.0f) {
            H = 0;
            S = 0;
        } else {
            if (L < 0.5f) {
                S = del_Max / (var_Max + var_Min);
            } else {
                S = del_Max / (2 - var_Max - var_Min);
            }
            float del_R = (((var_Max - var_R) / 6f) + (del_Max / 2f)) / del_Max;
            float del_G = (((var_Max - var_G) / 6f) + (del_Max / 2f)) / del_Max;
            float del_B = (((var_Max - var_B) / 6f) + (del_Max / 2f)) / del_Max;
            if (var_R == var_Max) {
                H = del_B - del_G;
            } else if (var_G == var_Max) {
                H = (1 / 3f) + del_R - del_B;
            } else {
                H = (2 / 3f) + del_G - del_R;
            }
            if (H < 0) {
                H += 1;
            }
            if (H > 1) {
                H -= 1;
            }
        }
        hsl[0] = H;
        hsl[1] = S;
        hsl[2] = L;
        return hsl;
    }

    /**
     * Returns the RGB equivalent of a given HSL (Hue/Saturation/Luminance) color.
     * Delegates to {@link #HSLtoRGBFloat(float, float, float)} and converts to 8-bit at the end.
     *
     * @param h the hue component, between 0.0 and 1.0
     * @param s the saturation component, between 0.0 and 1.0
     * @param l the luminance component, between 0.0 and 1.0
     * @return a new Color object equivalent to the HSL components
     */
    public static Color HSLtoRGB(float h, float s, float l) {
        float[] rgb = HSLtoRGBFloat(h, s, l);
        return new Color(Math.clamp(Math.round(rgb[0]), 0, 255),
                Math.clamp(Math.round(rgb[1]), 0, 255),
                Math.clamp(Math.round(rgb[2]), 0, 255));
    }

    /**
     * Returns the RGB equivalent as floats [0..255].
     * Avoids integer quantization for intermediate color pipelines.
     *
     * @param h the hue component, between 0.0 and 1.0
     * @param s the saturation component, between 0.0 and 1.0
     * @param l the luminance component, between 0.0 and 1.0
     * @return array of 3 floats representing R, G, B in [0..255]
     */
    public static float[] HSLtoRGBFloat(float h, float s, float l) {
        if (h < 0f) h = 0.0f;
        else if (h > 1.0f) h = 1.0f;
        if (s < 0f) s = 0.0f;
        else if (s > 1.0f) s = 1.0f;
        if (l < 0f) l = 0.0f;
        else if (l > 1.0f) l = 1.0f;
        float R, G, B;
        if (s - 0.01f <= 0.0f) {
            R = l * 255.0f;
            G = l * 255.0f;
            B = l * 255.0f;
        } else {
            float var_1, var_2;
            if (l < 0.5f) {
                var_2 = l * (1 + s);
            } else {
                var_2 = (l + s) - (s * l);
            }
            var_1 = 2 * l - var_2;
            R = 255.0f * hue2RGB(var_1, var_2, h + (1.0f / 3.0f));
            G = 255.0f * hue2RGB(var_1, var_2, h);
            B = 255.0f * hue2RGB(var_1, var_2, h - (1.0f / 3.0f));
        }
        return new float[]{R, G, B};
    }

    /**
     * Convert RGB floats [0..255] to HSL, preserving precision.
     *
     * @param r red   component [0..255]
     * @param g green component [0..255]
     * @param b blue  component [0..255]
     * @return array of 3 floats: H, S, L each in [0..1]
     */
    public static float[] RGBtoHSLFloat(float r, float g, float b) {
        float var_R = Math.clamp(r, 0f, 255f) / 255f;
        float var_G = Math.clamp(g, 0f, 255f) / 255f;
        float var_B = Math.clamp(b, 0f, 255f) / 255f;
        float var_Min, var_Max, del_Max;

        if (var_R > var_G) {
            var_Min = var_G;
            var_Max = var_R;
        } else {
            var_Min = var_R;
            var_Max = var_G;
        }
        if (var_B > var_Max) var_Max = var_B;
        if (var_B < var_Min) var_Min = var_B;

        del_Max = var_Max - var_Min;
        float L = (var_Max + var_Min) / 2f;
        float H, S;

        if (del_Max - 0.01f <= 0.0f) {
            H = 0;
            S = 0;
        } else {
            S = L < 0.5f ? del_Max / (var_Max + var_Min) : del_Max / (2 - var_Max - var_Min);
            float delR = ((var_Max - var_R) / 6f + del_Max / 2f) / del_Max;
            float delG = ((var_Max - var_G) / 6f + del_Max / 2f) / del_Max;
            float delB = ((var_Max - var_B) / 6f + del_Max / 2f) / del_Max;

            if (var_R == var_Max) H = delB - delG;
            else if (var_G == var_Max) H = (1f / 3f) + delR - delB;
            else H = (2f / 3f) + delG - delR;

            if (H < 0f) H += 1f;
            if (H > 1f) H -= 1f;
        }
        return new float[]{H, S, L};
    }

    /**
     * Convert RGB floats [0..255] to HSB (the Java AWT version of HSV), preserving precision.
     * Unlike {@link java.awt.Color#RGBtoHSB(int, int, int, float[])} this avoids the rounding
     * to int that happens inside the AWT implementation.
     *
     * @param r red   component [0..255]
     * @param g green component [0..255]
     * @param b blue  component [0..255]
     * @return array of 3 floats: H, S, B each in [0..1]
     */
    public static float[] RGBtoHSBFloat(float r, float g, float b) {
        float var_R = Math.clamp(r, 0f, 255f) / 255f;
        float var_G = Math.clamp(g, 0f, 255f) / 255f;
        float var_B = Math.clamp(b, 0f, 255f) / 255f;
        float var_Min, var_Max, del_Max;

        if (var_R > var_G) {
            var_Min = var_G;
            var_Max = var_R;
        } else {
            var_Min = var_R;
            var_Max = var_G;
        }
        if (var_B > var_Max) var_Max = var_B;
        if (var_B < var_Min) var_Min = var_B;

        del_Max = var_Max - var_Min;
        float B = var_Max;
        float H, S;

        if (B - 0.01f <= 0f) {
            H = 0f;
            S = 0f;
        } else {
            S = del_Max / B;
            float delR = ((var_Max - var_R) / 6f + del_Max / 2f) / del_Max;
            float delG = ((var_Max - var_G) / 6f + del_Max / 2f) / del_Max;
            float delB = ((var_Max - var_B) / 6f + del_Max / 2f) / del_Max;

            if (var_R == var_Max) H = delB - delG;
            else if (var_G == var_Max) H = (1f / 3f) + delR - delB;
            else H = (2f / 3f) + delG - delR;

            if (H < 0f) H += 1f;
            if (H > 1f) H -= 1f;
        }
        return new float[]{H, S, B};
    }

    /**
     * Convert HSB floats to RGB floats [0..255], preserving precision.
     * Analogous to {@link java.awt.Color#getHSBColor(float, float, float)} but without integer rounding.
     *
     * @param h hue   [0..1]
     * @param s saturation [0..1]
     * @param b brightness [0..1]
     * @return array of 3 floats representing R, G, B in [0..255]
     */
    public static float[] HSBtoRGBFloat(float h, float s, float b) {
        if (h < 0f) h = 0f;
        else if (h > 1f) h = 1f;
        if (s < 0f) s = 0f;
        else if (s > 1f) s = 1f;
        if (b < 0f) b = 0f;
        else if (b > 1f) b = 1f;

        float R, G, B;
        if (s - 0.01f <= 0f) {
            R = b * 255f;
            G = b * 255f;
            B = b * 255f;
        } else {
            float H = h * 360f;
            while (H >= 360f) H -= 360f;
            while (H < 0f) H += 360f;
            int Hi = (int) H / 60;
            float f = H / 60f - Hi;
            float p = b * (1f - s);
            float q = b * (1f - s * f);
            float t = b * (1f - s * (1f - f));

            switch (Hi) {
                case 0 -> {
                    R = b;
                    G = t;
                    B = p;
                }
                case 1 -> {
                    R = q;
                    G = b;
                    B = p;
                }
                case 2 -> {
                    R = p;
                    G = b;
                    B = t;
                }
                case 3 -> {
                    R = p;
                    G = q;
                    B = b;
                }
                case 4 -> {
                    R = t;
                    G = p;
                    B = b;
                }
                default -> {
                    R = b;
                    G = p;
                    B = q;
                }
            }
        }
        return new float[]{R * 255f, G * 255f, B * 255f};
    }

    /**
     * Transforms hue to RGB
     */
    private static float hue2RGB(float v1, float v2, float vH) {
        if (vH < 0.0f) {
            vH += 1.0f;
        }
        if (vH > 1.0f) {
            vH -= 1.0f;
        }
        if ((6.0f * vH) < 1.0f) {
            return (v1 + (v2 - v1) * 6.0f * vH);
        }
        if ((2.0f * vH) < 1.0f) {
            return (v2);
        }
        if ((3.0f * vH) < 2.0f) {
            return (v1 + (v2 - v1) * ((2.0f / 3.0f) - vH) * 6.0f);
        }
        return (v1);
    }

    /**
     * Apply white temp correction on RGB color, this is not a complete algorithm because we
     * don't have white channel on monitors, this is used for test canvas only, real full algorithm runs on
     * Glow Worm Luciferin Firmware
     *
     * @param r red channel
     * @param g green channel
     * @param b blue channel
     * @return RGB color
     */
    public static ColorRGBW calculateRgbMode(int r, int g, int b) {
        int[] colorCorrectionRGB = {0, 0, 0};
        int whiteTempInUse = MainSingleton.getInstance().config.getWhiteTemperature();
        int w;
        w = r < g ? (Math.min(r, b)) : (Math.min(g, b));
        if (MainSingleton.getInstance().config.getColorMode() == 2) {
            // subtract white in accurate mode
            r -= w;
            g -= w;
            b -= w;
        } else if (MainSingleton.getInstance().config.getColorMode() == 4) {
            // RGB only, turn off white led
            w = 0;
        }
        if (whiteTempInUse != Constants.DEFAULT_WHITE_TEMP) {
            colorKtoRGB(colorCorrectionRGB, whiteTempInUse);
            int[] rgb = new int[3];
            rgb[0] = (colorCorrectionRGB[0] * r) / 255; // correct R
            rgb[1] = (colorCorrectionRGB[1] * g) / 255; // correct G
            rgb[2] = (colorCorrectionRGB[2] * b) / 255; // correct B
            return new ColorRGBW(applyBrightnessCorrection(rgb[0]), applyBrightnessCorrection(rgb[1]), applyBrightnessCorrection(rgb[2]), applyBrightnessCorrection(w));
        } else {
            return new ColorRGBW(applyBrightnessCorrection(r), applyBrightnessCorrection(g), applyBrightnessCorrection(b), applyBrightnessCorrection(w));
        }
    }

    /**
     * Apply white balance from color temperature in Kelvin
     * (<a href="https://tannerhelland.com/2012/09/18/convert-temperature-rgb-algorithm-code.html">Converting temperature (Kelvin) to RGB</a>)
     *
     * @param rgb            RGB channel
     * @param whiteTempInUse white correction in use
     */
    public static void colorKtoRGB(int[] rgb, int whiteTempInUse) {
        float r, g, b;
        float temp = whiteTempInUse - 10;
        if (temp <= 66) {
            r = 255;
            g = Math.round(99.4708025861 * Math.log(temp) - 161.1195681661);
            if (temp <= 19) {
                b = 0;
            } else {
                b = Math.round(138.5177312231 * Math.log((temp - 10)) - 305.0447927307);
            }
        } else {
            r = Math.round(329.698727446 * Math.pow((temp - 60), -0.1332047592));
            g = Math.round(288.1221695283 * Math.pow((temp - 60), -0.0755148492));
            b = 255;
        }
        rgb[0] = (int) Math.clamp(r, 0f, 255f);
        rgb[1] = (int) Math.clamp(g, 0f, 255f);
        rgb[2] = (int) Math.clamp(b, 0f, 255f);
    }

    /**
     * Calculate brightness correction on C
     *
     * @param c color to correct
     * @return corrected color
     */
    public static int applyBrightnessCorrection(int c) {
        return c > 0 ? (c * ((MainSingleton.getInstance().config.getBrightness() * 100) / 255)) / 100 : c;
    }

}