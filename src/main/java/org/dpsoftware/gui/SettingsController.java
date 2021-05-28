/*
  SettingsController.java

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

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


/**
 * FXML Settings Controller
 */
@Slf4j
public class SettingsController {

    // Inject Tab controllers
    @FXML private MqttTabController mqttTabController;
    @FXML private DevicesTabController devicesTabController;
    @FXML private ModeTabController modeTabController;
    @FXML private MiscTabController miscTabController;
    // FXML binding
    @FXML private TabPane mainTabPane;
    @FXML private TextField ledStartOffset;
    @FXML private Button saveLedButton;
    @FXML private Button playButton;
    @FXML private Button showTestImageButton;
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
    @FXML private CheckBox splitBottomRow;
    @FXML private Label bottomLeftLedLabel;
    @FXML private Label bottomRightLedLabel;
    @FXML private Label bottomRowLedLabel;
    @FXML private Label displayLabel;
    ImageView imageView;
    Image controlImage;
    AnimationTimer animationTimer;
    Configuration currentConfig;
    StorageManager sm;
    DisplayManager displayManager;


    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {

        // Inject main controller into children
        mqttTabController.injectSettingsController(this);
        devicesTabController.injectSettingsController(this);
        modeTabController.injectSettingsController(this);
        miscTabController.injectSettingsController(this);

        Platform.setImplicitExit(false);
        sm = new StorageManager();
        displayManager = new DisplayManager();
        for (int i=1; i <= displayManager.displayNumber(); i++) {
            modeTabController.monitorNumber.getItems().add(i);
            switch (i) {
                case 1 -> devicesTabController.multiMonitor.getItems().add(Constants.MULTIMONITOR_1);
                case 2 -> devicesTabController.multiMonitor.getItems().add(Constants.MULTIMONITOR_2);
                case 3 -> devicesTabController.multiMonitor.getItems().add(Constants.MULTIMONITOR_3);
            }
        }
        currentConfig = sm.readConfig(false);
        initComboBox();
        if (NativeExecutor.isLinux()) {
            producerLabel.textProperty().bind(producerValueProperty());
            consumerLabel.textProperty().bind(consumerValueProperty());
            if (FireflyLuciferin.communicationError) {
                controlImage = setImage(Constants.PlayerStatus.GREY);
            } else if (FireflyLuciferin.RUNNING) {
                controlImage = setImage(Constants.PlayerStatus.PLAY_WAITING);
            } else {
                controlImage = setImage(Constants.PlayerStatus.STOP);
            }
            setButtonImage();
            version.setText("by Davide Perini (VERSION)".replaceAll("VERSION", FireflyLuciferin.version));
        }
        orientation.getItems().addAll(Constants.CLOCKWISE, Constants.ANTICLOCKWISE);
        showTestImageButton.setVisible(currentConfig != null);
        setSaveButtonText();
        // Init default values
        initDefaultValues();
        // Init tooltips
        setTooltips();
        // Force numeric fields
        setNumericTextField();
        runLater();
        initListeners();
        startAnimationTimer();

    }

    /**
     * Init combo boxes
     */
    void initComboBox() {

        modeTabController.initComboBox();
        miscTabController.initComboBox();

    }

    /**
     * Init form values
     */
    void initDefaultValues() {

        initOutputDeviceChooser(true);
        if (currentConfig == null) {
            ledStartOffset.setText(String.valueOf(0));
            mqttTabController.initDefaultValues();
            devicesTabController.initDefaultValues();
            modeTabController.initDefaultValues();
            miscTabController.initDefaultValues();
            orientation.setValue(Constants.CLOCKWISE);
            topLed.setText("33");
            leftLed.setText("18");
            rightLed.setText("18");
            bottomLeftLed.setText("13");
            bottomRightLed.setText("13");
            bottomRowLed.setText("26");
            bottomLeftLed.setVisible(true);
            bottomRightLed.setVisible(true);
            bottomRowLed.setVisible(false);
            bottomLeftLedLabel.setVisible(true);
            bottomRightLedLabel.setVisible(true);
            bottomRowLedLabel.setVisible(false);
            splitBottomRow.setSelected(true);
        } else {
            initValuesFromSettingsFile();
        }

    }

