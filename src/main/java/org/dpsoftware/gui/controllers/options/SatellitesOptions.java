/*
  SatellitesOptions.java

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

import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.utilities.CommonUtility;

import java.util.*;

/**
 * Choices and satellite conversion shared by the JavaFX dialog and web settings.
 */
public final class SatellitesOptions {

    /**
     * Prevents instantiation.
     */
    private SatellitesOptions() {
    }

    /**
     * Returns common and LED matrix zones, sorted like the dialog selector.
     *
     * @param config current configuration
     * @return unique selectable zones
     */
    public static List<Choice> zones(Configuration config) {
        Map<String, Choice> choices = new java.util.TreeMap<>();
        for (Enums.PossibleZones zone : Enums.PossibleZones.values()) {
            choices.put(zone.getI18n(), new Choice(zone.getBaseI18n(), zone.getI18n()));
        }
        if (config != null && config.getLedMatrix() != null) {
            LinkedHashMap<Integer, LEDCoordinate> matrix = config.getLedMatrixInUse(config.getDefaultLedMatrix());
            if (matrix != null) {
                for (LEDCoordinate coordinate : matrix.values()) {
                    String zone = coordinate.getZone();
                    if (zone != null && !zone.isBlank()) {
                        choices.putIfAbsent(zone, new Choice(zone, zone));
                    }
                }
            }
        }
        return new ArrayList<>(choices.values());
    }

    /**
     * Returns the direction choices shown in the satellite dialog.
     *
     * @return direction choices
     */
    public static List<Choice> directions() {
        return java.util.Arrays.stream(Enums.Direction.values())
                .map(direction -> new Choice(direction.getBaseI18n(), direction.getI18n())).toList();
    }

    /**
     * Returns the algorithm choices shown in the satellite dialog.
     *
     * @return algorithm choices
     */
    public static List<Choice> algorithms() {
        return java.util.Arrays.stream(Enums.Algo.values())
                .map(algo -> new Choice(algo.getBaseI18n(), algo.getI18n())).toList();
    }

    /**
     * Returns connected devices that are neither the main device nor assigned satellites.
     *
     * @param config  current configuration
     * @param devices connected devices
     * @return selectable satellite IPs and device names
     */
    public static List<Choice> availableDevices(Configuration config, Collection<GlowWormDevice> devices) {
        if (config == null || devices == null) {
            return List.of();
        }
        return devices.stream()
                .filter(device -> device.getDeviceIP() != null && NetworkManager.isValidIp(device.getDeviceIP()))
                .filter(device -> !Objects.equals(device.getDeviceIP(), config.getStaticGlowWormIp()))
                .filter(device -> !Objects.equals(device.getDeviceName(), config.getOutputDevice()))
                .filter(device -> config.getSatellites() == null || !config.getSatellites().containsKey(device.getDeviceIP()))
                .map(device -> new Choice(device.getDeviceIP(), device.getDeviceName() + " (" + device.getDeviceIP() + ")"))
                .toList();
    }

    /**
     * Extracts an IP from a typed address or a device label containing one in parentheses.
     *
     * @param selection typed or selected device
     * @return the IP address
     */
    public static String deviceIp(String selection) {
        if (selection == null) {
            return "";
        }
        String trimmed = selection.trim();
        int open = trimmed.lastIndexOf('(');
        int close = trimmed.lastIndexOf(')');
        return open >= 0 && close > open ? trimmed.substring(open + 1, close).trim() : trimmed;
    }

    /**
     * Converts the LED count entered in the dialog to a positive number.
     *
     * @param text entered LED count
     * @return positive count as text
     */
    public static String ledCount(String text) {
        int count = Integer.parseInt(text);
        return String.valueOf(Math.max(1, count));
    }

    /**
     * Stores satellite rows using base language values and resolved device names.
     *
     * @param config  configuration to update
     * @param rows    edited satellite rows
     * @param devices connected devices used to resolve names
     */
    public static void apply(Configuration config, Collection<Satellite> rows, Collection<GlowWormDevice> devices) {
        Map<String, Satellite> updated = new LinkedHashMap<>();
        for (Satellite row : rows) {
            String ip = deviceIp(row.getDeviceIp());
            if (!NetworkManager.isValidIp(ip)) {
                throw new IllegalArgumentException("Invalid satellite IP");
            }
            String zone = row.getZone();
            if (zone == null || zone.isBlank()) {
                throw new IllegalArgumentException("Satellite zone is required");
            }
            String direction = LocalizedEnum.fromTextToBase(Enums.Direction.class, row.getOrientation());
            String algorithm = LocalizedEnum.fromTextToBase(Enums.Algo.class, row.getAlgo());
            if (LocalizedEnum.fromBaseStr(Enums.Direction.class, direction) == null
                    || LocalizedEnum.fromBaseStr(Enums.Algo.class, algorithm) == null) {
                throw new IllegalArgumentException("Invalid satellite direction or algorithm");
            }
            String name = devices == null ? null : devices.stream()
                    .filter(device -> ip.equals(device.getDeviceIP()))
                    .map(GlowWormDevice::getDeviceName).findFirst().orElse(null);
            if (name == null || name.isBlank()) {
                name = row.getDeviceName() == null || row.getDeviceName().isBlank()
                        ? "GW" + (new Random().nextInt(9000) + 1000) : row.getDeviceName();
            }
            String storedZone = CommonUtility.isCommonZone(zone)
                    ? LocalizedEnum.fromTextToBase(Enums.PossibleZones.class, zone) : zone;
            updated.put(ip, new Satellite(storedZone, direction, ledCount(row.getLedNum()), ip, name, algorithm));
        }
        config.getSatellites().clear();
        config.getSatellites().putAll(updated);
    }

    /**
     * Selectable stored value and localized display label.
     */
    public record Choice(String value, String label) {
    }
}
