/*
  SettingsController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020  Davide Perini

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

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.StorageManager;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.GlowWormDevice;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;


/**
 * FXML Settings Controller
 */
@Slf4j
public class SettingsController {

    @FXML private TabPane mainTabPane;
    @FXML private TextField screenWidth;
    @FXML private TextField screenHeight;
    @FXML private TextField ledStartOffset;
    @FXML private ComboBox<String> scaling;
    @FXML private ComboBox<String> gamma;
    @FXML private ComboBox<Configuration.CaptureMethod> captureMethod;
    @FXML private TextField numberOfThreads;
    @FXML private Button saveLedButton;
    @FXML private Button playButton;
    @FXML private Button saveMQTTButton;
    @FXML private Button saveMiscButton;
    @FXML private Button saveSettingsButton;
    @FXML private Button saveDeviceButton;
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
    @FXML private CheckBox startWithSystem;
    @FXML private CheckBox checkForUpdates;
    @FXML private TextField topLed;
    @FXML private TextField leftLed;
    @FXML private TextField rightLed;
    @FXML private TextField bottomLeftLed;
    @FXML private TextField bottomRightLed;
    @FXML private TextField bottomRowLed;
    @FXML private ComboBox<String> orientation;
    @FXML private Label producerLabel;
    @FXML private Label consumerLabel;
    @FXML private Label version;
    @FXML private final StringProperty producerValue = new SimpleStringProperty("");
    @FXML private final StringProperty consumerValue = new SimpleStringProperty("");
    @FXML private TableView<GlowWormDevice> deviceTable;
    @FXML private TableColumn<GlowWormDevice, String> deviceNameColumn;
    @FXML private TableColumn<GlowWormDevice, String> deviceIPColumn;
    @FXML private TableColumn<GlowWormDevice, String> deviceVersionColumn;
    @FXML private TableColumn<GlowWormDevice, String> macColumn;
    @FXML private TableColumn<GlowWormDevice, String> numberOfLEDSconnectedColumn;
    @FXML private Label versionLabel;
    public static ObservableList<GlowWormDevice> deviceTableData = FXCollections.observableArrayList();
    @FXML private CheckBox autoStart;
    @FXML private CheckBox eyeCare;
    @FXML private CheckBox splitBottomRow;
    @FXML private ComboBox<String> framerate;
    @FXML private ColorPicker colorPicker;
    @FXML private ToggleButton toggleLed;
    @FXML private Slider brightness;
    @FXML private Label bottomLeftLedLabel;
    @FXML private Label bottomRightLedLabel;
    @FXML private Label bottomRowLedLabel;
    Image controlImage;
    ImageView imageView;
    AnimationTimer animationTimer;


    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {

        Platform.setImplicitExit(false);

        scaling.getItems().addAll("100%", "125%", "150%", "175%", "200%", "225%", "250%", "300%", "350%");
        gamma.getItems().addAll("1.0", "1.8", "2.0", "2.2", "2.4", "4.0", "5.0", "6.0", "8.0", "10.0");
        serialPort.getItems().add(Constants.SERIAL_PORT_AUTO);
        if (com.sun.jna.Platform.isWindows()) {
            for (int i=0; i<=256; i++) {
                serialPort.getItems().add(Constants.SERIAL_PORT_COM + i);
            }
            captureMethod.getItems().addAll(Configuration.CaptureMethod.DDUPL, Configuration.CaptureMethod.WinAPI, Configuration.CaptureMethod.CPU);
        } else if (com.sun.jna.Platform.isMac()) {
            captureMethod.getItems().addAll(Configuration.CaptureMethod.AVFVIDEOSRC);
        } else {
            if (FireflyLuciferin.communicationError) {
                controlImage = new Image(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY).toString(), true);
            } else if (FireflyLuciferin.RUNNING) {
                controlImage = new Image(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY).toString(), true);
            } else {
                controlImage = new Image(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO).toString(), true);
            }
            imageView = new ImageView(controlImage);
            imageView.setFitHeight(80);
            imageView.setPreserveRatio(true);
            playButton.setGraphic(imageView);
            for (int i=0; i<=256; i++) {
                serialPort.getItems().add(Constants.SERIAL_PORT_TTY + i);
            }
            captureMethod.getItems().addAll(Configuration.CaptureMethod.XIMAGESRC);
            version.setText("by Davide Perini (VERSION)".replaceAll("VERSION", FireflyLuciferin.version));
        }
        orientation.getItems().addAll(Constants.CLOCKWISE, Constants.ANTICLOCKWISE);
        aspectRatio.getItems().addAll(Constants.FULLSCREEN, Constants.LETTERBOX);
        framerate.getItems().addAll("10 FPS", "20 FPS", "30 FPS", "40 FPS", "50 FPS", "60 FPS", Constants.UNLOCKED);
        if (FireflyLuciferin.ledNumber > Constants.FIRST_CHUNK) {
            framerate.setDisable(true);
        }
        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readConfig();
        showTestImageButton.setVisible(currentConfig != null);
        setSaveButtonText(currentConfig);
        // Init default values
        initDefaultValues(currentConfig);
        // Init tooltips
        setTooltips(currentConfig);
        // Force numeric fields
        setNumericTextField();
        runLater();
        // Device table
        deviceNameColumn.setCellValueFactory(cellData -> cellData.getValue().deviceNameProperty());
        deviceIPColumn.setCellValueFactory(cellData -> cellData.getValue().deviceIPProperty());
        deviceVersionColumn.setCellValueFactory(cellData -> cellData.getValue().deviceVersionProperty());
        macColumn.setCellValueFactory(cellData -> cellData.getValue().macProperty());
        numberOfLEDSconnectedColumn.setCellValueFactory(cellData -> cellData.getValue().numberOfLEDSconnectedProperty());
        deviceTable.setItems(getDeviceTableData());
        initListeners(currentConfig);
        startAnimationTimer();

    }

    /**
     * Run Later after GUI Init
     */
    private void runLater() {

        if (com.sun.jna.Platform.isWindows()) {
            Platform.runLater(() -> {
                Stage stage = (Stage) mainTabPane.getScene().getWindow();
                stage.setOnCloseRequest(evt -> {
                    if (!SystemTray.isSupported() || com.sun.jna.Platform.isLinux()) {
                        System.exit(0);
                    } else {
                        animationTimer.stop();
                    }
                });
                orientation.requestFocus();
            });
        }

    }

    /**
     * Manage animation timer to update the UI every seconds
     */
    private void startAnimationTimer() {

        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0 ;
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 1_000_000_000) {
                    lastUpdate = now;
                    if (com.sun.jna.Platform.isWindows()) {
                        manageDeviceList();
                    } else {
                        manageDeviceList();
                        setProducerValue("Producing @ " + FireflyLuciferin.FPS_PRODUCER + " FPS");
                        setConsumerValue("Consuming @ " + (FireflyLuciferin.config.isMqttEnable() ? FireflyLuciferin.FPS_GW_CONSUMER : FireflyLuciferin.FPS_CONSUMER) + " FPS");
                    }
                }
            }
        };
        animationTimer.start();

    }

    /**
     * Init all the settings listener
     * @param currentConfig stored config
     */
    private void initListeners(Configuration currentConfig) {

        // Toggle LED button listener
        toggleLed.setOnAction(e -> {
            if ((toggleLed.isSelected())) {
                toggleLed.setText(Constants.TURN_LED_OFF);
                turnOnLEDs(currentConfig, true);
                FireflyLuciferin.config.setToggleLed(true);
            } else {
                toggleLed.setText(Constants.TURN_LED_ON);
                turnOffLEDs(currentConfig);
                FireflyLuciferin.config.setToggleLed(false);
            }
        });
        // Color picker listener
        EventHandler<ActionEvent> colorPickerEvent = e -> turnOnLEDs(currentConfig, true);
        colorPicker.setOnAction(colorPickerEvent);
        // Gamma can be changed on the fly
        gamma.valueProperty().addListener((ov, t, gamma) -> {
            if (currentConfig != null && currentConfig.isMqttEnable()) {
                FireflyLuciferin.guiManager.mqttManager.publishToTopic(Constants.FIREFLY_LUCIFERIN_GAMMA,
                        "{\""+Constants.MQTT_GAMMA+"\":" + gamma + "}");
            }
            FireflyLuciferin.config.setGamma(Double.parseDouble(gamma));
        });
        brightness.valueProperty().addListener((ov, old_val, new_val) -> turnOnLEDs(currentConfig, false));
        splitBottomRow.setOnAction(e -> splitBottomRow());

    }

    /**
     * Manage the device list tab update
     */
    void manageDeviceList() {

        Calendar calendar = Calendar.getInstance();
        ObservableList<GlowWormDevice> deviceTableDataToRemove = FXCollections.observableArrayList();
        deviceTableData.forEach(glowWormDevice -> {
            calendar.setTime(new Date());
            calendar.add(Calendar.SECOND, -20);
            try {
                if (calendar.getTime().after(FireflyLuciferin.formatter.parse(glowWormDevice.getLastSeen()))) {
                    deviceTableDataToRemove.add(glowWormDevice);
                }
            } catch (ParseException e) {
                log.error(e.getMessage());
            }
        });
        deviceTableData.removeAll(deviceTableDataToRemove);
        deviceTable.refresh();

    }

    /**
     * Init Save Button Text
     * @param currentConfig stored config
     */
    private void setSaveButtonText(Configuration currentConfig) {

        if (currentConfig == null) {
            saveLedButton.setText(Constants.SAVE);
            saveSettingsButton.setText(Constants.SAVE);
            saveMQTTButton.setText(Constants.SAVE);
            saveMiscButton.setText(Constants.SAVE);
            saveDeviceButton.setText(Constants.SAVE);
            if (com.sun.jna.Platform.isWindows()) {
                saveLedButton.setPrefWidth(95);
                saveSettingsButton.setPrefWidth(95);
                saveMQTTButton.setPrefWidth(95);
                saveMiscButton.setPrefWidth(95);
                saveDeviceButton.setPrefWidth(95);
            } else {
                saveLedButton.setPrefWidth(125);
                saveSettingsButton.setPrefWidth(125);
                saveMQTTButton.setPrefWidth(125);
                saveMiscButton.setPrefWidth(125);
                saveDeviceButton.setPrefWidth(125);
            }
        } else {
            saveLedButton.setText(Constants.SAVE_AND_CLOSE);
            saveSettingsButton.setText(Constants.SAVE_AND_CLOSE);
            saveMQTTButton.setText(Constants.SAVE_AND_CLOSE);
            saveMiscButton.setText(Constants.SAVE_AND_CLOSE);
            saveDeviceButton.setText(Constants.SAVE_AND_CLOSE);
        }

    }

    /**
     * Init form values
     */
    void initDefaultValues(Configuration currentConfig) {

        versionLabel.setText(Constants.FIREFLY_LUCIFERIN + " (v" + FireflyLuciferin.version + ")");
        brightness.setMin(0);
        brightness.setMax(100);
        brightness.setMajorTickUnit(10);
        brightness.setMinorTickCount(5);
        brightness.setShowTickMarks(true);
        brightness.setBlockIncrement(10);
        brightness.setShowTickLabels(true);

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
            ledStartOffset.setText(String.valueOf(0));
            scaling.setValue(((int) (screenInfo.getScaleX() * 100)) + Constants.PERCENT);
            if (com.sun.jna.Platform.isWindows()) {
                captureMethod.setValue(Configuration.CaptureMethod.DDUPL);
            } else if (com.sun.jna.Platform.isMac()) {
                captureMethod.setValue(Configuration.CaptureMethod.DDUPL);
            } else {
                captureMethod.setValue(Configuration.CaptureMethod.XIMAGESRC);
            }
            gamma.setValue(Constants.GAMMA_DEFAULT);
            serialPort.setValue(Constants.SERIAL_PORT_AUTO);
            numberOfThreads.setText("1");
            aspectRatio.setValue(Constants.FULLSCREEN);
            if (FireflyLuciferin.ledNumber > Constants.FIRST_CHUNK) {
                framerate.setValue("10 FPS");
            } else {
                framerate.setValue("30 FPS");
            }
            mqttHost.setText(Constants.DEFAULT_MQTT_HOST);
            mqttPort.setText(Constants.DEFAULT_MQTT_PORT);
            mqttTopic.setText(Constants.DEFAULT_MQTT_TOPIC);
            orientation.setValue(Constants.CLOCKWISE);
            topLed.setText("33");
            leftLed.setText("18");
            rightLed.setText("18");
            bottomLeftLed.setText("13");
            bottomRightLed.setText("13");
            bottomRowLed.setText("26");
            checkForUpdates.setSelected(true);
            toggleLed.setSelected(false);
            brightness.setValue(255);
            bottomLeftLed.setVisible(true);
            bottomRightLed.setVisible(true);
            bottomRowLed.setVisible(false);
            bottomLeftLedLabel.setVisible(true);
            bottomRightLedLabel.setVisible(true);
            bottomRowLedLabel.setVisible(false);
            splitBottomRow.setSelected(true);
        } else {
            initValuesFromSettingsFile(currentConfig);
        }
        deviceTable.setPlaceholder(new Label("No devices found"));

    }

    /**
     * Init form values by reading existing config file
     * @param currentConfig existing
     */
    private void initValuesFromSettingsFile(Configuration currentConfig) {

        if (com.sun.jna.Platform.isWindows()) {
            startWithSystem.setSelected(currentConfig.isStartWithSystem());
        }
        screenWidth.setText(String.valueOf(currentConfig.getScreenResX()));
        screenHeight.setText(String.valueOf(currentConfig.getScreenResY()));
        ledStartOffset.setText(String.valueOf(currentConfig.getLedStartOffset()));
        scaling.setValue(currentConfig.getOsScaling() + Constants.PERCENT);
        captureMethod.setValue(Configuration.CaptureMethod.valueOf(currentConfig.getCaptureMethod()));
        gamma.setValue(String.valueOf(currentConfig.getGamma()));
        serialPort.setValue(currentConfig.getSerialPort());
        numberOfThreads.setText(String.valueOf(currentConfig.getNumberOfCPUThreads()));
        aspectRatio.setValue(currentConfig.getDefaultLedMatrix());
        if (FireflyLuciferin.ledNumber > Constants.FIRST_CHUNK) {
            framerate.setValue("10 FPS");
        } else {
            framerate.setValue(currentConfig.getDesiredFramerate() + ((currentConfig.getDesiredFramerate().equals(Constants.UNLOCKED)) ? "" : " FPS"));
        }
        mqttHost.setText(currentConfig.getMqttServer().substring(0, currentConfig.getMqttServer().lastIndexOf(":")));
        mqttPort.setText(currentConfig.getMqttServer().substring(currentConfig.getMqttServer().lastIndexOf(":") + 1));
        mqttTopic.setText(currentConfig.getMqttTopic());
        mqttUser.setText(currentConfig.getMqttUsername());
        mqttPwd.setText(currentConfig.getMqttPwd());
        mqttEnable.setSelected(currentConfig.isMqttEnable());
        autoStart.setSelected(currentConfig.isAutoStartCapture());
        eyeCare.setSelected(currentConfig.isEyeCare());
        mqttStream.setSelected(currentConfig.isMqttStream());
        checkForUpdates.setSelected(currentConfig.isCheckForUpdates());
        orientation.setValue(currentConfig.getOrientation());
        topLed.setText(String.valueOf(currentConfig.getTopLed()));
        leftLed.setText(String.valueOf(currentConfig.getLeftLed()));
        rightLed.setText(String.valueOf(currentConfig.getRightLed()));
        bottomLeftLed.setText(String.valueOf(currentConfig.getBottomLeftLed()));
        bottomRightLed.setText(String.valueOf(currentConfig.getBottomRightLed()));
        bottomRowLed.setText(String.valueOf(currentConfig.getBottomRowLed()));
        String[] color = (FireflyLuciferin.config.getColorChooser().equals(Constants.DEFAULT_COLOR_CHOOSER)) ?
                currentConfig.getColorChooser().split(",") : FireflyLuciferin.config.getColorChooser().split(",");
        colorPicker.setValue(Color.rgb(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]), Double.parseDouble(color[3])/255));
        brightness.setValue((Double.parseDouble(color[3])/255)*100);
        if ((FireflyLuciferin.config.isToggleLed())) {
            toggleLed.setText(Constants.TURN_LED_OFF);
        } else {
            toggleLed.setText(Constants.TURN_LED_ON);
        }
        toggleLed.setSelected(FireflyLuciferin.config.isToggleLed());
        splitBottomRow.setSelected(currentConfig.isSplitBottomRow());
        splitBottomRow();

    }

    /**
     * Show hide bottom row options
     */
    private void splitBottomRow() {
        if (splitBottomRow.isSelected()) {
            bottomLeftLed.setVisible(true);
            bottomRightLed.setVisible(true);
            bottomRowLed.setVisible(false);
            bottomLeftLedLabel.setVisible(true);
            bottomRightLedLabel.setVisible(true);
            bottomRowLedLabel.setVisible(false);
        } else {
            bottomLeftLed.setVisible(false);
            bottomRightLed.setVisible(false);
            bottomRowLed.setVisible(true);
            bottomLeftLedLabel.setVisible(false);
            bottomRightLedLabel.setVisible(false);
            bottomRowLedLabel.setVisible(true);
        }
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
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()),
                Integer.parseInt(bottomRowLed.getText()), splitBottomRow);
        LinkedHashMap<Integer, LEDCoordinate> ledLetterboxMatrix = ledCoordinate.initLetterboxLedMatrix(Integer.parseInt(screenWidth.getText()),
                Integer.parseInt(screenHeight.getText()), Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(rightLed.getText()),
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()),
                Integer.parseInt(bottomRowLed.getText()), splitBottomRow);

        Configuration config = new Configuration(ledFullScreenMatrix,ledLetterboxMatrix);
        config.setNumberOfCPUThreads(Integer.parseInt(numberOfThreads.getText()));
        NativeExecutor nativeExecutor = new NativeExecutor();
        if (com.sun.jna.Platform.isWindows()) {
            switch (captureMethod.getValue()) {
                case DDUPL -> config.setCaptureMethod(Configuration.CaptureMethod.DDUPL.name());
                case WinAPI -> config.setCaptureMethod(Configuration.CaptureMethod.WinAPI.name());
                case CPU -> config.setCaptureMethod(Configuration.CaptureMethod.CPU.name());
            }
            if (startWithSystem.isSelected()) {
                nativeExecutor.writeRegistryKey();
            } else {
                nativeExecutor.deleteRegistryKey();
            }
            config.setStartWithSystem(startWithSystem.isSelected());
        } else if (com.sun.jna.Platform.isMac()) {
            if (captureMethod.getValue() == Configuration.CaptureMethod.AVFVIDEOSRC) {
                config.setCaptureMethod(Configuration.CaptureMethod.AVFVIDEOSRC.name());
            }
        } else {
            if (captureMethod.getValue() == Configuration.CaptureMethod.XIMAGESRC) {
                config.setCaptureMethod(Configuration.CaptureMethod.XIMAGESRC.name());
            }
        }
        config.setSerialPort(serialPort.getValue());
        config.setScreenResX(Integer.parseInt(screenWidth.getText()));
        config.setScreenResY(Integer.parseInt(screenHeight.getText()));
        config.setLedStartOffset(Integer.parseInt(ledStartOffset.getText()));
        config.setOsScaling(Integer.parseInt((scaling.getValue()).replace(Constants.PERCENT,"")));
        config.setGamma(Double.parseDouble(gamma.getValue()));
        config.setSerialPort(serialPort.getValue());
        config.setDefaultLedMatrix(aspectRatio.getValue());
        if (FireflyLuciferin.ledNumber > Constants.FIRST_CHUNK) {
            config.setDesiredFramerate("10");
        } else {
            config.setDesiredFramerate(framerate.getValue().equals(Constants.UNLOCKED) ? framerate.getValue() : framerate.getValue().substring(0,2));
        }
        config.setMqttServer(mqttHost.getText() + ":" + mqttPort.getText());
        config.setMqttTopic(mqttTopic.getText());
        config.setMqttUsername(mqttUser.getText());
        config.setMqttPwd(mqttPwd.getText());
        config.setMqttEnable(mqttEnable.isSelected());
        config.setEyeCare(eyeCare.isSelected());
        config.setAutoStartCapture(autoStart.isSelected());
        config.setMqttStream(mqttStream.isSelected());
        config.setCheckForUpdates(checkForUpdates.isSelected());
        config.setTopLed(Integer.parseInt(topLed.getText()));
        config.setLeftLed(Integer.parseInt(leftLed.getText()));
        config.setRightLed(Integer.parseInt(rightLed.getText()));
        config.setBottomLeftLed(Integer.parseInt(bottomLeftLed.getText()));
        config.setBottomRightLed(Integer.parseInt(bottomRightLed.getText()));
        config.setBottomRowLed(Integer.parseInt(bottomRowLed.getText()));
        config.setOrientation(orientation.getValue());
        config.setToggleLed(toggleLed.isSelected());
        config.setColorChooser((int)(colorPicker.getValue().getRed()*255) + "," + (int)(colorPicker.getValue().getGreen()*255) + ","
                + (int)(colorPicker.getValue().getBlue()*255) + "," + (int)(colorPicker.getValue().getOpacity()*255));
        config.setBrightness((int)(brightness.getValue()/100 *255));
        config.setSplitBottomRow(splitBottomRow.isSelected());
        try {
            StorageManager sm = new StorageManager();
            sm.writeConfig(config);
            boolean firstStartup = FireflyLuciferin.config == null;
            FireflyLuciferin.config = config;
            if (!firstStartup) {
                exit(e);
            } else {
                cancel(e);
            }
        } catch (IOException ioException) {
            log.error("Can't write config file.");
        }

    }

    /**
     * Save and Exit button event
     * @param event event
     */
    @FXML
    public void exit(InputEvent event) {

        cancel(event);
        if (FireflyLuciferin.guiManager != null) {
            FireflyLuciferin.guiManager.stopCapturingThreads();
        }
        try {
            TimeUnit.SECONDS.sleep(4);
            log.debug(Constants.CLEAN_EXIT);
            if (com.sun.jna.Platform.isWindows() || com.sun.jna.Platform.isLinux()) {
                NativeExecutor nativeExecutor = new NativeExecutor();
                try {
                    Runtime.getRuntime().exec(nativeExecutor.getInstallationPath());
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            System.exit(0);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }

    }

    /**
     * Cancel button event
     */
    @FXML
    public void cancel(InputEvent e) {

        animationTimer.stop();
        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();

    }

    /**
     * Open browser to the GitHub project page
     * @param link GitHub
     */
    @FXML
    public void onMouseClickedGitHubLink(ActionEvent link) {

        FireflyLuciferin.guiManager.surfToGitHub();

    }

    /**
     * Start and stop capturing
     * @param e InputEvent
     */
    @FXML
    public void onMouseClickedPlay(InputEvent e) {

        controlImage = new Image(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY).toString(), true);
        if (!FireflyLuciferin.communicationError) {
            if (FireflyLuciferin.RUNNING) {
                controlImage = new Image(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO).toString(), true);
            } else {
                controlImage = new Image(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY).toString(), true);
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
     * Show a canvas containing a test image for the LED Matrix in use
     * @param e event
     */
    @FXML
    public void showTestImage(InputEvent e) {

        TestCanvas testCanvas = new TestCanvas();
        testCanvas.buildAndShowTestImage(e);

    }

    /**
     * Turn ON LEDs
     * @param currentConfig stored config
     * @param setBrightness brightness level
     */
    void turnOnLEDs(Configuration currentConfig, boolean setBrightness) {

        if (setBrightness) {
            brightness.setValue((int)(colorPicker.getValue().getOpacity()*100));
        } else {
            colorPicker.setValue(Color.rgb((int)(colorPicker.getValue().getRed() * 255), (int)(colorPicker.getValue().getGreen() * 255),
                    (int)(colorPicker.getValue().getBlue() * 255), (brightness.getValue()/100)));
        }
        if (toggleLed.isSelected()) {
            if (currentConfig != null && currentConfig.isMqttEnable()) {
                FireflyLuciferin.guiManager.mqttManager.publishToTopic(FireflyLuciferin.config.getMqttTopic(), Constants.STATE_ON_SOLID_COLOR
                        .replace(Constants.RED_COLOR, String.valueOf((int)(colorPicker.getValue().getRed() * 255)))
                        .replace(Constants.GREEN_COLOR, String.valueOf((int)(colorPicker.getValue().getGreen() * 255)))
                        .replace(Constants.BLU_COLOR, String.valueOf((int)(colorPicker.getValue().getBlue() * 255)))
                        .replace(Constants.BRIGHTNESS, String.valueOf((int)((brightness.getValue() / 100) * 255))));
                FireflyLuciferin.usbBrightness = (int)((brightness.getValue() / 100) * 255);
            } else if (currentConfig != null && !currentConfig.isMqttEnable()) {
                java.awt.Color[] leds = new java.awt.Color[1];
                try {
                    leds[0] = new java.awt.Color((int)(colorPicker.getValue().getRed() * 255),
                            (int)(colorPicker.getValue().getGreen() * 255),
                            (int)(colorPicker.getValue().getBlue() * 255));
                    FireflyLuciferin.usbBrightness = (int)((brightness.getValue() / 100) * 255);
                    FireflyLuciferin.sendColorsViaUSB(leds, FireflyLuciferin.usbBrightness);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

    }

    /**
     * Turn ON LEDs
     * @param currentConfig stored config
     */
    void turnOffLEDs(Configuration currentConfig) {

        if (currentConfig != null) {
            if (currentConfig.isMqttEnable()) {
                FireflyLuciferin.guiManager.mqttManager.publishToTopic(FireflyLuciferin.config.getMqttTopic(), Constants.STATE_OFF_SOLID);
            } else {
                java.awt.Color[] leds = new java.awt.Color[1];
                try {
                    leds[0] = new java.awt.Color(0, 0, 0);
                    FireflyLuciferin.usbBrightness = 0;
                    FireflyLuciferin.sendColorsViaUSB(leds, FireflyLuciferin.usbBrightness);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

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

        topLed.setTooltip(createTooltip(Constants.TOOLTIP_TOPLED));
        leftLed.setTooltip(createTooltip(Constants.TOOLTIP_LEFTLED));
        rightLed.setTooltip(createTooltip(Constants.TOOLTIP_RIGHTLED));
        bottomLeftLed.setTooltip(createTooltip(Constants.TOOLTIP_BOTTOMLEFTLED));
        bottomRightLed.setTooltip(createTooltip(Constants.TOOLTIP_BOTTOMRIGHTLED));
        bottomRowLed.setTooltip(createTooltip(Constants.TOOLTIP_BOTTOMROWLED));
        orientation.setTooltip(createTooltip(Constants.TOOLTIP_ORIENTATION));
        screenWidth.setTooltip(createTooltip(Constants.TOOLTIP_SCREENWIDTH));
        screenHeight.setTooltip(createTooltip(Constants.TOOLTIP_SCREENHEIGHT));
        ledStartOffset.setTooltip(createTooltip(Constants.TOOLTIP_LEDSTARTOFFSET));
        scaling.setTooltip(createTooltip(Constants.TOOLTIP_SCALING));
        gamma.setTooltip(createTooltip(Constants.TOOLTIP_GAMMA));
        if (com.sun.jna.Platform.isWindows()) {
            captureMethod.setTooltip(createTooltip(Constants.TOOLTIP_CAPTUREMETHOD));
            startWithSystem.setTooltip(createTooltip(Constants.TOOLTIP_START_WITH_SYSTEM));
        } else if (com.sun.jna.Platform.isMac()) {
            captureMethod.setTooltip(createTooltip(Constants.TOOLTIP_MACCAPTUREMETHOD));
        } else {
            captureMethod.setTooltip(createTooltip(Constants.TOOLTIP_LINUXCAPTUREMETHOD));
        }
        numberOfThreads.setTooltip(createTooltip(Constants.TOOLTIP_NUMBEROFTHREADS));
        serialPort.setTooltip(createTooltip(Constants.TOOLTIP_SERIALPORT));
        aspectRatio.setTooltip(createTooltip(Constants.TOOLTIP_ASPECTRATIO));
        framerate.setTooltip(createTooltip(Constants.TOOLTIP_FRAMERATE));

        mqttHost.setTooltip(createTooltip(Constants.TOOLTIP_MQTTHOST));
        mqttPort.setTooltip(createTooltip(Constants.TOOLTIP_MQTTPORT));
        mqttTopic.setTooltip(createTooltip(Constants.TOOLTIP_MQTTTOPIC));
        mqttUser.setTooltip(createTooltip(Constants.TOOLTIP_MQTTUSER));
        mqttPwd.setTooltip(createTooltip(Constants.TOOLTIP_MQTTPWD));
        mqttEnable.setTooltip(createTooltip(Constants.TOOLTIP_MQTTENABLE));
        eyeCare.setTooltip(createTooltip(Constants.TOOLTIP_EYE_CARE));
        autoStart.setTooltip(createTooltip(Constants.TOOLTIP_AUTOSTART));
        mqttStream.setTooltip(createTooltip(Constants.TOOLTIP_MQTTSTREAM));
        checkForUpdates.setTooltip(createTooltip(Constants.TOOLTIP_CHECK_UPDATES));
        brightness.setTooltip(createTooltip(Constants.TOOLTIP_BRIGHTNESS));
        splitBottomRow.setTooltip(createTooltip(Constants.TOOLTIP_SPLIT_BOTTOM_ROW));
        if (currentConfig == null) {
            if (!com.sun.jna.Platform.isWindows()) {
                playButton.setTooltip(createTooltip(Constants.TOOLTIP_PLAYBUTTON_NULL, 50, 6000));
            }
            saveLedButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVELEDBUTTON_NULL));
            saveMQTTButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON_NULL));
            saveMiscButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON_NULL));
            saveSettingsButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVESETTINGSBUTTON_NULL));
            saveDeviceButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEDEVICEBUTTON_NULL));
        } else {
            if (!com.sun.jna.Platform.isWindows()) {
                playButton.setTooltip(createTooltip(Constants.TOOLTIP_PLAYBUTTON, 200, 6000));
            }
            saveLedButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVELEDBUTTON,200, 6000));
            saveMQTTButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON,200, 6000));
            saveMiscButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON,200, 6000));
            saveSettingsButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVESETTINGSBUTTON,200, 6000));
            saveDeviceButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEDEVICEBUTTON,200, 6000));
            showTestImageButton.setTooltip(createTooltip(Constants.TOOLTIP_SHOWTESTIMAGEBUTTON,200, 6000));
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
        addTextFieldListener(ledStartOffset);
        addTextFieldListener(numberOfThreads);
        addTextFieldListener(mqttPort);
        addTextFieldListener(topLed);
        addTextFieldListener(leftLed);
        addTextFieldListener(rightLed);
        addTextFieldListener(bottomLeftLed);
        addTextFieldListener(bottomRightLed);
        addTextFieldListener(bottomRowLed);

    }

    /**
     * Return the observable devices list
     * @return devices list
     */
    public ObservableList<GlowWormDevice> getDeviceTableData() {
        return deviceTableData;
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
