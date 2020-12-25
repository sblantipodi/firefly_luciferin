/*
  ImageProcessor.java

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
package org.dpsoftware.grabber;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;

/**
 * Convert screen capture into a "readable signal" for LED strip
 * Screen capture can be done using CPU (you need a lot of threads to get a good framerate) or via
 * GPU Hardware Acceleration using Java Native Access API
 */
@Slf4j
public class ImageProcessor {
    
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
    // Custom JNA Class for GDI32Util
    static CustomGDI32Util customGDI32Util;

    /**
     * Constructor
     */
    public ImageProcessor() {

        if (Platform.isWindows()) {
            user32 = com.sun.jna.platform.win32.User32.INSTANCE;
            hwnd = user32.GetDesktopWindow();
            customGDI32Util = new CustomGDI32Util(hwnd);
        }
        ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(FireflyLuciferin.config.getDefaultLedMatrix());
        rect = new Rectangle(new Dimension((FireflyLuciferin.config.getScreenResX()*100)/FireflyLuciferin.config.getOsScaling(), (FireflyLuciferin.config.getScreenResY()*100)/FireflyLuciferin.config.getOsScaling()));

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
            if (FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.WinAPI.name())) {
                screen = customGDI32Util.getScreenshot();
            } else {
                screen = robot.createScreenCapture(rect);
            }
            //ImageIO.write(bi, "png", new java.io.File("screenshot.png"));
        } else {
            screen = image;
        }

        int osScaling = FireflyLuciferin.config.getOsScaling();
        Color[] leds = new Color[ledMatrix.size()];

        // We need an ordered collection so no parallelStream here
        ledMatrix.forEach((key, value) ->
            leds[key - 1] = getAverageColor(value, osScaling)
        );

        return leds;

    }

    /**
     * Get the average color from the screen buffer section
     *
     * @param ledCoordinate led X,Y coordinates
     * @param osScaling OS scaling percentage
     * @return the average color
     */
    static Color getAverageColor(LEDCoordinate ledCoordinate, int osScaling) {

        int r = 0, g = 0, b = 0;
        int skipPixel = 5;
        // 6 pixel for X axis and 6 pixel for Y axis
        int pixelToUse = 6;
        int pickNumber = 0;
        int width = screen.getWidth()-(skipPixel*pixelToUse);
        int height = screen.getHeight()-(skipPixel*pixelToUse);
        int xCoordinate = !(FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.CPU.name())) ? ledCoordinate.getX() : ((ledCoordinate.getX() * 100) / osScaling);
        int yCoordinate = !(FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.CPU.name())) ? ledCoordinate.getY() : ((ledCoordinate.getY() * 100) / osScaling);

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
        r = gammaCorrection(r / pickNumber);
        g = gammaCorrection(g / pickNumber);
        b = gammaCorrection(b / pickNumber);
        if (FireflyLuciferin.config.isEyeCare() && (r+g+b) < 10) r = g = b = 5;

        return new Color(r, g, b);

    }

    /**
     * Adjust gamma based on a given color
     *
     * @param color the color to adjust
     * @return the average color
     */
    public static int gammaCorrection(int color) {

        return (int) (255.0 *  Math.pow((color/255.0), FireflyLuciferin.config.getGamma()));

    }

    /**
     * Load GStreamer libraries
     */
    public void initGStreamerLibraryPaths() {

        String libPath = getInstallationPath() + Constants.GSTREAMER_PATH;

        if (Platform.isWindows()) {
            try {
                Kernel32 k32 = Kernel32.INSTANCE;
                String path = System.getenv(Constants.PATH);
                if (path == null || path.trim().isEmpty()) {
                    k32.SetEnvironmentVariable(Constants.PATH, libPath);
                } else {
                    k32.SetEnvironmentVariable(Constants.PATH, libPath + File.pathSeparator + path);
                }
                return;
            } catch (Throwable e) {
                log.error(Constants.CANT_FIND_GSTREAMER);
            }
        }
        String jnaPath = System.getProperty(Constants.JNA_LIB_PATH, "").trim();
        if (jnaPath.isEmpty()) {
            System.setProperty(Constants.JNA_LIB_PATH, libPath);
        } else {
            System.setProperty(Constants.JNA_LIB_PATH, jnaPath + File.pathSeparator + libPath);
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
                    installationPath.lastIndexOf(Constants.FAT_JAR_NAME)) + Constants.CLASSES;
        } catch (StringIndexOutOfBoundsException e) {
            installationPath = installationPath.substring(6, installationPath.lastIndexOf(Constants.TARGET))
                    + Constants.MAIN_RES;
        }
        log.info(Constants.GSTREAMER_PATH_IN_USE + installationPath.replaceAll("%20", " "));
        return installationPath.replaceAll("%20", " ");

    }

}
