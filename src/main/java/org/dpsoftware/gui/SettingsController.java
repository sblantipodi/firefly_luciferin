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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.paint.Color;
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
    @FXML private Button showTestImageButton;
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
        aspectRatio.getItems().addAll("FullScreen", "Letterbox");
        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readConfig();
        if (currentConfig == null) {
            showTestImageButton.setVisible(false);
        } else {
            showTestImageButton.setVisible(true);
        }
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
     * Show a canvas containing a test image for the LED Matrix in use
     * @param e event
     */
    @FXML
    public void showTestImage(InputEvent e) {

        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readConfig();

        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();
        Group root = new Group();
        Scene s = new Scene(root, 330, 400, Color.BLACK);

        int scaleRatio = currentConfig.getOsScaling();
        Canvas canvas = new Canvas((scaleResolution(currentConfig.getScreenResX(), scaleRatio)),
                (scaleResolution(currentConfig.getScreenResY(), scaleRatio)));
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);

        // Hide canvas on key pressed
        canvas.setOnKeyPressed(t -> {
            stage.setFullScreen(false);
            stage.hide();
        });

        drawTestShapes(gc, currentConfig);

        root.getChildren().add(canvas);
        stage.setScene(s);
        stage.show();
        stage.setFullScreen(true);

    }

    private void drawTestShapes(GraphicsContext gc, Configuration conf) {

        LinkedHashMap<Integer, LEDCoordinate> ledMatrix = conf.getLedMatrixInUse(conf.getDefaultLedMatrix());

        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(10);
        gc.stroke();

        int scaleRatio = conf.getOsScaling();
        AtomicInteger ledDistance = new AtomicInteger();
        ledMatrix.forEach((key, coordinate) -> {

            int colorToUse = 1;
            colorToUse = key;
            if (key > 3) {
                while (colorToUse > 3) {
                    colorToUse -= 3;
                }
            }
            if (colorToUse == 1) {
                gc.setFill(Color.RED);
            } else if (colorToUse == 2) {
                gc.setFill(Color.GREEN);
            } else {
                gc.setFill(Color.BLUE);
            }
            double border = 0;
            if (conf.getDefaultLedMatrix().equals("Fullscreen")) {
                border = 0.10;
            } else {
                border = 0.15;
            }

            if (conf.getOrientation().equals("Anticlockwise")) {
                // Bottom right Anticlockwise
                if (key <= conf.getBottomRightLed()) {
                    if (ledDistance.get() == 0) {
                        ledDistance.set(scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) - scaleResolution(coordinate.getX(), scaleRatio));
                    }
                    gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio) - (ledDistance.get()/2), scaleResolution(coordinate.getY(), scaleRatio),
                            ledDistance.get() -10, scaleResolution(coordinate.getY(), scaleRatio));
                    gc.setFill(Color.WHITE);
                    gc.fillText("#" + key, (scaleResolution(coordinate.getX(), scaleRatio) - (ledDistance.get()/2))+2, scaleResolution(coordinate.getY(), scaleRatio) + 15);
                } else if (key <= conf.getBottomRightLed() + conf.getRightLed()) { // Right Anticlockwise
                    if (key == conf.getBottomRightLed() + 1) {
                        ledDistance.set(scaleResolution(coordinate.getY(), scaleRatio) - scaleResolution(ledMatrix.get(key + 1).getY(), scaleRatio));
                    }
                    gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio), scaleResolution(coordinate.getY(), scaleRatio),
                            scaleResolution(coordinate.getX(), scaleRatio), ledDistance.get() - 10);
                    gc.setFill(Color.WHITE);
                    gc.fillText("#" + key, scaleResolution(coordinate.getX(), scaleRatio) + 2, scaleResolution(coordinate.getY(), scaleRatio) + 15);
                } else if (key > (conf.getBottomRightLed() + conf.getRightLed()) && key <= (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed())) { // Top Anticlockwise
                    if (key == (conf.getBottomRightLed() + conf.getRightLed()) + 1) {
                        ledDistance.set(scaleResolution(coordinate.getX(), scaleRatio) - scaleResolution(ledMatrix.get(key+1).getX(), scaleRatio));
                    }
                    gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio), 0,
                           ledDistance.get() - 10, scaleResolution(scaleResolution((int) (conf.getScreenResY() * border), scaleRatio) - 10, scaleRatio));
                    gc.setFill(Color.WHITE);
                    gc.fillText("#" + key, scaleResolution(coordinate.getX(), scaleRatio) + 2, 15);
                } else if (key > (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed()) && key <= (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed() + conf.getLeftLed())) { // Left Anticlockwise
                    if (key == (conf.getBottomRightLed() + conf.getRightLed()+ conf.getTopLed()) + 1) {
                        ledDistance.set(scaleResolution(ledMatrix.get(key + 1).getY(), scaleRatio) - scaleResolution(coordinate.getY(), scaleRatio));
                    }
                    gc.fillRect(0, scaleResolution(coordinate.getY(), scaleRatio),
                            150, ledDistance.get() - 10);
                    gc.setFill(Color.WHITE);
                    gc.fillText("#" + key, 0, scaleResolution(coordinate.getY(), scaleRatio) + 15);
                } else { // bottom left Anticlockwise
                    if (key == (conf.getBottomRightLed() + conf.getRightLed()+ conf.getTopLed() + conf.getLeftLed()) + 1) {
                        ledDistance.set(scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) - scaleResolution(coordinate.getX(), scaleRatio));
                    }
                    gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio), scaleResolution(coordinate.getY(), scaleRatio),
                    ledDistance.get() - 10, scaleResolution(coordinate.getY(), scaleRatio));
                    gc.setFill(Color.WHITE);
                    gc.fillText("#" + key, scaleResolution(coordinate.getX(), scaleRatio) + 2, scaleResolution(coordinate.getY(), scaleRatio) + 15);
                }
            }


