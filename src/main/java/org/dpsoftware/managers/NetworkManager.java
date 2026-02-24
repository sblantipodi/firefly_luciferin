/*
  NetworkManager.java

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
package org.dpsoftware.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.controllers.NetworkTabController;
import org.dpsoftware.gui.elements.GlowWormDevice;
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
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class controls the MQTT traffic
 */
@Slf4j
public class NetworkManager implements MqttCallback {

    public boolean connected = false;
    String mqttDeviceName;
    Date lastActivity;
    private boolean isRestartingMqtt = false;
    private ScheduledFuture<?> scheduledMqttFuture;

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
                Platform.runLater(() -> MainSingleton.getInstance().guiManager.showLocalizedNotification(Constants.MQTT_ERROR_TITLE,
                        Constants.MQTT_ERROR_CONTEXT, Constants.MQTT_ERROR_TITLE, TrayIcon.MessageType.ERROR));
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
            if (MainSingleton.getInstance().config.isMqttEnable() && !forceHttpRequest && ManagerSingleton.getInstance().client != null) {
                String swappedMsg = swapMac(msg, null);
                publishMqttMsq(topic, swappedMsg, retainMsg, qos);
                if (MainSingleton.getInstance().config.getSatellites() != null) {
                    for (Map.Entry<String, Satellite> sat : MainSingleton.getInstance().config.getSatellites().entrySet()) {
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
                    String satMac = GuiSingleton.getInstance().deviceTableData.stream()
                            .filter(device -> sat.getDeviceIp().equals(device.getDeviceIP()))
                            .findFirst()
                            .map(GlowWormDevice::getMac)
                            .orElse(null);
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
        log.trace("Published on topic={}\n{}", topic, msg);
        try {
            ManagerSingleton.getInstance().client.publish(topic, message);
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
            if (MainSingleton.getInstance().config.getMultiMonitor() > 1 && !CommonUtility.isSingleDeviceMultiScreen()) {
                ManagerSingleton.getInstance().client.publish(getTopic(Constants.TOPIC_DEFAULT_MQTT) + Constants.MQTT_STREAM_TOPIC + MainSingleton.getInstance().whoAmI, msg.getBytes(), 0, false);
            } else {
                ManagerSingleton.getInstance().client.publish(getTopic(Constants.TOPIC_DEFAULT_MQTT) + Constants.MQTT_STREAM_TOPIC, msg.getBytes(), 0, false);
            }
        } catch (MqttException e) {
            log.error(Constants.MQTT_CANT_SEND);
        }
    }

    /**
     * Stream colors to main instance or to satellites.
     * Don't close the socket once written to it but reuse it, high CPU overhead instead.
     *
     * @param leds   array of colors to send
     * @param ledStr string to send
     */
    public static void streamColors(Color[] leds, StringBuilder ledStr) {
        // UDP stream or MQTT stream
        if (MainSingleton.getInstance().config.getStreamType().equals(Enums.StreamType.UDP.getStreamType())) {
            if (ManagerSingleton.getInstance().udpClient == null) {
                ManagerSingleton.getInstance().udpClient = new LinkedHashMap<>();
            }
            String deviceToUseIp = CommonUtility.getDeviceToUse().getDeviceIP();
            try {
                if (ManagerSingleton.getInstance().udpClient.get(deviceToUseIp) == null
                        || ManagerSingleton.getInstance().udpClient.get(deviceToUseIp).socket == null
                        || ManagerSingleton.getInstance().udpClient.get(deviceToUseIp).socket.isClosed()) {
                    ManagerSingleton.getInstance().udpClient.put(deviceToUseIp, new UdpClient(deviceToUseIp));
                }
                ManagerSingleton.getInstance().udpClient.get(deviceToUseIp).manageStream(leds);
                if (MainSingleton.getInstance().config.getSatellites() != null) {
                    for (Map.Entry<String, Satellite> sat : MainSingleton.getInstance().config.getSatellites().entrySet()) {
                        if ((ManagerSingleton.getInstance().udpClient == null || ManagerSingleton.getInstance().udpClient.isEmpty())
                                || ManagerSingleton.getInstance().udpClient.get(sat.getKey()) == null || ManagerSingleton.getInstance().udpClient.get(sat.getKey()).socket.isClosed()) {
                            assert ManagerSingleton.getInstance().udpClient != null;
                            ManagerSingleton.getInstance().udpClient.put(sat.getValue().getDeviceIp(), new UdpClient(sat.getValue().getDeviceIp()));
                        }
                        assert ManagerSingleton.getInstance().udpClient != null;
                        assert ManagerSingleton.getInstance().udpClient.get(sat.getKey()) == null;
                        sendColorToSatellites(leds, sat.getValue());
                    }
                }
            } catch (SocketException | UnknownHostException e) {
                log.error(e.getMessage());
                try {
                    ManagerSingleton.getInstance().udpClient.get(deviceToUseIp).close();
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
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
        if (Enums.Orientation.CLOCKWISE.equals((LocalizedEnum.fromBaseStr(Enums.Orientation.class, MainSingleton.getInstance().config.getOrientation())))) {
            Collections.reverse(Arrays.asList(ledMatrix));
        }
        java.util.List<Color> clonedLedsPrimary = new LinkedList<>();
        java.util.List<Color> clonedLedsSecondary = new LinkedList<>();
        java.util.List<Color> clonedLeds = new LinkedList<>();
        if (CommonUtility.isSplitBottomRow(MainSingleton.getInstance().config.getSplitBottomMargin()) && sat.getZone().equals(Enums.PossibleZones.BOTTOM.getBaseI18n())) {
            int tempSatNum = (int) Math.floor((double) Integer.parseInt(sat.getLedNum()) / 2);
            int satNum = Integer.parseInt(sat.getLedNum());
            sat.setLedNum(String.valueOf(tempSatNum));
            sat.setZone(Enums.PossibleZones.BOTTOM_LEFT.getBaseI18n());
            clonedLedsPrimary = getColorsForSat(sat, clonedLedsPrimary, ledMatrix);
            clonedLeds.addAll(clonedLedsPrimary);
            sat.setZone(Enums.PossibleZones.BOTTOM_RIGHT.getBaseI18n());
            clonedLedsSecondary = getColorsForSat(sat, clonedLedsSecondary, ledMatrix);
            clonedLeds.addAll(clonedLedsSecondary);
            sat.setLedNum(String.valueOf(satNum));
            sat.setZone(Enums.PossibleZones.BOTTOM.getBaseI18n());
        } else {
            clonedLeds.addAll(getColorsForSat(sat, clonedLedsPrimary, ledMatrix));
        }
        Color[] cToSend = clonedLeds.toArray(Color[]::new);
        if (Enums.Direction.NORMAL.equals((LocalizedEnum.fromBaseStr(Enums.Direction.class, sat.getOrientation())))) {
            Collections.reverse(Arrays.asList(cToSend));
        }
        ManagerSingleton.getInstance().udpClient.get(sat.getDeviceIp()).manageStream(cToSend);
    }

    /**
     * Calculate colors to send to the satellite
     *
     * @param sat        satellite in use
     * @param clonedLeds temp array with colors
     * @param ledMatrix  original led matrix
     * @return colors to send to the satellite
     */
    private static List<Color> getColorsForSat(Satellite sat, List<Color> clonedLeds, Color[] ledMatrix) {
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
        return clonedLeds;
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
                    MainSingleton.getInstance().config.setToggleLed(true);
                    String brightnessToSet;
                    if (mqttmsg.get(Constants.COLOR) != null) {
                        if (MainSingleton.getInstance().nightMode) {
                            brightnessToSet = String.valueOf(MainSingleton.getInstance().config.getBrightness());
                        } else {
                            brightnessToSet = String.valueOf(mqttmsg.get(Constants.MQTT_BRIGHTNESS));
                        }
                        MainSingleton.getInstance().config.setColorChooser(mqttmsg.get(Constants.COLOR).get("r") + "," + mqttmsg.get(Constants.COLOR).get("g") + ","
                                + mqttmsg.get(Constants.COLOR).get("b") + "," + brightnessToSet);
                    }
                } else if (mqttmsg.get(Constants.STATE).asText().equals(Constants.OFF) && mqttmsg.get(Constants.EFFECT).asText().equals(Constants.SOLID)) {
                    if (MainSingleton.getInstance().isInitialized() && !MainSingleton.getInstance().waitingWaylandToken) {
                        MainSingleton.getInstance().config.setToggleLed(false);
                    }
                }
                CommonUtility.updateFpsWithDeviceTopic(mqttmsg);
            }
        } else if (mqttmsg.get(Constants.START_STOP_INSTANCES) != null && mqttmsg.get(Constants.START_STOP_INSTANCES).asText().equals(Enums.PlayerStatus.STOP.name())) {
            MainSingleton.getInstance().guiManager.stopCapturingThreads(false);
        } else if (mqttmsg.get(Constants.START_STOP_INSTANCES) != null && mqttmsg.get(Constants.START_STOP_INSTANCES).asText().equals(Enums.PlayerStatus.PLAY.name())) {
            MainSingleton.getInstance().guiManager.startCapturingThreads();
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
        ObjectMapper mapper = new ObjectMapper();
        JsonNode mqttmsg = mapper.readTree(message.getPayload());
        if (message.toString().contains(Constants.MQTT_START)) {
            MainSingleton.getInstance().guiManager.startCapturingThreads();
        } else if (message.toString().contains(Constants.MQTT_STOP)) {
            if (mqttmsg.get(Constants.MAC) != null) {
                String mac = mqttmsg.get(Constants.MAC).asText();
                if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac().equals(mac)) {
                    MainSingleton.getInstance().guiManager.pipelineManager.stopCapturePipeline();
                }
            }
        } else if (message.toString().contains(Constants.STATE)) {
            if (mqttmsg.get(Constants.STATE).asText().equals(Constants.OFF)) {
                MainSingleton.getInstance().guiManager.pipelineManager.stopCapturePipeline();
            }
            if (message.toString().contains(Constants.WHITE_TEMP)) {
                MainSingleton.getInstance().config.setWhiteTemperature(mqttmsg.get(Constants.WHITE_TEMP).asInt());
            }
        }
        if (mqttmsg.get(Constants.MQTT_BRIGHTNESS) != null) {
            MainSingleton.getInstance().config.setBrightness(mqttmsg.get(Constants.MQTT_BRIGHTNESS).asInt());
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
        String defaultTopic = MainSingleton.getInstance().config.getMqttTopic();
        if (!MainSingleton.getInstance().config.isMqttEnable()) {
            defaultTopic = Constants.MQTT_BASE_TOPIC;
        }
        String defaultFireflyTopic = fireflyBaseTopic + "_" + MainSingleton.getInstance().config.getMqttTopic();
        if (Constants.TOPIC_DEFAULT_MQTT.equals(MainSingleton.getInstance().config.getMqttTopic())
                || gwBaseTopic.equals(MainSingleton.getInstance().config.getMqttTopic())) {
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
            case Constants.TOPIC_SET_EMA ->
                    topic = Constants.TOPIC_SET_EMA.replace(fireflyBaseTopic, defaultFireflyTopic);
            case Constants.TOPIC_SET_FG ->
                    topic = Constants.TOPIC_SET_FG.replace(fireflyBaseTopic, defaultFireflyTopic);
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
        if (MainSingleton.getInstance().whoAmI != 1 && MainSingleton.getInstance().config.isMqttEnable()) {
            StorageManager sm = new StorageManager();
            Configuration mainConfig = sm.readMainConfig();
            return !MainSingleton.getInstance().config.getMqttTopic().equals(mainConfig.getMqttTopic());
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
        connOpts.setConnectionTimeout(Constants.MQTT_CONN_TIMEOUT);
        connOpts.setMaxInflight(Constants.MAX_INFLIGHT);
        if (MainSingleton.getInstance().config.getMqttUsername() != null && !MainSingleton.getInstance().config.getMqttUsername().isEmpty()) {
            connOpts.setUserName(MainSingleton.getInstance().config.getMqttUsername());
        }
        if (MainSingleton.getInstance().config.getMqttPwd() != null && !MainSingleton.getInstance().config.getMqttPwd().isEmpty()) {
            connOpts.setPassword(MainSingleton.getInstance().config.getMqttPwd().toCharArray());
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
     * Set effect
     */
    private static void setEffect(String message) {
        String previousEffect = MainSingleton.getInstance().config.getEffect();
        MainSingleton.getInstance().config.setEffect(message);
        CommonUtility.sleepMilliseconds(200);
        if ((Enums.Effect.BIAS_LIGHT.getBaseI18n().equals(message)
                || Enums.Effect.MUSIC_MODE_VU_METER.getBaseI18n().equals(message)
                || Enums.Effect.MUSIC_MODE_VU_METER_DUAL.getBaseI18n().equals(message)
                || Enums.Effect.MUSIC_MODE_BRIGHT.getBaseI18n().equals(message)
                || Enums.Effect.MUSIC_MODE_RAINBOW.getBaseI18n().equals(message))) {
            if (!previousEffect.equals(message)) {
                PipelineManager.restartCapture(() -> log.info("Restarting capture upon effect change."));
            }
        } else {
            if (MainSingleton.getInstance().RUNNING) {
                MainSingleton.getInstance().guiManager.stopCapturingThreads(true);
                MainSingleton.getInstance().config.setEffect(message);
                MainSingleton.getInstance().config.setToggleLed(!message.contains(Constants.OFF));
                CommonUtility.turnOnLEDs();
            }
        }
    }

    /**
     * Manage aspect ratio topic
     *
     * @param message mqtt message
     */
    private void manageAspectRatio(MqttMessage message) {
        MainSingleton.getInstance().guiManager.trayIconManager.manageAspectRatioListener(message.toString(), false);
    }

    /**
     * Manage ema topic
     *
     * @param message mqtt message
     */
    private void manageEma(MqttMessage message) {
        float alpha = LocalizedEnum.fromBaseStr(Enums.Ema.class, message.toString()).getEmaAlpha();
        int target = MainSingleton.getInstance().config.getFrameInsertionTarget();
        MainSingleton.getInstance().config.setSmoothingType(Enums.Smoothing.findByFramerateAndAlpha(target, alpha).getBaseI18n());
        PipelineManager.restartCapture(() -> MainSingleton.getInstance().config.setEmaAlpha(alpha));
    }

    /**
     * Manage FG topic
     *
     * @param message mqtt message
     */
    private void manageFg(MqttMessage message) {
        float alpha = MainSingleton.getInstance().config.getEmaAlpha();
        int target = LocalizedEnum.fromBaseStr(Enums.FrameGeneration.class, message.toString()).getFrameGenerationTarget();
        MainSingleton.getInstance().config.setSmoothingType(Enums.Smoothing.findByFramerateAndAlpha(target, alpha).getBaseI18n());
        PipelineManager.restartCapture(() -> MainSingleton.getInstance().config.setFrameInsertionTarget(target));
    }

    /**
     * Manage effect topic
     *
     * @param message message
     */
    private void manageEffect(String message) {
        if (MainSingleton.getInstance().config != null) {
            CommonUtility.delayMilliseconds(() -> {
                log.info("Setting mode via MQTT - {}", message);
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
        if (MainSingleton.getInstance().config != null) {
            CommonUtility.delayMilliseconds(() -> {
                if (message.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                    NativeExecutor.restartNativeInstance(null);
                } else {
                    NativeExecutor.restartNativeInstance(message);
                }
            }, 200);
        }
    }

    /**
     * Manage firmware config topic
     * No swap because that topic needs MAC, no need to swap topic. Some topics are HTTP only via IP.
     *
     * @param message message
     */
    private void manageFirmwareConfig(String message) throws JsonProcessingException {
        if (MainSingleton.getInstance().config != null) {
            if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode mqttmsg = mapper.readTree(message);
                if (CommonUtility.getDeviceToUse().getMac().equals(mqttmsg.get(Constants.MAC).asText())) {
                    MainSingleton.getInstance().config.setColorMode(mqttmsg.get(Constants.COLOR_MODE).asInt());
                }
            }
            log.debug(message);
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
            MainSingleton.getInstance().config.setGamma(Double.parseDouble(gammaObj.get(Constants.MQTT_GAMMA).asText()));
        }
    }

    /**
     * Restart MQTT instance once mqtt upgrade msg has been received
     *
     * @param message mqtt message
     */
    private void restartMqttInstance(MqttMessage message) {
        if (ManagerSingleton.getInstance().deviceNameForSerialDevice.equals(message.toString())
                || ManagerSingleton.getInstance().deviceNameForSerialDevice.equals(message + Constants.CDC_DEVICE)) {
            log.info("Update successfull MQTT msg received={}", message);
            CommonUtility.delaySeconds(MainSingleton.getInstance().guiManager::startCapturingThreads, 60);
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
        if (scheduledMqttFuture == null || scheduledMqttFuture.isDone()) {
            scheduledMqttFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (!connected && !isRestartingMqtt) {
                    try {
                        isRestartingMqtt = true;
                        // if long disconnection, reconfigure microcontroller
                        long duration = new Date().getTime() - lastActivity.getTime();
                        lastActivity = new Date();
                        if (TimeUnit.MILLISECONDS.toSeconds(duration) > 60) {
                            log.info("Long disconnection occurred");
                            NativeExecutor.restartNativeInstance();
                        }
                        ManagerSingleton.getInstance().client.setCallback(this);
                        subscribeToTopics();
                        connected = true;
                        log.info(Constants.MQTT_RECONNECTED);
                        PipelineManager.restartCapture(() -> log.info("Restarting upon disconnection."));
                        scheduledExecutorService.shutdown();
                    } catch (MqttException e) {
                        log.error(Constants.MQTT_DISCONNECTED);
                    } finally {
                        isRestartingMqtt = false;
                    }
                }
            }, 0, 10, TimeUnit.SECONDS);
        }
    }

    /**
     * Subscribe to topics
     *
     * @throws MqttException can't subscribe
     */
    void subscribeToTopics() throws MqttException {
        ManagerSingleton.getInstance().client.subscribe(getTopic(Constants.TOPIC_DEFAULT_MQTT));
        ManagerSingleton.getInstance().client.subscribe(getTopic(Constants.TOPIC_DEFAULT_MQTT_STATE));
        ManagerSingleton.getInstance().client.subscribe(getTopic(Constants.TOPIC_UPDATE_RESULT_MQTT));
        ManagerSingleton.getInstance().client.subscribe(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_GAMMA));
        ManagerSingleton.getInstance().client.subscribe(getTopic(Constants.TOPIC_SET_EMA));
        ManagerSingleton.getInstance().client.subscribe(getTopic(Constants.TOPIC_SET_FG));
        ManagerSingleton.getInstance().client.subscribe(getTopic(Constants.TOPIC_SET_ASPECT_RATIO));
        ManagerSingleton.getInstance().client.subscribe(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_EFFECT));
        ManagerSingleton.getInstance().client.subscribe(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_PROFILE_SET));
        ManagerSingleton.getInstance().client.subscribe(Constants.TOPIC_GLOW_WORM_FIRM_CONFIG);
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
        log.trace("Received on topic={}\n{}", topic, message.toString());
        lastActivity = new Date();
        if (topic.equals(getTopic(Constants.TOPIC_DEFAULT_MQTT_STATE))) {
            manageDefaultTopic(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_UPDATE_RESULT_MQTT))) {
            restartMqttInstance(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_DEFAULT_MQTT))) {
            manageMqttSetTopic(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_FIREFLY_LUCIFERIN_GAMMA))) {
            manageGamma(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_SET_ASPECT_RATIO))) {
            manageAspectRatio(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_SET_EMA))) {
            manageEma(message);
        } else if (topic.equals(getTopic(Constants.TOPIC_SET_FG))) {
            manageFg(message);
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
        ManagerSingleton.getInstance().client = new MqttClient(MainSingleton.getInstance().config.getMqttServer(), mqttDeviceName, persistence);
        MqttConnectOptions connOpts = getMqttConnectOptions();
        ManagerSingleton.getInstance().client.connect(connOpts);
        ManagerSingleton.getInstance().client.setCallback(this);
        if (firstConnection) {
            // Wait that the device is engaged before updating MQTT discovery entities.
            ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
            es.scheduleAtFixedRate(() -> {
                if (!MainSingleton.getInstance().isInitialized() && CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac() != null && !CommonUtility.getDeviceToUse().getMac().isEmpty()) {
                    CommonUtility.turnOnLEDs();
                    if (ManagerSingleton.getInstance().updateMqttDiscovery) {
                        NetworkTabController.publishDiscoveryTopics(false);
                        NetworkTabController.publishDiscoveryTopics(true);
                        log.debug("MQTT discovery: entities has been updated");
                        es.shutdownNow();
                    }
                    MainSingleton.getInstance().setInitialized(true);
                }
            }, 0, 2, TimeUnit.SECONDS);
        }
        subscribeToTopics();
        log.info(Constants.MQTT_CONNECTED);
        connected = true;
    }

}