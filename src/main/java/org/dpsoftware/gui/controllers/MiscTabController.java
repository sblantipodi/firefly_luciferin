/*
  MiscTabController.java

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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.AudioLoopback;
import org.dpsoftware.audio.AudioLoopbackSoftware;
import org.dpsoftware.audio.AudioUtility;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.WidgetFactory;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.dto.*;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Misc Tab controller
 */
@Slf4j
public class MiscTabController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    // FXML binding
    @FXML private Label contextChooseColorChooseLoopback;
    @FXML public ColorPicker colorPicker;
    @FXML public ToggleButton toggleLed;
    @FXML public CheckBox startWithSystem;
    @FXML public ComboBox<String> framerate;
    @FXML public Slider brightness;
    @FXML private Label contextGammaGain;
    @FXML public ComboBox<String> colorMode;
    @FXML public ComboBox<String> gamma;
    @FXML public ComboBox<String> effect;
    @FXML public Slider audioGain;
    @FXML public ComboBox<String> audioChannels;
    @FXML public ComboBox<String> audioDevice;
    @FXML public CheckBox eyeCare;
    @FXML public Slider whiteTemp;
    @FXML public Spinner<LocalTime> nightModeFrom;
    @FXML public Spinner<LocalTime> nightModeTo;
    @FXML public Spinner<String> nightModeBrightness;
    @FXML public Button saveMiscButton;
    @FXML RowConstraints runLoginRow;
    @FXML Label runAtLoginLabel;


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

        if (FireflyLuciferin.config == null) {
            colorMode.setDisable(true);
            whiteTemp.setDisable(true);
        }
        if (NativeExecutor.isLinux()) {
            runLoginRow.setPrefHeight(0);
            runLoginRow.setMinHeight(0);
            runLoginRow.setPercentHeight(0);
            runAtLoginLabel.setVisible(false);
            startWithSystem.setVisible(false);
        }
        if (NativeExecutor.isWindows()) {
            audioDevice.getItems().add(Constants.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getI18n());
        }
        audioDevice.getItems().add(Constants.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getI18n());
        if (FireflyLuciferin.config != null && AudioLoopback.audioDevices.isEmpty()) {
            AudioUtility audioLoopback = new AudioLoopbackSoftware();
            for (AudioDevice device : audioLoopback.getLoopbackDevices().values()) {
                if (device.getDeviceName().contains(Constants.LOOPBACK)) audioDevice.getItems().add(device.getDeviceName());
            }
        } else {
            for (AudioDevice device : AudioLoopback.audioDevices.values()) {
                if (device.getDeviceName().contains(Constants.LOOPBACK)) audioDevice.getItems().add(device.getDeviceName());
            }
        }
        for (Constants.Framerate fps : Constants.Framerate.values()) {
            if (fps.getBaseI18n().equals(Constants.Framerate.UNLOCKED.getBaseI18n())) {
                framerate.getItems().add(fps.getI18n());
            } else {
                framerate.getItems().add(fps.getI18n() + " FPS");
            }
        }

    }

    /**
     * Init combo boxes
     */
    void initComboBox() {

        for (Constants.Gamma gma : Constants.Gamma.values()) {
            gamma.getItems().add(gma.getGamma());
        }
        for (Constants.Effect ef : Constants.Effect.values()) {
            effect.getItems().add(ef.getI18n());
        }
        for (Constants.AudioChannels audioChan : Constants.AudioChannels.values()) {
            audioChannels.getItems().add(audioChan.getI18n());
        }
        for (Constants.ColorMode colorModeVal : Constants.ColorMode.values()) {
            colorMode.getItems().add(colorModeVal.getI18n());
        }

    }

    /**
     * Init form values
     */
    void initDefaultValues() {

        gamma.setValue(Constants.GAMMA_DEFAULT);
        colorMode.setValue(Constants.ColorMode.RGB_MODE.getI18n());
        effect.setValue(Constants.Effect.BIAS_LIGHT.getI18n());
        framerate.setValue(Constants.Framerate.FPS_30.getI18n() + " FPS");
        toggleLed.setSelected(true);
        brightness.setValue(255);
        audioGain.setVisible(false);
        audioDevice.setVisible(false);
        audioChannels.setVisible(false);
        audioChannels.setValue(Constants.AudioChannels.AUDIO_CHANNEL_2.getI18n());
        if (NativeExecutor.isWindows()) {
            audioDevice.setValue(Constants.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getI18n());
        } else {
            audioDevice.setValue(Constants.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getI18n());
        }
        WidgetFactory widgetFactory = new WidgetFactory();
        nightModeFrom.setValueFactory(widgetFactory.timeSpinnerValueFactory(LocalTime.now().withHour(22).withMinute(0).truncatedTo(ChronoUnit.MINUTES)));
        nightModeTo.setValueFactory(widgetFactory.timeSpinnerValueFactory(LocalTime.now().withHour(8).withMinute(0).truncatedTo(ChronoUnit.MINUTES)));
        nightModeBrightness.setValueFactory(widgetFactory.spinnerNightModeValueFactory());
        enableDisableNightMode(Constants.NIGHT_MODE_OFF);
        startWithSystem.setSelected(true);

    }

    /**
     * Toggle night mode params
     * @param nightModeBrightness brightness param for night mode
     */
    public void enableDisableNightMode(String nightModeBrightness) {

        if (nightModeBrightness.equals(Constants.NIGHT_MODE_OFF)) {
            nightModeFrom.setDisable(true);
            nightModeTo.setDisable(true);
        } else {
            nightModeFrom.setDisable(false);
            nightModeTo.setDisable(false);
        }

    }

    /**
     * Init form values by reading existing config file
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {

        if (NativeExecutor.isWindows()) {
            startWithSystem.setSelected(currentConfig.isStartWithSystem());
        }
        gamma.setValue(String.valueOf(currentConfig.getGamma()));
        colorMode.setValue(Constants.ColorMode.values()[FireflyLuciferin.config.getColorMode() - 1].getI18n());
        if (!currentConfig.getDesiredFramerate().equals(Constants.Framerate.UNLOCKED.getBaseI18n())) {
            framerate.setValue(currentConfig.getDesiredFramerate() + " FPS");
        } else {
            framerate.setValue(LocalizedEnum.fromBaseStr(Constants.Framerate.class, currentConfig.getDesiredFramerate()).getI18n());
        }
        eyeCare.setSelected(currentConfig.isEyeCare());
        String[] color = (FireflyLuciferin.config.getColorChooser().equals(Constants.DEFAULT_COLOR_CHOOSER)) ?
                currentConfig.getColorChooser().split(",") : FireflyLuciferin.config.getColorChooser().split(",");
        colorPicker.setValue(Color.rgb(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]), Double.parseDouble(color[3])/255));
        brightness.setValue((Double.parseDouble(color[3])/255)*100);
        whiteTemp.setValue(FireflyLuciferin.config.getWhiteTemperature() * 100);
        audioGain.setValue(currentConfig.getAudioLoopbackGain());
        audioChannels.setValue(LocalizedEnum.fromBaseStr(Constants.AudioChannels.class, currentConfig.getAudioChannels()).getI18n());
        var audioDeviceFromStore = LocalizedEnum.fromBaseStr(Constants.Audio.class, currentConfig.getAudioDevice());
        String audioDeviceToDisplay;
        if (audioDeviceFromStore != null) {
            audioDeviceToDisplay = audioDeviceFromStore.getI18n();
        } else {
            audioDeviceToDisplay = currentConfig.getAudioDevice();
        }
        audioDevice.setValue(audioDeviceToDisplay);
        effect.setValue(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()).getI18n());
        if (FireflyLuciferin.config.isToggleLed()) {
            toggleLed.setText(CommonUtility.getWord(Constants.TURN_LED_OFF));
        } else {
            toggleLed.setText(CommonUtility.getWord(Constants.TURN_LED_ON));
        }
        toggleLed.setSelected(FireflyLuciferin.config.isToggleLed());
        WidgetFactory widgetFactory = new WidgetFactory();
        nightModeFrom.setValueFactory(widgetFactory.timeSpinnerValueFactory(LocalTime.parse(FireflyLuciferin.config.getNightModeFrom())));
        nightModeTo.setValueFactory(widgetFactory.timeSpinnerValueFactory(LocalTime.parse(FireflyLuciferin.config.getNightModeTo())));
        nightModeBrightness.setValueFactory(widgetFactory.spinnerNightModeValueFactory());
        enableDisableNightMode(nightModeBrightness.getValue());

    }

    /**
     * Setup the context menu based on the selected effect
     */
    public void setContextMenu() {

        if (Constants.Effect.MUSIC_MODE_VU_METER.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                || Constants.Effect.MUSIC_MODE_VU_METER_DUAL.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                || Constants.Effect.MUSIC_MODE_BRIGHT.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                || Constants.Effect.MUSIC_MODE_RAINBOW.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect())))  {
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
     * Send serialParams, this will cause a reboot on the microcontroller
     */
    void sendSerialParams() {

        java.awt.Color[] leds = new java.awt.Color[1];
        try {
            leds[0] = new java.awt.Color((int)(colorPicker.getValue().getRed() * 255),
                    (int)(colorPicker.getValue().getGreen() * 255),
                    (int)(colorPicker.getValue().getBlue() * 255));
            FireflyLuciferin.sendColorsViaUSB(leds);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }

    /**
     * Init all the settings listener
     * @param currentConfig stored config
     */
    public void initListeners(Configuration currentConfig) {

        // Toggle LED button listener
        toggleLed.setOnAction(e -> {
            if ((toggleLed.isSelected())) {
                toggleLed.setText(CommonUtility.getWord(Constants.TURN_LED_OFF));
                turnOnLEDs(currentConfig, true);
                if (FireflyLuciferin.config != null) {
                    FireflyLuciferin.config.setToggleLed(true);
                }
            } else {
                toggleLed.setText(CommonUtility.getWord(Constants.TURN_LED_ON));
                settingsController.turnOffLEDs(currentConfig);
                if (FireflyLuciferin.config != null) {
                    FireflyLuciferin.config.setToggleLed(false);
                }
            }
        });
        // Color picker listener
        EventHandler<ActionEvent> colorPickerEvent = e -> turnOnLEDs(currentConfig, true);
        colorPicker.setOnAction(colorPickerEvent);
        // Gamma can be changed on the fly
        gamma.valueProperty().addListener((ov, t, gamma) -> {
            if (currentConfig != null && currentConfig.isWifiEnable()) {
                GammaDto gammaDto = new GammaDto();
                gammaDto.setGamma(Double.parseDouble(gamma));
                MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_GAMMA),
                        CommonUtility.toJsonString(gammaDto));
            }
            FireflyLuciferin.config.setGamma(Double.parseDouble(gamma));
        });
        brightness.valueProperty().addListener((ov, oldVal, newVal) -> turnOnLEDs(currentConfig, false));
        // White temperature can be changed on the fly
        whiteTemp.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && whiteTemp.isFocused()) {
                turnOnLEDs(currentConfig, false);
            }
        });
        whiteTemp.setOnMouseReleased(event -> turnOnLEDs(currentConfig, false));
        audioGain.valueProperty().addListener((ov, oldVal, newVal) -> {
            DecimalFormat df = new DecimalFormat(Constants.NUMBER_FORMAT);
            float selectedGain = Float.parseFloat(df.format(newVal).replace(",","."));
            FireflyLuciferin.config.setAudioLoopbackGain(selectedGain);
        });
        effect.valueProperty().addListener((ov, oldVal, newVal) -> {
            newVal = LocalizedEnum.fromStr(Constants.Effect.class, newVal).getBaseI18n();
            if (FireflyLuciferin.config != null) {
                if (!oldVal.equals(newVal)) {
                    FireflyLuciferin.guiManager.stopCapturingThreads(FireflyLuciferin.RUNNING);
                    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                    String finalNewVal = newVal;
                    executor.schedule(() -> {
                        FireflyLuciferin.config.setEffect(finalNewVal);
                        PipelineManager.lastEffectInUse = finalNewVal;
                        FireflyLuciferin.config.setToggleLed(true);
                        turnOnLEDs(currentConfig, true);
                    }, currentConfig.isWifiEnable() ? 200 : 0, TimeUnit.MILLISECONDS);
                }
                FireflyLuciferin.config.setEffect(newVal);
                setContextMenu();
            }
        });
        nightModeFrom.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (FireflyLuciferin.config != null) {
                FireflyLuciferin.config.setNightModeFrom(newValue.toString());
            }
        });
        nightModeTo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (FireflyLuciferin.config != null) {
                FireflyLuciferin.config.setNightModeTo(newValue.toString());
            }
        });
        nightModeBrightness.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (FireflyLuciferin.config != null) {
                FireflyLuciferin.config.setNightModeBrightness(newValue);
            }
            enableDisableNightMode(newValue);
        });
        colorMode.valueProperty().addListener((ov, oldVal, newVal) -> {
            if (FireflyLuciferin.config != null) {
                FireflyLuciferin.config.setColorMode(colorMode.getSelectionModel().getSelectedIndex() + 1);
                FireflyLuciferin.guiManager.stopCapturingThreads(FireflyLuciferin.RUNNING);
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                executor.schedule(() -> {
                    if (FireflyLuciferin.config != null && FireflyLuciferin.config.isWifiEnable()) {
                        GlowWormDevice deviceToUse = CommonUtility.getDeviceToUse();
                        log.debug("Setting Color Mode");
                        FirmwareConfigDto colorModeDto = new FirmwareConfigDto();
                        colorModeDto.setColorMode(colorMode.getSelectionModel().getSelectedIndex() + 1);
                        colorModeDto.setMAC(deviceToUse.getMac());
                        MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FIRMWARE_CONFIG), CommonUtility.toJsonString(colorModeDto));
                    }
                    CommonUtility.sleepMilliseconds(200);
                    turnOnLEDs(currentConfig, true);
                }, currentConfig.isWifiEnable() ? 200 : 0, TimeUnit.MILLISECONDS);
            }
        });

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
        if (currentConfig != null) {
            if (toggleLed.isSelected() || !setBrightness) {
                CommonUtility.sleepMilliseconds(100);
                if (!FireflyLuciferin.RUNNING && (Constants.Effect.BIAS_LIGHT.equals(LocalizedEnum.fromStr(Constants.Effect.class, effect.getValue()))
                        || Constants.Effect.MUSIC_MODE_VU_METER.equals(LocalizedEnum.fromStr(Constants.Effect.class, effect.getValue()))
                        || Constants.Effect.MUSIC_MODE_VU_METER_DUAL.equals(LocalizedEnum.fromStr(Constants.Effect.class, effect.getValue()))
                        || Constants.Effect.MUSIC_MODE_BRIGHT.equals(LocalizedEnum.fromStr(Constants.Effect.class, effect.getValue()))
                        || Constants.Effect.MUSIC_MODE_RAINBOW.equals(LocalizedEnum.fromStr(Constants.Effect.class, effect.getValue())))) {
                    FireflyLuciferin.guiManager.startCapturingThreads();
                } else {
                    FireflyLuciferin.config.setBrightness((int)((brightness.getValue() / 100) * 255));
                    if (currentConfig.isWifiEnable()) {
                        StateDto stateDto = new StateDto();
                        stateDto.setState(Constants.ON);
                        if (!FireflyLuciferin.RUNNING) {
                            stateDto.setEffect(LocalizedEnum.fromStr(Constants.Effect.class, effect.getValue()).getBaseI18n().toLowerCase());
                        }
                        ColorDto colorDto = new ColorDto();
                        colorDto.setR((int)(colorPicker.getValue().getRed() * 255));
                        colorDto.setG((int)(colorPicker.getValue().getGreen() * 255));
                        colorDto.setB((int)(colorPicker.getValue().getBlue() * 255));
                        stateDto.setColor(colorDto);
                        stateDto.setBrightness(CommonUtility.getNightBrightness());
                        stateDto.setWhitetemp((int) (whiteTemp.getValue() / 100));
                        if (CommonUtility.getDeviceToUse() != null) {
                            stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
                        }
                        MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.toJsonString(stateDto));
                    } else {
                        sendSerialParams();
                    }
                    FireflyLuciferin.config.setWhiteTemperature((int) (whiteTemp.getValue() / 100));
                }
            }
        }

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

        config.setGamma(Double.parseDouble(gamma.getValue()));
        config.setColorMode(colorMode.getSelectionModel().getSelectedIndex() + 1);
        config.setDesiredFramerate(LocalizedEnum.fromStr(Constants.Framerate.class, framerate.getValue().replaceAll(" FPS", "")).getBaseI18n());
        config.setEyeCare(eyeCare.isSelected());
        config.setToggleLed(toggleLed.isSelected());
        config.setNightModeFrom(nightModeFrom.getValue().toString());
        config.setNightModeTo(nightModeTo.getValue().toString());
        config.setNightModeBrightness(nightModeBrightness.getValue());
        config.setBrightness((int) (brightness.getValue()/100 *255));
        config.setWhiteTemperature((int) (whiteTemp.getValue() / 100));
        config.setAudioChannels(LocalizedEnum.fromStr(Constants.AudioChannels.class, audioChannels.getValue()).getBaseI18n());
        config.setAudioLoopbackGain((float) audioGain.getValue());
        var audioDeviceFromConfig = LocalizedEnum.fromBaseStr(Constants.Audio.class, audioDevice.getValue());
        String audioDeviceToStore;
        if (audioDeviceFromConfig != null) {
            audioDeviceToStore = audioDeviceFromConfig.getBaseI18n();
        } else {
            audioDeviceToStore = audioDevice.getValue();
        }
        config.setAudioDevice(audioDeviceToStore);
        config.setEffect(LocalizedEnum.fromStr(Constants.Effect.class, effect.getValue()).getBaseI18n());
        config.setColorChooser((int)(colorPicker.getValue().getRed()*255) + "," + (int)(colorPicker.getValue().getGreen()*255) + ","
                + (int)(colorPicker.getValue().getBlue()*255) + "," + (int)(colorPicker.getValue().getOpacity()*255));

    }

    /**
     * Set form tooltips
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {

        gamma.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_GAMMA));
        if (NativeExecutor.isWindows()) {
            startWithSystem.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_START_WITH_SYSTEM));
        }
        framerate.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_FRAMERATE));
        eyeCare.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_EYE_CARE));
        brightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_BRIGHTNESS));
        audioDevice.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_AUDIO_DEVICE));
        audioChannels.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_AUDIO_CHANNELS));
        audioGain.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_AUDIO_GAIN));
        effect.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_EFFECT));
        colorPicker.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_COLORS));
        nightModeFrom.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_NIGHT_MODE_FROM));
        nightModeTo.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_NIGHT_MODE_TO));
        nightModeBrightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_NIGHT_MODE_BRIGHT));
        whiteTemp.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_WHITE_TEMP));
        colorMode.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_COLOR_MODE));
        if (currentConfig == null) {
            saveMiscButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON_NULL));
        } else {
            saveMiscButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON,200, 6000));
        }

    }

}
