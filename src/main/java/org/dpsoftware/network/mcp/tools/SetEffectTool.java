/*
  SetEffectTool.java

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
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.network.mcp.AbstractMcpTool;
import org.dpsoftware.utilities.CommonUtility;

import java.util.Arrays;
import java.util.Locale;

/**
 * MCP tool that updates the active effect, LED state, brightness, and capture pipeline.
 */
@Slf4j
public class SetEffectTool extends AbstractMcpTool {

    public static final String TOOL_NAME = "setEffect";
    private static final String EFFECT_DESCRIPTION = """
                Effect description:
                Accepts enum names or natural-language aliases, even in different languages.
                The agent should interpret the user request and select the enum that best matches the intended effect.
                    ambilight, bias light → BIAS_LIGHT
                    solid, fixed color → SOLID
                    rainbow, arcobaleno → RAINBOW
                Rules:
                    LEDs and Lights are synonyms.
                    Do not start or stop capture unless explicitly requested.
                    When setting an effect, set the effect first, then turn on the LEDs.
                    When changing brightness, update the value, then turn on the LEDs.
                    When changing color (r, g, b), update the color, then turn on the LEDs.
                    For a solid color: effect = SOLID + (r, g, b) values.
            """;

    public SetEffectTool(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * Format the list of available effect enum names as a readable string.
     *
     * @return a comma-separated string of effect names
     */
    private static String availableEffectNames() {
        return Arrays.toString(Arrays.stream(Enums.Effect.values()).map(Enum::name).toArray(String[]::new));
    }

    /**
     * Return the MCP tool name.
     *
     * @return {@code "setEffect"}
     */
    @Override
    public String getName() {
        return TOOL_NAME;
    }

    /**
     * Build the MCP tool definition JSON with schema for effect, LED toggle, brightness, capture state, and color.
     *
     * @return an ObjectNode describing the tool name, description, and input schema
     */
    @Override
    public ObjectNode getDefinition() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", TOOL_NAME);
        tool.put("description", "Set Firefly Luciferin effect, LED power state, brightness, and capture pipeline state.");
        ObjectNode annotations = tool.putObject("annotations");
        annotations.put("idempotentHint", true);
        ObjectNode inputSchema = tool.putObject("inputSchema");
        inputSchema.put("type", "object");
        ObjectNode properties = inputSchema.putObject("properties");

        ObjectNode effect = properties.putObject("effect");
        effect.put("type", "string");
        effect.put("description", EFFECT_DESCRIPTION + "Available enum names: " + availableEffectNames());
        ArrayNode examples = effect.putArray("examples");
        examples.add("BIAS_LIGHT");
        examples.add("RAINBOW");
        examples.add("rainbow");
        examples.add("Arcobaleno");
        examples.add("MUSIC_MODE_RAINBOW");

        ObjectNode toggleLed = properties.putObject("toggleLed");
        toggleLed.put("type", "boolean");
        toggleLed.put("description", "true turns LEDs on, false turns LEDs off.");

        ObjectNode brightness = properties.putObject("brightness");
        brightness.put("type", "integer");
        brightness.put("minimum", 0);
        brightness.put("maximum", 255);
        brightness.put("description", "Brightness from 0 to 255.");

        ObjectNode running = properties.putObject("running");
        running.put("type", "boolean");
        running.put("description", "true starts the capture pipeline, false stops it.");

        ObjectNode colorR = properties.putObject("r");
        colorR.put("type", "integer");
        colorR.put("minimum", 0);
        colorR.put("maximum", 255);
        colorR.put("description", "Red component of the solid color (0-255). Use together with g and b.");

        ObjectNode colorG = properties.putObject("g");
        colorG.put("type", "integer");
        colorG.put("minimum", 0);
        colorG.put("maximum", 255);
        colorG.put("description", "Green component of the solid color (0-255). Use together with r and b.");

        ObjectNode colorB = properties.putObject("b");
        colorB.put("type", "integer");
        colorB.put("minimum", 0);
        colorB.put("maximum", 255);
        colorB.put("description", "Blue component of the solid color (0-255). Use together with r and g.");

        inputSchema.putArray("required");
        return tool;
    }

