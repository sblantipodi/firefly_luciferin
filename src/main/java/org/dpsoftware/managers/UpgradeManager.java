/*
  UpgradeManager.java

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

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.GUIManager;
import org.dpsoftware.gui.controllers.DevicesTabController;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.dto.WebServerStarterDto;
import org.dpsoftware.network.tcpUdp.TcpClient;
import org.dpsoftware.utilities.CommonUtility;
import org.dpsoftware.utilities.PropertiesLoader;

import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * An utility class for Firefly Luciferin PC software upgrade
 * and for its companion Glow Worm Luciferin firmware upgrade
 */
@Slf4j
@Getter
@NoArgsConstructor
public class UpgradeManager {

    public static boolean serialVersionOk = false;
    public static String deviceNameForSerialDevice = "";
    String latestReleaseStr = "";

    /**
     * Transform release version to a comparable number with other releases
     * it handle up to 1000 Major, minor, hotfix numbers
     *
     * @param latestReleaseStr Release version
     * @return comparable number with other releases
     */
    public static long versionNumberToNumber(String latestReleaseStr) {
        String[] majorMinorHotfix = latestReleaseStr.split("\\.");
        return Long.parseLong((majorMinorHotfix[0]) + 1_000_000)
                + Long.parseLong((majorMinorHotfix[1] + 1_000))
                + Long.parseLong((majorMinorHotfix[2]));
    }

    /**
     * Check for Glow Worm Luciferin or Firefly Luciferin update on GitHub
     *
     * @param urlToVerionFile GitHub URL
     * @param currentVersion  current version
     * @param rawText         GitHub text where to extract the version
     * @return true if there is a new release
     */
    public boolean checkForUpdate(String urlToVerionFile, String currentVersion, boolean rawText) {
        try {
            if (currentVersion != null && !currentVersion.equals(Constants.LIGHT_FIRMWARE_DUMMY_VERSION) && !currentVersion.equals(Constants.DASH)) {
                long numericVerion = versionNumberToNumber(currentVersion);
                URL url = new URL(urlToVerionFile);
                URLConnection urlConnection = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains(Constants.POM_PRJ_VERSION) || rawText) {
                        latestReleaseStr = inputLine.replace(Constants.POM_PRJ_VERSION, "")
                                .replace(Constants.POM_PRJ_VERSION_CLOSE, "").trim();
                        long latestRelease = versionNumberToNumber(latestReleaseStr);
                        if (numericVerion < latestRelease) {
                            return true;
                        }
                    }
                }
                in.close();
            } else {
                return false;
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Surf to the GitHub release page of the project
     *
     * @param stage main stage
     */
    @SuppressWarnings({"rawtypes"})
    public void downloadNewVersion(Stage stage) {
        stage.setAlwaysOnTop(true);
        stage.setWidth(450);
        stage.setHeight(100);
        Group root = new Group();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(CommonUtility.getWord(Constants.DOWNLOADING) + " " + Constants.FIREFLY_LUCIFERIN + " v" + latestReleaseStr);
        GUIManager.setStageIcon(stage);

        Label label = new Label("");
        final ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(280);

        Task copyWorker = createWorker();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(copyWorker.progressProperty());
        copyWorker.messageProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(newValue);
            label.setText(newValue);
        });

        final HBox hb = new HBox();
        hb.setSpacing(5);
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(label, progressBar);
        scene.setRoot(hb);
        stage.show();

