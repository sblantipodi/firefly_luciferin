/*
  ManagerSingleton.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dpsoftware.network.tcpUdp.UdpClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.Map;

/**
 * Manager singleton used to share common data
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("all")
public class ManagerSingleton {

    @Getter
    private final static ManagerSingleton instance;

    static {
        instance = new ManagerSingleton();
    }

    public MqttClient client;
    public Map<String, UdpClient> udpClient;
    public boolean pipelineStarting = false;
    public boolean pipelineStopping = false;
    public String lastEffectInUse = "";
    public boolean updateMqttDiscovery = false;
    public boolean serialVersionOk = false;
    public String deviceNameForSerialDevice = "";

}

