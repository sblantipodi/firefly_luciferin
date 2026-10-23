/*
  SetEffectToolTest.java

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SetEffectTool}.
 * Tests tool definition, input parsing, and validation logic.
 */
class SetEffectToolTest {

    private SetEffectTool tool;
    private ObjectMapper mapper;

    private static java.util.Map<String, Object> invokeParseRequest(SetEffectTool tool, JsonNode args) {
        try {
            var method = SetEffectTool.class.getDeclaredMethod("parseRequest", JsonNode.class);
            method.setAccessible(true);
            Object requestObj = method.invoke(tool, args);

            // Extract fields from the private SetEffectRequest class via reflection
            var cls = requestObj.getClass();
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            for (var field : cls.getDeclaredFields()) {
                field.setAccessible(true);
                map.put(field.getName(), field.get(requestObj));
            }
            return map;
        } catch (Exception e) {
            fail("Failed to invoke parseRequest: " + e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        tool = new SetEffectTool(mapper);
    }

    @Test
    void getName_returnsSetEffect() {
        assertEquals("setEffect", tool.getName());
    }

    @Test
    void getDefinition_hasRequiredFields() {
        ObjectNode def = tool.getDefinition();
        assertEquals("setEffect", def.get("name").asText());
        assertTrue(def.has("description"));
        assertTrue(def.has("inputSchema"));

        JsonNode props = def.path("inputSchema").path("properties");
        assertTrue(props.has("effect"));
        assertTrue(props.has("toggleLed"));
        assertTrue(props.has("brightness"));
        assertTrue(props.has("running"));
        assertTrue(props.has("r"));
        assertTrue(props.has("g"));
        assertTrue(props.has("b"));
    }

    @Test
    void getDefinition_brightnessHasRange() {
        JsonNode brightness = tool.getDefinition()
                .path("inputSchema").path("properties").path("brightness");
        assertEquals(0, brightness.path("minimum").asInt());
        assertEquals(255, brightness.path("maximum").asInt());
    }

    // --- parseRequest validation via reflection (private method) ---

    @Test
    void getDefinition_colorComponentsHaveRange() {
        JsonNode props = tool.getDefinition()
                .path("inputSchema").path("properties");
        for (String comp : new String[]{"r", "g", "b"}) {
            assertEquals(0, props.path(comp).path("minimum").asInt());
            assertEquals(255, props.path(comp).path("maximum").asInt());
        }
    }

    @Test
    void parseRequest_nullArgumentsReturnsEmptyRequest() {
        var request = invokeParseRequest(tool, null);
        assertNull(request.get("errorMessage"));
    }

    @Test
    void parseRequest_validEffect() {
        ObjectNode args = mapper.createObjectNode();
        args.put("effect", "BIAS_LIGHT");
        var request = invokeParseRequest(tool, args);
        assertNull(request.get("errorMessage"));
        assertEquals(org.dpsoftware.config.Enums.Effect.BIAS_LIGHT, request.get("effect"));
    }

    @Test
    void parseRequest_effectAliasAmbilight() {
        ObjectNode args = mapper.createObjectNode();
        args.put("effect", "ambilight");
        var request = invokeParseRequest(tool, args);
        assertNull(request.get("errorMessage"));
        assertEquals(org.dpsoftware.config.Enums.Effect.BIAS_LIGHT, request.get("effect"));
    }

    @Test
    void parseRequest_effectAliasRainbow() {
        ObjectNode args = mapper.createObjectNode();
        args.put("effect", "rainbow");
        var request = invokeParseRequest(tool, args);
        assertNull(request.get("errorMessage"));
        assertEquals(org.dpsoftware.config.Enums.Effect.RAINBOW, request.get("effect"));
    }

    @Test
    void parseRequest_effectAliasSolid() {
        ObjectNode args = mapper.createObjectNode();
        args.put("effect", "solid");
        var request = invokeParseRequest(tool, args);
        assertNull(request.get("errorMessage"));
        assertEquals(org.dpsoftware.config.Enums.Effect.SOLID, request.get("effect"));
    }

    @Test
    void parseRequest_unknownEffectReturnsError() {
        ObjectNode args = mapper.createObjectNode();
        args.put("effect", "NONEXISTENT_EFFECT_XYZ");
        var request = invokeParseRequest(tool, args);
        assertNotNull(request.get("errorMessage"));
    }

    @Test
    void parseRequest_brightnessOutOfRange() {
        ObjectNode args = mapper.createObjectNode();
        args.put("brightness", 300);
        var request = invokeParseRequest(tool, args);
        assertNotNull(request.get("errorMessage"));
    }

    @Test
    void parseRequest_brightnessNegative() {
        ObjectNode args = mapper.createObjectNode();
        args.put("brightness", -1);
        var request = invokeParseRequest(tool, args);
        assertNotNull(request.get("errorMessage"));
    }

    @Test
    void parseRequest_toggleLedMustBeBoolean() {
        ObjectNode args = mapper.createObjectNode();
        args.putPOJO("toggleLed", "yes");
        var request = invokeParseRequest(tool, args);
        assertNotNull(request.get("errorMessage"));
    }

    @Test
    void parseRequest_partialColorReturnsError() {
        ObjectNode args = mapper.createObjectNode();
        args.put("r", 100);
        // missing g and b
        var request = invokeParseRequest(tool, args);
        assertNotNull(request.get("errorMessage"));
    }

    // --- Reflection helper to call private parseRequest ---

    @Test
    void parseRequest_validColor() {
        ObjectNode args = mapper.createObjectNode();
        args.put("r", 255);
        args.put("g", 128);
        args.put("b", 64);
        var request = invokeParseRequest(tool, args);
        assertNull(request.get("errorMessage"));
        assertEquals(255, request.get("colorR"));
        assertEquals(128, request.get("colorG"));
        assertEquals(64, request.get("colorB"));
    }
}
