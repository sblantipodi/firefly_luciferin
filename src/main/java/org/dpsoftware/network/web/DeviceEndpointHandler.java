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
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.managers.dto.DeviceDto;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** Handles connected devices and proxies their preferences. */
@Slf4j
public class DeviceEndpointHandler {

    /**
     * Read the {@code ip} query parameter from the request, or {@code null} when absent.
     *
     * @param exchange the HTTP exchange containing the request
     * @return the ip value or {@code null}
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
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(devices);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
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
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Missing or invalid ip parameter");
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
                log.warn("Device prefs response is not valid JSON: {}", e.getMessage());
            }
            byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(response.statusCode(), responseBytes.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(responseBytes);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Interrupted");
        } catch (Exception e) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_GATEWAY, "Device unreachable: " + e.getMessage());
        }
    }

    /**
     * Send a plain text error response.
     *
     * @param exchange   the HTTP exchange to reply on
     * @param statusCode the HTTP status code to return
     * @param message    the human-readable error description
     * @throws IOException when the response cannot be written
     */
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }
}
