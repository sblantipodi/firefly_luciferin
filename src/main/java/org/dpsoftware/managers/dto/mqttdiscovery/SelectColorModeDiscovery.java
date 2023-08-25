/*
  SelectColorModeDiscovery.java

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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.utilities.CommonUtility;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class SelectColorModeDiscovery implements DiscoveryObject {

    @JsonProperty("unique_id")
    String uniqueId;
    String name;
    @JsonProperty("state_topic")
    String stateTopic;
    @JsonProperty("command_template")
    String commandTemplate;
    @JsonProperty("command_topic")
    String commandTopic;
    String icon;
    List<String> options;
    @JsonProperty("value_template")
    String valueTemplate;

    @Override
    public String getDiscoveryTopic() {
        return MainSingleton.getInstance().config.getMqttDiscoveryTopic() + "/select/device" + CommonUtility.getDeviceToUse().getMac().replace(":", "") + "/colormode/config";
    }

    @Override
    public String getCreateEntityStr() {
        this.name = generateUniqueName("Luciferin Color Mode" + " " + CommonUtility.getDeviceToUse().getDeviceName());
        this.uniqueId = this.name.replaceAll(" ", "_") + CommonUtility.getDeviceToUse().getMac().replace(":", "");
        this.stateTopic = "lights/" + getBaseFireflyDiscoveryTopic() + "/framerate";
        this.valueTemplate = "{{ value_json.colorMode }}";
        this.options = new ArrayList<>();
        StringBuilder colorModeIndex = new StringBuilder();
        int cntOutput = 0;
        for (Enums.ColorMode colorMode : Enums.ColorMode.values()) {
            int ordinal = colorMode.ordinal();
            colorModeIndex.append("{% ").append((cntOutput == 0) ? "if" : "elif").append(" value == '").append(colorMode.getBaseI18n()).append("' %}").append(++ordinal);
            options.add(colorMode.getBaseI18n());
            cntOutput++;
        }
        colorModeIndex.append("{% endif %}");
        this.commandTopic = Constants.TOPIC_GLOW_WORM_FIRM_CONFIG;
        this.commandTemplate = "{\"colorMode\":\"" + colorModeIndex + "\",\"MAC\":\"" + CommonUtility.getDeviceToUse().getMac() + "\"}";
        this.icon = "mdi:palette";
        return CommonUtility.toJsonString(this);
    }

    @Override
    public String getDestroyEntityStr() {
        return "";
    }
}