/*
  GetDeviceTool.java

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
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.network.mcp.AbstractMcpTool;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP tool that returns the detected Glow Worm device names.
 */
@Slf4j
public class GetDeviceTool extends AbstractMcpTool {

    public static final String TOOL_NAME = "getDevice";

    public GetDeviceTool(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * Return the MCP tool name.
     *
     * @return {@code "getDevice"}
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
        tool.put("description", "List Glow Worm device names currently detected by Firefly Luciferin.");
        tool.put("icon", "device");
        ObjectNode annotations = tool.putObject("annotations");
        annotations.put("readOnlyHint", true);
        ObjectNode inputSchema = tool.putObject("inputSchema");
        inputSchema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        inputSchema.put("type", "object");
        inputSchema.putObject("properties");
        inputSchema.putArray("required");
        return tool;
    }

    /**
     * Execute the tool by collecting device names from the GUI table on the FX thread.
     *
     * @param arguments unused, retained for interface compatibility
     * @return a text result containing the JSON array of detected device names
     * @throws Exception if execution on the FX thread fails
     */
    @Override
    public ObjectNode execute(JsonNode arguments) throws Exception {
        List<String> deviceNames = runOnFxThread(this::snapshotDeviceNames);
        log.debug("MCP getDevice: {}", deviceNames);
        return createTextResult(objectMapper.writeValueAsString(deviceNames));
    }

    /**
     * Collect the non-blank device names from the GUI device table.
     *
     * @return a list of device name strings currently registered in the GUI
     */
    private List<String> snapshotDeviceNames() {
        if (GuiSingleton.getInstance().deviceTableData == null) {
            return List.of();
        }
        List<String> deviceNames = new ArrayList<>();
        for (GlowWormDevice device : GuiSingleton.getInstance().deviceTableData) {
            if (device != null && device.getDeviceName() != null && !device.getDeviceName().isBlank()) {
                deviceNames.add(device.getDeviceName());
            }
        }
        return deviceNames;
    }

}
