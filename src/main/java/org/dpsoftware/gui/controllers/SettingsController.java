/*
  SettingsController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.SerialManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.managers.dto.HSLColor;
import org.dpsoftware.managers.dto.LedMatrixInfo;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


/**
 * FXML Settings Controller
 */
@Slf4j
public class SettingsController {

    @FXML
    public MiscTabController miscTabController;
    // FXML binding
    @FXML
    public AnchorPane shadowPane;
    @FXML
    public TabPane mainTabPane;
    @FXML
    public AnchorPane ledsConfigTab;
    @FXML
    public AnchorPane controlTab;
    @FXML
    public AnchorPane modeTab;
    @FXML
    public AnchorPane mqttTab;
    @FXML
    public AnchorPane miscTab;
    @FXML
    public AnchorPane devicesTab;
    @FXML
    public Button closeWindowBtn;
    @FXML
    public Button minimizeWindowBtn;
    Configuration currentConfig;
    StorageManager sm;
    DisplayManager displayManager;
    // Inject children tab controllers
    @FXML
    private MqttTabController mqttTabController;
    @FXML
    private DevicesTabController devicesTabController;
    @FXML
    private ModeTabController modeTabController;
    @FXML
    private LedsConfigTabController ledsConfigTabController;
    @FXML
    private ControlTabController controlTabController;
    @FXML
    private ColorCorrectionDialogController colorCorrectionDialogController;
    @FXML
    private EyeCareDialogController eyeCareDialogController;
    @FXML
    private SatellitesDialogController satellitesDialogController;

