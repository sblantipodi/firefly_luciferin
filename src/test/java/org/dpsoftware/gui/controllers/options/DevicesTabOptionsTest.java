/*
  DevicesTabOptionsTest.java

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
package org.dpsoftware.gui.controllers.options;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the device tab choices shared by JavaFX and the web interface.
 */
class DevicesTabOptionsTest {

    /**
     * Checks that monitor choices follow the detected display count, up to three.
     */
    @Test
    void limitsMonitorChoicesToDetectedDisplays() {
        assertEquals(List.of(1), DevicesTabOptions.monitorChoices(1).stream()
                .map(DevicesTabOptions.MonitorChoice::count).toList());
        assertEquals(List.of(1, 2), DevicesTabOptions.monitorChoices(2).stream()
                .map(DevicesTabOptions.MonitorChoice::count).toList());
        assertEquals(List.of(1, 2, 3), DevicesTabOptions.monitorChoices(4).stream()
                .map(DevicesTabOptions.MonitorChoice::count).toList());
    }
}
