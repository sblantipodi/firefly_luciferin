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
package org.dpsoftware.grabber;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import org.dpsoftware.Configuration;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;

/**
 * Convert screen capture into a "readable signal" for LED strip
 * Screen capture can be done using CPU (you need a lot of threads to get a good framerate) or via
 * GPU Hardware Acceleration using Java Native Access API
 */
public class ImageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);

    // Only one instace must be used, Java Garbage Collector will not be fast enough in freeing memory with more instances
    static BufferedImage screen;
    //Get JNA User32 Instace
    com.sun.jna.platform.win32.User32 user32;
    //Get desktop windows handler
    WinDef.HWND hwnd;
    // LED Matrix Map
    static LinkedHashMap<Integer, LEDCoordinate> ledMatrix;
    // Screen capture rectangle
    static Rectangle rect;
    // Configuration saved in the yaml config file
    static Configuration config;
    // Custom JNA Class for GDI32Util
    static CustomGDI32Util customGDI32Util;

    /**
     * Constructor
     * @param config Configuration saved in the yaml config file
     */
    public ImageProcessor(Configuration config) {

        ImageProcessor.config = config;
        user32 = com.sun.jna.platform.win32.User32.INSTANCE;
        hwnd = user32.GetDesktopWindow();
        ledMatrix = config.getLedMatrixInUse(config.getDefaultLedMatrix());
        rect = new Rectangle(new Dimension((config.getScreenResX()*100)/config.getOsScaling(), (config.getScreenResY()*100)/config.getOsScaling()));
        customGDI32Util = new CustomGDI32Util(hwnd);

    }

    /**
     * Screen Capture and analysis
     *
     * @param robot an AWT Robot instance for screen capture.
     *              One instance every three threads seems to be the hot spot for performance.
     * @param image screenshot image
     * @return array of LEDs containing the avg color to be displayed on the LED strip
     */
    public static Color[] getColors(Robot robot, BufferedImage image) {

        // Choose between CPU and GPU acceleration
        if (image == null) {
            if (config.getCaptureMethod() == Configuration.CaptureMethod.WinAPI) {
                screen = customGDI32Util.getScreenshot();
            } else {
                screen = robot.createScreenCapture(rect);
            }
            //ImageIO.write(bi, "png", new java.io.File("screenshot.png"));
        } else {
            screen = image;
        }

        int osScaling = config.getOsScaling();
        Color[] leds = new Color[ledMatrix.size()];

        // We need an ordered collection so no parallelStream here
        ledMatrix.forEach((key, value) ->
            leds[key - 1] = getAverageColor(value, osScaling, config)
        );

        return leds;

    }

    /**
     * Get the average color from the screen buffer section
     *
     * @param ledCoordinate led X,Y coordinates
     * @param osScaling OS scaling percentage
     * @param config Configuration saved in the yaml config file
     * @return the average color
     */
    static Color getAverageColor(LEDCoordinate ledCoordinate, int osScaling, Configuration config) {

        int r = 0, g = 0, b = 0;
        int skipPixel = 5;
        // 6 pixel for X axis and 6 pixel for Y axis
        int pixelToUse = 6;
        int pickNumber = 0;
        int width = screen.getWidth()-(skipPixel*pixelToUse);
        int height = screen.getHeight()-(skipPixel*pixelToUse);
        int xCoordinate = config.getCaptureMethod() != Configuration.CaptureMethod.CPU ? ledCoordinate.getX() : ((ledCoordinate.getX() * 100) / osScaling);
        int yCoordinate = config.getCaptureMethod() != Configuration.CaptureMethod.CPU ? ledCoordinate.getY() : ((ledCoordinate.getY() * 100) / osScaling);

        // We start with a negative offset
        for (int x = 0; x < pixelToUse; x++) {
            for (int y = 0; y < pixelToUse; y++) {
                int offsetX = (xCoordinate + (skipPixel*x));
                int offsetY = (yCoordinate + (skipPixel*y));
                int rgb = screen.getRGB(Math.min(offsetX, width), Math.min(offsetY, height));
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
    public static int gammaCorrection(int color, Configuration config) {

        return (int) (255.0 *  Math.pow((color/255.0), config.getGamma()));

    }

    /**
     * Load GStreamer libraries
     */
    public void initGStreamerLibraryPaths() {

        String libPath = getInstallationPath() + "/gstreamer/1.0/x86_64/bin";

        if (Platform.isWindows()) {
            try {
                Kernel32 k32 = Kernel32.INSTANCE;
                String path = System.getenv("path");
                if (path == null || path.trim().isEmpty()) {
                    k32.SetEnvironmentVariable("path", libPath);
                } else {
                    k32.SetEnvironmentVariable("path", libPath + File.pathSeparator + path);
                }
                return;
            } catch (Throwable e) {
                logger.error("Cant' find GStreamer");
            }
        }
        String jnaPath = System.getProperty("jna.library.path", "").trim();
        if (jnaPath.isEmpty()) {
            System.setProperty("jna.library.path", libPath);
        } else {
            System.setProperty("jna.library.path", jnaPath + File.pathSeparator + libPath);
        }

    }

    /**
     * Get the path where the users installed the software
     * @return String path
     */
    public String getInstallationPath() {

        String installationPath = FireflyLuciferin.class.getProtectionDomain().getCodeSource().getLocation().toString();
        try {
            installationPath = installationPath.substring(6,
                    installationPath.lastIndexOf("FireflyLuciferin-jar-with-dependencies.jar")) + "classes";
        } catch (StringIndexOutOfBoundsException e) {
            installationPath = installationPath.substring(6, installationPath.lastIndexOf("target"))
                    + "src/main/resources";
        }
        logger.info("GStreamer path in use=" + installationPath.replaceAll("%20", " "));
        return installationPath.replaceAll("%20", " ");

    }

}
