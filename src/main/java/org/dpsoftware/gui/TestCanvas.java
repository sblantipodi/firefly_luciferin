/*
  TestCanvas.java

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
package org.dpsoftware.gui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.input.InputEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.controllers.ColorCorrectionDialogController;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.ColorUtilities;
import org.dpsoftware.utilities.CommonUtility;

import java.util.*;

import static org.dpsoftware.utilities.CommonUtility.scaleDownResolution;

/**
 * A class that draws a test image on a JavaFX Canvas, it is multi monitor aware
 */
@Slf4j
public class TestCanvas {

    GraphicsContext gc;
    private int taleDistance = 10;
    Canvas canvas;
    Stage stage;
    double stageX;
    double stageY;
    int imageHeight, itemsPositionY;

    /**
     * Show a canvas containing a test image for the LED Matrix in use
     * @param e event
     */
    public void buildAndShowTestImage(InputEvent e) {
        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readProfileInUseConfig();
        assert currentConfig != null;

        final Node source = (Node) e.getSource();
        Stage settingStage = (Stage) source.getScene().getWindow();
        settingStage.hide();
        Group root = new Group();
        Scene s;
        int scaleRatio = currentConfig.getOsScaling();
        s = new Scene(root, scaleDownResolution(currentConfig.getScreenResX(), scaleRatio), scaleDownResolution(currentConfig.getScreenResY(), scaleRatio), Color.BLACK);
        int screenPixels = scaleDownResolution(currentConfig.getScreenResX(), scaleRatio) * scaleDownResolution(currentConfig.getScreenResY(), scaleRatio);
        taleDistance = (screenPixels * taleDistance) / 3_686_400;
        taleDistance = Math.min(taleDistance, 10);
        log.debug("Tale distance=" + taleDistance);

        canvas = new Canvas((scaleDownResolution(currentConfig.getScreenResX(), scaleRatio)),
                (scaleDownResolution(currentConfig.getScreenResY(), scaleRatio)));
        gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);

