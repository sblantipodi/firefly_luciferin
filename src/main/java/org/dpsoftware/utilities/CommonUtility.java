/*
  CommonUtility.java

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
package org.dpsoftware.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Node;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.controllers.DevicesTabController;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.SerialManager;
import org.dpsoftware.managers.UpgradeManager;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.LedMatrixInfo;
import org.dpsoftware.managers.dto.StateDto;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.dpsoftware.FireflyLuciferin.config;

/**
 * CommonUtility class for useful methods
 */
@Slf4j
public class CommonUtility {

    public static int wifiStrength = 0;
    public static int ldrStrength = 0;

    /**
     * From Java Object to JSON String, useful to handle checked exceptions in lambdas
     *
     * @param obj generic Java object
     * @return JSON String
     */
    public static String toJsonStringPrettyPrinted(Object obj) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return "";
    }

    /**
     * From Java Object to JSON String, useful to handle checked exceptions in lambdas
     *
     * @param obj generic Java object
     * @return JSON String
     */
    public static String toJsonString(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return "";
    }

    /**
     * Transform JSON String to a JsonNode
     *
     * @param jsonString JSON string
     * @return JsonNode object
     */
    @SuppressWarnings("unused")
    public static JsonNode fromJsonToObject(String jsonString) {
        try {
            ObjectMapper jacksonObjMapper = new ObjectMapper();
            return jacksonObjMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            log.trace("Non JSON String, skipping: " + jsonString);
        }
        return null;
    }

    /**
     * Return current device infos, Serial or Wireless.
     *
     * @return device infos
     */
    public static GlowWormDevice getDeviceToUse() {
        GlowWormDevice glowWormDeviceToUse = new GlowWormDevice();
        // MQTT Stream
        if (FireflyLuciferin.config.isWirelessStream()) {
            if (!FireflyLuciferin.config.getOutputDevice().equals(Constants.SERIAL_PORT_AUTO) || FireflyLuciferin.config.getMultiMonitor() > 1) {
                glowWormDeviceToUse = DevicesTabController.deviceTableData.stream()
                        .filter(glowWormDevice -> glowWormDevice.getDeviceName().equals(FireflyLuciferin.config.getOutputDevice()))
                        .findAny().orElse(null);
            } else if (DevicesTabController.deviceTableData != null && DevicesTabController.deviceTableData.size() > 0) {
                glowWormDeviceToUse = DevicesTabController.deviceTableData.get(0);
            }
        } else if (FireflyLuciferin.config.isFullFirmware()) { // MQTT Enabled
            // Waiting both MQTT and serial device
            GlowWormDevice glowWormDeviceSerial = DevicesTabController.deviceTableData.stream()
                    .filter(glowWormDevice -> glowWormDevice.getDeviceName().equals(Constants.USB_DEVICE))
                    .findAny().orElse(null);
            if (glowWormDeviceSerial != null && glowWormDeviceSerial.getMac() != null) {
                glowWormDeviceToUse = DevicesTabController.deviceTableData.stream()
                        .filter(glowWormDevice -> glowWormDevice.getMac().equals(glowWormDeviceSerial.getMac()))
                        .filter(glowWormDevice -> !glowWormDevice.getDeviceName().equals(Constants.USB_DEVICE))
                        .findAny().orElse(null);
            }
        } else { // Serial only
            glowWormDeviceToUse = DevicesTabController.deviceTableData.stream()
                    .filter(glowWormDevice -> glowWormDevice.getDeviceName().equals(Constants.USB_DEVICE))
                    .findAny().orElse(null);
        }
        return glowWormDeviceToUse;
    }

    /**
     * Check if is single device main instance
     *
     * @return true or false
     */
    public static boolean isSingleDeviceMainInstance() {
        return FireflyLuciferin.config != null && FireflyLuciferin.config.isMultiScreenSingleDevice() && FireflyLuciferin.config.getMultiMonitor() > 1 && JavaFXStarter.whoAmI == 1;
    }

    /**
     * Check if is single device other instance
     *
     * @return true or false
     */
    public static boolean isSingleDeviceOtherInstance() {
        return FireflyLuciferin.config != null && FireflyLuciferin.config.isMultiScreenSingleDevice() && FireflyLuciferin.config.getMultiMonitor() > 1 && JavaFXStarter.whoAmI > 1;
    }

    /**
     * True if is MultiScreenSingleDevice radio is selected and if it's running in a multi monitor setup
     *
     * @return true or false
     */
    public static boolean isSingleDeviceMultiScreen() {
        return FireflyLuciferin.config != null && FireflyLuciferin.config.isMultiScreenSingleDevice() && FireflyLuciferin.config.getMultiMonitor() > 1;
    }

    /**
     * BLOCKING: Sleep current thread
     *
     * @param numberOfSeconds to sleep
     */
    @SuppressWarnings("unused")
    public static void sleepSeconds(int numberOfSeconds) {
        try {
            TimeUnit.SECONDS.sleep(numberOfSeconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * BLOCKING: Sleep current thread
     *
     * @param numberOfMilliseconds to sleep
     */
    public static void sleepMilliseconds(int numberOfMilliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(numberOfMilliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * NON BLOCKING: Delay a callable method using a single thread executor
     *
     * @param callable function to call after the delay
     * @param delay    time to delay
     */
    @NonNull
    @SuppressWarnings("all")
    public static <V> ScheduledFuture<V> delaySeconds(@NonNull Callable<V> callable, int delay) {
        try {
            return Executors.newSingleThreadScheduledExecutor().schedule(callable, delay, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    /**
     * NON BLOCKING: Delay a Runnable command using a single thread executor
     *
     * @param command function to call after the delay
     * @param delay   time to delay
     */
    @NonNull
    @SuppressWarnings("all")
    public static ScheduledFuture<?> delaySeconds(Runnable command, long delay) {
        try {
            return Executors.newSingleThreadScheduledExecutor().schedule(command, delay, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    /**
     * Delay a callable method
     *
     * @param callable function to call after the delay
     * @param delay    time to delay
     */
    @NonNull
    @SuppressWarnings("all")
    public static <V> ScheduledFuture<V> delayMilliseconds(@NonNull Callable<V> callable, int delay) {
        try {
            return Executors.newSingleThreadScheduledExecutor().schedule(callable, delay, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    /**
     * Delay a Runnable command
     *
     * @param command function to call after the delay
     * @param delay   time to delay
     */
    @NonNull
    @SuppressWarnings("all")
    public static ScheduledFuture<?> delayMilliseconds(Runnable command, long delay) {
        try {
            return Executors.newSingleThreadScheduledExecutor().schedule(command, delay, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    /**
     * Calculate brightness based on the night mode
     *
     * @return conditioned brightness
     */
    public static int getNightBrightness() {
        float nightBrightness = Float.parseFloat(FireflyLuciferin.config.getNightModeBrightness().replace("%", ""));
        if (FireflyLuciferin.nightMode && nightBrightness > 0) {
            nightBrightness = (int) (FireflyLuciferin.config.getBrightness() * (1 - (nightBrightness / 100)));
        } else {
            nightBrightness = FireflyLuciferin.config.getBrightness();
        }
        return (int) nightBrightness;
    }

    /**
     * Scale a number based on the OS scaling setting
     *
     * @param numberToScale number that should be scaled based on the OS scaling setting
     * @param scaleRatio    OS scaling
     * @return scaled number
     */
    public static int scaleDownResolution(int numberToScale, int scaleRatio) {
        return (numberToScale * 100) / scaleRatio;
    }

    /**
     * Add device to the connected device table
     *
     * @param actualObj JSON node
     */
    public static void addDevice(JsonNode actualObj) {
        try {
            if (actualObj.get(Constants.BAUD_RATE) != null) {
                log.debug(CommonUtility.toJsonStringPrettyPrinted(actualObj));
                boolean validBaudRate = Integer.parseInt(actualObj.get(Constants.BAUD_RATE).toString()) >= 1
                        && Integer.parseInt(actualObj.get(Constants.BAUD_RATE).toString()) <= 8;
                if (FireflyLuciferin.config.getMultiMonitor() == 1 && (FireflyLuciferin.config.getOutputDevice() == null
                        || FireflyLuciferin.config.getOutputDevice().isEmpty()
                        || FireflyLuciferin.config.getOutputDevice().equals(Constants.SERIAL_PORT_AUTO))) {
                    if (FireflyLuciferin.config.isWirelessStream()) {
                        FireflyLuciferin.config.setOutputDevice(actualObj.get(Constants.MQTT_DEVICE_NAME).textValue());
                    } else {
                        if (DevicesTabController.deviceTableData != null && DevicesTabController.deviceTableData.size() > 0) {
                            FireflyLuciferin.config.setOutputDevice(DevicesTabController.deviceTableData.get(0).getDeviceIP());
                        }
                    }
                }
                if (CommonUtility.isSingleDeviceMultiScreen() && (FireflyLuciferin.config.getOutputDevice().isEmpty()
                        || FireflyLuciferin.config.getOutputDevice().equals(Constants.SERIAL_PORT_AUTO))) {
                    FireflyLuciferin.config.setOutputDevice(actualObj.get(Constants.MQTT_DEVICE_NAME).textValue());
                }
                if (DevicesTabController.deviceTableData != null) {
                    if (actualObj.get(Constants.WIFI) == null) {
                        wifiStrength = actualObj.get(Constants.WIFI) != null ? actualObj.get(Constants.WIFI).asInt() : 0;
                    }
                    if (actualObj.get(Constants.MQTT_LDR_VALUE) == null) {
                        wifiStrength = actualObj.get(Constants.MQTT_LDR_VALUE) != null ? actualObj.get(Constants.MQTT_LDR_VALUE).asInt() : 0;
                    }
                    String deviceColorMode = Constants.DASH;
                    int deviceColorModeInt = 0;
                    int deviceColorOrderInt = 1;
                    if ((actualObj.get(Constants.COLOR) != null && actualObj.get(Constants.COLOR).get(Constants.COLOR_MODE) != null)) {
                        deviceColorModeInt = actualObj.get(Constants.COLOR).get(Constants.COLOR_MODE).asInt();
                        deviceColorMode = Enums.ColorMode.values()[deviceColorModeInt - 1].getI18n();
                        if (actualObj.get(Constants.COLOR).get(Constants.COLOR_ORDER) != null) {
                            deviceColorOrderInt = actualObj.get(Constants.COLOR).get(Constants.COLOR_ORDER).asInt();
                        }
                    }
                    String deviceVer = (actualObj.get(Constants.DEVICE_VER).textValue());
                    String deviceBoard = actualObj.get(Constants.DEVICE_BOARD) == null ? Constants.DASH : actualObj.get(Constants.DEVICE_BOARD).textValue();
                    if (config.isCheckForUpdates() && Enums.SupportedDevice.ESP32_S3_CDC.name().equals(deviceBoard)) {
                        deviceVer = Constants.FORCE_FIRMWARE_AUTO_UPGRADE;
                    }
                    DevicesTabController.deviceTableData.add(new GlowWormDevice(actualObj.get(Constants.MQTT_DEVICE_NAME).textValue(),
                            actualObj.get(Constants.STATE_IP).textValue(),
                            actualObj.get(Constants.STATE_DHCP) != null && actualObj.get(Constants.STATE_DHCP).asBoolean(),
                            (actualObj.get(Constants.WIFI) == null ? Constants.DASH : actualObj.get(Constants.WIFI) + Constants.PERCENT),
                            deviceVer,
                            deviceBoard,
                            (actualObj.get(Constants.MAC) == null ? Constants.DASH : actualObj.get(Constants.MAC).textValue()),
                            (actualObj.get(Constants.GPIO) == null ? Constants.DASH : actualObj.get(Constants.GPIO).toString()),
                            (actualObj.get(Constants.NUMBER_OF_LEDS) == null ? Constants.DASH : actualObj.get(Constants.NUMBER_OF_LEDS).textValue()),
                            (FireflyLuciferin.formatter.format(new Date())),
                            Enums.FirmwareType.FULL.name(),
                            (((actualObj.get(Constants.BAUD_RATE) == null) || !validBaudRate) ? Constants.DASH :
                                    Enums.BaudRate.findByValue(Integer.parseInt(actualObj.get(Constants.BAUD_RATE).toString())).getBaudRate()),
                            (actualObj.get(Constants.MQTT_TOPIC) == null ? FireflyLuciferin.config.isFullFirmware() ? Constants.MQTT_BASE_TOPIC : Constants.DASH
                                    : actualObj.get(Constants.MQTT_TOPIC).textValue()), deviceColorMode,
                            Enums.ColorOrder.findByValue(deviceColorOrderInt).name(),
                            (actualObj.get(Constants.MQTT_LDR_VALUE) == null ? Constants.DASH : actualObj.get(Constants.MQTT_LDR_VALUE).asInt() + Constants.PERCENT),
                            (actualObj.get(Constants.HTTP_LDR_RELAYPIN) == null ? Constants.DASH : actualObj.get(Constants.HTTP_LDR_RELAYPIN).toString()),
                            (actualObj.get(Constants.HTTP_LDR_SBPIN) == null ? Constants.DASH : actualObj.get(Constants.HTTP_LDR_SBPIN).toString()),
                            (actualObj.get(Constants.HTTP_LDR_LDRPIN) == null ? Constants.DASH : actualObj.get(Constants.HTTP_LDR_LDRPIN).toString()),
                            (actualObj.get(Constants.GPIO_CLOCK) == null ? Constants.DASH : actualObj.get(Constants.GPIO_CLOCK).toString())));
                    if (CommonUtility.getDeviceToUse() != null && actualObj.get(Constants.MAC) != null) {
                        if (CommonUtility.getDeviceToUse().getMac().equals(actualObj.get(Constants.MAC).textValue())) {
                            if (actualObj.get(Constants.WHITE_TEMP) != null) {
                                FireflyLuciferin.config.setWhiteTemperature(actualObj.get(Constants.WHITE_TEMP).asInt());
                            }
                            FireflyLuciferin.config.setColorMode(deviceColorModeInt);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("Can't add device, the instance is probably running.");
            log.error(e.getMessage());
        }
    }

    /**
     * Update device table with the received message
     *
     * @param mqttmsg device message
     */
    public static void updateDeviceTable(JsonNode mqttmsg) {
        String freshDeviceName = mqttmsg.get(Constants.MQTT_DEVICE_NAME).textValue();
        if (DevicesTabController.deviceTableData.isEmpty()) {
            CommonUtility.addDevice(mqttmsg);
        } else {
            AtomicBoolean isDevicePresent = new AtomicBoolean(false);
            DevicesTabController.deviceTableData.forEach(glowWormDevice -> {
                if (glowWormDevice.getDeviceName().equals(freshDeviceName)) {
                    isDevicePresent.set(true);
                    glowWormDevice.setLastSeen(FireflyLuciferin.formatter.format(new Date()));
                    if (mqttmsg.get(Constants.GPIO) != null) {
                        glowWormDevice.setGpio(mqttmsg.get(Constants.GPIO).toString());
                    }
                    if (mqttmsg.get(Constants.GPIO_CLOCK) != null) {
                        glowWormDevice.setGpioClock(mqttmsg.get(Constants.GPIO_CLOCK).toString());
                    }
                    if (mqttmsg.get(Constants.WIFI) != null) {
                        CommonUtility.wifiStrength = mqttmsg.get(Constants.WIFI) != null ? mqttmsg.get(Constants.WIFI).asInt() : 0;
                        glowWormDevice.setWifi(mqttmsg.get(Constants.WIFI).asInt() + Constants.PERCENT);
                    }
                    if (mqttmsg.get(Constants.STATE_IP) != null) {
                        glowWormDevice.setDeviceIP(mqttmsg.get(Constants.STATE_IP).textValue());
                    }
                    if (mqttmsg.get(Constants.WHITE_TEMP) != null) {
                        if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac().equals(glowWormDevice.getMac())) {
                            FireflyLuciferin.config.setWhiteTemperature(mqttmsg.get(Constants.WHITE_TEMP).asInt());
                        }
                    }
                    if (mqttmsg.get(Constants.MQTT_LDR_VALUE) != null) {
                        CommonUtility.ldrStrength = mqttmsg.get(Constants.MQTT_LDR_VALUE) != null ? mqttmsg.get(Constants.MQTT_LDR_VALUE).asInt() : 0;
                        glowWormDevice.setLdrValue(mqttmsg.get(Constants.MQTT_LDR_VALUE).asInt() + Constants.PERCENT);
                    }
                    if (mqttmsg.get(Constants.BAUD_RATE) != null) {
                        glowWormDevice.setBaudRate(Enums.BaudRate.findByValue(mqttmsg.get(Constants.BAUD_RATE).intValue()).getBaudRate());
                    }
                    if (mqttmsg.get(Constants.COLOR) != null && mqttmsg.get(Constants.COLOR).get(Constants.COLOR_MODE) != null) {
                        int tempColorMode = mqttmsg.get(Constants.COLOR).get(Constants.COLOR_MODE).asInt();
                        glowWormDevice.setColorMode(Enums.ColorMode.values()[tempColorMode - 1].getI18n());
                        if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac().equals(glowWormDevice.getMac())) {
                            FireflyLuciferin.config.setColorMode(tempColorMode);
                        }
                        int tempColorOrder = mqttmsg.get(Constants.COLOR).get(Constants.COLOR_ORDER).asInt();
                        glowWormDevice.setColorOrder(Enums.ColorOrder.findByValue(tempColorOrder).name());
                        if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getMac().equals(glowWormDevice.getMac())) {
                            FireflyLuciferin.colorOrder = tempColorOrder;
                        }
                    }
                    if (mqttmsg.get(Constants.NUMBER_OF_LEDS) != null) {
                        glowWormDevice.setNumberOfLEDSconnected(mqttmsg.get(Constants.NUMBER_OF_LEDS).asText());
                    }
                    if (mqttmsg.get(Constants.HTTP_LDR_RELAYPIN) != null) {
                        glowWormDevice.setRelayPin(mqttmsg.get(Constants.HTTP_LDR_RELAYPIN).asText());
                    }
                    if (mqttmsg.get(Constants.HTTP_LDR_SBPIN) != null) {
                        glowWormDevice.setSbPin(mqttmsg.get(Constants.HTTP_LDR_SBPIN).asText());
                    }
                    if (mqttmsg.get(Constants.HTTP_LDR_LDRPIN) != null) {
                        glowWormDevice.setLdrPin(mqttmsg.get(Constants.HTTP_LDR_LDRPIN).asText());
                    }
                    if (mqttmsg.get(Constants.STATE_DHCP) != null) {
                        glowWormDevice.setDhcpInUse(mqttmsg.get(Constants.STATE_DHCP).asBoolean());
                    }
                    if (mqttmsg.get(Constants.DEVICE_BOARD) != null) {
                        glowWormDevice.setDeviceBoard(mqttmsg.get(Constants.DEVICE_BOARD).asText());
                    }
                    if (mqttmsg.get(Constants.DEVICE_VER) != null) {
                        String deviceVer = (mqttmsg.get(Constants.DEVICE_VER).textValue());
                        if (config.isCheckForUpdates() && Enums.SupportedDevice.ESP32_S3_CDC.name().equals(glowWormDevice.getDeviceBoard())) {
                            deviceVer = Constants.FORCE_FIRMWARE_AUTO_UPGRADE;
                        }
                        glowWormDevice.setDeviceVersion(deviceVer);
                    }
                }
            });
            if (!isDevicePresent.get()) {
                CommonUtility.addDevice(mqttmsg);
            }
        }
        if (UpgradeManager.deviceNameForSerialDevice.isEmpty()) {
            GlowWormDevice mqttDeviceInUse = CommonUtility.getDeviceToUse();
            if (mqttDeviceInUse != null) {
                UpgradeManager.deviceNameForSerialDevice = mqttDeviceInUse.getDeviceName();
                if (Enums.SupportedDevice.ESP32_S3_CDC.name().equals(mqttDeviceInUse.getDeviceBoard())) {
                    UpgradeManager.deviceNameForSerialDevice += Constants.CDC_DEVICE;
                }
            }
        }
    }

    /**
     * Update device table using FPS topic
     * if there is no device in the deviceTableData but I received an update on the FPS topic,
     * and the device is in the deviceTableDataTemp, add it to the deviceTableData
     *
     * @param fpsTopicMsg json node
     */
    public static void updateFpsWithFpsTopic(JsonNode fpsTopicMsg) {
        if (fpsTopicMsg.get(Constants.MAC) != null) {
            String macToUpdate = fpsTopicMsg.get(Constants.MAC).textValue();
            List<GlowWormDevice> matchingDevice = DevicesTabController.deviceTableData.stream()
                    .filter(p -> p.getMac().equals(macToUpdate)).toList();
            if (matchingDevice.isEmpty()) {
                List<GlowWormDevice> matchingDeviceTemp = DevicesTabController.deviceTableDataTemp.stream()
                        .filter(p -> p.getMac().equals(macToUpdate)).toList();
                if (!matchingDeviceTemp.isEmpty()) {
                    log.info("Known device, adding to the device table.");
                    DevicesTabController.deviceTableData.addAll(matchingDeviceTemp);
                    DevicesTabController.deviceTableDataTemp.removeIf(e -> e.getMac().equals(macToUpdate));
                }
            }
            DevicesTabController.deviceTableData.forEach(glowWormDevice -> {
                if (glowWormDevice.getMac().equals(macToUpdate)) {
                    glowWormDevice.setLastSeen(FireflyLuciferin.formatter.format(new Date()));
                    glowWormDevice.setNumberOfLEDSconnected(fpsTopicMsg.get(Constants.NUMBER_OF_LEDS).textValue());
                    if (fpsTopicMsg.get(Constants.MQTT_LDR_VALUE) != null) {
                        glowWormDevice.setLdrValue(fpsTopicMsg.get(Constants.MQTT_LDR_VALUE).asInt() + Constants.PERCENT);
                        CommonUtility.ldrStrength = fpsTopicMsg.get(Constants.MQTT_LDR_VALUE) != null ? fpsTopicMsg.get(Constants.MQTT_LDR_VALUE).asInt() : 0;
                    }
                    if (glowWormDevice.getDeviceName().equals(FireflyLuciferin.config.getOutputDevice()) || glowWormDevice.getDeviceIP().equals(FireflyLuciferin.config.getOutputDevice())) {
                        FireflyLuciferin.FPS_GW_CONSUMER = Float.parseFloat(fpsTopicMsg.get(Constants.MQTT_TOPIC_FRAMERATE).asText());
                        CommonUtility.wifiStrength = fpsTopicMsg.get(Constants.WIFI) != null ? fpsTopicMsg.get(Constants.WIFI).asInt() : 0;
                    }
                }
            });
        }
    }

    /**
     * Update device table using Device topic
     *
     * @param mqttmsg msg from the topic
     */
    public static void updateFpsWithDeviceTopic(JsonNode mqttmsg) {
        if (mqttmsg.get(Constants.MQTT_TOPIC_FRAMERATE) != null) {
            String macToUpdate = mqttmsg.get(Constants.MAC).asText();
            DevicesTabController.deviceTableData.forEach(glowWormDevice -> {
                if (glowWormDevice.getMac().equals(macToUpdate)) {
                    if (glowWormDevice.getDeviceName().equals(FireflyLuciferin.config.getOutputDevice()) || glowWormDevice.getDeviceIP().equals(FireflyLuciferin.config.getOutputDevice())) {
                        FireflyLuciferin.FPS_GW_CONSUMER = Float.parseFloat(mqttmsg.get(Constants.MQTT_TOPIC_FRAMERATE).asText());
                    }
                }
            });
        }
    }

    /**
     * Turn ON LEDs when Luciferin starts or on profile switch
     */
    public static void turnOnLEDs() {
        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, FireflyLuciferin.config.getEffect());
        if (!Enums.Effect.BIAS_LIGHT.equals(effectInUse)
                && !Enums.Effect.MUSIC_MODE_VU_METER.equals(effectInUse)
                && !Enums.Effect.MUSIC_MODE_VU_METER_DUAL.equals(effectInUse)
                && !Enums.Effect.MUSIC_MODE_BRIGHT.equals(effectInUse)
                && !Enums.Effect.MUSIC_MODE_RAINBOW.equals(effectInUse)) {
            if (FireflyLuciferin.config.isToggleLed()) {
                String[] color = FireflyLuciferin.config.getColorChooser().split(",");
                if (FireflyLuciferin.config.isFullFirmware()) {
                    StateDto stateDto = new StateDto();
                    stateDto.setState(Constants.ON);
                    stateDto.setEffect(FireflyLuciferin.config.getEffect());
                    ColorDto colorDto = new ColorDto();
                    colorDto.setR(Integer.parseInt(color[0]));
                    colorDto.setG(Integer.parseInt(color[1]));
                    colorDto.setB(Integer.parseInt(color[2]));
                    stateDto.setColor(colorDto);
                    stateDto.setBrightness(CommonUtility.getNightBrightness());
                    if (CommonUtility.getDeviceToUse() != null) {
                        stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
                    }
                    NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.DEFAULT_MQTT_TOPIC), CommonUtility.toJsonString(stateDto));
                } else {
                    SerialManager serialManager = new SerialManager();
                    serialManager.sendSerialParams(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]));
                }
            } else {
                if (FireflyLuciferin.config.isFullFirmware()) {
                    StateDto stateDto = new StateDto();
                    stateDto.setState(Constants.OFF);
                    stateDto.setEffect(Constants.SOLID);
                    stateDto.setBrightness(CommonUtility.getNightBrightness());
                    if (CommonUtility.getDeviceToUse() != null) {
                        stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
                    }
                    NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.DEFAULT_MQTT_TOPIC), CommonUtility.toJsonString(stateDto));
                } else {
                    SerialManager serialManager = new SerialManager();
                    serialManager.sendSerialParams(0, 0, 0);
                }
            }
        }
    }

    /**
     * Turn OFF LEDs
     *
     * @param currentConfig stored config
     */
    public static void turnOffLEDs(Configuration currentConfig) {
        turnOffLEDs(currentConfig, 0);
    }

    /**
     * Turn OFF LEDs
     *
     * @param currentConfig stored config
     * @param qos           quality of service, 0 and 1 are supported, 2 is not supported. 0 is default.
     */
    public static void turnOffLEDs(Configuration currentConfig, int qos) {
        if (currentConfig != null) {
            if (FireflyLuciferin.RUNNING && !NativeExecutor.exitTriggered) {
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
            }
            if (!NativeExecutor.exitTriggered) {
                CommonUtility.sleepMilliseconds(100);
            }
            if (currentConfig.isFullFirmware()) {
                StateDto stateDto = new StateDto();
                stateDto.setState(Constants.OFF);
                stateDto.setEffect(Constants.SOLID);
                stateDto.setBrightness(CommonUtility.getNightBrightness());
                stateDto.setWhitetemp(FireflyLuciferin.config.getWhiteTemperature());
                if (CommonUtility.getDeviceToUse() != null) {
                    stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
                }
                NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.DEFAULT_MQTT_TOPIC), CommonUtility.toJsonString(stateDto), false, false, qos);
            } else {
                java.awt.Color[] leds = new java.awt.Color[1];
                try {
                    leds[0] = new java.awt.Color(0, 0, 0);
                    FireflyLuciferin.config.setBrightness(CommonUtility.getNightBrightness());
                    SerialManager serialManager = new SerialManager();
                    serialManager.sendColorsViaUSB(leds);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Check if a String contains an integer
     *
     * @param strNum string to check
     * @return if is a number or not
     */
    @SuppressWarnings("unused")
    public static boolean isInteger(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Get localized string
     *
     * @param key resource bundle key
     * @return localized String
     */
    public static String getWord(String key) {
        try {
            return FireflyLuciferin.bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Get localized string
     *
     * @param key    resource bundle key
     * @param locale locale to use
     * @return localized String
     */
    public static String getWord(String key, Locale locale) {
        try {
            return ResourceBundle.getBundle(Constants.MSG_BUNDLE, locale).getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Return true if slit bottom row is disabled (equals 0%)
     *
     * @param splitBottomMargin split bottom row value
     * @return boolean
     */
    public static boolean isSplitBottomRow(String splitBottomMargin) {
        return Integer.parseInt(splitBottomMargin.replace(Constants.PERCENT, "")) > 0;
    }

    /**
     * Set group based on the minimum number of LEDs in a row.
     *
     * @param ledMatrixInfo infos used to create led matrix
     */
    public static void groupByCalc(LedMatrixInfo ledMatrixInfo) {
        TreeSet<Integer> ledCollection = new TreeSet<>();
        ledCollection.add(ledMatrixInfo.getTopLedOriginal());
        ledCollection.add(ledMatrixInfo.getRightLedOriginal());
        ledCollection.add(ledMatrixInfo.getLeftLedOriginal());
        if (CommonUtility.isSplitBottomRow(ledMatrixInfo.getSplitBottomRow())) {
            ledCollection.add(ledMatrixInfo.getBottomLeftLedOriginal());
            ledCollection.add(ledMatrixInfo.getBottomRightLedOriginal());
        } else {
            ledCollection.add(ledMatrixInfo.getBottomRowLedOriginal());
        }
        int i = ledCollection.first();
        if (i == 0 && ledCollection.size() >= 2) {
            i = (int) ledCollection.toArray()[1];
        }
        if (i > 0) {
            ledMatrixInfo.setMinimumNumberOfLedsInARow(i);
            ledMatrixInfo.setTotaleNumOfLeds(ledCollection.stream().mapToInt(Integer::intValue).sum());
            if (ledMatrixInfo.getMinimumNumberOfLedsInARow() < ledMatrixInfo.getGroupBy()) {
                ledMatrixInfo.setGroupBy(ledMatrixInfo.getMinimumNumberOfLedsInARow());
            }
        } else {
            ledMatrixInfo.setTopLed(1);
            ledMatrixInfo.setTopLedOriginal(1);
            ledMatrixInfo.setGroupBy(1);
        }
    }

    /**
     * Capitalize
     *
     * @param str to capitalize
     * @return capitalized string
     */
    public static String capitalize(String str) {
        if (str == null || str.length() == 0) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Close current stage based on InputEvent
     *
     * @param e input event from a generic stage
     */
    public static void closeCurrentStage(InputEvent e) {
        Node source = (Node) e.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    /**
     * Calculate borders for fit to screen, 4:3, 16:9, 21:9, 32:9
     *
     * @param screenWidth  screen width
     * @param screenHeight screen height
     */
    public static Enums.MonitorAspectRatio checkMonitorAspectRatio(int screenWidth, int screenHeight) {
        double aspectRatio = Math.round(((double) screenWidth / (double) screenHeight) * 10) / 10.00; // Round aspect ratio to 2 decimals
        if (aspectRatio >= 1.2 && aspectRatio <= 1.4) { // standard 4:3
            return Enums.MonitorAspectRatio.AR_43;
        } else if (aspectRatio >= 1.6 && aspectRatio <= 1.8) { // widescreen 16:9
            return Enums.MonitorAspectRatio.AR_169;
        } else if (aspectRatio >= 2.1 && aspectRatio <= 2.5) { // ultra wide screen 21:9
            return Enums.MonitorAspectRatio.AR_219;
        } else if (aspectRatio > 2.5 && aspectRatio <= 3.7) { // ultra wide screen 32:9
            return Enums.MonitorAspectRatio.AR_329;
        } else {
            return Enums.MonitorAspectRatio.AR_169; // default
        }
    }

    /**
     * Remove all chars from a String but numbers
     *
     * @param str string containing chars and numbers
     * @return numbers
     */
    public static String removeChars(String str) {
        return str.replaceAll("\\D", "").replaceFirst("^0+(?!$)", "");
    }

}
