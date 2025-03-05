/*
  UdpClient.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

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
        socket = new DatagramSocket();
        // TODO
        socket.setSendBufferSize(Constants.UDP_MAX_BUFFER_SIZE);
        socket.setReceiveBufferSize(Constants.UDP_MAX_BUFFER_SIZE);
        socket.setSoTimeout(500);
        setTrafficClass();
        address = InetAddress.getByName(deviceIP);
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
            socket.send(packet);
        } catch (IOException e) {
            // TODO
            log.error("DADAD");
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
