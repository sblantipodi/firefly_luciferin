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

    private static final List<String> EXCLUDED_FIELDS = List.of("hueMap", "ledMatrix");
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
