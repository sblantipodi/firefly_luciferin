/*
  TrayIconManager.java

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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Create a tray icon in the tray bar
 */
public class TrayIconManager {

    TrayIcon trayIcon = null;
    // create a popup menu
    PopupMenu popup = new PopupMenu();

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
            MenuItem exitItem = new MenuItem("Exit");

            // create a action listener to listen for default action executed on the tray icon
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals("Stop")) {
                        popup.removeAll();
                        popup.add(startItem);
                        FastScreenCapture.RUNNING = false;
                        trayIcon.setImage(imageStop);
                    } else if (e.getActionCommand().equals("Start")) {
                        popup.removeAll();
                        popup.add(stopItem);
                        FastScreenCapture.RUNNING = true;
                        trayIcon.setImage(imagePlay);
                    } else {
                        System.exit(0);
                    }
                    popup.add(exitItem);
                }
            };

            stopItem.addActionListener(listener);
            startItem.addActionListener(listener);
            exitItem.addActionListener(listener);
            popup.add(stopItem);
            popup.add(exitItem);
            // construct a TrayIcon
            trayIcon = new TrayIcon(imagePlay, "Fast Screen Capture", popup);
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

}
