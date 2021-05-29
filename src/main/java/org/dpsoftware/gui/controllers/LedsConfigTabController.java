/*
  LedsConfigTabController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.TestCanvas;

/**
 * LEDs Config Tab controller
 */
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
    @FXML public TextField ledStartOffset;
    @FXML public Label bottomLeftLedLabel;
    @FXML public Label bottomRightLedLabel;
    @FXML public Label bottomRowLedLabel;
    @FXML public Label displayLabel;
    @FXML public Button showTestImageButton;
    @FXML public CheckBox splitBottomRow;
    @FXML public Button saveLedButton;


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

        orientation.getItems().addAll(Constants.CLOCKWISE, Constants.ANTICLOCKWISE);

    }

    /**
     * Init form values
     */
    void initDefaultValues() {

        ledStartOffset.setText(String.valueOf(0));
        orientation.setValue(Constants.CLOCKWISE);
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
        splitBottomRow.setSelected(true);

    }

    /**
     * Init form values by reading existing config file
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {

        switch (JavaFXStarter.whoAmI) {
            case 1:
                if ((currentConfig.getMultiMonitor() == 1)) {
                    displayLabel.setText(Constants.MAIN_DISPLAY);
                } else {
                    displayLabel.setText(Constants.RIGHT_DISPLAY);
                }
                break;
            case 2:
                if ((currentConfig.getMultiMonitor() == 2)) {
                    displayLabel.setText(Constants.LEFT_DISPLAY);
                } else {
                    displayLabel.setText(Constants.CENTER_DISPLAY);
                }
                break;
            case 3: displayLabel.setText(Constants.LEFT_DISPLAY); break;
        }
        ledStartOffset.setText(String.valueOf(currentConfig.getLedStartOffset()));
        orientation.setValue(currentConfig.getOrientation());
        topLed.setText(String.valueOf(currentConfig.getTopLed()));
        leftLed.setText(String.valueOf(currentConfig.getLeftLed()));
        rightLed.setText(String.valueOf(currentConfig.getRightLed()));
        bottomLeftLed.setText(String.valueOf(currentConfig.getBottomLeftLed()));
        bottomRightLed.setText(String.valueOf(currentConfig.getBottomRightLed()));
        bottomRowLed.setText(String.valueOf(currentConfig.getBottomRowLed()));
        splitBottomRow.setSelected(currentConfig.isSplitBottomRow());

    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {

        splitBottomRow.setOnAction(e -> splitBottomRow());

    }

    /**
     * Show hide bottom row options
     */
    public void splitBottomRow() {

        if (splitBottomRow.isSelected()) {
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

        config.setSplitBottomRow(splitBottomRow.isSelected());
        config.setTopLed(Integer.parseInt(topLed.getText()));
        config.setLeftLed(Integer.parseInt(leftLed.getText()));
        config.setRightLed(Integer.parseInt(rightLed.getText()));
        config.setBottomLeftLed(Integer.parseInt(bottomLeftLed.getText()));
        config.setBottomRightLed(Integer.parseInt(bottomRightLed.getText()));
        config.setBottomRowLed(Integer.parseInt(bottomRowLed.getText()));
        config.setOrientation(orientation.getValue());
        config.setLedStartOffset(Integer.parseInt(ledStartOffset.getText()));

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
        splitBottomRow.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SPLIT_BOTTOM_ROW));
        if (currentConfig == null) {
            saveLedButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVELEDBUTTON_NULL));
        } else {
            saveLedButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVELEDBUTTON,200, 6000));
            showTestImageButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SHOWTESTIMAGEBUTTON,200, 6000));
        }

    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {

        settingsController.addTextFieldListener(ledStartOffset);
        settingsController.addTextFieldListener(topLed);
        settingsController.addTextFieldListener(leftLed);
        settingsController.addTextFieldListener(rightLed);
        settingsController.addTextFieldListener(bottomLeftLed);
        settingsController.addTextFieldListener(bottomRightLed);
        settingsController.addTextFieldListener(bottomRowLed);

    }

}
