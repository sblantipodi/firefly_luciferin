/*
  PipelineManager.java

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
package org.dpsoftware.managers;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.*;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.DbusScreenCast;
import org.dpsoftware.grabber.GrabberSingleton;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.managers.dto.AudioDevice;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.managers.dto.UnsubscribeInstanceDto;
import org.dpsoftware.network.MessageClient;
import org.dpsoftware.network.NetworkSingleton;
import org.dpsoftware.utilities.CommonUtility;
import org.freedesktop.dbus.DBusMatchRule;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manage high performance pipeline for screen grabbing
 */
@Slf4j
public class PipelineManager {

    public ScheduledExecutorService scheduledExecutorService;
    UpgradeManager upgradeManager = new UpgradeManager();

    /**
     * Uses D-BUS to get the XDG ScreenCast stream ID & pipewire filedescriptor
     *
     * @return XDG ScreenCast stream details containing the ID from org.freedesktop.portal.ScreenCast:Start and
     * FileDescriptor from org.freedesktop.portal.ScreenCast:OpenPipeWireRemote
     * @throws RuntimeException on any concurrency or D-BUS issues
     */
    @SuppressWarnings("all")
    public static XdgStreamDetails getXdgStreamDetails() {
        try {
            AtomicBoolean restoreTokenMatch = new AtomicBoolean(false);
            AtomicBoolean alertShown = new AtomicBoolean(false);
            CompletableFuture<Void> sourcesSelectedMaybe = new CompletableFuture<>();
            CompletableFuture<String> sessionHandleMaybe = new CompletableFuture<>();
            CompletableFuture<Integer> streamIdMaybe = new CompletableFuture<>();
            DBusConnection dBusConnection = DBusConnectionBuilder.forSessionBus().build(); // cannot free/close this for the duration of the capture
            DbusScreenCast screenCastIface = dBusConnection.getRemoteObject("org.freedesktop.portal.Desktop", "/org/freedesktop/portal/desktop", DbusScreenCast.class);
            String handleToken = (Constants.FIREFLY_LUCIFERIN + MainSingleton.getInstance().whoAmI).replaceAll("-", "").replaceAll(" ", "");
            DBusMatchRule matchRule = new DBusMatchRule("signal", "org.freedesktop.portal.Request", "Response");
            dBusConnection.addGenericSigHandler(matchRule, signal -> {
                try {
                    if (signal.getParameters().length == 2 // verify amount of arguments
                            && signal.getParameters()[0] instanceof UInt32 // verify argument types
                            && signal.getParameters()[1] instanceof LinkedHashMap
                            && ((UInt32) signal.getParameters()[0]).intValue() == 0 // verify success-code
                    ) {
                        var parameters = (LinkedHashMap<?, ?>) signal.getParameters()[1];

                        // parse signal & set appropriate Future as the result
                        if (parameters.isEmpty()) {
                            sourcesSelectedMaybe.complete(null);
                        } else if (parameters.containsKey("session_handle")) {
                            var sessionHandle = (Variant<?>) parameters.get("session_handle");
                            sessionHandleMaybe.complete((String) sessionHandle.getValue());
                        } else if (parameters.containsKey("streams")) {
                            var restoreTokenVariant = (Variant<?>) parameters.get("restore_token");

                            if (restoreTokenVariant != null) {
                                restoreTokenMatch.set(true);
                                String restoreToken = (String) restoreTokenVariant.getValue();

                                try {
                                    if (!restoreToken.equals(MainSingleton.getInstance().config.getScreenCastRestoreToken())) {
                                        MainSingleton.getInstance().config.setScreenCastRestoreToken(restoreToken);
                                        StorageManager storageManager = new StorageManager();
                                        storageManager.writeConfig(MainSingleton.getInstance().config, null);
                                    }
                                } catch (IOException e) {
                                    log.error("Can't write config file.", e);
                                }
                            }

                            // Extract stream ID
                            var streamsVariant = (Variant<?>) parameters.get("streams");
                            var streams = (List<?>) streamsVariant.getValue();
                            var streamData = (Object[]) streams.get(0);
                            var streamId = (UInt32) streamData[0];
                            streamIdMaybe.complete(streamId.intValue());
                        }
                    }
                } catch (DBusException e) {
                    throw new RuntimeException(e); // couldn't parse, fail early
                }
            });
            screenCastIface.CreateSession(Map.of("session_handle_token", new Variant<>(handleToken)));
            DBusPath receivedSessionHandle = new DBusPath(sessionHandleMaybe.get());
            String restoreToken = MainSingleton.getInstance().config.getScreenCastRestoreToken();
            Map<String, Variant<?>> selectSourcesMap = new HashMap<>() {{
                put("multiple", new Variant<>(false));
                put("types", new Variant<>(new UInt32(1 | 2))); // bitmask, 1 - screens, 2 - windows
                put("persist_mode", new Variant<>(new UInt32(2)));
            }};
            if (restoreToken != null && !restoreToken.isEmpty()) {
                selectSourcesMap.put("restore_token", new Variant<>(restoreToken));
            }

            screenCastIface.SelectSources(receivedSessionHandle, selectSourcesMap);
            sourcesSelectedMaybe.get(); // Wait until source selection is ready

            if (NativeExecutor.isWayland() && (MainSingleton.getInstance().config.getScreenCastRestoreToken() == null || MainSingleton.getInstance().config.getScreenCastRestoreToken().isEmpty())) {
                showChooseDisplayAlert();
                alertShown.set(true);
            }

            screenCastIface.Start(receivedSessionHandle, "", Collections.emptyMap());

            var c = streamIdMaybe.thenApply(streamId -> {
                FileDescriptor fileDescriptor = screenCastIface.OpenPipeWireRemote(receivedSessionHandle, Collections.emptyMap()); // block until stream started before calling OpenPipeWireRemote
                return new XdgStreamDetails(streamId, fileDescriptor);
            }).get();
            return c;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Show an alert that inform the user to select the appropriate screen for recording
     */
    private static void showChooseDisplayAlert() {
        DisplayManager displayManager = new DisplayManager();
        if (displayManager.displayNumber() > 1) {
            String displayName = displayManager.getDisplayName(MainSingleton.getInstance().whoAmI - 1);
            MainSingleton.getInstance().guiManager.showAlert(Constants.FIREFLY_LUCIFERIN, CommonUtility.getWord(Constants.WAYLAND_SCREEN_REC_PERMISSION).replace("{0}", displayName),
                    CommonUtility.getWord(Constants.WAYLAND_SCREEN_REC_PERMISSION_CONTEXT).replace("{0}", displayName), Alert.AlertType.INFORMATION);
        }
    }

    /**
     * Calculate correct Pipeline for Linux
     *
     * @return params for Linux Pipeline
     */
    public static String getLinuxPipelineParams() {
        String gstreamerPipeline;
        String pipeline;
        if (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.PIPEWIREXDG.name())
                || MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.PIPEWIREXDG_NVIDIA.name())) {
            if (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.PIPEWIREXDG.name())) {
                pipeline = Constants.GSTREAMER_PIPELINE_PIPEWIREXDG;
            } else {
                pipeline = Constants.GSTREAMER_PIPELINE_PIPEWIREXDG_CUDA;
            }
            XdgStreamDetails xdgStreamDetails = getXdgStreamDetails();
            assert xdgStreamDetails != null;
            gstreamerPipeline = pipeline
                    .replace("{1}", String.valueOf(xdgStreamDetails.fileDescriptor.getIntFileDescriptor()))
                    .replace("{2}", xdgStreamDetails.streamId.toString());
        } else {
            // startx{0}, endx{1}, starty{2}, endy{3}
            if (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name())) {
                pipeline = Constants.GSTREAMER_PIPELINE_XIMAGESRC;
            } else {
                pipeline = Constants.GSTREAMER_PIPELINE_XIMAGESRC_CUDA;
            }
            DisplayManager displayManager = new DisplayManager();
            List<DisplayInfo> displayList = displayManager.getDisplayList();
            DisplayInfo monitorInfo = displayList.get(MainSingleton.getInstance().config.getMonitorNumber());
            gstreamerPipeline = pipeline
                    .replace("{0}", String.valueOf((int) (monitorInfo.getMinX() + 1)))
                    .replace("{1}", String.valueOf((int) (monitorInfo.getMinX() + monitorInfo.getWidth() - 1)))
                    .replace("{2}", String.valueOf((int) (monitorInfo.getMinY())))
                    .replace("{3}", String.valueOf((int) (monitorInfo.getMinY() + monitorInfo.getHeight() - 1)));
        }
        log.info(gstreamerPipeline);
        return gstreamerPipeline;
    }

    /**
     * Message offered to the queue is sent to the LED strip, if multi screen single instance, is sent via TCP Socket to the main instance
     *
     * @param leds colors to be sent to the LED strip
     */
    public static void offerToTheQueue(Color[] leds) {
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            if (NetworkSingleton.getInstance().msgClient == null || NetworkSingleton.getInstance().msgClient.clientSocket == null) {
                NetworkSingleton.getInstance().msgClient = new MessageClient();
                if (CommonUtility.isSingleDeviceMultiScreen()) {
                    NetworkSingleton.getInstance().msgClient.startConnection(Constants.MSG_SERVER_HOST, Constants.MSG_SERVER_PORT);
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append(MainSingleton.getInstance().whoAmI).append(",");
            for (Color color : leds) {
                sb.append(color.getRGB()).append(",");
            }
            NetworkSingleton.getInstance().msgClient.sendMessage(sb.toString());
        } else {
            ImageProcessor.exponentialMovingAverage(leds);
            ImageProcessor.adjustStripWhiteBalance(leds);
            //noinspection ResultOfMethodCallIgnored
            MainSingleton.getInstance().sharedQueue.offer(leds);
        }
    }

    /**
     * Search if all the configured satellites are engaged
     *
     * @return boolean if all satellites are engaged
     */
    public static boolean isSatellitesEngaged() {
        boolean result = true;
        for (Map.Entry<String, Satellite> sat : MainSingleton.getInstance().config.getSatellites().entrySet()) {
            result = GuiSingleton.getInstance().deviceTableData.stream().anyMatch(e -> e.getDeviceIP().equals(sat.getKey()));
            if (!result) break;
        }
        return result;
    }

    /**
     * Callback used to restart the capture pipeline. It acceps a method as input.
     *
     * @param command callback to execute during the restart process
     */
    public static void restartCapture(Runnable command) {
        restartCapture(command, false);
    }

    /**
     * Callback used to restart the capture pipeline. It acceps a method as input.
     *
     * @param command      callback to execute during the restart process
     * @param pipelineOnly if true, restarts the capturing pipeline but does not send the STOP signal to the firmware
     */
    public static void restartCapture(Runnable command, boolean pipelineOnly) {
        Platform.runLater(() -> {
            command.run();
            if (pipelineOnly) {
                MainSingleton.getInstance().guiManager.stopPipeline();
            } else {
                MainSingleton.getInstance().guiManager.stopCapturingThreads(MainSingleton.getInstance().RUNNING);
            }
            CommonUtility.delaySeconds(() -> MainSingleton.getInstance().guiManager.startCapturingThreads(), Constants.TIME_TO_RESTART_CAPTURE);
        });
    }

    /**
     * Start high performance pipeline, MQTT or Serial managed (FULL or LIGHT firmware)
     */
    public void startCapturePipeline() {
        ManagerSingleton.getInstance().pipelineStarting = true;
        ManagerSingleton.getInstance().pipelineStopping = false;
        if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
            initAudioCapture();
        }
        if ((ManagerSingleton.getInstance().client != null) || MainSingleton.getInstance().config.isFullFirmware()) {
            startWiFiMqttManagedPipeline();
        } else {
            startSerialManagedPipeline();
        }
    }

    /**
     * Initialize audio loopback, software or native based on the OS availability
     */
    void initAudioCapture() {
        AudioUtility audioLoopback;
        audioLoopback = new AudioLoopbackNative();
        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
        Enums.Effect lastEffectInUseFromConfig = LocalizedEnum.fromBaseStr(Enums.Effect.class, ManagerSingleton.getInstance().lastEffectInUse);
        boolean startAudioCapture = isStartAudioCapture(effectInUse, lastEffectInUseFromConfig);
        if (startAudioCapture) {
            Map<String, AudioDevice> loopbackDevices = audioLoopback.getLoopbackDevices();
            // if there is no native audio loopback (example stereo mix), fallback to software audio loopback using WASAPI
            if (loopbackDevices != null && !loopbackDevices.isEmpty()
                    && MainSingleton.getInstance().config.getAudioDevice().equals(Enums.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n())) {
                log.info("Starting native audio loopback.");
                audioLoopback.startVolumeLevelMeter();
            } else {
                audioLoopback = new AudioLoopbackSoftware();
                loopbackDevices = audioLoopback.getLoopbackDevices();
                if (loopbackDevices != null && !loopbackDevices.isEmpty()) {
                    log.info("Starting software audio loopback.");
                    audioLoopback.startVolumeLevelMeter();
                }
            }
        } else {
            audioLoopback.stopVolumeLevelMeter();
        }
    }

    /**
     * Check is audio capturing is needed
     *
     * @param effectInUse               name of the effect to use
     * @param lastEffectInUseFromConfig previous effect in use
     * @return true if audio capture is needed
     */
    private boolean isStartAudioCapture(Enums.Effect effectInUse, Enums.Effect lastEffectInUseFromConfig) {
        boolean startAudioCapture = false;
        switch (effectInUse) {
            case MUSIC_MODE_VU_METER, MUSIC_MODE_VU_METER_DUAL, MUSIC_MODE_BRIGHT, MUSIC_MODE_RAINBOW ->
                    startAudioCapture = true;
        }
        if (lastEffectInUseFromConfig != null) {
            switch (lastEffectInUseFromConfig) {
                case MUSIC_MODE_VU_METER, MUSIC_MODE_VU_METER_DUAL, MUSIC_MODE_BRIGHT, MUSIC_MODE_RAINBOW ->
                        startAudioCapture = true;
            }
        }
        return startAudioCapture;
    }

    /**
     * Start high performance Serial pipeline, LIGHT firmware required
     */
    private void startSerialManagedPipeline() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable framerateTask = () -> {
            // Waiting Device to Use, check if the connected device match the minimum firmware version requirements for this Firefly Luciferin version
            Boolean firmwareMatchMinRequirements = upgradeManager.firmwareMatchMinimumRequirements();
            if (CommonUtility.isSingleDeviceOtherInstance() || firmwareMatchMinRequirements != null) {
                if (CommonUtility.isSingleDeviceOtherInstance() || firmwareMatchMinRequirements) {
                    setRunning();
                    MainSingleton.getInstance().guiManager.trayIconManager.setTrayIconImage(Enums.PlayerStatus.PLAY);
                } else {
                    stopForFirmwareUpgrade();
                }
            } else {
                log.info("Waiting serial device for my instance...");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Start high performance WiFi/MQTT pipeline, FULL firmware required
     */
    public void startWiFiMqttManagedPipeline() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        AtomicInteger retryNumber = new AtomicInteger();
        Runnable framerateTask = () -> {
            // Waiting Device to Use
            GlowWormDevice glowWormDeviceToUse = CommonUtility.getDeviceToUse();
            // Check if the connected device match the minimum firmware version requirements for this Firefly Luciferin version
            Boolean firmwareMatchMinRequirements = (MainSingleton.getInstance().whoAmI == 1 || !CommonUtility.isSingleDeviceMultiScreen()) ? upgradeManager.firmwareMatchMinimumRequirements() : null;
            if ((MainSingleton.getInstance().config.getSatellites() != null && GuiSingleton.getInstance().deviceTableData != null)
                    && (((MainSingleton.getInstance().config.getSatellites().isEmpty()) || isSatellitesEngaged())
                    && (CommonUtility.isSingleDeviceOtherInstance() || firmwareMatchMinRequirements != null))) {
                if (CommonUtility.isSingleDeviceOtherInstance() || Boolean.TRUE.equals(firmwareMatchMinRequirements)) {
                    setRunning();
                    NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_ASPECT_RATIO), MainSingleton.getInstance().config.getDefaultLedMatrix());
                    MainSingleton.getInstance().guiManager.trayIconManager.setTrayIconImage(Enums.PlayerStatus.PLAY);
                    StateDto stateDto = new StateDto();
                    stateDto.setState(Constants.ON);
                    stateDto.setBrightness(CommonUtility.getNightBrightness());
                    stateDto.setWhitetemp(MainSingleton.getInstance().config.getWhiteTemperature());
                    stateDto.setMAC(glowWormDeviceToUse.getMac());
                    turnOnLEDs(stateDto);
                    if ((MainSingleton.getInstance().config.isFullFirmware() && MainSingleton.getInstance().config.isWirelessStream())) {
                        // If multi display change stream topic
                        if (retryNumber.getAndIncrement() < 5 && MainSingleton.getInstance().config.getMultiMonitor() > 1 && !CommonUtility.isSingleDeviceMultiScreen()) {
                            NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_UNSUBSCRIBE_STREAM),
                                    CommonUtility.toJsonString(new UnsubscribeInstanceDto(String.valueOf(MainSingleton.getInstance().whoAmI), MainSingleton.getInstance().config.getOutputDevice())));
                            CommonUtility.sleepSeconds(1);
                        } else {
                            retryNumber.set(0);
                            stateDto.setEffect(Constants.STATE_ON_GLOWWORMWIFI);
                            stateDto.setFfeffect(LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect()).getBaseI18n());
                            NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT), CommonUtility.toJsonString(stateDto));
                        }
                    } else {
                        stateDto.setEffect(Constants.STATE_ON_GLOWWORM);
                        NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT), CommonUtility.toJsonString(stateDto));
                    }
                    if (MainSingleton.getInstance().FPS_GW_CONSUMER > 0 || !MainSingleton.getInstance().RUNNING) {
                        scheduledExecutorService.shutdown();
                    }
                } else {
                    stopForFirmwareUpgrade();
                }
            } else {
                log.info("Waiting device for my instance...");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Turn ON LEDs if LEDs has been turned off by an external sources like Home Assistant
     *
     * @param stateDto status to send to the microcontroller
     */
    private void turnOnLEDs(StateDto stateDto) {
        StorageManager sm = new StorageManager();
        // This config is not modified by external sources
        Configuration currentConfig = sm.readProfileInUseConfig();
        String[] currentColor = currentConfig.getColorChooser().split(",");
        if (Integer.parseInt(currentColor[0]) == 0 && Integer.parseInt(currentColor[1]) == 0 && Integer.parseInt(currentColor[2]) == 0) {
            stateDto.setColor(new ColorDto(255, 255, 255));
        } else {
            stateDto.setColor(new ColorDto(Integer.parseInt(currentColor[0]), Integer.parseInt(currentColor[1]), Integer.parseInt(currentColor[2])));
        }
    }

    /**
     * Set running pipeline
     */
    private void setRunning() {
        MainSingleton.getInstance().RUNNING = true;
        MainSingleton.getInstance().config.setToggleLed(true);
        Enums.Effect effect = LocalizedEnum.fromBaseStr(Enums.Effect.class, ManagerSingleton.getInstance().lastEffectInUse);
        if (Enums.Effect.MUSIC_MODE_VU_METER.equals(effect)
                || Enums.Effect.MUSIC_MODE_VU_METER_DUAL.equals(effect)
                || Enums.Effect.MUSIC_MODE_BRIGHT.equals(effect)
                || Enums.Effect.MUSIC_MODE_RAINBOW.equals(effect)) {
            MainSingleton.getInstance().config.setEffect(ManagerSingleton.getInstance().lastEffectInUse);
        } else if (!ManagerSingleton.getInstance().lastEffectInUse.isEmpty()) {
            MainSingleton.getInstance().config.setEffect(Enums.Effect.BIAS_LIGHT.getBaseI18n());
        }
    }

    /**
     * Stop capturing pipeline, firmware on the running device is too old
     */
    private void stopForFirmwareUpgrade() {
        ManagerSingleton.getInstance().pipelineStarting = false;
        ManagerSingleton.getInstance().pipelineStopping = false;
        GuiSingleton.getInstance().oldFirmwareDevice = true;
        for (GlowWormDevice gwd : CommonUtility.getDeviceToUseWithSatellites()) {
            if (Boolean.FALSE.equals(UpgradeManager.checkFirmwareVersion(gwd))) {
                log.warn(CommonUtility.getWord(Constants.MIN_FIRMWARE_NOT_MATCH), gwd.getDeviceName(), gwd.getDeviceVersion());
            }
        }
        scheduledExecutorService.shutdown();
        MainSingleton.getInstance().guiManager.trayIconManager.setTrayIconImage(Enums.PlayerStatus.GREY);
    }

    /**
     * Stop high performance pipeline
     */
    public void stopCapturePipeline() {
        ManagerSingleton.getInstance().pipelineStarting = false;
        ManagerSingleton.getInstance().pipelineStopping = true;
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
        AudioLoopback audioLoopback = new AudioLoopback();
        audioLoopback.stopVolumeLevelMeter();
        MainSingleton.getInstance().guiManager.trayIconManager.setTrayIconImage(Enums.PlayerStatus.STOP);
        if (GrabberSingleton.getInstance().pipe != null && ((MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX11.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX12.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.PIPEWIREXDG.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.AVFVIDEOSRC.name())))) {
            GrabberSingleton.getInstance().pipe.stop();
        }
        MainSingleton.getInstance().FPS_PRODUCER_COUNTER = 0;
        MainSingleton.getInstance().FPS_CONSUMER_COUNTER = 0;
        MainSingleton.getInstance().FPS_CONSUMER = 0;
        MainSingleton.getInstance().FPS_PRODUCER = 0;
        MainSingleton.getInstance().RUNNING = false;
        AudioSingleton.getInstance().RUNNING_AUDIO = false;
        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
        switch (effectInUse) {
            case BIAS_LIGHT, MUSIC_MODE_VU_METER, MUSIC_MODE_VU_METER_DUAL, MUSIC_MODE_BRIGHT, MUSIC_MODE_RAINBOW ->
                    ManagerSingleton.getInstance().lastEffectInUse = MainSingleton.getInstance().config.getEffect();
        }
        AudioSingleton.getInstance().AUDIO_BRIGHTNESS = 255;
        MainSingleton.getInstance().config.setEffect(Enums.Effect.SOLID.getBaseI18n());
    }

    record XdgStreamDetails(Integer streamId, FileDescriptor fileDescriptor) {
    }

    /**
     * Manage gaming profile, if the GPU usage is high, switch to gaming profile.
     * If the GPU usage is low, switch back to default profile.
     */
    public static void manageGamingProfile() {
        StorageManager sm = new StorageManager();
        if (sm.listProfilesForThisInstance().contains(Constants.GAMING_PROFILE)) {
            ScheduledExecutorService gpuService = Executors.newScheduledThreadPool(1);
            AtomicInteger triggerCnt = new AtomicInteger();
            Runnable gpuTask = () -> {
                int repeatedTrigger = 2;
                Double gpuUsage = NativeExecutor.getGpuUsage();
                if (!MainSingleton.getInstance().profileArgs.equals(Constants.GAMING_PROFILE)) {
                    if (gpuUsage > Constants.GAMING_GPU_USAGE_TRIGGER) {
                        log.info("High GPU usage detected, switching to gaming profile: {}%", gpuUsage);
                        triggerCnt.getAndIncrement();
                        if (triggerCnt.get() >= repeatedTrigger) {
                            NativeExecutor.restartNativeInstance(Constants.GAMING_PROFILE);
                        }
                    } else {
                        triggerCnt.set(0);
                    }
                }
                if (MainSingleton.getInstance().profileArgs.equals(Constants.GAMING_PROFILE)) {
                    if (gpuUsage <= Constants.GAMING_GPU_USAGE_TRIGGER) {
                        log.info("Low GPU usage detected, switching to default profile: {}%", gpuUsage);
                        triggerCnt.getAndIncrement();
                        if (triggerCnt.get() >= repeatedTrigger) {
                            NativeExecutor.restartNativeInstance();
                        }
                    } else {
                        triggerCnt.set(0);
                    }
                }
            };
            gpuService.scheduleAtFixedRate(gpuTask, 10, 10, TimeUnit.SECONDS);
        }
    }

}
