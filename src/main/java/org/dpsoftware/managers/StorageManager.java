/*
  StorageManager.java

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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.GUIManager;
import org.dpsoftware.gui.controllers.ColorCorrectionDialogController;
import org.dpsoftware.managers.dto.LedMatrixInfo;
import org.dpsoftware.utilities.CommonUtility;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Write and read yaml configuration file
 */
@Slf4j
public class StorageManager {

    private final ObjectMapper mapper;
    private String path;
    public boolean restartNeeded = false;

    /**
     * Constructor
     */
    public StorageManager() {
        // Initialize yaml file writer
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Create FireflyLuciferin in the Documents folder
        path = System.getProperty(Constants.HOME_PATH) + File.separator + Constants.DOCUMENTS_FOLDER;
        path += File.separator + Constants.LUCIFERIN_FOLDER;
        File customDir = new File(path);

        if (customDir.mkdirs()) {
            log.info(customDir + " " + CommonUtility.getWord(Constants.WAS_CREATED));
        }
    }

    /**
     * Write params inside the configuration file
     *
     * @param config        file
     * @param forceFilename where to write the config
     * @throws IOException can't write to file
     */
    public void writeConfig(Configuration config, String forceFilename) throws IOException {
        String filename = switch (JavaFXStarter.whoAmI) {
            case 1 -> Constants.CONFIG_FILENAME;
            case 2 -> Constants.CONFIG_FILENAME_2;
            case 3 -> Constants.CONFIG_FILENAME_3;
            default -> "";
        };
        if (forceFilename != null) {
            filename = forceFilename;
        }
        Configuration currentConfig = readConfigFile(filename);
        if (currentConfig != null) {
            File file = new File(path + File.separator + filename);
            if (file.delete()) {
                log.info(CommonUtility.getWord(Constants.CLEANING_OLD_CONFIG));
            } else {
                log.info(CommonUtility.getWord(Constants.FAILED_TO_CLEAN_CONFIG));
            }
        }
        mapper.writeValue(new File(path + File.separator + filename), config);
    }

    /**
     * Load configuration file
     *
     * @param filename file to read
     * @return config file
     */
    public Configuration readConfigFile(String filename) {
        Configuration config = null;
        try {
            config = mapper.readValue(new File(path + File.separator + filename), Configuration.class);
        } catch (IOException e) {
            log.error(CommonUtility.getWord(Constants.ERROR_READING_CONFIG));
        }
        return config;
    }

    /**
     * Read profile from a given profile name, check the difference with the current config
     *
     * @param profileName profile to load
     * @param sm          storage manager instance
     * @return configuration to use
     */
    public Configuration readProfileAndCheckDifference(String profileName, StorageManager sm) {
        Configuration config = readProfileConfig(profileName);
        sm.checkProfileDifferences(config, FireflyLuciferin.config);
        return config;
    }

    /**
     * Read a config from a given profile name
     *
     * @param profileName profile to use
     * @return current configuration file
     */
    public Configuration readProfileConfig(String profileName) {
        return readConfig(false, profileName);
    }

    /**
     * Read config file, if a profile is set, read the profile in use
     *
     * @return current configuration file
     */
    public Configuration readProfileInUseConfig() {
        return readConfig(false, FireflyLuciferin.config != null ? FireflyLuciferin.config.getDefaultProfile() : Constants.DEFAULT);
    }

    /**
     * Read main config file
     *
     * @return current configuration file
     */
    public Configuration readMainConfig() {
        return readConfig(true, null);
    }

