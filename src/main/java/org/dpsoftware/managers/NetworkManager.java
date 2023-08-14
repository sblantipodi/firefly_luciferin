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
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.controllers.DevicesTabController;
import org.dpsoftware.gui.controllers.MqttTabController;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.network.tcpUdp.TcpClient;
import org.dpsoftware.network.tcpUdp.UdpClient;
import org.dpsoftware.utilities.CommonUtility;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.awt.*;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
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
    private static Map<String, UdpClient> udpClient;
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
                String swappedMsg = swapMac(msg, null);
                publishMqttMsq(topic, swappedMsg, retainMsg, qos);
                if (FireflyLuciferin.config.getSatellites() != null) {
                    for (Map.Entry<String, Satellite> sat : FireflyLuciferin.config.getSatellites().entrySet()) {
                        if (!Constants.HTTP_TOPIC_TO_SKIP_FOR_SATELLITES.contains(topic)) {
                            swappedMsg = swapMac(msg, sat.getValue());
                            publishMqttMsq(topic, swappedMsg, retainMsg, qos);
                        }
                    }
                }
            } else {
                if (!topic.contains(Constants.MQTT_FIREFLY_BASE_TOPIC)) {
                    return TcpClient.httpGet(msg, topic);
                }
            }
        }
        return null;
    }

    /**
     * If targeting a satellite, swap MAC address
     *
     * @param msg to send to the device
     * @param sat satellite
     * @return original message with MAC swapped
     */
    public static String swapMac(String msg, Satellite sat) {
        if (sat == null) {
            return msg;
        } else {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode jsonMsg = mapper.readTree(msg.getBytes());
                if (jsonMsg.get(Constants.MAC) != null) {
                    ObjectNode object = (ObjectNode) jsonMsg;
                    String satMac = Objects.requireNonNull(DevicesTabController.deviceTableData.stream()
                            .filter(device -> sat.getDeviceIp().equals(device.getDeviceIP()))
                            .findFirst()
                            .orElse(null)).getMac();
                    object.put(Constants.MAC, satMac);
                    return mapper.writeValueAsString(object);
                } else {
                    return msg;
                }
            } catch (IOException e) {
                return msg;
            }
        }
    }

    /**
     * Simple MQTT msg sender
     *
     * @param topic     to use
     * @param msg       to send
     * @param retainMsg retained
     * @param qos       quality of service
     */
    private static void publishMqttMsq(String topic, String msg, boolean retainMsg, int qos) {
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
                client.publish(getTopic(Constants.TOPIC_DEFAULT_MQTT) + Constants.MQTT_STREAM_TOPIC + JavaFXStarter.whoAmI, msg.getBytes(), 0, false);
            } else {
                client.publish(getTopic(Constants.TOPIC_DEFAULT_MQTT) + Constants.MQTT_STREAM_TOPIC, msg.getBytes(), 0, false);
            }
        } catch (MqttException e) {
            log.error(Constants.MQTT_CANT_SEND);
        }
    }

    /**
     * Stream colors to main instance or to satellites
     *
     * @param leds   array of colors to send
     * @param ledStr string to send
     */
    public static void streamColors(Color[] leds, StringBuilder ledStr) {
        // UDP stream or MQTT stream
        if (FireflyLuciferin.config.getStreamType().equals(Enums.StreamType.UDP.getStreamType())) {
            if (udpClient == null) {
                udpClient = new LinkedHashMap<>();
            }
            try {
                udpClient.put(CommonUtility.getDeviceToUse().getDeviceIP(), new UdpClient(CommonUtility.getDeviceToUse().getDeviceIP()));
                udpClient.get(CommonUtility.getDeviceToUse().getDeviceIP()).manageStream(leds);
                if (FireflyLuciferin.config.getSatellites() != null) {
                    for (Map.Entry<String, Satellite> sat : FireflyLuciferin.config.getSatellites().entrySet()) {
                        if ((udpClient == null || udpClient.isEmpty()) || udpClient.get(sat.getKey()) == null || udpClient.get(sat.getKey()).socket.isClosed()) {
                            assert udpClient != null;
                            udpClient.put(sat.getValue().getDeviceIp(), new UdpClient(sat.getValue().getDeviceIp()));
                        }
                        assert udpClient != null;
                        assert udpClient.get(sat.getKey()) == null;
                        sendColorToSatellites(leds, sat.getValue());
                    }
                }
            } catch (SocketException | UnknownHostException e) {
                log.error(e.getMessage());
            }
        } else {
            ledStr.append("0");
            NetworkManager.stream(ledStr.toString());
        }
    }

    /**
     * Sends color to satellites using average or dominant algorithm
     *
     * @param leds array of colors to send
     * @param sat  satellite where to send colors
     */
    private static void sendColorToSatellites(Color[] leds, Satellite sat) {
        Color[] ledMatrix = Arrays.stream(leds).toArray(Color[]::new);
        if (Enums.Orientation.CLOCKWISE.equals((LocalizedEnum.fromBaseStr(Enums.Orientation.class, FireflyLuciferin.config.getOrientation())))) {
            Collections.reverse(Arrays.asList(ledMatrix));
        }
        java.util.List<Color> clonedLeds = new LinkedList<>();
        LEDCoordinate.getStartEndLeds zoneDetail = LEDCoordinate.getGetStartEndLeds(sat);
        int zoneStart = zoneDetail.start() - 1;
        int zoneNumLed = (zoneDetail.end() - zoneDetail.start()) + 1;
        int zoneEnd = zoneDetail.end() - 1;
        int satNumLed = Integer.parseInt(sat.getLedNum());
        if (Enums.Algo.AVG_COLOR.getBaseI18n().equals(sat.getAlgo())) {
            if (satNumLed <= zoneNumLed) {
                clonedLeds = ImageProcessor.reduceColors(ledMatrix, sat, zoneDetail);
            } else {
                clonedLeds = ImageProcessor.padColors(ledMatrix, sat, zoneDetail);
            }
        } else {
            Color avgColor = ImageProcessor.getAverageForAllZones(ledMatrix, zoneStart, zoneEnd);
            for (int i = 0; i < satNumLed; i++) {
                clonedLeds.add(new Color(avgColor.getRed(), avgColor.getGreen(), avgColor.getBlue()));
            }
        }
        Color[] cToSend = clonedLeds.toArray(Color[]::new);
        if (Enums.Direction.NORMAL.equals((LocalizedEnum.fromBaseStr(Enums.Direction.class, sat.getOrientation())))) {
            Collections.reverse(Arrays.asList(cToSend));
        }
        udpClient.get(sat.getDeviceIp()).manageStream(cToSend);
    }

    /**
     * Manage default MQTT topic
     *
     * @param message mqtt message
     * @throws JsonProcessingException something went wrong during JSON processing
     */
    private static void manageDefaultTopic(MqttMessage message) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode mqttmsg = mapper.readTree(message.getPayload());
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
    private static void manageMqttSetTopic(MqttMessage message) throws IOException {
        if (message.toString().contains(Constants.MQTT_START)) {
            FireflyLuciferin.guiManager.startCapturingThreads();
        } else if (message.toString().contains(Constants.MQTT_STOP)) {
            ObjectMapper gammaMapper = new ObjectMapper();
            JsonNode macObj = gammaMapper.readTree(message.getPayload());
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
    private static void manageFpsTopic(MqttMessage message) throws IOException {
        ObjectMapper mapperFps = new ObjectMapper();
        JsonNode mqttmsg = mapperFps.readTree(message.getPayload());
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
        if (Constants.TOPIC_DEFAULT_MQTT.equals(FireflyLuciferin.config.getMqttTopic())
                || gwBaseTopic.equals(FireflyLuciferin.config.getMqttTopic())) {
            defaultTopic = gwBaseTopic;
            defaultFireflyTopic = fireflyBaseTopic;
        }
        switch (command) {
            case Constants.TOPIC_DEFAULT_MQTT ->
                    topic = Constants.TOPIC_DEFAULT_MQTT.replace(gwBaseTopic, defaultTopic);
            case Constants.TOPIC_DEFAULT_MQTT_STATE ->
                    topic = Constants.TOPIC_DEFAULT_MQTT_STATE.replace(gwBaseTopic, defaultTopic);
            case Constants.TOPIC_UPDATE_MQTT -> topic = Constants.TOPIC_UPDATE_MQTT.replace(gwBaseTopic, defaultTopic);
            case Constants.TOPIC_UPDATE_RESULT_MQTT ->
                    topic = Constants.TOPIC_UPDATE_RESULT_MQTT.replace(gwBaseTopic, defaultTopic);
            case Constants.TOPIC_FIREFLY_LUCIFERIN_FRAMERATE ->
                    topic = Constants.TOPIC_FIREFLY_LUCIFERIN_FRAMERATE.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.TOPIC_FIREFLY_LUCIFERIN_GAMMA ->
                    topic = Constants.TOPIC_FIREFLY_LUCIFERIN_GAMMA.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.TOPIC_ASPECT_RATIO ->
                    topic = Constants.TOPIC_ASPECT_RATIO.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.TOPIC_SET_ASPECT_RATIO ->
                    topic = Constants.TOPIC_SET_ASPECT_RATIO.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.TOPIC_SET_SMOOTHING ->
                    topic = Constants.TOPIC_SET_SMOOTHING.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.TOPIC_FIREFLY_LUCIFERIN_EFFECT ->
                    topic = Constants.TOPIC_FIREFLY_LUCIFERIN_EFFECT.replace(gwBaseTopic, defaultTopic);
            case Constants.TOPIC_GLOW_WORM_FIRM_CONFIG -> topic = Constants.TOPIC_GLOW_WORM_FIRM_CONFIG;
            case Constants.TOPIC_UNSUBSCRIBE_STREAM ->
                    topic = Constants.TOPIC_UNSUBSCRIBE_STREAM.replace(gwBaseTopic, defaultTopic);
            case Constants.HTTP_SET_LDR -> topic = Constants.HTTP_SET_LDR.replace(gwBaseTopic, defaultTopic);
            case Constants.TOPIC_FIREFLY_LUCIFERIN_PROFILE_SET ->
                    topic = Constants.TOPIC_FIREFLY_LUCIFERIN_PROFILE_SET.replace(fireflyBaseTopic, defaultFireflyTopic);
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
     * Get MQTT connection Options
     *
     * @return MQTT connection Options
     */
    private static MqttConnectOptions getMqttConnectOptions() {
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
        return connOpts;
    }

    /**
     * Check is a String is a valid IPv4 address
     *
     * @param ip address as String
     * @return true if the string is an IPv4 address
     */
    public static boolean isValidIp(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return false;
            }
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
            return !ip.endsWith(".");
        } catch (NumberFormatException nfe) {
            return false;
        }
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
     * Manage profile topic
     *
     * @param message message
     */
    private void manageProfile(String message) {
        if (FireflyLuciferin.config != null) {
            CommonUtility.delayMilliseconds(() -> FireflyLuciferin.guiManager.trayIconManager.manageProfileListener(message), 200);
        }
    }

    /**
     * Manage firmware config topic
     * No swap because that topic needs MAC, no need to swap topic. Some topics are HTTP only via IP.
     *
     * @param message message
     */
    private void manageFirmwareConfig(String message) throws JsonProcessingException {
        if (FireflyLuciferin.config != null) {
            if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode mqttmsg = mapper.readTree(message);
                if (CommonUtility.getDeviceToUse().getMac().equals(mqttmsg.get(Constants.MAC).asText())) {
                    FireflyLuciferin.config.setColorMode(mqttmsg.get(Constants.COLOR_MODE).asInt());
                }
            }
            log.debug(message);
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
    private void manageGamma(MqttMessage message) throws IOException {
        ObjectMapper gammaMapper = new ObjectMapper();
        JsonNode gammaObj = gammaMapper.readTree(message.getPayload());
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
        if (UpgradeManager.deviceNameForSerialDevice.equals(message.toString())
                || UpgradeManager.deviceNameForSerialDevice.equals(message + Constants.CDC_DEVICE)) {
            log.info("Update successfull=" + message);
            if (!CommonUtility.isSingleDeviceMultiScreen() || CommonUtility.isSingleDeviceMainInstance()) {
                javafx.application.Platform.runLater(() -> {
                    String notificationContext = message + " ";
                    if (UpgradeManager.deviceNameForSerialDevice.contains(Constants.CDC_DEVICE) && !FireflyLuciferin.config.isWirelessStream()) {
                        notificationContext += CommonUtility.getWord(Constants.DEVICEUPGRADE_SUCCESS_CDC);
                    } else {
                        notificationContext += CommonUtility.getWord(Constants.DEVICEUPGRADE_SUCCESS);
                    }
                    if (NativeExecutor.isWindows()) {
                        FireflyLuciferin.guiManager.showNotification(CommonUtility.getWord(Constants.UPGRADE_SUCCESS),
                                notificationContext, TrayIcon.MessageType.INFO);
                    } else {
                        FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN, CommonUtility.getWord(Constants.UPGRADE_SUCCESS),
                                notificationContext, Alert.AlertType.INFORMATION);
                    }
                });
            }
            CommonUtility.sleepSeconds(60);
            FireflyLuciferin.guiManager.startCapturingThreads();
        }
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
        client.subscribe(getTopic(Constants.TOPIC_DEFAULT_MQTT));
        client.subscribe(getTopic(Constants.TOPIC_DEFAULT_MQTT_STATE));
        client.subscribe(getTopic(Constants.TOPIC_UPDATE_RESULT_MQTT));
        client.subscribe(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_GAMMA));
        client.subscribe(getTopic(Constants.TOPIC_SET_SMOOTHING));
        client.subscribe(getTopic(Constants.TOPIC_SET_ASPECT_RATIO));
        client.subscribe(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_EFFECT));
        client.subscribe(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_PROFILE_SET));
        client.subscribe(Constants.TOPIC_GLOW_WORM_FIRM_CONFIG);
    }

    /**
     * Subscribe to the topic to START/STOP screen grabbing
     *
     * @param topic   MQTT topic where to publish/subscribe
     * @param message MQTT message to read
     */
    @Override
    @SuppressWarnings("Duplicates")
    public void messageArrived(String topic, MqttMessage message) throws IOException {
        lastActivity = new Date();
        if (topic.equals(getTopic(Constants.TOPIC_DEFAULT_MQTT_STATE))) {
            manageDefaultTopic(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_UPDATE_RESULT_MQTT))) {
            // If a new firmware version is detected, restart the screen capture.
            showUpdateNotification(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_DEFAULT_MQTT))) {
            manageMqttSetTopic(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_GAMMA))) {
            manageGamma(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_SET_ASPECT_RATIO))) {
            manageAspectRatio(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_SET_SMOOTHING))) {
            manageSmoothing(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_EFFECT))) {
            manageEffect(message.toString());
        } else if (topic.equals(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_PROFILE_SET))) {
            manageProfile(message.toString());
        } else if (topic.equals(Constants.TOPIC_GLOW_WORM_FIRM_CONFIG)) {
            manageFirmwareConfig(message.toString());
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
        MqttConnectOptions connOpts = getMqttConnectOptions();
        client.connect(connOpts);
        client.setCallback(this);
        if (firstConnection) {
            CommonUtility.turnOnLEDs();
            // Wait that the device is engaged before updating MQTT discovery entities.
            if (StorageManager.updateMqttDiscovery) {
                ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
                es.scheduleAtFixedRate(() -> {
                    if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac() != null && !CommonUtility.getDeviceToUse().getMac().isEmpty()) {
                        MqttTabController.publishDiscoveryTopics(false);
                        MqttTabController.publishDiscoveryTopics(true);
                        es.shutdownNow();
                    }
                }, 0, 2, TimeUnit.SECONDS);
            }
        }
        subscribeToTopics();
        log.info(Constants.MQTT_CONNECTED);
        connected = true;
    }

}