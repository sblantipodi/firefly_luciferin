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

import com.sun.jna.platform.win32.GDI32Util;
import com.sun.jna.platform.win32.WinDef;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Convert screen capture into a "readable signal" for LED strip
 * Screen capture can be done using CPU (you need a lot of threads to get a good framerate) or via
 * GPU Hardware Acceleration using Java Native Access API
 */
public class ImageProcessor {

    // Only one instace must be used, Java Garbage Collector will not be fast enough in freeing memory with more instances
    BufferedImage screen;
    //Get JNA User32 Instace
    com.sun.jna.platform.win32.User32 user32;
    //Get desktop windows handler
    WinDef.HWND hwnd;
    // LED Matrix Map
    Map<Integer, LEDCoordinate> ledMatrix;
    // Screen capture rectangle
    Rectangle rect;
    Configuration config;

    /**
     * Constructor
     * @param config Configuration saved in the yaml config file
     */
    public ImageProcessor(Configuration config) {

        this.config = config;
        user32 = com.sun.jna.platform.win32.User32.INSTANCE;
        hwnd = user32.GetDesktopWindow();
        ledMatrix = config.getLedMatrix();
        rect = new Rectangle(new Dimension((config.getScreenResX()*100)/config.getOsScaling(), (config.getScreenResY()*100)/config.getOsScaling()));

    }

    /**
     * Screen Capture and analysis
     *
     * @param robot an AWT Robot instance for screen capture.
     *              One instance every three threads seems to be the hot spot for performance.
     * @return array of LEDs containing the avg color to be displayed on the LED strip
     */
    Color[] getColors(Robot robot) {

        // Choose between CPU and GPU acceleration
        if (config.isGpuHwAcceleration()) {
            screen = GDI32Util.getScreenshot(hwnd);
        } else {
            screen = robot.createScreenCapture(rect);
        }
        //ImageIO.write(bi, "png", new java.io.File("screenshot.png"));

        int osScaling = config.getOsScaling();
        int ledOffset = config.getLedOffset();
        Color[] leds = new Color[ledMatrix.size()];

        // Stream is faster than standards iterations, we need an ordered collection so no parallelStream here
        ledMatrix.entrySet().stream().forEach(entry -> {
            leds[entry.getKey()-1] = getAverageColor(entry.getValue(), osScaling, ledOffset, config);
        });

        return leds;

    }

    /**
     * Get the average color from the screen buffer section
     *
     * @param ledCoordinate led X,Y coordinates
     * @param osScaling OS scaling percentage
     * @param ledOffset Offset from the LED X,Y
     * @param config Configuration saved in the yaml config file
     * @return the average color
     */
    Color getAverageColor(LEDCoordinate ledCoordinate, int osScaling, int ledOffset, Configuration config) {

        int r = 0, g = 0, b = 0;
        int skipPixel = 5;
        // 6 pixel for X axis and 6 pixel for Y axis
        int pixelToUse = 6;
        int pickNumber = 0;
        int width = screen.getWidth()-(skipPixel*pixelToUse);
        int height = screen.getHeight()-(skipPixel*pixelToUse);
        int xCoordinate = config.isGpuHwAcceleration() ? ledCoordinate.getX() : ((ledCoordinate.getX() * 100) / osScaling);
        int yCoordinate = config.isGpuHwAcceleration() ? ledCoordinate.getY() : ((ledCoordinate.getY() * 100) / osScaling);

        // We start with a negative offset
        for (int x = 0; x < pixelToUse; x++) {
            for (int y = 0; y < pixelToUse; y++) {
                int offsetX = (xCoordinate + ledOffset + (skipPixel*x));
                int offsetY = (yCoordinate + ledOffset + (skipPixel*y));
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