    /**
     * Read config file
     *
     * @param readMainConfig when true read main config, when false, read the config of the running instance
     * @return current configuration file
     */
    public Configuration readConfig(boolean readMainConfig, String profileName) {
        try {
            Configuration currentConfig;
            if (!CommonUtility.getWord(Constants.DEFAULT).equals(profileName) && !Constants.DEFAULT.equals(profileName) && !readMainConfig) {
                currentConfig = readConfigFile(getProfileFileName(profileName));
            } else {
                Configuration mainConfig = readConfigFile(Constants.CONFIG_FILENAME);
                if (readMainConfig) {
                    return mainConfig;
                }
                if (JavaFXStarter.whoAmI == 2) {
                    currentConfig = readConfigFile(Constants.CONFIG_FILENAME_2);
                } else if (JavaFXStarter.whoAmI == 3) {
                    currentConfig = readConfigFile(Constants.CONFIG_FILENAME_3);
                } else {
                    currentConfig = mainConfig;
                }
            }
            return currentConfig;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Some params should not be updated when switching profiles.
     * Some params needs a restart to take effect. Automatic restart is triggered on profile change only.
     *
     * @param defaultConfig stored config in the main file
     * @param profileConfig stored config in the profile file
     */
    public void checkProfileDifferences(Configuration defaultConfig, Configuration profileConfig) {
        if (profileConfig != null && defaultConfig != null) {
            restartNeeded = false;
            Set<String> restartReasons = new LinkedHashSet<>();
            if (!defaultConfig.getLanguage().equals(profileConfig.getLanguage()))
                restartReasons.add(Constants.TOOLTIP_LANGUAGE);
            if (!defaultConfig.getTheme().equals(profileConfig.getTheme())) restartReasons.add(Constants.TOOLTIP_THEME);
            if (!defaultConfig.getBaudRate().equals(profileConfig.getBaudRate()))
                restartReasons.add(Constants.TOOLTIP_BAUD_RATE);
            if (!defaultConfig.getCaptureMethod().equals(profileConfig.getCaptureMethod()))
                restartReasons.add(Constants.TOOLTIP_CAPTUREMETHOD);
            if (profileConfig.getSerialPort() != null && !Constants.SERIAL_PORT_AUTO.equals(defaultConfig.getSerialPort())) {
                if (!defaultConfig.getSerialPort().equals(profileConfig.getSerialPort()))
                    restartReasons.add(Constants.TOOLTIP_SERIALPORT);
            }
            if (defaultConfig.getNumberOfCPUThreads() != profileConfig.getNumberOfCPUThreads())
                restartReasons.add(Constants.TOOLTIP_NUMBEROFTHREADS);
            if (defaultConfig.isFullFirmware() != profileConfig.isFullFirmware())
                restartReasons.add(Constants.TOOLTIP_WIFIENABLE);
            if (defaultConfig.isWirelessStream() != profileConfig.isWirelessStream())
                restartReasons.add(Constants.TOOLTIP_MQTTSTREAM);
            if (defaultConfig.isMqttEnable() != profileConfig.isMqttEnable())
                restartReasons.add(Constants.TOOLTIP_MQTTENABLE);
            if (!defaultConfig.getStreamType().equals(profileConfig.getStreamType()))
                restartReasons.add(Constants.TOOLTIP_STREAMTYPE);
            if (!defaultConfig.getMqttServer().equals(profileConfig.getMqttServer()))
                restartReasons.add(Constants.TOOLTIP_MQTTHOST);
            if (!defaultConfig.getMqttTopic().equals(profileConfig.getMqttTopic()))
                restartReasons.add(Constants.TOOLTIP_MQTTTOPIC);
            if (!defaultConfig.getMqttUsername().equals(profileConfig.getMqttUsername()))
                restartReasons.add(Constants.TOOLTIP_MQTTUSER);
            if (!defaultConfig.getMqttPwd().equals(profileConfig.getMqttPwd()))
                restartReasons.add(Constants.TOOLTIP_MQTTPWD);
            if (defaultConfig.isMultiScreenSingleDevice() != profileConfig.isMultiScreenSingleDevice())
                restartReasons.add(Constants.TOOLTIP_MONITORNUMBER);
            if (defaultConfig.getMultiMonitor() != profileConfig.getMultiMonitor())
                restartReasons.add(Constants.TOOLTIP_MULTIMONITOR);
            if (restartReasons.size() > 0) {
                restartNeeded = true;
                log.debug(String.join("\n", restartReasons));
            }
        }
    }

    /**
     * Check if a file exist
     *
     * @param filename filename to check
     * @return current configuration file
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean checkIfFileExist(String filename) {
        File file = new File(path + File.separator + filename);
        return file.exists();
    }

    /**
     * Load config yaml and create a default config if not present
     */
    public Configuration loadConfigurationYaml() {
        Configuration config;
        if (FireflyLuciferin.profileArgs != null && !FireflyLuciferin.profileArgs.isEmpty()) {
            config = readProfileConfig(FireflyLuciferin.profileArgs);
            config.setDefaultProfile(FireflyLuciferin.profileArgs);
            FireflyLuciferin.profileArgs = "";
        } else {
            config = readProfileInUseConfig();
        }
        if (config == null) {
            try {
                String fxml;
                fxml = Constants.FXML_SETTINGS;
                Scene scene = new Scene(GUIManager.loadFXML(fxml));
                Stage stage = new Stage();
                stage.setTitle("  " + CommonUtility.getWord(Constants.SETTINGS));
                stage.setScene(scene);
                if (!NativeExecutor.isSystemTraySupported() || NativeExecutor.isLinux()) {
                    stage.setOnCloseRequest(evt -> FireflyLuciferin.exit());
                }
                GUIManager.setStageIcon(stage);
                stage.showAndWait();
                config = readProfileInUseConfig();
            } catch (IOException stageError) {
                log.error(stageError.getMessage());
            }
        }
        return config;
    }

    /**
     * Check if the config file updated, if not, write a new one
     *
     * @param config file
     * @throws IOException can't write to config file
     */
    public void updateConfigFile(Configuration config) throws IOException {
        // Firefly Luciferin v1.9.4 introduced a new aspect ratio, writing it without user interactions
        // Firefly Luciferin v1.10.2 introduced a config version and a refactored LED matrix
        // Firefly Luciferin v1.11.3 introduced a white temperature and a refactored LED matrix
        // Firefly Luciferin v2.2.5 introduced WiFi enable setting, MQTT is now optional when using Full firmware
        // Luciferin v2.4.7 introduced a new way to manage white temp
        boolean writeToStorage = false;
        if (config.getLedMatrix().size() < Constants.AspectRatio.values().length || config.getConfigVersion().isEmpty() || config.getWhiteTemperature() == 0
                || (config.isMqttEnable() && !config.isFullFirmware())) {
            log.debug("Config file is old, writing a new one.");
            configureLedMatrix(config);
            if (config.getWhiteTemperature() == 0) {
                config.setWhiteTemperature(Constants.DEFAULT_WHITE_TEMP);
            }
            if ((config.isMqttEnable() && !config.isFullFirmware())) {
                config.setFullFirmware(true);
            }
            writeToStorage = true;
        }
        if (config.getConfigVersion() != null && !config.getConfigVersion().isEmpty()) {
            writeToStorage = updatePrevious217(config, writeToStorage); // Version <= 2.1.7
            writeToStorage = updatePrevious247(config, writeToStorage); // Version <= 2.4.7
            writeToStorage = updatePrevious259(config, writeToStorage); // Version <= 2.5.9
            writeToStorage = updatePrevious273(config, writeToStorage); // Version <= 2.7.3
            if (config.getAudioDevice().equals(Constants.Audio.DEFAULT_AUDIO_OUTPUT.getBaseI18n())) {
                config.setAudioDevice(Constants.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n());
                writeToStorage = true;
            }
        }
        if (writeToStorage) {
            config.setConfigVersion(FireflyLuciferin.version);
            writeConfig(config, null);
        }
    }

    /**
     * Update configuration file previous than 2.1.7
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    private boolean updatePrevious217(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= 21011007) {
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
    private boolean updatePrevious247(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= 21041007) {
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
    private boolean updatePrevious259(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= 21051009) {
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
                config.setAudioDevice(Constants.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getBaseI18n());
            } else {
                config.setAudioDevice(Constants.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n());
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
    private boolean updatePrevious273(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= 21071003) {
            config.hueMap = ColorCorrectionDialogController.initHSLMap();
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
            config.getLedMatrix().put(Constants.AspectRatio.FULLSCREEN.getBaseI18n(), ledCoordinate.initFullScreenLedMatrix(ledMatrixInfoFullScreen));
            LedMatrixInfo ledMatrixInfoLetterbox = (LedMatrixInfo) ledMatrixInfo.clone();
            config.getLedMatrix().put(Constants.AspectRatio.LETTERBOX.getBaseI18n(), ledCoordinate.initLetterboxLedMatrix(ledMatrixInfoLetterbox));
            LedMatrixInfo ledMatrixInfoPillarbox = (LedMatrixInfo) ledMatrixInfo.clone();
            config.getLedMatrix().put(Constants.AspectRatio.PILLARBOX.getBaseI18n(), ledCoordinate.initPillarboxMatrix(ledMatrixInfoPillarbox));
        } catch (CloneNotSupportedException e) {
            log.debug(e.getMessage());
        }
    }

    /**
     * Check for all the available profiles on the file system for the current instance
     *
     * @return profiles list
     */
    public Set<String> listProfilesForThisInstance() {
        return Stream.of(Objects.requireNonNull(new File(path + File.separator).listFiles()))
                .filter(file -> !file.isDirectory())
                .filter(file -> file.getName().split("_")[0].equals(String.valueOf(JavaFXStarter.whoAmI)))
                .map(file -> file.getName().replace(Constants.YAML_EXTENSION, "").replace(JavaFXStarter.whoAmI + "_", ""))
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Delete profile file
     *
     * @param profileName profile to delete
     * @return true on success
     */
    public boolean deleteProfile(String profileName) {
        File profile = new File(path + File.separator + getProfileFileName(profileName));
        return profile.delete();
    }

    /**
     * Get profile file name based on profile name
     *
     * @param profileName profile name
     * @return file name
     */
    public String getProfileFileName(String profileName) {
        return JavaFXStarter.whoAmI + "_" + profileName + Constants.YAML_EXTENSION;
    }

    /**
     * Delete temp files
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void deleteTempFiles() {
        if (NativeExecutor.isWindows()) {
            File fireflyLuciferinTmpFile = new File(path + File.separator + Constants.SETUP_FILENAME_WINDOWS);
            if (fireflyLuciferinTmpFile.isFile()) fireflyLuciferinTmpFile.delete();
        } else if (NativeExecutor.isLinux()) {
            File fireflyLuciferinDebTmpFile = new File(path + File.separator + Constants.SETUP_FILENAME_LINUX_DEB);
            if (fireflyLuciferinDebTmpFile.isFile()) fireflyLuciferinDebTmpFile.delete();
            File fireflyLuciferinRpmTmpFile = new File(path + File.separator + Constants.SETUP_FILENAME_LINUX_RPM);
            if (fireflyLuciferinRpmTmpFile.isFile()) fireflyLuciferinRpmTmpFile.delete();
        }
        File glowWormEsp8266TmpFile = new File(path + File.separator + Constants.GW_FIRMWARE_BIN_ESP8266);
        if (glowWormEsp8266TmpFile.isFile()) glowWormEsp8266TmpFile.delete();
        File glowWormEsp32TmpFile = new File(path + File.separator + Constants.GW_FIRMWARE_BIN_ESP32);
        if (glowWormEsp32TmpFile.isFile()) glowWormEsp32TmpFile.delete();
    }

}