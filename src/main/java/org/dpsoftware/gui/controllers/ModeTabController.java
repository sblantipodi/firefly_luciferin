/*
  ModeTabController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.util.Locale;

/**
 * Mode Tab controller
 */
public class ModeTabController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    // FXML binding
    @FXML public TextField screenWidth;
    @FXML public TextField screenHeight;
    @FXML public ComboBox<String> scaling;
    @FXML public ComboBox<String> aspectRatio;
    @FXML public ComboBox<Configuration.CaptureMethod> captureMethod;
    @FXML public TextField numberOfThreads;
    @FXML public Label comWirelessLabel;
    @FXML public Button saveSettingsButton;
    @FXML public ComboBox<String> monitorNumber;
    @FXML public ComboBox<String> baudRate;
    @FXML public ComboBox<String> theme;
    @FXML public ComboBox<String> language;
    @FXML public ComboBox<String> serialPort; // NOTE: for multi display this contain the deviceName of the MQTT device where to stream
    int monitorIndex;

    /**
     * Inject main controller containing the TabPane
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {
        if (NativeExecutor.isLinux()) {
            captureMethod.getItems().addAll(Configuration.CaptureMethod.XIMAGESRC);
        }
        aspectRatio.getItems().addAll(Constants.AspectRatio.FULLSCREEN.getI18n(), Constants.AspectRatio.LETTERBOX.getI18n(),
                Constants.AspectRatio.PILLARBOX.getI18n(), CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readConfig(false);
        if (currentConfig != null && CommonUtility.isSingleDeviceOtherInstance()) {
            baudRate.setDisable(true);
            serialPort.setDisable(true);
        }
    }

    /**
     * Init combo boxes
     */
    public void initComboBox() {
        for (Constants.ScalingRatio scalingRatio : Constants.ScalingRatio.values()) {
            scaling.getItems().add(scalingRatio.getScalingRatio());
        }
        for (Constants.BaudRate br : Constants.BaudRate.values()) {
            baudRate.getItems().add(br.getBaudRate());
        }
        for (Constants.Theme th : Constants.Theme.values()) {
            theme.getItems().add(th.getI18n());
        }
        for (Constants.Language lang : Constants.Language.values()) {
            language.getItems().add(lang.getI18n());
        }
    }

    /**
     * Init form values
     */
    void initDefaultValues() {
        monitorIndex = 0;
        monitorNumber.setValue(settingsController.displayManager.getDisplayName(monitorIndex));
        comWirelessLabel.setText(CommonUtility.getWord(Constants.SERIAL_PORT));
        theme.setValue(Constants.Theme.DEFAULT.getI18n());
        language.setValue(Constants.Language.EN.getI18n());
        for (Constants.Language lang : Constants.Language.values()) {
            if (lang.name().equalsIgnoreCase(Locale.getDefault().getLanguage())) {
                language.setValue(lang.getI18n());
            }
        }
        baudRate.setValue(Constants.DEFAULT_BAUD_RATE);
        baudRate.setDisable(true);
        serialPort.setValue(Constants.SERIAL_PORT_AUTO);
        numberOfThreads.setText("1");
        aspectRatio.setValue(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
        if (settingsController.currentConfig == null) {
            DisplayInfo screenInfo = settingsController.displayManager.getFirstInstanceDisplay();
            setDispInfo(screenInfo);
            monitorNumber.setValue(settingsController.displayManager.getDisplayName(monitorIndex));
            if (NativeExecutor.isWindows()) {
                captureMethod.setValue(Configuration.CaptureMethod.DDUPL);
            } else if (NativeExecutor.isMac()) {
                captureMethod.setValue(Configuration.CaptureMethod.DDUPL);
            } else {
                captureMethod.setValue(Configuration.CaptureMethod.XIMAGESRC);
            }
        }
    }

    /**
     * Set display info on the controller
     * @param screenInfo display information
     */
    private void setDispInfo(DisplayInfo screenInfo) {
        double scaleX = screenInfo.getScaleX();
        double scaleY = screenInfo.getScaleY();

        screenWidth.setText(String.valueOf((int) (screenInfo.width * scaleX)));
        screenHeight.setText(String.valueOf((int) (screenInfo.height * scaleY)));
        scaling.setValue(((int) (screenInfo.getScaleX() * 100)) + Constants.PERCENT);
    }

    /**
     * Init form values by reading existing config file
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        if (currentConfig.getMultiMonitor() == 2 || currentConfig.getMultiMonitor() == 3) {
            serialPort.getItems().remove(0);
        }
        screenWidth.setText(String.valueOf(currentConfig.getScreenResX()));
        screenHeight.setText(String.valueOf(currentConfig.getScreenResY()));
        scaling.setValue(currentConfig.getOsScaling() + Constants.PERCENT);
        captureMethod.setValue(Configuration.CaptureMethod.valueOf(currentConfig.getCaptureMethod()));
        if (currentConfig.isMqttStream() && currentConfig.getSerialPort().equals(Constants.SERIAL_PORT_AUTO) && currentConfig.getMultiMonitor() == 1) {
            serialPort.setValue(FireflyLuciferin.config.getSerialPort());
        } else {
            serialPort.setValue(currentConfig.getSerialPort());
        }
        numberOfThreads.setText(String.valueOf(currentConfig.getNumberOfCPUThreads()));
        if (currentConfig.isAutoDetectBlackBars()) {
            aspectRatio.setValue(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
        } else {
            aspectRatio.setValue(LocalizedEnum.fromBaseStr(Constants.AspectRatio.class, currentConfig.getDefaultLedMatrix()).getI18n());
        }
        monitorIndex = currentConfig.getMonitorNumber();
        monitorNumber.setValue(settingsController.displayManager.getDisplayName(monitorIndex));
        baudRate.setValue(currentConfig.getBaudRate());
        baudRate.setDisable(CommonUtility.isSingleDeviceOtherInstance());
        theme.setValue(LocalizedEnum.fromBaseStr(Constants.Theme.class, currentConfig.getTheme()).getI18n());
        language.setValue(LocalizedEnum.fromBaseStr(Constants.Language.class, currentConfig.getLanguage() == null ? FireflyLuciferin.config.getLanguage() : currentConfig.getLanguage()).getI18n());
    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {
        monitorNumber.valueProperty().addListener((ov, oldVal, newVal) -> {
            monitorIndex = monitorNumber.getSelectionModel().getSelectedIndex();
            DisplayInfo screenInfo = settingsController.displayManager.getDisplayList().get(monitorIndex);
            setDispInfo(screenInfo);
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
        config.setNumberOfCPUThreads(Integer.parseInt(numberOfThreads.getText()));
        config.setSerialPort(serialPort.getValue());
        config.setScreenResX(Integer.parseInt(screenWidth.getText()));
        config.setScreenResY(Integer.parseInt(screenHeight.getText()));
        config.setOsScaling(Integer.parseInt((scaling.getValue()).replace(Constants.PERCENT, "")));
        config.setDefaultLedMatrix(aspectRatio.getValue().equals(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)) ?
                Constants.AspectRatio.FULLSCREEN.getBaseI18n() : LocalizedEnum.fromStr(Constants.AspectRatio.class, aspectRatio.getValue()).getBaseI18n());
        config.setAutoDetectBlackBars(aspectRatio.getValue().equals(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)));
        config.setMonitorNumber(monitorNumber.getSelectionModel().getSelectedIndex());
        config.setBaudRate(baudRate.getValue());
        config.setTheme(LocalizedEnum.fromStr(Constants.Theme.class, theme.getValue()).getBaseI18n());
        config.setLanguage(language.getValue());
        if (captureMethod.getValue().name().equals(Configuration.CaptureMethod.CPU.name())
                || captureMethod.getValue().name().equals(Configuration.CaptureMethod.WinAPI.name())) {
            config.setGroupBy(Constants.GROUP_BY_LEDS);
        }
    }

    /**
     * Set form tooltips
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        screenWidth.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SCREENWIDTH));
        screenHeight.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SCREENHEIGHT));
        scaling.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SCALING));
        if (NativeExecutor.isWindows()) {
            captureMethod.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_CAPTUREMETHOD));
        } else if (NativeExecutor.isMac()) {
            captureMethod.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MACCAPTUREMETHOD));
        } else {
            captureMethod.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_LINUXCAPTUREMETHOD));
        }
        numberOfThreads.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_NUMBEROFTHREADS));
        serialPort.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SERIALPORT));
        aspectRatio.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_ASPECTRATIO));
        monitorNumber.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MONITORNUMBER));
        baudRate.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_BAUD_RATE));
        theme.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_THEME));
        language.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_LANGUAGE));
        if (currentConfig == null) {
            saveSettingsButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVESETTINGSBUTTON_NULL));
        } else {
            saveSettingsButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVESETTINGSBUTTON,200, 6000));
        }
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        settingsController.addTextFieldListener(screenWidth);
        settingsController.addTextFieldListener(screenHeight);
        settingsController.addTextFieldListener(numberOfThreads);
    }
}
