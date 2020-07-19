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
package org.dpsoftware;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
public class GUIManager extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(GUIManager.class);

    final String DIALOG_LABEL = "Java Fast Screen Capture";
    private final Stage stage;
    // Tray icon
    TrayIcon trayIcon = null;
    // create a popup menu
    PopupMenu popup = new PopupMenu();
    // Label and framerate dialog
    @Getter JEditorPane jep = new JEditorPane();
    @Getter JFrame jFrame = new JFrame("Java Fast Screen Capture");
    // Menu items for start and stop
    MenuItem stopItem;
    MenuItem startItem;
    // Tray icons
    Image imagePlay;
    Image imageStop;
    @Getter String infoStr = "<div style='width:350px;height:100px;text-align:center'><font face=”Verdana”>" +
            "<br/><b>Java Fast Screen Capture</b><br/>" +
            "for PC Ambilight<br/>" +
            "by Davide Perini " +
            "<a href='https://github.com/sblantipodi/JavaFastScreenCapture'>GitHub</a>" + " (v." + FastScreenCapture.VERSION + ")" +
            "<br/><br/>" +
            "Producing @ FPS_PRODUCER FPS | Consuming @ FPS_CONSUMER FPS" +
            "</div></font>";
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
     * Create and initialize tray icon menu
     * @param config file
     */
    void initTray(Configuration config) {

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
    void showAlert(String title, String header, String context) {
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
                FastScreenCapture.scene = new Scene(FastScreenCapture.loadFXML("info"));
                stage.setScene(FastScreenCapture.scene);
                setStageIcon(stage);
                stage.show();
            } catch (IOException e) {
                logger.error(e.toString());
            }


        });

//        jep.setContentType("text/html");
//        jep.setText(infoStr);
//        jep.setEditable(false);
//        jep.setOpaque(false);
//        jep.addHyperlinkListener(hyperlinkEvent -> {
//            if (HyperlinkEvent.EventType.ACTIVATED.equals(hyperlinkEvent.getEventType())) {
//                Desktop desktop = Desktop.getDesktop();
//                try {
//                    desktop.browse(hyperlinkEvent.getURL().toURI());
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//        });
//        jFrame = new JFrame("Java Fast Screen Capture");
//        jFrame.setIconImage(imageStop);
//        jFrame.add(jep);
//        jFrame.pack();
//        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
//        jFrame.setLocation(dim.width/2-this.getSize().width/2-350/2, dim.height/2-this.getSize().height/2-100);
//        jFrame.setVisible(true);

    }

    /**
     * Set icon for every stage
     * @param stage in use
     */
    void setStageIcon(Stage stage) {
        stage.getIcons().add(new javafx.scene.image.Image(String.valueOf(this.getClass().getClassLoader().getResource("tray_stop.png"))));
    }

    /**
     * Stop capturing threads
     * @param config file
     */
    @SneakyThrows
    void stopCapturingThreads(Configuration config) {

        if (FastScreenCapture.RUNNING) {
            mqttManager.publishToTopic("{\"state\": \"ON\", \"effect\": \"solid\"}");
            TimeUnit.SECONDS.sleep(4);
            popup.remove(0);
            popup.insert(startItem, 0);
            FastScreenCapture.RUNNING = false;
            if (config.getCaptureMethod() == Configuration.CaptureMethod.DDUPL) {
                FastScreenCapture.pipe.stop();
            }
            FastScreenCapture.FPS_PRODUCER = 0;
            FastScreenCapture.FPS_CONSUMER = 0;
            trayIcon.setImage(imageStop);
        }

    }

    /**
     * Start capturing threads
     */
    @SneakyThrows
    void startCapturingThreads() {

        if (!FastScreenCapture.RUNNING) {
            popup.remove(0);
            popup.insert(stopItem, 0);
            FastScreenCapture.RUNNING = true;
            trayIcon.setImage(imagePlay);
            TimeUnit.SECONDS.sleep(4);
            mqttManager.publishToTopic("{\"state\": \"ON\", \"effect\": \"AmbiLight\"}");
        }

    }

}
