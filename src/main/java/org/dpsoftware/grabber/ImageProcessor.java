/*
  ImageProcessor.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.dto.HSLColor;
import org.dpsoftware.utilities.ColorUtilities;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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

    //Get JNA User32 Instace
    com.sun.jna.platform.win32.User32 user32;
    //Get desktop windows handler
    WinDef.HWND hwnd;
    public static Color[] previousColor = new Color[0];

    /**
     * Constructor
     */
    public ImageProcessor(boolean initLedMatrix) {
        if (NativeExecutor.isWindows()) {
            user32 = com.sun.jna.platform.win32.User32.INSTANCE;
            hwnd = user32.GetDesktopWindow();
            GrabberSingleton.getInstance().customGDI32Util = new CustomGDI32Util(hwnd);
        }
        if (initLedMatrix) {
            GrabberSingleton.getInstance().ledMatrix = MainSingleton.getInstance().config.getLedMatrixInUse(MainSingleton.getInstance().config.getDefaultLedMatrix());
            GrabberSingleton.getInstance().rect = new Rectangle(new Dimension((MainSingleton.getInstance().config.getScreenResX() * 100) / MainSingleton.getInstance().config.getOsScaling(),
                    (MainSingleton.getInstance().config.getScreenResY() * 100) / MainSingleton.getInstance().config.getOsScaling()));
        }
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
            if (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.WinAPI.name())) {
                GrabberSingleton.getInstance().screen = GrabberSingleton.getInstance().customGDI32Util.getScreenshot();
            } else {
                GrabberSingleton.getInstance().screen = robot.createScreenCapture(GrabberSingleton.getInstance().rect);
            }
            //ImageIO.write(bi, "png", new java.io.File("screenshot.png"));
        } else {
            GrabberSingleton.getInstance().screen = image;
        }

        // CHECK_ASPECT_RATIO is true 10 times per second, if true and black bars auto detection is on, auto detect black bars
        if (MainSingleton.getInstance().config.isAutoDetectBlackBars()) {
            if (GrabberSingleton.getInstance().CHECK_ASPECT_RATIO) {
                GrabberSingleton.getInstance().CHECK_ASPECT_RATIO = false;
                ImageProcessor.autodetectBlackBars(GrabberSingleton.getInstance().screen.getWidth(), GrabberSingleton.getInstance().screen.getHeight(), null);
                GrabberSingleton.getInstance().ledMatrix = MainSingleton.getInstance().config.getLedMatrixInUse(MainSingleton.getInstance().config.getDefaultLedMatrix());
            }
        }

        int osScaling = MainSingleton.getInstance().config.getOsScaling();
        Color[] leds = new Color[GrabberSingleton.getInstance().ledMatrix.size()];

        // We need an ordered collection so no parallelStream here
        GrabberSingleton.getInstance().ledMatrix.forEach((key, value) ->
                leds[key - 1] = getAverageColor(value, osScaling)
        );
        averageOnAllLeds(leds);
        return leds;
    }

    /**
     * Set the average color on all leds
     *
     * @param leds color array
     */
    public static void averageOnAllLeds(Color[] leds) {
        if (Enums.Algo.AVG_ALL_COLOR.getBaseI18n().equals(MainSingleton.getInstance().config.getAlgo())) {
            Color avgColor = ImageProcessor.getAverageForAllZones(leds, 0, leds.length);
            Arrays.fill(leds, avgColor);
        }
    }

    /**
     * Get the average color from the screen buffer section
     *
     * @param ledCoordinate led X,Y coordinates
     * @param osScaling     OS scaling percentage
     * @return the average color
     */
    public static Color getAverageColor(LEDCoordinate ledCoordinate, int osScaling) {
        return getAverageColor(ledCoordinate, osScaling, false);
    }

    /**
     * Get the average color from the screen buffer section
     * captured by a screenshot
     *
     * @param ledCoordinate        led X,Y coordinates
     * @param osScaling            OS scaling percentage
     * @param getAverageScreenshot if the buffer comes from a screenshot, apply os scaling
     * @return the average color
     */
    public static Color getAverageColor(LEDCoordinate ledCoordinate, int osScaling, boolean getAverageScreenshot) {
        int r = 0, g = 0, b = 0;
        int pickNumber = 0;
        int width = GrabberSingleton.getInstance().screen.getWidth() - 1;
        int height = GrabberSingleton.getInstance().screen.getHeight() - 1;
        int xCoordinate;
        int yCoordinate;
        if (getAverageScreenshot || MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.CPU.name())) {
            xCoordinate = ((ledCoordinate.getX() * 100) / osScaling);
            yCoordinate = ((ledCoordinate.getY() * 100) / osScaling);
        } else {
            xCoordinate = ledCoordinate.getX();
            yCoordinate = ledCoordinate.getY();
        }
        // We start with a negative offset
        for (int x = 0; x < ledCoordinate.getWidth(); x++) {
            for (int y = 0; y < ledCoordinate.getHeight(); y++) {
                int offsetX = (xCoordinate + x);
                int offsetY = (yCoordinate + y);
                int rgb = GrabberSingleton.getInstance().screen.getRGB(Math.min(offsetX, width), Math.min(offsetY, height));
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
     *
     * @param r          avg red channel
     * @param g          avg green channel
     * @param b          avg blue channel
     * @param pickNumber number of computed pixel, used to get the avg
     * @return corrected color
     */
    public static Color correctColors(int r, int g, int b, int pickNumber) {
        // AVG colors inside the tile, no need for the square root here since we calculate the gamma later
        Color adjusted = new Color(r / pickNumber, g / pickNumber, b / pickNumber);
        // Saturate colors and shift bits if needed, apply HSL correcction
        adjusted = manageColors(adjusted);
        // Apply gamma correction
        adjusted = gammaCorrection(adjusted);
        if (MainSingleton.getInstance().config.getLuminosityThreshold() != 0) {
            adjusted = adjustLuminosityThreshold(adjusted, MainSingleton.getInstance().config.getLuminosityThreshold() / 100.0f);
        }
        if (GrabberSingleton.getInstance().isNightLightAuto() || MainSingleton.getInstance().config.getNightLight().equals(Enums.NightLight.ENABLED.getBaseI18n())) {
            adjusted = switch (MainSingleton.getInstance().config.getNightLightLvl()) {
                case 10 -> removeBlueAndMakeItWarm(adjusted, 1.0, 1.0, 0.7);
                case 9 -> removeBlueAndMakeItWarm(adjusted, 0.95, 0.95, 0.6);
                case 8 -> removeBlueAndMakeItWarm(adjusted, 0.85, 0.90, 0.5);
                case 7 -> removeBlueAndMakeItWarm(adjusted, 0.75, 0.90, 0.4);
                case 6 -> removeBlueAndMakeItWarm(adjusted, 0.65, 0.85, 0.3);
                case 5 -> removeBlueAndMakeItWarm(adjusted, 0.55, 0.75, 0.2);
                case 4 -> removeBlueAndMakeItWarm(adjusted, 0.45, 0.65, 0.1);
                case 3 -> removeBlueAndMakeItWarm(adjusted, 0.35, 0.55, 0.1);
                case 2 -> removeBlueAndMakeItWarm(adjusted, 0.25, 0.45, 0.0);
                case 1 -> removeBlueAndMakeItWarm(adjusted, 0.15, 0.35, 0.0);
                default -> adjusted;
            };
        }
        // Brightness limiter to limit strobo effect
        if (MainSingleton.getInstance().config.getBrightnessLimiter() != 1.0F) {
            float[] brightnessLimitedRGB = ColorUtilities.RGBtoHSL(adjusted, null);
            if (brightnessLimitedRGB[2] >= MainSingleton.getInstance().config.getBrightnessLimiter()) {
                brightnessLimitedRGB[2] = MainSingleton.getInstance().config.getBrightnessLimiter();
            }
            return ColorUtilities.HSLtoRGB(brightnessLimitedRGB[0], brightnessLimitedRGB[1], brightnessLimitedRGB[2]);
        }
        return adjusted;
    }

    /**
     * Night light correction
     *
     * @param color          RGB color
     * @param blueReduction  blue reduction
     * @param redBoost       red boost
     * @param greenReduction green reduction
     * @return color
     */
    public static Color removeBlueAndMakeItWarm(Color color, double blueReduction, double redBoost, double greenReduction) {
        // Get normalized RGB components (0-1)
        double r = color.getRed() / 255.0;
        double g = color.getGreen() / 255.0;
        double b = color.getBlue() / 255.0;
        // Remove almost all blue
        b *= (1 - blueReduction);
        // If the color is bright, make it much warmer
        double brightness = (r + g + b) / 3.0; // Average RGB brightness
        if (brightness > 0.5) { // If the color is bright
            r = Math.min(1.0, r + redBoost);  // Strong red boost
            g = Math.max(0.0, g - greenReduction); // Reduce green to prevent greenish tint
        }
        // Convert back to 0-255 values
        return new Color(
                (int) (r * 255),
                (int) (g * 255),
                (int) (b * 255)
        );
    }

    /**
     * If the color is too dark, increase its brightness
     *
     * @param color         RGB color
     * @param minBrightness minimum brightness
     * @return color
     */
    public static Color adjustLuminosityThreshold(Color color, float minBrightness) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        if (hsb[2] < minBrightness) {
            hsb[2] = minBrightness;
        }
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Adjust gamma based on a given color
     *
     * @param color the color to adjust
     * @return the average color
     */
    public static Color gammaCorrection(Color color) {
        return new Color(
                (int) (255.0 * Math.pow((color.getRed() / 255.0), MainSingleton.getInstance().config.getGamma())),
                (int) (255.0 * Math.pow((color.getGreen() / 255.0), MainSingleton.getInstance().config.getGamma())),
                (int) (255.0 * Math.pow((color.getBlue() / 255.0), MainSingleton.getInstance().config.getGamma()))
        );
    }

    /**
     * Auto detect black bars when screen grabbing, set Fullscreen, Letterbox or Pillarbox accordingly
     *
     * @param width     screen width with scale ratio
     * @param height    screen height with scale ratio
     * @param rgbBuffer full screen captured buffer
     */
    public static void autodetectBlackBars(int width, int height, IntBuffer rgbBuffer) {
        int intBufferSize = (width * height) - 1;
        int[][] blackPixelMatrix;
        blackPixelMatrix = calculateBlackPixels(Enums.AspectRatio.LETTERBOX, width, height, intBufferSize, rgbBuffer);
        boolean letterbox = switchAspectRatio(Enums.AspectRatio.LETTERBOX, blackPixelMatrix, false);
        blackPixelMatrix = calculateBlackPixels(Enums.AspectRatio.PILLARBOX, width, height, intBufferSize, rgbBuffer);
        boolean pillarbox = false;
        if (!letterbox) {
            pillarbox = switchAspectRatio(Enums.AspectRatio.PILLARBOX, blackPixelMatrix, false);
        }
        if (!letterbox && !pillarbox) {
            switchAspectRatio(Enums.AspectRatio.PILLARBOX, blackPixelMatrix, true);
        }
    }

    /**
     * Calculate black pixels and put it into an array, works for every supported aspect ratios
     *
     * @param aspectRatio   If not Letterbox is Pillarbox
     * @param width         screen width with scale ratio
     * @param height        screen height with scale ratio
     * @param intBufferSize buffer size
     * @param rgbBuffer     full screen captured buffer
     * @return black pixels array, 0 for light pixel, 1 for black pixel
     */
    static int[][] calculateBlackPixels(Enums.AspectRatio aspectRatio, int width, int height, int intBufferSize, IntBuffer rgbBuffer) {
        int[][] blackPixelMatrix = new int[3][Constants.NUMBER_OF_AREA_TO_CHECK];
        int offsetX;
        int offsetY;
        int chunkSize = (aspectRatio == Enums.AspectRatio.LETTERBOX ? width : height) / Constants.NUMBER_OF_AREA_TO_CHECK;
        int threeWayOffset;
        for (int i = 0; i < (Constants.NUMBER_OF_AREA_TO_CHECK * 3); i++) {
            int j;
            int columnRowIndex;
            if (i < Constants.NUMBER_OF_AREA_TO_CHECK) {
                threeWayOffset = calculateBorders(aspectRatio);
                columnRowIndex = i;
                j = 0;
            } else if (i < (Constants.NUMBER_OF_AREA_TO_CHECK * 2)) {
                threeWayOffset = (aspectRatio == Enums.AspectRatio.LETTERBOX ? height : width) / 2;
                columnRowIndex = i - Constants.NUMBER_OF_AREA_TO_CHECK;
                j = 1;
            } else {
                threeWayOffset = (aspectRatio == Enums.AspectRatio.LETTERBOX ? height : width) - calculateBorders(aspectRatio);
                columnRowIndex = i - (Constants.NUMBER_OF_AREA_TO_CHECK * 2);
                j = 2;
            }
            int chunkSizeOffset = (i > 0) ? chunkSize * columnRowIndex : chunkSize;
            // If not Letterbox is Pillarbox
            if (aspectRatio == Enums.AspectRatio.LETTERBOX) {
                offsetX = chunkSizeOffset;
                offsetY = threeWayOffset;
            } else {
                offsetX = threeWayOffset;
                offsetY = chunkSizeOffset;
            }
            int r, g, b;
            // DUPL
            if (rgbBuffer != null) {
                int widthPlusStride = getWidthPlusStride(width, height, rgbBuffer);
                int bufferOffset = (Math.min(offsetX, widthPlusStride))
                        + ((offsetY < height) ? (offsetY * widthPlusStride) : (height * widthPlusStride));
                int rgb = rgbBuffer.get(Math.min(intBufferSize, bufferOffset));
                r = rgb >> 16 & 0xFF;
                g = rgb >> 8 & 0xFF;
                b = rgb & 0xFF;
            } else { // Other methods
                int rgb = GrabberSingleton.getInstance().screen.getRGB(Math.min(offsetX, width), Math.min(offsetY, height));
                Color color = new Color(rgb);
                r = color.getRed();
                g = color.getGreen();
                b = color.getBlue();
            }
            if (r <= Constants.DEEP_BLACK_CHANNEL_TOLERANCE && g <= Constants.DEEP_BLACK_CHANNEL_TOLERANCE && b <= Constants.DEEP_BLACK_CHANNEL_TOLERANCE) {
                blackPixelMatrix[j][columnRowIndex] = 1;
            } else {
                blackPixelMatrix[j][columnRowIndex] = 0;
            }
        }
        return blackPixelMatrix;
    }

    /**
     * Switch to the new aspect ratio based on black bars
     *
     * @param aspectRatio      Letterbox or Pillarbox
     * @param blackPixelMatrix contains black and non black pixels
     * @return boolean if aspect ratio is changed
     */
    static boolean switchAspectRatio(Enums.AspectRatio aspectRatio, int[][] blackPixelMatrix, boolean setFullscreen) {
        boolean isPillarboxLetterbox;
        int topMatrix = Arrays.stream(blackPixelMatrix[0]).sum();
        int centerMatrix = Arrays.stream(blackPixelMatrix[1]).sum();
        int bottomMatrix = Arrays.stream(blackPixelMatrix[2]).sum();
        // To swìtch to another aspect ratio some center pixels should not be black. Don't switch if the screen is too black.
        int whitePixelPercentage = (Constants.NUMBER_OF_AREA_TO_CHECK * Constants.MINIMUM_WHITE_PIXELS_PCT) / 100;
        boolean enoughWhitePixelForTheChange = centerMatrix < (Constants.NUMBER_OF_AREA_TO_CHECK - whitePixelPercentage);
        // NUMBER_OF_AREA_TO_CHECK must be black on botton/top left/right, center pixels must be less than NUMBER_OF_AREA_TO_CHECK (at least on NON black pixel in the center)
        if (topMatrix == Constants.NUMBER_OF_AREA_TO_CHECK && centerMatrix < Constants.NUMBER_OF_AREA_TO_CHECK && bottomMatrix == Constants.NUMBER_OF_AREA_TO_CHECK) {
            if (!MainSingleton.getInstance().config.getDefaultLedMatrix().equals(aspectRatio.getBaseI18n())) {
                if (enoughWhitePixelForTheChange) {
                    MainSingleton.getInstance().config.setDefaultLedMatrix(aspectRatio.getBaseI18n());
                    GStreamerGrabber.ledMatrix = MainSingleton.getInstance().config.getLedMatrixInUse(aspectRatio.getBaseI18n());
                    log.info("Switching to {} aspect ratio.", aspectRatio.getBaseI18n());
                    if (MainSingleton.getInstance().config.isMqttEnable()) {
                        NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_ASPECT_RATIO), aspectRatio.getBaseI18n());
                    }
                }
            }
            isPillarboxLetterbox = true;
        } else {
            if (!MainSingleton.getInstance().config.getDefaultLedMatrix().equals(Enums.AspectRatio.FULLSCREEN.getBaseI18n())) {
                if (setFullscreen && enoughWhitePixelForTheChange) {
                    MainSingleton.getInstance().config.setDefaultLedMatrix(Enums.AspectRatio.FULLSCREEN.getBaseI18n());
                    GStreamerGrabber.ledMatrix = MainSingleton.getInstance().config.getLedMatrixInUse(Enums.AspectRatio.FULLSCREEN.getBaseI18n());
                    log.info("Switching to {} aspect ratio.", Enums.AspectRatio.FULLSCREEN.getBaseI18n());
                    if (MainSingleton.getInstance().config.isMqttEnable()) {
                        NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_ASPECT_RATIO), Enums.AspectRatio.FULLSCREEN.getBaseI18n());
                    }
                }
            }
            isPillarboxLetterbox = false;
        }
        return isPillarboxLetterbox;
    }

    /**
     * Calculate borders for auto aspect ratio
     *
     * @param aspectRatio Letterbox or Pillarbox
     * @return borders
     */
    public static int calculateBorders(Enums.AspectRatio aspectRatio) {
        if (aspectRatio == Enums.AspectRatio.LETTERBOX) {
            return Math.max(0, (((MainSingleton.getInstance().config.getScreenResY() * Constants.AR_LETTERBOX_GAP) / Constants.REFERENCE_RESOLUTION_FOR_SCALING_Y) / Constants.RESAMPLING_FACTOR) - 5);
        } else {
            return Math.max(0, (((MainSingleton.getInstance().config.getScreenResY() * Constants.AR_PILLARBOX_GAP) / Constants.REFERENCE_RESOLUTION_FOR_SCALING_Y) / Constants.RESAMPLING_FACTOR) - 5);
        }
    }

    /**
     * Hue Saturation and Lightness management
     *
     * @param color color to manage
     * @return RGB integer, needs bit shifting
     */
    @SuppressWarnings("all")
    public static Color manageColors(Color color) {
        float[] hsl = ColorUtilities.RGBtoHSL(color, null);
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
        if (MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MASTER).getSaturation() != 0.0F
                || MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MASTER).getLightness() != 0.0F) {
            hslCorrectedColor.setSaturation((float) hslColor.getSaturation() + MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MASTER).getSaturation());
            hslColor.setSaturation(hslCorrectedColor.getSaturation());
            hslCorrectedColor.setLightness((float) hslColor.getLightness() + MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.MASTER).getLightness());
            hslColor.setLightness(hslCorrectedColor.getLightness());
        }
        // Colors channels
        boolean greyDetected = (hslColor.getSaturation() <= Constants.GREY_TOLERANCE);
        if (greyDetected) {
            if (MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREY).getLightness() != 0.0F) {
                correctGreyColors(hslColor, hslCorrectedColor);
            }
        } else if (hsvDegree >= Enums.ColorEnum.RED.getMin() || hsvDegree <= Enums.ColorEnum.RED.getMax() && !greyDetected) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Enums.ColorEnum.RED);
        } else if (hsvDegree >= Enums.ColorEnum.YELLOW.getMin() && hsvDegree <= Enums.ColorEnum.YELLOW.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Enums.ColorEnum.YELLOW);
        } else if (hsvDegree >= Enums.ColorEnum.GREEN.getMin() && hsvDegree <= Enums.ColorEnum.GREEN.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Enums.ColorEnum.GREEN);
        } else if (hsvDegree >= Enums.ColorEnum.CYAN.getMin() && hsvDegree <= Enums.ColorEnum.CYAN.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Enums.ColorEnum.CYAN);
        } else if (hsvDegree >= Enums.ColorEnum.BLUE.getMin() && hsvDegree <= Enums.ColorEnum.BLUE.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Enums.ColorEnum.BLUE);
        } else if (hsvDegree >= Enums.ColorEnum.MAGENTA.getMin() && hsvDegree <= Enums.ColorEnum.MAGENTA.getMax()) {
            correctColors(hslColor, hslCorrectedColor, hsvDegree, Enums.ColorEnum.MAGENTA);
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
        return color;
    }

    /**
     * Correct colors using the stored values
     *
     * @param hslColor          contains current HSL values without corrections
     * @param hslCorrectedColor contains current HSL values corrections
     * @param hsvDegree         current HSV value in degree 0-360°
     * @param currentColor      current color enum
     */
    private static void correctColors(HSLColor hslColor, HSLColor hslCorrectedColor, float hsvDegree, Enums.ColorEnum currentColor) {
        hslCorrectedColor.setHue(hslCorrectedColor.getHue() + (MainSingleton.getInstance().config.getHueMap().get(currentColor).getHue() / Constants.DEGREE_360));
        if (MainSingleton.getInstance().config.getHueMap().get(currentColor).getSaturation() != 0.0F || MainSingleton.getInstance().config.getHueMap().get(currentColor).getLightness() != 0.0F) {
            hslCorrectedColor.setSaturation((float) hslColor.getSaturation() + MainSingleton.getInstance().config.getHueMap().get(currentColor).getSaturation());
            hslCorrectedColor.setLightness((float) hslColor.getLightness() + MainSingleton.getInstance().config.getHueMap().get(currentColor).getLightness());
        }
        hslCorrectedColor.setHue(neighboringColors(hslCorrectedColor.getHue(), hsvDegree, hslCorrectedColor.getHue(), currentColor, Enums.HSL.H));
        hslCorrectedColor.setSaturation(neighboringColors(hslColor.getSaturation(), hsvDegree, hslCorrectedColor.getSaturation(), currentColor, Enums.HSL.S));
        hslCorrectedColor.setLightness(neighboringColors(hslColor.getLightness(), hsvDegree, hslCorrectedColor.getLightness(), currentColor, Enums.HSL.L));
    }

    /**
     * Correct grey colors using the stored values
     *
     * @param hslColor          contains current HSL values without corrections
     * @param hslCorrectedColor contains current HSL values corrections
     */
    private static void correctGreyColors(HSLColor hslColor, HSLColor hslCorrectedColor) {
        if (MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREY).getLightness() != 0.0F) {
            // Add lightness as percentage to the current ones
            hslCorrectedColor.setLightness(hslColor.getLightness() * (MainSingleton.getInstance().config.getHueMap().get(Enums.ColorEnum.GREY).getLightness() + 1.0F));
        }
    }

    /**
     * Affects values based on neighboring colors, red channel requires a different behaviour since
     * it's in between 330° and 30° in the HSL scale
     *
     * @param value        saturation or lightness
     * @param hsvDegree    current HSV value in degree 0-360°
     * @param valueToUse   updated value to use from previous computation
     * @param currentColor current color enum
     * @param hslToUse     use H, S or L
     * @return influenced value
     */
    @SuppressWarnings("all")
    private static Float neighboringColors(float value, float hsvDegree, Float valueToUse, Enums.ColorEnum currentColor, Enums.HSL hslToUse) {
        float nextColorSetting = 0, prevColorSetting = 0;
        switch (hslToUse) {
            case H -> {
                nextColorSetting = MainSingleton.getInstance().config.getHueMap().get(currentColor.next()).getHue() / Constants.DEGREE_360;
                prevColorSetting = MainSingleton.getInstance().config.getHueMap().get(currentColor.prev()).getHue() / Constants.DEGREE_360;
            }
            case S -> {
                nextColorSetting = MainSingleton.getInstance().config.getHueMap().get(currentColor.next()).getSaturation();
                prevColorSetting = MainSingleton.getInstance().config.getHueMap().get(currentColor.prev()).getSaturation();
            }
            case L -> {
                nextColorSetting = MainSingleton.getInstance().config.getHueMap().get(currentColor.next()).getLightness();
                prevColorSetting = MainSingleton.getInstance().config.getHueMap().get(currentColor.prev()).getLightness();
            }
        }
        // Next color
        float nextColorLimitHSL = currentColor.next().getMin();
        if ((hsvDegree >= nextColorLimitHSL - Constants.HSL_TOLERANCE) && (hsvDegree < Enums.ColorEnum.RED.getMin())) {
            float correctionUnit = nextColorSetting / Constants.HSL_TOLERANCE;
            float distance = nextColorLimitHSL - hsvDegree;
            if (valueToUse == null) valueToUse = value;
            valueToUse += correctionUnit * (Constants.HSL_TOLERANCE - distance);
        }
        // Previous color
        float prevColorLimitHSL = currentColor.prev().getMax();
        if ((hsvDegree <= prevColorLimitHSL + Constants.HSL_TOLERANCE) && (hsvDegree > Enums.ColorEnum.RED.getMax())) {
            float correctionUnit = prevColorSetting / Constants.HSL_TOLERANCE;
            if (hsvDegree == 0) hsvDegree = 360;
            float distance = hsvDegree - prevColorLimitHSL;
            if (valueToUse == null) valueToUse = value;
            valueToUse += correctionUnit * (Constants.HSL_TOLERANCE - distance);
        }
        return valueToUse;
    }

    /**
     * Add N colors for every Zone
     *
     * @param leds       array of colors to send
     * @param sat        satellite where to send colors
     * @param zoneDetail record with start end position
     * @return color array
     */
    public static java.util.List<Color> padColors(Color[] leds, Satellite sat, LEDCoordinate.getStartEndLeds zoneDetail) {
        int zoneStart = zoneDetail.start() - 1;
        int zoneNumLed = (zoneDetail.end() - zoneDetail.start()) + 1;
        int satNumLed = Integer.parseInt(sat.getLedNum());
        List<Color> clonedLeds;
        clonedLeds = new LinkedList<>();
        int multiplier = (int) Math.abs((double) satNumLed / zoneNumLed);
        for (int lIdx = 0; lIdx < zoneNumLed; lIdx++) {
            clonedLeds.add(leds[zoneStart + lIdx]);
            for (int j = 0; j < multiplier - 1; j++) {
                clonedLeds.add(leds[zoneStart + lIdx]);
            }
        }
        return addLeds(satNumLed, clonedLeds);
    }

    /**
     * Add colors on the head and the tail of the color list
     *
     * @param satNumLed  max number of LEDs on the satellite
     * @param clonedLeds array to use for the satellite
     * @return color array
     */
    private static List<Color> addLeds(int satNumLed, List<Color> clonedLeds) {
        int colorToAdd = satNumLed - clonedLeds.size();
        int colorAdded = 0;
        if (colorToAdd > 0) {
            int addEveryLed = Math.abs(clonedLeds.size() / colorToAdd);
            int addIdx = 0;
            ListIterator<Color> iterator = clonedLeds.listIterator();
            while (iterator.hasNext() && colorAdded < colorToAdd) {
                Color c = iterator.next();
                if (addIdx == addEveryLed) {
                    colorAdded++;
                    iterator.add(c);
                    addIdx = 0;
                }
                addIdx++;
            }
            for (int i = clonedLeds.size(); i < satNumLed; i++) {
                clonedLeds.add(clonedLeds.getLast());
            }
        }
        return clonedLeds;
    }

    /**
     * When a satellite has less LEDs than the number of captured zones, reduce colors on the array
     *
     * @param leds       array of colors to send
     * @param sat        satellite where to send colors
     * @param zoneDetail record with start end position
     * @return reduced array
     */
    public static java.util.List<Color> reduceColors(Color[] leds, Satellite sat, LEDCoordinate.getStartEndLeds zoneDetail) {
        int zoneStart = zoneDetail.start() - 1;
        int zoneNumLed = (zoneDetail.end() - zoneDetail.start()) + 1;
        int satNumLed = Integer.parseInt(sat.getLedNum());
        List<Color> clonedLeds;
        clonedLeds = new LinkedList<>();
        int divider = (int) Math.ceil((double) zoneNumLed / satNumLed);
        int r = 0, g = 0, b = 0;
        for (int i = 0; i < zoneNumLed; i++) {
            r += leds[zoneStart + i].getRed();
            g += leds[zoneStart + i].getGreen();
            b += leds[zoneStart + i].getBlue();
            if (i % divider == 0) {
                clonedLeds.add(new Color(r / divider, g / divider, b / divider));
                r = 0;
                g = 0;
                b = 0;
            }
        }
        return addLeds(satNumLed, clonedLeds);
    }

    /**
     * Returns an array of colors containing the average for all zones
     *
     * @param leds      original array of colors
     * @param zoneStart captured zone, start
     * @param zoneEnd   captured zone, end
     * @return avg color from every capture zones
     */
    public static Color getAverageForAllZones(Color[] leds, int zoneStart, int zoneEnd) {
        int rAccumulator = 0;
        int gAccumulator = 0;
        int bAccumulator = 0;
        for (int i = zoneStart; i < zoneEnd; i++) {
            rAccumulator += leds[i].getRed();
            gAccumulator += leds[i].getGreen();
            bAccumulator += leds[i].getBlue();
        }
        int zoneNum = (zoneEnd - zoneStart) + 1;
        return new Color(rAccumulator / zoneNum,
                gAccumulator / zoneNum,
                bAccumulator / zoneNum);
    }

    /**
     * The Exponential Moving Average (EMA) is a type of moving average that assigns more weight to recent data points,
     * making it more responsive to changes compared to the Simple Moving Average (SMA).
     * Unlike SMA, which gives equal weight to all past values, EMA prioritizes recent values,
     * making it more adaptive to rapid changes while still reducing noise.
     * Why Use EMA?
     * - Smoother Transitions
     * - Reduces flickering in color changes.
     * - Creates more natural-looking transitions.
     * - Faster Adaptation to Changes
     * - Avoids excessive lag while still filtering out sudden noise.
     * - Balances Stability and Real-Time Responsiveness
     * - Keeps the colors steady while allowing smooth adaptation to screen changes.
     * A low β (e.g., 0.2) → Smoother but slower response (better for cinematic effects).
     * A high β (e.g., 0.5 - 0.7) → Faster adaptation (better for gaming or fast motion).
     * By tuning β, you can adjust the balance between smoothness and responsiveness.
     *
     * @param leds leds array that will be sent to the strip
     */
    public static void exponentialMovingAverage(Color[] leds) {
        if (!MainSingleton.getInstance().config.getSmoothingType().equals(Enums.Smoothing.DISABLED.getBaseI18n()) && MainSingleton.getInstance().config.getEmaAlpha() > 0) {
            // smoothing factor (0 < β < 1), controlling how much the past influences the present.
            float alpha = MainSingleton.getInstance().config.getEmaAlpha();
            if (ImageProcessor.previousColor.length != leds.length) {
                ImageProcessor.previousColor = leds.clone();
            }
            for (int i = 0; i < leds.length; i++) {
                leds[i] = new Color(
                        (int) (alpha * leds[i].getRed() + (1 - alpha) * ImageProcessor.previousColor[i].getRed()),
                        (int) (alpha * leds[i].getGreen() + (1 - alpha) * ImageProcessor.previousColor[i].getGreen()),
                        (int) (alpha * leds[i].getBlue() + (1 - alpha) * ImageProcessor.previousColor[i].getBlue())
                );
                ImageProcessor.previousColor[i] = leds[i];
            }
        }
    }

    /**
     * Round to the nearest number
     *
     * @param nearestNumberToUse if 10, it rounds to the nearest 10, if 5, it rounds to the nearest five
     *                           example: 6 = 10, 4 = 0, 234 = 230
     * @param numberToRound      number to round
     * @return rounded number, 255 is rounded to 260 so it retuns max 255 for RGB
     */
    @SuppressWarnings("unused")
    public static int roundToTheNearestNumber(int nearestNumberToUse, int numberToRound) {
        int roundedNum = (int) (Math.round(numberToRound / (double) nearestNumberToUse) * nearestNumberToUse);
        return Math.min(roundedNum, 255);
    }

    /**
     * Return the stride of the buffer
     * <p>
     * NOTE: this is unnecessary when using GSTREAMER_PIPELINE_WINDOWS_HARDWARE_HANDLE_SYSTEM_MEMORY and GSTREAMER_PIPELINE_DDUPL_SYSTEM_MEMORY
     * System memory pipeline needs d3d11download element that copies the buffer from video memory to system memory.
     * During this copy, d3d11download aligns the memory and the copied buffer contains zero strides.
     * We want to avoid this expensive copy to system memory, and we want to use the buffer direct from the video memory.
     * Video memory is not aligned, and we need to align it by calculating the correct stride.
     * To have a visual example of "non-aligned memory" you can run a resolution that doesn't need alignment and one that needs it
     * and enable the TRACE debug level.
     * Example: NVIDIA 3840x2160 does not need alignment, 3440x1440 needs alignment.
     * TRACE debug level captures a screenshot of the captured image; when using 3840x2160 resolution, the captured image is correct.
     * When using 3440x1440 resolution, the captured image is scrambled. Every pixel is shifted by 4 pixels.
     * This is the stride that we calculate here.
     *
     * @param width     captured image width (includes rescaling)
     * @param height    captured image height (includes rescaling)
     * @param rgbBuffer captured image IntBuffer
     * @return width that contains stride for some resolutions that needs it like: 3440x1440 on NVIDIA or 1920x1080 on AMD
     */
    public static int getWidthPlusStride(int width, int height, IntBuffer rgbBuffer) {
        int widthPlusStride = width;
        final int exectedCapacityWithoutStride = width * height;
        if ((rgbBuffer.capacity()) != exectedCapacityWithoutStride) {
            double capacity = rgbBuffer.capacity();
            double difference = capacity - exectedCapacityWithoutStride;
            double stride = difference / height;
            widthPlusStride = width + (int) Math.round(stride);
        }
        return widthPlusStride;
    }

    /**
     * Find the distance between two colors
     *
     * @param r1 rgb channel
     * @param g1 rgb channel
     * @param b1 rgb channel
     * @param r2 rgb channel
     * @param g2 rgb channel
     * @param b2 rgb channel
     * @return distance
     */
    @SuppressWarnings("unused")
    public static double colorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
        double rmean = (double) (r1 + r2) / 2;
        int r = r1 - r2;
        int g = g1 - g2;
        int b = b1 - b2;
        double weightR = 2 + rmean / 256;
        double weightG = 4.0;
        double weightB = 2 + (255 - rmean) / 256;
        return Math.sqrt(weightR * r * r + weightG * g * g + weightB * b * b);
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
     * Unlock black bars algorithm every 100 milliseconds
     */
    public void calculateBorders() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable framerateTask = () -> GrabberSingleton.getInstance().CHECK_ASPECT_RATIO = true;
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 1, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Get the path where the users installed the software
     *
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
        log.info(Constants.GSTREAMER_PATH_IN_USE + "{}", installationPath.replaceAll("%20", " "));
        return installationPath.replaceAll("%20", " ");
    }

}