    /**
     * Force TextField to be numeric
     *
     * @param textField numeric fields
     */
    public static void addTextFieldListener(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                textField.setText("0");
            } else {
                textField.setText(CommonUtility.removeChars(newValue));
            }
        });
    }

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
        for (int i = 0; i < displayManager.displayNumber(); i++) {
            modeTabController.monitorNumber.getItems().add(displayManager.getDisplayName(i));
            switch (i) {
                case 0 ->
                        devicesTabController.multiMonitor.getItems().add(CommonUtility.getWord(Constants.MULTIMONITOR_1));
                case 1 ->
                        devicesTabController.multiMonitor.getItems().add(CommonUtility.getWord(Constants.MULTIMONITOR_2));
                case 2 ->
                        devicesTabController.multiMonitor.getItems().add(CommonUtility.getWord(Constants.MULTIMONITOR_3));
            }
        }
        currentConfig = sm.readProfileInUseConfig();
        ledsConfigTabController.showTestImageButton.setVisible(currentConfig != null);
        initComboBox();
        if (NativeExecutor.isWindows()) {
            mainTabPane.getTabs().remove(0);
        }
        if (currentConfig != null && CommonUtility.isSingleDeviceMultiScreen()) {
            if (MainSingleton.getInstance().whoAmI > 1) {
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
        Label titleBarLabel = (Label) shadowPane.lookup(Constants.TITLE_BAR_SELECTOR);
        if (titleBarLabel != null) {
            titleBarLabel.setText(GuiManager.createWindowTitle().trim());
        }
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
            if (eyeCareDialogController != null) {
                eyeCareDialogController.initDefaultValues();
            }
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
                        NativeExecutor.exit();
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
                    if (!modeTabController.serialPort.getItems().isEmpty() && modeTabController.serialPort.getItems().get(0).equals(Constants.SERIAL_PORT_AUTO)) {
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
     *
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        save(e, null);
    }

    /**
     * Save button event
     *
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
            Map<Enums.ColorEnum, HSLColor> hueMap = ColorCorrectionDialogController.initHSLMap();
            Configuration config = new Configuration(ledFullScreenMatrix, ledLetterboxMatrix, fitToScreenMatrix, hueMap);
            if (MainSingleton.getInstance().config != null) {
                config.setRuntimeLogLevel(MainSingleton.getInstance().config.getRuntimeLogLevel());
            }
            ledsConfigTabController.save(config);
            modeTabController.save(config);
            miscTabController.save(config);
            mqttTabController.save(config);
            devicesTabController.save(config);
            saveDialogues(config);
            setCaptureMethod(config);
            config.setConfigVersion(MainSingleton.getInstance().version);
            boolean firstStartup = MainSingleton.getInstance().config == null;
            if (config.isFullFirmware() && !config.isMqttEnable() && firstStartup) {
                config.setOutputDevice(Constants.SERIAL_PORT_AUTO);
            }
            if (firstStartup) {
                if (config.isFullFirmware()) {
                    config.setBaudRate(Enums.BaudRate.BAUD_RATE_115200.getBaudRate());
                } else {
                    config.setBaudRate(Enums.BaudRate.BAUD_RATE_500000.getBaudRate());
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
     * Save settings from sub dialogues
     *
     * @param config from file
     */
    private void saveDialogues(Configuration config) {
        if (colorCorrectionDialogController != null) {
            colorCorrectionDialogController.save(config);
        } else if (MainSingleton.getInstance().config != null) {
            config.setHueMap(MainSingleton.getInstance().config.getHueMap());
        }
        if (eyeCareDialogController != null) {
            eyeCareDialogController.save(config);
        } else {
            if (MainSingleton.getInstance().config != null) {
                config.setEnableLDR(MainSingleton.getInstance().config.isEnableLDR());
                config.setLdrTurnOff(MainSingleton.getInstance().config.isLdrTurnOff());
                config.setLdrInterval(MainSingleton.getInstance().config.getLdrInterval());
                config.setLdrMin(MainSingleton.getInstance().config.getLdrMin());
                config.setBrightnessLimiter(MainSingleton.getInstance().config.getBrightnessLimiter());
            }
        }
        if (satellitesDialogController != null) {
            satellitesDialogController.save(config);
        } else {
            if (MainSingleton.getInstance().config != null) {
                config.setSatellites(MainSingleton.getInstance().config.getSatellites());
            }
        }
    }

    /**
     * Write default config
     *
     * @param e            event that triggered the save event
     * @param config       config to save
     * @param firstStartup check if config exist
     * @throws IOException                can't write
     * @throws CloneNotSupportedException can't clone
     */
    private void writeDefaultConfig(InputEvent e, Configuration config, boolean firstStartup) throws IOException, CloneNotSupportedException {
        // Manage settings from one instance to the other, for multi monitor setup
        if (MainSingleton.getInstance().whoAmI != 1) {
            Configuration mainConfig = sm.readMainConfig();
            mainConfig.setGamma(config.getGamma());
            mainConfig.setWhiteTemperature(config.getWhiteTemperature());
            mainConfig.setCheckForUpdates(devicesTabController.checkForUpdates.isSelected());
            mainConfig.setSyncCheck(devicesTabController.syncCheck.isSelected());
            setConfig(config, mainConfig);
            sm.writeConfig(mainConfig, Constants.CONFIG_FILENAME);
        }
        if (config.getMultiMonitor() > 1) {
            switch (MainSingleton.getInstance().whoAmI) {
                case 1 -> {
                    writeOtherConfig(config, Constants.CONFIG_FILENAME_2);
                    if (config.getMultiMonitor() == 3) writeOtherConfig(config, Constants.CONFIG_FILENAME_3);
                }
                case 2 -> {
                    if (config.getMultiMonitor() == 3) writeOtherConfig(config, Constants.CONFIG_FILENAME_3);
                }
                case 3 -> writeOtherConfig(config, Constants.CONFIG_FILENAME_2);
            }
        }
        Configuration defaultConfig = sm.readProfileInUseConfig();
        sm.writeConfig(config, null);
        MainSingleton.getInstance().config = config;
        sm.checkProfileDifferences(defaultConfig, MainSingleton.getInstance().config);
        if (firstStartup || (MainSingleton.getInstance().whoAmI == 1 && ((config.getMultiMonitor() == 2 && !sm.checkIfFileExist(Constants.CONFIG_FILENAME_2))
                || (config.getMultiMonitor() == 3 && (!sm.checkIfFileExist(Constants.CONFIG_FILENAME_2) || !sm.checkIfFileExist(Constants.CONFIG_FILENAME_3)))))) {
            writeOtherConfigNew(config);
            cancel(e);
        }
        if (!firstStartup) {
            String oldBaudrate = currentConfig.getBaudRate();
            boolean isBaudRateChanged = !modeTabController.baudRate.getValue().equals(currentConfig.getBaudRate());
            if (isBaudRateChanged || isMqttParamChanged()) {
                programFirmware(config, e, oldBaudrate, isBaudRateChanged, isMqttParamChanged());
            } else if (sm.restartNeeded) {
                exit(e);
            }
        }
        refreshValuesOnScene();
    }

    /**
     * Check is one of the MQTT params has been changed by the user
     *
     * @return true if something changed
     */
    public boolean isMqttParamChanged() {
        return currentConfig.isMqttEnable() != mqttTabController.mqttEnable.isSelected()
                || !currentConfig.getMqttServer().equals(mqttTabController.mqttHost.getText() + ":" + mqttTabController.mqttPort.getText())
                || !currentConfig.getMqttTopic().equals(mqttTabController.mqttTopic.getText())
                || !currentConfig.getMqttUsername().equals(mqttTabController.mqttUser.getText())
                || !currentConfig.getMqttPwd().equals(mqttTabController.mqttPwd.getText());
    }

    /**
     * Refresh all the values displayed on the scene after save or profiles switch
     */
    public void refreshValuesOnScene() {
        mqttTabController.initValuesFromSettingsFile(MainSingleton.getInstance().config);
        devicesTabController.initValuesFromSettingsFile(MainSingleton.getInstance().config);
        miscTabController.initValuesFromSettingsFile(MainSingleton.getInstance().config, false);
        ledsConfigTabController.initValuesFromSettingsFile(MainSingleton.getInstance().config);
        controlTabController.initValuesFromSettingsFile();
        modeTabController.initValuesFromSettingsFile(MainSingleton.getInstance().config);
        ledsConfigTabController.splitBottomMargin.setValue(MainSingleton.getInstance().config.getSplitBottomMargin());
        ledsConfigTabController.splitBottomRow();
        miscTabController.setContextMenu();
        FireflyLuciferin.setLedNumber(MainSingleton.getInstance().config.getDefaultLedMatrix());
    }

    /**
     * Set capture method, write preferences to Windows Registry
     *
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
            } else if (modeTabController.captureMethod.getValue() == Configuration.CaptureMethod.PIPEWIREXDG) {
                config.setCaptureMethod(Configuration.CaptureMethod.PIPEWIREXDG.name());
            }
        }
    }

    /**
     * Program firmware, set baud rate and mqtt topic on all instances
     *
     * @param config             configuration on file
     * @param e                  event that launched
     * @param oldBaudrate        baud rate before the change
     * @param isBaudRateChanged  condition that monitor if baudrate is changed
     * @param isMqttParamChanged condition that monitor if mqtt params are changed
     */
    void programFirmware(Configuration config, InputEvent e, String oldBaudrate, boolean isBaudRateChanged, boolean isMqttParamChanged) throws IOException {
        AtomicReference<String> macToProgram = new AtomicReference<>("");
        if (currentConfig.isFullFirmware()) {
            if (GuiSingleton.getInstance().deviceTableData != null && !GuiSingleton.getInstance().deviceTableData.isEmpty()) {
                if (Constants.SERIAL_PORT_AUTO.equals(modeTabController.serialPort.getValue())) {
                    macToProgram.set(GuiSingleton.getInstance().deviceTableData.get(0).getMac());
                }
                GuiSingleton.getInstance().deviceTableData.forEach(glowWormDevice -> {
                    if (glowWormDevice.getDeviceName().equals(modeTabController.serialPort.getValue()) || glowWormDevice.getDeviceIP().equals(modeTabController.serialPort.getValue())) {
                        macToProgram.set(glowWormDevice.getMac());
                    }
                });
            }
            if (macToProgram.get() == null || macToProgram.get().isEmpty()) {
                log.error("No device can be programmed");
            }
        }
        if (isBaudRateChanged) {
            Optional<ButtonType> result = MainSingleton.getInstance().guiManager.showLocalizedAlert(Constants.BAUDRATE_TITLE, Constants.BAUDRATE_HEADER,
                    Constants.BAUDRATE_CONTEXT, Alert.AlertType.CONFIRMATION);
            ButtonType button = result.orElse(ButtonType.OK);
            if (button == ButtonType.OK) {
                if (currentConfig.isFullFirmware()) {
                    setFirmwareConfig(macToProgram.get(), true);
                } else {
                    MainSingleton.getInstance().baudRate = Enums.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + modeTabController.baudRate.getValue()).getBaudRateValue();
                    SerialManager serialManager = new SerialManager();
                    serialManager.sendSerialParams((int) (miscTabController.colorPicker.getValue().getRed() * 255),
                            (int) (miscTabController.colorPicker.getValue().getGreen() * 255),
                            (int) (miscTabController.colorPicker.getValue().getBlue() * 255));
                }
                exit(e);
            } else if (button == ButtonType.CANCEL) {
                config.setBaudRate(oldBaudrate);
                modeTabController.baudRate.setValue(oldBaudrate);
                sm.writeConfig(config, null);
            }
        } else if (isMqttParamChanged) {
            setFirmwareConfig(macToProgram.get(), false);
            exit(e);
        }
    }

    /**
     * Program FULL firmware with params that requires a device reboot.
     *
     * @param changeBaudrate true if baudrate must be changed
     */
    public void setFirmwareConfig(String macToProgram, boolean changeBaudrate) {
        if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getDeviceName() != null
                && (CommonUtility.getDeviceToUse().getDeviceName().equals(MainSingleton.getInstance().config.getOutputDevice())
                || CommonUtility.getDeviceToUse().getMac().equals(macToProgram))) {
            var device = CommonUtility.getDeviceToUse();
            FirmwareConfigDto firmwareConfigDto = new FirmwareConfigDto();
            firmwareConfigDto.setDeviceName(device.getDeviceName());
            firmwareConfigDto.setMicrocontrollerIP(device.isDhcpInUse() ? "" : device.getDeviceIP());
            firmwareConfigDto.setMqttCheckbox(mqttTabController.mqttEnable.isSelected());
            firmwareConfigDto.setSsid("");
            firmwareConfigDto.setWifipwd("");
            if (mqttTabController.mqttEnable.isSelected()) {
                firmwareConfigDto.setMqttIP(mqttTabController.mqttHost.getText().split("//")[1]);
                firmwareConfigDto.setMqttPort(mqttTabController.mqttPort.getText());
                firmwareConfigDto.setMqttTopic(mqttTabController.mqttTopic.getText());
                firmwareConfigDto.setMqttuser(mqttTabController.mqttUser.getText());
                firmwareConfigDto.setMqttpass(mqttTabController.mqttPwd.getText());
            }
            firmwareConfigDto.setAdditionalParam(device.getGpio());
            firmwareConfigDto.setColorMode(String.valueOf(miscTabController.colorMode.getSelectionModel().getSelectedIndex() + 1));
            if (changeBaudrate) {
                firmwareConfigDto.setBr(Enums.BaudRate.findByExtendedVal(modeTabController.baudRate.getValue()).getBaudRateValue());
            }
            firmwareConfigDto.setLednum(device.getNumberOfLEDSconnected());
            TcpResponse tcpResponse = NetworkManager.publishToTopic(Constants.HTTP_SETTING, CommonUtility.toJsonString(firmwareConfigDto), true);
            if (tcpResponse.getErrorCode() == Constants.HTTP_SUCCESS) {
                log.info(CommonUtility.getWord(Constants.FIRMWARE_PROGRAM_NOTIFY_HEADER));
                if (NativeExecutor.isWindows()) {
                    MainSingleton.getInstance().guiManager.showLocalizedNotification(CommonUtility.getWord(Constants.FIRMWARE_PROGRAM_NOTIFY),
                            CommonUtility.getWord(Constants.FIRMWARE_PROGRAM_NOTIFY_HEADER), TrayIcon.MessageType.INFO);
                }
            }
        }
    }

    /**
     * Write other config files if there are more than one instance running
     *
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
     *
     * @param config              configuration
     * @param otherConfigFilename file to write
     */
    void writeOtherConfig(Configuration config, String otherConfigFilename) throws IOException {
        Configuration otherConfig = sm.readConfigFile(otherConfigFilename);
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
     *
     * @param config      config
     * @param otherConfig otherConfig
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
            otherConfig.setOutputDevice(config.getOutputDevice());
            otherConfig.setStaticGlowWormIp(config.getStaticGlowWormIp());
            otherConfig.setBaudRate(config.getBaudRate());
            otherConfig.setMqttServer(config.getMqttServer());
            otherConfig.setMqttTopic(config.getMqttTopic());
            otherConfig.setMqttUsername(config.getMqttUsername());
            otherConfig.setMqttPwd(config.getMqttPwd());
            otherConfig.setWirelessStream(config.isWirelessStream());
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
     *
     * @param config     configuration
     * @param filename   filename to write
     * @param comPort    comport to use as defaults
     * @param monitorNum monitor number, it's relative to the instance number
     * @throws CloneNotSupportedException file exception
     * @throws IOException                file exception
     */
    void writeSingleConfigNew(Configuration config, String filename, int comPort, int monitorNum) throws CloneNotSupportedException, IOException {
        Configuration tempConfiguration = (Configuration) config.clone();
        if (tempConfiguration.isFullFirmware() && !tempConfiguration.isMqttEnable() && tempConfiguration.getMultiMonitor() > 1) {
            tempConfiguration.setOutputDevice(Constants.SERIAL_PORT_AUTO);
        } else {
            tempConfiguration.setOutputDevice(Constants.SERIAL_PORT_COM + comPort);
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
        config.getLedMatrix().put(Enums.AspectRatio.FULLSCREEN.getBaseI18n(), ledCoordinate.initFullScreenLedMatrix(ledMatrixInfoFullScreen));
        LedMatrixInfo ledMatrixInfoLetterbox = (LedMatrixInfo) ledMatrixInfo.clone();
        config.getLedMatrix().put(Enums.AspectRatio.LETTERBOX.getBaseI18n(), ledCoordinate.initFullScreenLedMatrix(ledMatrixInfoLetterbox));
        LedMatrixInfo ledMatrixInfoPillarbox = (LedMatrixInfo) ledMatrixInfo.clone();
        config.getLedMatrix().put(Enums.AspectRatio.PILLARBOX.getBaseI18n(), ledCoordinate.initFullScreenLedMatrix(ledMatrixInfoPillarbox));
        sm.writeConfig(tempConfiguration, filename);
    }

    /**
     * Initilize output device chooser
     *
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
                } else {
                    modeTabController.captureMethod.getItems().addAll(Configuration.CaptureMethod.XIMAGESRC, Configuration.CaptureMethod.PIPEWIREXDG);
                }
            }
            if (NativeExecutor.isWindows()) {
                SerialManager serialManager = new SerialManager();
                Map<String, Boolean> availableDevices = serialManager.getAvailableDevices();
                availableDevices.forEach((portName, isAvailable) -> modeTabController.serialPort.getItems().add(portName));
            } else {
                for (int i = 0; i <= 256; i++) {
                    modeTabController.serialPort.getItems().add(Constants.SERIAL_PORT_TTY + i);
                }
            }
            modeTabController.serialPort.setValue(deviceInUse);
        } else {
            modeTabController.comWirelessLabel.setText(CommonUtility.getWord(Constants.OUTPUT_DEVICE));
            if (!modeTabController.serialPort.isFocused()) {
                String deviceInUse = modeTabController.serialPort.getValue();
                modeTabController.serialPort.getItems().clear();
                GuiSingleton.getInstance().deviceTableData.forEach(glowWormDevice -> modeTabController.serialPort.getItems().add(glowWormDevice.getDeviceName()));
                modeTabController.serialPort.setValue(deviceInUse);
            }
        }
    }

    /**
     * Method used to enable disable sat button
     *
     * @param isFullFirmware true if full firmware
     */
    public void evaluateSatBtn(boolean isFullFirmware) {
        devicesTabController.evaluateSatelliteBtn(isFullFirmware);
    }

    /**
     * Save and Exit button event
     *
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
     *
     * @param text tooltip string
     */
    public Tooltip createTooltip(String text) {
        return createTooltip(text, Constants.TOOLTIP_DELAY);
    }

    /**
     * Set tooltip properties width delays
     *
     * @param text      tooltip string
     * @param showDelay delay used to show the tooltip
     */
    public Tooltip createTooltip(String text, int showDelay) {
        Tooltip tooltip;
        tooltip = new Tooltip(CommonUtility.getWord(text));
        tooltip.setShowDelay(Duration.millis(showDelay));
        tooltip.setMaxWidth(Constants.TOOLTIP_MAX_WIDTH);
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
        serialManager.sendSerialParams((int) (miscTabController.colorPicker.getValue().getRed() * 255),
                (int) (miscTabController.colorPicker.getValue().getGreen() * 255),
                (int) (miscTabController.colorPicker.getValue().getBlue() * 255));
    }

    /**
     * Check if the changed param requires Luciferin restart
     */
    public void checkProfileDifferences() {
        checkProfileDifferences(null);
    }

    /**
     * Check if the changed param requires Luciferin restart
     *
     * @param profileToUse profile to use for comparison
     */
    public void checkProfileDifferences(String profileToUse) {
        StorageManager sm = new StorageManager();
        Configuration profileInUse;
        Configuration currentSettingsInUse = new Configuration();
        if (profileToUse == null) {
            profileInUse = sm.readProfileInUseConfig();
        } else {
            profileInUse = sm.readProfileConfig(profileToUse);
        }
        setModeTabParams(currentSettingsInUse);
        setMqttTabParams(currentSettingsInUse);
        setDevicesTabParams(currentSettingsInUse);
        sm.checkProfileDifferences(profileInUse, currentSettingsInUse);
        if (sm.restartNeeded) {
            if (profileToUse != null) {
                setProfileButtonColor(true, 0);
            } else {
                setSaveButtonColor(Constants.SAVE_AND_CLOSE, 0);
            }
        } else {
            if (profileToUse != null) {
                setProfileButtonColor(false, Constants.TOOLTIP_DELAY);
            } else {
                setSaveButtonColor(Constants.SAVE, Constants.TOOLTIP_DELAY);
            }
        }
    }

    /**
     * Set Devices Tab params
     *
     * @param currentSettingsInUse object used for the comparison with the profile object
     */
    private void setDevicesTabParams(Configuration currentSettingsInUse) {
        if (devicesTabController.multiMonitor.getValue().equals(CommonUtility.getWord(Constants.MULTIMONITOR_2))) {
            currentSettingsInUse.setMultiMonitor(2);
        } else if (devicesTabController.multiMonitor.getValue().equals(CommonUtility.getWord(Constants.MULTIMONITOR_3))) {
            currentSettingsInUse.setMultiMonitor(3);
        } else {
            currentSettingsInUse.setMultiMonitor(1);
        }
        currentSettingsInUse.setMultiScreenSingleDevice(devicesTabController.multiScreenSingleDevice.isSelected());
    }

    /**
     * Set MQTT Tab params
     *
     * @param currentSettingsInUse object used for the comparison with the profile object
     */
    private void setMqttTabParams(Configuration currentSettingsInUse) {
        currentSettingsInUse.setMqttServer(mqttTabController.mqttHost.getText() + ":" + mqttTabController.mqttPort.getText());
        currentSettingsInUse.setMqttTopic(mqttTabController.mqttTopic.getText());
        currentSettingsInUse.setMqttUsername(mqttTabController.mqttUser.getText());
        currentSettingsInUse.setMqttPwd(mqttTabController.mqttPwd.getText());
        currentSettingsInUse.setFullFirmware(mqttTabController.wifiEnable.isSelected());
        currentSettingsInUse.setMqttEnable(mqttTabController.mqttEnable.isSelected());
        currentSettingsInUse.setWirelessStream(mqttTabController.mqttStream.isSelected());
        currentSettingsInUse.setStreamType(mqttTabController.streamType.getValue());
    }

    /**
     * Set Mode Tab params
     *
     * @param currentSettingsInUse object used for the comparison with the profile object
     */
    private void setModeTabParams(Configuration currentSettingsInUse) {
        currentSettingsInUse.setTheme(modeTabController.theme.getValue());
        currentSettingsInUse.setBaudRate(modeTabController.baudRate.getValue());
        currentSettingsInUse.setTheme(LocalizedEnum.fromStr(Enums.Theme.class, modeTabController.theme.getValue()).getBaseI18n());
        currentSettingsInUse.setLanguage(modeTabController.language.getValue());
        currentSettingsInUse.setNumberOfCPUThreads(Integer.parseInt(modeTabController.numberOfThreads.getText()));
        currentSettingsInUse.setCaptureMethod(modeTabController.captureMethod.getValue().name());
        currentSettingsInUse.setOutputDevice(modeTabController.serialPort.getValue());
    }

    /**
     * Set save button color and tooltip delay when some settings requires Luciferin restart
     */
    private void setSaveButtonColor(String buttonText, int tooltipDelay) {
        ledsConfigTabController.saveLedButton.setText(CommonUtility.getWord(buttonText));
        modeTabController.saveSettingsButton.setText(CommonUtility.getWord(buttonText));
        mqttTabController.saveMQTTButton.setText(CommonUtility.getWord(buttonText));
        miscTabController.saveMiscButton.setText(CommonUtility.getWord(buttonText));
        devicesTabController.saveDeviceButton.setText(CommonUtility.getWord(buttonText));
        if (buttonText.equals(Constants.SAVE)) {
            ledsConfigTabController.saveLedButton.getStyleClass().removeIf(Constants.CSS_STYLE_RED_BUTTON::equals);
            modeTabController.saveSettingsButton.getStyleClass().removeIf(Constants.CSS_STYLE_RED_BUTTON::equals);
            mqttTabController.saveMQTTButton.getStyleClass().removeIf(Constants.CSS_STYLE_RED_BUTTON::equals);
            miscTabController.saveMiscButton.getStyleClass().removeIf(Constants.CSS_STYLE_RED_BUTTON::equals);
            devicesTabController.saveDeviceButton.getStyleClass().removeIf(Constants.CSS_STYLE_RED_BUTTON::equals);
            ledsConfigTabController.saveLedButton.setTooltip(null);
            modeTabController.saveSettingsButton.setTooltip(null);
            mqttTabController.saveMQTTButton.setTooltip(null);
            miscTabController.saveMiscButton.setTooltip(null);
            devicesTabController.saveDeviceButton.setTooltip(null);
        } else {
            ledsConfigTabController.saveLedButton.getStyleClass().add(Constants.CSS_STYLE_RED_BUTTON);
            modeTabController.saveSettingsButton.getStyleClass().add(Constants.CSS_STYLE_RED_BUTTON);
            mqttTabController.saveMQTTButton.getStyleClass().add(Constants.CSS_STYLE_RED_BUTTON);
            miscTabController.saveMiscButton.getStyleClass().add(Constants.CSS_STYLE_RED_BUTTON);
            devicesTabController.saveDeviceButton.getStyleClass().add(Constants.CSS_STYLE_RED_BUTTON);
            ledsConfigTabController.saveLedButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVELEDBUTTON, tooltipDelay));
            modeTabController.saveSettingsButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVESETTINGSBUTTON, tooltipDelay));
            mqttTabController.saveMQTTButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON, tooltipDelay));
            miscTabController.saveMiscButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON, tooltipDelay));
            devicesTabController.saveDeviceButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEDEVICEBUTTON, tooltipDelay));
        }
    }

    /**
     * Set apply profile button color and tooltip delay when some settings requires Luciferin restart
     */
    void setProfileButtonColor(boolean addRedClass, int tooltipDelay) {
        if (addRedClass) {
            miscTabController.applyProfileButton.getStyleClass().add(Constants.CSS_STYLE_RED_BUTTON);
            miscTabController.applyProfileButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVESETTINGSBUTTON, tooltipDelay));
        } else {
            miscTabController.applyProfileButton.getStyleClass().removeIf(Constants.CSS_STYLE_RED_BUTTON::equals);
            miscTabController.applyProfileButton.setTooltip(createTooltip(Constants.TOOLTIP_PROFILES_APPLY));
        }
    }

    /**
     * Inject color correction dialogue controller into the main controller
     *
     * @param colorCorrectionDialogController dialog controller
     */
    public void injectColorCorrectionController(ColorCorrectionDialogController colorCorrectionDialogController) {
        this.colorCorrectionDialogController = colorCorrectionDialogController;
    }

    /**
     * Inject eye care dialogue controller into the main controller
     *
     * @param eyeCareDialogController dialog controller
     */
    public void injectEyeCareController(EyeCareDialogController eyeCareDialogController) {
        this.eyeCareDialogController = eyeCareDialogController;
    }

    /**
     * Inject satellites dialogue controller into the main controller
     *
     * @param satellitesDialogController dialog controller
     */
    public void injectSatellitesController(SatellitesDialogController satellitesDialogController) {
        this.satellitesDialogController = satellitesDialogController;
    }

    /**
     * Minimize window
     */
    @FXML
    public void minimizeWindow() {
        Stage obj = (Stage) closeWindowBtn.getScene().getWindow();
        obj.setIconified(true);
    }

    /**
     * Close window
     */
    @FXML
    public void closeWindow(InputEvent e) {
        CommonUtility.closeCurrentStage(e);
    }

}
