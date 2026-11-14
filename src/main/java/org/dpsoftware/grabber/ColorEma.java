/*
  ColorEma.java

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

/**
 * Color EMA using the configured alpha unchanged on every update.
 * Owns its history so concurrent capture producers cannot corrupt filter state.
 */
final class ColorEma {
    private ColorFloat[] previous;

    synchronized void reset() {
        previous = null;
    }

    /**
     * Mutates only the caller's array; stored colors remain immutable.
     * Each update advances by the same weight, even after a capture delay.
     * Fewer updates per second intentionally produce slower transitions.
     * Disabled filtering clears history, so enabling it cannot reuse stale colors.
     */
    synchronized void apply(ColorFloat[] colors, float alpha) {
        if (!(alpha > 0f && alpha < 1f)) {
            reset();
            return;
        }
        if (previous == null || previous.length != colors.length) {
            previous = colors.clone();
            return;
        }
        for (int i = 0; i < colors.length; i++) {
            ColorFloat color = new ColorFloat(
                    Math.clamp(alpha * colors[i].r() + (1f - alpha) * previous[i].r(), 0f, 255f),
                    Math.clamp(alpha * colors[i].g() + (1f - alpha) * previous[i].g(), 0f, 255f),
                    Math.clamp(alpha * colors[i].b() + (1f - alpha) * previous[i].b(), 0f, 255f));
            previous[i] = color;
            colors[i] = color;
        }
    }
}
