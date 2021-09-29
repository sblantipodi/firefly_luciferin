/*
  UdpServer.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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
    InetAddress localBroadcastAddress;

    /**
     * - Initialize the main socket for receiving devices infos.
     * - Find local IP address, used to get local broadcast address. This way works well when there are multiple network interfaces.
     *   It always returns the preferred outbound IP. The destination 8.8.8.8 is not needed to be reachable.
     */
    public UdpServer() {

        try {
            if (JavaFXStarter.whoAmI == 1) {
                socket = new DatagramSocket(Constants.UDP_BROADCAST_PORT);
            } else {
                socket = new DatagramSocket(5002);
            }
            socket.setBroadcast(true);
            try (final DatagramSocket socketForLocalIp = new DatagramSocket()) {
                socketForLocalIp.connect(InetAddress.getByName(Constants.UDP_IP_FOR_PREFERRED_OUTBOUND), Constants.UDP_PORT_PREFERRED_OUTBOUND);
                localIP = InetAddress.getByName(socketForLocalIp.getLocalAddress().getHostAddress());
                log.debug("Local IP= " + localIP.getHostAddress());
            }
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
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
                    if (!Constants.UDP_PING.equals(received)) {
                        CommonUtility.conditionedLog(this.getClass().getTypeName(), "Received UDP broadcast=" + received);
                    }
                    if (!Constants.UDP_PONG.equals(received) && !Constants.UDP_PING.equals(received)) {
                        JsonNode responseJson = CommonUtility.fromJsonToObject(received);
                        if (responseJson != null && responseJson.get(Constants.STATE) != null) {
                            CommonUtility.updateDeviceTable(Objects.requireNonNull(responseJson));
                            CommonUtility.updateFpsWithDeviceTopic(Objects.requireNonNull(responseJson));
                        } else if (responseJson != null && responseJson.get(Constants.MQTT_FRAMERATE) != null) {
                            CommonUtility.updateFpsWithFpsTopic(Objects.requireNonNull(responseJson));
                        } else if (UpgradeManager.deviceNameForSerialDevice.equals(received)) {
                            log.debug("Update successfull=" + received);
                            if (!CommonUtility.isSingleDeviceMultiScreen() || CommonUtility.isSingleDeviceMainInstance()) {
                                javafx.application.Platform.runLater(() -> FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN,
                                        Constants.UPGRADE_SUCCESS, received + Constants.DEVICEUPGRADE_SUCCESS,
                                        Alert.AlertType.INFORMATION));
                            }
                            CommonUtility.sleepSeconds(60);
                            FireflyLuciferin.guiManager.startCapturingThreads();
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
     * Send a broadcast PING every second to the correct Network Adapter on the correct broadcast address
     * This is needed on some routers that blocks UDP traffic when there is no bidirectional traffic
     */
    void broadcastToCorrectNetworkAdapter() {

        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // Do not want to use the loopback interface.
                if (!networkInterface.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                        InetAddress genericBroadcastAddress = interfaceAddress.getBroadcast();
                        if (genericBroadcastAddress != null && getLocalBroadCastAddress(genericBroadcastAddress, networkInterface)) {
                            ScheduledExecutorService serialscheduledExecutorService = Executors.newScheduledThreadPool(1);
                            Runnable framerateTask = () -> {
                                byte[] bufferBroadcastPing = Constants.UDP_PING.getBytes();
                                DatagramPacket broadCastPing;
                                try {
                                    broadCastPing = new DatagramPacket(bufferBroadcastPing, bufferBroadcastPing.length,
                                            genericBroadcastAddress, Constants.UDP_BROADCAST_PORT);
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
     * Compare current IP address with broadcast addresses of all the network adapter,
     * @param genericBroadcastAddress from a generic network adapter
     * @param networkInterface one of the network interfaces
     * @return true if a broadcast address matches current IP address
     */
    boolean getLocalBroadCastAddress(InetAddress genericBroadcastAddress, NetworkInterface networkInterface) {

        String localIpTrio = localIP.getHostAddress().substring(0, localIP.getHostAddress().lastIndexOf("."));
        String genericBroadcastAddressTrio = genericBroadcastAddress.getHostAddress().substring(0, genericBroadcastAddress.getHostAddress().lastIndexOf("."));
        if (genericBroadcastAddressTrio.equals(localIpTrio)) {
            localBroadcastAddress = genericBroadcastAddress;
            log.debug("Network adapter in use=" + networkInterface.getDisplayName());
            log.debug("Broadcast address found=" + localBroadcastAddress.getHostAddress());
            return true;
        }
        return false;

    }

}
