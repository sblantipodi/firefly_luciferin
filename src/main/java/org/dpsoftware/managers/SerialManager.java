/*
  SerialManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini  (https://github.com/sblantipodi)

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

import gnu.io.*;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.audio.AudioLoopback;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GUIManager;
import org.dpsoftware.gui.controllers.DevicesTabController;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.dpsoftware.FireflyLuciferin.config;
import static org.dpsoftware.FireflyLuciferin.serial;

/**
 * Serial port utility
 */
@Slf4j
public class SerialManager {

    private BufferedReader input;

    /**
     * Initialize Serial communication
     */
    public void initSerial(FireflyLuciferin fireflyLuciferin) {
        CommPortIdentifier serialPortId = null;
        if (!config.isMqttStream()) {
            int numberOfSerialDevices = 0;
            var enumComm = CommPortIdentifier.getPortIdentifiers();
            while (enumComm.hasMoreElements()) {
                numberOfSerialDevices++;
                CommPortIdentifier serialPortAvailable = (CommPortIdentifier) enumComm.nextElement();
                if (config.getSerialPort().equals(serialPortAvailable.getName()) || config.getSerialPort().equals(Constants.SERIAL_PORT_AUTO)) {
                    serialPortId = serialPortAvailable;
                }
            }
            try {
                if (serialPortId != null) {
                    log.debug(CommonUtility.getWord(Constants.SERIAL_PORT_IN_USE) + serialPortId.getName() + ", connecting...");
                    serial = serialPortId.open(fireflyLuciferin.getClass().getName(), config.getTimeout());
                    serial.setSerialPortParams(Integer.parseInt(config.getBaudRate()), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    input = new BufferedReader(new InputStreamReader(serial.getInputStream()));
                    // add event listeners
                    serial.addEventListener(fireflyLuciferin);
                    serial.notifyOnDataAvailable(true);
                    DevicesTabController.deviceTableData.add(new GlowWormDevice(Constants.USB_DEVICE, serialPortId.getName(),
                            Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH,
                            FireflyLuciferin.formatter.format(new Date()), Constants.DASH,  Constants.DASH, Constants.DASH, Constants.DASH));
                    GUIManager guiManager = new GUIManager();
                    if (numberOfSerialDevices > 1 && config.getSerialPort().equals(Constants.SERIAL_PORT_AUTO)) {
                        FireflyLuciferin.communicationError = true;
                        guiManager.showLocalizedAlert(Constants.SERIAL_ERROR_TITLE, Constants.SERIAL_PORT_AMBIGUOUS,
                                Constants.SERIAL_PORT_AMBIGUOUS_CONTEXT, Alert.AlertType.ERROR);
                        log.error(Constants.SERIAL_ERROR_OPEN_HEADER);
                    }
                    log.debug("Connected: Serial " + serialPortId.getName());
                    if (FireflyLuciferin.guiManager != null) {
                        FireflyLuciferin.guiManager.trayIconManager.resetTray();
                    }
                    FireflyLuciferin.serialConnected = true;
                    FireflyLuciferin.communicationError = false;
                    initOutputStream();
                }
            } catch (PortInUseException | UnsupportedCommOperationException | NullPointerException | IOException |
                     TooManyListenersException e) {
                log.error(e.getMessage());
                FireflyLuciferin.communicationError = true;
            }
        }
    }

    /**
     * Send color info via USB Serial
     * @param leds array with colors
     * @throws IOException can't write to serial
     */
    public void sendColorsViaUSB(Color[] leds) throws IOException {
        // Effect is set via MQTT when using Full Firmware
        if (config.isWifiEnable()) {
            FireflyLuciferin.fireflyEffect = 100;
        } else {
            for (Constants.Effect ef : Constants.Effect.values()) {
                if(ef.getBaseI18n().equals(config.getEffect())) {
                    FireflyLuciferin.fireflyEffect = ef.ordinal() + 1;
                }
            }
        }
        if (!UpgradeManager.serialVersionOk) {
            UpgradeManager upgradeManager = new UpgradeManager();
            // Check if the connected device match the minimum firmware version requirements for this Firefly Luciferin version
            Boolean firmwareMatchMinRequirements = upgradeManager.firmwareMatchMinimumRequirements();
            if (firmwareMatchMinRequirements != null) {
                if (firmwareMatchMinRequirements) {
                    UpgradeManager.serialVersionOk = true;
                }
            }
        } else {
            int i = 0, j = -1;
            byte[] ledsArray = new byte[(FireflyLuciferin.ledNumber * 3) + 16];
            // DPsoftware checksum
            int ledsCountHi = ((FireflyLuciferin.ledNumHighLowCount) >> 8) & 0xff;
            int ledsCountLo = (FireflyLuciferin.ledNumHighLowCount) & 0xff;
            int loSecondPart = (FireflyLuciferin.ledNumHighLowCountSecondPart) & 0xff;
            int brightnessToSend = (AudioLoopback.AUDIO_BRIGHTNESS == 255 ? CommonUtility.getNightBrightness() : AudioLoopback.AUDIO_BRIGHTNESS) & 0xff;
            int gpioToSend = (FireflyLuciferin.gpio) & 0xff;
            int baudRateToSend = (FireflyLuciferin.baudRate) & 0xff;
            int whiteTempToSend = (config.getWhiteTemperature()) & 0xff;
            int fireflyEffectToSend = (FireflyLuciferin.fireflyEffect) & 0xff;
            int colorModeToSend = (config.getColorMode()) & 0xff;

            ledsArray[++j] = (byte) ('D');
            ledsArray[++j] = (byte) ('P');
            ledsArray[++j] = (byte) ('s');
            ledsArray[++j] = (byte) ('o');
            ledsArray[++j] = (byte) ('f');
            ledsArray[++j] = (byte) ('t');
            ledsArray[++j] = (byte) (ledsCountHi);
            ledsArray[++j] = (byte) (ledsCountLo);
            ledsArray[++j] = (byte) (loSecondPart);
            ledsArray[++j] = (byte) (brightnessToSend);
            ledsArray[++j] = (byte) (gpioToSend);
            ledsArray[++j] = (byte) (baudRateToSend);
            ledsArray[++j] = (byte) (whiteTempToSend);
            ledsArray[++j] = (byte) (fireflyEffectToSend);
            ledsArray[++j] = (byte) (colorModeToSend);
            ledsArray[++j] = (byte) ((ledsCountHi ^ ledsCountLo ^ loSecondPart ^ brightnessToSend ^ gpioToSend ^ baudRateToSend ^ whiteTempToSend ^ fireflyEffectToSend ^ colorModeToSend ^ 0x55));

            if (leds.length == 1) {
                FireflyLuciferin.colorInUse = leds[0];
                while (i < FireflyLuciferin.ledNumber) {
                    ledsArray[++j] = (byte) leds[0].getRed();
                    ledsArray[++j] = (byte) leds[0].getGreen();
                    ledsArray[++j] = (byte) leds[0].getBlue();
                    i++;
                }
            } else {
                while (i < FireflyLuciferin.ledNumber) {
                    ledsArray[++j] = (byte) leds[i].getRed();
                    ledsArray[++j] = (byte) leds[i].getGreen();
                    ledsArray[++j] = (byte) leds[i].getBlue();
                    i++;
                }
            }
            FireflyLuciferin.output.write(ledsArray);
        }
    }

    /**
     * Check SOLID LEDs config and refresh LED strip state accordingly
     * This function works with GlowWormLuciferin Light, MQTT version does not need it
     */
    public void manageSolidLed() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (!FireflyLuciferin.RUNNING) {
                if (config.isToggleLed() && !config.isWifiEnable()) {
                    Color[] colorToUse = new Color[1];
                    if (FireflyLuciferin.colorInUse == null) {
                        String[] color = config.getColorChooser().split(",");
                        colorToUse[0] = new Color(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]));
                        config.setBrightness(Integer.parseInt(color[3]));
                    } else {
                        colorToUse[0] = FireflyLuciferin.colorInUse;
                    }
                    try {
                        Constants.Effect effectInUse = LocalizedEnum.fromBaseStr(Constants.Effect.class, config.getEffect());
                        if (Constants.Effect.RAINBOW.equals(effectInUse) || Constants.Effect.FIRE.equals(effectInUse)) {
                            for (int i = 0; i <= 10; i++) {
                                sendColorsViaUSB(colorToUse);
                                CommonUtility.sleepMilliseconds(10);
                            }
                        } else {
                            sendColorsViaUSB(colorToUse);
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        }, 2, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Initialize OutputStream
     */
    public void initOutputStream() {
        if (!config.isMqttStream() && !FireflyLuciferin.communicationError) {
            try {
                FireflyLuciferin.output = serial.getOutputStream();
            } catch (IOException | NullPointerException e) {
                FireflyLuciferin.communicationError = true;
                log.error(e.getMessage());
                log.error(Constants.SERIAL_ERROR_HEADER);
            }
        }
    }

    /**
     * Handle an event on the serial port. Read the data and print it.
     * @param event input event
     */
    public void handleSerialEvent(SerialPortEvent event) {
        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                if (input.ready()) {
                    String inputLine = input.readLine();
                    CommonUtility.conditionedLog(this.getClass().getName(), inputLine);
                    DevicesTabController.deviceTableData.forEach(glowWormDevice -> {
                        if (glowWormDevice.getDeviceName().equals(Constants.USB_DEVICE)) {
                            glowWormDevice.setLastSeen(FireflyLuciferin.formatter.format(new Date()));
                            // Skipping the Setting LED loop from Glow Worm Luciferin Serial communication
                            if (!inputLine.contains(Constants.SETTING_LED_SERIAL)) {
                                if (inputLine.contains(Constants.SERIAL_VERSION)) {
                                    glowWormDevice.setDeviceVersion(inputLine.replace(Constants.SERIAL_VERSION, ""));
                                } else if (inputLine.contains(Constants.SERIAL_LED_NUM)) {
                                    glowWormDevice.setNumberOfLEDSconnected(inputLine.replace(Constants.SERIAL_LED_NUM, ""));
                                } else if (inputLine.contains(Constants.SERIAL_BOARD)) {
                                    glowWormDevice.setDeviceBoard(inputLine.replace(Constants.SERIAL_BOARD, ""));
                                } else if (inputLine.contains(Constants.SERIAL_MAC)) {
                                    glowWormDevice.setMac(inputLine.replace(Constants.SERIAL_MAC, ""));
                                } else if (inputLine.contains(Constants.SERIAL_GPIO)) {
                                    glowWormDevice.setGpio(inputLine.replace(Constants.SERIAL_GPIO, ""));
                                } else if (inputLine.contains(Constants.SERIAL_FIRMWARE)) {
                                    glowWormDevice.setFirmwareType(inputLine.replace(Constants.SERIAL_FIRMWARE, ""));
                                } else if (inputLine.contains(Constants.SERIAL_MQTTTOPIC)) {
                                    glowWormDevice.setMqttTopic(inputLine.replace(Constants.SERIAL_MQTTTOPIC, ""));
                                } else if (inputLine.contains(Constants.SERIAL_COLOR_MODE)) {
                                    glowWormDevice.setColorMode(Constants.ColorMode.values()[Integer.parseInt(inputLine.replace(Constants.SERIAL_COLOR_MODE, "")) - 1].getI18n());
                                } else if (inputLine.contains(Constants.SERIAL_BAUDRATE)) {
                                    log.debug(inputLine);
                                    boolean validBaudrate = true;
                                    int receivedBaudrate = Integer.parseInt(inputLine.replace(Constants.SERIAL_BAUDRATE, ""));
                                    if (!(receivedBaudrate >= 1 && receivedBaudrate <= 8)) {
                                        validBaudrate = false;
                                    }
                                    glowWormDevice.setBaudRate(validBaudrate ? Constants.BaudRate.findByValue(receivedBaudrate).getBaudRate() : Constants.DASH);
                                } else if (!config.isWifiEnable() && inputLine.contains(Constants.SERIAL_FRAMERATE)) {
                                    FireflyLuciferin.FPS_GW_CONSUMER = Float.parseFloat(inputLine.replace(Constants.SERIAL_FRAMERATE, ""));
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                // We don't care about this exception, it's caused by unknown serial messages
            }
        }
    }

    /**
     * Return the list of connected serial devices, available or not
     * @return available devices
     */
    public Map<String, Boolean> getAvailableDevices() {
        CommPortIdentifier serialPortId = null;
        var enumComm = CommPortIdentifier.getPortIdentifiers();
        Map<String, Boolean> availableDevice = new HashMap<>();
        while (enumComm.hasMoreElements()) {
            try {
                serialPortId = (CommPortIdentifier) enumComm.nextElement();
                if (serialPortId != null) {
                    serial = serialPortId.open(FireflyLuciferin.class.getName(), config != null ? config.getTimeout() : 2000);
                    availableDevice.put(serialPortId.getName(), true);
                    serial.close();
                }
            } catch (PortInUseException | NullPointerException e) {
                if (serialPortId != null) {
                    availableDevice.put(serialPortId.getName(), false);
                }
            }
        }
        return availableDevice;
    }
}
