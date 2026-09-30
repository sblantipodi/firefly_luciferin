/*
  EyeCareOptionsTest.java

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

import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks eye care choices and firmware command values shared by the two interfaces.
 */
class EyeCareOptionsTest {

    /**
     * Checks the dialog's selectable values and LDR firmware command.
     */
    @Test
    void suppliesLdrControlsAndCommand() {
        assertEquals(10, EyeCareOptions.minimumBrightnessValues().getFirst());
        assertEquals(100, EyeCareOptions.minimumBrightnessValues().getLast());
        assertEquals(51, EyeCareOptions.luminosityThresholdValues().size());

        Configuration config = new Configuration();
        EyeCareOptions.applyLdrControls(config, true, false, Enums.LdrInterval.MINUTES_20.getLdrIntervalInteger(), 30);
        var command = EyeCareOptions.ldrCommand(config, 2);
        assertTrue(command.isLdrEnabled());
        assertEquals(20, command.getLdrInterval());
        assertEquals(30, command.getLdrMin());
        assertEquals(2, command.getLdrAction());
    }

    /**
     * Checks that the shared controls reject values absent from the JavaFX selectors.
     */
    @Test
    void rejectsUnsupportedLdrValues() {
        Configuration config = new Configuration();
        assertThrows(IllegalArgumentException.class,
                () -> EyeCareOptions.applyLdrControls(config, true, false, 15, 20));
        assertThrows(IllegalArgumentException.class,
                () -> EyeCareOptions.applyLdrControls(config, true, false, 20, 25));
    }

    /**
     * Checks the device response that JavaFX and the web page use for live LDR controls.
     */
    @Test
    void parsesFirmwareLdrControls() {
        var controls = EyeCareOptions.parseFirmwareLdr(
                "{\"ldrEnabled\":\"1\",\"ldrTurnOff\":\"0\",\"ldrInterval\":\"20\",\"ldrMin\":\"30\"}");
        assertTrue(controls.isPresent());
        assertTrue(controls.get().enabled());
        assertFalse(controls.get().turnOff());
        assertEquals(20, controls.get().interval());
        assertEquals(30, controls.get().minimum());
        assertTrue(EyeCareOptions.parseFirmwareLdr("{\"ldrEnabled\":\"1\"}").isEmpty());
    }
}
