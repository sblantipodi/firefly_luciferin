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
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.grabber.WebRtcStreamer;
import org.dpsoftware.gui.controllers.DisplayDialogController;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * Minimal HTTP server exposing the saved Configuration as JSON and providing a small web page to edit it.
 * Backed by the JDK HTTP server.
 * The server owns the lifecycle (bind, contexts, CORS guard, shutdown) and the configuration endpoints;
 * the individual endpoint families are delegated to dedicated handlers:
 * WebResourceServer for the settings page static resources,
 * LivePreviewWebHandler for the live preview endpoints,
 * ProfileHandler for the profile list/activation endpoints,
 * DeviceEndpointHandler for the connected device endpoints.
 */
@Slf4j
public class ConfigServer {

    private final StorageManager storageManager = new StorageManager();
    private final List<HttpServer> httpServers = new ArrayList<>();
    private final WebRtcStreamer webRtcStreamer = new WebRtcStreamer();
    private final WebRtcSignalingServer webRtcSignalingServer = new WebRtcSignalingServer(webRtcStreamer);
    private final WebResourceServer webResourceServer = new WebResourceServer();
    private final LivePreviewWebHandler livePreviewWebHandler = new LivePreviewWebHandler(webRtcSignalingServer);
    private final ProfileHandler profileHandler = new ProfileHandler();
    private final DeviceEndpointHandler deviceEndpointHandler = new DeviceEndpointHandler();
    private final Predicate<String> GET_METHOD = method -> method.equalsIgnoreCase("GET");
    private final Predicate<String> POST_METHOD = method -> method.equalsIgnoreCase("POST");

