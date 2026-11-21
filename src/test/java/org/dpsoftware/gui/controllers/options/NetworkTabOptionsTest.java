/*
  NetworkTabOptionsTest.java

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

import org.dpsoftware.config.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTabOptionsTest {

    @Test
    void splitsAndRebuildsMqttServer() {
        NetworkTabOptions.MqttAddress address = NetworkTabOptions.splitServer("tcp://192.168.1.3:1883");
        assertEquals("192.168.1.3", address.host());
        assertEquals("1883", address.port());
        assertEquals("tcp://192.168.1.3:1883", NetworkTabOptions.server(address.host(), address.port()));
    }

    @Test
    void onlyFirmwareRelevantMqttChangesRequireProgramming() {
        Configuration before = new Configuration();
        before.setMqttServer("tcp://broker:1883");
        before.setMqttTopic("topic");
        before.setMqttUsername("user");
        before.setMqttPwd("password");
        Configuration after = new Configuration();
        after.setMqttServer(before.getMqttServer());
        after.setMqttTopic(before.getMqttTopic());
        after.setMqttUsername(before.getMqttUsername());
        after.setMqttPwd(before.getMqttPwd());
        after.setMqttDiscoveryTopic("different-discovery-topic");
        assertFalse(NetworkTabOptions.requiresDeviceProgramming(before, after));
        after.setMqttTopic("different-topic");
        assertTrue(NetworkTabOptions.requiresDeviceProgramming(before, after));
    }
}
