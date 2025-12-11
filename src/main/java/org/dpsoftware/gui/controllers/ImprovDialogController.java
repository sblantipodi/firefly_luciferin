/*
  ImprovDialogController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.gui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.managers.SerialManager;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dialog controller for the IMPROV WiFi protocol management
 */
@Slf4j
public class ImprovDialogController {

    @FXML
    public ComboBox<String> ssid;
    @FXML
    public PasswordField wifiPwd;
    @FXML
    public ComboBox<String> baudrate;
    @FXML
    public ComboBox<String> comPort;
    @FXML
    public Button okButton;
    @FXML
    public Button cancelButton;
    @FXML
    private SettingsController settingsController;
    @FXML
    public TextField deviceName;

    /**
     * Inject main controller containing the TabPane
     *
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {
        Platform.runLater(() -> {
            if (NativeExecutor.isWindows()) {
                initSsid();
            }
            for (Enums.BaudRate br : Enums.BaudRate.values()) {
                baudrate.getItems().add(br.getBaudRate());
            }
            SerialManager serialManager = new SerialManager();
            Map<String, Boolean> availableDevices = serialManager.getAvailableDevices();
            availableDevices.forEach((portName, _) -> comPort.getItems().add(portName));
            if (comPort != null && !comPort.getItems().isEmpty()) {
                comPort.setValue(comPort.getItems().getFirst());
            }
            deviceName.setText(MainSingleton.getInstance().config.getOutputDevice());
            baudrate.setValue(Enums.BaudRate.BAUD_RATE_115200.getBaudRate());
        });
    }

    /**
     * Set tooltips
     */
    private void setTooltips() {
        GuiManager.createTooltip(Constants.TOOLTIP_IMPROV_SSID, ssid);
        GuiManager.createTooltip(Constants.TOOLTIP_IMPROV_PWD, wifiPwd);
        GuiManager.createTooltip(Constants.TOOLTIP_IMPROV_COM, comPort);
        GuiManager.createTooltip(Constants.TOOLTIP_IMPROV_BAUD, baudrate);
        GuiManager.createTooltip(Constants.TOOLTIP_DEV_NAME, deviceName);
    }

