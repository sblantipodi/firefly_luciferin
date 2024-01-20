/*
  UdpServer.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.network.NetworkSingleton;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Map;
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

    DatagramSocket socket;
    InetAddress localIP;
    InetAddress broadcastAddress;
    boolean firstConnection = true;

    /**
     * - Initialize the main socket for receiving devices infos.
     * - Find local IP address, used to get local broadcast address. This way works well when there are multiple network interfaces.
     * It always returns the preferred outbound IP. The destination 8.8.8.8 is not needed to be reachable.
     */
    public UdpServer() {
        try {
            if (MainSingleton.getInstance().whoAmI == 1) {
                socket = new DatagramSocket(Constants.UDP_BROADCAST_PORT);
            } else if (MainSingleton.getInstance().whoAmI == 2) {
                socket = new DatagramSocket(Constants.UDP_BROADCAST_PORT_2);
            } else if (MainSingleton.getInstance().whoAmI == 3) {
                socket = new DatagramSocket(Constants.UDP_BROADCAST_PORT_3);
            }
            assert socket != null;
            socket.setBroadcast(true);
            try (final DatagramSocket socketForLocalIp = new DatagramSocket()) {
                socketForLocalIp.connect(InetAddress.getByName(Constants.UDP_IP_FOR_PREFERRED_OUTBOUND), Constants.UDP_PORT_PREFERRED_OUTBOUND);
                localIP = InetAddress.getByName(socketForLocalIp.getLocalAddress().getHostAddress());
                log.info("Local IP= " + localIP.getHostAddress());
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
                while (NetworkSingleton.getInstance().udpBroadcastReceiverRunning) {
                    try {
                        socket.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength());
                        if (!received.startsWith(Constants.UDP_DEVICE_NAME) && !received.startsWith(Constants.UDP_DEVICE_NAME_STATIC)) {
                            if (!received.contains(Constants.UDP_PING)) {
                                log.trace("Received UDP broadcast=" + received);
                                // Share received broadcast with other Firefly Luciferin instances
                                shareBroadCastToOtherInstances(received);
                            }
                            if (LocalizedEnum.fromBaseStr(Enums.Effect.class, received) != null) {
                                MainSingleton.getInstance().config.setEffect(received);
                                if (!MainSingleton.getInstance().RUNNING) {
                                    MainSingleton.getInstance().guiManager.startCapturingThreads();
                                }
                            }
                            if (received.contains(Constants.STOP_STR)) {
                                if (MainSingleton.getInstance().RUNNING) {
                                    MainSingleton.getInstance().guiManager.stopCapturingThreads(false);
                                    CommonUtility.turnOnLEDs();
                                }
                            }
                            if (!Constants.UDP_PONG.equals(received) && !Constants.UDP_PING.equals(received) && !received.contains(Constants.UDP_PING)) {
                                JsonNode responseJson = CommonUtility.fromJsonToObject(received);
                                if (responseJson != null && responseJson.get(Constants.STATE) != null && responseJson.get(Constants.MQTT_DEVICE_NAME) != null) {
                                    turnOnLightFirstTime(responseJson);
                                    CommonUtility.updateDeviceTable(Objects.requireNonNull(responseJson));
                                    CommonUtility.updateFpsWithDeviceTopic(Objects.requireNonNull(responseJson));
                                } else if (responseJson != null && responseJson.get(Constants.MQTT_FRAMERATE) != null) {
                                    CommonUtility.updateFpsWithFpsTopic(Objects.requireNonNull(responseJson));
                                } else if (ManagerSingleton.getInstance().deviceNameForSerialDevice.equals(received)) {
                                    log.info("Update successful=" + received);
                                    CommonUtility.sleepSeconds(60);
                                    MainSingleton.getInstance().guiManager.startCapturingThreads();
                                } else {
                                    GuiSingleton.getInstance().deviceTableData.forEach(glowWormDevice -> {
                                        if (glowWormDevice.getDeviceName().equals(received)) {
                                            log.info("Update successful=" + received);
                                            shareBroadCastToOtherInstances(received);
                                        }
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        log.error("UDP msg contains errors");
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return null;
        });
    }

    /**
     * Turn ON lights at first startup
     *
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
     * Manage UDP communication with Glow Worm
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
                        boolean useBroadcast = !NetworkManager.isValidIp(MainSingleton.getInstance().config.getStaticGlowWormIp());
                        if (localIP != null && localIP.getHostAddress() != null && interfaceAddress != null && interfaceAddress.getAddress() != null
                                && interfaceAddress.getAddress().getHostAddress() != null && ((interfaceAddress.getBroadcast() != null) || useBroadcast)
                                && localIP.getHostAddress().equals(interfaceAddress.getAddress().getHostAddress())) {
                            log.info("Network adapter in use=" + networkInterface.getDisplayName());
                            if (useBroadcast) {
                                log.info("Broadcast address found=" + interfaceAddress.getBroadcast());
                            } else {
                                log.info("Use static IP address=" + MainSingleton.getInstance().config.getOutputDevice());
                            }
                            pingTask(interfaceAddress);
                            setIpTask(interfaceAddress);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Send device name to GW. If the device name is the one used by the GW device,
     * GW will use the Firefly Luciferin IP for the next communications.
     *
     * @param interfaceAddress inet address
     */
    private void setIpTask(InterfaceAddress interfaceAddress) {
        // Send device name every 2 seconds
        ScheduledExecutorService udpIpExecutorService = Executors.newScheduledThreadPool(1);
        Runnable setIpTask = () -> {
            try {
                DatagramPacket broadCastPing;
                // Send the name of the device where Firefly wants to connect
                if (!Constants.SERIAL_PORT_AUTO.equals(MainSingleton.getInstance().config.getOutputDevice())) {
                    if (MainSingleton.getInstance().config.getSatellites() != null) {
                        boolean useBroadcast = !NetworkManager.isValidIp(MainSingleton.getInstance().config.getStaticGlowWormIp());
                        if (useBroadcast) {
                            broadCastPing = getDatagramPacket(Constants.UDP_DEVICE_NAME, MainSingleton.getInstance().config.getOutputDevice(), interfaceAddress.getBroadcast(), interfaceAddress.getBroadcast());
                        } else {
                            broadCastPing = getDatagramPacket(Constants.UDP_DEVICE_NAME_STATIC, MainSingleton.getInstance().config.getOutputDevice(), interfaceAddress.getAddress(), InetAddress.getByName(MainSingleton.getInstance().config.getStaticGlowWormIp()));
                        }
                        socket.send(broadCastPing);
                        for (Map.Entry<String, Satellite> sat : MainSingleton.getInstance().config.getSatellites().entrySet()) {
                            broadCastPing = getDatagramPacket(Constants.UDP_DEVICE_NAME_STATIC, sat.getValue().getDeviceIp(), interfaceAddress.getAddress(), InetAddress.getByName(sat.getValue().getDeviceIp()));
                            socket.send(broadCastPing);
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        };
        udpIpExecutorService.scheduleAtFixedRate(setIpTask, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * Send a broadcast PING every second to the correct Network Adapter on the correct broadcast address
     * This is needed on some routers that blocks UDP traffic when there is no bidirectional traffic.
     * Glow Worm receives the PING and uses the IP from the Firefly instance for next communications.
     *
     * @param interfaceAddress inet address
     */
    private void pingTask(InterfaceAddress interfaceAddress) {
        // PING broadcast every seconds
        ScheduledExecutorService udpBrExecutorService = Executors.newScheduledThreadPool(1);
        Runnable pingTask = () -> {
            try {
                DatagramPacket broadCastPing;
                if (MainSingleton.getInstance().config.getSatellites() != null) {
                    boolean useBroadcast = !NetworkManager.isValidIp(MainSingleton.getInstance().config.getStaticGlowWormIp());
                    if (useBroadcast) {
                        broadCastPing = getDatagramPacket(Constants.UDP_PING, interfaceAddress.getBroadcast().toString().substring(1), interfaceAddress.getBroadcast(), interfaceAddress.getBroadcast());
                    } else {
                        broadCastPing = getDatagramPacket(Constants.UDP_PING, interfaceAddress.getAddress().toString().substring(1), interfaceAddress.getAddress(), InetAddress.getByName(MainSingleton.getInstance().config.getStaticGlowWormIp()));
                    }
                    socket.send(broadCastPing);
                    for (Map.Entry<String, Satellite> sat : MainSingleton.getInstance().config.getSatellites().entrySet()) {
                        broadCastPing = getDatagramPacket(Constants.UDP_PING, interfaceAddress.getAddress().toString().substring(1), interfaceAddress.getAddress(), InetAddress.getByName(sat.getValue().getDeviceIp()));
                        socket.send(broadCastPing);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        };
        udpBrExecutorService.scheduleAtFixedRate(pingTask, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Set Ip datagram packet
     *
     * @param outputDevice device name
     * @param broadcastIp  IP to use for broadcast
     * @param deviceIp     device IP for communication
     * @return datagram packet to use for sending the msg
     */
    private DatagramPacket getDatagramPacket(String prefix, String outputDevice, InetAddress broadcastIp, InetAddress deviceIp) {
        byte[] bufferBroadcastPing;
        DatagramPacket broadCastPing;
        String udpMsg;
        udpMsg = (prefix + outputDevice);
        bufferBroadcastPing = udpMsg.getBytes();
        log.trace(udpMsg);
        broadcastAddress = broadcastIp;
        broadCastPing = new DatagramPacket(bufferBroadcastPing, bufferBroadcastPing.length,
                deviceIp, Constants.UDP_BROADCAST_PORT);
        return broadCastPing;
    }

    /**
     * Share received broadcast with other Firefly Luciferin instances
     *
     * @param received received brodcast
     */
    private void shareBroadCastToOtherInstances(String received) {
        if (!MainSingleton.getInstance().config.isMultiScreenSingleDevice() && MainSingleton.getInstance().whoAmI == 1 && MainSingleton.getInstance().config.getMultiMonitor() >= 2) {
            shareBroadCastToOtherInstance(received.getBytes(), Constants.UDP_BROADCAST_PORT_2);
            log.trace("Sharing to instance 2 =" + received);
        }
        if (!MainSingleton.getInstance().config.isMultiScreenSingleDevice() && MainSingleton.getInstance().whoAmI == 1 && MainSingleton.getInstance().config.getMultiMonitor() == 3) {
            shareBroadCastToOtherInstance(received.getBytes(), Constants.UDP_BROADCAST_PORT_3);
            log.trace("Sharing to instance 3 =" + received);
        }
    }

    /**
     * Share received broadcast with other Firefly Luciferin instance
     *
     * @param bufferBroadcastPing message received on the main broadcast port from the devices
     * @param broadcastPort       boradcast to where to share the received msg
     */
    @SuppressWarnings("Duplicates")
    void shareBroadCastToOtherInstance(byte[] bufferBroadcastPing, int broadcastPort) {
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
