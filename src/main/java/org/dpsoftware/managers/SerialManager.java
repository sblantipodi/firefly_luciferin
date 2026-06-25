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
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.grabber.GrabberSingleton;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.network.NetworkSingleton;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

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
     * Compute cheksum and add config data to the serial payload
     *
     * @param ledsArray array with colors
     * @param j         serial byte cursor
     * @return serial byte cursor
     */
    private static int computeChecksumAndData(byte[] ledsArray, int j) {
        // Checksunm params
        int totalLeds = MainSingleton.getInstance().ledNumber; // Real numbers of LEDs
        // Byte is limited to 255, use byte splitting via byte shifting
        int ledsCountHi = (totalLeds >> 8) & 0xFF;             // Byte high
        int ledsCountLo = totalLeds & 0xFF;                    // Byte low
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
        int relayPinToSend = (MainSingleton.getInstance().relayPin >= 0 ? MainSingleton.getInstance().relayPin + 10 : 0) & 0xff;
        int relayInvToSend = (MainSingleton.getInstance().relayInv ? 11 : 10) & 0xff;
        int sbPinToSend = (MainSingleton.getInstance().sbPin >= 0 ? MainSingleton.getInstance().sbPin + 10 : 0) & 0xff;
        int ldrPinToSend = (MainSingleton.getInstance().ldrPin >= 0 ? MainSingleton.getInstance().ldrPin + 10 : 0) & 0xff;
        int gpioClockToSend = (MainSingleton.getInstance().gpioClockPin) & 0xff;
        // Write 26 bytes
        ledsArray[++j] = (byte) ('D');
        ledsArray[++j] = (byte) ('P');
        ledsArray[++j] = (byte) ('s');
        ledsArray[++j] = (byte) ('o');
        ledsArray[++j] = (byte) ('f');
        ledsArray[++j] = (byte) ('t');
        ledsArray[++j] = (byte) (ledsCountHi);
        ledsArray[++j] = (byte) (ledsCountLo);
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
        ledsArray[++j] = (byte) ((ledsCountHi ^ ledsCountLo ^ brightnessToSend ^ gpioToSend ^ baudRateToSend ^ whiteTempToSend ^ fireflyEffectToSend
                ^ enableLdr ^ ldrTurnOff ^ ldrInterval ^ ldrMin ^ ldrActionToUse ^ colorModeToSend ^ colorOrderToSend
                ^ relayPinToSend ^ relayInvToSend ^ sbPinToSend ^ ldrPinToSend ^ gpioClockToSend ^ 0x55));

        MainSingleton.getInstance().ldrAction = 1;
        return j;
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
                        initSerial();
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
     * Calculate the dimension of RLE groups based on the LED matrix configuration
     *
     * @param ledMatrix led infos
     * @return RLE group
     */
    private static List<Integer> getRleGroupDimension(LinkedHashMap<Integer, LEDCoordinate> ledMatrix) {
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
        return groupSizes;
    }

    /**
     * Print RLE maps for debugging purposes, only if debug logging is enabled and the RLE map has changed since the last print to avoid log flooding.
     *
     * @param rleEntries           RLE entries
     * @param ledMatrixWithLeaders array of leaders
     * @param length               total number of LEDs in the strip
     */
    private static void printRleMaps(List<byte[]> rleEntries, LinkedHashMap<Integer, LEDCoordinate> ledMatrixWithLeaders, int length) {
        if (!GrabberSingleton.getInstance().isLosslessCompressionLog()) {
            return;
        }
        // Group details
        StringBuilder sbEntries = new StringBuilder();
        StringBuilder groups = new StringBuilder();
        for (int i = 0; i < rleEntries.size(); i++) {
            byte[] entry = rleEntries.get(i);
            int count = entry[0] & 0xFF;
            int size = entry[1] & 0xFF;
            groups.append(String.format("[%dx%d]", count, size));
            if (i < rleEntries.size() - 1) {
                groups.append(",");
            }
        }
        if (!sbEntries.isEmpty() && NetworkSingleton.getInstance().getRleMapInUse().contentEquals(sbEntries)) {
            return;
        }
        sbEntries.append(groups);
        NetworkSingleton.getInstance().setRleMapInUse(sbEntries.toString());
        log.debug(sbEntries.toString());
        // Visual printing
        NetworkSingleton.printVisualRleMap(ledMatrixWithLeaders, groups, length);
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
                    int writeTimeout = 0;
                    if (ports != null && ports.length > 0) {
                        numberOfSerialDevices = ports.length;
                        for (SerialPort port : ports) {
                            if (MainSingleton.getInstance().config.getOutputDevice().equals(port.getSystemPortName())
                                    || (!portName.isEmpty() && portName.equals(port.getSystemPortName()))) {
                                port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, readTimeout, writeTimeout);
                                MainSingleton.getInstance().serial = port;
                            }
                        }
                        if (MainSingleton.getInstance().config.getOutputDevice().equals(Constants.SERIAL_PORT_AUTO) && portName.isEmpty()) {
                            ports[0].setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, readTimeout, writeTimeout);
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
     * Send color info via USB Serial
     *
     * @param leds array with colors
     * @throws IOException can't write to serial
     */
    public void sendColorsViaUSB(Color[] leds) throws IOException {
        // Solid LED management
        if (leds.length == 1) {
            Color c = leds[0];
            leds = new Color[MainSingleton.getInstance().ledNumber];
            for (int i = 0; i < MainSingleton.getInstance().ledNumber; i++) {
                leds[i] = c;
            }
            MainSingleton.getInstance().colorInUse = leds[0];
            boolean toggleLed = (leds[0].getRed() != 0 || leds[0].getGreen() != 0 || leds[0].getBlue() != 0);
            if (toggleLed != MainSingleton.getInstance().config.isToggleLed()) {
                MainSingleton.getInstance().config.setToggleLed(toggleLed);
            }
        }
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
            Boolean firmwareMatchMinRequirements = upgradeManager.firmwareMatchMinimumRequirements();
            if (firmwareMatchMinRequirements != null && firmwareMatchMinRequirements) {
                ManagerSingleton.getInstance().serialVersionOk = true;
            }
        } else {
            byte[] ledsArray = implementRleCompressionLogic(leds);
            // Send serial params with pacing control
            writeToSerialWithPacing(ledsArray);
        }
    }

    private static void rleBytePadding(List<byte[]> rleEntries, int count, int size) {
        while (size > 255) {
            rleEntries.add(new byte[]{(byte) count, (byte) 255});
            size -= 255;
        }
        if (size > 0) {
            rleEntries.add(new byte[]{(byte) count, (byte) size});
        }
    }

    /**
     * Implement RLE compression logic
     *
     * @param leds array with colors
     * @return leds array
     */
    private byte[] implementRleCompressionLogic(Color[] leds) {
        int j = -1;
        // Create new RLE leaders
        LinkedHashMap<Integer, LEDCoordinate> ledMatrixWithLeaders = NetworkSingleton.builtRleLeaders(leds);
        List<Color> leaderColors = new ArrayList<>();
        int ledIndex = 0;
        for (LEDCoordinate coord : ledMatrixWithLeaders.values()) {
            if (!coord.isGroupedLed()) {
                leaderColors.add(leds[ledIndex]);
            }
            ledIndex++;
        }
        // Calculate groups dimension
        List<Integer> groupSizes = getRleGroupDimension(ledMatrixWithLeaders);
        // Binary RLE encoding (couple count x size)
        List<byte[]> rleEntries = new ArrayList<>();
        int rleIdx = 0;
        while (rleIdx < groupSizes.size()) {
            int size = groupSizes.get(rleIdx);
            int count = 0;
            // How many elements with the same size
            while (rleIdx < groupSizes.size() && groupSizes.get(rleIdx) == size) {
                count++;
                rleIdx++;
                // Bytes can't exceed 255, create another group instead
                if (count == 255) {
                    rleBytePadding(rleEntries, count, size);
                    count = 0;
                }
            }
            if (count > 0) {
                rleBytePadding(rleEntries, count, size);
            }
        }
        byte numRleEntries = (byte) rleEntries.size();
        // Dyanmic buffer size
        int rleHeaderBytes = (2 + (rleEntries.size() * 2));
        int colorBytesCount = (leaderColors.size() * 3);
        // Visual debug output
        printRleMaps(rleEntries, ledMatrixWithLeaders, leds.length);
        byte[] ledsArray = new byte[Constants.SERIAL_PARAMS + rleHeaderBytes + colorBytesCount];
        // Checksum with data config
        j = computeChecksumAndData(ledsArray, j);
        // Write RLE bytes
        ledsArray[++j] = 1;
        ledsArray[++j] = numRleEntries;
        for (byte[] entry : rleEntries) {
            ledsArray[++j] = entry[0]; // count
            ledsArray[++j] = entry[1]; // size
        }
        // Built the final led array, this will be written to Serial with pacing control
        for (Color color : leaderColors) {
            ledsArray[++j] = (byte) color.getRed();
            ledsArray[++j] = (byte) color.getGreen();
            ledsArray[++j] = (byte) color.getBlue();
        }
        return ledsArray;
    }

    /**
     * Write to serial with pacing control to prevent overwhelming the microcontroller with too many frames in a short time,
     * especially during the initial ramp-up phase of the flow.
     *
     * @param ledsArray array with colors
     * @throws IOException can't write to serial
     */
    private void writeToSerialWithPacing(byte[] ledsArray) throws IOException {
        if (MainSingleton.getInstance().output == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        GrabberSingleton grabber = GrabberSingleton.getInstance();
        if (grabber.getFlowStartTime() == 0) {
            grabber.setFlowStartTime(currentTime);
            grabber.setLastSecondMark(currentTime);
            grabber.setLastActualSendTime(currentTime);
            grabber.setCurrentSecondToken(0);
        }
        // Reset the token counter every second (Time window)
        if (currentTime - grabber.getLastSecondMark() >= 1000) {
            grabber.setLastSecondMark(currentTime);
            grabber.setCurrentSecondToken(0);
        }
        // Dynamic calculation of the maximum framerate (10 second cubic ramp)
        long targetFramerate = GStreamerGrabber.getTargetFramerateForDevice();
        long elapsedMs = currentTime - grabber.getFlowStartTime();
        // Normalized progress factor (from 0.0 to 1.0 over 10 seconds)
        double progressFactor = Math.min(1.0, elapsedMs / 10000.0);
        // Cubic curve to ensure a stable 1 FPS at the beginning (as seen in logs)
        double curvedProgress = Math.pow(progressFactor, 3);
        // The framerate limit for this specific second
        int currentMaxFramerate = (int) Math.max(1, Math.min(targetFramerate, targetFramerate * curvedProgress));
        //  Hard Limit check on current second tokens (Simplified Token Bucket)
        if (grabber.getCurrentSecondToken() >= currentMaxFramerate) {
            return; // Framerate drop: we have exhausted the allowed frames for this second of the ramp
        }
        // Static anti-burst control (Dynamic minimum distance between successive frames)
        long minDelayMs = 1000 / currentMaxFramerate;
        // Apply a tolerance/slack to the minimum delay when at full regime, otherwise physiological thread jitter will cause spurious frame drops.
        if (progressFactor >= 1.0) {
            minDelayMs = minDelayMs - 3; // Lowers the threshold at full regime to let framerate pass smoothly with 3ms jitter
        }
        long timeSinceLastSend = currentTime - grabber.getLastActualSendTime();
        if (timeSinceLastSend < minDelayMs) {
            return; // Framerate drop: the frame is arriving too quickly compared to the current ramp pacing
        }
        // Transmit the data
        MainSingleton.getInstance().output.write(ledsArray);
        MainSingleton.getInstance().output.flush();
        // Update used tokens and the last successful send timestamp
        grabber.setCurrentSecondToken(grabber.getCurrentSecondToken() + 1);
        grabber.setLastActualSendTime(currentTime);
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
                        } else if (!MainSingleton.getInstance().getImprovActive().isEmpty() && inputLine.contains(Constants.SERIAL_IMPROV) || inputLine.contains(Constants.SERIAL_IMPROV_ETH)) {
                            MainSingleton.getInstance().improvActive = "";
                            MainSingleton.getInstance().guiManager.pipelineManager.startCapturePipeline();
                            log.info(CommonUtility.getWord(Constants.FIRMWARE_PROGRAM_NOTIFY_HEADER));
                            MainSingleton.getInstance().guiManager.showLocalizedNotification(CommonUtility.getWord(Constants.FIRMWARE_PROGRAM_NOTIFY),
                                    CommonUtility.getWord(Constants.FIRMWARE_PROGRAM_NOTIFY_HEADER), Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.INFO);
                        }
                    }
                }
            });
        } catch (Exception e) {
            // We don't care about this exception, it's caused by unknown serial messages
        }
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
        Color[] leds = new Color[1];
        try {
            leds[0] = new Color(r, g, b);
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
