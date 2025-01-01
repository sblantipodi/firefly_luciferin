/*
  SensorVersionDiscovery.java

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
import org.dpsoftware.utilities.CommonUtility;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class SensorVersionDiscovery extends DeviceDiscovery implements DiscoveryObject {

    @JsonProperty("value_template")
    String valueTemplate;
    @JsonProperty("unit_of_measurement")
    String unitOfMeasurement;
    @JsonProperty("force_update")
    boolean forceUpdate;
    String icon;

    @Override
    public String getDiscoveryTopic() {
        return MainSingleton.getInstance().config.getMqttDiscoveryTopic() + "/sensor/" + getBaseGWDiscoveryTopic() + "/GlowWorm_Version/config";
    }

    @Override
    public String getCreateEntityStr() {
        this.name = generateUniqueName("Glow Worm Version");
        this.uniqueId = this.name.replaceAll(" ", "_");
        this.stateTopic = "lights/" + MainSingleton.getInstance().config.getMqttTopic();
        this.valueTemplate = "{{ value_json.ver if value_json.ver is defined else this.state }}";
        this.forceUpdate = true;
        this.icon = "mdi:numeric";
        return CommonUtility.toJsonString(this);
    }

    @Override
    public String getDestroyEntityStr() {
        return "";
    }
}