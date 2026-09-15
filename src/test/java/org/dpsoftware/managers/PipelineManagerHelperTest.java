/*
  PipelineManagerHelperTest.java

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
package org.dpsoftware.managers;

import org.dpsoftware.config.Constants;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the static helper methods in {@link PipelineManager}:
 * {@code getPipeline()}, {@code getCap()}, {@code getBo()}.
 * <p>
 * These methods return a custom value if the corresponding constant
 * (populated from environment variables at class-load time) is non-null,
 * otherwise they return the default passed as argument.
 */
class PipelineManagerHelperTest {

    @Test
    void getPipeline_returnsDefaultWhenCustomIsNull() {
        if (Constants.CUSTOM_GSTREAMER_PIPELINE == null) {
            String defaultPipeline = "my-pipeline";
            assertEquals(defaultPipeline, PipelineManager.getPipeline(defaultPipeline));
        } else {
            // Custom is set in this environment — verify it takes precedence
            assertEquals(Constants.CUSTOM_GSTREAMER_PIPELINE,
                    PipelineManager.getPipeline("should-be-ignored"));
        }
    }

    @Test
    void getCap_returnsDefaultWhenCustomIsNull() {
        if (Constants.CUSTOM_GSTREAMER_CAPS == null) {
            String defaultCap = "video/x-raw";
            assertEquals(defaultCap, PipelineManager.getCap(defaultCap));
        } else {
            assertEquals(Constants.CUSTOM_GSTREAMER_CAPS,
                    PipelineManager.getCap("should-be-ignored"));
        }
    }

    @Test
    void getBo_returnsDefaultWhenCustomIsNull() {
        if (Constants.CUSTOM_GSTREAMER_BO == null) {
            String defaultBo = "my-bo";
            assertEquals(defaultBo, PipelineManager.getBo(defaultBo));
        } else {
            assertEquals(Constants.CUSTOM_GSTREAMER_BO,
                    PipelineManager.getBo("should-be-ignored"));
        }
    }

    @Test
    void getPipeline_consistentWithConstant() {
        // Result should always match: custom if non-null, else the default passed in
        String defaultVal = "default-pipeline";
        String result = PipelineManager.getPipeline(defaultVal);
        assertEquals(Objects.requireNonNullElse(Constants.CUSTOM_GSTREAMER_PIPELINE, defaultVal), result);
    }

    @Test
    void getCap_consistentWithConstant() {
        String defaultVal = "default-cap";
        String result = PipelineManager.getCap(defaultVal);
        assertEquals(Objects.requireNonNullElse(Constants.CUSTOM_GSTREAMER_CAPS, defaultVal), result);
    }

    @Test
    void getBo_consistentWithConstant() {
        String defaultVal = "default-bo";
        String result = PipelineManager.getBo(defaultVal);
        assertEquals(Objects.requireNonNullElse(Constants.CUSTOM_GSTREAMER_BO, defaultVal), result);
    }
}
