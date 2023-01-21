/*
  StorageManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
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
public class PowerSavingManager {

    public static boolean shutDownLedStrip = false;
    public static boolean unlockCheckLedDuplication = false;
    public static LocalDateTime lastFrameTime;
    public static Color[] ledArray;
    static PowerSavingScreenSaver powerSavingScreenSaver = PowerSavingScreenSaver.NOT_TRIGGERED;

    /**
     * Execute a task that checks if screensaver is enabled/running.
     */
    public static void addScreenSaverTask() {
        log.debug("Screen saver is enabled, adding hook for power saving.");
        ScheduledExecutorService scheduledExecutorServiceSS = Executors.newScheduledThreadPool(1);
        scheduledExecutorServiceSS.scheduleAtFixedRate(() -> {
            if (!FireflyLuciferin.RUNNING) {
                takeScreenshot(false);
            }
            managePowerSavingLeds();
            PowerSavingManager.unlockCheckLedDuplication = true;
        }, 60, 15, TimeUnit.SECONDS);
    }

    /**
     * Turn OFF/ON LEds if a power saving event has been triggered
     */
    private static void managePowerSavingLeds() {
        if ((isScreenSaverTaskNeeded() && NativeExecutor.isScreensaverRunning()) || shutDownLedStrip) {
            if (powerSavingScreenSaver != PowerSavingScreenSaver.TRIGGERED_NOT_RUNNING &&
                    powerSavingScreenSaver != PowerSavingScreenSaver.TRIGGERED_RUNNING) {
                if (FireflyLuciferin.RUNNING) {
                    powerSavingScreenSaver = PowerSavingScreenSaver.TRIGGERED_RUNNING;
                } else {
                    powerSavingScreenSaver = PowerSavingScreenSaver.TRIGGERED_NOT_RUNNING;
                }
                log.debug("Screen saver active, power saving on.");
                CommonUtility.turnOffLEDs(FireflyLuciferin.config);
                takeScreenshot(true);
            }
        } else {
            if (powerSavingScreenSaver != PowerSavingScreenSaver.NOT_TRIGGERED) {
                if (powerSavingScreenSaver == PowerSavingScreenSaver.TRIGGERED_RUNNING) {
                    FireflyLuciferin.guiManager.startCapturingThreads();
                    log.debug("Screen saver non active, power saving off.");
                } else if (powerSavingScreenSaver == PowerSavingScreenSaver.TRIGGERED_NOT_RUNNING) {
                    CommonUtility.turnOnLEDs();
                    log.debug("Screen saver non active, power saving off.");
                }
                powerSavingScreenSaver = PowerSavingScreenSaver.NOT_TRIGGERED;
            }
        }
    }

    /**
     * Take screenshot of the screen to check if the image on screen is changed.
     *
     * @param overWriteLedArray true when turning off LEDs while screen capturing. during screen capture the imaage is
     *                          captured using GPU accelerated API, when there is no screen capture the image is captured
     *                          via a simple screenshot. this is needed when switching to from screen capture to no screen capture
     *                          to align the arrays used to check differences.
     */
    @SuppressWarnings("unchecked")
    public static void takeScreenshot(boolean overWriteLedArray) {
        Robot robot;
        try {
            robot = new Robot();
            ImageProcessor.screen = robot.createScreenCapture(ImageProcessor.rect);
            int osScaling = FireflyLuciferin.config.getOsScaling();
            Color[] ledsScreenshotTmp = new Color[ImageProcessor.ledMatrix.size()];
            LinkedHashMap<Integer, LEDCoordinate> ledMatrixTmp = (LinkedHashMap<Integer, LEDCoordinate>) ImageProcessor.ledMatrix.clone();
            // We need an ordered collection so no parallelStream here
            ledMatrixTmp.forEach((key, value) -> {
                if (overWriteLedArray) {
                    ledArray[key - 1] = ImageProcessor.getAverageColor(value, osScaling);
                } else {
                    ledsScreenshotTmp[key - 1] = ImageProcessor.getAverageColor(value, osScaling);
                }
            });
            if (!overWriteLedArray) {
                checkForLedDuplication(ledsScreenshotTmp);
            }
        } catch (AWTException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Check if screen saver detection is needed.
     * Screen saver detection works on Windows only, when Power Saving is enabled and when Screen Saver is enabled.
     */
    public static boolean isScreenSaverTaskNeeded() {
        return NativeExecutor.isWindows() && (!Constants.PowerSaving.DISABLED.equals(LocalizedEnum.fromBaseStr(Constants.PowerSaving.class,
                FireflyLuciferin.config.getPowerSaving()))) && NativeExecutor.isScreenSaverEnabled();
    }

    /**
     * If there is LEDs duplication for more than N seconds, turn off the lights for power saving
     *
     * @param leds array containing colors
     */
    public static void checkForLedDuplication(Color[] leds) {
        if (!isLedArraysEqual(leds)) {
            lastFrameTime = LocalDateTime.now();
            ledArray = Arrays.copyOf(leds, leds.length);
        }
        int minutesToShutdown = Integer.parseInt(FireflyLuciferin.config.getPowerSaving().split(" ")[0]);
        shutDownLedStrip = lastFrameTime.isBefore(LocalDateTime.now().minusMinutes(minutesToShutdown));
    }

    /**
     * Check if the current led array is equal to the previous saved one
     *
     * @param leds array
     * @return if two frames are identical, return true.
     */
    public static boolean isLedArraysEqual(Color[] leds) {
        final int LED_TOLERANCE = 2;
        int difference = 0;
        if (ledArray == null) {
            return false;
        }
        for (int i = 0; i < leds.length; i++) {
            if (leds[i].getRGB() != ledArray[i].getRGB()) {
                difference++;
                if (difference > LED_TOLERANCE) return false;
            }
        }
        return true;
    }

    enum PowerSavingScreenSaver {
        NOT_TRIGGERED,
        TRIGGERED_RUNNING,
        TRIGGERED_NOT_RUNNING
    }

}