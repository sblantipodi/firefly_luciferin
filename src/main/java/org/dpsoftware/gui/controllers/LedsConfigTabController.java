/*
  LedsConfigTabController.java

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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.LedMatrixInfo;
import org.dpsoftware.utilities.CommonUtility;

/**
 * LEDs Config Tab controller
 */
@Slf4j
public class LedsConfigTabController {

    // FXML binding
    @FXML
    public TextField topLed;
    @FXML
    public TextField leftLed;
    @FXML
    public TextField rightLed;
    @FXML
    public TextField bottomLeftLed;
    @FXML
    public TextField bottomRightLed;
    @FXML
    public TextField bottomRowLed;
    @FXML
    public ComboBox<String> orientation;
    @FXML
    public ComboBox<String> ledStartOffset;
    @FXML
    public Label bottomLeftLedLabel;
    @FXML
    public Label bottomRightLedLabel;
    @FXML
    public Label bottomRowLedLabel;
    @FXML
    public Label displayLabel;
    @FXML
    public Button showTestImageButton;
    @FXML
    public ComboBox<String> splitBottomMargin;
    @FXML
    public ComboBox<String> grabberAreaTopBottom;
    @FXML
    public ComboBox<String> grabberSide;
    @FXML
    public ComboBox<String> gapTypeTopBottom;
    @FXML
    public ComboBox<String> gapTypeSide;
    @FXML
    public ComboBox<Integer> groupBy;
    @FXML
    public Button saveLedButton;
    // Inject main controller
    @FXML
    private SettingsController settingsController;
    @FXML
    private Label grabAreaTopLabel, grabAreaRightLabel, grabAreaBottomLabel, grabAreaLeftLabel;
    @FXML
    private Label cornerGapTopLabel, cornerGapRightLabel, cornerGapBottomLabel, cornerGapLeftLabel;

    /**
     * Set led matrix info
     *
     * @param config stored config
     * @return matrix infos
     */
    private static LedMatrixInfo getLedMatrixInfo(Configuration config) {
        LedMatrixInfo ledMatrixInfo = new LedMatrixInfo();
        ledMatrixInfo.setTopLedOriginal(config.getTopLed());
        ledMatrixInfo.setRightLedOriginal(config.getRightLed());
        ledMatrixInfo.setBottomRightLedOriginal(config.getBottomRightLed());
        ledMatrixInfo.setBottomLeftLedOriginal(config.getBottomLeftLed());
        ledMatrixInfo.setBottomRowLedOriginal(config.getBottomRowLed());
        ledMatrixInfo.setLeftLedOriginal(config.getLeftLed());
        ledMatrixInfo.setSplitBottomRow(config.getSplitBottomMargin());
        ledMatrixInfo.setGroupBy(config.getGroupBy());
        return ledMatrixInfo;
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
        orientation.getItems().addAll(Enums.Orientation.CLOCKWISE.getI18n(), Enums.Orientation.ANTICLOCKWISE.getI18n());
        ledStartOffset.getItems().add(String.valueOf(0));
        for (Enums.LedOffset offset : Enums.LedOffset.values()) {
            ledStartOffset.getItems().add(offset.getI18n());
        }
        splitBottomMargin.getItems().add(0 + Constants.PERCENT);
        for (int i = 0; i <= 95; i += 5) {
            splitBottomMargin.getItems().add(i + Constants.PERCENT);
        }
        for (int i = 1; i <= 40; i++) {
            grabberAreaTopBottom.getItems().add(i + Constants.PERCENT);
            grabberSide.getItems().add(i + Constants.PERCENT);
        }
        for (int i = 0; i <= 40; i++) {
            gapTypeTopBottom.getItems().add(i + Constants.PERCENT);
            gapTypeSide.getItems().add(i + Constants.PERCENT);
        }
        ledStartOffset.setEditable(true);
        splitBottomMargin.setEditable(true);
        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readProfileInUseConfig();
        if (currentConfig != null && CommonUtility.isSingleDeviceOtherInstance()) {
            orientation.setDisable(true);
            ledStartOffset.setDisable(true);
        }
        if (currentConfig != null && CommonUtility.isSingleDeviceMultiScreen()) {
            groupBy.setDisable(true);
            splitBottomMargin.setDisable(true);
            if (MainSingleton.getInstance().config.getMultiMonitor() == 3 && MainSingleton.getInstance().whoAmI == 2) {
                splitBottomMargin.setDisable(false);
            }
            ledStartOffset.getItems().clear();
            ledStartOffset.getItems().add("0");
        }
    }

