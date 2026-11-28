/*
  SetAspectRatioToolTest.java

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
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.trayicon.TrayIconHeadless;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SetAspectRatioToolTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SetAspectRatioTool tool = new SetAspectRatioTool(mapper) {
        @Override
        protected <T> T runOnFxThread(Callable<T> callable) throws Exception {
            return callable.call();
        }
    };

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "[]", "{\"aspectRatio\":null}", "{\"aspectRatio\":false}",
            "{\"aspectRatio\":16}", "{\"aspectRatio\":\"16:9\"}", "{\"aspectRatio\":\"\"}"})
    void rejectsInvalidModesBeforeAccessingConfiguration(String input) throws Exception {
        try (var singleton = mockStatic(MainSingleton.class)) {
            assertTrue(tool.execute(mapper.readTree(input)).path("isError").asBoolean());
            singleton.verifyNoInteractions();
        }
    }

    @Test
    void rejectsMissingArguments() throws Exception {
        assertTrue(tool.execute(null).path("isError").asBoolean());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AUTO", "FULLSCREEN", "LETTERBOX", "PILLARBOX", " letterbox "})
    void appliesSharedAspectRatioActionWithoutChangingCaptureOrPower(String input) throws Exception {
        var previousMatrix = GStreamerGrabber.ledMatrix;
        try {
            for (boolean running : new boolean[]{false, true}) {
                MainSingleton main = configuredMain();
                main.RUNNING = running;
                String mode = input.trim().toUpperCase(Locale.ROOT);
                boolean automatic = "AUTO".equals(mode);
                main.config.setAutoDetectBlackBars(!automatic);
                // Enabling AUTO should retain the detected matrix until the next detection.
                main.config.setDefaultLedMatrix(Enums.AspectRatio.LETTERBOX.getBaseI18n());
                GStreamerGrabber.ledMatrix = main.config.getLedMatrixInUse(main.config.getDefaultLedMatrix());
                var detectedMatrix = GStreamerGrabber.ledMatrix;
                String effect = main.config.getEffect();
                boolean power = main.config.isToggleLed();
                try (var singleton = mockStatic(MainSingleton.class)) {
                    singleton.when(MainSingleton::getInstance).thenReturn(main);
                    JsonNode state = stateForMode(input);
                    assertEquals(mode, state.path("aspectRatio").asText());
                    assertEquals(automatic, state.path("autoDetectBlackBars").asBoolean());
                    assertEquals(automatic, main.config.isAutoDetectBlackBars());
                    if (automatic) {
                        assertSame(detectedMatrix, GStreamerGrabber.ledMatrix);
                    } else {
                        assertEquals(Enums.AspectRatio.valueOf(mode).getBaseI18n(), main.config.getDefaultLedMatrix());
                        assertSame(main.config.getLedMatrixInUse(main.config.getDefaultLedMatrix()), GStreamerGrabber.ledMatrix);
                    }
                    assertEquals(main.config.getDefaultLedMatrix(), state.path("defaultLedMatrix").asText());
                    assertEquals(running, main.RUNNING);
                    assertEquals(effect, main.config.getEffect());
                    assertEquals(power, main.config.isToggleLed());
                    verifyNoInteractions(main.guiManager);
                }
            }
        } finally {
            GStreamerGrabber.ledMatrix = previousMatrix;
        }
    }

    @Test
    void rejectsUnavailableMatrixWithoutChangingSettings() {
        MainSingleton main = configuredMain();
        main.config.getLedMatrix().remove(Enums.AspectRatio.PILLARBOX.getBaseI18n());
        try (var singleton = mockStatic(MainSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            assertThrows(IllegalStateException.class, () -> stateForMode("PILLARBOX"));
            assertTrue(main.config.isAutoDetectBlackBars());
            assertEquals(Enums.AspectRatio.FULLSCREEN.getBaseI18n(), main.config.getDefaultLedMatrix());
        }
    }

    /**
     * Create a configuration with distinct matrices and the real headless tray action.
     */
    private MainSingleton configuredMain() {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        var matrices = new LinkedHashMap<String, LinkedHashMap<Integer, LEDCoordinate>>();
        for (Enums.AspectRatio mode : Enums.AspectRatio.values()) {
            matrices.put(mode.getBaseI18n(), new LinkedHashMap<>());
        }
        main.config.setLedMatrix(matrices);
        main.config.setDefaultLedMatrix(Enums.AspectRatio.FULLSCREEN.getBaseI18n());
        main.guiManager = mock(GuiManager.class);
        main.guiManager.trayIconManager = new TrayIconHeadless();
        return main;
    }

    /**
     * Execute a selection and decode its success payload.
     */
    private JsonNode stateForMode(String mode) throws Exception {
        JsonNode result = tool.execute(mapper.createObjectNode().put("aspectRatio", mode));
        assertFalse(result.path("isError").asBoolean());
        return mapper.readTree(result.path("content").get(0).path("text").asText());
    }
}
