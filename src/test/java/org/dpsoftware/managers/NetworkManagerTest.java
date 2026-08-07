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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link NetworkManager#isValidIp(String)}.
 */
class NetworkManagerTest {

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
