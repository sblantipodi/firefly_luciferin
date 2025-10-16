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

import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.InputEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.controllers.ColorCorrectionDialogController;
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
@Getter
@Setter
public class TestCanvas {

    public final int MIN_TILE_SIZE = 30;
    public final int MAX_TEXT_RESIZE_TRIGGER = 40;
    private final int INITIAL_TILE_DISTANCE = 10;
    private final int HISTORY_SIZE = 100;
    public Rectangle2D closeBtnBounds;
    public boolean tooltipVisible;
    GraphicsContext gc;
    Canvas canvas;
    Stage stage;
    double stageX;
    double stageY;
    int imageHeight, itemsPositionY;
    private int tileDistance = INITIAL_TILE_DISTANCE;
    private int lineWidth;
    private ColorCorrectionDialogController colorCorrectionDialogController;
    private TcInteractionHandler interactionHandler;
    private List<Configuration> configHistory;
    private int configHistoryIdx = 1;
    private int dialogY;

    /**
     * Show a canvas containing a test image for the LED Matrix in use
     *
     * @param e event
     */
    public void buildAndShowTestImage(InputEvent e) {
        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readProfileInUseConfig();
        configHistory = new ArrayList<>();
        configHistory.add(sm.readProfileInUseConfig());
        assert currentConfig != null;
        final Node source = (Node) e.getSource();
        Stage settingStage = (Stage) source.getScene().getWindow();
        settingStage.hide();
        Group root = new Group();
        Scene s;
        int scaleRatio = currentConfig.getOsScaling();
        s = new Scene(root, scaleDownResolution(currentConfig.getScreenResX(), scaleRatio), scaleDownResolution(currentConfig.getScreenResY(), scaleRatio), Color.TRANSPARENT);
        int screenPixels = scaleDownResolution(currentConfig.getScreenResX(), scaleRatio) * scaleDownResolution(currentConfig.getScreenResY(), scaleRatio);
        tileDistance = (screenPixels * tileDistance) / 3_686_400;
        tileDistance = Math.min(tileDistance, INITIAL_TILE_DISTANCE);
        lineWidth = tileDistance / 4;
        log.info("Tale distance={}", tileDistance);
        canvas = new Canvas((scaleDownResolution(currentConfig.getScreenResX(), scaleRatio)), (scaleDownResolution(currentConfig.getScreenResY(), scaleRatio)));
        gc = canvas.getGraphicsContext2D();
        // Set bold font
        Font currentFont = gc.getFont();
        Font boldFont = Font.font(currentFont.getFamily(), FontWeight.BOLD, currentFont.getSize());
        gc.setFont(boldFont);
        canvas.setFocusTraversable(true);
        stageX = settingStage.getX();
        stageY = settingStage.getY();
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        interactionHandler = new TcInteractionHandler(this);
        interactionHandler.manageCanvasKeyPressed(0);
        GuiSingleton.getInstance().selectedChannel = java.awt.Color.BLACK;
        drawTestShapes(currentConfig, 0);
        root.getChildren().add(canvas);
        stage.setScene(s);
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
        int totalLedNumber = MainSingleton.getInstance().ledNumber;
        if (MainSingleton.getInstance().config != null && CommonUtility.isSingleDeviceMultiScreen()) {
            totalLedNumber = MainSingleton.getInstance().config.getLedMatrixInUse(MainSingleton.getInstance().config.getDefaultLedMatrix()).size();
        }
        if (Enums.Orientation.CLOCKWISE.equals((LocalizedEnum.fromBaseStr(Enums.Orientation.class, conf.getOrientation())))) {
            lenNumInt = (totalLedNumber - (key - 1) - MainSingleton.getInstance().config.getLedStartOffset());
            if (lenNumInt <= 0) {
                lenNumInt = (totalLedNumber + lenNumInt);
            }
        } else {
            if (key <= MainSingleton.getInstance().config.getLedStartOffset()) {
                lenNumInt = (totalLedNumber - (MainSingleton.getInstance().config.getLedStartOffset() - (key)));
            } else {
                lenNumInt = ((key) - MainSingleton.getInstance().config.getLedStartOffset());
            }
        }
        return "#" + lenNumInt;
    }

