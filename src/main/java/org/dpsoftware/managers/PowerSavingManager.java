/*
  StorageManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.managers;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.GrabberSingleton;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.utilities.CommonUtility;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Write and read yaml configuration file
 */
@Slf4j
@Getter
@Setter
public class PowerSavingManager {

    final int PIXEL_LUMINANCE_TOLERANCE = 5;
    public boolean shutDownLedStrip = false;
    public boolean unlockCheckLedDuplication = false;
    public LocalDateTime lastFrameTime;
    public Color[] ledArray;
    boolean screenSaverTaskNeeded = false;
    boolean screenSaverRunning = false;
    int lastMouseX;
    int lastMouseY;
    boolean mouseMoved = true;
    boolean stateBeforeChange = false;
    boolean changedState = false;
    boolean turnedOffByPowerSaving = false;

    /**
     * Take screenshot of the screen to check if the image on screen is changed.
     */
    private static void takeScreenshot() throws AWTException, IOException {
        Robot robot;
        robot = new Robot();
        DisplayManager displayManager = new DisplayManager();
        DisplayInfo monitorInfo = displayManager.getDisplayInfo(MainSingleton.getInstance().config.getMonitorNumber());
        // We use the config file here because Linux thinks that the display width and height is the sum of the available screens
        GrabberSingleton.getInstance().screen = robot.createScreenCapture(new Rectangle(
                (int) (monitorInfo.getDisplayInfoAwt().getMinX() / monitorInfo.getScaleX()),
                (int) (monitorInfo.getDisplayInfoAwt().getMinY() / monitorInfo.getScaleX()),
                (int) (MainSingleton.getInstance().config.getScreenResX() / monitorInfo.getScaleX()),
                (int) (MainSingleton.getInstance().config.getScreenResY() / monitorInfo.getScaleX())
        ));
        if (log.isTraceEnabled()) {
            log.trace("Taking screenshot");
            ImageIO.write(GrabberSingleton.getInstance().screen, "png", new java.io.File("screenshot" + MainSingleton.getInstance().whoAmI + ".png"));
        }
    }

    /**
     * Execute a task that checks if screensaver is enabled/running.
     */
    public void addPowerSavingTask() {
        log.info("Adding hook for power saving.");
        PointerInfo a = MouseInfo.getPointerInfo();
        Point mouseCoordinate = a.getLocation();
        lastMouseX = (int) mouseCoordinate.getX();
        lastMouseY = (int) mouseCoordinate.getY();
        screenSaverTaskNeeded = checkIfScreensaverIsSet();
        ScheduledExecutorService scheduledExecutorServiceSS = Executors.newScheduledThreadPool(1);
        // The methods below must run in a separate thread from the capture pipeline
        scheduledExecutorServiceSS.scheduleAtFixedRate(() -> {
            if (!changedState) {
                stateBeforeChange = MainSingleton.getInstance().config.isToggleLed();
            }
            if (screenSaverTaskNeeded) {
                screenSaverRunning = NativeExecutor.isScreensaverRunning();
                if (screenSaverRunning)
                    log.info("Screen saver task started.");
                else
                    log.info("Screen saver task stoppedd.");
            }
            if (!MainSingleton.getInstance().RUNNING && !CommonUtility.isSingleDeviceMultiScreen() && stateBeforeChange) {
                evaluateStaticScreen();
                toggleLogic();
            }
            unlockCheckLedDuplication = true;
        }, 60, 10, TimeUnit.SECONDS);
        mouseListenerThread();
    }

    /**
     *
     */
    private void toggleLogic() {
        if (shutDownLedStrip && MainSingleton.getInstance().config.isToggleLed()) {
            changedState = true;
            MainSingleton.getInstance().config.setToggleLed(false);
            CommonUtility.turnOffLEDs(MainSingleton.getInstance().config);
            turnedOffByPowerSaving = true;
        }
        if (!shutDownLedStrip && !MainSingleton.getInstance().config.isToggleLed() && turnedOffByPowerSaving) {
            changedState = true;
            MainSingleton.getInstance().config.setToggleLed(true);
            CommonUtility.turnOnLEDs();
            turnedOffByPowerSaving = false;
        }
        if ((MainSingleton.getInstance().config.isToggleLed() == stateBeforeChange) && changedState) {
            changedState = false;
        }
    }