    /**
     * Parse the incoming arguments, validate fields, and delegate to {@link #applySetEffect(SetEffectRequest)}.
     *
     * @param arguments the JSON node containing tool parameters
     * @return a text result with the updated device state as JSON string
     * @throws Exception if execution on the FX thread fails
     */
    @Override
    public ObjectNode execute(JsonNode arguments) throws Exception {
        SetEffectRequest request = parseRequest(arguments);
        if (request.errorMessage != null) {
            log.debug("MCP setEffect validation error: {}", request.errorMessage);
            return createToolErrorResult(request.errorMessage);
        }
        ObjectNode state = runOnFxThread(() -> applySetEffect(request));
        log.debug("MCP setEffect result: {}", objectMapper.writeValueAsString(state));
        return createTextResult(objectMapper.writeValueAsString(state));
    }

    /**
     * Parse and validate the tool arguments into a typed request object.
     *
     * @param arguments the raw JSON arguments from the MCP call
     * @return a SetEffectRequest populated with validated values or an error message
     */
    private SetEffectRequest parseRequest(JsonNode arguments) {
        log.debug("MCP setEffect parsing arguments: {}", arguments);
        SetEffectRequest request = new SetEffectRequest();
        if (arguments == null || arguments.isMissingNode() || arguments.isNull()) {
            return request;
        }
        JsonNode effect = arguments.get("effect");
        if (effect != null && !effect.isNull()) {
            if (!effect.isTextual()) {
                request.errorMessage = "effect must be a string";
                return request;
            }
            Enums.Effect resolvedEffect = resolveEffect(effect.asText());
            if (resolvedEffect == null) {
                request.errorMessage = "Unknown effect: " + effect.asText();
                return request;
            }
            request.effect = resolvedEffect;
        }
        JsonNode brightness = arguments.get("brightness");
        if (brightness != null && !brightness.isNull()) {
            if (!brightness.isIntegralNumber() || !brightness.canConvertToInt() || brightness.asInt() < 0 || brightness.asInt() > 255) {
                request.errorMessage = "brightness must be an integer between 0 and 255";
                return request;
            }
            request.brightness = brightness.asInt();
        }
        JsonNode toggleLed = arguments.get("toggleLed");
        if (toggleLed != null && !toggleLed.isNull()) {
            if (!toggleLed.isBoolean()) {
                request.errorMessage = "toggleLed must be a boolean";
                return request;
            }
            request.toggleLed = toggleLed.asBoolean();
        }
        JsonNode running = arguments.get("running");
        if (running != null && !running.isNull()) {
            if (!running.isBoolean()) {
                request.errorMessage = "running must be a boolean";
                return request;
            }
            request.running = running.asBoolean();
        }
        JsonNode colorR = arguments.get("r");
        JsonNode colorG = arguments.get("g");
        JsonNode colorB = arguments.get("b");
        boolean hasR = colorR != null && !colorR.isNull();
        boolean hasG = colorG != null && !colorG.isNull();
        boolean hasB = colorB != null && !colorB.isNull();
        if (hasR || hasG || hasB) {
            if (!hasR || !hasG || !hasB) {
                request.errorMessage = "r, g, and b must all be provided together";
                return request;
            }
            if (!colorR.isIntegralNumber() || colorR.asInt() < 0 || colorR.asInt() > 255 || !colorG.isIntegralNumber() || colorG.asInt() < 0 || colorG.asInt() > 255 || !colorB.isIntegralNumber() || colorB.asInt() < 0 || colorB.asInt() > 255) {
                request.errorMessage = "r, g, b must be integers between 0 and 255";
                return request;
            }
            request.colorR = colorR.asInt();
            request.colorG = colorG.asInt();
            request.colorB = colorB.asInt();
        }
        return request;
    }

