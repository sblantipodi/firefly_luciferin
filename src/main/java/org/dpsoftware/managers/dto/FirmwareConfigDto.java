/*
  FirmwareConfigDto.java

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
package org.dpsoftware.managers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class FirmwareConfigDto {
    String deviceName;
    String microcontrollerIP;
    boolean mqttCheckbox;
    String ssid;
    String wifipwd;
    String mqttIP;
    String mqttPort;
    String mqttTopic;
    String mqttuser;
    String mqttpass;
    Integer gpio;
    Integer gpioClock;
    String additionalParam;
    String colorMode;
    String colorOrder;
    int br;
    String lednum;
    @JsonProperty("MAC")
    String MAC;
    Integer ldrPin;
    Integer relayPin;
    Boolean relayInv;
    Integer sbPin;
    Integer ledBuiltin;
}