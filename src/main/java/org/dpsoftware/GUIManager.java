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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;


/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
public class GUIManager {

    final String DIALOG_LABEL = "Java Fast Screen Capture";
    // Tray icon
    TrayIcon trayIcon = null;
    // create a popup menu
    PopupMenu popup = new PopupMenu();
    // Label and framerate dialog
    @Getter JLabel framerateLabel = new JLabel("", SwingConstants.CENTER);
    // JFrame for info menu
    JFrame framerateDialog = new JFrame(DIALOG_LABEL);
    // Menu items for start and stop
    MenuItem stopItem;
    MenuItem startItem;
    // Tray icons
    Image imagePlay;
    Image imageStop;

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
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand() == null) {
                        if (FastScreenCapture.RUNNING) {
                            stopCapturingThreads(config);
                        } else {
                            startCapturingThreads();
                        }
                    } else {
                        if (e.getActionCommand().equals("Stop")) {
                            stopCapturingThreads(config);
                        } else if (e.getActionCommand().equals("Start")) {
                            startCapturingThreads();
                        } else if (e.getActionCommand().equals("Info")) {
                            showFramerateDialog();
                        } else {
                            System.exit(0);
                        }
                    }
                }
            };
            stopItem.addActionListener(listener);
            startItem.addActionListener(listener);
            exitItem.addActionListener(listener);
            infoItem.addActionListener(listener);
            popup.add(startItem);
            popup.addSeparator();

            config.getLedMatrix().forEach((ledMatrixKey, ledMatrix) -> {

                CheckboxMenuItem checkboxMenuItem = new CheckboxMenuItem(ledMatrixKey,
                        ledMatrixKey.equals(config.getDefaultLedMatrix()));
                checkboxMenuItem.addItemListener(new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
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
                    }
                });
                popup.add(checkboxMenuItem);

            });

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
     * Show a dialog with a framerate counter
     */
    void showFramerateDialog() {

        framerateDialog.setLocationRelativeTo(null);
        framerateDialog.setVisible(true);
        framerateDialog.setBounds(0, 0, 330, 80);
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_ULTRABOLD);
        attributes.put(TextAttribute.SIZE, 12);
        framerateLabel.setFont(Font.getFont(attributes));
        framerateDialog.getContentPane().add(framerateLabel, BorderLayout.CENTER);

    }

    /**
     * Stop capturing threads
     * @param config
     */
    void stopCapturingThreads(Configuration config) {

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

    /**
     * Start capturing threads
     */
    void startCapturingThreads() {

        popup.remove(0);
        popup.insert(stopItem, 0);
        FastScreenCapture.RUNNING = true;
        trayIcon.setImage(imagePlay);

    }

}