    /**
     * Apply the validated request to the running configuration on the FX thread.
     *
     * @param request the parsed and validated set-effect request
     * @return a JSON object reflecting the current device state after applying changes
     */
    private ObjectNode applySetEffect(SetEffectRequest request) {
        if (MainSingleton.getInstance().config == null) {
            throw new IllegalStateException("Configuration is not available");
        }

        if (request.effect != null) {
            MainSingleton.getInstance().config.setEffect(request.effect.getBaseI18n());
            CommonUtility.turnOnLEDs();
            log.info("MCP effect set to {}", request.effect.getBaseI18n());
        }
        if (request.brightness != null) {
            MainSingleton.getInstance().config.setBrightness(request.brightness);
            CommonUtility.turnOnLEDs();
            log.info("MCP brightness set to {}", request.brightness);
        }
        if (request.toggleLed != null) {
            MainSingleton.getInstance().config.setToggleLed(request.toggleLed);
            if (request.toggleLed) {
                CommonUtility.turnOnLEDs();
            } else {
                CommonUtility.turnOffLEDs(MainSingleton.getInstance().config);
            }
            log.info("MCP LED state set to {}", request.toggleLed);
        }
        if (request.running != null && MainSingleton.getInstance().guiManager != null) {
            if (request.running) {
                MainSingleton.getInstance().guiManager.startCapturingThreads();
            } else if (MainSingleton.getInstance().guiManager.pipelineManager != null) {
                MainSingleton.getInstance().guiManager.pipelineManager.stopCapturePipeline();
            }
            log.info("MCP capture running set to {}", request.running);
        }
        if (request.colorR != null) {
            int brightness = MainSingleton.getInstance().config.getBrightness();
            MainSingleton.getInstance().config.setColorChooser(request.colorR + "," + request.colorG + "," + request.colorB + "," + brightness);
            CommonUtility.turnOnLEDs();
            log.info("MCP color set to r={} g={} b={}", request.colorR, request.colorG, request.colorB);
        }
        ObjectNode state = objectMapper.createObjectNode();
        state.put("effect", MainSingleton.getInstance().config.getEffect());
        state.put("brightness", MainSingleton.getInstance().config.getBrightness());
        state.put("toggleLed", MainSingleton.getInstance().config.isToggleLed());
        state.put("running", MainSingleton.getInstance().RUNNING);
        state.put("colorChooser", MainSingleton.getInstance().config.getColorChooser());
        return state;
    }

    /**
     * Resolve a user-supplied effect name to an {@link Enums.Effect} enum value.
     * Checks aliases, localized strings, and normalized enum names in order.
     *
     * @param effectName the raw effect name from the request
     * @return the resolved enum value, or {@code null} if no match is found
     */
    private Enums.Effect resolveEffect(String effectName) {
        if (effectName == null || effectName.isBlank()) {
            return null;
        }
        Enums.Effect aliasEffect = resolveEffectAlias(effectName);
        if (aliasEffect != null) {
            return aliasEffect;
        }
        Enums.Effect effect = LocalizedEnum.fromBaseStr(Enums.Effect.class, effectName);
        if (effect != null) {
            return effect;
        }
        effect = LocalizedEnum.fromStr(Enums.Effect.class, effectName);
        if (effect != null) {
            return effect;
        }
        String normalizedEffectName = effectName.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        try {
            return Enums.Effect.valueOf(normalizedEffectName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Map a normalized alias string to an {@link Enums.Effect}. Just a few example for the SLM/LLM.
     *
     * @param effectName the raw effect name to look up by alias
     * @return the resolved enum value, or {@code null} if no alias matches
     */
    private Enums.Effect resolveEffectAlias(String effectName) {
        String normalizedAlias = effectName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return switch (normalizedAlias) {
            case "ambilight", "bias", "biaslight", "illuminazionebias" -> Enums.Effect.BIAS_LIGHT;
            case "solid", "fixed", "fixedcolor" -> Enums.Effect.SOLID;
            case "rainbow", "rainboweffect" -> Enums.Effect.RAINBOW;
            default -> null;
        };
    }

    private static class SetEffectRequest {

        private Enums.Effect effect;
        private Integer brightness;
        private Boolean toggleLed;
        private Boolean running;
        private Integer colorR;
        private Integer colorG;
        private Integer colorB;
        private String errorMessage;

    }

}
