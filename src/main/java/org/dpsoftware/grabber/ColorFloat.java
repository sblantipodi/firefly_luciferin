/*
  ColorFloat.java

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

import java.awt.*;

/**
 * High-precision floating point RGB color representation [0.0f .. 255.0f].
 * Prevents intermediate quantization errors and loss of decimal precision during color transformations.
 */
public record ColorFloat(float r, float g, float b) {

    public static final ColorFloat BLACK = new ColorFloat(0.0f, 0.0f, 0.0f);

    /**
     * Convert floating-point RGB [0.0f .. 255.0f] to 8-bit java.awt.Color [0 .. 255].
     * Clamps values to [0, 255] and rounds to nearest integer.
     */
    public Color toColor() {
        return new Color(
                Math.clamp(Math.round(r), 0, 255),
                Math.clamp(Math.round(g), 0, 255),
                Math.clamp(Math.round(b), 0, 255)
        );
    }

    /**
     * Clamp all three channels to [0, 255].
     */
    public ColorFloat clamp() {
        return new ColorFloat(
                Math.clamp(this.r, 0f, 255f),
                Math.clamp(this.g, 0f, 255f),
                Math.clamp(this.b, 0f, 255f)
        );
    }
}
