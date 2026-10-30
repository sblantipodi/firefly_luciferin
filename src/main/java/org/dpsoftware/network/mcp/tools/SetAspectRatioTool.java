/*
  SetAspectRatioTool.java

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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.network.mcp.AbstractMcpTool;
import org.dpsoftware.utilities.CommonUtility;

import java.util.Locale;
import java.util.Set;

/**
 * MCP tool that selects a capture aspect ratio or automatic black bar detection.
 */
@Slf4j
public class SetAspectRatioTool extends AbstractMcpTool {

    public static final String TOOL_NAME = "setAspectRatio";
    private static final Set<String> MODES = Set.of("AUTO", "FULLSCREEN", "LETTERBOX", "PILLARBOX");

    public SetAspectRatioTool(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * Return the MCP tool name.
     *
     * @return {@code "setAspectRatio"}
     */
    @Override
    public String getName() {
        return TOOL_NAME;
    }

    /**
     * Describe the automatic and manual capture modes.
     *
     * @return tool definition with a required aspectRatio
     */
    @Override
    public ObjectNode getDefinition() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", TOOL_NAME);
        tool.put("description", "Set the capture aspect ratio. AUTO enables automatic black bar detection. "
                + "FULLSCREEN captures the full screen; LETTERBOX excludes horizontal black bars; "
                + "PILLARBOX excludes vertical black bars. Manual modes disable automatic detection. "
                + "Uses the same action as the interface, including MQTT synchronization. "
                + "Does not start or stop capture or change LED power. Returns the applied mode and current LED matrix.");
        tool.put("icon", "settings");
        tool.putObject("annotations").put("idempotentHint", true);
        ObjectNode schema = tool.putObject("inputSchema");
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        ObjectNode aspectRatio = schema.putObject("properties").putObject("aspectRatio");
        aspectRatio.put("type", "string");
        aspectRatio.putArray("enum").add("AUTO").add("FULLSCREEN").add("LETTERBOX").add("PILLARBOX");
        schema.putArray("required").add("aspectRatio");
        return tool;
    }

    /**
     * Validate the requested mode before applying it on the FX thread.
     *
     * @param arguments input containing an aspectRatio
     * @return applied aspect ratio settings or a validation error
     * @throws Exception if execution on the FX thread fails
     */
    @Override
    public ObjectNode execute(JsonNode arguments) throws Exception {
        JsonNode value = arguments != null ? arguments.get("aspectRatio") : null;
        if (value == null || !value.isTextual()) {
            return createToolErrorResult("aspectRatio must be AUTO, FULLSCREEN, LETTERBOX, or PILLARBOX");
        }
        String mode = value.asText().trim().toUpperCase(Locale.ROOT);
        if (!MODES.contains(mode)) {
            return createToolErrorResult("aspectRatio must be AUTO, FULLSCREEN, LETTERBOX, or PILLARBOX");
        }
        ObjectNode state = runOnFxThread(() -> applyAspectRatio(mode));
        log.debug("MCP setAspectRatio result: {}", objectMapper.writeValueAsString(state));
        return createTextResult(objectMapper.writeValueAsString(state));
    }

    /**
     * Apply the selection through the shared tray action, also available in headless mode.
     *
     * @param mode validated mode name
     * @return actual automatic detection state and current capture matrix
     */
    private ObjectNode applyAspectRatio(String mode) {
        MainSingleton main = MainSingleton.getInstance();
        Configuration config = main.config;
        if (config == null) {
            throw new IllegalStateException("Configuration is not available");
        }
        if (main.guiManager == null || main.guiManager.trayIconManager == null) {
            throw new IllegalStateException("Aspect ratio manager is not available");
        }
        String selection = "AUTO".equals(mode) ? CommonUtility.getWord(LabelKey.AUTO_DETECT_BLACK_BARS)
                : Enums.AspectRatio.valueOf(mode).getBaseI18n();
        if (!"AUTO".equals(mode) && (config.getLedMatrix() == null || config.getLedMatrixInUse(selection) == null)) {
            throw new IllegalStateException("LED matrix is not available for " + mode);
        }
        main.guiManager.trayIconManager.manageAspectRatioListener(selection, true);
        ObjectNode state = objectMapper.createObjectNode();
        state.put("aspectRatio", config.isAutoDetectBlackBars() ? "AUTO"
                : config.getDefaultLedMatrix().toUpperCase(Locale.ROOT));
        state.put("autoDetectBlackBars", config.isAutoDetectBlackBars());
        state.put("defaultLedMatrix", config.getDefaultLedMatrix());
        return state;
    }
}
