/*
  EyeCareDialogController.java

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

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.dto.LdrDto;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.util.Objects;
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
            for (int i = 10; i <= 100; i += 10) {
                minimumBrightness.getItems().add(i + Constants.PERCENT);
            }
            for (Enums.BrightnessLimiter brightnessLimit : Enums.BrightnessLimiter.values()) {
                brightnessLimiter.getItems().add(brightnessLimit.getI18n());
            }
            for (Enums.LdrInterval ldrVal : Enums.LdrInterval.values()) {
                ldrInterval.getItems().add(ldrVal.getI18n());
            }
            try {
                if (FireflyLuciferin.config.isFullFirmware()) {
                    TcpResponse tcp = NetworkManager.publishToTopic(Constants.HTTP_LDR, "", true);
                    JsonNode ldrDto = CommonUtility.fromJsonToObject(Objects.requireNonNull(tcp).getResponse());
                    enableLDR.setSelected(Objects.requireNonNull(ldrDto).get(Constants.HTTP_LDR_ENABLED).asText().equals("1"));
                    ldrTurnOff.setSelected(Objects.requireNonNull(ldrDto).get(Constants.HTTP_LDR_TURNOFF).asText().equals("1"));
                    ldrInterval.setValue(Enums.LdrInterval.findByValue(ldrDto.get(Constants.HTTP_LDR_INTERVAL).asInt()).getI18n());
                    minimumBrightness.setValue(ldrDto.get(Constants.HTTP_LDR_MIN).asText() + Constants.PERCENT);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            ldrLabel.textProperty().bind(ldrValueProperty());
            startAnimationTimer();
            enableLDR.setOnAction(e -> evaluateValues());
            ldrInterval.setOnAction(e -> evaluateValues());
            evaluateValues();
        });
    }

    /**
     * Set tooltips
     */
    private void setTooltips() {
        enableLDR.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_EYEC_ENABLE_LDR));
        ldrTurnOff.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_EYEC_TURNOFF));
        ldrInterval.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_EYEC_CONT_READING));
        minimumBrightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_EYEC_MIN_BRIGHT));
        calibrateLDR.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_EYEC_CAL));
        resetLDR.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_EYEC_RESET));
        ldrLabel.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_VAL));
        brightnessLimiter.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_BRIGHTNESS_LIMITER));
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
        setTooltips();
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
                setLdrValue(CommonUtility.ldrStrength + Constants.PERCENT)), 0, 1, TimeUnit.SECONDS);
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
        boolean showApplyAlert = FireflyLuciferin.config.isEnableLDR() != enableLDR.isSelected() && enableLDR.isSelected();
        settingsController.injectEyeCareController(this);
        settingsController.save(e);
        setLdrDto(4);
        if (showApplyAlert) {
            if (NativeExecutor.isWindows()) {
                FireflyLuciferin.guiManager.showLocalizedNotification(Constants.LDR_ALERT_ENABLED,
                        Constants.TOOLTIP_EYEC_ENABLE_LDR, TrayIcon.MessageType.INFO);
            } else {
                FireflyLuciferin.guiManager.showLocalizedAlert(Constants.LDR_ALERT_TITLE, Constants.LDR_ALERT_ENABLED,
                        Constants.TOOLTIP_EYEC_ENABLE_LDR, Alert.AlertType.INFORMATION);
            }
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
        config.setEnableLDR(enableLDR.isSelected());
        config.setLdrTurnOff(ldrTurnOff.isSelected());
        config.setLdrInterval(LocalizedEnum.fromStr(Enums.LdrInterval.class, ldrInterval.getValue()).getLdrIntervalInteger());
        config.setLdrMin(Integer.parseInt(minimumBrightness.getValue().replace(Constants.PERCENT, "")));
        config.setBrightnessLimiter(LocalizedEnum.fromStr(Enums.BrightnessLimiter.class, brightnessLimiter.getValue()).getBrightnessLimitFloat());
    }

    /**
     * Calibrate LDR, program microcontroller with a new value
     */
    @FXML
    public void calibrateLDR() {
        Optional<ButtonType> result = FireflyLuciferin.guiManager.showLocalizedAlert(Constants.LDR_ALERT_TITLE, Constants.LDR_ALERT_CONTINUE,
                Constants.TOOLTIP_EYEC_CAL, Alert.AlertType.CONFIRMATION);
        ButtonType button = result.orElse(ButtonType.OK);
        if (button == ButtonType.OK) {
            programMicrocontroller(2, Constants.LDR_ALERT_CAL_HEADER, Constants.LDR_ALERT_CAL_CONTENT);
        }
    }

    /**
     * Reset LDR, program microcontroller with default value
     */
    @FXML
    public void resetLDR() {
        Optional<ButtonType> result = FireflyLuciferin.guiManager.showLocalizedAlert(Constants.LDR_ALERT_TITLE, Constants.LDR_ALERT_CONTINUE,
                Constants.TOOLTIP_EYEC_RESET, Alert.AlertType.CONFIRMATION);
        ButtonType button = result.orElse(ButtonType.OK);
        if (button == ButtonType.OK) {
            programMicrocontroller(3, Constants.LDR_ALERT_RESET_HEADER, Constants.LDR_ALERT_RESET_CONTENT);
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
        if (!FireflyLuciferin.config.isFullFirmware() || tcpResponse.getErrorCode() == Constants.HTTP_SUCCESS) {
            if (NativeExecutor.isWindows()) {
                FireflyLuciferin.guiManager.showLocalizedNotification(ldrAlertResetHeader,
                        ldrAlertResetContent, TrayIcon.MessageType.INFO);
            } else {
                FireflyLuciferin.guiManager.showLocalizedAlert(Constants.LDR_ALERT_TITLE, ldrAlertResetHeader,
                        ldrAlertResetContent, Alert.AlertType.INFORMATION);
            }
        } else {
            if (NativeExecutor.isWindows()) {
                FireflyLuciferin.guiManager.showLocalizedNotification(Constants.LDR_ALERT_HEADER_ERROR,
                        Constants.LDR_ALERT_HEADER_CONTENT, TrayIcon.MessageType.ERROR);
            } else {
                FireflyLuciferin.guiManager.showLocalizedAlert(Constants.LDR_ALERT_TITLE, Constants.LDR_ALERT_HEADER_ERROR,
                        Constants.LDR_ALERT_HEADER_CONTENT, Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Set LDR DTO
     *
     * @param ldrAction 1 no action, 2 calibrate, 3 reset, 4 save
     * @return TCP response
     */
    private TcpResponse setLdrDto(int ldrAction) {
        LdrDto ldrDto = new LdrDto();
        ldrDto.setLdrEnabled(enableLDR.isSelected());
        ldrDto.setLdrTurnOff(ldrTurnOff.isSelected());
        ldrDto.setLdrInterval(LocalizedEnum.fromStr(Enums.LdrInterval.class, ldrInterval.getValue()).getLdrIntervalInteger());
        ldrDto.setLdrMin(Integer.parseInt(minimumBrightness.getValue().replace(Constants.PERCENT, "")));
        ldrDto.setLdrAction(ldrAction);
        FireflyLuciferin.config.setEnableLDR(ldrDto.isLdrEnabled());
        FireflyLuciferin.config.setLdrTurnOff(ldrDto.isLdrTurnOff());
        FireflyLuciferin.config.setLdrInterval(ldrDto.getLdrInterval());
        FireflyLuciferin.config.setLdrMin(Integer.parseInt(minimumBrightness.getValue().replace(Constants.PERCENT, "")));
        boolean toggleLed = false;
        if (ldrAction == 2 && settingsController.miscTabController.toggleLed.isSelected()) {
            if (ldrTurnOff.isSelected()) {
                settingsController.miscTabController.toggleLed.fire();
                toggleLed = true;
            }
            CommonUtility.sleepSeconds(4);
        }
        TcpResponse tcpResponse = null;
        FireflyLuciferin.ldrAction = ldrAction;
        if (FireflyLuciferin.config.isFullFirmware()) {
            // Note: this is HTTP only not MQTT.
            tcpResponse = NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.HTTP_SET_LDR), CommonUtility.toJsonString(ldrDto), true);
        } else {
            settingsController.sendSerialParams();
        }
        if (toggleLed) {
            CommonUtility.sleepSeconds(2);
            settingsController.miscTabController.toggleLed.fire();
        }
        return tcpResponse;
    }

    public StringProperty ldrValueProperty() {
        return ldrValue;
    }

    public void setLdrValue(String ldrValue) {
        this.ldrValue.set(ldrValue);
    }

}
