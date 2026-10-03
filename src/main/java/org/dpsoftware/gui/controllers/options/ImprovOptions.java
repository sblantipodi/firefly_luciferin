/*
  ImprovOptions.java

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
package org.dpsoftware.gui.controllers.options;

import com.fasterxml.jackson.databind.JsonNode;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.managers.SerialManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Options and serial provisioning shared by the JavaFX Improv dialog and web settings page.
 */
public final class ImprovOptions {

    private static final AtomicBoolean PROVISIONING = new AtomicBoolean();
    private static final Pattern SSID_PATTERN = Pattern.compile("SSID\\s*\\d+\\s*:\\s*(.+)");
    private static final Pattern SIGNAL_PATTERN = Pattern.compile("(?i)(Signal|Segnale|Signalstärke|Señal|Сигнал|Jelerősség|Sygnał)\\s*:\\s*(\\d+)%");

    /**
     * Prevents instantiation.
     */
    private ImprovOptions() {
    }

    /**
     * Reads the web provisioning values from a JSON object without relying on record binding.
     *
     * @param payload JSON request from the web form
     * @return provisioning values
     */
    public static ProvisionRequest fromJson(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("Provisioning request must be an object");
        }
        return new ProvisionRequest(
                text(payload, "ssid"), text(payload, "wifiPassword"), text(payload, "deviceName"),
                text(payload, "comPort"), text(payload, "baudRate"), text(payload, "ethernetMode"),
                text(payload, "ethernetBoard"), text(payload, "mi"), text(payload, "mo"),
                text(payload, "sck"), text(payload, "cs"), payload.path("mqttEnabled").asBoolean(false),
                text(payload, "mqttHost"), text(payload, "mqttPort"), text(payload, "mqttUser"),
                text(payload, "mqttPassword"));
    }

    /**
     * Reads a textual provisioning field, returning an empty value when it is missing.
     *
     * @param payload JSON request
     * @param field   field name
     * @return field text
     */
    private static String text(JsonNode payload, String field) {
        JsonNode value = payload.path(field);
        return value.isTextual() ? value.textValue() : "";
    }

    /**
     * Lists the baud rates offered by the Improv dialog.
     *
     * @return serial baud rate labels
     */
    public static List<String> baudRates() {
        return Arrays.stream(Enums.BaudRate.values()).map(Enums.BaudRate::getBaudRate).toList();
    }

    /**
     * Lists serial ports currently available for device provisioning.
     *
     * @return serial port names
     */
    public static List<String> serialPorts() {
        return new ArrayList<>(new SerialManager().getAvailableDevices().keySet());
    }

    /**
     * Lists Wi-Fi SSIDs detected by Windows, strongest signal first.
     *
     * @return detected SSIDs, or an empty list when scanning is unavailable
     */
    public static List<String> wifiSsids() {
        if (!NativeExecutor.isWindows()) {
            return List.of();
        }
        try {
            List<String> output = NativeExecutor.runNative(new String[]{"netsh", "wlan", "show", "networks", "mode=bssid"});
            Map<String, Integer> networks = new HashMap<>();
            String currentSsid = null;
            for (String line : output) {
                Matcher ssid = SSID_PATTERN.matcher(line.trim());
                if (ssid.matches()) {
                    currentSsid = ssid.group(1).trim();
                    networks.putIfAbsent(currentSsid, -100);
                    continue;
                }
                Matcher signal = SIGNAL_PATTERN.matcher(line.trim());
                if (signal.matches() && currentSsid != null) {
                    networks.merge(currentSsid, Integer.parseInt(signal.group(2)), Math::max);
                }
            }
            return networks.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .map(Map.Entry::getKey).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * Checks whether the selected Ethernet mode needs custom SPI pins.
     *
     * @param ethernetMode enum name of the selected Ethernet option
     * @return true for custom SPI boards
     */
    public static boolean needsSpiPins(String ethernetMode) {
        return Enums.EthernetOptions.ETH_CUSTOM_SPI.name().equals(ethernetMode);
    }

    /**
     * Checks whether the selected Ethernet mode needs a prebuilt board choice.
     *
     * @param ethernetMode enum name of the selected Ethernet option
     * @return true for prebuilt boards
     */
    public static boolean needsEthernetBoard(String ethernetMode) {
        return Enums.EthernetOptions.ETH_PREBUILT.name().equals(ethernetMode);
    }

    /**
     * Validates the values used to provision the device.
     *
     * @param request provisioning values
     * @return an existing localization key for the first error, or null when valid
     */
    public static String validationError(ProvisionRequest request) {
        if (request == null) {
            return "device.provision.error.header";
        }
        Enums.EthernetOptions mode;
        try {
            mode = Enums.EthernetOptions.valueOf(request.ethernetMode());
        } catch (RuntimeException e) {
            return "fxml.mqtttab.improv.ethernet";
        }
        if (mode == Enums.EthernetOptions.ETH_NO_ETH
                && (request.ssid() == null || request.ssid().isBlank()
                || request.wifiPassword() == null || request.wifiPassword().isBlank())) {
            return "fxml.mqtttab.improv.wifi.error";
        }
        if (mode == Enums.EthernetOptions.ETH_CUSTOM_SPI
                && (!pinValid(request.mi()) || !pinValid(request.mo())
                || !pinValid(request.sck()) || !pinValid(request.cs()))) {
            return "fxml.mqtttab.improv.eth.field.error";
        }
        if (mode == Enums.EthernetOptions.ETH_PREBUILT
                && Arrays.stream(Enums.EthernetBoards.values()).noneMatch(board -> board.getValue().equals(request.ethernetBoard()))) {
            return "fxml.mqtttab.improv.ethernet";
        }
        if (request.deviceName() == null || request.deviceName().isBlank()) {
            return "fxml.mqtttab.improv.devicename";
        }
        if (containsLineBreak(request.deviceName()) || containsLineBreak(request.ssid())
                || containsLineBreak(request.wifiPassword())) {
            return "device.provision.error.header";
        }
        if (request.mqttEnabled() && (request.mqttHost() == null
                || !request.mqttHost().matches("[^\\s:/]+")
                || request.mqttPort() == null || !request.mqttPort().matches("[0-9]{1,5}")
                || Integer.parseInt(request.mqttPort()) < 1 || Integer.parseInt(request.mqttPort()) > 65535
                || containsLineBreak(request.mqttUser()) || containsLineBreak(request.mqttPassword()))) {
            return "fxml.mqtttab.mqttserverhost";
        }
        if (request.comPort() == null || !serialPorts().contains(request.comPort())) {
            return "device.provision.error.header";
        }
        if (request.baudRate() == null || !baudRates().contains(request.baudRate())) {
            return "device.provision.error.header";
        }
        return null;
    }

    /**
     * Checks whether a custom SPI pin is a nonnegative decimal integer.
     *
     * @param pin entered pin
     * @return true when the pin is valid
     */
    private static boolean pinValid(String pin) {
        return pin != null && pin.matches("[0-9]+");
    }

    /**
     * Prevents line breaks from changing the line based serial command structure.
     *
     * @param value field value
     * @return true when the value contains a line break
     */
    private static boolean containsLineBreak(String value) {
        return value != null && (value.contains("\n") || value.contains("\r"));
    }

    /**
     * Sends the custom Improv command over an already opened serial output.
     *
     * @param request provisioning values
     * @throws IOException if the serial output fails
     */
    public static void sendEthernetConfig(ProvisionRequest request) throws IOException {
        if (MainSingleton.getInstance().output == null) {
            throw new IOException("Serial output is unavailable");
        }
        MainSingleton.getInstance().improvActive = request.deviceName();
        MainSingleton.getInstance().output.write(Constants.IMPROV_CUSTOM_HEADER);
        int ethernetDevice = Enums.EthernetOptions.ETH_NO_ETH.getOptionId();
        if (needsSpiPins(request.ethernetMode())) {
            ethernetDevice = Enums.EthernetOptions.ETH_CUSTOM_SPI.getOptionId();
        } else if (needsEthernetBoard(request.ethernetMode())) {
            ethernetDevice = Arrays.stream(Enums.EthernetBoards.values())
                    .filter(board -> board.getValue().equals(request.ethernetBoard()))
                    .findFirst().orElseThrow().getEthBoardId();
        }
        String[] values = {Constants.BREK_IMPROV, request.deviceName(), Constants.STATE_DHCP.toUpperCase(),
                request.ssid(), request.wifiPassword(), Constants.OTA_PWD,
                request.mqttEnabled() ? request.mqttHost() : "", request.mqttEnabled() ? request.mqttPort() : "",
                request.mqttEnabled() ? request.mqttUser() : "", request.mqttEnabled() ? request.mqttPassword() : "",
                String.valueOf(ethernetDevice), request.mi(), request.mo(), request.sck(), request.cs()};
        for (String value : values) {
            MainSingleton.getInstance().output.write(((value == null ? "" : value) + "\n").getBytes(StandardCharsets.US_ASCII));
            MainSingleton.getInstance().output.flush();
        }
    }

    /**
     * Opens the selected serial port and retries the provisioning command up to five times.
     *
     * @param request validated provisioning values
     * @return a future that completes when the command was sent or retries were exhausted
     */
    public static CompletableFuture<Boolean> provision(ProvisionRequest request) {
        if (!PROVISIONING.compareAndSet(false, true)) {
            throw new IllegalStateException("Device provisioning is already running");
        }
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        try {
            if (MainSingleton.getInstance().guiManager != null
                    && MainSingleton.getInstance().guiManager.pipelineManager != null) {
                MainSingleton.getInstance().guiManager.pipelineManager.stopCapturePipeline();
            }
            new SerialManager().initSerial(request.comPort(), request.baudRate());
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "improv-provisioning");
                thread.setDaemon(true);
                return thread;
            });
            AtomicInteger attempts = new AtomicInteger();
            scheduler.scheduleAtFixedRate(() -> {
                boolean sent = false;
                try {
                    if (MainSingleton.getInstance().config != null && MainSingleton.getInstance().serial != null
                            && MainSingleton.getInstance().serial.isOpen()) {
                        sendEthernetConfig(request);
                        sent = true;
                    }
                } catch (Exception ignored) {
                    // The next scheduled attempt may succeed after the serial port opens.
                }
                if (sent || attempts.incrementAndGet() >= 5) {
                    scheduler.shutdown();
                    PROVISIONING.set(false);
                    result.complete(sent);
                }
            }, 100, 2000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            PROVISIONING.set(false);
            result.completeExceptionally(e);
        }
        return result;
    }

    /**
     * Values sent to a device by the Improv provisioning action.
     */
    public record ProvisionRequest(String ssid, String wifiPassword, String deviceName,
                                   String comPort, String baudRate, String ethernetMode, String ethernetBoard,
                                   String mi, String mo, String sck, String cs,
                                   boolean mqttEnabled, String mqttHost, String mqttPort,
                                   String mqttUser, String mqttPassword) {
    }
}
