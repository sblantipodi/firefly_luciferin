/*
  ConfigServer.java

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
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.controllers.DisplayDialogController;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * Minimal HTTP server exposing the saved {@link Configuration} as JSON and providing a small web page to edit it.
 * Backed by the JDK HTTP server.
 * <p>
 * The server owns the lifecycle (bind, contexts, CORS guard, shutdown) and the configuration endpoints;
 * the individual endpoint families are delegated to dedicated handlers:
 * <ul>
 *     <li>{@link WebResourceServer} for the settings page static resources</li>
 *     <li>{@link LivePreviewWebHandler} for the live preview endpoints</li>
 *     <li>{@link ProfileHandler} for the profile list/activation endpoints</li>
 *     <li>{@link DeviceEndpointHandler} for the connected device endpoints</li>
 * </ul>
 */
@Slf4j
public class ConfigServer {

    /**
     * Configuration fields to strip from the JSON payload, they are huge and not useful to a client.
     */
    private static final List<String> EXCLUDED_FIELDS = List.of("hueMap", "ledMatrix");
    private static final String JSON_OK = "{\"status\":\"OK\"}";
    private final StorageManager storageManager = new StorageManager();
    private final List<HttpServer> httpServers = new ArrayList<>();
    private final WebResourceServer webResourceServer = new WebResourceServer();
    private final LivePreviewWebHandler livePreviewWebHandler = new LivePreviewWebHandler();
    private final ProfileHandler profileHandler = new ProfileHandler();
    private final DeviceEndpointHandler deviceEndpointHandler = new DeviceEndpointHandler();
    private final Predicate<String> GET_METHOD = method -> method.equalsIgnoreCase("GET");
    private final Predicate<String> POST_METHOD = method -> method.equalsIgnoreCase("POST");

