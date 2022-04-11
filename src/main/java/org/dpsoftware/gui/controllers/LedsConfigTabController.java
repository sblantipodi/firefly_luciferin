/*
  LedsConfigTabController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.TestCanvas;
import org.dpsoftware.utilities.CommonUtility;

/**
 * LEDs Config Tab controller
 */
@Slf4j
public class LedsConfigTabController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    // FXML binding
    @FXML public TextField topLed;
    @FXML public TextField leftLed;
    @FXML public TextField rightLed;
    @FXML public TextField bottomLeftLed;
    @FXML public TextField bottomRightLed;
    @FXML public TextField bottomRowLed;
    @FXML public ComboBox<String> orientation;
    @FXML public ComboBox<String> ledStartOffset;
    @FXML public Label bottomLeftLedLabel;
    @FXML public Label bottomRightLedLabel;
    @FXML public Label bottomRowLedLabel;
    @FXML public Label displayLabel;
    @FXML public Button showTestImageButton;
    @FXML public ComboBox<String> splitBottomMargin;
    @FXML public ComboBox<String> grabberAreaTopBottom;
    @FXML public ComboBox<String> grabberSide;
    @FXML public ComboBox<String> gapTypeTopBottom;
    @FXML public ComboBox<String> gapTypeSide;

    @FXML public Button saveLedButton;
    @FXML private Label grabAreaTopLabel, grabAreaRightLabel, grabAreaBottomLabel, grabAreaLeftLabel;
    @FXML private Label cornerGapTopLabel, cornerGapRightLabel, cornerGapBottomLabel, cornerGapLeftLabel;


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
        if (NativeExecutor.isLinux()) {
            grabAreaTopLabel.setText(Constants.LINUX_ARROW_TOP);
            grabAreaRightLabel.setText(Constants.LINUX_ARROW_RIGHT);
            grabAreaBottomLabel.setText(Constants.LINUX_ARROW_BOTTOM);
            grabAreaLeftLabel.setText(Constants.LINUX_ARROW_LEFT);
            cornerGapTopLabel.setText(Constants.LINUX_ARROW_TOP);
            cornerGapRightLabel.setText(Constants.LINUX_ARROW_RIGHT);
            cornerGapBottomLabel.setText(Constants.LINUX_ARROW_BOTTOM);
            cornerGapLeftLabel.setText(Constants.LINUX_ARROW_LEFT);
        }
        orientation.getItems().addAll(Constants.Orientation.CLOCKWISE.getI18n(), Constants.Orientation.ANTICLOCKWISE.getI18n());
        ledStartOffset.getItems().add(String.valueOf(0));
        for (Constants.LedOffset offset : Constants.LedOffset.values()) {
            ledStartOffset.getItems().add(offset.getI18n());
        }
        for (int i = 0; i <= 95; i += 5) {
            splitBottomMargin.getItems().add(i + Constants.PERCENT);
        }
        for (int i = 1; i <= 40; i += 1) {
            grabberAreaTopBottom.getItems().add(i + Constants.PERCENT);
            grabberSide.getItems().add(i + Constants.PERCENT);
        }
        for (int i = 0; i <= 40; i += 1) {
            gapTypeTopBottom.getItems().add(i + Constants.PERCENT);
            gapTypeSide.getItems().add(i + Constants.PERCENT);
        }
        ledStartOffset.setEditable(true);
    }

    /**
     * Init form values
     */
    void initDefaultValues() {
        ledStartOffset.setValue(String.valueOf(0));
        orientation.setValue(Constants.Orientation.CLOCKWISE.getI18n());
        topLed.setText("33");
        leftLed.setText("18");
        rightLed.setText("18");
        bottomLeftLed.setText("13");
        bottomRightLed.setText("13");
        bottomRowLed.setText("26");
        bottomLeftLed.setVisible(true);
        bottomRightLed.setVisible(true);
        bottomRowLed.setVisible(false);
        bottomLeftLedLabel.setVisible(true);
        bottomRightLedLabel.setVisible(true);
        bottomRowLedLabel.setVisible(false);
        splitBottomMargin.setValue(Constants.SPLIT_BOTTOM_MARGIN_DEFAULT);
        grabberAreaTopBottom.setValue(Constants.GRABBER_AREA_TOP_BOTTOM_DEFAULT);
        gapTypeTopBottom.setValue(Constants.GAP_TYPE_DEFAULT_TOP_BOTTOM);
        gapTypeSide.setValue(Constants.GAP_TYPE_DEFAULT_SIDE);
        grabberSide.setValue(Constants.GRABBER_AREA_SIDE_DEFAULT);
    }

    /**
     * Init form values by reading existing config file
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        switch (JavaFXStarter.whoAmI) {
            case 1:
                if ((currentConfig.getMultiMonitor() == 1)) {
                    displayLabel.setText(CommonUtility.getWord(Constants.MAIN_DISPLAY));
                } else {
                    displayLabel.setText(CommonUtility.getWord(Constants.RIGHT_DISPLAY));
                }
                break;
            case 2:
                if ((currentConfig.getMultiMonitor() == 2)) {
                    displayLabel.setText(CommonUtility.getWord(Constants.LEFT_DISPLAY));
                } else {
                    displayLabel.setText(CommonUtility.getWord(Constants.CENTER_DISPLAY));
                }
                break;
            case 3: displayLabel.setText(CommonUtility.getWord(Constants.LEFT_DISPLAY)); break;
        }
        ledStartOffset.setValue(String.valueOf(currentConfig.getLedStartOffset()));
        orientation.setValue(LocalizedEnum.fromBaseStr(Constants.Orientation.class, currentConfig.getOrientation()).getI18n());
        topLed.setText(String.valueOf(currentConfig.getTopLed()));
        leftLed.setText(String.valueOf(currentConfig.getLeftLed()));
        rightLed.setText(String.valueOf(currentConfig.getRightLed()));
        bottomLeftLed.setText(String.valueOf(currentConfig.getBottomLeftLed()));
        bottomRightLed.setText(String.valueOf(currentConfig.getBottomRightLed()));
        bottomRowLed.setText(String.valueOf(currentConfig.getBottomRowLed()));
        splitBottomMargin.setValue(currentConfig.getSplitBottomMargin());
        grabberAreaTopBottom.setValue(currentConfig.getGrabberAreaTopBottom());
        grabberSide.setValue(currentConfig.getGrabberSide());
        gapTypeTopBottom.setValue(currentConfig.getGapTypeTopBottom());
        gapTypeSide.setValue(currentConfig.getGapTypeSide());
    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {
        splitBottomMargin.setOnAction(e -> splitBottomRow());
    }

    /**
     * Show hide bottom row options
     */
    public void splitBottomRow() {
        if (CommonUtility.isSplitBottomRow(splitBottomMargin.getValue())) {
            bottomLeftLed.setVisible(true);
            bottomRightLed.setVisible(true);
            bottomRowLed.setVisible(false);
            bottomLeftLedLabel.setVisible(true);
            bottomRightLedLabel.setVisible(true);
            bottomRowLedLabel.setVisible(false);
        } else {
            bottomLeftLed.setVisible(false);
            bottomRightLed.setVisible(false);
            bottomRowLed.setVisible(true);
            bottomLeftLedLabel.setVisible(false);
            bottomRightLedLabel.setVisible(false);
            bottomRowLedLabel.setVisible(true);
        }
    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        settingsController.save(e);
    }

    /**
     * Save button from main controller
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        config.setSplitBottomMargin(splitBottomMargin.getValue());
        config.setGrabberAreaTopBottom(grabberAreaTopBottom.getValue());
        config.setGrabberSide(grabberSide.getValue());
        config.setGapTypeTopBottom(gapTypeTopBottom.getValue());
        config.setGapTypeSide(gapTypeSide.getValue());
        config.setTopLed(Integer.parseInt(topLed.getText()));
        config.setLeftLed(Integer.parseInt(leftLed.getText()));
        config.setRightLed(Integer.parseInt(rightLed.getText()));
        config.setBottomLeftLed(Integer.parseInt(bottomLeftLed.getText()));
        config.setBottomRightLed(Integer.parseInt(bottomRightLed.getText()));
        config.setBottomRowLed(Integer.parseInt(bottomRowLed.getText()));
        config.setOrientation(LocalizedEnum.fromStr(Constants.Orientation.class, orientation.getValue()).getBaseI18n());
        config.setLedStartOffset(Integer.parseInt(ledStartOffset.getValue()));
        int totalLed;
        // Force at least one LED if not LEDs is configured
        if (CommonUtility.isSplitBottomRow(config.getSplitBottomMargin())) {
            totalLed = config.getTopLed() + config.getRightLed() + config.getBottomLeftLed() + config.getBottomRightLed() + config.getRightLed();
        } else {
            totalLed = config.getTopLed() + config.getRightLed() + config.getBottomRowLed() + config.getRightLed();
        }
        if (totalLed == 0) {
            config.setTopLed(1);
        }
    }

    /**
     * Show a canvas containing a test image for the LED Matrix in use
     * @param e event
     */
    @FXML
    public void showTestImage(InputEvent e) {
        TestCanvas testCanvas = new TestCanvas();
        testCanvas.buildAndShowTestImage(e);
    }

    /**
     * Set form tooltips
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        topLed.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_TOPLED));
        leftLed.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_LEFTLED));
        rightLed.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_RIGHTLED));
        bottomLeftLed.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_BOTTOMLEFTLED));
        bottomRightLed.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_BOTTOMRIGHTLED));
        bottomRowLed.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_BOTTOMROWLED));
        orientation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_ORIENTATION));
        ledStartOffset.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_LEDSTARTOFFSET));
        splitBottomMargin.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SPLIT_BOTTOM_ROW));
        grabberAreaTopBottom.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_GRABBER_AREA_TOP_BOTTOM));
        grabberSide.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_GRABBER_AREA_SIDE));
        gapTypeTopBottom.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_CORNER_GAP));
        gapTypeSide.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_CORNER_GAP));
        if (currentConfig == null) {
            saveLedButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVELEDBUTTON_NULL));
        } else {
            saveLedButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVELEDBUTTON,200, 6000));
            showTestImageButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SHOWTESTIMAGEBUTTON,200, 6000));
        }
    }

    /**
     * Init LED offset listener
     */
    void addLedOffsetListener() {
        ledStartOffset.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!CommonUtility.isSplitBottomRow(splitBottomMargin.getValue()) && orientation.getValue().equals(Constants.Orientation.ANTICLOCKWISE.getI18n())) {
                if (newValue.equals(Constants.LedOffset.BOTTOM_LEFT.getI18n())) {
                    setLedOffset("0");
                } else if (newValue.equals(Constants.LedOffset.BOTTOM_CENTER.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomRowLed.getText()) / 2));
                } else if (newValue.equals(Constants.LedOffset.BOTTOM_RIGHT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomRowLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.UPPER_RIGHT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomRowLed.getText()) + Integer.parseInt(rightLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.UPPER_LEFT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomRowLed.getText()) + Integer.parseInt(rightLed.getText()) + Integer.parseInt(topLed.getText())));
                } else {
                    forceLedOffsetValidation(newValue);
                }
            } else if (!CommonUtility.isSplitBottomRow(splitBottomMargin.getValue()) && orientation.getValue().equals(Constants.Orientation.CLOCKWISE.getI18n())) {
                if (newValue.equals(Constants.LedOffset.BOTTOM_LEFT.getI18n())) {
                    setLedOffset("0");
                } else if (newValue.equals(Constants.LedOffset.BOTTOM_CENTER.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText()) + Integer.parseInt(rightLed.getText()) + (Integer.parseInt(bottomRowLed.getText()) / 2)));
                } else if (newValue.equals(Constants.LedOffset.BOTTOM_RIGHT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText()) + Integer.parseInt(rightLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.UPPER_RIGHT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.UPPER_LEFT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(leftLed.getText())));
                } else {
                    forceLedOffsetValidation(newValue);
                }
            } else if (CommonUtility.isSplitBottomRow(splitBottomMargin.getValue()) && orientation.getValue().equals(Constants.Orientation.ANTICLOCKWISE.getI18n())) {
                if (newValue.equals(Constants.LedOffset.BOTTOM_LEFT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomRightLed.getText()) + Integer.parseInt(rightLed.getText()) + Integer.parseInt(topLed.getText()) + Integer.parseInt(leftLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.BOTTOM_CENTER.getI18n())) {
                    setLedOffset("0");
                } else if (newValue.equals(Constants.LedOffset.BOTTOM_RIGHT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomRightLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.UPPER_RIGHT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomRightLed.getText()) + Integer.parseInt(rightLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.UPPER_LEFT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomRightLed.getText()) + Integer.parseInt(rightLed.getText()) + Integer.parseInt(topLed.getText())));
                } else {
                    forceLedOffsetValidation(newValue);
                }
            } else if (CommonUtility.isSplitBottomRow(splitBottomMargin.getValue()) && orientation.getValue().equals(Constants.Orientation.CLOCKWISE.getI18n())) {
                if (newValue.equals(Constants.LedOffset.BOTTOM_LEFT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomLeftLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.BOTTOM_CENTER.getI18n())) {
                    setLedOffset("0");
                } else if (newValue.equals(Constants.LedOffset.BOTTOM_RIGHT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomLeftLed.getText()) + Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText()) + Integer.parseInt(rightLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.UPPER_RIGHT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomLeftLed.getText()) + Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText())));
                } else if (newValue.equals(Constants.LedOffset.UPPER_LEFT.getI18n())) {
                    setLedOffset(String.valueOf(Integer.parseInt(bottomLeftLed.getText()) + Integer.parseInt(leftLed.getText())));
                } else {
                    forceLedOffsetValidation(newValue);
                }
            }
        });
    }

    /**
     * Force LED offset validation
     * @param newValue combobox new value
     */
    private void forceLedOffsetValidation(String newValue) {
        ledStartOffset.cancelEdit();
        if (newValue.length() == 0) {
            setLedOffset("0");
        } else {
            String val = newValue.replaceAll("[^\\d]", "").replaceFirst("^0+(?!$)", "");
            ledStartOffset.getItems().set(0, val);
            ledStartOffset.setValue(val);
        }
    }

    /**
     * Set led offset comboxbox
     * @param val led offset
     */
    void setLedOffset(String val) {
        ledStartOffset.getItems().set(0, val);
        ledStartOffset.setValue(val);
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        addLedOffsetListener();
        settingsController.addTextFieldListener(topLed);
        settingsController.addTextFieldListener(leftLed);
        settingsController.addTextFieldListener(rightLed);
        settingsController.addTextFieldListener(bottomLeftLed);
        settingsController.addTextFieldListener(bottomRightLed);
        settingsController.addTextFieldListener(bottomRowLed);
    }
}
