/*
  SwitchRebootDiscovery.java

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
package org.dpsoftware.managers.dto.mqttdiscovery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.utilities.CommonUtility;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class SwitchRebootDiscovery extends DeviceDiscovery implements DiscoveryObject {

    String stateTopic;
    @JsonProperty("command_topic")
    String commandTopic;
    int qos;
    boolean retain;
    @JsonProperty("payload_press")
    String payloadPress;
    String icon;

    @Override
    public String getDiscoveryTopic() {
        return MainSingleton.getInstance().config.getMqttDiscoveryTopic() + "/button/" + getBaseGWDiscoveryTopic() + "/rebootglowworm/config";
    }

    @Override
    public String getCreateEntityStr() {
        this.name = generateUniqueName("Reboot Glow Worm");
        this.uniqueId = this.name.replaceAll(" ", "_");
        this.stateTopic = "stat/" + MainSingleton.getInstance().config.getMqttTopic() + "/reboot";
        this.commandTopic = "cmnd/" + MainSingleton.getInstance().config.getMqttTopic() + "/reboot";
        this.qos = 1;
        this.retain = false;
        this.payloadPress = "OFF";
        this.icon = "mdi:restart";
        return CommonUtility.toJsonString(this);
    }

    @Override
    public String getDestroyEntityStr() {
        return "";
    }
}