    /**
     * Init group by combo for smoothing effect
     */
    private void initGroupByCombo() {
        int grpBy = groupBy.getValue();
        LedMatrixInfo ledMatrixInfo = new LedMatrixInfo();
        ledMatrixInfo.setTopLedOriginal(Integer.parseInt(topLed.getText()));
        ledMatrixInfo.setRightLedOriginal(Integer.parseInt(rightLed.getText()));
        ledMatrixInfo.setBottomRightLedOriginal(Integer.parseInt(bottomRightLed.getText()));
        ledMatrixInfo.setBottomLeftLedOriginal(Integer.parseInt(bottomLeftLed.getText()));
        ledMatrixInfo.setBottomRowLedOriginal(Integer.parseInt(bottomRowLed.getText()));
        ledMatrixInfo.setLeftLedOriginal(Integer.parseInt(leftLed.getText()));
        ledMatrixInfo.setSplitBottomRow(splitBottomMargin.getValue());
        ledMatrixInfo.setGroupBy(grpBy);
        CommonUtility.groupByCalc(ledMatrixInfo);
        groupBy.getItems().clear();
        for (int i = 1; i <= ledMatrixInfo.getMinimumNumberOfLedsInARow(); i++) {
            groupBy.getItems().add(i);
        }
        groupBy.setValue(Math.min(grpBy, ledMatrixInfo.getMinimumNumberOfLedsInARow()));
    }

    /**
     * Init form values
     */
    void initDefaultValues() {
        ledStartOffset.setValue(String.valueOf(0));
        orientation.setValue(Enums.Orientation.CLOCKWISE.getI18n());
        topLed.setText("10");
        leftLed.setText("10");
        rightLed.setText("10");
        bottomLeftLed.setText("10");
        bottomRightLed.setText("10");
        bottomRowLed.setText("20");
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
        groupBy.setValue(Constants.GROUP_BY_LEDS);
        initGroupByCombo();
    }

