/*
  ConfigurationPayload.java

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
package org.dpsoftware.network.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.controllers.options.*;
import org.dpsoftware.utilities.CaptureDeviceUtilities;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuration fields exchanged with the web interface.
 * Applies the editable payload fields to the saved configuration and serializes it back for the web client,
 * skipping the fields excluded from the web interface.
 */
final class ConfigurationPayload {

    private static final List<String> EXCLUDED_FIELDS = List.of("hueMap", "ledMatrix", "colorChooserHex", "serialPort");
    private static final List<String> LED_FIELDS = List.of(WebFieldNames.TOP_LED, WebFieldNames.LEFT_LED,
            WebFieldNames.RIGHT_LED, WebFieldNames.BOTTOM_LEFT_LED, WebFieldNames.BOTTOM_RIGHT_LED,
            WebFieldNames.BOTTOM_ROW_LED);
    private static final Map<String, Integer> PERCENT_LIMITS = Map.of(
            WebFieldNames.SPLIT_BOTTOM_MARGIN, 95,
            WebFieldNames.GRABBER_AREA_TOP_BOTTOM, 40,
            WebFieldNames.GRABBER_SIDE, 40,
            WebFieldNames.GAP_TYPE_TOP_BOTTOM, 40,
            WebFieldNames.GAP_TYPE_SIDE, 40);
    private static final Pattern PERCENT = Pattern.compile("^(0|[1-9][0-9]?)%$");
    private static final Pattern MQTT_SERVER = Pattern.compile("^tcp://[^\\s:/]+:[0-9]{1,5}$");

    /**
     * Prevents instantiation.
     */
    private ConfigurationPayload() {
    }

