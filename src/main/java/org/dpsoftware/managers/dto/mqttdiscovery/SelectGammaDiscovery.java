/*
  SelectGammaDiscovery.java

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
public class SelectGammaDiscovery implements DiscoveryObject {

    @JsonProperty("unique_id")
    String uniqueId;
    String mqttDiscoveryTopic;
    String name;
    @JsonProperty("state_topic")
    String stateTopic;
    @JsonProperty("command_template")
    String commandTemplate;
    @JsonProperty("command_topic")
    String commandTopic;
    String icon;
    String[] options;
    @JsonProperty("value_template")
    String valueTemplate;

    @Override
    public String getDiscoveryTopic() {
        return FireflyLuciferin.config.getMqttDiscoveryTopic() + "/select/" + getBaseGWDiscoveryTopic() + "/gamma/config";
    }

    @Override
    public String getCreateEntityStr() {
        this.name = generateUniqueName("Luciferin Gamma");
        this.uniqueId = this.name.replaceAll(" ", "_");
        this.stateTopic = "lights/" + getBaseFireflyDiscoveryTopic() + "/gamma";
        this.commandTemplate = "{\"gamma\":\"{{value}}\"}";
        this.commandTopic = "lights/" + getBaseFireflyDiscoveryTopic() + "/gamma";
        this.icon = "mdi:gamma";
        this.options = new String[]{
                "1.0",
                "1.2",
                "1.4",
                "1.6",
                "1.8",
                "2.0",
                "2.2",
                "2.4",
                "2.6",
                "2.8",
                "3.0",
                "4.0",
                "5.0",
                "6.0",
                "8.0",
                "10.0"
        };
        this.valueTemplate = "{{ value_json.gamma }}";
        return CommonUtility.toJsonString(this);
    }

    @Override
    public String getDestroyEntityStr() {
        return "";
    }
}