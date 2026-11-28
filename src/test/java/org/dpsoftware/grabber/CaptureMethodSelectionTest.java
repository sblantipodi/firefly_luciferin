/*
  CaptureMethodSelectionTest.java

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

import org.dpsoftware.config.Configuration.CaptureMethod;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CaptureMethodSelectionTest {
    private static List<CaptureMethod> attempts(boolean windows, boolean mac, boolean wayland, boolean usb) {
        CaptureMethodSelection selection = new CaptureMethodSelection(windows, mac, wayland, usb);
        List<CaptureMethod> methods = new ArrayList<>();
        do {
            methods.add(selection.current());
        } while (selection.advance());
        assertFalse(selection.advance());
        assertEquals(methods.getLast(), selection.current());
        return methods;
    }

    @Test
    void windowsDisplayFallsBackToDx11() {
        assertEquals(List.of(CaptureMethod.DDUPL_DX12, CaptureMethod.DDUPL_DX11),
                attempts(true, false, false, false));
    }

    @Test
    void windowsUsbHasNoAlternative() {
        assertEquals(List.of(CaptureMethod.WIN_USB_VIDEO), attempts(true, false, false, true));
    }

    @Test
    void linuxDisplayAndUsbTryAccelerationBeforeCompatibilityFallback() {
        assertEquals(List.of(CaptureMethod.XIMAGESRC_NVIDIA, CaptureMethod.XIMAGESRC),
                attempts(false, false, false, false));
        assertEquals(List.of(CaptureMethod.PIPEWIREXDG_NVIDIA, CaptureMethod.PIPEWIREXDG_AMD_HIP,
                        CaptureMethod.PIPEWIREXDG_AMD_INTEL,
                        CaptureMethod.PIPEWIREXDG_OPENGL, CaptureMethod.PIPEWIREXDG),
                attempts(false, false, true, false));
        assertEquals(List.of(CaptureMethod.USB_VIDEO_NVIDIA, CaptureMethod.USB_VIDEO_AMD_HIP,
                        CaptureMethod.USB_VIDEO_AMD_INTEL,
                        CaptureMethod.USB_VIDEO_OPENGL, CaptureMethod.USB_VIDEO),
                attempts(false, false, true, true));
    }

    @Test
    void macHasOneMethod() {
        assertEquals(List.of(CaptureMethod.AVFVIDEOSRC), attempts(false, true, false, false));
    }
}
