/*
  DeviceEndpointHandler.java

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
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.controllers.options.MiscTabOptions;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.dto.DeviceDto;
import org.dpsoftware.network.tcpUdp.TcpClient;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Handles connected devices and proxies their preferences.
 */
@Slf4j
public class DeviceEndpointHandler {

    /**
     * Validates the LED state, RGB channels and white temperature of a picker command.
     *
     * @param payload the command containing only state, color and whitetemp
     * @throws IllegalArgumentException when a required field is missing, invalid or unexpected
     */
    static void validateDeviceState(JsonNode payload) {
        if (payload == null || !payload.isObject()
                || !payload.path(Constants.STATE).isTextual()
                || !(Constants.ON.equals(payload.path(Constants.STATE).asText())
                || Constants.OFF.equals(payload.path(Constants.STATE).asText()))
                || !payload.path(Constants.COLOR).isObject()
                || !inRange(payload.path(Constants.WHITE_TEMP), 20, 110)) {
            throw new IllegalArgumentException("Invalid device state");
        }
        for (String channel : List.of("r", "g", "b")) {
            if (!inRange(payload.path(Constants.COLOR).path(channel), 0, 255)) {
                throw new IllegalArgumentException("Invalid color channel");
            }
        }
        if (payload.size() != 3 || payload.path(Constants.COLOR).size() != 3) {
            throw new IllegalArgumentException("Unexpected device state fields");
        }
    }

    /**
     * Checks whether a JSON value is an integer within the inclusive bounds.
     *
     * @param value the JSON value to check
     * @param min   the minimum allowed value
     * @param max   the maximum allowed value
     * @return true when the value fits in an int and lies within the bounds
     */
    private static boolean inRange(JsonNode value, int min, int max) {
        return value.isIntegralNumber() && value.canConvertToInt() && value.asInt() >= min && value.asInt() <= max;
    }

    /**
     * Updates the runtime picker state while preserving the stored color opacity.
     *
     * @param config  the runtime configuration to update
     * @param payload the picker command already checked by {@link #validateDeviceState(JsonNode)}
     */
    static void applyDeviceState(Configuration config, JsonNode payload) {
        String[] previousColor = config.getColorChooser().split(",");
        JsonNode color = payload.path(Constants.COLOR);
        config.setColorChooser(MiscTabOptions.colorChooser(color.path("r").asInt(), color.path("g").asInt(),
                color.path("b").asInt(), previousColor.length > 3 ? Integer.parseInt(previousColor[3]) : 255));
        config.setToggleLed(Constants.ON.equals(payload.path(Constants.STATE).asText()));
        config.setWhiteTemperature(payload.path(Constants.WHITE_TEMP).asInt());
    }

    /**
     * Applies picker commands to the runtime configuration before sending them to the output device.
     * MQTT's set topic processes color updates without the HTTP endpoint's effectToFf feedback.
     *
     * @param exchange the HTTP exchange containing the picker command and response
     * @throws IOException when the request or response cannot be processed
     */
    public void handleDeviceState(HttpExchange exchange) throws IOException {
        String ip = queryIpParam(exchange);
        var device = CommonUtility.getDeviceToUse();
        if (ip == null || !NetworkManager.isValidIp(ip) || device == null || !ip.equals(device.getDeviceIP())) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid output device");
            return;
        }
        JsonNode payload;
        try (var body = exchange.getRequestBody()) {
            payload = CommonUtility.JSON_MAPPER.readTree(body);
        } catch (IOException e) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON payload");
            return;
        }
        Configuration config = MainSingleton.getInstance().config;
        try {
            validateDeviceState(payload);
        } catch (IllegalArgumentException e) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, e.getMessage());
            return;
        }
        if (config.isMqttEnable() && (ManagerSingleton.getInstance().client == null
                || !ManagerSingleton.getInstance().client.isConnected())) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_UNAVAILABLE, "MQTT is disconnected");
            return;
        }
        applyDeviceState(config, payload);
        ObjectNode command = payload.deepCopy();
        command.put(Constants.MAC, device.getMac());
        if (config.isMqttEnable()) {
            NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT),
                    CommonUtility.toJsonString(command));
        } else {
            // setLeds() also reads effect on HTTP requests; supply it to preserve the current effect.
            command.put(Constants.EFFECT, config.getEffect());
            var response = TcpClient.httpGet(CommonUtility.toJsonString(command), Constants.TOPIC_DEFAULT_MQTT, ip);
            if (response == null || response.getErrorCode() != Constants.HTTP_SUCCESS) {
                HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_GATEWAY, "Device did not respond");
                return;
            }
        }
        HttpResponses.sendOk(exchange);
    }

    /**
     * Read the ip query parameter from the request, or null when absent.
     *
     * @param exchange the HTTP exchange containing the request
     * @return the ip value or null
     */
    private static String queryIpParam(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals("ip")) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    /**
     * Returns connected devices.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleGetDevices(HttpExchange exchange) throws IOException {
        List<DeviceDto> devices = DeviceDto.fromDevices(GuiSingleton.getInstance().getDeviceTableData());
        HttpResponses.sendJson(exchange, devices);
    }

    /**
     * Proxies device preferences to avoid browser CORS restrictions.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleDevicePrefs(HttpExchange exchange) throws IOException {
        String ip = queryIpParam(exchange);
        if (ip == null || !ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Missing or invalid ip parameter");
            return;
        }
        URI devicePrefsUri = URI.create("http://" + ip + "/prefs");
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()) {
            HttpRequest request = HttpRequest.newBuilder(devicePrefsUri).timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            try {
                JsonNode prefsNode = CommonUtility.JSON_MAPPER.readTree(body);
                if (prefsNode.isObject()) {
                    String effectKey = WebFieldNames.PREFS_EFFECT;
                    String ffEffectKey = WebFieldNames.PREFS_FF_EFFECT;
                    if (Constants.STATE_ON_GLOWWORMWIFI.equalsIgnoreCase(prefsNode.get(effectKey).asText())) {
                        ((ObjectNode) prefsNode).put(effectKey, Enums.Effect.BIAS_LIGHT.getI18n());
                        ((ObjectNode) prefsNode).put(ffEffectKey, Enums.Effect.BIAS_LIGHT.getI18n());
                    } else {
                        ((ObjectNode) prefsNode).put(effectKey, LocalizedEnum.fromBaseStr(Enums.Effect.class, prefsNode.get(effectKey).asText()).getI18n());
                        if (prefsNode.get(ffEffectKey).asText().equals(WebFieldNames.PREFS_NULL_VALUE)) {
                            ((ObjectNode) prefsNode).remove(ffEffectKey);
                        } else {
                            ((ObjectNode) prefsNode).put(ffEffectKey, LocalizedEnum.fromBaseStr(Enums.Effect.class, prefsNode.get(ffEffectKey).asText()).getI18n());
                        }
                    }
                    body = CommonUtility.JSON_MAPPER.writeValueAsString(prefsNode);
                }
            } catch (Exception e) {
                if (log.isTraceEnabled()) {
                    log.warn("Device prefs response is not valid JSON: {}", e.getMessage());
                }
            }
            HttpResponses.sendRawJson(exchange, response.statusCode(), body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Interrupted");
        } catch (Exception e) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_GATEWAY, "Device unreachable: " + e.getMessage());
        }
    }
}
