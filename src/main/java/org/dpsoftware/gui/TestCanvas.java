/*
  TestCanvas.java

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
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.ColorRGBW;
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
    Canvas canvas;
    Stage stage;
    double stageX;
    double stageY;
    int imageHeight, itemsPositionY;
    private int taleDistance = 10;

    /**
     * Set dialog margin
     *
     * @param stage current stage
     */
    public void setDialogMargin(Stage stage) {
        int index = 0;
        DisplayManager displayManager = new DisplayManager();
        for (DisplayInfo displayInfo : displayManager.getDisplayList()) {
            if (index == MainSingleton.getInstance().config.getMonitorNumber()) {
                CommonUtility.toJsonString(displayInfo);
                stage.setX((displayInfo.getMinX() + (displayInfo.getWidth() / 2)) - (stage.getWidth() / 2));
                stage.setY((displayInfo.getMinY() + displayInfo.getHeight()) - calculateDialogY(stage));
            }
            index++;
        }
    }

    /**
     * Calculate dialog Y
     *
     * @return pixels
     */
    public int calculateDialogY(Stage stage) {
        var monitorAR = CommonUtility.checkMonitorAspectRatio(MainSingleton.getInstance().config.getScreenResX(), MainSingleton.getInstance().config.getScreenResY());
        int rowHeight = (scaleDownResolution(MainSingleton.getInstance().config.getScreenResY(), MainSingleton.getInstance().config.getOsScaling()) / Constants.HEIGHT_ROWS);
        int itemPositionY = 0;
        switch (monitorAR) {
            case AR_43, AR_169 -> itemPositionY = rowHeight * 3;
            case AR_219 -> itemPositionY = rowHeight * 4;
            case AR_329 -> itemPositionY = rowHeight * 2;
        }
        if (MainSingleton.getInstance().config.getDefaultLedMatrix().equals(Enums.AspectRatio.LETTERBOX.getBaseI18n())) {
            itemPositionY += rowHeight;
        }
        return (int) (stage.getHeight() + itemPositionY);
    }

    /**
     * Draw LED label on the canvas
     *
     * @param conf in memory config
     * @param key  led matrix key
     */
    public String drawNumLabel(Configuration conf, Integer key) {
        int lenNumInt;
        if (Enums.Orientation.CLOCKWISE.equals((LocalizedEnum.fromBaseStr(Enums.Orientation.class, conf.getOrientation())))) {
            lenNumInt = (MainSingleton.getInstance().ledNumber - (key - 1) - MainSingleton.getInstance().config.getLedStartOffset());
            if (lenNumInt <= 0) {
                lenNumInt = (MainSingleton.getInstance().ledNumber + lenNumInt);
            }
        } else {
            if (key <= MainSingleton.getInstance().config.getLedStartOffset()) {
                lenNumInt = (MainSingleton.getInstance().ledNumber - (MainSingleton.getInstance().config.getLedStartOffset() - (key)));
            } else {
                lenNumInt = ((key) - MainSingleton.getInstance().config.getLedStartOffset());
            }
        }
        return "#" + lenNumInt;
    }

    /**
     * Show a canvas containing a test image for the LED Matrix in use
     *
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
        log.info("Tale distance={}", taleDistance);

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
        canvas.setOnKeyPressed(_ -> hideCanvas());
        GuiSingleton.getInstance().selectedChannel = java.awt.Color.BLACK;
        drawTestShapes(currentConfig, null, 0);
        Text fireflyLuciferin = new Text(Constants.FIREFLY_LUCIFERIN);
        fireflyLuciferin.setFill(Color.CHOCOLATE);
        fireflyLuciferin.setStyle(Constants.TC_BOLD_TEXT);
        fireflyLuciferin.setFont(Font.font(java.awt.Font.MONOSPACED, Constants.FIREFLY_LUCIFERIN_FONT_SIZE));
        Effect glow = new Glow(1.0);
        fireflyLuciferin.setEffect(glow);
        final int textPositionX = (int) (((double) scaleDownResolution(currentConfig.getScreenResX(), scaleRatio) / 2) - (fireflyLuciferin.getLayoutBounds().getWidth() / 2));
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
            if (index == MainSingleton.getInstance().config.getMonitorNumber()) {
                stage.setX(displayInfo.getMinX());
                stage.setY(displayInfo.getMinY());
                stage.setWidth(displayInfo.getWidth());
                stage.setHeight(displayInfo.getHeight());
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
        MainSingleton.getInstance().guiManager.showSettingsDialog(false);
    }

    /**
     * DisplayInfo a canvas, useful to test LED matrix
     *
     * @param conf       stored config
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    public void drawTestShapes(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrixToUse, int saturation) {
        LinkedHashMap<Integer, LEDCoordinate> ledMatrix;
        float saturationToUse;
        switch (saturation) {
            case 1 -> saturationToUse = 0.75F;
            case 2 -> saturationToUse = 0.50F;
            case 3 -> saturationToUse = 0.25F;
            default -> saturationToUse = 1.0F;
        }
        boolean draw = ledMatrixToUse == null;
        ledMatrix = conf.getLedMatrixInUse(Objects.requireNonNullElse(MainSingleton.getInstance().config, conf).getDefaultLedMatrix());
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
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
                int taleBorder = drawTiles(conf, saturationToUse, draw, numbersList, ledNumWithOffset, x, y, width, height, colorToUse);
                gc.fillRect(x + taleBorder, y + taleBorder, width - taleBorder, height - taleBorder);
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, x + taleBorder + 2, y + taleBorder + 15);
            }
        });
        drawLogo(conf, scaleRatio);
        drawBeforeAfterText(conf, scaleRatio, saturationToUse);
    }

    /**
     * Draw tiles
     */
    private int drawTiles(Configuration conf, float saturationToUse, boolean draw, List<Integer> numbersList,
                          int ledNumWithOffset, int x, int y, int width, int height, int colorToUse) {
        int taleBorder = LEDCoordinate.calculateTaleBorder(conf.getScreenResX());
        gc.setFill(Color.BLACK);
        gc.fillRect(x + taleBorder, y + taleBorder, width - taleBorder, height - taleBorder);
        if (draw) {
            if (GuiSingleton.getInstance().selectedChannel.equals(java.awt.Color.BLACK)) {
                switch (colorToUse) {
                    case 1 -> gc.setFill(new Color(1.0F, 0F, 0F, saturationToUse));
                    case 2 -> gc.setFill(new Color(0F, 0.8F, 0F, saturationToUse));
                    default -> gc.setFill(new Color(0F, 0F, 1.0F, saturationToUse));
                }
            } else if (GuiSingleton.getInstance().selectedChannel.equals(java.awt.Color.WHITE)) {
                gc.setFill(new Color(1.0F, 1.0F, 1.0F, saturationToUse));
            } else if (GuiSingleton.getInstance().selectedChannel.equals(java.awt.Color.GRAY)) {
                java.awt.Color awtTileColor = ColorUtilities.HSLtoRGB(0, 0, saturationToUse + ((GuiSingleton.getInstance().hueTestImageValue / 30F) / 2F));
                Color javafxTileColor = new Color(awtTileColor.getRed() / 255F, awtTileColor.getGreen() / 255F, awtTileColor.getBlue() / 255F, 1);
                // Prevent to trigger pillarbox aspect ratio if tiles are too black
                if (javafxTileColor.getRed() == 0 && javafxTileColor.getGreen() == 0 && javafxTileColor.getBlue() == 0) {
                    javafxTileColor = new Color(0.03F, 0.03F, 0.03F, 1);
                }
                gc.setFill(javafxTileColor);
            } else {
                java.awt.Color awtTileColor = ColorUtilities.HSLtoRGB(GuiSingleton.getInstance().hueTestImageValue / Constants.DEGREE_360, saturationToUse, 0.5F);
                Color javafxTileColor = new Color(awtTileColor.getRed() / 255F, awtTileColor.getGreen() / 255F, awtTileColor.getBlue() / 255F, 1);
                gc.setFill(javafxTileColor);
            }
        }
        if (ledNumWithOffset == numbersList.getFirst() || ledNumWithOffset == numbersList.getLast()) {
            gc.setFill(new Color(1.0, 0.45, 0.0, 1.0));
        }
        return taleBorder;
    }

    /**
     * Draw before and after text on canvas
     *
     * @param conf            current config from file
     * @param scaleRatio      aspect ratio of the current monitor
     * @param saturationToUse use full or half saturation, this is influenced by the combo box
     */
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private void drawBeforeAfterText(Configuration conf, int scaleRatio, float saturationToUse) {
        int textPos = itemsPositionY + imageHeight + Constants.FIREFLY_LUCIFERIN_FONT_SIZE + Constants.BEFORE_AFTER_TEXT_MARGIN;
        gc.setFill(Color.BLACK);
        gc.fillRect((scaleDownResolution((conf.getScreenResX()), scaleRatio) / 2) - Constants.BEFORE_AFTER_TEXT_SIZE * 1.5,
                textPos - (Constants.BEFORE_AFTER_TEXT_MARGIN / 2), Constants.BEFORE_AFTER_TEXT_SIZE * 3, Constants.BEFORE_AFTER_TEXT_SIZE);
        if (!GuiSingleton.getInstance().selectedChannel.equals(java.awt.Color.BLACK)) {
            var ta = gc.getTextAlign();
            gc.setTextAlign(TextAlignment.CENTER);
            Effect glow = new Glow(1.0);
            gc.setEffect(glow);
            java.awt.Color hslBefore;
            if (GuiSingleton.getInstance().selectedChannel.equals(java.awt.Color.WHITE)) {
                hslBefore = new java.awt.Color(1.0F, 1.0F, 1.0F);
            } else if (GuiSingleton.getInstance().selectedChannel.equals(java.awt.Color.GRAY)) {
                hslBefore = ColorUtilities.HSLtoRGB(0.0F, 0.0F, saturationToUse + ((GuiSingleton.getInstance().hueTestImageValue / 30F) / 2F));
            } else {
                hslBefore = ColorUtilities.HSLtoRGB(GuiSingleton.getInstance().hueTestImageValue / Constants.DEGREE_360, saturationToUse, 0.5F);
            }
            gc.setFill(new Color(hslBefore.getRed() / 255F, hslBefore.getGreen() / 255F, hslBefore.getBlue() / 255F, 1));
            gc.fillText(CommonUtility.getWord(Constants.TC_BEFORE_TEXT)
                            .replace("{0}", String.valueOf(hslBefore.getRed()))
                            .replace("{1}", String.valueOf(hslBefore.getGreen()))
                            .replace("{2}", String.valueOf(hslBefore.getBlue())),
                    scaleDownResolution((conf.getScreenResX() / 2), scaleRatio), textPos);
            var hslAfter = ImageProcessor.manageColors(hslBefore);
            ColorRGBW colorRGBW;
            if (hslAfter.getRGB() != hslBefore.getRGB()) {
                colorRGBW = ColorUtilities.calculateRgbMode(hslAfter.getRed(), hslAfter.getGreen(), hslAfter.getBlue());
                drawAfterText(conf, scaleRatio, textPos, colorRGBW);
            } else {
                colorRGBW = ColorUtilities.calculateRgbMode(hslBefore.getRed(), hslBefore.getGreen(), hslBefore.getBlue());
                drawAfterText(conf, scaleRatio, textPos, colorRGBW);
            }
            gc.setTextAlign(ta);
            gc.setEffect(null);
        }
    }

    /**
     * Draw after text
     *
     * @param conf       current config from file
     * @param scaleRatio aspect ratio of the current monitor
     * @param textPos    text position
     * @param colorRGBW  int colors
     */
    private void drawAfterText(Configuration conf, int scaleRatio, int textPos, ColorRGBW colorRGBW) {
        if (colorRGBW.getRed() == 0 && colorRGBW.getGreen() == 0 && colorRGBW.getBlue() == 0 && colorRGBW.getWhite() != 0) {
            gc.setFill(new Color(colorRGBW.getWhite() / 255F, colorRGBW.getWhite() / 255F, colorRGBW.getWhite() / 255F, 1));
        } else {
            gc.setFill(new Color(colorRGBW.getRed() / 255F, colorRGBW.getGreen() / 255F, colorRGBW.getBlue() / 255F, 1));
        }
        String afterString = (MainSingleton.getInstance().config.getColorMode() > 1) ?
                CommonUtility.getWord(Constants.TC_AFTER_TEXT_RGBW) : CommonUtility.getWord(Constants.TC_AFTER_TEXT);
        afterString = afterString.replace("{0}", String.valueOf(colorRGBW.getRed()));
        afterString = afterString.replace("{1}", String.valueOf(colorRGBW.getGreen()));
        afterString = afterString.replace("{2}", String.valueOf(colorRGBW.getBlue()));
        afterString = afterString.replace("{3}", String.valueOf(colorRGBW.getWhite()));
        //noinspection IntegerDivisionInFloatingPointContext
        gc.fillText(afterString, scaleDownResolution((conf.getScreenResX() / 2), scaleRatio),
                textPos + (Constants.BEFORE_AFTER_TEXT_MARGIN / 2));
    }

    /**
     * Draw Luciferin Logo
     *
     * @param conf       current config from file
     * @param scaleRatio aspect ratio of the current monitor
     */
    private void drawLogo(Configuration conf, int scaleRatio) {
        Image image = new Image(Objects.requireNonNull(getClass().getResource(Constants.IMAGE_CONTROL_LOGO)).toString());
        imageHeight = (int) image.getHeight();
        calculateLogoTextPositionY(conf, scaleRatio);
        gc.drawImage(image, scaleDownResolution((conf.getScreenResX() / 2), scaleRatio) - (image.getWidth() / 2), itemsPositionY);
    }

    /**
     * Calculate logo and text position Y
     *
     * @param conf       current conf
     * @param scaleRatio current scale ratio
     */
    private void calculateLogoTextPositionY(Configuration conf, int scaleRatio) {
        var monitorAR = CommonUtility.checkMonitorAspectRatio(conf.getScreenResX(), conf.getScreenResY());
        int rowHeight = (scaleDownResolution(conf.getScreenResY(), scaleRatio) / Constants.HEIGHT_ROWS);
        switch (monitorAR) {
            case AR_43, AR_169 -> itemsPositionY = rowHeight * 6;
            case AR_219 -> itemsPositionY = rowHeight * 4;
            case AR_329 -> itemsPositionY = rowHeight * 3;
        }
        if (MainSingleton.getInstance().config.getDefaultLedMatrix().equals(Enums.AspectRatio.LETTERBOX.getBaseI18n())) {
            itemsPositionY += rowHeight;
        }
    }

}
