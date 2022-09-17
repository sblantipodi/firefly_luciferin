/*
  ImageProcessor.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.dto.HSLColor;
import org.dpsoftware.utilities.ColorUtilities;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    public static boolean CHECK_ASPECT_RATIO = true;
    public boolean unlockCheckLedDuplication = true;
    public Color[] ledArray;
    public LocalDateTime lastFrameTime;
    public boolean shutDownLedStrip = false;

    /**
     * Constructor
     */
    public ImageProcessor(boolean initLedMatrix) {
        if (NativeExecutor.isWindows()) {
            user32 = com.sun.jna.platform.win32.User32.INSTANCE;
            hwnd = user32.GetDesktopWindow();
            customGDI32Util = new CustomGDI32Util(hwnd);
        }
        if (initLedMatrix) {
            ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(FireflyLuciferin.config.getDefaultLedMatrix());
            rect = new Rectangle(new Dimension((FireflyLuciferin.config.getScreenResX()*100)/FireflyLuciferin.config.getOsScaling(), (FireflyLuciferin.config.getScreenResY()*100)/FireflyLuciferin.config.getOsScaling()));
        }
    }

    /**
     * Screen Capture and analysis
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

        // CHECK_ASPECT_RATIO is true 10 times per second, if true and black bars auto detection is on, auto detect black bars
        if (FireflyLuciferin.config.isAutoDetectBlackBars()) {
            if (ImageProcessor.CHECK_ASPECT_RATIO) {
                ImageProcessor.CHECK_ASPECT_RATIO = false;
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
     * @param ledCoordinate led X,Y coordinates
     * @param osScaling     OS scaling percentage
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
        return correctColors(r, g, b, pickNumber);
    }

    /**
     * Correct colors using various techniques.
     * - AVG colors, no need for the square root here since we calculate the gamma later
     * - Gamma correction
     * - HSL correction
     * - Don't turn LED off (eye care)
     * - Brightness limiter to limit strobo effect
     * @param r avg red channel
     * @param g avg green channel
     * @param b avg blue channel
     * @param pickNumber number of computed pixel, used to get the avg
     * @return corrected color
     */
    public static Color correctColors(int r, int g, int b, int pickNumber) {
        // AVG colors inside the tile, no need for the square root here since we calculate the gamma later
        r = (r / pickNumber);
        g = (g / pickNumber);
        b = (b / pickNumber);
        // Saturate colors and shift bits if needed, apply HSL correcction
        Color rgb = manageColors(r, g, b);
        if (rgb != null) {
            r = rgb.getRed();
            g = rgb.getGreen();
            b = rgb.getBlue();
        }
        // Apply gamma correction
        r = gammaCorrection(r);
        g = gammaCorrection(g);
        b = gammaCorrection(b);
        // Don't turn off LEDs when they are black (eye care)
        if (FireflyLuciferin.config.isEyeCare() && (r+g+b) < 10) {
            r = g = b = (Constants.DEEP_BLACK_CHANNEL_TOLERANCE * 2);
        }
        // Brightness limiter to limit strobo effect
        if (FireflyLuciferin.config.getBrightnessLimiter() != 1.0F) {
            float[] brightnessLimitedRGB = ColorUtilities.RGBtoHSL(r, g, b, null);
            if (brightnessLimitedRGB[2] >= FireflyLuciferin.config.getBrightnessLimiter()) {
                brightnessLimitedRGB[2] = FireflyLuciferin.config.getBrightnessLimiter();
            }
            return ColorUtilities.HSLtoRGB(brightnessLimitedRGB[0], brightnessLimitedRGB[1], brightnessLimitedRGB[2]);
        }
        return new Color(r, g, b);
    }

    /**
     * Adjust gamma based on a given color
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
                log.error(CommonUtility.getWord(Constants.CANT_FIND_GSTREAMER));
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
     * @param width     screen width with scale ratio
     * @param height    screen height with scale ratio
     * @param rgbBuffer full screen captured buffer
     */
    public static void autodetectBlackBars(int width, int height, IntBuffer rgbBuffer) {
        int intBufferSize = (width*height)-1;
        int[][] blackPixelMatrix;
        blackPixelMatrix = calculateBlackPixels(Constants.AspectRatio.LETTERBOX, width, height, intBufferSize, rgbBuffer);
        boolean letterbox = switchAspectRatio(Constants.AspectRatio.LETTERBOX, blackPixelMatrix, false);
        blackPixelMatrix = calculateBlackPixels(Constants.AspectRatio.PILLARBOX, width, height, intBufferSize, rgbBuffer);
        boolean pillarbox = false;
        if (!letterbox) {
            pillarbox = switchAspectRatio(Constants.AspectRatio.PILLARBOX, blackPixelMatrix, false);
        }
        if (!letterbox && !pillarbox) {
            switchAspectRatio(Constants.AspectRatio.PILLARBOX, blackPixelMatrix, true);
        }
    }

    /**
     * Calculate black pixels and put it into an array, works for every supported aspect ratios
     * @param aspectRatio   If not Letterbox is Pillarbox
     * @param width         screen width with scale ratio
     * @param height        screen height with scale ratio
     * @param intBufferSize buffer size
     * @param rgbBuffer     full screen captured buffer
     * @return black pixels array, 0 for light pixel, 1 for black pixel
     */
    static int[][] calculateBlackPixels(Constants.AspectRatio aspectRatio, int width, int height, int intBufferSize, IntBuffer rgbBuffer) {
        int[][] blackPixelMatrix = new int[3][Constants.NUMBER_OF_AREA_TO_CHECK];
        int offsetX;
        int offsetY;
        int chunkSize = (aspectRatio == Constants.AspectRatio.LETTERBOX ? width : height) / Constants.NUMBER_OF_AREA_TO_CHECK;
        int threeWayOffset;
        for (int i = 0; i < (Constants.NUMBER_OF_AREA_TO_CHECK * 3); i++) {
            int j;
            int columnRowIndex;
            if (i < Constants.NUMBER_OF_AREA_TO_CHECK) {
                threeWayOffset = calculateBorders(aspectRatio);
                columnRowIndex = i;
                j = 0;
            } else if (i < (Constants.NUMBER_OF_AREA_TO_CHECK * 2)) {
                threeWayOffset = (aspectRatio == Constants.AspectRatio.LETTERBOX ? height : width) / 2;
                columnRowIndex = i - Constants.NUMBER_OF_AREA_TO_CHECK;
                j = 1;
            } else {
                threeWayOffset = (aspectRatio == Constants.AspectRatio.LETTERBOX ? height : width) - calculateBorders(aspectRatio);
                columnRowIndex = i - (Constants.NUMBER_OF_AREA_TO_CHECK * 2);
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
            // Ignore monochrome pixels that are not on borders, since they might be subtitles
            boolean atBorder = offsetX < 10;
            boolean monochrome = (r == g && g == b);
            boolean dark = (r <= Constants.DEEP_BLACK_CHANNEL_TOLERANCE && g <= Constants.DEEP_BLACK_CHANNEL_TOLERANCE && b <= Constants.DEEP_BLACK_CHANNEL_TOLERANCE);
            if (dark || (!atBorder && monochrome)) {
                blackPixelMatrix[j][columnRowIndex] = 1;
            } else {
                blackPixelMatrix[j][columnRowIndex] = 0;
            }
        }
        return blackPixelMatrix;
    }

    /**
     * Switch to the new aspect ratio based on black bars
     * @param aspectRatio      Letterbox or Pillarbox
     * @param blackPixelMatrix contains black and non black pixels
     * @return boolean if aspect ratio is changed
     */
    static boolean switchAspectRatio(Constants.AspectRatio aspectRatio, int[][] blackPixelMatrix, boolean setFullscreen) {
        boolean isPillarboxLetterbox;
        int topMatrix = Arrays.stream(blackPixelMatrix[0]).sum();
        int centerMatrix = Arrays.stream(blackPixelMatrix[1]).sum();
        int bottomMatrix = Arrays.stream(blackPixelMatrix[2]).sum();
        // NUMBER_OF_AREA_TO_CHECK must be black on botton/top left/right, center pixels must be less than NUMBER_OF_AREA_TO_CHECK (at least on NON black pixel in the center)
        if (topMatrix == Constants.NUMBER_OF_AREA_TO_CHECK && centerMatrix < Constants.NUMBER_OF_AREA_TO_CHECK && bottomMatrix == Constants.NUMBER_OF_AREA_TO_CHECK) {
            if (!FireflyLuciferin.config.getDefaultLedMatrix().equals(aspectRatio.getBaseI18n())) {
                FireflyLuciferin.config.setDefaultLedMatrix(aspectRatio.getBaseI18n());
                GStreamerGrabber.ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(aspectRatio.getBaseI18n());
                log.debug("Switching to " + aspectRatio.getBaseI18n() + " aspect ratio.");
                if (FireflyLuciferin.config.isMqttEnable()) {
                    MQTTManager.publishToTopic(Constants.ASPECT_RATIO_TOPIC, aspectRatio.getBaseI18n());
                }
            }
            isPillarboxLetterbox = true;
        } else {
            if (!FireflyLuciferin.config.getDefaultLedMatrix().equals(Constants.AspectRatio.FULLSCREEN.getBaseI18n())) {
                if (setFullscreen) {
                    FireflyLuciferin.config.setDefaultLedMatrix(Constants.AspectRatio.FULLSCREEN.getBaseI18n());
                    GStreamerGrabber.ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(Constants.AspectRatio.FULLSCREEN.getBaseI18n());
                    log.debug("Switching to " + Constants.AspectRatio.FULLSCREEN.getBaseI18n() + " aspect ratio.");
                    if (FireflyLuciferin.config.isMqttEnable()) {
                        MQTTManager.publishToTopic(Constants.ASPECT_RATIO_TOPIC, Constants.AspectRatio.FULLSCREEN.getBaseI18n());
                    }
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
            return (((FireflyLuciferin.config.getScreenResY() * 480) / 2160) / Constants.RESAMPLING_FACTOR) - 5;
        }
    }

    /**
     * Unlock black bars algorithm every 100 milliseconds
     */
    public void calculateBorders() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable framerateTask = () -> ImageProcessor.CHECK_ASPECT_RATIO = true;
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 1, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Check if there is LEDs duplication every 10 seconds
     */
    public void checkForLedDuplicationTask() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable duplicationTask = () -> unlockCheckLedDuplication = true;
        scheduledExecutorService.scheduleAtFixedRate(duplicationTask, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * If there is LEDs dupliucation for more than N seconds, turn off the lights for power saving
     * @param leds array containing colors
     */
    public void checkForLedDuplication(Color[] leds) {
        unlockCheckLedDuplication = false;
        if (!Arrays.equals(ledArray, leds)) {
            lastFrameTime = LocalDateTime.now();
            ledArray = Arrays.copyOf(leds, leds.length);
        }
        int minutesToShutdown = Integer.parseInt(FireflyLuciferin.config.getPowerSaving().split(" ")[0]);
        if (lastFrameTime.isBefore(LocalDateTime.now().minusMinutes(minutesToShutdown))) {
            if (!shutDownLedStrip) log.debug("Power saving mode ON");
            shutDownLedStrip = true;
        } else {
            if (shutDownLedStrip) log.debug("Power saving mode OFF");
            shutDownLedStrip = false;
        }
    }

    /**
     * Hue Saturation and Lightness management
     * @param r red channel to change
     * @param g green channel to change
     * @param b blue channel to change
     * @return RGB integer, needs bit shifting
     */
    @SuppressWarnings("all")
    public static Color manageColors(int r, int g, int b) {
        float[] hsl = ColorUtilities.RGBtoHSL(r, g, b, null);
        // Current color without corrections
        HSLColor hslColor = new HSLColor();
        hslColor.setHue(hsl[0]);
        hslColor.setSaturation(hsl[1]);
        hslColor.setLightness(hsl[2]);
        // Current color with corrections
        HSLColor hslCorrectedColor = new HSLColor();
        hslCorrectedColor.setHue(0.0F);
        hslCorrectedColor.setSaturation(null);
        hslCorrectedColor.setLightness(null);
        float hsvDegree = hslColor.getHue() * Constants.DEGREE_360;
        // Master channel adds to all color channels
        if (FireflyLuciferin.config.getHueMap().get(Constants.ColorEnum.MASTER).getSaturation() != 0.0F 
                || FireflyLuciferin.config.getHueMap().get(Constants.ColorEnum.MASTER).getLightness() != 0.0F) {
            hslCorrectedColor.setSaturation((float) hslColor.getSaturation() + FireflyLuciferin.config.getHueMap().get(Constants.ColorEnum.MASTER).getSaturation());
            hslColor.setSaturation(hslCorrectedColor.getSaturation());
            hslCorrectedColor.setLightness((float) hslColor.getLightness() + FireflyLuciferin.config.getHueMap().get(Constants.ColorEnum.MASTER).getLightness());
            hslColor.setLightness(hslCorrectedColor.getLightness());
        }
        // Colors channels
        boolean greyDetected = (hslColor.getSaturation() <= Constants.GREY_TOLERANCE);
        if (greyDetected) {
            if (FireflyLuciferin.config.getHueMap().get(Constants.ColorEnum.GREY).getLightness() != 0.0F) {
                correctGreyColors(hslColor, hslCorrectedColor);
            }
        } else if (hsvDegree >= Constants.ColorEnum.RED.getMin() || hsvDegree <= Constants.ColorEnum.RED.getMax() && !greyDetected) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Constants.ColorEnum.RED);
        } else if (hsvDegree >= Constants.ColorEnum.YELLOW.getMin() && hsvDegree <= Constants.ColorEnum.YELLOW.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Constants.ColorEnum.YELLOW);
        } else if (hsvDegree >= Constants.ColorEnum.GREEN.getMin() && hsvDegree <= Constants.ColorEnum.GREEN.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Constants.ColorEnum.GREEN);
        } else if (hsvDegree >= Constants.ColorEnum.CYAN.getMin() && hsvDegree <= Constants.ColorEnum.CYAN.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Constants.ColorEnum.CYAN);
        } else if (hsvDegree >= Constants.ColorEnum.BLUE.getMin() && hsvDegree <= Constants.ColorEnum.BLUE.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Constants.ColorEnum.BLUE);
        } else if (hsvDegree >= Constants.ColorEnum.MAGENTA.getMin() && hsvDegree <= Constants.ColorEnum.MAGENTA.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Constants.ColorEnum.MAGENTA);
        } else {
            log.error("HSV color out of range, this may cause flickering.");
        }
        if (hslCorrectedColor.getSaturation() != null || hslCorrectedColor.getLightness() != null || hslCorrectedColor.getHue() != 0) {
            float hueToUse = hslColor.getHue() + hslCorrectedColor.getHue();
            if (hueToUse < 0.0F) {
                hueToUse = 1.0F + hueToUse; // hueToUse is a negative value, I add it to subtract to hueToUse
            }
            return ColorUtilities.HSLtoRGB(hueToUse, hslCorrectedColor.getSaturation() != null ? hslCorrectedColor.getSaturation() : hslColor.getSaturation(),
                    hslCorrectedColor.getLightness() != null ? hslCorrectedColor.getLightness() : hslColor.getLightness());
        }
        return null;
    }

    /**
     * Correct colors using the stored values
     * @param hslColor           contains current HSL values without corrections
     * @param hslCorrectedColor  contains current HSL values corrections
     * @param hsvDegree          current HSV value in degree 0-360°
     * @param currentColor       current color enum
     */
    private static void correctColors(HSLColor hslColor, HSLColor hslCorrectedColor, float hsvDegree, Constants.ColorEnum currentColor) {
        hslCorrectedColor.setHue(hslCorrectedColor.getHue() + (FireflyLuciferin.config.getHueMap().get(currentColor).getHue() / Constants.DEGREE_360));
        if (FireflyLuciferin.config.getHueMap().get(currentColor).getSaturation() != 0.0F || FireflyLuciferin.config.getHueMap().get(currentColor).getLightness() != 0.0F) {
            hslCorrectedColor.setSaturation((float) hslColor.getSaturation() + FireflyLuciferin.config.getHueMap().get(currentColor).getSaturation());
            hslCorrectedColor.setLightness((float) hslColor.getLightness() + FireflyLuciferin.config.getHueMap().get(currentColor).getLightness());
        }
        hslCorrectedColor.setHue(neighboringColors(hslCorrectedColor.getHue(), hsvDegree, hslCorrectedColor.getHue(), currentColor, Constants.HSL.H));
        hslCorrectedColor.setSaturation(neighboringColors(hslColor.getSaturation(), hsvDegree, hslCorrectedColor.getSaturation(), currentColor, Constants.HSL.S));
        hslCorrectedColor.setLightness(neighboringColors(hslColor.getLightness(), hsvDegree, hslCorrectedColor.getLightness(), currentColor, Constants.HSL.L));
    }

    /**
     * Correct grey colors using the stored values
     * @param hslColor           contains current HSL values without corrections
     * @param hslCorrectedColor  contains current HSL values corrections
     */
    private static void correctGreyColors(HSLColor hslColor, HSLColor hslCorrectedColor) {
        if (FireflyLuciferin.config.getHueMap().get(Constants.ColorEnum.GREY).getLightness() != 0.0F) {
            // Add lightness as percentage to the current ones
            hslCorrectedColor.setLightness(hslColor.getLightness() * (FireflyLuciferin.config.getHueMap().get(Constants.ColorEnum.GREY).getLightness() + 1.0F));
        }
    }

    /**
     * Affects values based on neighboring colors, red channel requires a different behaviour since
     * it's in between 330° and 30° in the HSL scale
     * @param value         saturation or lightness
     * @param hsvDegree     current HSV value in degree 0-360°
     * @param valueToUse    updated value to use from previous computation
     * @param currentColor  current color enum
     * @param hslToUse      use H, S or L
     * @return influenced value
     */
    @SuppressWarnings("all")
    private static Float neighboringColors(float value, float hsvDegree, Float valueToUse, Constants.ColorEnum currentColor, Constants.HSL hslToUse) {
        float nextColorSetting = 0, prevColorSetting = 0;
        switch (hslToUse) {
            case H -> {
                nextColorSetting = FireflyLuciferin.config.getHueMap().get(currentColor.next()).getHue() / Constants.DEGREE_360;
                prevColorSetting = FireflyLuciferin.config.getHueMap().get(currentColor.prev()).getHue() / Constants.DEGREE_360;
            }
            case S -> {
                nextColorSetting = FireflyLuciferin.config.getHueMap().get(currentColor.next()).getSaturation();
                prevColorSetting = FireflyLuciferin.config.getHueMap().get(currentColor.prev()).getSaturation();
            }
            case L -> {
                nextColorSetting = FireflyLuciferin.config.getHueMap().get(currentColor.next()).getLightness();
                prevColorSetting = FireflyLuciferin.config.getHueMap().get(currentColor.prev()).getLightness();
            }
        }
        // Next color
        float nextColorLimitHSL = currentColor.next().getMin();
        if ((hsvDegree >= nextColorLimitHSL - Constants.HSL_TOLERANCE) && (hsvDegree < Constants.ColorEnum.RED.getMin())) {
            float correctionUnit = nextColorSetting / Constants.HSL_TOLERANCE;
            float distance = nextColorLimitHSL - hsvDegree;
            if (valueToUse == null) valueToUse = value;
            valueToUse += correctionUnit * (Constants.HSL_TOLERANCE - distance);
        }
        // Previous color
        float prevColorLimitHSL = currentColor.prev().getMax();
        if ((hsvDegree <= prevColorLimitHSL + Constants.HSL_TOLERANCE) && (hsvDegree > Constants.ColorEnum.RED.getMax())) {
            float correctionUnit = prevColorSetting / Constants.HSL_TOLERANCE;
            if (hsvDegree == 0) hsvDegree = 360;
            float distance = hsvDegree - prevColorLimitHSL;
            if (valueToUse == null) valueToUse = value;
            valueToUse += correctionUnit * (Constants.HSL_TOLERANCE - distance);
        }
        return valueToUse;
    }

}
