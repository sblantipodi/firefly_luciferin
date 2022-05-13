/*
  TrayIconManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini  (https://github.com/sblantipodi)

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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

import static org.dpsoftware.utilities.CommonUtility.scaleDownResolution;

/**
 * Serial port utility
 */
@Slf4j
public class TrayIconManager {

    // Tray icon
    @Getter @Setter
    TrayIcon trayIcon = null;
    public static JPopupMenu popupMenu;
    JMenu aspectRatioSubMenu;
    public JMenu presetsSubMenu;
    // hidden dialog displayed behing the system tray to auto hide the popup menu when clicking somewhere else on the screen
    final JDialog hiddenDialog = new JDialog();
    ActionListener menuListener;
    // Tray icons
    Image imagePlay, imagePlayCenter, imagePlayLeft, imagePlayRight, imagePlayWaiting, imagePlayWaitingCenter, imagePlayWaitingLeft, imagePlayWaitingRight;
    Image imageStop, imageStopCenter, imageStopLeft, imageStopRight;
    Image imageGreyStop, imageGreyStopCenter, imageGreyStopLeft, imageGreyStopRight;

    /**
     * Constructor
     */
    public TrayIconManager() {
        setMenuItemStyle(null, null, null);
        popupMenu = new JPopupMenu();
        popupMenu.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(160, 160, 160)));
        aspectRatioSubMenu = createSubMenuItem(CommonUtility.getWord(Constants.ASPECT_RATIO) + " ");
        presetsSubMenu = createSubMenuItem(CommonUtility.getWord(Constants.PRESETS) + " ");
        initMenuListener();
    }

    /**
     * Init menu listener
     */
    private void initMenuListener() {
        //Action listener to get click on top menu items
        menuListener = e -> {
            JMenuItem jMenuItem = (JMenuItem) e.getSource();
            String menuItemText = getMenuString(jMenuItem);
            if (CommonUtility.getWord(Constants.STOP).equals(menuItemText)) {
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
            } else if (CommonUtility.getWord(Constants.START).equals(menuItemText)) {
                FireflyLuciferin.guiManager.startCapturingThreads();
            } else if (CommonUtility.getWord(Constants.SETTINGS).equals(menuItemText)) {
                FireflyLuciferin.guiManager.showSettingsDialog();
            } else if (CommonUtility.getWord(Constants.INFO).equals(menuItemText)) {
                FireflyLuciferin.guiManager.showFramerateDialog();
            } else {
                StorageManager sm = new StorageManager();
                if (sm.listPresetsForThisInstance().stream().anyMatch(preset -> preset.equals(menuItemText))
                        || menuItemText.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                    managePresetListener(menuItemText);
                }
                manageAspectRatioListener(menuItemText, jMenuItem);
                if (CommonUtility.getWord(Constants.TRAY_EXIT).equals(menuItemText)) {
                    if (FireflyLuciferin.RUNNING) {
                        FireflyLuciferin.guiManager.stopCapturingThreads(true);
                    }
                    log.debug(Constants.CLEAN_EXIT);
                    FireflyLuciferin.exit();
                }
            }
        };
    }

    /**
     * Manage aspect ratio listener actions
     * @param menuItemText item text
     * @param jMenuItem menu object
     */
    private void manageAspectRatioListener(String menuItemText, JMenuItem jMenuItem) {
        if (Constants.AspectRatio.FULLSCREEN.getBaseI18n().equals(menuItemText)
                || Constants.AspectRatio.LETTERBOX.getBaseI18n().equals(menuItemText)
                || Constants.AspectRatio.PILLARBOX.getBaseI18n().equals(menuItemText)) {
            setAspetRatio(jMenuItem);
            aspectRatioSubMenu.removeAll();
            populateAspectRatio();
        } else if (CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS).equals(menuItemText)) {
            log.info(CommonUtility.getWord(Constants.CAPTURE_MODE_CHANGED) + CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
            FireflyLuciferin.config.setAutoDetectBlackBars(true);
            if (FireflyLuciferin.config.isMqttEnable()) {
                MQTTManager.publishToTopic(Constants.ASPECT_RATIO_TOPIC, CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
            }
            aspectRatioSubMenu.removeAll();
            populateAspectRatio();
        }
    }

    /**
     * Manage Presets Listener
     * @param menuItemText item text
     */
    private void managePresetListener(String menuItemText) {
        FireflyLuciferin.config.setDefaultPreset(menuItemText);
        StorageManager sm = new StorageManager();
        Configuration defaultConfig = sm.readConfig(false);
        sm.setPresetDifferences(defaultConfig, FireflyLuciferin.config);
        if (menuItemText.equals(CommonUtility.getWord(Constants.DEFAULT))) {
            FireflyLuciferin.config = defaultConfig;
        } else {
            FireflyLuciferin.config = sm.readPreset(menuItemText);
            sm.setPresetDifferences(defaultConfig, FireflyLuciferin.config);
        }
        if (sm.restartNeeded) {
            log.debug(menuItemText);
            if (menuItemText.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                NativeExecutor.restartNativeInstance(null);
            } else {
                NativeExecutor.restartNativeInstance(menuItemText);
            }
        }
        FireflyLuciferin.config.setDefaultPreset(menuItemText);
        CommonUtility.turnOnLEDs();
        Constants.Effect effectInUse = LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect());
        boolean requirePipeline = Constants.Effect.BIAS_LIGHT.equals(effectInUse)
                || Constants.Effect.MUSIC_MODE_VU_METER.equals(effectInUse)
                || Constants.Effect.MUSIC_MODE_VU_METER_DUAL.equals(effectInUse)
                || Constants.Effect.MUSIC_MODE_BRIGHT.equals(effectInUse)
                || Constants.Effect.MUSIC_MODE_RAINBOW.equals(effectInUse);
        if (!FireflyLuciferin.RUNNING && requirePipeline) {
            FireflyLuciferin.guiManager.startCapturingThreads();
        } else if (FireflyLuciferin.RUNNING) {
            FireflyLuciferin.guiManager.stopCapturingThreads(false);
            if (requirePipeline) {
                FireflyLuciferin.guiManager.startCapturingThreads();
            }
        }
        presetsSubMenu.removeAll();
        populatePresets();
    }

    /**
     * Set aspect ratio
     * @param jMenuItem menu item
     */
    private void setAspetRatio(JMenuItem jMenuItem) {
        String menuItemText = getMenuString(jMenuItem);
        FireflyLuciferin.config.setDefaultLedMatrix(menuItemText);
        log.info(CommonUtility.getWord(Constants.CAPTURE_MODE_CHANGED) + menuItemText);
        GStreamerGrabber.ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(menuItemText);
        FireflyLuciferin.config.setAutoDetectBlackBars(false);
        if (FireflyLuciferin.config.isMqttEnable()) {
            MQTTManager.publishToTopic(Constants.ASPECT_RATIO_TOPIC, menuItemText);
        }
    }

    /**
     * Create and initialize tray icon menu
     */
    public void initTray() {
        if (NativeExecutor.isSystemTraySupported() && !NativeExecutor.isLinux()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // init tray images
            initializeImages();
            // create menu item for the default action
            popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.START)));
            addSeparator();
            populateAspectRatio();
            aspectRatioSubMenu.getPopupMenu().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(160, 160, 160)));
            populatePresets();
            presetsSubMenu.getPopupMenu().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(160, 160, 160)));
            popupMenu.add(aspectRatioSubMenu);
            popupMenu.add(presetsSubMenu);
            popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.SETTINGS)));
            popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.INFO)));
            addSeparator();
            popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.TRAY_EXIT)));
            // listener based on the focus to auto hide the hidden dialog and the popup menu when the hidden dialog box lost focus
            hiddenDialog.setSize(10,10);
            hiddenDialog.addWindowFocusListener(new WindowFocusListener() {
                public void windowLostFocus (final WindowEvent e) {
                    hiddenDialog.setVisible(false);
                }
                public void windowGainedFocus (final WindowEvent e) {
                    //Nothing to do
                }
            });
            // construct a TrayIcon
            if (FireflyLuciferin.communicationError) {
                trayIcon = new TrayIcon(setTrayIconImage(Constants.PlayerStatus.GREY), Constants.FIREFLY_LUCIFERIN);
            } else {
                trayIcon = new TrayIcon(setTrayIconImage(Constants.PlayerStatus.STOP), Constants.FIREFLY_LUCIFERIN);
            }
            initTrayListener();
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error(String.valueOf(e));
            }
        }
    }

    /**
     * Populate aspect ratio sub menu
     */
    private void populateAspectRatio() {
        aspectRatioSubMenu.add(createMenuItem(Constants.AspectRatio.FULLSCREEN.getI18n()), 0);
        aspectRatioSubMenu.add(createMenuItem(Constants.AspectRatio.LETTERBOX.getI18n()), 1);
        aspectRatioSubMenu.add(createMenuItem(Constants.AspectRatio.PILLARBOX.getI18n()), 2);
        aspectRatioSubMenu.add(createMenuItem(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)), 3);
    }

    /**
     * Populate presets submenu
     */
    public void populatePresets() {
        StorageManager sm = new StorageManager();
        int index = 0;
        for (String preset : sm.listPresetsForThisInstance()) {
            presetsSubMenu.add(createMenuItem(preset), index++);
        }
        presetsSubMenu.add(createMenuItem(CommonUtility.getWord(Constants.DEFAULT)));
    }

    /**
     * Initialize listeners for tray icon
     */
    private void initTrayListener() {
        // add a listener to display the popupmenu and the hidden dialog box when the tray icon is clicked
        MouseListener ml = new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (FireflyLuciferin.RUNNING) {
                        FireflyLuciferin.guiManager.stopCapturingThreads(true);
                    } else {
                        FireflyLuciferin.guiManager.startCapturingThreads();
                    }
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == 3) {
                    DisplayManager displayManager = new DisplayManager();
                    int mainScreenOsScaling = (int) (displayManager.getPrimaryDisplay().getScaleX()*100);
                    // the dialog is also displayed at this position but it is behind the system tray
                    popupMenu.setLocation(scaleDownResolution(e.getX(), mainScreenOsScaling),
                            scaleDownResolution(e.getY(), mainScreenOsScaling));
                    hiddenDialog.setLocation(scaleDownResolution(e.getX(), mainScreenOsScaling),
                            scaleDownResolution(Constants.FAKE_GUI_TRAY_ICON, mainScreenOsScaling));
                    // important: set the hidden dialog as the invoker to hide the menu with this dialog lost focus
                    popupMenu.setInvoker(hiddenDialog);
                    hiddenDialog.setVisible(true);
                    popupMenu.setVisible(true);
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        };
        trayIcon.addMouseListener(ml);
    }

    /**
     * Initialize images for the tray icon
     */
    private void initializeImages() {
        // load an image
        imagePlay = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY));
        imagePlayCenter = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY_CENTER));
        imagePlayLeft = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY_LEFT));
        imagePlayRight = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY_RIGHT));
        imagePlayWaiting = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY_WAITING));
        imagePlayWaitingCenter = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY_WAITING_CENTER));
        imagePlayWaitingLeft = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY_WAITING_LEFT));
        imagePlayWaitingRight = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY_WAITING_RIGHT));
        imageStop = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_STOP));
        imageStopCenter = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_STOP_CENTER));
        imageStopLeft = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_STOP_LEFT));
        imageStopRight = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_STOP_RIGHT));
        imageGreyStop = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_GREY));
        imageGreyStopCenter = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_GREY_CENTER));
        imageGreyStopLeft = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_GREY_LEFT));
        imageGreyStopRight = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_GREY_RIGHT));
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            imagePlayRight = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY_RIGHT_GOLD));
            imagePlayWaitingRight = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_PLAY_WAITING_RIGHT_GOLD));
            imageStopRight = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_STOP_RIGHT_GOLD));
            imageGreyStopRight = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Constants.IMAGE_TRAY_GREY_RIGHT_GOLD));
        }
    }

    /**
     * Add a menu item to the tray icon popupMenu
     * @param menuLabel label to use on the menu item
     */
    public JMenuItem createMenuItem(String menuLabel) {
        final JMenuItem jMenuItem = new JMenuItem(menuLabel);
        jMenuItem.setOpaque(true);
        Constants.AspectRatio aspectRatio = LocalizedEnum.fromStr(Constants.AspectRatio.class, menuLabel);
        String menuItemText = aspectRatio != null ? aspectRatio.getBaseI18n() : jMenuItem.getText();
        Font f = new Font("verdana", Font.BOLD, 10);
        jMenuItem.setFont(f);
        setMenuItemStyle(menuLabel, jMenuItem, menuItemText);
        jMenuItem.setBorder(BorderFactory.createMatteBorder(3, 10, 3, 7, Color.GRAY));
        jMenuItem.setBorderPainted(false);
        jMenuItem.addActionListener(menuListener);
        jMenuItem.setBackground(getBackgroundColor());
        return jMenuItem;
    }

    /**
     * Add submenu to the tray popupmenu
     * @param menuLabel label to use
     * @return formatted JMenu
     */
    public JMenu createSubMenuItem(String menuLabel) {
        final JMenu menu = new JMenu(menuLabel);
        menu.setOpaque(true);
        Constants.AspectRatio aspectRatio = LocalizedEnum.fromStr(Constants.AspectRatio.class, menuLabel);
        String menuItemText = aspectRatio != null ? aspectRatio.getBaseI18n() : menu.getText();
        Font f = new Font("verdana", Font.BOLD, 10);
        menu.setFont(f);
        setMenuItemStyle(menuLabel, menu, menuItemText);
        menu.setBorder(BorderFactory.createMatteBorder(3, 10, 3, 7, Color.GRAY));
        menu.setBorderPainted(false);
        menu.setBackground(getBackgroundColor());
        return menu;
    }

    /**
     * Get color to use for the menu background
     * @return color based on the theme in use
     */
    private Color getBackgroundColor() {
        var theme = LocalizedEnum.fromBaseStr(Constants.Theme.class, FireflyLuciferin.config.getTheme());
        Color color = Color.WHITE;
        switch (theme) {
            case DARK_THEME_CYAN -> color = new Color(80, 89, 96);
            case DARK_BLUE_THEME -> color = new Color(46, 61, 88);
            case DARK_THEME_ORANGE -> color = new Color(72, 72, 72);
            case DARK_THEME_PURPLE -> color = new Color(105, 105, 130);
            case DEFAULT -> color = new Color(244, 244, 244);
        }
        return color;
    }

    /**
     * Add a separator between menuitems
     */
    private void addSeparator() {
        JSeparator s = new JSeparator();
        s.setOrientation(JSeparator.HORIZONTAL);
        s.setBackground(new Color(215, 215, 215));
        s.setForeground(new Color(215, 215, 215));
        s.setBorder(new EmptyBorder(0, 0, 0, 0));
        popupMenu.add(s);
    }

    /**
     * Set style on menu items
     * @param menuLabel item label
     * @param jMenuItem item object
     * @param menuItemText used to color text when aspect ratio is set to Auto
     */
    private void setMenuItemStyle(String menuLabel, JMenuItem jMenuItem, String menuItemText) {
        var theme = LocalizedEnum.fromBaseStr(Constants.Theme.class, FireflyLuciferin.config.getTheme());
        switch (theme) {
            case DARK_THEME_CYAN -> {
                UIManager.put("MenuItem.selectionBackground", new Color(0, 153, 255));
                UIManager.put("MenuItem.selectionForeground", new Color(211, 211, 211));
                UIManager.put("MenuItem.foreground", new Color(211, 211, 211));
                UIManager.put("Menu.foreground", new Color(211, 211, 211));
                UIManager.put("Menu.selectionBackground", new Color(0, 153, 255));
                UIManager.put("Menu.selectionForeground", new Color(211, 211, 211));
            }
            case DARK_BLUE_THEME -> {
                UIManager.put("MenuItem.selectionBackground", new Color(29, 168, 255));
                UIManager.put("MenuItem.selectionForeground", Color.WHITE);
                UIManager.put("MenuItem.foreground", Color.WHITE);
                UIManager.put("Menu.foreground", Color.WHITE);
                UIManager.put("Menu.selectionBackground", new Color(29, 168, 255));
                UIManager.put("Menu.selectionForeground", Color.WHITE);
            }
            case DARK_THEME_ORANGE -> {
                UIManager.put("MenuItem.selectionBackground", Color.ORANGE);
                UIManager.put("MenuItem.selectionForeground", new Color(101, 101, 101));
                UIManager.put("MenuItem.foreground", new Color(211, 211, 211));
                UIManager.put("Menu.foreground", new Color(211, 211, 211));
                UIManager.put("Menu.selectionBackground", Color.ORANGE);
                UIManager.put("Menu.selectionForeground", new Color(101, 101, 101));
            }
            case DARK_THEME_PURPLE -> {
                UIManager.put("MenuItem.selectionBackground", new Color(206, 157, 255));
                UIManager.put("MenuItem.selectionForeground", Color.WHITE);
                UIManager.put("MenuItem.foreground", Color.WHITE);
                UIManager.put("Menu.foreground", Color.WHITE);
                UIManager.put("Menu.selectionBackground", new Color(206, 157, 255));
                UIManager.put("Menu.selectionForeground", Color.WHITE);
            }
            case DEFAULT -> {
                UIManager.put("MenuItem.selectionBackground", new Color(0, 153, 255));
                UIManager.put("MenuItem.selectionForeground", new Color(211, 211, 211));
                UIManager.put("MenuItem.foreground", new Color(50, 50, 50));
                UIManager.put("Menu.foreground", new Color(50, 50, 50));
                UIManager.put("Menu.selectionBackground", new Color(0, 153, 255));
                UIManager.put("Menu.selectionForeground", new Color(211, 211, 211));
            }
        }
        if (menuLabel != null && menuItemText != null && jMenuItem != null) {
            if ((menuItemText.equals(FireflyLuciferin.config.getDefaultLedMatrix()) && !FireflyLuciferin.config.isAutoDetectBlackBars())
                    || (menuLabel.equals(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)) && FireflyLuciferin.config.isAutoDetectBlackBars())) {
                jMenuItem.setForeground(new Color(0, 153, 255));
            }
            if (menuLabel.equals(FireflyLuciferin.config.getDefaultPreset())
                    || (menuLabel.equals(CommonUtility.getWord(Constants.DEFAULT))
                    && FireflyLuciferin.config.getDefaultPreset().equals(Constants.DEFAULT))) {
                jMenuItem.setForeground(new Color(0, 153, 255));
            }
        }
    }

    /**
     * Return the localized tray icon menu string
     * @param jMenuItem containing the base locale string
     * @return localized string if any
     */
    private String getMenuString(JMenuItem jMenuItem) {
        Constants.AspectRatio aspectRatio = LocalizedEnum.fromStr(Constants.AspectRatio.class, jMenuItem.getText());
        return aspectRatio != null ? aspectRatio.getBaseI18n() : jMenuItem.getText();
    }

    /**
     * Reset try icon after a serial reconnection
     */
    public void resetTray() {
        if (NativeExecutor.isSystemTraySupported() && !NativeExecutor.isLinux()) {
            setTrayIconImage(Constants.PlayerStatus.STOP);
        }
    }

    /**
     * Set and return tray icon image
     * @param playerStatus status
     * @return tray icon
     */
    @SuppressWarnings("Duplicates")
    public Image setTrayIconImage(Constants.PlayerStatus playerStatus) {
        Image img = switch (playerStatus) {
            case PLAY -> setImage(imagePlay, imagePlayRight, imagePlayLeft, imagePlayCenter);
            case PLAY_WAITING -> setImage(imagePlayWaiting, imagePlayWaitingRight, imagePlayWaitingLeft, imagePlayWaitingCenter);
            case STOP -> setImage(imageStop, imageStopRight, imageStopLeft, imageStopCenter);
            case GREY -> setImage(imageGreyStop, imageGreyStopRight, imageGreyStopLeft, imageGreyStopCenter);
        };
        if (trayIcon != null) {
            trayIcon.setImage(img);
        }
        return img;
    }

    /**
     * Set image
     * @param imagePlay         image
     * @param imagePlayRight    image
     * @param imagePlayLeft     image
     * @param imagePlayCenter   image
     * @return tray image
     */
    @SuppressWarnings("Duplicates")
    private Image setImage(Image imagePlay, Image imagePlayRight, Image imagePlayLeft, Image imagePlayCenter) {
        Image img = null;
        switch (JavaFXStarter.whoAmI) {
            case 1 -> {
                if ((FireflyLuciferin.config.getMultiMonitor() == 1)) {
                    img = imagePlay;
                } else {
                    img = imagePlayRight;
                }
            }
            case 2 -> {
                if ((FireflyLuciferin.config.getMultiMonitor() == 2)) {
                    img = imagePlayLeft;
                } else {
                    img = imagePlayCenter;
                }
            }
            case 3 -> img = imagePlayLeft;
        }
        return img;
    }

}
