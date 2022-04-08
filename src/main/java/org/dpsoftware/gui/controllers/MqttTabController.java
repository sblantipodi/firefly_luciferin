/*
  MqttTabController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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
import javafx.scene.input.InputEvent;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;

/**
 * MQTT Tab controller
 */
public class MqttTabController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    // FXML binding
    @FXML public Button saveMQTTButton;
    @FXML public TextField mqttHost;
    @FXML public TextField mqttPort;
    @FXML public TextField mqttTopic;
    @FXML public TextField mqttUser;
    @FXML public PasswordField mqttPwd;
    @FXML public CheckBox mqttEnable;
    @FXML public CheckBox mqttStream; // this refers to wireless stream, old name for compatibility with previous version
    @FXML public CheckBox wifiEnable;
    @FXML public ComboBox<String> streamType;

    /**
     * Inject main controller containing the TabPane
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Init combo boxes
     */
    public void initComboBox() {
        for (Constants.StreamType stream : Constants.StreamType.values()) {
            streamType.getItems().add(stream.getStreamType());
        }
    }

    /**
     * Init form values
     */
    void initDefaultValues() {
        mqttTopic.setDisable(true);
        mqttHost.setDisable(true);
        mqttUser.setDisable(true);
        mqttPwd.setDisable(true);
        mqttPort.setDisable(true);
        streamType.setDisable(true);
        mqttHost.setText(Constants.DEFAULT_MQTT_HOST);
        mqttPort.setText(Constants.DEFAULT_MQTT_PORT);
        mqttTopic.setText(Constants.MQTT_BASE_TOPIC);
        streamType.setValue(Constants.StreamType.UDP.getStreamType());
    }

    /**
     * Init form values by reading existing config file
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        mqttHost.setText(currentConfig.getMqttServer().substring(0, currentConfig.getMqttServer().lastIndexOf(":")));
        mqttPort.setText(currentConfig.getMqttServer().substring(currentConfig.getMqttServer().lastIndexOf(":") + 1));
        mqttTopic.setText(currentConfig.getMqttTopic().equals(Constants.DEFAULT_MQTT_TOPIC) ? Constants.MQTT_BASE_TOPIC : currentConfig.getMqttTopic());
        mqttUser.setText(currentConfig.getMqttUsername());
        mqttPwd.setText(currentConfig.getMqttPwd());
        wifiEnable.setSelected(currentConfig.isWifiEnable());
        mqttEnable.setSelected(currentConfig.isMqttEnable());
        mqttStream.setSelected(currentConfig.isMqttStream());
        mqttTopic.setDisable(false);
        streamType.setDisable(!mqttStream.isSelected());
        streamType.setValue(currentConfig.getStreamType());
        if (!wifiEnable.isSelected()) {
            streamType.setDisable(true);
            mqttStream.setDisable(true);
            mqttEnable.setDisable(true);
        }
        if (!mqttEnable.isSelected()) {
            mqttHost.setDisable(true);
            mqttPort.setDisable(true);
            mqttTopic.setDisable(true);
            mqttUser.setDisable(true);
            mqttPwd.setDisable(true);
        }
    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {
        wifiEnable.setOnAction(e -> {
            if (!wifiEnable.isSelected()) {
                mqttHost.setDisable(true);
                mqttPort.setDisable(true);
                mqttTopic.setDisable(true);
                mqttUser.setDisable(true);
                mqttPwd.setDisable(true);
                mqttStream.setSelected(false);
                mqttEnable.setSelected(false);
                mqttStream.setDisable(true);
                mqttEnable.setDisable(true);
                streamType.setDisable(true);
            } else {
                mqttEnable.setDisable(false);
                mqttStream.setDisable(false);
                streamType.setDisable(false);
            }
            settingsController.initOutputDeviceChooser(false);
        });
        mqttStream.setOnAction(e -> {
            streamType.setDisable(!mqttStream.isSelected());
            settingsController.initOutputDeviceChooser(false);
        });
        mqttEnable.setOnAction(e -> {
            if (!mqttEnable.isSelected()) {
                mqttHost.setDisable(true);
                mqttPort.setDisable(true);
                mqttTopic.setDisable(true);
                mqttUser.setDisable(true);
                mqttPwd.setDisable(true);
                streamType.setValue(Constants.StreamType.UDP.getStreamType());
            } else {
                mqttHost.setDisable(false);
                mqttPort.setDisable(false);
                mqttTopic.setDisable(false);
                mqttUser.setDisable(false);
                mqttPwd.setDisable(false);
            }
            settingsController.initOutputDeviceChooser(false);
        });
        streamType.setOnAction(e -> {
            if (streamType.getValue().equals(Constants.StreamType.UDP.getStreamType()) && mqttEnable.isSelected()) {
                mqttEnable.setSelected(false);
                mqttHost.setDisable(true);
                mqttPort.setDisable(true);
                mqttTopic.setDisable(true);
                mqttUser.setDisable(true);
                mqttPwd.setDisable(true);
            }
            if (streamType.getValue().equals(Constants.StreamType.MQTT.getStreamType()) && !mqttEnable.isSelected()) {
                mqttEnable.setSelected(true);
                mqttHost.setDisable(false);
                mqttPort.setDisable(false);
                mqttTopic.setDisable(false);
                mqttUser.setDisable(false);
                mqttPwd.setDisable(false);
            }
        });
    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        settingsController.save(e);
    }

    /**
     * Save button from main controller
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        config.setMqttServer(mqttHost.getText() + ":" + mqttPort.getText());
        config.setMqttTopic(mqttTopic.getText());
        config.setMqttUsername(mqttUser.getText());
        config.setMqttPwd(mqttPwd.getText());
        config.setWifiEnable(wifiEnable.isSelected());
        config.setMqttEnable(mqttEnable.isSelected());
        config.setMqttStream(mqttStream.isSelected());
        config.setStreamType(streamType.getValue());
    }

    /**
     * Set form tooltips
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        mqttHost.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MQTTHOST));
        mqttPort.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MQTTPORT));
        mqttTopic.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MQTTTOPIC));
        mqttUser.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MQTTUSER));
        mqttPwd.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MQTTPWD));
        wifiEnable.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_WIFIENABLE));
        mqttEnable.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MQTTENABLE));
        mqttStream.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MQTTSTREAM));
        if (currentConfig == null) {
            saveMQTTButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON_NULL));
        } else {
            saveMQTTButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON,200, 6000));
        }
        streamType.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_STREAMTYPE));
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        settingsController.addTextFieldListener(mqttPort);
    }
}
