/*
  DisplayManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2021  Davide Perini

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

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.gui.elements.DisplayInfo;

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
        int i = 1;
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D visualBounds = screen.getVisualBounds();
            Rectangle2D bounds = screen.getBounds();
            DisplayInfo displayInfo = new DisplayInfo();
            displayInfo.setFxDisplayNumber(i);
            displayInfo.setWidth(bounds.getWidth());
            displayInfo.setHeight(bounds.getHeight());
            displayInfo.setScaleX(screen.getOutputScaleX());
            displayInfo.setScaleY(screen.getOutputScaleY());
            displayInfo.setMinX(visualBounds.getMinX());
            displayInfo.setMinY(visualBounds.getMinY());
            displayInfoList.add(displayInfo);
            i++;
        }
        displayInfoList.sort(comparing(DisplayInfo::getMinX).reversed());
        return displayInfoList;

    }

    /**
     * Return infos about current display
     * @return current display infos
     */
    public DisplayInfo getFirstInstanceDisplay() {

        return getDisplayList().get(0);

    }

    /**
     * Return infos about current display
     * @return current display infos
     */
    public DisplayInfo getDisplayInfo(int fxDisplayNumber) {

        return getDisplayList().stream().filter(displayInfo -> displayInfo.getFxDisplayNumber() == fxDisplayNumber).findAny().orElse(null);

    }

}