    /**
     * Inject dialog controller
     */
    public void injectColorDialogController() {
        // Inject dialog controller
        Stage stage = GuiSingleton.getInstance().colorDialog;
        if (stage != null) {
            colorCorrectionDialogController = (ColorCorrectionDialogController) stage.getProperties().get(Constants.FXML_COLOR_CORRECTION_DIALOG);
        }
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
     * Get hue
     *
     * @param numbersList      list of numbers
     * @param ledNumWithOffset led number with offset
     * @return hue value
     */
    private static float getHue(List<Integer> numbersList, float ledNumWithOffset) {
        int totalLeds = numbersList.size();
        // In the HSL format 360° represent the entire hue range, we limit the range from 105° to 45° to avoid the yellow, too bright for the purpose
        float hueStart = 105f;
        float hueEnd = 45f + 360f;
        // Calculate the hue for every LEDs
        float hue = hueStart + (ledNumWithOffset / totalLeds) * (hueEnd - hueStart);
        hue = hue % 360f; // don't exceed 360°
        return hue;
    }

    /**
     * Draw tiles
     *
     * @param conf            stored config
     * @param ledMatrix       led matrix
     * @param scaleRatio      aspect ratio of the current monitor
     * @param saturationToUse use full or half saturation, this is influenced by the combo box
     * @param numbersList     list of numbers to draw
     */
    private void drawTiles(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, int scaleRatio, float saturationToUse, List<Integer> numbersList) {
        Font originalFont = gc.getFont(); // salva il font originale
        ledMatrix.forEach((key, coordinate) -> {
            if (!coordinate.isGroupedLed()) {
                String ledNum = drawNumLabel(conf, key);
                int ledNumWithOffset = Integer.parseInt(ledNum.replace("#", ""));
                int x = scaleDownResolution(coordinate.getX(), scaleRatio);
                int y = scaleDownResolution(coordinate.getY(), scaleRatio);
                int width = scaleDownResolution(coordinate.getWidth(), scaleRatio);
                int height = scaleDownResolution(coordinate.getHeight(), scaleRatio);
                int colorToUse = key / conf.getGroupBy();
                if (key > 3) {
                    while (colorToUse > 3) colorToUse -= 3;
                }
                int taleBorder = drawTile(conf, saturationToUse, numbersList, ledNumWithOffset, x, y, width, height, colorToUse, coordinate);
                // draw LED rectangle
                gc.fillRect(x + taleBorder, y + taleBorder, width - taleBorder, height - taleBorder);
                // draw LED num
                gc.setFill(Color.WHITE);
                Font currentFont = gc.getFont();
                currentFont = setSmaller(height, 60, width, currentFont);
                currentFont = setSmaller(height, 55, width, currentFont);
                currentFont = setSmaller(height, 40, width, currentFont);
                currentFont = setSmaller(height, 39, width, currentFont);
                int lenNumOffset = 15;
                int onOffOffset = 7;
                int sizeOffset = 0;
                if (height < MAX_TEXT_RESIZE_TRIGGER) {
                    lenNumOffset = 10;
                    onOffOffset = 4;
                    sizeOffset = 4;
                }
                gc.fillText(ledNum, x + taleBorder + lineWidth, y + taleBorder + lenNumOffset);
                Font onFont = Font.font(currentFont.getFamily(),
                        FontWeight.findByName(currentFont.getStyle().toUpperCase()),
                        FontPosture.REGULAR,
                        currentFont.getSize() * 0.6);
                gc.setFont(onFont);
                Text text = new Text(ledNum);
                text.setFont(currentFont);
                double ledNumHeight = text.getLayoutBounds().getHeight();
                String onOff = coordinate.isActive() ? CommonUtility.capitalize(Constants.ON.toLowerCase()) : CommonUtility.capitalize(Constants.OFF.toLowerCase());
                gc.fillText(onOff, x + taleBorder + lineWidth, y + taleBorder + onOffOffset + ledNumHeight);
                currentFont = setSmaller(height, 50, width, currentFont);
                currentFont = setSmaller(height, 45, width, currentFont);
                gc.setFont(currentFont);
                Font dimFont = Font.font(currentFont.getFamily(),
                        FontWeight.findByName(currentFont.getStyle().toUpperCase()),
                        FontPosture.REGULAR,
                        currentFont.getSize() * 0.9);
                gc.setFont(dimFont);
                gc.fillText(height + "x" + width, x + taleBorder + lineWidth, ((y + ((double) taleBorder / 2)) + height - 10) + sizeOffset);
                if (!CommonUtility.isCommonZone(coordinate.getZone())) {
                    drawTruncatedText(gc, coordinate.getZone(),
                            x + taleBorder + lineWidth,
                            (y + (double) height / 2) + taleBorder,
                            width - 8);
                }
                gc.setFont(originalFont);
                interactionHandler.drawSmallRects(originalFont, x, width, y, height, conf);
            }
        });
    }

    /**
     * Draw truncated text
     *
     * @param gc       graphics context
     * @param text     text
     * @param x        position
     * @param y        position
     * @param maxWidth max width
     */
    private void drawTruncatedText(GraphicsContext gc, String text, double x, double y, double maxWidth) {
        Text helper = new Text(text);
        helper.setFont(gc.getFont());
        double textWidth = helper.getLayoutBounds().getWidth();
        if (textWidth > maxWidth) {
            String ellipsis = Constants.ELLIPSIS;
            helper.setText(ellipsis);
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                sb.append(c);
                helper.setText(sb + ellipsis);
                if (helper.getLayoutBounds().getWidth() > maxWidth) {
                    sb.setLength(sb.length() - 1);
                    break;
                }
            }
            text = sb + ellipsis;
        }
        gc.fillText(text, x, y);
    }

