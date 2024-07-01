/*
  StorageManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
import javafx.stage.Stage;
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
    public boolean restartNeeded = false;
    private final String path;

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
        return readConfig(false, MainSingleton.getInstance().config != null ? MainSingleton.getInstance().profileArgs : Constants.DEFAULT);
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
        if (MainSingleton.getInstance().profileArgs != null && !MainSingleton.getInstance().profileArgs.isEmpty()) {
            config = readProfileConfig(MainSingleton.getInstance().profileArgs);
        } else {
            config = readProfileInUseConfig();
        }
        if (config == null) {
            try {
                Stage stage = new Stage();
                MainSingleton.getInstance().guiManager = new GuiManager(stage, false);
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
        // Firefly Luciferin v1.9.4 introduced a new aspect ratio, writing it without user interactions
        // Firefly Luciferin v1.10.2 introduced a config version and a refactored LED matrix
        // Firefly Luciferin v1.11.3 introduced a white temperature and a refactored LED matrix
        // Firefly Luciferin v2.2.5 introduced WiFi enable setting, MQTT is now optional when using Full firmware
        // Luciferin v2.4.7 introduced a new way to manage white temp
        boolean writeToStorage = false;
        log.debug("Firefly Luciferin version: {}, version number: {}", config.getConfigVersion(), UpgradeManager.versionNumberToNumber(config.getConfigVersion()));
        if (config.getLedMatrix().size() < Enums.AspectRatio.values().length || config.getConfigVersion().isEmpty() || config.getWhiteTemperature() == 0
                || (config.isMqttEnable() && !config.isFullFirmware())) {
            log.info("Config file is old, writing a new one.");
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
            writeToStorage = updatePrevious21010(config, writeToStorage); // Version <= 2.10.10
            writeToStorage = updatePrevious2124(config, writeToStorage); // Version <= 2.12.4
            if (config.getAudioDevice().equals(Enums.Audio.DEFAULT_AUDIO_OUTPUT.getBaseI18n())) {
                config.setAudioDevice(Enums.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n());
                writeToStorage = true;
            }
        }
        if (writeToStorage) {
            config.setConfigVersion(MainSingleton.getInstance().version);
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
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= 21071003) {
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
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) <= 21101010) {
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
        if (UpgradeManager.versionNumberToNumber(config.getConfigVersion()) < 21121004) {
            if (config.isMqttEnable()) {
                if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
                    ManagerSingleton.getInstance().updateMqttDiscovery = true;
                    writeToStorage = true;
                }
            }
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