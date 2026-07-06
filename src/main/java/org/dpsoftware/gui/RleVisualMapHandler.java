/*
  RleVisualMapHandler.java

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
package org.dpsoftware.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.network.NetworkSingleton;
import org.dpsoftware.utilities.CommonUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all RLE (Run-Length Encoding) visual map overlay logic extracted from TestCanvas.
 * Follows the same delegation pattern as TcInteractionHandler: holds a reference back to
 * TestCanvas and delegates drawing through its GraphicsContext, Stage, and helpers.
 */
@Getter
@Setter
@Slf4j
public class RleVisualMapHandler {

    private final TestCanvas tc;
    private Rectangle2D rleOverlayXBounds;
    private Rectangle2D rleOverlayPanelBounds;
    private boolean rleOverlayOnlyMode;
    private double rleOverlayYOffset = 0;
    private Timeline rleOverlayAnimation;

    /**
     * Constructor
     *
     * @param tc test canvas reference for delegating drawing and stage access
     */
    public RleVisualMapHandler(TestCanvas tc) {
        this.tc = tc;
    }

    /**
     * Update the stage and canvas bounds according to the active mode.
     * Resizes and moves the stage to wrap only the RLE panel in overlay-only mode,
     * or restores it to full monitor bounds in normal mode.
     */
    public void updateStageBounds() {
        DisplayInfo displayInfo = new DisplayManager().getDisplayInfo(MainSingleton.getInstance().config.getMonitorNumber());
        if (displayInfo == null) {
            return;
        }
        double virtualWidth = displayInfo.getWidth();
        double virtualHeight = displayInfo.getHeight();
        if (rleOverlayOnlyMode) {
            RleLayout layout = calculateRleLayout(virtualWidth, virtualHeight);
            double panelX = layout.leftMargin - 4;
            double panelY = layout.panelTopY - 2;
            double panelW = layout.availWidth + 8;
            double panelH = layout.panelBottomY - layout.panelTopY;
            if (tc.getStage().isFullScreen()) {
                tc.getStage().setFullScreen(false);
            }
            tc.getStage().setX(displayInfo.getMinX() + panelX);
            tc.getStage().setY(displayInfo.getMinY() + panelY);
            tc.getStage().setWidth(panelW);
            tc.getStage().setHeight(panelH);
            tc.getCanvas().setWidth(panelW);
            tc.getCanvas().setHeight(panelH);
        } else {
            if (NativeExecutor.isLinux()) {
                tc.getStage().setFullScreen(true);
            } else {
                tc.getStage().setX(displayInfo.getMinX());
                tc.getStage().setY(displayInfo.getMinY());
                tc.getStage().setWidth(virtualWidth);
                tc.getStage().setHeight(virtualHeight);
            }
            tc.getCanvas().setWidth(virtualWidth);
            tc.getCanvas().setHeight(virtualHeight);
        }
    }

    /**
     * Enter overlay-only mode: a periodic animation redraws only the RLE overlay on a black background.
     */
    public void startOverlayOnlyMode() {
        int minSpeed = Integer.parseInt(Constants.DEFAULT_FRAMERATE);
        rleOverlayOnlyMode = true;
        updateStageBounds();
        tc.getStage().setAlwaysOnTop(true);
        tc.getStage().toFront();
        if (rleOverlayAnimation != null) {
            rleOverlayAnimation.stop();
        }
        drawOverlayOnly();
        double gwFps = MainSingleton.getInstance().FPS_GW_CONSUMER < minSpeed ? minSpeed : MainSingleton.getInstance().FPS_GW_CONSUMER;
        KeyFrame frame = new KeyFrame(javafx.util.Duration.millis(1000 / gwFps), _ -> drawOverlayOnly());
        rleOverlayAnimation = new Timeline(frame);
        rleOverlayAnimation.setCycleCount(Integer.MAX_VALUE);
        rleOverlayAnimation.playFromStart();
    }

    /**
     * Exit overlay-only mode: stop the animation and restore normal rendering.
     */
    public void stopOverlayOnlyMode() {
        rleOverlayOnlyMode = false;
        updateStageBounds();
        tc.getStage().setAlwaysOnTop(false);
        if (rleOverlayAnimation != null) {
            rleOverlayAnimation.stop();
            rleOverlayAnimation = null;
        }
        if (MainSingleton.getInstance().config != null) {
            tc.drawTestShapes(MainSingleton.getInstance().config, 0);
        }
    }

    /**
     * Clear canvas to black and draw only the RLE overlay. Used by the overlay-only animation loop.
     */
    public void drawOverlayOnly() {
        GraphicsContext gc = tc.getGc();
        gc.clearRect(0, 0, tc.getCanvas().getWidth(), tc.getCanvas().getHeight());
        if (GuiSingleton.getInstance().rleVisualMapVisible || rleOverlayOnlyMode) {
            drawRleVisualMap();
        }
    }

