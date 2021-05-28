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
package org.dpsoftware.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
    @FXML public CheckBox mqttStream;


    /**
     * Inject main controller containing the TabPane
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Init form values
     */
    void initDefaultValues() {

        mqttTopic.setDisable(true);
        mqttHost.setText(Constants.DEFAULT_MQTT_HOST);
        mqttPort.setText(Constants.DEFAULT_MQTT_PORT);
        mqttTopic.setText(Constants.MQTT_BASE_TOPIC);

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

    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {

        mqttEnable.setOnAction(e -> {
            if (!mqttEnable.isSelected()) mqttStream.setSelected(false);
        });
        mqttStream.setOnAction(e -> {
            if (mqttStream.isSelected()) mqttEnable.setSelected(true);
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

    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {

        settingsController.addTextFieldListener(mqttPort);

    }

}
