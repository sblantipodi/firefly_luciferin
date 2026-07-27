/*
  GetInfoTool.java

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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.network.NetworkSingleton;
import org.dpsoftware.network.mcp.AbstractMcpTool;

/**
 * MCP tool that returns runtime statistics: LED layout, gamma, FPS, and pipeline state.
 */
public class GetInfoTool extends AbstractMcpTool {

    public static final String TOOL_NAME = "getInfo";

    public GetInfoTool(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * Return the MCP tool name.
     *
     * @return {@code "getInfo"}
     */
    @Override
    public String getName() {
        return TOOL_NAME;
    }

    /**
     * Build the MCP tool definition JSON with an empty input schema.
     *
     * @return an ObjectNode describing the tool name, description, and empty input schema
     */
    @Override
    public ObjectNode getDefinition() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", TOOL_NAME);
        tool.put("description", "Return runtime statistics: LED layout, adaptive gamma, FPS, and capture pipeline state.");
        ObjectNode annotations = tool.putObject("annotations");
        annotations.put("readOnlyHint", true);
        ObjectNode inputSchema = tool.putObject("inputSchema");
        inputSchema.put("type", "object");
        inputSchema.putObject("properties");
        inputSchema.putArray("required");
        return tool;
    }

    /**
     * Execute the tool by snapshotting the current runtime statistics on the FX thread.
     *
     * @param arguments unused, retained for interface compatibility
     * @return a text result containing the JSON representation of runtime statistics
     * @throws Exception if execution on the FX thread fails
     */
    @Override
    public ObjectNode execute(JsonNode arguments) throws Exception {
        ObjectNode info = runOnFxThread(this::snapshotInfo);
        return createTextResult(objectMapper.writeValueAsString(info));
    }

    /**
     * Collect the current runtime statistics into a structured JSON object.
     *
     * @return an ObjectNode with led, gamma, fps, and running sections
     */
    private ObjectNode snapshotInfo() {
        ObjectNode result = objectMapper.createObjectNode();

        // LED layout section
        ObjectNode led = result.putObject("led");
        led.put("count", NetworkSingleton.getInstance().totalLedNum);
        led.put("groupBy", MainSingleton.getInstance().config.getGroupBy());

        // Gamma section
        ObjectNode gamma = result.putObject("gamma");
        double gammaValue = Double.longBitsToDouble(ImageProcessor.currentGammaAtomic.get());
        gamma.put("adaptive", Math.round(gammaValue * 1000.0) / 1000.0);

        // FPS section
        ObjectNode fps = result.putObject("fps");
        fps.put("producer", MainSingleton.getInstance().FPS_PRODUCER);
        fps.put("consumer", MainSingleton.getInstance().FPS_GW_CONSUMER);

        // Pipeline state
        result.put("running", MainSingleton.getInstance().RUNNING);

        return result;
    }

}
