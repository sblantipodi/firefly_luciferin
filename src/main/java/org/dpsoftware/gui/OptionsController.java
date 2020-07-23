/*
  OptionsController.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/

package org.dpsoftware.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;
import org.dpsoftware.Configuration;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MQTTManager;
import org.dpsoftware.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;

public class OptionsController {

    private static final Logger logger = LoggerFactory.getLogger(OptionsController.class);

    @FXML TextField screenWidth;
    @FXML TextField screenHeight;
    @FXML ComboBox scaling;
    @FXML ComboBox gamma;
    @FXML ComboBox captureMethod;
    @FXML TextField numberOfThreads;
    @FXML public Button saveButton;
    @FXML public Button cancelButton;
    @FXML ComboBox serialPort;
    @FXML ComboBox aspectRatio;
    @FXML TextField mqttHost;
    @FXML TextField mqttPort;
    @FXML TextField mqttTopic;
    @FXML TextField mqttUsername;
    @FXML TextField mqttPassword;

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {

        // Get OS scaling using JNA
        GraphicsConfiguration screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        AffineTransform screenInfo = screen.getDefaultTransform();
        double scaleX = screenInfo.getScaleX();
        double scaleY = screenInfo.getScaleY();
        scaling.getItems().addAll("100%", "125%", "150%", "175%", "200%", "225%", "250%", "300%", "350%");
        scaling.setValue(((int) (screenInfo.getScaleX() * 100)) + "%");
        // Get screen resolution
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth.setText(String.valueOf((int) (screenSize.width * scaleX)));
        screenHeight.setText(String.valueOf((int) (screenSize.height * scaleY)));
        // Init gamma
        gamma.getItems().addAll("1.8", "2.0", "2.2", "2.4");
        gamma.setValue("2.2");
        // Init capture methods
        captureMethod.getItems().addAll(Configuration.CaptureMethod.DDUPL, Configuration.CaptureMethod.WinAPI, Configuration.CaptureMethod.CPU);
        captureMethod.setValue(Configuration.CaptureMethod.DDUPL);
        // Init threads
        numberOfThreads.setText("1");
        // Serial ports, most OSs cap at 256
        serialPort.getItems().add("AUTO");
        for (int i=0; i<=256; i++) {
            serialPort.getItems().add("COM" + i);
        }
        serialPort.setValue("AUTO");
        // Aspect ratio
        aspectRatio.getItems().addAll("FullScreen", "LetterBox");
        aspectRatio.setValue("FullScreen");
        // Set MQTT defaults
        mqttHost.setText("tcp://192.168.1.3");
        mqttPort.setText("1883");
        mqttTopic.setText("lights/pcambiligh/set");
        mqttUsername.setText("");
        mqttPassword.setText("");
        // Form tooltips
        setTooltips();
        // Set numeric field
        setNumericTextField();
        // Set focus on the save button
        Platform.runLater(() -> saveButton.requestFocus());

    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {

        // No config found, init with a default config
        LEDCoordinate ledCoordinate = new LEDCoordinate();
        Configuration config = new Configuration(ledCoordinate.initFullScreenLedMatrix(Integer.valueOf(screenWidth.getText()), Integer.valueOf(screenHeight.getText())),
                ledCoordinate.initLetterboxLedMatrix(Integer.valueOf(screenWidth.getText()), Integer.valueOf(screenHeight.getText())));
        config.setNumberOfCPUThreads(Integer.valueOf(numberOfThreads.getText()));
        switch ((Configuration.CaptureMethod) captureMethod.getValue()) {
            case DDUPL:
                config.setCaptureMethod(Configuration.CaptureMethod.DDUPL);
                break;
            case WinAPI:
                config.setCaptureMethod(Configuration.CaptureMethod.WinAPI);
                break;
            case CPU:
                config.setCaptureMethod(Configuration.CaptureMethod.CPU);
                break;
        }
        config.setSerialPort((String) serialPort.getValue());
        config.setScreenResX(Integer.valueOf(screenWidth.getText()));
        config.setScreenResY(Integer.valueOf(screenHeight.getText()));
        config.setOsScaling(Integer.valueOf(((String)scaling.getValue()).replace("%","")));
        config.setGamma(Double.valueOf((String)gamma.getValue()));
        config.setSerialPort((String) serialPort.getValue());
        config.setDefaultLedMatrix((String) aspectRatio.getValue());
        config.setMqttServer(mqttHost.getText() + ":" + mqttPort.getText());
        config.setMqttTopic(mqttTopic.getText());
        config.setMqttUsername(mqttUsername.getText());
        config.setMqttPwd(mqttPassword.getText());
        try {
            StorageManager sm = new StorageManager();
            sm.writeConfig(config);
        } catch (IOException ioException) {
            logger.error("Can't write config file.");
        }
        cancel(e);

    }

    /**
     * Cancel button event
     * @param e event
     */
    @FXML
    public void cancel(InputEvent e) {

        Platform.setImplicitExit(false);
        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();

    }

    /**
     * Force TextField to be numeric
     * @param textField numeric fields
     */
    void addTextFieldListener(TextField textField) {

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("\\d*")) return;
            textField.setText(newValue.replaceAll("[^\\d]", ""));
        });

    }

    /**
     * Set form tooltips
     */
    void setTooltips() {

        Tooltip screenWidthTooltip = new Tooltip("Monitor resolution");
        screenWidth.setTooltip(screenWidthTooltip);
        Tooltip screenHeightTooltip = new Tooltip("Monitor resolution");
        screenHeight.setTooltip(screenHeightTooltip);
        Tooltip scalingTooltip = new Tooltip("OS scaling feature, you should not change this setting");
        scaling.setTooltip(scalingTooltip);
        Tooltip gammaTooltip = new Tooltip("Smaller values results in brighter LEDs but less accurate colors");
        gamma.setTooltip(gammaTooltip);
        Tooltip captureMethodTooltip = new Tooltip("If you have a GPU, Desktop Duplication API (DDUPL) is faster than other methods");
        captureMethod.setTooltip(captureMethodTooltip);
        Tooltip numberOfThreadsTooltip = new Tooltip("1 thread is enough when using DDUPL, 3 or more threads are recommended for other capture methods");
        numberOfThreads.setTooltip(numberOfThreadsTooltip);
        Tooltip serialPortTooltip = new Tooltip("AUTO detects first serial port available, change it if you have more than one serial port available");
        serialPort.setTooltip(serialPortTooltip);
        Tooltip aspectRatioTooltip = new Tooltip("LetterBox is recommended for films, you can change this option later");
        aspectRatio.setTooltip(aspectRatioTooltip);
        Tooltip saveButtonTooltip = new Tooltip("You can change this options later");
        saveButton.setTooltip(saveButtonTooltip);

        Tooltip mqttHostTooltip = new Tooltip("OPTIONAL: MQTT protocol://host");
        mqttHost.setTooltip(mqttHostTooltip);
        Tooltip mqttPortTooltip = new Tooltip("OPTIONAL: MQTT port");
        mqttPort.setTooltip(mqttPortTooltip);
        Tooltip mqttTopicTooltip = new Tooltip("OPTIONAL: MQTT topic, used to start/stop capturing on your microcontroller");
        mqttTopic.setTooltip(mqttTopicTooltip);
        Tooltip mqttUsernameTooltip = new Tooltip("OPTIONAL: MQTT username");
        mqttUsername.setTooltip(mqttUsernameTooltip);
        Tooltip mqttPasswordTooltip = new Tooltip("OPTIONAL: MQTT password");
        mqttPassword.setTooltip(mqttPasswordTooltip);

    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {

        addTextFieldListener(screenWidth);
        addTextFieldListener(screenHeight);
        addTextFieldListener(numberOfThreads);
        addTextFieldListener(mqttPort);

    }

}