    /**
     * Applies editable fields to a copy and regenerates the LED matrix when needed.
     *
     * @param payload     the JSON payload with the editable configuration fields
     * @param savedConfig the saved configuration to apply the payload to
     * @return the updated configuration
     * @throws IOException when the JSON payload or configuration cannot be parsed
     */
    static Configuration apply(JsonNode payload, Configuration savedConfig) throws IOException {
        validateFields(payload);
        ObjectNode configTree = CommonUtility.JSON_MAPPER.valueToTree(savedConfig);
        for (Map.Entry<String, JsonNode> entry : payload.properties()) {
            if (!EXCLUDED_FIELDS.contains(entry.getKey()) && !entry.getKey().equals("captureDeviceName")
                    && !entry.getValue().isNull()) {
                configTree.set(entry.getKey(), entry.getValue());
            }
        }
        if (payload.has("colorChooserHex")) {
            configTree.put("colorChooser", MiscTabOptions.colorChooserFromHex(
                    payload.path("colorChooserHex").asText(), savedConfig.getColorChooser()));
        }
        if (payload.has(WebFieldNames.BRIGHTNESS)) {
            configTree.put(WebFieldNames.BRIGHTNESS,
                    MiscTabOptions.storedBrightness(payload.path(WebFieldNames.BRIGHTNESS).asDouble()));
        }
        if (payload.has(WebFieldNames.WHITE_TEMPERATURE)) {
            configTree.put(WebFieldNames.WHITE_TEMPERATURE,
                    MiscTabOptions.storedWhiteTemperature(payload.path(WebFieldNames.WHITE_TEMPERATURE).asDouble()));
        }
        if (payload.has(WebFieldNames.DESIRED_FRAMERATE)) {
            configTree.put(WebFieldNames.DESIRED_FRAMERATE,
                    MiscTabOptions.storedFramerate(payload.path(WebFieldNames.DESIRED_FRAMERATE).asText()));
        }
        if (payload.has(WebFieldNames.MONITOR_NUMBER)) {
            String deviceName = payload.path("captureDeviceName").asText("");
            if (!deviceName.isBlank()) {
                if (!savedConfig.hasCaptureDevice() || !deviceName.equals(savedConfig.getCaptureDeviceFriendlyName())) {
                    CaptureDeviceUtilities.BestCaptureFormat device = CaptureDeviceUtilities.findPixelFormat(deviceName);
                    if (device == null) {
                        throw new IllegalArgumentException("Capture device not found: " + deviceName);
                    }
                    configTree.set("captureDevice", CommonUtility.JSON_MAPPER.valueToTree(device));
                }
                configTree.put(WebFieldNames.MONITOR_NUMBER, 0);
            } else {
                configTree.putNull("captureDevice");
            }
        }
        if (payload.path(WebFieldNames.AUTO_DETECT_BLACK_BARS).asBoolean(false)) {
            configTree.put(WebFieldNames.DEFAULT_LED_MATRIX, Enums.AspectRatio.FULLSCREEN.getBaseI18n());
        }
        Configuration updatedConfig = CommonUtility.JSON_MAPPER.treeToValue(configTree, Configuration.class);
        if (payload.has("serialPort")) {
            DevicesTabOptions.applyOutput(updatedConfig, payload.path("serialPort").asText());
        }
        if (payload.has(WebFieldNames.CUBE_LUT)) {
            updatedConfig.setCubeLut(DisplayDialogOptions.selectedLut(payload.path(WebFieldNames.CUBE_LUT).asText()));
        }
        if (payload.has(WebFieldNames.ENABLE_AUTOMATIC_GAMMA) || payload.has(WebFieldNames.GAMMA_LEVEL)) {
            GammaOptions.apply(updatedConfig, updatedConfig.isEnableAutomaticGamma(), updatedConfig.getGammaLevel());
        }
        if (payload.has(WebFieldNames.EMA_ALPHA) && payload.has(WebFieldNames.FRAME_INSERTION_TARGET)
                && payload.has(WebFieldNames.SMOOTHING_TARGET_FRAMERATE)) {
            SmoothingOptions.applyControls(updatedConfig, updatedConfig.getEmaAlpha(),
                    updatedConfig.getFrameInsertionTarget(), updatedConfig.getSmoothingTargetFramerate());
        } else if (payload.has(WebFieldNames.SMOOTHING_TYPE)
                && !payload.path(WebFieldNames.SMOOTHING_TYPE).asText().equals(savedConfig.getSmoothingType())) {
            SmoothingOptions.applyPreset(updatedConfig, payload.path(WebFieldNames.SMOOTHING_TYPE).asText());
        }
        int minimum = Math.min(Math.min(updatedConfig.getTopLed(), updatedConfig.getLeftLed()),
                Math.min(Math.min(updatedConfig.getRightLed(), updatedConfig.getBottomLeftLed()),
                        updatedConfig.getBottomRightLed()));
        if (minimum < 1 || updatedConfig.getGroupBy() < 1 || updatedConfig.getGroupBy() > minimum) {
            throw new IllegalArgumentException("groupBy must be between 1 and the smallest LED count");
        }
        if (updatedConfig.ledMatrixParamsChanged(savedConfig)) {
            updatedConfig.regenerateLedMatrix();
        }
        return updatedConfig;
    }

