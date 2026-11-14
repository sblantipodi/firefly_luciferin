/*
  SetProfileToolTest.java

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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SetProfileTool}.
 * Tests tool definition schema and input parsing.
 */
class SetProfileToolTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SetProfileTool tool = new SetProfileTool(mapper);

    @Test
    void getName_returnsSetProfile() {
        assertEquals("setProfile", tool.getName());
    }

    @Test
    void getDefinition_hasRequiredFields() {
        ObjectNode def = tool.getDefinition();
        assertEquals("setProfile", def.get("name").asText());
        assertTrue(def.has("description"));
        assertTrue(def.has("inputSchema"));
    }

    @Test
    void getDefinition_hasProfileNameProperty() {
        JsonNode props = tool.getDefinition()
                .path("inputSchema").path("properties");
        assertTrue(props.has("profileName"));
        assertEquals("string", props.path("profileName").path("type").asText());
    }

    @Test
    void getDefinition_noRequiredProperties() {
        JsonNode required = tool.getDefinition()
                .path("inputSchema").path("required");
        assertTrue(required.isArray());
        assertEquals(0, required.size(), "profileName is optional");
    }

    @Test
    void getDefinition_hasIdempotentHint() {
        JsonNode annotations = tool.getDefinition().path("annotations");
        assertTrue(annotations.has("idempotentHint"));
        assertTrue(annotations.path("idempotentHint").asBoolean());
    }

    @Test
    void getDefinition_descriptionContainsKeyTerms() {
        String desc = tool.getDefinition().get("description").asText();
        assertTrue(desc.toLowerCase().contains("profile"));
        assertTrue(desc.toLowerCase().contains("list"));
    }

}
