/*
  UpgradeManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020  Davide Perini

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
package org.dpsoftware.gui;

import com.sun.jna.Platform;
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
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class UpgradeManager {

    private static final Logger logger = LoggerFactory.getLogger(UpgradeManager.class);

    String latestReleaseStr = "";

    /**
     * Check for Update
     * @return true if there is a new release
     */
    public boolean checkForUpdate(String urlToVerionFile, String currentVersion, boolean rawText) {

        try {
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
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return false;

    }

    /**
     * Transform release version to a comparable number with other releases
     * it handle up to 1000 Major, minor, hotfix numbers
     * @param latestReleaseStr Release version
     * @return comparable number with other releases
     */
    long versionNumberToNumber(String latestReleaseStr) {

        String[] majorMinorHotfix = latestReleaseStr.split("\\.");
        return Long.parseLong((majorMinorHotfix[0]) + 1000000)
                + Long.parseLong((majorMinorHotfix[1] + 1000))
                + Long.parseLong((majorMinorHotfix[2]));

    }

    /**
     * Surf to the GitHub release page of the project
     * @param stage main stage
     */
    public void downloadNewVersion(Stage stage) {

        stage.setAlwaysOnTop(true);
        stage.setWidth(450);
        stage.setHeight(100);
        Group root = new Group();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(Constants.DOWNLOADING + " " + Constants.FIREFLY_LUCIFERIN + " v" + latestReleaseStr);
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
     * @return downloader task
     */
    private Task createWorker() {

        return new Task() {
            @Override
            protected Object call() throws Exception {

                try {
                    String filename;
                    if (Platform.isWindows()) {
                        filename = Constants.SETUP_FILENAME_WINDOWS;
                    } else if (Platform.isMac()) {
                        filename = Constants.SETUP_FILENAME_MAC;
                    } else {
                        List<String> commandOutput = NativeExecutor.runNative(Constants.DPKG_CHECK_CMD);
                        if (commandOutput.size() > 0) {
                            filename = Constants.SETUP_FILENAME_LINUX_DEB;
                        } else {
                            filename = Constants.SETUP_FILENAME_LINUX_RPM;
                        }
                    }
                    URL website = new URL(Constants.GITHUB_RELEASES + latestReleaseStr + "/" + filename);
                    URLConnection connection = website.openConnection();
                    ReadableByteChannel rbc = Channels.newChannel( connection.getInputStream());
                    String downloadPath = System.getProperty(Constants.HOME_PATH) + File.separator + Constants.DOCUMENTS_FOLDER
                            + File.separator + Constants.LUCIFERIN_PLACEHOLDER + File.separator;
                    downloadPath += filename;
                    FileOutputStream fos = new FileOutputStream(downloadPath);
                    long expectedSize = connection.getContentLength();
                    logger.info(Constants.EXPECTED_SIZE + expectedSize);
                    long transferedSize = 0L;
                    long percentage;
                    while(transferedSize < expectedSize) {
                        transferedSize += fos.getChannel().transferFrom( rbc, transferedSize, 1 << 8);
                        percentage = ((transferedSize * 100) / expectedSize);
                        updateMessage(Constants.DOWNLOAD_PROGRESS_BAR + percentage + Constants.PERCENT);
                        updateProgress(percentage, 100);
                    }
                    if (transferedSize >= expectedSize) {
                        logger.info(transferedSize + Constants.DOWNLOAD_COMPLETE);
                    }
                    fos.close();
                    Thread.sleep(1000);
                    if (Platform.isWindows()) {
                        Runtime.getRuntime().exec(downloadPath);
                    }
                    System.exit(0);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
                return true;

            }
        };

    }

    /**
     * Check Firefly Luciferin updates
     * @param stage JavaFX stage
     * @param guiManager running GuiManager instance
     * @return GlowWorm Luciferin check is done if Firefly Luciferin is up to date
     */
    boolean checkFireflyUpdates(Stage stage, GUIManager guiManager) {

        logger.debug("Checking for Firefly Luciferin Update");
        boolean fireflyUpdate = checkForUpdate(Constants.GITHUB_POM_URL, FireflyLuciferin.version, false);
        if (FireflyLuciferin.config.isCheckForUpdates() && fireflyUpdate) {
            String upgradeContext;
            if (com.sun.jna.Platform.isWindows()) {
                if (FireflyLuciferin.config.isMqttEnable()) {
                    upgradeContext = Constants.CLICK_OK_DOWNLOAD;
                } else {
                    upgradeContext = Constants.CLICK_OK_DOWNLOAD_LIGHT;
                }
            } else if (com.sun.jna.Platform.isMac()) {
                if (FireflyLuciferin.config.isMqttEnable()) {
                    upgradeContext = Constants.CLICK_OK_DOWNLOAD_LINUX + Constants.ONCE_DOWNLOAD_FINISHED;
                } else {
                    upgradeContext = Constants.CLICK_OK_DOWNLOAD_LINUX + Constants.ONCE_DOWNLOAD_FINISHED_LIGHT;
                }
            } else {
                if (FireflyLuciferin.config.isMqttEnable()) {
                    upgradeContext = Constants.CLICK_OK_DOWNLOAD_LINUX + Constants.ONCE_DOWNLOAD_FINISHED;
                } else {
                    upgradeContext = Constants.CLICK_OK_DOWNLOAD_LINUX + Constants.ONCE_DOWNLOAD_FINISHED_LIGHT;
                }
            }
            Optional<ButtonType> result = guiManager.showAlert(Constants.FIREFLY_LUCIFERIN, Constants.NEW_VERSION_AVAILABLE,
                    upgradeContext, Alert.AlertType.CONFIRMATION);
            ButtonType button = result.orElse(ButtonType.OK);
            if (button == ButtonType.OK) {
                downloadNewVersion(stage);
            }
        }
        return fireflyUpdate;

    }

    /**
     * Check for Glow Worm Luciferin updates
     * @param guiManager running GuiManager instance
     * @param fireflyUpdate check is done if Firefly Luciferin is up to date
     */
    void checkGlowWormUpdates(GUIManager guiManager, boolean fireflyUpdate) {

        if (FireflyLuciferin.config.isCheckForUpdates() && !FireflyLuciferin.communicationError && !fireflyUpdate) {
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule(() -> {
                logger.debug("Checking for Glow Worm Luciferin Update");
                if (!SettingsController.deviceTableData.isEmpty()) {
                    ArrayList<GlowWormDevice> devicesToUpdate = new ArrayList<>();
                    SettingsController.deviceTableData.forEach(glowWormDevice -> {
                        if (!glowWormDevice.getDeviceName().equals(Constants.USB_DEVICE)) {
                            if (checkForUpdate(Constants.GITHUB_GLOW_WORM_URL, glowWormDevice.getDeviceVersion(), true)) {
                                devicesToUpdate.add(glowWormDevice);
                            }
                        }
                    });
                    if (!devicesToUpdate.isEmpty()) {
                        javafx.application.Platform.runLater(() -> {
                            String deviceToUpdateStr = devicesToUpdate
                                    .stream()
                                    .map(s -> Constants.DASH + " " + "("+ s.getDeviceIP() +") " + s.getDeviceName() + "\n")
                                    .collect(Collectors.joining());
                            String deviceContent;
                            if (devicesToUpdate.size() == 1) {
                                deviceContent = Constants.DEVICE_UPDATED;
                            } else {
                                deviceContent = Constants.DEVICES_UPDATED;
                            }
                            Optional<ButtonType> result = guiManager.showAlert(Constants.FIREFLY_LUCIFERIN, Constants.NEW_FIRMWARE_AVAILABLE,
                                    deviceContent + deviceToUpdateStr + Constants.UPDATE_BACKGROUND + "\n", Alert.AlertType.CONFIRMATION);
                            ButtonType button = result.orElse(ButtonType.OK);
                            if (button == ButtonType.OK) {
                                FireflyLuciferin.guiManager.mqttManager.publishToTopic(Constants.UPDATE_MQTT_TOPIC, Constants.START_WEB_SERVER_MSG);
                                devicesToUpdate.forEach(this::executeUpdate);
                            }
                        });
                    }
                }
            }, 30, TimeUnit.SECONDS);
        }

    }

    /**
     * Execute the firmware upgrade on the microcontroller
     * @param glowWormDevice device info
     */
    void executeUpdate(GlowWormDevice glowWormDevice) {

        try {
            // Firmware previous than v4.0.3 does not support auto update
            if (versionNumberToNumber(glowWormDevice.getDeviceVersion()) > versionNumberToNumber(Constants.MINIMUM_FIRMWARE_FOR_AUTO_UPGRADE)) {
                TimeUnit.SECONDS.sleep(4);
                var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
                String filename = null;
                if (glowWormDevice.getDeviceBoard().equals(Constants.ESP8266)) {
                    filename = Constants.UPDATE_FILENAME.replace(Constants.DEVICE_BOARD, Constants.ESP8266);
                } else if (glowWormDevice.getDeviceBoard().equals(Constants.ESP32)) {
                    filename = Constants.UPDATE_FILENAME.replace(Constants.DEVICE_BOARD, Constants.ESP32);
                }

                downloadFile(filename);

                Path localFile = Paths.get(System.getProperty(Constants.HOME_PATH) + File.separator + Constants.DOCUMENTS_FOLDER
                        + File.separator + Constants.LUCIFERIN_PLACEHOLDER + File.separator + filename);

                Map<Object, Object> data = new LinkedHashMap<>();
                data.put(Constants.UPGRADE_FILE, localFile);
                String boundary = new BigInteger(256, new Random()).toString();

                HttpRequest request = HttpRequest.newBuilder()
                        .header(Constants.UPGRADE_CONTENT_TYPE, Constants.UPGRADE_MULTIPART + boundary)
                        .POST(ofMimeMultipartData(data, boundary))
                        .uri(URI.create(Constants.UPGRADE_URL.replace(Constants.DASH, glowWormDevice.getDeviceIP())))
                        .build();

                client.send(request, HttpResponse.BodyHandlers.discarding());

                SettingsController.deviceTableData.remove(glowWormDevice);

            } else {
                FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN, Constants.CANT_UPGRADE_TOO_OLD,
                        Constants.MANUAL_UPGRADE, Alert.AlertType.INFORMATION);
            }
        } catch (InterruptedException | IOException e) {
            logger.error(e.getMessage());
        }

    }

    /**
     * MimeMultipartData for ESP microcontrollers, standard POST with Java 11 does not work as expected
     * @param data data to be transferred
     * @param boundary boundary
     * @return body publisher
     * @throws IOException something bad happened in the connection
     */
    public static HttpRequest.BodyPublisher ofMimeMultipartData(Map<Object, Object> data, String boundary) throws IOException {

        var byteArrays = new ArrayList<byte[]>();
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            byteArrays.add(separator);
            if (entry.getValue() instanceof Path) {
                var path = (Path) entry.getValue();
                String mimeType = Files.probeContentType(path);
                byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName()
                        + "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                byteArrays.add(Files.readAllBytes(path));
                byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);

    }

    /**
     * Download Glow Worm Luciferin firmware
     * @param filename file to download
     * @throws IOException error during download
     */
    void downloadFile(String filename) throws IOException {

        URL website = new URL(Constants.GITHUB_RELEASES_FIRMWARE + latestReleaseStr + "/" + filename);
        URLConnection connection = website.openConnection();
        ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
        String downloadPath = System.getProperty(Constants.HOME_PATH) + File.separator + Constants.DOCUMENTS_FOLDER
                + File.separator + Constants.LUCIFERIN_PLACEHOLDER + File.separator;
        downloadPath += filename;
        FileOutputStream fos = new FileOutputStream(downloadPath);
        long expectedSize = connection.getContentLength();
        logger.info(Constants.EXPECTED_SIZE + expectedSize);
        long transferedSize = 0L;
        while(transferedSize < expectedSize) {
            transferedSize += fos.getChannel().transferFrom( rbc, transferedSize, 1 << 8);
        }
        if (transferedSize >= expectedSize) {
            logger.info(transferedSize + Constants.DOWNLOAD_COMPLETE);
        }
        fos.close();

    }

}
