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

import lombok.Getter;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;


/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
public class GUIManager extends JFrame {

    final String DIALOG_LABEL = "Java Fast Screen Capture";
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
     * @param mqttManager
     * @throws HeadlessException
     */
    public GUIManager(MQTTManager mqttManager) throws HeadlessException {

        this.mqttManager = mqttManager;

    }

    /**
     * Create and initialize tray icon menu
     * @param config
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
                System.err.println(e);
            }
        }

    }

    /**
     * Init popup menu
     * @param config
     * @return
     */
    ActionListener initPopupMenuListener(Configuration config) {

        ActionListener listener = actionEvent -> {
            if (actionEvent.getActionCommand() == null) {
                if (FastScreenCapture.RUNNING) {
                    stopCapturingThreads(config);
                } else {
                    startCapturingThreads();
                }
            } else {
                if (actionEvent.getActionCommand().equals("Stop")) {
                    stopCapturingThreads(config);
                } else if (actionEvent.getActionCommand().equals("Start")) {
                    startCapturingThreads();
                } else if (actionEvent.getActionCommand().equals("Info")) {
                    showFramerateDialog();
                } else {
                    stopCapturingThreads(config);
                    System.exit(0);
                }
            }
        };
        return listener;

    }

    void initGrabMode(Configuration config) {

        config.getLedMatrix().forEach((ledMatrixKey, ledMatrix) -> {

            CheckboxMenuItem checkboxMenuItem = new CheckboxMenuItem(ledMatrixKey,
                    ledMatrixKey.equals(config.getDefaultLedMatrix()));
            checkboxMenuItem.addItemListener(itemListener -> {
                System.out.println("Stopping Threads...");
                stopCapturingThreads(config);
                for (int i=0; i < popup.getItemCount(); i++) {
                    if (popup.getItem(i) instanceof CheckboxMenuItem) {
                        if (!popup.getItem(i).getLabel().equals(checkboxMenuItem.getLabel())) {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(false);
                        } else {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(true);
                            config.setDefaultLedMatrix(checkboxMenuItem.getLabel());
                            System.out.println("Capture mode changed to " + checkboxMenuItem.getLabel());
                            startCapturingThreads();
                        }
                    }
                }
            });
            popup.add(checkboxMenuItem);

        });

    }

    /**
     * Show a dialog with a framerate counter
     */
    void showFramerateDialog() {

        jep.setContentType("text/html");
        jep.setText(infoStr);
        jep.setEditable(false);
        jep.setOpaque(false);
        jep.addHyperlinkListener(hyperlinkEvent -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(hyperlinkEvent.getEventType())) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(hyperlinkEvent.getURL().toURI());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        jFrame = new JFrame("Java Fast Screen Capture");
        jFrame.setIconImage(imageStop);
        jFrame.add(jep);
        jFrame.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        jFrame.setLocation(dim.width/2-this.getSize().width/2-350/2, dim.height/2-this.getSize().height/2-100);
        jFrame.setVisible(true);

    }

    /**
     * Stop capturing threads
     * @param config
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
