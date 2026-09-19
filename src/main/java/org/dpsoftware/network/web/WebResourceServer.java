/*
  WebResourceServer.java

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

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Serves the static resources of the web settings page: the HTML page, its JavaScript files,
 * and the 404 fallback for unknown paths.
 * <p>
 * The resources are co-located in this package ({@code org.dpsoftware.network.web}).
 * The package is {@code opens} in the module descriptor so the class loader can read the
 * resources in JPMS module mode (a non opened package would be invisible to it).
 */
public class WebResourceServer {

    /**
     * Settings web page resource, co-located in this package.
     */
    private static final String SET_CONFIG_PAGE_RESOURCE = "setConfig.html";
    /**
     * Settings page JavaScript resource, co-located in this package.
     */
    private static final String SET_CONFIG_PAGE_JS_RESOURCE = "setConfig.js";
    private static final String SET_CONFIG_CORE_JS_RESOURCE = "setConfig-core.js";
    private static final String SET_CONFIG_DEVICE_JS_RESOURCE = "setConfig-device.js";
    private static final String SET_CONFIG_UI_JS_RESOURCE = "setConfig-ui.js";

    /**
     * Handle GET /setConfigPage, serving the minimal HTML page that hosts the settings form.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleSetConfigPage(HttpExchange exchange) throws IOException {
        sendResource(exchange, SET_CONFIG_PAGE_RESOURCE, "text/html; charset=utf-8");
    }

    /**
     * Handle GET requests for the settings page JavaScript files.
     * Serves setConfig.js, setConfig-core.js, setConfig-device.js, setConfig-ui.js.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleSetConfigPageJs(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String resource;
        if (path.endsWith("setConfig-core.js")) {
            resource = SET_CONFIG_CORE_JS_RESOURCE;
        } else if (path.endsWith("setConfig-device.js")) {
            resource = SET_CONFIG_DEVICE_JS_RESOURCE;
        } else if (path.endsWith("setConfig-ui.js")) {
            resource = SET_CONFIG_UI_JS_RESOURCE;
        } else {
            resource = SET_CONFIG_PAGE_JS_RESOURCE;
        }
        sendResource(exchange, resource, "application/javascript; charset=utf-8");
    }

    /**
     * Handle GET / (and any unknown path), serving the settings page.
     * The other endpoints are matched by their specific contexts before falling back to this root context.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isEmpty()) {
            handleSetConfigPage(exchange);
        } else if (path.endsWith(".js")) {
            handleSetConfigPageJs(exchange);
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
                byte[] responseBytes = ("Resource not found: " + resource).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, responseBytes.length);
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    responseBody.write(responseBytes);
                }
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            resourceStream.transferTo(buffer);
            byte[] responseBytes = buffer.toByteArray();
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(responseBytes);
            }
        }
    }
}
