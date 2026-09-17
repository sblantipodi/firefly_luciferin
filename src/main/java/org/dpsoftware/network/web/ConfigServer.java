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
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.DeviceDto;
import org.dpsoftware.utilities.CommonUtility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * Minimal HTTP server exposing the saved {@link Configuration} as JSON and providing a small web page to edit it.
 * Backed by the JDK HTTP server.
 */
@Slf4j
public class ConfigServer {

    /**
     * Configuration fields to strip from the JSON payload, they are huge and not useful to a client.
     */
    private static final List<String> EXCLUDED_FIELDS = List.of("hueMap", "ledMatrix");
    /**
     * Settings web page resource, co-located in this package ({@code org.dpsoftware.network.web}).
     * <p>
     * The package is {@code opens} in the module descriptor so the class loader can read the
     * resource in JPMS module mode (a non opened package would be invisible to it).
     */
    private static final String SET_CONFIG_PAGE_RESOURCE = "setConfig.html";
    /**
     * Settings page JavaScript resource, co-located in this package.
     */
    private static final String SET_CONFIG_PAGE_JS_RESOURCE = "setConfig.js";
    private final StorageManager storageManager = new StorageManager();
    private final List<HttpServer> httpServers = new java.util.ArrayList<>();
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
     * Extract a query parameter value from a URL query string.
     *
     * @param query the query string (without the leading '?')
     * @param name  the parameter name
     * @return the value or {@code null} when absent
     */
    private static String queryParam(String query, String name) {
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    /**
     * Build the map of possible values for every configuration field that is backed by an enum,
     * using the enums as the single source of truth (no value list is duplicated in the client).
     * Each entry exposes the value to persist and the English display label, plus the value type
     * so the client can cast it correctly.
     *
     * @return map of configuration field name to its possible values
     */
    private static Map<String, FieldOptions> getFieldOptions() {
        Map<String, FieldOptions> options = new LinkedHashMap<>();
        options.put("orientation", localized(Enums.Orientation.class));
        options.put("defaultLedMatrix", localized(Enums.AspectRatio.class));
        options.put("baudRate", new FieldOptions(Arrays.stream(Enums.BaudRate.values())
                .map(b -> new FieldOptions.Option(b.getBaudRate(), b.getBaudRate())).toList(), "string"));
        options.put("desiredFramerate", new FieldOptions(Arrays.stream(Enums.Framerate.values())
                .map(f -> new FieldOptions.Option(f.getBaseI18n(), f.getBaseI18n())).toList(), "string"));
        options.put("simdAvx", new FieldOptions(Arrays.stream(Enums.SimdAvxOption.values())
                .map(s -> new FieldOptions.Option(String.valueOf(s.getSimdOptionNumeric()), s.getBaseI18n())).toList(), "number"));
        options.put("resamplingFactor", new FieldOptions(Arrays.stream(Enums.ResamplingFactor.values())
                .map(r -> new FieldOptions.Option(String.valueOf(r.getResamplingFactorValue()), r.getBaseI18n())).toList(), "number"));
        options.put("algo", localized(Enums.Algo.class));
        options.put("theme", localized(Enums.Theme.class));
        options.put("language", localized(Enums.Language.class));
        options.put("smoothingType", localized(Enums.Smoothing.class));
        options.put("streamType", new FieldOptions(Arrays.stream(Enums.StreamType.values())
                .map(s -> new FieldOptions.Option(s.getStreamType(), s.getStreamType())).toList(), "string"));
        options.put("effect", localized(Enums.Effect.class));
        options.put("colorMode", new FieldOptions(Arrays.stream(Enums.ColorMode.values())
                .map(c -> new FieldOptions.Option(String.valueOf(c.ordinal() + 1), c.getBaseI18n())).toList(), "number"));
        options.put("gammaLevel", localized(Enums.GammaLevel.class));
        options.put("nightLight", localized(Enums.NightLight.class));
        options.put("brightnessLimiter", new FieldOptions(Arrays.stream(Enums.BrightnessLimiter.values())
                .map(b -> new FieldOptions.Option(String.valueOf(b.getBrightnessLimitFloat()), b.getBaseI18n())).toList(), "number"));
        options.put("powerSaving", localized(Enums.PowerSaving.class));
        options.put("multiMonitor", new FieldOptions(List.of(
                new FieldOptions.Option("1", "Disabled"),
                new FieldOptions.Option("2", "Dual display"),
                new FieldOptions.Option("3", "Triple display")), "number"));
        return options;
    }

    /**
     * Handle GET /getConfig, serializing the saved configuration to JSON.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetConfig(HttpExchange exchange) throws IOException {
        Configuration config = storageManager.readConfigFile(Constants.CONFIG_FILENAME);
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
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(configNode);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Build {@link FieldOptions} for a localized enum, using its base (English) i18n value to persist
     * and its English i18n text as label.
     *
     * @param enumClass the localized enum class
     * @param <E>       the localized enum type
     * @return the field options
     */
    private static <E extends Enum<E> & LocalizedEnum> FieldOptions localized(Class<E> enumClass) {
        List<FieldOptions.Option> opts = Arrays.stream(enumClass.getEnumConstants())
                .map(e -> new FieldOptions.Option(e.getBaseI18n(), e.getBaseI18n()))
                .toList();
        return new FieldOptions(opts, "string");
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
                server.createContext(Constants.SET_CONFIG_PAGE_ENDPOINT, withGuard(this::handleSetConfigPage, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_PAGE_JS_ENDPOINT, withGuard(this::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_ENDPOINT, withGuard(this::handleSetConfig, POST_METHOD));
                server.createContext(Constants.DEVICE_PREFS_ENDPOINT, withGuard(this::handleDevicePrefs, GET_METHOD));
                server.createContext("/", withGuard(this::handleRoot, GET_METHOD));
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
    }

    /**
     * Handle GET /getDevices, exposing the currently connected devices (in-memory device table) as JSON.
     * Read-only, the connected devices are a runtime state and cannot be persisted.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetDevices(HttpExchange exchange) throws IOException {
        List<DeviceDto> devices = DeviceDto.fromDevices(GuiSingleton.getInstance().getDeviceTableData());
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(devices);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
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
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(getFieldOptions());
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle GET /setConfigPage, serving the minimal HTML page that hosts the settings form.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleSetConfigPage(HttpExchange exchange) throws IOException {
        sendResource(exchange, SET_CONFIG_PAGE_RESOURCE, "text/html; charset=utf-8");
    }

    /**
     * Handle GET /setConfig.js, serving the JavaScript that drives the settings page.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleSetConfigPageJs(HttpExchange exchange) throws IOException {
        sendResource(exchange, SET_CONFIG_PAGE_JS_RESOURCE, "application/javascript; charset=utf-8");
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
        Configuration config = storageManager.readConfigFile(Constants.CONFIG_FILENAME);
        if (config == null) {
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Configuration not found");
            return;
        }
        ObjectNode configTree = CommonUtility.JSON_MAPPER.valueToTree(config);
        mergePayload(payload, configTree);
        Configuration updatedConfig = CommonUtility.JSON_MAPPER.treeToValue(configTree, Configuration.class);
        try {
            storageManager.writeConfig(updatedConfig, Constants.CONFIG_FILENAME);
        } catch (IOException e) {
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Unable to save configuration: " + e.getMessage());
            return;
        }
        log.info("Configuration updated via setConfig endpoint");
        sendJson(exchange, HttpURLConnection.HTTP_OK, "{\"status\":\"OK\"}");
    }

    /**
     * Merge the payload fields into the configuration tree, excluding the fields not exposed to a client.
     *
     * @param payload    the JSON object received from the client
     * @param configTree the configuration tree to update in place
     */
    private void mergePayload(JsonNode payload, ObjectNode configTree) {
        payload.fields().forEachRemaining(entry -> {
            if (!EXCLUDED_FIELDS.contains(entry.getKey()) && !entry.getValue().isNull()) {
                configTree.set(entry.getKey(), entry.getValue());
            }
        });
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
     * Read a class relative resource and send it as the HTTP response body.
     *
     * @param exchange the HTTP exchange to send the response on
     * @param resource the class relative resource name
     * @param mimeType the response MIME type
     * @throws IOException when the resource is missing or the response cannot be written
     */
    private void sendResource(HttpExchange exchange, String resource, String mimeType) throws IOException {
        try (InputStream resourceStream = getClass().getResourceAsStream(resource)) {
            if (resourceStream == null) {
                sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Resource not found: " + resource);
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            resourceStream.transferTo(buffer);
            byte[] responseBytes = buffer.toByteArray();
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(responseBytes);
            }
        }
    }

    /**
     * Send a JSON string as the HTTP response body.
     *
     * @param exchange   the HTTP exchange to send the response on
     * @param statusCode the HTTP status code to return
     * @param json       the JSON content to send
     * @throws IOException when the response cannot be written
     */
    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle GET /devicePrefs?ip=<addr>, proxying the device {@code /prefs} endpoint server-side.
     * The device firmware does not send CORS headers, so the browser cannot read {@code /prefs} directly;
     * this endpoint fetches it from the JVM (no CORS) and returns the JSON to the client.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleDevicePrefs(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String ip = query == null ? null : queryParam(query, "ip");
        if (ip == null || !ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Missing or invalid ip parameter");
            return;
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + ip + "/prefs")).timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            byte[] responseBytes = response.body().getBytes(StandardCharsets.UTF_8);
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
     * Handle GET / (and any unknown path), serving the settings page.
     * The other endpoints are matched by their specific contexts before falling back to this root context.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isEmpty()) {
            handleSetConfigPage(exchange);
        } else {
            byte[] responseBytes = ("Not found: " + path).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, responseBytes.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(responseBytes);
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
         * Possible values for a single configuration field, exposed to the web settings page.
         * The {@code value} is the exact value to persist (English i18n string for localized enums,
         * or the numeric value for numeric enums) and the {@code label} is the human readable English text.
         */
        private record FieldOptions(List<Option> options, String type) {

        /**
         * A single selectable value.
         */
        private record Option(String value, String label) {
            }
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
