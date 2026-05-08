/*
  UdpServer.java

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
import java.util.*;
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
    List<InterfaceAddress> eligibleInterfaceAddresses = Collections.emptyList();
    boolean firstConnection = true;

    /**
     * Initialize the main socket for receiving devices infos.
     * Find local IP address, used to get local broadcast address. This way works well when there are multiple network interfaces.
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
            log.debug("UDP discovery socket initialized on port={}", socket.getLocalPort());
        } catch (SocketException e) {
            log.error(e.getMessage());
        }
    }

    private static boolean interfaceMatchesExcludedKeywords(NetworkInterface networkInterface) {
        String interfaceName = networkInterface.getName() != null ? networkInterface.getName().toLowerCase() : "";
        String displayName = networkInterface.getDisplayName() != null ? networkInterface.getDisplayName().toLowerCase() : "";
        return Enums.InterfaceToExclude.contains(interfaceName) || Enums.InterfaceToExclude.contains(displayName);
    }

    /**
     * Receive UDP broadcast from Glow Worm Luciferin device
     */
    @SuppressWarnings("Duplicates")
    public void receiveBroadcastUDPPacket() {
        broadcastToCorrectNetworkAdapter();
        CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("UDP broadcast listen on {}", socket.getLocalAddress());
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                while (NetworkSingleton.getInstance().udpBroadcastReceiverRunning) {
                    try {
                        socket.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength());
                        if (!received.startsWith(Constants.UDP_DEVICE_NAME) && !received.startsWith(Constants.UDP_DEVICE_NAME_STATIC)) {
                            if (!received.contains(Constants.UDP_PING)) {
                                log.trace("Received UDP broadcast={}", received);
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
                                    logUpdateSuccessful(received);
                                    CommonUtility.sleepSeconds(60);
                                    MainSingleton.getInstance().guiManager.startCapturingThreads();
                                } else {
                                    GuiSingleton.getInstance().deviceTableData.forEach(glowWormDevice -> {
                                        if (glowWormDevice.getDeviceName().equals(received)) {
                                            logUpdateSuccessful(received);
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

    private void logUpdateSuccessful(String received) {
        log.info("Update successful={}", received);
    }

    /**
     * Manage UDP communication with Glow Worm
     */
    @SuppressWarnings("Duplicates")
    void broadcastToCorrectNetworkAdapter() {
        try {
            boolean useBroadcast = !NetworkManager.isValidIp(MainSingleton.getInstance().config.getStaticGlowWormIp());
            log.debug("Starting UDP discovery. mode={}, staticGlowWormIp={}",
                    useBroadcast ? "broadcast" : "static-ip", MainSingleton.getInstance().config.getStaticGlowWormIp());
            eligibleInterfaceAddresses = getEligibleInterfaceAddresses(useBroadcast);
            log.info("Eligible UDP interface addresses count={}", eligibleInterfaceAddresses.size());
            for (InterfaceAddress interfaceAddress : eligibleInterfaceAddresses) {
                InetAddress address = interfaceAddress.getAddress();
                if (address != null) {
                    log.info("Local IP={}", address.getHostAddress());
                }
                if (useBroadcast) {
                    log.info("Broadcast address found={}", interfaceAddress.getBroadcast());
                } else {
                    log.info("Use static IP address={} via interface={}",
                            MainSingleton.getInstance().config.getStaticGlowWormIp(), address != null ? address.getHostAddress() : "unknown");
                }
                pingTask(interfaceAddress);
                setIpTask(interfaceAddress);
            }
            if (eligibleInterfaceAddresses.isEmpty()) {
                log.warn("No eligible network adapter found for UDP discovery. useBroadcast={}", useBroadcast);
            }
        } catch (SocketException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Find all IPv4 interfaces that can be used for UDP discovery.
     *
     * @param useBroadcast whether to target subnet broadcast addresses
     * @return eligible interface addresses
     */
    private List<InterfaceAddress> getEligibleInterfaceAddresses(boolean useBroadcast) throws SocketException {
        List<InterfaceAddress> interfaceAddresses = new ArrayList<>();
        String staticGlowWormIp = MainSingleton.getInstance().config.getStaticGlowWormIp();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            log.debug("Inspecting interface name={}, displayName={}, up={}, loopback={}, virtual={}",
                    networkInterface.getName(), networkInterface.getDisplayName(), networkInterface.isUp(),
                    networkInterface.isLoopback(), networkInterface.isVirtual());
            if (!isEligibleNetworkInterface(networkInterface)) {
                log.debug("Skipping interface {} because it is not eligible for UDP discovery", networkInterface.getDisplayName());
                continue;
            }
            log.debug("Network adapter candidate={}", networkInterface.getDisplayName());
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress address = interfaceAddress.getAddress();
                log.debug("Inspecting interface address ip={}, broadcast={}, prefixLength={}",
                        address != null ? address.getHostAddress() : "null",
                        interfaceAddress.getBroadcast(),
                        interfaceAddress.getNetworkPrefixLength());
                if (!(address instanceof Inet4Address)) {
                    log.debug("Skipping interface address {} because it is not IPv4",
                            address != null ? address.getHostAddress() : "null");
                    continue;
                }
                if (useBroadcast) {
                    if (interfaceAddress.getBroadcast() != null) {
                        interfaceAddresses.add(interfaceAddress);
                        log.debug("Selected interface {} with IPv4 {} for broadcast discovery on {}",
                                networkInterface.getDisplayName(), address.getHostAddress(), interfaceAddress.getBroadcast());
                    } else {
                        log.debug("Skipping IPv4 {} because it has no broadcast address", address.getHostAddress());
                    }
                } else {
                    boolean sameSubnet = isAddressInSameSubnet(interfaceAddress, staticGlowWormIp);
                    if (sameSubnet) {
                        interfaceAddresses.add(interfaceAddress);
                        log.debug("Selected interface {} with IPv4 {} for static target {}",
                                networkInterface.getDisplayName(), address.getHostAddress(), staticGlowWormIp);
                    } else {
                        log.debug("Skipping IPv4 {} because it is not in the same subnet as {}",
                                address.getHostAddress(), staticGlowWormIp);
                    }
                }
            }
        }
        return interfaceAddresses;
    }

    /**
     * Validate a network interface before using it for discovery.
     *
     * @param networkInterface interface to check
     * @return true if the interface can be used
     */
    private boolean isEligibleNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        if (!networkInterface.isUp()) {
            log.debug("Interface {} excluded because it is down", networkInterface.getDisplayName());
            return false;
        }
        if (networkInterface.isLoopback()) {
            log.debug("Interface {} excluded because it is loopback", networkInterface.getDisplayName());
            return false;
        }
        if (networkInterface.isVirtual()) {
            log.debug("Interface {} excluded because it is virtual", networkInterface.getDisplayName());
            return false;
        }
        if (networkInterface.isPointToPoint()) {
            log.debug("Interface {} excluded because it is point-to-point", networkInterface.getDisplayName());
            return false;
        }
        if (interfaceMatchesExcludedKeywords(networkInterface)) {
            log.debug("Interface {} excluded because it matches the interface exclusion list", networkInterface.getDisplayName());
            return false;
        }
        return networkInterface.isUp()
                && !networkInterface.isLoopback()
                && !networkInterface.isVirtual()
                && !networkInterface.isPointToPoint()
                && !interfaceMatchesExcludedKeywords(networkInterface);
    }

    /**
     * Check if a target IP belongs to the same subnet of an interface.
     *
     * @param interfaceAddress interface to check
     * @param targetIp         target IP address
     * @return true if the target IP belongs to the same subnet
     */
    private boolean isAddressInSameSubnet(InterfaceAddress interfaceAddress, String targetIp) {
        if (!NetworkManager.isValidIp(targetIp) || interfaceAddress == null || interfaceAddress.getAddress() == null) {
            log.debug("Cannot evaluate subnet match for interfaceAddress={} and targetIp={}", interfaceAddress, targetIp);
            return false;
        }
        short prefixLength = interfaceAddress.getNetworkPrefixLength();
        if (prefixLength < 0 || prefixLength > 32) {
            log.debug("Invalid prefixLength={} for address={}", prefixLength, interfaceAddress.getAddress().getHostAddress());
            return false;
        }
        byte[] localAddress = interfaceAddress.getAddress().getAddress();
        byte[] targetAddress;
        try {
            targetAddress = InetAddress.getByName(targetIp).getAddress();
        } catch (UnknownHostException e) {
            log.debug("Unable to resolve staticGlowWormIp={} while checking subnet match", targetIp);
            return false;
        }
        // Split the CIDR prefix into full bytes plus any remaining bits of the next byte.
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        for (int index = 0; index < fullBytes; index++) {
            if (localAddress[index] != targetAddress[index]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = (0xFF << (8 - remainingBits)) & 0xFF;
        boolean sameSubnet = (localAddress[fullBytes] & mask) == (targetAddress[fullBytes] & mask);
        log.debug("Subnet match check localIp={}, targetIp={}, prefixLength={}, result={}",
                interfaceAddress.getAddress().getHostAddress(), targetIp, prefixLength, sameSubnet);
        return sameSubnet;
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
                            broadCastPing = getDatagramPacket(Constants.UDP_DEVICE_NAME, MainSingleton.getInstance().config.getOutputDevice(), interfaceAddress.getBroadcast());
                        } else {
                            broadCastPing = getDatagramPacket(Constants.UDP_DEVICE_NAME_STATIC, MainSingleton.getInstance().config.getOutputDevice(), InetAddress.getByName(MainSingleton.getInstance().config.getStaticGlowWormIp()));
                        }
                        log.trace("Sending UDP device-name packet from interfaceIp={} to targetIp={} payloadPrefix={}",
                                interfaceAddress.getAddress() != null ? interfaceAddress.getAddress().getHostAddress() : "unknown",
                                broadCastPing.getAddress() != null ? broadCastPing.getAddress().getHostAddress() : "unknown",
                                useBroadcast ? Constants.UDP_DEVICE_NAME : Constants.UDP_DEVICE_NAME_STATIC);
                        socket.send(broadCastPing);
                        for (Map.Entry<String, Satellite> sat : MainSingleton.getInstance().config.getSatellites().entrySet()) {
                            broadCastPing = getDatagramPacket(Constants.UDP_DEVICE_NAME_STATIC, sat.getValue().getDeviceIp(), InetAddress.getByName(sat.getValue().getDeviceIp()));
                            log.trace("Sending UDP satellite device-name packet from interfaceIp={} to satelliteIp={}",
                                    interfaceAddress.getAddress() != null ? interfaceAddress.getAddress().getHostAddress() : "unknown",
                                    sat.getValue().getDeviceIp());
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
                        broadCastPing = getDatagramPacket(Constants.UDP_PING, interfaceAddress.getBroadcast().toString().substring(1), interfaceAddress.getBroadcast());
                    } else {
                        broadCastPing = getDatagramPacket(Constants.UDP_PING, interfaceAddress.getAddress().toString().substring(1), InetAddress.getByName(MainSingleton.getInstance().config.getStaticGlowWormIp()));
                    }
                    log.trace("Sending UDP ping from interfaceIp={} to targetIp={} mode={}",
                            interfaceAddress.getAddress() != null ? interfaceAddress.getAddress().getHostAddress() : "unknown",
                            broadCastPing.getAddress() != null ? broadCastPing.getAddress().getHostAddress() : "unknown",
                            useBroadcast ? "broadcast" : "static-ip");
                    socket.send(broadCastPing);
                    for (Map.Entry<String, Satellite> sat : MainSingleton.getInstance().config.getSatellites().entrySet()) {
                        broadCastPing = getDatagramPacket(Constants.UDP_PING, interfaceAddress.getAddress().toString().substring(1), InetAddress.getByName(sat.getValue().getDeviceIp()));
                        log.trace("Sending UDP ping from interfaceIp={} to satelliteIp={}",
                                interfaceAddress.getAddress() != null ? interfaceAddress.getAddress().getHostAddress() : "unknown",
                                sat.getValue().getDeviceIp());
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
     * @param deviceIp     device IP for communication
     * @return datagram packet to use for sending the msg
     */
    private DatagramPacket getDatagramPacket(String prefix, String outputDevice, InetAddress deviceIp) {
        byte[] bufferBroadcastPing;
        DatagramPacket broadCastPing;
        String udpMsg;
        udpMsg = (prefix + outputDevice);
        bufferBroadcastPing = udpMsg.getBytes();
        log.trace(udpMsg);
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
            log.trace("Sharing to instance 2 ={}", received);
        }
        if (!MainSingleton.getInstance().config.isMultiScreenSingleDevice() && MainSingleton.getInstance().whoAmI == 1 && MainSingleton.getInstance().config.getMultiMonitor() == 3) {
            shareBroadCastToOtherInstance(received.getBytes(), Constants.UDP_BROADCAST_PORT_3);
            log.trace("Sharing to instance 3 ={}", received);
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
        try {
            for (InterfaceAddress interfaceAddress : eligibleInterfaceAddresses) {
                InetAddress broadcastAddress = interfaceAddress.getBroadcast();
                if (broadcastAddress == null) {
                    log.debug("Skipping UDP relay on interfaceIp={} because no broadcast address is available",
                            interfaceAddress.getAddress() != null ? interfaceAddress.getAddress().getHostAddress() : "unknown");
                    continue;
                }
                DatagramPacket broadCastPing = new DatagramPacket(bufferBroadcastPing, bufferBroadcastPing.length,
                        broadcastAddress, broadcastPort);
                log.debug("Relaying UDP payload to broadcastIp={} port={}", broadcastAddress.getHostAddress(), broadcastPort);
                socket.send(broadCastPing);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
