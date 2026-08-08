/*
  GetDeviceToolTest.java

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

  You should have received a copy of the GNU Lesser General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware.network.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GetDeviceTool}.
 * Tests tool definition schema and annotation structure.
 */
class GetDeviceToolTest {

    private GetDeviceTool tool;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        tool = new GetDeviceTool(mapper);
    }

    @Test
    void getName_returnsGetDevice() {
        assertEquals("getDevice", tool.getName());
    }

    @Test
    void getDefinition_hasRequiredFields() {
        ObjectNode def = tool.getDefinition();
        assertEquals("getDevice", def.get("name").asText());
        assertTrue(def.has("description"));
        assertTrue(def.has("inputSchema"));
    }

    @Test
    void getDefinition_emptyInputSchema() {
        JsonNode props = tool.getDefinition()
                .path("inputSchema").path("properties");
        assertTrue(props.isObject());
        assertEquals(0, props.size(), "getDevice takes no input parameters");
    }

    @Test
    void getDefinition_noRequiredProperties() {
        JsonNode required = tool.getDefinition()
                .path("inputSchema").path("required");
        assertTrue(required.isArray());
        assertEquals(0, required.size());
    }

    @Test
    void getDefinition_hasReadOnlyHint() {
        JsonNode annotations = tool.getDefinition().path("annotations");
        assertTrue(annotations.has("readOnlyHint"));
        assertTrue(annotations.path("readOnlyHint").asBoolean());
    }

    @Test
    void getDefinition_descriptionContainsKeyTerms() {
        String desc = tool.getDefinition().get("description").asText();
        assertTrue(desc.toLowerCase().contains("device"));
        assertTrue(desc.toLowerCase().contains("glow worm") || desc.toLowerCase().contains("glowworm"));
    }

    @Test
    void getDefinition_inputSchemaTypeIsObject() {
        JsonNode inputSchema = tool.getDefinition().path("inputSchema");
        assertEquals("object", inputSchema.path("type").asText());
    }
}
