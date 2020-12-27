/*
  GUIManager.java

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

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.MQTTManager;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
@Slf4j
@NoArgsConstructor
public class GUIManager extends JFrame {

    private Stage stage;
    // Tray icon
    TrayIcon trayIcon = null;
    // create a popup menu
    PopupMenu popup = new PopupMenu();
    // Label and framerate dialog
    @Getter JEditorPane jep = new JEditorPane();
    @Getter JFrame jFrame = new JFrame(Constants.FIREFLY_LUCIFERIN);
    // Menu items for start and stop
    MenuItem stopItem;
    MenuItem startItem;
    // Tray icons
    Image imagePlay;
    Image imageStop;
    Image imageGreyStop;
    MQTTManager mqttManager;


    /**
     * Constructor
     * @param mqttManager class for mqtt management
     * @param stage JavaFX stage
     * @throws HeadlessException GUI exception
     */
    public GUIManager(MQTTManager mqttManager, Stage stage) throws HeadlessException {

        this.mqttManager = mqttManager;
        this.stage = stage;

    }

    /**
     *
     * @param fxml GUI file
     * @return fxmlloader
     * @throws IOException file exception
     */
    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GUIManager.class.getResource( fxml + Constants.FXML));
        return fxmlLoader.load();
    }

    /**
     * Create and initialize tray icon menu
     */
    public void initTray() {

        if (SystemTray.isSupported() && !com.sun.jna.Platform.isLinux()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            imagePlay = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY));
            imageStop = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_STOP));
            imageGreyStop = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_GREY));

            // create menu item for the default action
            stopItem = new MenuItem(Constants.STOP);
            startItem = new MenuItem(Constants.START);
            MenuItem settingsItem = new MenuItem(Constants.SETTINGS);
            MenuItem infoItem = new MenuItem(Constants.INFO);
            MenuItem exitItem = new MenuItem(Constants.EXIT);

            // create a action listener to listen for default action executed on the tray icon
            ActionListener listener = initPopupMenuListener();

            stopItem.addActionListener(listener);
            startItem.addActionListener(listener);
            exitItem.addActionListener(listener);
            settingsItem.addActionListener(listener);
            infoItem.addActionListener(listener);
            popup.add(startItem);
            popup.addSeparator();

            initGrabMode();

            popup.addSeparator();
            popup.add(settingsItem);
            popup.add(infoItem);
            popup.addSeparator();
            popup.add(exitItem);
            // construct a TrayIcon
            if (FireflyLuciferin.communicationError) {
                trayIcon = new TrayIcon(imageGreyStop, Constants.FIREFLY_LUCIFERIN, popup);
            } else {
                trayIcon = new TrayIcon(imageStop, Constants.FIREFLY_LUCIFERIN, popup);
            }
            // set the TrayIcon properties
            trayIcon.addActionListener(listener);
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error(String.valueOf(e));
            }
        }

        if (!com.sun.jna.Platform.isWindows() && !com.sun.jna.Platform.isMac()) {
            showSettingsDialog();
        }

        checkForUpdates();

    }

    /**
     *  Check for updates
     */
    void checkForUpdates() {

        UpgradeManager vm = new UpgradeManager();
        // Check Firefly updates
        boolean fireflyUpdate = vm.checkFireflyUpdates(stage, this);
        // If Firefly Luciferin is up to date, check for the Glow Worm Luciferin firmware
        vm.checkGlowWormUpdates(this, fireflyUpdate);

    }

    /**
     * Init popup menu
     * @return tray icon listener
     */
    ActionListener initPopupMenuListener() {

        return actionEvent -> {
            if (actionEvent.getActionCommand() == null) {
                if (FireflyLuciferin.RUNNING) {
                    stopCapturingThreads();
                } else {
                    startCapturingThreads();
                }
            } else {
                switch (actionEvent.getActionCommand()) {
                    case Constants.STOP -> stopCapturingThreads();
                    case Constants.START -> startCapturingThreads();
                    case Constants.SETTINGS -> showSettingsDialog();
                    case Constants.INFO -> showFramerateDialog();
                    default -> {
                        if (FireflyLuciferin.RUNNING) {
                            stopCapturingThreads();
                        }
                        try {
                            TimeUnit.SECONDS.sleep(4);
                            log.debug(Constants.CLEAN_EXIT);
                            FireflyLuciferin.exit();
                        } catch (InterruptedException e) {
                            log.error(e.getMessage());
                        }
                    }
                }
            }
        };

    }

    /**
     * Add params in the tray icon menu for every ledMatrix found in the FireflyLuciferin.yaml
     */
    void initGrabMode() {

        FireflyLuciferin.config.getLedMatrix().forEach((ledMatrixKey, ledMatrix) -> {

            CheckboxMenuItem checkboxMenuItem = new CheckboxMenuItem(ledMatrixKey,
                    ledMatrixKey.equals(FireflyLuciferin.config.getDefaultLedMatrix()));
            checkboxMenuItem.addItemListener(itemListener -> {
                log.info(Constants.STOPPING_THREADS);
                stopCapturingThreads();
                for (int i=0; i < popup.getItemCount(); i++) {
                    if (popup.getItem(i) instanceof CheckboxMenuItem) {
                        if (!popup.getItem(i).getLabel().equals(checkboxMenuItem.getLabel())) {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(false);
                        } else {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(true);
                            FireflyLuciferin.config.setDefaultLedMatrix(checkboxMenuItem.getLabel());
                            log.info(Constants.CAPTURE_MODE_CHANGED + checkboxMenuItem.getLabel());
                            startCapturingThreads();
                        }
                    }
                }
            });
            popup.add(checkboxMenuItem);

        });

    }

    /**
     * Show alert in a JavaFX dialog
     * @param title dialog title
     * @param header dialog header
     * @param content dialog msg
     * @param alertType alert type
     * @return an Object when we can listen for commands
     */
    public Optional<ButtonType> showAlert(String title, String header, String content, Alert.AlertType alertType) {

        Platform.setImplicitExit(false);
        Alert alert = new Alert(alertType);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        setStageIcon(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setContentText(content);
        return alert.showAndWait();

    }

    /**
     * Show a dialog with all the settings
     */
    void showSettingsDialog() {

        String fxml;
        if (com.sun.jna.Platform.isWindows() || com.sun.jna.Platform.isMac()) {
            fxml = Constants.FXML_SETTINGS;
        } else {
            fxml = Constants.FXML_SETTINGS_LINUX;
        }
        showStage(fxml);

    }

    /**
     * Show a dialog with a framerate counter
     */
    void showFramerateDialog() {

        showStage(Constants.FXML_INFO);

    }

    /**
     * Show a stage
     * @param stageName stage to show
     */
    void showStage(String stageName) {

        Platform.runLater(() -> {
            try {
                Scene scene = new Scene(loadFXML(stageName));
                if(stage == null) {
                    stage = new Stage();
                }
                stage.resizableProperty().setValue(Boolean.FALSE);
                stage.setScene(scene);
                stage.setTitle("  " + Constants.FIREFLY_LUCIFERIN);
                setStageIcon(stage);
                stage.show();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });

    }

    /**
     * Set icon for every stage
     * @param stage in use
     */
    public static void setStageIcon(Stage stage) {
        stage.getIcons().add(new javafx.scene.image.Image(String.valueOf(GUIManager.class.getResource(Constants.IMAGE_TRAY_STOP))));
    }

    /**
     * Stop capturing threads
     */
    public void stopCapturingThreads() {

        if (FireflyLuciferin.RUNNING) {
            if (trayIcon != null) {
                trayIcon.setImage(imageStop);
                popup.remove(0);
                popup.insert(startItem, 0);
            }
            if (mqttManager != null) {
                // lednum 0 will stop stream on the firmware immediately
                if (FireflyLuciferin.config.isMqttEnable() && FireflyLuciferin.config.isMqttStream()) {
                    mqttManager.stream("{\"lednum\":0}");
                } else {
                    mqttManager.publishToTopic(FireflyLuciferin.config.getMqttTopic(), Constants.STATE_OFF_SOLID);
                }
                try {
                    TimeUnit.SECONDS.sleep(4);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
                if (FireflyLuciferin.config.isMqttEnable() && FireflyLuciferin.config.isMqttStream()) {
                    mqttManager.publishToTopic(FireflyLuciferin.config.getMqttTopic(), Constants.STATE_OFF_SOLID);
                }
            }
            FireflyLuciferin.RUNNING = false;
            if ((FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name()))
                    || (FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name()))
                    || (FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.AVFVIDEOSRC.name()))) {
                FireflyLuciferin.pipe.stop();
            }
            FireflyLuciferin.FPS_PRODUCER_COUNTER = 0;
            FireflyLuciferin.FPS_CONSUMER_COUNTER = 0;
        }

    }

    /**
     * Start capturing threads
     */
    public void startCapturingThreads() {

        if (!FireflyLuciferin.RUNNING) {
            if (trayIcon != null) {
                trayIcon.setImage(imagePlay);
                popup.remove(0);
                popup.insert(stopItem, 0);
            }
            FireflyLuciferin.RUNNING = true;
            if (mqttManager != null) {
                try {
                    TimeUnit.SECONDS.sleep(4);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
                if ((FireflyLuciferin.config.isMqttEnable() && FireflyLuciferin.config.isMqttStream())) {
                    mqttManager.publishToTopic(FireflyLuciferin.config.getMqttTopic(), Constants.STATE_ON_GLOWWORMWIFI);
                } else {
                    mqttManager.publishToTopic(FireflyLuciferin.config.getMqttTopic(), Constants.STATE_ON_GLOWWORM);
                }
            }
        }

    }

    /**
     * Surf to the project GitHub page
     */
    public void surfToGitHub() {

        if(Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                URI github = new URI(Constants.GITHUB_URL);
                desktop.browse(github);
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }

    }

}