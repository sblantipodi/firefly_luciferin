/*
  MQTTManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini  (https://github.com/sblantipodi)

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
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.GUIManager;
import org.dpsoftware.managers.dto.GammaDto;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.network.tcpUdp.TcpClient;
import org.dpsoftware.utilities.CommonUtility;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * This class controls the MQTT traffic
 */
@Slf4j
public class MQTTManager implements MqttCallback {

    public static MqttClient client;
    boolean connected = false;
    String mqttDeviceName;
    Date lastActivity;

    /**
     * Constructor
     */
    public MQTTManager() {
        try {
            lastActivity = new Date();
            attemptReconnect();
        } catch (MqttException | RuntimeException e) {
            connected = false;
            FireflyLuciferin.communicationError = true;
            GUIManager guiManager = new GUIManager();
            guiManager.showLocalizedAlert(Constants.MQTT_ERROR_TITLE, Constants.MQTT_ERROR_HEADER, Constants.MQTT_ERROR_CONTEXT, Alert.AlertType.ERROR);
            log.error("Can't connect to the MQTT Server");
        }
    }

    /**
     * Reconnect to MQTT Broker
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
            publishToTopic(getMqttTopic(Constants.MQTT_GAMMA), CommonUtility.toJsonString(gammaDto));
        }
        subscribeToTopics();
        log.info(Constants.MQTT_CONNECTED);
        connected = true;
    }

    /**
     * Publish to a topic
     * @param topic where to publish the message
     * @param msg   msg for the queue
     * @return TCP response if it's converted in an HTTP request
     */
    public static TcpResponse publishToTopic(String topic, String msg) {
        return publishToTopic(topic, msg, false);
    }

    /**
     * Publish to a topic
     * @param topic where to publish the message
     * @param msg   msg for the queue
     * @param forceHttpRequest force HTTP request even if MQTT is enabled
     * @return TCP response if it's converted in an HTTP request
     */
    public static TcpResponse publishToTopic(String topic, String msg, boolean forceHttpRequest) {
        if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
            if (FireflyLuciferin.config.isMqttEnable() && !forceHttpRequest) {
                MqttMessage message = new MqttMessage();
                message.setPayload(msg.getBytes());
                message.setRetained(false);
                CommonUtility.conditionedLog("MQTTManager", "Topic=" + topic + "\n" + msg);
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
     * Stream messages to the stream topic
     * @param msg msg for the queue
     */
    public static void stream(String msg) {
        try {
            // If multi display change stream topic
            if (FireflyLuciferin.config.getMultiMonitor() > 1 && !CommonUtility.isSingleDeviceMultiScreen()) {
                client.publish(getMqttTopic(Constants.MQTT_SET) + Constants.MQTT_STREAM_TOPIC + JavaFXStarter.whoAmI, msg.getBytes(), 0, false);
            } else {
                client.publish(getMqttTopic(Constants.MQTT_SET) + Constants.MQTT_STREAM_TOPIC, msg.getBytes(), 0, false);
            }
        } catch (MqttException e) {
            log.error(Constants.MQTT_CANT_SEND);
        }
    }

    /**
     * Reconnect on connection lost
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
                        log.debug("Long disconnection occurred");
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
     * @throws MqttException can't subscribe
     */
    void subscribeToTopics() throws MqttException {
        client.subscribe(getMqttTopic(Constants.MQTT_SET));
        client.subscribe(getMqttTopic(Constants.MQTT_EMPTY));
        client.subscribe(getMqttTopic(Constants.MQTT_UPDATE_RES));
        client.subscribe(getMqttTopic(Constants.MQTT_GAMMA));
        client.subscribe(getMqttTopic(Constants.MQTT_FPS));
    }

    /**
     * Subscribe to the topic to START/STOP screen grabbing
     * @param topic   MQTT topic where to publish/subscribe
     * @param message MQTT message to read
     */
    @Override
    @SuppressWarnings("Duplicates")
    public void messageArrived(String topic, MqttMessage message) throws JsonProcessingException {
        lastActivity = new Date();
        if (topic.equals(getMqttTopic(Constants.MQTT_EMPTY))) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode mqttmsg = mapper.readTree(new String(message.getPayload()));
            if (mqttmsg.get(Constants.STATE) != null) {
                if (mqttmsg.get(Constants.START_STOP_INSTANCES) != null && mqttmsg.get(Constants.START_STOP_INSTANCES).asText().equals(Constants.PlayerStatus.STOP.name())) {
                    FireflyLuciferin.guiManager.stopCapturingThreads(false);
                } else if (mqttmsg.get(Constants.START_STOP_INSTANCES) != null && mqttmsg.get(Constants.START_STOP_INSTANCES).asText().equals(Constants.PlayerStatus.PLAY.name())) {
                    FireflyLuciferin.guiManager.startCapturingThreads();
                } else {
                    if (mqttmsg.get(Constants.STATE).asText().equals(Constants.ON) && mqttmsg.get(Constants.EFFECT).asText().equals(Constants.SOLID)) {
                        FireflyLuciferin.config.setToggleLed(true);
                        String brightnessToSet;
                        if (mqttmsg.get(Constants.COLOR) != null) {
                            if (FireflyLuciferin.nightMode) {
                                brightnessToSet = FireflyLuciferin.config.getBrightness() + "";
                            } else {
                                brightnessToSet = mqttmsg.get(Constants.MQTT_BRIGHTNESS) + "";
                            }
                            FireflyLuciferin.config.setColorChooser(mqttmsg.get(Constants.COLOR).get("r") + "," + mqttmsg.get(Constants.COLOR).get("g") + ","
                                    + mqttmsg.get(Constants.COLOR).get("b") + "," + brightnessToSet);
                        }
                    }
                    CommonUtility.updateFpsWithDeviceTopic(mqttmsg);
                }
            }
            // Skip retained message, we want fresh data here
            if (!message.isRetained()) {
                if (mqttmsg.get(Constants.MQTT_DEVICE_NAME) != null) {
                    CommonUtility.updateDeviceTable(mqttmsg);
                }
            }
        } else if (topic.equals(getMqttTopic(Constants.MQTT_UPDATE_RES))) {
            // If a new firmware version is detected, restart the screen capture.
            if (UpgradeManager.deviceNameForSerialDevice.equals(message.toString())) {
                log.debug("Update successfull=" + message);
                if (!CommonUtility.isSingleDeviceMultiScreen() || CommonUtility.isSingleDeviceMainInstance()) {
                    javafx.application.Platform.runLater(() -> FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN,
                            CommonUtility.getWord(Constants.UPGRADE_SUCCESS), message + " " + CommonUtility.getWord(Constants.DEVICEUPGRADE_SUCCESS),
                            Alert.AlertType.INFORMATION));
                }
                CommonUtility.sleepSeconds(60);
                FireflyLuciferin.guiManager.startCapturingThreads();
            }
        } else if (topic.equals(getMqttTopic(Constants.MQTT_SET))) {
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
        } else if (topic.equals(getMqttTopic(Constants.MQTT_GAMMA))) {
            ObjectMapper gammaMapper = new ObjectMapper();
            JsonNode gammaObj = gammaMapper.readTree(new String(message.getPayload()));
            if (gammaObj.get(Constants.MQTT_GAMMA) != null) {
                FireflyLuciferin.config.setGamma(Double.parseDouble(gammaObj.get(Constants.MQTT_GAMMA).asText()));
            }
        } else if (topic.equals(getMqttTopic(Constants.MQTT_FPS))) {
            ObjectMapper mapperFps = new ObjectMapper();
            JsonNode mqttmsg = mapperFps.readTree(new String(message.getPayload()));
            CommonUtility.updateFpsWithFpsTopic(mqttmsg);
        }
    }

