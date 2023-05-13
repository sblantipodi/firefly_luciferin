/*
  NetworkManager.java

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.dto.GammaDto;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.network.tcpUdp.TcpClient;
import org.dpsoftware.utilities.CommonUtility;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.awt.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class controls the MQTT traffic
 */
@Slf4j
public class NetworkManager implements MqttCallback {

    public static MqttClient client;
    public boolean connected = false;
    String mqttDeviceName;
    Date lastActivity;

    /**
     * Constructor
     *
     * @param showErrorIfAny error notification after 3 attemps
     * @param retryCounter   number of attemps while trying to connect
     */
    public NetworkManager(boolean showErrorIfAny, AtomicInteger retryCounter) {
        try {
            lastActivity = new Date();
            attemptReconnect();
        } catch (MqttException | RuntimeException e) {
            connected = false;
            if (showErrorIfAny && retryCounter.get() == 3) {
                if (NativeExecutor.isWindows()) {
                    FireflyLuciferin.guiManager.showLocalizedNotification(Constants.MQTT_ERROR_TITLE, Constants.MQTT_ERROR_CONTEXT, TrayIcon.MessageType.ERROR);
                } else {
                    FireflyLuciferin.guiManager.showLocalizedAlert(Constants.MQTT_ERROR_TITLE, Constants.MQTT_ERROR_HEADER, Constants.MQTT_ERROR_CONTEXT, Alert.AlertType.ERROR);
                }
            }
            log.error("Can't connect to the MQTT Server");
        }
    }

    /**
     * Publish to a topic
     *
     * @param topic where to publish the message
     * @param msg   msg for the queue
     * @return TCP response if it's converted in an HTTP request
     */
    public static TcpResponse publishToTopic(String topic, String msg) {
        return publishToTopic(topic, msg, false);
    }

