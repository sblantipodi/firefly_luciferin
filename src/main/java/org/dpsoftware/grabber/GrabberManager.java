/*
  GrabberManager.java

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
package org.dpsoftware.grabber;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.AudioLoopback;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.controllers.SettingsController;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.MqttFramerateDto;
import org.dpsoftware.utilities.CommonUtility;
import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.dpsoftware.FireflyLuciferin.*;

/**
 * Screen grabbing manager
 */
@Slf4j
public class GrabberManager {

    /**
     * Launch Advanced screen grabber (DDUPL for Windows, ximagesrc for Linux)
     *
     * @param scheduledExecutorService executor service used to restart grabbing if it fails
     * @param imageProcessor           image processor utility
     */
    public void launchAdvancedGrabber(ScheduledExecutorService scheduledExecutorService, ImageProcessor imageProcessor) {
        imageProcessor.initGStreamerLibraryPaths();
        //System.setProperty("gstreamer.GNative.nameFormats", "%s-0|lib%s-0|%s|lib%s");
        Gst.init(Constants.SCREEN_GRABBER, "");
        AtomicInteger pipelineRetry = new AtomicInteger();

        String linuxParams = null;
        if (NativeExecutor.isLinux()) {
            linuxParams = PipelineManager.getLinuxPipelineParams();
        }
        String finalLinuxParams = linuxParams;
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (!PipelineManager.pipelineStopping && RUNNING && FPS_PRODUCER_COUNTER == 0) {
                pipelineRetry.getAndIncrement();
                if (pipe == null || !pipe.isPlaying() || pipelineRetry.get() >= 2) {
                    if (pipe != null) {
                        log.debug("Restarting pipeline");
                        pipe.stop();
                    } else {
                        log.debug("Starting a new pipeline");
                    }
                    GStreamerGrabber vc = new GStreamerGrabber();
                    Bin bin;
                    if (NativeExecutor.isWindows()) {
                        DisplayManager displayManager = new DisplayManager();
                        String monitorNativePeer = String.valueOf(displayManager.getDisplayInfo(FireflyLuciferin.config.getMonitorNumber()).getNativePeer());
                        bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_WINDOWS_HARDWARE_HANDLE.replace("{0}", monitorNativePeer), true);
                    } else if (NativeExecutor.isLinux()) {
                        bin = Gst.parseBinFromDescription(finalLinuxParams, true);
                    } else {
                        bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_MAC, true);
                    }
                    pipe = new Pipeline();
                    pipe.addMany(bin, vc.getElement());
                    Pipeline.linkMany(bin, vc.getElement());
                    JFrame f = new JFrame(Constants.SCREEN_GRABBER);
                    f.add(vc);
                    vc.setPreferredSize(new Dimension(config.getScreenResX(), config.getScreenResY()));
                    f.pack();
                    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    pipe.play();
                    f.setVisible(false);
                }
            } else {
                pipelineRetry.set(0);
            }
        }, 1, 2, TimeUnit.SECONDS);
    }

    /**
     * Producers for CPU and WinAPI capturing
     *
     * @param scheduledExecutorService executor service used to restart grabbing if it fails
     * @param executorNumber           number of threads to execute standard pipeline
     * @throws AWTException GUI exception
     */
    public void launchStandardGrabber(ScheduledExecutorService scheduledExecutorService, int executorNumber) throws AWTException {
        Robot robot = null;
        for (int i = 0; i < executorNumber; i++) {
            // One AWT Robot instance every 3 threads seems to be the sweet spot for performance/memory.
            if (!(config.getCaptureMethod().equals(Configuration.CaptureMethod.WinAPI.name())) && i % 3 == 0) {
                robot = new Robot();
                log.info(CommonUtility.getWord(Constants.SPAWNING_ROBOTS));
            }
            Robot finalRobot = robot;
            // No need for completablefuture here, we wrote the queue with a producer and we forget it
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (RUNNING) {
                    producerTask(finalRobot);
                }
            }, 0, 25, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Write Serial Stream to the Serial Output
     *
     * @param robot an AWT Robot instance for screen capture.
     *              One instance every three threads seems to be the hot spot for performance.
     */
    private void producerTask(Robot robot) {
        if (!AudioLoopback.RUNNING_AUDIO || Constants.Effect.MUSIC_MODE_BRIGHT.getBaseI18n().equals(FireflyLuciferin.config.getEffect())
                || Constants.Effect.MUSIC_MODE_RAINBOW.getBaseI18n().equals(FireflyLuciferin.config.getEffect())) {
            PipelineManager.offerToTheQueue(ImageProcessor.getColors(robot, null));
            FPS_PRODUCER_COUNTER++;
        }
        //System.gc(); // uncomment when hammering the JVM
    }

    /**
     * Calculate Screen Capture Framerate and how fast your microcontroller can consume it
     */
    public void getFPS() {
        AtomicInteger framerateAlert = new AtomicInteger();
        AtomicBoolean notified = new AtomicBoolean(false);
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // Create a task that runs every 5 seconds
        Runnable framerateTask = () -> {
            if (FPS_PRODUCER_COUNTER > 0 || FPS_CONSUMER_COUNTER > 0) {
                if (CommonUtility.isSingleDeviceOtherInstance() && FireflyLuciferin.config.getEffect().contains(Constants.MUSIC_MODE)) {
                    FPS_PRODUCER = FPS_GW_CONSUMER;
                } else {
                    FPS_PRODUCER = FPS_PRODUCER_COUNTER / 5;
                }
                FPS_CONSUMER = FPS_CONSUMER_COUNTER / 5;
                CommonUtility.conditionedLog(this.getClass().getName(),
                        " --* Producing @ " + FPS_PRODUCER + " FPS *-- " + " --* Consuming @ " + FPS_GW_CONSUMER + " FPS *-- ");
                FPS_CONSUMER_COUNTER = FPS_PRODUCER_COUNTER = 0;
            } else {
                FPS_PRODUCER = FPS_CONSUMER = 0;
            }
            runBenchmark(framerateAlert, notified);
            if (config.isMqttEnable()) {
                NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.FIREFLY_LUCIFERIN_FRAMERATE),
                        CommonUtility.toJsonString(new MqttFramerateDto(String.valueOf(FPS_PRODUCER), String.valueOf(FPS_CONSUMER))));
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Small benchmark to check if Glow Worm Luciferin firmware can keep up with Firefly Luciferin PC software
     *
     * @param framerateAlert number of times Firefly was faster than Glow Worm
     * @param notified       don't alert user more than one time
     */
    private void runBenchmark(AtomicInteger framerateAlert, AtomicBoolean notified) {
        if (!notified.get()) {
            if ((FPS_PRODUCER > 0) && (framerateAlert.get() < Constants.NUMBER_OF_BENCHMARK_ITERATION)
                    && (FPS_GW_CONSUMER < FPS_PRODUCER - Constants.BENCHMARK_ERROR_MARGIN)) {
                framerateAlert.getAndIncrement();
            } else {
                framerateAlert.set(0);
            }
            if (FPS_GW_CONSUMER == 0 && framerateAlert.get() == 6 && config.isFullFirmware()) {
                log.debug("Glow Worm Luciferin is not responding, restarting...");
                NativeExecutor.restartNativeInstance();
            }
            if (framerateAlert.get() == Constants.NUMBER_OF_BENCHMARK_ITERATION && !notified.get() && FPS_GW_CONSUMER > 0) {
                notified.set(true);
                javafx.application.Platform.runLater(() -> {
                    int suggestedFramerate;
                    if (FPS_GW_CONSUMER > (144 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 144;
                    } else if (FPS_GW_CONSUMER > (120 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 120;
                    } else if (FPS_GW_CONSUMER > (90 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 90;
                    } else if (FPS_GW_CONSUMER > (60 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 60;
                    } else if (FPS_GW_CONSUMER > (50 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 50;
                    } else if (FPS_GW_CONSUMER > (40 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 40;
                    } else if (FPS_GW_CONSUMER > (30 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 30;
                    } else if (FPS_GW_CONSUMER > (25 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 25;
                    } else if (FPS_GW_CONSUMER > (20 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 20;
                    } else if (FPS_GW_CONSUMER > (15 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 15;
                    } else if (FPS_GW_CONSUMER > (10 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 10;
                    } else {
                        suggestedFramerate = 5;
                    }
                    log.error(CommonUtility.getWord(Constants.FRAMERATE_HEADER) + ". " + CommonUtility.getWord(Constants.FRAMERATE_CONTEXT)
                            .replace("{0}", String.valueOf(suggestedFramerate)));
                    if (config.isSyncCheck()) {
                        Optional<ButtonType> result = guiManager.showAlert(CommonUtility.getWord(Constants.FRAMERATE_TITLE), CommonUtility.getWord(Constants.FRAMERATE_HEADER),
                                CommonUtility.getWord(Constants.FRAMERATE_CONTEXT).replace("{0}", String.valueOf(suggestedFramerate)), Alert.AlertType.CONFIRMATION);
                        ButtonType button = result.orElse(ButtonType.OK);
                        if (button == ButtonType.OK) {
                            try {
                                StorageManager sm = new StorageManager();
                                config.setDesiredFramerate(String.valueOf(suggestedFramerate));
                                sm.writeConfig(config, null);
                                SettingsController settingsController = new SettingsController();
                                settingsController.exit(null);
                            } catch (IOException ioException) {
                                log.error("Can't write config file.");
                            }
                        }
                    }
                });
            }
        }
    }
}
