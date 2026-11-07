/*
  NetworkSingletonOrderArrayTest.java

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

import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkSingletonOrderArrayTest {

    private static Configuration config(String splitBottomMargin, int leftLed, int rightLed, ZoneCount... zones) {
        Configuration config = new Configuration();
        config.setSplitBottomMargin(splitBottomMargin);
        config.setLeftLed(leftLed);
        config.setRightLed(rightLed);
        LinkedHashMap<Integer, LEDCoordinate> matrix = new LinkedHashMap<>();
        for (ZoneCount zone : zones) {
            for (int i = 0; i < zone.count(); i++) {
                LEDCoordinate coordinate = new LEDCoordinate();
                coordinate.setZone(zone.zone().getBaseI18n());
                matrix.put(matrix.size() + 1, coordinate);
            }
        }
        LinkedHashMap<String, LinkedHashMap<Integer, LEDCoordinate>> matrices = new LinkedHashMap<>();
        matrices.put(Enums.AspectRatio.FULLSCREEN.getBaseI18n(), matrix);
        config.setLedMatrix(matrices);
        return config;
    }

    @Test
    void tripleMonitorWithSplitOuterBottomRowsPreservesEveryColor() {
        MainSingleton main = MainSingleton.getInstance();
        NetworkSingleton network = NetworkSingleton.getInstance();
        Configuration previousConfig = main.config;
        MessageServer previousServer = network.messageServer;
        try {
            Configuration config1 = config("15%", 0, 22,
                    new ZoneCount(Enums.PossibleZones.BOTTOM_RIGHT, 17),
                    new ZoneCount(Enums.PossibleZones.RIGHT, 22),
                    new ZoneCount(Enums.PossibleZones.TOP, 10));
            Configuration config2 = config("15%", 0, 0,
                    new ZoneCount(Enums.PossibleZones.BOTTOM_RIGHT, 10),
                    new ZoneCount(Enums.PossibleZones.TOP, 20),
                    new ZoneCount(Enums.PossibleZones.BOTTOM_LEFT, 10));
            Configuration config3 = config("15%", 22, 0,
                    new ZoneCount(Enums.PossibleZones.TOP, 10),
                    new ZoneCount(Enums.PossibleZones.LEFT, 22),
                    new ZoneCount(Enums.PossibleZones.BOTTOM_LEFT, 17));
            config1.setMultiMonitor(3);
            config1.setMultiScreenSingleDevice(true);
            main.config = config1;
            MessageServer server = new MessageServer();
            server.setMonitorConfig1(config1);
            server.setMonitorConfig2(config2);
            server.setMonitorConfig3(config3);
            network.messageServer = server;

            Color[] colors = new Color[138];
            for (int i = 0; i < colors.length; i++) colors[i] = new Color(i);
            Color[] original = colors.clone();

            assertTrue(network.isLedOrderRequired());
            network.orderArray(colors);

            Color[] expected = new Color[colors.length];
            int offset = 0;
            for (int[] range : new int[][]{{49, 59}, {0, 49}, {59, 79}, {89, 138}, {79, 89}}) {
                System.arraycopy(original, range[0], expected, offset, range[1] - range[0]);
                offset += range[1] - range[0];
            }
            assertArrayEquals(expected, colors);
        } finally {
            main.config = previousConfig;
            network.messageServer = previousServer;
        }
    }

    @Test
    void incompleteOrderingKeepsOriginalFrame() {
        MainSingleton main = MainSingleton.getInstance();
        NetworkSingleton network = NetworkSingleton.getInstance();
        Configuration previousConfig = main.config;
        MessageServer previousServer = network.messageServer;
        try {
            Configuration config1 = config("15%", 0, 0,
                    new ZoneCount(Enums.PossibleZones.TOP, 1));
            Configuration config2 = config("15%", 0, 0,
                    new ZoneCount(Enums.PossibleZones.TOP, 1));
            Configuration config3 = config("15%", 0, 0,
                    new ZoneCount(Enums.PossibleZones.RIGHT, 1));
            config1.setMultiMonitor(3);
            config1.setMultiScreenSingleDevice(true);
            main.config = config1;
            MessageServer server = new MessageServer();
            server.setMonitorConfig1(config1);
            server.setMonitorConfig2(config2);
            server.setMonitorConfig3(config3);
            network.messageServer = server;

            Color[] colors = {Color.RED, Color.GREEN, Color.BLUE};
            Color[] original = Arrays.copyOf(colors, colors.length);
            network.orderArray(colors);

            assertArrayEquals(original, colors);
        } finally {
            main.config = previousConfig;
            network.messageServer = previousServer;
        }
    }

    private record ZoneCount(Enums.PossibleZones zone, int count) {
    }
}
