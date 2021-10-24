/*
  TestCanvas.java

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
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that draws a test image on a JavaFX Canvas, it is multi monitor aware
 */
@Slf4j
public class TestCanvas {

    GraphicsContext gc;
    private int taleDistance = 10;

    /**
     * Show a canvas containing a test image for the LED Matrix in use
     * @param e event
     */
    public void buildAndShowTestImage(InputEvent e) {

        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readConfig(false);
        assert currentConfig != null;

        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();
        Group root = new Group();
        Scene s;
        if (NativeExecutor.isWindows()) {
            s = new Scene(root, 330, 400, Color.BLACK);
        } else {
            s = new Scene(root, currentConfig.getScreenResX(), currentConfig.getScreenResY(), Color.BLACK);
        }
        int scaleRatio = currentConfig.getOsScaling();

        int screenPixels = CommonUtility.scaleResolution(currentConfig.getScreenResX(), scaleRatio) * CommonUtility.scaleResolution(currentConfig.getScreenResY(), scaleRatio);
        taleDistance = (screenPixels * taleDistance) / 3_686_400;
        taleDistance = Math.min(taleDistance, 10);
        log.debug("Tale distance=" + taleDistance);

        Canvas canvas = new Canvas((CommonUtility.scaleResolution(currentConfig.getScreenResX(), scaleRatio)),
                (CommonUtility.scaleResolution(currentConfig.getScreenResY(), scaleRatio)));
        gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);

        double stageX = stage.getX();
        double stageY = stage.getY();
        // Hide canvas on key pressed
        canvas.setOnKeyPressed(t -> {
            stage.setFullScreen(false);
            stage.hide();
            stage.setX(stageX);
            stage.setY(stageY);
            FireflyLuciferin.guiManager.showSettingsDialog();
        });

        drawTestShapes(currentConfig);

