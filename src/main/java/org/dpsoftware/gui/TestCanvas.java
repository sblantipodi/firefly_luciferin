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

import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
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
import static org.dpsoftware.utilities.CommonUtility.scaleUpResolution;

/**
 * A class that draws a test image on a JavaFX Canvas, it is multi monitor aware
 */
@Slf4j
public class TestCanvas {

    private final int RESIZE_RECT_SIZE = 8;
    private final int MIN_TILE_SIZE = 40;
    private final Set<LEDCoordinate> selectedLeds = new LinkedHashSet<>();
    private final Map<LEDCoordinate, Point2D> dragOffsets = new HashMap<>();
    GraphicsContext gc;
    Canvas canvas;
    Stage stage;
    double stageX;
    double stageY;
    int imageHeight, itemsPositionY;
    private final int INITIAL_TILE_DISTANCE = 10;
    boolean draggingTile = false;
    private int tileDistance = INITIAL_TILE_DISTANCE;
    private int lineWidth;
    private LEDCoordinate draggedLed = null;
    private LEDCoordinate resizingLed = null;
    private boolean selectionRectActive = false;
    private double selRectStartX, selRectStartY, selRectEndX, selRectEndY;

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
        s = new Scene(root, scaleDownResolution(currentConfig.getScreenResX(), scaleRatio), scaleDownResolution(currentConfig.getScreenResY(), scaleRatio), Color.TRANSPARENT);
        int screenPixels = scaleDownResolution(currentConfig.getScreenResX(), scaleRatio) * scaleDownResolution(currentConfig.getScreenResY(), scaleRatio);
        tileDistance = (screenPixels * tileDistance) / 3_686_400;
        tileDistance = Math.min(tileDistance, INITIAL_TILE_DISTANCE);
        lineWidth = tileDistance / 4;

        log.info("Tale distance={}", tileDistance);

