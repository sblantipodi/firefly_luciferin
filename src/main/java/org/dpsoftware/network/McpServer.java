/*
  McpServer.java

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
package org.dpsoftware.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.network.mcp.GetDeviceTool;
import org.dpsoftware.network.mcp.McpTool;
import org.dpsoftware.network.mcp.SetEffectTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Model Context Protocol. Minimal MCP Streamable HTTP server backed by the JDK HTTP server.
 */
@Slf4j
public class McpServer {

    private static final String MCP_ENDPOINT = "/mcp";
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";
    private static final int DEFAULT_PORT = 33555;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, McpTool> tools = List.<McpTool>of(
            new GetDeviceTool(objectMapper),
            new SetEffectTool(objectMapper)
    ).stream().collect(Collectors.toMap(McpTool::getName, Function.identity()));
    private HttpServer httpServer;

    /**
     * Start the MCP HTTP endpoint on localhost.
     */
    public void start() {
        if (httpServer != null) {
            return;
        }
        try {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", DEFAULT_PORT), 0);
            httpServer.createContext(MCP_ENDPOINT, this::handleMcpRequest);
            httpServer.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "firefly-mcp-server");
                thread.setDaemon(true);
                return thread;
            }));
            httpServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "firefly-mcp-shutdown"));
            log.info("MCP server listening on http://127.0.0.1:{}{}", DEFAULT_PORT, MCP_ENDPOINT);
        } catch (IOException e) {
            log.warn("Unable to start MCP server: {}", e.getMessage());
        }
    }

    /**
     * Stop the MCP endpoint.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    private void handleMcpRequest(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendEmpty(exchange, 204);
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, null, -32600, "Only POST is supported");
            return;
        }

        JsonNode request;
        try (InputStream body = exchange.getRequestBody()) {
            request = objectMapper.readTree(body);
        } catch (Exception e) {
            sendError(exchange, null, -32700, "Parse error");
            return;
        }

        JsonNode id = request.get("id");
        String method = request.path("method").asText();
        if (id == null || id.isNull()) {
            handleNotification(exchange);
            return;
        }

        try {
            switch (method) {
                case "initialize" -> sendJson(exchange, 200, createInitializeResponse(id));
                case "tools/list" -> sendJson(exchange, 200, createToolsListResponse(id));
                case "tools/call" -> sendJson(exchange, 200, createToolCallResponse(id, request.path("params")));
                case "ping" -> sendJson(exchange, 200, createResultResponse(id, objectMapper.createObjectNode()));
                default -> sendError(exchange, id, -32601, "Method not found");
            }
        } catch (Exception e) {
            log.warn("MCP request failed: {}", e.getMessage());
            sendError(exchange, id, -32603, "Internal error");
        }
    }

    private ObjectNode createInitializeResponse(JsonNode id) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", MCP_PROTOCOL_VERSION);
        ObjectNode capabilities = result.putObject("capabilities");
        capabilities.putObject("tools").put("listChanged", false);
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", "firefly-luciferin");
        serverInfo.put("version", "1.0.0");
        return createResultResponse(id, result);
    }

    private ObjectNode createToolsListResponse(JsonNode id) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode toolList = result.putArray("tools");
        tools.values().forEach(tool -> toolList.add(tool.getDefinition()));
        return createResultResponse(id, result);
    }

    private ObjectNode createToolCallResponse(JsonNode id, JsonNode params) throws Exception {
        String toolName = params.path("name").asText();
        McpTool tool = tools.get(toolName);
        if (tool == null) {
            return createResultResponse(id, createToolErrorResponse("Unknown tool: " + toolName));
        }
        return createResultResponse(id, tool.execute(params.path("arguments")));
    }

    private ObjectNode createToolErrorResponse(String message) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("isError", true);
        ArrayNode content = result.putArray("content");
        ObjectNode textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", message);
        return result;
    }

    private ObjectNode createResultResponse(JsonNode id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private void sendError(HttpExchange exchange, JsonNode id, int code, String message) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id == null || id.isNull()) {
            response.putNull("id");
        } else {
            response.set("id", id);
        }
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        sendJson(exchange, 200, response);
    }

    private void handleNotification(HttpExchange exchange) throws IOException {
        sendEmpty(exchange, 202);
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, MCP-Protocol-Version");
    }

    private void sendJson(HttpExchange exchange, int statusCode, JsonNode response) throws IOException {
        byte[] responseBytes = objectMapper.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    private void sendEmpty(HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }

}
