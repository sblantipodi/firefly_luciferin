/*
  ConfigServerSatelliteWaitTest.java

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
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ConfigServerSatelliteWaitTest {
    @Test
    void fpsEndpointExposesCurrentSatelliteWaitState() throws Exception {
        var main = MainSingleton.getInstance();
        var previousConfig = main.config;
        boolean previousWaiting = GuiSingleton.getInstance().waitingForSatellites;
        try {
            main.config = new Configuration();
            var server = mock(ConfigServer.class, CALLS_REAL_METHODS);
            var handler = ConfigServer.class.getDeclaredMethod("handleGetFps", HttpExchange.class);
            handler.setAccessible(true);
            for (boolean waiting : new boolean[]{false, true, false}) {
                GuiSingleton.getInstance().waitingForSatellites = waiting;
                var exchange = mock(HttpExchange.class);
                var output = new ByteArrayOutputStream();
                when(exchange.getResponseHeaders()).thenReturn(new Headers());
                when(exchange.getResponseBody()).thenReturn(output);
                handler.invoke(server, exchange);
                var json = CommonUtility.JSON_MAPPER.readTree(output.toByteArray());
                assertTrue(json.has("waitingForSatellites"));
                assertEquals(waiting, json.path("waitingForSatellites").asBoolean());
                assertTrue(json.has("fireflyUpdateAvailable"));
                assertTrue(json.has("glowWormUpdateAvailable"));
            }
        } finally {
            main.config = previousConfig;
            GuiSingleton.getInstance().waitingForSatellites = previousWaiting;
        }
    }
}
