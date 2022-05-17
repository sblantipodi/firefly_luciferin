/*
  SettingsController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini  (https://github.com/sblantipodi)

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

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
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
import org.dpsoftware.managers.SerialManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.managers.dto.LedMatrixInfo;
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

    // Inject children tab controllers
    @FXML private MqttTabController mqttTabController;
    @FXML private DevicesTabController devicesTabController;
    @FXML private ModeTabController modeTabController;
    @FXML private MiscTabController miscTabController;
    @FXML private LedsConfigTabController ledsConfigTabController;
    @FXML private ControlTabController controlTabController;
    // FXML binding
    @FXML public TabPane mainTabPane;
    Configuration currentConfig;
    StorageManager sm;
    DisplayManager displayManager;
    Configuration tempConf;


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
        ledsConfigTabController.injectSettingsController(this);
        controlTabController.injectSettingsController(this);

        Platform.setImplicitExit(false);
        sm = new StorageManager();
        displayManager = new DisplayManager();
        displayManager.logDisplayInfo();
        for (int i=0; i < displayManager.displayNumber(); i++) {
            modeTabController.monitorNumber.getItems().add(displayManager.getDisplayName(i));
            switch (i) {
                case 0 -> devicesTabController.multiMonitor.getItems().add(CommonUtility.getWord(Constants.MULTIMONITOR_1));
                case 1 -> devicesTabController.multiMonitor.getItems().add(CommonUtility.getWord(Constants.MULTIMONITOR_2));
                case 2 -> devicesTabController.multiMonitor.getItems().add(CommonUtility.getWord(Constants.MULTIMONITOR_3));
            }
        }
        currentConfig = sm.readConfig(false);
        ledsConfigTabController.showTestImageButton.setVisible(currentConfig != null);

        initComboBox();
        if (NativeExecutor.isWindows()) {
            mainTabPane.getTabs().remove(0);
        }
        if (currentConfig != null && CommonUtility.isSingleDeviceMultiScreen()) {
            if (JavaFXStarter.whoAmI > 1) {
                if (NativeExecutor.isLinux()) {
                    mainTabPane.getTabs().remove(3, 6);
                } else if (NativeExecutor.isWindows()) {
                    mainTabPane.getTabs().remove(2, 5);
                }
            }
        }
        // Init default values
        initDefaultValues();
        // Init tooltips
        setTooltips();
        // Force numeric fields
        setNumericTextField();
        runLater();
        initListeners();
        controlTabController.startAnimationTimer();
    }

    /**
     * Init combo boxes
     */
    void initComboBox() {
        modeTabController.initComboBox();
        miscTabController.initComboBox();
        devicesTabController.initComboBox();
        mqttTabController.initComboBox();
    }

    /**
     * Init form values
     */
    void initDefaultValues() {
        initOutputDeviceChooser(true);
        if (currentConfig == null) {
            mqttTabController.initDefaultValues();
            devicesTabController.initDefaultValues();
            modeTabController.initDefaultValues();
            miscTabController.initDefaultValues();
            ledsConfigTabController.initDefaultValues();
        } else {
            initValuesFromSettingsFile();
        }
    }

    /**
     * Init form values by reading existing config file
     */
    private void initValuesFromSettingsFile() {
        mqttTabController.initValuesFromSettingsFile(currentConfig);
        devicesTabController.initValuesFromSettingsFile(currentConfig);
        modeTabController.initValuesFromSettingsFile(currentConfig);
        miscTabController.initValuesFromSettingsFile(currentConfig);
        ledsConfigTabController.initValuesFromSettingsFile(currentConfig);
        controlTabController.initValuesFromSettingsFile();
        ledsConfigTabController.splitBottomRow();
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
                        controlTabController.animationTimer.stop();
                    }
                });
            }
            devicesTabController.setTableEdit();
            ledsConfigTabController.orientation.requestFocus();
        });
    }

    /**
     * Init all the settings listener
     */
    private void initListeners() {
        setSerialPortAvailableCombo();
        mqttTabController.initListeners();
        modeTabController.initListeners();
        miscTabController.initListeners(currentConfig);
        ledsConfigTabController.initListeners();
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
        SerialManager serialManager = new SerialManager();
        Map<String, Boolean> availableDevices = serialManager.getAvailableDevices();
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
    public void manageDeviceList() {
        devicesTabController.manageDeviceList();
        if (!devicesTabController.cellEdit) {
            if (mqttTabController.mqttStream.isSelected()) {
                initOutputDeviceChooser(true);
            }
        }
    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        try {
            tempConf = (Configuration) FireflyLuciferin.config.clone();
        } catch (CloneNotSupportedException ex) {
            log.debug(ex.getMessage());
        }
        save(e, null);
    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e, String profileName) {
        // No config found, init with a default config

        LEDCoordinate ledCoordinate = new LEDCoordinate();
        LedMatrixInfo ledMatrixInfo = new LedMatrixInfo(Integer.parseInt(modeTabController.screenWidth.getText()),
                Integer.parseInt(modeTabController.screenHeight.getText()), Integer.parseInt(ledsConfigTabController.bottomRightLed.getText()), Integer.parseInt(ledsConfigTabController.rightLed.getText()),
                Integer.parseInt(ledsConfigTabController.topLed.getText()), Integer.parseInt(ledsConfigTabController.leftLed.getText()), Integer.parseInt(ledsConfigTabController.bottomLeftLed.getText()),
                Integer.parseInt(ledsConfigTabController.bottomRowLed.getText()), ledsConfigTabController.splitBottomMargin.getValue(), ledsConfigTabController.grabberAreaTopBottom.getValue(),
                ledsConfigTabController.grabberSide.getValue(), ledsConfigTabController.gapTypeTopBottom.getValue(), ledsConfigTabController.gapTypeSide.getValue(), ledsConfigTabController.groupBy.getValue());
        try {
            LedMatrixInfo ledMatrixInfoFullScreen = (LedMatrixInfo) ledMatrixInfo.clone();
            LinkedHashMap<Integer, LEDCoordinate> ledFullScreenMatrix = ledCoordinate.initFullScreenLedMatrix(ledMatrixInfoFullScreen);
            LedMatrixInfo ledMatrixInfoLetterbox = (LedMatrixInfo) ledMatrixInfo.clone();
            LinkedHashMap<Integer, LEDCoordinate> ledLetterboxMatrix = ledCoordinate.initLetterboxLedMatrix(ledMatrixInfoLetterbox);
            LedMatrixInfo ledMatrixInfoPillarbox = (LedMatrixInfo) ledMatrixInfo.clone();
            LinkedHashMap<Integer, LEDCoordinate> fitToScreenMatrix = ledCoordinate.initPillarboxMatrix(ledMatrixInfoPillarbox);
            Configuration config = new Configuration(ledFullScreenMatrix, ledLetterboxMatrix, fitToScreenMatrix);
            ledsConfigTabController.save(config);
            modeTabController.save(config);
            miscTabController.save(config);
            mqttTabController.save(config);
            devicesTabController.save(config);
            setCaptureMethod(config);
            config.setConfigVersion(FireflyLuciferin.version);
            boolean firstStartup = FireflyLuciferin.config == null;
            if (config.isWifiEnable() && !config.isMqttEnable() && firstStartup) {
                config.setSerialPort(Constants.SERIAL_PORT_AUTO);
            }
            if (firstStartup) {
                if (config.isWifiEnable()) {
                    config.setBaudRate(Constants.BaudRate.BAUD_RATE_115200.getBaudRate());
                } else {
                    config.setBaudRate(Constants.BaudRate.BAUD_RATE_500000.getBaudRate());
                }
            }
            if (profileName == null) {
                writeDefaultConfig(e, config, firstStartup);
            } else {
                sm.writeConfig(config, profileName);
            }
        } catch (IOException | CloneNotSupportedException ioException) {
            log.error("Can't write config file.");
        }
    }

    /**
     * Write default config
     * @param e event that triggered the save event
     * @param config config to save
     * @param firstStartup check if config exist
     * @throws IOException can't write
     * @throws CloneNotSupportedException can't clone
     */
    private void writeDefaultConfig(InputEvent e, Configuration config, boolean firstStartup) throws IOException, CloneNotSupportedException {
        // Manage settings from one instance to the other, for multi monitor setup
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
        Configuration defaultConfig = sm.readConfig(false);
        sm.writeConfig(config, null);
        FireflyLuciferin.config = config;
        sm.setProfileDifferences(defaultConfig, FireflyLuciferin.config);
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
            } else if (sm.restartNeeded) {
                Optional<ButtonType> result = FireflyLuciferin.guiManager.showLocalizedAlert(Constants.BAUDRATE_TITLE, Constants.BAUDRATE_HEADER,
                        Constants.BAUDRATE_CONTEXT, Alert.AlertType.CONFIRMATION);
                ButtonType button = result.orElse(ButtonType.OK);
                if (button == ButtonType.OK) {
                    exit(e);
                } else {
                    sm.restartNeeded = false;
                    FireflyLuciferin.config = tempConf;
                }
            }
        }
        refreshValuesOnScene();
    }

    /**
     * Refresh all the values displayed on the scene after save or profiles switch
     */
    public void refreshValuesOnScene() {
        mqttTabController.initValuesFromSettingsFile(FireflyLuciferin.config);
        devicesTabController.initValuesFromSettingsFile(FireflyLuciferin.config);
        miscTabController.initValuesFromSettingsFile(FireflyLuciferin.config, false);
        ledsConfigTabController.initValuesFromSettingsFile(FireflyLuciferin.config);
        controlTabController.initValuesFromSettingsFile();
        ledsConfigTabController.splitBottomMargin.setValue(FireflyLuciferin.config.getSplitBottomMargin());
        ledsConfigTabController.splitBottomRow();
        miscTabController.setContextMenu();
        FireflyLuciferin.setLedNumber(FireflyLuciferin.config.getDefaultLedMatrix());
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
        if (currentConfig.isWifiEnable()) {
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
                log.error("No device can be programmed");
            }
        }
        if (isBaudRateChanged) {
            Optional<ButtonType> result = FireflyLuciferin.guiManager.showLocalizedAlert(Constants.BAUDRATE_TITLE, Constants.BAUDRATE_HEADER,
                    Constants.BAUDRATE_CONTEXT, Alert.AlertType.CONFIRMATION);
            ButtonType button = result.orElse(ButtonType.OK);
            if (button == ButtonType.OK) {
                if (currentConfig.isWifiEnable()) {
                    firmwareConfigDto.setBaudrate(String.valueOf(Constants.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + modeTabController.baudRate.getValue()).getBaudRateValue()));
                    if (isMqttTopicChanged) {
                        firmwareConfigDto.setMqttopic(mqttTopic);
                    }
                    MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FIRMWARE_CONFIG), CommonUtility.toJsonString(firmwareConfigDto));
                } else {
                    FireflyLuciferin.baudRate = Constants.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + modeTabController.baudRate.getValue()).getBaudRateValue();
                    SerialManager serialManager = new SerialManager();
                    serialManager.sendSerialParams((int)(miscTabController.colorPicker.getValue().getRed() * 255),
                            (int)(miscTabController.colorPicker.getValue().getGreen() * 255),
                            (int)(miscTabController.colorPicker.getValue().getBlue() * 255));
                }
                exit(e);
            } else if (button == ButtonType.CANCEL) {
                config.setBaudRate(oldBaudrate);
                modeTabController.baudRate.setValue(oldBaudrate);
                sm.writeConfig(config, null);
            }
        } else if (isMqttTopicChanged && currentConfig.isMqttEnable()) {
            firmwareConfigDto.setMqttopic(mqttTopic);
            MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FIRMWARE_CONFIG), CommonUtility.toJsonString(firmwareConfigDto));
            exit(e);
        }
    }

    /**
     * Write other config files if there are more than one instance running
     * @param config configuration
     */
    void writeOtherConfigNew(Configuration config) throws IOException, CloneNotSupportedException {
        if (config.getMultiMonitor() == 2 || config.getMultiMonitor() == 3) {
            writeSingleConfigNew(config, Constants.CONFIG_FILENAME_2, 22, 1);
        }
        if (config.getMultiMonitor() == 3) {
            writeSingleConfigNew(config, Constants.CONFIG_FILENAME_3, 23, 2);
        }
    }

    /**
     * Write other config files if there are more than one instance running
     * @param config              configuration
     * @param otherConfigFilename file to write
     */
    void writeOtherConfig(Configuration config, String otherConfigFilename) throws IOException {
        Configuration otherConfig = sm.readConfig(otherConfigFilename);
        if (otherConfig != null) {
            otherConfig.setCheckForUpdates(devicesTabController.checkForUpdates.isSelected());
            otherConfig.setSyncCheck(devicesTabController.syncCheck.isSelected());
            otherConfig.setLanguage(currentConfig.getLanguage());
            otherConfig.setTheme(config.getTheme());
            otherConfig.setLanguage(config.getLanguage());
            if (CommonUtility.isSingleDeviceMultiScreen()) {
                otherConfig.setGamma(config.getGamma());
                otherConfig.setDesiredFramerate(config.getDesiredFramerate());
                otherConfig.setWhiteTemperature(config.getWhiteTemperature());
            }
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
        otherConfig.setMultiScreenSingleDevice(config.isMultiScreenSingleDevice());
        otherConfig.setEyeCare(config.isEyeCare());
        otherConfig.setNightModeFrom(config.getNightModeFrom());
        otherConfig.setNightModeTo(config.getNightModeTo());
        otherConfig.setNightModeBrightness(config.getNightModeBrightness());
        otherConfig.setAudioDevice(config.getAudioDevice());
        otherConfig.setAudioChannels(config.getAudioChannels());
        otherConfig.setAudioLoopbackGain(config.getAudioLoopbackGain());
        if (NativeExecutor.isWindows()) {
            otherConfig.setStartWithSystem(miscTabController.startWithSystem.isSelected());
        }
        if (config.isMultiScreenSingleDevice() && config.getMultiMonitor() > 1) {
            otherConfig.setSerialPort(config.getSerialPort());
            otherConfig.setBaudRate(config.getBaudRate());
            otherConfig.setMqttServer(config.getMqttServer());
            otherConfig.setMqttTopic(config.getMqttTopic());
            otherConfig.setMqttUsername(config.getMqttUsername());
            otherConfig.setMqttPwd(config.getMqttPwd());
            otherConfig.setMqttStream(config.isMqttStream());
            otherConfig.setBrightness(config.getBrightness());
            otherConfig.setEffect(config.getEffect());
            otherConfig.setColorChooser(config.getColorChooser());
            otherConfig.setToggleLed(config.isToggleLed());
        }
        otherConfig.setCheckForUpdates(config.isCheckForUpdates());
        otherConfig.setSyncCheck(config.isSyncCheck());
    }

    /**
     * Write a config file for an instance
     * @param config     configuration
     * @param filename   filename to write
     * @param comPort    comport to use as defaults
     * @param monitorNum monitor number, it's relative to the instance number
     * @throws CloneNotSupportedException file exception
     * @throws IOException file exception
     */
    void writeSingleConfigNew(Configuration config, String filename, int comPort, int monitorNum) throws CloneNotSupportedException, IOException {
        Configuration tempConfiguration = (Configuration) config.clone();
        if (tempConfiguration.isWifiEnable() && !tempConfiguration.isMqttEnable() && tempConfiguration.getMultiMonitor() > 1) {
            tempConfiguration.setSerialPort(Constants.SERIAL_PORT_AUTO);
        } else {
            tempConfiguration.setSerialPort(Constants.SERIAL_PORT_COM + comPort);
        }
        DisplayInfo screenInfo = displayManager.getDisplayList().get(monitorNum);
        double scaleX = screenInfo.getScaleX();
        double scaleY = screenInfo.getScaleY();
        tempConfiguration.setMonitorNumber(monitorNum);
        tempConfiguration.setScreenResX((int) (screenInfo.width * scaleX));
        tempConfiguration.setScreenResY((int) (screenInfo.height * scaleY));
        tempConfiguration.setOsScaling((int) (screenInfo.getScaleX() * 100));
        config.getLedMatrix().clear();
        LEDCoordinate ledCoordinate = new LEDCoordinate();
        LedMatrixInfo ledMatrixInfo = new LedMatrixInfo(tempConfiguration.getScreenResX(),
                tempConfiguration.getScreenResY(), config.getBottomRightLed(), config.getRightLed(), config.getTopLed(), config.getLeftLed(),
                config.getBottomLeftLed(), config.getBottomRowLed(), config.getSplitBottomMargin(), ledsConfigTabController.grabberAreaTopBottom.getValue(), ledsConfigTabController.grabberSide.getValue(),
                ledsConfigTabController.gapTypeTopBottom.getValue(), ledsConfigTabController.gapTypeSide.getValue(), ledsConfigTabController.groupBy.getValue());
        LedMatrixInfo ledMatrixInfoFullScreen = (LedMatrixInfo) ledMatrixInfo.clone();
        config.getLedMatrix().put(Constants.AspectRatio.FULLSCREEN.getBaseI18n(), ledCoordinate.initFullScreenLedMatrix(ledMatrixInfoFullScreen));
        LedMatrixInfo ledMatrixInfoLetterbox = (LedMatrixInfo) ledMatrixInfo.clone();
        config.getLedMatrix().put(Constants.AspectRatio.LETTERBOX.getBaseI18n(), ledCoordinate.initFullScreenLedMatrix(ledMatrixInfoLetterbox));
        LedMatrixInfo ledMatrixInfoPillarbox = (LedMatrixInfo) ledMatrixInfo.clone();
        config.getLedMatrix().put(Constants.AspectRatio.PILLARBOX.getBaseI18n(), ledCoordinate.initFullScreenLedMatrix(ledMatrixInfoPillarbox));
        sm.writeConfig(tempConfiguration, filename);
    }

    /**
     * Initilize output device chooser
     * @param initCaptureMethod re-init capture method
     */
    public void initOutputDeviceChooser(boolean initCaptureMethod) {
        if (!mqttTabController.mqttStream.isSelected()) {
            String deviceInUse = modeTabController.serialPort.getValue();
            modeTabController.comWirelessLabel.setText(CommonUtility.getWord(Constants.OUTPUT_DEVICE));
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
                SerialManager serialManager = new SerialManager();
                Map<String, Boolean> availableDevices = serialManager.getAvailableDevices();
                availableDevices.forEach((portName, isAvailable) -> modeTabController.serialPort.getItems().add(portName));
            } else {
                for (int i=0; i<=256; i++) {
                    modeTabController.serialPort.getItems().add(Constants.SERIAL_PORT_TTY + i);
                }
            }
            modeTabController.serialPort.setValue(deviceInUse);
        } else {
            modeTabController.comWirelessLabel.setText(CommonUtility.getWord(Constants.OUTPUT_DEVICE));
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
            controlTabController.animationTimer.stop();
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
        FireflyLuciferin.guiManager.surfToURL(Constants.GITHUB_URL);
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
            if (currentConfig.isWifiEnable()) {
                StateDto stateDto = new StateDto();
                stateDto.setState(Constants.OFF);
                stateDto.setEffect(Constants.SOLID);
                stateDto.setBrightness(CommonUtility.getNightBrightness());
                stateDto.setWhitetemp(FireflyLuciferin.config.getWhiteTemperature());
                if (CommonUtility.getDeviceToUse() != null) {
                    stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
                }
                MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.toJsonString(stateDto));
            } else {
                java.awt.Color[] leds = new java.awt.Color[1];
                try {
                    leds[0] = new java.awt.Color(0, 0, 0);
                    FireflyLuciferin.config.setBrightness(CommonUtility.getNightBrightness());
                    SerialManager serialManager = new SerialManager();
                    serialManager.sendColorsViaUSB(leds);
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
            if (newValue.length() == 0) {
                textField.setText("0");
            } else {
                textField.setText(newValue.replaceAll("\\D", "").replaceFirst("^0+(?!$)", ""));
            }
        });
    }

    /**
     * Set form tooltips
     */
    void setTooltips() {
        mqttTabController.setTooltips(currentConfig);
        devicesTabController.setTooltips(currentConfig);
        modeTabController.setTooltips(currentConfig);
        miscTabController.setTooltips(currentConfig);
        ledsConfigTabController.setTooltips(currentConfig);
        controlTabController.setTooltips(currentConfig);
    }

    /**
     * Set tooltip properties
     * @param text tooltip string
     */
    public Tooltip createTooltip(String text) {
        return createTooltip(text, 300);
    }

    /**
     * Set tooltip properties width delays
     * @param text      tooltip string
     * @param showDelay delay used to show the tooltip
     */
    public Tooltip createTooltip(String text, int showDelay) {
        Tooltip tooltip;
        tooltip = new Tooltip(CommonUtility.getWord(text));
        tooltip.setShowDelay(Duration.millis(showDelay));
        tooltip.setMaxWidth(300);
        tooltip.setWrapText(true);
        tooltip.setHideOnEscape(true);
        return tooltip;
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        mqttTabController.setNumericTextField();
        modeTabController.setNumericTextField();
        ledsConfigTabController.setNumericTextField();
    }

    /**
     * Send serial params
     */
    public void sendSerialParams() {
        SerialManager serialManager = new SerialManager();
        serialManager.sendSerialParams((int)(miscTabController.colorPicker.getValue().getRed() * 255),
                (int)(miscTabController.colorPicker.getValue().getGreen() * 255),
                (int)(miscTabController.colorPicker.getValue().getBlue() * 255));
    }
}
