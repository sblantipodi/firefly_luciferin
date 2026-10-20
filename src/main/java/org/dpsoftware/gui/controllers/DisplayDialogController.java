/*
  DisplayDialogController.java

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

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.InputEvent;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.controllers.options.DisplayDialogOptions;
import org.dpsoftware.lut.CubeLutToneMap;
import org.dpsoftware.utilities.CommonUtility;

import java.util.List;

/**
 * Display dialog controller
 */
public class DisplayDialogController {

    @FXML
    public ComboBox<String> cubeLutCombo;
    @FXML
    public Button okButton;
    @FXML
    public Button cancelButton;

    @FXML
    private SettingsController settingsController;

    private String savedLut = Constants.DISABLED;
    private boolean previewEnabled;

    /**
     * Handle cube LUT combo change, method used by the Web Server too
     *
     * @param value lut filename
     */
    public static void handleCubeLutCombo(String value) {
        MainSingleton.getInstance().config.setCubeLut(value);
        CubeLutToneMap.refresh();
    }

    /**
     * Inject main controller containing the TabPane
     *
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Apply committed combo box selections immediately, without writing the configuration file.
     */
    @FXML
    protected void initialize() {
        cubeLutCombo.valueProperty().addListener((_, _, value) -> {
            if (previewEnabled && MainSingleton.getInstance().config != null) {
                handleCubeLutCombo(DisplayDialogOptions.selectedLut(value));
            }
        });
    }

    /**
     * Init default values
     */
    public void initDefaultValues() {
        previewEnabled = false;
        savedLut = Constants.DISABLED;
        cubeLutCombo.getItems().setAll(DisplayDialogOptions.availableLuts());
        cubeLutCombo.setValue(Constants.DISABLED);
        setTooltips();
        previewEnabled = true;
    }

    /**
     * Init form values by reading existing config file
     *
     * @param currentConfig current configuration
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        previewEnabled = false;
        savedLut = currentConfig.getCubeLut();
        List<String> luts = DisplayDialogOptions.availableLuts();
        cubeLutCombo.getItems().setAll(luts);
        String current = currentConfig.getCubeLut();
        if (current != null && !current.isBlank() && !cubeLutCombo.getItems().contains(current)) {
            // A configured LUT not present in the available list (e.g. deleted file);
            // keep it selectable so the user can see and fix it.
            cubeLutCombo.getItems().add(current);
        }
        cubeLutCombo.setValue(DisplayDialogOptions.selectedLut(current));
        setTooltips();
        previewEnabled = true;
    }

    /**
     * Set tooltips
     */
    private void setTooltips() {
        GuiManager.createTooltip(LabelKey.TOOLTIP_CUBE_LUT, cubeLutCombo);
    }

    /**
     * Close dialog
     *
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        cancelPreview();
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Restore the last confirmed LUT, including the form value used by subsequent settings saves.
     */
    public void cancelPreview() {
        previewEnabled = false;
        cubeLutCombo.setValue(DisplayDialogOptions.selectedLut(savedLut));
        if (cubeLutCombo.isEditable()) {
            cubeLutCombo.getEditor().setText(DisplayDialogOptions.selectedLut(savedLut));
        }
        if (MainSingleton.getInstance().config != null) {
            handleCubeLutCombo(savedLut);
        }
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
        settingsController.injectDisplayController(this);
        settingsController.save(e);
        CubeLutToneMap.refresh();
        if (MainSingleton.getInstance().config != null) {
            savedLut = MainSingleton.getInstance().config.getCubeLut();
        }
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        String selected = cubeLutCombo.isEditable() ? cubeLutCombo.getEditor().getText() : cubeLutCombo.getValue();
        config.setCubeLut(DisplayDialogOptions.selectedLut(selected));
    }

}