    /**
     * Init default values
     */
    public void initDefaultValues() {

    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile() {
        setTooltips();
    }

    /**
     * Close dialog
     *
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save and close dialog
     *
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        ssid.commitValue();
        wifiPwd.commitValue();
        baudrate.commitValue();
        comPort.commitValue();
        manageImprov();
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Program device using the IMPROV WiFi protocol
     */
    private void manageImprov() {
        SerialManager serialManager = new SerialManager();
        serialManager.initSerial(comPort.getValue(), baudrate.getValue());
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger retryNumber = new AtomicInteger(0);
        MainSingleton.getInstance().guiManager.pipelineManager.stopCapturePipeline();
        final int MAX_RETRY = 5;
        Runnable checkAndRun = () -> {
            boolean improvError;
            retryNumber.getAndIncrement();
            log.debug("Trying to send an Improv WiFi command");
            if (MainSingleton.getInstance().config != null && MainSingleton.getInstance().serial != null && MainSingleton.getInstance().serial.isOpen()) {
                MainSingleton.getInstance().config.setOutputDevice(settingsController.modeTabController.serialPort.getValue());
                MainSingleton.getInstance().config.setMqttEnable(settingsController.networkTabController.mqttEnable.isSelected());
                if (MainSingleton.getInstance().config.isMqttEnable()) {
                    MainSingleton.getInstance().config.setMqttServer(settingsController.networkTabController.mqttHost.getText()
                            + ":" + settingsController.networkTabController.mqttPort.getText());
                    MainSingleton.getInstance().config.setMqttTopic(settingsController.networkTabController.mqttTopic.getText());
                    MainSingleton.getInstance().config.setMqttUsername(settingsController.networkTabController.mqttUser.getText());
                    MainSingleton.getInstance().config.setMqttPwd(settingsController.networkTabController.mqttPwd.getText());
                }
                try {
                    improvError = sendImprov();
                } catch (IOException ex) {
                    log.error(ex.getMessage());
                    improvError = true;
                }
            } else {
                improvError = true;
            }
            if (!improvError || retryNumber.get() >= MAX_RETRY) {
                scheduler.shutdown();
                if (MainSingleton.getInstance().communicationError) {
                    MainSingleton.getInstance().guiManager.showLocalizedNotification(CommonUtility.getWord(Constants.FIRMWARE_PROVISION_NOTIFY),
                            CommonUtility.getWord(Constants.FIRMWARE_PROVISION_NOTIFY_HEADER), Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.ERROR);
                }
            }
        };
        scheduler.scheduleAtFixedRate(checkAndRun, 100, 2000, TimeUnit.MILLISECONDS);
    }

    /**
     * Send improv wifi msg
     *
     * @return error
     * @throws IOException can't open port
     */
    private boolean sendImprov() throws IOException {
        byte version = 0x01;
        byte rpcPacketType = 0x03;
        byte rpcCommandType = 0x01;
        byte[] ssidBytes = ssid.getValue().getBytes(StandardCharsets.UTF_8);
        byte[] passBytes = wifiPwd.getText().getBytes(StandardCharsets.UTF_8);
        int dataLen = 1 + ssidBytes.length + 1 + passBytes.length;
        int packetLen = Constants.IMPROV_HEADER.length + 1 + 1 + 1 + 1 + 1 + dataLen + 1;
        byte[] packet = new byte[packetLen];
        int idx = 0;
        // Header
        for (byte b : Constants.IMPROV_HEADER) packet[idx++] = b;
        packet[idx++] = version;
        packet[idx++] = rpcPacketType;
        packet[idx++] = (byte) (dataLen + 2);
        packet[idx++] = rpcCommandType;
        // Data
        packet[idx++] = (byte) (ssidBytes.length + passBytes.length);
        packet[idx++] = (byte) ssidBytes.length;
        System.arraycopy(ssidBytes, 0, packet, idx, ssidBytes.length);
        idx += ssidBytes.length;
        packet[idx++] = (byte) passBytes.length;
        System.arraycopy(passBytes, 0, packet, idx, passBytes.length);
        idx += passBytes.length;
        // Checksum
        int checksum = 0;
        for (int i = 0; i < idx; i++) {
            checksum += packet[i] & 0xFF;
        }
        byte checksumByte = (byte) (checksum & 0xFF);
        packet[idx] = checksumByte;
        if (MainSingleton.getInstance().output != null) {
            log.debug("Improv WiFi packet sent");
            MainSingleton.getInstance().improvActive = deviceName.getText();
            MainSingleton.getInstance().output.write(packet);
        } else {
            return true;
        }
        return false;
    }

    /**
     * Save button from main controller
     *
     */
    @FXML
    @SuppressWarnings("Duplicates")
    public void save() {
    }

    /**
     * Initialize the SSID combo box with WLAN SSID
     */
    private void initSsid() {
        try {
            List<String> output = NativeExecutor.runNative(new String[]{"netsh", "wlan", "show", "networks", "mode=bssid"});
            class NetworkInfo {
                final String ssid;
                int maxSignal;
                boolean is24GHz;

                NetworkInfo(String ssid) {
                    this.ssid = ssid;
                    this.maxSignal = -100;
                    this.is24GHz = false;
                }
            }
            Map<String, NetworkInfo> networks = new HashMap<>();
            NetworkInfo currentNetwork = null;
            Pattern ssidPattern = Pattern.compile("SSID\\s*\\d+\\s*:\\s*(.+)");
            Pattern signalPattern = Pattern.compile(
                    "(?i)(Signal|Segnale|Signalstärke|Señal|Сигнал|Jelerősség|Sygnał)\\s*:\\s*(\\d+)%"
            );
            Pattern channelPattern = Pattern.compile(
                    "(?i)(Channel|Canale|Canal|Kanal|Канал|Csatorna|Kanał)\\s*:\\s*(\\d+)"
            );
            for (String line : output) {
                line = line.trim();
                Matcher ssidMatcher = ssidPattern.matcher(line);
                if (ssidMatcher.matches()) {
                    String ssidName = ssidMatcher.group(1).trim();
                    if (!networks.containsKey(ssidName)) {
                        networks.put(ssidName, new NetworkInfo(ssidName));
                    }
                    currentNetwork = networks.get(ssidName);
                }
                Matcher signalMatcher = signalPattern.matcher(line);
                if (signalMatcher.matches() && currentNetwork != null) {
                    int signalStrength = Integer.parseInt(signalMatcher.group(2));
                    if (signalStrength > currentNetwork.maxSignal) {
                        currentNetwork.maxSignal = signalStrength;
                    }
                }
                Matcher channelMatcher = channelPattern.matcher(line);
                if (channelMatcher.matches() && currentNetwork != null) {
                    int channel = Integer.parseInt(channelMatcher.group(2));
                    // Channel 1-14 refers to 2.4GHz
                    if (channel >= 1 && channel <= 14) {
                        currentNetwork.is24GHz = true;
                    }
                }
            }
            List<NetworkInfo> networks24GHz = new ArrayList<>();
            List<NetworkInfo> allNetworks = new ArrayList<>();
            for (NetworkInfo network : networks.values()) {
                if (network.is24GHz) {
                    networks24GHz.add(network);
                }
                allNetworks.add(network);
            }
            Comparator<NetworkInfo> signalComparator = (a, b) -> Integer.compare(b.maxSignal, a.maxSignal);
            networks24GHz.sort(signalComparator);
            allNetworks.sort(signalComparator);
            ssid.getItems().clear();
            if (!networks24GHz.isEmpty()) {
                for (NetworkInfo network : networks24GHz) {
                    ssid.getItems().add(network.ssid);
                }
            } else {
                for (NetworkInfo network : allNetworks) {
                    ssid.getItems().add(network.ssid);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
