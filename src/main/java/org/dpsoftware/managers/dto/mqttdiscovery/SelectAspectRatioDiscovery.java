/*
  SelectAspectRatioDiscovery.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class SelectAspectRatioDiscovery extends DeviceDiscovery implements DiscoveryObject {

    String mqttDiscoveryTopic;
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
        return MainSingleton.getInstance().config.getMqttDiscoveryTopic() + "/select/" + getBaseGWDiscoveryTopic() + "/setaspectratio/config";
    }

    @Override
    public String getCreateEntityStr() {
        this.name = generateUniqueName("Aspect Ratio");
        this.uniqueId = this.name.replaceAll(" ", "_");
        this.stateTopic = "lights/" + getBaseFireflyDiscoveryTopic() + "/framerate";
        this.valueTemplate = "{{ value_json.aspectRatio }}";
        this.commandTopic = "lights/" + getBaseFireflyDiscoveryTopic() + "/setaspectratio";
        this.icon = "mdi:monitor-screenshot";
        this.options = new ArrayList<>();
        for (Enums.AspectRatio ar : Enums.AspectRatio.values()) {
            options.add(ar.getBaseI18n());
        }
        this.options.add(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS, Locale.ENGLISH));
        return CommonUtility.toJsonString(this);
    }

    @Override
    public String getDestroyEntityStr() {
        return "";
    }
}