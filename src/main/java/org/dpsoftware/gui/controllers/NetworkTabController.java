/*
  NetworkTabController.java

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

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.SerialManager;
import org.dpsoftware.managers.dto.mqttdiscovery.*;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Network Tab controller
 */
@Slf4j
public class NetworkTabController {

    // FXML binding
    @FXML
    public Button saveMQTTButton;
    @FXML
    public Button programDeviceButton;
    @FXML
    public TextField mqttHost;
    @FXML
    public TextField mqttPort;
    @FXML
    public TextField mqttTopic;
    @FXML
    public TextField mqttUser;
    @FXML
    public TextField mqttDiscoveryTopic;
    @FXML
    public PasswordField mqttPwd;
    @FXML
    public CheckBox mqttEnable;
    @FXML
    public CheckBox mqttStream; // this refers to wireless stream, old name for compatibility with previous version
    @FXML
    public ComboBox<String> streamType;
    @FXML
    public Button addButton;
    @FXML
    public Button removeButton;
    // Inject main controller
    @FXML
    private SettingsController settingsController;

    /**
     * Publish all the topics needed for the MQTT discovery process.
     *
     * @param createEntity if true create the MQTT entity, if false it destroys the entity
     */
    public static void publishDiscoveryTopics(boolean createEntity) {
        publishDiscoveryTopic(new SensorLastUpdateFFDiscovery(), createEntity);
        publishDiscoveryTopic(new LightDiscovery(), createEntity);
        publishDiscoveryTopic(new NumberWhiteTempDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectGammaDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectEmaDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectFrameGenDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectProfileDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorConsumingDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorProducingDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorVersionDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorLedsDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorLastUpdateDiscovery(), createEntity);
        publishDiscoveryTopic(new SwitchRebootDiscovery(), createEntity);
        publishDiscoveryTopic(new SelectAspectRatioDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorAspectRatioDiscovery(), createEntity);
        if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac() != null) {
            publishDiscoveryTopic(new SelectColorModeDiscovery(), createEntity);
        }
        publishDiscoveryTopic(new SelectEffectDiscovery(), createEntity);
        publishDiscoveryTopic(new SwitchBiasLightDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorGWConsumingDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorGpioDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorWiFiDiscovery(), createEntity);
        publishDiscoveryTopic(new SensorLdrDiscovery(), createEntity);
    }

    /**
     * Publish to a discovery topic to create or destroy the MQTT entity
     *
     * @param discoveryObject MQTT entity object
     * @param createEntity    if true create the MQTT entity, if false it destroys the entity
     */
    public static void publishDiscoveryTopic(DiscoveryObject discoveryObject, boolean createEntity) {
        log.info("Sending MQTT discovery msg to topic: {}", discoveryObject.getDiscoveryTopic());
        log.info("Message sent: {}", discoveryObject.getCreateEntityStr());
        NetworkManager.publishToTopic(discoveryObject.getDiscoveryTopic(), createEntity ?
                discoveryObject.getCreateEntityStr() : discoveryObject.getDestroyEntityStr(), false, true, 0);
        CommonUtility.sleepMilliseconds(Constants.MQTT_DISCOVERY_CALL_DELAY);
    }

    /**
     * Inject main controller containing the TabPane
     *
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Init combo boxes
     */
    public void initComboBox() {
        for (Enums.StreamType stream : Enums.StreamType.values()) {
            streamType.getItems().add(stream.getStreamType());
        }
    }

    /**
     * Init form values
     */
    void initDefaultValues() {
        mqttTopic.setDisable(true);
        mqttDiscoveryTopic.setDisable(true);
        addButton.setDisable(true);
        removeButton.setDisable(true);
        mqttHost.setDisable(true);
        mqttUser.setDisable(true);
        mqttPwd.setDisable(true);
        mqttPort.setDisable(true);
        streamType.setDisable(true);
        mqttHost.setText(Constants.DEFAULT_MQTT_HOST);
        mqttPort.setText(Constants.DEFAULT_MQTT_PORT);
        mqttTopic.setText(Constants.MQTT_BASE_TOPIC);
        mqttDiscoveryTopic.setText(Constants.MQTT_DISCOVERY_TOPIC);
        streamType.setValue(Enums.StreamType.UDP.getStreamType());
    }

    /**
     * Init form values by reading existing config file
     *
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        mqttHost.setText(currentConfig.getMqttServer().substring(0, currentConfig.getMqttServer().lastIndexOf(":")));
        mqttPort.setText(currentConfig.getMqttServer().substring(currentConfig.getMqttServer().lastIndexOf(":") + 1));
        mqttTopic.setText(currentConfig.getMqttTopic().equals(Constants.TOPIC_DEFAULT_MQTT) ? Constants.MQTT_BASE_TOPIC : currentConfig.getMqttTopic());
        mqttDiscoveryTopic.setText(currentConfig.getMqttDiscoveryTopic());
        mqttUser.setText(currentConfig.getMqttUsername());
        mqttPwd.setText(currentConfig.getMqttPwd());
        mqttEnable.setSelected(currentConfig.isMqttEnable());
        mqttStream.setSelected(currentConfig.isWirelessStream());
        mqttTopic.setDisable(false);
        mqttDiscoveryTopic.setDisable(false);
        addButton.setDisable(false);
        removeButton.setDisable(false);
        streamType.setDisable(!mqttStream.isSelected());
        streamType.setValue(currentConfig.getStreamType());
        if (!mqttEnable.isSelected()) {
            mqttHost.setDisable(true);
            mqttPort.setDisable(true);
            mqttTopic.setDisable(true);
            mqttDiscoveryTopic.setDisable(true);
            addButton.setDisable(true);
            removeButton.setDisable(true);
            mqttUser.setDisable(true);
            mqttPwd.setDisable(true);
        }
    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {
        mqttStream.setOnAction(_ -> {
            streamType.setDisable(!mqttStream.isSelected());
            settingsController.initOutputDeviceChooser(false);
        });
        mqttEnable.setOnAction(_ -> {
            if (!mqttEnable.isSelected()) {
                mqttHost.setDisable(true);
                mqttPort.setDisable(true);
                mqttTopic.setDisable(true);
                mqttDiscoveryTopic.setDisable(true);
                addButton.setDisable(true);
                removeButton.setDisable(true);
                mqttUser.setDisable(true);
                mqttPwd.setDisable(true);
                streamType.setValue(Enums.StreamType.UDP.getStreamType());
            } else {
                mqttHost.setDisable(false);
                mqttPort.setDisable(false);
                mqttTopic.setDisable(false);
                mqttDiscoveryTopic.setDisable(false);
                addButton.setDisable(false);
                removeButton.setDisable(false);
                mqttUser.setDisable(false);
                mqttPwd.setDisable(false);
            }
            if (MainSingleton.getInstance().config == null) {
                addButton.setDisable(true);
                removeButton.setDisable(true);
            }
            settingsController.initOutputDeviceChooser(false);
        });
        streamType.setOnAction(_ -> {
            if (streamType.getValue().equals(Enums.StreamType.MQTT.getStreamType()) && !mqttEnable.isSelected()
                    || streamType.getValue().equals(Enums.StreamType.UDP.getStreamType()) && mqttEnable.isSelected()) {
                mqttEnable.setSelected(true);
                mqttHost.setDisable(false);
                mqttPort.setDisable(false);
                mqttTopic.setDisable(false);
                mqttDiscoveryTopic.setDisable(false);
                addButton.setDisable(false);
                removeButton.setDisable(false);
                mqttUser.setDisable(false);
                mqttPwd.setDisable(false);
            }
            settingsController.checkProfileDifferences();
        });
    }

    private static byte[] buildImprovWifiPacket(String ssid, String password) {
        byte version = 0x01; // IMPROV_VERSION del firmware
        byte packetType = 0x00; // RPC_Command
        byte rpcCommandType = 0x01; // Command_Wifi

        byte[] ssidBytes = ssid.getBytes(StandardCharsets.UTF_8);
        byte[] passBytes = password.getBytes(StandardCharsets.UTF_8);

        // rpcData = [totalLen][ssidLen][ssid...][passLen][pass...]
        int rpcDataLen = 2 + ssidBytes.length + 1 + passBytes.length;
        byte[] rpcData = new byte[rpcDataLen];

        int i = 0;
        rpcData[i++] = (byte) (ssidBytes.length + 1 + passBytes.length); // total len
        rpcData[i++] = (byte) ssidBytes.length;
        System.arraycopy(ssidBytes, 0, rpcData, i, ssidBytes.length);
        i += ssidBytes.length;
        rpcData[i++] = (byte) passBytes.length;
        System.arraycopy(passBytes, 0, rpcData, i, passBytes.length);

        // Length field nel pacchetto = RPC_CommandType (1 byte) + rpcData length
        byte lengthField = (byte) (1 + rpcData.length);

        int totalLen = 6 + 1 + 1 + 1 + lengthField + 1;
        byte[] packet = new byte[totalLen];

        // Header "IMPROV"
        packet[0] = 0x49; // 'I'
        packet[1] = 0x4D; // 'M'
        packet[2] = 0x50; // 'P'
        packet[3] = 0x52; // 'R'
        packet[4] = 0x4F; // 'O'
        packet[5] = 0x56; // 'V'

        packet[6] = version;
        packet[7] = packetType;
        packet[8] = lengthField;
        packet[9] = rpcCommandType;

        System.arraycopy(rpcData, 0, packet, 10, rpcData.length);

        // Checksum = somma di TUTTI i byte tranne il checksum stesso
        int checksum = 0;
        for (int j = 0; j < packet.length - 1; j++) {
            checksum = (checksum + (packet[j] & 0xFF)) & 0xFF;
        }
        packet[packet.length - 1] = (byte) checksum;

        return packet;
    }

    /**
     * Program device
     *
     * @param e event
     */
    @FXML
    public void programDevice(InputEvent e) {
        SerialManager serialManager = new SerialManager();

        if (MainSingleton.getInstance().serial != null)
            MainSingleton.getInstance().serial.closePort();
        serialManager.initSerial("COM14");
        try {
//            MainSingleton.getInstance().output.write("I".getBytes());
//            MainSingleton.getInstance().output.write("M".getBytes());
//            MainSingleton.getInstance().output.write("P".getBytes());
//            MainSingleton.getInstance().output.write("R".getBytes());
//            MainSingleton.getInstance().output.write("O".getBytes());
//            MainSingleton.getInstance().output.write("V".getBytes());
//            MainSingleton.getInstance().output.write("improv".getBytes());
//            MainSingleton.getInstance().output.write("CoNfIg\n".getBytes());
//            MainSingleton.getInstance().output.write("CoNfIg".getBytes());            MainSingleton.getInstance().output.write("CoNfIg\n".getBytes());
//            MainSingleton.getInstance().output.write("CoNfIg".getBytes());
//            MainSingleton.getInstance().output.write("CoNfIg\n".getBytes());
//            MainSingleton.getInstance().output.write("dsada\n".getBytes());
//            MainSingleton.getInstance().output.write("aaaaaaaaaaaa\n".getBytes());
//            MainSingleton.getInstance().output.write("aaaaaaaaaaaa\n".getBytes());
//            MainSingleton.getInstance().output.write("aaaaaaaaaaaa\n".getBytes());
//            MainSingleton.getInstance().output.write("aaaaaaaaaaaa\n".getBytes());
//            MainSingleton.getInstance().output.write("bvvvvvvvvvv\n".getBytes());
//            MainSingleton.getInstance().output.write(" bbbbbbbbbb\n".getBytes());

            String ssid = "";
            String password = "";

            byte[] header = {'I', 'M', 'P', 'R', 'O', 'V'};
            byte version = 0x01;
            byte rpcPacketType = 0x03;
            byte rpcCommandType = 0x01;

            byte[] ssidBytes = ssid.getBytes(StandardCharsets.UTF_8);
            byte[] passBytes = password.getBytes(StandardCharsets.UTF_8);

            int dataLen = 1 + ssidBytes.length + 1 + passBytes.length;
            int packetLen = header.length + 1 + 1 + 1 + 1 + 1 + dataLen + 1;

            byte[] packet = new byte[packetLen];
            int idx = 0;

// Header
            for (byte b : header) packet[idx++] = b;
            packet[idx++] = version;
            packet[idx++] = rpcPacketType;
//            packet[idx++] = (byte) (dataLen);   // Length
            packet[idx++] = (byte) (dataLen + 2);   // Length
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
            byte checksumByte = (byte) (checksum & 0xFF);  // modulo 256, proprio come Arduino

            packet[idx++] = checksumByte;
//            packet[idx++] = 32;


//            String s = new String(new byte[]{b}, StandardCharsets.UTF_8);
//            System.out.println(s);

//            String s = new String(packet, StandardCharsets.UTF_8);
//            System.out.println(s);


            MainSingleton.getInstance().output.write(packet);
//            MainSingleton.getInstance().output.flush();

//            MainSingleton.getInstance().output.write(packet);

//            MainSingleton.getInstance().output.close();


        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
    }

    /**
     * Save button event
     *
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        settingsController.save(e);
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        config.setMqttServer(mqttHost.getText() + ":" + mqttPort.getText());
        config.setMqttTopic(mqttTopic.getText());
        config.setMqttDiscoveryTopic(mqttDiscoveryTopic.getText());
        config.setMqttUsername(mqttUser.getText());
        config.setMqttPwd(mqttPwd.getText());
        config.setMqttEnable(mqttEnable.isSelected());
        config.setWirelessStream(mqttStream.isSelected());
        config.setStreamType(streamType.getValue());
    }

    /**
     * Set red button if a param requires Firefly restart
     */
    @FXML
    public void saveButtonHover() {
        settingsController.checkProfileDifferences();
    }

    /**
     * Send an MQTT discovery message to the MQTT discovery topic to add the Glow Worm device
     */
    @FXML
    public void discoveryAdd() {
        log.info("Sending entities for MQTT auto discovery...");
        publishDiscoveryTopics(true);
        MainSingleton.getInstance().guiManager.showLocalizedNotification(Constants.MQTT_DISCOVERY,
                Constants.MQTT_ADD_DEVICE, Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.INFO);
    }

    /**
     * Send an MQTT discovery message to the MQTT discovery topic to remove the Glow Worm device
     */
    @FXML
    public void discoveryRemove() {
        log.info("Removing entities using MQTT auto discovery...");
        publishDiscoveryTopics(false);
        MainSingleton.getInstance().guiManager.showLocalizedNotification(Constants.MQTT_DISCOVERY,
                Constants.MQTT_REMOVE_DEVICE, Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.INFO);
    }

    /**
     * Set form tooltips
     *
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTHOST, mqttHost);
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTPORT, mqttPort);
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTTOPIC, mqttTopic);
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTDISCOVERYTOPIC, mqttDiscoveryTopic);
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTDISCOVERYTOPIC_ADD, addButton);
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTDISCOVERYTOPIC_REMOVE, removeButton);
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTUSER, mqttUser);
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTPWD, mqttPwd);
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTENABLE, mqttEnable);
        GuiManager.createTooltip(Constants.TOOLTIP_MQTTSTREAM, mqttStream);
        if (currentConfig == null) {
            GuiManager.createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON_NULL, saveMQTTButton);
        }
        GuiManager.createTooltip(Constants.TOOLTIP_STREAMTYPE, streamType);
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        SettingsController.addTextFieldListener(mqttPort);
    }
}
