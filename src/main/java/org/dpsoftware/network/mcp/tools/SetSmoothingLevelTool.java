/*
  SetSmoothingLevelTool.java

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
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.controllers.options.SmoothingOptions;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.network.mcp.AbstractMcpTool;
import org.dpsoftware.utilities.CommonUtility;

/**
 * MCP tool that applies one of the built-in smoothing levels.
 */
@Slf4j
public class SetSmoothingLevelTool extends AbstractMcpTool {

    public static final String TOOL_NAME = "setSmoothingLevel";

    public SetSmoothingLevelTool(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * Return the MCP tool name.
     *
     * @return {@code "setSmoothingLevel"}
     */
    @Override
    public String getName() {
        return TOOL_NAME;
    }

    /**
     * Describe the supported levels, including zero to disable smoothing.
     *
     * @return tool definition with a required integer level
     */
    @Override
    public ObjectNode getDefinition() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", TOOL_NAME);
        tool.put("description", "Set the Firefly Luciferin smoothing level. Use level 0 to disable smoothing, "
                + "or levels 1 through 6 for increasingly smooth color transitions. Applies the same presets as the interface. "
                + "Restarts capture only if it is already running and the smoothing parameters change; "
                + "does not start stopped capture. Returns the applied level, smoothing parameters, and whether a restart was scheduled.");
        tool.put("icon", "settings");
        tool.putObject("annotations").put("idempotentHint", true);
        ObjectNode inputSchema = tool.putObject("inputSchema");
        inputSchema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        inputSchema.put("type", "object");
        ObjectNode level = inputSchema.putObject("properties").putObject("level");
        level.put("type", "integer");
        level.put("minimum", 0);
        level.put("maximum", 6);
        level.put("description", "Smoothing level: 0 disables smoothing; 1 is the lightest and 6 is the strongest smoothing.");
        inputSchema.putArray("required").add("level");
        return tool;
    }

    /**
     * Validate the level before updating the runtime configuration on the FX thread.
     *
     * @param arguments input containing an integer level from zero to six
     * @return applied smoothing settings or a validation error
     * @throws Exception if execution on the FX thread fails
     */
    @Override
    public ObjectNode execute(JsonNode arguments) throws Exception {
        JsonNode level = arguments != null ? arguments.get("level") : null;
        if (level == null || !level.isIntegralNumber() || !level.canConvertToInt()
                || level.intValue() < 0 || level.intValue() > 6) {
            return createToolErrorResult("level must be an integer between 0 (disabled) and 6");
        }
        ObjectNode state = runOnFxThread(() -> applyLevel(level.intValue()));
        log.debug("MCP setSmoothingLevel result: {}", objectMapper.writeValueAsString(state));
        return createTextResult(objectMapper.writeValueAsString(state));
    }

    /**
     * Apply a preset and restart an active pipeline only when its parameters change.
     *
     * @param level validated smoothing level
     * @return actual applied settings and restart status
     */
    private ObjectNode applyLevel(int level) {
        MainSingleton main = MainSingleton.getInstance();
        Configuration config = main.config;
        if (config == null) {
            throw new IllegalStateException("Configuration is not available");
        }
        Enums.Smoothing smoothing = level == 0 ? Enums.Smoothing.DISABLED
                : Enums.Smoothing.valueOf("SMOOTHING_LVL_" + level);
        boolean changed = config.getEmaAlpha() != smoothing.getEmaAlpha()
                || config.getFrameInsertionTarget() != smoothing.getFrameInsertionFramerate()
                || config.getSmoothingTargetFramerate() != Constants.DEFAULT_SMOOTHING_TARGET;
        boolean restartScheduled = main.RUNNING && changed;
        if (restartScheduled && main.guiManager == null) {
            throw new IllegalStateException("Capture manager is not available");
        }
        SmoothingOptions.applyPreset(config, smoothing.getBaseI18n());
        if (restartScheduled) {
            PipelineManager.restartCapture(CommonUtility::run);
        }
        ObjectNode state = objectMapper.createObjectNode();
        state.put("level", level);
        state.put("smoothingType", config.getSmoothingType());
        state.put("emaAlpha", config.getEmaAlpha());
        state.put("frameInsertionTarget", config.getFrameInsertionTarget());
        state.put("smoothingTargetFramerate", config.getSmoothingTargetFramerate());
        state.put("restartScheduled", restartScheduled);
        return state;
    }
}