    /**
     * Draw the RLE visual map on the canvas: layout calculation, background panel, and delegate content drawing.
     */
    public void drawRleVisualMap() {
        if (NetworkSingleton.lastRleEntries.isEmpty()) {
            return;
        }
        DisplayInfo displayInfo = new DisplayManager().getDisplayInfo(MainSingleton.getInstance().config.getMonitorNumber());
        double virtualWidth = displayInfo != null ? displayInfo.getWidth() : tc.getCanvas().getWidth();
        double virtualHeight = displayInfo != null ? displayInfo.getHeight() : tc.getCanvas().getHeight();
        RleLayout layout = calculateRleLayout(virtualWidth, virtualHeight);
        Font fpsFont = Font.font(java.awt.Font.MONOSPACED, FontWeight.BOLD, 11);
        GraphicsContext gc = tc.getGc();
        gc.save();
        if (rleOverlayOnlyMode) {
            gc.translate(-(layout.leftMargin - 4), -(layout.panelTopY - 2));
        }
        gc.setFill(new Color(0, 0, 0, 0.65));
        gc.fillRoundRect(layout.leftMargin - 4, layout.panelTopY - 2, layout.availWidth + 8, layout.panelBottomY - layout.panelTopY, 4, 4);
        this.rleOverlayPanelBounds = new Rectangle2D(
                rleOverlayOnlyMode ? 0 : layout.leftMargin - 4,
                rleOverlayOnlyMode ? 0 : layout.panelTopY - 2,
                layout.availWidth + 8,
                layout.panelBottomY - layout.panelTopY
        );
        double rleCloseSize = 14;
        double rleCloseMargin = 6;
        double rleCloseX = layout.leftMargin + layout.availWidth + 8 - rleCloseSize - rleCloseMargin;
        double rleCloseY = layout.panelTopY - 2 + rleCloseMargin;
        tc.drawCloseButton(rleCloseX, rleCloseY, rleCloseSize);
        this.rleOverlayXBounds = new Rectangle2D(
                rleOverlayOnlyMode ? rleCloseX - (layout.leftMargin - 4) : rleCloseX,
                rleOverlayOnlyMode ? rleCloseY - (layout.panelTopY - 2) : rleCloseY,
                rleCloseSize,
                rleCloseSize
        );
        drawRleVisualMapContent(fpsFont, layout.leftMargin, layout.availWidth, (int) layout.cellSize, (int) layout.cellGap,
                layout.visualHeight, layout.visualXStart, layout.entriesLineY, layout.statsLineY,
                layout.statsMain, layout.statsGamma);
        gc.restore();
    }

    /**
     * Draw the block-cell grid representing each LED position (colors or leaders).
     * Accepts and returns the Y cursor so the caller can continue laying out subsequent rows.
     */
    private void drawRleBlockCells(double cursorY, double availWidth, int cellSize, int cellGap,
                                   double visualHeight, double visualXStart) {
        final char FULL_BLOCK = '█';
        String visualPattern = NetworkSingleton.lastRleVisualBar;
        int charsPerLine = (int) (availWidth / (cellSize + cellGap));
        if (charsPerLine < 10) charsPerLine = 10;
        double rowBaseY = cellSize <= 6 ? cursorY + visualHeight : cursorY;
        GraphicsContext gc = tc.getGc();
        for (int vi = 0; vi < visualPattern.length(); vi += charsPerLine) {
            int endLimit = Math.min(vi + charsPerLine, visualPattern.length());
            double lineCursorY = rowBaseY;
            for (int ci = vi; ci < endLimit; ci++) {
                char ch = visualPattern.charAt(ci);
                boolean isLeader = ch == FULL_BLOCK || ch == '#';
                double cellX = visualXStart + (ci - vi) * (cellSize + cellGap);
                Color cellColor;
                // Draw a contrasting background frame behind each cell so it stands out
                gc.setFill(new Color(0.15, 0.15, 0.35, 1.0));
                gc.fillRect(cellX - 1, lineCursorY - 1, cellSize + 2, cellSize + 2);
                if (NetworkSingleton.lastRleLedsColors != null && ci < NetworkSingleton.lastRleLedsColors.length) {
                    java.awt.Color ledColor = NetworkSingleton.lastRleLedsColors[ci];
                    cellColor = new Color(ledColor.getRed() / 255.0, ledColor.getGreen() / 255.0,
                            ledColor.getBlue() / 255.0, ledColor.getAlpha() / 255.0);
                    cellColor = TestCanvas.overbrighten(cellColor, 1.4);
                    gc.setFill(isLeader ? new Color(0, 1, 0, 0.85) : new Color(0.25, 0.25, 0.25, 0.7));
                    gc.fillRect(cellX, lineCursorY + visualHeight, cellSize, cellSize);
                } else {
                    cellColor = isLeader ? Color.WHITE : new Color(0.35, 0.35, 0.35, 1.0);
                }
                gc.setFill(cellColor);
                gc.fillRect(cellX, lineCursorY, cellSize, cellSize);
            }
            rowBaseY += visualHeight;
        }
    }

