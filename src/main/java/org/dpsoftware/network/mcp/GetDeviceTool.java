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
package org.dpsoftware.network.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.elements.GlowWormDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP tool that returns the detected Glow Worm device names.
 */
public class GetDeviceTool extends AbstractMcpTool {

    public static final String TOOL_NAME = "getDevice";

    public GetDeviceTool(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public ObjectNode getDefinition() {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", TOOL_NAME);
        tool.put("description", "List Glow Worm device names currently detected by Firefly Luciferin.");
        ObjectNode inputSchema = tool.putObject("inputSchema");
        inputSchema.put("type", "object");
        inputSchema.putObject("properties");
        inputSchema.putArray("required");
        return tool;
    }

    @Override
    public ObjectNode execute(JsonNode arguments) throws Exception {
        return createTextResult(objectMapper.writeValueAsString(runOnFxThread(this::snapshotDeviceNames)));
    }

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
