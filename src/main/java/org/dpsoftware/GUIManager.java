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
import java.awt.font.TextAttribute;
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
     */
    void initTray() {

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
                        popup.removeAll();
                        popup.add(startItem);
                        popup.add(framerateItem);
                        FastScreenCapture.RUNNING = false;
                        trayIcon.setImage(imageStop);
                    } else if (e.getActionCommand().equals("Start")) {
                        popup.removeAll();
                        popup.add(stopItem);
                        popup.add(framerateItem);
                        FastScreenCapture.RUNNING = true;
                        trayIcon.setImage(imagePlay);
                    } else if (e.getActionCommand().equals("FPS")) {
                        showFramerateDialog();
                    } else {
                        System.exit(0);
                    }
                    popup.add(exitItem);
                }
            };

            stopItem.addActionListener(listener);
            startItem.addActionListener(listener);
            exitItem.addActionListener(listener);
            framerateItem.addActionListener(listener);
            popup.add(stopItem);
            popup.add(framerateItem);
            popup.add(exitItem);
            // construct a TrayIcon
            trayIcon = new TrayIcon(imagePlay, DIALOG_LABEL, popup);
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

}
