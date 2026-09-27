/*
  CaptureMethodSelection.java

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

import java.util.List;

/**
 * Ordered capture probing; the final candidate is also the compatibility fallback.
 */
public final class CaptureMethodSelection {
    private final List<CaptureMethod> candidates;
    private int index;

    public CaptureMethodSelection(boolean windows, boolean mac, boolean wayland, boolean usb) {
        if (windows) {
            candidates = usb ? List.of(CaptureMethod.WIN_USB_VIDEO)
                    : List.of(CaptureMethod.DDUPL_DX12, CaptureMethod.DDUPL_DX11);
        } else if (mac) {
            candidates = List.of(CaptureMethod.AVFVIDEOSRC);
        } else if (usb) {
            candidates = List.of(
                    CaptureMethod.USB_VIDEO_NVIDIA,
                    CaptureMethod.USB_VIDEO_AMD_HIP,
                    CaptureMethod.USB_VIDEO_AMD_INTEL,
                    CaptureMethod.USB_VIDEO_OPENGL,
                    CaptureMethod.USB_VIDEO);
        } else if (wayland) {
            candidates = List.of(
                    CaptureMethod.PIPEWIREXDG_NVIDIA,
                    CaptureMethod.PIPEWIREXDG_AMD_HIP,
                    CaptureMethod.PIPEWIREXDG_AMD_INTEL,
                    CaptureMethod.PIPEWIREXDG_OPENGL,
                    CaptureMethod.PIPEWIREXDG);
        } else {
            candidates = List.of(
                    CaptureMethod.XIMAGESRC_NVIDIA,
                    CaptureMethod.XIMAGESRC);
        }
    }

    public CaptureMethod current() {
        return candidates.get(index);
    }

    /** Advances after failure, retaining the fallback when all candidates have failed. */
    public boolean advance() {
        if (index + 1 == candidates.size()) {
            return false;
        }
        index++;
        return true;
    }
}
