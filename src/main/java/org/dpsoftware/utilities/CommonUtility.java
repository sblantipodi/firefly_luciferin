/*
  CommonUtility.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.controllers.DevicesTabController;
import org.dpsoftware.gui.elements.GlowWormDevice;

import java.util.concurrent.TimeUnit;

/**
 * CommonUtility class for useful methods
 */
@Slf4j
public class CommonUtility {

    /**
     * From Java Object to JSON String, useful to handle checked exceptions in lambdas
     * @param obj generic Java object
     * @return JSON String
     */
    public static String writeValueAsString(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return "";
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
        } else if (FireflyLuciferin.config.isMqttEnable()) { // MQTT Enabled
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

}
