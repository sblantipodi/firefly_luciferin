/*
  GuiSingleton.java

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
package org.dpsoftware.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.gui.elements.Satellite;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    @Getter(AccessLevel.NONE)
    private final Set<String> glowWormDevicesAwaitingUpdate = ConcurrentHashMap.newKeySet();
    public ObservableList<Satellite> satellitesTableData = FXCollections.observableArrayList();
    public boolean firmTypeFull = false;
    public volatile boolean oldFirmwareDevice = false;
    public volatile boolean upgrade = false;
    public volatile boolean glowWormUpdateInProgress = false;
    public boolean rleVisualMapVisible;
    public Stage colorDialog;
    // Last TestCanvas instance, reused across toggles to avoid repeatedly tearing down and re-creating JavaFX scenes
    // (orphaned NGCanvas in the render graph + Prism texture pool exhaustion).
    public TestCanvas testCanvas;
    // Grabber manager instance, used to read the latest captured RGB frame as a test canvas background
    public volatile org.dpsoftware.grabber.GrabberManager grabberManager;
    public boolean showLiveCapture = false;

    /**
     * Identify a device consistently across discovery and update results.
     *
     * @param device device to identify
     * @return stable device key based on MAC address, IP address, or device name
     */
    private static String firmwareUpdateKey(GlowWormDevice device) {
        String mac = device.getMac();
        if (mac != null && !mac.isBlank() && !"-".equals(mac)) {
            return "mac:" + mac.trim().toLowerCase(Locale.ROOT);
        }
        String ip = device.getDeviceIP();
        if (ip != null && !ip.isBlank() && !"-".equals(ip)) {
            return "ip:" + ip.trim().toLowerCase(Locale.ROOT);
        }
        return "name:" + String.valueOf(device.getDeviceName()).trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Record a device that still requires a firmware update.
     *
     * @param device device requiring an update
     */
    public void registerGlowWormUpdate(GlowWormDevice device) {
        glowWormDevicesAwaitingUpdate.add(firmwareUpdateKey(device));
    }

    /**
     * Remove a device after its firmware update succeeds.
     *
     * @param device successfully updated device
     */
    public void completeGlowWormUpdate(GlowWormDevice device) {
        glowWormDevicesAwaitingUpdate.remove(firmwareUpdateKey(device));
    }

    /**
     * Report whether any connected device still requires a firmware update.
     *
     * @return true when at least one device is awaiting a successful update
     */
    public boolean isGlowWormUpdateAvailable() {
        return !glowWormDevicesAwaitingUpdate.isEmpty();
    }

}