    /**
     * DisplayInfo a canvas, useful to test LED matrix
     *
     * @param conf       stored config
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    public void drawTestShapes(Configuration conf, int saturation) {
        LinkedHashMap<Integer, LEDCoordinate> ledMatrix;
        float saturationToUse;
        switch (saturation) {
            case 1 -> saturationToUse = 0.75F;
            case 2 -> saturationToUse = 0.50F;
            case 3 -> saturationToUse = 0.25F;
            case 4 -> saturationToUse = 0.15F;
            case 5 -> saturationToUse = 0.99F;
            default -> saturationToUse = 1.0F;
        }
        ledMatrix = conf.getLedMatrixInUse(Objects.requireNonNullElse(MainSingleton.getInstance().config, conf).getDefaultLedMatrix());
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        int scaleRatio = conf.getOsScaling();
        // 50% opacity if dragging
        if (interactionHandler.isCanvasClicked()) {
            drawLogo(conf, scaleRatio);
            gc.setFill(Color.BLACK);
            gc.setFill(new Color(0, 0, 0, 0.5));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            drawLogo(conf, scaleRatio);
        }
        drawFireflyText();
        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(INITIAL_TILE_DISTANCE);
        gc.stroke();
        List<Integer> numbersList = new ArrayList<>();
        ledMatrix.forEach((key, coordinate) -> {
            if (!coordinate.isGroupedLed()) {
                String ledNum = drawNumLabel(conf, key);
                numbersList.add(Integer.parseInt(ledNum.replace("#", "")));
            }
        });
        Collections.sort(numbersList);
        drawTiles(conf, ledMatrix, scaleRatio, saturationToUse, numbersList);
        interactionHandler.enableDragging(conf, ledMatrix, saturation);
        MainSingleton.getInstance().config.getLedMatrix().get(MainSingleton.getInstance().config.getDefaultLedMatrix()).putAll(ledMatrix);
        drawBeforeAfterText(conf, scaleRatio, saturationToUse);
        if (tooltipVisible) {
            drawTooltip(gc);
        }
    }

    /**
     * Draw tile
     *
     * @param conf             stored config
     * @param saturationToUse  use full or half saturation, this is influenced by the combo box
     * @param numbersList      list of numbers to draw
     * @param ledNumWithOffset led number with offset
     * @param x                x position
     * @param y                y position
     * @param width            width
     * @param height           height
     * @param colorToUse       color to use
     * @return new x position
     */
    private int drawTile(Configuration conf, float saturationToUse, List<Integer> numbersList, int ledNumWithOffset,
                         int x, int y, int width, int height, int colorToUse, LEDCoordinate coordinate) {
        int taleBorder = LEDCoordinate.calculateTaleBorder(conf.getScreenResX());
        gc.setFill(Color.BLACK);
        gc.fillRect(x + taleBorder, y + taleBorder, width - taleBorder, height - taleBorder);
        if (!coordinate.isActive()) {
            // black tile -> white border
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(x + taleBorder, y + taleBorder, width - taleBorder, height - taleBorder);
        } else {
            // active tile -> use normal colors
            if (GuiSingleton.getInstance().selectedChannel.equals(java.awt.Color.BLACK)) {
                if (ledNumWithOffset == numbersList.getFirst() || ledNumWithOffset == numbersList.getLast()) {
                    if (MainSingleton.getInstance().config.isMultiScreenSingleDevice() && MainSingleton.getInstance().whoAmI == 2) {
                        gc.setFill(new Color(0.0, 1.0, 0.8, 1.0));
                    } else if (MainSingleton.getInstance().config.isMultiScreenSingleDevice() && MainSingleton.getInstance().whoAmI == 3) {
                        gc.setFill(new Color(0.7, 0.0, 1.0, 1.0));
                    } else {
                        gc.setFill(new Color(1.0, 0.45, 0.0, 1.0));
                    }
                }
                if (saturationToUse == 0.99F) {
                    switch (colorToUse) {
                        case 1 -> gc.setFill(new Color(1.0F, 0F, 0F, saturationToUse));
                        case 2 -> gc.setFill(new Color(0F, 0.8F, 0F, saturationToUse));
                        default -> gc.setFill(new Color(0F, 0F, 1.0F, saturationToUse));
                    }
                } else {
                    float hue = getHue(numbersList, (float) ledNumWithOffset);
                    java.awt.Color awtRainbowColor = ColorUtilities.HSLtoRGB(hue / 360f, 1.0f, 0.5f);
                    Color javafxRainbowColor = new Color(
                            awtRainbowColor.getRed() / 255.0,
                            awtRainbowColor.getGreen() / 255.0,
                            awtRainbowColor.getBlue() / 255.0,
                            saturationToUse
                    );
                    gc.setFill(javafxRainbowColor);
                }
            } else if (GuiSingleton.getInstance().selectedChannel.equals(java.awt.Color.WHITE)) {
                gc.setFill(new Color(1.0F, 1.0F, 1.0F, saturationToUse));
            } else if (GuiSingleton.getInstance().selectedChannel.equals(java.awt.Color.GRAY)) {
                java.awt.Color awtTileColor = ColorUtilities.HSLtoRGB(0, 0, saturationToUse + ((GuiSingleton.getInstance().hueTestImageValue / 30F) / 2F));
                Color javafxTileColor = new Color(awtTileColor.getRed() / 255F, awtTileColor.getGreen() / 255F, awtTileColor.getBlue() / 255F, 1);
                if (javafxTileColor.getRed() == 0 && javafxTileColor.getGreen() == 0 && javafxTileColor.getBlue() == 0) {
                    javafxTileColor = new Color(0.03F, 0.03F, 0.03F, 1);
                }
                gc.setFill(javafxTileColor);
            } else {
                java.awt.Color awtTileColor = ColorUtilities.HSLtoRGB(GuiSingleton.getInstance().hueTestImageValue / Constants.DEGREE_360,
                        saturationToUse, 0.5F);
                Color javafxTileColor = new Color(awtTileColor.getRed() / 255F, awtTileColor.getGreen() / 255F, awtTileColor.getBlue() / 255F, 1);
                gc.setFill(javafxTileColor);
            }
            drawFirstAndLastLed(numbersList, ledNumWithOffset);
        }
        return taleBorder;
    }

