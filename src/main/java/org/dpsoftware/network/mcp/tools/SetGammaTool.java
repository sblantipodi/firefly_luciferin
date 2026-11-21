/*
  SetGammaTool.java

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.controllers.options.GammaOptions;
import org.dpsoftware.gui.controllers.options.MiscTabOptions;
import org.dpsoftware.network.mcp.AbstractMcpTool;

import java.util.Locale;

/**
 * MCP tool that changes manual gamma and adaptive gamma settings.
 */
@Slf4j
public class SetGammaTool extends AbstractMcpTool {

    public static final String TOOL_NAME = "setGamma";

    public SetGammaTool(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * Return the MCP tool name.
     *
     * @return {@code "setGamma"}
     */
    @Override
    public String getName() {
        return TOOL_NAME;
    }

    /**
     * Describe the supported manual gamma values and adaptive gamma controls.
     *
     * @return tool definition requiring at least one setting
     */
    @Override
    public ObjectNode getDefinition() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", TOOL_NAME);
        tool.put("description", "Set manual or automatic (adaptive) gamma. "
                + "Providing gamma alone selects manual gamma. Providing level enables automatic gamma, "
                + "unless automatic is explicitly false. An explicit automatic value always takes precedence. "
                + "With automatic true, gamma sets the baseline for adaptive gamma. "
                + "At least one of gamma, automatic, or level is required. "
                + "Does not start, stop, or restart capture or change LED power. Returns the applied settings.");
        tool.put("icon", "settings");
        tool.putObject("annotations").put("idempotentHint", true);
        ObjectNode schema = tool.putObject("inputSchema");
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode gamma = properties.putObject("gamma");
        gamma.put("type", "number");
        gamma.put("description", "Manual gamma value, or adaptive gamma baseline when automatic is enabled.");
        ArrayNode gammaValues = gamma.putArray("enum");
        MiscTabOptions.gammaValues().forEach(value -> gammaValues.add(Double.parseDouble(value)));
        ObjectNode automatic = properties.putObject("automatic");
        automatic.put("type", "boolean");
        automatic.put("description", "Enable or disable adaptive gamma; overrides the mode implied by gamma or level.");
        ObjectNode level = properties.putObject("level");
        level.put("type", "string");
        level.put("description", "Adaptive gamma brightness level, matching the interface.");
        level.putArray("enum").add("LOW").add("MEDIUM").add("HIGH");
        ArrayNode anyOf = schema.putArray("anyOf");
        for (String field : new String[]{"gamma", "automatic", "level"}) {
            anyOf.addObject().putArray("required").add(field);
        }
        return tool;
    }

    /**
     * Validate all supplied fields before updating settings on the FX thread.
     *
     * @param arguments gamma, automatic mode, and/or adaptive brightness level
     * @return applied gamma settings or a validation error
     * @throws Exception if execution on the FX thread fails
     */
    @Override
    public ObjectNode execute(JsonNode arguments) throws Exception {
        if (arguments == null || !arguments.isObject()
                || !(arguments.has("gamma") || arguments.has("automatic") || arguments.has("level"))) {
            return createToolErrorResult("Provide at least one of gamma, automatic, or level");
        }
        JsonNode gamma = arguments.get("gamma");
        if (gamma != null && (!gamma.isNumber() || !Double.isFinite(gamma.doubleValue())
                || !MiscTabOptions.gammaValues().contains(Double.toString(gamma.doubleValue())))) {
            return createToolErrorResult("gamma must be one of " + MiscTabOptions.gammaValues());
        }
        JsonNode automatic = arguments.get("automatic");
        if (automatic != null && !automatic.isBoolean()) {
            return createToolErrorResult("automatic must be boolean");
        }
        JsonNode level = arguments.get("level");
        Enums.GammaLevel selectedLevel = null;
        if (level != null) {
            if (!level.isTextual()) {
                return createToolErrorResult("level must be LOW, MEDIUM, or HIGH");
            }
            try {
                selectedLevel = Enums.GammaLevel.valueOf(level.asText().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return createToolErrorResult("level must be LOW, MEDIUM, or HIGH");
            }
        }
        boolean enableAutomatic = automatic != null ? automatic.booleanValue() : selectedLevel != null;
        Enums.GammaLevel validatedLevel = selectedLevel;
        ObjectNode state = runOnFxThread(() -> applyGamma(gamma, enableAutomatic, validatedLevel));
        log.debug("MCP setGamma result: {}", objectMapper.writeValueAsString(state));
        return createTextResult(objectMapper.writeValueAsString(state));
    }

    /**
     * Apply validated settings using the same helpers as the web controls.
     *
     * @param gamma     optional manual value or adaptive baseline
     * @param automatic desired adaptive gamma mode
     * @param level     optional adaptive brightness level
     * @return actual manual gamma, automatic mode, and adaptive level
     */
    private ObjectNode applyGamma(JsonNode gamma, boolean automatic, Enums.GammaLevel level) {
        Configuration config = MainSingleton.getInstance().config;
        if (config == null) {
            throw new IllegalStateException("Configuration is not available");
        }
        GammaOptions.apply(config, automatic, level != null ? level.getBaseI18n() : config.getGammaLevel());
        if (gamma != null) {
            MiscTabOptions.applyWebChange(config, "gamma", objectMapper.getNodeFactory().textNode(Double.toString(gamma.doubleValue())));
        }
        ObjectNode state = objectMapper.createObjectNode();
        state.put("gamma", config.getGamma());
        state.put("automatic", config.isEnableAutomaticGamma());
        state.put("level", config.getGammaLevel().toUpperCase(Locale.ROOT));
        return state;
    }
}
