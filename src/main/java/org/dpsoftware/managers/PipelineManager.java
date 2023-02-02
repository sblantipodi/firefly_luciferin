/*
  PipelineManager.java

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
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.audio.AudioLoopback;
import org.dpsoftware.audio.AudioLoopbackNative;
import org.dpsoftware.audio.AudioLoopbackSoftware;
import org.dpsoftware.audio.AudioUtility;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.TrayIconManager;
import org.dpsoftware.gui.controllers.DevicesTabController;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.dto.AudioDevice;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.managers.dto.UnsubscribeInstanceDto;
import org.dpsoftware.network.MessageClient;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manage high performance pipeline for screen grabbing
 */
@Slf4j
public class PipelineManager {

    public static boolean pipelineStarting = false;
    public static boolean pipelineStopping = false;
    public static String lastEffectInUse = "";
    UpgradeManager upgradeManager = new UpgradeManager();
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * Calculate correct Pipeline for Linux
     *
     * @return params for Linux Pipeline
     */
    public static String getLinuxPipelineParams() {
        // startx{0}, endx{1}, starty{2}, endy{3}
        DisplayManager displayManager = new DisplayManager();
        List<DisplayInfo> displayList = displayManager.getDisplayList();
        DisplayInfo monitorInfo = displayList.get(FireflyLuciferin.config.getMonitorNumber());
        String gstreamerPipeline = Constants.GSTREAMER_PIPELINE_LINUX
                .replace("{0}", String.valueOf((int) (monitorInfo.getMinX() + 1)))
                .replace("{1}", String.valueOf((int) (monitorInfo.getMinX() + monitorInfo.getWidth() - 1)))
                .replace("{2}", String.valueOf((int) (monitorInfo.getMinY())))
                .replace("{3}", String.valueOf((int) (monitorInfo.getMinY() + monitorInfo.getHeight() - 1)));
        log.debug(gstreamerPipeline);
        return gstreamerPipeline;
    }

    /**
     * Message offered to the queue is sent to the LED strip, if multi screen single instance, is sent via TCP Socket to the main instance
     *
     * @param leds colors to be sent to the LED strip
     */
    public static void offerToTheQueue(Color[] leds) {
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            if (MessageClient.msgClient == null || MessageClient.msgClient.clientSocket == null) {
                MessageClient.msgClient = new MessageClient();
                if (CommonUtility.isSingleDeviceMultiScreen()) {
                    MessageClient.msgClient.startConnection(Constants.MSG_SERVER_HOST, Constants.MSG_SERVER_PORT);
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append(JavaFXStarter.whoAmI).append(",");
            for (Color color : leds) {
                sb.append(color.getRGB()).append(",");
            }
            MessageClient.msgClient.sendMessage(sb.toString());
        } else {
            //noinspection ResultOfMethodCallIgnored
            FireflyLuciferin.sharedQueue.offer(leds);
        }
    }

    /**
     * Start high performance pipeline, MQTT or Serial managed (FULL or LIGHT firmware)
     */
    public void startCapturePipeline() {
        PipelineManager.pipelineStarting = true;
        PipelineManager.pipelineStopping = false;
        if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
            initAudioCapture();
        }
        if ((NetworkManager.client != null) || FireflyLuciferin.config.isFullFirmware()) {
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
        Constants.Effect effectInUse = LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect());
        Constants.Effect lastEffectInUseFromConfig = LocalizedEnum.fromBaseStr(Constants.Effect.class, lastEffectInUse);
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
        if (startAudioCapture) {
            Map<String, AudioDevice> loopbackDevices = audioLoopback.getLoopbackDevices();
            // if there is no native audio loopback (example stereo mix), fallback to software audio loopback using WASAPI
            if (loopbackDevices != null && !loopbackDevices.isEmpty()
                    && FireflyLuciferin.config.getAudioDevice().equals(Constants.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n())) {
                log.debug("Starting native audio loopback.");
                audioLoopback.startVolumeLevelMeter();
            } else {
                audioLoopback = new AudioLoopbackSoftware();
                loopbackDevices = audioLoopback.getLoopbackDevices();
                if (loopbackDevices != null && !loopbackDevices.isEmpty()) {
                    log.debug("Starting software audio loopback.");
                    audioLoopback.startVolumeLevelMeter();
                }
            }
        } else {
            audioLoopback.stopVolumeLevelMeter();
        }
    }

