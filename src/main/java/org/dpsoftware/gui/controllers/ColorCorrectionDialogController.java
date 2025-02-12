/*
  ColorCorrectionDialogController.java

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

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.TestCanvas;
import org.dpsoftware.managers.dto.HSLColor;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Color correction dialog controller
 */
@Slf4j
public class ColorCorrectionDialogController {

    @FXML
    public Slider redSaturation, redLightness, redHue;
    @FXML
    public Slider yellowSaturation, yellowLightness, yellowHue;
    @FXML
    public Slider greenSaturation, greenLightness, greenHue;
    @FXML
    public Slider cyanSaturation, cyanLightness, cyanHue;
    @FXML
    public Slider blueSaturation, blueLightness, blueHue;
    @FXML
    public Slider magentaSaturation, magentaLightness, magentaHue;
    @FXML
    public Slider saturation, saturationLightness;
    @FXML
    public Slider hueMonitorSlider;
    @FXML
    public Slider greyChannel;
    @FXML
    public Slider whiteTemp;
    @FXML
    public Label redLabel;
    @FXML
    public Label yellowLabel;
    @FXML
    public Label greenLabel;
    @FXML
    public Label cyanLabel;
    @FXML
    public Label blueLabel;
    @FXML
    public Label magentaLabel;
    @FXML
    public Label masterLabel;
    @FXML
    public Label whiteGreyLabel;
    @FXML
    public ComboBox<String> halfFullSaturation;
    @FXML
    public ToggleButton latencyTestToggle;
    @FXML
    public ComboBox<String> latencyTestSpeed;
    TestCanvas testCanvas;
    boolean useHalfSaturation = false;
    int latencyTestMilliseconds = 1000;
    AnimationTimer animationTimer;
    // Inject main controller
    @FXML
    private SettingsController settingsController;