    /**
     * Init form values by reading existing config file
     */
    private void initValuesFromSettingsFile() {

        switch (JavaFXStarter.whoAmI) {
            case 1:
                if ((currentConfig.getMultiMonitor() == 1)) {
                    displayLabel.setText(Constants.MAIN_DISPLAY);
                } else {
                    displayLabel.setText(Constants.RIGHT_DISPLAY);
                }
                break;
            case 2:
                if ((currentConfig.getMultiMonitor() == 2)) {
                    displayLabel.setText(Constants.LEFT_DISPLAY);
                } else {
                    displayLabel.setText(Constants.CENTER_DISPLAY);
                }
                break;
            case 3: displayLabel.setText(Constants.LEFT_DISPLAY); break;
        }
        if (!NativeExecutor.isWindows() && FireflyLuciferin.config.isToggleLed() && (Constants.Effect.BIAS_LIGHT.getEffect().equals(FireflyLuciferin.config.getEffect())
                || Constants.Effect.MUSIC_MODE_VU_METER.getEffect().equals(FireflyLuciferin.config.getEffect())
                || Constants.Effect.MUSIC_MODE_BRIGHT.getEffect().equals(FireflyLuciferin.config.getEffect())
                || Constants.Effect.MUSIC_MODE_RAINBOW.getEffect().equals(FireflyLuciferin.config.getEffect()))) {
            controlImage = setImage(Constants.PlayerStatus.PLAY_WAITING);
            setButtonImage();
        }
        ledStartOffset.setText(String.valueOf(currentConfig.getLedStartOffset()));
        orientation.setValue(currentConfig.getOrientation());
        topLed.setText(String.valueOf(currentConfig.getTopLed()));
        leftLed.setText(String.valueOf(currentConfig.getLeftLed()));
        rightLed.setText(String.valueOf(currentConfig.getRightLed()));
        bottomLeftLed.setText(String.valueOf(currentConfig.getBottomLeftLed()));
        bottomRightLed.setText(String.valueOf(currentConfig.getBottomRightLed()));
        bottomRowLed.setText(String.valueOf(currentConfig.getBottomRowLed()));
        splitBottomRow.setSelected(currentConfig.isSplitBottomRow());
        mqttTabController.initValuesFromSettingsFile(currentConfig);
        devicesTabController.initValuesFromSettingsFile(currentConfig);
        modeTabController.initValuesFromSettingsFile(currentConfig);
        miscTabController.initValuesFromSettingsFile(currentConfig);
        splitBottomRow();
        miscTabController.setContextMenu();

    }

    /**
     * Run Later after GUI Init
     */
    private void runLater() {

        Platform.runLater(() -> {
            Stage stage = (Stage) mainTabPane.getScene().getWindow();
            if (stage != null) {
                stage.setOnCloseRequest(evt -> {
                    if (!NativeExecutor.isSystemTraySupported() || NativeExecutor.isLinux()) {
                        FireflyLuciferin.exit();
                    } else {
                        animationTimer.stop();
                    }
                });
            }
            devicesTabController.setTableEdit();
            orientation.requestFocus();
        });

    }

