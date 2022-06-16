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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.utilities.CommonUtility;

/**
 * Color correction dialog controller
 */
@Slf4j
public class ColorCorrectionDialogController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    @FXML public Slider redSaturation, redLightness;
    @FXML public Slider yellowSaturation, yellowLightness;
    @FXML public Slider greenSaturation, greenLightness;
    @FXML public Slider cyanSaturation, cyanLightness;
    @FXML public Slider blueSaturation, blueLightness;
    @FXML public Slider magentaSaturation, magentaLightness;
    @FXML public Slider saturation, saturationLightness;

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
            initSaturationListeners();
            initLightnessListeners();
        });
    }

    /**
     * Init saturation listeners
     */
    private void initSaturationListeners() {
        redSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && redSaturation.isFocused()) {
                FireflyLuciferin.config.setRedSaturation(redSaturation.getValue());
            }
        });
        redSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setRedSaturation(redSaturation.getValue()));
        yellowSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && yellowSaturation.isFocused()) {
                FireflyLuciferin.config.setYellowSaturation(yellowSaturation.getValue());
            }
        });
        yellowSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setYellowSaturation(yellowSaturation.getValue()));
        greenSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && greenSaturation.isFocused()) {
                FireflyLuciferin.config.setGreenSaturation(greenSaturation.getValue());
            }
        });
        greenSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setGreenSaturation(greenSaturation.getValue()));
        cyanSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && cyanSaturation.isFocused()) {
                FireflyLuciferin.config.setCyanSaturation(cyanSaturation.getValue());
            }
        });
        cyanSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setCyanSaturation(cyanSaturation.getValue()));
        blueSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && blueSaturation.isFocused()) {
                FireflyLuciferin.config.setBlueSaturation(blueSaturation.getValue());
            }
        });
        blueSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setBlueSaturation(blueSaturation.getValue()));
        magentaSaturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && magentaSaturation.isFocused()) {
                FireflyLuciferin.config.setMagentaSaturation(magentaSaturation.getValue());
            }
        });
        magentaSaturation.setOnMouseReleased(event -> FireflyLuciferin.config.setMagentaSaturation(magentaSaturation.getValue()));
        saturation.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && saturation.isFocused()) {
                FireflyLuciferin.config.setSaturation(saturation.getValue());
            }
        });
        saturation.setOnMouseReleased(event -> FireflyLuciferin.config.setSaturation(saturation.getValue()));
    }

    /**
     * Init lightness listeners
     */
    private void initLightnessListeners() {
        redLightness.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && redLightness.isFocused()) {
                FireflyLuciferin.config.setRedLightness(redLightness.getValue());
            }
        });
        redLightness.setOnMouseReleased(event -> FireflyLuciferin.config.setRedLightness(redLightness.getValue()));
        yellowLightness.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && yellowLightness.isFocused()) {
                FireflyLuciferin.config.setYellowLightness(yellowLightness.getValue());
            }
        });
        yellowLightness.setOnMouseReleased(event -> FireflyLuciferin.config.setYellowLightness(yellowLightness.getValue()));
        greenLightness.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && greenLightness.isFocused()) {
                FireflyLuciferin.config.setGreenLightness(greenLightness.getValue());
            }
        });
        greenLightness.setOnMouseReleased(event -> FireflyLuciferin.config.setGreenLightness(greenLightness.getValue()));
        cyanLightness.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && cyanLightness.isFocused()) {
                FireflyLuciferin.config.setCyanLightness(cyanLightness.getValue());
            }
        });
        cyanLightness.setOnMouseReleased(event -> FireflyLuciferin.config.setCyanLightness(cyanLightness.getValue()));
        blueLightness.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && blueLightness.isFocused()) {
                FireflyLuciferin.config.setBlueLightness(blueLightness.getValue());
            }
        });
        blueLightness.setOnMouseReleased(event -> FireflyLuciferin.config.setBlueLightness(blueLightness.getValue()));
        magentaLightness.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && magentaLightness.isFocused()) {
                FireflyLuciferin.config.setMagentaLightness(magentaLightness.getValue());
            }
        });
        magentaLightness.setOnMouseReleased(event -> FireflyLuciferin.config.setMagentaLightness(magentaLightness.getValue()));
        saturationLightness.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && saturationLightness.isFocused()) {
                FireflyLuciferin.config.setSaturationLightness(saturationLightness.getValue());
            }
        });
        saturationLightness.setOnMouseReleased(event -> FireflyLuciferin.config.setSaturationLightness(saturationLightness.getValue()));
    }

    /**
     * Init form values by reading existing config file
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        initSaturationValues(currentConfig);
        initLightnessValues(currentConfig);
        setTooltips();
    }

    /**
     * Init saturation values
     * @param currentConfig from file
     */
    private void initSaturationValues(Configuration currentConfig) {
        redSaturation.setValue(currentConfig.getRedSaturation());
        yellowSaturation.setValue(currentConfig.getYellowSaturation());
        greenSaturation.setValue(currentConfig.getGreenSaturation());
        cyanSaturation.setValue(currentConfig.getCyanSaturation());
        blueSaturation.setValue(currentConfig.getBlueSaturation());
        magentaSaturation.setValue(currentConfig.getMagentaSaturation());
        saturation.setValue(currentConfig.getSaturation());
    }

    /**
     * Init lightness values
     * @param currentConfig from file
     */
    private void initLightnessValues(Configuration currentConfig) {
        redLightness.setValue(currentConfig.getRedLightness());
        yellowLightness.setValue(currentConfig.getYellowLightness());
        greenLightness.setValue(currentConfig.getGreenLightness());
        cyanLightness.setValue(currentConfig.getCyanLightness());
        blueLightness.setValue(currentConfig.getBlueLightness());
        magentaLightness.setValue(currentConfig.getMagentaLightness());
        saturationLightness.setValue(currentConfig.getSaturationLightness());
    }

    /**
     * Save and close color correction dialog
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        settingsController.injectColorCorrectionController(this);
        settingsController.save(e);
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save button from main controller
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        saveSaturationValues(config);
        saveLightnessValues(config);
    }

    /**
     * Save saturation values
     * @param config from file
     */
    private void saveSaturationValues(Configuration config) {
        config.setRedSaturation(redSaturation .getValue());
        config.setYellowSaturation(yellowSaturation.getValue());
        config.setGreenSaturation(greenSaturation.getValue());
        config.setCyanSaturation(cyanSaturation.getValue());
        config.setBlueSaturation(blueSaturation.getValue());
        config.setMagentaSaturation(magentaSaturation.getValue());
        config.setSaturation(saturation.getValue());
    }

    /**
     * Save lightness values
     * @param config from file
     */
    private void saveLightnessValues(Configuration config) {
        config.setRedLightness(redLightness .getValue());
        config.setYellowLightness(yellowLightness.getValue());
        config.setGreenLightness(greenLightness.getValue());
        config.setCyanLightness(cyanLightness.getValue());
        config.setBlueLightness(blueLightness.getValue());
        config.setMagentaLightness(magentaLightness.getValue());
        config.setSaturationLightness(saturationLightness.getValue());
    }

    /**
     * Reset all sliders
     */
    @FXML
    public void reset() {
        resetSaturationValues();
        resetLightnessValues();
    }

    /**
     * Reset saturation values
     */
    private void resetSaturationValues() {
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
     * Reset lightness values
     */
    private void resetLightnessValues() {
        redLightness.setValue(0.0F);
        FireflyLuciferin.config.setRedLightness(0.0F);
        yellowLightness.setValue(0.0F);
        FireflyLuciferin.config.setYellowLightness(0.0F);
        greenLightness.setValue(0.0F);
        FireflyLuciferin.config.setGreenLightness(0.0F);
        cyanLightness.setValue(0.0F);
        FireflyLuciferin.config.setCyanLightness(0.0F);
        blueLightness.setValue(0.0F);
        FireflyLuciferin.config.setBlueLightness(0.0F);
        magentaLightness.setValue(0.0F);
        FireflyLuciferin.config.setMagentaLightness(0.0F);
        saturationLightness.setValue(0.0F);
        FireflyLuciferin.config.setSaturationLightness(0.0F);
    }

    /**
     * Close color correction dialog
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Set form tooltips
     */
    void setTooltips() {
        setSaturationTooltips();
        setLightnessTooltips();
    }

    /**
     * Set saturation tooltips
     */
    private void setSaturationTooltips() {
        redSaturation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_RED_SATURATION));
        yellowSaturation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_YELLOW_SATURATION));
        greenSaturation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_GREEN_SATURATION));
        cyanSaturation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_CYAN_SATURATION));
        blueSaturation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_BLUE_SATURATION));
        magentaSaturation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MAGENTA_SATURATION));
        saturation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SATURATION));
    }

    /**
     * Set Lightness tooltips
     */
    private void setLightnessTooltips() {
        redLightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_RED_LIGHTNESS));
        yellowLightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_YELLOW_LIGHTNESS));
        greenLightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_GREEN_LIGHTNESS));
        cyanLightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_CYAN_LIGHTNESS));
        blueLightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_BLUE_LIGHTNESS));
        magentaLightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MAGENTA_LIGHTNESS));
        saturationLightness.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_LIGHTNESS));
    }

}
