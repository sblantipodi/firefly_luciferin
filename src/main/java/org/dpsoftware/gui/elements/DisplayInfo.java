/*
  DisplayInfo.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.gui.elements;

import lombok.Getter;
import lombok.Setter;

/**
 * A class that map display infos
 */
@Getter
@Setter
public class DisplayInfo {

    public double width, height;
    public double scaleX;
    public double scaleY;
    public double minX, minY;
    public double maxX, maxY;
    public long nativePeer; // HMONITOR Handle casted to guint64
    public String monitorName;
    boolean primaryDisplay;
    DisplayInfo displayInfoAwt;

}
