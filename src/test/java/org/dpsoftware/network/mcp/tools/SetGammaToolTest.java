/*
  SetGammaToolTest.java

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SetGammaToolTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SetGammaTool tool = new SetGammaTool(mapper) {
        @Override
        protected <T> T runOnFxThread(Callable<T> callable) throws Exception {
            return callable.call();
        }
    };

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "[]", "{\"gamma\":null}", "{\"gamma\":0}", "{\"gamma\":-1}",
            "{\"gamma\":11}", "{\"gamma\":2.3}", "{\"gamma\":\"2.2\"}", "{\"gamma\":true}",
            "{\"automatic\":null}", "{\"automatic\":\"true\"}", "{\"level\":null}", "{\"level\":3}",
            "{\"level\":\"invalid\"}", "{\"gamma\":2.4,\"automatic\":true,\"level\":\"invalid\"}"})
    void rejectsInvalidSettingsBeforeChangingConfiguration(String input) throws Exception {
        try (var singleton = mockStatic(MainSingleton.class)) {
            assertTrue(tool.execute(mapper.readTree(input)).path("isError").asBoolean());
            singleton.verifyNoInteractions();
        }
    }

    @Test
    void rejectsMissingAndNonFiniteArguments() throws Exception {
        assertTrue(tool.execute(null).path("isError").asBoolean());
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertTrue(tool.execute(mapper.createObjectNode().put("gamma", value)).path("isError").asBoolean());
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.2, 2.4, 2.6, 2.8, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0})
    void manualGammaDisablesAutomaticGammaAndPreservesAdaptiveLevel(double gamma) throws Exception {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.config.setEnableAutomaticGamma(true);
        main.config.setGammaLevel(Enums.GammaLevel.HIGH.getBaseI18n());
        try (var singleton = mockStatic(MainSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            JsonNode state = stateForInput(mapper.createObjectNode().put("gamma", gamma));
            assertEquals(gamma, main.config.getGamma());
            assertEquals(gamma, state.path("gamma").asDouble());
            assertFalse(main.config.isEnableAutomaticGamma());
            assertFalse(state.path("automatic").asBoolean());
            assertEquals("HIGH", state.path("level").asText());
            assertEquals(Enums.GammaLevel.HIGH.getBaseI18n(), main.config.getGammaLevel());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOW", "MEDIUM", "HIGH", " high "})
    void adaptiveLevelEnablesAutomaticGammaAndKeepsBaseline(String level) throws Exception {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.config.setGamma(2.4);
        main.config.setEnableAutomaticGamma(false);
        try (var singleton = mockStatic(MainSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            JsonNode state = stateForInput(mapper.createObjectNode().put("level", level));
            assertTrue(main.config.isEnableAutomaticGamma());
            assertTrue(state.path("automatic").asBoolean());
            assertEquals(level.trim().toUpperCase(java.util.Locale.ROOT), state.path("level").asText());
            assertEquals(2.4, main.config.getGamma());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void explicitAutomaticModeTakesPrecedenceWithoutChangingCaptureOrPower(boolean automatic) throws Exception {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.guiManager = mock(GuiManager.class);
        main.RUNNING = true;
        String effect = main.config.getEffect();
        boolean power = main.config.isToggleLed();
        try (var singleton = mockStatic(MainSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            JsonNode state = stateForInput(mapper.createObjectNode().put("gamma", 2.6)
                    .put("automatic", automatic).put("level", "MEDIUM"));
            assertEquals(automatic, main.config.isEnableAutomaticGamma());
            assertEquals(automatic, state.path("automatic").asBoolean());
            assertEquals(2.6, main.config.getGamma());
            assertEquals("MEDIUM", state.path("level").asText());
            assertTrue(main.RUNNING);
            assertEquals(effect, main.config.getEffect());
            assertEquals(power, main.config.isToggleLed());
            verifyNoInteractions(main.guiManager);
        }
    }

    @Test
    void togglesAutomaticGammaWithoutChangingOtherSettings() throws Exception {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.config.setGamma(2.4);
        main.config.setGammaLevel(Enums.GammaLevel.HIGH.getBaseI18n());
        try (var singleton = mockStatic(MainSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            for (boolean automatic : new boolean[]{false, true}) {
                JsonNode state = stateForInput(mapper.createObjectNode().put("automatic", automatic));
                assertEquals(automatic, main.config.isEnableAutomaticGamma());
                assertEquals(automatic, state.path("automatic").asBoolean());
                assertEquals(2.4, main.config.getGamma());
                assertEquals("HIGH", state.path("level").asText());
                assertFalse(main.RUNNING);
            }
        }
    }

    /**
     * Execute a settings request and decode its success payload.
     */
    private JsonNode stateForInput(JsonNode input) throws Exception {
        JsonNode result = tool.execute(input);
        assertFalse(result.path("isError").asBoolean());
        return mapper.readTree(result.path("content").get(0).path("text").asText());
    }
}
