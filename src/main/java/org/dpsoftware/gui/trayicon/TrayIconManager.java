/*
  TrayIconManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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

import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Enums;

/**
 * Tray icon manager interface
 */
public interface TrayIconManager {

    void manageAspectRatioListener(String menuItemText, boolean sendSetCmd);

    void manageProfileListener(String menuItemText);

    void updateTray();

    void initTray();

    void populateProfiles();
    
    default void resetTray() {
        if (NativeExecutor.isSystemTraySupported()) {
            setTrayIconImage(Enums.PlayerStatus.STOP);
        }
    }

    String setTrayIconImage(Enums.PlayerStatus playerStatus);

}
