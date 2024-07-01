/*
  SerialManager.java

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
package org.dpsoftware.managers;

import gnu.io.*;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.GuiSingleton;
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
        if (!MainSingleton.getInstance().config.isWirelessStream()) {
            int numberOfSerialDevices = 0;
            var enumComm = CommPortIdentifier.getPortIdentifiers();
            while (enumComm.hasMoreElements()) {
                numberOfSerialDevices++;
                CommPortIdentifier serialPortAvailable = (CommPortIdentifier) enumComm.nextElement();
                if (MainSingleton.getInstance().config.getOutputDevice().equals(serialPortAvailable.getName()) || MainSingleton.getInstance().config.getOutputDevice().equals(Constants.SERIAL_PORT_AUTO)) {
                    serialPortId = serialPortAvailable;
                }
            }
            try {
                if (serialPortId != null) {
                    log.info("{}{}, connecting...", CommonUtility.getWord(Constants.SERIAL_PORT_IN_USE), serialPortId.getName());
                    MainSingleton.getInstance().serial = serialPortId.open(fireflyLuciferin.getClass().getName(), MainSingleton.getInstance().config.getTimeout());
                    MainSingleton.getInstance().serial.setSerialPortParams(Integer.parseInt(MainSingleton.getInstance().config.getBaudRate()), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    input = new BufferedReader(new InputStreamReader(MainSingleton.getInstance().serial.getInputStream()));
                    // add event listeners
                    MainSingleton.getInstance().serial.addEventListener(fireflyLuciferin);
                    MainSingleton.getInstance().serial.notifyOnDataAvailable(true);
                    GuiSingleton.getInstance().deviceTableData.add(new GlowWormDevice(Constants.USB_DEVICE, serialPortId.getName(), false,
                            Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH,
                            MainSingleton.getInstance().formatter.format(new Date()), Constants.DASH, Constants.DASH, Constants.DASH, Enums.ColorOrder.GRB.name(),
                            Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH));
                    GuiManager guiManager = new GuiManager();
                    if (numberOfSerialDevices > 1 && MainSingleton.getInstance().config.getOutputDevice().equals(Constants.SERIAL_PORT_AUTO)) {
                        MainSingleton.getInstance().communicationError = true;
                        if (NativeExecutor.isWindows()) {
                            guiManager.showLocalizedNotification(Constants.SERIAL_PORT_AMBIGUOUS,
                                    Constants.SERIAL_PORT_AMBIGUOUS_CONTEXT, TrayIcon.MessageType.ERROR);
                        } else {
                            guiManager.showLocalizedAlert(Constants.SERIAL_ERROR_TITLE, Constants.SERIAL_PORT_AMBIGUOUS,
                                    Constants.SERIAL_PORT_AMBIGUOUS_CONTEXT, Alert.AlertType.ERROR);
                        }
                        log.error(Constants.SERIAL_ERROR_OPEN_HEADER);
                    }
                    log.info("Connected: Serial {}", serialPortId.getName());
                    if (MainSingleton.getInstance().guiManager != null) {
                        MainSingleton.getInstance().guiManager.trayIconManager.resetTray();
                    }
                    MainSingleton.getInstance().serialConnected = true;
                    MainSingleton.getInstance().communicationError = false;
                    initOutputStream();
                }
            } catch (PortInUseException | UnsupportedCommOperationException | NullPointerException | IOException |
                     TooManyListenersException e) {
                log.error(e.getMessage());
                MainSingleton.getInstance().communicationError = true;
            }
        }
    }

    /**
     * Send color info via USB Serial
     *
     * @param leds array with colors
     * @throws IOException can't write to serial
     */
    public void sendColorsViaUSB(Color[] leds) throws IOException {
        // Effect is set via MQTT when using Full Firmware
        if (MainSingleton.getInstance().config.isFullFirmware()) {
            MainSingleton.getInstance().fireflyEffect = 100;
        } else {
            for (Enums.Effect ef : Enums.Effect.values()) {
                if (ef.getBaseI18n().equals(MainSingleton.getInstance().config.getEffect())) {
                    MainSingleton.getInstance().fireflyEffect = ef.ordinal() + 1;
                }
            }
        }
        if (!ManagerSingleton.getInstance().serialVersionOk) {
            UpgradeManager upgradeManager = new UpgradeManager();
            // Check if the connected device match the minimum firmware version requirements for this Firefly Luciferin version
            Boolean firmwareMatchMinRequirements = upgradeManager.firmwareMatchMinimumRequirements();
            if (firmwareMatchMinRequirements != null) {
                if (firmwareMatchMinRequirements) {
                    ManagerSingleton.getInstance().serialVersionOk = true;
                }
            }
        } else {
            int i = 0, j = -1;
            byte[] ledsArray = new byte[(MainSingleton.getInstance().ledNumber * 3) + 26];
            // DPsoftware checksum
            int ledsCountHi = ((MainSingleton.getInstance().ledNumHighLowCount) >> 8) & 0xff;
            int ledsCountLo = (MainSingleton.getInstance().ledNumHighLowCount) & 0xff;
            int loSecondPart = (MainSingleton.getInstance().ledNumHighLowCountSecondPart) & 0xff;
            int brightnessToSend = (AudioSingleton.getInstance().AUDIO_BRIGHTNESS == 255 ? CommonUtility.getNightBrightness() : AudioSingleton.getInstance().AUDIO_BRIGHTNESS) & 0xff;
            int gpioToSend = (MainSingleton.getInstance().gpio) & 0xff;
            int baudRateToSend = (MainSingleton.getInstance().baudRate) & 0xff;
            int whiteTempToSend = (MainSingleton.getInstance().config.getWhiteTemperature()) & 0xff;
            int fireflyEffectToSend = (MainSingleton.getInstance().fireflyEffect) & 0xff;
            int enableLdr = (MainSingleton.getInstance().config.isEnableLDR() ? 1 : 2) & 0xff;
            int ldrTurnOff = (MainSingleton.getInstance().config.isLdrTurnOff() ? 1 : 2) & 0xff;
            int ldrInterval = (MainSingleton.getInstance().config.getLdrInterval()) & 0xff;
            int ldrMin = (MainSingleton.getInstance().config.getLdrMin()) & 0xff;
            int ldrActionToUse = (MainSingleton.getInstance().ldrAction) & 0xff;
            int colorModeToSend = (MainSingleton.getInstance().config.getColorMode()) & 0xff;
            int colorOrderToSend = (MainSingleton.getInstance().colorOrder) & 0xff;
            // Pins is set to +10 because null values are zero, so GPIO 0 is 10, GPIO 1 is 11.
            int relayPinToSend = (MainSingleton.getInstance().relayPin >= 0 ? MainSingleton.getInstance().relayPin + 10 : 0) & 0xff;
            int sbPinToSend = (MainSingleton.getInstance().sbPin >= 0 ? MainSingleton.getInstance().sbPin + 10 : 0) & 0xff;
            int ldrPinToSend = (MainSingleton.getInstance().ldrPin >= 0 ? MainSingleton.getInstance().ldrPin + 10 : 0) & 0xff;
            int gpioClockToSend = (MainSingleton.getInstance().gpioClockPin) & 0xff;
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
            ledsArray[++j] = (byte) (enableLdr);
            ledsArray[++j] = (byte) (ldrTurnOff);
            ledsArray[++j] = (byte) (ldrInterval);
            ledsArray[++j] = (byte) (ldrMin);
            ledsArray[++j] = (byte) (ldrActionToUse);
            ledsArray[++j] = (byte) (colorModeToSend);
            ledsArray[++j] = (byte) (colorOrderToSend);
            ledsArray[++j] = (byte) (relayPinToSend);
            ledsArray[++j] = (byte) (sbPinToSend);
            ledsArray[++j] = (byte) (ldrPinToSend);
            ledsArray[++j] = (byte) (gpioClockToSend);
            ledsArray[++j] = (byte) ((ledsCountHi ^ ledsCountLo ^ loSecondPart ^ brightnessToSend ^ gpioToSend ^ baudRateToSend ^ whiteTempToSend ^ fireflyEffectToSend
                    ^ enableLdr ^ ldrTurnOff ^ ldrInterval ^ ldrMin ^ ldrActionToUse ^ colorModeToSend ^ colorOrderToSend ^ relayPinToSend ^ sbPinToSend ^ ldrPinToSend ^ gpioClockToSend ^ 0x55));
            MainSingleton.getInstance().ldrAction = 1;
            if (leds.length == 1) {
                MainSingleton.getInstance().colorInUse = leds[0];
                while (i < MainSingleton.getInstance().ledNumber) {
                    ledsArray[++j] = (byte) leds[0].getRed();
                    ledsArray[++j] = (byte) leds[0].getGreen();
                    ledsArray[++j] = (byte) leds[0].getBlue();
                    i++;
                }
            } else {
                while (i < MainSingleton.getInstance().ledNumber) {
                    ledsArray[++j] = (byte) leds[i].getRed();
                    ledsArray[++j] = (byte) leds[i].getGreen();
                    ledsArray[++j] = (byte) leds[i].getBlue();
                    i++;
                }
            }
            MainSingleton.getInstance().output.write(ledsArray);
        }
    }

    /**
     * Check SOLID LEDs config and refresh LED strip state accordingly
     * This function works with GlowWormLuciferin Light, MQTT version does not need it
     */
    public void manageSolidLed() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (!MainSingleton.getInstance().RUNNING) {
                if (MainSingleton.getInstance().config.isToggleLed() && !MainSingleton.getInstance().config.isFullFirmware()) {
                    Color[] colorToUse = new Color[1];
                    if (MainSingleton.getInstance().colorInUse == null) {
                        String[] color = MainSingleton.getInstance().config.getColorChooser().split(",");
                        colorToUse[0] = new Color(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]));
                        MainSingleton.getInstance().config.setBrightness(Integer.parseInt(color[3]));
                    } else {
                        colorToUse[0] = MainSingleton.getInstance().colorInUse;
                    }
                    try {
                        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
                        if (Enums.Effect.RAINBOW.equals(effectInUse) || Enums.Effect.FIRE.equals(effectInUse)) {
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
        if (!MainSingleton.getInstance().config.isWirelessStream() && !MainSingleton.getInstance().communicationError) {
            try {
                MainSingleton.getInstance().output = MainSingleton.getInstance().serial.getOutputStream();
            } catch (IOException | NullPointerException e) {
                MainSingleton.getInstance().communicationError = true;
                log.error(e.getMessage());
                log.error(Constants.SERIAL_ERROR_HEADER);
            }
        }
    }

    /**
     * Handle an event on the serial port. Read the data and print it.
     *
     * @param event input event
     */
    public void handleSerialEvent(SerialPortEvent event) {
        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                if (input.ready()) {
                    String inputLine = input.readLine();
                    log.debug(inputLine);
                    GuiSingleton.getInstance().deviceTableData.forEach(glowWormDevice -> {
                        if (glowWormDevice.getDeviceName().equals(Constants.USB_DEVICE)) {
                            if (!MainSingleton.getInstance().config.isMqttEnable() && MainSingleton.getInstance().config.isFullFirmware()) {
                                GuiSingleton.getInstance().deviceTableData.forEach(gwDevice -> {
                                    if (glowWormDevice.getMac().equals(gwDevice.getMac())) {
                                        gwDevice.setLastSeen(MainSingleton.getInstance().formatter.format(new Date()));
                                    }
                                });
                            }
                            glowWormDevice.setLastSeen(MainSingleton.getInstance().formatter.format(new Date()));
                            // Skipping the Setting LED loop from Glow Worm Luciferin Serial communication
                            if (!inputLine.contains(Constants.SETTING_LED_SERIAL)) {
                                if (inputLine.contains(Constants.SERIAL_VERSION)) {
                                    String deviceVer = inputLine.replace(Constants.SERIAL_VERSION, "");
                                    if (MainSingleton.getInstance().config.isCheckForUpdates() && Enums.SupportedDevice.ESP32_S3_CDC.name().equals(glowWormDevice.getDeviceBoard())) {
                                        deviceVer = Constants.FORCE_FIRMWARE_AUTO_UPGRADE;
                                    }
                                    glowWormDevice.setDeviceVersion(deviceVer);
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
                                    glowWormDevice.setColorMode(Enums.ColorMode.values()[Integer.parseInt(inputLine.replace(Constants.SERIAL_COLOR_MODE, "")) - 1].getI18n());
                                } else if (inputLine.contains(Constants.SERIAL_COLOR_ORDER)) {
                                    glowWormDevice.setColorOrder(Enums.ColorOrder.findByValue(Integer.parseInt(inputLine.replace(Constants.SERIAL_COLOR_ORDER, ""))).name());
                                } else if (inputLine.contains(Constants.SERIAL_BAUDRATE)) {
                                    boolean validBaudrate = true;
                                    int receivedBaudrate = Integer.parseInt(inputLine.replace(Constants.SERIAL_BAUDRATE, ""));
                                    if (!(receivedBaudrate >= 1 && receivedBaudrate <= 8)) {
                                        validBaudrate = false;
                                    }
                                    glowWormDevice.setBaudRate(validBaudrate ? Enums.BaudRate.findByValue(receivedBaudrate).getBaudRate() : Constants.DASH);
                                } else if ((!MainSingleton.getInstance().config.isFullFirmware() || !MainSingleton.getInstance().config.isMqttEnable()) && inputLine.contains(Constants.SERIAL_FRAMERATE)) {
                                    MainSingleton.getInstance().FPS_GW_CONSUMER = Float.parseFloat(inputLine.replace(Constants.SERIAL_FRAMERATE, ""));
                                } else if (inputLine.contains(Constants.SERIAL_LDR)) {
                                    MainSingleton.getInstance().ldrStrength = Integer.parseInt(inputLine.replace(Constants.SERIAL_LDR, ""));
                                    glowWormDevice.setLdrValue(inputLine.replace(Constants.SERIAL_LDR, "") + Constants.PERCENT);
                                } else if (inputLine.contains(Constants.SERIAL_LDR_LDRPIN)) {
                                    glowWormDevice.setLdrPin(inputLine.replace(Constants.SERIAL_LDR_LDRPIN, ""));
                                } else if (inputLine.contains(Constants.SERIAL_LDR_RELAYPIN)) {
                                    glowWormDevice.setRelayPin(inputLine.replace(Constants.SERIAL_LDR_RELAYPIN, ""));
                                } else if (inputLine.contains(Constants.SERIAL_LDR_SBPIN)) {
                                    glowWormDevice.setSbPin(inputLine.replace(Constants.SERIAL_LDR_SBPIN, ""));
                                } else if (inputLine.contains(Constants.SERIAL_GPIO_CLOCK)) {
                                    glowWormDevice.setGpioClock(inputLine.replace(Constants.SERIAL_GPIO_CLOCK, ""));
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
     *
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
                    MainSingleton.getInstance().serial = serialPortId.open(FireflyLuciferin.class.getName(), MainSingleton.getInstance().config != null ? MainSingleton.getInstance().config.getTimeout() : 2000);
                    availableDevice.put(serialPortId.getName(), true);
                    MainSingleton.getInstance().serial.close();
                }
            } catch (PortInUseException | NullPointerException e) {
                if (serialPortId != null) {
                    availableDevice.put(serialPortId.getName(), false);
                }
            }
        }
        return availableDevice;
    }

    /**
     * Send serialParams, this will cause a reboot on the microcontroller
     */
    public void sendSerialParams(int r, int g, int b) {
        java.awt.Color[] leds = new java.awt.Color[1];
        try {
            leds[0] = new java.awt.Color(r, g, b);
            sendColorsViaUSB(leds);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
