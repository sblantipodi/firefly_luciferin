/*
  HttpResponses.java

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
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Writes HTTP response bodies without changing endpoint specific headers or status codes.
 */
final class HttpResponses {

    private HttpResponses() {
    }

    /**
     * Serializes a value with the application's JSON mapper.
     */
    static void sendJson(HttpExchange exchange, Object value) throws IOException {
        sendBytes(exchange, HttpURLConnection.HTTP_OK, "application/json; charset=utf-8",
                CommonUtility.JSON_MAPPER.writeValueAsBytes(value));
    }

    /**
     * Sends JSON that is already serialized, including responses proxied from a device.
     */
    static void sendRawJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        sendBytes(exchange, statusCode, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends a 200 OK response with a JSON status body.
     *
     * @param exchange the HTTP exchange to send the response on
     * @throws IOException when the response cannot be written
     */
    static void sendOk(HttpExchange exchange) throws IOException {
        sendRawJson(exchange, HttpURLConnection.HTTP_OK, "{\"status\":\"OK\"}");
    }

    /**
     * Sends a plain text response with the given status code.
     *
     * @param exchange   the HTTP exchange to send the response on
     * @param statusCode the HTTP status code
     * @param message    the plain text body
     * @throws IOException when the response cannot be written
     */
    static void sendText(HttpExchange exchange, int statusCode, String message) throws IOException {
        sendBytes(exchange, statusCode, "text/plain; charset=utf-8", message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Preserves any headers set by the caller and closes the response body after writing.
     *
     * @param exchange the HTTP exchange to send the response on
     * @param statusCode the HTTP status code
     * @param contentType the response MIME type
     * @param body the response body
     * @throws IOException when the response cannot be written
     */
    static void sendBytes(HttpExchange exchange, int statusCode, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(body);
        }
    }
}
