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
package org.dpsoftware.network.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
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
            Effect to use. Accepts enum names and natural aliases.
            Common choices:
            - ambilight, bias light -> BIAS_LIGHT
            - solid, fixed color -> SOLID
            - rainbow, rainbow effect, arcobaleno -> RAINBOW
            NOTE: 
              LEDs and Lights are synonyms. If you are asked to toggle light, you can toggle LEDs.
              Do not start or stop the capture if not asked.              
              When you are asked to set an effect, set the effect, then turn on the LEDs.
              When you are asked to change the brightness, change the brightness, then turn on the LEDs.
              When you are asked to change the light color, change the color (r, g, b), then turn on the LEDs.
              To set a solid color, set the effect to SOLID and provide r, g, b values.
            """;

    public SetEffectTool(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    private static String availableEffectNames() {
        return Arrays.toString(Arrays.stream(Enums.Effect.values()).map(Enum::name).toArray(String[]::new));
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public ObjectNode getDefinition() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", TOOL_NAME);
        tool.put("description", "Set Firefly Luciferin effect, LED power state, brightness, and capture pipeline state.");
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

    @Override
    public ObjectNode execute(JsonNode arguments) throws Exception {
        SetEffectRequest request = parseRequest(arguments);
        if (request.errorMessage != null) {
            return createToolErrorResult(request.errorMessage);
        }
        ObjectNode state = runOnFxThread(() -> applySetEffect(request));
        return createTextResult(objectMapper.writeValueAsString(state));
    }

    private SetEffectRequest parseRequest(JsonNode arguments) {
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
            if (!colorR.isIntegralNumber() || colorR.asInt() < 0 || colorR.asInt() > 255
                    || !colorG.isIntegralNumber() || colorG.asInt() < 0 || colorG.asInt() > 255
                    || !colorB.isIntegralNumber() || colorB.asInt() < 0 || colorB.asInt() > 255) {
                request.errorMessage = "r, g, b must be integers between 0 and 255";
                return request;
            }
            request.colorR = colorR.asInt();
            request.colorG = colorG.asInt();
            request.colorB = colorB.asInt();
        }

        return request;
    }

    private ObjectNode applySetEffect(SetEffectRequest request) {
        if (MainSingleton.getInstance().config == null) {
            throw new IllegalStateException("Configuration is not available");
        }

        if (request.effect != null) {
            MainSingleton.getInstance().config.setEffect(request.effect.getBaseI18n());
            log.info("MCP effect set to {}", request.effect.getBaseI18n());
        }
        if (request.brightness != null) {
            MainSingleton.getInstance().config.setBrightness(request.brightness);
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
            MainSingleton.getInstance().config.setColorChooser(
                    request.colorR + "," + request.colorG + "," + request.colorB + "," + brightness);

            MainSingleton.getInstance().config.setColorChooser(
                    request.colorR + "," + request.colorG + "," + request.colorB + "," + brightness);
            log.info("MCP color set to r={} g={} b={}", request.colorR, request.colorG, request.colorB);
            CommonUtility.turnOnLEDs();

        }

        ObjectNode state = objectMapper.createObjectNode();
        state.put("effect", MainSingleton.getInstance().config.getEffect());
        state.put("brightness", MainSingleton.getInstance().config.getBrightness());
        state.put("toggleLed", MainSingleton.getInstance().config.isToggleLed());
        state.put("running", MainSingleton.getInstance().RUNNING);
        state.put("colorChooser", MainSingleton.getInstance().config.getColorChooser());
        return state;
    }

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

    private Enums.Effect resolveEffectAlias(String effectName) {
        String normalizedAlias = effectName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return switch (normalizedAlias) {
            case "ambilight", "bias", "biaslight", "illuminazionebias" -> Enums.Effect.BIAS_LIGHT;
            case "solid", "fixed", "fixedcolor", "solido", "colorefisso" -> Enums.Effect.SOLID;
            case "rainbow", "rainboweffect", "arcobaleno", "effettoarcobaleno" -> Enums.Effect.RAINBOW;
            case "slowrainbow", "superslowrainbow", "arcobalenolento" -> Enums.Effect.SUPER_SLOW_RAINBOW;
            case "chaserainbow", "rainbowchase", "arcobalenoinseguimento" -> Enums.Effect.CHASE_RAINBOW;
            case "solidrainbow", "arcobalenosolido" -> Enums.Effect.SOLID_RAINBOW;
            case "randomcolors", "coloricasuali" -> Enums.Effect.RANDOM_COLORS;
            case "rainbowcolors", "colorirainbow", "coloriarcobaleno", "coloridellarcobaleno" ->
                    Enums.Effect.RAINBOW_COLORS;
            case "pulsingrainbow", "rain1", "arcobalenopulsante" -> Enums.Effect.RAIN1;
            case "randommarquee", "marqueecasuale" -> Enums.Effect.RANDOM_MARQUEE;
            case "rainbowmarquee", "marqueearcobaleno" -> Enums.Effect.RAINBOW_MARQUEE;
            case "fire", "fuoco" -> Enums.Effect.FIRE;
            case "twinkle", "scintillio" -> Enums.Effect.TWINKLE;
            case "bpm" -> Enums.Effect.BPM;
            case "meteor", "meteora" -> Enums.Effect.METEOR;
            case "waterfall", "colorwaterfall", "cascata", "cascatacolore" -> Enums.Effect.COLOR_WATERFALL;
            case "christmas", "natale" -> Enums.Effect.CHRISTMAS;
            case "musicvumeter", "vumeter", "musicmodevumeter" -> Enums.Effect.MUSIC_MODE_VU_METER;
            case "dualvumeter", "musicdualvumeter", "musicmodevumeterdual" -> Enums.Effect.MUSIC_MODE_VU_METER_DUAL;
            case "musicbright", "musicscreencapture", "musicmodebright" -> Enums.Effect.MUSIC_MODE_BRIGHT;
            case "musicrainbow", "musicmoderainbow" -> Enums.Effect.MUSIC_MODE_RAINBOW;
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