    /**
     * Draw first and last LED
     *
     * @param numbersList      list of numbers
     * @param ledNumWithOffset led number with offset
     */
    private void drawFirstAndLastLed(List<Integer> numbersList, int ledNumWithOffset) {
        if (ledNumWithOffset == numbersList.getFirst() || ledNumWithOffset == numbersList.getLast()) {
            Color targetColor;
            Color startColor;
            if (MainSingleton.getInstance().config.isMultiScreenSingleDevice() && MainSingleton.getInstance().whoAmI == 2) {
                targetColor = new Color(1, 1, 1, 1.0);
                startColor = new Color(1, 1, 1, 0.5);
            } else if (MainSingleton.getInstance().config.isMultiScreenSingleDevice() && MainSingleton.getInstance().whoAmI == 3) {
                targetColor = new Color(0.75, 0.75, 0.75, 1.0);
                startColor = new Color(0.75, 0.75, 0.75, 0.5);
            } else {
                targetColor = new Color(0.5, 0.5, 0.5, 1.0);
                startColor = new Color(0.5, 0.5, 0.5, 0.2);
            }
            if (ledNumWithOffset == numbersList.getFirst()) {
                linearGradient(1, startColor, 0, targetColor);
            } else if (ledNumWithOffset == numbersList.getLast()) {
                linearGradient(0, startColor, 1, targetColor);
            }
        }
    }