        new Thread(copyWorker).start();
    }

    /**
     * Download worker
     *
     * @return downloader task
     */
    @SuppressWarnings({"all"})
    private Task createWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                try {
                    String filename;
                    if (NativeExecutor.isWindows()) {
                        filename = Constants.SETUP_FILENAME_WINDOWS;
                    } else if (NativeExecutor.isMac()) {
                        filename = Constants.SETUP_FILENAME_MAC;
                    } else {
                        List<String> commandOutput = NativeExecutor.runNativeWaitForOutput(Constants.DPKG_CHECK_CMD.split(" "));
                        if (commandOutput.size() > 0) {
                            filename = Constants.SETUP_FILENAME_LINUX_DEB;
                        } else {
                            filename = Constants.SETUP_FILENAME_LINUX_RPM;
                        }
                    }
                    URL website = new URL(Constants.GITHUB_RELEASES + latestReleaseStr + "/" + filename);
                    URLConnection connection = website.openConnection();
                    ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
                    String downloadPath = System.getProperty(Constants.HOME_PATH) + File.separator + Constants.DOCUMENTS_FOLDER
                            + File.separator + Constants.LUCIFERIN_PLACEHOLDER + File.separator;
                    downloadPath += filename;
                    FileOutputStream fos = new FileOutputStream(downloadPath);
                    long expectedSize = connection.getContentLength();
                    log.info(CommonUtility.getWord(Constants.EXPECTED_SIZE) + expectedSize);
                    long transferedSize = 0L;
                    long percentage;
                    while (transferedSize < expectedSize) {
                        transferedSize += fos.getChannel().transferFrom(rbc, transferedSize, 1 << 8);
                        percentage = ((transferedSize * 100) / expectedSize);
                        updateMessage(CommonUtility.getWord(Constants.DOWNLOAD_PROGRESS_BAR) + percentage + Constants.PERCENT);
                        updateProgress(percentage, 100);
                    }
                    if (transferedSize >= expectedSize) {
                        log.info(transferedSize + CommonUtility.getWord(Constants.DOWNLOAD_COMPLETE));
                    }
                    fos.close();
                    Thread.sleep(1000);
                    if (NativeExecutor.isWindows()) {
                        Runtime.getRuntime().exec(downloadPath);
                    }
                    NativeExecutor.exit();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                return true;
            }
        };
    }

    /**
     * Check Firefly Luciferin updates
     *
     * @param stage JavaFX stage
     * @return GlowWorm Luciferin check is done if Firefly Luciferin is up to date
     */
    public boolean checkFireflyUpdates(Stage stage) {
        boolean fireflyUpdate = false;
        if (FireflyLuciferin.config.isCheckForUpdates()) {
            log.debug("Checking for Firefly Luciferin Update");
            fireflyUpdate = checkForUpdate(Constants.GITHUB_POM_URL, FireflyLuciferin.version, false);
            if (fireflyUpdate) {
                String upgradeContext;
                if (NativeExecutor.isWindows()) {
                    upgradeContext = CommonUtility.getWord(Constants.CLICK_OK_DOWNLOAD);
                } else if (NativeExecutor.isMac()) {
                    upgradeContext = CommonUtility.getWord(Constants.CLICK_OK_DOWNLOAD_LINUX) + CommonUtility.getWord(Constants.ONCE_DOWNLOAD_FINISHED);
                } else {
                    upgradeContext = CommonUtility.getWord(Constants.CLICK_OK_DOWNLOAD_LINUX) + CommonUtility.getWord(Constants.ONCE_DOWNLOAD_FINISHED);
                }
                Optional<ButtonType> result = FireflyLuciferin.guiManager.showWebAlert(Constants.FIREFLY_LUCIFERIN,
                        CommonUtility.getWord(Constants.NEW_VERSION_AVAILABLE) + " " + upgradeContext,
                        Constants.GITHUB_CHANGELOG, Alert.AlertType.CONFIRMATION);
                ButtonType button = result.orElse(ButtonType.OK);
                if (button == ButtonType.OK) {
                    downloadNewVersion(stage);
                }
            }
        }
        return fireflyUpdate;
    }

    /**
     * Check for Glow Worm Luciferin updates
     *
     * @param fireflyUpdate check is done if Firefly Luciferin is up to date
     */
    public void checkGlowWormUpdates(boolean fireflyUpdate) {
        if (FireflyLuciferin.config.isCheckForUpdates() && !FireflyLuciferin.communicationError && !fireflyUpdate) {
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule(() -> {
                log.debug("Checking for Glow Worm Luciferin Update");
                if (!DevicesTabController.deviceTableData.isEmpty()) {
                    ArrayList<GlowWormDevice> devicesToUpdate = new ArrayList<>();
                    // Updating MQTT devices for FULL firmware or Serial devices for LIGHT firmware
                    DevicesTabController.deviceTableData.forEach(glowWormDevice -> {
                        if (!FireflyLuciferin.config.isFullFirmware() || !glowWormDevice.getDeviceName().equals(Constants.USB_DEVICE)) {
                            // USB Serial device prior to 4.3.8 and there is no version information, needs the update so fake the version
                            if (glowWormDevice.getDeviceVersion().equals(Constants.DASH)) {
                                glowWormDevice.setDeviceVersion(Constants.LIGHT_FIRMWARE_DUMMY_VERSION);
                            }
                            if (checkForUpdate(Constants.GITHUB_GLOW_WORM_URL, glowWormDevice.getDeviceVersion(), true)) {
                                // If MQTT is enabled only first instance manage the update, if MQTT is disabled every instance, manage its notification
                                if (!FireflyLuciferin.config.isFullFirmware() || JavaFXStarter.whoAmI == 1 || NetworkManager.currentTopicDiffersFromMainTopic()) {
                                    devicesToUpdate.add(glowWormDevice);
                                }
                            }
                        }
                    });
                    if (!devicesToUpdate.isEmpty()) {
                        javafx.application.Platform.runLater(() -> {
                            String deviceToUpdateStr = devicesToUpdate
                                    .stream()
                                    .map(s -> Constants.DASH + " " + "(" + s.getDeviceIP() + ") " + s.getDeviceName() + "\n")
                                    .collect(Collectors.joining());
                            String deviceContent;
                            if (devicesToUpdate.size() == 1) {
                                deviceContent = FireflyLuciferin.config.isFullFirmware() ? CommonUtility.getWord(Constants.DEVICE_UPDATED) : CommonUtility.getWord(Constants.DEVICE_UPDATED_LIGHT);
                            } else {
                                deviceContent = CommonUtility.getWord(Constants.DEVICES_UPDATED);
                            }
                            String upgradeMessage;
                            if (NativeExecutor.isLinux()) {
                                upgradeMessage = CommonUtility.getWord(Constants.UPDATE_NEEDED_LINUX);
                            } else {
                                upgradeMessage = CommonUtility.getWord(Constants.UPDATE_NEEDED);
                            }
                            Optional<ButtonType> result = FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN,
                                    CommonUtility.getWord(Constants.NEW_FIRMWARE_AVAILABLE), deviceContent + deviceToUpdateStr
                                            + (FireflyLuciferin.config.isFullFirmware() ? CommonUtility.getWord(Constants.UPDATE_BACKGROUND) : upgradeMessage)
                                            + "\n", Alert.AlertType.CONFIRMATION);
                            ButtonType button = result.orElse(ButtonType.OK);
                            if (FireflyLuciferin.config.isFullFirmware()) {
                                if (button == ButtonType.OK) {
                                    if (FireflyLuciferin.RUNNING) {
                                        FireflyLuciferin.guiManager.stopCapturingThreads(true);
                                        CommonUtility.sleepSeconds(15);
                                    }
                                    if (FireflyLuciferin.config.isMqttEnable()) {
                                        log.debug("Starting web server");
                                        NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.UPDATE_MQTT_TOPIC),
                                                CommonUtility.toJsonString(new WebServerStarterDto(true)));
                                        devicesToUpdate.forEach(glowWormDevice -> executeUpdate(glowWormDevice, false));
                                    } else {
                                        devicesToUpdate.forEach(glowWormDevice -> {
                                            log.debug("Starting web server: " + glowWormDevice.getDeviceIP());
                                            TcpClient.httpGet(CommonUtility.toJsonString(new WebServerStarterDto(true)),
                                                    NetworkManager.getTopic(Constants.UPDATE_MQTT_TOPIC), glowWormDevice.getDeviceIP());
                                            log.debug("Updating: " + glowWormDevice.getDeviceIP());
                                            CommonUtility.sleepSeconds(5);
                                            executeUpdate(glowWormDevice, false);
                                        });
                                        FireflyLuciferin.guiManager.startCapturingThreads();
                                    }
                                }
                            } else {
                                if (button == ButtonType.OK) {
                                    if (NativeExecutor.isLinux()) {
                                        devicesToUpdate.forEach(glowWormDevice -> executeUpdate(glowWormDevice, true));
                                    } else {
                                        FireflyLuciferin.guiManager.surfToURL(Constants.WEB_INSTALLER_URL);
                                    }
                                }
                            }
                        });
                    }
                }
            }, 15, TimeUnit.SECONDS);
        }
    }

    /**
     * Execute the firmware upgrade on the microcontroller
     *
     * @param glowWormDevice       device info
     * @param downloadFirmwareOnly if true download the firmware but does not execeute the update (LIGHT firmware)
     */
    void executeUpdate(GlowWormDevice glowWormDevice, boolean downloadFirmwareOnly) {
        try {
            // Firmware previous than v4.0.3 does not support auto update
            if (versionNumberToNumber(glowWormDevice.getDeviceVersion()) > versionNumberToNumber(Constants.MINIMUM_FIRMWARE_FOR_AUTO_UPGRADE)) {
                CommonUtility.sleepSeconds(4);
                String filename;
                if (FireflyLuciferin.config.isFullFirmware()) {
                    filename = Constants.UPDATE_FILENAME;
                } else {
                    filename = Constants.UPDATE_FILENAME_LIGHT;
                }
                if (glowWormDevice.getDeviceBoard().equals(Constants.ESP8266)) {
                    filename = filename.replace(Constants.DEVICE_BOARD, Constants.ESP8266);
                } else if (glowWormDevice.getDeviceBoard().equals(Constants.ESP32)) {
                    filename = filename.replace(Constants.DEVICE_BOARD, Constants.ESP32);
                }
                downloadFile(filename);
                Path localFile = Paths.get(System.getProperty(Constants.HOME_PATH) + File.separator + Constants.DOCUMENTS_FOLDER
                        + File.separator + Constants.LUCIFERIN_PLACEHOLDER + File.separator + filename);
                if (!downloadFirmwareOnly) {
                    // Send data
                    postDataToMicrocontroller(glowWormDevice, localFile);
                }
            } else {
                if (NativeExecutor.isWindows()) {
                    FireflyLuciferin.guiManager.showLocalizedNotification(Constants.CANT_UPGRADE_TOO_OLD, Constants.MANUAL_UPGRADE, TrayIcon.MessageType.INFO);
                } else {
                    FireflyLuciferin.guiManager.showLocalizedAlert(Constants.FIREFLY_LUCIFERIN, Constants.CANT_UPGRADE_TOO_OLD,
                            Constants.MANUAL_UPGRADE, Alert.AlertType.INFORMATION);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * MimeMultipartData for ESP microcontrollers, standard POST with Java 11 does not work as expected
     * Java 16 broke it again
     *
     * @param glowWormDevice deviceToUpgrade
     * @param path           firmware path to file
     * @throws IOException something bad happened in the connection
     */
    private void postDataToMicrocontroller(GlowWormDevice glowWormDevice, Path path) throws IOException {
        String boundary = new BigInteger(256, new Random()).toString();
        String url = Constants.UPGRADE_URL.replace("{0}", glowWormDevice.getDeviceIP());

        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty(Constants.UPGRADE_CONTENT_TYPE, Constants.UPGRADE_MULTIPART + boundary);

        byte[] input1 = Constants.MULTIPART_1.replace("{0}", boundary).getBytes(StandardCharsets.UTF_8);
        byte[] input2 = Constants.MULTIPART_2.replace("{0}", path.getFileName().toString()).getBytes(StandardCharsets.UTF_8);
        byte[] input3 = (Files.readAllBytes(path));
        byte[] input4 = Constants.MULTIPART_4.getBytes(StandardCharsets.UTF_8);
        byte[] input5 = Constants.MULTIPART_5.replace("{0}", boundary).getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(input1);
        output.write(input2);
        output.write(input3);
        output.write(input4);
        output.write(input5);
        // Write POST data
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = output.toByteArray();
            os.write(input, 0, input.length);
        }
        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            log.debug("Response=" + response);
        }
        if (Constants.OK.equals(response.toString())) {
            log.debug(CommonUtility.getWord(Constants.FIRMWARE_UPGRADE_RES), glowWormDevice.getDeviceName(), Constants.OK);
            if (!FireflyLuciferin.config.isMqttEnable()) {
                String notificationContext = glowWormDevice.getDeviceName() + " " + CommonUtility.getWord(Constants.DEVICEUPGRADE_SUCCESS);
                if (NativeExecutor.isWindows()) {
                    FireflyLuciferin.guiManager.showNotification(CommonUtility.getWord(Constants.UPGRADE_SUCCESS), notificationContext, TrayIcon.MessageType.INFO);
                } else {
                    FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN, CommonUtility.getWord(Constants.UPGRADE_SUCCESS), notificationContext, Alert.AlertType.INFORMATION);
                }
            }
        } else {
            log.debug(CommonUtility.getWord(Constants.FIRMWARE_UPGRADE_RES), glowWormDevice.getDeviceName(), Constants.KO);
        }
    }

    /**
     * Download Glow Worm Luciferin firmware
     *
     * @param filename file to download
     * @throws IOException error during download
     */
    @SuppressWarnings({"all"})
    void downloadFile(String filename) throws IOException {
        URL website = new URL(Constants.GITHUB_RELEASES_FIRMWARE + latestReleaseStr + "/" + filename);
        URLConnection connection = website.openConnection();
        ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
        String downloadPath = System.getProperty(Constants.HOME_PATH) + File.separator + Constants.DOCUMENTS_FOLDER
                + File.separator + Constants.LUCIFERIN_PLACEHOLDER + File.separator;
        downloadPath += filename;
        FileOutputStream fos = new FileOutputStream(downloadPath);
        long expectedSize = connection.getContentLength();
        log.info(CommonUtility.getWord(Constants.EXPECTED_SIZE) + expectedSize);
        long transferedSize = 0L;
        while (transferedSize < expectedSize) {
            transferedSize += fos.getChannel().transferFrom(rbc, transferedSize, 1 << 8);
        }
        if (transferedSize >= expectedSize) {
            log.info(transferedSize + " " + CommonUtility.getWord(Constants.DOWNLOAD_COMPLETE));
        }
        fos.close();
    }

    /**
     * Check for updates
     *
     * @param stage JavaFX stage
     */
    public void checkForUpdates(Stage stage) {
        UpgradeManager vm = new UpgradeManager();
        // Check Firefly updates
        boolean fireflyUpdate = false;
        if (JavaFXStarter.whoAmI == 1) {
            fireflyUpdate = vm.checkFireflyUpdates(stage);
        }
        // If Firefly Luciferin is up to date, check for the Glow Worm Luciferin firmware
        vm.checkGlowWormUpdates(fireflyUpdate);
    }

    /**
     * Check if the connected device match the minimum firmware version requirements for this Firefly Luciferin version
     * Returns true if the connected device have a compatible firmware version
     *
     * @return true or false
     */
    public Boolean firmwareMatchMinimumRequirements() {
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        GlowWormDevice glowWormDeviceInUse = CommonUtility.getDeviceToUse();
        if (glowWormDeviceInUse != null && glowWormDeviceInUse.getMac() != null && !Constants.DASH.equals(glowWormDeviceInUse.getDeviceVersion())
                && !glowWormDeviceInUse.getDeviceVersion().isEmpty() && !Constants.LIGHT_FIRMWARE_DUMMY_VERSION.equals(glowWormDeviceInUse.getDeviceVersion())) {
            String minimumFirmwareVersionProp = propertiesLoader.retrieveProperties(Constants.PROP_MINIMUM_FIRMWARE_VERSION);
            long minimumFirmwareVersion = versionNumberToNumber(minimumFirmwareVersionProp);
            long deviceVersion = versionNumberToNumber(glowWormDeviceInUse.getDeviceVersion());
            return (deviceVersion >= minimumFirmwareVersion);
        } else {
            return null;
        }
    }
}
