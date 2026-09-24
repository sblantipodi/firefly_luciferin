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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Handles profile listing, creation, activation and deletion.
 */
@Slf4j
public class ProfileHandler {

    private final StorageManager storageManager = new StorageManager();

    /**
     * Read the name query parameter from the request, or null when absent.
     *
     * @param exchange the HTTP exchange containing the request
     * @return the name value or null
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
        HttpResponses.sendJson(exchange, node);
    }

    /**
     * Creates a profile from the supplied configuration without activating it.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleAddProfile(HttpExchange exchange) throws IOException {
        String name = exchange.getRequestURI().getQuery();
        String profileName = null;
        if (name != null) {
            for (String pair : name.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && pair.substring(0, eq).equals("name")) {
                    profileName = pair.substring(eq + 1);
                }
            }
        }
        if (profileName == null || profileName.isEmpty()) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Missing or empty name parameter");
            return;
        }
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
        Configuration savedConfig = storageManager.readProfileInUseConfig();
        if (savedConfig == null) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Configuration not found");
            return;
        }
        Configuration updatedConfig = ConfigurationPayload.apply(payload, savedConfig);
        updatedConfig.setEffect(LocalizedEnum.fromTextToBase(Enums.Effect.class, updatedConfig.getEffect()));
        int whoAmI = MainSingleton.getInstance().whoAmI;
        String filename;
        if (profileName.equals(CommonUtility.getWord(Constants.DEFAULT))) {
            filename = whoAmI == 2 ? Constants.CONFIG_FILENAME_2 : whoAmI == 3 ? Constants.CONFIG_FILENAME_3 : Constants.CONFIG_FILENAME;
        } else {
            filename = whoAmI + "_" + profileName + Constants.YAML_EXTENSION;
        }
        try {
            storageManager.writeConfig(updatedConfig, filename);
        } catch (IOException e) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Unable to save profile: " + e.getMessage());
            return;
        }
        log.info("Profile created via addProfile endpoint: {}", filename);
        HttpResponses.sendOk(exchange);
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
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Missing or empty name parameter");
            return;
        }
        // The default profile uses the main configuration.
        HttpResponses.sendOk(exchange);
        if (name.equals(CommonUtility.getWord(Constants.DEFAULT))) {
            NativeExecutor.restartNativeInstance("\"" + Constants.DEFAULT + "\"");
        } else {
            NativeExecutor.restartNativeInstance("\"" + name + "\"");
        }
    }

    /**
     * Deletes an inactive, non default profile.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleRemoveProfile(HttpExchange exchange) throws IOException {
        String name = queryNameParam(exchange);
        if (name == null || name.isEmpty()) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Missing or empty name parameter");
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
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Cannot remove the " + (name.equals(defaultWord) ? "default" : "active") + " profile");
            return;
        }
        boolean deleted = storageManager.deleteProfile(name);
        if (!deleted) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Unable to delete profile: " + name);
            return;
        }
        HttpResponses.sendOk(exchange);
    }
}
