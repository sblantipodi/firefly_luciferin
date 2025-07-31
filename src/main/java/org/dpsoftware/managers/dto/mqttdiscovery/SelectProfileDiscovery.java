/*
  SelectProfileDiscovery.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class SelectProfileDiscovery extends DeviceDiscovery implements DiscoveryObject {

    @JsonProperty("value_template")
    String valueTemplate;
    @JsonProperty("command_topic")
    String commandTopic;
    @JsonProperty("force_update")
    boolean forceUpdate;
    String icon;
    List<String> options;

    @Override
    public String getDiscoveryTopic() {
        return MainSingleton.getInstance().config.getMqttDiscoveryTopic() + "/select/" + getBaseFireflyDiscoveryTopic() + "/profile/config";
    }

    @Override
    public String getCreateEntityStr() {
        this.name = generateUniqueName("Profiles");
        this.uniqueId = this.name.replaceAll(" ", "_");
        this.commandTopic = "lights/" + getBaseFireflyDiscoveryTopic() + "/profile/set";
        this.stateTopic = "lights/" + getBaseFireflyDiscoveryTopic() + "/framerate";
        this.valueTemplate = "{{ value_json.profile | default('Default') }}";
        this.forceUpdate = true;
        this.icon = "mdi:folder-arrow-left-right";
        this.options = new ArrayList<>();
        StorageManager sm = new StorageManager();
        this.options.addAll(sm.listProfilesForThisInstance());
        this.options.add(CommonUtility.getWord(Constants.DEFAULT));
        return CommonUtility.toJsonString(this);
    }

    @Override
    public String getDestroyEntityStr() {
        return "";
    }
}