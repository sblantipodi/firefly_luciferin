/*
  UdpClient.java

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

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * UDP Client to manage UDP wireless stream, this is an alternative to MQTT stream
 */
@Slf4j
public class UdpClient {

    public final DatagramSocket socket;
    final int UDP_PORT = Constants.UDP_PORT;
    private final InetAddress address;

    /**
     * UDP constructor for the socket
     *
     * @param deviceIP device IP
     */
    public UdpClient(String deviceIP) throws SocketException, UnknownHostException {
        address = InetAddress.getByName(deviceIP);
        socket = createSocket(address);
        socket.setSendBufferSize(Constants.UDP_MAX_BUFFER_SIZE);
        setTrafficClass();
    }

    /**
     * Create a UDP socket bound to the local interface that can best reach the target device.
     *
     * @param targetAddress device IP address
     * @return UDP socket
     * @throws SocketException when the socket cannot be created
     */
    private DatagramSocket createSocket(InetAddress targetAddress) throws SocketException {
        InetAddress localBindAddress = findLocalAddressForTarget(targetAddress);
        if (localBindAddress != null) {
            DatagramSocket datagramSocket = new DatagramSocket(new InetSocketAddress(localBindAddress, 0));
            log.info("UDP stream socket bound to localIp={} for targetIp={}",
                    localBindAddress.getHostAddress(), targetAddress.getHostAddress());
            return datagramSocket;
        }
        log.info("UDP stream socket using OS-selected interface for targetIp={}", targetAddress.getHostAddress());
        return new DatagramSocket();
    }

