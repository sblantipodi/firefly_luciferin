/*
  TrayIconAwt.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.gui.trayicon;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

import static org.dpsoftware.utilities.CommonUtility.scaleDownResolution;

/**
 * This class manages the AWT tray icon features
 */
@Slf4j
public class TrayIconAwt extends TrayIconBase implements TrayIconManager {

    // hidden dialog displayed behing the system tray to auto hide the popup menu when clicking somewhere else on the screen
    final JDialog hiddenDialog = new JDialog();
    // Tray icon
    public TrayIcon trayIcon = null;
    public JMenu profilesSubMenu;
    JMenu aspectRatioSubMenu;
    ActionListener menuListener;
    int popupMenuHeight;

    /**
     * Constructor
     */
    public TrayIconAwt() {
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
                stopAction();
            } else if (CommonUtility.getWord(Constants.START).equals(menuItemText)) {
                startAction();
            } else if (CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_OFF).toLowerCase()).equals(menuItemText)) {
                turnOffAction();
            } else if (CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_ON).toLowerCase()).equals(menuItemText)) {
                turnOnAction();
            } else if (CommonUtility.getWord(Constants.SETTINGS).equals(menuItemText)) {
                settingsAction();
            } else if (CommonUtility.getWord(Constants.INFO).equals(menuItemText)) {
                infoAction();
            } else if ((MainSingleton.getInstance().whoAmI == 1) && (CommonUtility.getWord(Constants.CHECK_UPDATE).equals(menuItemText) || CommonUtility.getWord(Constants.INSTALL_UPDATE).equals(menuItemText))) {
                showCheckForUpdate();
            } else {
                profileAction(menuItemText);
            }
        };
    }

    /**
     * Manage profile listener action
     *
     * @param selectedProfile from the tray icon
     */
    public void profileAction(String selectedProfile) {
        super.profileAction(selectedProfile);
        manageAspectRatioListener(selectedProfile, true);
    }

    /**
     * Manage aspect ratio listener actions
     *
     * @param menuItemText item text
     * @param sendSetCmd   send mqtt msg back
     */
    @Override
    public void manageAspectRatioListener(String menuItemText, boolean sendSetCmd) {
        super.manageAspectRatioListener(menuItemText, sendSetCmd);
        aspectRatioSubMenu.removeAll();
        populateAspectRatio();
    }

    /**
     * Manage Profiles Listener
     *
     * @param menuItemText item text
     */
    @Override
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
     * Udpate tray icon with new profiles
     */
    @Override
    public void updateTray() {
        if (MainSingleton.getInstance().guiManager != null && MainSingleton.getInstance().guiManager.trayIconManager != null && ((TrayIconAwt) MainSingleton.getInstance().guiManager.trayIconManager).profilesSubMenu != null) {
            populateTrayWithItems();
        }
    }

    /**
     * Create and initialize tray icon menu
     */
    @Override
    public void initTray() {
        if (NativeExecutor.isSystemTraySupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
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
            String tooltipStr = getTooltip();
            if (MainSingleton.getInstance().communicationError) {
                trayIcon = new TrayIcon(getImage(setTrayIconImage(Enums.PlayerStatus.GREY)), tooltipStr);
            } else if (MainSingleton.getInstance().config.isToggleLed()) {
                trayIcon = new TrayIcon(getImage(setTrayIconImage(Enums.PlayerStatus.STOP)), tooltipStr);
            } else {
                trayIcon = new TrayIcon(getImage(setTrayIconImage(Enums.PlayerStatus.OFF)), tooltipStr);
            }
            initTrayListener();
            try {
                trayIcon.setImageAutoSize(NativeExecutor.isWindows());
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
        if ((MainSingleton.getInstance().whoAmI == 1)) {
            if (GuiSingleton.getInstance().isUpgrade() && !NativeExecutor.isRunningOnSandbox()) {
                addSeparator();
                GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.INSTALL_UPDATE)));
            } else {
                GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.CHECK_UPDATE)));
            }
        }
        addSeparator();
        GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.TRAY_EXIT)));
        if (popupMenuHeight == 0) {
            popupMenuHeight = GuiSingleton.getInstance().popupMenu.getPreferredSize().height;
        }
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
    @Override
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
                    int mainScreenOsScaling = 100;
                    DisplayManager displayManager = new DisplayManager();
                    if (displayManager.getPrimaryDisplay() != null) {
                        mainScreenOsScaling = (int) (displayManager.getPrimaryDisplay().scaleX * 100);
                    } else if (displayManager.getFirstInstanceDisplay() != null) {
                        mainScreenOsScaling = (int) (displayManager.getFirstInstanceDisplay().scaleX * 100);
                    }
                    int screenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
                    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
                    int popupMenuPositionY = scaleDownResolution(e.getY(), mainScreenOsScaling);
                    // if taskbar is at the bottom position, put the popup menu on top of the taskbar
                    if (e.getY() > screenHeight / 2) {
                        int taskbarHeight = screenInsets.bottom;
                        popupMenuPositionY = screenHeight - popupMenuHeight - taskbarHeight;
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
     * Set and return tray icon image
     *
     * @param playerStatus status
     * @return tray icon
     */
    @Override
    public String setTrayIconImage(Enums.PlayerStatus playerStatus) {
        String imgStr = GuiManager.computeImageToUse(playerStatus);
        if (trayIcon != null) {
            trayIcon.setImageAutoSize(NativeExecutor.isWindows());
            trayIcon.setImage(getImage(imgStr));
        }
        return imgStr;
    }

    /**
     * Create an image from a path
     *
     * @param imgPath image path
     * @return Image
     */
    @SuppressWarnings("all")
    public Image getImage(String imgPath) {
        if (NativeExecutor.isLinux()) {
            return Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(imgPath)).getScaledInstance(16, 16, Image.SCALE_DEFAULT);
        } else {
            return Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(imgPath));
        }
    }

}
