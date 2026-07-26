/*
  McpServer.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © HttpURLConnection.HTTP_ACCEPTED0 - HttpURLConnection.HTTP_ACCEPTED6  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.config.Constants;
import org.dpsoftware.network.mcp.tools.GetDeviceTool;
import org.dpsoftware.network.mcp.McpTool;
import org.dpsoftware.network.mcp.tools.SetEffectTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Model Context Protocol. Minimal MCP Streamable HTTP server backed by the JDK HTTP server.
 */
@Slf4j
public class McpServer {

    private static final String MCP_ENDPOINT = "/mcp";
    private static final String MCP_JSONRPC_KEY = "jsonrpc";
    private static final String MCP_JSONRPC_VERSION = "2.0";
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";
    private static final int MCP_DEFAULT_PORT = 33555;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, McpTool> tools = Stream.<McpTool>of(
            new GetDeviceTool(objectMapper),
            new SetEffectTool(objectMapper)
    ).collect(Collectors.toMap(McpTool::getName, Function.identity()));
    private HttpServer httpServer;

    /**
     * Start the MCP HTTP endpoint on localhost.
     */
    public void start() {
        if (httpServer != null) {
            return;
        }
        try {
            httpServer = HttpServer.create(new InetSocketAddress(Constants.MSG_SERVER_HOST, MCP_DEFAULT_PORT), 0);
            httpServer.createContext(MCP_ENDPOINT, this::handleMcpRequest);
            httpServer.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "firefly-mcp-server");
                thread.setDaemon(true);
                return thread;
            }));
            httpServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "firefly-mcp-shutdown"));
            log.info("MCP server listening on http://{}:{}{}", Constants.MSG_SERVER_HOST, MCP_DEFAULT_PORT, MCP_ENDPOINT);
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
            sendEmpty(exchange, HttpURLConnection.HTTP_NO_CONTENT);
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
                case "initialize" -> sendJson(exchange, HttpURLConnection.HTTP_OK, createInitializeResponse(id));
                case "tools/list" -> sendJson(exchange, HttpURLConnection.HTTP_OK, createToolsListResponse(id));
                case "tools/call" -> sendJson(exchange, HttpURLConnection.HTTP_OK, createToolCallResponse(id, request.path("params")));
                case "ping" -> sendJson(exchange, HttpURLConnection.HTTP_OK, createResultResponse(id, objectMapper.createObjectNode()));
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
        serverInfo.put("name", Constants.SOFTWARE_NAME);
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
        response.put(MCP_JSONRPC_KEY, MCP_JSONRPC_VERSION);
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private void sendError(HttpExchange exchange, JsonNode id, int code, String message) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put(MCP_JSONRPC_KEY, MCP_JSONRPC_VERSION);
        if (id == null || id.isNull()) {
            response.putNull("id");
        } else {
            response.set("id", id);
        }
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        sendJson(exchange, HttpURLConnection.HTTP_OK, response);
    }

    private void handleNotification(HttpExchange exchange) throws IOException {
        sendEmpty(exchange, HttpURLConnection.HTTP_ACCEPTED);
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
