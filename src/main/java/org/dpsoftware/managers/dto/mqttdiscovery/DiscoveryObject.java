/*
  DiscoveryObject.java

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
package org.dpsoftware.managers.dto.mqttdiscovery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;

/**
 * Interface used for entities used in the MQTT discovery process
 */
public interface DiscoveryObject {

    /**
     * Topic used to create/destroy the MQTT entity
     *
     * @return String
     */
    @JsonIgnore
    String getDiscoveryTopic();

    /**
     * Utility method to create a base discovery string for Glow Worm. This string changes if the mqtt topic changes.
     * In this way you can add multiple entities to the MQTT discovery process that controls multiple devices with the same mqtt topic.
     *
     * @return base path for the discovery string
     */
    @JsonIgnore
    default String getBaseGWDiscoveryTopic() {
        String discoveryTopicBasePath = Constants.MQTT_DISCOVERY_TOPIC_BASE_PATH;
        if (!MainSingleton.getInstance().config.getMqttTopic().equals(Constants.MQTT_BASE_TOPIC)) {
            discoveryTopicBasePath += "_" + MainSingleton.getInstance().config.getMqttTopic();
        }
        return discoveryTopicBasePath;
    }

    /**
     * Utility method to create a base discovery string for Luciferin. This string changes if the mqtt topic changes.
     * In this way you can add multiple entities to the MQTT discovery process that controls multiple devices with the same mqtt topic.
     *
     * @return base path for the discovery string
     */
    @JsonIgnore
    default String getBaseFireflyDiscoveryTopic() {
        String discoveryTopicBasePath = Constants.MQTT_FIREFLY_BASE_TOPIC;
        if (!MainSingleton.getInstance().config.getMqttTopic().equals(Constants.MQTT_BASE_TOPIC)) {
            discoveryTopicBasePath += "_" + MainSingleton.getInstance().config.getMqttTopic();
        }
        return discoveryTopicBasePath;
    }

    /**
     * Generates a unique name based on the mqtt topic in use.
     * In this way you can add multiple entities to the MQTT discovery process that controls multiple devices with the same mqtt topic.
     *
     * @param nameBaseStr base name
     * @return unique name
     */
    @JsonIgnore
    default String generateUniqueName(String nameBaseStr) {
        if (!MainSingleton.getInstance().config.getMqttTopic().equals(Constants.MQTT_BASE_TOPIC)) {
            nameBaseStr += "_" + MainSingleton.getInstance().config.getMqttTopic();
        }
        return nameBaseStr;
    }

    /**
     * JSON String containing all the initializers for the MQTT entity that will be discovered/created
     *
     * @return String
     */
    @JsonIgnore
    String getCreateEntityStr();

    /**
     * JSON String used to destroy the entity via the MQTT discovery process
     *
     * @return String
     */
    @JsonIgnore
    String getDestroyEntityStr();

}