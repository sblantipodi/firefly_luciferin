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
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.TestCanvas;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;

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
    @FXML public Slider hueMonitorSlider;
    @FXML public Slider hueLedSlider;
    TestCanvas testCanvas;
    public static float hueTestImageValue = 0.0F;
    public static Color selectedChannel = Color.BLACK;
    @FXML public Label redLabel;
    @FXML public Label yellowLabel;
    @FXML public Label greenLabel;
    @FXML public Label cyanLabel;
    @FXML public Label blueLabel;
    @FXML public Label magentaLabel;
    @FXML public Label masterLabel;
    @FXML public Label whiteLabel;
    @FXML public Slider whiteTemp;

    /**
     * Inject main controller containing the TabPane
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Inject main test canvas
     * @param testCanvas testCanvas instance
     */
    public void injectTestCanvas(TestCanvas testCanvas) {
        this.testCanvas = testCanvas;
    }

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {
        Platform.runLater(() -> {
            initListeners(redSaturation, yellowSaturation, greenSaturation, cyanSaturation, blueSaturation, magentaSaturation, saturation);
            initListeners(redLightness, yellowLightness, greenLightness, cyanLightness, blueLightness, magentaLightness, saturationLightness);
            hueMonitorSlider.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if ((event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) && hueMonitorSlider.isFocused()) {
                    manageHueSliderValue();
                }
            });
            hueMonitorSlider.setOnMouseReleased(event -> manageHueSliderValue());
            hueLedSlider.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if ((event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) && hueLedSlider.isFocused()) {
                    manageHueSliderValue();
                }
            });
            hueLedSlider.setOnMouseReleased(event -> manageHueSliderValue());
            whiteTemp.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && whiteTemp.isFocused()) {
                    setWhiteTemperature();
                }
            });
            whiteTemp.setOnMouseReleased(event -> setWhiteTemperature());
            whiteTemp.setValue(FireflyLuciferin.config.getWhiteTemperature() * 100);
            applyLabelClass(masterLabel, Constants.CSS_CLASS_LABEL);
            selectedChannel = Color.BLACK;
            hueTestImageValue = 0.0F;
            whiteLabel.getStyleClass().add(Constants.CSS_SMALL_LINE_SPACING);
        });
    }

    /**
     * Manage hue slider value
     */
    private void manageHueSliderValue() {
        hueTestImageValue = (int) hueMonitorSlider.getValue();
        if (Color.RED.equals(selectedChannel)) {
            hueTestImageValue += Constants.RED_HUE;
            if (hueTestImageValue < 0) {
                hueTestImageValue = 360 - hueTestImageValue;
            }
            FireflyLuciferin.config.getHueMap().put(Constants.ColorEnum.RED, (float) hueLedSlider.getValue());
        } else if (Color.YELLOW.equals(selectedChannel)) {
            hueTestImageValue += Constants.YELLOW_HUE;
            FireflyLuciferin.config.getHueMap().put(Constants.ColorEnum.YELLOW, (float) hueLedSlider.getValue());
        } else if (Color.GREEN.equals(selectedChannel)) {
            hueTestImageValue += Constants.GREEN_HUE;
            FireflyLuciferin.config.getHueMap().put(Constants.ColorEnum.GREEN, (float) hueLedSlider.getValue());
        } else if (Color.CYAN.equals(selectedChannel)) {
            hueTestImageValue += Constants.CYAN_HUE;
            FireflyLuciferin.config.getHueMap().put(Constants.ColorEnum.CYAN, (float) hueLedSlider.getValue());
        } else if (Color.BLUE.equals(selectedChannel)) {
            hueTestImageValue += Constants.BLUE_HUE;
            FireflyLuciferin.config.getHueMap().put(Constants.ColorEnum.BLUE, (float) hueLedSlider.getValue());
        } else if (Color.MAGENTA.equals(selectedChannel)) {
            hueTestImageValue += Constants.MAGENTA_HUE;
            FireflyLuciferin.config.getHueMap().put(Constants.ColorEnum.MAGENTA, (float) hueLedSlider.getValue());
        }
        testCanvas.drawTestShapes(FireflyLuciferin.config, null);
    }

    /**
     * Set HSL value
     * @param redChannel slider value
     * @param yellowChannel slider value
     * @param greenChannel slider value
     * @param cyanChannel slider value
     * @param blueChannel slider value
     * @param magentaChannel slider value
     * @param saturationChannel slider value
     */
    private void initListeners(Slider redChannel, Slider yellowChannel, Slider greenChannel, Slider cyanChannel, Slider blueChannel, Slider magentaChannel, Slider saturationChannel) {
        redChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && redChannel.isFocused()) {
                setRedChannel();
            }
        });
        redChannel.setOnMouseReleased(event -> setRedChannel());
        yellowChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && yellowChannel.isFocused()) {
                setYellowChannel();
            }
        });
        yellowChannel.setOnMouseReleased(event -> setYellowChannel());
        greenChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && greenChannel.isFocused()) {
                setGreenChannel();
            }
        });
        greenChannel.setOnMouseReleased(event -> setGreenChannel());
        cyanChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && cyanChannel.isFocused()) {
                setCyanChannel();
            }
        });
        cyanChannel.setOnMouseReleased(event -> setCyanChannel());
        blueChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && blueChannel.isFocused()) {
                setBlueChannel();
            }
        });
        blueChannel.setOnMouseReleased(event -> setBlueChannel());
        magentaChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && magentaChannel.isFocused()) {
                setMagentaChannel();
            }
        });
        magentaChannel.setOnMouseReleased(event -> setMagentaChannel());
        saturationChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && saturationChannel.isFocused()) {
                setMasterChannel();
            }
        });
        saturationChannel.setOnMouseReleased(event -> setMasterChannel());
    }

    /**
     * Add/remove CSS class to sliders and labels
     * @param cssStyle style class to use
     */
    private void setSliderAndLabelClass(String cssStyle, Constants.ColorEnum color) {
        if (hueMonitorSlider != null && hueMonitorSlider.getStyleClass() != null) {
            setHueSlider(cssStyle, hueMonitorSlider);
        }
        if (hueLedSlider != null && hueLedSlider.getStyleClass() != null) {
            setHueSlider(cssStyle, hueLedSlider);
            if (color == null) {
                hueLedSlider.setValue(0);
            } else {
                hueLedSlider.setValue(FireflyLuciferin.config.getHueMap().get(color));
            }
        }
    }

    /**
     * Set hue sliders value and CSS classes
     * @param cssStyle style to use
     * @param slider hueMonitorSlider or hueLedSlider
     */
    private void setHueSlider(String cssStyle, Slider slider) {
        slider.getStyleClass().clear();
        slider.getStyleClass().add(Constants.CSS_STYLE_SLIDER);
        slider.getStyleClass().add(cssStyle);
        slider.setDisable(cssStyle.equals(Constants.CSS_STYLE_MASTER_HUE));
        setLabelClass();
    }

    /**
     * Set White Temp
     */
    private void setWhiteTemperature() {
        selectedChannel = Color.WHITE;
        settingsController.miscTabController.whiteTemp.setValue((int) whiteTemp.getValue());
        FireflyLuciferin.config.setWhiteTemperature((int) whiteTemp.getValue());
        settingsController.miscTabController.turnOnLEDs(FireflyLuciferin.config, false);
        testCanvas.drawTestShapes(FireflyLuciferin.config, null);
        setSliderAndLabelClass(Constants.CSS_STYLE_MASTER_HUE, null);
        hueMonitorSlider.setValue(0);
    }

    /**
     * Set red channel
     */
    private void setRedChannel() {
        FireflyLuciferin.config.setRedSaturation((float) redSaturation.getValue());
        FireflyLuciferin.config.setRedLightness((float) redLightness.getValue());
        if (!selectedChannel.equals(Color.RED)) {
            hueTestImageValue = Constants.RED_HUE;
            hueMonitorSlider.setValue(0.0F);
        }
        selectedChannel = Color.RED;
        testCanvas.drawTestShapes(FireflyLuciferin.config, null);
        setSliderAndLabelClass(Constants.CSS_STYLE_RED_HUE, Constants.ColorEnum.RED);
    }

    /**
     * Set yellow channel
     */
    private void setYellowChannel() {
        FireflyLuciferin.config.setYellowSaturation((float) yellowSaturation.getValue());
        FireflyLuciferin.config.setYellowLightness((float) yellowLightness.getValue());
        if (!selectedChannel.equals(Color.YELLOW)) {
            hueTestImageValue = Constants.YELLOW_HUE;
            hueMonitorSlider.setValue(0.0F);
        }
        selectedChannel = Color.YELLOW;
        testCanvas.drawTestShapes(FireflyLuciferin.config, null);
        setSliderAndLabelClass(Constants.CSS_STYLE_YELLOW_HUE, Constants.ColorEnum.YELLOW);
    }

    /**
     * Set green channel
     */
    private void setGreenChannel() {
        FireflyLuciferin.config.setGreenSaturation((float) greenSaturation.getValue());
        FireflyLuciferin.config.setGreenLightness((float) greenLightness.getValue());
        if (!selectedChannel.equals(Color.GREEN)) {
            hueTestImageValue = Constants.GREEN_HUE;
            hueMonitorSlider.setValue(0.0F);
        }
        selectedChannel = Color.GREEN;
        testCanvas.drawTestShapes(FireflyLuciferin.config, null);
        setSliderAndLabelClass(Constants.CSS_STYLE_GREEN_HUE, Constants.ColorEnum.GREEN);
    }

    /**
     * Set cyan channel
     */
    private void setCyanChannel() {
        FireflyLuciferin.config.setCyanSaturation((float) cyanSaturation.getValue());
        FireflyLuciferin.config.setCyanLightness((float) cyanLightness.getValue());
        if (!selectedChannel.equals(Color.CYAN)) {
            hueTestImageValue = Constants.CYAN_HUE;
            hueMonitorSlider.setValue(0.0F);
        }
        selectedChannel = Color.CYAN;
        testCanvas.drawTestShapes(FireflyLuciferin.config, null);
        setSliderAndLabelClass(Constants.CSS_STYLE_CYAN_HUE, Constants.ColorEnum.CYAN);
    }

    /**
     * Set blue channel
     */
    private void setBlueChannel() {
        FireflyLuciferin.config.setBlueSaturation((float) blueSaturation.getValue());
        FireflyLuciferin.config.setBlueLightness((float) blueLightness.getValue());
        if (!selectedChannel.equals(Color.BLUE)) {
            hueTestImageValue = Constants.BLUE_HUE;
            hueMonitorSlider.setValue(0.0F);
        }
        selectedChannel = Color.BLUE;
        testCanvas.drawTestShapes(FireflyLuciferin.config, null);
        setSliderAndLabelClass(Constants.CSS_STYLE_BLUE_HUE, Constants.ColorEnum.BLUE);
    }

    /**
     * Set magenta channel
     */
    private void setMagentaChannel() {
        FireflyLuciferin.config.setMagentaSaturation((float) magentaSaturation.getValue());
        FireflyLuciferin.config.setMagentaLightness((float) magentaLightness.getValue());
        if (!selectedChannel.equals(Color.MAGENTA)) {
            hueTestImageValue = Constants.MAGENTA_HUE;
            hueMonitorSlider.setValue(0.0F);
        }
        selectedChannel = Color.MAGENTA;
        testCanvas.drawTestShapes(FireflyLuciferin.config, null);
        setSliderAndLabelClass(Constants.CSS_STYLE_MAGENTA_HUE, Constants.ColorEnum.MAGENTA);
    }

    /**
     * Set master channel
     */
    private void setMasterChannel() {
        FireflyLuciferin.config.setSaturation((float) saturation.getValue());
        FireflyLuciferin.config.setSaturationLightness((float) saturationLightness.getValue());
        if (!selectedChannel.equals(Color.BLACK)) {
            hueTestImageValue = 0;
            hueMonitorSlider.setValue(0.0F);
        }
        selectedChannel = Color.BLACK;
        testCanvas.drawTestShapes(FireflyLuciferin.config, null);
        setSliderAndLabelClass(Constants.CSS_STYLE_MASTER_HUE, null);
    }

    /**
     * Set label class
     */
    private void setLabelClass() {
        if (selectedChannel.equals(Color.RED)) {
            applyLabelClass(redLabel, Constants.CSS_STYLE_REDTEXT);
        } else if (selectedChannel.equals(Color.YELLOW)) {
            applyLabelClass(yellowLabel, Constants.CSS_STYLE_YELLOWTEXT);
        } else if (selectedChannel.equals(Color.GREEN)) {
            applyLabelClass(greenLabel, Constants.CSS_STYLE_GREENTEXT);
        } else if (selectedChannel.equals(Color.CYAN)) {
            applyLabelClass(cyanLabel, Constants.CSS_STYLE_CYANTEXT);
        } else if (selectedChannel.equals(Color.BLUE)) {
            applyLabelClass(blueLabel, Constants.CSS_STYLE_BLUETEXT);
        } else if (selectedChannel.equals(Color.MAGENTA)) {
            applyLabelClass(magentaLabel, Constants.CSS_STYLE_MAGENTATEXT);
        } else if (selectedChannel.equals(Color.BLACK)) {
            applyLabelClass(masterLabel, Constants.CSS_CLASS_LABEL);
        } else if (selectedChannel.equals(Color.WHITE)) {
            applyLabelClass(whiteLabel, Constants.CSS_CLASS_LABEL);
            whiteLabel.getStyleClass().add(Constants.CSS_SMALL_LINE_SPACING);
        }
    }

    /**
     * Set style class to a specific label
     * @param label where to apply styles
     * @param labelClass class to apply
     */
    private void applyLabelClass(Label label, String labelClass) {
        redLabel.getStyleClass().clear();
        yellowLabel.getStyleClass().clear();
        greenLabel.getStyleClass().clear();
        cyanLabel.getStyleClass().clear();
        blueLabel.getStyleClass().clear();
        magentaLabel.getStyleClass().clear();
        masterLabel.getStyleClass().clear();
        whiteLabel.getStyleClass().clear();
        label.getStyleClass().add(Constants.CSS_CLASS_BOLD);
        label.getStyleClass().add(Constants.CSS_STYLE_UNDERLINE);
        label.getStyleClass().add(labelClass);
        redLabel.getStyleClass().add(Constants.CSS_CLASS_LABEL);
        yellowLabel.getStyleClass().add(Constants.CSS_CLASS_LABEL);
        greenLabel.getStyleClass().add(Constants.CSS_CLASS_LABEL);
        cyanLabel.getStyleClass().add(Constants.CSS_CLASS_LABEL);
        blueLabel.getStyleClass().add(Constants.CSS_CLASS_LABEL);
        magentaLabel.getStyleClass().add(Constants.CSS_CLASS_LABEL);
        masterLabel.getStyleClass().add(Constants.CSS_CLASS_LABEL);
        whiteLabel.getStyleClass().add(Constants.CSS_CLASS_LABEL);
        whiteLabel.getStyleClass().add(Constants.CSS_SMALL_LINE_SPACING);
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
        testCanvas.hideCanvas();
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
        config.setHueMap(FireflyLuciferin.config.getHueMap());
    }

    /**
     * Save saturation values
     * @param config from file
     */
    private void saveSaturationValues(Configuration config) {
        config.setRedSaturation((float) redSaturation .getValue());
        config.setYellowSaturation((float) yellowSaturation.getValue());
        config.setGreenSaturation((float) greenSaturation.getValue());
        config.setCyanSaturation((float) cyanSaturation.getValue());
        config.setBlueSaturation((float) blueSaturation.getValue());
        config.setMagentaSaturation((float) magentaSaturation.getValue());
        config.setSaturation((float) saturation.getValue());
    }

    /**
     * Save lightness values
     * @param config from file
     */
    private void saveLightnessValues(Configuration config) {
        config.setRedLightness((float) redLightness .getValue());
        config.setYellowLightness((float) yellowLightness.getValue());
        config.setGreenLightness((float) greenLightness.getValue());
        config.setCyanLightness((float) cyanLightness.getValue());
        config.setBlueLightness((float) blueLightness.getValue());
        config.setMagentaLightness((float) magentaLightness.getValue());
        config.setSaturationLightness((float) saturationLightness.getValue());
    }

    /**
     * Reset all sliders
     */
    @FXML
    public void reset() {
        resetSaturationValues();
        resetLightnessValues();
        hueMonitorSlider.setValue(0.0F);
        hueLedSlider.setValue(0.0F);
        whiteTemp.setValue(Constants.DEFAULT_WHITE_TEMP * 100);
        setWhiteTemperature();
        FireflyLuciferin.config.hueMap.put(Constants.ColorEnum.RED, 0.0F);
        FireflyLuciferin.config.hueMap.put(Constants.ColorEnum.YELLOW, 0.0F);
        FireflyLuciferin.config.hueMap.put(Constants.ColorEnum.GREEN, 0.0F);
        FireflyLuciferin.config.hueMap.put(Constants.ColorEnum.CYAN, 0.0F);
        FireflyLuciferin.config.hueMap.put(Constants.ColorEnum.BLUE, 0.0F);
        FireflyLuciferin.config.hueMap.put(Constants.ColorEnum.MAGENTA, 0.0F);
        selectedChannel = Color.BLACK;
        applyLabelClass(masterLabel, Constants.CSS_CLASS_LABEL);
        manageHueSliderValue();
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
        testCanvas.hideCanvas();
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Set form tooltips
     */
    void setTooltips() {
        setSaturationTooltips();
        setLightnessTooltips();
        hueMonitorSlider.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_HUE_MONITOR_SLIDER));
        hueLedSlider.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_HUE_LED_SLIDER));
        whiteTemp.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_WHITE_TEMP));
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
