/*
  GrabberManager.java

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

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.controllers.SettingsController;
import org.dpsoftware.managers.*;
import org.dpsoftware.managers.dto.MqttFramerateDto;
import org.dpsoftware.utilities.CommonUtility;
import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Screen grabbing manager
 */
@Slf4j
public class GrabberManager {

    public Bin bin;
    GStreamerGrabber vc;

    /**
     * Get suggested framerate
     *
     * @return suggested framerate
     */
    private static int getSuggestedFramerate() {
        int suggestedFramerate;
        if (MainSingleton.getInstance().FPS_GW_CONSUMER > (144 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 144;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (120 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 120;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (90 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 90;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (60 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 60;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (50 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 50;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (40 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 40;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (30 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 30;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (25 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 25;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (20 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 20;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (15 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 15;
        } else if (MainSingleton.getInstance().FPS_GW_CONSUMER > (10 + Constants.BENCHMARK_ERROR_MARGIN)) {
            suggestedFramerate = 10;
        } else {
            suggestedFramerate = 5;
        }
        return suggestedFramerate;
    }

    /**
     * Launch Advanced screen grabber (DDUPL for Windows, ximagesrc for Linux)
     *
     * @param imageProcessor image processor utility
     */
    public void launchAdvancedGrabber(ImageProcessor imageProcessor) {
        imageProcessor.initGStreamerLibraryPaths();
        //System.setProperty("gstreamer.GNative.nameFormats", "%s-0|lib%s-0|%s|lib%s");
        Gst.init(Constants.SCREEN_GRABBER, "");
        AtomicInteger pipelineRetry = new AtomicInteger();
        String linuxParams = null;
        if (NativeExecutor.isLinux()) {
            linuxParams = PipelineManager.getLinuxPipelineParams();
        }
        String finalLinuxParams = linuxParams;
        Gst.getExecutor().scheduleAtFixedRate(() -> {
            if (!ManagerSingleton.getInstance().pipelineStopping && MainSingleton.getInstance().RUNNING && MainSingleton.getInstance().FPS_PRODUCER_COUNTER == 0) {
                pipelineRetry.getAndIncrement();
                if (GrabberSingleton.getInstance().pipe == null || !GrabberSingleton.getInstance().pipe.isPlaying() || pipelineRetry.get() >= 2) {
                    if (GrabberSingleton.getInstance().pipe != null) {
                        log.info("Restarting pipeline");
                        GrabberSingleton.getInstance().pipe.stop();
                    } else {
                        log.info("Starting a new pipeline");
                        GrabberSingleton.getInstance().pipe = new Pipeline();
                        if (NativeExecutor.isWindows()) {
                            DisplayManager displayManager = new DisplayManager();
                            String monitorNativePeer = String.valueOf(displayManager.getDisplayInfo(MainSingleton.getInstance().config.getMonitorNumber()).getNativePeer());
                            if (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX11.name())) {
                                bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_WINDOWS_HARDWARE_HANDLE_DX11.replace("{0}", monitorNativePeer), true);
                            } else {
                                bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_WINDOWS_HARDWARE_HANDLE_DX12.replace("{0}", monitorNativePeer), true);
                            }
                        } else if (NativeExecutor.isLinux()) {
                            bin = Gst.parseBinFromDescription(finalLinuxParams, true);
                        } else {
                            bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_MAC, true);
                        }
                    }
                    vc = new GStreamerGrabber();
                    GrabberSingleton.getInstance().pipe.addMany(bin, vc.getElement());
                    Pipeline.linkMany(bin, vc.getElement());
                    JFrame f = new JFrame(Constants.SCREEN_GRABBER);
                    f.add(vc);
                    vc.setPreferredSize(new Dimension(MainSingleton.getInstance().config.getScreenResX(), MainSingleton.getInstance().config.getScreenResY()));
                    f.pack();
                    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    GrabberSingleton.getInstance().pipe.play();
                    f.setVisible(false);
                }
            } else {
                pipelineRetry.set(0);
            }
            disposePipeline();
        }, 1, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Old pipeline is not needed anymore, dispose the pipeline and all the related objects to free up system memory.
     */
    private void disposePipeline() {
        if (GrabberSingleton.getInstance().pipe != null && !GrabberSingleton.getInstance().pipe.isPlaying() && !ManagerSingleton.getInstance().pipelineStarting) {
            log.info("Free up system memory");
            Gst.invokeLater(bin::dispose);
            Gst.invokeLater(vc.videosink::dispose);
            Gst.invokeLater(vc.getElement()::dispose);
            Gst.invokeLater(GrabberSingleton.getInstance().pipe::dispose);
            GStreamerGrabber.ledMatrix = null;
            bin = null;
            vc.videosink = null;
            vc = null;
            GrabberSingleton.getInstance().pipe = null;
            System.gc();
        }
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
            if (!(MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.WinAPI.name())) && i % 3 == 0) {
                robot = new Robot();
                log.info(CommonUtility.getWord(Constants.SPAWNING_ROBOTS));
            }
            Robot finalRobot = robot;
            // No need for completablefuture here, we wrote the queue with a producer and we forget it
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (MainSingleton.getInstance().RUNNING) {
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
        if (!AudioSingleton.getInstance().RUNNING_AUDIO || Enums.Effect.MUSIC_MODE_BRIGHT.getBaseI18n().equals(MainSingleton.getInstance().config.getEffect())
                || Enums.Effect.MUSIC_MODE_RAINBOW.getBaseI18n().equals(MainSingleton.getInstance().config.getEffect())) {
            PipelineManager.offerToTheQueue(ImageProcessor.getColors(robot, null));
            MainSingleton.getInstance().FPS_PRODUCER_COUNTER++;
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
        Runnable framerateTask = () -> {
            if (MainSingleton.getInstance().FPS_PRODUCER_COUNTER > 0 || MainSingleton.getInstance().FPS_CONSUMER_COUNTER > 0) {
                if (CommonUtility.isSingleDeviceOtherInstance() && MainSingleton.getInstance().config.getEffect().contains(Constants.MUSIC_MODE)) {
                    MainSingleton.getInstance().FPS_PRODUCER = MainSingleton.getInstance().FPS_GW_CONSUMER;
                } else {
                    MainSingleton.getInstance().FPS_PRODUCER = MainSingleton.getInstance().FPS_PRODUCER_COUNTER / 5;
                }
                MainSingleton.getInstance().FPS_CONSUMER = MainSingleton.getInstance().FPS_CONSUMER_COUNTER / 5;
                log.trace(" --* Producing @ {} FPS *--  --* Consuming @ {} FPS *-- ", MainSingleton.getInstance().FPS_PRODUCER, MainSingleton.getInstance().FPS_GW_CONSUMER);
                MainSingleton.getInstance().FPS_CONSUMER_COUNTER = MainSingleton.getInstance().FPS_PRODUCER_COUNTER = 0;
            } else {
                MainSingleton.getInstance().FPS_PRODUCER = MainSingleton.getInstance().FPS_CONSUMER = 0;
            }
            runBenchmark(framerateAlert, notified);
            if (MainSingleton.getInstance().config.isMqttEnable()) {
                if (!MainSingleton.getInstance().exitTriggered) {
                    MqttFramerateDto mqttFramerateDto = new MqttFramerateDto();
                    mqttFramerateDto.setProducing(String.valueOf(MainSingleton.getInstance().FPS_PRODUCER));
                    mqttFramerateDto.setConsuming(String.valueOf(MainSingleton.getInstance().FPS_CONSUMER));
                    mqttFramerateDto.setEffect(MainSingleton.getInstance().config.getEffect());
                    mqttFramerateDto.setColorMode(String.valueOf(Enums.ColorMode.values()[MainSingleton.getInstance().config.getColorMode() - 1].getBaseI18n()));
                    mqttFramerateDto.setAspectRatio(MainSingleton.getInstance().config.isAutoDetectBlackBars() ?
                            CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS) : MainSingleton.getInstance().config.getDefaultLedMatrix());
                    mqttFramerateDto.setGamma(String.valueOf(MainSingleton.getInstance().config.getGamma()));
                    mqttFramerateDto.setSmoothingLvl((Enums.Ema.findByValue(MainSingleton.getInstance().config.getEmaAlpha()).getBaseI18n()));
                    mqttFramerateDto.setFrameGen((Enums.FrameGeneration.findByValue(MainSingleton.getInstance().config.getFrameInsertionTarget()).getBaseI18n()));
                    mqttFramerateDto.setProfile(Constants.DEFAULT.equals(MainSingleton.getInstance().profileArg) ?
                            CommonUtility.getWord(Constants.DEFAULT) : MainSingleton.getInstance().profileArg);
                    NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_FRAMERATE),
                            CommonUtility.toJsonString(mqttFramerateDto));
                }
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Ping device
     */
    public void pingDevice() {
        if (MainSingleton.getInstance().config.isFullFirmware() && log.isDebugEnabled()) {
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            Runnable framerateTask = () -> {
                if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getDeviceIP() != null
                        && NetworkManager.isValidIp(CommonUtility.getDeviceToUse().getDeviceIP())) {
                    List<String> pingCmd = new ArrayList<>(Arrays.stream(NativeExecutor.isWindows() ? Constants.PING_WINDOWS : Constants.PING_LINUX).toList());
                    pingCmd.add(CommonUtility.getDeviceToUse().getDeviceIP());
                    NativeExecutor.runNative(pingCmd.toArray(String[]::new), 4000);
                }
            };
            scheduledExecutorService.scheduleAtFixedRate(framerateTask, 0, 5, TimeUnit.SECONDS);
        }
    }

    /**
     * Small benchmark to check if Glow Worm Luciferin firmware can keep up with Firefly Luciferin PC software
     *
     * @param framerateAlert number of times Firefly was faster than Glow Worm
     * @param notified       don't alert user more than one time
     */
    public void runBenchmark(AtomicInteger framerateAlert, AtomicBoolean notified) {
        int benchIteration = Constants.NUMBER_OF_BENCHMARK_ITERATION;
        // Wayland has a more swinging frame rate due to the fact that it doesn't capture an image if frame is still, give it some more room for error.
        if (NativeExecutor.isWayland()) {
            benchIteration = Constants.NUMBER_OF_BENCHMARK_ITERATION * 4;
        }
        if (!notified.get()) {
            if ((MainSingleton.getInstance().FPS_PRODUCER > 0) && (framerateAlert.get() < benchIteration)
                    && (MainSingleton.getInstance().FPS_GW_CONSUMER < MainSingleton.getInstance().FPS_PRODUCER - Constants.BENCHMARK_ERROR_MARGIN)) {
                framerateAlert.getAndIncrement();
            } else {
                framerateAlert.set(0);
            }
            int iterationNumber;
            if (MainSingleton.getInstance().config.isMultiScreenSingleDevice()) {
                iterationNumber = benchIteration;
            } else {
                iterationNumber = benchIteration / 2;
            }
            if (MainSingleton.getInstance().FPS_GW_CONSUMER == 0 && framerateAlert.get() == iterationNumber && MainSingleton.getInstance().config.isFullFirmware()) {
                log.info("Glow Worm Luciferin is not responding, restarting...");
                NativeExecutor.restartNativeInstance();
            }
            if (MainSingleton.getInstance().FPS_GW_CONSUMER == 0 && framerateAlert.get() == 1 && MainSingleton.getInstance().config.isFullFirmware() && !MainSingleton.getInstance().config.isMultiScreenSingleDevice()) {
                if (MainSingleton.getInstance().guiManager.pipelineManager.scheduledExecutorService.isShutdown()) {
                    log.info("Reconnecting with the device...");
                    MainSingleton.getInstance().guiManager.pipelineManager.startWiFiMqttManagedPipeline();
                }
            }
            if (framerateAlert.get() == benchIteration && !notified.get() && MainSingleton.getInstance().FPS_GW_CONSUMER > 0) {
                notified.set(true);
                javafx.application.Platform.runLater(() -> {
                    int suggestedFramerate = getSuggestedFramerate();
                    log.error("{}. {}", CommonUtility.getWord(Constants.FRAMERATE_HEADER), CommonUtility.getWord(Constants.FRAMERATE_CONTEXT)
                            .replace("{0}", String.valueOf(suggestedFramerate)));
                    if (MainSingleton.getInstance().config.isSyncCheck() && (MainSingleton.getInstance().config.getSmoothingType().equals(Enums.Smoothing.DISABLED.getBaseI18n()) || MainSingleton.getInstance().config.getFrameInsertionTarget() == 0)) {
                        Optional<ButtonType> result = MainSingleton.getInstance().guiManager.showAlert(CommonUtility.getWord(Constants.FRAMERATE_TITLE), CommonUtility.getWord(Constants.FRAMERATE_HEADER),
                                CommonUtility.getWord(Constants.FRAMERATE_CONTEXT).replace("{0}", String.valueOf(suggestedFramerate)), Alert.AlertType.CONFIRMATION);
                        ButtonType button = result.orElse(ButtonType.OK);
                        if (button == ButtonType.OK) {
                            try {
                                StorageManager sm = new StorageManager();
                                MainSingleton.getInstance().config.setDesiredFramerate(String.valueOf(suggestedFramerate));
                                sm.writeConfig(MainSingleton.getInstance().config, null);
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
