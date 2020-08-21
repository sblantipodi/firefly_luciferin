/*
  GUIManager.java

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
import lombok.SneakyThrows;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.MQTTManager;
import org.dpsoftware.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@NoArgsConstructor
public class GUIManager extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(GUIManager.class);

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
                logger.error(String.valueOf(e));
            }
        }

        if (!com.sun.jna.Platform.isWindows()) {
            showSettingsDialog();
        }

        UpgradeManager vm = new UpgradeManager();
        if (vm.checkForUpdate()) {
            String upgradeContext;
            if (com.sun.jna.Platform.isWindows()) {
                upgradeContext = Constants.CLICK_OK_DOWNLOAD;
            } else {
                upgradeContext = Constants.CLICK_OK_DOWNLOAD_LINUX + Constants.ONCE_DOWNLOAD_FINISHED;
            }
            Optional<ButtonType> result = showAlert(Constants.FIREFLY_LUCIFERIN, Constants.NEW_VERSION_AVAILABLE,
                    upgradeContext, Alert.AlertType.CONFIRMATION);
            ButtonType button = result.orElse(ButtonType.OK);
            if (button == ButtonType.OK) {
                vm.downloadNewVersion(stage);
            }
        }

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
                        stopCapturingThreads();
                        System.exit(0);
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
                logger.info(Constants.STOPPING_THREADS);
                stopCapturingThreads();
                for (int i=0; i < popup.getItemCount(); i++) {
                    if (popup.getItem(i) instanceof CheckboxMenuItem) {
                        if (!popup.getItem(i).getLabel().equals(checkboxMenuItem.getLabel())) {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(false);
                        } else {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(true);
                            FireflyLuciferin.config.setDefaultLedMatrix(checkboxMenuItem.getLabel());
                            logger.info(Constants.CAPTURE_MODE_CHANGED + checkboxMenuItem.getLabel());
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
     * @param context dialog msg
     * @param alertType alert type
     * @return an Object when we can listen for commands
     */
    public Optional<ButtonType> showAlert(String title, String header, String context, Alert.AlertType alertType) {

        Platform.setImplicitExit(false);
        Alert alert = new Alert(alertType);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        setStageIcon(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setContentText(context);
        return alert.showAndWait();

    }

    /**
     * Show a dialog with all the settings
     */
    void showSettingsDialog() {

        String fxml;
        if (com.sun.jna.Platform.isWindows()) {
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
                if (stageName.equals(Constants.FXML_SETTINGS) || stageName.equals(Constants.FXML_SETTINGS_LINUX)) {
                    if (!SystemTray.isSupported() || com.sun.jna.Platform.isLinux()) {
                        stage.setOnCloseRequest(evt -> System.exit(0));
                    }
                }
                setStageIcon(stage);
                stage.show();
            } catch (IOException e) {
                logger.error(e.getMessage());
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
    @SneakyThrows
    public void stopCapturingThreads() {

        if (FireflyLuciferin.RUNNING) {
            if (trayIcon != null) {
                trayIcon.setImage(imageStop);
                popup.remove(0);
                popup.insert(startItem, 0);
            }
            if (mqttManager != null) {
                mqttManager.publishToTopic(Constants.STATE_ON_SOLID);
                TimeUnit.SECONDS.sleep(4);
            }
            FireflyLuciferin.RUNNING = false;
            if ((FireflyLuciferin.config.getCaptureMethod().equals(Configuration.WindowsCaptureMethod.DDUPL.name()))
                    || (FireflyLuciferin.config.getCaptureMethod().equals(Configuration.LinuxCaptureMethod.XIMAGESRC.name()))) {
                FireflyLuciferin.pipe.stop();
            }
            FireflyLuciferin.FPS_PRODUCER_COUNTER = 0;
            FireflyLuciferin.FPS_CONSUMER_COUNTER = 0;
        }

    }

    /**
     * Start capturing threads
     */
    @SneakyThrows
    public void startCapturingThreads() {

        if (!FireflyLuciferin.RUNNING) {
            if (trayIcon != null) {
                trayIcon.setImage(imagePlay);
                popup.remove(0);
                popup.insert(stopItem, 0);
            }
            FireflyLuciferin.RUNNING = true;
            if (mqttManager != null) {
                TimeUnit.SECONDS.sleep(4);
                if ((FireflyLuciferin.config.isMqttEnable() && FireflyLuciferin.config.isMqttStream())) {
                    mqttManager.publishToTopic(Constants.STATE_ON_GLOWWORM);
                } else {
                    mqttManager.publishToTopic(Constants.STATE_ON_GLOWWORMWIFI);
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
                ex.printStackTrace();
            }
        }

    }

}