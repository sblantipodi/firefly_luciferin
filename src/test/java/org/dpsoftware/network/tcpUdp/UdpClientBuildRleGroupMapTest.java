/*
  UdpClientBuildRleGroupMapTest.java

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

  You should have received a copy of the GNU Lesser General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware.network.tcpUdp;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UdpClient#buildRleGroupMap(int, List)}.
 * Tests the RLE group map string construction logic.
 */
class UdpClientBuildRleGroupMapTest {

    private UdpClient client;
    private MockedStatic<MainSingleton> mockedMainSingleton;

    @BeforeEach
    void setUp() throws SocketException, UnknownHostException {
        // UdpClient constructor calls setTrafficClass() which reads MainSingleton.config
        MainSingleton fakeInstance = new MainSingleton();
        fakeInstance.config = new Configuration();
        fakeInstance.config.setUdpTrafficClass(0);
        mockedMainSingleton = Mockito.mockStatic(MainSingleton.class);
        mockedMainSingleton.when(MainSingleton::getInstance).thenReturn(fakeInstance);

        client = new UdpClient("127.0.0.1");
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (mockedMainSingleton != null) {
            mockedMainSingleton.close();
        }
    }

    @Test
    void buildRleGroupMap_singleEntry() {
        List<int[]> entries = new ArrayList<>();
        entries.add(new int[]{10, 3});

        String result = client.buildRleGroupMap(30, entries);

        assertEquals("DPsoftwareGRP,30,1,10x3", result);
    }

    @Test
    void buildRleGroupMap_multipleEntries() {
        List<int[]> entries = new ArrayList<>();
        entries.add(new int[]{7, 2});
        entries.add(new int[]{1, 3});
        entries.add(new int[]{42, 2});
        entries.add(new int[]{1, 3});
        entries.add(new int[]{7, 2});

        String result = client.buildRleGroupMap(60, entries);

        assertEquals("DPsoftwareGRP,60,5,7x2,1x3,42x2,1x3,7x2", result);
    }

    @Test
    void buildRleGroupMap_emptyListReturnsEmptyString() {
        String result = client.buildRleGroupMap(10, Collections.emptyList());
        assertEquals("", result);
    }

    @Test
    void buildRleGroupMap_nullListReturnsEmptyString() {
        String result = client.buildRleGroupMap(10, null);
        assertEquals("", result);
    }

    @Test
    void buildRleGroupMap_singleLedSingleEntry() {
        List<int[]> entries = new ArrayList<>();
        entries.add(new int[]{1, 1});

        String result = client.buildRleGroupMap(1, entries);

        assertEquals("DPsoftwareGRP,1,1,1x1", result);
    }

    @Test
    void buildRleGroupMap_largeNumberOfEntries() {
        List<int[]> entries = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            entries.add(new int[]{i + 1, 1});
        }

        String result = client.buildRleGroupMap(500, entries);

        assertTrue(result.startsWith("DPsoftwareGRP,500,100,"));
        assertTrue(result.contains("1x1,2x1"));
        assertTrue(result.endsWith("100x1"));
    }

    @Test
    void buildRleGroupMap_entryWithLargeCount() {
        List<int[]> entries = new ArrayList<>();
        entries.add(new int[]{255, 255});

        String result = client.buildRleGroupMap(500, entries);

        assertEquals("DPsoftwareGRP,500,1,255x255", result);
    }

    @Test
    void buildRleGroupMap_noTrailingComma() {
        List<int[]> entries = new ArrayList<>();
        entries.add(new int[]{3, 4});
        entries.add(new int[]{5, 6});

        String result = client.buildRleGroupMap(20, entries);

        assertFalse(result.endsWith(","), "Should not have trailing comma");
        assertEquals("DPsoftwareGRP,20,2,3x4,5x6", result);
    }
}
