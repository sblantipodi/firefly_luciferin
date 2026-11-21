/*
  AbstractMcpToolHelperTest.java

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
package org.dpsoftware.network.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the helper methods in {@link AbstractMcpTool}.
 * Uses SetEffectTool as a concrete subclass to access protected helpers.
 */
class AbstractMcpToolHelperTest {

    private TestableMcpTool tool;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        tool = new TestableMcpTool(mapper);
    }

    @Test
    void createTextResult_hasContentArray() {
        ObjectNode result = tool.testCreateTextResult("hello world");

        assertTrue(result.has("content"));
        ArrayNode content = (ArrayNode) result.get("content");
        assertEquals(1, content.size());
    }

    @Test
    void createTextResult_hasTypeText() {
        ObjectNode result = tool.testCreateTextResult("test message");

        ArrayNode content = (ArrayNode) result.get("content");
        assertEquals("text", content.get(0).get("type").asText());
    }

    @Test
    void createTextResult_containsPayload() {
        ObjectNode result = tool.testCreateTextResult("payload data");

        ArrayNode content = (ArrayNode) result.get("content");
        assertEquals("payload data", content.get(0).get("text").asText());
    }

    @Test
    void createTextResult_emptyString() {
        ObjectNode result = tool.testCreateTextResult("");

        ArrayNode content = (ArrayNode) result.get("content");
        assertEquals("", content.get(0).get("text").asText());
    }

    @Test
    void createToolErrorResult_markedAsError() {
        ObjectNode result = tool.testCreateToolErrorResult("something went wrong");

        assertTrue(result.has("isError"));
        assertTrue(result.get("isError").asBoolean());
    }

    @Test
    void createToolErrorResult_containsErrorMessage() {
        ObjectNode result = tool.testCreateToolErrorResult("error details");

        ArrayNode content = (ArrayNode) result.get("content");
        assertEquals("error details", content.get(0).get("text").asText());
    }

    @Test
    void createToolErrorResult_hasTypeText() {
        ObjectNode result = tool.testCreateToolErrorResult("fail");

        ArrayNode content = (ArrayNode) result.get("content");
        assertEquals("text", content.get(0).get("type").asText());
    }

    /**
     * Concrete subclass that exposes protected helpers for testing.
     */
    private static class TestableMcpTool extends AbstractMcpTool {

        TestableMcpTool(ObjectMapper mapper) {
            super(mapper);
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public ObjectNode getDefinition() {
            return objectMapper.createObjectNode();
        }

        @Override
        public ObjectNode execute(JsonNode arguments) {
            return objectMapper.createObjectNode();
        }

        ObjectNode testCreateTextResult(String text) {
            return createTextResult(text);
        }

        ObjectNode testCreateToolErrorResult(String message) {
            return createToolErrorResult(message);
        }
    }
}
