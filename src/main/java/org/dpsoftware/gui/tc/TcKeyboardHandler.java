/*
  TcKeyboardHandler.java

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
package org.dpsoftware.gui.tc;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.gui.TestCanvas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.dpsoftware.utilities.CommonUtility.scaleUpResolution;

/**
 * Keyboard event handling for the test canvas, extracted from TcInteractionHandler.
 */
@Slf4j
public class TcKeyboardHandler {

    private final TestCanvas tc;
    private final TcInteractionHandler parent;

    /**
     * Constructor
     *
     * @param tc     test canvas reference for delegate drawing and canvas access
     * @param parent interaction handler that owns history, overlay, and CRUD operations
     */
    public TcKeyboardHandler(TestCanvas tc, TcInteractionHandler parent) {
        this.tc = tc;
        this.parent = parent;
    }

    /**
     * Register all keyboard listeners on the canvas.
     *
     * @param saturation use full or half saturation, this is influenced by the combo box
     */
    public void setupKeyboardListeners(int saturation) {
        installKeyPressed(saturation);
        installKeyReleased();
    }

    /**
     * Register the canvas key-pressed listener.
     * Handles ESC (hide canvas), Ctrl+Z/Y (undo/redo), DELETE, arrow-key movement, and TAB rearrangement.
     */
    private void installKeyPressed(int saturation) {
        tc.getCanvas().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                tc.hideCanvas();
                return;
            }
            handleUndoRedo(event, saturation);
            if (event.getCode() == KeyCode.DELETE) {
                parent.deleteSelectedTiles();
            }
            keyboardClamp(saturation, event);
            handleTabKey(event);
        });
    }

    /**
     * Register the canvas key-released listener.
     * Resets the canvas-clicked flag and redraws when TAB, CTRL or SHIFT are released.
     */
    private void installKeyReleased() {
        tc.getCanvas().setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.CONTROL || event.getCode() == KeyCode.SHIFT) {
                parent.setCanvasClicked(false);
                tc.drawTestShapes(MainSingleton.getInstance().config, 0);
                parent.drawSelectionOverlay(MainSingleton.getInstance().config);
            }
        });
    }

    /**
     * Handle undo (Ctrl+Z) and redo (Ctrl+Y) by advancing or retracting the configuration history index.
     *
     * @param event      the key event
     * @param saturation current saturation value
     */
    private void handleUndoRedo(KeyEvent event, int saturation) {
        if (event.isControlDown() && event.getCode() == KeyCode.Z) {
            if ((tc.getConfigHistory().size() - tc.getConfigHistoryIdx()) > 0) {
                parent.manageHistory(tc.getConfigHistoryIdx() + 1, saturation);
            }
        }
        if (event.isControlDown() && event.getCode() == KeyCode.Y) {
            if (tc.getConfigHistory().size() - tc.getConfigHistoryIdx() < (tc.getConfigHistory().size() - 1)) {
                parent.manageHistory(tc.getConfigHistoryIdx() - 1, saturation);
            }
        }
    }

    /**
     * Handle TAB key for tile rearrangement.
     * Plain TAB triggers shrink-and-equalize, SHIFT+TAB triggers edge-aware equalization.
     * A history entry is saved after the operation so the change can be undone.
     *
     * @param event the key event
     */
    private void handleTabKey(KeyEvent event) {
        if (event.getCode() == KeyCode.TAB) {
            boolean shiftDown = event.isShiftDown();
            parent.setCanvasClicked(true);
            if (shiftDown) {
                equalizeTilesEdgeAware();
            } else {
                shrinkAndEqualizeTiles();
            }
            tc.drawTestShapes(MainSingleton.getInstance().config, 0);
            parent.drawSelectionOverlay(MainSingleton.getInstance().config);
            parent.manageHistoryOnRelease(MainSingleton.getInstance().config);
        }
    }

    /**
     * Move selected tiles one step using arrow keys.
     * Holding Ctrl increases the step size from 1 to 10.
     */
    private void keyboardClamp(int saturation, KeyEvent event) {
        if (parent.getSelectedLeds().isEmpty()) return;
        Configuration conf = MainSingleton.getInstance().config;
        int scaledCanvasHeight = scaleUpResolution((int) tc.getCanvas().getHeight(), conf.getOsScaling());
        int scaledCanvasWidth = scaleUpResolution((int) tc.getCanvas().getWidth(), conf.getOsScaling());

        if (event.getCode() != KeyCode.UP && event.getCode() != KeyCode.DOWN
                && event.getCode() != KeyCode.LEFT && event.getCode() != KeyCode.RIGHT) {
            return;
        }

        int step = event.isControlDown() ? 10 : 1;
        int dx = 0, dy = 0;
        switch (event.getCode()) {
            case UP -> dy = -step;
            case DOWN -> dy = step;
            case LEFT -> dx = -step;
            case RIGHT -> dx = step;
        }
        for (LEDCoordinate coord : parent.getSelectedLeds()) {
            coord.setX(Math.clamp(coord.getX() + dx, 0, scaledCanvasWidth - coord.getWidth()));
            coord.setY(Math.clamp(coord.getY() + dy, 0, scaledCanvasHeight - coord.getHeight()));
        }
        tc.drawTestShapes(conf, saturation);
        parent.drawSelectionOverlay(conf);
    }

    /**
     * Equalise selected tiles so they fill their bounding box evenly ( SHIFT + TAB ).
     */
    void equalizeTilesEdgeAware() {
        var tiles = filterSelectable(new ArrayList<>(parent.getSelectedLeds()));
        if (tiles.size() < 2) return;

        BoundingBox bb = boundingBox(tiles);
        boolean horizontal = (bb.maxX - bb.minX) > (bb.maxY - bb.minY);

        if (horizontal) {
            tiles.sort(Comparator.comparingInt(LEDCoordinate::getX));
            int totalWidth = bb.maxX - bb.minX;
            int count = tiles.size();
            int baseWidth = totalWidth / count;
            int remainder = totalWidth % count;
            int currentX = bb.minX;
            for (int i = 0; i < count; i++) {
                LEDCoordinate t = tiles.get(i);
                int w = baseWidth + (i < remainder ? 1 : 0);
                t.setWidth(Math.max(w, tc.getMIN_TILE_SIZE()));
                t.setX(currentX);
                currentX += w;
            }
        } else {
            tiles.sort(Comparator.comparingInt(LEDCoordinate::getY));
            int totalHeight = bb.maxY - bb.minY;
            int count = tiles.size();
            int baseHeight = totalHeight / count;
            int remainder = totalHeight % count;
            int currentY = bb.minY;
            for (int i = 0; i < count; i++) {
                LEDCoordinate t = tiles.get(i);
                int h = baseHeight + (i < remainder ? 1 : 0);
                t.setHeight(Math.max(h, tc.getMIN_TILE_SIZE()));
                t.setY(currentY);
                currentY += h;
            }
        }
    }

    /**
     * Shrink overlapping tiles and redistribute them evenly ( TAB ).
     */
    void shrinkAndEqualizeTiles() {
        var tiles = filterSelectable(new ArrayList<>(parent.getSelectedLeds()));
        if (tiles.size() < 2) return;

        BoundingBox bb = boundingBox(tiles);
        boolean horizontal = (bb.maxX - bb.minX) > (bb.maxY - bb.minY);

        if (horizontal) {
            tiles.sort(Comparator.comparingInt(LEDCoordinate::getX));
        } else {
            tiles.sort(Comparator.comparingInt(LEDCoordinate::getY));
        }

        if (horizontal) {
            int space = (bb.maxX - bb.minX) / tiles.size();
            int newWidth = Math.max(space - 1, tc.getMIN_TILE_SIZE());
            for (int i = 0; i < tiles.size(); i++) {
                LEDCoordinate t = tiles.get(i);
                t.setWidth(newWidth);
                t.setX(bb.minX + i * space);
            }
        } else {
            int space = (bb.maxY - bb.minY) / tiles.size();
            int newHeight = Math.max(space - 1, tc.getMIN_TILE_SIZE());
            for (int i = 0; i < tiles.size(); i++) {
                LEDCoordinate t = tiles.get(i);
                t.setHeight(newHeight);
                t.setY(bb.minY + i * space);
            }
        }
    }

    /**
     * Remove grouped LEDs from the tile list in-place so only user-selectable tiles remain.
     *
     * @param tiles mutable list to filter
     * @return the filtered list (same reference)
     */
    private List<LEDCoordinate> filterSelectable(List<LEDCoordinate> tiles) {
        tiles.removeIf(LEDCoordinate::isGroupedLed);
        return tiles;
    }

    /**
     * Compute the minimal axis-aligned bounding rectangle that encloses all tiles.
     *
     * @param tiles list of LED coordinates
     * @return bounding box spanning min/max edges across every tile
     */
    private BoundingBox boundingBox(List<LEDCoordinate> tiles) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (LEDCoordinate t : tiles) {
            minX = Math.min(minX, t.getX());
            minY = Math.min(minY, t.getY());
            maxX = Math.max(maxX, t.getX() + t.getWidth());
            maxY = Math.max(maxY, t.getY() + t.getHeight());
        }
        return new BoundingBox(minX, minY, maxX, maxY);
    }

    private record BoundingBox(int minX, int minY, int maxX, int maxY) {
    }
}