    /**
     * Init form values by reading existing config file
     *
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        setDisplayLabel(currentConfig);
        ledStartOffset.setValue(String.valueOf(currentConfig.getLedStartOffset()));
        orientation.setValue(LocalizedEnum.fromBaseStr(Enums.Orientation.class, currentConfig.getOrientation()).getI18n());
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
        groupBy.setValue(currentConfig.getGroupBy());
        forceSingleDeviceMultiScreenValues();
        initGroupByCombo();
    }

    /**
     * Force single device multi screen values
     */
    private void forceSingleDeviceMultiScreenValues() {
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            switch (MainSingleton.getInstance().whoAmI) {
                case 1 -> {
                    if ((MainSingleton.getInstance().config.getMultiMonitor() != 1)) {
                        // RIGHT
                        leftLed.setDisable(true);
                        leftLed.setText(String.valueOf(0));
                        splitBottomMargin.setValue(Constants.SPLIT_BOTTOM_MARGIN_OFF);
                    }
                }
                case 2 -> {
                    if ((MainSingleton.getInstance().config.getMultiMonitor() == 2)) {
                        // LEFT
                        rightLed.setDisable(true);
                        rightLed.setText(String.valueOf(0));
                        splitBottomMargin.setValue(Constants.SPLIT_BOTTOM_MARGIN_OFF);
                    } else {
                        // CENTER
                        leftLed.setDisable(true);
                        rightLed.setDisable(true);
                        leftLed.setText(String.valueOf(0));
                        rightLed.setText(String.valueOf(0));
                    }
                }
                case 3 -> {
                    // LEFT
                    rightLed.setDisable(true);
                    rightLed.setText(String.valueOf(0));
                    splitBottomMargin.setValue(Constants.SPLIT_BOTTOM_MARGIN_OFF);
                }
            }
            if (CommonUtility.isSingleDeviceOtherInstance()) {
                ledStartOffset.setValue(String.valueOf(0));
            }
        }
    }

    /**
     * Set display label
     *
     * @param currentConfig stored config
     */
    private void setDisplayLabel(Configuration currentConfig) {
        switch (MainSingleton.getInstance().whoAmI) {
            case 1 -> {
                if ((currentConfig.getMultiMonitor() == 1)) {
                    displayLabel.setText(CommonUtility.getWord(Constants.MAIN_DISPLAY));
                } else {
                    displayLabel.setText(CommonUtility.getWord(Constants.RIGHT_DISPLAY));
                }
            }
            case 2 -> {
                if ((currentConfig.getMultiMonitor() == 2)) {
                    displayLabel.setText(CommonUtility.getWord(Constants.LEFT_DISPLAY));
                } else {
                    displayLabel.setText(CommonUtility.getWord(Constants.CENTER_DISPLAY));
                }
            }
            case 3 -> displayLabel.setText(CommonUtility.getWord(Constants.LEFT_DISPLAY));
        }
    }

    /**
     * Init all the settings listener
     */
    public void initListeners() {
        splitBottomMargin.setOnAction(_ -> splitBottomRow());
        splitBottomMargin.setOnKeyPressed(_ -> {
            if (MainSingleton.getInstance().config != null) {
                String marginInt = CommonUtility.removeChars(splitBottomMargin.getValue());
                splitBottomMargin.setValue(marginInt + Constants.PERCENT);
            }
        });
        splitBottomMargin.getEditor().textProperty().addListener((_, _, newValue) -> {
            forceFramerateValidation(newValue);
            if (splitBottomMargin.isShowing()) {
                forceFramerateValidation(newValue);
            }
        });
        splitBottomMargin.focusedProperty().addListener((_, _, focused) -> {
            if (!focused) {
                String fpsInt = CommonUtility.removeChars(splitBottomMargin.getValue());
                splitBottomMargin.setValue(fpsInt + Constants.PERCENT);
            }
        });
        topLed.setOnKeyReleased(_ -> initGroupByCombo());
        rightLed.setOnKeyReleased(_ -> initGroupByCombo());
        bottomRightLed.setOnKeyReleased(_ -> initGroupByCombo());
        bottomLeftLed.setOnKeyReleased(_ -> initGroupByCombo());
        bottomRowLed.setOnKeyReleased(_ -> initGroupByCombo());
        leftLed.setOnKeyReleased(_ -> initGroupByCombo());
    }

    /**
     * Force splitBottomMargin validation
     *
     * @param newValue combobox new value
     */
    private void forceFramerateValidation(String newValue) {
        if (MainSingleton.getInstance().config != null) {
            splitBottomMargin.cancelEdit();
            String val = CommonUtility.removeChars(newValue);
            try {
                if (val.isEmpty()) {
                    val = "0";
                }
                val = (Integer.parseInt(val) > 95) ? String.valueOf(95) : val;
                val = (Integer.parseInt(val) < 0) ? String.valueOf(0) : val;
                if (newValue.contains(Constants.PERCENT)) {
                    val += Constants.PERCENT;
                }
                splitBottomMargin.getItems().set(0, val);
                splitBottomMargin.setValue(val);
            } catch (NumberFormatException ignored) {
            }
        }
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
     *
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        settingsController.save(e);
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        checkIfCaptureRestartNeeded();
        config.setSplitBottomMargin(splitBottomMargin.getValue());
        config.setGrabberAreaTopBottom(grabberAreaTopBottom.getValue());
        config.setGrabberSide(grabberSide.getValue());
        config.setGapTypeTopBottom(gapTypeTopBottom.getValue());
        config.setGapTypeSide(gapTypeSide.getValue());
        config.setGroupBy(groupBy.getValue());
        config.setTopLed(Integer.parseInt(topLed.getText()));
        config.setLeftLed(Integer.parseInt(leftLed.getText()));
        config.setRightLed(Integer.parseInt(rightLed.getText()));
        config.setBottomLeftLed(Integer.parseInt(bottomLeftLed.getText()));
        config.setBottomRightLed(Integer.parseInt(bottomRightLed.getText()));
        config.setBottomRowLed(Integer.parseInt(bottomRowLed.getText()));
        config.setOrientation(LocalizedEnum.fromStr(Enums.Orientation.class, orientation.getValue()).getBaseI18n());
        config.setLedStartOffset(Integer.parseInt(ledStartOffset.getValue()));
        LedMatrixInfo ledMatrixInfo = getLedMatrixInfo(config);
        CommonUtility.groupByCalc(ledMatrixInfo);
        config.setGroupBy(ledMatrixInfo.getGroupBy());
        if (ledMatrixInfo.getTotaleNumOfLeds() == 0) {
            config.setTopLed(1);
        }
    }

    /**
     * Check if capture restart is needed
     */
    private void checkIfCaptureRestartNeeded() {
        if (MainSingleton.getInstance() != null && MainSingleton.getInstance().config != null) {
            boolean restartCapture = false;
            if (!MainSingleton.getInstance().config.getGrabberAreaTopBottom().equals(grabberAreaTopBottom.getValue())) {
                restartCapture = true;
            } else if (!MainSingleton.getInstance().config.getGrabberSide().equals(grabberSide.getValue())) {
                restartCapture = true;
            } else if (!MainSingleton.getInstance().config.getGapTypeTopBottom().equals(gapTypeTopBottom.getValue())) {
                restartCapture = true;
            } else if (!MainSingleton.getInstance().config.getGapTypeSide().equals(gapTypeSide.getValue())) {
                restartCapture = true;
            } else if (!MainSingleton.getInstance().config.getSplitBottomMargin().equals(splitBottomMargin.getValue())) {
                restartCapture = true;
            } else if (!MainSingleton.getInstance().config.getOrientation().equals(LocalizedEnum.fromStr(Enums.Orientation.class, orientation.getValue()).getBaseI18n())) {
                restartCapture = true;
            } else if (MainSingleton.getInstance().config.getLedStartOffset() != Integer.parseInt(ledStartOffset.getValue())) {
                restartCapture = true;
            } else if (MainSingleton.getInstance().config.getTopLed() != Integer.parseInt(topLed.getText())) {
                restartCapture = true;
            } else if (MainSingleton.getInstance().config.getLeftLed() != Integer.parseInt(leftLed.getText())) {
                restartCapture = true;
            } else if (MainSingleton.getInstance().config.getRightLed() != Integer.parseInt(rightLed.getText())) {
                restartCapture = true;
            } else if (MainSingleton.getInstance().config.getBottomLeftLed() != Integer.parseInt(bottomLeftLed.getText())) {
                restartCapture = true;
            } else if (MainSingleton.getInstance().config.getBottomRightLed() != Integer.parseInt(bottomRightLed.getText())) {
                restartCapture = true;
            } else if (MainSingleton.getInstance().config.getBottomRowLed() != Integer.parseInt(bottomRowLed.getText())) {
                restartCapture = true;
            } else if (MainSingleton.getInstance().config.getGroupBy() != groupBy.getValue()) {
                restartCapture = true;
            }
            if (restartCapture && MainSingleton.getInstance().RUNNING) {
                PipelineManager.restartCapture(() -> log.info("Restarting capture due to a change in the LEDs configuration"));
            }
        }
    }

    /**
     * Show a canvas containing a test image for the LED Matrix in use
     *
     * @param e event
     */
    @FXML
    public void showTestImage(InputEvent e) {
        MainSingleton.getInstance().guiManager.showColorCorrectionDialog(settingsController, e);
    }

    /**
     * Set form tooltips
     *
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        GuiManager.createTooltip(Constants.TOOLTIP_TOPLED, topLed);
        GuiManager.createTooltip(Constants.TOOLTIP_LEFTLED, leftLed);
        GuiManager.createTooltip(Constants.TOOLTIP_RIGHTLED, rightLed);
        GuiManager.createTooltip(Constants.TOOLTIP_BOTTOMLEFTLED, bottomLeftLed);
        GuiManager.createTooltip(Constants.TOOLTIP_BOTTOMRIGHTLED, bottomRightLed);
        GuiManager.createTooltip(Constants.TOOLTIP_BOTTOMROWLED, bottomRowLed);
        GuiManager.createTooltip(Constants.TOOLTIP_ORIENTATION, orientation);
        GuiManager.createTooltip(Constants.TOOLTIP_LEDSTARTOFFSET, ledStartOffset);
        GuiManager.createTooltip(Constants.TOOLTIP_SPLIT_BOTTOM_ROW, splitBottomMargin);
        GuiManager.createTooltip(Constants.TOOLTIP_GRABBER_AREA_TOP_BOTTOM, grabberAreaTopBottom);
        GuiManager.createTooltip(Constants.TOOLTIP_GRABBER_AREA_SIDE, grabberSide);
        GuiManager.createTooltip(Constants.TOOLTIP_CORNER_GAP, gapTypeTopBottom);
        GuiManager.createTooltip(Constants.TOOLTIP_CORNER_GAP, gapTypeSide);
        GuiManager.createTooltip(Constants.TOOLTIP_GROUP_BY, groupBy);
        if (currentConfig == null) {
            GuiManager.createTooltip(Constants.TOOLTIP_SAVELEDBUTTON_NULL, saveLedButton);
        } else {
            GuiManager.createTooltip(Constants.TOOLTIP_SHOWTESTIMAGEBUTTON, 200, showTestImageButton);
        }
    }

    /**
     * Init LED offset listener
     */
    void addLedOffsetListener() {
        ledStartOffset.getEditor().textProperty().addListener((_, _, newValue) -> {
            if (!CommonUtility.isSplitBottomRow(splitBottomMargin.getValue()) && orientation.getValue().equals(Enums.Orientation.ANTICLOCKWISE.getI18n())) {
                calcLedOffset(newValue, "0",
                        String.valueOf(Integer.parseInt(bottomRowLed.getText()) / 2), Integer.parseInt(bottomRowLed.getText()),
                        Integer.parseInt(bottomRowLed.getText()) + Integer.parseInt(rightLed.getText()),
                        Integer.parseInt(bottomRowLed.getText()) + Integer.parseInt(rightLed.getText()) + Integer.parseInt(topLed.getText()));
            } else if (!CommonUtility.isSplitBottomRow(splitBottomMargin.getValue()) && orientation.getValue().equals(Enums.Orientation.CLOCKWISE.getI18n())) {
                calcLedOffset(newValue, "0",
                        String.valueOf(Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText()) + Integer.parseInt(rightLed.getText()) + (Integer.parseInt(bottomRowLed.getText()) / 2)),
                        Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText()) + Integer.parseInt(rightLed.getText()),
                        Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()));
            } else if (CommonUtility.isSplitBottomRow(splitBottomMargin.getValue()) && orientation.getValue().equals(Enums.Orientation.ANTICLOCKWISE.getI18n())) {
                calcLedOffset(newValue, String.valueOf(Integer.parseInt(bottomRightLed.getText()) + Integer.parseInt(rightLed.getText()) + Integer.parseInt(topLed.getText()) + Integer.parseInt(leftLed.getText())), "0",
                        Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(bottomRightLed.getText()) + Integer.parseInt(rightLed.getText()),
                        Integer.parseInt(bottomRightLed.getText()) + Integer.parseInt(rightLed.getText()) + Integer.parseInt(topLed.getText()));
            } else if (CommonUtility.isSplitBottomRow(splitBottomMargin.getValue()) && orientation.getValue().equals(Enums.Orientation.CLOCKWISE.getI18n())) {
                calcLedOffset(newValue, String.valueOf(Integer.parseInt(bottomLeftLed.getText())), "0",
                        Integer.parseInt(bottomLeftLed.getText()) + Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText()) + Integer.parseInt(rightLed.getText()),
                        Integer.parseInt(bottomLeftLed.getText()) + Integer.parseInt(leftLed.getText()) + Integer.parseInt(topLed.getText()),
                        Integer.parseInt(bottomLeftLed.getText()) + Integer.parseInt(leftLed.getText()));
            }
        });
    }

    /**
     * Calculate how big must be the LED offset based on existing config
     *
     * @param newValue     combobox new value
     * @param val          existing combobox value
     * @param ledDistance1 led distance to use
     * @param ledDistance2 led distance to use
     * @param ledDistance3 led distance to use
     * @param ledDistance4 led distance to use
     */
    private void calcLedOffset(String newValue, String val, String ledDistance1, int ledDistance2, int ledDistance3, int ledDistance4) {
        if (newValue.equals(Enums.LedOffset.BOTTOM_LEFT.getI18n())) {
            setLedOffset(val);
        } else if (newValue.equals(Enums.LedOffset.BOTTOM_CENTER.getI18n())) {
            setLedOffset(ledDistance1);
        } else if (newValue.equals(Enums.LedOffset.BOTTOM_RIGHT.getI18n())) {
            setLedOffset(String.valueOf(ledDistance2));
        } else if (newValue.equals(Enums.LedOffset.UPPER_RIGHT.getI18n())) {
            setLedOffset(String.valueOf(ledDistance3));
        } else if (newValue.equals(Enums.LedOffset.UPPER_LEFT.getI18n())) {
            setLedOffset(String.valueOf(ledDistance4));
        } else {
            forceLedOffsetValidation(newValue);
        }
    }

    /**
     * Force LED offset validation
     *
     * @param newValue combobox new value
     */
    private void forceLedOffsetValidation(String newValue) {
        ledStartOffset.cancelEdit();
        if (newValue.isEmpty()) {
            setLedOffset("0");
        } else {
            String val = CommonUtility.removeChars(newValue);
            ledStartOffset.getItems().set(0, val);
            ledStartOffset.setValue(val);
        }
    }

    /**
     * Set led offset comboxbox
     *
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
        SettingsController.addTextFieldListener(topLed, false);
        SettingsController.addTextFieldListener(leftLed, false);
        SettingsController.addTextFieldListener(rightLed, false);
        SettingsController.addTextFieldListener(bottomLeftLed, false);
        SettingsController.addTextFieldListener(bottomRightLed, false);
        SettingsController.addTextFieldListener(bottomRowLed, false);
    }
}