    /**
     * Collect the addresses to bind: loopback (127.0.0.1) plus every non-link-local IPv4 interface address.
     * Link-local (169.254.x.x) addresses are excluded to avoid exposing the endpoint on ad-hoc Wi-Fi or
     * Bluetooth networks; multicast and IPv6 addresses are skipped as well.
     *
     * @return the set of addresses to bind, ordered (loopback first)
     */
    private static Set<InetAddress> localBindAddresses() {
        Set<InetAddress> addresses = new LinkedHashSet<>();
        try {
            addresses.add(InetAddress.getLoopbackAddress());
        } catch (Exception ignored) {
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addressesEnum = networkInterface.getInetAddresses();
                while (addressesEnum.hasMoreElements()) {
                    InetAddress address = addressesEnum.nextElement();
                    if (address.isLoopbackAddress()) {
                        continue;
                    }
                    String host = address.getHostAddress();
                    // Skip IPv6, link-local (169.254.x.x) and any multicast
                    if (host.contains(":") || host.startsWith("169.254.") || address.isMulticastAddress()) {
                        continue;
                    }
                    addresses.add(address);
                }
            }
        } catch (SocketException e) {
            log.warn("Unable to enumerate network interfaces: {}", e.getMessage());
        }
        return addresses;
    }

    /**
     * Handle GET /listProfiles, exposing the list of profile names available for this instance.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleListProfiles(HttpExchange exchange) throws IOException {
        profileHandler.handleListProfiles(exchange);
    }

    /**
     * Handle GET /fps, exposing the current producing and consuming framerate.
     * Read-only, the values are the live counters kept in {@link MainSingleton}.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetFps(HttpExchange exchange) throws IOException {
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(new FpsDto(
                MainSingleton.getInstance().FPS_PRODUCER,
                MainSingleton.getInstance().FPS_GW_CONSUMER));
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle POST /activateProfile?name=<profile>, activating a profile by restarting the native
     * instance with it. When {@code name} is absent or empty the current (default) profile is used.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleActivateProfile(HttpExchange exchange) throws IOException {
        profileHandler.handleActivateProfile(exchange);
    }

    /**
     * Handle POST /setConfig, taking a JSON payload with some configuration parameters,
     * merging it into the saved configuration (the excluded fields are preserved) and persisting it.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleSetConfig(HttpExchange exchange) throws IOException {
        JsonNode payload;
        try (InputStream requestBody = exchange.getRequestBody()) {
            payload = CommonUtility.JSON_MAPPER.readTree(requestBody);
        } catch (IOException e) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON payload");
            return;
        }
        if (payload == null || payload.isNull() || !payload.isObject()) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Payload must be a JSON object");
            return;
        }
        log.info("setConfig payload received: {}", CommonUtility.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
        // Read the config of the profile in use (or the main config when no profile is set).
        Configuration config = storageManager.readProfileInUseConfig();
        if (config == null) {
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Configuration not found");
            return;
        }
        ObjectNode configTree = CommonUtility.JSON_MAPPER.valueToTree(config);
        mergePayload(payload, configTree);
        Configuration updatedConfig = CommonUtility.JSON_MAPPER.treeToValue(configTree, Configuration.class);
        try {
            // Persist into the profile in use (or the main config when no profile is set); null lets
            // writeConfig pick the right file based on profileArg and whoAmI.
            updatedConfig.setEffect(LocalizedEnum.fromStr(Enums.Effect.class, updatedConfig.getEffect()).getBaseI18n());
            storageManager.writeConfig(updatedConfig, null);
        } catch (IOException e) {
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Unable to save configuration: " + e.getMessage());
            return;
        }
        log.info("Configuration updated via setConfig endpoint");
        sendOkJson(exchange);
        // Restart Firefly with the profile in use (if any) and preserving headless mode.
        NativeExecutor.restartNativeInstanceWithCurrentProfile();
    }

    /**
     * Handle GET /getConfig, serializing the saved configuration to JSON.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetConfig(HttpExchange exchange) throws IOException {
        // Expose the config of the profile in use (or the main config when no profile is set).
        Configuration config = storageManager.readProfileInUseConfig();
        if (config == null) {
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Configuration not found");
            return;
        }
        sendConfiguration(exchange, config);
    }

    /**
     * Serialize the configuration to JSON, strip the heavy/not interesting fields, and write it as the HTTP response body.
     *
     * @param exchange the HTTP exchange to send the response on
     * @param config   the configuration to serialize
     * @throws IOException when the response cannot be written
     */
    private void sendConfiguration(HttpExchange exchange, Configuration config) throws IOException {
        ObjectNode configNode = CommonUtility.JSON_MAPPER.valueToTree(config);
        EXCLUDED_FIELDS.forEach(configNode::remove);
        // Expose the active profile (when a non-default profile is in use) so the client can show it.
        String profileArg = MainSingleton.getInstance().profileArg;
        if (profileArg != null && !profileArg.isEmpty()
                && !Constants.DEFAULT.equals(profileArg)
                && !CommonUtility.getWord(Constants.DEFAULT).equals(profileArg)) {
            configNode.put("activeProfile", profileArg);
        }
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(configNode);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Stop the config endpoint on every bound interface.
     */
    public void stop() {
        httpServers.forEach(server -> {
            try {
                server.stop(0);
            } catch (Exception ignored) {
            }
        });
        httpServers.clear();
        livePreviewWebHandler.stopLivePreviewWatchdog();
    }

    /**
     * Handle GET /getDevices, exposing the currently connected devices (in-memory device table) as JSON.
     * Read-only, the connected devices are a runtime state and cannot be persisted.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetDevices(HttpExchange exchange) throws IOException {
        deviceEndpointHandler.handleGetDevices(exchange);
    }

    /**
     * Start the config HTTP endpoint on the loopback address and every non-link-local local IPv4 interface.
     * A separate {@link HttpServer} is created per interface because the JDK {@code HttpServer} can only
     * bind to a single {@link InetSocketAddress} at a time. Link-local (169.254.x.x) and site-local
     * multicast addresses are skipped so the endpoint is not exposed on Wi-Fi/Bluetooth ad-hoc networks.
     */
    @SuppressWarnings("all")
    public void start() {
        if (!httpServers.isEmpty()) {
            return;
        }
        try {
            for (InetAddress address : localBindAddresses()) {
                HttpServer server = HttpServer.create(new InetSocketAddress(address, Constants.CONFIG_SERVER_DEFAULT_PORT), 0);
                server.createContext(Constants.CONFIG_ENDPOINT, withGuard(this::handleGetConfig, GET_METHOD));
                server.createContext(Constants.GET_DEVICES_ENDPOINT, withGuard(this::handleGetDevices, GET_METHOD));
                server.createContext(Constants.FIELD_OPTIONS_ENDPOINT, withGuard(this::handleGetFieldOptions, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_PAGE_ENDPOINT, withGuard(webResourceServer::handleSetConfigPage, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_PAGE_JS_ENDPOINT, withGuard(webResourceServer::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_CORE_JS_ENDPOINT, withGuard(webResourceServer::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_DEVICE_JS_ENDPOINT, withGuard(webResourceServer::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_UI_JS_ENDPOINT, withGuard(webResourceServer::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_CSS_ENDPOINT, withGuard(webResourceServer::handleSetConfigCss, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_ENDPOINT, withGuard(this::handleSetConfig, POST_METHOD));
                server.createContext(Constants.DEVICE_PREFS_ENDPOINT, withGuard(deviceEndpointHandler::handleDevicePrefs, GET_METHOD));
                server.createContext(Constants.FPS_ENDPOINT, withGuard(this::handleGetFps, GET_METHOD));
                server.createContext(Constants.SCREENSHOT_ENDPOINT, withGuard(livePreviewWebHandler::handleGetScreenshot, GET_METHOD));
                server.createContext(Constants.SCREENSHOT_ENABLE_ENDPOINT, withGuard(livePreviewWebHandler::handleEnableScreenshot, POST_METHOD));
                server.createContext(Constants.LIST_PROFILES_ENDPOINT, withGuard(this::handleListProfiles, GET_METHOD));
                server.createContext(Constants.ACTIVATE_PROFILE_ENDPOINT, withGuard(this::handleActivateProfile, POST_METHOD));
                server.createContext(Constants.COMBO_CHANGE_ENDPOINT, withGuard(this::handleComboChange, POST_METHOD));
                server.createContext(Constants.SECTION_TITLES_ENDPOINT, withGuard(this::handleGetSectionTitles, GET_METHOD));
                server.createContext("/", withGuard(webResourceServer::handleRoot, GET_METHOD));
                server.setExecutor(Executors.newCachedThreadPool(runnable -> {
                    Thread thread = new Thread(runnable, "firefly-config-server");
                    thread.setDaemon(true);
                    return thread;
                }));
                server.start();
                httpServers.add(server);
                log.info("Config server listening on http://{}:{}", address.getHostAddress(), Constants.CONFIG_SERVER_DEFAULT_PORT);
            }
            if (httpServers.isEmpty()) {
                log.warn("No local interface found, config server not started");
            }
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "firefly-config-shutdown"));
        } catch (IOException e) {
            stop();
            log.warn("Unable to start config server: {}", e.getMessage());
        }
    }

    /**
     * Handle GET /getFieldOptions, exposing the possible values for every configuration field backed by an enum.
     * Read-only, the options are derived from the enums (single source of truth, no client-side duplication).
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetFieldOptions(HttpExchange exchange) throws IOException {
        ObjectNode response = CommonUtility.JSON_MAPPER.createObjectNode();
        response.set("options", CommonUtility.JSON_MAPPER.valueToTree(FieldOptions.getFieldOptions()));
        Map<String, String> labels = FieldOptions.getFieldLabels();
        FieldOptions.applyToggleLedLabels(labels);
        response.set("labels", CommonUtility.JSON_MAPPER.valueToTree(labels));
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle GET /sectionTitles, returning the localized titles for every section and sub-accordion.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetSectionTitles(HttpExchange exchange) throws IOException {
        ObjectNode response = CommonUtility.JSON_MAPPER.valueToTree(FieldOptions.getSectionTitles());
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle POST /comboChange, applying the web page select change to the running configuration.
     * Expected JSON body: {"comboName": "cubeLut", "value": "HDR2SDR_tonemap_LUT_1.cube"}.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleComboChange(HttpExchange exchange) throws IOException {
        JsonNode payload;
        try (InputStream requestBody = exchange.getRequestBody()) {
            payload = CommonUtility.JSON_MAPPER.readTree(requestBody);
        } catch (IOException e) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON payload");
            return;
        }
        String comboName = payload.has(WebFieldNames.COMBO_NAME) ? payload.get(WebFieldNames.COMBO_NAME).asText() : "";
        JsonNode valueNode = payload.has(WebFieldNames.VALUE) ? payload.get(WebFieldNames.VALUE) : null;
        log.info("Web combo change: {} = {}", comboName, valueNode);
        applyComboChange(comboName, valueNode);
        sendOkJson(exchange);
    }

    /**
     * Apply a combo change to the running configuration on the JavaFX thread.
     *
     * @param comboName the name of the combo box that changed
     * @param value     the newly selected value, as a JSON node (string or boolean)
     */
    private void applyComboChange(String comboName, JsonNode value) {
        String valueText = value != null ? value.asText() : "";
        if (valueText == null || valueText.isEmpty()) {
            return;
        }
        switch (comboName) {
            case WebFieldNames.CUBE_LUT ->
                    CommonUtility.delayMilliseconds(() -> DisplayDialogController.handleCubeLutCombo(valueText), 200);
            case WebFieldNames.DESIRED_FRAMERATE -> CommonUtility.delayMilliseconds(() -> {
                MainSingleton.getInstance().config.setDesiredFramerate(valueText);
                PipelineManager.restartCapture(CommonUtility::run);
            }, 200);
            case WebFieldNames.RESAMPLING_FACTOR -> CommonUtility.delayMilliseconds(() -> {
                Enums.ResamplingFactor rf = Enums.ResamplingFactor.findByValue(Integer.parseInt(valueText));
                if (rf != null) {
                    PipelineManager.restartCapture(CommonUtility::run, () ->
                            MainSingleton.getInstance().config.setResamplingFactor(rf.getResamplingFactorValue()));
                }
            }, 200);
            case WebFieldNames.EFFECT_SELECT -> {
                // effectSelect options use the base i18n key as value (see FieldOptions.effectOptions);
                // fall back to the localized label for back-compat with older clients.
                Enums.Effect effect = LocalizedEnum.fromBaseStr(Enums.Effect.class, valueText);
                if (effect == null) {
                    effect = LocalizedEnum.fromStr(Enums.Effect.class, valueText);
                }
                if (effect != null) {
                    NetworkManager.setEffect(effect.getBaseI18n());
                }
            }
            case WebFieldNames.TOGGLE_LED -> {
                boolean on;
                if (value.isBoolean()) {
                    on = value.asBoolean();
                } else {
                    on = Boolean.parseBoolean(valueText);
                }
                CommonUtility.delayMilliseconds(() -> {
                    MainSingleton.getInstance().config.setToggleLed(on);
                    if (on) {
                        CommonUtility.turnOnLEDs();
                    } else {
                        CommonUtility.turnOffLEDs(MainSingleton.getInstance().config);
                    }
                }, 200);
            }
            default -> {
                // Not yet wired
            }
        }
    }

    /**
     * Merge the payload fields into the configuration tree, excluding the fields not exposed to a client.
     *
     * @param payload    the JSON object received from the client
     * @param configTree the configuration tree to update in place
     */
    private void mergePayload(JsonNode payload, ObjectNode configTree) {
        for (Map.Entry<String, JsonNode> entry : payload.properties()) {
            if (!EXCLUDED_FIELDS.contains(entry.getKey()) && !entry.getValue().isNull()) {
                configTree.set(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Human readable description of the method allowed by a guard predicate.
     *
     * @param methodAllowed the predicate deciding which method the handler accepts
     * @return the method name the guard allows
     */
    private String methodAllowedDescription(Predicate<String> methodAllowed) {
        for (String candidate : List.of("GET", "POST")) {
            if (methodAllowed.test(candidate)) {
                return candidate;
            }
        }
        return "GET";
    }

    /**
     * Wrap an HTTP handler enforcing the CORS preflight handling and a single allowed method.
     * OPTIONS requests are answered with no content, disallowed methods are rejected with a 405.
     *
     * @param handler       the handler to guard
     * @param methodAllowed the predicate deciding which method the handler accepts
     * @return the guarded handler
     */
    private HttpHandler withGuard(HttpHandler handler, Predicate<String> methodAllowed) {
        return exchange -> {
            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
                    exchange.close();
                    return;
                }
                if (!methodAllowed.test(exchange.getRequestMethod())) {
                    byte[] responseBytes = ("Only the " + methodAllowedDescription(methodAllowed) + " method is supported").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(405, responseBytes.length);
                    try (OutputStream responseBody = exchange.getResponseBody()) {
                        responseBody.write(responseBytes);
                    }
                    return;
                }
                handler.handle(exchange);
            } catch (IOException e) {
                log.error("Config server request failed: {}", e.getMessage());
            }
        };
    }

    /**
     * Send a JSON {@code OK} response.
     *
     * @param exchange the HTTP exchange to send the response on
     * @throws IOException when the response cannot be written
     */
    private void sendOkJson(HttpExchange exchange) throws IOException {
        byte[] responseBytes = JSON_OK.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Plain data holder for the current framerate counters, JSON friendly.
     *
     * @param producing the producer framerate (frames captured per second)
     * @param consuming the consumer framerate (frames written to the LED per second)
     */
    public record FpsDto(float producing, float consuming) {
    }

    /**
     * Send a plain text error response and close the exchange.
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