    /**
     * Publish to a topic
     *
     * @param topic            where to publish the message
     * @param msg              msg for the queue
     * @param forceHttpRequest force HTTP request even if MQTT is enabled
     * @param retainMsg        set if the msg must be retained by the MQTT broker
     * @param qos              quality of service, 0 and 1 are supported, 2 is not supported. 0 is default.
     * @return TCP response if it's converted in an HTTP request
     */
    public static TcpResponse publishToTopic(String topic, String msg, boolean forceHttpRequest, boolean retainMsg, int qos) {
        if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
            if (FireflyLuciferin.config.isMqttEnable() && !forceHttpRequest && client != null) {
                MqttMessage message = new MqttMessage();
                message.setPayload(msg.getBytes());
                message.setRetained(retainMsg);
                message.setQos(qos);
                log.trace("Topic=" + topic + "\n" + msg);
                try {
                    client.publish(topic, message);
                } catch (MqttException e) {
                    log.error(Constants.MQTT_CANT_SEND);
                }
            } else {
                if (!topic.contains("firelyluciferin")) {
                    return TcpClient.httpGet(msg, topic);
                }
            }
        }
        return null;
    }

    /**
     * Publish to a topic
     *
     * @param topic            where to publish the message
     * @param msg              msg for the queue
     * @param forceHttpRequest force HTTP request even if MQTT is enabled
     * @return TCP response if it's converted in an HTTP request
     */
    public static TcpResponse publishToTopic(String topic, String msg, boolean forceHttpRequest) {
        return publishToTopic(topic, msg, forceHttpRequest, false, 0);
    }

    /**
     * Stream messages to the stream topic
     *
     * @param msg msg for the queue
     */
    public static void stream(String msg) {
        try {
            // If multi display change stream topic
            if (FireflyLuciferin.config.getMultiMonitor() > 1 && !CommonUtility.isSingleDeviceMultiScreen()) {
                client.publish(getTopic(Constants.DEFAULT_MQTT_TOPIC) + Constants.MQTT_STREAM_TOPIC + JavaFXStarter.whoAmI, msg.getBytes(), 0, false);
            } else {
                client.publish(getTopic(Constants.DEFAULT_MQTT_TOPIC) + Constants.MQTT_STREAM_TOPIC, msg.getBytes(), 0, false);
            }
        } catch (MqttException e) {
            log.error(Constants.MQTT_CANT_SEND);
        }
    }

    /**
     * Manage default MQTT topic
     *
     * @param message mqtt message
     * @throws JsonProcessingException something went wrong during JSON processing
     */
    private static void manageDefaultTopic(MqttMessage message) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode mqttmsg = mapper.readTree(new String(message.getPayload()));
        if (mqttmsg.get(Constants.STATE) != null && mqttmsg.get(Constants.MQTT_TOPIC) != null) {
            if (mqttmsg.get(Constants.MQTT_TOPIC) != null) {
                if (mqttmsg.get(Constants.STATE).asText().equals(Constants.ON) && mqttmsg.get(Constants.EFFECT).asText().equals(Constants.SOLID)) {
                    FireflyLuciferin.config.setToggleLed(true);
                    String brightnessToSet;
                    if (mqttmsg.get(Constants.COLOR) != null) {
                        if (FireflyLuciferin.nightMode) {
                            brightnessToSet = String.valueOf(FireflyLuciferin.config.getBrightness());
                        } else {
                            brightnessToSet = String.valueOf(mqttmsg.get(Constants.MQTT_BRIGHTNESS));
                        }
                        FireflyLuciferin.config.setColorChooser(mqttmsg.get(Constants.COLOR).get("r") + "," + mqttmsg.get(Constants.COLOR).get("g") + ","
                                + mqttmsg.get(Constants.COLOR).get("b") + "," + brightnessToSet);
                    }
                }
                CommonUtility.updateFpsWithDeviceTopic(mqttmsg);
            }
        } else if (mqttmsg.get(Constants.START_STOP_INSTANCES) != null && mqttmsg.get(Constants.START_STOP_INSTANCES).asText().equals(Enums.PlayerStatus.STOP.name())) {
            FireflyLuciferin.guiManager.stopCapturingThreads(false);
        } else if (mqttmsg.get(Constants.START_STOP_INSTANCES) != null && mqttmsg.get(Constants.START_STOP_INSTANCES).asText().equals(Enums.PlayerStatus.PLAY.name())) {
            FireflyLuciferin.guiManager.startCapturingThreads();
        } else if (mqttmsg.get(Constants.STATE) != null) {
            manageFpsTopic(message);
        }
        // Skip retained message, we want fresh data here
        if (!message.isRetained()) {
            if (mqttmsg.get(Constants.MQTT_DEVICE_NAME) != null) {
                CommonUtility.updateDeviceTable(mqttmsg);
            }
        }
    }

    /**
     * Manage MQTT set topic
     *
     * @param message mqtt message
     * @throws JsonProcessingException something went wrong during JSON processing
     */
    private static void manageMqttSetTopic(MqttMessage message) throws JsonProcessingException {
        if (message.toString().contains(Constants.MQTT_START)) {
            FireflyLuciferin.guiManager.startCapturingThreads();
        } else if (message.toString().contains(Constants.MQTT_STOP)) {
            ObjectMapper gammaMapper = new ObjectMapper();
            JsonNode macObj = gammaMapper.readTree(new String(message.getPayload()));
            if (macObj.get(Constants.MAC) != null) {
                String mac = macObj.get(Constants.MAC).asText();
                if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac().equals(mac)) {
                    FireflyLuciferin.guiManager.pipelineManager.stopCapturePipeline();
                }
            }
        }
    }

    /**
     * Manage FPS topic
     *
     * @param message mqtt message
     * @throws JsonProcessingException something went wrong during JSON processing
     */
    private static void manageFpsTopic(MqttMessage message) throws JsonProcessingException {
        ObjectMapper mapperFps = new ObjectMapper();
        JsonNode mqttmsg = mapperFps.readTree(new String(message.getPayload()));
        CommonUtility.updateFpsWithFpsTopic(mqttmsg);
    }

    /**
     * Return an MQTT topic using the configuration file, this is used to construct HTTP url too.
     *
     * @param command MQTT command
     * @return MQTT topic
     */
    public static String getTopic(String command) {
        String topic = null;
        String gwBaseTopic = Constants.MQTT_BASE_TOPIC;
        String fireflyBaseTopic = Constants.MQTT_FIREFLY_BASE_TOPIC;
        String defaultTopic = FireflyLuciferin.config.getMqttTopic();
        if (!FireflyLuciferin.config.isMqttEnable()) {
            defaultTopic = Constants.MQTT_BASE_TOPIC;
        }
        String defaultFireflyTopic = fireflyBaseTopic + "_" + FireflyLuciferin.config.getMqttTopic();
        if (Constants.DEFAULT_MQTT_TOPIC.equals(FireflyLuciferin.config.getMqttTopic())
                || gwBaseTopic.equals(FireflyLuciferin.config.getMqttTopic())) {
            defaultTopic = gwBaseTopic;
            defaultFireflyTopic = fireflyBaseTopic;
        }
        switch (command) {
            case Constants.DEFAULT_MQTT_TOPIC ->
                    topic = Constants.DEFAULT_MQTT_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.DEFAULT_MQTT_STATE_TOPIC ->
                    topic = Constants.DEFAULT_MQTT_STATE_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.UPDATE_MQTT_TOPIC -> topic = Constants.UPDATE_MQTT_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.UPDATE_RESULT_MQTT_TOPIC ->
                    topic = Constants.UPDATE_RESULT_MQTT_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.FIREFLY_LUCIFERIN_FRAMERATE ->
                    topic = Constants.FIREFLY_LUCIFERIN_FRAMERATE.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.FIREFLY_LUCIFERIN_GAMMA ->
                    topic = Constants.FIREFLY_LUCIFERIN_GAMMA.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.ASPECT_RATIO_TOPIC ->
                    topic = Constants.ASPECT_RATIO_TOPIC.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.SET_ASPECT_RATIO_TOPIC ->
                    topic = Constants.SET_ASPECT_RATIO_TOPIC.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.SET_SMOOTHING_TOPIC ->
                    topic = Constants.SET_SMOOTHING_TOPIC.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.SMOOTHING_TOPIC ->
                    topic = Constants.SMOOTHING_TOPIC.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.FIREFLY_LUCIFERIN_EFFECT_TOPIC ->
                    topic = Constants.FIREFLY_LUCIFERIN_EFFECT_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.GLOW_WORM_FIRM_CONFIG_TOPIC -> topic = Constants.GLOW_WORM_FIRM_CONFIG_TOPIC;
            case Constants.UNSUBSCRIBE_STREAM_TOPIC ->
                    topic = Constants.UNSUBSCRIBE_STREAM_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.LDR_TOPIC -> topic = Constants.LDR_TOPIC.replace(gwBaseTopic, defaultTopic);
        }
        return topic;
    }

    /**
     * Check if current topic differs from main topic, this is needed when upgrading instances that uses different topics
     *
     * @return true if current topic is different from main topic
     */
    public static boolean currentTopicDiffersFromMainTopic() {
        if (JavaFXStarter.whoAmI != 1 && FireflyLuciferin.config.isMqttEnable()) {
            StorageManager sm = new StorageManager();
            Configuration mainConfig = sm.readMainConfig();
            return !FireflyLuciferin.config.getMqttTopic().equals(mainConfig.getMqttTopic());
        }
        return false;
    }

    /**
     * Manage aspect ratio topic
     *
     * @param message mqtt message
     */
    private void manageAspectRatio(MqttMessage message) {
        FireflyLuciferin.guiManager.trayIconManager.manageAspectRatioListener(message.toString(), false);
    }

    /**
     * Manage smoothing topic
     *
     * @param message mqtt message
     */
    private void manageSmoothing(MqttMessage message) {
        if (FireflyLuciferin.RUNNING) {
            Platform.runLater(() -> {
                FireflyLuciferin.config.setFrameInsertion(LocalizedEnum.fromBaseStr(Enums.FrameInsertion.class, message.toString()).getBaseI18n());
                FireflyLuciferin.guiManager.stopCapturingThreads(FireflyLuciferin.RUNNING);
                CommonUtility.delaySeconds(() -> FireflyLuciferin.guiManager.startCapturingThreads(), 4);
            });
            if (FireflyLuciferin.config.isMqttEnable()) {
                CommonUtility.delaySeconds(() -> NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.SMOOTHING_TOPIC), message.toString()), 1);
            }
        }
    }

    /**
     * Manage effect topic
     *
     * @param message message
     */
    private void manageEffect(String message) {
        if (FireflyLuciferin.config != null) {
            CommonUtility.delayMilliseconds(() -> {
                log.info("Setting mode via MQTT - " + message);
                setEffect(message);
            }, 200);
        }
    }

    /**
     * Set effect
     */
    private void setEffect(String message) {
        String previousEffect = FireflyLuciferin.config.getEffect();
        FireflyLuciferin.config.setEffect(message);
        CommonUtility.sleepMilliseconds(200);
        if ((Enums.Effect.BIAS_LIGHT.getBaseI18n().equals(message)
                || Enums.Effect.MUSIC_MODE_VU_METER.getBaseI18n().equals(message)
                || Enums.Effect.MUSIC_MODE_VU_METER_DUAL.getBaseI18n().equals(message)
                || Enums.Effect.MUSIC_MODE_BRIGHT.getBaseI18n().equals(message)
                || Enums.Effect.MUSIC_MODE_RAINBOW.getBaseI18n().equals(message))) {
            if (!FireflyLuciferin.RUNNING) {
                FireflyLuciferin.guiManager.startCapturingThreads();
            } else {
                if (!previousEffect.equals(message)) {
                    FireflyLuciferin.guiManager.stopCapturingThreads(true);
                    CommonUtility.sleepSeconds(1);
                    FireflyLuciferin.guiManager.startCapturingThreads();
                }
            }
        } else {
            if (FireflyLuciferin.RUNNING) {
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
                FireflyLuciferin.config.setToggleLed(!message.contains(Constants.OFF));
                CommonUtility.turnOnLEDs();
            }
        }
    }

    /**
     * Manage gamma
     *
     * @param message mqtt message
     * @throws JsonProcessingException something went wrong during JSON processing
     */
    private void manageGamma(MqttMessage message) throws JsonProcessingException {
        ObjectMapper gammaMapper = new ObjectMapper();
        JsonNode gammaObj = gammaMapper.readTree(new String(message.getPayload()));
        if (gammaObj.get(Constants.MQTT_GAMMA) != null) {
            FireflyLuciferin.config.setGamma(Double.parseDouble(gammaObj.get(Constants.MQTT_GAMMA).asText()));
        }
    }

    /**
     * Show update notification/alert and restart the screen capture
     *
     * @param message mqtt message
     */
    private void showUpdateNotification(MqttMessage message) {
        if (UpgradeManager.deviceNameForSerialDevice.equals(message.toString())) {
            log.info("Update successfull=" + message);
            if (!CommonUtility.isSingleDeviceMultiScreen() || CommonUtility.isSingleDeviceMainInstance()) {
                javafx.application.Platform.runLater(() -> {
                    if (NativeExecutor.isWindows()) {
                        FireflyLuciferin.guiManager.showNotification(CommonUtility.getWord(Constants.UPGRADE_SUCCESS),
                                message + " " + CommonUtility.getWord(Constants.DEVICEUPGRADE_SUCCESS), TrayIcon.MessageType.INFO);
                    } else {
                        FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN, CommonUtility.getWord(Constants.UPGRADE_SUCCESS),
                                message + " " + CommonUtility.getWord(Constants.DEVICEUPGRADE_SUCCESS), Alert.AlertType.INFORMATION);
                    }
                });
            }
            CommonUtility.sleepSeconds(60);
            FireflyLuciferin.guiManager.startCapturingThreads();
        }
    }

    /**
     * Reconnect to MQTT Broker
     *
     * @throws MqttException can't handle MQTT connection
     */
    void attemptReconnect() throws MqttException {
        boolean firstConnection = mqttDeviceName == null;
        if (NativeExecutor.isWindows()) {
            mqttDeviceName = Constants.MQTT_DEVICE_NAME_WIN;
        } else if (NativeExecutor.isLinux()) {
            mqttDeviceName = Constants.MQTT_DEVICE_NAME_LIN;
        } else {
            mqttDeviceName = Constants.MQTT_DEVICE_NAME_MAC;
        }
        mqttDeviceName += "_" + ThreadLocalRandom.current().nextInt();
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient(FireflyLuciferin.config.getMqttServer(), mqttDeviceName, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setAutomaticReconnect(true);
        connOpts.setCleanSession(true);
        connOpts.setConnectionTimeout(10);
        connOpts.setMaxInflight(1000); // Default = 10
        if (FireflyLuciferin.config.getMqttUsername() != null && !FireflyLuciferin.config.getMqttUsername().isEmpty()) {
            connOpts.setUserName(FireflyLuciferin.config.getMqttUsername());
        }
        if (FireflyLuciferin.config.getMqttPwd() != null && !FireflyLuciferin.config.getMqttPwd().isEmpty()) {
            connOpts.setPassword(FireflyLuciferin.config.getMqttPwd().toCharArray());
        }
        client.connect(connOpts);
        client.setCallback(this);
        if (firstConnection) {
            CommonUtility.turnOnLEDs();
            GammaDto gammaDto = new GammaDto();
            gammaDto.setGamma(FireflyLuciferin.config.getGamma());
            publishToTopic(getTopic(Constants.FIREFLY_LUCIFERIN_GAMMA), CommonUtility.toJsonString(gammaDto));
        }
        subscribeToTopics();
        log.info(Constants.MQTT_CONNECTED);
        connected = true;
    }

    /**
     * Reconnect on connection lost
     *
     * @param cause MQTT connection lost cause
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.error("Connection Lost");
        connected = false;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (!connected) {
                try {
                    // if long disconnection, reconfigure microcontroller
                    long duration = new Date().getTime() - lastActivity.getTime();
                    if (TimeUnit.MILLISECONDS.toSeconds(duration) > 60) {
                        log.info("Long disconnection occurred");
                        NativeExecutor.restartNativeInstance();
                    }
                    client.setCallback(this);
                    subscribeToTopics();
                    connected = true;
                    log.info(Constants.MQTT_RECONNECTED);
                } catch (MqttException e) {
                    log.error(Constants.MQTT_DISCONNECTED);
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * Subscribe to topics
     *
     * @throws MqttException can't subscribe
     */
    void subscribeToTopics() throws MqttException {
        client.subscribe(getTopic(Constants.DEFAULT_MQTT_TOPIC));
        client.subscribe(getTopic(Constants.DEFAULT_MQTT_STATE_TOPIC));
        client.subscribe(getTopic(Constants.UPDATE_RESULT_MQTT_TOPIC));
        client.subscribe(getTopic(Constants.FIREFLY_LUCIFERIN_GAMMA));
        client.subscribe(getTopic(Constants.SET_SMOOTHING_TOPIC));
        client.subscribe(getTopic(Constants.SMOOTHING_TOPIC));
        client.subscribe(getTopic(Constants.SET_ASPECT_RATIO_TOPIC));
        client.subscribe(getTopic(Constants.FIREFLY_LUCIFERIN_EFFECT_TOPIC));
    }

    /**
     * Subscribe to the topic to START/STOP screen grabbing
     *
     * @param topic   MQTT topic where to publish/subscribe
     * @param message MQTT message to read
     */
    @Override
    @SuppressWarnings("Duplicates")
    public void messageArrived(String topic, MqttMessage message) throws JsonProcessingException {
        lastActivity = new Date();
        if (topic.equals(getTopic(Constants.DEFAULT_MQTT_STATE_TOPIC))) {
            manageDefaultTopic(message);
        } else if (topic.equals(getTopic(Constants.UPDATE_RESULT_MQTT_TOPIC))) {
            // If a new firmware version is detected, restart the screen capture.
            showUpdateNotification(message);
        } else if (topic.equals(getTopic(Constants.DEFAULT_MQTT_TOPIC))) {
            manageMqttSetTopic(message);
        } else if (topic.equals(getTopic(Constants.FIREFLY_LUCIFERIN_GAMMA))) {
            manageGamma(message);
        } else if (topic.equals(getTopic(Constants.SET_ASPECT_RATIO_TOPIC))) {
            manageAspectRatio(message);
        } else if (topic.equals(getTopic(Constants.SET_SMOOTHING_TOPIC))) {
            manageSmoothing(message);
        } else if (topic.equals(getTopic(Constants.FIREFLY_LUCIFERIN_EFFECT_TOPIC))) {
            manageEffect(message.toString());
        }
    }

    /**
     * Callback for MQTT message sent
     *
     * @param token mqtt token
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //log.info("delivered");
    }

}