    /**
     * Draw the content inside the RLE visual map panel: entries text, visual pattern cells, stats line, and FPS labels.
     */
    private void drawRleVisualMapContent(Font fpsFont, double marginX, double availWidth,
                                         int cellSize, int cellGap, double visualHeight, double visualXStart,
                                         double entriesLineY, double statsLineY, String statsMain, String statsGamma) {
        GraphicsContext gc = tc.getGc();
        // Entries text wrap onto new lines when exceeding available width
        Font currentFont = Font.font(java.awt.Font.MONOSPACED, FontWeight.BOLD, fpsFont.getSize() * 0.85);
        Text tempText = new Text();
        tempText.setFont(currentFont);
        List<String> entryLines = wrapLongRleEntries(NetworkSingleton.lastRleEntries, availWidth, currentFont);
        gc.setFill(new Color(0.9, 0.75, 0.3, 1));
        double lineH = tempText.getLayoutBounds().getHeight();
        int lineGap = entryLines.size() > 1 ? 0 : 1; // tighter spacing when wrapping
        double cursorY = entriesLineY;
        for (String line : entryLines) {
            gc.setFont(currentFont);
            gc.fillText(line, marginX, cursorY);
            cursorY += lineH + lineGap;
        }
        // Visual pattern block cells always anchored at the original entries line position
        // so they don't shift when RLE entries wrap onto multiple lines
        drawRleBlockCells(entriesLineY + lineH + lineGap, availWidth, cellSize, cellGap, visualHeight, visualXStart);
        // Stats line
        gc.setFont(fpsFont);
        gc.setFill(new Color(0.7, 1, 0.7, 1));
        gc.fillText(statsMain, marginX, statsLineY);
        gc.setFill(new Color(1.0F, 0.5F, 0F, 1.0F));
        gc.fillText(statsGamma, marginX + 2, statsLineY + 5 + fpsFont.getSize());
        // FPS labels right-aligned, stacked vertically above stats
        String producerFps = CommonUtility.getWord(Constants.INFO_PRODUCING) + MainSingleton.getInstance().FPS_PRODUCER + Constants.FPS_VAL;
        String consumerFps = CommonUtility.getWord(Constants.INFO_CONSUMING) + MainSingleton.getInstance().FPS_GW_CONSUMER + Constants.FPS_VAL;
        Text fpsMeasure = new Text();
        fpsMeasure.setFont(fpsFont);
        double fpsLineHeight;
        double rightEdge = marginX + availWidth;
        gc.setFill(new Color(1, 1, 1, 0.85));
        gc.setFont(fpsFont);
        double producerY = statsLineY + (fpsFont.getSize() * 2) - 2;
        fpsMeasure.setText(consumerFps);
        fpsLineHeight = fpsMeasure.getLayoutBounds().getHeight();
        gc.fillText(consumerFps, rightEdge - fpsMeasure.getLayoutBounds().getWidth(), producerY);
        double consumerY = producerY - fpsLineHeight - 2;
        fpsMeasure.setText(producerFps);
        gc.fillText(producerFps, rightEdge - fpsMeasure.getLayoutBounds().getWidth(), consumerY);
    }

