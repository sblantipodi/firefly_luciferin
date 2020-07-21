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
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.dpsoftware.Configuration;
import org.dpsoftware.FastScreenCapture;
import org.dpsoftware.MQTTManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
@NoArgsConstructor
public class GUIManager extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(GUIManager.class);

    final String DIALOG_LABEL = "Java Fast Screen Capture";
    private Stage stage;
    // Tray icon
    TrayIcon trayIcon = null;
    // create a popup menu
    PopupMenu popup = new PopupMenu();
    // Label and framerate dialog
    @Getter JEditorPane jep = new JEditorPane();
    @Getter JFrame jFrame = new JFrame(DIALOG_LABEL);
    // Menu items for start and stop
    MenuItem stopItem;
    MenuItem startItem;
    // Tray icons
    Image imagePlay;
    Image imageStop;
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
        FXMLLoader fxmlLoader = new FXMLLoader(GUIManager.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * Create and initialize tray icon menu
     * @param config file
     */
    public void initTray(Configuration config) {

        if (SystemTray.isSupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            imagePlay = Toolkit.getDefaultToolkit().getImage(this.getClass().getClassLoader().getResource("tray_play.png"));
            imageStop = Toolkit.getDefaultToolkit().getImage(this.getClass().getClassLoader().getResource("tray_stop.png"));

            // create menu item for the default action
            stopItem = new MenuItem("Stop");
            startItem = new MenuItem("Start");
            MenuItem infoItem = new MenuItem("Info");
            MenuItem exitItem = new MenuItem("Exit");

            // create a action listener to listen for default action executed on the tray icon
            ActionListener listener = initPopupMenuListener(config);

            stopItem.addActionListener(listener);
            startItem.addActionListener(listener);
            exitItem.addActionListener(listener);
            infoItem.addActionListener(listener);
            popup.add(startItem);
            popup.addSeparator();

            initGrabMode(config);

            popup.addSeparator();
            popup.add(infoItem);
            popup.addSeparator();
            popup.add(exitItem);
            // construct a TrayIcon
            trayIcon = new TrayIcon(imageStop, DIALOG_LABEL, popup);
            // set the TrayIcon properties
            trayIcon.addActionListener(listener);
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                logger.error(String.valueOf(e));
            }
        }

    }

    /**
     * Init popup menu
     * @param config file
     * @return tray icon listener
     */
    ActionListener initPopupMenuListener(Configuration config) {

        return actionEvent -> {
            if (actionEvent.getActionCommand() == null) {
                if (FastScreenCapture.RUNNING) {
                    stopCapturingThreads(config);
                } else {
                    startCapturingThreads();
                }
            } else {
                switch (actionEvent.getActionCommand()) {
                    case "Stop" -> stopCapturingThreads(config);
                    case "Start" -> startCapturingThreads();
                    case "Info" -> showFramerateDialog();
                    default -> {
                        stopCapturingThreads(config);
                        System.exit(0);
                    }
                }
            }
        };

    }

    /**
     * Add params in the tray icon menu for every ledMatrix found in the FastScreenCapture.yaml
     * @param config file
     */
    void initGrabMode(Configuration config) {

        config.getLedMatrix().forEach((ledMatrixKey, ledMatrix) -> {

            CheckboxMenuItem checkboxMenuItem = new CheckboxMenuItem(ledMatrixKey,
                    ledMatrixKey.equals(config.getDefaultLedMatrix()));
            checkboxMenuItem.addItemListener(itemListener -> {
                logger.info("Stopping Threads...");
                stopCapturingThreads(config);
                for (int i=0; i < popup.getItemCount(); i++) {
                    if (popup.getItem(i) instanceof CheckboxMenuItem) {
                        if (!popup.getItem(i).getLabel().equals(checkboxMenuItem.getLabel())) {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(false);
                        } else {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(true);
                            config.setDefaultLedMatrix(checkboxMenuItem.getLabel());
                            logger.info("Capture mode changed to " + checkboxMenuItem.getLabel());
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
     */
    public void showAlert(String title, String header, String context) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        setStageIcon(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(context);
        alert.showAndWait();

    }

    /**
     * Show a dialog with a framerate counter
     */
    void showFramerateDialog() {

        Platform.runLater(() -> {
            try {
                FastScreenCapture.scene = new Scene(loadFXML("info"));
                stage.setScene(FastScreenCapture.scene);
                setStageIcon(stage);
                stage.show();
            } catch (IOException e) {
                logger.error(e.toString());
            }
        });

    }

    /**
     * Set icon for every stage
     * @param stage in use
     */
    public static void setStageIcon(Stage stage) {
        stage.getIcons().add(new javafx.scene.image.Image(String.valueOf(GUIManager.class.getClassLoader().getResource("tray_stop.png"))));
    }

    /**
     * Stop capturing threads
     * @param config file
     */
    @SneakyThrows
    public void stopCapturingThreads(Configuration config) {

        if (FastScreenCapture.RUNNING) {
            if (mqttManager != null) {
                mqttManager.publishToTopic("{\"state\": \"ON\", \"effect\": \"solid\"}");
                TimeUnit.SECONDS.sleep(4);
            }
            popup.remove(0);
            popup.insert(startItem, 0);
            FastScreenCapture.RUNNING = false;
            if (config.getCaptureMethod() == Configuration.CaptureMethod.DDUPL) {
                FastScreenCapture.pipe.stop();
            }
            FastScreenCapture.FPS_PRODUCER_COUNTER = 0;
            FastScreenCapture.FPS_CONSUMER_COUNTER = 0;
            trayIcon.setImage(imageStop);
        }

    }

    /**
     * Start capturing threads
     */
    @SneakyThrows
    public void startCapturingThreads() {

        if (!FastScreenCapture.RUNNING) {
            popup.remove(0);
            popup.insert(stopItem, 0);
            FastScreenCapture.RUNNING = true;
            trayIcon.setImage(imagePlay);
            if (mqttManager != null) {
                TimeUnit.SECONDS.sleep(4);
                mqttManager.publishToTopic("{\"state\": \"ON\", \"effect\": \"AmbiLight\"}");
            }
        }

    }

}