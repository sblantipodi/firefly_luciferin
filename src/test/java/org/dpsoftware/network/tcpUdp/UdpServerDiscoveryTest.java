/*
  UdpServerDiscoveryTest.java

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
package org.dpsoftware.network.tcpUdp;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.Satellite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UdpServerDiscoveryTest {

    private UdpServer server;
    private InterfaceAddress networkAddress;
    private InetAddress localAddress;
    private InetAddress broadcastAddress;

    @BeforeEach
    void setUp() throws Exception {
        localAddress = InetAddress.getByName("192.168.0.110");
        broadcastAddress = InetAddress.getByName("192.168.0.255");
        server = mock(UdpServer.class, CALLS_REAL_METHODS);
        server.socket = mock(DatagramSocket.class);
        when(server.socket.getLocalPort()).thenReturn(Constants.UDP_BROADCAST_PORT);
        networkAddress = mock(InterfaceAddress.class);
        when(networkAddress.getAddress()).thenReturn(localAddress);
        when(networkAddress.getBroadcast()).thenReturn(broadcastAddress);
    }

    @ParameterizedTest
    @ValueSource(ints = {5001, 5002, 5003})
    void keepsInterfaceBoundPingForEveryInstanceAndAddsCompatibilityPingOnlyForMain(int port) throws Exception {
        when(server.socket.getLocalPort()).thenReturn(port);
        DatagramPacket packet = ping(broadcastAddress);
        try (var sockets = mockConstruction(DatagramSocket.class, (socket, context) ->
                assertEquals(List.of(new InetSocketAddress(localAddress, 0)), context.arguments()))) {
            server.sendDiscoveryPing(networkAddress, packet);
            assertEquals(1, sockets.constructed().size());
            DatagramSocket interfaceSocket = sockets.constructed().getFirst();
            verify(interfaceSocket).setBroadcast(true);
            verify(interfaceSocket).send(packet);
            verify(interfaceSocket).close();
            verify(server.socket, times(port == 5001 ? 1 : 0)).send(packet);
            verify(server.socket, never()).close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"192.168.0.56", "192.168.50.56", "10.8.0.56"})
    void explicitTargetsKeepExistingUnicastPath(String targetIp) throws Exception {
        DatagramPacket packet = ping(InetAddress.getByName(targetIp));
        try (var sockets = mockConstruction(DatagramSocket.class)) {
            server.sendDiscoveryPing(networkAddress, packet);
            verify(sockets.constructed().getFirst()).send(packet);
            verify(server.socket, never()).send(any());
        }
    }

    @Test
    void interfaceWithoutBroadcastDoesNotUseCompatibilityPath() throws Exception {
        when(networkAddress.getBroadcast()).thenReturn(null);
        DatagramPacket packet = ping(InetAddress.getByName("10.8.0.56"));
        try (var sockets = mockConstruction(DatagramSocket.class)) {
            server.sendDiscoveryPing(networkAddress, packet);
            verify(sockets.constructed().getFirst()).send(packet);
            verify(server.socket, never()).send(any());
        }
    }

    @Test
    void compatibilityFailureDoesNotEscapeAfterSuccessfulInterfaceSend() throws Exception {
        doThrow(new IOException("blocked compatibility broadcast")).when(server.socket).send(any());
        DatagramPacket packet = ping(broadcastAddress);
        try (var sockets = mockConstruction(DatagramSocket.class)) {
            assertDoesNotThrow(() -> server.sendDiscoveryPing(networkAddress, packet));
            verify(sockets.constructed().getFirst()).send(packet);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {5002, 5003})
    void relayRetainsExplicitSourceOnEachInterface(int targetPort) throws Exception {
        InetAddress otherLocal = InetAddress.getByName("192.168.50.10");
        InetAddress otherBroadcast = InetAddress.getByName("192.168.50.255");
        InterfaceAddress other = mock(InterfaceAddress.class);
        when(other.getAddress()).thenReturn(otherLocal);
        when(other.getBroadcast()).thenReturn(otherBroadcast);
        server.eligibleInterfaceAddresses = List.of(networkAddress, other);
        byte[] payload = "device-status".getBytes(StandardCharsets.UTF_8);
        try (var sockets = mockConstruction(DatagramSocket.class, (socket, context) ->
                assertEquals(new InetSocketAddress(context.getCount() == 1 ? localAddress : otherLocal, 0), context.arguments().getFirst()))) {
            server.shareBroadCastToOtherInstance(payload, targetPort);
            assertEquals(2, sockets.constructed().size());
            for (int index = 0; index < 2; index++) {
                ArgumentCaptor<DatagramPacket> packet = ArgumentCaptor.forClass(DatagramPacket.class);
                verify(sockets.constructed().get(index)).send(packet.capture());
                assertEquals(targetPort, packet.getValue().getPort());
                assertEquals(index == 0 ? broadcastAddress : otherBroadcast, packet.getValue().getAddress());
                assertArrayEquals(payload, packet.getValue().getData());
            }
            verify(server.socket, never()).send(any());
        }
    }

    @Test
    void scheduledPingStillSendsSatellitePingWhenCompatibilityBroadcastFails() throws Exception {
        MainSingleton main = configuredMain();
        main.config.getSatellites().put("satellite", new Satellite("", "", "", "192.168.50.56", "satellite", ""));
        doThrow(new IOException("blocked compatibility broadcast")).when(server.socket).send(any());
        try (var sockets = mockConstruction(DatagramSocket.class, (socket, context) ->
                when(socket.getLocalAddress()).thenReturn(localAddress))) {
            runScheduledTask("pingTask", main);
            // Existing flow: bound main send, satellite route probe, bound satellite send.
            assertEquals(3, sockets.constructed().size());
            ArgumentCaptor<DatagramPacket> sent = ArgumentCaptor.forClass(DatagramPacket.class);
            verify(sockets.constructed().get(2)).send(sent.capture());
            assertEquals(InetAddress.getByName("192.168.50.56"), sent.getValue().getAddress());
            assertEquals(Constants.UDP_BROADCAST_PORT, sent.getValue().getPort());
            assertEquals("PING192.168.0.110", payload(sent.getValue()));
        }
    }

    @Test
    void deviceNameTaskKeepsBoundSourceAndDoesNotUseListeningSocket() throws Exception {
        try (var sockets = mockConstruction(DatagramSocket.class)) {
            runScheduledTask("setIpTask", configuredMain());
            assertEquals(1, sockets.constructed().size());
            ArgumentCaptor<DatagramPacket> sent = ArgumentCaptor.forClass(DatagramPacket.class);
            verify(sockets.constructed().getFirst()).send(sent.capture());
            assertEquals(broadcastAddress, sent.getValue().getAddress());
            assertEquals(Constants.UDP_DEVICE_NAME + "GW_ESP32_S3_2230", payload(sent.getValue()));
            verify(server.socket, never()).send(any());
        }
    }

    @Test
    void receivesFullDeviceStatusAfterShortDiscoveryPacket() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        try (DatagramSocket receiver = new DatagramSocket(0, loopback);
             DatagramSocket sender = new DatagramSocket()) {
            receiver.setSoTimeout(2000);
            DatagramPacket incoming = new DatagramPacket(new byte[512], 512);
            String shortPing = "PING192.168.1.255";
            String deviceStatus = "{\"deviceName\":\"GW_ESP32_S3_2230\",\"state\":\"ON\",\"IP\":\"192.168.1.42\",\"MAC\":\"AA:BB:CC:DD:EE:FF\"}";

            send(sender, receiver, shortPing);
            send(sender, receiver, deviceStatus);

            assertEquals(shortPing, UdpServer.receiveDatagram(receiver, incoming));
            assertEquals(deviceStatus, UdpServer.receiveDatagram(receiver, incoming));
        }
    }

    private void send(DatagramSocket sender, DatagramSocket receiver, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        sender.send(new DatagramPacket(bytes, bytes.length, receiver.getLocalAddress(), receiver.getLocalPort()));
    }

    private MainSingleton configuredMain() {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.config.setStaticGlowWormIp("AUTO");
        main.config.setOutputDevice("GW_ESP32_S3_2230");
        return main;
    }

    private void runScheduledTask(String name, MainSingleton main) throws Exception {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        try (var singleton = mockStatic(MainSingleton.class);
             var executors = mockStatic(Executors.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            executors.when(() -> Executors.newScheduledThreadPool(1)).thenReturn(executor);
            Method method = UdpServer.class.getDeclaredMethod(name, InterfaceAddress.class);
            method.setAccessible(true);
            method.invoke(server, networkAddress);
            ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
            verify(executor).scheduleAtFixedRate(task.capture(), eq(0L), anyLong(), eq(TimeUnit.SECONDS));
            task.getValue().run();
        }
    }

    private DatagramPacket ping(InetAddress target) {
        byte[] bytes = (Constants.UDP_PING + broadcastAddress.getHostAddress()).getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(bytes, bytes.length, target, Constants.UDP_BROADCAST_PORT);
    }

    private String payload(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }
}
