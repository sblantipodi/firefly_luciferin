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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
}

