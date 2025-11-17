/*
  MiscTabController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.AudioLoopbackSoftware;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.audio.AudioUtility;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.WidgetFactory;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.*;
import org.dpsoftware.managers.dto.AudioDevice;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.managers.dto.mqttdiscovery.SelectProfileDiscovery;
import org.dpsoftware.utilities.CommonUtility;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Misc Tab controller
 */
@Slf4j
public class MiscTabController {

    @FXML
    public ColorPicker colorPicker;
    @FXML
    public ToggleButton toggleLed;
    @FXML
    public ComboBox<String> framerate;
    @FXML
    public ComboBox<String> smoothing;
    @FXML
    public Slider brightness;
    @FXML
    public ComboBox<String> colorMode;
    @FXML
    public ComboBox<String> gamma;
    @FXML
    public ComboBox<String> effect;
    @FXML
    public Slider audioGain;
    @FXML
    public ComboBox<String> audioChannels;
    @FXML
    public ComboBox<String> audioDevice;
    @FXML
    public Slider whiteTemp;
    @FXML
    public Spinner<LocalTime> nightModeFrom;
    @FXML
    public Spinner<LocalTime> nightModeTo;
    @FXML
    public Spinner<String> nightModeBrightness;
    @FXML
    public Button saveMiscButton;
    @FXML
    public Button addProfileButton;
    @FXML
    public Button removeProfileButton;
    @FXML
    public Button applyProfileButton;
    @FXML
    public Button eyeCareBtn;
    @FXML
    public Button smoothingBtn;
    @FXML
    public ComboBox<String> profiles;
    // Inject main controller
    @FXML
    private SettingsController settingsController;
    @FXML
    private Label contextChooseColorChooseLoopback;
    @FXML
    private Label contextGammaGain;

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
        if (MainSingleton.getInstance().config == null) {
            colorMode.setDisable(true);
            whiteTemp.setDisable(true);
        }
        if (MainSingleton.getInstance().config != null) {
            Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
            CommonUtility.delaySeconds(() -> initAudioComboWhenNeeded(effectInUse), 3);
            eyeCareBtn.setDisable(false);
        } else {
            eyeCareBtn.setDisable(true);
        }
        manageFramerate();
    }

    /**
     * Init audio combo
     */
    private void initAudioCombo() {
        if (MainSingleton.getInstance().config != null && AudioSingleton.getInstance().audioDevices.isEmpty()) {
            AudioUtility audioLoopback = new AudioLoopbackSoftware();
            for (AudioDevice device : audioLoopback.getLoopbackDevices().values()) {
                addStaticDevices();
                if (device.getDeviceName().contains(Constants.LOOPBACK) || device.getDeviceName().contains(Constants.SHARED)) {
                    audioDevice.getItems().add(device.getDeviceName());
                }
            }
        } else {
            for (AudioDevice device : AudioSingleton.getInstance().audioDevices.values()) {
                addStaticDevices();
                if (!audioDevice.getItems().contains(device.getDeviceName())) {
                    audioDevice.getItems().add(device.getDeviceName());
                }
            }
        }
    }

    /**
     * Add static devices
     */
    private void addStaticDevices() {
        if (NativeExecutor.isWindows() && !audioDevice.getItems().contains(Enums.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getI18n())) {
            audioDevice.getItems().add(Enums.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getI18n());
        }
        if (!audioDevice.getItems().contains(Enums.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getI18n())) {
            audioDevice.getItems().add(Enums.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getI18n());
        }
    }

    /**
     * Manage framerate field
     */
    private void manageFramerate() {
        for (Enums.Framerate fps : Enums.Framerate.values()) {
            if (fps.getBaseI18n().equals(Enums.Framerate.UNLOCKED.getBaseI18n())) {
                framerate.getItems().add(fps.getI18n());
            } else {
                framerate.getItems().add(fps.getI18n() + Constants.FPS_VAL);
            }
        }
        framerate.setEditable(true);
        framerate.setOnKeyPressed(event -> {
            if (MainSingleton.getInstance().config != null) {
                String fpsInt = CommonUtility.removeChars(framerate.getValue());
                if (LocalizedEnum.fromStr(Enums.Framerate.class, framerate.getValue()) != Enums.Framerate.UNLOCKED) {
                    framerate.setValue(fpsInt + Constants.FPS_VAL);
                }
                forceFramerateValidation(framerate.getValue());
                if (event.getCode() == KeyCode.ENTER) {
                    if (!framerate.getValue().isEmpty() && Integer.parseInt(CommonUtility.removeChars(framerate.getValue())) > 0
                            || (LocalizedEnum.fromStr(Enums.Framerate.class, framerate.getValue()) == Enums.Framerate.UNLOCKED)) {
                        setFramerateIntoConfig(MainSingleton.getInstance().config);
                        PipelineManager.restartCapture(CommonUtility::run);
                    }
                }
            }
        });
        framerate.getEditor().textProperty().addListener((_, _, newValue) -> {
            forceFramerateValidation(newValue);
            if (framerate.isShowing()) {
                forceFramerateValidation(newValue);
                if (MainSingleton.getInstance().config != null) {
                    setFramerateIntoConfig(MainSingleton.getInstance().config);
                    PipelineManager.restartCapture(CommonUtility::run);
                }
            }
        });
        framerate.focusedProperty().addListener((_, _, focused) -> {
            if (!focused) {
                String fpsInt = CommonUtility.removeChars(framerate.getValue());
                if (LocalizedEnum.fromStr(Enums.Framerate.class, framerate.getValue()) != Enums.Framerate.UNLOCKED) {
                    framerate.setValue(fpsInt + Constants.FPS_VAL);
                }
                if (MainSingleton.getInstance().config != null && !fpsInt.equals(MainSingleton.getInstance().config.getDesiredFramerate())) {
                    if (MainSingleton.getInstance().RUNNING && !framerate.getValue().equals(MainSingleton.getInstance().config.getDesiredFramerate())) {
                        Platform.runLater(() -> {
                            setFramerateIntoConfig(MainSingleton.getInstance().config);
                            PipelineManager.restartCapture(CommonUtility::run);
                        });
                    }
                }
            }
        });
    }

    /**
     * Set capture framerate into config
     *
     * @param config currecnt config file
     */
    private void setFramerateIntoConfig(Configuration config) {
        if (LocalizedEnum.fromStr(Enums.Framerate.class, framerate.getValue()) != Enums.Framerate.UNLOCKED) {
            config.setDesiredFramerate(framerate.getValue().replaceAll(Constants.FPS_VAL, ""));
        } else {
            config.setDesiredFramerate(Enums.Framerate.UNLOCKED.getBaseI18n());
        }
    }

    /**
     * Init combo boxes
     */
    void initComboBox() {
        for (Enums.Gamma gma : Enums.Gamma.values()) {
            gamma.getItems().add(gma.getGamma());
        }
        for (Enums.Effect ef : Enums.Effect.values()) {
            effect.getItems().add(ef.getI18n());
        }
        for (Enums.AudioChannels audioChan : Enums.AudioChannels.values()) {
            audioChannels.getItems().add(audioChan.getI18n());
        }
        for (Enums.ColorMode colorModeVal : Enums.ColorMode.values()) {
            colorMode.getItems().add(colorModeVal.getI18n());
        }
        for (Enums.Smoothing sm : Enums.Smoothing.values()) {
            smoothing.getItems().add(sm.getI18n());
        }
    }

    /**
     * Init form values
     */
    void initDefaultValues() {
        gamma.setValue(Constants.GAMMA_DEFAULT);
        colorMode.setValue(Enums.ColorMode.RGB_MODE.getI18n());
        effect.setValue(Enums.Effect.BIAS_LIGHT.getI18n());
        framerate.setValue(Enums.Framerate.FPS_30.getI18n() + Constants.FPS_VAL);
        smoothing.setValue(CommonUtility.getWord(Constants.NO_SMOOTHING));
        toggleLed.setSelected(true);
        brightness.setValue(255);
        audioGain.setVisible(false);
        audioDevice.setVisible(false);
        audioChannels.setVisible(false);
        audioChannels.setValue(Enums.AudioChannels.AUDIO_CHANNEL_2.getI18n());
        if (NativeExecutor.isWindows()) {
            audioDevice.setValue(Enums.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getI18n());
        } else {
            audioDevice.setValue(Enums.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getI18n());
        }
        WidgetFactory widgetFactory = new WidgetFactory();
        nightModeFrom.setValueFactory(widgetFactory.timeSpinnerValueFactory(LocalTime.now().withHour(22).withMinute(0).truncatedTo(ChronoUnit.MINUTES)));
        nightModeTo.setValueFactory(widgetFactory.timeSpinnerValueFactory(LocalTime.now().withHour(8).withMinute(0).truncatedTo(ChronoUnit.MINUTES)));
        nightModeBrightness.setValueFactory(widgetFactory.spinnerNightModeValueFactory());
        enableDisableNightMode(Constants.PERCENTAGE_OFF);
        addProfileButton.setDisable(true);
        removeProfileButton.setDisable(true);
        applyProfileButton.setDisable(true);
        profiles.setDisable(true);
        profiles.setValue(CommonUtility.getWord(Constants.DEFAULT));
        colorPicker.setValue(Constants.DEFAULT_COLOR);
        smoothingBtn.setDisable(true);
    }

    /**
     * Toggle night mode params
     *
     * @param nightModeBrightness brightness param for night mode
     */
    public void enableDisableNightMode(String nightModeBrightness) {
        if (nightModeBrightness.equals(Constants.PERCENTAGE_OFF)) {
            nightModeFrom.setDisable(true);
            nightModeTo.setDisable(true);
        } else {
            nightModeFrom.setDisable(false);
            nightModeTo.setDisable(false);
        }
    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        initValuesFromSettingsFile(currentConfig, true);
    }

    /**
     * Init form values by reading existing config file
     *
     * @param updateProfiles choose if update profiles or not
     */
    public void initValuesFromSettingsFile(Configuration currentConfig, boolean updateProfiles) {
        smoothingBtn.setDisable(false);
        smoothing.setDisable((!currentConfig.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX11.name()))
                && (!currentConfig.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX12.name()))
                && (!currentConfig.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name()))
                && (!currentConfig.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC_NVIDIA.name()))
                && (!currentConfig.getCaptureMethod().equals(Configuration.CaptureMethod.PIPEWIREXDG.name()))
                && (!currentConfig.getCaptureMethod().equals(Configuration.CaptureMethod.PIPEWIREXDG_NVIDIA.name()))
                && (!currentConfig.getCaptureMethod().equals(Configuration.CaptureMethod.AVFVIDEOSRC.name())));
        gamma.setValue(String.valueOf(MainSingleton.getInstance().config.getGamma()));
        colorMode.setValue(Enums.ColorMode.values()[MainSingleton.getInstance().config.getColorMode() - 1].getI18n());
        if (!MainSingleton.getInstance().config.getDesiredFramerate().equals(Enums.Framerate.UNLOCKED.getBaseI18n())) {
            framerate.setValue(MainSingleton.getInstance().config.getDesiredFramerate() + Constants.FPS_VAL);
        } else {
            framerate.setValue(LocalizedEnum.fromBaseStr(Enums.Framerate.class, MainSingleton.getInstance().config.getDesiredFramerate()).getI18n());
        }
        smoothing.setValue(LocalizedEnum.fromBaseStr(Enums.Smoothing.class, MainSingleton.getInstance().config.getSmoothingType()).getI18n());
        framerate.setDisable(setFramerateEditable(LocalizedEnum.fromBaseStr(Enums.Smoothing.class, MainSingleton.getInstance().config.getSmoothingType())));
        String[] color = (MainSingleton.getInstance().config.getColorChooser().equals(Constants.DEFAULT_COLOR_CHOOSER)) ?
                currentConfig.getColorChooser().split(",") : MainSingleton.getInstance().config.getColorChooser().split(",");
        colorPicker.setValue(Color.rgb(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]), Double.parseDouble(color[3]) / 255));
        brightness.setValue((Double.parseDouble(color[3]) / 255) * 100);
        whiteTemp.setValue(MainSingleton.getInstance().config.getWhiteTemperature() * 100);
        audioGain.setValue(MainSingleton.getInstance().config.getAudioLoopbackGain());
        audioChannels.setValue(LocalizedEnum.fromBaseStr(Enums.AudioChannels.class, MainSingleton.getInstance().config.getAudioChannels()).getI18n());
        var audioDeviceFromStore = LocalizedEnum.fromBaseStr(Enums.Audio.class, MainSingleton.getInstance().config.getAudioDevice());
        String audioDeviceToDisplay;
        if (audioDeviceFromStore != null) {
            audioDeviceToDisplay = audioDeviceFromStore.getI18n();
        } else {
            audioDeviceToDisplay = MainSingleton.getInstance().config.getAudioDevice();
        }
        audioDevice.setValue(audioDeviceToDisplay);
        if (!Constants.OFF.equals(MainSingleton.getInstance().config.getEffect())) {
            effect.setValue(LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect()).getI18n());
        } else {
            effect.setValue(Enums.Effect.SOLID.getI18n());
        }
        if (MainSingleton.getInstance().config.isToggleLed()) {
            toggleLed.setText(CommonUtility.getWord(Constants.TURN_LED_OFF));
        } else {
            toggleLed.setText(CommonUtility.getWord(Constants.TURN_LED_ON));
        }
        toggleLed.setSelected(MainSingleton.getInstance().config.isToggleLed());
        WidgetFactory widgetFactory = new WidgetFactory();
        nightModeFrom.setValueFactory(widgetFactory.timeSpinnerValueFactory(LocalTime.parse(MainSingleton.getInstance().config.getNightModeFrom())));
        nightModeTo.setValueFactory(widgetFactory.timeSpinnerValueFactory(LocalTime.parse(MainSingleton.getInstance().config.getNightModeTo())));
        nightModeBrightness.setValueFactory(widgetFactory.spinnerNightModeValueFactory());
        enableDisableNightMode(nightModeBrightness.getValue());
        if (updateProfiles) {
            StorageManager sm = new StorageManager();
            profiles.getItems().addAll(sm.listProfilesForThisInstance());
            profiles.getItems().add(CommonUtility.getWord(Constants.DEFAULT));
        }
        if (MainSingleton.getInstance().profileArg.equals(Constants.DEFAULT)) {
            profiles.setValue(CommonUtility.getWord(Constants.DEFAULT));
        } else {
            profiles.setValue(MainSingleton.getInstance().profileArg);
        }
        enableDisableProfileButtons();
        evaluateLDRConnectedFeatures();
        setProfileButtonContext();
    }

    /**
     * Setup the context menu based on the selected effect
     */
    public void setContextMenu() {
        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
        if (Enums.Effect.MUSIC_MODE_VU_METER.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_VU_METER_DUAL.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_BRIGHT.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_RAINBOW.equals(effectInUse)) {
            colorPicker.setVisible(false);
            contextChooseColorChooseLoopback.setText(CommonUtility.getWord(Constants.CONTEXT_MENU_AUDIO_DEVICE));
            gamma.setVisible(false);
            contextGammaGain.setText(CommonUtility.getWord(Constants.CONTEXT_MENU_AUDIO_GAIN));
            audioGain.setVisible(true);
            audioDevice.setVisible(true);
            audioChannels.setVisible(true);
            colorMode.setVisible(false);
        } else {
            colorPicker.setVisible(true);
            contextChooseColorChooseLoopback.setText(CommonUtility.getWord(Constants.CONTEXT_MENU_COLOR));
            gamma.setVisible(true);
            contextGammaGain.setText(CommonUtility.getWord(Constants.CONTEXT_MENU_GAMMA));
            audioGain.setVisible(false);
            audioDevice.setVisible(false);
            audioChannels.setVisible(false);
            colorMode.setVisible(true);
        }
    }

    /**
     * Init all the settings listener
     *
     * @param currentConfig stored config
     */
    public void initListeners(Configuration currentConfig) {
        initColorListeners(currentConfig);
        initBrightnessGammaListeners(currentConfig);
        initWhiteTempListeners(currentConfig);
        audioGain.valueProperty().addListener((_, _, newVal) -> {
            DecimalFormat df = new DecimalFormat(Constants.NUMBER_FORMAT);
            float selectedGain = Float.parseFloat(df.format(newVal).replace(",", "."));
            MainSingleton.getInstance().config.setAudioLoopbackGain(selectedGain);
        });
        initNightModeListeners();
        initColorModeListeners(currentConfig);
        initProfilesListener();
        smoothing.valueProperty().addListener((_, _, newVal) -> manageSmoothingCombo(newVal));
    }

    /**
     * Init profile listener
     */
    private void initProfilesListener() {
        profiles.getEditor().addEventFilter(KeyEvent.KEY_RELEASED, _ -> {
            profiles.commitValue();
            enableDisableProfileButtons();
        });
        profiles.setOnAction((_) -> {
            int selectedIndex = profiles.getSelectionModel().getSelectedIndex();
            enableDisableProfileButtons();
            if (selectedIndex >= 0) {
                StorageManager sm = new StorageManager();
                sm.readProfileAndCheckDifference(profiles.getValue(), sm);
                if (MainSingleton.getInstance().isRestartNeeded()) {
                    settingsController.setProfileButtonColor(true, 0);
                } else {
                    settingsController.setProfileButtonColor(false, Constants.TOOLTIP_DELAY);
                }
            }
            setProfileButtonContext();
        });
    }

    /**
     * Enable or disable profile buttons based on the current state
     */
    private void enableDisableProfileButtons() {
        String profileName = getFormattedProfileName();
        if (profileName.isEmpty()) {
            addProfileButton.setDisable(true);
            removeProfileButton.setDisable(true);
            applyProfileButton.setDisable(true);
        } else {
            addProfileButton.setDisable(false);
            removeProfileButton.setDisable(false);
            applyProfileButton.setDisable(false);
        }
        if (!profileName.isEmpty()) {
            StorageManager sm = new StorageManager();
            removeProfileButton.setDisable(!sm.checkIfFileExist(sm.getProfileFileName(profileName)));
            applyProfileButton.setDisable(!sm.checkIfFileExist(sm.getProfileFileName(profileName)));
            if (profileName.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                addProfileButton.setDisable(false);
                removeProfileButton.setDisable(true);
                applyProfileButton.setDisable(false);
            }
        }
    }

    /**
     * Init color mode listeners
     *
     * @param currentConfig current configuration
     */
    private void initColorModeListeners(Configuration currentConfig) {
        colorMode.valueProperty().addListener((_, _, _) -> {
            if (MainSingleton.getInstance().config != null) {
                MainSingleton.getInstance().config.setColorMode(colorMode.getSelectionModel().getSelectedIndex() + 1);
                MainSingleton.getInstance().guiManager.stopCapturingThreads(MainSingleton.getInstance().RUNNING);
                CommonUtility.delayMilliseconds(() -> {
                    if (MainSingleton.getInstance().config != null && MainSingleton.getInstance().config.isFullFirmware()) {
                        GlowWormDevice deviceToUse = CommonUtility.getDeviceToUse();
                        log.info("Setting Color Mode");
                        FirmwareConfigDto colorModeDto = new FirmwareConfigDto();
                        colorModeDto.setColorMode(String.valueOf(colorMode.getSelectionModel().getSelectedIndex() + 1));
                        colorModeDto.setMAC(deviceToUse.getMac());
                        NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_GLOW_WORM_FIRM_CONFIG), CommonUtility.toJsonString(colorModeDto));
                    }
                    CommonUtility.sleepMilliseconds(200);
                    turnOnLEDs(currentConfig, true);
                }, currentConfig.isFullFirmware() ? 200 : 0);
            }
        });
    }

    /**
     * Init color mode listeners
     *
     * @param currentConfig current configuration
     */
    private void initColorListeners(Configuration currentConfig) {
        // Toggle LED button listener
        toggleLed.setOnAction(_ -> {
            if ((toggleLed.isSelected())) {
                toggleLed.setText(CommonUtility.getWord(Constants.TURN_LED_OFF));
                turnOnLEDs(currentConfig, true);
                if (MainSingleton.getInstance().config != null) {
                    MainSingleton.getInstance().config.setToggleLed(true);
                }
            } else {
                toggleLed.setText(CommonUtility.getWord(Constants.TURN_LED_ON));
                CommonUtility.turnOffLEDs(currentConfig);
                if (MainSingleton.getInstance().config != null) {
                    MainSingleton.getInstance().config.setToggleLed(false);
                }
            }
            MainSingleton.getInstance().guiManager.trayIconManager.updateTray();
        });
        // Color picker listener
        EventHandler<ActionEvent> colorPickerEvent = _ -> turnOnLEDs(currentConfig, true);
        colorPicker.setOnAction(colorPickerEvent);
        effect.valueProperty().addListener((_, oldVal, newVal) -> {
            newVal = LocalizedEnum.fromStr(Enums.Effect.class, newVal).getBaseI18n();
            if (MainSingleton.getInstance().config != null) {
                if (!oldVal.equals(newVal)) {
                    if (audioDevice.getItems().isEmpty()) {
                        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, newVal);
                        initAudioComboWhenNeeded(effectInUse);
                    }
                    MainSingleton.getInstance().guiManager.stopCapturingThreads(MainSingleton.getInstance().RUNNING);
                    String finalNewVal = newVal;
                    CommonUtility.delayMilliseconds(() -> {
                        MainSingleton.getInstance().config.setEffect(finalNewVal);
                        ManagerSingleton.getInstance().lastEffectInUse = finalNewVal;
                        MainSingleton.getInstance().config.setToggleLed(true);
                        turnOnLEDs(currentConfig, true);
                    }, currentConfig.isFullFirmware() ? 200 : 0);
                }
                MainSingleton.getInstance().config.setEffect(newVal);
                setContextMenu();
            }
        });
        audioDevice.valueProperty().addListener((_, _, newVal) -> {
            MainSingleton.getInstance().config.setAudioDevice(newVal);
            PipelineManager.restartCapture(() -> log.info("Restarting capture due to audio device change"));
        });
    }

    /**
     * Init audio combo when needed
     */
    private void initAudioComboWhenNeeded(Enums.Effect effectInUse) {
        if (MainSingleton.getInstance().config != null) {
            if (effectInUse == Enums.Effect.MUSIC_MODE_RAINBOW || effectInUse == Enums.Effect.MUSIC_MODE_VU_METER
                    || effectInUse == Enums.Effect.MUSIC_MODE_VU_METER_DUAL || effectInUse == Enums.Effect.MUSIC_MODE_BRIGHT) {
                initAudioCombo();
            }
        }
    }

    /**
     * Init brightness and gamma listeners
     *
     * @param currentConfig current configuration
     */
    private void initBrightnessGammaListeners(Configuration currentConfig) {
        // Gamma can be changed on the fly
        gamma.valueProperty().addListener((_, _, gamma) -> MainSingleton.getInstance().config.setGamma(Double.parseDouble(gamma)));
        brightness.valueProperty().addListener((_, _, _) -> turnOnLEDs(currentConfig, false, true));
    }

    /**
     * Init white temp listeners
     *
     * @param currentConfig current configuration
     */
    private void initWhiteTempListeners(Configuration currentConfig) {
        // White temperature can be changed on the fly
        whiteTemp.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && whiteTemp.isFocused()) {
                turnOnLEDs(currentConfig, false);
            }
        });
        whiteTemp.setOnMouseReleased(_ -> turnOnLEDs(currentConfig, false));
    }

    /**
     * Init night mode listeners
     */
    private void initNightModeListeners() {
        nightModeFrom.valueProperty().addListener((_, _, newValue) -> {
            if (MainSingleton.getInstance().config != null) {
                MainSingleton.getInstance().config.setNightModeFrom(newValue.toString());
            }
        });
        nightModeTo.valueProperty().addListener((_, _, newValue) -> {
            if (MainSingleton.getInstance().config != null) {
                MainSingleton.getInstance().config.setNightModeTo(newValue.toString());
            }
        });
        nightModeBrightness.valueProperty().addListener((_, _, newValue) -> {
            if (MainSingleton.getInstance().config != null) {
                MainSingleton.getInstance().config.setNightModeBrightness(newValue);
            }
            enableDisableNightMode(newValue);
        });
    }

    /**
     * Turn ON LEDs
     *
     * @param currentConfig stored config
     * @param setBrightness brightness level
     */
    public void turnOnLEDs(Configuration currentConfig, boolean setBrightness) {
        turnOnLEDs(currentConfig, setBrightness, false);
    }

    /**
     * Turn ON LEDs
     *
     * @param currentConfig stored config
     * @param setBrightness brightness level
     */
    public void turnOnLEDs(Configuration currentConfig, boolean setBrightness, boolean changeBrightness) {
        if (setBrightness) {
            brightness.setValue((int) (colorPicker.getValue().getOpacity() * 100));
        } else {
            colorPicker.setValue(Color.rgb((int) (colorPicker.getValue().getRed() * 255), (int) (colorPicker.getValue().getGreen() * 255),
                    (int) (colorPicker.getValue().getBlue() * 255), (brightness.getValue() / 100)));
        }
        if (currentConfig != null) {
            if (toggleLed.isSelected() || !setBrightness) {
                CommonUtility.sleepMilliseconds(100);
                Enums.Effect effectInUse = LocalizedEnum.fromStr(Enums.Effect.class, effect.getValue());
                if (!MainSingleton.getInstance().RUNNING && (Enums.Effect.BIAS_LIGHT.equals(effectInUse)
                        || Enums.Effect.MUSIC_MODE_VU_METER.equals(effectInUse)
                        || Enums.Effect.MUSIC_MODE_VU_METER_DUAL.equals(effectInUse)
                        || Enums.Effect.MUSIC_MODE_BRIGHT.equals(effectInUse)
                        || Enums.Effect.MUSIC_MODE_RAINBOW.equals(effectInUse))) {
                    MainSingleton.getInstance().guiManager.startCapturingThreads();
                } else {
                    MainSingleton.getInstance().config.setBrightness((int) ((brightness.getValue() / 100) * 255));
                    if (currentConfig.isFullFirmware()) {
                        StateDto stateDto = getStateDto(changeBrightness, effectInUse);
                        NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT), CommonUtility.toJsonString(stateDto));
                    } else {
                        SerialManager serialManager = new SerialManager();
                        serialManager.sendSerialParams((int) (colorPicker.getValue().getRed() * 255),
                                (int) (colorPicker.getValue().getGreen() * 255),
                                (int) (colorPicker.getValue().getBlue() * 255));
                    }
                    MainSingleton.getInstance().config.setWhiteTemperature((int) (whiteTemp.getValue() / 100));
                }
            }
        }
    }

    /**
     * Set state dto
     *
     * @param changeBrightness true or false if a brightness change is needed
     * @param effectInUse      name of the effect to use
     * @return state dto
     */
    private StateDto getStateDto(boolean changeBrightness, Enums.Effect effectInUse) {
        StateDto stateDto = new StateDto();
        stateDto.setState(Constants.ON);
        if (!MainSingleton.getInstance().RUNNING) {
            stateDto.setEffect(effectInUse.getBaseI18n());
        } else {
            if (MainSingleton.getInstance().config.isWirelessStream()) {
                stateDto.setEffect(Constants.STATE_ON_GLOWWORMWIFI);
            } else {
                stateDto.setEffect(Constants.STATE_ON_GLOWWORM);
            }
        }
        ColorDto colorDto = getColorDto(changeBrightness);
        // RGB to 0 means that the LEDs must shut down
        if (colorDto.getR() == 0 && colorDto.getG() == 0 && colorDto.getB() == 0) {
            stateDto.setState(Constants.OFF);
            colorDto.setR(255);
            colorDto.setG(255);
            colorDto.setB(255);
        }
        stateDto.setColor(colorDto);
        stateDto.setBrightness(CommonUtility.getNightBrightness());
        stateDto.setWhitetemp((int) (whiteTemp.getValue() / 100));
        if (CommonUtility.getDeviceToUse() != null) {
            stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
        }
        return stateDto;
    }

    /**
     * Set color dto
     *
     * @param changeBrightness true or false if a brightness change is needed
     * @return color dto
     */
    private ColorDto getColorDto(boolean changeBrightness) {
        ColorDto colorDto = new ColorDto();
        int r = (int) (colorPicker.getValue().getRed() * 255);
        int g = (int) (colorPicker.getValue().getGreen() * 255);
        int b = (int) (colorPicker.getValue().getBlue() * 255);
        if (changeBrightness && MainSingleton.getInstance().RUNNING) {
            colorDto.setR(255);
            colorDto.setG(255);
            colorDto.setB(255);
        } else {
            colorDto.setR(r);
            colorDto.setG(g);
            colorDto.setB(b);
        }
        return colorDto;
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
    @SuppressWarnings("Duplicates")
    public void save(Configuration config) {
        config.setGamma(Double.parseDouble(gamma.getValue()));
        config.setColorMode(colorMode.getSelectionModel().getSelectedIndex() + 1);
        if (framerate.getValue().isEmpty()) {
            framerate.setValue(Constants.DEFAULT_FRAMERATE);
            config.setDesiredFramerate(Constants.DEFAULT_FRAMERATE);
        } else {
            setFramerateIntoConfig(config);
        }
        var smooth = LocalizedEnum.fromStr(Enums.Smoothing.class, smoothing.getValue());
        config.setSmoothingType(smooth.getBaseI18n());
        if (MainSingleton.getInstance().config != null) {
            config.setFrameInsertionTarget(MainSingleton.getInstance().config.getFrameInsertionTarget());
            config.setEmaAlpha(MainSingleton.getInstance().config.getEmaAlpha());
        } else {
            config.setFrameInsertionTarget(smooth.getFrameInsertionFramerate());
            config.setEmaAlpha(smooth.getEmaAlpha());
        }
        config.setToggleLed(toggleLed.isSelected());
        config.setNightModeFrom(nightModeFrom.getValue().toString());
        config.setNightModeTo(nightModeTo.getValue().toString());
        config.setNightModeBrightness(nightModeBrightness.getValue());
        config.setBrightness((int) (brightness.getValue() / 100 * 255));
        config.setWhiteTemperature((int) (whiteTemp.getValue() / 100));
        config.setAudioChannels(LocalizedEnum.fromStr(Enums.AudioChannels.class, audioChannels.getValue()).getBaseI18n());
        config.setAudioLoopbackGain((float) audioGain.getValue());
        var audioDeviceFromConfig = LocalizedEnum.fromStr(Enums.Audio.class, audioDevice.getValue());
        String audioDeviceToStore;
        if (audioDeviceFromConfig != null) {
            audioDeviceToStore = audioDeviceFromConfig.getBaseI18n();
        } else {
            audioDeviceToStore = audioDevice.getValue();
        }
        config.setAudioDevice(audioDeviceToStore);
        config.setEffect(LocalizedEnum.fromStr(Enums.Effect.class, effect.getValue()).getBaseI18n());
        config.setColorChooser((int) (colorPicker.getValue().getRed() * 255) + "," + (int) (colorPicker.getValue().getGreen() * 255) + ","
                + (int) (colorPicker.getValue().getBlue() * 255) + "," + (int) (colorPicker.getValue().getOpacity() * 255));
    }

    /**
     * Add profile event
     *
     * @param e event
     */
    @FXML
    @SuppressWarnings("unused")
    public void addProfile(InputEvent e) {
        profiles.commitValue();
        saveUsingProfile(e);
        MainSingleton.getInstance().profileArg = profiles.getValue();
        if (MainSingleton.getInstance().config.isMqttEnable()) {
            NetworkTabController.publishDiscoveryTopic(new SelectProfileDiscovery(), false);
            NetworkTabController.publishDiscoveryTopic(new SelectProfileDiscovery(), true);
        }
        setProfileButtonContext();
        enableDisableProfileButtons();
    }

    /**
     * Remove profile event
     *
     * @param e event
     */
    @FXML
    @SuppressWarnings("unused")
    public void removeProfile(InputEvent e) {
        String profileName = profiles.getValue();
        if (!profileName.equals(CommonUtility.getWord(Constants.DEFAULT))) {
            profiles.getItems().remove(profileName);
            profiles.commitValue();
            StorageManager sm = new StorageManager();
            if (sm.deleteProfile(profileName)) {
                MainSingleton.getInstance().guiManager.trayIconManager.updateTray();
            }
        }
        if (MainSingleton.getInstance().config.isMqttEnable()) {
            NetworkTabController.publishDiscoveryTopic(new SelectProfileDiscovery(), false);
            NetworkTabController.publishDiscoveryTopic(new SelectProfileDiscovery(), true);
        }
    }

    /**
     * Apply profile event
     *
     * @param e event
     */
    @FXML
    @SuppressWarnings("unused")
    public void applyProfile(InputEvent e) {
        MainSingleton.getInstance().profileArg = profiles.getValue();
        int selectedIndex = profiles.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            MainSingleton.getInstance().guiManager.trayIconManager.manageProfileListener(getFormattedProfileName());
            settingsController.refreshValuesOnScene();
        }
        setProfileButtonContext();
    }

    /**
     * Set the text of the apply profile button based on the current profile
     */
    private void setProfileButtonContext() {
        if (NativeExecutor.isWindows()) {
            if (!MainSingleton.getInstance().profileArg.equals(CommonUtility.getWord(Constants.DEFAULT)) && MainSingleton.getInstance().profileArg.equals(profiles.getValue())) {
                applyProfileButton.setText(Constants.DIALOG);
                applyProfileButton.setOnMouseClicked(this::openProfileDialog);
            } else {
                applyProfileButton.setText(Constants.CHECK);
                applyProfileButton.setOnMouseClicked(this::applyProfile);
            }
        }
    }

    /**
     * Show color correction dialog
     */
    @FXML
    public void openCCDialog(InputEvent e) {
        if (MainSingleton.getInstance().guiManager != null && MainSingleton.getInstance().config != null) {
            MainSingleton.getInstance().guiManager.showColorCorrectionDialog(settingsController, e);
        }
    }

    /**
     * Show eye care dialog
     */
    @FXML
    public void openEyeCareDialog() {
        if (MainSingleton.getInstance().guiManager != null) {
            MainSingleton.getInstance().guiManager.showEyeCareDialog(settingsController);
        }
    }

    /**
     * Show profile dialog
     */
    @FXML
    public void openProfileDialog(MouseEvent mouseEvent) {
        if (MainSingleton.getInstance().guiManager != null) {
            MainSingleton.getInstance().guiManager.showProfileDialog(settingsController);
        }
    }

    /**
     * Show smoothing dialog
     */
    @FXML
    public void openSmoothingDialog() {
        if (MainSingleton.getInstance().guiManager != null) {
            MainSingleton.getInstance().guiManager.showSmoothingDialog(settingsController);
        }
    }

    /**
     * Save to config file using profiles
     *
     * @param e action event
     */
    private void saveUsingProfile(InputEvent e) {
        String profileName = getFormattedProfileName();
        if (!profileName.isEmpty()) {
            String fileToWrite = profileName;
            if (profileName.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                switch (MainSingleton.getInstance().whoAmI) {
                    case 1 -> fileToWrite = Constants.CONFIG_FILENAME;
                    case 2 -> fileToWrite = Constants.CONFIG_FILENAME_2;
                    case 3 -> fileToWrite = Constants.CONFIG_FILENAME_3;
                }
            } else {
                fileToWrite = MainSingleton.getInstance().whoAmI + "_" + fileToWrite + Constants.YAML_EXTENSION;
            }
            settingsController.save(e, fileToWrite);
            profiles.getItems().removeIf(value -> value.equals(profileName));
            profiles.getItems().add(profileName);
            profiles.setValue(profileName);
            profiles.commitValue();
            MainSingleton.getInstance().guiManager.trayIconManager.updateTray();
        }
    }

    /**
     * Create a profile name that does not overwrite the reserved tray icon items and format the name
     *
     * @return profile name
     */
    private String getFormattedProfileName() {
        String profile = profiles.getValue() != null ? profiles.getValue().toLowerCase() : "";
        profile = CommonUtility.capitalize(profile);
        if (CommonUtility.getWord(Constants.STOP).equals(profile)
                || CommonUtility.getWord(Constants.START).equals(profile)
                || CommonUtility.getWord(Constants.SETTINGS).equals(profile)
                || CommonUtility.getWord(Constants.INFO).equals(profile)) {
            return profile + " ";
        }
        return profile;
    }

    /**
     * Enable or disable features if a LDR is in use
     */
    public void evaluateLDRConnectedFeatures() {
        if (MainSingleton.getInstance().config.isEnableLDR()) {
            brightness.setDisable(true);
            WidgetFactory widgetFactory = new WidgetFactory();
            MainSingleton.getInstance().config.setNightModeBrightness(Constants.PERCENTAGE_OFF);
            nightModeBrightness.setValueFactory(widgetFactory.spinnerNightModeValueFactory());
            nightModeBrightness.setDisable(true);
            enableDisableNightMode(Constants.PERCENTAGE_OFF);
        } else {
            brightness.setDisable(false);
            nightModeBrightness.setDisable(false);
        }
    }

    /**
     * Smoothing management
     */
    public void evaluateSmoothing() {
        var smooth = Enums.Smoothing.findByFramerateAndAlpha(MainSingleton.getInstance().config.getFrameInsertionTarget(), MainSingleton.getInstance().config.getEmaAlpha());
        smoothing.setValue(smooth.getI18n());
        setFramerateEditable(smooth);
    }

    /**
     * Set form tooltips
     *
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        GuiManager.createTooltip(Constants.TOOLTIP_GAMMA, gamma);
        GuiManager.createTooltip(Constants.TOOLTIP_FRAMERATE, framerate);
        GuiManager.createTooltip(Constants.TOOLTIP_SMOOTHING, smoothing);
        GuiManager.createTooltip(Constants.TOOLTIP_BRIGHTNESS, brightness);
        GuiManager.createTooltip(Constants.TOOLTIP_AUDIO_DEVICE, audioDevice);
        GuiManager.createTooltip(Constants.TOOLTIP_AUDIO_CHANNELS, audioChannels);
        GuiManager.createTooltip(Constants.TOOLTIP_AUDIO_GAIN, audioGain);
        GuiManager.createTooltip(Constants.TOOLTIP_EFFECT, effect);
        GuiManager.createTooltip(Constants.TOOLTIP_COLORS, colorPicker);
        GuiManager.createTooltip(Constants.TOOLTIP_NIGHT_MODE_FROM, nightModeFrom);
        GuiManager.createTooltip(Constants.TOOLTIP_NIGHT_MODE_TO, nightModeTo);
        GuiManager.createTooltip(Constants.TOOLTIP_NIGHT_MODE_BRIGHT, nightModeBrightness);
        GuiManager.createTooltip(Constants.TOOLTIP_WHITE_TEMP, whiteTemp);
        GuiManager.createTooltip(Constants.TOOLTIP_COLOR_MODE, colorMode);
        if (currentConfig == null) {
            GuiManager.createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON_NULL, saveMiscButton);
        }
        GuiManager.createTooltip(Constants.TOOLTIP_PROFILES, profiles);
        GuiManager.createTooltip(Constants.TOOLTIP_PROFILES_REMOVE, removeProfileButton);
        GuiManager.createTooltip(Constants.TOOLTIP_PROFILES_ADD, addProfileButton);
        GuiManager.createTooltip(Constants.TOOLTIP_PROFILES_APPLY, applyProfileButton);
        GuiManager.createTooltip(Constants.SHOW_MORE_SETTINGS, smoothingBtn);
        GuiManager.createTooltip(Constants.SHOW_MORE_SETTINGS, eyeCareBtn);
    }

    /**
     * Force framerate validation
     *
     * @param newValue combobox new value
     */
    private void forceFramerateValidation(String newValue) {
        if (MainSingleton.getInstance().config != null) {
            framerate.cancelEdit();
            if (LocalizedEnum.fromStr(Enums.Framerate.class, framerate.getValue()) != Enums.Framerate.UNLOCKED) {
                String val = CommonUtility.removeChars(newValue);
                if (newValue.contains(Constants.FPS_VAL)) {
                    val += Constants.FPS_VAL;
                }
                framerate.getItems().set(0, val);
                framerate.setValue(val);
            } else {
                Platform.runLater(() -> framerate.setValue(newValue));
            }
        }
    }

    /**
     * Manage smoothing combo
     */
    private void manageSmoothingCombo(String newVal) {
        if (MainSingleton.getInstance().config != null) {
            Enums.Smoothing smooth = LocalizedEnum.fromBaseStr(Enums.Smoothing.class, newVal);
            if (smooth == null) {
                smooth = LocalizedEnum.fromStr(Enums.Smoothing.class, newVal);
            }
            MainSingleton.getInstance().config.setSmoothingType(smooth.getBaseI18n());
            if (smooth == Enums.Smoothing.DISABLED) {
                MainSingleton.getInstance().config.setFrameInsertionTarget(0);
                MainSingleton.getInstance().config.setEmaAlpha(0F);
                MainSingleton.getInstance().config.setSmoothingTargetFramerate(Constants.DEFAULT_SMOOTHING_TARGET);
            } else if (smooth == Enums.Smoothing.CUSTOM) {
                MainSingleton.getInstance().config.setFrameInsertionTarget(MainSingleton.getInstance().config.getFrameInsertionTarget());
                MainSingleton.getInstance().config.setEmaAlpha(MainSingleton.getInstance().config.getEmaAlpha());
                MainSingleton.getInstance().config.setSmoothingTargetFramerate(MainSingleton.getInstance().config.getSmoothingTargetFramerate());
            } else {
                MainSingleton.getInstance().config.setFrameInsertionTarget(smooth.getFrameInsertionFramerate());
                MainSingleton.getInstance().config.setEmaAlpha(smooth.getEmaAlpha());
                MainSingleton.getInstance().config.setSmoothingTargetFramerate(Constants.DEFAULT_SMOOTHING_TARGET);
            }
            setFramerateEditable(smooth);
            if (settingsController != null && settingsController.smoothingDialogController != null) {
                settingsController.smoothingDialogController.initValuesFromSettingsFile(MainSingleton.getInstance().config);
            }
            PipelineManager.restartCapture(CommonUtility::run);
        }
    }

    /**
     * Enable or disable framerate combo
     *
     * @param smooth smoothing in use
     * @return if framerate must be disabled
     */
    private boolean setFramerateEditable(Enums.Smoothing smooth) {
        boolean disable = true;
        if (smooth == Enums.Smoothing.DISABLED || smooth == Enums.Smoothing.SMOOTHING_LVL_1) {
            disable = false;
        } else if (MainSingleton.getInstance().config.getFrameInsertionTarget() == 0) {
            disable = false;
        }
        framerate.setDisable(disable);
        return disable;
    }

}
