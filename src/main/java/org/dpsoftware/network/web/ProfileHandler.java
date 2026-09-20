/*
  ProfileHandler.java

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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Handles profile listing, activation and deletion. */
public class ProfileHandler {

    private static final String JSON_OK = "{\"status\":\"OK\"}";
    private final StorageManager storageManager = new StorageManager();

    /**
     * Read the {@code name} query parameter from the request, or {@code null} when absent.
     *
     * @param exchange the HTTP exchange containing the request
     * @return the name value or {@code null}
     */
    private static String queryNameParam(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals("name")) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    /**
     * Returns profile names for this instance.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleListProfiles(HttpExchange exchange) throws IOException {
        List<String> profiles = new ArrayList<>(new LinkedHashSet<>(storageManager.listProfilesForThisInstance()));
        profiles.removeIf(p -> CommonUtility.getWord(Constants.DEFAULT, Locale.ENGLISH).equals(p));
        profiles.addFirst(CommonUtility.getWord(Constants.DEFAULT, Locale.ENGLISH));
        String profileArg = MainSingleton.getInstance().profileArg;
        String activeProfile;
        if (profileArg == null || profileArg.isEmpty()
                || Constants.DEFAULT.equals(profileArg)
                || CommonUtility.getWord(Constants.DEFAULT).equals(profileArg)) {
            activeProfile = CommonUtility.getWord(Constants.DEFAULT, Locale.ENGLISH);
        } else {
            activeProfile = profileArg;
        }
        ObjectNode node = CommonUtility.JSON_MAPPER.createObjectNode();
        node.putPOJO("profiles", profiles);
        node.put("activeProfile", activeProfile);
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(node);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Activates the requested or default profile.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleActivateProfile(HttpExchange exchange) throws IOException {
        String name = queryNameParam(exchange);
        if (name == null || name.isEmpty()) {
            sendMissingNameError(exchange);
            return;
        }
        // The default profile uses the main configuration.
        sendOkJson(exchange);
        if (name.equals(CommonUtility.getWord(Constants.DEFAULT))) {
            NativeExecutor.restartNativeInstance("\"" + Constants.DEFAULT + "\"");
        } else {
            NativeExecutor.restartNativeInstance("\"" + name + "\"");
        }
    }

    /**
     * Deletes an inactive, non-default profile.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleRemoveProfile(HttpExchange exchange) throws IOException {
        String name = queryNameParam(exchange);
        if (name == null || name.isEmpty()) {
            sendMissingNameError(exchange);
            return;
        }
        String defaultWord = CommonUtility.getWord(Constants.DEFAULT, Locale.ENGLISH);
        String profileArg = MainSingleton.getInstance().profileArg;
        String activeProfile;
        if (profileArg == null || profileArg.isEmpty() || Constants.DEFAULT.equals(profileArg) || CommonUtility.getWord(Constants.DEFAULT).equals(profileArg)) {
            activeProfile = defaultWord;
        } else {
            activeProfile = profileArg;
        }
        if (name.equals(defaultWord) || name.equals(activeProfile)) {
            sendBadRequest(exchange, "Cannot remove the " + (name.equals(defaultWord) ? "default" : "active") + " profile");
            return;
        }
        boolean deleted = storageManager.deleteProfile(name);
        if (!deleted) {
            sendInternalError(exchange, "Unable to delete profile: " + name);
            return;
        }
        sendOkJson(exchange);
    }

    /**
     * Send a plain text internal server error response.
     *
     * @param exchange the HTTP exchange to reply on
     * @param message  the human-readable error description
     * @throws IOException when the response cannot be written
     */
    private void sendInternalError(HttpExchange exchange, String message) throws IOException {
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
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
     * Send a 400 plain text error response for a missing name parameter.
     *
     * @param exchange the HTTP exchange to reply on
     * @throws IOException when the response cannot be written
     */
    private void sendMissingNameError(HttpExchange exchange) throws IOException {
        sendBadRequest(exchange, "Missing or empty name parameter");
    }

    /**
     * Send a 400 plain text error response with a custom message.
     *
     * @param exchange the HTTP exchange to reply on
     * @param message  the human-readable error description
     * @throws IOException when the response cannot be written
     */
    private void sendBadRequest(HttpExchange exchange, String message) throws IOException {
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }
}
