/*
  AutomaticCaptureConfigurationTest.java

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
package org.dpsoftware.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutomaticCaptureConfigurationTest {
    @Test
    void autoIsTheDefaultAndProbeStateIsNotPersisted() throws Exception {
        Configuration config = new Configuration();
        assertEquals(Configuration.CaptureMethod.AUTO.name(), config.getCaptureMethod());
        config.setAutomaticCapturePending(true);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String yaml = mapper.writeValueAsString(config);
        assertEquals("AUTO", mapper.readTree(yaml).path("captureMethod").asText());
        assertFalse(mapper.readTree(yaml).has("automaticCapturePending"));
    }
}
