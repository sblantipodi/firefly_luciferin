/*
  SwitchSolidEffectDiscovery.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.utilities.CommonUtility;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class SwitchSolidEffectDiscovery implements DiscoveryObject {

    @JsonProperty("unique_id")
    String uniqueId;
    String name;
    @JsonProperty("state_topic")
    String stateTopic;
    @JsonProperty("command_topic")
    String commandTopic;
    @JsonProperty("payload_on")
    String payloadOn;
    @JsonProperty("payload_off")
    String payloadOff;
    @JsonProperty("value_template")
    String valueTemplate;
    @JsonProperty("state_off")
    boolean stateOff;
    @JsonProperty("state_on")
    boolean stateOn;
    String icon;

    @Override
    public String getDiscoveryTopic() {
        return FireflyLuciferin.config.getMqttDiscoveryTopic() + "/switch/" + getBaseGWDiscoveryTopic() + "/solideffect/config";
    }

    @Override
    public String getCreateEntityStr() {
        this.name = generateUniqueName("Luciferin Solid Effect");
        this.uniqueId = this.name.replaceAll(" ", "_");
        this.stateTopic = "lights/" + getBaseFireflyDiscoveryTopic() + "/framerate";
        this.commandTopic = "lights/" + FireflyLuciferin.config.getMqttTopic();
        this.payloadOn = "{\"state\":\"ON\",\"startStopInstances\":\"PLAY\"}";
        this.payloadOff = "{\"state\":\"ON\",\"startStopInstances\":\"STOP\"}";
        this.valueTemplate = "{{ value_json.producing | int > 0 }}";
        this.icon = "mdi:television-ambient-light";
        this.stateOn = true;
        this.stateOff = false;
        return CommonUtility.toJsonString(this);
    }

    @Override
    public String getDestroyEntityStr() {
        return "";
    }
}