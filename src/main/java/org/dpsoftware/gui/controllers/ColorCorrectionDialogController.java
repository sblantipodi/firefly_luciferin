/*
  ColorCorrectionDialogController.java

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

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;
import org.dpsoftware.config.Configuration;

import java.awt.event.ActionEvent;

/**
 * Color correction dialog controller
 */
public class ColorCorrectionDialogController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    @FXML public Slider redSaturation;
    @FXML public Slider greenSaturation;
    @FXML public Slider blueSaturation;
    @FXML public Slider saturation;

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

    }

    /**
     * Init combo boxes
     */
    public void initComboBox() {

    }

    /**
     * Init form values
     */
    void initDefaultValues() {

    }



    /**
     * Init form values by reading existing config file
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {

    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {

    }


    /**
     * Save and close color correction dialog
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        Node source = (Node)  e.getSource();
        Stage stage  = (Stage) source.getScene().getWindow();
        stage.close();
    }

    /**
     * Close color correction dialog
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        Node source = (Node)  e.getSource();
        Stage stage  = (Stage) source.getScene().getWindow();
        stage.close();
    }

    /**
     * Set form tooltips
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {

    }


}
