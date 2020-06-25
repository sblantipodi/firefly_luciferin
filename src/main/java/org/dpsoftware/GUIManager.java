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
import java.io.ObjectInputFilter;
import java.util.HashMap;
import java.util.Map;


/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
public class GUIManager {

    final String DIALOG_LABEL = "Fast Screen Capture";
    // Tray icon
    TrayIcon trayIcon = null;
    // create a popup menu
    PopupMenu popup = new PopupMenu();
    // Label and framerate dialog
    @Getter JLabel framerateLabel = new JLabel("", SwingConstants.CENTER);
    JFrame framerateDialog = new JFrame(DIALOG_LABEL);

    /**
     * Create and initialize tray icon menu
     * @param config
     */
    void initTray(Configuration config) {

        if (SystemTray.isSupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            Image imagePlay = Toolkit.getDefaultToolkit().getImage(this.getClass().getClassLoader().getResource("tray_play.png"));
            Image imageStop = Toolkit.getDefaultToolkit().getImage(this.getClass().getClassLoader().getResource("tray_stop.png"));

            // create menu item for the default action
            MenuItem stopItem = new MenuItem("Stop");
            MenuItem startItem = new MenuItem("Start");
            MenuItem framerateItem = new MenuItem("FPS");
            MenuItem exitItem = new MenuItem("Exit");

            // create a action listener to listen for default action executed on the tray icon
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals("Stop")) {
                        popup.remove(0);
                        popup.insert(startItem, 0);
                        stopCapturingThreads(config);
                        trayIcon.setImage(imageStop);
                    } else if (e.getActionCommand().equals("Start")) {
                        popup.remove(0);
                        popup.insert(stopItem, 0);
                        startCapturingThreads(config);
                        trayIcon.setImage(imagePlay);
                    } else if (e.getActionCommand().equals("FPS")) {
                        showFramerateDialog();
                    } else {
                        System.exit(0);
                    }
                }
            };

            stopItem.addActionListener(listener);
            startItem.addActionListener(listener);
            exitItem.addActionListener(listener);
            framerateItem.addActionListener(listener);
            popup.add(startItem);
            popup.add(framerateItem);
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
                                    startCapturingThreads(config);
                                }
                            }
                        }
                    }
                });
                popup.add(checkboxMenuItem);

            });

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

        FastScreenCapture.RUNNING = false;
        if (config.getCaptureMethod() == Configuration.CaptureMethod.DDUPL) {
            FastScreenCapture.pipe.stop();
        }
        FastScreenCapture.FPS_PRODUCER = 0;
        FastScreenCapture.FPS_CONSUMER = 0;

    }

    /**
     * Start capturing threads
     * @param config
     */
    void startCapturingThreads(Configuration config) {

        FastScreenCapture.RUNNING = true;

    }

}
