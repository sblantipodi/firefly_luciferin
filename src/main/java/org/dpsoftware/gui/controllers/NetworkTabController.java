/*
  NetworkTabController.java

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
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.controllers.options.NetworkTabOptions;
import org.dpsoftware.managers.dto.mqttdiscovery.DiscoveryObject;

import java.awt.*;

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
        NetworkTabOptions.publishDiscoveryTopics(createEntity);
    }

    /**
     * Publish one MQTT discovery entity through the shared network options.
     *
     * @param discoveryObject MQTT entity object
     * @param createEntity whether to create the entity
     */
    public static void publishDiscoveryTopic(DiscoveryObject discoveryObject, boolean createEntity) {
        NetworkTabOptions.publishDiscoveryTopic(discoveryObject, createEntity);
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
        NetworkTabOptions.MqttAddress address = NetworkTabOptions.splitServer(currentConfig.getMqttServer());
        mqttHost.setText(address.host());
        mqttPort.setText(address.port());
        mqttTopic.setText(Constants.TOPIC_DEFAULT_MQTT.equals(currentConfig.getMqttTopic()) ? Constants.MQTT_BASE_TOPIC : currentConfig.getMqttTopic());
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

    /**
     * Show improv dialog
     */
    @FXML
    public void improvWiFiDialog() {
        if (MainSingleton.getInstance().guiManager != null) {
            MainSingleton.getInstance().guiManager.showImprovDialog(settingsController);
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
        config.setMqttServer(NetworkTabOptions.server(mqttHost.getText(), mqttPort.getText()));
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
        MainSingleton.getInstance().guiManager.showLocalizedNotification(LabelKey.MQTT_DISCOVERY,
                LabelKey.MQTT_ADD_DEVICE, Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.INFO);
    }

    /**
     * Send an MQTT discovery message to the MQTT discovery topic to remove the Glow Worm device
     */
    @FXML
    public void discoveryRemove() {
        log.info("Removing entities using MQTT auto discovery...");
        publishDiscoveryTopics(false);
        MainSingleton.getInstance().guiManager.showLocalizedNotification(LabelKey.MQTT_DISCOVERY,
                LabelKey.MQTT_REMOVE_DEVICE, Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.INFO);
    }

    /**
     * Set form tooltips
     *
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTHOST, mqttHost);
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTPORT, mqttPort);
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTTOPIC, mqttTopic);
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTDISCOVERYTOPIC, mqttDiscoveryTopic);
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTDISCOVERYTOPIC_ADD, addButton);
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTDISCOVERYTOPIC_REMOVE, removeButton);
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTUSER, mqttUser);
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTPWD, mqttPwd);
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTENABLE, mqttEnable);
        GuiManager.createTooltip(LabelKey.TOOLTIP_MQTTSTREAM, mqttStream);
        GuiManager.createTooltip(LabelKey.TOOLTIP_IMPROV_CONTEXT, programDeviceButton);
        if (currentConfig == null) {
            GuiManager.createTooltip(LabelKey.TOOLTIP_SAVEMQTTBUTTON_NULL, saveMQTTButton);
        }
        GuiManager.createTooltip(LabelKey.TOOLTIP_STREAMTYPE, streamType);
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        SettingsController.addTextFieldListener(mqttPort, false);
    }
}
