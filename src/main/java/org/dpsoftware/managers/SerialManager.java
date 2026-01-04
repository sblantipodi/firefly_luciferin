/*
  SerialManager.java

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
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.network.tcpUdp.TcpClient;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serial port utility
 */
@Slf4j
public class SerialManager {

    ScheduledExecutorService serialAttachScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledFuture;
    boolean alertSent = false;

    /**
     * Initialize Serial communication
     */
    public void initSerial() {
        initSerial("", "");
    }

    /**
     * Initialize Serial communication
     */
    public void initSerial(String portName, String baudrate) {
        if (!MainSingleton.getInstance().config.isWirelessStream() || !portName.isEmpty()) {
            CommonUtility.delayMilliseconds(() -> {
                try {
                    closeSerial();
                    SerialPort[] ports = SerialPort.getCommPorts();
                    int numberOfSerialDevices = 0;
                    int readTimeout = MainSingleton.getInstance().config.getTimeout();
                    int writeTimeout = MainSingleton.getInstance().config.getTimeout();
                    if (ports != null && ports.length > 0) {
                        numberOfSerialDevices = ports.length;
                        for (SerialPort port : ports) {
                            if (MainSingleton.getInstance().config.getOutputDevice().equals(port.getSystemPortName())
                                    || (!portName.isEmpty() && portName.equals(port.getSystemPortName()))) {
                                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, readTimeout, writeTimeout);
                                MainSingleton.getInstance().serial = port;
                            }
                        }
                        if (MainSingleton.getInstance().config.getOutputDevice().equals(Constants.SERIAL_PORT_AUTO) && portName.isEmpty()) {
                            ports[0].setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, readTimeout, writeTimeout);
                            MainSingleton.getInstance().serial = ports[0];
                        }
                    }
                    MainSingleton.getInstance().serial.setDTRandRTS(false, false);
                    openSerial(portName, baudrate, readTimeout, writeTimeout, numberOfSerialDevices);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    MainSingleton.getInstance().communicationError = true;
                }
                if (MainSingleton.getInstance().communicationError) {
                    scheduleReconnect();
                }
            }, 10);
        }
    }

    /**
     * Open serial
     *
     * @param portName              serial port
     * @param baudrate              baudrate
     * @param readTimeout           read timeout
     * @param writeTimeout          write timeout
     * @param numberOfSerialDevices number of serial devices
     */
    private void openSerial(String portName, String baudrate, int readTimeout, int writeTimeout, int numberOfSerialDevices) {
        if (MainSingleton.getInstance().serial != null && MainSingleton.getInstance().serial.openPort()) {
            int baudrateToUse = baudrate.isEmpty() ? Integer.parseInt(MainSingleton.getInstance().config.getBaudRate()) : Integer.parseInt(baudrate);
            MainSingleton.getInstance().serial.setComPortParameters(baudrateToUse, 8, 1, SerialPort.NO_PARITY);
            MainSingleton.getInstance().serial.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, readTimeout, writeTimeout);
            log.info("{}{}", CommonUtility.getWord(Constants.SERIAL_PORT_IN_USE), MainSingleton.getInstance().serial.getSystemPortName());
            GlowWormDevice gwDevice = createDefaultDevice();
            GuiSingleton.getInstance().deviceTableData.add(gwDevice);
            GuiManager guiManager = new GuiManager();
            if (numberOfSerialDevices > 1 && MainSingleton.getInstance().config.getOutputDevice().equals(Constants.SERIAL_PORT_AUTO) && portName.isEmpty()) {
                MainSingleton.getInstance().communicationError = true;
                guiManager.showLocalizedNotification(Constants.SERIAL_PORT_AMBIGUOUS, Constants.SERIAL_PORT_AMBIGUOUS_CONTEXT, Constants.SERIAL_ERROR_TITLE, TrayIcon.MessageType.ERROR);
                log.error(Constants.SERIAL_ERROR_OPEN_HEADER);
            }
            log.info("Connected: Serial {}", MainSingleton.getInstance().serial.getDescriptivePortName());
            if (MainSingleton.getInstance().guiManager != null) {
                MainSingleton.getInstance().guiManager.trayIconManager.resetTray();
            }
            MainSingleton.getInstance().serialConnected = true;
            MainSingleton.getInstance().communicationError = false;
            MainSingleton.getInstance().output = MainSingleton.getInstance().serial.getOutputStream();
            listenSerialEvents();
        } else {
            if (NativeExecutor.isLinux() && !alertSent) {
                alertSent = true;
                Platform.runLater(() -> {
                    String content = CommonUtility.getWord(Constants.USB_NOT_AVAILABLE_CONTENT);
                    if (NativeExecutor.isSnap()) {
                        content += CommonUtility.getWord(Constants.USB_NOT_AVAILABLE_CONTENT_SNAP);
                    }
                    MainSingleton.getInstance().guiManager.showAlert(CommonUtility.getWord(Constants.USB_NOT_AVAILABLE_TITLE),
                            CommonUtility.getWord(Constants.USB_NOT_AVAILABLE_HEADER), content, Alert.AlertType.WARNING);
                });
            }
            MainSingleton.getInstance().communicationError = true;
        }
    }

    /**
     * Create a default device
     *
     * @return default device
     */
    private GlowWormDevice createDefaultDevice() {
        GlowWormDevice gwDevice = new GlowWormDevice();
        gwDevice.setDeviceName(Constants.USB_DEVICE);
        gwDevice.setDeviceIP(MainSingleton.getInstance().serial.getSystemPortName());
        gwDevice.setDhcpInUse(false);
        gwDevice.setWifi(Constants.DASH);
        gwDevice.setDeviceVersion(Constants.DASH);
        gwDevice.setDeviceBoard(Constants.DASH);
        gwDevice.setMac(Constants.DASH);
        gwDevice.setGpio(Constants.DASH);
        gwDevice.setNumberOfLEDSconnected(Constants.DASH);
        gwDevice.setLastSeen(MainSingleton.getInstance().formatter.format(new Date()));
        gwDevice.setFirmwareType(Constants.DASH);
        gwDevice.setBaudRate(Constants.DASH);
        gwDevice.setMqttTopic(Constants.DASH);
        gwDevice.setColorMode(Constants.DASH);
        gwDevice.setColorOrder(Enums.ColorOrder.GRB_GRBW.name());
        gwDevice.setLdrValue(Constants.DASH);
        gwDevice.setRelayPin(Constants.DASH);
        gwDevice.setRelayInvertedPin(false);
        gwDevice.setSbPin(Constants.DASH);
        gwDevice.setLdrPin(Constants.DASH);
        gwDevice.setGpioClock(Constants.DASH);
        gwDevice.setLedBuiltin(Constants.DASH);
        return gwDevice;
    }

    /**
     * Add a listener on USB ports
     */
    @SuppressWarnings("all")
    private void listenSerialEvents() {
        // No autocloseable because this thread must not be terminated
        ExecutorService executor = Executors.newSingleThreadExecutor();
        MainSingleton.getInstance().serial.addDataListener(new SerialPortDataListener() {
            private StringBuilder lineBuffer = new StringBuilder();

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    SerialPort serial = MainSingleton.getInstance().serial;
                    int available = serial.bytesAvailable();
                    if (available <= 0) return;

                    byte[] buffer = new byte[available];
                    int numRead = serial.readBytes(buffer, buffer.length);
                    if (numRead > 0) {
                        String data = new String(buffer, StandardCharsets.UTF_8);
                        lineBuffer.append(data);

                        String line;
                        while ((line = extractLineFromBuffer(lineBuffer)) != null) {
                            handleSerialEvent(line);
                        }
                    }
                } else if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                    log.info("USB device disconnected");
                    SerialManager sm = new SerialManager();
                    sm.closeSerial();
                    scheduleReconnect();
                }
            }

            // Get a complete line from the buffer, if present
            private String extractLineFromBuffer(StringBuilder buffer) {
                int idx = buffer.indexOf("\n");
                if (idx == -1) return null;
                String line = buffer.substring(0, idx).replaceAll("\r$", "");
                buffer.delete(0, idx + 1);
                return line;
            }
        });
    }

    /**
     * Reconnect USB if disconnected (example a device reboot)
     */
    private void scheduleReconnect() {
        if (scheduledFuture == null || scheduledFuture.isDone()) {
            scheduledFuture = serialAttachScheduler.scheduleAtFixedRate(() -> {
                try {
                    if (MainSingleton.getInstance().serial != null && MainSingleton.getInstance().serial.openPort()) {
                        log.debug("USB device reconnected successfully");
                        MainSingleton.getInstance().communicationError = false;
                        MainSingleton.getInstance().guiManager.startCapturingThreads();
                        serialAttachScheduler.shutdown();
                    } else {
                        log.debug("Retrying connection on USB device");
                        initSerial();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }, 0, 5, TimeUnit.SECONDS);
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
            byte[] ledsArray = new byte[(MainSingleton.getInstance().ledNumber * 3) + Constants.SERIAL_PARAMS];
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
            int relayInvToSend = (MainSingleton.getInstance().relayInv ? 11 : 10) & 0xff;
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
            ledsArray[++j] = (byte) (relayInvToSend);
            ledsArray[++j] = (byte) (sbPinToSend);
            ledsArray[++j] = (byte) (ldrPinToSend);
            ledsArray[++j] = (byte) (gpioClockToSend);
            ledsArray[++j] = (byte) ((ledsCountHi ^ ledsCountLo ^ loSecondPart ^ brightnessToSend ^ gpioToSend ^ baudRateToSend ^ whiteTempToSend ^ fireflyEffectToSend
                    ^ enableLdr ^ ldrTurnOff ^ ldrInterval ^ ldrMin ^ ldrActionToUse ^ colorModeToSend ^ colorOrderToSend ^ relayPinToSend ^ relayInvToSend ^ sbPinToSend ^ ldrPinToSend ^ gpioClockToSend ^ 0x55));
            MainSingleton.getInstance().ldrAction = 1;
            if (leds.length == 1) {
                MainSingleton.getInstance().colorInUse = leds[0];
                while (i < MainSingleton.getInstance().ledNumber) {
                    ledsArray[++j] = (byte) leds[0].getRed();
                    ledsArray[++j] = (byte) leds[0].getGreen();
                    ledsArray[++j] = (byte) leds[0].getBlue();
                    i++;
                }
                boolean toggleLed = (leds[0].getRed() != 0 || leds[0].getGreen() != 0 || leds[0].getBlue() != 0);
                if (toggleLed != MainSingleton.getInstance().config.isToggleLed()) {
                    MainSingleton.getInstance().config.setToggleLed(toggleLed);
                }
            } else {
                while (i < MainSingleton.getInstance().ledNumber) {
                    ledsArray[++j] = (byte) leds[i].getRed();
                    ledsArray[++j] = (byte) leds[i].getGreen();
                    ledsArray[++j] = (byte) leds[i].getBlue();
                    i++;
                }
            }
            if (MainSingleton.getInstance().output != null) {
                MainSingleton.getInstance().output.write(ledsArray);
            }
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
                        if (MainSingleton.getInstance().serial != null && MainSingleton.getInstance().serial.isOpen()) {
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
     * Handle an event on the serial port. Read the data and print it.
     *
     * @param inputLine input string
     */
    public void handleSerialEvent(String inputLine) {
        try {
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
                            if (!(receivedBaudrate >= 1 && receivedBaudrate <= Constants.MAX_BAUDRATE)) {
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
                        } else if (inputLine.contains(Constants.SERIAL_LDR_RELAYINV)) {
                            glowWormDevice.setRelayInvertedPin(inputLine.replace(Constants.SERIAL_LDR_RELAYINV, "").equals("1"));
                        } else if (inputLine.contains(Constants.SERIAL_LDR_SBPIN)) {
                            glowWormDevice.setSbPin(inputLine.replace(Constants.SERIAL_LDR_SBPIN, ""));
                        } else if (inputLine.contains(Constants.SERIAL_GPIO_CLOCK)) {
                            glowWormDevice.setGpioClock(inputLine.replace(Constants.SERIAL_GPIO_CLOCK, ""));
                        } else if ((inputLine.contains(Constants.SERIAL_IMPROV) && inputLine.contains(Constants.HTTP))
                                || (inputLine.contains(Constants.IP_ADDRESS) && !inputLine.contains(Constants.BC))) {
                            if (!MainSingleton.getInstance().getImprovActive().isEmpty()) {
                                Pattern p = Pattern.compile(Constants.REGEXP_IP);
                                Matcher m = p.matcher(inputLine);
                                if (m.find()) {
                                    String deviceToProvision = MainSingleton.getInstance().improvActive;
                                    MainSingleton.getInstance().improvActive = "";
                                    programFirmwareAfterImprov(inputLine, deviceToProvision);
                                }
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            // We don't care about this exception, it's caused by unknown serial messages
        }
    }

    /**
     * This method programs firmware after the improv protocol has been triggered
     *
     * @param inputLine         input received via Serial port
     * @param deviceToProvision device to program
     */
    private void programFirmwareAfterImprov(String inputLine, String deviceToProvision) {
        Pattern p = Pattern.compile(Constants.REGEXP_URL);
        Matcher m = p.matcher(inputLine);
        Pattern p2 = Pattern.compile(Constants.REGEXP_IP);
        Matcher m2 = p2.matcher(inputLine);
        String ip;
        if (m.find()) {
            ip = m.group(1);
        } else if (m2.find()) {
            ip = m2.group();
        } else {
            ip = "";
        }
        if (!ip.isEmpty()) {
            log.info("IMPROV protocol, device connected: {}", ip);
            closeSerial();
            if (MainSingleton.getInstance().config != null) {
                CommonUtility.delaySeconds(() -> {
                    FirmwareConfigDto firmwareConfigDto = getFirmwareConfigDto(deviceToProvision);
                    TcpResponse tcpResponse = null;
                    final int MAX_RETRY = 30;
                    for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
                        tcpResponse = TcpClient.httpGet(CommonUtility.toJsonString(firmwareConfigDto), Constants.HTTP_SETTING, ip);
                        if (tcpResponse.getErrorCode() == Constants.HTTP_SUCCESS) {
                            MainSingleton.getInstance().guiManager.pipelineManager.startCapturePipeline();
                            log.info(CommonUtility.getWord(Constants.FIRMWARE_PROGRAM_NOTIFY_HEADER));
                            MainSingleton.getInstance().guiManager.showLocalizedNotification(CommonUtility.getWord(Constants.FIRMWARE_PROGRAM_NOTIFY),
                                    CommonUtility.getWord(Constants.FIRMWARE_PROGRAM_NOTIFY_HEADER), Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.INFO);
                            break;
                        }
                        // No response, retry
                        log.warn("Attempt to program firmware {} of {}. Retrying...", attempt, MAX_RETRY);
                    }
                    // After all retries
                    if (tcpResponse.getErrorCode() != Constants.HTTP_SUCCESS) {
                        log.error("Unable to contact IP {} after {} attempts.", ip, MAX_RETRY);
                    }
                }, 5);
            }
        }
    }

    /**
     * Firmware configuration
     *
     * @param deviceToProvision device to program
     * @return Firmware configuration
     */
    private FirmwareConfigDto getFirmwareConfigDto(String deviceToProvision) {
        Configuration config = MainSingleton.getInstance().config;
        FirmwareConfigDto firmwareConfigDto = new FirmwareConfigDto();
        String deviceNameForAuto = "GW_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        firmwareConfigDto.setDeviceName(Constants.SERIAL_PORT_AUTO.equals(config.getOutputDevice()) ? deviceNameForAuto : deviceToProvision);
        firmwareConfigDto.setMicrocontrollerIP("");
        firmwareConfigDto.setMqttCheckbox(config.isMqttEnable());
        firmwareConfigDto.setSsid("");
        firmwareConfigDto.setWifipwd("");
        if (config.isMqttEnable()) {
            firmwareConfigDto.setMqttIP(config.getMqttServer().substring(0, config.getMqttServer().lastIndexOf(":")).replace(Constants.DEFAULT_MQTT_PROTOCOL, ""));
            firmwareConfigDto.setMqttPort(config.getMqttServer().substring(config.getMqttServer().lastIndexOf(":") + 1));
            firmwareConfigDto.setMqttTopic(config.getMqttTopic().equals(Constants.TOPIC_DEFAULT_MQTT) ? Constants.MQTT_BASE_TOPIC : config.getMqttTopic());
            firmwareConfigDto.setMqttuser(config.getMqttUsername());
            firmwareConfigDto.setMqttpass(config.getMqttPwd());
        }
        return firmwareConfigDto;
    }

    /**
     * Return the list of connected serial devices, available or not
     *
     * @return available devices
     */
    public Map<String, Boolean> getAvailableDevices() {
        Map<String, Boolean> availableDevice = new HashMap<>();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            availableDevice.put(port.getSystemPortName(), true);
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

    /**
     * Close serial communication and remove all attached listeners
     */
    public void closeSerial() {
        log.debug("Closing serial connection");
        if (MainSingleton.getInstance().output != null) {
            try {
                MainSingleton.getInstance().output.flush();
                MainSingleton.getInstance().output.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        if (MainSingleton.getInstance().serial != null) {
            MainSingleton.getInstance().serial.removeDataListener();
            MainSingleton.getInstance().serial.setDTRandRTS(false, false);
            MainSingleton.getInstance().serial.closePort();
        }
        MainSingleton.getInstance().output = null;
        MainSingleton.getInstance().serial = null;
    }

}
