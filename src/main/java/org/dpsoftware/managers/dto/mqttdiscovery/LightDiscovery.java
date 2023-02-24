/*
  LightDiscovery.java

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
public class LightDiscovery implements DiscoveryObject {

    @JsonProperty("unique_id")
    String uniqueId;
    String name;
    String schema;
    @JsonProperty("state_topic")
    String stateTopic;
    @JsonProperty("command_topic")
    String commandTopic;
    boolean effect;
    boolean brightness;
    boolean rgb;
    boolean optimistic;
    String icon;
    @JsonProperty("effect_list")
    String[] effectList;
    String brightness_state_topic, brightness_value_template;

    @Override
    public String getDiscoveryTopic() {
        return FireflyLuciferin.config.getMqttDiscoveryTopic() + "/light/" + getBaseGWDiscoveryTopic() + "/GlowWorm/config";
    }

    @Override
    public String getCreateEntityStr() {
        this.name = generateUniqueName("Glow Worm Luciferin");
        this.uniqueId = this.name.replaceAll(" ", "_");
        this.schema = "json";
        this.stateTopic = "lights/" + FireflyLuciferin.config.getMqttTopic();
        this.brightness_state_topic = "lights/" + FireflyLuciferin.config.getMqttTopic();
        this.brightness_value_template = "{{ value_json.brightness }}";
        this.commandTopic = "lights/" + FireflyLuciferin.config.getMqttTopic() + "/set";
        this.effect = true;
        this.brightness = true;
        this.rgb = true;
        this.optimistic = true;
        this.icon = "mdi:google-circles-communities";
        this.effectList = new String[]{
                "GlowWorm",
                "GlowWormWifi",
                "Bpm",
                "Fire",
                "Twinkle",
                "Rainbow",
                "Chase rainbow",
                "Solid rainbow",
                "Mixed rainbow",
                "Solid"
        };
        return CommonUtility.toJsonString(this);
    }

    @Override
    public String getDestroyEntityStr() {
        return "";
    }
}