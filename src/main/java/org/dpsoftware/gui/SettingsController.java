/*
  SettingsController.java

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
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.dpsoftware.Configuration;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @FXML private TextField screenWidth;
    @FXML private TextField screenHeight;
    @FXML private ComboBox<String> scaling;
    @FXML private ComboBox<String> gamma;
    @FXML private ComboBox<Configuration.CaptureMethod> captureMethod;
    @FXML private TextField numberOfThreads;
    @FXML private Button saveLedButton;
    @FXML private Button saveMQTTButton;
    @FXML private Button saveSettingsButton;
    @FXML private ComboBox<String> serialPort;
    @FXML private ComboBox<String> aspectRatio;
    @FXML private TextField mqttHost;
    @FXML private TextField mqttPort;
    @FXML private TextField mqttTopic;
    @FXML private TextField mqttUser;
    @FXML private PasswordField mqttPwd;
    @FXML private CheckBox mqttEnable;
    @FXML private TextField topLed;
    @FXML private TextField leftLed;
    @FXML private TextField rightLed;
    @FXML private TextField bottomLeftLed;
    @FXML private TextField bottomRightLed;
    @FXML private ComboBox<String> orientation;

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {

        Platform.setImplicitExit(false);

        scaling.getItems().addAll("100%", "125%", "150%", "175%", "200%", "225%", "250%", "300%", "350%");
        gamma.getItems().addAll("1.8", "2.0", "2.2", "2.4");
        captureMethod.getItems().addAll(Configuration.CaptureMethod.DDUPL, Configuration.CaptureMethod.WinAPI, Configuration.CaptureMethod.CPU);
        serialPort.getItems().add("AUTO");
        for (int i=0; i<=256; i++) {
            serialPort.getItems().add("COM" + i);
        }
        orientation.getItems().addAll("Clockwise", "Anticlockwise");
        aspectRatio.getItems().addAll("FullScreen", "LetterBox");
        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readConfig();
        initDefaultValues(currentConfig);
        setTooltips(currentConfig);
        setNumericTextField();
        Platform.runLater(() -> orientation.requestFocus());

    }

    /**
     * Init form values
     */
    void initDefaultValues(Configuration currentConfig) {

        if (currentConfig == null) {
            // Get OS scaling using JNA
            GraphicsConfiguration screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            AffineTransform screenInfo = screen.getDefaultTransform();
            double scaleX = screenInfo.getScaleX();
            double scaleY = screenInfo.getScaleY();
            // Get screen resolution
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            screenWidth.setText(String.valueOf((int) (screenSize.width * scaleX)));
            screenHeight.setText(String.valueOf((int) (screenSize.height * scaleY)));
            scaling.setValue(((int) (screenInfo.getScaleX() * 100)) + "%");
            captureMethod.setValue(Configuration.CaptureMethod.DDUPL);
            gamma.setValue("2.2");
            serialPort.setValue("AUTO");
            numberOfThreads.setText("1");
            aspectRatio.setValue("FullScreen");
            mqttHost.setText("tcp://192.168.1.3");
            mqttPort.setText("1883");
            mqttTopic.setText("lights/glowwormluciferin/set");
            orientation.setValue("Anticlockwise");
            topLed.setText("33");
            leftLed.setText("18");
            rightLed.setText("18");
            bottomLeftLed.setText("13");
            bottomRightLed.setText("13");
        } else {
            initValuesFromSettingsFile(currentConfig);
        }

    }

    /**
     * Init form values by reading existing config file
     * @param currentConfig existing
     */
    private void initValuesFromSettingsFile(Configuration currentConfig) {

        screenWidth.setText(String.valueOf(currentConfig.getScreenResX()));
        screenHeight.setText(String.valueOf(currentConfig.getScreenResY()));
        scaling.setValue(currentConfig.getOsScaling() + "%");
        captureMethod.setValue(currentConfig.getCaptureMethod());
        gamma.setValue(String.valueOf(currentConfig.getGamma()));
        serialPort.setValue(currentConfig.getSerialPort());
        numberOfThreads.setText(String.valueOf(currentConfig.getNumberOfCPUThreads()));
        aspectRatio.setValue(currentConfig.getDefaultLedMatrix());
        mqttHost.setText(currentConfig.getMqttServer().substring(0, currentConfig.getMqttServer().lastIndexOf(":")));
        mqttPort.setText(currentConfig.getMqttServer().substring(currentConfig.getMqttServer().lastIndexOf(":") + 1));
        mqttTopic.setText(currentConfig.getMqttTopic());
        mqttUser.setText(currentConfig.getMqttUsername());
        mqttPwd.setText(currentConfig.getMqttPwd());
        mqttEnable.setSelected(currentConfig.isMqttEnable());
        orientation.setValue(currentConfig.getOrientation());
        topLed.setText(String.valueOf(currentConfig.getTopLed()));
        leftLed.setText(String.valueOf(currentConfig.getLeftLed()));
        rightLed.setText(String.valueOf(currentConfig.getRightLed()));
        bottomLeftLed.setText(String.valueOf(currentConfig.getBottomLeftLed()));
        bottomRightLed.setText(String.valueOf(currentConfig.getBottomRightLed()));

    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {

        // No config found, init with a default config
        LEDCoordinate ledCoordinate = new LEDCoordinate();
        LinkedHashMap<Integer, LEDCoordinate> ledFullScreenMatrix = ledCoordinate.initFullScreenLedMatrix(Integer.parseInt(screenWidth.getText()),
                Integer.parseInt(screenHeight.getText()), Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(rightLed.getText()),
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()));
        LinkedHashMap<Integer, LEDCoordinate> ledLetterboxMatrix = ledCoordinate.initLetterboxLedMatrix(Integer.parseInt(screenWidth.getText()),
                Integer.parseInt(screenHeight.getText()), Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(rightLed.getText()),
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()));

        if (orientation.getValue().equals("Clockwise")) {
            reverseMap(ledFullScreenMatrix);
            reverseMap(ledLetterboxMatrix);
        }

        Configuration config = new Configuration(ledFullScreenMatrix,ledLetterboxMatrix);
        config.setNumberOfCPUThreads(Integer.parseInt(numberOfThreads.getText()));
        switch (captureMethod.getValue()) {
            case DDUPL -> config.setCaptureMethod(Configuration.CaptureMethod.DDUPL);
            case WinAPI -> config.setCaptureMethod(Configuration.CaptureMethod.WinAPI);
            case CPU -> config.setCaptureMethod(Configuration.CaptureMethod.CPU);
        }
        config.setSerialPort(serialPort.getValue());
        config.setScreenResX(Integer.parseInt(screenWidth.getText()));
        config.setScreenResY(Integer.parseInt(screenHeight.getText()));
        config.setOsScaling(Integer.parseInt((scaling.getValue()).replace("%","")));
        config.setGamma(Double.parseDouble(gamma.getValue()));
        config.setSerialPort(serialPort.getValue());
        config.setDefaultLedMatrix(aspectRatio.getValue());
        config.setMqttServer(mqttHost.getText() + ":" + mqttPort.getText());
        config.setMqttTopic(mqttTopic.getText());
        config.setMqttUsername(mqttUser.getText());
        config.setMqttPwd(mqttPwd.getText());
        config.setMqttEnable(mqttEnable.isSelected());
        config.setTopLed(Integer.parseInt(topLed.getText()));
        config.setLeftLed(Integer.parseInt(leftLed.getText()));
        config.setRightLed(Integer.parseInt(rightLed.getText()));
        config.setBottomLeftLed(Integer.parseInt(bottomLeftLed.getText()));
        config.setBottomRightLed(Integer.parseInt(bottomRightLed.getText()));
        config.setOrientation(orientation.getValue());

        try {
            StorageManager sm = new StorageManager();
            sm.writeConfig(config);
            cancel(e);
        } catch (IOException ioException) {
            logger.error("Can't write config file.");
        }

    }

    /**
     * Reverse an ordered like a LinkedHashMap
     * @param map Generic map
     */
    void reverseMap(Map<Integer, LEDCoordinate> map) {

        TreeMap tmap = new TreeMap<>(map);
        map.clear();
        AtomicInteger i = new AtomicInteger(1);
        tmap.descendingMap().forEach((k, v) -> map.put(i.getAndIncrement(), (LEDCoordinate) v));

    }
    

    /**
     * Cancel button event
     * @param e event
     */
    @FXML
    public void cancel(InputEvent e) {

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
    void setTooltips(Configuration currentConfig) {

        topLed.setTooltip(createTooltip("# of LEDs in the top row"));
        leftLed.setTooltip(createTooltip("# of LEDs in the left column"));
        rightLed.setTooltip(createTooltip("# of LEDs in the right column"));
        bottomLeftLed.setTooltip(createTooltip("# of LEDs in bottom left row"));
        bottomRightLed.setTooltip(createTooltip("# of LEDs in the bottom right row"));
        orientation.setTooltip(createTooltip("Orientation of your LED strip"));
        screenWidth.setTooltip(createTooltip("Monitor resolution"));
        screenHeight.setTooltip(createTooltip("Monitor resolution"));
        scaling.setTooltip(createTooltip("OS scaling feature, you should not change this setting"));
        gamma.setTooltip(createTooltip("Smaller values results in brighter LEDs but less accurate colors"));
        captureMethod.setTooltip(createTooltip("If you have a GPU, Desktop Duplication API (DDUPL) is faster than other methods"));
        numberOfThreads.setTooltip(createTooltip("1 thread is enough when using DDUPL, 3 or more threads are recommended for other capture methods"));
        serialPort.setTooltip(createTooltip("AUTO detects first serial port available, change it if you have more than one serial port available"));
        aspectRatio.setTooltip(createTooltip("LetterBox is recommended for films, you can change this option later"));

        mqttHost.setTooltip(createTooltip("OPTIONAL: MQTT protocol://host"));
        mqttPort.setTooltip(createTooltip("OPTIONAL: MQTT port"));
        mqttTopic.setTooltip(createTooltip("OPTIONAL: MQTT topic, used to start/stop capturing and the action to your MQTT Broker (Easy integration with Home Assistant or openHAB)"));
        mqttUser.setTooltip(createTooltip("OPTIONAL: MQTT username"));
        mqttPwd.setTooltip(createTooltip("OPTIONAL: MQTT password"));
        mqttEnable.setTooltip(createTooltip("MQTT is Optional"));

        if (currentConfig == null) {
            saveLedButton.setTooltip(createTooltip("You can change this options later"));
            saveMQTTButton.setTooltip(createTooltip("You can change this options later"));
            saveSettingsButton.setTooltip(createTooltip("You can change this options later"));
        } else {
            saveLedButton.setTooltip(createTooltip("Changes will take effect the next time you launch the app"));
            saveMQTTButton.setTooltip(createTooltip("Changes will take effect the next time you launch the app"));
            saveSettingsButton.setTooltip(createTooltip("Changes will take effect the next time you launch the app"));
        }

    }

    /**
     * Set tooltip
     * @param text tooltip string
     */
    public Tooltip createTooltip(String text) {

        Tooltip tooltip;
        tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(200));
        return tooltip;

    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {

        addTextFieldListener(screenWidth);
        addTextFieldListener(screenHeight);
        addTextFieldListener(numberOfThreads);
        addTextFieldListener(mqttPort);
        addTextFieldListener(topLed);
        addTextFieldListener(leftLed);
        addTextFieldListener(rightLed);
        addTextFieldListener(bottomLeftLed);
        addTextFieldListener(bottomRightLed);

    }

}
