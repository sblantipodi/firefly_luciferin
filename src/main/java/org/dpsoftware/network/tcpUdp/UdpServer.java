/*
  UdpServer.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.config.Constants;
import org.dpsoftware.managers.UpgradeManager;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * UDP server class
 */
@Slf4j
public class UdpServer {

    public static boolean udpBroadcastReceiverRunning = false;
    DatagramSocket socket;
    InetAddress localIP;
    InetAddress broadcastAddress;
    boolean firstConnection = true;

    /**
     * - Initialize the main socket for receiving devices infos.
     * - Find local IP address, used to get local broadcast address. This way works well when there are multiple network interfaces.
     *   It always returns the preferred outbound IP. The destination 8.8.8.8 is not needed to be reachable.
     */
    public UdpServer() {

        try {
            if (JavaFXStarter.whoAmI == 1) {
                socket = new DatagramSocket(Constants.UDP_BROADCAST_PORT);
            } else if (JavaFXStarter.whoAmI == 2) {
                socket = new DatagramSocket(Constants.UDP_BROADCAST_PORT_2);
            } else if (JavaFXStarter.whoAmI == 3) {
                socket = new DatagramSocket(Constants.UDP_BROADCAST_PORT_3);
            }
            assert socket != null;
            socket.setBroadcast(true);
            try (final DatagramSocket socketForLocalIp = new DatagramSocket()) {
                socketForLocalIp.connect(InetAddress.getByName(Constants.UDP_IP_FOR_PREFERRED_OUTBOUND), Constants.UDP_PORT_PREFERRED_OUTBOUND);
                localIP = InetAddress.getByName(socketForLocalIp.getLocalAddress().getHostAddress());
                log.debug("Local IP= " + localIP.getHostAddress());
            }
        } catch (SocketException | UnknownHostException e) {
            log.error(e.getMessage());
        }

    }