    /**
     * Start high performance Serial pipeline, LIGHT firmware required
     */
    private void startSerialManagedPipeline() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable framerateTask = () -> {
            // Waiting Device to Use
            GlowWormDevice glowWormDeviceSerial = CommonUtility.getDeviceToUse();
            // Check if the connected device match the minimum firmware version requirements for this Firefly Luciferin version
            Boolean firmwareMatchMinRequirements = upgradeManager.firmwareMatchMinimumRequirements();
            if (CommonUtility.isSingleDeviceOtherInstance() || firmwareMatchMinRequirements != null) {
                if (CommonUtility.isSingleDeviceOtherInstance() || firmwareMatchMinRequirements) {
                    setRunning();
                    if (FireflyLuciferin.guiManager.trayIconManager.getTrayIcon() != null) {
                        FireflyLuciferin.guiManager.trayIconManager.setTrayIconImage(Constants.PlayerStatus.PLAY);
                    }
                } else {
                    stopForFirmwareUpgrade(glowWormDeviceSerial);
                }
            } else {
                log.debug("Waiting device for my instance...");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Start high performance WiFi/MQTT pipeline, FULL firmware required
     */
    private void startWiFiMqttManagedPipeline() {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        AtomicInteger retryNumber = new AtomicInteger();
        Runnable framerateTask = () -> {
            // Waiting Device to Use
            GlowWormDevice glowWormDeviceToUse = CommonUtility.getDeviceToUse();
            // Check if the connected device match the minimum firmware version requirements for this Firefly Luciferin version
            Boolean firmwareMatchMinRequirements = (JavaFXStarter.whoAmI == 1 || !CommonUtility.isSingleDeviceMultiScreen()) ? upgradeManager.firmwareMatchMinimumRequirements() : null;
            if (CommonUtility.isSingleDeviceOtherInstance() || firmwareMatchMinRequirements != null) {
                if (CommonUtility.isSingleDeviceOtherInstance() || Boolean.TRUE.equals(firmwareMatchMinRequirements)) {
                    setRunning();
                    NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.ASPECT_RATIO_TOPIC), FireflyLuciferin.config.getDefaultLedMatrix());
                    if (FireflyLuciferin.guiManager.trayIconManager.getTrayIcon() != null) {
                        FireflyLuciferin.guiManager.trayIconManager.setTrayIconImage(Constants.PlayerStatus.PLAY);
                    }
                    StateDto stateDto = new StateDto();
                    stateDto.setState(Constants.ON);
                    stateDto.setBrightness(CommonUtility.getNightBrightness());
                    stateDto.setWhitetemp(FireflyLuciferin.config.getWhiteTemperature());
                    stateDto.setMAC(glowWormDeviceToUse.getMac());
                    turnOnLEDs(stateDto);
                    if ((FireflyLuciferin.config.isFullFirmware() && FireflyLuciferin.config.isWirelessStream())) {
                        // If multi display change stream topic
                        if (retryNumber.getAndIncrement() < 5 && FireflyLuciferin.config.getMultiMonitor() > 1 && !CommonUtility.isSingleDeviceMultiScreen()) {
                            NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.UNSUBSCRIBE_STREAM_TOPIC),
                                    CommonUtility.toJsonString(new UnsubscribeInstanceDto(String.valueOf(JavaFXStarter.whoAmI), FireflyLuciferin.config.getOutputDevice())));
                            CommonUtility.sleepSeconds(1);
                        } else {
                            retryNumber.set(0);
                            stateDto.setEffect(Constants.STATE_ON_GLOWWORMWIFI);
                            NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.DEFAULT_MQTT_TOPIC), CommonUtility.toJsonString(stateDto));
                        }
                    } else {
                        stateDto.setEffect(Constants.STATE_ON_GLOWWORM);
                        NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.DEFAULT_MQTT_TOPIC), CommonUtility.toJsonString(stateDto));
                    }
                    if (FireflyLuciferin.FPS_GW_CONSUMER > 0 || !FireflyLuciferin.RUNNING) {
                        scheduledExecutorService.shutdown();
                    }
                } else {
                    stopForFirmwareUpgrade(glowWormDeviceToUse);
                }
            } else {
                log.debug("Waiting device for my instance...");
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
        FireflyLuciferin.RUNNING = true;
        FireflyLuciferin.config.setToggleLed(true);
        Constants.Effect effect = LocalizedEnum.fromBaseStr(Constants.Effect.class, lastEffectInUse);
        if (Constants.Effect.MUSIC_MODE_VU_METER.equals(effect)
                || Constants.Effect.MUSIC_MODE_VU_METER_DUAL.equals(effect)
                || Constants.Effect.MUSIC_MODE_BRIGHT.equals(effect)
                || Constants.Effect.MUSIC_MODE_RAINBOW.equals(effect)) {
            FireflyLuciferin.config.setEffect(lastEffectInUse);
        } else if (!lastEffectInUse.isEmpty()) {
            FireflyLuciferin.config.setEffect(Constants.Effect.BIAS_LIGHT.getBaseI18n());
        }
    }

    /**
     * Stop capturing pipeline, firmware on the running device is too old
     *
     * @param glowWormDeviceToUse Glow Worm device selected in use on the current Firfly Luciferin instance
     */
    private void stopForFirmwareUpgrade(GlowWormDevice glowWormDeviceToUse) {
        PipelineManager.pipelineStarting = false;
        PipelineManager.pipelineStopping = false;
        DevicesTabController.oldFirmwareDevice = true;
        log.error(CommonUtility.getWord(Constants.MIN_FIRMWARE_NOT_MATCH), glowWormDeviceToUse.getDeviceName(), glowWormDeviceToUse.getDeviceVersion());
        scheduledExecutorService.shutdown();
        if (FireflyLuciferin.guiManager.trayIconManager.getTrayIcon() != null) {
            FireflyLuciferin.guiManager.trayIconManager.setTrayIconImage(Constants.PlayerStatus.GREY);
        }
    }

    /**
     * Stop high performance pipeline
     */
    public void stopCapturePipeline() {
        PipelineManager.pipelineStarting = false;
        PipelineManager.pipelineStopping = true;
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
        AudioLoopback audioLoopback = new AudioLoopback();
        audioLoopback.stopVolumeLevelMeter();
        if (FireflyLuciferin.guiManager.trayIconManager.getTrayIcon() != null) {
            FireflyLuciferin.guiManager.trayIconManager.setTrayIconImage(Constants.PlayerStatus.STOP);
            TrayIconManager.popupMenu.remove(0);
            TrayIconManager.popupMenu.add(FireflyLuciferin.guiManager.trayIconManager.createMenuItem(CommonUtility.getWord(Constants.START)), 0);
        }
        if (FireflyLuciferin.pipe != null && ((FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name()))
                || (FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name()))
                || (FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.AVFVIDEOSRC.name())))) {
            FireflyLuciferin.pipe.stop();
        }
        FireflyLuciferin.FPS_PRODUCER_COUNTER = 0;
        FireflyLuciferin.FPS_CONSUMER_COUNTER = 0;
        FireflyLuciferin.FPS_CONSUMER = 0;
        FireflyLuciferin.FPS_PRODUCER = 0;
        FireflyLuciferin.RUNNING = false;
        AudioLoopback.RUNNING_AUDIO = false;
        Constants.Effect effectInUse = LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect());
        switch (effectInUse) {
            case BIAS_LIGHT, MUSIC_MODE_VU_METER, MUSIC_MODE_VU_METER_DUAL, MUSIC_MODE_BRIGHT, MUSIC_MODE_RAINBOW ->
                    lastEffectInUse = FireflyLuciferin.config.getEffect();
        }
        AudioLoopback.AUDIO_BRIGHTNESS = 255;
        FireflyLuciferin.config.setEffect(Constants.Effect.SOLID.getBaseI18n());
    }
}
