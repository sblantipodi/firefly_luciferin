/*
  JmDnsLegacyQueryTest.java

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

import org.junit.jupiter.api.Test;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies Android-style one-shot DNS queries using the real JmDNS socket listener.
 */
class JmDnsLegacyQueryTest {
    /**
     * Sends an Android-style query without the QU bit and verifies its unicast IPv4 answer.
     *
     * @throws Exception when the local responder or DNS exchange fails
     */
    @Test
    void queryFromEphemeralPortWithoutQuBitReceivesUnicastAnswer() throws Exception {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        String name = "firefly-test-" + UUID.randomUUID();
        try (JmDNS dns = JmDNS.create(address, name);
             DatagramSocket client = new DatagramSocket(0, address)) {
            dns.registerService(ServiceInfo.create("_http._tcp.local.", name, 8091, "path=/"));
            assertTrue(((JmDNSImpl) dns).getLocalHost().waitForAnnounced(5000), "Responder did not finish probing");
            assertNotEquals(5353, client.getLocalPort());
            client.setSoTimeout(3000);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream query = new DataOutputStream(bytes)) {
                query.writeShort(0x1234); // Legacy DNS transaction ID must be echoed.
                query.writeShort(0);
                query.writeShort(1);
                query.writeShort(0);
                query.writeShort(0);
                query.writeShort(0);
                for (String label : dns.getHostName().split("\\.")) {
                    byte[] encoded = label.getBytes(StandardCharsets.US_ASCII);
                    query.writeByte(encoded.length);
                    query.write(encoded);
                }
                query.writeByte(0);
                query.writeShort(1); // A record.
                query.writeShort(1); // IN class, deliberately without the QU bit.
            }
            byte[] request = bytes.toByteArray();
            client.send(new DatagramPacket(request, request.length, address, 5353));
            DatagramPacket response = new DatagramPacket(new byte[1500], 1500);
            client.receive(response); // A multicast-only response cannot reach this ephemeral socket.

            assertEquals(5353, response.getPort());
            assertEquals(address, response.getAddress());
            assertEquals(0x1234, Short.toUnsignedInt(ByteBuffer.wrap(response.getData()).getShort()));
            DNSIncoming answer = new DNSIncoming(response);
            assertTrue(answer.isResponse());
            assertEquals(1, answer.getQuestions().size());
            assertTrue(answer.getAnswers().stream().anyMatch(record ->
                            record instanceof DNSRecord.IPv4Address host
                                    && host.getName().equalsIgnoreCase(dns.getHostName())
                                    && Arrays.asList(host.getServiceInfo(false).getInet4Addresses()).contains(address)),
                    "Missing IPv4 answer for the advertised hostname");
        }
    }
}
