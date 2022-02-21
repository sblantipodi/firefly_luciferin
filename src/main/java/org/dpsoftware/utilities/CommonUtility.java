/*
  CommonUtility.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.controllers.DevicesTabController;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.UpgradeManager;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.StateDto;

import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CommonUtility class for useful methods
 */
@Slf4j
public class CommonUtility {

    public static int wifiStrength = 0;

    /**
     * From Java Object to JSON String, useful to handle checked exceptions in lambdas
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
     * @param jsonString JSON string
     * @return JsonNode object
     */
    @SuppressWarnings("unused")
    public static JsonNode fromJsonToObject(String jsonString) {

        try {
            ObjectMapper jacksonObjMapper = new ObjectMapper();
            return jacksonObjMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            log.error("Non JSON String: " + jsonString);
        }
        return null;

    }

    /**
     * Return current device infos, Serial or Wireless.
     * @return device infos
     */
    public static GlowWormDevice getDeviceToUse() {

        GlowWormDevice glowWormDeviceToUse = new GlowWormDevice();
        // MQTT Stream
        if (FireflyLuciferin.config.isMqttStream()) {
            if (!FireflyLuciferin.config.getSerialPort().equals(Constants.SERIAL_PORT_AUTO) || FireflyLuciferin.config.getMultiMonitor() > 1) {
                glowWormDeviceToUse = DevicesTabController.deviceTableData.stream()
                        .filter(glowWormDevice -> glowWormDevice.getDeviceName().equals(FireflyLuciferin.config.getSerialPort()))
                        .findAny().orElse(null);
            } else if (DevicesTabController.deviceTableData != null && DevicesTabController.deviceTableData.size() > 0) {
                glowWormDeviceToUse = DevicesTabController.deviceTableData.get(0);
            }
        } else if (FireflyLuciferin.config.isWifiEnable()) { // MQTT Enabled
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
     * @return true or false
     */
    public static boolean isSingleDeviceMainInstance() {

        return FireflyLuciferin.config != null && FireflyLuciferin.config.isMultiScreenSingleDevice() && FireflyLuciferin.config.getMultiMonitor() > 1 && JavaFXStarter.whoAmI == 1;

    }

    /**
     * Check if is single device other instance
     * @return true or false
     */
    public static boolean isSingleDeviceOtherInstance() {

        return FireflyLuciferin.config != null && FireflyLuciferin.config.isMultiScreenSingleDevice() && FireflyLuciferin.config.getMultiMonitor() > 1 && JavaFXStarter.whoAmI > 1;

    }

    /**
     * True if is MultiScreenSingleDevice radio is selected and if it's running in a multi monitor setup
     * @return true or false
     */
    public static boolean isSingleDeviceMultiScreen() {

        return FireflyLuciferin.config != null && FireflyLuciferin.config.isMultiScreenSingleDevice() && FireflyLuciferin.config.getMultiMonitor() > 1;

    }

    /**
     * Sleep current thread
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
     * Sleep current thread
     * @param numberOfSeconds to sleep
     */
    public static void sleepMilliseconds(int numberOfSeconds) {

        try {
            TimeUnit.MILLISECONDS.sleep(numberOfSeconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Print log only if extended log is enabled in the config .yml file
     * @param className the class that wants to log
     * @param msgToLog  msg to log
     */
    public static void conditionedLog(String className, String msgToLog) {

        if (FireflyLuciferin.config != null && FireflyLuciferin.config.isExtendedLog()) {
            log.debug("[{}] {}", className, msgToLog);
        }

    }

    /**
     * Calculate brightness based on the night mode
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
     * Bottom num led
     * @return numbers of led on the bottom
     */
    public static int getBottomLed(Configuration config) {

        if (!config.isSplitBottomRow()) {
            return config.getBottomRowLed();
        } else {
            return config.getBottomLeftLed() + config.getBottomRightLed();
        }

    }

    /**
     * Scale a number based on the OS scaling setting
     * @param numberToScale number that should be scaled based on the OS scaling setting
     * @param scaleRatio    OS scaling
     * @return scaled number
     */
    public static int scaleResolution(int numberToScale, int scaleRatio) {

        return (numberToScale*100)/scaleRatio;

    }

    /**
     * Add device to the connected device table
     * @param actualObj JSON node
     */
    public static void addDevice(JsonNode actualObj) {

        try {
            CommonUtility.conditionedLog("CommonUtility", CommonUtility.toJsonStringPrettyPrinted(actualObj));
            boolean validBaudRate = Integer.parseInt(actualObj.get(Constants.BAUD_RATE).toString()) >= 1
                    && Integer.parseInt(actualObj.get(Constants.BAUD_RATE).toString()) <= 7;
            if (FireflyLuciferin.config.getMultiMonitor() == 1 && (FireflyLuciferin.config.getSerialPort() == null
                    || FireflyLuciferin.config.getSerialPort().isEmpty()
                    || FireflyLuciferin.config.getSerialPort().equals(Constants.SERIAL_PORT_AUTO))) {
                if (FireflyLuciferin.config.isMqttStream()) {
                    FireflyLuciferin.config.setSerialPort(actualObj.get(Constants.MQTT_DEVICE_NAME).textValue());
                } else {
                    if (DevicesTabController.deviceTableData != null && DevicesTabController.deviceTableData.size() > 0) {
                        FireflyLuciferin.config.setSerialPort(DevicesTabController.deviceTableData.get(0).getDeviceIP());
                    }
                }
            }
            if (DevicesTabController.deviceTableData != null) {
                if (actualObj.get(Constants.WIFI) == null) {
                    wifiStrength = actualObj.get(Constants.WIFI) != null ? actualObj.get(Constants.WIFI).asInt() : 0;
                }
                DevicesTabController.deviceTableData.add(new GlowWormDevice(actualObj.get(Constants.MQTT_DEVICE_NAME).textValue(),
                        actualObj.get(Constants.STATE_IP).textValue(),
                        (actualObj.get(Constants.WIFI) == null ? Constants.DASH : actualObj.get(Constants.WIFI) + Constants.PERCENT),
                        (actualObj.get(Constants.DEVICE_VER).textValue()),
                        (actualObj.get(Constants.DEVICE_BOARD) == null ? Constants.DASH : actualObj.get(Constants.DEVICE_BOARD).textValue()),
                        (actualObj.get(Constants.MAC) == null ? Constants.DASH : actualObj.get(Constants.MAC).textValue()),
                        (actualObj.get(Constants.GPIO) == null ? Constants.DASH : actualObj.get(Constants.GPIO).toString()),
                        (actualObj.get(Constants.NUMBER_OF_LEDS) == null ? Constants.DASH : actualObj.get(Constants.NUMBER_OF_LEDS).textValue()),
                        (FireflyLuciferin.formatter.format(new Date())),
                        Constants.FirmwareType.FULL.name(),
                        (((actualObj.get(Constants.BAUD_RATE) == null) || !validBaudRate) ? Constants.DASH :
                                Constants.BaudRate.values()[Integer.parseInt(actualObj.get(Constants.BAUD_RATE).toString()) - 1].getBaudRate()),
                        (actualObj.get(Constants.MQTT_TOPIC) == null ? FireflyLuciferin.config.isWifiEnable() ? Constants.MQTT_BASE_TOPIC : Constants.DASH
                                : actualObj.get(Constants.MQTT_TOPIC).textValue())));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    /**
     * Update device table with the received message
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
                    if (mqttmsg.get(Constants.DEVICE_VER) != null) {
                        glowWormDevice.setDeviceVersion(mqttmsg.get(Constants.DEVICE_VER).textValue());
                    }
                    if (mqttmsg.get(Constants.WIFI) != null) {
                        CommonUtility.wifiStrength = mqttmsg.get(Constants.WIFI) != null ? mqttmsg.get(Constants.WIFI).asInt() : 0;
                        glowWormDevice.setWifi(mqttmsg.get(Constants.WIFI) + Constants.PERCENT);
                    }
                    if (mqttmsg.get(Constants.STATE_IP) != null) {
                        glowWormDevice.setDeviceIP(mqttmsg.get(Constants.STATE_IP).textValue());
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
            }
        }

    }

    /**
     * Update device table using FPS topic
     * @param fpsTopicMsg json node
     */
    public static void updateFpsWithFpsTopic(JsonNode fpsTopicMsg) {

        String macToUpdate = fpsTopicMsg.get(Constants.MAC).textValue();
        if (fpsTopicMsg.get(Constants.MAC) != null) {
            DevicesTabController.deviceTableData.forEach(glowWormDevice -> {
                if (glowWormDevice.getMac().equals(macToUpdate)) {
                    glowWormDevice.setLastSeen(FireflyLuciferin.formatter.format(new Date()));
                    glowWormDevice.setNumberOfLEDSconnected(fpsTopicMsg.get(Constants.NUMBER_OF_LEDS).textValue());
                    if (glowWormDevice.getDeviceName().equals(FireflyLuciferin.config.getSerialPort()) || glowWormDevice.getDeviceIP().equals(FireflyLuciferin.config.getSerialPort())) {
                        FireflyLuciferin.FPS_GW_CONSUMER = Float.parseFloat(fpsTopicMsg.get(Constants.MQTT_TOPIC_FRAMERATE).asText());
                        CommonUtility.wifiStrength = fpsTopicMsg.get(Constants.WIFI) != null ? fpsTopicMsg.get(Constants.WIFI).asInt() : 0;
                    }
                }
            });
        }

    }

    /**
     * Update device table using Device topic
     * @param mqttmsg msg from the topic
     */
    public static void updateFpsWithDeviceTopic(JsonNode mqttmsg) {

        if (mqttmsg.get(Constants.MQTT_TOPIC_FRAMERATE) != null) {
            String macToUpdate = mqttmsg.get(Constants.MAC).asText();
            DevicesTabController.deviceTableData.forEach(glowWormDevice -> {
                if (glowWormDevice.getMac().equals(macToUpdate)) {
                    if (glowWormDevice.getDeviceName().equals(FireflyLuciferin.config.getSerialPort()) || glowWormDevice.getDeviceIP().equals(FireflyLuciferin.config.getSerialPort())) {
                        FireflyLuciferin.FPS_GW_CONSUMER = Float.parseFloat(mqttmsg.get(Constants.MQTT_TOPIC_FRAMERATE).asText());
                    }
                }
            });
        }

    }

    /**
     * Turn ON LEDs when Luciferin starts
     */
    public static void turnOnLEDs() {

        if (!Constants.Effect.BIAS_LIGHT.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                && !Constants.Effect.MUSIC_MODE_VU_METER.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                && !Constants.Effect.MUSIC_MODE_VU_METER_DUAL.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                && !Constants.Effect.MUSIC_MODE_BRIGHT.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                && !Constants.Effect.MUSIC_MODE_RAINBOW.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))) {
            if (FireflyLuciferin.config.isToggleLed()) {
                if (FireflyLuciferin.config.isWifiEnable()) {
                    String[] color = FireflyLuciferin.config.getColorChooser().split(",");
                    StateDto stateDto = new StateDto();
                    stateDto.setState(Constants.ON);
                    stateDto.setEffect(FireflyLuciferin.config.getEffect().toLowerCase());
                    ColorDto colorDto = new ColorDto();
                    colorDto.setR(Integer.parseInt(color[0]));
                    colorDto.setG(Integer.parseInt(color[1]));
                    colorDto.setB(Integer.parseInt(color[2]));
                    stateDto.setColor(colorDto);
                    stateDto.setBrightness(CommonUtility.getNightBrightness());
                    MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.toJsonString(stateDto));
                }
            } else {
                if (FireflyLuciferin.config.isWifiEnable()) {
                    StateDto stateDto = new StateDto();
                    stateDto.setState(Constants.OFF);
                    stateDto.setEffect(Constants.SOLID);
                    stateDto.setBrightness(CommonUtility.getNightBrightness());
                    MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.toJsonString(stateDto));
                }
            }
        }

    }

    /**
     * Check if a String contains an integer
     * @param strNum string to check
     * @return if is a number or not
     */
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
     * @param key resource bundle key
     * @return localized String
     */
    public static String getWord(String key) {

        return FireflyLuciferin.bundle.getString(key);

    }

    /**
     * Get localized string
     * @param key resource bundle key
     * @param locale locale to use
     * @return localized String
     */
    public static String getWord(String key, Locale locale) {

        return ResourceBundle.getBundle(Constants.MSG_BUNDLE, locale).getString(key);

    }

}