    /**
     * Receive UDP broadcast from Glow Worm Luciferin device
     */
    @SuppressWarnings("Duplicates")
    public void receiveBroadcastUDPPacket() {

        broadcastToCorrectNetworkAdapter();
        CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("UDP broadcast listen on " + socket.getLocalAddress());
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                while (udpBroadcastReceiverRunning) {
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    if (received.contains("STOP")) {
                        FireflyLuciferin.guiManager.stopCapturingThreads(false);
                    } else if (received.contains("PLAY")) {
                        if (!FireflyLuciferin.RUNNING) {
                            FireflyLuciferin.guiManager.startCapturingThreads();
                        }
                    } else {
                        if (!Constants.UDP_PING.equals(received)) {
                            CommonUtility.conditionedLog(this.getClass().getTypeName(), "Received UDP broadcast=" + received);
                            // Share received broadcast with other Firefly Luciferin instances
                            if (!FireflyLuciferin.config.isMultiScreenSingleDevice() && JavaFXStarter.whoAmI == 1 && FireflyLuciferin.config.getMultiMonitor() >= 2) {
                                shareBroadCastToOtherInstances(received.getBytes(), Constants.UDP_BROADCAST_PORT_2);
                            } else if (!FireflyLuciferin.config.isMultiScreenSingleDevice() && JavaFXStarter.whoAmI == 1 && FireflyLuciferin.config.getMultiMonitor() == 3) {
                                shareBroadCastToOtherInstances(received.getBytes(), Constants.UDP_BROADCAST_PORT_3);
                            }
                        }
                        if (!Constants.UDP_PONG.equals(received) && !Constants.UDP_PING.equals(received)) {
                            JsonNode responseJson = CommonUtility.fromJsonToObject(received);
                            if (responseJson != null && responseJson.get(Constants.STATE) != null) {
                                turnOnLightFirstTime(responseJson);
                                CommonUtility.updateDeviceTable(Objects.requireNonNull(responseJson));
                                CommonUtility.updateFpsWithDeviceTopic(Objects.requireNonNull(responseJson));
                            } else if (responseJson != null && responseJson.get(Constants.MQTT_FRAMERATE) != null) {
                                CommonUtility.updateFpsWithFpsTopic(Objects.requireNonNull(responseJson));
                            } else if (UpgradeManager.deviceNameForSerialDevice.equals(received)) {
                                log.debug("Update successfull=" + received);
                                if (!CommonUtility.isSingleDeviceMultiScreen() || CommonUtility.isSingleDeviceMainInstance()) {
                                    javafx.application.Platform.runLater(() -> FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN,
                                            CommonUtility.getWord(Constants.UPGRADE_SUCCESS), received + CommonUtility.getWord(Constants.DEVICEUPGRADE_SUCCESS),
                                            Alert.AlertType.INFORMATION));
                                }
                                CommonUtility.sleepSeconds(60);
                                FireflyLuciferin.guiManager.startCapturingThreads();
                            }
                        }
                    }

                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            return null;
        });

    }

    /**
     * Turn ON lights at first startup
     * @param udpMsg message arrived from Glow Worm devices via UDP broadcast
     */
    private void turnOnLightFirstTime(JsonNode udpMsg) {

        String deviceName = udpMsg.get(Constants.MQTT_DEVICE_NAME).textValue();
        if (firstConnection && deviceName != null && CommonUtility.getDeviceToUse() != null
                && deviceName.equals(CommonUtility.getDeviceToUse().getDeviceName())) {
            firstConnection = false;
            CommonUtility.turnOnLEDs();
        }

    }

    /**
     * Send a broadcast PING every second to the correct Network Adapter on the correct broadcast address
     * This is needed on some routers that blocks UDP traffic when there is no bidirectional traffic
     */
    @SuppressWarnings("Duplicates")
    void broadcastToCorrectNetworkAdapter() {

        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // Do not want to use the loopback interface.
                if (!networkInterface.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        if (localIP != null && localIP.getHostAddress() != null && interfaceAddress != null && interfaceAddress.getAddress() != null
                                && interfaceAddress.getAddress().getHostAddress() != null && interfaceAddress.getBroadcast() != null
                                && localIP.getHostAddress().equals(interfaceAddress.getAddress().getHostAddress())) {
                            log.debug("Network adapter in use=" + networkInterface.getDisplayName());
                            log.debug("Broadcast address found=" + interfaceAddress.getBroadcast());
                            ScheduledExecutorService serialscheduledExecutorService = Executors.newScheduledThreadPool(1);
                            // PING broadcast every seconds
                            Runnable framerateTask = () -> {
                                byte[] bufferBroadcastPing = Constants.UDP_PING.getBytes();
                                DatagramPacket broadCastPing;
                                try {
                                    broadcastAddress = interfaceAddress.getBroadcast();
                                    broadCastPing = new DatagramPacket(bufferBroadcastPing, bufferBroadcastPing.length,
                                            interfaceAddress.getBroadcast(), Constants.UDP_BROADCAST_PORT);
                                    socket.send(broadCastPing);
                                } catch (IOException e) {
                                    log.error(e.getMessage());
                                }
                            };
                            serialscheduledExecutorService.scheduleAtFixedRate(framerateTask, 0, 1, TimeUnit.SECONDS);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            log.error(e.getMessage());
        }

    }

    /**
     * Share received broadcast with other Firefly Luciferin instances
     * @param bufferBroadcastPing message received on the main broadcast port from the devices
     * @param broadcastPort boradcast to where to share the received msg
     */
    @SuppressWarnings("Duplicates")
    void shareBroadCastToOtherInstances(byte[] bufferBroadcastPing, int broadcastPort) {

        DatagramPacket broadCastPing;
        try {
            broadCastPing = new DatagramPacket(bufferBroadcastPing, bufferBroadcastPing.length,
                    broadcastAddress, broadcastPort);
            socket.send(broadCastPing);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }

}
