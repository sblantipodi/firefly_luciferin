/*
  ModeTabController.java

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

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.util.StringConverter;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Mode Tab controller
 */
public class ModeTabController {

    // FXML binding
    @FXML
    public TextField screenWidth;
    @FXML
    public TextField screenHeight;
    @FXML
    public ComboBox<String> scaling;
    @FXML
    public ComboBox<String> aspectRatio;
    @FXML
    public ComboBox<Configuration.CaptureMethod> captureMethod;
    @FXML
    public ComboBox<String> algo;
    @FXML
    public TextField numberOfThreads;
    @FXML
    public Label comWirelessLabel;
    @FXML
    public Button saveSettingsButton;
    @FXML
    public Button resetButton;
    @FXML
    public ComboBox<String> monitorNumber;
    @FXML
    public ComboBox<String> baudRate;
    @FXML
    public ComboBox<String> theme;
    @FXML
    public ComboBox<String> language;
    @FXML
    public ComboBox<String> serialPort; // NOTE: for multi display this contain the deviceName of the MQTT device where to stream
    int monitorIndex;
    // Inject main controller
    @FXML
    private SettingsController settingsController;

    /**
     * Inject main controller containing the TabPane
     *
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
        for (Enums.Algo al : Enums.Algo.values()) {
            algo.getItems().add(al.getI18n());
        }
        for (Enums.AspectRatio ar : Enums.AspectRatio.values()) {
            aspectRatio.getItems().add(ar.getI18n());
        }
        aspectRatio.getItems().add(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readProfileInUseConfig();
        if (currentConfig != null && CommonUtility.isSingleDeviceOtherInstance()) {
            baudRate.setDisable(true);
            serialPort.setDisable(true);
        }
    }

    /**
     * Handle key valye combo box
     */
    public void setCaptureMethodConverter() {
        captureMethod.setConverter(new StringConverter<>() {
            @Override
            public String toString(Configuration.CaptureMethod object) {
                return switch (object) {
                    case CPU -> Configuration.CaptureMethod.CPU.name();
                    case WinAPI -> Configuration.CaptureMethod.WinAPI.name();
                    case DDUPL -> Configuration.CaptureMethod.DDUPL.name();
                    case XIMAGESRC -> Configuration.CaptureMethod.XIMAGESRC.name();
                    case XIMAGESRC_NVIDIA -> Configuration.CaptureMethod.XIMAGESRC_NVIDIA.getCaptureMethod();
                    case PIPEWIREXDG -> Configuration.CaptureMethod.PIPEWIREXDG.name();
                    case PIPEWIREXDG_NVIDIA -> Configuration.CaptureMethod.PIPEWIREXDG_NVIDIA.getCaptureMethod();
                    default -> null;
                };
            }

            @Override
            public Configuration.CaptureMethod fromString(String string) {
                return null;
            }
        });
    }

    /**
     * Init combo boxes
     */
    public void initComboBox() {
        for (Enums.ScalingRatio scalingRatio : Enums.ScalingRatio.values()) {
            scaling.getItems().add(scalingRatio.getScalingRatio());
        }
        for (Enums.BaudRate br : Enums.BaudRate.values()) {
            baudRate.getItems().add(br.getBaudRate());
        }
        for (Enums.Theme th : Enums.Theme.values()) {
            theme.getItems().add(th.getI18n());
        }
        for (Enums.Language lang : Enums.Language.values()) {
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
        theme.setValue(Enums.Theme.DEFAULT.getI18n());
        algo.setValue(Enums.Algo.AVG_COLOR.getI18n());
        language.setValue(Enums.Language.EN.getI18n());
        for (Enums.Language lang : Enums.Language.values()) {
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
                captureMethod.setValue(Configuration.CaptureMethod.AVFVIDEOSRC);
            } else {
                if (NativeExecutor.isWayland()) {
                    captureMethod.setValue(Configuration.CaptureMethod.PIPEWIREXDG);
                } else {
                    captureMethod.setValue(Configuration.CaptureMethod.XIMAGESRC);
                }
            }
        }
    }

