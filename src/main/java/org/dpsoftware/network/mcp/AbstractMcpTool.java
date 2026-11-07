/*
  AbstractMcpTool.java

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Shared helpers for MCP tools.
 */
@Slf4j
public abstract class AbstractMcpTool implements McpTool {

    private static final int FX_OPERATION_TIMEOUT_SECONDS = 3;

    protected final ObjectMapper objectMapper;

    protected AbstractMcpTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Construct a success text result node.
     *
     * @param text the plain-text payload
     * @return a JSON object with a single {@code text} entry in its content array
     */
    protected ObjectNode createTextResult(String text) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode content = result.putArray("content");
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", text);
        return result;
    }

    /**
     * Construct an error result node with the given message.
     *
     * @param message the error description text
     * @return a JSON object marked as {@code isError} containing the message
     */
    protected ObjectNode createToolErrorResult(String message) {
        ObjectNode result = createTextResult(message);
        result.put("isError", true);
        return result;
    }

    /**
     * Execute a callable on the JavaFX application thread, waiting up to 3 seconds for completion.
     *
     * @param <T>      the return type of the callable
     * @param callable the code to run on the FX thread
     * @return the result returned by the callable
     * @throws Exception if the callable throws or if the operation times out
     */
    protected <T> T runOnFxThread(Callable<T> callable) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                future.complete(callable.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        try {
            return future.get(FX_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("MCP JavaFX operation timed out");
            throw e;
        }
    }

}
