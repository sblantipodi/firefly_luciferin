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
import lombok.Getter;
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
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.network.NetworkSingleton;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serial port utility
 */
@Slf4j
public class SerialManager {

    private static final String BOOT_BAUD_RATE = "BAUDRATE IN USE=";
    // Glow Worm sends its serial status every ten seconds when UDP streaming is idle.
    private static final long BAUD_RATE_PROBE_NANOS = TimeUnit.SECONDS.toNanos(11);
    private static final Object CONNECTION_LOCK = new Object();
    private static final ScheduledExecutorService CONNECTION_EXECUTOR = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("serial-connection").factory());
    private static long connectionRequest;
    private static volatile BaudRateAdjustment baudRateAdjustment;
    /**
     * -- GETTER --
     * Return the localized message while the port is trying alternative baud rates.
     *
     * @return current scan message, or {@code null} when no scan is in progress
     */
    @Getter
    private static volatile String baudRateScanNotice;
    private boolean alertSent;

    /**
     * Initialize Serial communication
     */
    public void initSerial() {
        initSerial("", "");
    }

    /**
     * Append device settings and a checksum to the serial payload.
     *
     * @param ledsArray serial frame buffer
     * @param j current write position
     * @param baudRateToSend firmware baud rate code, or zero to keep the current rate
     * @return position of the last byte written
     */
    private static int computeChecksumAndData(byte[] ledsArray, int j, int baudRateToSend) {
        // Checksunm params
        int totalLeds = MainSingleton.getInstance().ledNumber; // Real numbers of LEDs
        // Byte is limited to 255, use byte splitting via byte shifting
        int ledsCountHi = (totalLeds >> 8) & 0xFF;             // Byte high
        int ledsCountLo = totalLeds & 0xFF;                    // Byte low
        int brightnessToSend = (AudioSingleton.getInstance().AUDIO_BRIGHTNESS == 255 ? CommonUtility.getNightBrightness() : AudioSingleton.getInstance().AUDIO_BRIGHTNESS) & 0xff;
        int gpioToSend = (MainSingleton.getInstance().gpio) & 0xff;
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
     * Read available serial bytes and retain any unfinished line for the next read.
     *
     * @param serial  port to read from
     * @param pending buffer shared across reads from this port
     * @return complete lines received, in order
     */
    private static List<String> readSerialLines(SerialPort serial, StringBuilder pending) {
        int available = Math.min(serial.bytesAvailable(), 4096);
        if (available <= 0) {
            return List.of();
        }
        byte[] bytes = new byte[available];
        int read = serial.readBytes(bytes, bytes.length);
        if (read <= 0) {
            return List.of();
        }
        pending.append(new String(bytes, 0, read, StandardCharsets.UTF_8));
        List<String> lines = new ArrayList<>();
        int end;
        while ((end = pending.indexOf("\n")) >= 0) {
            lines.add(pending.substring(0, end).stripTrailing());
            pending.delete(0, end + 1);
        }
        if (pending.length() > 4096) {
            pending.setLength(0);
        }
        return lines;
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
     * Decode a baud rate from a firmware startup message or periodic status line.
     *
     * @param line complete serial line
     * @return supported baud rate reported by the firmware, or {@code null} if the line does not report one
     */
    static Enums.BaudRate reportedBaudRate(String line) {
        if (line.startsWith(BOOT_BAUD_RATE)) {
            return Enums.BaudRate.findByExtendedVal(line.substring(BOOT_BAUD_RATE.length()));
        }
        if (line.startsWith(Constants.SERIAL_BAUDRATE)) {
            try {
                return Enums.BaudRate.findByValue(Integer.parseInt(line.substring(Constants.SERIAL_BAUDRATE.length())));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Check whether the selected output identifies the serial port or uses automatic selection.
     *
     * @param selected   output device from the configuration
     * @param serialPort system name of the serial port
     * @return whether the configured output matches the port or is set to {@code AUTO}
     */
    public static boolean matchesOutputDevice(String selected, String serialPort) {
        return Constants.SERIAL_PORT_AUTO.equals(selected) || Objects.equals(selected, serialPort);
    }

    /**
     * Find the latest baud rate correction that still matches the active serial profile.
     *
     * @return applicable correction, or {@code null} when none applies
     */
    public static BaudRateAdjustment activeBaudRateAdjustment() {
        BaudRateAdjustment adjustment = baudRateAdjustment;
        MainSingleton main = MainSingleton.getInstance();
        var config = main.config;
        return adjustment != null && config != null && !config.isWirelessStream()
                && Objects.equals(adjustment.profile(), main.profileArg)
                && matchesOutputDevice(config.getOutputDevice(), adjustment.port())
                && Objects.equals(config.getBaudRate(), adjustment.detectedRate()) ? adjustment : null;
    }

    /**
     * Update the web scan status and send a desktop notification when scanning starts.
     *
     * @param message localized scan notice, or {@code null} to clear the web status
     */
    private static void setBaudRateScanNotice(String message) {
        if (Objects.equals(baudRateScanNotice, message)) {
            return;
        }
        baudRateScanNotice = message;
        MainSingleton main = MainSingleton.getInstance();
        if (message != null && main.guiManager != null && !main.isHeadlessMode()) {
            GuiManager gui = main.guiManager;
            Platform.runLater(() -> gui.showSerialBaudRateScanNotice(message));
        }
    }

    /**
     * Write and flush a complete frame while holding the output stream lock.
     *
     * @param output active serial output stream
     * @param frame  encoded serial frame
     * @throws IOException if writing or flushing the frame fails
     */
    private static void writeSerialFrame(OutputStream output, byte[] frame) throws IOException {
        synchronized (output) {
            output.write(frame);
            output.flush();
        }
    }

    /**
     * Register the USB device, replay status lines read during detection, and enable serial output.
     *
     * @param serial       open serial port
     * @param request      identifier of the current connection request
     * @param statusLines  complete device status lines collected during detection
     * @param pending      buffer holding any partial line from the probe
     * @param ongoingProbe probe to accept a late response at the fallback rate, or {@code null} once detection is complete
     */
    private void finishSerialConnection(SerialPort serial, long request, List<String> statusLines, StringBuilder pending, BaudRateProbe ongoingProbe) {
        MainSingleton main = MainSingleton.getInstance();
        serial.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, main.config.getTimeout(), 0);
        log.info("{}{}", CommonUtility.getWord(LabelKey.SERIAL_PORT_IN_USE), serial.getSystemPortName());
        GuiSingleton.getInstance().deviceTableData.add(createDefaultDevice());
        statusLines.forEach(this::handleSerialEvent);
        log.info("Connected: Serial {} at {} baud", serial.getDescriptivePortName(), serial.getBaudRate());
        main.serialConnected = true;
        main.communicationError = false;
        main.output = serial.getOutputStream();
        listenSerialEvents(serial, request, pending, ongoingProbe);
        if (main.guiManager != null) {
            main.guiManager.trayIconManager.resetTray();
        }
    }

    /**
     * Process incoming status lines and reconnect when the active USB port disconnects.
     *
     * @param serial       port to monitor
     * @param request      identifier used to ignore events from a superseded connection
     * @param pending      buffer holding any partial line between reads
     * @param ongoingProbe probe awaiting a firmware baud rate report after fallback, or {@code null}
     */
    private void listenSerialEvents(SerialPort serial, long request, StringBuilder pending, BaudRateProbe ongoingProbe) {
        serial.addDataListener(new SerialPortDataListener() {
            private boolean awaitingRate = ongoingProbe != null;

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    for (String line : readSerialLines(serial, pending)) {
                        handleSerialEvent(line);
                        if (awaitingRate) {
                            Enums.BaudRate detected = reportedBaudRate(line);
                            if (detected != null) {
                                awaitingRate = false;
                                CONNECTION_EXECUTOR.execute(() -> {
                                    synchronized (CONNECTION_LOCK) {
                                        if (request == connectionRequest) {
                                            ongoingProbe.applyDetectedRate(detected);
                                        }
                                    }
                                });
                            }
                        }
                    }
                } else if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                    CONNECTION_EXECUTOR.execute(() -> {
                        synchronized (CONNECTION_LOCK) {
                            if (request == connectionRequest && serial == MainSingleton.getInstance().serial) {
                                log.info("USB device disconnected");
                                closeSerial();
                                initSerial();
                            }
                        }
                    });
                }
            }
        });
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
     * Queue a serial connection with optional port and baud rate overrides.
     *
     * @param portName explicit port name, or an empty string to use the configured output
     * @param baudrate explicit baud rate, or an empty string to detect it from the firmware
     */
    public void initSerial(String portName, String baudrate) {
        synchronized (CONNECTION_LOCK) {
            if (!MainSingleton.getInstance().config.isWirelessStream() || !portName.isEmpty()) {
                long request = ++connectionRequest;
                CONNECTION_EXECUTOR.execute(() -> connectSerial(request, portName, baudrate));
            }
        }
    }

    /**
     * Open the selected port and apply the explicit baud rate or start automatic detection.
     *
     * @param request  identifier of the queued connection request
     * @param portName explicit port name, or an empty string to use the configured output
     * @param baudRate explicit baud rate, or an empty string to enable detection
     */
    private void connectSerial(long request, String portName, String baudRate) {
        synchronized (CONNECTION_LOCK) {
            MainSingleton main = MainSingleton.getInstance();
            if (request != connectionRequest || (portName.isEmpty() && main.config.isWirelessStream())) {
                return;
            }
            try {
                closeSerialPort();
                SerialPort[] ports = SerialPort.getCommPorts();
                String selectedPort = portName.isEmpty() ? main.config.getOutputDevice() : portName;
                boolean auto = portName.isEmpty() && Constants.SERIAL_PORT_AUTO.equals(selectedPort);
                SerialPort serial = auto && ports.length > 0 ? ports[0] : Arrays.stream(ports)
                        .filter(port -> port.getSystemPortName().equals(selectedPort)).findFirst().orElse(null);
                if (serial != null) {
                    main.serial = serial;
                    serial.setDTRandRTS(false, false);
                    serial.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
                    if (serial.openPort()) {
                        if (auto && ports.length > 1) {
                            new GuiManager().showLocalizedNotification(LabelKey.SERIAL_PORT_AMBIGUOUS, LabelKey.SERIAL_PORT_AMBIGUOUS_CONTEXT,
                                    LabelKey.SERIAL_ERROR_TITLE, TrayIcon.MessageType.ERROR);
                            log.error(CommonUtility.getWord(LabelKey.SERIAL_ERROR_OPEN_HEADER));
                        }
                        if (baudRate.isEmpty()) {
                            new BaudRateProbe(serial, request).tryNextRate();
                            return;
                        }
                        if (serial.setComPortParameters(Integer.parseInt(baudRate), 8, 1, SerialPort.NO_PARITY)) {
                            finishSerialConnection(serial, request, List.of(), new StringBuilder(), null);
                            return;
                        }
                    }
                }
            } catch (RuntimeException e) {
                log.error("Could not open serial port", e);
            }
            retrySerialConnection(request, portName, baudRate);
            if (NativeExecutor.isLinux() && !alertSent) {
                alertSent = true;
                Platform.runLater(() -> {
                    String content = CommonUtility.getWord(LabelKey.USB_NOT_AVAILABLE_CONTENT);
                    if (NativeExecutor.isSnap()) {
                        content += CommonUtility.getWord(LabelKey.USB_NOT_AVAILABLE_CONTENT_SNAP);
                    }
                    main.guiManager.showAlert(CommonUtility.getWord(LabelKey.USB_NOT_AVAILABLE_TITLE),
                            CommonUtility.getWord(LabelKey.USB_NOT_AVAILABLE_HEADER), content, Alert.AlertType.WARNING);
                });
            }
        }
    }

    /**
     * Close the failed connection and schedule another attempt with the same parameters.
     *
     * @param request  identifier used to discard the retry if a newer request supersedes it
     * @param portName explicit port name, or an empty string to use the configured output
     * @param baudRate explicit baud rate, or an empty string to enable detection
     */
    private void retrySerialConnection(long request, String portName, String baudRate) {
        closeSerialPort();
        MainSingleton.getInstance().communicationError = true;
        CONNECTION_EXECUTOR.schedule(() -> connectSerial(request, portName, baudRate), 5, TimeUnit.SECONDS);
    }

    /**
     * Send color info via USB Serial
     *
     * @param leds array with colors
     * @throws IOException can't write to serial
     */
    public void sendColorsViaUSB(Color[] leds) throws IOException {
        // The firmware treats a zero baud rate code as no change.
        sendColorsViaUSB(leds, 0);
    }

    /**
     * Send LED colors and device settings once the serial firmware version has been verified.
     *
     * @param leds           LED colors, or a single color for the whole strip
     * @param baudRateToSend firmware baud rate code, or zero to keep the current rate
     * @throws IOException if writing to the serial port fails
     */
    private void sendColorsViaUSB(Color[] leds, int baudRateToSend) throws IOException {
        OutputStream output = MainSingleton.getInstance().output;
        if (output == null) {
            return;
        }
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
        }
        if (ManagerSingleton.getInstance().serialVersionOk) {
            byte[] ledsArray = implementRleCompressionLogic(leds, baudRateToSend);
            if (baudRateToSend == 0) {
                writeToSerialWithPacing(output, ledsArray);
            } else {
                writeSerialFrame(output, ledsArray);
            }
        }
    }

    /**
     * Implement RLE compression logic
     *
     * @param leds array with colors
     * @param baudRateToSend firmware code for an explicit baud rate change, or zero
     * @return leds array
     */
    private byte[] implementRleCompressionLogic(Color[] leds, int baudRateToSend) {
        int j = -1;
        // Create new RLE leaders (grouping + RLE entries are computed once inside builtRleLeaders)
        NetworkSingleton.RleLeadersResult rleLeadersResult = NetworkSingleton.builtRleLeaders(leds);
        LinkedHashMap<Integer, LEDCoordinate> ledMatrixWithLeaders = rleLeadersResult.ledMatrix();
        List<Color> leaderColors = new ArrayList<>();
        int ledIndex = 0;
        for (LEDCoordinate coord : ledMatrixWithLeaders.values()) {
            if (!coord.isGroupedLed()) {
                leaderColors.add(leds[ledIndex]);
            }
            ledIndex++;
        }
        // Reuse the RLE entries already computed by builtRleLeaders, do not recompute the grouping here.
        // Count and size are always <= 255 by construction (see NetworkSingleton#computeRleEntries),
        // so narrowing to byte is safe.
        List<byte[]> rleEntries = new ArrayList<>(rleLeadersResult.rleEntries().size());
        for (int[] entry : rleLeadersResult.rleEntries()) {
            rleEntries.add(new byte[]{(byte) entry[0], (byte) entry[1]});
        }
        byte numRleEntries = (byte) rleEntries.size();
        // Dyanmic buffer size
        int rleHeaderBytes = (2 + (rleEntries.size() * 2));
        int colorBytesCount = (leaderColors.size() * 3);
        // Visual debug output
        printRleMaps(rleEntries, ledMatrixWithLeaders, leds.length);
        byte[] ledsArray = new byte[Constants.SERIAL_PARAMS + rleHeaderBytes + colorBytesCount];
        // Checksum with data config
        j = computeChecksumAndData(ledsArray, j, baudRateToSend);
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
     * @param output active serial stream
     * @param ledsArray encoded serial frame containing LED colors and device settings
     * @throws IOException can't write to serial
     */
    private void writeToSerialWithPacing(OutputStream output, byte[] ledsArray) throws IOException {
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
        int currentMaxFramerate = (int) Math.clamp(targetFramerate * curvedProgress, 1, targetFramerate);
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
        writeSerialFrame(output, ledsArray);
        // Update used tokens and the last successful send timestamp
        grabber.setCurrentSecondToken(grabber.getCurrentSecondToken() + 1);
        grabber.setLastActualSendTime(currentTime);
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
                            if (!deviceVer.equals(glowWormDevice.getDeviceVersion())) {
                                glowWormDevice.setMatterSupported(false);
                            }
                            glowWormDevice.setDeviceVersion(deviceVer);
                        } else if (inputLine.contains(Constants.SERIAL_LED_NUM)) {
                            glowWormDevice.setNumberOfLEDSconnected(inputLine.replace(Constants.SERIAL_LED_NUM, ""));
                        } else if (inputLine.contains(Constants.SERIAL_BOARD)) {
                            glowWormDevice.setDeviceBoard(inputLine.replace(Constants.SERIAL_BOARD, ""));
                        } else if (inputLine.startsWith(Constants.SERIAL_MATTER)) {
                            glowWormDevice.setMatterSupported(Boolean.parseBoolean(inputLine.substring(Constants.SERIAL_MATTER.length())));
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
                        } else if (inputLine.startsWith(Constants.SERIAL_BAUDRATE) || inputLine.startsWith(BOOT_BAUD_RATE)) {
                            Enums.BaudRate rate = reportedBaudRate(inputLine);
                            if (rate != null) {
                                glowWormDevice.setBaudRate(rate.getBaudRate());
                            }
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
                            log.info(CommonUtility.getWord(LabelKey.FIRMWARE_PROGRAM_NOTIFY_HEADER));
                            MainSingleton.getInstance().guiManager.showLocalizedNotification(CommonUtility.getWord(LabelKey.FIRMWARE_PROGRAM_NOTIFY),
                                    CommonUtility.getWord(LabelKey.FIRMWARE_PROGRAM_NOTIFY_HEADER), Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.INFO);
                        }
                    }
                }
            });
        } catch (Exception e) {
            // We don't care about this exception, it's caused by unknown serial messages
            log.trace("Unknown serial message ignored", e);
        }
    }

    /**
     * Send the current LED and device parameters over serial.
     *
     * @param r red channel value
     * @param g green channel value
     * @param b blue channel value
     */
    public void sendSerialParams(int r, int g, int b) {
        sendSerialParams(r, g, b, 0);
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
     * Send a solid color and device settings with an optional firmware baud rate change.
     *
     * @param r              red channel value
     * @param g              green channel value
     * @param b              blue channel value
     * @param baudRateToSend firmware baud rate code, or zero to keep the current rate
     */
    public void sendSerialParams(int r, int g, int b, int baudRateToSend) {
        try {
            sendColorsViaUSB(new Color[]{new Color(r, g, b)}, baudRateToSend);
        } catch (IOException e) {
            log.error(e.getMessage());
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
     * Close serial communication and remove all attached listeners
     */
    public void closeSerial() {
        synchronized (CONNECTION_LOCK) {
            connectionRequest++;
            closeSerialPort();
        }
    }

    /**
     * Release the current serial port and clear its device entry while keeping the connection request valid for retries.
     */
    private void closeSerialPort() {
        setBaudRateScanNotice(null);
        log.debug("Closing serial connection");
        MainSingleton main = MainSingleton.getInstance();
        OutputStream output = main.output;
        main.output = null;
        main.serialConnected = false;
        ManagerSingleton.getInstance().serialVersionOk = false;
        if (output != null) {
            try {
                output.flush();
                output.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        if (main.serial != null) {
            main.serial.removeDataListener();
            main.serial.setDTRandRTS(false, false);
            main.serial.closePort();
            main.serial = null;
            GuiSingleton.getInstance().deviceTableData.removeIf(entry -> Constants.USB_DEVICE.equals(entry.getDeviceName()));
        }
    }

    /**
     * A baud rate correction saved to the active profile and shown in the desktop and web interfaces.
     *
     * @param profile      active profile identifier
     * @param port         system name of the serial port
     * @param previousRate baud rate previously stored in the profile
     * @param detectedRate baud rate reported by the device and saved to the profile
     * @param message      localized explanation of the correction
     */
    public record BaudRateAdjustment(String profile, String port, String previousRate, String detectedRate,
                                     String message) {
    }

    /**
     * Listen for firmware status while trying the baud rates supported by Luciferin.
     */
    private final class BaudRateProbe implements Runnable {
        private final SerialPort serial;
        private final long request;
        private final String profile;
        private final String configuredRate;
        private final Enums.BaudRate fallback;
        private final Iterator<Enums.BaudRate> rates;
        private final StringBuilder pending = new StringBuilder();
        private final Deque<String> statusLines = new ArrayDeque<>();
        private boolean tryingConfiguredRate;
        private long deadline;

        /**
         * Capture the active profile and build the list of rates to try, starting with the saved rate when supported.
         *
         * @param serial  open port to probe
         * @param request identifier of the connection that owns the probe
         */
        private BaudRateProbe(SerialPort serial, long request) {
            this.serial = serial;
            this.request = request;
            MainSingleton main = MainSingleton.getInstance();
            var config = main.config;
            profile = main.profileArg;
            configuredRate = config.getBaudRate();
            Enums.BaudRate preferred = config.isFullFirmware() ? Enums.BaudRate.BAUD_RATE_115200 : Enums.BaudRate.BAUD_RATE_500000;
            Enums.BaudRate saved = Enums.BaudRate.findByExtendedVal(configuredRate);
            tryingConfiguredRate = saved != null;
            fallback = saved == null ? preferred : saved;
            Set<Enums.BaudRate> candidates = new LinkedHashSet<>(List.of(fallback, preferred));
            candidates.add(config.isFullFirmware() ? Enums.BaudRate.BAUD_RATE_500000 : Enums.BaudRate.BAUD_RATE_115200);
            candidates.addAll(List.of(Enums.BaudRate.values()));
            rates = candidates.iterator();
        }

        /**
         * Log and try each candidate until the driver accepts one, or restore the fallback rate when the list is exhausted.
         */
        private void tryNextRate() {
            pending.setLength(0);
            statusLines.clear();
            while (rates.hasNext()) {
                if (!tryingConfiguredRate && baudRateScanNotice == null) {
                    log.info("Scanning for the device baud rate on {}", serial.getSystemPortName());
                    setBaudRateScanNotice(CommonUtility.getWord(LabelKey.BAUDRATE_SCAN_MESSAGE).replace("{0}", serial.getSystemPortName()));
                }
                tryingConfiguredRate = false;
                int rate = Integer.parseInt(rates.next().getBaudRate());
                log.info("Trying {} baud on {}", rate, serial.getSystemPortName());
                serial.flushIOBuffers();
                if (serial.setComPortParameters(rate, 8, 1, SerialPort.NO_PARITY)) {
                    deadline = System.nanoTime() + BAUD_RATE_PROBE_NANOS;
                    CONNECTION_EXECUTOR.schedule(this, 50, TimeUnit.MILLISECONDS);
                    return;
                }
            }
            setBaudRateScanNotice(null);
            if (serial.setBaudRate(Integer.parseInt(fallback.getBaudRate()))) {
                log.warn("Could not detect baud rate on {}. Using configured rate {}", serial.getSystemPortName(), fallback.getBaudRate());
                finishSerialConnection(serial, request, List.of(), pending, this);
            } else {
                retrySerialConnection(request, "", "");
            }
        }

        /**
         * Poll only while this connection request is current and retry the connection if polling fails.
         */
        @Override
        public void run() {
            synchronized (CONNECTION_LOCK) {
                if (request != connectionRequest) {
                    return;
                }
                try {
                    poll();
                } catch (RuntimeException e) {
                    log.error("Could not detect baud rate on {}", serial.getSystemPortName(), e);
                    retrySerialConnection(request, "", "");
                }
            }
        }

        /**
         * Check for a firmware report and advance to the next baud rate when the current attempt times out.
         */
        private void poll() {
            if (serial.bytesAvailable() < 0) {
                retrySerialConnection(request, "", "");
                return;
            }
            Enums.BaudRate detected = null;
            for (String line : readSerialLines(serial, pending)) {
                if (statusLines.size() == 32) {
                    statusLines.removeFirst();
                }
                statusLines.addLast(line);
                Enums.BaudRate reported = reportedBaudRate(line);
                if (reported != null) {
                    detected = reported;
                }
            }
            if (detected != null) {
                setBaudRateScanNotice(null);
                applyDetectedRate(detected);
                finishSerialConnection(serial, request, List.copyOf(statusLines), pending, null);
            } else if (System.nanoTime() >= deadline) {
                tryNextRate();
            } else {
                CONNECTION_EXECUTOR.schedule(this, 50, TimeUnit.MILLISECONDS);
            }
        }

        /**
         * Apply the reported rate to the port and publish an alert after a profile correction is saved.
         *
         * @param detected supported baud rate reported by the firmware
         */
        private void applyDetectedRate(Enums.BaudRate detected) {
            int workingRate = serial.getBaudRate();
            String detectedRate = detected.getBaudRate();
            int reportedRate = Integer.parseInt(detectedRate);
            String port = serial.getSystemPortName();
            if (reportedRate != workingRate) {
                log.info("Applying detected rate of {} baud on {}", reportedRate, port);
                if (!serial.setBaudRate(reportedRate)) {
                    serial.setBaudRate(workingRate);
                    log.warn("Could not apply reported rate {} on {}", reportedRate, port);
                }
            }
            if (Objects.equals(configuredRate, detectedRate)) {
                return;
            }
            try {
                if (!new StorageManager().writeDetectedBaudRate(profile, port, configuredRate, detectedRate)) {
                    return;
                }
                String message = CommonUtility.getWord(LabelKey.BAUDRATE_MISMATCH_MESSAGE)
                        .replace("{0}", port).replace("{1}", Objects.toString(configuredRate, Constants.DASH)).replace("{2}", detectedRate);
                BaudRateAdjustment adjustment = new BaudRateAdjustment(profile, port, configuredRate, detectedRate, message);
                baudRateAdjustment = adjustment;
                log.warn("Baud rate mismatch on {}: updated the profile from {} to {} baud", port, configuredRate, detectedRate);
                GuiManager gui = MainSingleton.getInstance().guiManager;
                if (gui != null) {
                    Platform.runLater(() -> gui.showSerialBaudRateAdjustment(adjustment));
                }
            } catch (IOException e) {
                log.error("Could not save detected baud rate {} on {}", detectedRate, port, e);
            }
        }
    }

}
