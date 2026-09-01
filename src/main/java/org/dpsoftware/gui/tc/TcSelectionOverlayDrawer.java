/*
  TcSelectionOverlayDrawer.java

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

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.gui.TestCanvas;

import java.util.Set;

import static org.dpsoftware.utilities.CommonUtility.scaleDownResolution;

/**
 * Rendering helpers for the LED selection overlay, extracted from TcInteractionHandler.
 */
public class TcSelectionOverlayDrawer {

    private static final int RESIZE_RECT_SIZE = 8;
    private static final int DOTTED_SELECTION_WIDTH = 6;

    private final TestCanvas tc;
    private final TcInteractionHandler parent;

    public TcSelectionOverlayDrawer(TestCanvas tc, TcInteractionHandler parent) {
        this.tc = tc;
        this.parent = parent;
    }

    /**
     * Draw the full selection overlay: cyan dotted borders around every selected LED
     * plus the transparent rubber-band rectangle when a drag-selection is in progress.
     */
    public void drawOverlay(Configuration conf) {
        tc.getGc().save();
        tc.getGc().setLineWidth((double) DOTTED_SELECTION_WIDTH / 2);
        tc.getGc().setLineDashes(DOTTED_SELECTION_WIDTH);
        tc.getGc().setStroke(Color.CYAN);

        Set<LEDCoordinate> selected = parent.selectedLeds;
        int offsetFix = scaleDownResolution((int) tc.getCanvas().getWidth(), conf.getOsScaling()) == 2560 ? 4 : 3;

        for (LEDCoordinate coord : selected) {
            if (coord.isGroupedLed()) continue;
            double x = scaleDownResolution(coord.getX(), conf.getOsScaling());
            double y = scaleDownResolution(coord.getY(), conf.getOsScaling());
            double w = scaleDownResolution(coord.getWidth(), conf.getOsScaling());
            double h = scaleDownResolution(coord.getHeight(), conf.getOsScaling());
            tc.getGc().strokeRect(
                    x + ((double) tc.getTileDistance() / 2) - offsetFix,
                    y + ((double) tc.getTileDistance() / 2) - offsetFix,
                    w - ((double) tc.getTileDistance() / 2) + 7,
                    h - (double) (tc.getTileDistance() / 2) + 7);
        }

        if (parent.selectionRectActive) {
            double rx = Math.min(parent.selRectStartX, parent.selRectEndX);
            double ry = Math.min(parent.selRectStartY, parent.selRectEndY);
            double rw = Math.abs(parent.selRectEndX - parent.selRectStartX);
            double rh = Math.abs(parent.selRectEndY - parent.selRectStartY);
            tc.getGc().setGlobalAlpha(0.15);
            tc.getGc().setFill(Color.CYAN);
            tc.getGc().fillRect(rx, ry, rw, rh);
            tc.getGc().setGlobalAlpha(1.0);
            tc.getGc().strokeRect(rx, ry, rw, rh);
        }

        tc.getGc().setLineDashes(0);
        tc.getGc().restore();
    }

    /**
     * Draw resize / add-LED handles on a single tile.
     * Called from TestCanvas for each tile during the main drawing pass.
     */
    public void drawHandles(Font font, int x, int width, int y, int height, Configuration conf) {
        tc.getGc().setFont(font);
        tc.getGc().setStroke(Color.WHITE);

        if (width < tc.getMAX_TEXT_RESIZE_TRIGGER()) {
            drawInvertedLMarks(x, y, width, height, conf);
        } else {
            drawPlusHandles(x, y, width, height);
        }
        tc.getGc().setLineWidth(tc.getLineWidth());
    }

    /**
     * Inverted-"L" corner markers for narrow tiles.
     */
    private void drawInvertedLMarks(int x, int y, int width, int height, Configuration conf) {
        int taleBorder = LEDCoordinate.calculateTaleBorder(conf.getScreenResX());
        tc.getGc().setLineWidth(1);
        double lineLength = RESIZE_RECT_SIZE * 0.6;

        double rightX = x + width - 1;
        double topY = y + 1 + taleBorder;
        tc.getGc().strokeLine(rightX - lineLength, topY, rightX, topY);
        tc.getGc().strokeLine(rightX, topY, rightX, topY + lineLength);

        double bottomY = y + height - 1;
        tc.getGc().strokeLine(rightX - lineLength, bottomY, rightX, bottomY);
        tc.getGc().strokeLine(rightX, bottomY - lineLength, rightX, bottomY);
    }

    /**
     * Classic "+" rectangle handles for wide tiles.
     */
    private void drawPlusHandles(int x, int y, int width, int height) {
        tc.getGc().setLineWidth(1);
        double rectX = x + width - (RESIZE_RECT_SIZE + tc.getLineWidth());
        double rectY = y + RESIZE_RECT_SIZE;
        double rectSize = RESIZE_RECT_SIZE;
        tc.getGc().strokeRect(rectX, rectY, rectSize, rectSize);

        double centerX = rectX + rectSize / 2.0;
        double centerY = rectY + rectSize / 2.0;
        tc.getGc().strokeLine(centerX, rectY + 2, centerX, rectY + rectSize - 2);
        tc.getGc().strokeLine(rectX + 2, centerY, rectX + rectSize - 2, centerY);

        // Resize rectangle bottom-right
        tc.getGc().setLineWidth(tc.getLineWidth() + ((double) tc.getLineWidth() / 2));
        tc.getGc().strokeRect(
                x + width - (RESIZE_RECT_SIZE + tc.getLineWidth()),
                y + height - (RESIZE_RECT_SIZE + tc.getLineWidth()),
                RESIZE_RECT_SIZE, RESIZE_RECT_SIZE);
    }
}