    /**
     * Returns loopback and active non link local IPv4 addresses.
     *
     * @return the set of addresses to bind the config server on
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
                    // Skip IPv6, non link local and multicast addresses.
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
     * Handle GET /fps, exposing the current producing and consuming framerate.
     * Read only, the values are the live counters kept in MainSingleton.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetFps(HttpExchange exchange) throws IOException {
        HttpResponses.sendJson(exchange, new FpsDto(
                MainSingleton.getInstance().FPS_PRODUCER,
                MainSingleton.getInstance().FPS_GW_CONSUMER));
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
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON payload");
            return;
        }
        if (payload == null || payload.isNull() || !payload.isObject()) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Payload must be a JSON object");
            return;
        }
        log.info("setConfig payload received: {}", CommonUtility.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
        // Read the active or main configuration.
        Configuration savedConfig = storageManager.readProfileInUseConfig();
        if (savedConfig == null) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Configuration not found");
            return;
        }
        Configuration updatedConfig = ConfigurationPayload.apply(payload, savedConfig);
        try {
            // Persist to the active or main configuration.
            updatedConfig.setEffect(LocalizedEnum.fromTextToBase(Enums.Effect.class, updatedConfig.getEffect()));
            storageManager.writeConfig(updatedConfig, null);
        } catch (IOException e) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Unable to save configuration: " + e.getMessage());
            return;
        }
        log.info("Configuration updated via setConfig endpoint");
        HttpResponses.sendOk(exchange);
        // Restart with the active profile and headless mode.
        NativeExecutor.restartNativeInstanceWithCurrentProfile();
    }

    /**
     * Handle GET /getConfig, serializing the saved configuration to JSON.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetConfig(HttpExchange exchange) throws IOException {
        // Expose the active or main configuration.
        Configuration config = storageManager.readProfileInUseConfig();
        if (config == null) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Configuration not found");
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
        ObjectNode configNode = ConfigurationPayload.toWebConfig(config);
        // Include the active non-default profile.
        String profileArg = MainSingleton.getInstance().profileArg;
        if (profileArg != null && !profileArg.isEmpty()
                && !Constants.DEFAULT.equals(profileArg)
                && !CommonUtility.getWord(Constants.DEFAULT).equals(profileArg)) {
            configNode.put("activeProfile", profileArg);
        }
        HttpResponses.sendJson(exchange, configNode);
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
        GStreamerGrabber.webRtcStreamer = null;
        GStreamerGrabber.imageLivePreviewFallback = false;
        webRtcStreamer.stop();
        webRtcSignalingServer.stop();
    }

    /**
     * Starts one server per loopback or active non link local IPv4 address.
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
                server.createContext(Constants.GET_DEVICES_ENDPOINT, withGuard(deviceEndpointHandler::handleGetDevices, GET_METHOD));
                server.createContext(Constants.FIELD_OPTIONS_ENDPOINT, withGuard(this::handleGetFieldOptions, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_PAGE_ENDPOINT, withGuard(webResourceServer::handleSetConfigPage, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_PAGE_JS_ENDPOINT, withGuard(webResourceServer::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_CORE_JS_ENDPOINT, withGuard(webResourceServer::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_DEVICE_JS_ENDPOINT, withGuard(webResourceServer::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_UI_JS_ENDPOINT, withGuard(webResourceServer::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_CSS_ENDPOINT, withGuard(webResourceServer::handleSetConfigCss, GET_METHOD));
                server.createContext(Constants.WEBRTC_PREVIEW_JS_ENDPOINT, withGuard(webResourceServer::handleWebrtcPreviewJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_ENDPOINT, withGuard(this::handleSetConfig, POST_METHOD));
                server.createContext(Constants.DEVICE_PREFS_ENDPOINT, withGuard(deviceEndpointHandler::handleDevicePrefs, GET_METHOD));
                server.createContext(Constants.FPS_ENDPOINT, withGuard(this::handleGetFps, GET_METHOD));
                server.createContext(Constants.SCREENSHOT_ENDPOINT, withGuard(livePreviewWebHandler::handleGetScreenshot, GET_METHOD));
                server.createContext(Constants.SCREENSHOT_ENABLE_ENDPOINT, withGuard(livePreviewWebHandler::handleEnableScreenshot, POST_METHOD));
                server.createContext(Constants.LIST_PROFILES_ENDPOINT, withGuard(profileHandler::handleListProfiles, GET_METHOD));
                server.createContext(Constants.ACTIVATE_PROFILE_ENDPOINT, withGuard(profileHandler::handleActivateProfile, POST_METHOD));
                server.createContext(Constants.ADD_PROFILE_ENDPOINT, withGuard(profileHandler::handleAddProfile, POST_METHOD));
                server.createContext(Constants.REMOVE_PROFILE_ENDPOINT, withGuard(profileHandler::handleRemoveProfile, POST_METHOD));
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
            GStreamerGrabber.webRtcStreamer = webRtcStreamer;
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
        HttpResponses.sendJson(exchange, response);
    }

    /**
     * Handle GET /sectionTitles, returning the localized titles for every section and sub-accordion.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetSectionTitles(HttpExchange exchange) throws IOException {
        ObjectNode response = CommonUtility.JSON_MAPPER.valueToTree(FieldOptions.getSectionTitles());
        HttpResponses.sendJson(exchange, response);
    }

    /**
     * Applies a select field change to the running configuration.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleComboChange(HttpExchange exchange) throws IOException {
        JsonNode payload;
        try (InputStream requestBody = exchange.getRequestBody()) {
            payload = CommonUtility.JSON_MAPPER.readTree(requestBody);
        } catch (IOException e) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON payload");
            return;
        }
        String comboName = payload.has(WebFieldNames.COMBO_NAME) ? payload.get(WebFieldNames.COMBO_NAME).asText() : "";
        JsonNode valueNode = payload.has(WebFieldNames.VALUE) ? payload.get(WebFieldNames.VALUE) : null;
        log.info("Web combo change: {} = {}", comboName, valueNode);
        applyComboChange(comboName, valueNode);
        HttpResponses.sendOk(exchange);
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
     * Wraps a handler with CORS and method validation.
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
                    HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_METHOD,
                            "Only the " + methodAllowedDescription(methodAllowed) + " method is supported");
                    return;
                }
                handler.handle(exchange);
            } catch (IOException e) {
                log.error("Config server request failed: {}", e.getMessage());
            }
        };
    }

    /**
     * Plain data holder for the current framerate counters, JSON friendly.
     *
     * @param producing the producer framerate (frames captured per second)
     * @param consuming the consumer framerate (frames written to the LED per second)
     */
    public record FpsDto(float producing, float consuming) {
    }
}
