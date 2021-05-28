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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.DisplayInfo;

/**
 * MQTT Tab controller
 */
public class ModeTabController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    @FXML public TextField screenWidth;
    @FXML public TextField screenHeight;
    @FXML public ComboBox<String> scaling;
    @FXML public ComboBox<String> aspectRatio;
    @FXML public ComboBox<Configuration.CaptureMethod> captureMethod;
    @FXML public TextField numberOfThreads;
    @FXML public Label comWirelessLabel;
    @FXML public Button saveSettingsButton;
    @FXML public ComboBox<Integer> monitorNumber;
    @FXML public ComboBox<String> baudRate;
    @FXML public ComboBox<String> serialPort; // NOTE: for multi display this contain the deviceName of the MQTT device where to stream

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
        aspectRatio.getItems().addAll(Constants.AspectRatio.FULLSCREEN.getAspectRatio(), Constants.AspectRatio.LETTERBOX.getAspectRatio(),
                Constants.AspectRatio.PILLARBOX.getAspectRatio(), Constants.AUTO_DETECT_BLACK_BARS);

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

    }

    /**
     * Init form values
     */
    void initDefaultValues() {

        monitorNumber.setValue(1);
        comWirelessLabel.setText(Constants.SERIAL_PORT);
        baudRate.setValue(Constants.DEFAULT_BAUD_RATE);
        baudRate.setDisable(true);
        serialPort.setValue(Constants.SERIAL_PORT_AUTO);
        numberOfThreads.setText("1");
        aspectRatio.setValue(Constants.AUTO_DETECT_BLACK_BARS);
        if (settingsController.currentConfig == null) {
            DisplayInfo screenInfo = settingsController.displayManager.getFirstInstanceDisplay();
            setDispInfo(screenInfo);
            monitorNumber.setValue(screenInfo.getFxDisplayNumber());
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
            aspectRatio.setValue(Constants.AUTO_DETECT_BLACK_BARS);
        } else {
            aspectRatio.setValue(currentConfig.getDefaultLedMatrix());
        }
        monitorNumber.setValue(currentConfig.getMonitorNumber());
        baudRate.setValue(currentConfig.getBaudRate());
        baudRate.setDisable(false);

    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {

        monitorNumber.valueProperty().addListener((ov, oldVal, newVal) -> {
            DisplayInfo screenInfo = settingsController.displayManager.getDisplayList().get(1);
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
