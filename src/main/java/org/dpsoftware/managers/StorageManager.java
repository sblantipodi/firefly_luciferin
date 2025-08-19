/*
  StorageManager.java

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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.InstanceConfigurer;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.controllers.ColorCorrectionDialogController;
import org.dpsoftware.managers.dto.LedMatrixInfo;
import org.dpsoftware.utilities.CommonUtility;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Write and read yaml configuration file
 */
@Slf4j
public class StorageManager {

    private final ObjectMapper mapper;
    private final String path;
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
        // Create FireflyLuciferin in the XDG_HOME folder
        path = InstanceConfigurer.getConfigPath();
    }

    /**
     * Delete folder with all the subfolders
     *
     * @param directory to delete
     */
    public static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (directory.delete()) {
            log.debug("Folder correcly deleted: {}", directory.getAbsolutePath());
        } else {
            log.debug("Can't delete folder: {}", directory.getAbsolutePath());
        }
    }

    /**
     * Utility method used to copy a folder to another folder
     *
     * @param src  folder
     * @param dest folder
     */
    public static void copyDir(String src, String dest) {
        log.info("Copy src folder={} to destination folder: {}", src, dest);
        try (Stream<Path> walk = Files.walk(Paths.get(src))) {
            walk.forEach(a -> {
                Path b = Paths.get(dest, a.toString().substring(src.length()));
                try {
                    if (!a.toString().equals(src)) Files.copy(a, b, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            });
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

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
     * Copy file (FileInputStream) to GZIPOutputStream
     *
     * @param source file
     * @param target compressed file
     * @throws IOException something went wrong
     */
    public static void compressGzip(Path source, Path target) throws IOException {
        log.info("File before compression: {}", source.toFile().length());
        try (CustomGZIPOutputStream gos = new CustomGZIPOutputStream(new FileOutputStream(target.toFile()));
             FileInputStream fis = new FileInputStream(source.toFile())) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gos.write(buffer, 0, len);
            }
        }
        log.info("File after compression: {}", target.toFile().length());
    }

    /**
     * Write params inside the configuration file
     *
     * @param config        file
     * @param forceFilename where to write the config
     * @throws IOException can't write to file
     */
    public void writeConfig(Configuration config, String forceFilename) throws IOException {
        String filename = switch (MainSingleton.getInstance().whoAmI) {
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
        sm.checkProfileDifferences(config, MainSingleton.getInstance().config);
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
        return readConfig(false, MainSingleton.getInstance().config != null ? MainSingleton.getInstance().profileArg : Constants.DEFAULT);
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
                if (MainSingleton.getInstance().whoAmI == 2) {
                    currentConfig = readConfigFile(Constants.CONFIG_FILENAME_2);
                } else if (MainSingleton.getInstance().whoAmI == 3) {
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
            if (profileConfig.getOutputDevice() != null && !defaultConfig.getOutputDevice().equals(profileConfig.getOutputDevice()))
                restartReasons.add(Constants.TOOLTIP_SERIALPORT);
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
            if (defaultConfig.getSimdAvx() != profileConfig.getSimdAvx())
                restartReasons.add(Constants.TOOLTIP_SIMD);
            if (!restartReasons.isEmpty()) {
                restartNeeded = true;
                log.info(String.join("\n", restartReasons));
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
        if (MainSingleton.getInstance().profileArg != null && !MainSingleton.getInstance().profileArg.isEmpty()) {
            config = readProfileConfig(MainSingleton.getInstance().profileArg);
        } else {
            config = readProfileInUseConfig();
        }
        if (config == null) {
            try {
                MainSingleton.getInstance().guiManager = new GuiManager(false);
                MainSingleton.getInstance().guiManager.showStage(Constants.FXML_SETTINGS, false, false);
                config = readProfileInUseConfig();
            } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
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
        boolean writeToStorage = false;
        log.debug("Firefly Luciferin version: {}, version number: {}", config.getConfigVersion(), UpgradeManager.versionNumberToNumber(config.getConfigVersion()));
        if (config.getConfigVersion() != null && !config.getConfigVersion().isEmpty()) {
            String filename;
            if (MainSingleton.getInstance().whoAmI == 3) {
                filename = Constants.CONFIG_FILENAME_3;
            } else if (MainSingleton.getInstance().whoAmI == 2) {
                filename = Constants.CONFIG_FILENAME_2;
            } else {
                filename = Constants.CONFIG_FILENAME;
            }
            writeToStorage = updateConfig(config, filename);
        }
        if (writeToStorage) {
            log.info("Config file is old, writing a new one.");
            config.setConfigVersion(MainSingleton.getInstance().version);
            // Update current instance config file
            writeConfig(config, null);
            // Update profiles linked to this instance
            for (String profileFilename : listProfilesForThisInstance()) {
                Configuration profileConfig = readProfileConfig(profileFilename);
                writeToStorage = updateConfig(profileConfig, getProfileFileName(profileFilename));
                if (writeToStorage) {
                    log.info("Profile ({}) is old, writing a new one.", profileFilename);
                    writeConfig(profileConfig, MainSingleton.getInstance().whoAmI + "_" + profileFilename + Constants.YAML_EXTENSION);
                }
            }
        }
    }

    /**
     * Update config object based on new requirements from newer version
     *
     * @param config   object to update
     * @param filename this filename is used to manually load the yaml file without a direct map to a Configuration object
     *                 it is useful when you need to rename a properties in the yaml file
     * @return true if the corresponding object must be written to file
     */
    @SuppressWarnings("all")
    private boolean updateConfig(Configuration config, String filename) {
        boolean writeToStorage = false;
        writeToStorage = updatePrevious217(config, writeToStorage); // Version <= 2.1.7
        writeToStorage = updatePrevious247(config, writeToStorage); // Version <= 2.4.7
        writeToStorage = updatePrevious259(config, writeToStorage); // Version <= 2.5.9
        writeToStorage = updatePrevious273(config, writeToStorage); // Version <= 2.7.3
        writeToStorage = updatePrevious21010(config, writeToStorage); // Version < 2.10.10
        writeToStorage = updatePrevious2124(config, writeToStorage); // Version < 2.12.4
        writeToStorage = updatePrevious2187(config, writeToStorage); // Version < 2.18.7
        writeToStorage = updatePrevious2226(config, writeToStorage, filename); // Version <= 2.22.6
        writeToStorage = updatePrevious2237(config, writeToStorage); // Version <= 2.23.7
        return writeToStorage;
    }

    /**
     * Update configuration file previous than 2.1.7
     *
     * @param config         configuration to update
     * @param writeToStorage if an update is needed, write to storage
     * @return true if update is needed
     */
    private boolean updatePrevious217(Configuration config, boolean writeToStorage) {
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
    private boolean updatePrevious247(Configuration config, boolean writeToStorage) {
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
    private boolean updatePrevious259(Configuration config, boolean writeToStorage) {
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
    private boolean updatePrevious273(Configuration config, boolean writeToStorage) {
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
    private boolean updatePrevious21010(Configuration config, boolean writeToStorage) {
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
    private boolean updatePrevious2124(Configuration config, boolean writeToStorage) {
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
    private boolean updatePrevious2187(Configuration config, boolean writeToStorage) {
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
    private boolean updatePrevious2226(Configuration config, boolean writeToStorage, String filename) {
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
    private boolean updatePrevious2237(Configuration config, boolean writeToStorage) {
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) < UpgradeManager.versionNumberToNumber("2.23.7")) {
            writeToStorage = updateMqttDiscoveryEntities(config, writeToStorage);
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
            config.getLedMatrix().put(Enums.AspectRatio.FULLSCREEN.getBaseI18n(), ledCoordinate.initFullScreenLedMatrix(ledMatrixInfoFullScreen));
            LedMatrixInfo ledMatrixInfoLetterbox = (LedMatrixInfo) ledMatrixInfo.clone();
            config.getLedMatrix().put(Enums.AspectRatio.LETTERBOX.getBaseI18n(), ledCoordinate.initLetterboxLedMatrix(ledMatrixInfoLetterbox));
            LedMatrixInfo ledMatrixInfoPillarbox = (LedMatrixInfo) ledMatrixInfo.clone();
            config.getLedMatrix().put(Enums.AspectRatio.PILLARBOX.getBaseI18n(), ledCoordinate.initPillarboxMatrix(ledMatrixInfoPillarbox));
        } catch (CloneNotSupportedException e) {
            log.info(e.getMessage());
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
                .filter(file -> file.getName().split("_")[0].equals(String.valueOf(MainSingleton.getInstance().whoAmI)))
                .map(file -> file.getName().replace(Constants.YAML_EXTENSION, "").replace(MainSingleton.getInstance().whoAmI + "_", ""))
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
        return MainSingleton.getInstance().whoAmI + "_" + profileName + Constants.YAML_EXTENSION;
    }

    /**
     * In the programming realm, glob is a pattern with wildcards to match filenames.
     * Using glob patterns to filter a list of filenames for our example.
     * Using the popular wildcards “*” and “?”.
     *
     * @param rootDir path where to search (includes subfolders)
     * @param pattern to search (ex: "glob: pattern")
     *                *.java	Matches all files with extension “java”
     *                *.{java,class}	Matches all files with extensions of “java” or “class”
     *                *.*	Matches all files with a “.” somewhere in its name
     *                ????	Matches all files with four characters in its name
     *                [test].docx	Matches all files with filename ‘t', ‘e', ‘s', or ‘t' and “docx” extension
     *                [0-4].csv	Matches all files with filename ‘0', ‘1', ‘2', ‘3', or ‘4' with “csv” extension
     *                C:\\temp\\*	Matches all files in the “C:\temp” directory on Windows systems
     *                src/test/*	Matches all files in the “src/test/” directory on Unix-based systems
     * @return list of filename
     * @throws IOException io
     */
    @SuppressWarnings("all")
    List<String> searchFilesWithWc(Path rootDir, String pattern) throws IOException {
        List<String> matchesList = new ArrayList<>();
        FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attribs) {
                FileSystem fs = FileSystems.getDefault();
                PathMatcher matcher = fs.getPathMatcher(pattern);
                Path name = file.getFileName();
                if (matcher.matches(name)) {
                    matchesList.add(name.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(rootDir, matcherVisitor);
        return matchesList;
    }

    /**
     * Delete temp files
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void deleteTempFiles() {
        try {
            if (NativeExecutor.isWindows()) {
                File fireflyLuciferinTmpFile = new File(path + File.separator + Constants.SETUP_FILENAME_WINDOWS);
                if (fireflyLuciferinTmpFile.isFile()) fireflyLuciferinTmpFile.delete();
            } else if (NativeExecutor.isLinux()) {
                File fireflyLuciferinDebTmpFile = new File(path + File.separator + Constants.SETUP_FILENAME_LINUX_DEB);
                if (fireflyLuciferinDebTmpFile.isFile()) fireflyLuciferinDebTmpFile.delete();
                File fireflyLuciferinRpmTmpFile = new File(path + File.separator + Constants.SETUP_FILENAME_LINUX_RPM);
                if (fireflyLuciferinRpmTmpFile.isFile()) fireflyLuciferinRpmTmpFile.delete();
                if (NativeExecutor.isSnap()) {
                    File openJfxPath = new File(InstanceConfigurer.getOpenJfxCachePath());
                    deleteDirectory(openJfxPath);
                }
            }
            Path rootDir = Paths.get(path);
            List<String> firmwareFiles = searchFilesWithWc(rootDir, Constants.FIRMWARE_FILENAME_PATTERN);
            if (!firmwareFiles.isEmpty()) {
                firmwareFiles.addAll(searchFilesWithWc(rootDir, Constants.FIRMWARE_COMPRESSED_FILENAME_PATTERN));
            }
            for (String firmwareFilename : firmwareFiles) {
                File fileToDelete = new File(path + File.separator + firmwareFilename);
                if (fileToDelete.isFile()) fileToDelete.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}