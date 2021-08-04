/*
  MqttTabController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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
        mqttHost.setText(Constants.DEFAULT_MQTT_HOST);
        mqttPort.setText(Constants.DEFAULT_MQTT_PORT);
        mqttTopic.setText(Constants.MQTT_BASE_TOPIC);
        streamType.setValue(Constants.StreamType.UDP.getStreamType());
        streamType.setDisable(true);

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
        mqttEnable.setSelected(currentConfig.isMqttEnable());
        mqttStream.setSelected(currentConfig.isMqttStream());
        mqttTopic.setDisable(false);
        streamType.setDisable(!mqttStream.isSelected());
        streamType.setValue(currentConfig.getStreamType());

    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {

        mqttEnable.setOnAction(e -> {
            if (!mqttEnable.isSelected()) {
                mqttStream.setSelected(false);
                streamType.setDisable(true);
            }
        });
        mqttStream.setOnAction(e -> {
            if (mqttStream.isSelected()) {
                mqttEnable.setSelected(true);
                streamType.setDisable(false);
            } else {
                streamType.setDisable(true);
            }
            settingsController.initOutputDeviceChooser(false);
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
