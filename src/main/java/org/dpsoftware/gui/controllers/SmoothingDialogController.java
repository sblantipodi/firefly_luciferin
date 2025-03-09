/*
  SmoothingDialogController.java

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
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.utilities.CommonUtility;

/**
 * Eye care dialog controller
 */
@Slf4j
public class SmoothingDialogController {

    @FXML
    public ComboBox<String> frameGen;
    @FXML
    public ComboBox<String> smoothingLvl;
    @FXML
    public ComboBox<String> targetFramerate;
    @FXML
    public TextField captureFramerate;
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
        Platform.runLater(() -> {
            for (Enums.Ema ema : Enums.Ema.values()) {
                smoothingLvl.getItems().add(ema.getI18n());
            }
            for (Enums.FrameGeneration frameInsertion : Enums.FrameGeneration.values()) {
                frameGen.getItems().add(frameInsertion.getI18n());
            }
            for (Enums.SmoothingTarget smoothingTarget : Enums.SmoothingTarget.values()) {
                targetFramerate.getItems().add(smoothingTarget.getSmoothingTarget());
            }
            frameGen.valueProperty().addListener((_, _, _) -> handleCombo());
            smoothingLvl.valueProperty().addListener((_, _, _) -> handleCombo());
            targetFramerate.valueProperty().addListener((_, _, _) -> handleCombo());
        });
    }

    /**
     * Set tooltips
     */
    private void setTooltips() {
        SettingsController.createTooltip(Constants.TOOLTIP_EMA, smoothingLvl);
        SettingsController.createTooltip(Constants.TOOLTIP_FG, frameGen);
    }

    /**
     * Init default values
     */
    public void initDefaultValues() {
        smoothingLvl.setValue(Enums.Ema.findByValue(Constants.DEFAULT_EMA).getI18n());
        frameGen.setValue(Enums.FrameGeneration.findByValue(Constants.DEFAULT_FRAMGEN).getI18n());
        targetFramerate.setValue(Enums.SmoothingTarget.TARGET_60_FPS.getSmoothingTarget());
    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        smoothingLvl.setValue(Enums.Ema.findByValue(currentConfig.getEmaAlpha()).getI18n());
        frameGen.setValue(Enums.FrameGeneration.findByValue(currentConfig.getFrameInsertionTarget()).getI18n());
        targetFramerate.setValue(Enums.SmoothingTarget.findByValue(currentConfig.getSmoothingTargetFramerate()).getSmoothingTarget());
        captureFramerate.setText(captureFramerateLabel(currentConfig));
        setTooltips();
        handleCombo(false);
    }

    /**
     * Generate capture framerate label
     */
    private String captureFramerateLabel(Configuration currentConfig) {
        int targetFramerate = Enums.FrameGeneration.findByValue(currentConfig.getFrameInsertionTarget()).getFrameGenerationTarget();
        if (targetFramerate == 0) {
            targetFramerate = Integer.parseInt(currentConfig.getDesiredFramerate());
        } else if (currentConfig.getSmoothingTargetFramerate() == Enums.SmoothingTarget.TARGET_120_FPS.getSmoothingTargetValue()) {
            targetFramerate = targetFramerate * 2;
        }
        return targetFramerate + " FPS";
    }

    /**
     * Handle user interacting with the combos
     *
     * @param restart if true it restart the screen capture
     */
    private void handleCombo(boolean restart) {
        evaluateSmoothing(MainSingleton.getInstance().config);
        if (restart) {
            PipelineManager.restartCapture(CommonUtility::run);
        }
    }

    /**
     * Handle combo box
     */
    private void handleCombo() {
        handleCombo(true);
    }

    /**
     * Close dialog
     *
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        evaluateSmoothing(MainSingleton.getInstance().config);
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save and close dialog
     *
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        settingsController.injectSmoothingController(this);
        settingsController.save(e);
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    @SuppressWarnings("Duplicates")
    public void save(Configuration config) {
        evaluateSmoothing(config);
    }

    /**
     * Evaluate smoothing
     *
     * @param config stored config
     */
    private void evaluateSmoothing(Configuration config) {
        float alpha = LocalizedEnum.fromStr(Enums.Ema.class, smoothingLvl.getValue()).getEmaAlpha();
        int target = LocalizedEnum.fromStr(Enums.FrameGeneration.class, frameGen.getValue()).getFrameGenerationTarget();
        config.setEmaAlpha(alpha);
        config.setFrameInsertionTarget(target);
        config.setSmoothingType(Enums.Smoothing.findByFramerateAndAlpha(target, alpha).getBaseI18n());
        config.setSmoothingTargetFramerate(Enums.SmoothingTarget.findByExtendedVal(targetFramerate.getValue()).getSmoothingTargetValue());
        captureFramerate.setText(captureFramerateLabel(config));
        targetFramerate.setDisable(Enums.Smoothing.findByFramerateAndAlpha(target, alpha) == Enums.Smoothing.DISABLED);
        settingsController.miscTabController.evaluateSmoothing();
    }

}
