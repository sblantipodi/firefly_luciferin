/*
  GetInfoToolTest.java

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
 * Unit tests for {@link GetInfoTool}.
 * Tests tool definition schema and annotation structure.
 */
class GetInfoToolTest {

    private GetInfoTool tool;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        tool = new GetInfoTool(mapper);
    }

    @Test
    void getName_returnsGetInfo() {
        assertEquals("getInfo", tool.getName());
    }

    @Test
    void getDefinition_hasRequiredFields() {
        ObjectNode def = tool.getDefinition();
        assertEquals("getInfo", def.get("name").asText());
        assertTrue(def.has("description"));
        assertTrue(def.has("inputSchema"));
    }

    @Test
    void getDefinition_emptyInputSchema() {
        JsonNode props = tool.getDefinition()
                .path("inputSchema").path("properties");
        assertTrue(props.isObject());
        assertEquals(0, props.size(), "getInfo takes no input parameters");
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
        assertTrue(desc.toLowerCase().contains("led"));
        assertTrue(desc.toLowerCase().contains("gamma") || desc.toLowerCase().contains("fps"));
    }

    @Test
    void getDefinition_inputSchemaTypeIsObject() {
        JsonNode inputSchema = tool.getDefinition().path("inputSchema");
        assertEquals("object", inputSchema.path("type").asText());
    }
}
