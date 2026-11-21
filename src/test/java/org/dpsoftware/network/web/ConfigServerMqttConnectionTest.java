/*
  ConfigServerMqttConnectionTest.java

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
import org.dpsoftware.config.Configuration;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ConfigServerMqttConnectionTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void mqttWarningIsExposedUntilRecoveredOrDisabled(boolean headless) throws Exception {
        MainSingleton main = new MainSingleton();
        main.headlessMode = headless;
        main.config = new Configuration();
        main.bundle = ResourceBundle.getBundle("messagebundle", Locale.ENGLISH);
        ManagerSingleton state = new ManagerSingleton();
        try (var singleton = mockStatic(MainSingleton.class);
             var managers = mockStatic(ManagerSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            managers.when(ManagerSingleton::getInstance).thenReturn(state);
            var server = mock(ConfigServer.class, CALLS_REAL_METHODS);
            var handler = ConfigServer.class.getDeclaredMethod("handleGetFps", HttpExchange.class);
            handler.setAccessible(true);
            // Healthy, failed, recovered, and MQTT disabled with a previous failure.
            for (boolean[] scenario : new boolean[][]{{true, false}, {true, true}, {true, false}, {false, true}}) {
                main.config.setMqttEnable(scenario[0]);
                state.mqttConnectionError = scenario[1];
                var exchange = mock(HttpExchange.class);
                var output = new ByteArrayOutputStream();
                when(exchange.getResponseHeaders()).thenReturn(new Headers());
                when(exchange.getResponseBody()).thenReturn(output);
                handler.invoke(server, exchange);
                var json = CommonUtility.JSON_MAPPER.readTree(output.toByteArray());
                if (scenario[0] && scenario[1]) {
                    assertEquals("Luciferin is unable to connect to the MQTT server, please correct your settings and retry.",
                            json.path("mqttConnectionError").asText());
                } else {
                    assertTrue(json.path("mqttConnectionError").isNull());
                }
            }
        }
    }
}