//            // Bottom right Clockwise
//            if (currentConfig.getOrientation().equals("Clockwise") && key <= currentConfig.getBottomRightLed()) {
//                if (ledDistance.get() == 0) {
//                    ledDistance.set(scaleResolution(coordinate.getX(), scaleRatio) - scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) );
//                }
//                if (key == currentConfig.getBottomRightLed()) {
//                    gc.fillRect(0, scaleResolution(ledMatrix.get(key).getY(), scaleRatio),
//                            ledDistance.get() -10, scaleResolution(coordinate.getY(), scaleRatio));
//                } else {
//                    gc.fillRect(scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio), scaleResolution(ledMatrix.get(key + 1).getY(), scaleRatio),
//                            ledDistance.get() -10, scaleResolution(coordinate.getY(), scaleRatio));
//                }
//            }

        });

//        gc.setFill(Color.GREEN);
//        gc.setStroke(Color.BLUE);
//        gc.setLineWidth(5);
//        gc.strokeLine(40, 10, 10, 40);
//        gc.fillOval(10, 60, 30, 30);
//        gc.strokeOval(60, 60, 30, 30);
//        gc.fillRoundRect(110, 60, 30, 30, 10, 10);
//        gc.strokeRoundRect(160, 60, 30, 30, 10, 10);
//        gc.fillArc(10, 110, 30, 30, 45, 240, ArcType.OPEN);
//        gc.fillArc(60, 110, 30, 30, 45, 240, ArcType.CHORD);
//        gc.fillArc(110, 110, 30, 30, 45, 240, ArcType.ROUND);
//        gc.strokeArc(10, 160, 30, 30, 45, 240, ArcType.OPEN);
//        gc.strokeArc(60, 160, 30, 30, 45, 240, ArcType.CHORD);
//        gc.strokeArc(110, 160, 30, 30, 45, 240, ArcType.ROUND);
//        gc.fillPolygon(new double[]{10, 40, 10, 40},
//                new double[]{210, 210, 240, 240}, 4);
//        gc.strokePolygon(new double[]{60, 90, 60, 90},
//                new double[]{210, 210, 240, 240}, 4);
//        gc.strokePolyline(new double[]{110, 140, 110, 140},
//                new double[]{210, 210, 240, 240}, 4);
//        gc.setFill(Color.GREEN);
//        gc.setStroke(Color.BLUE);
////        gc.fillRect(0,0,2480,1340);
    }

    int scaleResolution(int numberToScale, int scaleRatio) {
        return (numberToScale*100)/scaleRatio;
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
