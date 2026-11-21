/*
  DeviceDto.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020   2026  Davide Perini  (https://github.com/sblantipodi)

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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dpsoftware.gui.elements.GlowWormDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain data holder for a connected device, JSON friendly (no JavaFX types).
 */
@Data
@AllArgsConstructor
public class DeviceDto {
    private String deviceName;
    private String deviceIP;
    private boolean dhcpInUse;
    private String wifi;
    private String deviceVersion;
    private String deviceBoard;
    private String mac;
    private String gpio;
    private String gpioClock;
    private String ledBuiltin;
    private String firmwareType;
    private String baudRate;
    private String mqttTopic;
    private String colorMode;
    private String colorOrder;
    private String ldrValue;
    private String ldrPin;
    private String relayPin;
    private Boolean relayInvertedPin;
    private String sbPin;
    private String numberOfLEDSconnected;
    private String lastSeen;

    /**
     * Normalize the in memory connected devices into a plain, JSON serializable list.
     * {@link GlowWormDevice} holds a JavaFX {@code Hyperlink} for the device IP which Jackson cannot
     * serialize, so each device is mapped to a {@link DeviceDto} using the plain getter values.
     *
     * @param devices the connected devices from the device table
     * @return a list of serializable device DTOs
     */
    public static List<DeviceDto> fromDevices(Iterable<GlowWormDevice> devices) {
        List<DeviceDto> dtos = new ArrayList<>();
        if (devices == null) {
            return dtos;
        }
        for (GlowWormDevice device : devices) {
            dtos.add(new DeviceDto(
                    device.getDeviceName(),
                    device.getDeviceIP(),
                    device.isDhcpInUse(),
                    device.getWifi(),
                    device.getDeviceVersion(),
                    device.getDeviceBoard(),
                    device.getMac(),
                    device.getGpio(),
                    device.getGpioClock(),
                    device.getLedBuiltin(),
                    device.getFirmwareType(),
                    device.getBaudRate(),
                    device.getMqttTopic(),
                    device.getColorMode(),
                    device.getColorOrder(),
                    device.getLdrValue(),
                    device.getLdrPin(),
                    device.getRelayPin(),
                    device.getRelayInvertedPin(),
                    device.getSbPin(),
                    device.getNumberOfLEDSconnected(),
                    device.getLastSeen()));
        }
        return dtos;
    }
}
