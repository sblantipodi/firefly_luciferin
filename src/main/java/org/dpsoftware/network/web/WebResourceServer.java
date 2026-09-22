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
import org.dpsoftware.config.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Serves static resources for the settings page. */
public class WebResourceServer {

    private static final String SET_CONFIG_PAGE_RESOURCE = "set-config.html"; // Settings page resource.
    private static final String SET_CONFIG_PAGE_JS_RESOURCE = "set-config.js"; // Settings script resource.
    private static final String SET_CONFIG_CORE_JS_RESOURCE = "set-config-core.js";
    private static final String SET_CONFIG_DEVICE_JS_RESOURCE = "set-config-device.js";
    private static final String SET_CONFIG_UI_JS_RESOURCE = "set-config-ui.js";
    private static final String SET_CONFIG_CSS_RESOURCE = "set-config.css";
    private static final String WEBRTC_PREVIEW_JS_RESOURCE = "webrtc-preview.js";
    private static final Set<String> SETTINGS_MODULES = Set.of(
            "set-config-app.js", "set-config-api.js", "set-config-state.js", "set-config-schema.js",
            "set-config-profiles.js", "set-config-preview.js", "set-config-status.js");
    // Marker replaced at serve time with the Java-side default config server port.
    private static final String PORT_PLACEHOLDER = "__CONFIG_SERVER_DEFAULT_PORT__";

    /**
     * Serves the settings page.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleSetConfigPage(HttpExchange exchange) throws IOException {
        sendResource(exchange, SET_CONFIG_PAGE_RESOURCE, "text/html; charset=utf-8");
    }

    /**
     * Handle GET /setConfig.css, serving the settings page stylesheet.
     *
     * @param exchange the HTTP exchange to send the response on
     * @throws IOException when the response cannot be written
     */
    public void handleSetConfigCss(HttpExchange exchange) throws IOException {
        sendResource(exchange, SET_CONFIG_CSS_RESOURCE, "text/css; charset=utf-8");
    }

    /**
     * Serves settings JavaScript files.
     *
     * @param exchange the HTTP exchange to send the response on
     * @throws IOException when the response cannot be written
     */
    public void handleSetConfigPageJs(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String resource;
        String filename = path.substring(path.lastIndexOf('/') + 1);
        if (SETTINGS_MODULES.contains(filename)) {
            resource = filename;
        } else if (path.endsWith("set-config-core.js")) {
            resource = SET_CONFIG_CORE_JS_RESOURCE;
        } else if (path.endsWith("set-config-device.js")) {
            resource = SET_CONFIG_DEVICE_JS_RESOURCE;
        } else if (path.endsWith("set-config-ui.js")) {
            resource = SET_CONFIG_UI_JS_RESOURCE;
        } else {
            resource = SET_CONFIG_PAGE_JS_RESOURCE;
        }
        sendResource(exchange, resource, "application/javascript; charset=utf-8");
    }

    /**
     * Handle GET /webrtc-preview.js, serving the browser-side WebRTC live preview module.
     *
     * @param exchange the HTTP exchange to send the response on
     * @throws IOException when the resource is missing or the response cannot be written
     */
    public void handleWebrtcPreviewJs(HttpExchange exchange) throws IOException {
        String resource = readResource(WEBRTC_PREVIEW_JS_RESOURCE);
        if (resource == null) {
            sendResource(exchange, WEBRTC_PREVIEW_JS_RESOURCE, "application/javascript; charset=utf-8");
            return;
        }
        resource = resource.replace(PORT_PLACEHOLDER, String.valueOf(Constants.CONFIG_SERVER_DEFAULT_PORT));
        byte[] responseBytes = resource.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        HttpResponses.sendBytes(exchange, HttpURLConnection.HTTP_OK, "application/javascript; charset=utf-8", responseBytes);
    }

    /**
     * Handles the root and static-resource fallback.
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
        } else if (path.endsWith(".css")) {
            handleSetConfigCss(exchange);
        } else {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Not found: " + path);
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
        String content = readResource(resource);
        if (content == null) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Resource not found: " + resource);
            return;
        }
        byte[] responseBytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        HttpResponses.sendBytes(exchange, HttpURLConnection.HTTP_OK, mimeType, responseBytes);
    }

    /**
     * Read a class relative resource as a UTF-8 string.
     *
     * @param resource the class relative resource name
     * @return the resource content, or null when the resource is missing
     * @throws IOException when the resource cannot be read
     */
    private String readResource(String resource) throws IOException {
        try (InputStream resourceStream = getClass().getResourceAsStream(resource)) {
            if (resourceStream == null) {
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            resourceStream.transferTo(buffer);
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }
}