    /**
     * Rejects invalid numeric LED and screen values, grouping, and percentages.
     *
     * @param payload the incoming web configuration values
     */
    private static void validateFields(JsonNode payload) {
        JsonNode cubeLut = payload.get(WebFieldNames.CUBE_LUT);
        if (cubeLut != null && !cubeLut.isTextual()) {
            throw new IllegalArgumentException("cubeLut must be a LUT name");
        }
        JsonNode output = payload.get("serialPort");
        if (output != null && (!output.isTextual() || output.asText().isBlank())) {
            throw new IllegalArgumentException("serialPort must be a device name, serial port, AUTO, or IP");
        }
        JsonNode baudRate = payload.get(WebFieldNames.BAUD_RATE);
        if (baudRate != null && (!baudRate.isTextual() || java.util.Arrays.stream(Enums.BaudRate.values())
                .noneMatch(baud -> baud.getBaudRate().equals(baudRate.asText())))) {
            throw new IllegalArgumentException("Invalid baud rate");
        }
        JsonNode multiMonitor = payload.get(WebFieldNames.MULTI_MONITOR);
        if (multiMonitor != null && (!multiMonitor.isIntegralNumber()
                || multiMonitor.asInt() < 1 || multiMonitor.asInt() > 3)) {
            throw new IllegalArgumentException("multiMonitor must be between 1 and 3");
        }
        JsonNode brightness = payload.get(WebFieldNames.BRIGHTNESS);
        if (brightness != null && (!brightness.isNumber() || brightness.asDouble() < 0 || brightness.asDouble() > 100)) {
            throw new IllegalArgumentException("brightness must be between 0 and 100%");
        }
        JsonNode whiteTemperature = payload.get(WebFieldNames.WHITE_TEMPERATURE);
        if (whiteTemperature != null && (!whiteTemperature.isNumber()
                || whiteTemperature.asDouble() < 2000 || whiteTemperature.asDouble() > 11000)) {
            throw new IllegalArgumentException("whiteTemperature must be between 2000 and 11000 K");
        }
        JsonNode audioGain = payload.get("audioLoopbackGain");
        if (audioGain != null && (!audioGain.isNumber() || audioGain.asDouble() < -5 || audioGain.asDouble() > 5)) {
            throw new IllegalArgumentException("audioLoopbackGain must be between -5 and 5");
        }
        JsonNode color = payload.get("colorChooserHex");
        if (color != null && (!color.isTextual() || !color.textValue().matches("#[0-9a-fA-F]{6}"))) {
            throw new IllegalArgumentException("colorChooserHex must be a six-digit color");
        }
        JsonNode mqttServer = payload.get(WebFieldNames.MQTT_SERVER);
        if (mqttServer != null && (!mqttServer.isTextual() || !MQTT_SERVER.matcher(mqttServer.textValue()).matches()
                || !validMqttPort(mqttServer.textValue()))) {
            throw new IllegalArgumentException("mqttServer must contain a host and a port from 1 to 65535");
        }
        for (String field : List.of(WebFieldNames.SCREEN_RES_X, WebFieldNames.SCREEN_RES_Y)) {
            JsonNode value = payload.get(field);
            if (value != null && (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0)) {
                throw new IllegalArgumentException(field + " must be a nonnegative whole number");
            }
        }
        JsonNode monitor = payload.get(WebFieldNames.MONITOR_NUMBER);
        if (monitor != null && (!monitor.isIntegralNumber() || !monitor.canConvertToInt() || monitor.intValue() < 0)) {
            throw new IllegalArgumentException("monitorNumber must be a nonnegative whole number");
        }
        for (String field : LED_FIELDS) {
            JsonNode value = payload.get(field);
            if (value != null && (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0)) {
                throw new IllegalArgumentException(field + " must be a nonnegative whole number");
            }
        }
        JsonNode group = payload.get(WebFieldNames.GROUP_BY);
        if (group != null && (!group.isIntegralNumber() || !group.canConvertToInt() || group.intValue() < 1)) {
            throw new IllegalArgumentException("groupBy must be a positive whole number");
        }
        PERCENT_LIMITS.forEach((field, maximum) -> {
            JsonNode value = payload.get(field);
            if (value != null && (!value.isTextual() || !PERCENT.matcher(value.textValue()).matches()
                    || Integer.parseInt(value.textValue().replace("%", "")) > maximum)) {
                throw new IllegalArgumentException(field + " must be between 0% and " + maximum + "%");
            }
        });
    }

    /**
     * Checks the port component of a stored MQTT server URL.
     *
     * @param server MQTT server URL
     * @return true when its port is valid
     */
    private static boolean validMqttPort(String server) {
        int port = Integer.parseInt(server.substring(server.lastIndexOf(':') + 1));
        return port >= 1 && port <= 65535;
    }

    /**
     * Serializes the configuration without fields excluded from the web interface.
     *
     * @param config the configuration to serialize
     * @return the JSON node with the excluded fields removed
     */
    static ObjectNode toWebConfig(Configuration config) {
        ObjectNode configNode = CommonUtility.JSON_MAPPER.valueToTree(config);
        EXCLUDED_FIELDS.forEach(configNode::remove);
        return configNode;
    }
}
