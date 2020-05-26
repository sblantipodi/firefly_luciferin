/*
  ImageProcessor.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/
package org.dpsoftware;

import lombok.NoArgsConstructor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Convert screen capture into a "readable signal" for LED strip
 */
@NoArgsConstructor
public class ImageProcessor {

    /**
     * Screen Capture and analysis
     *
     * @param robot an AWT Robot instance for screen capture.
     *              One instance every three threads seems to be the hot spot for performance.
     * @param rect Screen rect
     * @param config Configuration saved in the yaml config file
     * @param ledMatrix LED Matrix Map
     * @return array of LEDs containing the avg color to be displayed on the LED strip
     */
    Color[] getColors(Robot robot, Rectangle rect, Configuration config, Map<Integer, LEDCoordinate> ledMatrix) {

        BufferedImage screen = robot.createScreenCapture(rect);

        int osScaling = config.getOsScaling();
        int ledOffset = config.getLedOffset();
        Color[] leds = new Color[ledMatrix.size()];

        // Stream is faster than standards iterations, we need an ordered collection so no parallelStream here
        ledMatrix.entrySet().stream().forEach(entry -> {
            leds[entry.getKey()-1] = getAverageColor(screen, entry.getValue(), osScaling, ledOffset, config);
        });

        return leds;

    }

    /**
     * Get the average color from the screen buffer section
     *
     * @param screen a little portion of the screen
     * @param ledCoordinate led X,Y coordinates
     * @param osScaling OS scaling percentage
     * @param ledOffset Offset from the LED X,Y
     * @param config Configuration saved in the yaml config file
     * @return the average color
     */
    Color getAverageColor(BufferedImage screen, LEDCoordinate ledCoordinate, int osScaling, int ledOffset, Configuration config) {

        int r = 0, g = 0, b = 0;
        int skipPixel = 5;
        // 6 pixel for X axis and 6 pixel for Y axis
        int pixelToUse = 6;
        int pickNumber = 0;
        int width = screen.getWidth()-(skipPixel*pixelToUse);
        int height = screen.getHeight()-(skipPixel*pixelToUse);

        // We start with a negative offset
        for (int x = 0; x < pixelToUse; x++) {
            for (int y = 0; y < pixelToUse; y++) {
                int offsetX = (((ledCoordinate.getX() * 100) / osScaling) + ledOffset + (skipPixel*x));
                int offsetY = (((ledCoordinate.getY() * 100) / osScaling) + ledOffset + (skipPixel*y));
                int rgb = screen.getRGB((offsetX < width) ? offsetX : width, (offsetY < height) ? offsetY : height);
                Color color = new Color(rgb);
                r += color.getRed();
                g += color.getGreen();
                b += color.getBlue();
                pickNumber++;
            }
        }
        r = gammaCorrection(r / pickNumber, config);
        g = gammaCorrection(g / pickNumber, config);
        b = gammaCorrection(b / pickNumber, config);

        return new Color(r, g, b);

    }

    /**
     * Adjust gamma based on a given color
     *
     * @param color the color to adjust
     * @param config Configuration saved in the yaml config file
     * @return the average color
     */
    int gammaCorrection(int color, Configuration config) {
        return (int) (255.0 *  Math.pow((color/255.0), config.getGamma()));
    }

}
