/*
  TcInteractionHandler.java

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

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.dpsoftware.utilities.CommonUtility.scaleDownResolution;
import static org.dpsoftware.utilities.CommonUtility.scaleUpResolution;

/**
 * Interaction handler for the test canvas
 */
@Getter
@Setter
@Slf4j
public class TcInteractionHandler {

    public final Set<LEDCoordinate> selectedLeds = new LinkedHashSet<>();
    private final TestCanvas tc;
    private final int RESIZE_RECT_SIZE = 8;
    private final Map<LEDCoordinate, Point2D> dragOffsets = new HashMap<>();
    private final Map<LEDCoordinate, Point2D> initialPositions = new HashMap<>();
    boolean draggingTile = false;
    boolean canvasClicked = false;
    private LEDCoordinate draggedLed = null;
    private LEDCoordinate resizingLed = null;
    private boolean selectionRectActive = false;
    private double selRectStartX, selRectStartY, selRectEndX, selRectEndY;

    /**
     * Constructor
     *
     * @param tc test canvas
     */
    public TcInteractionHandler(TestCanvas tc) {
        this.tc = tc;
    }

    /**
     * Manage key pressed on canvas
     */
    public void manageCanvasKeyPressed() {
        tc.getCanvas().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                tc.hideCanvas();
            }
            if (event.isControlDown() && event.getCode() == KeyCode.Z) {
                tc.injectColorDialogController();
                tc.getColorCorrectionDialogController().reset();
            }
            if (event.getCode() == KeyCode.DELETE) {
                deleteSelectedTiles();
            }
        });
    }

    /**
     * Enable dragging
     *
     * @param conf       stored config
     * @param ledMatrix  led matrix
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    public void enableDragging(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, int saturation) {
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
        tc.getCanvas().setOnMousePressed(event -> {
            int mouseX = (int) event.getX();
            int mouseY = (int) event.getY();
            draggedLed = null;
            canvasClicked = true;
            resizingLed = null;
            dragOffsets.clear();
            initialPositions.clear();
            draggingTile = false;
            boolean hitShape = false;
            // Check if you click the tile or the resize rectangle
            for (LEDCoordinate coord : ledMatrix.values()) {
                int x = scaleDownResolution(coord.getX(), conf.getOsScaling());
                int y = scaleDownResolution(coord.getY(), conf.getOsScaling());
                int w = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
                int h = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
                int mouseZoneSize = RESIZE_RECT_SIZE * 2;
                if (mouseX >= x + w - mouseZoneSize && mouseX <= x + w && mouseY >= y && mouseY <= y + mouseZoneSize) {
                    hitShape = isHitShapeTopRightCorner(ledMatrix, coord, conf, saturation);
                    break;
                }
                if (mouseX >= x + w - mouseZoneSize && mouseX <= x + w && mouseY >= y + h - mouseZoneSize && mouseY <= y + h) {
                    hitShape = isHitShapeBottomCorner(event, coord);
                    break;
                } else if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                    hitShape = isHitShape(conf, ledMatrix, event, coord, saturation);
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
                tc.getCanvas().setCursor(Cursor.CROSSHAIR);
            }
            // save initial positions of the selected tiles this ensures that during dragging the block maintains its original spacing
            initialPositions.clear();
            if (draggingTile && draggedLed != null && !selectedLeds.isEmpty()) {
                for (LEDCoordinate c : selectedLeds) {
                    initialPositions.put(c, new Point2D(scaleDownResolution(c.getX(), conf.getOsScaling()), scaleDownResolution(c.getY(), conf.getOsScaling())));
                }
            }
            tc.drawTestShapes(conf, saturation);
            drawSelectionOverlay(conf);
            FireflyLuciferin.setLedNumber(MainSingleton.getInstance().config.getDefaultLedMatrix());
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
            manageAddLed(ledMatrix, conf, saturation);
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
        tc.getCanvas().setCursor(Cursor.CLOSED_HAND);
        GuiSingleton.getInstance().colorDialog.setOpacity(0.5);
        return true;
    }

    /**
     * Show tile category dialog
     *
     * @param conf       stored config
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    private String showTileCategoryDialog(Configuration conf, int saturation) {
        AtomicReference<String> zoneName = new AtomicReference<>("");
        Dialog<String> dialog = new Dialog<>();
        MainSingleton.getInstance().guiManager.setDialogTheme(dialog);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle(CommonUtility.getWord(Constants.CANVAS_ZONE_TITLE));
        dialog.setHeaderText(CommonUtility.getWord(Constants.CANVAS_ZONE_DESCRIPTION));
        // Ok / Cancel button
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        // Editable ComboBox
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        comboBox.getItems().add(Enums.PossibleZones.TOP.getI18n());
        comboBox.getItems().add(Enums.PossibleZones.RIGHT.getI18n());
        if (CommonUtility.isSplitBottomRow(MainSingleton.getInstance().config.getSplitBottomMargin())) {
            comboBox.getItems().add(Enums.PossibleZones.BOTTOM_RIGHT.getI18n());
            comboBox.getItems().add(Enums.PossibleZones.BOTTOM_LEFT.getI18n());
        } else {
            comboBox.getItems().add(Enums.PossibleZones.BOTTOM.getI18n());
        }
        comboBox.getItems().add(Enums.PossibleZones.LEFT.getI18n());
        comboBox.setPromptText(CommonUtility.getWord(Constants.CANVAS_ZONE_TEXT));
        dialog.getDialogPane().setContent(comboBox);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return comboBox.getEditor().getText();
            }
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(zn -> {
            if (CommonUtility.isCommonZone(zn)) {
                zoneName.set(LocalizedEnum.fromStr(Enums.PossibleZones.class, zn).getBaseI18n());
            } else {
                zoneName.set(zn);
            }
            Platform.runLater(() -> {
                tc.drawTestShapes(conf, saturation);
                drawSelectionOverlay(conf);
            });
        });
        return zoneName.get();
    }

    /**
     * Orientation logic for mouse pressed
     *
     * @param ledMatrix led matrix
     * @param zoneName  zone name
     */
    private void addLedUsingOrientationLogic(LinkedHashMap<Integer, LEDCoordinate> ledMatrix, String zoneName) {
        LinkedHashMap<Integer, LEDCoordinate> newMap = new LinkedHashMap<>();
        boolean isClockwise = Enums.Orientation.CLOCKWISE.equals(LocalizedEnum.fromBaseStr(Enums.Orientation.class, MainSingleton.getInstance().config.getOrientation()));
        int nextId = 1;
        if (isClockwise) {
            // Insert the copies at the beginning
            for (LEDCoordinate c : selectedLeds) {
                LEDCoordinate copy = tc.getLedCoordinate(c, zoneName);
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
                LEDCoordinate copy = tc.getLedCoordinate(c, zoneName);
                newMap.put(nextId++, copy);
            }
        }
        // Replace the original map with the new map with updated IDs
        ledMatrix.clear();
        ledMatrix.putAll(newMap);
    }

    /**
     * Check if the tile has been pressed in the top right corner (button)
     *
     * @param ledMatrix  led matrix
     * @param coord      led coordinate
     * @param conf       stored config
     * @param saturation use full or half saturation, this is influenced by the combo box
     * @return true if clicked
     */
    private boolean isHitShapeTopRightCorner(LinkedHashMap<Integer, LEDCoordinate> ledMatrix, LEDCoordinate coord, Configuration conf, int saturation) {
        selectedLeds.add(coord);
        manageAddLed(ledMatrix, conf, saturation);
        return true;
    }

    /**
     * Manage add LED
     *
     * @param ledMatrix  led matrix
     * @param conf       stored config
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    private void manageAddLed(LinkedHashMap<Integer, LEDCoordinate> ledMatrix, Configuration conf, int saturation) {
        String zoneName = showTileCategoryDialog(conf, saturation);
        if (zoneName == null || zoneName.isEmpty()) {
            MainSingleton.getInstance().guiManager.showLocalizedNotification(Constants.CANVAS_ZONE_EMPTY_TITLE,
                    Constants.CANVAS_ZONE_EMPTY, Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.ERROR);
            canvasClicked = false;
            return;
        }
        addLedUsingOrientationLogic(ledMatrix, zoneName);
        if (Enums.AspectRatio.FULLSCREEN.getBaseI18n().equals(MainSingleton.getInstance().config.getDefaultLedMatrix())) {
            addLedUsingOrientationLogic(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.LETTERBOX.getBaseI18n()), zoneName);
            addLedUsingOrientationLogic(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.PILLARBOX.getBaseI18n()), zoneName);
        } else if (Enums.AspectRatio.PILLARBOX.getBaseI18n().equals(MainSingleton.getInstance().config.getDefaultLedMatrix())) {
            addLedUsingOrientationLogic(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()), zoneName);
            addLedUsingOrientationLogic(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.LETTERBOX.getBaseI18n()), zoneName);
        } else if (Enums.AspectRatio.LETTERBOX.getBaseI18n().equals(MainSingleton.getInstance().config.getDefaultLedMatrix())) {
            addLedUsingOrientationLogic(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()), zoneName);
            addLedUsingOrientationLogic(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.PILLARBOX.getBaseI18n()), zoneName);
        }
        updateLedParam(+1, zoneName);
        selectedLeds.clear();
        Platform.runLater(() -> {
            if (CommonUtility.isCommonZone(zoneName)) {
                tc.injectColorDialogController();
                tc.getColorCorrectionDialogController().resetLedMatrix();
            }
            tc.drawTestShapes(MainSingleton.getInstance().config, saturation);
            drawSelectionOverlay(MainSingleton.getInstance().config);
        });
        // Restart capture with new LED count
        if (MainSingleton.getInstance().RUNNING) {
            PipelineManager.restartCapture(CommonUtility::run);
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
        tc.getCanvas().setCursor(Cursor.SE_RESIZE);
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
        final int SNAP_THRESHOLD = tc.getTileDistance();
        tc.getCanvas().setOnMouseDragged(event -> {
            boolean snapEnabled = !event.isControlDown(); // disable snap when holding SHIFT
            int mouseX = (int) event.getX();
            int mouseY = (int) event.getY();
            if (selectionRectActive) {
                selRectEndX = event.getX();
                selRectEndY = event.getY();
                tc.drawTestShapes(conf, saturation);
                drawSelectionOverlay(conf);
                return;
            }
            if (draggedLed != null && !selectedLeds.isEmpty()) {
                int canvasWidth = (int) tc.getCanvas().getWidth();
                int canvasHeight = (int) tc.getCanvas().getHeight();
                // relative offset of the main tile (calculated in isHitShape at mousePressed)
                Point2D mainOffset = dragOffsets.get(draggedLed);
                int cw = scaleDownResolution(draggedLed.getWidth(), conf.getOsScaling());
                int ch = scaleDownResolution(draggedLed.getHeight(), conf.getOsScaling());
                int proposedX = (int) (mouseX - (mainOffset != null ? mainOffset.getX() : 0));
                int proposedY = (int) (mouseY - (mainOffset != null ? mainOffset.getY() : 0));
                proposedX = Math.max(0, Math.min(proposedX, canvasWidth - cw));
                proposedY = Math.max(0, Math.min(proposedY, canvasHeight - ch));
                // Snap on dragged led only
                int snappedX = proposedX;
                int snappedY = proposedY;
                if (snapEnabled) {
                    LinkedHashMap<Integer, LEDCoordinate> ledMatrix = conf.getLedMatrixInUse(Objects.requireNonNullElse(MainSingleton.getInstance().config, conf).getDefaultLedMatrix());
                    for (LEDCoordinate other : ledMatrix.values()) {
                        if (selectedLeds.contains(other)) continue;
                        int otherX = scaleDownResolution(other.getX(), conf.getOsScaling());
                        int otherY = scaleDownResolution(other.getY(), conf.getOsScaling());
                        int otherW = scaleDownResolution(other.getWidth(), conf.getOsScaling());
                        int otherH = scaleDownResolution(other.getHeight(), conf.getOsScaling());
                        int otherRight = otherX + otherW;
                        int otherBottom = otherY + otherH;
                        int otherCenterX = otherX + otherW / 2;
                        int otherCenterY = otherY + otherH / 2;
                        int draggedLeft = snappedX;
                        int draggedRight = snappedX + cw;
                        int draggedTop = snappedY;
                        int draggedBottom = snappedY + ch;
                        int draggedCenterX = snappedX + cw / 2;
                        int draggedCenterY = snappedY + ch / 2;
                        // horizontal snap to edges
                        if (Math.abs(draggedLeft - otherRight) <= SNAP_THRESHOLD) {
                            snappedX = otherRight;
                        } else if (Math.abs(draggedRight - otherX) <= SNAP_THRESHOLD) {
                            snappedX = otherX - cw;
                        }
                        // vertical snap to edges
                        if (Math.abs(draggedTop - otherBottom) <= SNAP_THRESHOLD) {
                            snappedY = otherBottom;
                        } else if (Math.abs(draggedBottom - otherY) <= SNAP_THRESHOLD) {
                            snappedY = otherY - ch;
                        }
                        // horizontal center snap
                        if (Math.abs(draggedCenterX - otherCenterX) <= SNAP_THRESHOLD) {
                            snappedX = otherCenterX - cw / 2;
                        }
                        // vertical center snap
                        if (Math.abs(draggedCenterY - otherCenterY) <= SNAP_THRESHOLD) {
                            snappedY = otherCenterY - ch / 2;
                        }
                        // same y-level snap (for horizontal alignment)
                        if (Math.abs(draggedTop - otherY) <= SNAP_THRESHOLD) {
                            snappedY = otherY;
                        } else if (Math.abs(draggedBottom - otherBottom) <= SNAP_THRESHOLD) {
                            snappedY = otherBottom - ch;
                        }
                        // same x-level snap (for vertical alignment)
                        if (Math.abs(draggedLeft - otherX) <= SNAP_THRESHOLD) {
                            snappedX = otherX;
                        } else if (Math.abs(draggedRight - otherRight) <= SNAP_THRESHOLD) {
                            snappedX = otherRight - cw;
                        }
                    }
                }
                // calculate delta relative to the INITIAL POSITIONS of the drag (not the current positions)
                Point2D startDragged = initialPositions.get(draggedLed);
                int startDraggedX = startDragged != null ? (int) startDragged.getX() : scaleDownResolution(draggedLed.getX(), conf.getOsScaling());
                int startDraggedY = startDragged != null ? (int) startDragged.getY() : scaleDownResolution(draggedLed.getY(), conf.getOsScaling());
                int deltaX = snappedX - startDraggedX;
                int deltaY = snappedY - startDraggedY;
                // move all selected tiles keeping their spacing (always use initialPositions)
                for (LEDCoordinate coord : selectedLeds) {
                    Point2D startPos = initialPositions.get(coord);
                    int startX = startPos != null ? (int) startPos.getX() : scaleDownResolution(coord.getX(), conf.getOsScaling());
                    int startY = startPos != null ? (int) startPos.getY() : scaleDownResolution(coord.getY(), conf.getOsScaling());
                    int newX = startX + deltaX;
                    int newY = startY + deltaY;
                    // Clamp
                    int w = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
                    int h = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
                    newX = Math.max(0, Math.min(newX, canvasWidth - w));
                    newY = Math.max(0, Math.min(newY, canvasHeight - h));
                    coord.setX(scaleUpResolution(newX, conf.getOsScaling()));
                    coord.setY(scaleUpResolution(newY, conf.getOsScaling()));
                }
                tc.drawTestShapes(conf, saturation);
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
                    coord.setWidth(Math.max(newW, scaleUpResolution(tc.getMIN_TILE_SIZE(), conf.getOsScaling())));
                    coord.setHeight(Math.max(newH, scaleUpResolution(tc.getMIN_TILE_SIZE(), conf.getOsScaling())));
                }
                tc.drawTestShapes(conf, saturation);
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
        tc.getCanvas().setOnMouseMoved(event -> {
            int mouseX = (int) event.getX();
            int mouseY = (int) event.getY();
            boolean overResize = false;
            boolean overShape = false;
            boolean overTopRight = false;
            for (LEDCoordinate coord : ledMatrix.values()) {
                int x = scaleDownResolution(coord.getX(), conf.getOsScaling());
                int y = scaleDownResolution(coord.getY(), conf.getOsScaling());
                int w = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
                int h = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
                int mouseZoneSize = RESIZE_RECT_SIZE * 2;
                if (mouseX >= x + w - mouseZoneSize && mouseX <= x + w && mouseY >= y && mouseY <= y + mouseZoneSize) {
                    overTopRight = true;
                    break;
                }
                if (mouseX >= x + w - mouseZoneSize && mouseX <= x + w && mouseY >= y + h - mouseZoneSize && mouseY <= y + h) {
                    overResize = true;
                    break;
                } else if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                    overShape = true;
                }
            }
            if (overTopRight) {
                tc.getCanvas().setCursor(Cursor.HAND);
            } else if (overResize) {
                tc.getCanvas().setCursor(Cursor.SE_RESIZE);
            } else if (overShape) {
                tc.getCanvas().setCursor(Cursor.OPEN_HAND);
            } else {
                tc.getCanvas().setCursor(Cursor.DEFAULT);
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
        tc.getCanvas().setOnMouseReleased(event -> {
            canvasClicked = false;
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
                initialPositions.clear(); // clear the saved positions at the end of the drag
                tc.getCanvas().setCursor(Cursor.DEFAULT);
            }
            tc.drawTestShapes(conf, saturation);
            drawSelectionOverlay(conf);
            GuiSingleton.getInstance().colorDialog.setOpacity(1.0);
        });
    }

    /**
     * Draw small rects for resize and add LED
     *
     * @param newFont font
     * @param x       x
     * @param width   width
     * @param y       y
     * @param height  height
     */
    void drawSmallRects(Font newFont, int x, int width, int y, int height) {
        tc.getGc().setFont(newFont);
        // draw small rectangle for resize
        tc.getGc().setStroke(Color.WHITE);
        tc.getGc().setLineWidth(tc.getLineWidth() + ((double) tc.getLineWidth() / 2));
        tc.getGc().strokeRect(x + width - (RESIZE_RECT_SIZE + tc.getLineWidth()), y + height - (RESIZE_RECT_SIZE + tc.getLineWidth()), RESIZE_RECT_SIZE, RESIZE_RECT_SIZE);
        // draw small rectangle to add new LED
        tc.getGc().setLineWidth(1);
        double rectX = x + width - (RESIZE_RECT_SIZE + tc.getLineWidth());
        double rectY = y + (RESIZE_RECT_SIZE);
        double rectSize = RESIZE_RECT_SIZE;
        tc.getGc().strokeRect(rectX, rectY, rectSize, rectSize);
        double centerX = rectX + rectSize / 2.0;
        double centerY = rectY + rectSize / 2.0;
        tc.getGc().strokeLine(centerX, rectY + 2, centerX, rectY + rectSize - 2);   // vertical
        tc.getGc().strokeLine(rectX + 2, centerY, rectX + rectSize - 2, centerY);   // horizontal
        tc.getGc().setLineWidth(tc.getLineWidth());
    }

    /**
     * Delete selected tiles from the ledMatrix
     */
    public void deleteSelectedTiles() {
        if (selectedLeds.isEmpty()) return;
        Configuration conf = MainSingleton.getInstance().config;
        if (conf == null) return;
        LinkedHashMap<Integer, LEDCoordinate> ledMatrix = conf.getLedMatrixInUse(conf.getDefaultLedMatrix());
        updateLedParam(-1, "");
        List<Integer> keyToRemove = new ArrayList<>();
        deleteTilesFromMatrix(ledMatrix, keyToRemove);
        if (Enums.AspectRatio.FULLSCREEN.getBaseI18n().equals(MainSingleton.getInstance().config.getDefaultLedMatrix())) {
            deleteTilesFromMatrix(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.LETTERBOX.getBaseI18n()), keyToRemove);
            deleteTilesFromMatrix(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.PILLARBOX.getBaseI18n()), keyToRemove);
        } else if (Enums.AspectRatio.PILLARBOX.getBaseI18n().equals(MainSingleton.getInstance().config.getDefaultLedMatrix())) {
            deleteTilesFromMatrix(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()), keyToRemove);
            deleteTilesFromMatrix(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.LETTERBOX.getBaseI18n()), keyToRemove);
        } else if (Enums.AspectRatio.LETTERBOX.getBaseI18n().equals(MainSingleton.getInstance().config.getDefaultLedMatrix())) {
            deleteTilesFromMatrix(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()), keyToRemove);
            deleteTilesFromMatrix(MainSingleton.getInstance().config.getLedMatrix().get(Enums.AspectRatio.PILLARBOX.getBaseI18n()), keyToRemove);
        }
        // Clear selection
        selectedLeds.clear();
        FireflyLuciferin.setLedNumber(MainSingleton.getInstance().config.getDefaultLedMatrix());
        // Redraw
        Platform.runLater(() -> {
            tc.drawTestShapes(conf, 0);
            drawSelectionOverlay(conf);
            // Restart capture with new LED count
            if (MainSingleton.getInstance().RUNNING) {
                PipelineManager.restartCapture(CommonUtility::run);
            }
        });
    }

    /**
     * Update led param on delete or add
     *
     * @param delta    delta
     * @param zoneName zone name
     */
    private void updateLedParam(int delta, String zoneName) {
        boolean updateLedParam = false;
        var config = MainSingleton.getInstance().config;
        for (LEDCoordinate selectedLed : selectedLeds) {
            String zone = zoneName.isEmpty() ? selectedLed.getZone() : zoneName;
            if (Enums.PossibleZones.TOP.getBaseI18n().equals(zone)) {
                config.setTopLed(config.getTopLed() + delta);
                updateLedParam = true;
            } else if (Enums.PossibleZones.RIGHT.getBaseI18n().equals(zone)) {
                config.setRightLed(config.getRightLed() + delta);
                updateLedParam = true;
            } else if (Enums.PossibleZones.BOTTOM_RIGHT.getBaseI18n().equals(zone)) {
                config.setBottomRightLed(config.getBottomRightLed() + delta);
                updateLedParam = true;
            } else if (Enums.PossibleZones.BOTTOM.getBaseI18n().equals(zone)) {
                config.setBottomRowLed(config.getBottomRowLed() + delta);
                updateLedParam = true;
            } else if (Enums.PossibleZones.BOTTOM_LEFT.getBaseI18n().equals(zone)) {
                config.setBottomLeftLed(config.getBottomLeftLed() + delta);
                updateLedParam = true;
            } else if (Enums.PossibleZones.LEFT.getBaseI18n().equals(zone)) {
                config.setLeftLed(config.getLeftLed() + delta);
                updateLedParam = true;
            }
        }
        if (updateLedParam) {
            tc.injectColorDialogController();
            tc.getColorCorrectionDialogController().updateLedConfigParam();
        }
    }

    /**
     * Delete tiles from matrix
     *
     * @param ledMatrix   led matrix
     * @param keyToRemove list of keys to remove
     */
    private void deleteTilesFromMatrix(LinkedHashMap<Integer, LEDCoordinate> ledMatrix, List<Integer> keyToRemove) {
        // Removed selected
        if (keyToRemove.isEmpty()) {
            for (LEDCoordinate selectedLed : selectedLeds) {
                Integer foundKey = ledMatrix.entrySet().stream().filter(entry -> entry.getValue().equals(selectedLed)).map(Map.Entry::getKey).findFirst().orElse(null);
                ledMatrix.remove(foundKey);
                keyToRemove.add(foundKey);
            }
        } else {
            for (Integer key : keyToRemove) {
                ledMatrix.remove(key);
            }
        }
        // Reindex
        LinkedHashMap<Integer, LEDCoordinate> newMap = new LinkedHashMap<>();
        int newId = 1;
        for (LEDCoordinate c : ledMatrix.values()) {
            newMap.put(newId++, c);
        }
        ledMatrix.clear();
        ledMatrix.putAll(newMap);
    }

    /**
     * Draw selection overlay
     *
     * @param conf stored config
     */
    void drawSelectionOverlay(Configuration conf) {
        // Draw overlay on top of the existing drawing
        final int LINE_DASHES_WIDTH = tc.getLineWidth() * 3;
        final int LINE_WIDTH_DOUBLE = (tc.getLineWidth() * 2) + (tc.getLineWidth() <= 1 ? 0 : 1);
        tc.getGc().save();
        tc.getGc().setLineWidth(tc.getLineWidth());
        tc.getGc().setLineDashes(LINE_DASHES_WIDTH);
        tc.getGc().setStroke(Color.CYAN);
        for (LEDCoordinate coord : selectedLeds) {
            int x = scaleDownResolution(coord.getX(), conf.getOsScaling());
            int y = scaleDownResolution(coord.getY(), conf.getOsScaling());
            int w = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
            int h = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
            tc.getGc().strokeRect(x + tc.getTileDistance() - LINE_WIDTH_DOUBLE, y + tc.getTileDistance() - LINE_WIDTH_DOUBLE, w + LINE_WIDTH_DOUBLE - tc.getTileDistance(), h - LINE_WIDTH_DOUBLE - 1);
        }
        // If the selection rectangle is active, draw it (with transparent fill)
        if (selectionRectActive) {
            double rx = Math.min(selRectStartX, selRectEndX);
            double ry = Math.min(selRectStartY, selRectEndY);
            double rw = Math.abs(selRectEndX - selRectStartX);
            double rh = Math.abs(selRectEndY - selRectStartY);
            tc.getGc().setGlobalAlpha(0.15);
            tc.getGc().setFill(Color.CYAN);
            tc.getGc().fillRect(rx, ry, rw, rh);
            tc.getGc().setGlobalAlpha(1.0);
            tc.getGc().setLineDashes(LINE_WIDTH_DOUBLE);
            tc.getGc().strokeRect(rx, ry, rw, rh);
        }
        tc.getGc().setLineDashes(0);
        tc.getGc().restore();
    }

}
