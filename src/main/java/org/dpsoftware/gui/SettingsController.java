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

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.dpsoftware.Configuration;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.image.Image;

public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @FXML private TextField screenWidth;
    @FXML private TextField screenHeight;
    @FXML private ComboBox<String> scaling;
    @FXML private ComboBox<String> gamma;
    @FXML private ComboBox<Configuration.WindowsCaptureMethod> captureMethod;
    @FXML private ComboBox<Configuration.LinuxCaptureMethod> linuxCaptureMethod;
    @FXML private TextField numberOfThreads;
    @FXML private Button saveLedButton;
    @FXML private Button playButton;
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
    @FXML private CheckBox mqttStream;
    @FXML private TextField topLed;
    @FXML private TextField leftLed;
    @FXML private TextField rightLed;
    @FXML private TextField bottomLeftLed;
    @FXML private TextField bottomRightLed;
    @FXML private ComboBox<String> orientation;
    @FXML private Label producerLabel;
    @FXML private Label consumerLabel;
    @FXML private Label version;
    @FXML private final StringProperty producerValue = new SimpleStringProperty("");
    @FXML private final StringProperty consumerValue = new SimpleStringProperty("");
    Image controlImage;
    ImageView imageView;

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {

        Platform.setImplicitExit(false);

        scaling.getItems().addAll("100%", "125%", "150%", "175%", "200%", "225%", "250%", "300%", "350%");
        gamma.getItems().addAll("1.8", "2.0", "2.2", "2.4", "4", "5", "6", "8", "10");
        serialPort.getItems().add("AUTO");
        if (com.sun.jna.Platform.isWindows()) {
            for (int i=0; i<=256; i++) {
                serialPort.getItems().add("COM" + i);
            }
            captureMethod.getItems().addAll(Configuration.WindowsCaptureMethod.DDUPL, Configuration.WindowsCaptureMethod.WinAPI, Configuration.WindowsCaptureMethod.CPU);
        } else {
            if (FireflyLuciferin.communicationError) {
                controlImage = new Image(this.getClass().getResource("/org/dpsoftware/gui/img/java_fast_screen_capture_logo_grey.png").toString(), true);
            } else if (FireflyLuciferin.RUNNING) {
                controlImage = new Image(this.getClass().getResource("/org/dpsoftware/gui/img/java_fast_screen_capture_logo_play.png").toString(), true);
            } else {
                controlImage = new Image(this.getClass().getResource("/org/dpsoftware/gui/img/java_fast_screen_capture_logo.png").toString(), true);
            }
            imageView = new ImageView(controlImage);
            imageView.setFitHeight(80);
            imageView.setPreserveRatio(true);
            playButton.setGraphic(imageView);
            for (int i=0; i<=256; i++) {
                serialPort.getItems().add("/dev/ttyUSB" + i);
            }
            linuxCaptureMethod.getItems().addAll(Configuration.LinuxCaptureMethod.XIMAGESRC);
        }
        orientation.getItems().addAll("Clockwise", "Anticlockwise");
        aspectRatio.getItems().addAll("FullScreen", "Letterbox");
        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readConfig();
        showTestImageButton.setVisible(currentConfig != null);
        initDefaultValues(currentConfig);
        setTooltips(currentConfig);
        setNumericTextField();
        if (com.sun.jna.Platform.isWindows()) {
            Platform.runLater(() -> orientation.requestFocus());
        } else {
            producerLabel.textProperty().bind(producerValueProperty());
            consumerLabel.textProperty().bind(consumerValueProperty());
            version.setText("by Davide Perini (VERSION)".replaceAll("VERSION", FireflyLuciferin.VERSION));
            new AnimationTimer() {
                @Override
                public void handle(long now) {
                    setProducerValue("Producing @ " + FireflyLuciferin.FPS_PRODUCER + " FPS");
                    setConsumerValue("Consuming @ " + FireflyLuciferin.FPS_CONSUMER + " FPS");
                }
            }.start();
        }

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
            if (com.sun.jna.Platform.isWindows()) {
                captureMethod.setValue(Configuration.WindowsCaptureMethod.DDUPL);
            } else {
                linuxCaptureMethod.setValue(Configuration.LinuxCaptureMethod.XIMAGESRC);
            }
            gamma.setValue("2.2");
            serialPort.setValue("AUTO");
            numberOfThreads.setText("1");
            aspectRatio.setValue("FullScreen");
            mqttHost.setText("tcp://192.168.1.3");
            mqttPort.setText("1883");
            mqttTopic.setText("lights/glowwormluciferin/set");
            orientation.setValue("Clockwise");
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
        if (com.sun.jna.Platform.isWindows()) {
            captureMethod.setValue(Configuration.WindowsCaptureMethod.valueOf(currentConfig.getCaptureMethod()));
        } else {
            linuxCaptureMethod.setValue(Configuration.LinuxCaptureMethod.valueOf(currentConfig.getCaptureMethod()));
        }
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
        mqttStream.setSelected(currentConfig.isMqttStream());
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

        Configuration config = new Configuration(ledFullScreenMatrix,ledLetterboxMatrix);
        config.setNumberOfCPUThreads(Integer.parseInt(numberOfThreads.getText()));
        if (com.sun.jna.Platform.isWindows()) {
            switch (captureMethod.getValue()) {
                case DDUPL -> config.setCaptureMethod(Configuration.WindowsCaptureMethod.DDUPL.name());
                case WinAPI -> config.setCaptureMethod(Configuration.WindowsCaptureMethod.WinAPI.name());
                case CPU -> config.setCaptureMethod(Configuration.WindowsCaptureMethod.CPU.name());
            }
        } else {
            switch (linuxCaptureMethod.getValue()) {
                case XIMAGESRC -> config.setCaptureMethod(Configuration.LinuxCaptureMethod.XIMAGESRC.name());
            }
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
        config.setMqttStream(mqttStream.isSelected());
        config.setTopLed(Integer.parseInt(topLed.getText()));
        config.setLeftLed(Integer.parseInt(leftLed.getText()));
        config.setRightLed(Integer.parseInt(rightLed.getText()));
        config.setBottomLeftLed(Integer.parseInt(bottomLeftLed.getText()));
        config.setBottomRightLed(Integer.parseInt(bottomRightLed.getText()));
        config.setOrientation(orientation.getValue());

        try {
            StorageManager sm = new StorageManager();
            sm.writeConfig(config);
            FireflyLuciferin.config = config;
            cancel(e);
        } catch (IOException ioException) {
            logger.error("Can't write config file.");
        }

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

        Text fireflyLuciferin = new Text("Firefly Luciferin");
        fireflyLuciferin.setFill(Color.CHOCOLATE);
        fireflyLuciferin.setStyle("-fx-font-weight: bold");
        fireflyLuciferin.setFont(Font.font(java.awt.Font.MONOSPACED, 60));
        Effect glow = new Glow(1.0);
        fireflyLuciferin.setEffect(glow);
        final int textPositionX = (int) ((scaleResolution(currentConfig.getScreenResX(),scaleRatio)/2) - (fireflyLuciferin.getLayoutBounds().getWidth()/2));
        fireflyLuciferin.setX(textPositionX);
        fireflyLuciferin.setY(scaleResolution((currentConfig.getScreenResY()/2), scaleRatio));
        root.getChildren().add(fireflyLuciferin);

        root.getChildren().add(canvas);
        stage.setScene(s);
        stage.show();
        stage.setFullScreen(true);

    }

    /**
     * Open browser to the GitHub project page
     * @param link GitHub
     */
    @FXML
    public void onMouseClickedGitHubLink(ActionEvent link) {

        Desktop desktop = Desktop.getDesktop();
        try {
            String myUrl = "https://github.com/sblantipodi/firefly_luciferin";
            URI github = new URI(myUrl);
            desktop.browse(github);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Start and stop capturing
     * @param e InputEvent
     */
    @FXML
    public void onMouseClickedPlay(InputEvent e) {

        controlImage = new Image(this.getClass().getResource("/org/dpsoftware/gui/img/java_fast_screen_capture_logo_grey.png").toString(), true);
        if (!FireflyLuciferin.communicationError) {
            if (FireflyLuciferin.RUNNING) {
                controlImage = new Image(this.getClass().getResource("/org/dpsoftware/gui/img/java_fast_screen_capture_logo.png").toString(), true);
            } else {
                controlImage = new Image(this.getClass().getResource("/org/dpsoftware/gui/img/java_fast_screen_capture_logo_play.png").toString(), true);
            }
            imageView = new ImageView(controlImage);
            imageView.setFitHeight(80);
            imageView.setPreserveRatio(true);
            playButton.setGraphic(imageView);
            if (FireflyLuciferin.RUNNING) {
                FireflyLuciferin.guiManager.stopCapturingThreads();
            } else {
                FireflyLuciferin.guiManager.startCapturingThreads();
            }
        }

    }

    /**
     * Display a canvas, useful to test LED matrix
     * @param gc graphics canvas
     * @param conf stored config
     */
    private void drawTestShapes(GraphicsContext gc, Configuration conf) {

        LinkedHashMap<Integer, LEDCoordinate> ledMatrix = conf.getLedMatrixInUse(conf.getDefaultLedMatrix());

        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(10);
        gc.stroke();

        int scaleRatio = conf.getOsScaling();
        AtomicInteger ledDistance = new AtomicInteger();
        ledMatrix.forEach((key, coordinate) -> {

            int colorToUse = key;
            if (key > 3) {
                while (colorToUse > 3) {
                    colorToUse -= 3;
                }
            }
            switch (colorToUse) {
                case 1 -> gc.setFill(Color.RED);
                case 2 -> gc.setFill(Color.GREEN);
                default -> gc.setFill(Color.BLUE);
            }

            String ledNum;
            if ("Clockwise".equals(conf.getOrientation())) {
                ledNum = "#" + ((conf.getBottomRightLed()+conf.getRightLed()+conf.getTopLed()+conf.getLeftLed()+conf.getBottomLeftLed()) - (key-1));
            } else {
                ledNum = "#" + key;
            }
            int twelveX = scaleResolution(conf.getScreenResX(), scaleRatio) / 12;

            if (key <= conf.getBottomRightLed()) { // Bottom right
                if (ledDistance.get() == 0) {
                    ledDistance.set(scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) - scaleResolution(coordinate.getX(), scaleRatio));
                }
                gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio)+10, scaleResolution(coordinate.getY(), scaleRatio),
                        ledDistance.get() - 10, scaleResolution(coordinate.getY(), scaleRatio));
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, scaleResolution(coordinate.getX(), scaleRatio) + 12, scaleResolution(coordinate.getY(), scaleRatio) + 15);
            } else if (key <= conf.getBottomRightLed() + conf.getRightLed()) { // Right
                if (key == conf.getBottomRightLed() + 1) {
                    ledDistance.set(scaleResolution(coordinate.getY(), scaleRatio) - scaleResolution(ledMatrix.get(key + 1).getY(), scaleRatio));
                }
                gc.fillRect(scaleResolution(conf.getScreenResX(), scaleRatio) - twelveX, scaleResolution(coordinate.getY(), scaleRatio),
                        twelveX, ledDistance.get() - 10);
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, scaleResolution(conf.getScreenResX(), scaleRatio) - (twelveX) + 2, scaleResolution(coordinate.getY(), scaleRatio) + 15);
            } else if (key > (conf.getBottomRightLed() + conf.getRightLed()) && key <= (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed())) { // Top
                if (key == (conf.getBottomRightLed() + conf.getRightLed()) + 1) {
                    ledDistance.set(scaleResolution(coordinate.getX(), scaleRatio) - scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio));
                }
                gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio), 0,
                        ledDistance.get() - 10, scaleResolution(coordinate.getY() + 20, scaleRatio));
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, scaleResolution(coordinate.getX(), scaleRatio) + 2, 15);
            } else if (key > (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed()) && key <= (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed() + conf.getLeftLed())) { // Left
                if (key == (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed()) + 1) {
                    ledDistance.set(scaleResolution(ledMatrix.get(key + 1).getY(), scaleRatio) - scaleResolution(coordinate.getY(), scaleRatio));
                }
                gc.fillRect(0, scaleResolution(coordinate.getY(), scaleRatio),
                        twelveX, ledDistance.get() - 10);
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, 0, scaleResolution(coordinate.getY(), scaleRatio) + 15);
            } else { // bottom left
                if (key == (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed() + conf.getLeftLed()) + 1) {
                    ledDistance.set(scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) - scaleResolution(coordinate.getX(), scaleRatio));
                }
                gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio), scaleResolution(coordinate.getY(), scaleRatio),
                        ledDistance.get() - 10, scaleResolution(coordinate.getY(), scaleRatio));
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, scaleResolution(coordinate.getX(), scaleRatio) + 2, scaleResolution(coordinate.getY(), scaleRatio) + 15);
            }

            Image image = new Image(getClass().getResource("/org/dpsoftware/gui/img/java_fast_screen_capture_logo.png").toString());
            gc.drawImage(image, scaleResolution((conf.getScreenResX()/2), scaleRatio)-64,scaleResolution((conf.getScreenResY()/3), scaleRatio) );

        });

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
        gamma.setTooltip(createTooltip("Smaller values results in brighter LEDs but less accurate colors. 2.2 is generally good for SDR contents, 6.0 is generally good for HDR contents"));
        if (com.sun.jna.Platform.isWindows()) {
            captureMethod.setTooltip(createTooltip("If you have a GPU, Desktop Duplication API (DDUPL) is faster than other methods"));
        } else {
            linuxCaptureMethod.setTooltip(createTooltip("If you have a GPU, Desktop Duplication API (DDUPL) is faster than other methods"));
        }
        numberOfThreads.setTooltip(createTooltip("1 thread is enough when using DDUPL, 3 or more threads are recommended for other capture methods"));
        serialPort.setTooltip(createTooltip("AUTO detects first serial port available, change it if you have more than one serial port available"));
        aspectRatio.setTooltip(createTooltip("LetterBox is recommended for films, you can change this option later"));

        mqttHost.setTooltip(createTooltip("OPTIONAL: MQTT protocol://host"));
        mqttPort.setTooltip(createTooltip("OPTIONAL: MQTT port"));
        mqttTopic.setTooltip(createTooltip("OPTIONAL: MQTT topic, used to start/stop capturing. Don't change it if you want to use Glow Worm Luciferin Firmware."));
        mqttUser.setTooltip(createTooltip("OPTIONAL: MQTT username"));
        mqttPwd.setTooltip(createTooltip("OPTIONAL: MQTT password"));
        mqttEnable.setTooltip(createTooltip("MQTT is Optional"));
        mqttStream.setTooltip(createTooltip("Prefer wireless stream over serial port (USB cable). This option is ignored if MQTT is disabled. Enable this option if you don't have the possibility to use a USB cable."));

        if (currentConfig == null) {
            if (!com.sun.jna.Platform.isWindows()) {
                playButton.setTooltip(createTooltip("Please configure and save before capturing", 0, 6000));
            }
            saveLedButton.setTooltip(createTooltip("You can change this options later"));
            saveMQTTButton.setTooltip(createTooltip("You can change this options later"));
            saveSettingsButton.setTooltip(createTooltip("You can change this options later"));
        } else {
            if (!com.sun.jna.Platform.isWindows()) {
                playButton.setTooltip(createTooltip("START/STOP capturing", 50, 6000));
            }
            saveLedButton.setTooltip(createTooltip("Changes will take effect the next time you launch the app",0, 6000));
            saveMQTTButton.setTooltip(createTooltip("Changes will take effect the next time you launch the app",0, 6000));
            saveSettingsButton.setTooltip(createTooltip("Changes will take effect the next time you launch the app",0, 6000));
            showTestImageButton.setTooltip(createTooltip("Show a test image, useful to check for LED alignment behind the monitor",200, 6000));
        }

    }

    /**
     * Set tooltip properties
     * @param text tooltip string
     */
    public Tooltip createTooltip(String text) {

        Tooltip tooltip;
        tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(500));
        tooltip.setHideDelay(Duration.millis(6000));
        return tooltip;

    }

    /**
     * Set tooltip properties width delays
     * @param text tooltip string
     * @param showDelay delay used to show the tooltip
     * @param hideDelay delay used to hide the tooltip
     */
    public Tooltip createTooltip(String text, int showDelay, int hideDelay) {

        Tooltip tooltip;
        tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(showDelay));
        tooltip.setHideDelay(Duration.millis(hideDelay));
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

    public StringProperty producerValueProperty() {
        return producerValue;
    }

    public void setProducerValue(String producerValue) {
        this.producerValue.set(producerValue);
    }

    public StringProperty consumerValueProperty() {
        return consumerValue;
    }

    public void setConsumerValue(String consumerValue) {
        this.consumerValue.set(consumerValue);
    }

}