        Text fireflyLuciferin = new Text(Constants.FIREFLY_LUCIFERIN);
        fireflyLuciferin.setFill(Color.CHOCOLATE);
        fireflyLuciferin.setStyle("-fx-font-weight: bold");
        fireflyLuciferin.setFont(Font.font(java.awt.Font.MONOSPACED, 60));
        Effect glow = new Glow(1.0);
        fireflyLuciferin.setEffect(glow);
        final int textPositionX = (int) ((CommonUtility.scaleResolution(currentConfig.getScreenResX(),scaleRatio)/2) - (fireflyLuciferin.getLayoutBounds().getWidth()/2));
        fireflyLuciferin.setX(textPositionX);
        fireflyLuciferin.setY(CommonUtility.scaleResolution((currentConfig.getScreenResY()/2), scaleRatio));
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
        stage.show();
        stage.setFullScreen(true);

    }

    /**
     * DisplayInfo a canvas, useful to test LED matrix
     * @param conf stored config
     */
    private void drawTestShapes(Configuration conf) {

        LinkedHashMap<Integer, LEDCoordinate> ledMatrix;
        ledMatrix = conf.getLedMatrixInUse(Objects.requireNonNullElse(FireflyLuciferin.config, conf).getDefaultLedMatrix());
        gc.setFill(Color.GREEN);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(10);
        gc.stroke();

        int scaleRatio = conf.getOsScaling();
        AtomicInteger ledDistance = new AtomicInteger();
        ledMatrix.forEach((key, coordinate) -> {

            int colorToUse = key;
            if (key > 3) {
                while (colorToUse > 3) {
                    colorToUse -= 3;
                }
            }
            switch (colorToUse) {
                case 1 -> gc.setFill(Color.RED);
                case 2 -> gc.setFill(Color.GREEN);
                default -> gc.setFill(Color.BLUE);
            }

            String ledNum = drawNumLabel(conf, key);

            int twelveX = CommonUtility.scaleResolution(conf.getScreenResX(), scaleRatio) / 12;

            if (conf.isSplitBottomRow()) {
                if (key <= conf.getBottomRightLed()) { // Bottom right
                    drawBottomRightRow(ledMatrix, ledDistance, coordinate, ledNum, scaleRatio, key);
                } else if (key <= conf.getBottomRightLed() + conf.getRightLed()) { // Right
                    drawRightColumn(conf, ledMatrix, ledDistance, coordinate, ledNum, scaleRatio, twelveX, key, conf.getBottomRightLed());
                } else if (key > (conf.getBottomRightLed() + conf.getRightLed()) && key <= (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed())) { // Top
                    drawTopRow(conf, ledMatrix, ledDistance, coordinate, ledNum, scaleRatio, key, conf.getBottomRightLed());
                } else if (key > (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed()) && key <= (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed() + conf.getLeftLed())) { // Left
                    drawLeftColumn(conf, ledMatrix, ledDistance, coordinate, ledNum, scaleRatio, twelveX, key, conf.getBottomRightLed());
                } else { // bottom left
                    drawBottomLeftRow(conf, ledMatrix, ledDistance, coordinate, ledNum, scaleRatio, key);
                }
            } else {
                if (key <= conf.getBottomRowLed()) { // Bottom row
                    drawBottomRow(ledMatrix, ledDistance, coordinate, ledNum, scaleRatio, key);
                } else if (key <= conf.getBottomRowLed() + conf.getRightLed()) { // Right
                    drawRightColumn(conf, ledMatrix, ledDistance, coordinate, ledNum, scaleRatio, twelveX, key, conf.getBottomRowLed());
                } else if (key > (conf.getBottomRowLed() + conf.getRightLed()) && key <= (conf.getBottomRowLed() + conf.getRightLed() + conf.getTopLed())) { // Top
                    drawTopRow(conf, ledMatrix, ledDistance, coordinate, ledNum, scaleRatio, key, conf.getBottomRowLed());
                } else if (key > (conf.getBottomRowLed() + conf.getRightLed() + conf.getTopLed()) && key <= (conf.getBottomRowLed() + conf.getRightLed() + conf.getTopLed() + conf.getLeftLed())) { // Left
                    drawLeftColumn(conf, ledMatrix, ledDistance, coordinate, ledNum, scaleRatio, twelveX, key, conf.getBottomRowLed());
                }
            }

            Image image = new Image(Objects.requireNonNull(getClass().getResource(Constants.IMAGE_CONTROL_LOGO)).toString());
            gc.drawImage(image, CommonUtility.scaleResolution((conf.getScreenResX()/2), scaleRatio)-64,CommonUtility.scaleResolution((conf.getScreenResY()/3), scaleRatio) );

        });

    }

    /**
     * Draw Right Columns on the Canvas
     * @param conf        in memory config
     * @param ledMatrix   led array
     * @param ledDistance distance between LEDs
     * @param coordinate  X,Y coordinate of a LED
     * @param ledNum      total number of LEDs
     * @param scaleRatio  OS scaling
     * @param twelveX     padding
     * @param key         led matrix key
     * @param bottomParam number of leds before the right col
     */
    void drawRightColumn(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, AtomicInteger ledDistance,
                         LEDCoordinate coordinate, String ledNum, int scaleRatio, int twelveX, Integer key, int bottomParam) {

        if (key == bottomParam + 1) {
            ledDistance.set(CommonUtility.scaleResolution(coordinate.getY(), scaleRatio) - CommonUtility.scaleResolution(ledMatrix.get(key + 1).getY(), scaleRatio));
        }
        int x = CommonUtility.scaleResolution(conf.getScreenResX(), scaleRatio) - twelveX;
        if (FireflyLuciferin.config.getDefaultLedMatrix().equals(Constants.AspectRatio.PILLARBOX.getAspectRatio())) {
            x -= CommonUtility.scaleResolution(LEDCoordinate.calculateBorders(conf.getScreenResX(), conf.getScreenResY()), scaleRatio);
        }
        gc.fillRect(x, CommonUtility.scaleResolution(coordinate.getY(), scaleRatio), twelveX, ledDistance.get() - taleDistance);
        gc.setFill(Color.WHITE);
        gc.fillText(ledNum, x + 2, CommonUtility.scaleResolution(coordinate.getY(), scaleRatio) + 15);

    }

    /**
     * Draw Top Row on the Canvas
     * @param conf        in memory config
     * @param ledMatrix   led array
     * @param ledDistance distance between LEDs
     * @param coordinate  X,Y coordinate of a LED
     * @param ledNum      total number of LEDs
     * @param scaleRatio  OS scaling
     * @param key         led matrix key
     * @param bottomParam number of leds before the right col
     */
    void drawTopRow(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, AtomicInteger ledDistance,
                    LEDCoordinate coordinate, String ledNum, int scaleRatio, Integer key, int bottomParam) {

        if (key == (bottomParam + conf.getRightLed()) + 1) {
            ledDistance.set(CommonUtility.scaleResolution(coordinate.getX(), scaleRatio) - CommonUtility.scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio));
        }
        coordinate.setY(coordinate.getY()+20);
        int topBorder = CommonUtility.scaleResolution(coordinate.getY() + 70, scaleRatio);
        int topBorderLabel = 0;
        if (FireflyLuciferin.config.getDefaultLedMatrix().equals(Constants.AspectRatio.LETTERBOX.getAspectRatio())) {
            topBorder = CommonUtility.scaleResolution(coordinate.getY() + 70, scaleRatio) - scaleLetterboxBorder(scaleRatio);
            topBorderLabel = scaleLetterboxBorder(scaleRatio);
        }
        gc.fillRect(CommonUtility.scaleResolution(coordinate.getX(), scaleRatio), topBorderLabel, ledDistance.get() - taleDistance, topBorder);
        gc.setFill(Color.WHITE);
        gc.fillText(ledNum, CommonUtility.scaleResolution(coordinate.getX(), scaleRatio) + 2, topBorderLabel + 15);

    }

    /**
     * Draw Left Column on the Canvas
     * @param conf        in memory config
     * @param ledMatrix   led array
     * @param ledDistance distance between LEDs
     * @param coordinate  X,Y coordinate of a LED
     * @param ledNum      total number of LEDs
     * @param scaleRatio  OS scaling
     * @param twelveX     padding
     * @param key         led matrix key
     * @param bottomParam number of leds before the right col
     */
    void drawLeftColumn(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, AtomicInteger ledDistance,
                        LEDCoordinate coordinate, String ledNum, int scaleRatio, int twelveX, Integer key, int bottomParam) {

        if (key == (bottomParam + conf.getRightLed() + conf.getTopLed()) + 1) {
            ledDistance.set(CommonUtility.scaleResolution(ledMatrix.get(key + 1).getY(), scaleRatio) - CommonUtility.scaleResolution(coordinate.getY(), scaleRatio));
        }
        int x = 0;
        if (FireflyLuciferin.config.getDefaultLedMatrix().equals(Constants.AspectRatio.PILLARBOX.getAspectRatio())) {
            x += LEDCoordinate.calculateBorders(conf.getScreenResX(), conf.getScreenResY());
            x = CommonUtility.scaleResolution(x, scaleRatio);
        }
        gc.fillRect(x, CommonUtility.scaleResolution(coordinate.getY(), scaleRatio),
                twelveX, ledDistance.get() - taleDistance);
        gc.setFill(Color.WHITE);
        gc.fillText(ledNum, x, CommonUtility.scaleResolution(coordinate.getY(), scaleRatio) + 15);

    }

    /**
     * Draw Bottom Right Row on the Canvas
     * @param ledMatrix   led array
     * @param ledDistance distance between LEDs
     * @param coordinate  X,Y coordinate of a LED
     * @param ledNum      total number of LEDs
     * @param scaleRatio  OS scaling
     * @param key         led matrix key
     */
    void drawBottomRightRow(LinkedHashMap<Integer, LEDCoordinate> ledMatrix, AtomicInteger ledDistance,
                            LEDCoordinate coordinate, String ledNum, int scaleRatio, Integer key) {

        if (ledDistance.get() == 0) {
            ledDistance.set(CommonUtility.scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) - CommonUtility.scaleResolution(coordinate.getX(), scaleRatio));
        }
        coordinate.setX(coordinate.getX()-(taleDistance*2));
        drawHorizontalRect(ledDistance, coordinate, ledNum, scaleRatio);

    }

    /**
     * Draw Bottom Left Row on the Canvas
     * @param conf        in memory config
     * @param ledMatrix   led array
     * @param ledDistance distance between LEDs
     * @param coordinate  X,Y coordinate of a LED
     * @param ledNum      total number of LEDs
     * @param scaleRatio  OS scaling
     * @param key         led matrix key
     */
    void drawBottomLeftRow(Configuration conf, LinkedHashMap<Integer, LEDCoordinate> ledMatrix, AtomicInteger ledDistance,
                            LEDCoordinate coordinate, String ledNum, int scaleRatio, Integer key) {

        if (key == (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed() + conf.getLeftLed()) + 1) {
            ledDistance.set(CommonUtility.scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) - CommonUtility.scaleResolution(coordinate.getX(), scaleRatio));
        }
        gc.fillRect(CommonUtility.scaleResolution(coordinate.getX(), scaleRatio), CommonUtility.scaleResolution(coordinate.getY(), scaleRatio),
                ledDistance.get() - taleDistance, CommonUtility.scaleResolution(coordinate.getY(), scaleRatio));
        gc.setFill(Color.WHITE);
        gc.fillText(ledNum, CommonUtility.scaleResolution(coordinate.getX(), scaleRatio) + 2, CommonUtility.scaleResolution(coordinate.getY(), scaleRatio) + 15);
        if (FireflyLuciferin.config.getDefaultLedMatrix().equals(Constants.AspectRatio.LETTERBOX.getAspectRatio())) {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, CommonUtility.scaleResolution(FireflyLuciferin.config.getScreenResY(), scaleRatio) - scaleLetterboxBorder(scaleRatio),
                    FireflyLuciferin.config.getScreenResX(), scaleLetterboxBorder(scaleRatio));
        }

    }

    /**
     * Draw Bottom Row on the Canvas
     * @param ledMatrix   led array
     * @param ledDistance distance between LEDs
     * @param coordinate  X,Y coordinate of a LED
     * @param ledNum      total number of LEDs
     * @param scaleRatio  OS scaling
     * @param key         led matrix key
     */
    void drawBottomRow(LinkedHashMap<Integer, LEDCoordinate> ledMatrix, AtomicInteger ledDistance,
                           LEDCoordinate coordinate, String ledNum, int scaleRatio, Integer key) {

        if (key == 1) {
            ledDistance.set(CommonUtility.scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) - CommonUtility.scaleResolution(coordinate.getX(), scaleRatio));
        }
        coordinate.setX(coordinate.getX() - (taleDistance * 2));
        drawHorizontalRect(ledDistance, coordinate, ledNum, scaleRatio);

    }

    /**
     * Draw horizontal rect
     * @param ledDistance distance between rects
     * @param coordinate  X,Y coordinate of a LED
     * @param ledNum      total number of LEDs
     * @param scaleRatio  OS scaling
     */
    private void drawHorizontalRect(AtomicInteger ledDistance, LEDCoordinate coordinate, String ledNum, int scaleRatio) {

        gc.fillRect(CommonUtility.scaleResolution(coordinate.getX(), scaleRatio) + taleDistance, CommonUtility.scaleResolution(coordinate.getY(), scaleRatio),
                ledDistance.get() - taleDistance, CommonUtility.scaleResolution(coordinate.getY(), scaleRatio));
        gc.setFill(Color.WHITE);
        gc.fillText(ledNum, CommonUtility.scaleResolution(coordinate.getX(), scaleRatio) + taleDistance + 2, CommonUtility.scaleResolution(coordinate.getY(), scaleRatio) + 15);
        if (FireflyLuciferin.config.getDefaultLedMatrix().equals(Constants.AspectRatio.LETTERBOX.getAspectRatio())) {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, CommonUtility.scaleResolution(FireflyLuciferin.config.getScreenResY(), scaleRatio) - scaleLetterboxBorder(scaleRatio),
                    FireflyLuciferin.config.getScreenResX(), scaleLetterboxBorder(scaleRatio));
        }

    }

    /**
     * Draw LED label on the canvas
     * @param conf in memory config
     * @param key  led matrix key
     */
    String drawNumLabel(Configuration conf, Integer key) {

        int lenNumInt;
        if (Constants.CLOCKWISE.equals(conf.getOrientation())) {
            lenNumInt = (FireflyLuciferin.ledNumber - (key-1) - FireflyLuciferin.config.getLedStartOffset());
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
        String ledNum = "#" + lenNumInt;

        if (lenNumInt == 1) {
            gc.setFill(Color.ORANGE);
        } else if (lenNumInt == FireflyLuciferin.ledNumber) {
            gc.setFill(Color.ORANGE);
        }
        return ledNum;

    }

    /**
     * Scale letterbox border
     * @param scaleRatio OS scaling
     * @return scaled number
     */
    int scaleLetterboxBorder(int scaleRatio) {

        return CommonUtility.scaleResolution(((120 * FireflyLuciferin.config.getScreenResY()) / 2160), scaleRatio);

    }

}
