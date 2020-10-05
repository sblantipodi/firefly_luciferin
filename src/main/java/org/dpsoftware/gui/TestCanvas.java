/*
  TestCanvas.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020  Davide Perini

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
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.StorageManager;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCanvas {

    /**
     * Show a canvas containing a test image for the LED Matrix in use
     * @param e event
     */
    public void buildAndShowTestImage(InputEvent e) {

        StorageManager sm = new StorageManager();
        Configuration currentConfig = sm.readConfig();

        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();
        Group root = new Group();
        Scene s;
        if (com.sun.jna.Platform.isWindows()) {
            s = new Scene(root, 330, 400, Color.BLACK);
        } else {
            s = new Scene(root, currentConfig.getScreenResX(), currentConfig.getScreenResY(), Color.BLACK);
        }
        int scaleRatio = currentConfig.getOsScaling();
        Canvas canvas = new Canvas((scaleResolution(currentConfig.getScreenResX(), scaleRatio)),
                (scaleResolution(currentConfig.getScreenResY(), scaleRatio)));
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);

        // Hide canvas on key pressed
        canvas.setOnKeyPressed(t -> {
            stage.setFullScreen(false);
            stage.hide();
            FireflyLuciferin.guiManager.showSettingsDialog();
        });

        drawTestShapes(gc, currentConfig);

        Text fireflyLuciferin = new Text(Constants.FIREFLY_LUCIFERIN);
        fireflyLuciferin.setFill(Color.CHOCOLATE);
        fireflyLuciferin.setStyle("-fx-font-weight: bold");
        fireflyLuciferin.setFont(Font.font(java.awt.Font.MONOSPACED, 60));
        Effect glow = new Glow(1.0);
        fireflyLuciferin.setEffect(glow);
        final int textPositionX = (int) ((scaleResolution(currentConfig.getScreenResX(),scaleRatio)/2) - (fireflyLuciferin.getLayoutBounds().getWidth()/2));
        fireflyLuciferin.setX(textPositionX);
        fireflyLuciferin.setY(scaleResolution((currentConfig.getScreenResY()/2), scaleRatio));
        root.getChildren().add(fireflyLuciferin);

        root.getChildren().add(canvas);
        stage.setScene(s);
        stage.show();
        stage.setFullScreen(true);

    }

    /**
     * Display a canvas, useful to test LED matrix
     * @param gc graphics canvas
     * @param conf stored config
     */
    private void drawTestShapes(GraphicsContext gc, Configuration conf) {

        LinkedHashMap<Integer, LEDCoordinate> ledMatrix = conf.getLedMatrixInUse(conf.getDefaultLedMatrix());

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

            String ledNum;
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
            ledNum = "#" + lenNumInt;

            if (lenNumInt == 1) {
                gc.setFill(Color.ORANGE);
            } else if (lenNumInt == FireflyLuciferin.ledNumber) {
                gc.setFill(Color.ORANGE);
            }

            int twelveX = scaleResolution(conf.getScreenResX(), scaleRatio) / 12;

            if (key <= conf.getBottomRightLed()) { // Bottom right
                if (ledDistance.get() == 0) {
                    ledDistance.set(scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) - scaleResolution(coordinate.getX(), scaleRatio));
                }
                gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio)+10, scaleResolution(coordinate.getY(), scaleRatio),
                        ledDistance.get() - 10, scaleResolution(coordinate.getY(), scaleRatio));
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, scaleResolution(coordinate.getX(), scaleRatio) + 12, scaleResolution(coordinate.getY(), scaleRatio) + 15);
            } else if (key <= conf.getBottomRightLed() + conf.getRightLed()) { // Right
                if (key == conf.getBottomRightLed() + 1) {
                    ledDistance.set(scaleResolution(coordinate.getY(), scaleRatio) - scaleResolution(ledMatrix.get(key + 1).getY(), scaleRatio));
                }
                gc.fillRect(scaleResolution(conf.getScreenResX(), scaleRatio) - twelveX, scaleResolution(coordinate.getY(), scaleRatio),
                        twelveX, ledDistance.get() - 10);
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, scaleResolution(conf.getScreenResX(), scaleRatio) - (twelveX) + 2, scaleResolution(coordinate.getY(), scaleRatio) + 15);
            } else if (key > (conf.getBottomRightLed() + conf.getRightLed()) && key <= (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed())) { // Top
                if (key == (conf.getBottomRightLed() + conf.getRightLed()) + 1) {
                    ledDistance.set(scaleResolution(coordinate.getX(), scaleRatio) - scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio));
                }
                gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio), 0,
                        ledDistance.get() - 10, scaleResolution(coordinate.getY() + 20, scaleRatio));
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, scaleResolution(coordinate.getX(), scaleRatio) + 2, 15);
            } else if (key > (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed()) && key <= (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed() + conf.getLeftLed())) { // Left
                if (key == (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed()) + 1) {
                    ledDistance.set(scaleResolution(ledMatrix.get(key + 1).getY(), scaleRatio) - scaleResolution(coordinate.getY(), scaleRatio));
                }
                gc.fillRect(0, scaleResolution(coordinate.getY(), scaleRatio),
                        twelveX, ledDistance.get() - 10);
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, 0, scaleResolution(coordinate.getY(), scaleRatio) + 15);
            } else { // bottom left
                if (key == (conf.getBottomRightLed() + conf.getRightLed() + conf.getTopLed() + conf.getLeftLed()) + 1) {
                    ledDistance.set(scaleResolution(ledMatrix.get(key + 1).getX(), scaleRatio) - scaleResolution(coordinate.getX(), scaleRatio));
                }
                gc.fillRect(scaleResolution(coordinate.getX(), scaleRatio), scaleResolution(coordinate.getY(), scaleRatio),
                        ledDistance.get() - 10, scaleResolution(coordinate.getY(), scaleRatio));
                gc.setFill(Color.WHITE);
                gc.fillText(ledNum, scaleResolution(coordinate.getX(), scaleRatio) + 2, scaleResolution(coordinate.getY(), scaleRatio) + 15);
            }

            Image image = new Image(getClass().getResource(Constants.IMAGE_CONTROL_LOGO).toString());
            gc.drawImage(image, scaleResolution((conf.getScreenResX()/2), scaleRatio)-64,scaleResolution((conf.getScreenResY()/3), scaleRatio) );

        });

    }

    int scaleResolution(int numberToScale, int scaleRatio) {
        return (numberToScale*100)/scaleRatio;
    }

}
