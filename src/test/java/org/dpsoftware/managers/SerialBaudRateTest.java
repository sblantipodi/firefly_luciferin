/*
  SerialBaudRateTest.java

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

import com.fazecast.jSerialComm.SerialPort;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.LabelKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SerialBaudRateTest {

    /**
     * Verify that the parser recognizes both firmware status codes and startup baud rate values.
     */
    @Test
    void readsFirmwareStatusAndStartupBaudRates() {
        assertEquals(Enums.BaudRate.BAUD_RATE_500000, SerialManager.reportedBaudRate("baudrate:3"));
        assertEquals(Enums.BaudRate.BAUD_RATE_115200, SerialManager.reportedBaudRate("baudrate:8"));
        assertEquals(Enums.BaudRate.BAUD_RATE_6000000, SerialManager.reportedBaudRate("baudrate:10"));
        assertEquals(Enums.BaudRate.BAUD_RATE_921600, SerialManager.reportedBaudRate("BAUDRATE IN USE=921600"));
    }

    /**
     * Verify that unrelated messages, malformed status lines, and unsupported rates are ignored.
     */
    @Test
    void ignoresUnrelatedMessagesAndUnsupportedRates() {
        assertNull(SerialManager.reportedBaudRate("rst:0x1 (POWERON_RESET)"));
        assertNull(SerialManager.reportedBaudRate("DEBUG baudrate:3"));
        assertNull(SerialManager.reportedBaudRate("baudrate:3[garbled]"));
        assertNull(SerialManager.reportedBaudRate("baudrate:14"));
        assertNull(SerialManager.reportedBaudRate("BAUDRATE IN USE=74880"));
    }

    /**
     * Verify that each supported language supplies the scan and mismatch alerts with their message placeholders.
     *
     * @throws IOException if a language bundle cannot be read
     */
    @Test
    void definesBaudRateAlertsInEverySupportedLanguage() throws IOException {
        for (Enums.Language language : Enums.Language.values()) {
            String resource = "/messagebundle_" + language.name().toLowerCase(Locale.ROOT) + ".properties";
            try (var stream = getClass().getResourceAsStream(resource)) {
                assertNotNull(stream, resource);
                Properties bundle = new Properties();
                bundle.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
                assertNotNull(bundle.getProperty(LabelKey.BAUDRATE_MISMATCH_TITLE), resource);
                String message = bundle.getProperty(LabelKey.BAUDRATE_MISMATCH_MESSAGE);
                assertNotNull(message, resource);
                assertTrue(message.contains("{0}") && message.contains("{1}") && message.contains("{2}"), resource);
                assertNotNull(bundle.getProperty(LabelKey.BAUDRATE_SCAN_TITLE), resource);
                String scanMessage = bundle.getProperty(LabelKey.BAUDRATE_SCAN_MESSAGE);
                assertNotNull(scanMessage, resource);
                assertTrue(scanMessage.contains("{0}"), resource);
            }
        }
    }

    /**
     * Verify that the notice appears before the first alternative rate and clears on disconnect.
     *
     * @param rejected whether the driver immediately rejects the configured baud rate
     * @throws ReflectiveOperationException if the probe cannot be exercised directly
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void showsTheScanNoticeOnlyWhenTryingAlternatives(boolean rejected) throws ReflectiveOperationException {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.config.setFullFirmware(false);
        main.config.setBaudRate("115200");
        main.bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, Locale.ENGLISH);
        SerialPort port = mock(SerialPort.class);
        when(port.getSystemPortName()).thenReturn("COM14");
        when(port.setComPortParameters(115200, 8, 1, SerialPort.NO_PARITY)).thenAnswer(_ -> {
            assertNull(SerialManager.getBaudRateScanNotice());
            return !rejected;
        });
        when(port.setComPortParameters(500000, 8, 1, SerialPort.NO_PARITY)).thenAnswer(_ -> {
            assertTrue(SerialManager.getBaudRateScanNotice().contains("COM14"));
            return true;
        });
        try (var singleton = mockStatic(MainSingleton.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            SerialManager manager = new SerialManager();
            try {
                var probeClass = Class.forName(SerialManager.class.getName() + "$BaudRateProbe");
                var constructor = probeClass.getDeclaredConstructor(SerialManager.class, SerialPort.class, long.class);
                constructor.setAccessible(true);
                // Use a superseded request so the scheduled callback cannot race the test.
                Object probe = constructor.newInstance(manager, port, Long.MIN_VALUE);
                var tryNextRate = probeClass.getDeclaredMethod("tryNextRate");
                tryNextRate.setAccessible(true);
                tryNextRate.invoke(probe);
                if (!rejected) {
                    assertNull(SerialManager.getBaudRateScanNotice());
                    tryNextRate.invoke(probe);
                }
                assertTrue(SerialManager.getBaudRateScanNotice().contains("two minutes"));
            } finally {
                manager.closeSerial();
            }
            assertNull(SerialManager.getBaudRateScanNotice());
        }
    }
}
