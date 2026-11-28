/*
  SetSmoothingLevelToolTest.java

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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.managers.PipelineManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class SetSmoothingLevelToolTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SetSmoothingLevelTool tool = new SetSmoothingLevelTool(mapper) {
        @Override
        protected <T> T runOnFxThread(Callable<T> callable) throws Exception {
            return callable.call();
        }
    };

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "[]", "{\"level\":null}", "{\"level\":-1}", "{\"level\":7}",
            "{\"level\":2.5}", "{\"level\":true}", "{\"level\":\"3\"}", "{\"level\":4294967296}"})
    void rejectsInvalidLevelsWithoutChangingConfiguration(String input) throws Exception {
        try (var singleton = mockStatic(MainSingleton.class)) {
            JsonNode result = tool.execute(mapper.readTree(input));
            assertTrue(result.path("isError").asBoolean());
            singleton.verifyNoInteractions();
        }
    }

    @Test
    void rejectsMissingArguments() throws Exception {
        assertTrue(tool.execute(null).path("isError").asBoolean());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void appliesBuiltInPresetsWithoutStartingStoppedCapture(int level) throws Exception {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        // Start with custom settings so every preset must replace all dependent values.
        main.config.setSmoothingType(Enums.Smoothing.CUSTOM.getBaseI18n());
        main.config.setEmaAlpha(0.25f);
        main.config.setFrameInsertionTarget(30);
        main.config.setSmoothingTargetFramerate(120);
        Enums.Smoothing expected = level == 0 ? Enums.Smoothing.DISABLED
                : Enums.Smoothing.valueOf("SMOOTHING_LVL_" + level);
        try (var singleton = mockStatic(MainSingleton.class);
             var pipeline = mockStatic(PipelineManager.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            JsonNode state = stateForLevel(level);
            assertEquals(expected.getBaseI18n(), main.config.getSmoothingType());
            assertEquals(expected.getEmaAlpha(), main.config.getEmaAlpha());
            assertEquals(expected.getFrameInsertionFramerate(), main.config.getFrameInsertionTarget());
            assertEquals(60, main.config.getSmoothingTargetFramerate());
            assertEquals(level, state.path("level").asInt());
            assertEquals(main.config.getSmoothingType(), state.path("smoothingType").asText());
            assertEquals(main.config.getEmaAlpha(), (float) state.path("emaAlpha").asDouble());
            assertEquals(main.config.getFrameInsertionTarget(), state.path("frameInsertionTarget").asInt());
            assertEquals(60, state.path("smoothingTargetFramerate").asInt());
            assertFalse(state.path("restartScheduled").asBoolean());
            assertFalse(main.RUNNING);
            pipeline.verifyNoInteractions();
        }
    }

    @Test
    void restartsActiveCaptureOnlyWhenParametersChange() throws Exception {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.config.setEmaAlpha(0.35f);
        main.RUNNING = true;
        main.guiManager = mock(GuiManager.class);
        try (var singleton = mockStatic(MainSingleton.class);
             var pipeline = mockStatic(PipelineManager.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            assertTrue(stateForLevel(6).path("restartScheduled").asBoolean());
            pipeline.verify(() -> PipelineManager.restartCapture(any(Runnable.class)));
            pipeline.clearInvocations();
            assertFalse(stateForLevel(6).path("restartScheduled").asBoolean());
            pipeline.verifyNoInteractions();
        }
    }

    @Test
    void rejectsUnavailableConfiguration() {
        try (var singleton = mockStatic(MainSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(new MainSingleton());
            assertThrows(IllegalStateException.class, () -> stateForLevel(3));
        }
    }

    /**
     * Execute a tool request and decode its success payload.
     */
    private JsonNode stateForLevel(int level) throws Exception {
        JsonNode result = tool.execute(mapper.createObjectNode().put("level", level));
        assertFalse(result.path("isError").asBoolean());
        return mapper.readTree(result.path("content").get(0).path("text").asText());
    }
}
