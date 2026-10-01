/*
  GrabberManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

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

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.controllers.SettingsController;
import org.dpsoftware.managers.*;
import org.dpsoftware.managers.dto.MqttFramerateDto;
import org.dpsoftware.utilities.CaptureDeviceUtilities;
import org.dpsoftware.utilities.CommonUtility;
import org.freedesktop.gstreamer.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Screen grabbing manager
 */
@Slf4j
public class GrabberManager {

    public Bin bin;
    public GStreamerGrabber vc;
    private boolean linuxPingUnavailable = false;
    private String linuxPipelineParams;
    private PipelineManager.XdgStreamDetails xdgStreamDetails;
    private static final long CAPTURE_PROBE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(10);
    private CaptureMethodSelection captureSelection;
    private long captureProbeStarted;
    private long captureRetryAfter;
    private String probeUnavailableSource;
    private ScheduledExecutorService linuxCaptureExecutor;
    private ScheduledFuture<?> captureTask;
    private final AtomicBoolean captureProbeFailed = new AtomicBoolean();

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
     */
    @SuppressWarnings("all") // The scheduled executor lives until shutdownCaptureScheduler().
    public void launchAdvancedGrabber() {
        shutdownCaptureScheduler();
        MainSingleton main = MainSingleton.getInstance();
        AtomicInteger restartCounter = new AtomicInteger();
        ImageProcessor.initGStreamerLibraryPaths();
        // WebRTC support in gst1-java is available from GStreamer 1.14. Supplying the minimum version here is also necessary for the binding's
        Gst.init(Version.of(1, 14), Constants.SCREEN_GRABBER, "");
        AtomicInteger pipelineRetry = new AtomicInteger();
        if (main.getConfig().isAutomaticCapturePending()) {
            boolean usb = main.getConfig().hasCaptureDevice() || main.isHeadlessMode();
            captureSelection = new CaptureMethodSelection(NativeExecutor.isWindows(), NativeExecutor.isMac(),
                    NativeExecutor.isWayland(), usb);
        }
        AtomicReference<String> missingSource = new AtomicReference<>();
        AtomicInteger missingSourceTicks = new AtomicInteger();
        // Linux portal and native state changes can block; keep the GStreamer bus executor free.
        ScheduledExecutorService captureExecutor;
        if (NativeExecutor.isLinux()) {
            linuxCaptureExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "capture-controller");
                thread.setDaemon(true);
                return thread;
            });
            captureExecutor = linuxCaptureExecutor;
        } else {
            captureExecutor = Gst.getExecutor();
        }
        captureTask = captureExecutor.scheduleAtFixedRate(() -> runCaptureTask(() ->
                checkCapturePipeline(main, restartCounter, pipelineRetry, missingSource, missingSourceTicks)),
                1, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancel the capture task, close the Linux executor owned by this manager,
     * and stop the current grabber's interpolation worker.
     */
    public void shutdownCaptureScheduler() {
        if (captureTask != null) {
            captureTask.cancel(false);
            captureTask = null;
        }
        if (linuxCaptureExecutor != null) {
            linuxCaptureExecutor.shutdownNow();
            linuxCaptureExecutor = null;
        }
        stopFrameGeneration();
    }

    /**
     * Stop frame generation on the current video grabber, if one exists.
     */
    public void stopFrameGeneration() {
        if (vc != null) {
            vc.stopFrameGeneration();
        }
    }

    /**
     * Check capture progress and source availability, then start or restart the pipeline when needed.
     *
     * @param main current application state and capture configuration
     * @param restartCounter number of consecutive pipeline restarts
     * @param pipelineRetry number of watchdog ticks without produced frames
     * @param missingSource last unavailable source, used to avoid repeated alerts
     * @param missingSourceTicks watchdog ticks while the source is unavailable
     */
    private void checkCapturePipeline(MainSingleton main, AtomicInteger restartCounter, AtomicInteger pipelineRetry,
                                      AtomicReference<String> missingSource, AtomicInteger missingSourceTicks) {
        if (captureSelection != null) {
            probeCaptureMethod(restartCounter, main);
            return;
        }
        if (!ManagerSingleton.getInstance().pipelineStopping && main.RUNNING && main.FPS_PRODUCER_COUNTER == 0) {
            pipelineRetry.getAndIncrement();
            boolean pipeNull = GrabberSingleton.getInstance().pipe == null;
            boolean notPlaying = !pipeNull && !GrabberSingleton.getInstance().pipe.isPlaying();
            boolean tooManyRetries = pipelineRetry.get() >= 2;
            log.debug("Watchdog tick #{}: pipeNull={}, notPlaying={}, tooManyRetries={}", pipelineRetry.get(), pipeNull, notPlaying, tooManyRetries);
            if (pipeNull || notPlaying || tooManyRetries) {
                // Device discovery can be slow: retry every three seconds while the source is missing.
                if (missingSource.get() != null && missingSourceTicks.incrementAndGet() < 6) {
                    disposePipeline();
                    return;
                }
                missingSourceTicks.set(0);
                String unavailableSource = getUnavailableCaptureSource();
                if (unavailableSource != null) {
                    if (GrabberSingleton.getInstance().pipe != null) {
                        GrabberSingleton.getInstance().pipe.stop();
                    }
                    pipelineRetry.set(0);
                    if (!unavailableSource.equals(missingSource.getAndSet(unavailableSource))) {
                        log.warn("Capture source unavailable; pipeline not started: {}", unavailableSource);
                        if (!main.isHeadlessMode() && main.guiManager != null) {
                            Platform.runLater(() -> main.guiManager.showAlert(Constants.SCREEN_GRABBER,
                                    CommonUtility.getWord(LabelKey.CAPTURE_SOURCE_UNAVAILABLE),
                                    CommonUtility.getWord(LabelKey.CAPTURE_SOURCE_UNAVAILABLE_CONTEXT).replace("{0}", unavailableSource),
                                    Alert.AlertType.WARNING));
                        }
                    }
                    disposePipeline();
                    return;
                }
                if (missingSource.getAndSet(null) != null) {
                    log.info("Capture source available again; starting pipeline");
                }
                restartCapturePipeline(main, restartCounter,
                        pipeNull ? "pipeNull" : (notPlaying ? "notPlaying" : "tooManyRetries"));
            }
        } else {
            pipelineRetry.set(0);
        }
        disposePipeline();
    }

    /**
     * Start or restart capture, attach the video sink and play the pipeline.
     *
     * @param main current application state and capture configuration
     * @param restartCounter number of consecutive pipeline restarts
     * @param reason watchdog condition that triggered the restart
     */
    private void restartCapturePipeline(MainSingleton main, AtomicInteger restartCounter, String reason) {
        if (GrabberSingleton.getInstance().pipe != null) {
            log.info("Restarting pipeline (reason={})", reason);
            GrabberSingleton.getInstance().pipe.stop();
            restartCounter.getAndIncrement();
            if (restartCounter.get() >= Constants.MAX_PIPELINE_RESTARTS) {
                log.error("Pipeline restarted too many times, restarting...");
                NativeExecutor.restartNativeInstanceWithCurrentProfile();
            }
        } else {
            startPipeline(restartCounter, main);
        }
        vc = new GStreamerGrabber();
        GrabberSingleton.getInstance().pipe.addMany(bin, vc.getElement());
        Pipeline.linkMany(bin, vc.getElement());
        if (!MainSingleton.getInstance().isHeadlessMode()) {
            JFrame f = new JFrame(Constants.SCREEN_GRABBER);
            JPanel panel = new JPanel();
            panel.setPreferredSize(new Dimension(main.getConfig().getScreenResX(), main.getConfig().getScreenResY()));
            panel.setBackground(Color.BLACK);
            f.add(panel);
            f.pack();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            GrabberSingleton.getInstance().pipe.play();
            f.setVisible(false);
        } else {
            GrabberSingleton.getInstance().pipe.play();
        }
    }

    /**
     * Keep the periodic capture task alive even when startup or cleanup fails.
     *
     * @param task capture check to execute on this scheduler tick
     */
    void runCaptureTask(Runnable task) {
        if (System.nanoTime() < captureRetryAfter) {
            return;
        }
        try {
            task.run();
        } catch (RuntimeException e) {
            captureRetryAfter = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            if (e instanceof PipelineManager.MissingCaptureElementException missing) {
                log.warn("GStreamer element {} unavailable; retrying capture in three seconds", missing.getElement());
            } else {
                log.warn("Capture pipeline unavailable; retrying in three seconds", e);
            }
            try {
                releaseProbePipeline();
            } catch (RuntimeException cleanupFailure) {
                log.warn("Cannot release failed capture pipeline", cleanupFailure);
            }
        }
    }

    /**
     * Try the configured backends until a real video frame reaches the appsink.
     *
     * @param restartCounter number of consecutive pipeline restarts
     * @param main current application state and capture configuration
     */
    private void probeCaptureMethod(AtomicInteger restartCounter, MainSingleton main) {
        if (!main.RUNNING || ManagerSingleton.getInstance().pipelineStopping) {
            return;
        }
        try {
            if (captureProbeStarted == 0) {
                main.getConfig().setCaptureMethod(captureSelection.current().name());
                linuxPipelineParams = null;
                String unavailableSource = getUnavailableCaptureSource();
                if (unavailableSource != null) {
                    if (!unavailableSource.equals(probeUnavailableSource)) {
                        log.warn("Waiting for capture source: {}", unavailableSource);
                    }
                    probeUnavailableSource = unavailableSource;
                    captureRetryAfter = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                    return;
                }
                probeUnavailableSource = null;
                log.info("Trying capture method {}", captureSelection.current());
                startPipeline(restartCounter, main);
                captureProbeFailed.set(false);
                GrabberSingleton.getInstance().pipe.getBus().connect((Bus.ERROR) (_, code, message) -> {
                    log.warn("Capture probe failed: {} ({})", message, code);
                    captureProbeFailed.set(true);
                });
                vc = new GStreamerGrabber();
                GrabberSingleton.getInstance().pipe.addMany(bin, vc.getElement());
                if (!Pipeline.linkMany(bin, vc.getElement())) {
                    throw new IllegalStateException("Cannot link capture pipeline to video sink");
                }
                if (GrabberSingleton.getInstance().pipe.play() == StateChangeReturn.FAILURE) {
                    throw new IllegalStateException("GStreamer could not start the capture pipeline");
                }
                captureProbeStarted = System.nanoTime();
            } else if (vc != null && vc.isFrameReceived()) {
                finishCaptureSelection(main, false);
            } else if (captureProbeFailed.get()
                    || System.nanoTime() - captureProbeStarted >= CAPTURE_PROBE_TIMEOUT_NANOS) {
                failCaptureProbe(main);
            }
        } catch (RuntimeException e) {
            if (e instanceof PipelineManager.MissingCaptureElementException missing) {
                log.warn("Capture method {} unavailable: missing GStreamer element {}",
                        main.getConfig().getCaptureMethod(), missing.getElement());
            } else {
                log.warn("Capture method {} unavailable", main.getConfig().getCaptureMethod(), e);
            }
            failCaptureProbe(main);
        }
    }

    /**
     * Release the failed candidate and advance to the next capture method.
     *
     * @param main current application state whose capture method may change
     */
    private void failCaptureProbe(MainSingleton main) {
        releaseProbePipeline();
        captureProbeStarted = 0;
        if (!captureSelection.advance()) {
            finishCaptureSelection(main, true);
        }
    }

    /**
     * Persist the selected capture method and clear the pending automatic selection.
     *
     * @param main current application state whose configuration is saved
     * @param fallback true when every candidate failed and the final method is retained
     */
    private void finishCaptureSelection(MainSingleton main, boolean fallback) {
        Configuration config = main.getConfig();
        config.setCaptureMethod(captureSelection.current().name());
        config.setAutomaticCapturePending(false);
        captureSelection = null;
        log.info("{} capture method {}", fallback ? "Using compatibility fallback" : "Selected", config.getCaptureMethod());
        try {
            new StorageManager().writeConfig(config, null);
        } catch (IOException e) {
            log.error("Cannot save selected capture method", e);
        }
        if (!main.isHeadlessMode() && main.guiManager != null) {
            main.guiManager.refreshAutomaticCaptureMethod();
        }
    }

    /**
     * Stop and dispose the pipeline, bin and video sink of the current probe.
     */
    private void releaseProbePipeline() {
        Pipeline pipeline = GrabberSingleton.getInstance().pipe;
        GrabberSingleton.getInstance().pipe = null;
        if (pipeline != null) {
            try {
                pipeline.stop();
            } catch (RuntimeException e) {
                log.warn("Cannot stop failed capture probe", e);
            } finally {
                pipeline.dispose();
            }
        }
        if (bin != null) {
            bin.dispose();
            bin = null;
        }
        if (vc != null) {
            vc.stopFrameGeneration();
            vc.videosink.dispose();
            vc.getElement().dispose();
            vc = null;
        }
        GStreamerGrabber.ledMatrix = null;
    }

    /**
     * Initializes and starts a new pipeline for screen grabbing, resetting the restart counter
     * and configuring the pipeline based on the operating system and specified parameters.
     *
     * @param restartCounter   An AtomicInteger used to track the number of restarts for the pipeline.
     * @param main             The MainSingleton instance containing shared configuration and state data.
     */
    private void startPipeline(AtomicInteger restartCounter, MainSingleton main) {
        String description;
        if (NativeExecutor.isWindows()) {
            String friendlyName = main.getConfig().getCaptureDeviceFriendlyName();
            if (main.getConfig().getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX11.name())) {
                String monitorNativePeer = String.valueOf(new DisplayManager().getDisplayInfo(main.getConfig().getMonitorNumber()).getNativePeer());
                description = PipelineManager.getPipeline(Constants.GSTREAMER_PIPELINE_WINDOWS_HARDWARE_HANDLE_DX11)
                        .replace("{0}", monitorNativePeer);
            } else if (main.getConfig().getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX12.name())) {
                String monitorNativePeer = String.valueOf(new DisplayManager().getDisplayInfo(main.getConfig().getMonitorNumber()).getNativePeer());
                description = PipelineManager.getPipeline(Constants.GSTREAMER_PIPELINE_WINDOWS_HARDWARE_HANDLE_DX12)
                        .replace("{0}", monitorNativePeer);
            } else {
                description = PipelineManager.setUsbVideoPipelineParams(PipelineManager.getPipeline(Constants.GSTREAMER_PIPELINE_WINDOWS_EXT_SRC))
                        .replace("{0}", friendlyName);
            }
        } else if (NativeExecutor.isLinux()) {
            if (linuxPipelineParams == null) {
                linuxPipelineParams = PipelineManager.getLinuxPipelineParams(() -> {
                    if (xdgStreamDetails == null) {
                        xdgStreamDetails = PipelineManager.getXdgStreamDetails();
                    }
                    return xdgStreamDetails;
                });
            }
            String devPath = main.getConfig().hasCaptureDevice() ? main.getConfig().getCaptureDevice().getDevPath() : "";
            int keepAliveTime = Math.max(1, (1000 / GStreamerGrabber.getTargetFramerate()) / 2);
            description = linuxPipelineParams
                    .replace(Constants.PIPEWIRE_KEEPALIVE, String.valueOf(keepAliveTime))
                    .replace(Constants.FPS_PLACEHOLDER, String.valueOf(GStreamerGrabber.getTargetFramerate()));
            if (!devPath.isEmpty()) {
                description = description.replace("{0}", devPath);
            }
        } else {
            description = PipelineManager.getPipeline(Constants.GSTREAMER_PIPELINE_MAC);
        }
        // Linux checks its complete element list before requesting the Wayland portal.
        if (!NativeExecutor.isLinux() && org.dpsoftware.config.EnvConstants.CUSTOM_GSTREAMER_PIPELINE == null) {
            PipelineManager.requireCaptureElements(description);
        }
        log.info("Starting a new pipeline");
        restartCounter.set(0);
        Pipeline pipeline = new Pipeline();
        try {
            bin = Gst.parseBinFromDescription(description, true);
            GrabberSingleton.getInstance().pipe = pipeline;
        } catch (RuntimeException e) {
            pipeline.dispose();
            throw e;
        }
    }

    /**
     * Returns a description of the missing configured source, or null when it is available.
     */
    private String getUnavailableCaptureSource() {
        Configuration config = MainSingleton.getInstance().config;
        String method = config.getCaptureMethod();
        boolean usb = Configuration.CaptureMethod.valueOf(method).isUsb();
        if (usb) {
            var selected = config.getCaptureDevice();
            if (selected == null) {
                return CommonUtility.getWord(LabelKey.CAPTURE_SOURCE_USB_UNCONFIGURED);
            }
            String identity = NativeExecutor.isWindows() ? selected.getFriendlyName() : selected.getDevPath();
            if (identity == null || identity.isBlank()) {
                return CommonUtility.getWord(LabelKey.CAPTURE_SOURCE_USB_UNCONFIGURED);
            }
            boolean present = CaptureDeviceUtilities.discover().stream().anyMatch(device ->
                    NativeExecutor.isWindows() ? identity.equalsIgnoreCase(device.getFriendlyName())
                            : identity.equals(device.getDevPath()));
            return present ? null : CommonUtility.getWord(LabelKey.CAPTURE_SOURCE_USB).replace("{0}", identity);
        }
        boolean monitor = Configuration.CaptureMethod.DDUPL_DX11.name().equals(method)
                || Configuration.CaptureMethod.DDUPL_DX12.name().equals(method)
                || Configuration.CaptureMethod.XIMAGESRC.name().equals(method)
                || Configuration.CaptureMethod.XIMAGESRC_NVIDIA.name().equals(method);
        if (monitor) {
            int index = config.getMonitorNumber();
            if (index < 0 || new DisplayManager().getDisplayInfo(index) == null) {
                return CommonUtility.getWord(LabelKey.CAPTURE_SOURCE_MONITOR).replace("{0}", String.valueOf(index));
            }
        }
        return null;
    }

    /**
     * Old pipeline is not needed anymore, dispose the pipeline and all the related objects to free up system memory.
     */
    private void disposePipeline() {
        if (GrabberSingleton.getInstance().pipe != null && !GrabberSingleton.getInstance().pipe.isPlaying() && !ManagerSingleton.getInstance().pipelineStarting) {
            stopFrameGeneration();
            log.info("Dispose pipeline: releasing bin and pipeline (this clears lastRgbBuffer)");
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
                log.info(CommonUtility.getWord(LabelKey.SPAWNING_ROBOTS));
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
     * It creates background tasks that runs every 5 seconds.
     * - Calculate Screen Capture Framerate and how fast your microcontroller can consume it.
     * - Check if HDR is ON to enable dynamic gamma calculation.
     */
    public void createBackgroundTasks() {
        AtomicInteger framerateAlert = new AtomicInteger();
        AtomicBoolean notified = new AtomicBoolean(false);
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable framerateTask = () -> {
            boolean isHdrActive = NativeExecutor.isHdrActive();
            if (isHdrActive != MainSingleton.getInstance().isHdrActive()) {
                MainSingleton.getInstance().setHdrActive(isHdrActive);
                log.info("HDR is {}", MainSingleton.getInstance().isHdrActive() ? "ON" : "OFF");
            }
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
                            CommonUtility.getWord(LabelKey.AUTO_DETECT_BLACK_BARS) : MainSingleton.getInstance().config.getDefaultLedMatrix());
                    mqttFramerateDto.setGamma(String.valueOf(MainSingleton.getInstance().config.getGamma()));
                    String adaptiveGamma = String.format("%.3f", Double.longBitsToDouble(ImageProcessor.currentGammaAtomic.get()));
                    if (NativeExecutor.isWindows())
                        adaptiveGamma += " (" + (MainSingleton.getInstance().hdrActive ? Constants.HDR : Constants.SDR) + ")";
                    mqttFramerateDto.setAdaptiveGamma(adaptiveGamma);
                    mqttFramerateDto.setSmoothingLvl((Enums.Ema.findByValue(MainSingleton.getInstance().config.getEmaAlpha()).getBaseI18n()));
                    mqttFramerateDto.setFrameGen((Enums.FrameGeneration.findByValue(MainSingleton.getInstance().config.getFrameInsertionTarget()).getBaseI18n()));
                    mqttFramerateDto.setProfile(LabelKey.DEFAULT.equals(MainSingleton.getInstance().profileArg) ?
                            CommonUtility.getWord(LabelKey.DEFAULT) : MainSingleton.getInstance().profileArg);
                    mqttFramerateDto.setCubeLut(MainSingleton.getInstance().config.getCubeLut());
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
                    String deviceIp = CommonUtility.getDeviceToUse().getDeviceIP();
                    if (NativeExecutor.isWindows() || !linuxPingUnavailable) {
                        List<String> pingCmd = new ArrayList<>(Arrays.stream(NativeExecutor.isWindows() ? Constants.PING_WINDOWS : Constants.PING_LINUX).toList());
                        pingCmd.add(deviceIp);
                        List<String> pingResult = NativeExecutor.runNative(pingCmd.toArray(String[]::new), 4000);
                        if (NativeExecutor.isWindows() || !pingResult.isEmpty()) {
                            return;
                        }
                        linuxPingUnavailable = true;
                        log.debug("Linux ping command not available, using curl HEAD fallback.");
                    }
                    List<String> curlCmd = new ArrayList<>(Arrays.stream(Constants.CURL_HEAD_LINUX).toList());
                    curlCmd.add(Constants.HTTP + deviceIp);
                    NativeExecutor.runNative(curlCmd.toArray(String[]::new), 4000);
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
            benchIteration = Constants.NUMBER_OF_BENCHMARK_ITERATION * 10;
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
                    log.error("{}. {}", CommonUtility.getWord(LabelKey.FRAMERATE_HEADER), CommonUtility.getWord(LabelKey.FRAMERATE_CONTEXT)
                            .replace("{0}", String.valueOf(suggestedFramerate)));
                    if (MainSingleton.getInstance().config.isSyncCheck() && (MainSingleton.getInstance().config.getSmoothingType().equals(Enums.Smoothing.DISABLED.getBaseI18n()) || MainSingleton.getInstance().config.getFrameInsertionTarget() == 0)) {
                        Optional<ButtonType> result = MainSingleton.getInstance().guiManager.showAlert(CommonUtility.getWord(LabelKey.FRAMERATE_TITLE), CommonUtility.getWord(LabelKey.FRAMERATE_HEADER),
                                CommonUtility.getWord(LabelKey.FRAMERATE_CONTEXT).replace("{0}", String.valueOf(suggestedFramerate)), Alert.AlertType.CONFIRMATION);
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
