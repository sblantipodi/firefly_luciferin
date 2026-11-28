/*
  ConfigServerUpdateTest.java

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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import static org.mockito.Mockito.*;

class ConfigServerUpdateTest {

    /**
     * Verifies that the update handler responds while the web server is running and the GUI manager is still starting.
     *
     * @throws ReflectiveOperationException if the handler cannot be invoked
     * @throws IOException                  if the response headers cannot be written
     */
    @Test
    void returnsServiceUnavailableUntilGuiManagerIsReady() throws ReflectiveOperationException, IOException {
        var exchange = mock(HttpExchange.class);
        var requestHeaders = new Headers();
        requestHeaders.set("Content-Type", "application/json");
        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(OutputStream.nullOutputStream());
        var handler = ConfigServer.class.getDeclaredMethod("handleCheckForUpdates", HttpExchange.class);
        handler.setAccessible(true);

        try (var singleton = mockStatic(MainSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(new MainSingleton());
            handler.invoke(new ConfigServer(), exchange);
        }

        verify(exchange).sendResponseHeaders(eq(HttpURLConnection.HTTP_UNAVAILABLE), anyLong());
    }
}
