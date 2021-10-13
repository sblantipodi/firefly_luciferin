/*
  DisplayManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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
package org.dpsoftware.managers;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import javafx.stage.Screen;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.DisplayInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;

/**
 * An utility class for managing displays
 */
@Slf4j
@NoArgsConstructor
public class DisplayManager {

    /**
     * How many displays are available
     * @return # of displays available
     */
    public int displayNumber() {

        return Screen.getScreens().size();

    }

    /**
     * Return a list of displays with infos, ordered by position
     * @return display infos
     */
    public List<DisplayInfo> getDisplayList() {

        List<DisplayInfo> displayInfoList = new ArrayList<>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice gd : gs) {
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (GraphicsConfiguration graphicsConfiguration : gc) {
                Rectangle gcBounds = graphicsConfiguration.getBounds();
                DisplayInfo displayInfo = new DisplayInfo();
                displayInfo.setWidth(gcBounds.getWidth());
                displayInfo.setHeight(gcBounds.getHeight());
                displayInfo.setScaleX(graphicsConfiguration.getDefaultTransform().getScaleX());
                displayInfo.setScaleY(graphicsConfiguration.getDefaultTransform().getScaleY());
                displayInfo.setMinX(gcBounds.x);
                displayInfo.setMinY(gcBounds.y);
                displayInfoList.add(displayInfo);
            }
        }
        if (NativeExecutor.isWindows()) {
            User32.INSTANCE.EnumDisplayMonitors(null, null, (hMonitor, hdc, rect, lparam) -> {
                enumerate(hMonitor, displayInfoList);
                return 1;
            }, new WinDef.LPARAM(0));
        }
        displayInfoList.sort(comparing(DisplayInfo::getMinX).reversed());
        return displayInfoList;

    }

    /**
     * Detect monitor infos from hardware monitor using JNA
     * ./gst-device-monitor-1.0.exe "Source/Monitor"
     * ./gst-launch-1.0 d3d11desktopdupsrc monitor-handle=221948 ! d3d11convert ! d3d11download ! autovideosink
     * @param hMonitor hardware monitor info
     * @param displayInfoList utility list for monitor infos
     */
    private void enumerate(WinUser.HMONITOR hMonitor, List<DisplayInfo> displayInfoList) {

        for (DisplayInfo dispInfo : displayInfoList) {
            WinUser.MONITORINFOEX info = new WinUser.MONITORINFOEX();
            User32.INSTANCE.GetMonitorInfo(hMonitor, info);
            if ((info.rcWork.left == dispInfo.getMinX()) && (info.rcWork.top == dispInfo.getMinY())) {
                dispInfo.setNativePeer(Pointer.nativeValue(hMonitor.getPointer()));
                WinDef.DWORDByReference pdwNumberOfPhysicalMonitors = new WinDef.DWORDByReference();
                Dxva2.INSTANCE.GetNumberOfPhysicalMonitorsFromHMONITOR(hMonitor, pdwNumberOfPhysicalMonitors);
                int monitorCount = pdwNumberOfPhysicalMonitors.getValue().intValue();
                PhysicalMonitorEnumerationAPI.PHYSICAL_MONITOR[] physMons = new PhysicalMonitorEnumerationAPI.PHYSICAL_MONITOR[monitorCount];
                Dxva2.INSTANCE.GetPhysicalMonitorsFromHMONITOR(hMonitor, monitorCount, physMons);
                for (int i = 0; i < monitorCount; i++) {
                    String monitorName = new String(physMons[i].szPhysicalMonitorDescription);
                    dispInfo.setMonitorName(monitorName);
                }
                Dxva2.INSTANCE.DestroyPhysicalMonitors(monitorCount, physMons);
            }
        }

    }

    /**
     * Return infos about main display
     * @return current display infos
     */
    public DisplayInfo getFirstInstanceDisplay() {

        return getDisplayList().get(0);

    }

    /**
     * Return infos about display at a certain index
     * @param monitorIndex right is 1, center is 2, left is 3
     * @return current display infos
     */
    public DisplayInfo getDisplayInfo(int monitorIndex) {

        return getDisplayList().get(monitorIndex);

    }

    /**
     * Log display information
     */
    public void logDisplayInfo() {

        getDisplayList().forEach(displayInfo -> {
            if (NativeExecutor.isWindows()) {
                log.debug("Native HMONITOR peer: " + displayInfo.getNativePeer() + " -> " + displayInfo.getMonitorName());
            }
            log.debug("Width: " + displayInfo.getWidth() + " Height: " + displayInfo.getHeight() + " Scaling: "
                    + displayInfo.getScaleX() + " MinX: " + displayInfo.getMinX() + " MinY: " + displayInfo.getMinY());
        });

    }

    /**
     * Return display name at a certain index
     * @param monitorIndex right is 1, center is 2, left is 3
     * @return display name
     */
    public String getDisplayName(int monitorIndex) {

        DisplayInfo dispInfo = getDisplayInfo(monitorIndex);
        String displayName = "";
        int screenNumber = displayNumber();
        if (screenNumber == 1) {
            displayName = Constants.SCREEN_MAIN;
        } else if (screenNumber == 2) {
            if (monitorIndex == 0) {
                displayName = Constants.SCREEN_RIGHT;
            } else {
                displayName = Constants.SCREEN_LEFT;
            }
        } else if (screenNumber == 3) {
            if (monitorIndex == 0) {
                displayName = Constants.SCREEN_RIGHT;
            } else if (monitorIndex == 1) {
                displayName = Constants.SCREEN_CENTER;
            } else {
                displayName = Constants.SCREEN_LEFT;
            }
        }
        if (dispInfo.getMonitorName().length() > 0) {
            displayName = displayName.replace("{0}", dispInfo.getMonitorName());
        } else {
            displayName = displayName.replace(" ({0})", "");
        }
        return displayName;

    }

}
