/*
  EyeCareDialogController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.utilities.CommonUtility;

/**
 * Eye care dialog controller
 */
@Slf4j
public class EyeCareDialogController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    @FXML public CheckBox enableLDR;
    @FXML public CheckBox ldrContinuousReading;
    @FXML public ComboBox<String> minimumBrightness;
    @FXML public Button calibrateLDR;
    @FXML public Button okButton;
    @FXML public Button cancelButton;

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
        Platform.runLater(() -> {
            for (int i=20; i <= 100; i+=10) {
                minimumBrightness.getItems().add(i + Constants.PERCENT);
            }
        });
    }

    /**
     * Init default values
     */
    public void initDefaultValues() {
        enableLDR.setSelected(false);
        ldrContinuousReading.setSelected(false);
        minimumBrightness.setValue(20 + Constants.PERCENT);
    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        enableLDR.setSelected(currentConfig.isEnableLDR());
        ldrContinuousReading.setSelected(currentConfig.isLdrContinuousReading());
        minimumBrightness.setValue(currentConfig.getMinimumBrightness() + Constants.PERCENT);
    }

    /**
     * Close dialog
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save and close dialog
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        settingsController.injectEyeCareController(this);
        settingsController.save(e);
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save button from main controller
     * @param config stored config
     */
    @FXML
    @SuppressWarnings("Duplicates")
    public void save(Configuration config) {
        config.setEnableLDR(enableLDR.isSelected());
        config.setLdrContinuousReading(ldrContinuousReading.isSelected());
        config.setMinimumBrightness(Integer.parseInt(minimumBrightness.getValue().replace(Constants.PERCENT, "")));
    }

}
