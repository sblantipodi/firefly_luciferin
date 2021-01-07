/*
  MQTTManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2021  Davide Perini

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
package org.dpsoftware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Platform;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.GUIManager;
import org.dpsoftware.gui.SettingsController;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * This class controls the MQTT traffic
 */
@Slf4j
public class MQTTManager implements MqttCallback {

    MqttClient client;
    boolean connected = false;
    boolean reconnectionThreadRunning = false;
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
            guiManager.showAlert(Constants.MQTT_ERROR_TITLE,
                    Constants.MQTT_ERROR_HEADER,
                    Constants.MQTT_ERROR_CONTEXT, Alert.AlertType.ERROR);
            log.error("Can't connect to the MQTT Server");
        }

    }

    /**
     * Reconnect to MQTT Broker
     * @throws MqttException can't handle MQTT connection
     */
    void attemptReconnect() throws MqttException {

        boolean firstConnection = false;
        if (mqttDeviceName == null) {
            firstConnection = true;
        }
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
            turnOnLEDs();
            publishToTopic(Constants.FIREFLY_LUCIFERIN_GAMMA, "{\""+Constants.MQTT_GAMMA+"\":" + FireflyLuciferin.config.getGamma() + "}");
        }
        client.subscribe(FireflyLuciferin.config.getMqttTopic());
        client.subscribe(Constants.DEFAULT_MQTT_STATE_TOPIC);
        client.subscribe(Constants.UPDATE_RESULT_MQTT_TOPIC);
        client.subscribe(Constants.FIREFLY_LUCIFERIN_GAMMA);
        client.subscribe(Constants.FPS_TOPIC);
        log.info(Constants.MQTT_CONNECTED);
        connected = true;
        
    }

    /**
     * Publish to a topic
     * @param topic where to publish the message
     * @param msg msg for the queue
     */
    public void publishToTopic(String topic, String msg) {

        MqttMessage message = new MqttMessage();
        message.setPayload(msg.getBytes());
        message.setRetained(false);
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            log.error(Constants.MQTT_CANT_SEND);
        }

    }

    /**
     * Stream messages to the stream topic
     * @param msg msg for the queue
     */
    public void stream(String msg) {

        try {
            client.publish(FireflyLuciferin.config.getMqttTopic() + Constants.MQTT_STREAM_TOPIC, msg.getBytes(), 0, false);
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
        if (!reconnectionThreadRunning) {
            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (!connected) {
                    try {
                        // if long disconnection, reconfigure microcontroller
                        long duration = new Date().getTime() - lastActivity.getTime();
                        if (TimeUnit.MILLISECONDS.toMinutes(duration) > 1) {
                            log.debug("Long disconnection occurred");
                            if (FireflyLuciferin.guiManager != null) {
                                FireflyLuciferin.guiManager.stopCapturingThreads();
                            }
                            try {
                                TimeUnit.SECONDS.sleep(4);
                                log.debug(Constants.CLEAN_EXIT);
                                NativeExecutor.restartNativeInstance();
                                FireflyLuciferin.exit();
                            } catch (InterruptedException e) {
                                log.error(e.getMessage());
                            }
                        }
                        client.setCallback(this);
                        client.subscribe(FireflyLuciferin.config.getMqttTopic());
                        client.subscribe(Constants.DEFAULT_MQTT_STATE_TOPIC);
                        client.subscribe(Constants.UPDATE_RESULT_MQTT_TOPIC);
                        connected = true;
                        log.info(Constants.MQTT_RECONNECTED);
                    } catch (MqttException e) {
                        log.error(Constants.MQTT_DISCONNECTED);
                    }
                }
            }, 0, 10, TimeUnit.SECONDS);
        }

    }

    /**
     * Subscribe to the topic to START/STOP screen grabbing
     * @param topic MQTT topic where to publish/subscribe
     * @param message MQTT message to read
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws JsonProcessingException {

        lastActivity = new Date();
        switch (topic) {
            case Constants.DEFAULT_MQTT_STATE_TOPIC:
                ObjectMapper mapper = new ObjectMapper();
                JsonNode mqttmsg = mapper.readTree(new String(message.getPayload()));
                if (mqttmsg.get(Constants.STATE) != null) {
                    if (mqttmsg.get(Constants.STATE).asText().equals(Constants.OFF)) {
                        FireflyLuciferin.config.setToggleLed(false);
                    } else if (mqttmsg.get(Constants.STATE).asText().equals(Constants.ON) && mqttmsg.get(Constants.EFFECT).asText().equals(Constants.SOLID)) {
                        FireflyLuciferin.config.setToggleLed(true);
                        if (mqttmsg.get(Constants.COLOR) != null) {
                            FireflyLuciferin.config.setColorChooser(mqttmsg.get(Constants.COLOR).get("r") + "," + mqttmsg.get(Constants.COLOR).get("g") + ","
                                + mqttmsg.get(Constants.COLOR).get("b") + "," + mqttmsg.get(Constants.MQTT_BRIGHTNESS));
                        }
                    }
                    if (mqttmsg.get(Constants.MQTT_TOPIC_FRAMERATE) != null) {
                        FireflyLuciferin.FPS_GW_CONSUMER = Float.parseFloat(mqttmsg.get(Constants.MQTT_TOPIC_FRAMERATE).asText());
                    }
                }
                // Skip retained message, we want fresh data here
                if (!message.isRetained()) {
                    if (mqttmsg.get(Constants.WHOAMI) != null) {
                        if (SettingsController.deviceTableData.isEmpty()) {
                            addDevice(mqttmsg);
                        } else {
                            AtomicBoolean isDevicePresent = new AtomicBoolean(false);
                            SettingsController.deviceTableData.forEach(glowWormDevice -> {
                                String newDeviceName = mqttmsg.get(Constants.WHOAMI).textValue();
                                if (glowWormDevice.getDeviceName().equals(newDeviceName)) {
                                    isDevicePresent.set(true);
                                    glowWormDevice.setLastSeen(FireflyLuciferin.formatter.format(new Date()));
                                    if (mqttmsg.get(Constants.GPIO) != null) glowWormDevice.setGpio(mqttmsg.get(Constants.GPIO).textValue());
                                }
                            });
                            if (!isDevicePresent.get()) {
                                addDevice(mqttmsg);
                            }
                        }
                    }
                }
                break;
            case Constants.UPDATE_RESULT_MQTT_TOPIC:
                log.debug("Update successfull=" + message.toString());
                javafx.application.Platform.runLater(() -> FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN,
                        Constants.UPGRADE_SUCCESS, message.toString() + " " + Constants.DEVICEUPGRADE_SUCCESS,
                        Alert.AlertType.INFORMATION));
                break;
            case Constants.DEFAULT_MQTT_TOPIC:
                if (message.toString().contains(Constants.MQTT_START)) {
                    FireflyLuciferin.guiManager.startCapturingThreads();
                } else if (message.toString().contains(Constants.MQTT_STOP)) {
                    FireflyLuciferin.guiManager.stopCapturingThreads();
                }
                break;
            case Constants.FIREFLY_LUCIFERIN_GAMMA:
                ObjectMapper gammaMapper = new ObjectMapper();
                JsonNode gammaObj = gammaMapper.readTree(new String(message.getPayload()));
                if (gammaObj.get(Constants.MQTT_GAMMA) != null) {
                    FireflyLuciferin.config.setGamma(Double.parseDouble(gammaObj.get(Constants.MQTT_GAMMA).asText()));
                }
                break;
            case Constants.FPS_TOPIC:
                ObjectMapper fpsMapper = new ObjectMapper();
                JsonNode fpsTopicMsg = fpsMapper.readTree(new String(message.getPayload()));
                if (fpsTopicMsg.get(Constants.MQTT_DEVICE_NAME) != null) {
                    SettingsController.deviceTableData.forEach(glowWormDevice -> {
                        String deviceToUpdate = fpsTopicMsg.get(Constants.MQTT_DEVICE_NAME).textValue();
                        if (glowWormDevice.getDeviceName().equals(deviceToUpdate)) {
                            glowWormDevice.setLastSeen(FireflyLuciferin.formatter.format(new Date()));
                            glowWormDevice.setNumberOfLEDSconnected(fpsTopicMsg.get(Constants.NUMBER_OF_LEDS).textValue());
                            FireflyLuciferin.FPS_GW_CONSUMER = Float.parseFloat(fpsTopicMsg.get(Constants.MQTT_TOPIC_FRAMERATE).asText());
                        }
                    });
                }
                break;
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
     * Add device to the connected device table
     * @param actualObj JSON node
     */
    private void addDevice(JsonNode actualObj) {

        SettingsController.deviceTableData.add(new GlowWormDevice(actualObj.get(Constants.WHOAMI).textValue(),
                actualObj.get(Constants.STATE_IP).textValue(), actualObj.get(Constants.DEVICE_VER).textValue(),
                (actualObj.get(Constants.DEVICE_BOARD) == null ? Constants.DASH : actualObj.get(Constants.DEVICE_BOARD).textValue()),
                (actualObj.get(Constants.MAC) == null ? Constants.DASH : actualObj.get(Constants.MAC).textValue()),
                (actualObj.get(Constants.GPIO) == null ? Constants.DASH : actualObj.get(Constants.GPIO).textValue()),
                (actualObj.get(Constants.NUMBER_OF_LEDS) == null ? Constants.DASH : actualObj.get(Constants.NUMBER_OF_LEDS).textValue()),
                (FireflyLuciferin.formatter.format(new Date()))));

    }

    /**
     * Turn ON LEDs when Luciferin starts
     */
    void turnOnLEDs() {

        if (!FireflyLuciferin.config.isAutoStartCapture()) {
            if (FireflyLuciferin.config.isToggleLed()) {
                if (FireflyLuciferin.config.isMqttEnable()) {
                    String[] color = FireflyLuciferin.config.getColorChooser().split(",");
                    publishToTopic(FireflyLuciferin.config.getMqttTopic(), Constants.STATE_ON_SOLID_COLOR
                            .replace(Constants.RED_COLOR, color[0])
                            .replace(Constants.GREEN_COLOR, color[1])
                            .replace(Constants.BLU_COLOR, color[2])
                            .replace(Constants.BRIGHTNESS, color[3]));
                }
            } else {
                if (FireflyLuciferin.config.isMqttEnable()) {
                    publishToTopic(FireflyLuciferin.config.getMqttTopic(), Constants.STATE_OFF_SOLID);
                }
            }
        }

    }

}