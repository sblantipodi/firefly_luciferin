/*
  NetworkSingleton.java

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
package org.dpsoftware.network;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Network singleton used to share common data
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class NetworkSingleton {

    @Getter
    private final static NetworkSingleton instance;

    static {
        instance = new NetworkSingleton();
    }

    public boolean udpBroadcastReceiverRunning = false;
    public MessageClient msgClient;
    public boolean closeServer = false;
    public int totalLedNum = MainSingleton.getInstance().ledNumber;
    public MessageServer messageServer;
    public String rleMapInUse = "";

    /**
     * Orders the zoned list based on the zones and monitor numbers.
     * This method will reorder the colors according to the zones and monitor numbers.
     *
     * @param zonedList   List of ZonedLedCoordinate objects containing zone and color information.
     * @param config2     Configuration for the second monitor (central).
     * @param orderedList List to store the ordered colors.
     */
    private static void orderZonedList(List<ZonedLedCoordinate> zonedList, Configuration config2, List<Color> orderedList) {
        // Find elements with zone TOP and monitorNumber 1, 2, 3
        List<Integer> topIndices = new ArrayList<>();
        List<ZonedLedCoordinate> topElements = new ArrayList<>();
        for (int idx = 0; idx < zonedList.size(); idx++) {
            ZonedLedCoordinate z = zonedList.get(idx);
            if (z.getZone() == Enums.PossibleZones.TOP) {
                topIndices.add(idx);
                topElements.add(z);
            }
        }
        // Order the elements in topElements by monitorNumber
        topElements.sort(Comparator.comparingInt(ZonedLedCoordinate::getMonitorNumber));
        // Put the ordered elements back into the original list at the same indices
        for (int idx = 0; idx < topIndices.size(); idx++) {
            zonedList.set(topIndices.get(idx), topElements.get(idx));
        }
        // Used when not splitting bottom rows, no need to split bottom rows on a dual monitor setup.
        // Triple monitor can split bottom rows on the central monitor only.
        if (MainSingleton.getInstance().config.getMultiMonitor() == 2
                || (MainSingleton.getInstance().config.getMultiMonitor() == 3 && !CommonUtility.isSplitBottomRow(config2.getSplitBottomMargin()))) {
            for (Enums.PossibleZones zone : Enums.PossibleZones.values()) {
                zonedList.forEach(zonedItem -> {
                    if (zonedItem.getZone() == zone) {
                        orderedList.add(zonedItem.getColor());
                    }
                });
            }
        } else {
            manageBottomRowSplit(zonedList, orderedList);
        }
    }

    /**
     * Manage the split of the bottom row for the zoned list.
     * This method orders the colors based on the zones and monitor numbers.
     *
     * @param zonedList   List of ZonedLedCoordinate objects containing zone and color information.
     * @param orderedList List to store the ordered colors.
     */
    private static void manageBottomRowSplit(List<ZonedLedCoordinate> zonedList, List<Color> orderedList) {
        zonedList.forEach(zonedItem -> {
            if (zonedItem.getZone() == Enums.PossibleZones.BOTTOM_RIGHT && zonedItem.getMonitorNumber() == 2) {
                orderedList.add(zonedItem.getColor());
            }
        });
        zonedList.forEach(zonedItem -> {
            if (zonedItem.getZone() == Enums.PossibleZones.BOTTOM && zonedItem.getMonitorNumber() == 1) {
                orderedList.add(zonedItem.getColor());
            }
        });
        zonedList.forEach(zonedItem -> {
            if (zonedItem.getZone() == Enums.PossibleZones.RIGHT && zonedItem.getMonitorNumber() == 1) {
                orderedList.add(zonedItem.getColor());
            }
        });
        zonedList.forEach(zonedItem -> {
            if (zonedItem.getZone() == Enums.PossibleZones.TOP && zonedItem.getMonitorNumber() == 1) {
                orderedList.add(zonedItem.getColor());
            }
        });
        zonedList.forEach(zonedItem -> {
            if (zonedItem.getZone() == Enums.PossibleZones.TOP && zonedItem.getMonitorNumber() == 2) {
                orderedList.add(zonedItem.getColor());
            }
        });
        zonedList.forEach(zonedItem -> {
            if (zonedItem.getZone() == Enums.PossibleZones.TOP && zonedItem.getMonitorNumber() == 3) {
                orderedList.add(zonedItem.getColor());
            }
        });
        zonedList.forEach(zonedItem -> {
            if (zonedItem.getZone() == Enums.PossibleZones.LEFT && zonedItem.getMonitorNumber() == 3) {
                orderedList.add(zonedItem.getColor());
            }
        });
        zonedList.forEach(zonedItem -> {
            if (zonedItem.getZone() == Enums.PossibleZones.BOTTOM && zonedItem.getMonitorNumber() == 3) {
                orderedList.add(zonedItem.getColor());
            }
        });
        zonedList.forEach(zonedItem -> {
            if (zonedItem.getZone() == Enums.PossibleZones.BOTTOM_LEFT && zonedItem.getMonitorNumber() == 2) {
                orderedList.add(zonedItem.getColor());
            }
        });
    }

    /**
     * Orders the array of colors based on the zoned LED coordinates.
     * This method will reorder the colors according to the zones and monitor numbers.
     *
     * @param colorArray Array of Color objects to be ordered. Anticlockwise order by default, reverse happens before sending.
     */
    public void orderArray(Color[] colorArray) {
        Configuration config1 = NetworkSingleton.getInstance().messageServer.getMonitorConfig1();
        Configuration config2 = NetworkSingleton.getInstance().messageServer.getMonitorConfig2();
        Configuration config3 = NetworkSingleton.getInstance().messageServer.getMonitorConfig3();
        List<ZonedLedCoordinate> zonedList = new ArrayList<>();
        List<ZonedLedCoordinate> zonedList1 = new ArrayList<>();
        List<ZonedLedCoordinate> zonedList2 = new ArrayList<>();
        List<ZonedLedCoordinate> zonedList3 = new ArrayList<>();
        List<Color> orderedList = new ArrayList<>();
        AtomicInteger i = new AtomicInteger();
        config1.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()).forEach((_, value) -> {
            if (CommonUtility.isCommonZone(value.getZone())) {
                zonedList1.add(new ZonedLedCoordinate(1, LocalizedEnum.fromBaseStr(Enums.PossibleZones.class, value.getZone()), colorArray[i.getAndIncrement()]));
            }
        });
        config2.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()).forEach((_, value) -> {
            if (CommonUtility.isCommonZone(value.getZone())) {
                zonedList2.add(new ZonedLedCoordinate(2, LocalizedEnum.fromBaseStr(Enums.PossibleZones.class, value.getZone()), colorArray[i.getAndIncrement()]));
            }
        });
        if (MainSingleton.getInstance().config.getMultiMonitor() == 3) {
            config3.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()).forEach((_, value) -> {
                if (CommonUtility.isCommonZone(value.getZone())) {
                zonedList3.add(new ZonedLedCoordinate(3, LocalizedEnum.fromBaseStr(Enums.PossibleZones.class, value.getZone()), colorArray[i.getAndIncrement()]));
                }
            });
            zonedList.addAll(zonedList3);
        }
        zonedList.addAll(zonedList2);
        zonedList.addAll(zonedList1);
        orderZonedList(zonedList, config2, orderedList);
        orderedList.toArray(colorArray);
    }

    /**
     * Check if the multi screen single device LED stream must be reordered.
     * Reordering is needed when at least one monitor does not have a full strip around all its sides.
     *
     * @return true if LED data must be reordered before sending it to the device
     */
    public boolean isLedOrderRequired() {
        if (messageServer == null || !CommonUtility.isSingleDeviceMultiScreen()) {
            return false;
        }
        if (messageServer.getMonitorConfig1() == null || messageServer.getMonitorConfig2() == null
                || (MainSingleton.getInstance().config.getMultiMonitor() == 3 && messageServer.getMonitorConfig3() == null)) {
            return false;
        }
        if (MainSingleton.getInstance().config.getMultiMonitor() == 3) {
            return messageServer.getMonitorConfig1().getLeftLed() == 0
                    && messageServer.getMonitorConfig2().getLeftLed() == 0
                    && messageServer.getMonitorConfig2().getRightLed() == 0
                    && messageServer.getMonitorConfig3().getRightLed() == 0;
        }
        if (MainSingleton.getInstance().config.getMultiMonitor() == 2) {
            return messageServer.getMonitorConfig1().getLeftLed() == 0
                    && messageServer.getMonitorConfig2().getRightLed() == 0;
        }
        return false;
    }

    /**
     * Generates a new map with the updated groupedLed status based on color changes,
     * leaving the original input map completely unaltered (Deep Copy).
     *
     * @param leds The current array of LED colors
     * @return A new LinkedHashMap with the same keys and values as the original, but with the groupedLed status updated according to the color changes in the leds array.
     */
    @SuppressWarnings("all")
    public static LinkedHashMap<Integer, LEDCoordinate> builtRleLeaders(Color[] leds) {
        // Apply the dynamic color based grouping logic on the cloned matrix
        Color tmpC = null;
        LinkedHashMap<Integer, LEDCoordinate> clonedMatrix = new LinkedHashMap<>();
        int consecutiveFollowers = 0;
        for (int i = 0; i < leds.length; i++) {
            LEDCoordinate coord = new LEDCoordinate();
            if (coord != null) { // Safe check in case the leds array is longer than the matrix
                // If it's the very first LED or if its color is different from the previous one
                if (consecutiveFollowers == 254 || MainSingleton.getInstance().config.getGroupBy() == 10 || tmpC == null || leds[i].getRGB() != tmpC.getRGB()) {
                    // This LED becomes the new group leader
                    tmpC = leds[i];
                    coord.setGroupedLed(false); // It is not grouped, it's the leader
                    consecutiveFollowers = 0;
                } else {
                    // The color is IDENTICAL to the previous one, so it joins the current group
                    coord.setGroupedLed(true); // It is grouped (follower)
                    consecutiveFollowers++;
                }
            }
            if (i == 0) {
                coord.setGroupedLed(false);
                consecutiveFollowers = 0;
            }
            clonedMatrix.put(i + 1, coord);
        }
        // Return the duplicated and modified map
        return clonedMatrix;
    }

    /**
     * Print RLE maps for debugging purposes, only if debug logging is enabled and the RLE map has changed since the last print to avoid log flooding.
     *
     * @param ledMatrixWithLeaders array of leaders
     * @param groups               string builder containing the RLE groups
     * @param length               total number of LEDs in the strip
     */
    public static void printVisualRleMap(LinkedHashMap<Integer, LEDCoordinate> ledMatrixWithLeaders, StringBuilder groups, int length) {
        StringBuilder visual = new StringBuilder();
        int leadersCount = 0;
        for (LEDCoordinate coord : ledMatrixWithLeaders.values()) {
            if (!coord.isGroupedLed()) {
                leadersCount++;
                visual.append("█");
            } else {
                visual.append("░");
            }
        }
        int groupsSum = 0;
        Pattern pattern = Pattern.compile("(\\d+)x(\\d+)");
        Matcher matcher = pattern.matcher(groups);
        while (matcher.find()) {
            int qty = Integer.parseInt(matcher.group(1));
            int val = Integer.parseInt(matcher.group(2));
            groupsSum += (qty * val);
        }
        log.trace(visual.toString());
        visual = new StringBuilder();
        visual.append("[")
                .append("LEDs: ")
                .append(length)
                .append(", Total sum of groups: ")
                .append(groupsSum)
                .append(", Group by: ")
                .append(MainSingleton.getInstance().config.getGroupBy())
                .append(", Leaders: ")
                .append(leadersCount)
                .append("]");
        log.trace(visual.toString());

        if (length != groupsSum) {
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            log.error("AAAAAAAAAAAAAAAAAAAAAAAAAEERRR");
            System.exit(0);
        }
    }

}
