/*
  DevicesTabOptions.java

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
package org.dpsoftware.gui.controllers.options;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.SerialManager;
import org.dpsoftware.utilities.CommonUtility;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Device selector values and conversions shared by JavaFX and web settings.
 */
public final class DevicesTabOptions {

    /**
     * Prevents instantiation.
     */
    private DevicesTabOptions() {
    }

    /**
     * Returns the available output choices for serial or wireless streaming.
     *
     * @param wirelessStream whether network streaming is selected
     * @param multiMonitor   configured monitor count
     * @param connectedNames connected Glow Worm device names
     * @return ordered choices for the editable output selector
     */
    public static List<String> outputChoices(boolean wirelessStream, int multiMonitor, List<String> connectedNames) {
        Set<String> choices = new LinkedHashSet<>();
        if (multiMonitor == 1) {
            choices.add(Constants.SERIAL_PORT_AUTO);
        }
        if (wirelessStream) {
            choices.addAll(connectedNames);
        } else {
            try {
                choices.addAll(new SerialManager().getAvailableDevices().keySet().stream().sorted().toList());
            } catch (RuntimeException | LinkageError ignored) {
                // Serial discovery may be unavailable while the web server is starting.
            }
        }
        return new ArrayList<>(choices);
    }

    /**
     * Returns the text shown by the editable output selector for a saved configuration.
     *
     * @param config saved configuration
     * @return static IP when set, otherwise the output device
     */
    public static String selectedOutput(Configuration config) {
        return NetworkManager.isValidIp(config.getStaticGlowWormIp())
                ? config.getStaticGlowWormIp() : config.getOutputDevice();
    }

    /**
     * Stores an output name, serial port, AUTO, or valid IP like the JavaFX device tab.
     *
     * @param config    configuration to update
     * @param selection editable selector value
     */
    public static void applyOutput(Configuration config, String selection) {
        if (selection == null || selection.isBlank()) {
            throw new IllegalArgumentException("Output device is required");
        }
        if (NetworkManager.isValidIp(selection)) {
            config.setOutputDevice(Constants.DASH);
            config.setStaticGlowWormIp(selection);
        } else {
            config.setOutputDevice(selection);
            config.setStaticGlowWormIp(Constants.DASH);
        }
    }

    /**
     * Checks whether the baud rate change requires firmware programming.
     *
     * @param before saved configuration
     * @param after  edited configuration
     * @return true when the baud rate changed
     */
    public static boolean baudRateChanged(Configuration before, Configuration after) {
        return !java.util.Objects.equals(before.getBaudRate(), after.getBaudRate());
    }

    /**
     * Checks whether the single-device multi-screen control can be used.
     *
     * @return true when more than one display is connected
     */
    public static boolean multipleDisplaysAvailable() {
        return availableMonitorChoices().size() > 1;
    }

    /**
     * Returns the monitor counts offered by the device tab for the detected displays.
     *
     * @param displayCount number of detected displays
     * @return one to three localized monitor choices
     */
    public static List<MonitorChoice> monitorChoices(int displayCount) {
        return IntStream.rangeClosed(1, Math.clamp(displayCount, 1, 3))
                .mapToObj(count -> new MonitorChoice(count, switch (count) {
                    case 2 -> CommonUtility.getWord(LabelKey.MULTIMONITOR_2);
                    case 3 -> CommonUtility.getWord(LabelKey.MULTIMONITOR_3);
                    default -> CommonUtility.getWord(LabelKey.MULTIMONITOR_1);
                })).toList();
    }

    /**
     * Detects displays and returns the matching monitor count choices for the web tab.
     *
     * @return localized monitor choices, defaulting to one when display detection is unavailable
     */
    public static List<MonitorChoice> availableMonitorChoices() {
        try {
            return monitorChoices(new DisplayManager().displayNumber());
        } catch (RuntimeException | LinkageError ignored) {
            return monitorChoices(1);
        }
    }

    /**
     * Programs a legacy serial device with the selected baud rate and current LED color.
     *
     * @param config edited configuration
     * @param red    current red channel
     * @param green  current green channel
     * @param blue   current blue channel
     */
    public static void programLegacyBaudRate(Configuration config, int red, int green, int blue) {
        Enums.BaudRate selected = Enums.BaudRate.findByExtendedVal(config.getBaudRate());
        if (selected == null) {
            throw new IllegalArgumentException("Invalid baud rate");
        }
        MainSingleton.getInstance().baudRate = selected.getBaudRateValue();
        new SerialManager().sendSerialParams(red, green, blue);
    }

    /**
     * Available monitor count and its localized label.
     */
    public record MonitorChoice(int count, String label) {
    }
}