    /**
     * Calculate layout coordinates and properties for the RLE visual map.
     *
     * @param virtualWidth  virtual width of the screen / monitor
     * @param virtualHeight virtual height of the screen / monitor
     * @return layout calculations container
     */
    private RleLayout calculateRleLayout(double virtualWidth, double virtualHeight) {
        RleLayout layout = new RleLayout();
        Font fpsFont = Font.font(java.awt.Font.MONOSPACED, FontWeight.BOLD, 11);
        layout.availWidth = virtualWidth * 0.80;
        layout.leftMargin = (virtualWidth - layout.availWidth) / 2;
        Text tempText = new Text("[Entries: " + NetworkSingleton.lastRleEntries + "]");
        tempText.setFont(fpsFont);
        double entriesLineHeight = tempText.getLayoutBounds().getHeight();
        int charCount = Math.max(1, NetworkSingleton.lastRleLedCount);
        double cellsTargetWidth = layout.availWidth * 0.75;
        layout.cellGap = 1;
        layout.cellSize = Math.max(1, (int) (cellsTargetWidth / charCount) - layout.cellGap);
        if (layout.cellSize > 8) {
            layout.cellSize = 8;
        }
        layout.visualHeight = layout.cellSize + 4;
        layout.statsMain = "[LEDs: " + NetworkSingleton.lastRleLedCount
                + ", Group by: " + MainSingleton.getInstance().config.getGroupBy()
                + ", Group count: " + NetworkSingleton.lastRleGroupCount
                + ", Leaders: " + NetworkSingleton.lastRleLeaderCount + "]";
        layout.statsGamma = "Dynamic Gamma: " + String.format("%.3f", Double.longBitsToDouble(ImageProcessor.currentGammaAtomic.get()));
        tempText = new Text(layout.statsMain);
        tempText.setFont(fpsFont);
        double statsHeight = tempText.getLayoutBounds().getHeight();
        // On Windows: dynamic offset based on actual taskbar height via DisplayManager.
        double baseOffset = 100;
        if (NativeExecutor.isWindows()) {
            DisplayInfo disp = new DisplayManager().getDisplayInfo(MainSingleton.getInstance().config.getMonitorNumber());
            double fullHeight = disp.getHeight();
            double visualExtent = disp.getMaxY() - disp.getMinY();
            baseOffset = Math.max(0, fullHeight - visualExtent);
        }
        double bottomAnchor = virtualHeight - baseOffset + rleOverlayYOffset;
        double currentY = bottomAnchor;
        currentY -= statsHeight + 4;
        layout.statsLineY = currentY;
        currentY -= layout.visualHeight;
        double visualCellWidth = (layout.cellSize + layout.cellGap) * charCount;
        layout.visualXStart = layout.leftMargin + (layout.availWidth - visualCellWidth) / 2;
        currentY -= entriesLineHeight + 4;
        layout.entriesLineY = currentY;
        layout.panelTopY = layout.entriesLineY - fpsFont.getSize();
        layout.panelBottomY = bottomAnchor;
        double minPanelTopY = 4;
        double maxPanelBottomY = virtualHeight - 4;
        double correction = 0;
        if (layout.panelTopY < minPanelTopY) {
            correction = minPanelTopY - layout.panelTopY;
        } else if (layout.panelBottomY > maxPanelBottomY) {
            correction = maxPanelBottomY - layout.panelBottomY;
        }
        if (correction != 0) {
            rleOverlayYOffset += correction;
            layout.statsLineY += correction;
            layout.entriesLineY += correction;
            layout.panelTopY += correction;
            layout.panelBottomY += correction;
        }
        double midPanelY = (layout.panelTopY + layout.panelBottomY) / 2.0;
        double centerYOffset = midPanelY - layout.statsLineY;
        layout.statsLineY += centerYOffset;
        return layout;
    }

    /**
     * Wrap a comma-separated RLE entries string so that each line fits within {@code maxWidth}.
     * Returns a list of lines. Breaks only at comma boundaries to keep individual groups intact.
     */
    private List<String> wrapLongRleEntries(String rleEntries, double maxWidth, Font font) {
        Text measure = new Text();
        measure.setFont(font);
        String[] groups = rleEntries.split(",");
        if (groups.length == 0 || (groups.length == 1 && groups[0].isEmpty())) {
            return List.of("[Entries: " + rleEntries + "]");
        }
        double currentLineW = 0;
        StringBuilder lineBuilder = new StringBuilder();
        List<String> lines = new ArrayList<>();
        for (String group : groups) {
            String entry;
            if (MainSingleton.getInstance().config.isFullFirmware() && MainSingleton.getInstance().config.isWirelessStream()) {
                entry = "[" + group + "]";
            } else {
                entry = group;
            }
            measure.setText(entry);
            double entryW = measure.getLayoutBounds().getWidth() + 2; // include separator
            if (currentLineW + entryW > maxWidth && currentLineW > 0) {
                lines.add(lineBuilder.toString());
                lineBuilder.setLength(0);
                currentLineW = 0;
            }
            if (!lineBuilder.isEmpty()) {
                lineBuilder.append(", ");
                currentLineW += 10; // space for separator in monospace
            }
            lineBuilder.append(entry);
            currentLineW += entryW;
        }
        if (!lineBuilder.isEmpty()) {
            lines.add(lineBuilder.toString());
        }
        return lines;
    }

    /**
     * Layout coordinates and properties for the RLE visual map panel.
     */
    private static class RleLayout {
        double leftMargin;
        double panelTopY;
        double panelBottomY;
        double availWidth;
        double entriesLineY;
        double statsLineY;
        double cellSize;
        double cellGap;
        double visualHeight;
        double visualXStart;
        String statsMain;
        String statsGamma;
    }
}
