/*
  TrayIconManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;

import static org.dpsoftware.utilities.CommonUtility.scaleDownResolution;

/**
 * Serial port utility
 */
@Slf4j
public class TrayIconManager {

    // hidden dialog displayed behing the system tray to auto hide the popup menu when clicking somewhere else on the screen
    final JDialog hiddenDialog = new JDialog();
    public JMenu profilesSubMenu;
    // Tray icon
    @Getter
    @Setter
    TrayIcon trayIcon = null;
    JMenu aspectRatioSubMenu;
    ActionListener menuListener;
    // Tray icons
    Image imagePlay, imagePlayCenter, imagePlayLeft, imagePlayRight, imagePlayWaiting, imagePlayWaitingCenter, imagePlayWaitingLeft, imagePlayWaitingRight;
    Image imageStop, imageStopCenter, imageStopLeft, imageStopRight, imageStopOff, imageStopCenterOff, imageStopLeftOff, imageStopRightOff;
    Image imageGreyStop, imageGreyStopCenter, imageGreyStopLeft, imageGreyStopRight;
    private Timer timer;

    /**
     * Constructor
     */
    public TrayIconManager() {
        setMenuItemStyle(null, null, null);
        GuiSingleton.getInstance().popupMenu = new JPopupMenu();
        GuiSingleton.getInstance().popupMenu.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(160, 160, 160)));
        aspectRatioSubMenu = createSubMenuItem(CommonUtility.getWord(Constants.ASPECT_RATIO) + " ");
        profilesSubMenu = createSubMenuItem(CommonUtility.getWord(Constants.PROFILES) + " ");
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
                MainSingleton.getInstance().guiManager.stopCapturingThreads(true);
            } else if (CommonUtility.getWord(Constants.START).equals(menuItemText)) {
                MainSingleton.getInstance().guiManager.startCapturingThreads();
            } else if (CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_OFF).toLowerCase()).equals(menuItemText)) {
                manageOnOff();
            } else if (CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_ON).toLowerCase()).equals(menuItemText)) {
                manageOnOff();
            } else if (CommonUtility.getWord(Constants.SETTINGS).equals(menuItemText)) {
                MainSingleton.getInstance().guiManager.showSettingsDialog(false);
            } else if (CommonUtility.getWord(Constants.INFO).equals(menuItemText)) {
                MainSingleton.getInstance().guiManager.showFramerateDialog();
            } else {
                StorageManager sm = new StorageManager();
                if (sm.listProfilesForThisInstance().stream().anyMatch(profile -> profile.equals(menuItemText))
                        || menuItemText.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                    if (menuItemText.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                        NativeExecutor.restartNativeInstance(null);
                    } else {
                        NativeExecutor.restartNativeInstance(menuItemText);
                    }
                }
                manageAspectRatioListener(menuItemText, true);
                if (CommonUtility.getWord(Constants.TRAY_EXIT).equals(menuItemText)) {
                    NativeExecutor.exit();
                }
            }
        };
    }

    /**
     * Manage aspect ratio listener actions
     *
     * @param menuItemText item text
     * @param sendSetCmd   send mqtt msg back
     */
    public void manageAspectRatioListener(String menuItemText, boolean sendSetCmd) {
        if (MainSingleton.getInstance().config != null && (!menuItemText.equals(MainSingleton.getInstance().config.getDefaultLedMatrix())
                || (MainSingleton.getInstance().config.isAutoDetectBlackBars() && !CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS).equals(menuItemText)))) {
            if (Enums.AspectRatio.FULLSCREEN.getBaseI18n().equals(menuItemText)
                    || Enums.AspectRatio.LETTERBOX.getBaseI18n().equals(menuItemText)
                    || Enums.AspectRatio.PILLARBOX.getBaseI18n().equals(menuItemText)) {
                setAspectRatio(menuItemText, sendSetCmd);
                aspectRatioSubMenu.removeAll();
                populateAspectRatio();
            } else if (CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS).equals(menuItemText) ||
                    CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS, Locale.ENGLISH).equals(menuItemText)) {
                log.info("{}{}", CommonUtility.getWord(Constants.CAPTURE_MODE_CHANGED), CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
                MainSingleton.getInstance().config.setAutoDetectBlackBars(true);
                if (MainSingleton.getInstance().config.isMqttEnable()) {
                    CommonUtility.delaySeconds(() -> NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_ASPECT_RATIO), CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS, Locale.ENGLISH)), 1);
                    if (sendSetCmd) {
                        CommonUtility.delaySeconds(() -> NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_SET_ASPECT_RATIO), CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS, Locale.ENGLISH)), 1);
                    }
                }
                aspectRatioSubMenu.removeAll();
                populateAspectRatio();
            }
        }
    }

    /**
     * Manage Profiles Listener
     *
     * @param menuItemText item text
     */
    public void manageProfileListener(String menuItemText) {
        MainSingleton.getInstance().profileArgs = menuItemText;
        setProfileAndRestart(menuItemText);
        MainSingleton.getInstance().profileArgs = menuItemText;
        updateLEDs();
        profilesSubMenu.removeAll();
        populateProfiles();
        FireflyLuciferin.setLedNumber(MainSingleton.getInstance().config.getDefaultLedMatrix());
    }

    /**
     * Update LEDs state based on profiles
     */
    private void updateLEDs() {
        CommonUtility.turnOnLEDs();
        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
        boolean requirePipeline = Enums.Effect.BIAS_LIGHT.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_VU_METER.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_VU_METER_DUAL.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_BRIGHT.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_RAINBOW.equals(effectInUse);
        if (!MainSingleton.getInstance().RUNNING && requirePipeline) {
            MainSingleton.getInstance().guiManager.startCapturingThreads();
        } else if (MainSingleton.getInstance().RUNNING) {
            MainSingleton.getInstance().guiManager.stopCapturingThreads(true);
            if (requirePipeline) {
                CommonUtility.delaySeconds(() -> MainSingleton.getInstance().guiManager.startCapturingThreads(), 4);
            }
        }
    }

    /**
     * Udpate tray icon with new profiles
     */
    public void updateTray() {
        if (MainSingleton.getInstance().guiManager != null && MainSingleton.getInstance().guiManager.trayIconManager != null && MainSingleton.getInstance().guiManager.trayIconManager.profilesSubMenu != null) {
            MainSingleton.getInstance().guiManager.trayIconManager.profilesSubMenu.removeAll();
            MainSingleton.getInstance().guiManager.trayIconManager.populateProfiles();
        }
    }

    /**
     * Set profiles and restart if needed
     *
     * @param menuItemText text of the menu clicked
     */
    private void setProfileAndRestart(String menuItemText) {
        StorageManager sm = new StorageManager();
        MainSingleton.getInstance().config = sm.readProfileAndCheckDifference(menuItemText, sm);
        if (sm.restartNeeded) {
            if (menuItemText.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                NativeExecutor.restartNativeInstance(null);
            } else {
                NativeExecutor.restartNativeInstance(menuItemText);
            }
        }
    }

    /**
     * Set aspect ratio
     *
     * @param jMenuItemStr menu item
     * @param sendSetCmd   send mqtt msg back
     */
    private void setAspectRatio(String jMenuItemStr, boolean sendSetCmd) {
        MainSingleton.getInstance().config.setDefaultLedMatrix(jMenuItemStr);
        log.info("{}{}", CommonUtility.getWord(Constants.CAPTURE_MODE_CHANGED), jMenuItemStr);
        GStreamerGrabber.ledMatrix = MainSingleton.getInstance().config.getLedMatrixInUse(jMenuItemStr);
        MainSingleton.getInstance().config.setAutoDetectBlackBars(false);
        if (MainSingleton.getInstance().config.isMqttEnable()) {
            CommonUtility.delaySeconds(() -> NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_ASPECT_RATIO), jMenuItemStr), 1);
            if (sendSetCmd) {
                CommonUtility.delaySeconds(() -> NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_SET_ASPECT_RATIO), jMenuItemStr), 1);
            }
        }
    }

    /**
     * Create and initialize tray icon menu
     */
    public void initTray() {
        if (NativeExecutor.isSystemTraySupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // init tray images
            initializeImages();
            populateTrayWithItems();
            // listener based on the focus to auto hide the hidden dialog and the popup menu when the hidden dialog box lost focus
            hiddenDialog.setSize(10, 10);
            hiddenDialog.addWindowFocusListener(new WindowFocusListener() {
                public void windowLostFocus(final WindowEvent e) {
                    hiddenDialog.setVisible(false);
                }

                public void windowGainedFocus(final WindowEvent e) {
                    //Nothing to do
                }
            });
            // construct a TrayIcon
            String tooltipStr;
            if (MainSingleton.getInstance().config.getMultiMonitor() > 1) {
                if (Constants.SERIAL_PORT_AUTO.equals(MainSingleton.getInstance().config.getOutputDevice()) && NetworkManager.isValidIp(MainSingleton.getInstance().config.getStaticGlowWormIp())) {
                    tooltipStr = MainSingleton.getInstance().config.getStaticGlowWormIp();
                } else {
                    tooltipStr = MainSingleton.getInstance().config.getOutputDevice();
                }
            } else {
                tooltipStr = Constants.FIREFLY_LUCIFERIN;
            }
            if (MainSingleton.getInstance().communicationError) {
                trayIcon = new TrayIcon(setTrayIconImage(Enums.PlayerStatus.GREY), tooltipStr);
            } else if (MainSingleton.getInstance().config.isToggleLed()) {
                trayIcon = new TrayIcon(setTrayIconImage(Enums.PlayerStatus.STOP), tooltipStr);
            } else {
                trayIcon = new TrayIcon(setTrayIconImage(Enums.PlayerStatus.OFF), tooltipStr);
            }
            initTrayListener();
            try {
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error(String.valueOf(e));
            }
        }
    }

    /**
     * Populate tray with icons
     */
    private void populateTrayWithItems() {
        // create menu item for the default action
        GuiSingleton.getInstance().popupMenu.removeAll();
        profilesSubMenu.removeAll();
        aspectRatioSubMenu.removeAll();
        if (MainSingleton.getInstance().RUNNING || ManagerSingleton.getInstance().pipelineStarting) {
            GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.STOP)));
        } else {
            GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.START)));
        }
        if (MainSingleton.getInstance().config.isToggleLed()) {
            GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_OFF).toLowerCase())));
        } else {
            GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_ON).toLowerCase())));
        }
        addSeparator();
        populateAspectRatio();
        aspectRatioSubMenu.getPopupMenu().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(160, 160, 160)));
        populateProfiles();
        profilesSubMenu.getPopupMenu().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(160, 160, 160)));
        GuiSingleton.getInstance().popupMenu.add(aspectRatioSubMenu);
        GuiSingleton.getInstance().popupMenu.add(profilesSubMenu);
        GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.SETTINGS)));
        GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.INFO)));
        addSeparator();
        GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.TRAY_EXIT)));
    }

    /**
     * Populate aspect ratio sub menu
     */
    private void populateAspectRatio() {
        aspectRatioSubMenu.add(createMenuItem(Enums.AspectRatio.FULLSCREEN.getI18n()), 0);
        aspectRatioSubMenu.add(createMenuItem(Enums.AspectRatio.LETTERBOX.getI18n()), 1);
        aspectRatioSubMenu.add(createMenuItem(Enums.AspectRatio.PILLARBOX.getI18n()), 2);
        aspectRatioSubMenu.add(createMenuItem(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)), 3);
    }

    /**
     * Populate profiles submenu
     */
    public void populateProfiles() {
        StorageManager sm = new StorageManager();
        int index = 0;
        for (String profile : sm.listProfilesForThisInstance()) {
            profilesSubMenu.add(createMenuItem(profile), index++);
        }
        profilesSubMenu.add(createMenuItem(CommonUtility.getWord(Constants.DEFAULT)));
    }

    /**
     * Initialize listeners for tray icon
     */
    private void initTrayListener() {
        // add a listener to display the popupmenu and the hidden dialog box when the tray icon is clicked
        MouseListener ml = new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                if (timer != null && timer.isRunning()) {
                    timer.stop();
                    timer = null;
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        log.trace("Double click");
                        if (MainSingleton.getInstance().RUNNING) {
                            MainSingleton.getInstance().guiManager.stopCapturingThreads(true);
                        } else {
                            MainSingleton.getInstance().guiManager.startCapturingThreads();
                        }
                    }
                } else {
                    timer = new Timer(Constants.DBL_CLK_DELAY, _ -> {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            MainSingleton.getInstance().guiManager.showSettingsDialog(false);
                        }
                        log.trace("Single click");
                        timer.stop();
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int mainScreenOsScaling = MainSingleton.getInstance().config.getOsScaling();
                    int screenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
                    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
                    int popupMenuPositionY = scaleDownResolution(e.getY(), mainScreenOsScaling);
                    // if taskbar is at the bottom position, put the popup menu on top of the taskbar
                    if (e.getY() > screenHeight / 2) {
                        int taskbarHeight = screenInsets.bottom;
                        popupMenuPositionY = screenHeight - GuiSingleton.getInstance().popupMenu.getPreferredSize().height - taskbarHeight;
                    }
                    populateTrayWithItems();
                    GuiSingleton.getInstance().popupMenu.setLocation(scaleDownResolution(e.getX(), mainScreenOsScaling), popupMenuPositionY);
                    hiddenDialog.setLocation(scaleDownResolution(e.getX(), mainScreenOsScaling), scaleDownResolution(Constants.FAKE_GUI_TRAY_ICON, mainScreenOsScaling));
                    // important: set the hidden dialog as the invoker to hide the menu with this dialog lost focus
                    GuiSingleton.getInstance().popupMenu.setInvoker(hiddenDialog);
                    hiddenDialog.setVisible(true);
                    GuiSingleton.getInstance().popupMenu.setVisible(true);
                }
                if (e.getButton() == MouseEvent.BUTTON2) {
                    manageOnOff();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        };
        trayIcon.addMouseListener(ml);
    }

    /**
     * Toggle LEDs
     */
    private void manageOnOff() {
        MainSingleton.getInstance().config.setEffect(Enums.Effect.SOLID.getBaseI18n());
        if (MainSingleton.getInstance().config.isToggleLed()) {
            MainSingleton.getInstance().config.setToggleLed(false);
            CommonUtility.turnOffLEDs(MainSingleton.getInstance().config);
            MainSingleton.getInstance().config.setToggleLed(false);
        } else {
            MainSingleton.getInstance().config.setToggleLed(true);
            CommonUtility.turnOnLEDs();
            MainSingleton.getInstance().config.setToggleLed(true);
        }
    }

    /**
     * Initialize images for the tray icon
     */
    private void initializeImages() {
        // load an image
        imagePlay = getImage(Constants.IMAGE_CONTROL_PLAY);
        imagePlayCenter = getImage(Constants.IMAGE_CONTROL_PLAY_CENTER);
        imagePlayLeft = getImage(Constants.IMAGE_CONTROL_PLAY_LEFT);
        imagePlayRight = getImage(Constants.IMAGE_CONTROL_PLAY_RIGHT);
        imagePlayWaiting = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING);
        imagePlayWaitingCenter = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING_CENTER);
        imagePlayWaitingLeft = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT);
        imagePlayWaitingRight = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT);
        imageStop = getImage(Constants.IMAGE_TRAY_STOP);
        imageStopOff = getImage(Constants.IMAGE_CONTROL_LOGO_OFF);
        imageStopCenter = getImage(Constants.IMAGE_CONTROL_LOGO_CENTER);
        imageStopLeft = getImage(Constants.IMAGE_CONTROL_LOGO_LEFT);
        imageStopRight = getImage(Constants.IMAGE_CONTROL_LOGO_RIGHT);
        imageStopCenterOff = getImage(Constants.IMAGE_CONTROL_LOGO_CENTER_OFF);
        imageStopLeftOff = getImage(Constants.IMAGE_CONTROL_LOGO_LEFT_OFF);
        imageStopRightOff = getImage(Constants.IMAGE_CONTROL_LOGO_RIGHT_OFF);
        imageGreyStop = getImage(Constants.IMAGE_CONTROL_GREY);
        imageGreyStopCenter = getImage(Constants.IMAGE_CONTROL_GREY_CENTER);
        imageGreyStopLeft = getImage(Constants.IMAGE_CONTROL_GREY_LEFT);
        imageGreyStopRight = getImage(Constants.IMAGE_CONTROL_GREY_RIGHT);
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            imagePlayRight = getImage(Constants.IMAGE_CONTROL_PLAY_RIGHT_GOLD);
            imagePlayWaitingRight = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT_GOLD);
            imageStopRight = getImage(Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD);
            imageStopRightOff = getImage(Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD_OFF);
            imageGreyStopRight = getImage(Constants.IMAGE_CONTROL_GREY_RIGHT_GOLD);
        }
    }

    /**
     * Create an image from a path
     *
     * @param imgPath image path
     * @return Image
     */
    private Image getImage(String imgPath) {
        if (NativeExecutor.isWindows() || !NativeExecutor.isSystemTraySupported()) {
            return Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(imgPath));
        } else {
            return Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(imgPath)).getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        }
    }

    /**
     * Add a menu item to the tray icon popupMenu
     *
     * @param menuLabel label to use on the menu item
     */
    public JMenuItem createMenuItem(String menuLabel) {
        final JMenuItem jMenuItem = new JMenuItem(menuLabel);
        jMenuItem.setOpaque(true);
        Enums.AspectRatio aspectRatio = LocalizedEnum.fromStr(Enums.AspectRatio.class, menuLabel);
        String menuItemText = aspectRatio != null ? aspectRatio.getBaseI18n() : jMenuItem.getText();
        Font f = new Font(Constants.TRAY_MENU_FONT_TYPE, Font.BOLD, Constants.TRAY_MENU_FONT_SIZE);
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
     *
     * @param menuLabel label to use
     * @return formatted JMenu
     */
    public JMenu createSubMenuItem(String menuLabel) {
        final JMenu menu = new JMenu(menuLabel);
        menu.setOpaque(true);
        Enums.AspectRatio aspectRatio = LocalizedEnum.fromStr(Enums.AspectRatio.class, menuLabel);
        String menuItemText = aspectRatio != null ? aspectRatio.getBaseI18n() : menu.getText();
        Font f = new Font(Constants.TRAY_MENU_FONT_TYPE, Font.BOLD, Constants.TRAY_MENU_FONT_SIZE);
        menu.setFont(f);
        setMenuItemStyle(menuLabel, menu, menuItemText);
        menu.setBorder(BorderFactory.createMatteBorder(3, 10, 3, 7, Color.GRAY));
        menu.setBorderPainted(false);
        menu.setBackground(getBackgroundColor());
        return menu;
    }

    /**
     * Get color to use for the menu background
     *
     * @return color based on the theme in use
     */
    private Color getBackgroundColor() {
        var theme = LocalizedEnum.fromBaseStr(Enums.Theme.class, MainSingleton.getInstance().config.getTheme());
        Color color = Color.WHITE;
        switch (theme) {
            case DARK_THEME_CYAN -> color = new Color(80, 89, 96);
            case DARK_BLUE_THEME -> color = new Color(46, 61, 88);
            case DARK_THEME_ORANGE -> color = new Color(72, 72, 72);
            case DARK_THEME_PURPLE -> color = new Color(105, 105, 130);
            case CLASSIC -> color = new Color(244, 244, 244);
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
        GuiSingleton.getInstance().popupMenu.add(s);
    }

    /**
     * Set style on menu items
     *
     * @param menuLabel    item label
     * @param jMenuItem    item object
     * @param menuItemText used to color text when aspect ratio is set to Auto
     */
    private void setMenuItemStyle(String menuLabel, JMenuItem jMenuItem, String menuItemText) {
        var theme = LocalizedEnum.fromBaseStr(Enums.Theme.class, MainSingleton.getInstance().config.getTheme());
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
            case CLASSIC -> {
                UIManager.put("MenuItem.selectionBackground", new Color(0, 153, 255));
                UIManager.put("MenuItem.selectionForeground", new Color(211, 211, 211));
                UIManager.put("MenuItem.foreground", new Color(50, 50, 50));
                UIManager.put("Menu.foreground", new Color(50, 50, 50));
                UIManager.put("Menu.selectionBackground", new Color(0, 153, 255));
                UIManager.put("Menu.selectionForeground", new Color(211, 211, 211));
            }
        }
        if (menuLabel != null && menuItemText != null && jMenuItem != null) {
            if ((menuItemText.equals(MainSingleton.getInstance().config.getDefaultLedMatrix()) && !MainSingleton.getInstance().config.isAutoDetectBlackBars())
                    || (menuLabel.equals(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)) && MainSingleton.getInstance().config.isAutoDetectBlackBars())) {
                jMenuItem.setForeground(new Color(0, 153, 255));
            }
            if (menuLabel.equals(MainSingleton.getInstance().profileArgs)
                    || (menuLabel.equals(CommonUtility.getWord(Constants.DEFAULT))
                    && MainSingleton.getInstance().profileArgs.equals(Constants.DEFAULT))) {
                jMenuItem.setForeground(new Color(0, 153, 255));
            }
        }
    }

    /**
     * Return the localized tray icon menu string
     *
     * @param jMenuItem containing the base locale string
     * @return localized string if any
     */
    private String getMenuString(JMenuItem jMenuItem) {
        Enums.AspectRatio aspectRatio = LocalizedEnum.fromStr(Enums.AspectRatio.class, jMenuItem.getText());
        return aspectRatio != null ? aspectRatio.getBaseI18n() : jMenuItem.getText();
    }

    /**
     * Reset try icon after a serial reconnection
     */
    public void resetTray() {
        if (NativeExecutor.isSystemTraySupported()) {
            setTrayIconImage(Enums.PlayerStatus.STOP);
        }
    }

    /**
     * Set and return tray icon image
     *
     * @param playerStatus status
     * @return tray icon
     */
    @SuppressWarnings("Duplicates")
    public Image setTrayIconImage(Enums.PlayerStatus playerStatus) {
        Image img = switch (playerStatus) {
            case PLAY -> setImage(imagePlay, imagePlayRight, imagePlayLeft, imagePlayCenter);
            case PLAY_WAITING ->
                    setImage(imagePlayWaiting, imagePlayWaitingRight, imagePlayWaitingLeft, imagePlayWaitingCenter);
            case STOP -> setImage(imageStop, imageStopRight, imageStopLeft, imageStopCenter);
            case GREY -> setImage(imageGreyStop, imageGreyStopRight, imageGreyStopLeft, imageGreyStopCenter);
            case OFF -> setImage(imageStopOff, imageStopRightOff, imageStopLeftOff, imageStopCenterOff);
        };
        if (trayIcon != null) {
            trayIcon.setImageAutoSize(NativeExecutor.isWindows());
            trayIcon.setImage(img);
        }
        return img;
    }

    /**
     * Set image
     *
     * @param imagePlay       image
     * @param imagePlayRight  image
     * @param imagePlayLeft   image
     * @param imagePlayCenter image
     * @return tray image
     */
    @SuppressWarnings("Duplicates")
    private Image setImage(Image imagePlay, Image imagePlayRight, Image imagePlayLeft, Image imagePlayCenter) {
        Image img = null;
        switch (MainSingleton.getInstance().whoAmI) {
            case 1 -> {
                if ((MainSingleton.getInstance().config.getMultiMonitor() == 1)) {
                    img = imagePlay;
                } else {
                    img = imagePlayRight;
                }
            }
            case 2 -> {
                if ((MainSingleton.getInstance().config.getMultiMonitor() == 2)) {
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
