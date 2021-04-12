/*
  ImageProcessor.java

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
package org.dpsoftware.grabber;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.Arrays;
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

        if (NativeExecutor.isWindows()) {
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

        // CHECK_ASPECT_RATIO is true 1 time every 5 seconds, if true and black bars auto detection is on, auto detect black bars
        if (FireflyLuciferin.config.isAutoDetectBlackBars()) {
            if (FireflyLuciferin.CHECK_ASPECT_RATIO) {
                FireflyLuciferin.CHECK_ASPECT_RATIO = false;
                ImageProcessor.autodetectBlackBars(screen.getWidth(), screen.getHeight(), null);
                ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(FireflyLuciferin.config.getDefaultLedMatrix());
            }
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

        if (NativeExecutor.isWindows()) {
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
        } else if (NativeExecutor.isMac()) {
            String gstPath = System.getProperty(Constants.JNA_GSTREAMER_PATH, Constants.JNA_LIB_PATH_FOLDER);
            if (!gstPath.isEmpty()) {
                String jnaPath = System.getProperty(Constants.JNA_LIB_PATH, "").trim();
                if (jnaPath.isEmpty()) {
                    System.setProperty(Constants.JNA_LIB_PATH, gstPath);
                } else {
                    System.setProperty(Constants.JNA_LIB_PATH, jnaPath + File.pathSeparator + gstPath);
                }
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
            installationPath = installationPath.substring(6, installationPath.lastIndexOf(Constants.FAT_JAR_NAME)) + Constants.CLASSES;
        } catch (StringIndexOutOfBoundsException e) {
            installationPath = installationPath.substring(6, installationPath.lastIndexOf(Constants.TARGET))
                    + Constants.MAIN_RES;
        }
        log.info(Constants.GSTREAMER_PATH_IN_USE + installationPath.replaceAll("%20", " "));
        return installationPath.replaceAll("%20", " ");

    }

    /**
     * Auto detect black bars when screen grabbing, set Fullscreen, Letterbox or Pillarbox accordingly
     * @param width screen width with scale ratio
     * @param height screen height with scale ratio
     * @param rgbBuffer full screen captured buffer
     */
    public static void autodetectBlackBars(int width, int height, IntBuffer rgbBuffer) {

        int checkNumber = 10;
        int intBufferSize = (width*height)-1;
        int[][] blackPixelMatrix;
        blackPixelMatrix = calculateBlackPixels(Constants.AspectRatio.LETTERBOX, width, height, checkNumber, intBufferSize, rgbBuffer);
        boolean letterbox = switchAspectRatio(Constants.AspectRatio.LETTERBOX, blackPixelMatrix, checkNumber, false);
        blackPixelMatrix = calculateBlackPixels(Constants.AspectRatio.PILLARBOX, width, height, checkNumber, intBufferSize, rgbBuffer);
        boolean pillarbox = switchAspectRatio(Constants.AspectRatio.PILLARBOX, blackPixelMatrix, checkNumber, false);
        if (!letterbox && !pillarbox) {
            switchAspectRatio(Constants.AspectRatio.PILLARBOX, blackPixelMatrix, checkNumber, true);
        }


    }

    /**
     * Calculate black pixels and put it into an array, works for every supported aspect ratios
     * @param aspectRatio If not Letterbox is Pillarbox
     * @param width screen width with scale ratio
     * @param height screen height with scale ratio
     * @param checkNumber number of pixels to analyze
     * @param intBufferSize buffer size
     * @param rgbBuffer full screen captured buffer
     * @return black pixels array, 0 for light pixel, 1 for black pixel
     */
    static int[][] calculateBlackPixels(Constants.AspectRatio aspectRatio, int width, int height, int checkNumber, int intBufferSize, IntBuffer rgbBuffer) {

        int[][] blackPixelMatrix = new int[3][checkNumber];
        int offsetX;
        int offsetY;
        int chunkSize = (aspectRatio == Constants.AspectRatio.LETTERBOX ? width : height) / checkNumber;
        int threeWayOffset;
        for (int i = 0; i < (checkNumber * 3); i++) {
            int j;
            int columnRowIndex;
            if (i < checkNumber) {
                threeWayOffset = calculateBorders(aspectRatio);
                columnRowIndex = i;
                j = 0;
            } else if (i < (checkNumber * 2)) {
                threeWayOffset = (aspectRatio == Constants.AspectRatio.LETTERBOX ? height : width) / 2;
                columnRowIndex = i - checkNumber;
                j = 1;
            } else {
                threeWayOffset = (aspectRatio == Constants.AspectRatio.LETTERBOX ? height : width) - calculateBorders(aspectRatio);
                columnRowIndex = i - (checkNumber * 2);
                j = 2;
            }
            int chunkSizeOffset = (i > 0) ? chunkSize * columnRowIndex : chunkSize;
            // If not Letterbox is Pillarbox
            if (aspectRatio == Constants.AspectRatio.LETTERBOX) {
                offsetX = chunkSizeOffset;
                offsetY = threeWayOffset;
            } else {
                offsetX = threeWayOffset;
                offsetY = chunkSizeOffset;
            }
            int r, g, b;
            // DUPL
            if (rgbBuffer != null) {
                int bufferOffset = (Math.min(offsetX, width)) + ((offsetY < height) ? (offsetY * width) : (height * width));
                int rgb = rgbBuffer.get(Math.min(intBufferSize, bufferOffset));
                r = rgb >> 16 & 0xFF;
                g = rgb >> 8 & 0xFF;
                b = rgb & 0xFF;
            } else { // Other methods
                int rgb = screen.getRGB(Math.min(offsetX, width), Math.min(offsetY, height));
                Color color = new Color(rgb);
                r = color.getRed();
                g = color.getGreen();
                b = color.getBlue();
            }
            if (r == 0 && g == 0 && b == 0) {
                blackPixelMatrix[j][columnRowIndex] = 1;
            } else {
                blackPixelMatrix[j][columnRowIndex] = 0;
            }
        }
        return blackPixelMatrix;

    }

    /**
     * Switch to the new aspect ratio based on black bars
     * @param aspectRatio Letterbox or Pillarbox
     * @param blackPixelMatrix contains black and non black pixels
     * @param checkNumber numbers of pixels to analyze
     * @return boolean if aspect ratio is changed
     */
    static boolean switchAspectRatio(Constants.AspectRatio aspectRatio, int[][] blackPixelMatrix, int checkNumber, boolean setFullscreen) {

        boolean isPillarboxLetterbox;
        int topMatrix = Arrays.stream(blackPixelMatrix[0]).sum();
        int centerMatrix = Arrays.stream(blackPixelMatrix[1]).sum();
        int bottomMatrix = Arrays.stream(blackPixelMatrix[2]).sum();
        if (topMatrix == checkNumber && centerMatrix == 0 && bottomMatrix == checkNumber) {
            if (!FireflyLuciferin.config.getDefaultLedMatrix().equals(aspectRatio.getAspectRatio())) {
                FireflyLuciferin.config.setDefaultLedMatrix(aspectRatio.getAspectRatio());
                GStreamerGrabber.ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(aspectRatio.getAspectRatio());
                log.debug("Switching to " + aspectRatio.getAspectRatio() + " aspect ratio.");
            }
            isPillarboxLetterbox = true;
        } else {
            if (!FireflyLuciferin.config.getDefaultLedMatrix().equals(Constants.AspectRatio.FULLSCREEN.getAspectRatio())) {
                if (setFullscreen) {
                    FireflyLuciferin.config.setDefaultLedMatrix(Constants.AspectRatio.FULLSCREEN.getAspectRatio());
                    GStreamerGrabber.ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(Constants.AspectRatio.FULLSCREEN.getAspectRatio());
                    log.debug("Switching to " + Constants.AspectRatio.FULLSCREEN.getAspectRatio() + " aspect ratio.");
                }
            }
            isPillarboxLetterbox = false;
        }
        return isPillarboxLetterbox;

    }

    /**
     * Calculate borders for auto aspect ratio
     * @param aspectRatio Letterbox or Pillarbox
     * @return borders
     */
    public static int calculateBorders(Constants.AspectRatio aspectRatio) {

        if (aspectRatio == Constants.AspectRatio.LETTERBOX) {
            return (((FireflyLuciferin.config.getScreenResY() * 280) / 2160) / Constants.RESAMPLING_FACTOR) - 5;
        } else {
            return (((FireflyLuciferin.config.getScreenResY() * 580) / 2160) / Constants.RESAMPLING_FACTOR) - 5;
        }

    }

}