    /**
     * Callback for MQTT message sent
     * @param token mqtt token
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        //log.info("delivered");
    }

    /**
     * Return an MQTT topic using the configuration file
     * @param command MQTT command
     * @return MQTT topic
     */
    public static String getMqttTopic(String command) {
        String topic = null;
        String gwBaseTopic = Constants.MQTT_BASE_TOPIC;
        String fireflyBaseTopic = Constants.MQTT_FIREFLY_BASE_TOPIC;

        String defaultTopic = FireflyLuciferin.config.getMqttTopic();
        String defaultFireflyTopic = fireflyBaseTopic + FireflyLuciferin.config.getMqttTopic();
        if (Constants.DEFAULT_MQTT_TOPIC.equals(FireflyLuciferin.config.getMqttTopic())
                || gwBaseTopic.equals(FireflyLuciferin.config.getMqttTopic())) {
            defaultTopic = gwBaseTopic;
            defaultFireflyTopic = fireflyBaseTopic;
        }
        switch (command) {
            case Constants.MQTT_SET -> topic = Constants.DEFAULT_MQTT_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.MQTT_EMPTY -> topic = Constants.DEFAULT_MQTT_STATE_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.MQTT_UPDATE -> topic = Constants.UPDATE_MQTT_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.MQTT_FPS -> topic = Constants.FPS_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.MQTT_UPDATE_RES -> topic = Constants.UPDATE_RESULT_MQTT_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.MQTT_FRAMERATE -> topic = Constants.FIREFLY_LUCIFERIN_FRAMERATE.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.MQTT_GAMMA -> topic = Constants.FIREFLY_LUCIFERIN_GAMMA.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.MQTT_FIRMWARE_CONFIG -> topic = Constants.GLOW_WORM_FIRM_CONFIG_TOPIC;
            case Constants.MQTT_UNSUBSCRIBE -> topic = Constants.UNSUBSCRIBE_STREAM_TOPIC.replace(gwBaseTopic, defaultTopic);
            case Constants.MQTT_LDR -> topic = Constants.LDR_TOPIC.replace(gwBaseTopic, defaultTopic);
        }
        return topic;
    }

    /**
     * Check if current topic differs from main topic, this is needed when upgrading instances that uses different topics
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
}