        canvas = new Canvas((scaleDownResolution(currentConfig.getScreenResX(), scaleRatio)), (scaleDownResolution(currentConfig.getScreenResY(), scaleRatio)));
        gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);
        stageX = settingStage.getX();
        stageY = settingStage.getY();
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        manageCanvasKeyPressed();
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
     * Manage key pressed on canvas
     */
    private void manageCanvasKeyPressed() {
        canvas.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hideCanvas();
            }
            if (event.isControlDown() && event.getCode() == KeyCode.Z) {
                // Inject dialog controller
                Stage stage = GuiSingleton.getInstance().colorDialog;
                if (stage != null) {
                    ColorCorrectionDialogController controller = (ColorCorrectionDialogController) stage.getProperties().get(Constants.FXML_COLOR_CORRECTION_DIALOG);
                    if (controller != null) {
                        controller.reset();
                    }
                }
            }
        });
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
    public void drawTestShapes(Configuration conf, int saturation) {
        LinkedHashMap<Integer, LEDCoordinate> ledMatrix;
        float saturationToUse;
        switch (saturation) {
            case 1 -> saturationToUse = 0.75F;
            case 2 -> saturationToUse = 0.50F;
            case 3 -> saturationToUse = 0.25F;
            default -> saturationToUse = 1.0F;
        }
        ledMatrix = conf.getLedMatrixInUse(Objects.requireNonNullElse(MainSingleton.getInstance().config, conf).getDefaultLedMatrix());
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        int scaleRatio = conf.getOsScaling();
        // 50% opacity if dragging
        if (draggingTile) {
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
        enableDragging(conf, ledMatrix, saturation);
        MainSingleton.getInstance().config.getLedMatrix().get(MainSingleton.getInstance().config.getDefaultLedMatrix()).putAll(ledMatrix);
        drawBeforeAfterText(conf, scaleRatio, saturationToUse);
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
                int taleBorder = drawTile(conf, saturationToUse, numbersList, ledNumWithOffset, x, y, width, height, colorToUse);
                // draw LED rectangle
                gc.fillRect(x + taleBorder, y + taleBorder, width - taleBorder, height - taleBorder);
                // draw LED num
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, x + taleBorder + lineWidth, y + taleBorder + 15);
                Font currentFont = gc.getFont();
                Font newFont = Font.font(currentFont.getFamily(), FontWeight.findByName(currentFont.getStyle().toUpperCase()), // bold, normal
                        FontPosture.REGULAR, currentFont.getSize() * 0.9  // height x width text is a bit smaller
                );
                gc.setFont(newFont);
                gc.fillText(height + "x" + width, x + taleBorder + lineWidth, (y + ((double) taleBorder / 2)) + height - 10);
                // TODO
                if (!Enums.PossibleZones.TOP.equals(LocalizedEnum.fromBaseStr(Enums.PossibleZones.class, coordinate.getZone()))
                        && !Enums.PossibleZones.TOP_LEFT.equals(LocalizedEnum.fromBaseStr(Enums.PossibleZones.class, coordinate.getZone()))
                        && !Enums.PossibleZones.TOP_RIGHT.equals(LocalizedEnum.fromBaseStr(Enums.PossibleZones.class, coordinate.getZone()))) {
                    gc.fillText(coordinate.getZone(), x + taleBorder + lineWidth, (y + (double) height / 2) + taleBorder);
                }
                newFont = Font.font(currentFont.getFamily(), FontWeight.findByName(currentFont.getStyle().toUpperCase()), // bold, normal
                        FontPosture.REGULAR, currentFont.getSize() * 1  // set the font size back
                );
                gc.setFont(newFont);
                // draw small rectangle for resize
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(lineWidth + ((double) lineWidth / 2));
                gc.strokeRect(x + width - (RESIZE_RECT_SIZE + lineWidth), y + height - (RESIZE_RECT_SIZE + lineWidth), RESIZE_RECT_SIZE, RESIZE_RECT_SIZE);
                gc.setLineWidth(lineWidth);
            }
        });
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
        if (draggingTile) {
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
     * Enable dragging
     *
     * @param conf       stored config
     * @param ledMatrix  led matrix
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    private void enableDragging(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, int saturation) {
        onMousePressed(conf, ledMatrix, saturation);
        onMouseDragged(conf, saturation);
        onMouseMoved(conf, ledMatrix);
        onMouseReleased(conf, ledMatrix, saturation);
    }

    /**
     * On mouse pressed
     *
     * @param conf       stored config
     * @param ledMatrix  led matrix
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    private void onMousePressed(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, int saturation) {
        canvas.setOnMousePressed(event -> {
            int mouseX = (int) event.getX();
            int mouseY = (int) event.getY();
            draggedLed = null;
            resizingLed = null;
            dragOffsets.clear();
            draggingTile = false;
            boolean hitShape = false;
            // Check if you click the tile or the resize rectangle
            for (LEDCoordinate coord : ledMatrix.values()) {
                int x = scaleDownResolution(coord.getX(), conf.getOsScaling());
                int y = scaleDownResolution(coord.getY(), conf.getOsScaling());
                int w = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
                int h = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
                boolean onBottomRightCorner = Math.abs(mouseX - (x + w)) <= RESIZE_RECT_SIZE && Math.abs(mouseY - (y + h)) <= RESIZE_RECT_SIZE;
                if (onBottomRightCorner) {
                    hitShape = isHitShapeBottomCorner(event, coord);
                    break;
                } else if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                    hitShape = isHitShape(conf, ledMatrix, event, coord, saturation);
                    break;
                }
            }
            if (!hitShape) {
                selectionRectActive = true;
                selRectStartX = event.getX();
                selRectStartY = event.getY();
                selRectEndX = selRectStartX;
                selRectEndY = selRectStartY;
                if (!event.isControlDown() && !event.isShiftDown()) {
                    selectedLeds.clear();
                }
                canvas.setCursor(Cursor.CROSSHAIR);
            }
            drawTestShapes(conf, saturation);
            drawSelectionOverlay(conf);
            Enums.AspectRatio currentAr = LocalizedEnum.fromBaseStr(Enums.AspectRatio.class, conf.getDefaultLedMatrix());
            FireflyLuciferin.setLedNumber(currentAr.getBaseI18n());
        });
    }

    /**
     * Check if the tiles has been pressed
     *
     * @param conf       stored config
     * @param ledMatrix  led matrix
     * @param event      mouse event
     * @param coord      led coordinate
     * @param saturation use full or half saturation, this is influenced by the combo box
     * @return true if the tile has been pressed
     */
    private boolean isHitShape(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, MouseEvent event, LEDCoordinate coord, int saturation) {
        draggingTile = true;
        draggedLed = coord;
        if (event.isAltDown() && selectedLeds.contains(coord)) {
            showTileCategoryDialog(conf, saturation);
            Set<LEDCoordinate> newSelection = new LinkedHashSet<>();
            // offset in pixels with respect to the original tile
            boolean isClockwise = Enums.Orientation.CLOCKWISE.equals(LocalizedEnum.fromBaseStr(Enums.Orientation.class, MainSingleton.getInstance().config.getOrientation()));
            LinkedHashMap<Integer, LEDCoordinate> newMap = new LinkedHashMap<>();
            int nextId = 1;
            orientationLogicForMousePressed(ledMatrix, isClockwise, newSelection, newMap, nextId);
            // Replace the original map with the new map with updated IDs
            ledMatrix.clear();
            ledMatrix.putAll(newMap);
            selectedLeds.clear();
            selectedLeds.addAll(newSelection);
        }
        if (event.isShiftDown()) {
            // remove the tile from the selection if present
            selectedLeds.remove(coord);
        } else if (event.isControlDown()) {
            // add to the selection if not already present
            selectedLeds.add(coord);
        } else {
            // if the tile is already selected, keep the selection as is
            if (!selectedLeds.contains(coord)) {
                selectedLeds.clear();
                selectedLeds.add(coord);
            }
        }
        // Calculate offset for all selected tiles
        dragOffsets.clear();
        for (LEDCoordinate c : selectedLeds) {
            int cx = scaleDownResolution(c.getX(), conf.getOsScaling());
            int cy = scaleDownResolution(c.getY(), conf.getOsScaling());
            dragOffsets.put(c, new Point2D(event.getX() - cx, event.getY() - cy));
        }
        canvas.setCursor(Cursor.CLOSED_HAND);
        GuiSingleton.getInstance().colorDialog.setOpacity(0.5);
        return true;
    }

    /**
     * Show tile category dialog
     *
     * @param conf       stored config
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    private void showTileCategoryDialog(Configuration conf, int saturation) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuova Tile");
        dialog.setHeaderText("Inserisci l'etichetta per la nuova tile");
        dialog.setContentText("Label:");
        MainSingleton.getInstance().guiManager.setDialogTheme(dialog);
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(label -> {
            System.out.println("Hai inserito la label: " + label);
            GuiSingleton.getInstance().colorDialog.setOpacity(1.0);
            draggingTile = false;
            drawTestShapes(conf, saturation);
            drawSelectionOverlay(conf);
        });
    }

    /**
     * Orientation logic for mouse pressed
     *
     * @param ledMatrix    led matrix
     * @param isClockwise  is clockwise
     * @param newSelection new selection
     * @param newMap       new map
     * @param nextId       next id
     */
    private void orientationLogicForMousePressed(LinkedHashMap<Integer, LEDCoordinate> ledMatrix, boolean isClockwise, Set<LEDCoordinate> newSelection, LinkedHashMap<Integer, LEDCoordinate> newMap, int nextId) {
        if (isClockwise) {
            // Insert the copies at the beginning
            for (LEDCoordinate c : selectedLeds) {
                LEDCoordinate copy = getLedCoordinate(c);
                newSelection.add(copy);
                newMap.put(nextId++, copy);
            }
            // Then all the existing tiles
            for (LEDCoordinate existing : ledMatrix.values()) {
                newMap.put(nextId++, existing);
            }
            ledMatrix.clear();
            ledMatrix.putAll(newMap);
        } else {
            // Insert the copies at the end
            for (LEDCoordinate existing : ledMatrix.values()) {
                newMap.put(nextId++, existing);
            }
            for (LEDCoordinate c : selectedLeds) {
                LEDCoordinate copy = getLedCoordinate(c);
                newSelection.add(copy);
                newMap.put(nextId++, copy);
            }
        }
    }

    /**
     * Check if the tile has been pressed in the bottom right corner for the resize
     *
     * @param event mouse event
     * @param coord led coordinate
     * @return true if the tile has been pressed in the bottom right corner for the resize
     */
    private boolean isHitShapeBottomCorner(MouseEvent event, LEDCoordinate coord) {
        draggingTile = true;
        resizingLed = coord;
        if (!event.isControlDown() && !selectedLeds.contains(coord)) {
            selectedLeds.clear();
            selectedLeds.add(coord);
        } else if (event.isControlDown()) {
            selectedLeds.add(coord);
        }
        canvas.setCursor(Cursor.SE_RESIZE);
        GuiSingleton.getInstance().colorDialog.setOpacity(0.5);
        return true;
    }

    /**
     * On mouse dragged
     *
     * @param conf       stored config
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    private void onMouseDragged(Configuration conf, int saturation) {
        canvas.setOnMouseDragged(event -> {
            int mouseX = (int) event.getX();
            int mouseY = (int) event.getY();
            if (selectionRectActive) {
                selRectEndX = event.getX();
                selRectEndY = event.getY();
                drawTestShapes(conf, saturation);
                drawSelectionOverlay(conf);
                return;
            }
            if (draggedLed != null && !selectedLeds.isEmpty()) {
                int canvasWidth = (int) canvas.getWidth();
                int canvasHeight = (int) canvas.getHeight();
                for (LEDCoordinate coord : selectedLeds) {
                    Point2D offset = dragOffsets.get(coord);
                    if (offset != null) {
                        int cw = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
                        int ch = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
                        // “Proposed” position without limits
                        int proposedX = (int) (mouseX - offset.getX());
                        int proposedY = (int) (mouseY - offset.getY());
                        // Clamp to the canvas edges
                        proposedX = Math.max(0, Math.min(proposedX, canvasWidth - cw));
                        proposedY = Math.max(0, Math.min(proposedY, canvasHeight - ch));
                        // Assign position
                        coord.setX(scaleUpResolution(proposedX, conf.getOsScaling()));
                        coord.setY(scaleUpResolution(proposedY, conf.getOsScaling()));
                    }
                }
                drawTestShapes(conf, saturation);
                drawSelectionOverlay(conf);
            } else if (resizingLed != null && !selectedLeds.isEmpty()) {
                int mouseXi = (int) event.getX();
                int mouseYi = (int) event.getY();
                int baseWidth = Math.max(1, scaleDownResolution(resizingLed.getWidth(), conf.getOsScaling()));
                int baseHeight = Math.max(1, scaleDownResolution(resizingLed.getHeight(), conf.getOsScaling()));
                double scaleX = (mouseXi - scaleDownResolution(resizingLed.getX(), conf.getOsScaling())) / (double) baseWidth;
                double scaleY = (mouseYi - scaleDownResolution(resizingLed.getY(), conf.getOsScaling())) / (double) baseHeight;
                for (LEDCoordinate coord : selectedLeds) {
                    int newW = (int) (coord.getWidth() * scaleX);
                    int newH = (int) (coord.getHeight() * scaleY);
                    coord.setWidth(Math.max(newW, scaleUpResolution(MIN_TILE_SIZE, conf.getOsScaling())));
                    coord.setHeight(Math.max(newH, scaleUpResolution(MIN_TILE_SIZE, conf.getOsScaling())));
                }
                drawTestShapes(conf, saturation);
                drawSelectionOverlay(conf);
            }
        });
    }

    /**
     * On mouse moved
     *
     * @param conf      stored config
     * @param ledMatrix led matrix
     */
    private void onMouseMoved(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix) {
        canvas.setOnMouseMoved(event -> {
            int mouseX = (int) event.getX();
            int mouseY = (int) event.getY();
            boolean overResize = false;
            boolean overShape = false;
            for (LEDCoordinate coord : ledMatrix.values()) {
                int x = scaleDownResolution(coord.getX(), conf.getOsScaling());
                int y = scaleDownResolution(coord.getY(), conf.getOsScaling());
                int w = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
                int h = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
                if (Math.abs(mouseX - (x + w)) <= RESIZE_RECT_SIZE && Math.abs(mouseY - (y + h)) <= RESIZE_RECT_SIZE) {
                    overResize = true;
                    break;
                } else if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                    overShape = true;
                }
            }
            if (overResize) {
                canvas.setCursor(Cursor.SE_RESIZE);
            } else if (overShape) {
                canvas.setCursor(Cursor.OPEN_HAND);
            } else {
                canvas.setCursor(Cursor.DEFAULT);
            }
        });
    }

    /**
     * On mouse released
     *
     * @param conf       stored config
     * @param ledMatrix  led matrix
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    private void onMouseReleased(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, int saturation) {
        canvas.setOnMouseReleased(event -> {
            draggingTile = false;
            if (selectionRectActive) {
                selectionRectActive = false;
                double rx1 = Math.min(selRectStartX, selRectEndX);
                double ry1 = Math.min(selRectStartY, selRectEndY);
                double rx2 = Math.max(selRectStartX, selRectEndX);
                double ry2 = Math.max(selRectStartY, selRectEndY);
                boolean ctrlDown = event.isControlDown();
                boolean shiftDown = event.isShiftDown();
                if (!ctrlDown && !shiftDown) {
                    selectedLeds.clear();
                }
                for (LEDCoordinate coord : ledMatrix.values()) {
                    int x = scaleDownResolution(coord.getX(), conf.getOsScaling());
                    int y = scaleDownResolution(coord.getY(), conf.getOsScaling());
                    int w = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
                    int h = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
                    if (!(x + w < rx1 || x > rx2 || y + h < ry1 || y > ry2)) {
                        selectedLeds.add(coord);
                    }
                }
            } else {
                draggedLed = null;
                resizingLed = null;
                dragOffsets.clear();
                canvas.setCursor(Cursor.DEFAULT);
            }
            drawTestShapes(conf, saturation);
            drawSelectionOverlay(conf);
            GuiSingleton.getInstance().colorDialog.setOpacity(1.0);
        });
    }

    /**
     * Get led coordinate
     *
     * @param c led coordinate
     * @return new led coordinate
     */
    private LEDCoordinate getLedCoordinate(LEDCoordinate c) {
        int canvasWidth = (int) canvas.getWidth();
        int canvasHeight = (int) canvas.getHeight();
        int newX;
        int newY;
        // Check horizontal boundaries
        if (c.getX() + MIN_TILE_SIZE + c.getWidth() > canvasWidth) newX = c.getX() - MIN_TILE_SIZE;
        else if (c.getX() - MIN_TILE_SIZE < 0) newX = c.getX() + MIN_TILE_SIZE;
        else newX = c.getX() + MIN_TILE_SIZE;
        // Check vertical boundaries
        if (c.getY() + MIN_TILE_SIZE + c.getHeight() > canvasHeight) newY = c.getY() - MIN_TILE_SIZE;
        else if (c.getY() - MIN_TILE_SIZE < 0) newY = c.getY() + MIN_TILE_SIZE;
        else newY = c.getY() + MIN_TILE_SIZE;
        return new LEDCoordinate(newX, newY, c.getWidth(), c.getHeight(), false, Enums.PossibleZones.CUSTOM.getBaseI18n());
    }

    /**
     * Draw selection overlay
     *
     * @param conf stored config
     */
    private void drawSelectionOverlay(Configuration conf) {
        // Draw overlay on top of the existing drawing
        final int LINE_DASHES_WIDTH = lineWidth * 3;
        final int LINE_WIDTH_DOUBLE = (lineWidth * 2) + (lineWidth <= 1 ? 0 : 1);
        gc.save();
        gc.setLineWidth(lineWidth);
        gc.setLineDashes(LINE_DASHES_WIDTH);
        gc.setStroke(Color.CYAN);
        for (LEDCoordinate coord : selectedLeds) {
            int x = scaleDownResolution(coord.getX(), conf.getOsScaling());
            int y = scaleDownResolution(coord.getY(), conf.getOsScaling());
            int w = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
            int h = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
            gc.strokeRect(x + tileDistance - LINE_WIDTH_DOUBLE, y + tileDistance - LINE_WIDTH_DOUBLE, w + LINE_WIDTH_DOUBLE - tileDistance, h - LINE_WIDTH_DOUBLE - 1);
        }
        // If the selection rectangle is active, draw it (with transparent fill)
        if (selectionRectActive) {
            double rx = Math.min(selRectStartX, selRectEndX);
            double ry = Math.min(selRectStartY, selRectEndY);
            double rw = Math.abs(selRectEndX - selRectStartX);
            double rh = Math.abs(selRectEndY - selRectStartY);
            gc.setGlobalAlpha(0.15);
            gc.setFill(Color.CYAN);
            gc.fillRect(rx, ry, rw, rh);
            gc.setGlobalAlpha(1.0);
            gc.setLineDashes(LINE_WIDTH_DOUBLE);
            gc.strokeRect(rx, ry, rw, rh);
        }
        gc.setLineDashes(0);
        gc.restore();
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
    private int drawTile(Configuration conf, float saturationToUse, List<Integer> numbersList, int ledNumWithOffset, int x, int y, int width, int height, int colorToUse) {
        int taleBorder = LEDCoordinate.calculateTaleBorder(conf.getScreenResX());
        gc.setFill(Color.BLACK);
        gc.fillRect(x + taleBorder, y + taleBorder, width - taleBorder, height - taleBorder);
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
        if (ledNumWithOffset == numbersList.getFirst() || ledNumWithOffset == numbersList.getLast()) {
            if (MainSingleton.getInstance().config.isMultiScreenSingleDevice() && MainSingleton.getInstance().whoAmI == 2) {
                gc.setFill(new Color(0.0, 1.0, 0.8, 1.0));
            } else if (MainSingleton.getInstance().config.isMultiScreenSingleDevice() && MainSingleton.getInstance().whoAmI == 3) {
                gc.setFill(new Color(0.7, 0.0, 1.0, 1.0));
            } else {
                gc.setFill(new Color(1.0, 0.45, 0.0, 1.0));
            }
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
