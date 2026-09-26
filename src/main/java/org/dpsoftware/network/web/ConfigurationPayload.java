/*
  ConfigurationPayload.java

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
package org.dpsoftware.network.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Configuration fields exchanged with the web interface.
 * Applies the editable payload fields to the saved configuration and serializes it back for the web client,
 * skipping the fields excluded from the web interface.
 */
final class ConfigurationPayload {

    private static final List<String> EXCLUDED_FIELDS = List.of("hueMap", "ledMatrix");

    /**
     * Prevents instantiation.
     */
    private ConfigurationPayload() {
    }

    /**
     * Applies editable fields to a copy and regenerates the LED matrix when needed.
     *
     * @param payload     the JSON payload with the editable configuration fields
     * @param savedConfig the saved configuration to apply the payload to
     * @return the updated configuration
     * @throws IOException when the JSON payload or configuration cannot be parsed
     */
    static Configuration apply(JsonNode payload, Configuration savedConfig) throws IOException {
        ObjectNode configTree = CommonUtility.JSON_MAPPER.valueToTree(savedConfig);
        for (Map.Entry<String, JsonNode> entry : payload.properties()) {
            if (!EXCLUDED_FIELDS.contains(entry.getKey()) && !entry.getValue().isNull()) {
                configTree.set(entry.getKey(), entry.getValue());
            }
        }
        Configuration updatedConfig = CommonUtility.JSON_MAPPER.treeToValue(configTree, Configuration.class);
        if (updatedConfig.ledMatrixParamsChanged(savedConfig)) {
            updatedConfig.regenerateLedMatrix();
        }
        return updatedConfig;
    }

    /**
     * Serializes the configuration without fields excluded from the web interface.
     *
     * @param config the configuration to serialize
     * @return the JSON node with the excluded fields removed
     */
    static ObjectNode toWebConfig(Configuration config) {
        ObjectNode configNode = CommonUtility.JSON_MAPPER.valueToTree(config);
        EXCLUDED_FIELDS.forEach(configNode::remove);
        return configNode;
    }
}
