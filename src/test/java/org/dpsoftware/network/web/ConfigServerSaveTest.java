/*
  ConfigServerSaveTest.java

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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests persistence and restart decisions for the web settings endpoint.
 */
class ConfigServerSaveTest {

    /**
     * Verify the HTTP response, persistence, and restart behavior for each save scenario.
     *
     * @param payload      JSON settings sent with the request
     * @param allowRestart whether the request permits an application restart
     * @param capturing    whether capture is active before the save
     * @param status       expected HTTP response status
     * @param restart      whether an application restart is expected
     * @throws Exception if the test cannot invoke the handler or read its response
     */
    @ParameterizedTest
    @CsvSource({
            "'{}', false, true, 200, false",
            "'{\"brightness\": 50, \"toggleLed\": false}', false, true, 200, false",
            "'{\"ledStartOffset\": 1}', false, true, 200, false",
            "'{\"ledStartOffset\": 1}', false, false, 200, false",
            "'{\"language\": \"it\"}', false, true, 409, false",
            "'{\"language\": \"it\"}', true, true, 200, true"
    })
    void savesAndRestartsAccordingToTheChangedSettings(String payload, boolean allowRestart, boolean capturing,
                                                       int status, boolean restart) throws Exception {
        Configuration saved = new Configuration();
        saved.setLanguage("en");
        saved.setOutputDevice("AUTO");
        saved.setBrightness(255);
        saved.setTopLed(6);
        saved.setLeftLed(6);
        saved.setRightLed(6);
        saved.setBottomLeftLed(6);
        saved.setBottomRightLed(6);
        MainSingleton main = new MainSingleton();
        Configuration running = (Configuration) saved.clone();
        running.setOutputDevice("Glow Worm");
        running.setLedMatrix(new LinkedHashMap<>());
        main.config = running;
        main.RUNNING = capturing;
        main.restartNeeded = true;
        main.bundle = ResourceBundle.getBundle("messagebundle", Locale.ENGLISH);
        try (var singleton = mockStatic(MainSingleton.class);
             var nativeExecutor = mockStatic(NativeExecutor.class);
             var pipeline = mockStatic(PipelineManager.class);
             var storage = mockConstruction(StorageManager.class,
                     (manager, context) -> when(manager.readProfileInUseConfig()).thenReturn(saved))) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            var server = new ConfigServer();
            var exchange = mock(HttpExchange.class);
            var response = new ByteArrayOutputStream();
            String query = allowRestart ? "" : "?allowRestart=false&request=1";
            when(exchange.getRequestURI()).thenReturn(URI.create("/setConfig" + query));
            when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)));
            when(exchange.getResponseHeaders()).thenReturn(new Headers());
            when(exchange.getResponseBody()).thenReturn(response);

            var handler = ConfigServer.class.getDeclaredMethod("handleSetConfig", HttpExchange.class);
            handler.setAccessible(true);
            handler.invoke(server, exchange);

            verify(exchange).sendResponseHeaders(eq(status), anyLong());
            var manager = storage.constructed().getFirst();
            verify(manager, times(status == 200 ? 1 : 0)).writeConfig(any(Configuration.class), isNull());
            nativeExecutor.verify(NativeExecutor::restartNativeInstanceWithCurrentProfile, times(restart ? 1 : 0));
            boolean offsetChanged = payload.contains("ledStartOffset");
            pipeline.verify(() -> PipelineManager.restartCapture(any(Runnable.class)), times(offsetChanged && capturing ? 1 : 0));
            pipeline.verify(PipelineManager::refreshCaptureLedState, times(offsetChanged && !capturing ? 1 : 0));
            assertTrue(main.restartNeeded);
            if (status == 200 && !restart) {
                var config = CommonUtility.JSON_MAPPER.readTree(response.toByteArray()).path("config");
                assertEquals("AUTO", config.path("outputDevice").asText());
                assertEquals("Glow Worm", main.config.getOutputDevice());
                assertSame(running.getLedMatrix(), main.config.getLedMatrix());
                assertEquals(main.config.getBrightness(), config.path("brightness").asInt());
                assertEquals(main.config.isToggleLed(), config.path("toggleLed").asBoolean());
                if (payload.contains("brightness")) {
                    assertEquals(127, main.config.getBrightness());
                    assertFalse(main.config.isToggleLed());
                }
            } else {
                assertSame(running, main.config);
            }
        }
    }
}
