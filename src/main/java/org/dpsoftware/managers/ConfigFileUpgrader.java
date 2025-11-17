/*
  ConfigFileUpgrader.java

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

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.controllers.ColorCorrectionDialogController;
import org.dpsoftware.managers.dto.LedMatrixInfo;
import org.dpsoftware.utilities.CommonUtility;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * Utility methods used to upgrade legacy config file to new version
 */
@Slf4j
public record ConfigFileUpgrader(ObjectMapper mapper, String path) {

    /**
     * Updates all the MQTT discovery entities
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    private static boolean updateMqttDiscoveryEntities(Configuration config, boolean writeToStorage) {
        if (config.isMqttEnable()) {
            if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
                ManagerSingleton.getInstance().updateMqttDiscovery = true;
                writeToStorage = true;
            }
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.1.7
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    boolean updatePrevious217(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= UpgradeManager.versionNumberToNumber("2.1.7")) {
            config.setMonitorNumber(config.getMonitorNumber() - 1);
            config.setTimeout(100);
            writeToStorage = true;
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.4.7
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    boolean updatePrevious247(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= UpgradeManager.versionNumberToNumber("2.4.7")) {
            // this must match WHITE_TEMP_CORRECTION_DISABLE in GlowWorm firmware
            config.setWhiteTemperature(Constants.DEFAULT_WHITE_TEMP);
            writeToStorage = true;
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.5.9
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    boolean updatePrevious259(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= UpgradeManager.versionNumberToNumber("2.5.9")) {
            config.setSplitBottomMargin(Constants.SPLIT_BOTTOM_MARGIN_OFF);
            if (config.isSplitBottomRow()) {
                config.setSplitBottomMargin(Constants.SPLIT_BOTTOM_MARGIN_DEFAULT);
            }
            config.setGrabberAreaTopBottom(Constants.GRABBER_AREA_TOP_BOTTOM_DEFAULT);
            config.setGrabberSide(Constants.GRABBER_AREA_SIDE_DEFAULT);
            config.setGapTypeTopBottom(Constants.GAP_TYPE_DEFAULT_TOP_BOTTOM);
            config.setGapTypeSide(Constants.GAP_TYPE_DEFAULT_SIDE);
            config.setGroupBy(Constants.GROUP_BY_LEDS);
            configureLedMatrix(config);
            if (NativeExecutor.isWindows()) {
                config.setAudioDevice(Enums.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getBaseI18n());
            } else {
                config.setAudioDevice(Enums.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n());
            }
            writeToStorage = true;
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.7.3
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    boolean updatePrevious273(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= UpgradeManager.versionNumberToNumber("2.7.3")) {
            config.setHueMap(ColorCorrectionDialogController.initHSLMap());
            writeToStorage = true;
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.10.10
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    boolean updatePrevious21010(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= UpgradeManager.versionNumberToNumber("2.10.10")) {
            if (config.getRuntimeLogLevel().equals(Constants.TRUE)) {
                config.setRuntimeLogLevel(Level.TRACE.levelStr);
            } else if (config.getRuntimeLogLevel().equals(Constants.FALSE)) {
                config.setRuntimeLogLevel(Level.INFO.levelStr);
            }
            writeToStorage = true;
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.12.4
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    boolean updatePrevious2124(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) < UpgradeManager.versionNumberToNumber("2.12.4")) {
            writeToStorage = updateMqttDiscoveryEntities(config, writeToStorage);
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.18.7
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    boolean updatePrevious2187(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) < UpgradeManager.versionNumberToNumber("2.18.7")) {
            writeToStorage = updateMqttDiscoveryEntities(config, writeToStorage);
            if (config.getCaptureMethod().equals(Constants.GSTREAMER_DDUPL)) {
                config.setCaptureMethod(Configuration.CaptureMethod.DDUPL_DX12.name());
                writeToStorage = true;
            }
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.22.6
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @param filename       this filename is used to manually load the yaml file without a direct map to a Configuration object
     *                       it is useful when you need to rename a properties in the yaml file
     * @return true if update is needed
     */
    @SuppressWarnings("unchecked")
    boolean updatePrevious2226(Configuration config, boolean writeToStorage, String filename) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) < UpgradeManager.versionNumberToNumber("2.22.6")) {
            if (config.getLedMatrix().size() < Enums.AspectRatio.values().length || config.getConfigVersion().isEmpty() || config.getWhiteTemperature() == 0
                    || (config.isMqttEnable() && !config.isFullFirmware())) {
                configureLedMatrix(config);
                if (config.getWhiteTemperature() == 0) {
                    config.setWhiteTemperature(Constants.DEFAULT_WHITE_TEMP);
                }
                if ((config.isMqttEnable() && !config.isFullFirmware())) {
                    config.setFullFirmware(true);
                }
                writeToStorage = true;
            }
            writeToStorage = updateMqttDiscoveryEntities(config, writeToStorage);
            Map<String, Object> data;
            try {
                data = mapper.readValue(new File(path + File.separator + filename), Map.class);
                if (!data.get("frameInsertion").equals(CommonUtility.getWord(Constants.NO_SMOOTHING, Locale.ENGLISH))) {
                    config.setFrameInsertionTarget(Constants.DEFAULT_FRAMGEN);
                    config.setEmaAlpha(Constants.DEFAULT_EMA);
                    config.setSmoothingType(Constants.DEFAULT_SMOOTHING);
                    config.setSmoothingTargetFramerate(Constants.DEFAULT_SMOOTHING_TARGET);
                } else {
                    config.setSmoothingType(Enums.Smoothing.DISABLED.getBaseI18n());
                }
                config.setFullFirmware((Boolean) data.get("wifiEnable"));
                config.setOutputDevice((String) data.get("serialPort"));
                config.setRuntimeLogLevel((String) data.get("extendedLog"));
                config.setWirelessStream((Boolean) data.get("mqttStream"));
                config.setLuminosityThreshold((Boolean) data.get("eyeCare") ? 1 : 0);
                config.setAudioDevice(Enums.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n());
                writeToStorage = true;
            } catch (Exception ignored) {
            }
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.23.7
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    boolean updatePrevious2237(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) < UpgradeManager.versionNumberToNumber("2.23.7")) {
            writeToStorage = updateMqttDiscoveryEntities(config, writeToStorage);
        }
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.24.8
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    // TODO change version
    boolean updatePrevious2248(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) < UpgradeManager.versionNumberToNumber("2.24.8")) {
            switch (config.getTheme()) {
                case "Classic theme":
                    config.setTheme(Enums.Theme.CLASSIC.getBaseI18n());
                    break;
                case "Dark theme":
                    config.setTheme(Enums.Theme.DARK_THEME_CYAN.getBaseI18n());
                    break;
                case "Dark blue theme":
                    config.setTheme(Enums.Theme.DARK_BLUE_THEME.getBaseI18n());
                    break;
                case "Dark purple theme":
                    config.setTheme(Enums.Theme.DARK_THEME_PURPLE.getBaseI18n());
                    break;
                case "Light gray theme":
                    config.setTheme(Enums.Theme.DARK_THEME_ORANGE.getBaseI18n());
                    break;
            }
            writeToStorage = true;
        }
        return writeToStorage;
    }

    /**
     * Reconfigure LED matrix
     *
     * @param config app config params
     */
    private void configureLedMatrix(Configuration config) {
        LEDCoordinate ledCoordinate = new LEDCoordinate();
        LedMatrixInfo ledMatrixInfo = new LedMatrixInfo(config.getScreenResX(),
                config.getScreenResY(), config.getBottomRightLed(), config.getRightLed(), config.getTopLed(), config.getLeftLed(),
                config.getBottomLeftLed(), config.getBottomRowLed(), config.getSplitBottomMargin(), config.getGrabberAreaTopBottom(), config.getGrabberSide(),
                config.getGapTypeTopBottom(), config.getGapTypeSide(), config.getGroupBy());
        try {
            LedMatrixInfo ledMatrixInfoFullScreen = (LedMatrixInfo) ledMatrixInfo.clone();
            config.getLedMatrix().put(Enums.AspectRatio.FULLSCREEN.getBaseI18n(), ledCoordinate.initializeLedMatrix(Enums.AspectRatio.FULLSCREEN, ledMatrixInfoFullScreen, false));
            LedMatrixInfo ledMatrixInfoLetterbox = (LedMatrixInfo) ledMatrixInfo.clone();
            config.getLedMatrix().put(Enums.AspectRatio.LETTERBOX.getBaseI18n(), ledCoordinate.initializeLedMatrix(Enums.AspectRatio.LETTERBOX, ledMatrixInfoLetterbox, false));
            LedMatrixInfo ledMatrixInfoPillarbox = (LedMatrixInfo) ledMatrixInfo.clone();
            config.getLedMatrix().put(Enums.AspectRatio.PILLARBOX.getBaseI18n(), ledCoordinate.initializeLedMatrix(Enums.AspectRatio.PILLARBOX, ledMatrixInfoPillarbox, false));
        } catch (CloneNotSupportedException e) {
            log.info(e.getMessage());
        }
    }

}