        stageX = settingStage.getX();
        stageY = settingStage.getY();

        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        // Hide canvas on key pressed
        canvas.setOnKeyPressed(t -> hideCanvas());
        ColorCorrectionDialogController.selectedChannel = java.awt.Color.BLACK;
        drawTestShapes(currentConfig, null, false);
        Text fireflyLuciferin = new Text(Constants.FIREFLY_LUCIFERIN);
        fireflyLuciferin.setFill(Color.CHOCOLATE);
        fireflyLuciferin.setStyle(Constants.TC_BOLD_TEXT);
        fireflyLuciferin.setFont(Font.font(java.awt.Font.MONOSPACED, Constants.FIREFLY_LUCIFERIN_FONT_SIZE));
        Effect glow = new Glow(1.0);
        fireflyLuciferin.setEffect(glow);
        final int textPositionX = (int) ((scaleDownResolution(currentConfig.getScreenResX(), scaleRatio) / 2) - (fireflyLuciferin.getLayoutBounds().getWidth() / 2));
        int textPositionY = itemsPositionY + Constants.FIREFLY_LUCIFERIN_FONT_SIZE + imageHeight;
        fireflyLuciferin.setX(textPositionX);
        fireflyLuciferin.setY(textPositionY);
        root.getChildren().add(fireflyLuciferin);
        root.getChildren().add(canvas);
        stage.setScene(s);
        // Show canvas on the correct display number
        int index = 0;
        DisplayManager displayManager = new DisplayManager();
        for (DisplayInfo displayInfo : displayManager.getDisplayList()) {
            if (index == FireflyLuciferin.config.getMonitorNumber()) {
                stage.setX(displayInfo.getMinX());
                stage.setY(displayInfo.getMinY());
            }
            index++;
        }
        if (NativeExecutor.isLinux()) {
            stage.setFullScreen(true);
        }
        stage.show();
    }

    /**
     * Hide test image canvas
     */
    public void hideCanvas() {
        stage.setFullScreen(false);
        stage.hide();
        stage.setX(stageX);
        stage.setY(stageY);
        FireflyLuciferin.guiManager.showSettingsDialog();
    }

    /**
     * DisplayInfo a canvas, useful to test LED matrix
     * @param conf              stored config
     * @param useHalfSaturation use full or half saturation, this is influenced by the combo box
     */
    public void drawTestShapes(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrixToUse, boolean useHalfSaturation) {
        LinkedHashMap<Integer, LEDCoordinate> ledMatrix;
        float saturationToUse = useHalfSaturation ? 0.5F : 1.0F;
        boolean draw = ledMatrixToUse == null;
        ledMatrix = conf.getLedMatrixInUse(Objects.requireNonNullElse(FireflyLuciferin.config, conf).getDefaultLedMatrix());
        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(10);
        gc.stroke();
        int scaleRatio = conf.getOsScaling();
        List<Integer> numbersList = new ArrayList<>();
        ledMatrix.forEach((key, coordinate) -> {
            if (!coordinate.isGroupedLed()) {
                String ledNum = drawNumLabel(conf, key);
                numbersList.add(Integer.parseInt(ledNum.replace("#", "")));
            }
        });
        Collections.sort(numbersList);
        ledMatrix.forEach((key, coordinate) -> {
            if (!coordinate.isGroupedLed()) {
                String ledNum = drawNumLabel(conf, key);
                int ledNumWithOffset = Integer.parseInt(ledNum.replace("#", ""));
                int x = scaleDownResolution(coordinate.getX(), scaleRatio);
                int y = scaleDownResolution(coordinate.getY(), scaleRatio);
                int width = scaleDownResolution(coordinate.getWidth(), scaleRatio);
                int height = scaleDownResolution(coordinate.getHeight(), scaleRatio);
                int colorToUse = key;
                colorToUse = colorToUse / conf.getGroupBy();
                if (key > 3) {
                    while (colorToUse > 3) {
                        colorToUse -= 3;
                    }
                }
                int taleBorder = LEDCoordinate.calculateTaleBorder(conf.getScreenResX());
                gc.setFill(Color.BLACK);
                gc.fillRect(x + taleBorder, y + taleBorder, width - taleBorder, height - taleBorder);
                if (draw) {
                    if (ColorCorrectionDialogController.selectedChannel.equals(java.awt.Color.BLACK)) {
                        switch (colorToUse) {
                            case 1 -> gc.setFill(new Color(1.0F, 0F, 0F, saturationToUse));
                            case 2 -> gc.setFill(new Color(0F, 0.8F, 0F, saturationToUse));
                            default -> gc.setFill(new Color(0F, 0F, 1.0F, saturationToUse));
                        }
                    } else if (ColorCorrectionDialogController.selectedChannel.equals(java.awt.Color.WHITE)) {
                        gc.setFill(new Color(1.0F, 1.0F, 1.0F, saturationToUse));
                    } else {
                        java.awt.Color awtTileColor = ColorUtilities.HSLtoRGB(ColorCorrectionDialogController.hueTestImageValue / 360F, saturationToUse, 0.5F);
                        javafx.scene.paint.Color javafxTileColor = new Color(awtTileColor.getRed() / 255F, awtTileColor.getGreen() / 255F, awtTileColor.getBlue() / 255F, 1);
                        gc.setFill(javafxTileColor);
                    }
                }
                if (ledNumWithOffset == numbersList.get(0) || ledNumWithOffset == numbersList.get(numbersList.size() - 1)) {
                    gc.setFill(Color.ORANGE);
                }
                gc.fillRect(x + taleBorder, y + taleBorder, width - taleBorder, height - taleBorder);
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, x + taleBorder + 2, y + taleBorder + 15);
            }
        });
        drawLogo(conf, scaleRatio);
        drawBeforeAfterText(conf, scaleRatio, saturationToUse);
    }

    /**
     * Draw before and after text on canvas
     * @param conf current config from file
     * @param scaleRatio aspect ratio of the current monitor
     * @param saturationToUse use full or half saturation, this is influenced by the combo box
     */
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private void drawBeforeAfterText(Configuration conf, int scaleRatio, float saturationToUse) {
        int textPos = itemsPositionY + imageHeight + Constants.FIREFLY_LUCIFERIN_FONT_SIZE + Constants.BEFORE_AFTER_TEXT_MARGIN;
        gc.setFill(Color.BLACK);
        gc.fillRect((scaleDownResolution((conf.getScreenResX()), scaleRatio) / 2) - Constants.BEFORE_AFTER_TEXT_SIZE * 1.5,
                textPos - (Constants.BEFORE_AFTER_TEXT_MARGIN / 2), Constants.BEFORE_AFTER_TEXT_SIZE * 3, Constants.BEFORE_AFTER_TEXT_SIZE);
        if (!ColorCorrectionDialogController.selectedChannel.equals(java.awt.Color.BLACK)) {
            var ta = gc.getTextAlign();
            gc.setTextAlign(TextAlignment.CENTER);
            Effect glow = new Glow(1.0);
            gc.setEffect(glow);
            java.awt.Color hslBefore;
            if (ColorCorrectionDialogController.selectedChannel.equals(java.awt.Color.WHITE)) {
                hslBefore = new java.awt.Color(1.0F, 1.0F, 1.0F);
            } else {
                hslBefore = ColorUtilities.HSLtoRGB(ColorCorrectionDialogController.hueTestImageValue / 360F, saturationToUse, 0.5F);
            }
            gc.setFill(new Color(hslBefore.getRed() / 255F, hslBefore.getGreen() / 255F, hslBefore.getBlue() / 255F, 1));
            gc.fillText(CommonUtility.getWord(Constants.TC_BEFORE_TEXT)
                            .replace("{0}", String.valueOf(hslBefore.getRed()))
                            .replace("{1}", String.valueOf(hslBefore.getGreen()))
                            .replace("{2}", String.valueOf(hslBefore.getBlue())),
                    scaleDownResolution((conf.getScreenResX() / 2), scaleRatio), textPos);
            var hslAfter = ImageProcessor.manageColors(hslBefore.getRed(), hslBefore.getGreen(), hslBefore.getBlue());
            if (hslAfter != null) {
                hslAfter = ColorUtilities.calculateRgbMode(hslAfter.getRed(), hslAfter.getGreen(), hslAfter.getBlue());
                gc.setFill(new Color(hslAfter.getRed() / 255F, hslAfter.getGreen() / 255F, hslAfter.getBlue() / 255F, 1));
                gc.fillText(CommonUtility.getWord(Constants.TC_AFTER_TEXT)
                                .replace("{0}", String.valueOf(hslAfter.getRed()))
                                .replace("{1}", String.valueOf(hslAfter.getGreen()))
                                .replace("{2}", String.valueOf(hslAfter.getBlue())),
                        scaleDownResolution((conf.getScreenResX() / 2), scaleRatio), textPos + (Constants.BEFORE_AFTER_TEXT_MARGIN / 2));
            } else {
                hslAfter = ColorUtilities.calculateRgbMode(hslBefore.getRed(), hslBefore.getGreen(), hslBefore.getBlue());
                gc.fillText(CommonUtility.getWord(Constants.TC_AFTER_TEXT)
                                .replace("{0}", String.valueOf(hslAfter.getRed()))
                                .replace("{1}", String.valueOf(hslAfter.getGreen()))
                                .replace("{2}", String.valueOf(hslAfter.getBlue())),
                        scaleDownResolution((conf.getScreenResX() / 2), scaleRatio), textPos + (Constants.BEFORE_AFTER_TEXT_MARGIN / 2));
            }
            gc.setTextAlign(ta);
            gc.setEffect(null);
        }
    }

    /**
     * Draw Luciferin Logo
     * @param conf current config from file
     * @param scaleRatio aspect ratio of the current monitor
     */
    private void drawLogo(Configuration conf, int scaleRatio) {
        Image image = new Image(Objects.requireNonNull(getClass().getResource(Constants.IMAGE_CONTROL_LOGO)).toString());
        imageHeight = (int) image.getHeight();
        calculateLogoTextPositionY(conf, scaleRatio);
        gc.drawImage(image, scaleDownResolution((conf.getScreenResX() / 2), scaleRatio) - (image.getWidth() / 2), itemsPositionY);
    }

    /**
     * Draw LED label on the canvas
     * @param conf in memory config
     * @param key  led matrix key
     */
    String drawNumLabel(Configuration conf, Integer key) {
        int lenNumInt;
        if (Constants.Orientation.CLOCKWISE.equals((LocalizedEnum.fromBaseStr(Constants.Orientation.class, conf.getOrientation())))) {
            lenNumInt = (FireflyLuciferin.ledNumber - (key - 1) - FireflyLuciferin.config.getLedStartOffset());
            if (lenNumInt <= 0) {
                lenNumInt = (FireflyLuciferin.ledNumber + lenNumInt);
            }
        } else {
            if (key <= FireflyLuciferin.config.getLedStartOffset()) {
                lenNumInt = (FireflyLuciferin.ledNumber - (FireflyLuciferin.config.getLedStartOffset() - (key)));
            } else {
                lenNumInt = ((key) - FireflyLuciferin.config.getLedStartOffset());
            }
        }
        return "#" + lenNumInt;
    }

    /**
     * Set color correction dialog margin
     * @param stage current stage
     */
    public static void setColorCorrectionDialogMargin(Stage stage) {
        int index = 0;
        DisplayManager displayManager = new DisplayManager();
        for (DisplayInfo displayInfo : displayManager.getDisplayList()) {
            if (index == FireflyLuciferin.config.getMonitorNumber()) {
                CommonUtility.toJsonString(displayInfo);
                if (NativeExecutor.isWindows()) {
                    stage.setX((displayInfo.getMinX() + (displayInfo.getWidth() / 2)) - Constants.COLOR_CORRECTION_XMARGIN_DIALOG);
                    stage.setY((displayInfo.getMinY() + displayInfo.getHeight()) - calculateColorManagementDialogY());
                } else {
                    stage.setX((displayInfo.getWidth() / 2) - Constants.COLOR_CORRECTION_XMARGIN_DIALOG);
                    stage.setY(displayInfo.getHeight() - calculateColorManagementDialogY());
                }
            }
            index++;
        }
    }

    /**
     * Calculate dialog Y
     * @return pixels
     */
    public static int calculateColorManagementDialogY() {
        var monitorAR= CommonUtility.checkMonitorAspectRatio(FireflyLuciferin.config.getScreenResX(), FireflyLuciferin.config.getScreenResY());
        int itemPositionY = (scaleDownResolution(FireflyLuciferin.config.getScreenResY(), FireflyLuciferin.config.getOsScaling()) / Constants.HEIGHT_ROWS);
        switch (monitorAR) {
            case AR_43, AR_169 -> itemPositionY = itemPositionY * 3;
            case AR_219 -> itemPositionY = itemPositionY * 4;
            case AR_329 -> itemPositionY = itemPositionY * 2;
        }
        return Constants.COLOR_CORRECTION_DIALOG_HEIGHT + itemPositionY;
    }

    /**
     * Calculate logo and text position Y
     * @param conf current conf
     * @param scaleRatio current scale ratio
     */
    private void calculateLogoTextPositionY(Configuration conf, int scaleRatio) {
        var monitorAR = CommonUtility.checkMonitorAspectRatio(conf.getScreenResX(), conf.getScreenResY());
        itemsPositionY = (scaleDownResolution(conf.getScreenResY(), scaleRatio) / Constants.HEIGHT_ROWS);
        switch (monitorAR) {
            case AR_43, AR_169 -> itemsPositionY = itemsPositionY * 6;
            case AR_219 -> itemsPositionY = itemsPositionY * 4;
            case AR_329 -> itemsPositionY = itemsPositionY * 3;
        }
    }

}
