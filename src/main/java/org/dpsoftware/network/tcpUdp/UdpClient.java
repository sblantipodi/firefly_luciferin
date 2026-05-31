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
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.List;

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
     * Find the local IPv4 address that the OS routing table would use for the target device.
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
                boolean routedViaInterface = isReachableViaInterface(localAddress, targetAddress);
                log.debug("UDP stream routing check localIp={}, targetIp={}, result={}",
                        localAddress.getHostAddress(), targetAddress.getHostAddress(), routedViaInterface);
                if (routedViaInterface) {
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
     * Check if a target IP is routable via a specific local interface address.
     *
     * @param localAddress  local interface address to bind to
     * @param targetAddress target IP address
     * @return true if the OS routes the target through the local interface
     */
    private boolean isReachableViaInterface(InetAddress localAddress, InetAddress targetAddress) {
        if (localAddress == null || targetAddress == null || !NetworkManager.isValidIp(targetAddress.getHostAddress())) {
            return false;
        }
        try (DatagramSocket testSocket = new DatagramSocket(null)) {
            testSocket.bind(new InetSocketAddress(localAddress, 0));
            testSocket.connect(new InetSocketAddress(targetAddress, UDP_PORT));
            InetAddress routedVia = testSocket.getLocalAddress();
            return routedVia != null && routedVia.equals(localAddress);
        } catch (Exception e) {
            log.debug("UDP stream routing check failed for localIp={} targetIp={}: {}",
                    localAddress.getHostAddress(), targetAddress.getHostAddress(), e.getMessage());
            return false;
        }
    }

    /**
     * UDP priority is really important for latency.
     * Linux firewalls may block "unusual" priority classes
     * |------------------------------ |----------|-----------|------------------|------------------|
     * | QoS Class                     | DSCP Dec | DSCP Hex  | TrafficClass Dec | TrafficClass Hex |
     * |------------------------------ |----------|-----------|------------------|------------------|
     * | Network Control              | 56       | 0x38      | 224              | 0xE0             |
     * | Internetwork Control         | 48       | 0x30      | 192              | 0xC0             |
     * | Expedited Forwarding (EF)    | 46       | 0x2E      | 184              | 0xB8             |
     * | Voice, less than 10ms        | 40       | 0x28      | 160              | 0xA0             |
     * | Video, less than 100ms       | 32       | 0x20      | 128              | 0x80             |
     * | Assured Forwarding Class 4   | 34       | 0x22      | 136              | 0x88             |
     * | Assured Forwarding Class 3   | 26       | 0x1A      | 104              | 0x68             |
     * | Assured Forwarding Class 2   | 18       | 0x12      | 72               | 0x48             |
     * | Critical Applications        | 24       | 0x18      | 96               | 0x60             |
     * | Assured Forwarding Class 1   | 10       | 0x0A      | 40               | 0x28             |
     * | Excellent Effort             | 16       | 0x10      | 64               | 0x40             |
     * | Background                   | 8        | 0x08      | 32               | 0x20             |
     */
    private void setTrafficClass() {
        try {
            socket.setTrafficClass(MainSingleton.getInstance().config.getUdpTrafficClass());
        } catch (SocketException e) {
            log.warn("Cannot set UDP traffic class {}, (not supported on this OS/NIC): {}", MainSingleton.getInstance().config.getUdpTrafficClass(), e.getMessage());
        }
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
        LinkedHashMap<Integer, LEDCoordinate> ledMatrix = MainSingleton.getInstance().config
                .getLedMatrixInUse(MainSingleton.getInstance().config.getDefaultLedMatrix());

        // Build compressed LED array (only capogruppo)
        List<Color> leaderColors = new ArrayList<>();
        for (LEDCoordinate coord : ledMatrix.values()) {
            if (!coord.isGroupedLed()) {
                leaderColors.add(leds[leaderColors.size()]);
            }
        }
        // Fix: iterate with index to map correctly
        leaderColors.clear();
        int ledIndex = 0;
        for (LEDCoordinate coord : ledMatrix.values()) {
            if (!coord.isGroupedLed()) {
                leaderColors.add(leds[ledIndex]);
            }
            ledIndex++;
        }

        Color[] compressedLeds = leaderColors.toArray(new Color[0]);
        String rleMap = buildRleGroupMap(MainSingleton.getInstance().config,
                MainSingleton.getInstance().config.getDefaultLedMatrix());
        // Extract just the RLE entries part (after "DPsoftwareGRP,118,5,")
        String rleEntries = rleMap.substring(rleMap.indexOf(',', rleMap.indexOf(',') + 1) + 1); // skip header and numLedsPhysical
        // Simpler: rebuild just the "numRleEntries,e1,e2,..." part
        String[] rleparts = rleMap.split(",", 4);
        String rleInline = rleparts[2] + "," + rleparts[3]; // "5,7x2,1x3,42x2,1x3,7x2"

        int numLedsPhysical = leds.length;
        int chunkTotal = (int) Math.ceil(compressedLeds.length / Constants.UDP_CHUNK_SIZE);

        for (int chunkNum = 0; chunkNum < chunkTotal; chunkNum++) {
            StringBuilder sb = new StringBuilder();
            sb.append("DPsoftware").append(",");
            sb.append(numLedsPhysical).append(",");
            sb.append((AudioSingleton.getInstance().AUDIO_BRIGHTNESS == 255 ? CommonUtility.getNightBrightness() : AudioSingleton.getInstance().AUDIO_BRIGHTNESS)).append(",");
            sb.append(chunkTotal).append(",");
            sb.append(chunkNum).append(",");
            // RLE map only in first chunk
            if (chunkNum == 0) {
                sb.append(rleInline).append(",");
            }
            int chunkSizeInteger = (int) Constants.UDP_CHUNK_SIZE * chunkNum;
            int nextChunk = (int) (chunkSizeInteger + Constants.UDP_CHUNK_SIZE);
            Color[] ledChunk = Arrays.copyOfRange(compressedLeds, chunkSizeInteger, Math.min(nextChunk, compressedLeds.length));
            for (int i = 0; i < ledChunk.length; i++) {
                sb.append(ledChunk[i].getRGB());
                if (i < ledChunk.length - 1) sb.append(",");
            }
            sendUdpStream(sb.toString());

            log.debug(sb.toString());
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

    /**
     * Builds a compact RLE (Run-Length Encoding) group map from the LED matrix for a given aspect ratio.
     * The map is used by the ESP32 to expand compressed color streams.
     * Format: "DPsoftwareGRP,<numLedsPhysical>,<numRleEntries>,<count1>x<size1>,..."
     *
     * @param config      the current configuration containing the LED matrix
     * @param aspectRatio the aspect ratio key to use (e.g. "FullScreen")
     * @return the RLE-encoded group map string ready to send via UDP
     */
    public String buildRleGroupMap(Configuration config, String aspectRatio) {
        LinkedHashMap<Integer, LEDCoordinate> ledMatrix = config.getLedMatrix().get(aspectRatio);
        if (ledMatrix == null || ledMatrix.isEmpty()) {
            return "";
        }

        // Step 1: build flat groupSize array (one entry per capogruppo)
        List<Integer> groupSizes = new ArrayList<>();
        int currentGroupSize = 0;
        for (LEDCoordinate led : ledMatrix.values()) {
            if (!led.isGroupedLed()) {
                if (currentGroupSize > 0) {
                    groupSizes.add(currentGroupSize);
                }
                currentGroupSize = 1;
            } else {
                currentGroupSize++;
            }
        }
        if (currentGroupSize > 0) {
            groupSizes.add(currentGroupSize);
        }

        // Step 2: RLE encode the flat array
        List<int[]> rle = new ArrayList<>();
        int i = 0;
        while (i < groupSizes.size()) {
            int size = groupSizes.get(i);
            int count = 0;
            while (i < groupSizes.size() && groupSizes.get(i) == size) {
                count++;
                i++;
            }
            rle.add(new int[]{count, size});
        }

        // Step 3: build the UDP payload string
        int numLedsPhysical = ledMatrix.size();
        StringBuilder sb = new StringBuilder();
        sb.append("DPsoftwareGRP").append(",");
        sb.append(numLedsPhysical).append(",");
        sb.append(rle.size()).append(",");
        for (int j = 0; j < rle.size(); j++) {
            sb.append(rle.get(j)[0]).append("x").append(rle.get(j)[1]);
            if (j < rle.size() - 1) sb.append(",");
        }

        log.debug("RLE Group Map: {}", sb);
        return sb.toString();
    }

}