    /**
     * Manage mouse events in a separate thread
     */
    private void mouseListenerThread() {
        ScheduledExecutorService ssMouse = Executors.newScheduledThreadPool(1);
        ssMouse.scheduleAtFixedRate(() -> {
            if (!screenSaverRunning) {
                PointerInfo a = MouseInfo.getPointerInfo();
                Point mouseCoordinates = a.getLocation();
                if (lastMouseX == (int) mouseCoordinates.getX() && lastMouseY == (int) mouseCoordinates.getY()) {
                    mouseMoved = false;
                } else {
                    mouseMoved = true;
                    lastFrameTime = LocalDateTime.now();
                    shutDownLedStrip = false;
                    toggleLogic();
                }
                lastMouseX = (int) mouseCoordinates.getX();
                lastMouseY = (int) mouseCoordinates.getY();
            }
        }, 30, 1, TimeUnit.SECONDS);
    }

    /**
     * Evaluate screen when screen capture is stopped
     */
    @SuppressWarnings("unchecked")
    public void evaluateStaticScreen() {
        try {
            takeScreenshot();
            int osScaling = MainSingleton.getInstance().config.getOsScaling();
            Color[] ledsScreenshotTmp = new Color[GrabberSingleton.getInstance().ledMatrix.size()];
            LinkedHashMap<Integer, LEDCoordinate> ledMatrixTmp = (LinkedHashMap<Integer, LEDCoordinate>) GrabberSingleton.getInstance().ledMatrix.clone();
            // We need an ordered collection so no parallelStream here
            ledMatrixTmp.forEach((key, value) -> ledsScreenshotTmp[key - 1] = ImageProcessor.getAverageColor(value, osScaling));
            checkForLedDuplication(ledsScreenshotTmp);
        } catch (AWTException | IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Check if screen saver detection is needed.
     * Screen saver detection works on Windows only, when Power Saving is enabled and when Screen Saver is enabled.
     */
    public boolean checkIfScreensaverIsSet() {
        return NativeExecutor.isWindows() && (!Enums.PowerSaving.DISABLED.equals(LocalizedEnum.fromBaseStr(Enums.PowerSaving.class,
                MainSingleton.getInstance().config.getPowerSaving()))) && NativeExecutor.isScreenSaverEnabled();
    }

    /**
     * If there is LEDs duplication for more than N seconds, turn off the lights for power saving.
     * If screensaver running turn off the screen.
     *
     * @param leds array containing colors
     */
    public void checkForLedDuplication(Color[] leds) {
        if (!isLedArraysEqual(leds)) {
            lastFrameTime = LocalDateTime.now();
            ledArray = Arrays.copyOf(leds, leds.length);
        }
        int minutesToShutdown = Integer.parseInt(MainSingleton.getInstance().config.getPowerSaving().split(" ")[0]);
        if (!screenSaverRunning) {
            shutDownLedStrip = lastFrameTime.isBefore(LocalDateTime.now().minusMinutes(minutesToShutdown));
        } else {
            shutDownLedStrip = true;
        }
    }

    /**
     * Check if the current led array is equal to the previous saved one
     *
     * @param leds array
     * @return if two frames are identical, return true.
     */
    public boolean isLedArraysEqual(Color[] leds) {
        final int LED_DIFFERENCE_TOLERANCE = 16;
        int difference = 0;
        if (ledArray == null) {
            return false;
        }
        for (int i = 0; i < leds.length; i++) {
            if (leds[i].getRGB() != ledArray[i].getRGB()) {
                boolean differenceOutOfTolerance = Math.abs(leds[i].getRed() - ledArray[i].getRed()) > PIXEL_LUMINANCE_TOLERANCE
                        || Math.abs(leds[i].getGreen() - ledArray[i].getGreen()) > PIXEL_LUMINANCE_TOLERANCE
                        || Math.abs(leds[i].getBlue() - ledArray[i].getBlue()) > PIXEL_LUMINANCE_TOLERANCE;
                if (differenceOutOfTolerance) {
                    difference++;
                }
                if (difference > LED_DIFFERENCE_TOLERANCE) return false;
            }
        }
        return true;
    }

}