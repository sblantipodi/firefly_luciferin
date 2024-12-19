/*
  GuiSingleton.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.gui.elements.Satellite;

import javax.swing.*;
import java.awt.*;

/**
 * GUI singleton used to share common data
 */
@Getter
@Setter
@NoArgsConstructor
public class GuiSingleton {

    @Getter
    private final static GuiSingleton instance;

    static {
        instance = new GuiSingleton();
    }

    public JPopupMenu popupMenu;
    public float hueTestImageValue = 0.0F;
    public Color selectedChannel = Color.BLACK;
    public ObservableList<GlowWormDevice> deviceTableData = FXCollections.observableArrayList();
    public ObservableList<GlowWormDevice> deviceTableDataTemp = FXCollections.observableArrayList();
    public boolean oldFirmwareDevice = false;
    public ObservableList<Satellite> satellitesTableData = FXCollections.observableArrayList();
    public boolean firmTypeFull = false;
    public boolean upgrade = false;

}

