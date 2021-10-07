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

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.gui.elements.DisplayInfo;

import javax.swing.*;
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
     * //TODO Overwrite getDisplayList() function and Switch to this function as soon as GstDeviceProvider is implemented in GStreamer
     *
     * The GraphicsDevice class describes the graphics devices that might be available in a particular graphics
     * environment. These include screen and printer devices. Note that there can be many screens and many
     * printers in an instance of GraphicsEnvironment. Each graphics device has one or more GraphicsConfiguration
     * objects associated with it. These objects specify the different configurations in which the GraphicsDevice
     * can be used.
     * In a multi-screen environment, the GraphicsConfiguration objects can be used to render components on
     * multiple screens. The following code sample demonstrates how to create a JFrame object for
     * each GraphicsConfiguration on each screen device in the GraphicsEnvironment:
     * @return display infos
     */
    @SuppressWarnings({"unused", "deprecation"})
    public List<DisplayInfo> getDisplayListFromGraphicsDevice() {

        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = env.getScreenDevices();
        int sequence = 1;
        for (GraphicsDevice device : devices) {
            System.out.println("Screen Number [" + (sequence++) + "]");
            System.out.println("Width       : " + device.getDisplayMode().getWidth());
            System.out.println("Height      : " + device.getDisplayMode().getHeight());
            System.out.println("Refresh Rate: " + device.getDisplayMode().getRefreshRate());
            System.out.println("Bit Depth   : " + device.getDisplayMode().getBitDepth());
        }

        GraphicsEnvironment ge = GraphicsEnvironment.
                getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice gd : gs) {
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i = 0; i < gc.length; i++) {
                JFrame f = new JFrame(gd.getDefaultConfiguration());
                Canvas c = new Canvas(gc[i]);
                Rectangle gcBounds = gc[i].getBounds();
                int xoffs = gcBounds.x;
                int yoffs = gcBounds.y;
                f.getContentPane().add(c);
                f.setLocation((i * 50) + xoffs, (i * 60) + yoffs);
                f.show();
            }
        }
        return null;

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
