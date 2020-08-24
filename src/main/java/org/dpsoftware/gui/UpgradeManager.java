/*
  UpgradeManager.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/
package org.dpsoftware.gui;

import com.sun.jna.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dpsoftware.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

@Getter
@NoArgsConstructor
public class UpgradeManager {

    private static final Logger logger = LoggerFactory.getLogger(UpgradeManager.class);

    String latestReleaseStr = "";

    /**
     * Check for Update
     * @return true if there is a new release
     */
    public boolean checkForUpdate() {
        try {
            int numericVerion = Integer.parseInt(Constants.FIREFLY_LUCIFERIN_VERSION.replace(".", ""));
            URL url = new URL(Constants.GITHUB_POM_URL);
            URLConnection urlConnection = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains(Constants.POM_PRJ_VERSION)) {
                    latestReleaseStr = inputLine.replace(Constants.POM_PRJ_VERSION, "")
                            .replace(Constants.POM_PRJ_VERSION_CLOSE, "").trim();
                    int latestRelease = Integer.parseInt(latestReleaseStr.replace(".",""));
                    if (numericVerion < latestRelease) {
                        return true;
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            logger.error(e.toString());
        }
        return false;
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
        stage.setTitle(Constants.DOWNLOADING + " " + Constants.FIREFLY_LUCIFERIN);
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
                    } else {
                        //TODO sostituisci con FireflyLuciferinLinux.tar.gz
                        filename = Constants.SETUP_FILENAME_LINUX;
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
                    e.printStackTrace();
                }
                return true;

            }
        };

    }

}
