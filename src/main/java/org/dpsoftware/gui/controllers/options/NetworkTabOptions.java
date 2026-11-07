/*
  NetworkTabOptions.java

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
package org.dpsoftware.gui.controllers.options;

import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.managers.dto.mqttdiscovery.*;
import org.dpsoftware.utilities.CommonUtility;

import java.util.Objects;

/**
 * MQTT values and firmware settings shared by the JavaFX and web network tabs.
 */
public final class NetworkTabOptions {

    /**
     * Prevents instantiation.
     */
    private NetworkTabOptions() {
    }

    /**
     * Splits the stored MQTT server into its host and port controls.
     *
     * @param server stored MQTT server URL
     * @return host and port
     */
    public static MqttAddress splitServer(String server) {
        String address = server == null ? "" : server;
        if (address.startsWith(Constants.DEFAULT_MQTT_PROTOCOL)) {
            address = address.substring(Constants.DEFAULT_MQTT_PROTOCOL.length());
        }
        int separator = address.lastIndexOf(':');
        return separator > 0
                ? new MqttAddress(address.substring(0, separator), address.substring(separator + 1))
                : new MqttAddress(address, Constants.DEFAULT_MQTT_PORT);
    }

    /**
     * Creates the stored MQTT server URL from the host and port controls.
     *
     * @param host MQTT host
     * @param port MQTT port
     * @return stored MQTT server URL
     */
    public static String server(String host, String port) {
        return Constants.DEFAULT_MQTT_PROTOCOL + host + ":" + port;
    }

    /**
     * Checks whether the MQTT settings that require device programming changed.
     *
     * @param before saved configuration
     * @param after  edited configuration
     * @return true when device MQTT settings changed
     */
    public static boolean requiresDeviceProgramming(Configuration before, Configuration after) {
        return before.isWirelessStream() != after.isWirelessStream()
                || before.isMqttEnable() != after.isMqttEnable()
                || !Objects.equals(before.getMqttServer(), after.getMqttServer())
                || !Objects.equals(before.getMqttTopic(), after.getMqttTopic())
                || !Objects.equals(before.getMqttUsername(), after.getMqttUsername())
                || !Objects.equals(before.getMqttPwd(), after.getMqttPwd());
    }

    /**
     * Publishes every MQTT discovery entity to add or remove the device.
     *
     * @param createEntity whether to add the entities
     */
    public static void publishDiscoveryTopics(boolean createEntity) {
        publishDiscoveryTopic(new SensorLastUpdateFFDiscovery(), createEntity);
        publishDiscoveryTopic(new LightDiscovery(), createEntity);
        publishDiscoveryTopic(new NumberWhiteTempDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectGammaDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectEmaDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectFrameGenDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectProfileDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectCubeLutDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorConsumingDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorProducingDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorVersionDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorLedsDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorLastUpdateDiscovery(), createEntity);
        publishDiscoveryTopic(new SwitchRebootDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectAspectRatioDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorAspectRatioDiscovery(), createEntity);
        if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac() != null) {
            publishDiscoveryTopic(new SelectColorModeDiscovery(), createEntity);
        }
        publishDiscoveryTopic(new SelectEffectDiscovery(), createEntity);
        publishDiscoveryTopic(new SwitchBiasLightDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorGWConsumingDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorGpioDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorWiFiDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorGammaDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorLdrDiscovery(), createEntity);
    }

    /**
     * Publishes one MQTT discovery entity.
     *
     * @param discoveryObject discovery entity
     * @param createEntity    whether to add the entity
     */
    public static void publishDiscoveryTopic(DiscoveryObject discoveryObject, boolean createEntity) {
        NetworkManager.publishToTopic(discoveryObject.getDiscoveryTopic(), createEntity
                ? discoveryObject.getCreateEntityStr() : discoveryObject.getDestroyEntityStr(), false, true, 0);
        CommonUtility.sleepMilliseconds(Constants.MQTT_DISCOVERY_CALL_DELAY);
    }

    /**
     * Builds the firmware configuration using a selected device and MQTT settings.
     *
     * @param config    configuration to program
     * @param device    selected device
     * @param colorMode firmware color mode
     * @param baudRate  selected baud rate when it changed, otherwise null
     * @return firmware configuration message
     */
    public static FirmwareConfigDto firmwareConfig(Configuration config, GlowWormDevice device,
                                                   String colorMode, String baudRate) {
        FirmwareConfigDto dto = new FirmwareConfigDto();
        dto.setDeviceName(device.getDeviceName());
        dto.setMicrocontrollerIP(device.isDhcpInUse() ? "" : device.getDeviceIP());
        dto.setMqttCheckbox(config.isMqttEnable());
        dto.setSsid("");
        dto.setWifipwd("");
        if (config.isMqttEnable()) {
            MqttAddress address = splitServer(config.getMqttServer());
            dto.setMqttIP(address.host());
            dto.setMqttPort(address.port());
            dto.setMqttTopic(config.getMqttTopic());
            dto.setMqttuser(config.getMqttUsername());
            dto.setMqttpass(config.getMqttPwd());
        }
        dto.setAdditionalParam(device.getGpio());
        dto.setColorMode(colorMode);
        String effectiveBaudRate = baudRate != null ? baudRate
                : device.getBaudRate() == null || device.getBaudRate().isEmpty()
                ? config.getBaudRate() : device.getBaudRate();
        try {
            dto.setBr(Enums.BaudRate.findByExtendedVal(effectiveBaudRate).getBaudRateValue());
        } catch (Exception ignored) {
            dto.setBr(Enums.BaudRate.BAUD_RATE_115200.getBaudRateValue());
        }
        dto.setLednum(device.getNumberOfLEDSconnected());
        return dto;
    }

    /**
     * Host and port shown by the network tab.
     */
    public record MqttAddress(String host, String port) {
    }
}
