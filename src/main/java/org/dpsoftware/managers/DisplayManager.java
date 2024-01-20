/*
  DisplayManager.java

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
package org.dpsoftware.managers;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.utilities.CommonUtility;

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
     * Set and get Display info
     *
     * @param gd   graphics device
     * @param mode display mode
     * @return display info
     */
    private static DisplayInfo getDisplayInfo(GraphicsDevice gd, DisplayMode mode) {
        Rectangle bounds = gd.getDefaultConfiguration().getBounds();
        DisplayInfo displayInfo = new DisplayInfo();
        displayInfo.setWidth(mode.getWidth());
        displayInfo.setHeight(mode.getHeight());
        displayInfo.setScaleX(gd.getDefaultConfiguration().getDefaultTransform().getScaleX());
        displayInfo.setScaleY(gd.getDefaultConfiguration().getDefaultTransform().getScaleY());
        displayInfo.setMinX(bounds.getMinX());
        displayInfo.setMinY(bounds.getMinY());
        displayInfo.setMaxX(bounds.getMaxX());
        displayInfo.setMaxY(bounds.getMaxY());
        return displayInfo;
    }

    /**
     * How many displays are available
     *
     * @return # of displays available
     */
    public int displayNumber() {
        return Screen.getScreens().size();
    }

    /**
     * Return a list of displays with infos, ordered by position.
     * On Windows it uses both AWT and JavaFX to get all the needed infos.
     *
     * @return display infos
     */
    public List<DisplayInfo> getDisplayList() {
        List<DisplayInfo> displayInfoListJavaFX;
        List<DisplayInfo> displayInfoListAwt = getScreensWithAWT();
        if (NativeExecutor.isWindows()) {
            User32.INSTANCE.EnumDisplayMonitors(null, null, (hMonitor, hdc, rect, lparam) -> {
                enumerate(hMonitor, displayInfoListAwt);
                return 1;
            }, new WinDef.LPARAM(0));
        }
        displayInfoListJavaFX = enrichJavaFxInfoWithAwt(displayInfoListAwt);
        return displayInfoListJavaFX;
    }

    /**
     * Add screen infos using AWT
     *
     * @param displayInfoListAwt awt screen infos
     * @return a list of screen infos gathered using JavaFX and AWT
     */
    private List<DisplayInfo> enrichJavaFxInfoWithAwt(List<DisplayInfo> displayInfoListAwt) {
        List<DisplayInfo> displayInfoListJavaFX;
        displayInfoListJavaFX = getScreensWithJavaFX();
        for (int i = 0; i < displayInfoListAwt.size(); i++) {
            if (NativeExecutor.isWindows()) {
                displayInfoListJavaFX.get(i).setNativePeer(displayInfoListAwt.get(i).getNativePeer());
                displayInfoListJavaFX.get(i).setMonitorName(displayInfoListAwt.get(i).getMonitorName());
                displayInfoListJavaFX.get(i).setPrimaryDisplay(displayInfoListAwt.get(i).isPrimaryDisplay());
            }
            displayInfoListJavaFX.get(i).setDisplayInfoAwt(new DisplayInfo());
            displayInfoListJavaFX.get(i).getDisplayInfoAwt().setHeight(displayInfoListAwt.get(i).getHeight());
            displayInfoListJavaFX.get(i).getDisplayInfoAwt().setWidth(displayInfoListAwt.get(i).getWidth());
            displayInfoListJavaFX.get(i).getDisplayInfoAwt().setMinX(displayInfoListAwt.get(i).getMinX());
            displayInfoListJavaFX.get(i).getDisplayInfoAwt().setMinY(displayInfoListAwt.get(i).getMinY());
            displayInfoListJavaFX.get(i).getDisplayInfoAwt().setMaxX(displayInfoListAwt.get(i).getMaxX());
            displayInfoListJavaFX.get(i).getDisplayInfoAwt().setMaxY(displayInfoListAwt.get(i).getMaxY());
        }
        return displayInfoListJavaFX;
    }

    /**
     * Utility method for retrieving screens infos using JavaFX
     *
     * @return screens infos
     */
    private List<DisplayInfo> getScreensWithJavaFX() {
        List<DisplayInfo> displayInfoList = new ArrayList<>();
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D visualBounds = screen.getBounds();
            Rectangle2D bounds = screen.getBounds();
            DisplayInfo displayInfo = getDisplayInfo(screen, bounds, visualBounds);
            displayInfoList.add(displayInfo);
        }
        displayInfoList.sort(comparing(DisplayInfo::getMinX).reversed());
        return displayInfoList;
    }

    /**
     * Set and get Display info
     *
     * @param screen       data
     * @param bounds       screen bound
     * @param visualBounds visual
     * @return display info
     */
    private DisplayInfo getDisplayInfo(Screen screen, Rectangle2D bounds, Rectangle2D visualBounds) {
        DisplayInfo displayInfo = new DisplayInfo();
        displayInfo.setWidth(bounds.getWidth());
        displayInfo.setHeight(bounds.getHeight());
        displayInfo.setScaleX(screen.getOutputScaleX());
        displayInfo.setScaleY(screen.getOutputScaleY());
        displayInfo.setMinX(visualBounds.getMinX());
        displayInfo.setMinY(visualBounds.getMinY());
        displayInfo.setMaxX(visualBounds.getMaxX());
        displayInfo.setMaxY(visualBounds.getMaxY());
        return displayInfo;
    }

    /**
     * Utility method for retrieving screens infos using AWT
     *
     * @return screens infos
     */
    private List<DisplayInfo> getScreensWithAWT() {
        List<DisplayInfo> displayInfoList = new ArrayList<>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice gd : gs) {
            DisplayMode mode = gd.getDisplayMode();
            DisplayInfo displayInfo = getDisplayInfo(gd, mode);
            displayInfoList.add(displayInfo);
        }
        displayInfoList.sort(comparing(DisplayInfo::getMinX).reversed());
        return displayInfoList;
    }

    /**
     * Detect monitor infos from hardware monitor using JNA, this works with AWT using Windows API
     *
     * @param hMonitor        hardware monitor info
     * @param displayInfoList utility list for monitor infos
     */
    private void enumerate(WinUser.HMONITOR hMonitor, List<DisplayInfo> displayInfoList) {
        for (DisplayInfo dispInfo : displayInfoList) {
            WinUser.MONITORINFOEX info = new WinUser.MONITORINFOEX();
            User32.INSTANCE.GetMonitorInfo(hMonitor, info);
            if ((dispInfo.getMinX() >= (info.rcWork.left - Constants.PRIMARY_DISPLAY_TOLERANCE) && dispInfo.getMinX() <= (info.rcWork.left + Constants.PRIMARY_DISPLAY_TOLERANCE))
                    && (dispInfo.getMinY() >= (info.rcWork.top - Constants.PRIMARY_DISPLAY_TOLERANCE) && dispInfo.getMinY() <= (info.rcWork.top + Constants.PRIMARY_DISPLAY_TOLERANCE))) {
                dispInfo.setPrimaryDisplay((info.dwFlags & WinUser.MONITORINFOF_PRIMARY) != 0);
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
     *
     * @return current display infos
     */
    public DisplayInfo getFirstInstanceDisplay() {
        return getDisplayList().getFirst();
    }

    /**
     * Return infos about main display
     *
     * @return current display infos
     */
    public DisplayInfo getPrimaryDisplay() {
        return getDisplayList().stream().filter(DisplayInfo::isPrimaryDisplay).findAny().orElse(null);
    }

    /**
     * Return infos about display at a certain index
     *
     * @param monitorIndex right is 1, center is 2, left is 3
     * @return current display infos
     */
    public DisplayInfo getDisplayInfo(int monitorIndex) {
        try {
            return getDisplayList().get(monitorIndex);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Log display information
     */
    public void logDisplayInfo() {
        getDisplayList().forEach(displayInfo -> {
            if (NativeExecutor.isWindows()) {
                log.info("Native HMONITOR peer: " + displayInfo.getNativePeer() + " -> " + displayInfo.getMonitorName());
            }
            log.info("Width: " + displayInfo.getWidth() + " Height: " + displayInfo.getHeight() + " Scaling: "
                    + displayInfo.getScaleX() + " MinX: " + displayInfo.getMinX() + " MinY: " + displayInfo.getMinY());
        });
    }

    /**
     * Return display name at a certain index
     *
     * @param monitorIndex right is 1, center is 2, left is 3
     * @return display name
     */
    public String getDisplayName(int monitorIndex) {
        DisplayInfo dispInfo = getDisplayInfo(monitorIndex);
        String displayName = "";
        int screenNumber = displayNumber();
        if (screenNumber == 1) {
            displayName = CommonUtility.getWord(Constants.SCREEN_MAIN);
        } else if (screenNumber == 2) {
            if (monitorIndex == 0) {
                displayName = CommonUtility.getWord(Constants.SCREEN_RIGHT);
            } else {
                displayName = CommonUtility.getWord(Constants.SCREEN_LEFT);
            }
        } else if (screenNumber >= 3) {
            if (monitorIndex == 0) {
                displayName = CommonUtility.getWord(Constants.SCREEN_RIGHT);
            } else if (monitorIndex == 1) {
                displayName = CommonUtility.getWord(Constants.SCREEN_CENTER);
            } else {
                displayName = CommonUtility.getWord(Constants.SCREEN_LEFT);
            }
        }
        if (dispInfo == null) {
            displayName = "Screen " + monitorIndex;
        } else {
            if (dispInfo.getMonitorName() != null && !dispInfo.getMonitorName().isEmpty()) {
                displayName = displayName.replace("{0}", dispInfo.getMonitorName());
            } else {
                displayName = displayName.replace(" ({0})", "");
            }
        }
        return displayName;
    }
}
