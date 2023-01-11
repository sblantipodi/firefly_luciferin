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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.GUIManager;
import org.dpsoftware.gui.controllers.ColorCorrectionDialogController;
import org.dpsoftware.managers.dto.LedMatrixInfo;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Write and read yaml configuration file
 */
@Slf4j
public class PowerSavingManager {

    static PowerSavingScreenSaver powerSavingScreenSaver = PowerSavingScreenSaver.NOT_TRIGGERED;
    public static boolean shutDownLedStrip = false;
    public static boolean unlockCheckLedDuplication = true;
    public static LocalDateTime lastFrameTime;
    public static Color[] ledArray;


    enum PowerSavingScreenSaver {
        NOT_TRIGGERED,
        TRIGGERED_RUNNING,
        TRIGGERED_NOT_RUNNING
    }
    
    /**
     * Execute a task that checks if screensaver is enabled/running.
     */
    public static void addScreenSaverTask() {
//        if (isScreenSaverTaskNeeded()) {
        if (shutDownLedStrip) {
            // todo questo blocco sotto lo puoi usare per i 2 if sopra
            log.debug("Screen saver is enabled, adding hook for power saving.");
            ScheduledExecutorService scheduledExecutorServiceSS = Executors.newScheduledThreadPool(1);
            scheduledExecutorServiceSS.scheduleAtFixedRate(() -> {
                // todo riprovare questa logica
                if (NativeExecutor.isScreensaverRunning()) {
                    if (powerSavingScreenSaver != PowerSavingScreenSaver.TRIGGERED_NOT_RUNNING &&
                            powerSavingScreenSaver != PowerSavingScreenSaver.TRIGGERED_RUNNING) {
                        if (FireflyLuciferin.RUNNING) {
                            powerSavingScreenSaver = PowerSavingScreenSaver.TRIGGERED_RUNNING;
                        } else {
                            powerSavingScreenSaver = PowerSavingScreenSaver.TRIGGERED_NOT_RUNNING;
                        }
                        CommonUtility.conditionedLog(NativeExecutor.class.getName(), "Screen saver active, power saving on.");
                        CommonUtility.turnOffLEDs(FireflyLuciferin.config);
                        log.debug("SS OFF");
                    }
                } else {
                    if (powerSavingScreenSaver != PowerSavingScreenSaver.NOT_TRIGGERED) {
                        if (powerSavingScreenSaver == PowerSavingScreenSaver.TRIGGERED_RUNNING) {
                            FireflyLuciferin.guiManager.startCapturingThreads();
                            log.debug("SS ON");
                            CommonUtility.conditionedLog(NativeExecutor.class.getName(), "Screen saver non active, power saving off.");

                        } else if (powerSavingScreenSaver == PowerSavingScreenSaver.TRIGGERED_NOT_RUNNING) {
                            CommonUtility.turnOnLEDs();
                            log.debug("SS ON");
                            CommonUtility.conditionedLog(NativeExecutor.class.getName(), "Screen saver non active, power saving off.");

                        }
                        powerSavingScreenSaver = PowerSavingScreenSaver.NOT_TRIGGERED;
                    }
                }
                // todo 60 15
            }, 10, 5, TimeUnit.SECONDS);
        }
    }

    /**
     *
     */
    public static void powerSavingNotRunning() {
        ScheduledExecutorService powerSavingTask = Executors.newScheduledThreadPool(1);
        powerSavingTask.scheduleAtFixedRate(() -> {
            if (!FireflyLuciferin.RUNNING) {
                Robot robot = null;
                BufferedImage screenshot;
                try {
                    robot = new Robot();
                    ImageProcessor.screen = robot.createScreenCapture(ImageProcessor.rect);
                    int osScaling = FireflyLuciferin.config.getOsScaling();
                    Color[] ledsTmp = new Color[ImageProcessor.ledMatrix.size()];
                    LinkedHashMap<Integer, LEDCoordinate> ledMatrixTmp = (LinkedHashMap<Integer, LEDCoordinate>) ImageProcessor.ledMatrix.clone();
                    // We need an ordered collection so no parallelStream here
                    ledMatrixTmp.forEach((key, value) -> {
                        ledsTmp[key - 1] = ImageProcessor.getAverageColor(value, osScaling);
//                        log.debug(ledsTmp[key - 1].getRed() + " " + ledsTmp[key - 1].getGreen() + " " + ledsTmp[key - 1].getBlue() + " ");
                    });
                    checkForLedDuplication(ledsTmp);

                    log.debug("SHUTD" + shutDownLedStrip);


                } catch (AWTException e) {
                    log.error(e.getMessage());
                }
            }
        }, 10, 15, TimeUnit.SECONDS);
    }

    /**
     * @return
     */
    public static boolean isScreenSaverTaskNeeded() {
        return NativeExecutor.isWindows() && (!Constants.PowerSaving.DISABLED.equals(LocalizedEnum.fromBaseStr(Constants.PowerSaving.class,
                FireflyLuciferin.config.getPowerSaving()))) && NativeExecutor.isScreenSaverEnabled();
    }

    /**
     * Check if there is LEDs duplication every 10 seconds
     */
    public static void checkForLedDuplicationTask() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable duplicationTask = () -> unlockCheckLedDuplication = true;
        scheduledExecutorService.scheduleAtFixedRate(duplicationTask, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * If there is LEDs duplication for more than N seconds, turn off the lights for power saving
     *
     * @param leds array containing colors
     */
    public static void checkForLedDuplication(Color[] leds) {
        unlockCheckLedDuplication = false;
        if (!isLedArraysEqual(leds)) {
            if (!isLedsAllBlack(leds)) {
                lastFrameTime = LocalDateTime.now();
                ledArray = Arrays.copyOf(leds, leds.length);
            }
        }
        int minutesToShutdown = Integer.parseInt(FireflyLuciferin.config.getPowerSaving().split(" ")[0]);
        // todo
//        if (lastFrameTime.isBefore(LocalDateTime.now().minusMinutes(minutesToShutdown))) {
        if (lastFrameTime.isBefore(LocalDateTime.now().minusSeconds(minutesToShutdown))) {
            if (!shutDownLedStrip) log.debug("Power saving mode ON");
            shutDownLedStrip = true;
        } else {
            if (shutDownLedStrip) log.debug("Power saving mode OFF");
            shutDownLedStrip = false;
        }
    }

    /**
     * Check if the strip is turned off (all LEDs are black)
     *
     * @param leds array
     * @return boolean if the strip is turned off
     */
    public static boolean isLedsAllBlack(Color[] leds) {
        boolean allBlack = true;
        for (Color c : leds) {
            if (c.getRed() != 0 || c.getGreen() != 0 || c.getBlue() != 0) {
                allBlack = false;
                break;
            }
        }
        return allBlack;
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

}