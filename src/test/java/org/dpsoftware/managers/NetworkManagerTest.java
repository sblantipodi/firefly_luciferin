/*
  NetworkManagerTest.java

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

import javafx.application.Platform;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.gui.GuiManager;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MQTT connection state and IPv4 validation.
 */
class NetworkManagerTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void mqttWarningTracksInitialFailureConnectionLossAndRecovery(boolean headless) throws Exception {
        MainSingleton main = new MainSingleton();
        main.headlessMode = headless;
        main.config = new Configuration();
        main.config.setMqttEnable(true);
        main.config.setMqttServer("tcp://localhost:1883");
        main.guiManager = mock(GuiManager.class);
        ManagerSingleton state = new ManagerSingleton();
        AtomicBoolean failConnect = new AtomicBoolean(true);
        AtomicBoolean failSubscribe = new AtomicBoolean(false);
        try (var singleton = mockStatic(MainSingleton.class);
             var managers = mockStatic(ManagerSingleton.class);
             var platform = mockStatic(Platform.class);
             var clients = mockConstruction(MqttClient.class, (client, context) -> {
                 doAnswer(invocation -> {
                     if (failConnect.get()) throw new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR);
                     return null;
                 }).when(client).connect(any(MqttConnectOptions.class));
                 doAnswer(invocation -> {
                     if (failSubscribe.get()) throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
                     return null;
                 }).when(client).subscribe(anyString());
             })) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            managers.when(ManagerSingleton::getInstance).thenReturn(state);
            NetworkManager network = new NetworkManager(true, new AtomicInteger(3));
            assertFalse(network.connected);
            assertTrue(state.mqttConnectionError);
            platform.verify(() -> Platform.runLater(any(Runnable.class)), times(headless ? 0 : 1));

            failConnect.set(false);
            network.attemptReconnect();
            assertTrue(network.connected);
            assertFalse(state.mqttConnectionError);

            var scheduler = mock(ScheduledExecutorService.class);
            try (var executors = mockStatic(Executors.class);
                 var pipelines = mockStatic(PipelineManager.class)) {
                executors.when(() -> Executors.newScheduledThreadPool(1)).thenReturn(scheduler);
                network.connectionLost(new IllegalStateException("Broker offline"));
                assertFalse(network.connected);
                assertTrue(state.mqttConnectionError);
                var retry = ArgumentCaptor.forClass(Runnable.class);
                verify(scheduler).scheduleAtFixedRate(retry.capture(), eq(0L), eq(10L), eq(TimeUnit.SECONDS));

                failSubscribe.set(true);
                retry.getValue().run();
                assertTrue(state.mqttConnectionError);
                assertFalse(network.connected);
                verify(scheduler, never()).shutdown();

                failSubscribe.set(false);
                retry.getValue().run();
                assertFalse(state.mqttConnectionError);
                assertTrue(network.connected);
                verify(scheduler).shutdown();
            }
        }
    }

    @Test
    void isValidIp_validIpv4Addresses() {
        assertTrue(NetworkManager.isValidIp("192.168.1.1"));
        assertTrue(NetworkManager.isValidIp("10.0.0.1"));
        assertTrue(NetworkManager.isValidIp("172.16.0.1"));
        assertTrue(NetworkManager.isValidIp("127.0.0.1"));
        assertTrue(NetworkManager.isValidIp("255.255.255.255"));
        assertTrue(NetworkManager.isValidIp("0.0.0.0"));
    }

    @Test
    void isValidIp_nullAndEmpty() {
        assertFalse(NetworkManager.isValidIp(null));
        assertFalse(NetworkManager.isValidIp(""));
    }

    @Test
    void isValidIp_tooFewOctets() {
        assertFalse(NetworkManager.isValidIp("192.168.1"));
        assertFalse(NetworkManager.isValidIp("192.168"));
        assertFalse(NetworkManager.isValidIp("192"));
    }

    @Test
    void isValidIp_tooManyOctets() {
        assertFalse(NetworkManager.isValidIp("192.168.1.1.1"));
        assertFalse(NetworkManager.isValidIp("1.2.3.4.5.6"));
    }

    @Test
    void isValidIp_valuesOutOfRange() {
        assertFalse(NetworkManager.isValidIp("256.1.1.1"));
        assertFalse(NetworkManager.isValidIp("1.256.1.1"));
        assertFalse(NetworkManager.isValidIp("1.1.256.1"));
        assertFalse(NetworkManager.isValidIp("1.1.1.256"));
        assertFalse(NetworkManager.isValidIp("-1.0.0.0"));
    }

    @Test
    void isValidIp_nonNumericParts() {
        assertFalse(NetworkManager.isValidIp("abc.def.ghi.jkl"));
        assertFalse(NetworkManager.isValidIp("192.168.1.a"));
        assertFalse(NetworkManager.isValidIp("192.168.1."));
    }

    @Test
    void isValidIp_trailingDot() {
        assertFalse(NetworkManager.isValidIp("192.168.1.1."));
    }

    @Test
    void isValidIp_withSpaces() {
        assertFalse(NetworkManager.isValidIp(" 192.168.1.1"));
        assertFalse(NetworkManager.isValidIp("192.168.1.1 "));
        assertFalse(NetworkManager.isValidIp("192. 168.1.1"));
    }

    @Test
    void isValidIp_boundaryValues() {
        assertTrue(NetworkManager.isValidIp("0.0.0.0"));
        assertTrue(NetworkManager.isValidIp("255.255.255.255"));
    }
}
