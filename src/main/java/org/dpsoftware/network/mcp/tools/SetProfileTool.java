/*
  SetProfileTool.java

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
package org.dpsoftware.network.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.network.mcp.AbstractMcpTool;
import org.dpsoftware.utilities.CommonUtility;

import java.util.Set;

/**
 * MCP tool that lists available profiles and applies a profile by setting profileArg.
 */
@Slf4j
public class SetProfileTool extends AbstractMcpTool {

    public static final String TOOL_NAME = "setProfile";

    public SetProfileTool(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * Return the MCP tool name.
     *
     * @return {@code "setProfile"}
     */
    @Override
    public String getName() {
        return TOOL_NAME;
    }

    /**
     * Build the MCP tool definition JSON with schema for profileName.
     *
     * @return an ObjectNode describing the tool name, description, and input schema
     */
    @Override
    public ObjectNode getDefinition() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", TOOL_NAME);
        tool.put("description", "List available profiles or apply a profile. If no argument is provided, returns the list of available profiles. If profileName is provided, applies that profile.");
        ObjectNode annotations = tool.putObject("annotations");
        annotations.put("idempotentHint", true);
        ObjectNode inputSchema = tool.putObject("inputSchema");
        inputSchema.put("type", "object");
        ObjectNode properties = inputSchema.putObject("properties");

        ObjectNode profileName = properties.putObject("profileName");
        profileName.put("type", "string");
        profileName.put("description", "Profile name to apply. If omitted, returns the list of available profiles.");

        inputSchema.putArray("required");
        return tool;
    }

    /**
     * Execute the tool: list profiles or apply a profile.
     *
     * @param arguments the JSON node containing optional profileName parameter
     * @return a text result containing the list of profiles or confirmation of applied profile
     * @throws Exception if execution on the FX thread fails
     */
    @Override
    public ObjectNode execute(JsonNode arguments) throws Exception {
        String profileName = (arguments != null && !arguments.isNull() && arguments.has("profileName") && !arguments.get("profileName").isNull())
                ? arguments.get("profileName").asText() : null;

        if (profileName == null || profileName.isBlank()) {
            return listProfiles();
        }
        return applyProfile(profileName);
    }

    /**
     * Return the list of available profiles for this instance.
     *
     * @return a text result containing the JSON array of profile names
     */
    private ObjectNode listProfiles() {
        Set<String> profiles = new StorageManager().listProfilesForThisInstance();
        log.debug("MCP setProfile list: {}", profiles);
        try {
            return createTextResult(objectMapper.writeValueAsString(profiles));
        } catch (Exception e) {
            return createToolErrorResult("Failed to serialize profiles: " + e.getMessage());
        }
    }

    /**
     * Apply the named profile if it exists.
     *
     * @param profileName the profile name to apply
     * @return a success or error result node
     */
    private ObjectNode applyProfile(String profileName) {
        Set<String> profiles = new StorageManager().listProfilesForThisInstance();
        if (!profiles.contains(profileName)) {
            String msg = "Profile not found: " + profileName + ". Available profiles: " + profiles;
            log.warn("MCP {}", msg);
            return createToolErrorResult(msg);
        }
        if (profileName.equals(CommonUtility.getWord(Constants.DEFAULT))) {
            NativeExecutor.restartNativeInstance(null);
        } else {
            NativeExecutor.restartNativeInstance(profileName);
        }
        log.info("MCP profile set to {}", profileName);
        ObjectNode result = objectMapper.createObjectNode();
        result.put("profile", profileName);
        result.put("status", "applied");
        try {
            return createTextResult(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            return createToolErrorResult("Failed to serialize result: " + e.getMessage());
        }
    }

}
