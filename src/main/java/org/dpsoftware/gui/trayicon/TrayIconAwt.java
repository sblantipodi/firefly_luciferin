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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    Map<String, Color> css;

    /**
     * Constructor
     */
    public TrayIconAwt() {
        var theme = LocalizedEnum.fromBaseStr(Enums.Theme.class, MainSingleton.getInstance().config.getTheme());
        css = loadColors(theme.getCssPath());
        setMenuItemStyle(null, null, null);
        GuiSingleton.getInstance().popupMenu = new JPopupMenu();
        GuiSingleton.getInstance().popupMenu.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, css.get(Constants.CSS_TRAY_MENU_BORDER)));
        aspectRatioSubMenu = createSubMenuItem(CommonUtility.getWord(Constants.ASPECT_RATIO) + " ");
        profilesSubMenu = createSubMenuItem(CommonUtility.getWord(Constants.PROFILES) + " ");
        initMenuListener();
    }

    /**
     * Load css values into a map
     *
     * @param cssPath css path
     * @return map of css properties
     */
    public static Map<String, Color> loadColors(String cssPath) {
        Map<String, Color> colors = new HashMap<>();
        try (InputStream is = TrayIconAwt.class.getResourceAsStream(Constants.GUI_RES_PATH + cssPath)) {
            if (is == null) {
                throw new RuntimeException("CSS non found: " + cssPath);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            // Regex that captures classname and hex color
            Pattern pattern = Pattern.compile(Constants.CSS_COLOR_REGEX);
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    String key = m.group(1).trim();
                    String hex = m.group(2).trim();
                    if (!hex.startsWith(Constants.SHARP)) {
                        hex = Constants.SHARP + hex;
                    }
                    Color color = parseColor(hex);
                    colors.put(key, color);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return colors;
    }

    /**
     * Parse hex colors
     *
     * @param hex color
     * @return hex color from css
     */
    private static Color parseColor(String hex) {
        hex = hex.replace(Constants.SHARP, "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        int a = (hex.length() == 8) ? Integer.parseInt(hex.substring(6, 8), 16) : 255;
        return new Color(r, g, b, a);
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
        MainSingleton.getInstance().profileArg = menuItemText;
        setProfileAndRestart(menuItemText);
        MainSingleton.getInstance().profileArg = menuItemText;
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
        aspectRatioSubMenu.getPopupMenu().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, css.get(Constants.CSS_TRAY_MENU_BORDER)));
        populateProfiles();
        profilesSubMenu.getPopupMenu().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, css.get(Constants.CSS_TRAY_MENU_BORDER)));
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
        jMenuItem.setBorder(BorderFactory.createMatteBorder(3, 10, 3, 7, css.get(Constants.CSS_TRAY_ITEM_BORDER)));
        jMenuItem.setBorderPainted(false);
        jMenuItem.addActionListener(menuListener);
        jMenuItem.setBackground(css.get("tray_background"));
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
        menu.setBorder(BorderFactory.createMatteBorder(3, 10, 3, 7, css.get(Constants.CSS_TRAY_ITEM_BORDER)));
        menu.setBorderPainted(false);
        menu.setBackground(css.get("tray_background"));
        return menu;
    }

    /**
     * Add a separator between menuitems
     */
    private void addSeparator() {
        JSeparator s = new JSeparator();
        s.setOrientation(JSeparator.HORIZONTAL);
        s.setBackground(css.get("tray_separator"));
        s.setForeground(css.get("tray_separator"));
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
        UIManager.put(Constants.CSS_TRAY_ITEM_SELECTIONBACKGROUND_KEY, css.get(Constants.CSS_TRAY_ITEM_SELECTIONBACKGROUND));
        UIManager.put(Constants.CSS_TRAY_ITEM_SELECTIONFOREGROUND_KEY, css.get(Constants.CSS_TRAY_ITEM_SELECTIONFOREGROUND));
        UIManager.put(Constants.CSS_TRAY_ITEM_FOREGROUND_KEY, css.get(Constants.CSS_TRAY_ITEM_FOREGROUND));
        UIManager.put(Constants.CSS_TRAY_FOREGROUND_KEY, css.get(Constants.CSS_TRAY_FOREGROUND));
        UIManager.put(Constants.CSS_TRAY_SELECTIONBACKGROUND_KEY, css.get(Constants.CSS_TRAY_SELECTIONBACKGROUND));
        UIManager.put(Constants.CSS_TRAY_SELECTIONFOREGROUND_KEY, css.get(Constants.CSS_TRAY_SELECTIONFOREGROUND));
        if (menuLabel != null && menuItemText != null && jMenuItem != null) {
            if ((menuItemText.equals(MainSingleton.getInstance().config.getDefaultLedMatrix()) && !MainSingleton.getInstance().config.isAutoDetectBlackBars())
                    || (menuLabel.equals(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)) && MainSingleton.getInstance().config.isAutoDetectBlackBars())) {
                jMenuItem.setForeground(css.get(Constants.CSS_TRAY_ITEM_TEXT));
            }
            if (menuLabel.equals(MainSingleton.getInstance().profileArg)
                    || (menuLabel.equals(CommonUtility.getWord(Constants.DEFAULT))
                    && MainSingleton.getInstance().profileArg.equals(Constants.DEFAULT))) {
                jMenuItem.setForeground(css.get(Constants.CSS_TRAY_ITEM_TEXT));
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