    /**
     * Linear gradient
     *
     * @param v           value
     * @param startColor  start color
     * @param v1          value
     * @param targetColor target color
     */
    private void linearGradient(int v, Color startColor, int v1, Color targetColor) {
        LinearGradient lg = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop(v, startColor), new Stop(v1, targetColor));
        gc.setFill(lg);
    }

    /**
     * Set current font smaller
     *
     * @param height      dimension for resize
     * @param x           dimension for resize
     * @param width       dimension for resize
     * @param currentFont current font
     * @return new font
     */
    private Font setSmaller(int height, int x, int width, Font currentFont) {
        if (height < x || width < x) {
            currentFont = Font.font(currentFont.getFamily(), FontWeight.findByName(currentFont.getStyle().toUpperCase()), FontPosture.REGULAR, currentFont.getSize() - 1);
            gc.setFont(currentFont);
        }
        return currentFont;
    }

    /**
     * Draw Firefly text on canvas
     */
    private void drawFireflyText() {
        Text tempText = new Text(Constants.FIREFLY_LUCIFERIN);
        tempText.setFill(Color.CHOCOLATE);
        tempText.setFont(Font.font(java.awt.Font.MONOSPACED, FontWeight.BOLD, Constants.FIREFLY_LUCIFERIN_FONT_SIZE));
        tempText.setEffect(new Glow(1.0));
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        if (interactionHandler.isCanvasClicked()) {
            tempText.setOpacity(0.5F);
        } else {
            tempText.setOpacity(1.0F);
        }
        WritableImage textImage = tempText.snapshot(sp, null);
        double textPositionX = (canvas.getWidth() / 2.0) - (textImage.getWidth() / 2);
        double textPositionY = itemsPositionY + (double) Constants.FIREFLY_LUCIFERIN_FONT_SIZE / 6 + imageHeight;
        gc.drawImage(textImage, textPositionX, textPositionY);
    }

    /**
     * Draw tooltip
     *
     * @param gc gc
     */
    public void drawTooltip(GraphicsContext gc) {
        gc.save();
        double canvasWidth = gc.getCanvas().getWidth();
        double canvasHeight = gc.getCanvas().getHeight();
        gc.setFont(new Font("Arial", 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        double padding = 12;
        double lineSpacing = 4;
        String[] lines = CommonUtility.getWord(Constants.CANVAS_HELPER).split("\n");
        double textWidth = 0;
        double textHeight = 0;
        double[] lineHeights = new double[lines.length];
        for (int i = 0; i < lines.length; i++) {
            Text tempText = new Text(lines[i]);
            tempText.setFont(gc.getFont());
            double h = tempText.getLayoutBounds().getHeight();
            lineHeights[i] = h;
            textHeight += h;
            textWidth = Math.max(textWidth, tempText.getLayoutBounds().getWidth());
        }
        textHeight += lineSpacing * (lines.length - 1);
        double boxWidth = textWidth + padding * 2;
        double boxHeight = textHeight + padding * 2;
        double x = (canvasWidth - boxWidth) / 2;
        double y = (canvasHeight - boxHeight) / 2;
        // tooltip on top of the fxml dialog
        int boxMarginY = (int) ((canvasHeight / 2) + (boxHeight / 2));
        if (boxMarginY > dialogY) {
            y = dialogY - boxHeight - tileDistance;
        }
        // Background with gradient
        gc.setFill(new LinearGradient(0, y, 0, y + boxHeight, false, CycleMethod.NO_CYCLE, new Stop(0, Color.rgb(0, 0, 0, 0.9)), new Stop(1, Color.rgb(30, 30, 30, 0.9))));
        gc.fillRoundRect(x, y, boxWidth, boxHeight, 15, 15);
        // Border
        gc.setStroke(Color.rgb(255, 255, 255, 0.8));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, boxWidth, boxHeight, 15, 15);
        // Draw close btn
        double closeBtnSize = 18;
        double closeBtnX = x + boxWidth - closeBtnSize - 6;
        double closeBtnY = y + 6;
        LinearGradient closeBtnGradient = new LinearGradient(
                0, closeBtnY, 0, closeBtnY + closeBtnSize, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(50, 50, 50)),
                new Stop(1, Color.rgb(20, 20, 20))
        );
        gc.setFill(closeBtnGradient);
        gc.fillRoundRect(closeBtnX, closeBtnY, closeBtnSize, closeBtnSize, 4, 4);
        gc.setStroke(Color.rgb(255, 255, 255, 0.6));
        gc.setLineWidth(1);
        gc.strokeRoundRect(closeBtnX, closeBtnY, closeBtnSize, closeBtnSize, 4, 4);
        // Draw "X"
        gc.setStroke(Color.rgb(255, 255, 255, 0.9));
        gc.setLineWidth(2);
        double margin = 4;
        gc.strokeLine(closeBtnX + margin, closeBtnY + margin,
                closeBtnX + closeBtnSize - margin, closeBtnY + closeBtnSize - margin);
        gc.strokeLine(closeBtnX + margin, closeBtnY + closeBtnSize - margin,
                closeBtnX + closeBtnSize - margin, closeBtnY + margin);
        this.closeBtnBounds = new Rectangle2D(closeBtnX, closeBtnY, closeBtnSize, closeBtnSize);
        double textY = y + padding + (boxHeight - 2 * padding - textHeight) / 2 + lineHeights[0] / 2;
        for (int i = 0; i < lines.length; i++) {
            gc.setFill(Color.WHITE);
            gc.fillText(lines[i], canvasWidth / 2, textY);
            if (i < lines.length - 1) {
                textY += lineHeights[i] + lineSpacing;
            }
        }
        gc.restore();
    }

    /**
     * Get led coordinate
     *
     * @param c        led coordinate
     * @param zoneName zone name
     * @return new led coordinate
     */
    LEDCoordinate getLedCoordinate(LEDCoordinate c, String zoneName) {
        int canvasWidth = (int) canvas.getWidth();
        int canvasHeight = (int) canvas.getHeight();
        int newX;
        int newY;
        int distanceFromTile = (int) (MIN_TILE_SIZE * 1.5);
        // Check horizontal boundaries
        if (c.getX() + distanceFromTile + c.getWidth() > canvasWidth) newX = c.getX() - distanceFromTile;
        else if (c.getX() - distanceFromTile < 0) newX = c.getX() + distanceFromTile;
        else newX = c.getX() + distanceFromTile;
        // Check vertical boundaries
        if (c.getY() + distanceFromTile + c.getHeight() > canvasHeight) newY = c.getY() - distanceFromTile;
        else if (c.getY() - distanceFromTile < 0) newY = c.getY() + distanceFromTile;
        else newY = c.getY() + distanceFromTile;
        return new LEDCoordinate(newX, newY, c.getWidth(), c.getHeight(), false, zoneName);
    }

    /**
     * Draw before and after text on canvas
     *
     * @param conf            current config from file
     * @param scaleRatio      aspect ratio of the current monitor
     * @param saturationToUse use full or half saturation, this is influenced by the combo box
     */
    private void drawBeforeAfterText(Configuration conf, int scaleRatio, float saturationToUse) {
        int textPos = itemsPositionY + imageHeight + Constants.FIREFLY_LUCIFERIN_FONT_SIZE + Constants.BEFORE_AFTER_TEXT_MARGIN;
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
            gc.fillText(CommonUtility.getWord(Constants.TC_BEFORE_TEXT).replace("{0}", String.valueOf(hslBefore.getRed())).replace("{1}", String.valueOf(hslBefore.getGreen())).replace("{2}", String.valueOf(hslBefore.getBlue())), scaleDownResolution((conf.getScreenResX() / 2), scaleRatio), textPos);
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
        String afterString = (MainSingleton.getInstance().config.getColorMode() > 1) ? CommonUtility.getWord(Constants.TC_AFTER_TEXT_RGBW) : CommonUtility.getWord(Constants.TC_AFTER_TEXT);
        afterString = afterString.replace("{0}", String.valueOf(colorRGBW.getRed()));
        afterString = afterString.replace("{1}", String.valueOf(colorRGBW.getGreen()));
        afterString = afterString.replace("{2}", String.valueOf(colorRGBW.getBlue()));
        afterString = afterString.replace("{3}", String.valueOf(colorRGBW.getWhite()));
        //noinspection IntegerDivisionInFloatingPointContext
        gc.fillText(afterString, scaleDownResolution((conf.getScreenResX() / 2), scaleRatio), textPos + (Constants.BEFORE_AFTER_TEXT_MARGIN / 2));
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