    /**
     * Init HSL Map
     *
     * @return clean hue map
     */
    public static Map<Enums.ColorEnum, HSLColor> initHSLMap() {
        Map<Enums.ColorEnum, HSLColor> hueMap = new HashMap<>();
        hueMap.put(Enums.ColorEnum.RED, new HSLColor(0.0F, 0.0F, 0.0F));
        hueMap.put(Enums.ColorEnum.YELLOW, new HSLColor(0.0F, 0.0F, 0.0F));
        hueMap.put(Enums.ColorEnum.GREEN, new HSLColor(0.0F, 0.0F, 0.0F));
        hueMap.put(Enums.ColorEnum.CYAN, new HSLColor(0.0F, 0.0F, 0.0F));
        hueMap.put(Enums.ColorEnum.BLUE, new HSLColor(0.0F, 0.0F, 0.0F));
        hueMap.put(Enums.ColorEnum.MAGENTA, new HSLColor(0.0F, 0.0F, 0.0F));
        hueMap.put(Enums.ColorEnum.MASTER, new HSLColor(0.0F, 0.0F, 0.0F));
        hueMap.put(Enums.ColorEnum.GREY, new HSLColor(0.0F, 0.0F, 0.0F));
        return hueMap;
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
     * Inject main test canvas
     *
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
            initListeners(redHue, yellowHue, greenHue, cyanHue, blueHue, magentaHue, null);
            hueMonitorSlider.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if ((event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) && hueMonitorSlider.isFocused()) {
                    manageHueSliderValue();
                }
            });
            hueMonitorSlider.setOnMouseReleased(_ -> manageHueSliderValue());
            whiteTemp.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && whiteTemp.isFocused()) {
                    setWhiteTemperature();
                }
            });
            whiteTemp.setOnMouseReleased(_ -> setWhiteTemperature());
            whiteGreyLabel.setOnMouseReleased(_ -> setWhiteGreyValue());
            whiteTemp.setValue(MainSingleton.getInstance().config.getWhiteTemperature() * 100);
            applyLabelClass(masterLabel, Constants.CSS_CLASS_LABEL);
            GuiSingleton.getInstance().selectedChannel = Color.BLACK;
            GuiSingleton.getInstance().hueTestImageValue = 0.0F;
            whiteGreyLabel.getStyleClass().add(Constants.CSS_SMALL_LINE_SPACING);
            halfFullSaturation.getItems().add(CommonUtility.getWord(Constants.TC_FULL_SATURATION) + " (100%)");
            halfFullSaturation.getItems().add(CommonUtility.getWord(Constants.TC_FULL_SATURATION) + " (75%)");
            halfFullSaturation.getItems().add(CommonUtility.getWord(Constants.TC_FULL_SATURATION) + " (50%)");
            halfFullSaturation.getItems().add(CommonUtility.getWord(Constants.TC_FULL_SATURATION) + " (25%)");
            halfFullSaturation.setValue(CommonUtility.getWord(Constants.TC_FULL_SATURATION) + " (100%)");
            halfFullSaturation.valueProperty().addListener((_, _, _) ->
                    testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex()));
            for (int i = 1; i <= 10; i++) {
                latencyTestSpeed.getItems().add(i + "x");
            }
            latencyTestSpeed.setValue("1x");
            latencyTestSpeed.valueProperty().addListener((_, _, newVal) -> {
                stopLatencyTest();
                latencyTestMilliseconds = 1000 / Integer.parseInt(CommonUtility.removeChars(newVal));
                latencyTest();
            });
        });
    }

    /**
     * Manage hue slider value
     */
    private void manageHueSliderValue() {
        GuiSingleton.getInstance().hueTestImageValue = (int) hueMonitorSlider.getValue();
        if (Color.RED.equals(GuiSingleton.getInstance().selectedChannel)) {
            GuiSingleton.getInstance().hueTestImageValue += Enums.ColorEnum.RED.getVal();
            if (GuiSingleton.getInstance().hueTestImageValue < 0) {
                GuiSingleton.getInstance().hueTestImageValue = Constants.DEGREE_360 + GuiSingleton.getInstance().hueTestImageValue; // subtract a negative value
            }
        } else if (Color.YELLOW.equals(GuiSingleton.getInstance().selectedChannel)) {
            GuiSingleton.getInstance().hueTestImageValue += Enums.ColorEnum.YELLOW.getVal();
        } else if (Color.GREEN.equals(GuiSingleton.getInstance().selectedChannel)) {
            GuiSingleton.getInstance().hueTestImageValue += Enums.ColorEnum.GREEN.getVal();
        } else if (Color.CYAN.equals(GuiSingleton.getInstance().selectedChannel)) {
            GuiSingleton.getInstance().hueTestImageValue += Enums.ColorEnum.CYAN.getVal();
        } else if (Color.BLUE.equals(GuiSingleton.getInstance().selectedChannel)) {
            GuiSingleton.getInstance().hueTestImageValue += Enums.ColorEnum.BLUE.getVal();
        } else if (Color.MAGENTA.equals(GuiSingleton.getInstance().selectedChannel)) {
            GuiSingleton.getInstance().hueTestImageValue += Enums.ColorEnum.MAGENTA.getVal();
        }
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
    }

    /**
     * Set HSL value
     *
     * @param redChannel        slider value
     * @param yellowChannel     slider value
     * @param greenChannel      slider value
     * @param cyanChannel       slider value
     * @param blueChannel       slider value
     * @param magentaChannel    slider value
     * @param saturationChannel slider value
     */
    private void initListeners(Slider redChannel, Slider yellowChannel, Slider greenChannel, Slider cyanChannel, Slider blueChannel, Slider magentaChannel, Slider saturationChannel) {
        redChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && redChannel.isFocused()) {
                setRedChannel();
            }
        });
        redChannel.setOnMouseReleased(_ -> setRedChannel());
        redLabel.setOnMouseReleased(_ -> setRedChannel());
        yellowChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && yellowChannel.isFocused()) {
                setYellowChannel();
            }
        });
        yellowChannel.setOnMouseReleased(_ -> setYellowChannel());
        yellowLabel.setOnMouseReleased(_ -> setYellowChannel());
        greenChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && greenChannel.isFocused()) {
                setGreenChannel();
            }
        });
        greenChannel.setOnMouseReleased(_ -> setGreenChannel());
        greenLabel.setOnMouseReleased(_ -> setGreenChannel());
        cyanChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && cyanChannel.isFocused()) {
                setCyanChannel();
            }
        });
        cyanChannel.setOnMouseReleased(_ -> setCyanChannel());
        cyanLabel.setOnMouseReleased(_ -> setCyanChannel());
        blueChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && blueChannel.isFocused()) {
                setBlueChannel();
            }
        });
        blueChannel.setOnMouseReleased(_ -> setBlueChannel());
        blueLabel.setOnMouseReleased(_ -> setBlueChannel());
        magentaChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && magentaChannel.isFocused()) {
                setMagentaChannel();
            }
        });
        magentaChannel.setOnMouseReleased(_ -> setMagentaChannel());
        magentaLabel.setOnMouseReleased(_ -> setMagentaChannel());
        greyChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && greyChannel.isFocused()) {
                setGreyLightness();
            }
        });
        greyChannel.setOnMouseReleased(_ -> setGreyLightness());
        if (saturationChannel != null) {
            saturationChannel.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                if ((event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) && saturationChannel.isFocused()) {
                    setMasterChannel();
                }
            });
            saturationChannel.setOnMouseReleased(_ -> setMasterChannel());
        }
        masterLabel.setOnMouseReleased(_ -> setMasterChannel());
    }

    /**
     * Add/remove CSS class to sliders and labels
     *
     * @param cssStyle style class to use
     */
    private void setSliderAndLabelClass(String cssStyle) {
        if (hueMonitorSlider != null && hueMonitorSlider.getStyleClass() != null) {
            setHueSlider(cssStyle, hueMonitorSlider);
        }
    }

    /**
     * Set hue sliders value and CSS classes
     *
     * @param cssStyle style to use
     * @param slider   hueMonitorSlider or hueLedSlider
     */
    private void setHueSlider(String cssStyle, Slider slider) {
        slider.getStyleClass().clear();
        slider.getStyleClass().add(Constants.CSS_STYLE_SLIDER);
        slider.getStyleClass().add(cssStyle);
        slider.setDisable(cssStyle.equals(Constants.CSS_STYLE_MASTER_HUE));
        setLabelClass();
    }

    /**
     * Set white or gray value
     */
    private void setWhiteGreyValue() {
        if (whiteGreyLabel.getText().equals(CommonUtility.getWord(Constants.WHITE_LABEL_CORRECTION))) {
            setWhiteTemperature();
        } else {
            setGreyLightness();
        }
    }

    /**
     * Set White Temp
     */
    private void setWhiteTemperature() {
        GuiSingleton.getInstance().selectedChannel = Color.WHITE;
        settingsController.miscTabController.whiteTemp.setValue((int) whiteTemp.getValue());
        MainSingleton.getInstance().config.setWhiteTemperature((int) whiteTemp.getValue());
        settingsController.miscTabController.turnOnLEDs(MainSingleton.getInstance().config, false);
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
        setSliderAndLabelClass(Constants.CSS_STYLE_MASTER_HUE);
        hueMonitorSlider.setValue(0);
    }

    /**
     * Set White Temp
     */
    private void setGreyLightness() {
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREY).setLightness((float) greyChannel.getValue() / Constants.LIGHTNESS_PRECISION);
        if (!GuiSingleton.getInstance().selectedChannel.equals(Color.GRAY)) {
            GuiSingleton.getInstance().hueTestImageValue = Enums.ColorEnum.GREY.getVal();
            hueMonitorSlider.setValue(0.0F);
        }
        GuiSingleton.getInstance().selectedChannel = Color.GRAY;
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
        setSliderAndLabelClass(Constants.CSS_STYLE_GREY_HUE_VERTICAL);
    }

    /**
     * Set red channel
     */
    private void setRedChannel() {
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.RED).setSaturation((float) redSaturation.getValue());
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.RED).setLightness((float) redLightness.getValue() / Constants.LIGHTNESS_PRECISION);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.RED).setHue((float) redHue.getValue());
        if (!GuiSingleton.getInstance().selectedChannel.equals(Color.RED)) {
            GuiSingleton.getInstance().hueTestImageValue = Enums.ColorEnum.RED.getVal();
            hueMonitorSlider.setValue(0.0F);
        }
        GuiSingleton.getInstance().selectedChannel = Color.RED;
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
        setSliderAndLabelClass(Constants.CSS_STYLE_RED_HUE_VERTICAL);
    }

    /**
     * Set yellow channel
     */
    private void setYellowChannel() {
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.YELLOW).setSaturation((float) yellowSaturation.getValue());
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.YELLOW).setLightness((float) yellowLightness.getValue() / Constants.LIGHTNESS_PRECISION);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.YELLOW).setHue((float) yellowHue.getValue());
        if (!GuiSingleton.getInstance().selectedChannel.equals(Color.YELLOW)) {
            GuiSingleton.getInstance().hueTestImageValue = Enums.ColorEnum.YELLOW.getVal();
            hueMonitorSlider.setValue(0.0F);
        }
        GuiSingleton.getInstance().selectedChannel = Color.YELLOW;
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
        setSliderAndLabelClass(Constants.CSS_STYLE_YELLOW_HUE_VERTICAL);
    }

    /**
     * Set green channel
     */
    private void setGreenChannel() {
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREEN).setSaturation((float) greenSaturation.getValue());
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREEN).setLightness((float) greenLightness.getValue() / Constants.LIGHTNESS_PRECISION);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREEN).setHue((float) greenHue.getValue());
        if (!GuiSingleton.getInstance().selectedChannel.equals(Color.GREEN)) {
            GuiSingleton.getInstance().hueTestImageValue = Enums.ColorEnum.GREEN.getVal();
            hueMonitorSlider.setValue(0.0F);
        }
        GuiSingleton.getInstance().selectedChannel = Color.GREEN;
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
        setSliderAndLabelClass(Constants.CSS_STYLE_GREEN_HUE_VERTICAL);
    }

    /**
     * Set cyan channel
     */
    private void setCyanChannel() {
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.CYAN).setSaturation((float) cyanSaturation.getValue());
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.CYAN).setLightness((float) cyanLightness.getValue() / Constants.LIGHTNESS_PRECISION);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.CYAN).setHue((float) cyanHue.getValue());
        if (!GuiSingleton.getInstance().selectedChannel.equals(Color.CYAN)) {
            GuiSingleton.getInstance().hueTestImageValue = Enums.ColorEnum.CYAN.getVal();
            hueMonitorSlider.setValue(0.0F);
        }
        GuiSingleton.getInstance().selectedChannel = Color.CYAN;
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
        setSliderAndLabelClass(Constants.CSS_STYLE_CYAN_HUE_VERTICAL);
    }

    /**
     * Set blue channel
     */
    private void setBlueChannel() {
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.BLUE).setSaturation((float) blueSaturation.getValue());
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.BLUE).setLightness((float) blueLightness.getValue() / Constants.LIGHTNESS_PRECISION);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.BLUE).setHue((float) blueHue.getValue());
        if (!GuiSingleton.getInstance().selectedChannel.equals(Color.BLUE)) {
            GuiSingleton.getInstance().hueTestImageValue = Enums.ColorEnum.BLUE.getVal();
            hueMonitorSlider.setValue(0.0F);
        }
        GuiSingleton.getInstance().selectedChannel = Color.BLUE;
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
        setSliderAndLabelClass(Constants.CSS_STYLE_BLUE_HUE_VERTICAL);
    }

    /**
     * Set magenta channel
     */
    private void setMagentaChannel() {
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MAGENTA).setSaturation((float) magentaSaturation.getValue());
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MAGENTA).setLightness((float) magentaLightness.getValue() / Constants.LIGHTNESS_PRECISION);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MAGENTA).setHue((float) magentaHue.getValue());
        if (!GuiSingleton.getInstance().selectedChannel.equals(Color.MAGENTA)) {
            GuiSingleton.getInstance().hueTestImageValue = Enums.ColorEnum.MAGENTA.getVal();
            hueMonitorSlider.setValue(0.0F);
        }
        GuiSingleton.getInstance().selectedChannel = Color.MAGENTA;
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
        setSliderAndLabelClass(Constants.CSS_STYLE_MAGENTA_HUE_VERTICAL);
    }

    /**
     * Set master channel
     */
    private void setMasterChannel() {
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MASTER).setSaturation((float) saturation.getValue());
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MASTER).setLightness((float) saturationLightness.getValue() / Constants.LIGHTNESS_PRECISION);
        if (!GuiSingleton.getInstance().selectedChannel.equals(Color.BLACK)) {
            GuiSingleton.getInstance().hueTestImageValue = 0;
            hueMonitorSlider.setValue(0.0F);
        }
        GuiSingleton.getInstance().selectedChannel = Color.BLACK;
        testCanvas.drawTestShapes(MainSingleton.getInstance().config, null, halfFullSaturation.getSelectionModel().getSelectedIndex());
        setSliderAndLabelClass(Constants.CSS_STYLE_MASTER_HUE);
    }

    /**
     * Set label class
     */
    private void setLabelClass() {
        if (GuiSingleton.getInstance().selectedChannel.equals(Color.RED)) {
            applyLabelClass(redLabel, Constants.CSS_STYLE_REDTEXT);
        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.YELLOW)) {
            applyLabelClass(yellowLabel, Constants.CSS_STYLE_YELLOWTEXT);
        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.GREEN)) {
            applyLabelClass(greenLabel, Constants.CSS_STYLE_GREENTEXT);
        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.CYAN)) {
            applyLabelClass(cyanLabel, Constants.CSS_STYLE_CYANTEXT);
        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.BLUE)) {
            applyLabelClass(blueLabel, Constants.CSS_STYLE_BLUETEXT);
        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.MAGENTA)) {
            applyLabelClass(magentaLabel, Constants.CSS_STYLE_MAGENTATEXT);
        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.BLACK)) {
            applyLabelClass(masterLabel, Constants.CSS_CLASS_LABEL);
        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.WHITE) || GuiSingleton.getInstance().selectedChannel.equals(Color.GRAY)) {
            if (GuiSingleton.getInstance().selectedChannel.equals(Color.WHITE)) {
                whiteGreyLabel.setText(CommonUtility.getWord(Constants.WHITE_LABEL_CORRECTION));
            } else {
                whiteGreyLabel.setText(CommonUtility.getWord(Constants.GREY_LABEL_CORRECTION));
            }
            applyLabelClass(whiteGreyLabel, Constants.CSS_CLASS_LABEL);
            whiteGreyLabel.getStyleClass().add(Constants.CSS_SMALL_LINE_SPACING);
        }
    }

    /**
     * Set style class to a specific label
     *
     * @param label      where to apply styles
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
        whiteGreyLabel.getStyleClass().clear();
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
        whiteGreyLabel.getStyleClass().add(Constants.CSS_CLASS_LABEL);
        whiteGreyLabel.getStyleClass().add(Constants.CSS_SMALL_LINE_SPACING);
    }

    /**
     * Init form values by reading existing config file
     *
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        initSaturationValues(currentConfig);
        initLightnessValues(currentConfig);
        initHueValues(currentConfig);
        setTooltips();
    }

    /**
     * Init saturation values
     *
     * @param currentConfig from file
     */
    private void initSaturationValues(Configuration currentConfig) {
        redSaturation.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.RED).getSaturation());
        yellowSaturation.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.YELLOW).getSaturation());
        greenSaturation.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.GREEN).getSaturation());
        cyanSaturation.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.CYAN).getSaturation());
        blueSaturation.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.BLUE).getSaturation());
        magentaSaturation.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.MAGENTA).getSaturation());
        saturation.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.MASTER).getSaturation());
    }

    /**
     * Init lightness values
     *
     * @param currentConfig from file
     */
    private void initLightnessValues(Configuration currentConfig) {
        redLightness.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.RED).getLightness() * Constants.LIGHTNESS_PRECISION);
        yellowLightness.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.YELLOW).getLightness() * Constants.LIGHTNESS_PRECISION);
        greenLightness.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.GREEN).getLightness() * Constants.LIGHTNESS_PRECISION);
        cyanLightness.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.CYAN).getLightness() * Constants.LIGHTNESS_PRECISION);
        blueLightness.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.BLUE).getLightness() * Constants.LIGHTNESS_PRECISION);
        magentaLightness.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.MAGENTA).getLightness() * Constants.LIGHTNESS_PRECISION);
        saturationLightness.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.MASTER).getLightness() * Constants.LIGHTNESS_PRECISION);
        greyChannel.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.GREY).getLightness() * Constants.LIGHTNESS_PRECISION);
    }

    /**
     * Init hue values
     *
     * @param currentConfig from file
     */
    private void initHueValues(Configuration currentConfig) {
        redHue.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.RED).getHue());
        yellowHue.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.YELLOW).getHue());
        greenHue.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.GREEN).getHue());
        cyanHue.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.CYAN).getHue());
        blueHue.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.BLUE).getHue());
        magentaHue.setValue(currentConfig.getHueMap().get(Enums.ColorEnum.MAGENTA).getHue());
    }

    /**
     * Save and close color correction dialog
     *
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        stopLatencyTest();
        settingsController.injectColorCorrectionController(this);
        settingsController.save(e);
        testCanvas.hideCanvas();
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        stopLatencyTest();
        saveSaturationValues(config);
        saveLightnessValues(config);
        saveHueValues(config);
        config.setHueMap(MainSingleton.getInstance().config.getHueMap());
    }

    /**
     * Save button from main controller
     */
    @FXML
    public void latencyTest() {
        if (latencyTestToggle.isSelected()) {
            latencyTestSpeed.setDisable(false);
            setRedChannel();
            animationTimer = new AnimationTimer() {
                private long lastUpdate = 0;

                @Override
                public void handle(long now) {
                    now = now / (latencyTestMilliseconds * 1_000_000L);
                    if (now - lastUpdate >= 1) {
                        lastUpdate = now;
                        if (GuiSingleton.getInstance().selectedChannel.equals(Color.RED)) {
                            setYellowChannel();
                        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.YELLOW)) {
                            setGreenChannel();
                        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.GREEN)) {
                            setCyanChannel();
                        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.CYAN)) {
                            setBlueChannel();
                        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.BLUE)) {
                            setMagentaChannel();
                        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.MAGENTA)) {
                            setMasterChannel();
                        } else if (GuiSingleton.getInstance().selectedChannel.equals(Color.BLACK)) {
                            setRedChannel();
                        }
                    }
                }
            };
            animationTimer.start();
        } else {
            latencyTestSpeed.setDisable(true);
            stopLatencyTest();
        }
    }

    /**
     * Show settings dialog
     */
    @FXML
    public void showSettings() {
        settingsController.injectColorCorrectionController(this);
        MainSingleton.getInstance().guiManager.showSettingsDialog(false);
    }

    /**
     * Save saturation values
     *
     * @param config from file
     */
    private void saveSaturationValues(Configuration config) {
        config.getHueMap().get(Enums.ColorEnum.RED).setSaturation((float) redSaturation.getValue());
        config.getHueMap().get(Enums.ColorEnum.YELLOW).setSaturation((float) yellowSaturation.getValue());
        config.getHueMap().get(Enums.ColorEnum.GREEN).setSaturation((float) greenSaturation.getValue());
        config.getHueMap().get(Enums.ColorEnum.CYAN).setSaturation((float) cyanSaturation.getValue());
        config.getHueMap().get(Enums.ColorEnum.BLUE).setSaturation((float) blueSaturation.getValue());
        config.getHueMap().get(Enums.ColorEnum.MAGENTA).setSaturation((float) magentaSaturation.getValue());
        config.getHueMap().get(Enums.ColorEnum.MASTER).setSaturation((float) saturation.getValue());
    }

    /**
     * Save lightness values
     *
     * @param config from file
     */
    private void saveLightnessValues(Configuration config) {
        config.getHueMap().get(Enums.ColorEnum.RED).setLightness((float) redLightness.getValue());
        config.getHueMap().get(Enums.ColorEnum.YELLOW).setLightness((float) yellowLightness.getValue());
        config.getHueMap().get(Enums.ColorEnum.GREEN).setLightness((float) greenLightness.getValue());
        config.getHueMap().get(Enums.ColorEnum.CYAN).setLightness((float) cyanLightness.getValue());
        config.getHueMap().get(Enums.ColorEnum.BLUE).setLightness((float) blueLightness.getValue());
        config.getHueMap().get(Enums.ColorEnum.MAGENTA).setLightness((float) magentaLightness.getValue());
        config.getHueMap().get(Enums.ColorEnum.MASTER).setLightness((float) saturationLightness.getValue());
        config.getHueMap().get(Enums.ColorEnum.GREY).setLightness((float) greyChannel.getValue());
    }

    /**
     * Save hue values
     *
     * @param config from file
     */
    private void saveHueValues(Configuration config) {
        config.getHueMap().get(Enums.ColorEnum.RED).setHue((float) redHue.getValue());
        config.getHueMap().get(Enums.ColorEnum.YELLOW).setHue((float) yellowHue.getValue());
        config.getHueMap().get(Enums.ColorEnum.GREEN).setHue((float) greenHue.getValue());
        config.getHueMap().get(Enums.ColorEnum.CYAN).setHue((float) cyanHue.getValue());
        config.getHueMap().get(Enums.ColorEnum.BLUE).setHue((float) blueHue.getValue());
        config.getHueMap().get(Enums.ColorEnum.MAGENTA).setHue((float) magentaHue.getValue());
    }

    /**
     * Stop latency test executor
     */
    void stopLatencyTest() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    /**
     * Reset all sliders
     */
    @FXML
    public void reset() {
        useHalfSaturation = false;
        halfFullSaturation.setValue(halfFullSaturation.getItems().getFirst());
        resetSaturationValues();
        resetLightnessValues();
        resetHueValues();
        hueMonitorSlider.setValue(0.0F);
        whiteTemp.setValue(Constants.DEFAULT_WHITE_TEMP * 100);
        setWhiteTemperature();
        MainSingleton.getInstance().config.setHueMap(initHSLMap());
        GuiSingleton.getInstance().selectedChannel = Color.BLACK;
        applyLabelClass(masterLabel, Constants.CSS_CLASS_LABEL);
        manageHueSliderValue();
    }

    /**
     * Reset saturation values
     */
    private void resetSaturationValues() {
        redSaturation.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.RED).setSaturation(0.0F);
        yellowSaturation.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.YELLOW).setSaturation(0.0F);
        greenSaturation.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREEN).setSaturation(0.0F);
        cyanSaturation.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.CYAN).setSaturation(0.0F);
        blueSaturation.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.BLUE).setSaturation(0.0F);
        magentaSaturation.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MAGENTA).setSaturation(0.0F);
        saturation.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MASTER).setSaturation(0.0F);
    }

    /**
     * Reset lightness values
     */
    private void resetLightnessValues() {
        redLightness.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.RED).setLightness(0.0F);
        yellowLightness.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.YELLOW).setLightness(0.0F);
        greenLightness.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREEN).setLightness(0.0F);
        cyanLightness.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.CYAN).setLightness(0.0F);
        blueLightness.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.BLUE).setLightness(0.0F);
        magentaLightness.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MAGENTA).setLightness(0.0F);
        saturationLightness.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MASTER).setLightness(0.0F);
        greyChannel.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREY).setLightness(0.0F);
    }

    /**
     * Reset hue values
     */
    private void resetHueValues() {
        redHue.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.RED).setHue(0.0F);
        yellowHue.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.YELLOW).setHue(0.0F);
        greenHue.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREEN).setHue(0.0F);
        cyanHue.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.CYAN).setHue(0.0F);
        blueHue.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.BLUE).setHue(0.0F);
        magentaHue.setValue(0.0F);
        MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MAGENTA).setHue(0.0F);
    }

    /**
     * Close color correction dialog
     *
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        stopLatencyTest();
        testCanvas.hideCanvas();
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Set form tooltips
     */
    void setTooltips() {
        setSaturationTooltips();
        setLightnessTooltips();
        setHueTooltips();
        halfFullSaturation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_HALF_SATURATION));
        hueMonitorSlider.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_HUE_MONITOR_SLIDER));
        whiteTemp.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_WHITE_TEMP));
        latencyTestToggle.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_LATENCY_TEST));
        latencyTestSpeed.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_LATENCY_TEST_SPEED));
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
        greyChannel.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_GREY_LIGHTNESS));
    }

    /**
     * Set hue tooltips
     */
    private void setHueTooltips() {
        redHue.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_RED_HUE));
        yellowHue.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_YELLOW_HUE));
        greenHue.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_GREEN_HUE));
        cyanHue.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_CYAN_HUE));
        blueHue.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_BLUE_HUE));
        magentaHue.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MAGENTA_HUE));
    }

}
