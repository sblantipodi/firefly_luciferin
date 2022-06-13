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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Configuration;

/**
 * Color correction dialog controller
 */
@Slf4j
public class ColorCorrectionDialogController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    @FXML public Slider redSaturation;
    @FXML public Slider yellowSaturation;
    @FXML public Slider greenSaturation;
    @FXML public Slider cyanSaturation;
    @FXML public Slider blueSaturation;
    @FXML public Slider magentaSaturation;
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

        redSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && redSaturation.isFocused()) {
                FireflyLuciferin.config.setRedSaturation(redSaturation.getValue());
            }
        });
        redSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setRedSaturation(redSaturation.getValue()));
        yellowSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && yellowSaturation.isFocused()) {
                FireflyLuciferin.config.setYellowSaturation(yellowSaturation.getValue());
            }
        });
        yellowSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setYellowSaturation(yellowSaturation.getValue()));
        greenSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && greenSaturation.isFocused()) {
                FireflyLuciferin.config.setGreenSaturation(greenSaturation.getValue());
            }
        });
        greenSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setGreenSaturation(greenSaturation.getValue()));
        cyanSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && cyanSaturation.isFocused()) {
                FireflyLuciferin.config.setCyanSaturation(cyanSaturation.getValue());
            }
        });
        cyanSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setCyanSaturation(cyanSaturation.getValue()));
        blueSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && blueSaturation.isFocused()) {
                FireflyLuciferin.config.setBlueSaturation(blueSaturation.getValue());
            }
        });
        blueSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setBlueSaturation(blueSaturation.getValue()));
        magentaSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && magentaSaturation.isFocused()) {
                FireflyLuciferin.config.setMagentaSaturation(magentaSaturation.getValue());
            }
        });
        magentaSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setMagentaSaturation(magentaSaturation.getValue()));
        saturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && saturation.isFocused()) {
                FireflyLuciferin.config.setSaturation(saturation.getValue());
            }
        });
        saturation.setOnMouseReleased(event -> FireflyLuciferin.config.setSaturation(saturation.getValue()));

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
     * Reset all sliders
     */
    @FXML
    public void reset() {
        redSaturation.setValue(0.0F);
        FireflyLuciferin.config.setRedSaturation(0.0F);
        yellowSaturation.setValue(0.0F);
        FireflyLuciferin.config.setYellowSaturation(0.0F);
        greenSaturation.setValue(0.0F);
        FireflyLuciferin.config.setGreenSaturation(0.0F);
        cyanSaturation.setValue(0.0F);
        FireflyLuciferin.config.setCyanSaturation(0.0F);
        blueSaturation.setValue(0.0F);
        FireflyLuciferin.config.setBlueSaturation(0.0F);
        magentaSaturation.setValue(0.0F);
        FireflyLuciferin.config.setMagentaSaturation(0.0F);
        saturation.setValue(0.0F);
        FireflyLuciferin.config.setSaturation(0.0F);
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
