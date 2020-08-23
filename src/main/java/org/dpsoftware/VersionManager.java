package org.dpsoftware;

import com.sun.jna.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

@Getter
@NoArgsConstructor
public class VersionManager {

    private static final Logger logger = LoggerFactory.getLogger(VersionManager.class);

    public String version = "1.1.1";
    String latestReleaseStr = "";

    /**
     * Check for Update
     * @return true if there is a new release
     */
    public boolean checkForUpdate() {
        String urlStr = "https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/pom.xml";
        try {
            int numericVerion = Integer.parseInt(version.replace(".", ""));
            URL url = new URL(urlStr);
            URLConnection urlConnection = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("<project.version>")) {
                    latestReleaseStr = inputLine.replace("<project.version>", "")
                            .replace("</project.version>", "").trim();
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
     * @param stage
     */
    public void downloadNewVersion(Stage stage) {

        stage.setAlwaysOnTop(true);
        stage.setWidth(450);
        stage.setHeight(100);
        Group root = new Group();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Downloading Firefly Luciferin");
        FireflyLuciferin.guiManager.setStageIcon(stage);

        Label label = new Label("");
        Task copyWorker;
        final ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(280);

        copyWorker = createWorker();
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
     * @return
     */
    public Task createWorker() {

        return new Task() {
            @Override
            protected Object call() throws Exception {

                try {
                    String filename = "";
                    if (Platform.isWindows()) {
                        filename = "FireflyLuciferinSetup.exe";
                    } else {
                        //TODO sostituisci con FireflyLuciferinLinux.tar.gz
                        filename = "FireflyLuciferinSetup.exe";
                    }
                    URL website = new URL("https://github.com/sblantipodi/firefly_luciferin/releases/download/v" + latestReleaseStr + "/" + filename);
                    URLConnection connection = website.openConnection();
                    ReadableByteChannel rbc = Channels.newChannel( connection.getInputStream());
                    String downloadPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "FireflyLuciferin" + File.separator;
                    if (Platform.isWindows()) {
                        downloadPath += filename;
                    } else {
                        downloadPath += filename;
                    }
                    FileOutputStream fos = new FileOutputStream(downloadPath);
                    long expectedSize = connection.getContentLength();
                    logger.info("Expected size: " + expectedSize);
                    long transferedSize = 0L;
                    long percentage = 0;
                    while(transferedSize < expectedSize) {
                        transferedSize += fos.getChannel().transferFrom( rbc, transferedSize, 1 << 8);
                        percentage = ((transferedSize * 100) / expectedSize);
                        updateMessage("Downloading : " + percentage + "%");
                        updateProgress(percentage, 100);
                    }
                    if (transferedSize >= expectedSize) {
                        logger.info(transferedSize + " download completed");
                    }
                    fos.close();
                    if (Platform.isWindows()) {
                        Runtime.getRuntime().exec(downloadPath);
                        Thread.sleep(1000);
                        System.exit(0);
                    } else {
                        Thread.sleep(1000);
                        System.exit(0);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;

            }
        };

    }

}
