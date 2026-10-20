/*
  ColorEmaTest.java

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorEmaTest {
    private static ColorFloat[] colors(float value) {
        return new ColorFloat[]{new ColorFloat(value, value / 2, value / 4)};
    }

    @ParameterizedTest
    @CsvSource({"0.35,70,115.5", "0.30,60,102", "0.25,50,87.5",
            "0.20,40,72", "0.15,30,55.5", "0.10,20,38"})
    void everyLevelRetainsOriginalPerFrameResponse(float alpha, float firstExpected, float secondExpected) {
        ColorEma ema = new ColorEma();
        ema.apply(colors(0), alpha);
        ColorFloat[] first = colors(200);
        ema.apply(first, alpha);
        assertEquals(firstExpected, first[0].r(), .001);
        assertEquals(firstExpected / 2, first[0].g(), .001);
        assertEquals(firstExpected / 4, first[0].b(), .001);
        ColorFloat[] second = colors(200);
        ema.apply(second, alpha);
        assertEquals(secondExpected, second[0].r(), .001);
    }

    @Test
    void resetDisableAndResizeStartFromCurrentColorsAndNeverAliasOutput() {
        ColorEma ema = new ColorEma();
        ColorFloat[] first = colors(40);
        ema.apply(first, .2f);
        first[0] = ColorFloat.BLACK;
        ColorFloat[] next = colors(200);
        ema.apply(next, .2f);
        assertEquals(72, next[0].r(), .001);
        ema.reset();
        ColorFloat[] reset = colors(60);
        ema.apply(reset, .2f);
        assertEquals(60, reset[0].r());
        ema.apply(colors(80), 0);
        ColorFloat[] enabled = colors(100);
        ema.apply(enabled, .2f);
        assertEquals(100, enabled[0].r());
        ColorFloat[] resized = {colors(120)[0], colors(160)[0]};
        ema.apply(resized, .2f);
        assertEquals(120, resized[0].r());
        assertEquals(160, resized[1].r());
    }
}
