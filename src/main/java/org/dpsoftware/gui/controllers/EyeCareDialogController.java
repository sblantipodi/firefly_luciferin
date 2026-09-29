/*
  EyeCareDialogController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.GrabberSingleton;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.WidgetFactory;
import org.dpsoftware.gui.controllers.options.EyeCareOptions;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Eye care dialog controller
 */
@Slf4j
public class EyeCareDialogController {

    @FXML
    private final StringProperty ldrValue = new SimpleStringProperty("");
    @FXML
    public CheckBox enableLDR;
    @FXML
    public CheckBox ldrTurnOff;
    @FXML
    public ComboBox<String> brightnessLimiter;
    @FXML
    public ComboBox<String> ldrInterval;
    @FXML
    public ComboBox<String> nightLight;
    @FXML
    public ComboBox<String> minimumBrightness;
    @FXML
    public Button calibrateLDR;
    @FXML
    public Button resetLDR;
    @FXML
    public Button okButton;
    @FXML
    public Button applyButton;
    @FXML
    public Button cancelButton;
    @FXML
    public Spinner<String> luminosityThreshold;
    @FXML
    public Spinner<Integer> nightLightLvl;
    ScheduledExecutorService scheduledExecutorService;
    // Inject main controller
    @FXML
    private SettingsController settingsController;
    @FXML
    private Label ldrLabel;

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
        Platform.runLater(() -> {
            ldrValue.setValue(Constants.DASH);
            for (int i : EyeCareOptions.minimumBrightnessValues()) {
                minimumBrightness.getItems().add(i + Constants.PERCENT);
            }
            for (Enums.BrightnessLimiter brightnessLimit : EyeCareOptions.brightnessLimiters()) {
                brightnessLimiter.getItems().add(brightnessLimit.getI18n());
            }
            for (Enums.LdrInterval ldrVal : EyeCareOptions.ldrIntervals()) {
                ldrInterval.getItems().add(ldrVal.getI18n());
            }
            for (Enums.NightLight nightLightVal : Enums.NightLight.values()) {
                nightLight.getItems().add(nightLightVal.getI18n());
            }
            if (MainSingleton.getInstance().config.isFullFirmware()) {
                EyeCareOptions.firmwareLdrControls().ifPresent(controls -> {
                    enableLDR.setSelected(controls.enabled());
                    ldrTurnOff.setSelected(controls.turnOff());
                    ldrInterval.setValue(Enums.LdrInterval.findByValue(controls.interval()).getI18n());
                    minimumBrightness.setValue(controls.minimum() + Constants.PERCENT);
                });
            }
            ldrLabel.textProperty().bind(ldrValueProperty());
            startAnimationTimer();
            enableLDR.setOnAction(_ -> evaluateValues());
            ldrInterval.setOnAction(_ -> evaluateValues());
            WidgetFactory widgetFactory = new WidgetFactory();
            luminosityThreshold.setValueFactory(widgetFactory.luminosityThresholdValueFactory());
            nightLightLvl.setValueFactory(widgetFactory.nightLightLvlValueFactory());
            brightnessLimiter.valueProperty().addListener((_, _, _) ->
                    MainSingleton.getInstance().config.setBrightnessLimiter(LocalizedEnum.fromStr(Enums.BrightnessLimiter.class, brightnessLimiter.getValue()).getBrightnessLimitFloat()));
            nightLight.valueProperty().addListener((_, _, _) -> {
                MainSingleton.getInstance().config.setNightLight(LocalizedEnum.fromStr(Enums.NightLight.class, nightLight.getValue()).getBaseI18n());
                handleNightLightExecutor();
                handleNightLvl(MainSingleton.getInstance().config);
            });
            evaluateValues();
        });
    }

    /**
     * Set tooltips
     */
    private void setTooltips() {
        GuiManager.createTooltip(LabelKey.TOOLTIP_EYEC_ENABLE_LDR, enableLDR);
        GuiManager.createTooltip(LabelKey.TOOLTIP_EYEC_TURNOFF, ldrTurnOff);
        GuiManager.createTooltip(LabelKey.TOOLTIP_EYEC_CONT_READING, ldrInterval);
        GuiManager.createTooltip(LabelKey.TOOLTIP_EYEC_MIN_BRIGHT, minimumBrightness);
        GuiManager.createTooltip(LabelKey.TOOLTIP_EYEC_CAL, calibrateLDR);
        GuiManager.createTooltip(LabelKey.TOOLTIP_EYEC_RESET, resetLDR);
        GuiManager.createTooltip(LabelKey.TOOLTIP_VAL, ldrLabel);
        GuiManager.createTooltip(LabelKey.TOOLTIP_BRIGHTNESS_LIMITER, brightnessLimiter);
        GuiManager.createTooltip(LabelKey.TOOLTIP_EYE_CARE, luminosityThreshold);
        GuiManager.createTooltip(LabelKey.TOOLTIP_NIGHT_LIGHT, nightLight);
        GuiManager.createTooltip(LabelKey.TOOLTIP_NIGHT_LIGHT, nightLightLvl);
    }

    /**
     * Init default values
     */
    public void initDefaultValues() {
        enableLDR.setSelected(false);
        ldrTurnOff.setSelected(false);
        ldrInterval.setValue(Enums.LdrInterval.CONTINUOUS.getI18n());
        minimumBrightness.setValue(20 + Constants.PERCENT);
        brightnessLimiter.setValue(Enums.BrightnessLimiter.BRIGHTNESS_LIMIT_DISABLED.getI18n());
        nightLight.setValue(Enums.NightLight.DISABLED.getI18n());
    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        enableLDR.setSelected(currentConfig.isEnableLDR());
        ldrTurnOff.setSelected(currentConfig.isLdrTurnOff());
        ldrInterval.setValue(Enums.LdrInterval.findByValue(currentConfig.getLdrInterval()).getI18n());
        minimumBrightness.setValue(currentConfig.getLdrMin() + Constants.PERCENT);
        brightnessLimiter.setValue(Enums.BrightnessLimiter.findByValue(currentConfig.getBrightnessLimiter()).getI18n());
        nightLight.setValue(Enums.NightLight.findByValue(currentConfig.getNightLight()).getI18n());
        handleNightLvl(currentConfig);
        setTooltips();
    }

    /**
     * Enable/Disable night light level based on Night Light status
     *
     * @param currentConfig current configuration
     */
    private void handleNightLvl(Configuration currentConfig) {
        nightLightLvl.setDisable(currentConfig.getNightLight().equals(Enums.NightLight.DISABLED.getI18n()));
    }

    /**
     * Enable disables items
     */
    private void evaluateValues() {
        if (!enableLDR.isSelected()) {
            ldrInterval.setDisable(true);
            ldrTurnOff.setDisable(true);
            minimumBrightness.setDisable(true);
            calibrateLDR.setDisable(true);
            resetLDR.setDisable(true);
            ldrLabel.setDisable(true);
        } else {
            ldrInterval.setDisable(false);
            ldrTurnOff.setDisable(LocalizedEnum.fromStr(Enums.LdrInterval.class, ldrInterval.getValue()).getLdrIntervalInteger() == 0);
            minimumBrightness.setDisable(false);
            calibrateLDR.setDisable(false);
            resetLDR.setDisable(false);
            ldrLabel.setDisable(false);
        }
    }

    /**
     * Manage animation timer to update the UI every seconds
     */
    private void startAnimationTimer() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> Platform.runLater(() ->
                setLdrValue(MainSingleton.getInstance().ldrStrength + Constants.PERCENT)), 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Close dialog
     *
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        scheduledExecutorService.shutdownNow();
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save and close dialog
     *
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        scheduledExecutorService.shutdownNow();
        apply(e);
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Apply settings
     *
     * @param e event
     */
    @FXML
    public void apply(InputEvent e) {
        boolean showApplyAlert = MainSingleton.getInstance().config.isEnableLDR() != enableLDR.isSelected() && enableLDR.isSelected();
        settingsController.injectEyeCareController(this);
        settingsController.save(e);
        setLdrDto(4);
        if (showApplyAlert) {
            MainSingleton.getInstance().guiManager.showLocalizedNotification(LabelKey.LDR_ALERT_ENABLED,
                    LabelKey.TOOLTIP_EYEC_ENABLE_LDR, LabelKey.LDR_ALERT_TITLE, TrayIcon.MessageType.INFO);
        }
        settingsController.miscTabController.evaluateLDRConnectedFeatures();
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    @SuppressWarnings("Duplicates")
    public void save(Configuration config) {
        EyeCareOptions.applyLdrControls(config, enableLDR.isSelected(), ldrTurnOff.isSelected(),
                LocalizedEnum.fromStr(Enums.LdrInterval.class, ldrInterval.getValue()).getLdrIntervalInteger(),
                Integer.parseInt(minimumBrightness.getValue().replace(Constants.PERCENT, "")));
        config.setBrightnessLimiter(LocalizedEnum.fromStr(Enums.BrightnessLimiter.class, brightnessLimiter.getValue()).getBrightnessLimitFloat());
        config.setNightLight(LocalizedEnum.fromStr(Enums.NightLight.class, nightLight.getValue()).getBaseI18n());
        config.setNightLightLvl(nightLightLvl.getValue());
        config.setLuminosityThreshold(Integer.parseInt(luminosityThreshold.getValue().substring(0, luminosityThreshold.getValue().length() - 1)));
    }

    /**
     * If night light is set to Auto, an executor is started to check if the OS night light is active
     */
    private void handleNightLightExecutor() {
        try {
            if (MainSingleton.getInstance().config.getNightLight().equals(Enums.NightLight.DISABLED.getI18n()) || MainSingleton.getInstance().config.getNightLight().equals(Enums.NightLight.ENABLED.getI18n())) {
                if (!GrabberSingleton.getInstance().getNightLightExecutor().isShutdown() || !GrabberSingleton.getInstance().getNightLightExecutor().isTerminated()) {
                    GrabberSingleton.getInstance().getNightLightExecutor().shutdown();
                    GrabberSingleton.getInstance().setNightLightAuto(false);
                }
            } else {
                GrabberSingleton.getInstance().getNightLightExecutor().shutdownNow();
                GrabberSingleton.getInstance().setNightLightExecutor(Executors.newScheduledThreadPool(1));
                GrabberSingleton.getInstance().getNightLightExecutor().scheduleAtFixedRate(GrabberSingleton.getInstance().getNightLightTask(), 0, 5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.debug("Night light executor restart failed", e);
        }
    }

    /**
     * Calibrate LDR, program microcontroller with a new value
     */
    @FXML
    public void calibrateLDR() {
        Optional<ButtonType> result = MainSingleton.getInstance().guiManager.showLocalizedAlert(LabelKey.LDR_ALERT_TITLE, LabelKey.LDR_ALERT_CONTINUE,
                LabelKey.TOOLTIP_EYEC_CAL, Alert.AlertType.CONFIRMATION);
        ButtonType button = result.orElse(ButtonType.OK);
        if (button == ButtonType.OK) {
            programMicrocontroller(2, LabelKey.LDR_ALERT_CAL_HEADER, LabelKey.LDR_ALERT_CAL_CONTENT);
        }
    }

    /**
     * Reset LDR, program microcontroller with default value
     */
    @FXML
    public void resetLDR() {
        Optional<ButtonType> result = MainSingleton.getInstance().guiManager.showLocalizedAlert(LabelKey.LDR_ALERT_TITLE, LabelKey.LDR_ALERT_CONTINUE,
                LabelKey.TOOLTIP_EYEC_RESET, Alert.AlertType.CONFIRMATION);
        ButtonType button = result.orElse(ButtonType.OK);
        if (button == ButtonType.OK) {
            programMicrocontroller(3, LabelKey.LDR_ALERT_RESET_HEADER, LabelKey.LDR_ALERT_RESET_CONTENT);
        }
    }

    /**
     * Program microcontroller with LDR settings
     *
     * @param ldrAction            1 no action, 2 calibrate, 3 reset, 4 save
     * @param ldrAlertResetHeader  alert msg
     * @param ldrAlertResetContent alert msg
     */
    private void programMicrocontroller(int ldrAction, String ldrAlertResetHeader, String ldrAlertResetContent) {
        TcpResponse tcpResponse = setLdrDto(ldrAction);
        if (!MainSingleton.getInstance().config.isFullFirmware()
                || (tcpResponse != null && tcpResponse.getErrorCode() == Constants.HTTP_SUCCESS)) {
            MainSingleton.getInstance().guiManager.showLocalizedNotification(ldrAlertResetHeader,
                    ldrAlertResetContent, LabelKey.LDR_ALERT_TITLE, TrayIcon.MessageType.INFO);
        } else {
            MainSingleton.getInstance().guiManager.showLocalizedNotification(LabelKey.LDR_ALERT_HEADER_ERROR,
                    LabelKey.LDR_ALERT_HEADER_CONTENT, LabelKey.LDR_ALERT_TITLE, TrayIcon.MessageType.ERROR);
        }
    }

    /**
     * Set LDR DTO
     *
     * @param ldrAction 1 no action, 2 calibrate, 3 reset, 4 save
     * @return TCP response
     */
    private TcpResponse setLdrDto(int ldrAction) {
        Configuration config = MainSingleton.getInstance().config;
        EyeCareOptions.applyLdrControls(config, enableLDR.isSelected(), ldrTurnOff.isSelected(),
                LocalizedEnum.fromStr(Enums.LdrInterval.class, ldrInterval.getValue()).getLdrIntervalInteger(),
                Integer.parseInt(minimumBrightness.getValue().replace(Constants.PERCENT, "")));
        return EyeCareOptions.programWithCalibrationLighting(config, ldrAction,
                () -> settingsController.miscTabController.toggleLed.fire(),
                () -> settingsController.miscTabController.toggleLed.fire(),
                () -> EyeCareOptions.programLdr(config, ldrAction, settingsController::sendSerialParams));
    }

    public StringProperty ldrValueProperty() {
        return ldrValue;
    }

    public void setLdrValue(String ldrValue) {
        this.ldrValue.set(ldrValue);
    }

}
