/*
  TrayIconState.java

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
package org.dpsoftware.gui.trayicon;

import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiManager;

/**
 * Shares the tray's requested player state with other interfaces, including when
 * the operating system has no visible tray. Image selection stays in GuiManager.
 */
public final class TrayIconState {

    private static volatile Enums.PlayerStatus playerStatus = Enums.PlayerStatus.STOP;

    private TrayIconState() {
    }

    public static String update(Enums.PlayerStatus status) {
        playerStatus = status;
        return GuiManager.computeImageToUse(status);
    }

    public static String currentImage() {
        // Flatpak flattens update icon names for AppIndicator, while the web
        // interface reads the original PNG from the application's resources.
        return GuiManager.computeImageToUse(playerStatus)
                .replace(Constants.IMG_PATH + "update_", Constants.IMG_PATH_UPDATE);
    }
}
