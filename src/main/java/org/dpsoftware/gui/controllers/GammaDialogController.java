/*
  GammaDialogController.java

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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.utilities.CommonUtility;

/**
 * Gamma dialog controller
 */
@Slf4j
public class GammaDialogController {

    @FXML
    public CheckBox enableAutomaticGammaCheck;
    @FXML
    public ComboBox<String> gammaLevel;
    @FXML
    public Button okButton;
    @FXML
    public Button applyButton;
    @FXML
    public Button cancelButton;

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
            for (Enums.GammaLevel gammaVal : Enums.GammaLevel.values()) {
                gammaLevel.getItems().add(gammaVal.getI18n());
            }
            enableAutomaticGammaCheck.selectedProperty().addListener((_, _, _) -> toggleValues());
        });
    }

    /**
     * Init default values
     */
    public void initDefaultValues() {
        enableAutomaticGammaCheck.setSelected(true);
        gammaLevel.setValue(Enums.GammaLevel.MEDIUM.getI18n());
    }

    /**
     * Init form values by reading existing config file
     *
     * @param currentConfig current configuration
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        enableAutomaticGammaCheck.setSelected(currentConfig.isEnableAutomaticGamma());
        gammaLevel.setValue(Enums.GammaLevel.findByValue(currentConfig.getGammaLevel()).getI18n());
        toggleValues();
        setTooltips();
    }

    /**
     * Toggle combo box based on checkbox state
     */
    private void toggleValues() {
        gammaLevel.setDisable(!enableAutomaticGammaCheck.isSelected());
    }

    /**
     * Set tooltips
     */
    private void setTooltips() {
        GuiManager.createTooltip(Constants.TOOLTIP_GAMMA_ENABLE_AUTO, enableAutomaticGammaCheck);
        GuiManager.createTooltip(Constants.TOOLTIP_GAMMA_LEVEL, gammaLevel);
    }

    /**
     * Close dialog
     *
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save and close dialog
     *
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
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
        settingsController.injectGammaController(this);
        settingsController.save(e);
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        config.setEnableAutomaticGamma(enableAutomaticGammaCheck.isSelected());
        config.setGammaLevel(LocalizedEnum.fromStr(Enums.GammaLevel.class, gammaLevel.getValue()).getBaseI18n());
    }

}