    /**
     * Set display info on the controller
     *
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
     *
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        if ((currentConfig.getMultiMonitor() == 2 || currentConfig.getMultiMonitor() == 3)
                && serialPort.getItems() != null && !serialPort.getItems().isEmpty()) {
            serialPort.getItems().removeFirst();
        }
        screenWidth.setText(String.valueOf(currentConfig.getScreenResX()));
        screenHeight.setText(String.valueOf(currentConfig.getScreenResY()));
        scaling.setValue(currentConfig.getOsScaling() + Constants.PERCENT);
        captureMethod.setValue(Configuration.CaptureMethod.valueOf(currentConfig.getCaptureMethod()));
        if (currentConfig.isWirelessStream() && currentConfig.getOutputDevice().equals(Constants.SERIAL_PORT_AUTO)
                && ((currentConfig.getMultiMonitor() == 1) || (currentConfig.isMultiScreenSingleDevice()))) {
            if (NetworkManager.isValidIp(MainSingleton.getInstance().config.getStaticGlowWormIp())) {
                serialPort.setValue(MainSingleton.getInstance().config.getStaticGlowWormIp());
            } else {
                serialPort.setValue(MainSingleton.getInstance().config.getOutputDevice());
            }
        } else {
            if (NetworkManager.isValidIp(currentConfig.getStaticGlowWormIp())) {
                serialPort.setValue(currentConfig.getStaticGlowWormIp());
            } else {
                serialPort.setValue(currentConfig.getOutputDevice());
            }
        }
        numberOfThreads.setText(String.valueOf(currentConfig.getNumberOfCPUThreads()));
        if (currentConfig.isAutoDetectBlackBars()) {
            aspectRatio.setValue(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
        } else {
            aspectRatio.setValue(LocalizedEnum.fromBaseStr(Enums.AspectRatio.class, currentConfig.getDefaultLedMatrix()).getI18n());
        }
        algo.setValue(LocalizedEnum.fromBaseStr(Enums.Algo.class, currentConfig.getAlgo()).getI18n());
        monitorIndex = currentConfig.getMonitorNumber();
        monitorNumber.setValue(settingsController.displayManager.getDisplayName(monitorIndex));
        baudRate.setValue(currentConfig.getBaudRate());
        baudRate.setDisable(CommonUtility.isSingleDeviceOtherInstance());
        theme.setValue(LocalizedEnum.fromBaseStr(Enums.Theme.class, currentConfig.getTheme()).getI18n());
        language.setValue(LocalizedEnum.fromBaseStr(Enums.Language.class, currentConfig.getLanguage() == null ? MainSingleton.getInstance().config.getLanguage() : currentConfig.getLanguage()).getI18n());
        resetButton.setVisible(Configuration.CaptureMethod.valueOf(currentConfig.getCaptureMethod()).equals(Configuration.CaptureMethod.PIPEWIREXDG)
                || Configuration.CaptureMethod.valueOf(currentConfig.getCaptureMethod()).equals(Configuration.CaptureMethod.PIPEWIREXDG_NVIDIA));
    }

    /**
     * Manage monitor action
     */
    @FXML
    private void monitorAction() {
        monitorIndex = monitorNumber.getSelectionModel().getSelectedIndex();
        DisplayInfo screenInfo = settingsController.displayManager.getDisplayList().get(monitorIndex);
        setDispInfo(screenInfo);
    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {
        monitorNumber.valueProperty().addListener((ov, oldVal, newVal) -> monitorAction());
        serialPort.valueProperty().addListener((ov, oldVal, newVal) -> {
            if (oldVal != null && newVal != null && !oldVal.equals(newVal)) {
                settingsController.checkProfileDifferences();
            }
        });
        captureMethod.valueProperty().addListener((ov, oldVal, newVal) -> {
            if (newVal.equals(Configuration.CaptureMethod.XIMAGESRC_NVIDIA) || newVal.equals(Configuration.CaptureMethod.PIPEWIREXDG_NVIDIA)) {
                List<String> scrProcess = NativeExecutor.runNative(Constants.CMD_CUDA_CHECK, Constants.CMD_WAIT_DELAY);
                boolean pluginsFound = true;
                for (String plugin : Constants.CUDA_REQUIRED_PLUGINS) {
                    if (scrProcess.stream().noneMatch(str -> str.trim().contains(plugin))) {
                        pluginsFound = false;
                    }
                }
                if (!pluginsFound) {
                    Optional<ButtonType> result = MainSingleton.getInstance().guiManager.showAlert(CommonUtility.getWord(Constants.CUDA_ERROR_TITLE), CommonUtility.getWord(Constants.CUDA_ERROR_HEADER),
                            CommonUtility.getWord(Constants.CUDA_ERROR_CONTEXT), Alert.AlertType.CONFIRMATION);
                    ButtonType button = result.orElse(ButtonType.OK);
                    if (button == ButtonType.OK) {
                        MainSingleton.getInstance().guiManager.surfToURL(Constants.LINUX_WIKI_URL);
                    }
                }
            }
        });
    }

    /**
     * Reset button event
     *
     * @param e event
     */
    @FXML
    public void reset(InputEvent e) {
        MainSingleton.getInstance().config.setScreenCastRestoreToken("");
        settingsController.save(e);
        NativeExecutor.restartNativeInstance();
    }

    /**
     * Save button event
     *
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        settingsController.save(e);
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        if (MainSingleton.getInstance() != null && MainSingleton.getInstance().config != null) {
            config.setScreenCastRestoreToken(MainSingleton.getInstance().config.getScreenCastRestoreToken());
        }
        config.setNumberOfCPUThreads(Integer.parseInt(numberOfThreads.getText()));
        if (NetworkManager.isValidIp(serialPort.getValue())) {
            config.setOutputDevice(Constants.SERIAL_PORT_AUTO);
            config.setStaticGlowWormIp(serialPort.getValue());
        } else {
            config.setOutputDevice(serialPort.getValue());
            config.setStaticGlowWormIp(Constants.DASH);
        }
        config.setScreenResX(Integer.parseInt(screenWidth.getText()));
        config.setScreenResY(Integer.parseInt(screenHeight.getText()));
        config.setOsScaling(Integer.parseInt((scaling.getValue()).replace(Constants.PERCENT, "")));
        config.setDefaultLedMatrix(aspectRatio.getValue().equals(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)) ?
                Enums.AspectRatio.FULLSCREEN.getBaseI18n() : LocalizedEnum.fromStr(Enums.AspectRatio.class, aspectRatio.getValue()).getBaseI18n());
        config.setAutoDetectBlackBars(aspectRatio.getValue().equals(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)));
        config.setMonitorNumber(monitorNumber.getSelectionModel().getSelectedIndex());
        config.setBaudRate(baudRate.getValue());
        config.setTheme(LocalizedEnum.fromStr(Enums.Theme.class, theme.getValue()).getBaseI18n());
        config.setLanguage(language.getValue());
        if (captureMethod.getValue().name().equals(Configuration.CaptureMethod.CPU.name())
                || captureMethod.getValue().name().equals(Configuration.CaptureMethod.WinAPI.name())) {
            config.setGroupBy(Constants.GROUP_BY_LEDS);
        }
        config.setAlgo(LocalizedEnum.fromStr(Enums.Algo.class, algo.getValue()).getBaseI18n());
    }

    /**
     * Set red button if a param requires Firefly restart
     */
    @FXML
    public void saveButtonHover() {
        settingsController.checkProfileDifferences();
    }

    /**
     * Set form tooltips
     *
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        resetButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_RESET_WAYLAND));
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
            saveSettingsButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVESETTINGSBUTTON, 200));
        }
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        SettingsController.addTextFieldListener(screenWidth);
        SettingsController.addTextFieldListener(screenHeight);
        SettingsController.addTextFieldListener(numberOfThreads);
    }
}