    /**
     * Find the local IPv4 address that belongs to the same subnet as the target device.
     *
     * @param targetAddress device IP address
     * @return matching local IPv4 address, or null if none is found
     */
    private InetAddress findLocalAddressForTarget(InetAddress targetAddress) throws SocketException {
        if (!(targetAddress instanceof Inet4Address) || !NetworkManager.isValidIp(targetAddress.getHostAddress())) {
            log.debug("UDP stream targetIp={} is not a valid IPv4 candidate for local binding", targetAddress.getHostAddress());
            return null;
        }
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            log.debug("Inspecting UDP stream interface name={}, displayName={}, up={}, loopback={}, virtual={}",
                    networkInterface.getName(), networkInterface.getDisplayName(), networkInterface.isUp(),
                    networkInterface.isLoopback(), networkInterface.isVirtual());
            if (!isEligibleNetworkInterface(networkInterface)) {
                log.debug("Skipping UDP stream interface {} because it is not eligible", networkInterface.getDisplayName());
                continue;
            }
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress localAddress = interfaceAddress.getAddress();
                if (!(localAddress instanceof Inet4Address)) {
                    continue;
                }
                boolean sameSubnet = isAddressInSameSubnet(interfaceAddress, targetAddress.getHostAddress());
                log.debug("UDP stream subnet check localIp={}, targetIp={}, prefixLength={}, result={}",
                        localAddress.getHostAddress(), targetAddress.getHostAddress(), interfaceAddress.getNetworkPrefixLength(), sameSubnet);
                if (sameSubnet) {
                    log.debug("Selected UDP stream interface {} with localIp={} for targetIp={}",
                            networkInterface.getDisplayName(), localAddress.getHostAddress(), targetAddress.getHostAddress());
                    return localAddress;
                }
            }
        }
        return null;
    }

    private boolean interfaceMatchesExcludedKeywords(NetworkInterface networkInterface) {
        String interfaceName = networkInterface.getName() != null ? networkInterface.getName().toLowerCase() : "";
        String displayName = networkInterface.getDisplayName() != null ? networkInterface.getDisplayName().toLowerCase() : "";
        return Enums.InterfaceToExclude.contains(interfaceName) || Enums.InterfaceToExclude.contains(displayName);
    }

    /**
     * Validate a network interface before using it for UDP streaming.
     *
     * @param networkInterface interface to check
     * @return true if the interface can be used
     */
    private boolean isEligibleNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        if (!networkInterface.isUp()) {
            return false;
        }
        if (networkInterface.isLoopback() || networkInterface.isVirtual() || networkInterface.isPointToPoint()) {
            return false;
        }
        return !interfaceMatchesExcludedKeywords(networkInterface);
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
            return false;
        }
        short prefixLength = interfaceAddress.getNetworkPrefixLength();
        if (prefixLength < 0 || prefixLength > 32) {
            return false;
        }
        byte[] localAddress = interfaceAddress.getAddress().getAddress();
        byte[] targetAddress;
        try {
            targetAddress = InetAddress.getByName(targetIp).getAddress();
        } catch (UnknownHostException e) {
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
        return (localAddress[fullBytes] & mask) == (targetAddress[fullBytes] & mask);
    }

    /**
     * UDP priority is really important for latency.
     * Linux firewalls may block "unusual" priority classes
     * | Traffic class               | Decimal value   | Hex value          |
     * |-----------------------------|-----------------|--------------------|
     * | Network Control             | 56              | 0x38               |
     * | Internetwork Control        | 48              | 0x30               |
     * | Expedited Forwarding        | 46              | 0x2E               |
     * | Voice, less than 10ms       | 40              | 0x28               |
     * | Video, less than 100ms      | 32              | 0x20               |
     * | Assured Forwarding Class 4  | 34              | 0x22               |
     * | Assured Forwarding Class 3  | 26              | 0x1A               |
     * | Assured Forwarding Class 2  | 18              | 0x12               |
     * | Critical Applications       | 24              | 0x18               |
     * | Assured Forwarding Class 1  | 10              | 0x0A               |
     * | Excellent Effort            | 16              | 0x10               |
     * | Background                  | 8               | 0x08               |
     */
    private void setTrafficClass() throws SocketException {
        socket.setTrafficClass(MainSingleton.getInstance().config.getUdpTrafficClass());
    }

    /**
     * Send message
     *
     * @param msg to send
     */
    public void sendUdpStream(String msg) {
        byte[] buf = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, UDP_PORT);
        try {
            log.trace("Sending UDP stream packet from localPort={} to targetIp={} targetPort={}",
                    socket.getLocalPort(), address.getHostAddress(), UDP_PORT);
            socket.send(packet);
        } catch (IOException e) {
            socket.close();
            log.error(e.getMessage());
        }
    }

    /**
     * Organize led data and send it via UDP stream
     *
     * @param leds array containing color information
     */
    public void manageStream(Color[] leds) {
        int chunkTotal;
        chunkTotal = (int) Math.ceil(leds.length / Constants.UDP_CHUNK_SIZE);
        for (int chunkNum = 0; chunkNum < chunkTotal; chunkNum++) {
            StringBuilder sb = new StringBuilder();
            sb.append("DPsoftware").append(",");
            sb.append(leds.length).append(",");
            sb.append((AudioSingleton.getInstance().AUDIO_BRIGHTNESS == 255 ? CommonUtility.getNightBrightness() : AudioSingleton.getInstance().AUDIO_BRIGHTNESS)).append(",");
            sb.append(chunkTotal).append(",");
            sb.append(chunkNum).append(",");
            int chunkSizeInteger = (int) Constants.UDP_CHUNK_SIZE * chunkNum;
            int nextChunk = (int) (chunkSizeInteger + Constants.UDP_CHUNK_SIZE);
            Color[] ledChunk = Arrays.copyOfRange(leds, chunkSizeInteger, Math.min(nextChunk, leds.length));
            for (int ledIndex = 0; ledIndex < ledChunk.length; ledIndex++) {
                sb.append(ledChunk[ledIndex].getRGB());
                if (ledIndex < ledChunk.length - 1) {
                    sb.append(",");
                }
            }
            sendUdpStream(sb.toString());
            // Let the microcontroller rest for 1 milliseconds before next stream
            if (Constants.UDP_MICROCONTROLLER_REST_TIME > 0) {
                CommonUtility.sleepMilliseconds(Constants.UDP_MICROCONTROLLER_REST_TIME);
            }
        }
    }

    /**
     * Close stream
     */
    public void close() {
        socket.close();
    }

}
