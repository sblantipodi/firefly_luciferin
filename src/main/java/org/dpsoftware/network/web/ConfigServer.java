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
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
    private HttpServer httpServer;

    /**
     * Start the config HTTP endpoint on localhost.
     */
    @SuppressWarnings("all")
    public void start() {
        if (httpServer != null) {
            return;
        }
        try {
            httpServer = HttpServer.create(new InetSocketAddress(Constants.MSG_SERVER_HOST, Constants.CONFIG_SERVER_DEFAULT_PORT), 0);
            httpServer.createContext(Constants.CONFIG_ENDPOINT, withGuard(this::handleGetConfig, method -> method.equalsIgnoreCase("GET")));
            httpServer.createContext(Constants.SET_CONFIG_PAGE_ENDPOINT, withGuard(this::handleSetConfigPage, method -> method.equalsIgnoreCase("GET")));
            httpServer.createContext(Constants.SET_CONFIG_PAGE_JS_ENDPOINT, withGuard(this::handleSetConfigPageJs, method -> method.equalsIgnoreCase("GET")));
            httpServer.createContext(Constants.SET_CONFIG_ENDPOINT, withGuard(this::handleSetConfig, method -> method.equalsIgnoreCase("POST")));
            httpServer.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "firefly-config-server");
                thread.setDaemon(true);
                return thread;
            }));
            httpServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "firefly-config-shutdown"));
            log.info("Config server listening on http://{}:{}{}", Constants.MSG_SERVER_HOST, Constants.CONFIG_SERVER_DEFAULT_PORT, Constants.CONFIG_ENDPOINT);
        } catch (IOException e) {
            log.warn("Unable to start config server: {}", e.getMessage());
        }
    }

    /**
     * Stop the config endpoint.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
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