    /**
     * Manage animation timer to update the UI every seconds
     */
    private void startAnimationTimer() {

        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0 ;
            @Override
            public void handle(long now) {
                now = now / 1_000_000_000;
                if (now - lastUpdate >= 1) {
                    lastUpdate = now;
                    if (NativeExecutor.isWindows()) {
                        manageDeviceList();
                    } else {
                        manageDeviceList();
                        setProducerValue("Producing @ " + FireflyLuciferin.FPS_PRODUCER + " FPS");
                        setConsumerValue("Consuming @ " + FireflyLuciferin.FPS_GW_CONSUMER + " FPS");
                        if (FireflyLuciferin.RUNNING && controlImage != null && controlImage.getUrl().contains("waiting")) {
                            controlImage = setImage(Constants.PlayerStatus.PLAY);
                            setButtonImage();
                        }
                    }
                }
            }
        };
        animationTimer.start();

    }

    /**
     * Init all the settings listener
     */
    private void initListeners() {

        setSerialPortAvailableCombo();
        splitBottomRow.setOnAction(e -> splitBottomRow());
        mqttTabController.initListeners();
        modeTabController.initListeners();
        miscTabController.initListeners(currentConfig);
        devicesTabController.multiMonitor.valueProperty().addListener((ov, t, value) -> {
            if (!modeTabController.serialPort.isFocused()) {
                if (!value.equals(Constants.MULTIMONITOR_1)) {
                    if (modeTabController.serialPort.getItems().size() > 0 && modeTabController.serialPort.getItems().get(0).equals(Constants.SERIAL_PORT_AUTO)) {
                        modeTabController.serialPort.getItems().remove(0);
                        if (NativeExecutor.isWindows()) {
                            modeTabController.serialPort.setValue(Constants.SERIAL_PORT_COM + 1);
                        } else {
                            modeTabController.serialPort.setValue(Constants.SERIAL_PORT_TTY + 1);
                        }
                    }
                } else {
                    if (!modeTabController.serialPort.getItems().contains(Constants.SERIAL_PORT_AUTO)) {
                        modeTabController.serialPort.getItems().add(0, Constants.SERIAL_PORT_AUTO);
                    }
                }
            }
        });

    }

    /**
     * Add bold style to the available serial ports
     */
    void setSerialPortAvailableCombo() {

        Map<String, Boolean> availableDevices = FireflyLuciferin.getAvailableDevices();
        modeTabController.serialPort.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item);
                            this.getStyleClass().remove(Constants.CSS_CLASS_BOLD);
                            availableDevices.forEach((portName, isAvailable) -> {
                                if (item.contains(portName) && isAvailable) {
                                    this.getStyleClass().add(Constants.CSS_CLASS_BOLD);
                                } else if (item.contains(portName) && !isAvailable) {
                                    this.getStyleClass().add(Constants.CSS_CLASS_BOLD);
                                    this.getStyleClass().add(Constants.CSS_CLASS_RED);
                                }
                            });
                        } else {
                            setText(null);
                        }
                    }
                };
            }
        });

    }

    /**
     * Manage the device list tab update
     */
    void manageDeviceList() {

        devicesTabController.manageDeviceList();
        if (!devicesTabController.cellEdit) {
            if (mqttTabController.mqttStream.isSelected()) {
                initOutputDeviceChooser(true);
            }
        }

    }

    /**
     * Init Save Button Text
     */
    private void setSaveButtonText() {

        if (currentConfig == null) {
            saveLedButton.setText(Constants.SAVE);
            modeTabController.saveSettingsButton.setText(Constants.SAVE);
            mqttTabController.saveMQTTButton.setText(Constants.SAVE);
            miscTabController.saveMiscButton.setText(Constants.SAVE);
            devicesTabController.saveDeviceButton.setText(Constants.SAVE);
            if (NativeExecutor.isWindows()) {
                saveLedButton.setPrefWidth(95);
                modeTabController.saveSettingsButton.setPrefWidth(95);
                mqttTabController.saveMQTTButton.setPrefWidth(95);
                miscTabController.saveMiscButton.setPrefWidth(95);
                devicesTabController.saveDeviceButton.setPrefWidth(95);
            } else {
                saveLedButton.setPrefWidth(125);
                modeTabController.saveSettingsButton.setPrefWidth(125);
                mqttTabController.saveMQTTButton.setPrefWidth(125);
                miscTabController.saveMiscButton.setPrefWidth(125);
                devicesTabController.saveDeviceButton.setPrefWidth(125);
            }
        } else {
            saveLedButton.setText(Constants.SAVE_AND_CLOSE);
            modeTabController.saveSettingsButton.setText(Constants.SAVE_AND_CLOSE);
            mqttTabController.saveMQTTButton.setText(Constants.SAVE_AND_CLOSE);
            miscTabController.saveMiscButton.setText(Constants.SAVE_AND_CLOSE);
            devicesTabController.saveDeviceButton.setText(Constants.SAVE_AND_CLOSE);
        }

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
        LinkedHashMap<Integer, LEDCoordinate> ledFullScreenMatrix = ledCoordinate.initFullScreenLedMatrix(Integer.parseInt(modeTabController.screenWidth.getText()),
                Integer.parseInt(modeTabController.screenHeight.getText()), Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(rightLed.getText()),
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()),
                Integer.parseInt(bottomRowLed.getText()), splitBottomRow.isSelected());
        LinkedHashMap<Integer, LEDCoordinate> ledLetterboxMatrix = ledCoordinate.initLetterboxLedMatrix(Integer.parseInt(modeTabController.screenWidth.getText()),
                Integer.parseInt(modeTabController.screenHeight.getText()), Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(rightLed.getText()),
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()),
                Integer.parseInt(bottomRowLed.getText()), splitBottomRow.isSelected());
        LinkedHashMap<Integer, LEDCoordinate> fitToScreenMatrix = ledCoordinate.initPillarboxMatrix(Integer.parseInt(modeTabController.screenWidth.getText()),
                Integer.parseInt(modeTabController.screenHeight.getText()), Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(rightLed.getText()),
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()),
                Integer.parseInt(bottomRowLed.getText()), splitBottomRow.isSelected());
        try {
            Configuration config = new Configuration(ledFullScreenMatrix, ledLetterboxMatrix, fitToScreenMatrix);
            config.setNumberOfCPUThreads(Integer.parseInt(modeTabController.numberOfThreads.getText()));
            setCaptureMethod(config);
            config.setConfigVersion(FireflyLuciferin.version);
            config.setSerialPort(modeTabController.serialPort.getValue());
            config.setScreenResX(Integer.parseInt(modeTabController.screenWidth.getText()));
            config.setScreenResY(Integer.parseInt(modeTabController.screenHeight.getText()));
            config.setLedStartOffset(Integer.parseInt(ledStartOffset.getText()));
            config.setOsScaling(Integer.parseInt((modeTabController.scaling.getValue()).replace(Constants.PERCENT,"")));
            config.setGamma(Double.parseDouble(miscTabController.gamma.getValue()));
            config.setWhiteTemperature(miscTabController.whiteTemperature.getSelectionModel().getSelectedIndex() + 1);
            config.setSerialPort(modeTabController.serialPort.getValue());
            config.setDefaultLedMatrix(modeTabController.aspectRatio.getValue().equals(Constants.AUTO_DETECT_BLACK_BARS) ?
                    Constants.AspectRatio.FULLSCREEN.getAspectRatio() : modeTabController.aspectRatio.getValue());
            config.setAutoDetectBlackBars(modeTabController.aspectRatio.getValue().equals(Constants.AUTO_DETECT_BLACK_BARS));
            switch (devicesTabController.multiMonitor.getValue()) {
                case Constants.MULTIMONITOR_2 -> config.setMultiMonitor(2);
                case Constants.MULTIMONITOR_3 -> config.setMultiMonitor(3);
                default -> config.setMultiMonitor(1);
            }
            config.setMonitorNumber(modeTabController.monitorNumber.getValue());
            config.setDesiredFramerate(miscTabController.framerate.getValue().equals(Constants.UNLOCKED) ?
                    miscTabController.framerate.getValue() : miscTabController.framerate.getValue().split(" ")[0]);
            config.setMqttServer(mqttTabController.mqttHost.getText() + ":" + mqttTabController.mqttPort.getText());
            config.setMqttTopic(mqttTabController.mqttTopic.getText());
            config.setMqttUsername(mqttTabController.mqttUser.getText());
            config.setMqttPwd(mqttTabController.mqttPwd.getText());
            config.setMqttEnable(mqttTabController.mqttEnable.isSelected());
            config.setEyeCare(miscTabController.eyeCare.isSelected());
            config.setMqttStream(mqttTabController.mqttStream.isSelected());
            config.setCheckForUpdates(devicesTabController.checkForUpdates.isSelected());
            config.setSyncCheck(devicesTabController.syncCheck.isSelected());
            config.setToggleLed(miscTabController.toggleLed.isSelected());
            config.setNightModeFrom(miscTabController.nightModeFrom.getValue());
            config.setNightModeTo(miscTabController.nightModeTo.getValue());
            config.setNightModeBrightness(miscTabController.nightModeBrightness.getValue());
            config.setBrightness((int) (miscTabController.brightness.getValue()/100 *255));
            config.setAudioChannels(miscTabController.audioChannels.getValue());
            config.setAudioLoopbackGain((float) miscTabController.audioGain.getValue());
            config.setAudioDevice(miscTabController.audioDevice.getValue());
            config.setEffect(miscTabController.effect.getValue());
            config.setColorChooser((int)(miscTabController.colorPicker.getValue().getRed()*255) + "," + (int)(miscTabController.colorPicker.getValue().getGreen()*255) + ","
                    + (int)(miscTabController.colorPicker.getValue().getBlue()*255) + "," + (int)(miscTabController.colorPicker.getValue().getOpacity()*255));
            if (JavaFXStarter.whoAmI != 1) {
                Configuration mainConfig = sm.readConfig(true);
                mainConfig.setGamma(config.getGamma());
                mainConfig.setWhiteTemperature(config.getWhiteTemperature());
                mainConfig.setCheckForUpdates(devicesTabController.checkForUpdates.isSelected());
                mainConfig.setSyncCheck(devicesTabController.syncCheck.isSelected());
                setConfig(config, mainConfig);
                sm.writeConfig(mainConfig, Constants.CONFIG_FILENAME);
            }
            if (config.getMultiMonitor() > 1) {
                switch (JavaFXStarter.whoAmI) {
                    case 1:
                        writeOtherConfig(config, Constants.CONFIG_FILENAME_2);
                        if (config.getMultiMonitor() == 3) writeOtherConfig(config, Constants.CONFIG_FILENAME_3);
                        break;
                    case 2:
                        if (config.getMultiMonitor() == 3) writeOtherConfig(config, Constants.CONFIG_FILENAME_3);
                        break;
                    case 3:
                        writeOtherConfig(config, Constants.CONFIG_FILENAME_2);
                        break;
                }
            }
            config.setTopLed(Integer.parseInt(topLed.getText()));
            config.setLeftLed(Integer.parseInt(leftLed.getText()));
            config.setRightLed(Integer.parseInt(rightLed.getText()));
            config.setBottomLeftLed(Integer.parseInt(bottomLeftLed.getText()));
            config.setBottomRightLed(Integer.parseInt(bottomRightLed.getText()));
            config.setBottomRowLed(Integer.parseInt(bottomRowLed.getText()));
            config.setOrientation(orientation.getValue());
            config.setBaudRate(modeTabController.baudRate.getValue());
            config.setSplitBottomRow(splitBottomRow.isSelected());
            sm.writeConfig(config, null);
            boolean firstStartup = FireflyLuciferin.config == null;
            FireflyLuciferin.config = config;
            if (firstStartup || (JavaFXStarter.whoAmI == 1 && ((config.getMultiMonitor() == 2 && !sm.checkIfFileExist(Constants.CONFIG_FILENAME_2))
                    || (config.getMultiMonitor() == 3 && (!sm.checkIfFileExist(Constants.CONFIG_FILENAME_2) || !sm.checkIfFileExist(Constants.CONFIG_FILENAME_3)))) ) ) {
                writeOtherConfigNew(config);
                cancel(e);
            }
            if (!firstStartup) {
                String oldBaudrate = currentConfig.getBaudRate();
                boolean isBaudRateChanged = !modeTabController.baudRate.getValue().equals(currentConfig.getBaudRate());
                boolean isMqttTopicChanged = (!mqttTabController.mqttTopic.getText().equals(currentConfig.getMqttTopic()) && config.isMqttEnable());
                if (isBaudRateChanged || isMqttTopicChanged) {
                    programFirmware(config, e, oldBaudrate, mqttTabController.mqttTopic.getText(), isBaudRateChanged, isMqttTopicChanged);
                } else {
                    exit(e);
                }
            }
        } catch (IOException | CloneNotSupportedException ioException) {
            log.error("Can't write config file.");
        }

    }

    /**
     * Set capture method, write preferences to Windows Registry
     * @param config preferences
     */
    void setCaptureMethod(Configuration config) {

        NativeExecutor nativeExecutor = new NativeExecutor();
        if (NativeExecutor.isWindows()) {
            switch (modeTabController.captureMethod.getValue()) {
                case DDUPL -> config.setCaptureMethod(Configuration.CaptureMethod.DDUPL.name());
                case WinAPI -> config.setCaptureMethod(Configuration.CaptureMethod.WinAPI.name());
                case CPU -> config.setCaptureMethod(Configuration.CaptureMethod.CPU.name());
            }
            if (miscTabController.startWithSystem.isSelected()) {
                nativeExecutor.writeRegistryKey();
            } else {
                nativeExecutor.deleteRegistryKey();
            }
            config.setStartWithSystem(miscTabController.startWithSystem.isSelected());
        } else if (NativeExecutor.isMac()) {
            if (modeTabController.captureMethod.getValue() == Configuration.CaptureMethod.AVFVIDEOSRC) {
                config.setCaptureMethod(Configuration.CaptureMethod.AVFVIDEOSRC.name());
            }
        } else {
            if (modeTabController.captureMethod.getValue() == Configuration.CaptureMethod.XIMAGESRC) {
                config.setCaptureMethod(Configuration.CaptureMethod.XIMAGESRC.name());
            }
        }

    }

    /**
     * Program firmware, set baud rate and mqtt topic on all instances
     * @param config             configuration on file
     * @param e                  event that launched
     * @param oldBaudrate        baud rate before the change
     * @param mqttTopic          swap microcontroller mqtt topic
     * @param isBaudRateChanged  condition that monitor if baudrate is changed
     * @param isMqttTopicChanged condition that monitor if mqtt topipc is changed
     */
    void programFirmware(Configuration config, InputEvent e, String oldBaudrate, String mqttTopic, boolean isBaudRateChanged, boolean isMqttTopicChanged) throws IOException {

        FirmwareConfigDto firmwareConfigDto = new FirmwareConfigDto();
        if (currentConfig.isMqttEnable()) {
            if (DevicesTabController.deviceTableData != null && DevicesTabController.deviceTableData.size() > 0) {
                if (Constants.SERIAL_PORT_AUTO.equals(modeTabController.serialPort.getValue())) {
                    firmwareConfigDto.setMAC(DevicesTabController.deviceTableData.get(0).getMac());
                }
                DevicesTabController.deviceTableData.forEach(glowWormDevice -> {
                    if (glowWormDevice.getDeviceName().equals(modeTabController.serialPort.getValue()) || glowWormDevice.getDeviceIP().equals(modeTabController.serialPort.getValue())) {
                        firmwareConfigDto.setMAC(glowWormDevice.getMac());
                    }
                });
            }
            if (firmwareConfigDto.getMAC() == null || firmwareConfigDto.getMAC().isEmpty()) {
                log.error("No device can be programed");
            }
        }
        if (isBaudRateChanged) {
            Optional<ButtonType> result = FireflyLuciferin.guiManager.showAlert(Constants.BAUDRATE_TITLE, Constants.BAUDRATE_HEADER,
                    Constants.BAUDRATE_CONTEXT, Alert.AlertType.CONFIRMATION);
            ButtonType button = result.orElse(ButtonType.OK);
            if (button == ButtonType.OK) {
                if (currentConfig.isMqttEnable()) {
                    firmwareConfigDto.setBaudrate(String.valueOf(Constants.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + modeTabController.baudRate.getValue()).ordinal() + 1));
                    if (isMqttTopicChanged) {
                        firmwareConfigDto.setMqttopic(mqttTopic);
                    }
                    MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FIRMWARE_CONFIG), CommonUtility.writeValueAsString(firmwareConfigDto));
                } else {
                    FireflyLuciferin.baudRate = Constants.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + modeTabController.baudRate.getValue()).ordinal() + 1;
                    miscTabController.sendSerialParams();
                }
                exit(e);
            } else if (button == ButtonType.CANCEL) {
                config.setBaudRate(oldBaudrate);
                modeTabController.baudRate.setValue(oldBaudrate);
                sm.writeConfig(config, null);
            }
        } else if (isMqttTopicChanged && currentConfig.isMqttEnable()) {
            firmwareConfigDto.setMqttopic(mqttTopic);
            MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FIRMWARE_CONFIG), CommonUtility.writeValueAsString(firmwareConfigDto));
            exit(e);
        }

    }

    /**
     * Write other config files if there are more than one instance running
     * @param config configuration
     */
    void writeOtherConfigNew(Configuration config) throws IOException, CloneNotSupportedException {

        if (config.getMultiMonitor() == 2 || config.getMultiMonitor() == 3) {
            writeSingleConfig(config, Constants.CONFIG_FILENAME_2, 22, 1);
        }
        if (config.getMultiMonitor() == 3) {
            writeSingleConfig(config, Constants.CONFIG_FILENAME_3, 23, 2);
        }

    }

    /**
     * Write other config files if there are more than one instance running
     * @param config              configuration
     * @param otherConfigFilename file to write
     */
    void writeOtherConfig(Configuration config, String otherConfigFilename) throws IOException, CloneNotSupportedException {

        Configuration otherConfig = sm.readConfig(otherConfigFilename);
        if (otherConfig != null) {
            otherConfig.setCheckForUpdates(devicesTabController.checkForUpdates.isSelected());
            otherConfig.setSyncCheck(devicesTabController.syncCheck.isSelected());
            otherConfig.setGamma(config.getGamma());
            otherConfig.setWhiteTemperature(config.getWhiteTemperature());
            setConfig(config, otherConfig);
            sm.writeConfig(otherConfig, otherConfigFilename);
        }

    }

    /**
     * Set configuration
     * @param config       config
     * @param otherConfig  otherConfig
     */
    private void setConfig(Configuration config, Configuration otherConfig) {

        otherConfig.setMultiMonitor(config.getMultiMonitor());
        otherConfig.setToggleLed(config.isToggleLed());
        otherConfig.setColorChooser(config.getColorChooser());
        otherConfig.setEyeCare(config.isEyeCare());
        otherConfig.setNightModeFrom(config.getNightModeFrom());
        otherConfig.setNightModeTo(config.getNightModeTo());
        otherConfig.setNightModeBrightness(config.getNightModeBrightness());
        otherConfig.setAudioDevice(config.getAudioDevice());
        otherConfig.setAudioChannels(config.getAudioChannels());
        otherConfig.setAudioLoopbackGain(config.getAudioLoopbackGain());
        otherConfig.setBrightness(config.getBrightness());
        otherConfig.setEffect(config.getEffect());
        if (NativeExecutor.isWindows()) {
            otherConfig.setStartWithSystem(miscTabController.startWithSystem.isSelected());
        }

    }

    /**
     * Write a config file for an instance
     * @param config     configuration
     * @param filename   filename to write
     * @param comPort    comport to use as defaults
     * @param monitorNum monitor number, it's relative to the instance number
     * @throws CloneNotSupportedException file exception
     * @throws IOException                file exception
     */
    void writeSingleConfig(Configuration config, String filename, int comPort, int monitorNum) throws CloneNotSupportedException, IOException {

        Configuration tempConfiguration = (Configuration) config.clone();
        tempConfiguration.setSerialPort(Constants.SERIAL_PORT_COM + comPort);
        DisplayInfo screenInfo = displayManager.getDisplayList().get(monitorNum);
        double scaleX = screenInfo.getScaleX();
        double scaleY = screenInfo.getScaleY();
        tempConfiguration.setScreenResX((int) (screenInfo.width * scaleX));
        tempConfiguration.setScreenResY((int) (screenInfo.height * scaleY));
        tempConfiguration.setOsScaling((int) (screenInfo.getScaleX() * 100));
        tempConfiguration.setMonitorNumber(screenInfo.getFxDisplayNumber());
        sm.writeConfig(tempConfiguration, filename);

    }

    /**
     * Initilize output device chooser
     * @param initCaptureMethod re-init capture method
     */
    public void initOutputDeviceChooser(boolean initCaptureMethod) {

        if (!mqttTabController.mqttStream.isSelected()) {
            String deviceInUse = modeTabController.serialPort.getValue();
            modeTabController.comWirelessLabel.setText(Constants.OUTPUT_DEVICE);
            modeTabController.serialPort.getItems().clear();
            modeTabController.serialPort.getItems().add(Constants.SERIAL_PORT_AUTO);
            if (initCaptureMethod) {
                modeTabController.captureMethod.getItems().clear();
                if (NativeExecutor.isWindows()) {
                    modeTabController.captureMethod.getItems().addAll(Configuration.CaptureMethod.DDUPL, Configuration.CaptureMethod.WinAPI, Configuration.CaptureMethod.CPU);
                } else if (NativeExecutor.isMac()) {
                    modeTabController.captureMethod.getItems().addAll(Configuration.CaptureMethod.AVFVIDEOSRC);
                }
            }
            if (NativeExecutor.isWindows()) {
                for (int i=0; i<=256; i++) {
                    modeTabController.serialPort.getItems().add(Constants.SERIAL_PORT_COM + i);
                }
            } else {
                for (int i=0; i<=256; i++) {
                    modeTabController.serialPort.getItems().add(Constants.SERIAL_PORT_TTY + i);
                }
            }
            modeTabController.serialPort.setValue(deviceInUse);
        } else {
            modeTabController.comWirelessLabel.setText(Constants.OUTPUT_DEVICE);
            if (!modeTabController.serialPort.isFocused()) {
                String deviceInUse = modeTabController.serialPort.getValue();
                modeTabController.serialPort.getItems().clear();
                DevicesTabController.deviceTableData.forEach(glowWormDevice -> modeTabController.serialPort.getItems().add(glowWormDevice.getDeviceName()));
                modeTabController.serialPort.setValue(deviceInUse);
            }
        }

    }

    /**
     * Save and Exit button event
     * @param event event
     */
    @FXML
    public void exit(InputEvent event) {

        cancel(event);
        NativeExecutor.restartNativeInstance();

    }

    /**
     * Cancel button event
     */
    @FXML
    public void cancel(InputEvent event) {

        if (event != null) {
            animationTimer.stop();
            final Node source = (Node) event.getSource();
            final Stage stage = (Stage) source.getScene().getWindow();
            stage.hide();
        }

    }

    /**
     * Open browser to the GitHub project page
     * @param link GitHub
     */
    @FXML
    @SuppressWarnings("unused")
    public void onMouseClickedGitHubLink(ActionEvent link) {

        FireflyLuciferin.guiManager.surfToGitHub();

    }

    /**
     * Start and stop capturing
     * @param e InputEvent
     */
    @FXML
    @SuppressWarnings("unused")
    public void onMouseClickedPlay(InputEvent e) {

        controlImage = setImage(Constants.PlayerStatus.GREY);
        if (!FireflyLuciferin.communicationError) {
            if (FireflyLuciferin.RUNNING) {
                controlImage = setImage(Constants.PlayerStatus.STOP);
            } else {
                controlImage = setImage(Constants.PlayerStatus.PLAY_WAITING);
            }
            setButtonImage();
            if (FireflyLuciferin.RUNNING) {
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
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
     */
    void turnOffLEDs(Configuration currentConfig) {

        if (currentConfig != null) {
            if (FireflyLuciferin.RUNNING) {
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
            }
            CommonUtility.sleepMilliseconds(100);
            if (currentConfig.isMqttEnable()) {
                StateDto stateDto = new StateDto();
                stateDto.setState(Constants.OFF);
                stateDto.setEffect(Constants.SOLID);
                stateDto.setBrightness(CommonUtility.getNightBrightness());
                stateDto.setWhitetemp(FireflyLuciferin.config.getWhiteTemperature());
                MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.writeValueAsString(stateDto));
            } else {
                java.awt.Color[] leds = new java.awt.Color[1];
                try {
                    leds[0] = new java.awt.Color(0, 0, 0);
                    FireflyLuciferin.config.setBrightness(0);
                    FireflyLuciferin.sendColorsViaUSB(leds);
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
    void setTooltips() {

        topLed.setTooltip(createTooltip(Constants.TOOLTIP_TOPLED));
        leftLed.setTooltip(createTooltip(Constants.TOOLTIP_LEFTLED));
        rightLed.setTooltip(createTooltip(Constants.TOOLTIP_RIGHTLED));
        bottomLeftLed.setTooltip(createTooltip(Constants.TOOLTIP_BOTTOMLEFTLED));
        bottomRightLed.setTooltip(createTooltip(Constants.TOOLTIP_BOTTOMRIGHTLED));
        bottomRowLed.setTooltip(createTooltip(Constants.TOOLTIP_BOTTOMROWLED));
        orientation.setTooltip(createTooltip(Constants.TOOLTIP_ORIENTATION));
        ledStartOffset.setTooltip(createTooltip(Constants.TOOLTIP_LEDSTARTOFFSET));
        splitBottomRow.setTooltip(createTooltip(Constants.TOOLTIP_SPLIT_BOTTOM_ROW));
        mqttTabController.setTooltips(currentConfig);
        devicesTabController.setTooltips(currentConfig);
        modeTabController.setTooltips(currentConfig);
        miscTabController.setTooltips(currentConfig);
        if (currentConfig == null) {
            if (!NativeExecutor.isWindows()) {
                playButton.setTooltip(createTooltip(Constants.TOOLTIP_PLAYBUTTON_NULL, 50, 6000));
            }
            saveLedButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVELEDBUTTON_NULL));
        } else {
            if (!NativeExecutor.isWindows()) {
                playButton.setTooltip(createTooltip(Constants.TOOLTIP_PLAYBUTTON, 200, 6000));
            }
            saveLedButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVELEDBUTTON,200, 6000));
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
     * @param text      tooltip string
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

        addTextFieldListener(ledStartOffset);
        addTextFieldListener(topLed);
        addTextFieldListener(leftLed);
        addTextFieldListener(rightLed);
        addTextFieldListener(bottomLeftLed);
        addTextFieldListener(bottomRightLed);
        addTextFieldListener(bottomRowLed);
        mqttTabController.setNumericTextField();
        modeTabController.setNumericTextField();

    }

    /**
     * Set and return LED tab image
     * @param playerStatus PLAY, STOP, GREY
     * @return tray icon
     */
    @SuppressWarnings("ConstantConditions")
    Image setImage(Constants.PlayerStatus playerStatus) {

        String imgPath = "";
        if (currentConfig == null) {
            imgPath = Constants.IMAGE_CONTROL_PLAY;
        } else {
            switch (playerStatus) {
                case PLAY:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_RIGHT;
                            }
                            break;
                        case 2:
                            if ((currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_PLAY_LEFT;
                            break;
                    }
                    break;
                case PLAY_WAITING:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT;
                            }
                            break;
                        case 2:
                            if ((currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT;
                            break;
                    }
                    break;
                case STOP:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_LOGO;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_LOGO_RIGHT;
                            }
                            break;
                        case 2:
                            if ((currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_LOGO_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_LOGO_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_LOGO_LEFT;
                            break;
                    }
                    break;
                case GREY:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_GREY;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_GREY_RIGHT;
                            }
                            break;
                        case 2:
                            if ((currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_GREY_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_GREY_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_GREY_LEFT;
                            break;
                    }
                    break;
            }
        }
        return new Image(this.getClass().getResource(imgPath).toString(), true);

    }

    /**
     * Send serial params
     */
    public void sendSerialParams() {

        miscTabController.sendSerialParams();

    }


    /**
     * Set button image
     */
    private void setButtonImage() {

        imageView = new ImageView(controlImage);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);
        playButton.setGraphic(imageView);

    }

    /**
     * Return the observable devices list
     * @return devices list
     */
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
