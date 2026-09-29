/*
  TrayIconAwt.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicMenuUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
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
        stylePopupMenu(GuiSingleton.getInstance().popupMenu);
        aspectRatioSubMenu = createSubMenuItem(CommonUtility.getWord(LabelKey.ASPECT_RATIO) + " ");
        profilesSubMenu = createSubMenuItem(CommonUtility.getWord(LabelKey.PROFILES) + " ");
        stylePopupMenu(aspectRatioSubMenu.getPopupMenu());
        stylePopupMenu(profilesSubMenu.getPopupMenu());
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
            if (CommonUtility.getWord(LabelKey.STOP).equals(menuItemText)) {
                stopAction();
            } else if (CommonUtility.getWord(LabelKey.START).equals(menuItemText)) {
                startAction();
            } else if (CommonUtility.capitalize(CommonUtility.getWord(LabelKey.TURN_LED_OFF).toLowerCase()).equals(menuItemText)) {
                turnOffAction();
            } else if (CommonUtility.capitalize(CommonUtility.getWord(LabelKey.TURN_LED_ON).toLowerCase()).equals(menuItemText)) {
                turnOnAction();
            } else if (CommonUtility.getWord(LabelKey.SETTINGS).equals(menuItemText)) {
                settingsAction();
            } else if (CommonUtility.getWord(LabelKey.WEB_INTERFACE).equals(menuItemText)) {
                webInterfaceAction();
            } else if (CommonUtility.getWord(LabelKey.INFO).equals(menuItemText)) {
                infoAction();
            } else if ((MainSingleton.getInstance().whoAmI == 1) && (CommonUtility.getWord(LabelKey.CHECK_UPDATE).equals(menuItemText) || CommonUtility.getWord(LabelKey.INSTALL_UPDATE).equals(menuItemText))) {
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
     * Paint a rounded highlight for an armed or selected menu item.
     *
     * @param g              graphics context
     * @param item           menu item to highlight
     * @param selectionColor highlight color from the theme
     */
    private static void paintMenuSelection(Graphics g, JMenuItem item, Color selectionColor) {
        if (!item.getModel().isArmed() && !item.getModel().isSelected()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(selectionColor);
        g2.fillRoundRect(2, 1, item.getWidth() - 4, item.getHeight() - 2, 10, 10);
        g2.dispose();
    }

    /**
     * Populate aspect ratio sub menu
     */
    private void populateAspectRatio() {
        aspectRatioSubMenu.add(createMenuItem(Enums.AspectRatio.FULLSCREEN.getI18n()), 0);
        aspectRatioSubMenu.add(createMenuItem(Enums.AspectRatio.LETTERBOX.getI18n()), 1);
        aspectRatioSubMenu.add(createMenuItem(Enums.AspectRatio.PILLARBOX.getI18n()), 2);
        aspectRatioSubMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.AUTO_DETECT_BLACK_BARS)), 3);
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
        profilesSubMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.DEFAULT)));
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
            GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.STOP)));
        } else {
            GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.START)));
        }
        if (MainSingleton.getInstance().config.isToggleLed()) {
            GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.capitalize(CommonUtility.getWord(LabelKey.TURN_LED_OFF).toLowerCase())));
        } else {
            GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.capitalize(CommonUtility.getWord(LabelKey.TURN_LED_ON).toLowerCase())));
        }
        addSeparator();
        populateAspectRatio();
        populateProfiles();
        GuiSingleton.getInstance().popupMenu.add(aspectRatioSubMenu);
        GuiSingleton.getInstance().popupMenu.add(profilesSubMenu);
        GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.SETTINGS)));
        if (MainSingleton.getInstance().config.isWebMcpServerEnabled()) {
            GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.WEB_INTERFACE)));
        }
        GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.INFO)));
        if ((MainSingleton.getInstance().whoAmI == 1)) {
            if (GuiSingleton.getInstance().isUpgrade() && !NativeExecutor.isRunningOnSandbox()) {
                addSeparator();
                GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.INSTALL_UPDATE)));
            } else {
                GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.CHECK_UPDATE)));
            }
        }
        addSeparator();
        GuiSingleton.getInstance().popupMenu.add(createMenuItem(CommonUtility.getWord(LabelKey.TRAY_EXIT)));
        popupMenuHeight = GuiSingleton.getInstance().popupMenu.getPreferredSize().height;
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
                    int screenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
                    int screenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
                    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
                    Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
                    populateTrayWithItems();
                    int popupMenuWidth = (int) GuiSingleton.getInstance().popupMenu.getPreferredSize().getWidth();
                    int popupMenuPositionX = scaleDownResolution(e.getX(), mainScreenOsScaling);
                    int popupMenuPositionY = scaleDownResolution(e.getY(), mainScreenOsScaling);
                    // if the taskbar is at the bottom, put the popup menu on top of the taskbar
                    if (screenInsets.bottom > 0) {
                        popupMenuPositionY = screenHeight - screenInsets.bottom - popupMenuHeight;
                    } else if (screenInsets.top > 0) {
                        // if the taskbar is at the top, put the popup menu below the taskbar
                        popupMenuPositionY = screenInsets.top;
                    }
                    // if the taskbar is on the left, put the popup menu on the right of the taskbar
                    if (screenInsets.left > 0) {
                        popupMenuPositionX = screenInsets.left;
                    } else if (screenInsets.right > 0) {
                        // if the taskbar is on the right, put the popup menu on the left of the taskbar
                        popupMenuPositionX = screenWidth - screenInsets.right - popupMenuWidth;
                    }
                    // clamp the popup inside the primary monitor bounds, in case the click coords are unreliable
                    popupMenuPositionX = Math.clamp(popupMenuPositionX, bounds.x, (int) bounds.getMaxX() - popupMenuWidth);
                    popupMenuPositionY = Math.clamp(popupMenuPositionY, bounds.y, (int) bounds.getMaxY() - popupMenuHeight);
                    log.trace("Tray popup at ({},{}) | taskbar insets t={} b={} l={} r={} | screen {}x{} | bounds {}",
                            popupMenuPositionX, popupMenuPositionY, screenInsets.top, screenInsets.bottom, screenInsets.left, screenInsets.right,
                            screenWidth, screenHeight, bounds);
                    GuiSingleton.getInstance().popupMenu.setLocation(popupMenuPositionX, popupMenuPositionY);
                    hiddenDialog.setLocation(scaleDownResolution(e.getX(), mainScreenOsScaling), scaleDownResolution(Constants.FAKE_GUI_TRAY_ICON, mainScreenOsScaling));
                    // important: set the hidden dialog as the invoker to hide the menu with this dialog lost focus
                    GuiSingleton.getInstance().popupMenu.setInvoker(hiddenDialog);
                    hiddenDialog.setVisible(true);
                    GuiSingleton.getInstance().popupMenu.setVisible(true);
                    clipPopupWindow(GuiSingleton.getInstance().popupMenu);
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
        jMenuItem.setUI(new BasicMenuItemUI() {
            @Override
            protected void paintBackground(Graphics g, JMenuItem item, Color background) {
                paintMenuSelection(g, item, css.get(Constants.CSS_TRAY_ITEM_SELECTIONBACKGROUND));
            }
        });
        jMenuItem.setOpaque(false);
        Enums.AspectRatio aspectRatio = LocalizedEnum.fromStr(Enums.AspectRatio.class, menuLabel);
        String menuItemText = aspectRatio != null ? aspectRatio.getBaseI18n() : jMenuItem.getText();
        Font f = new Font(Constants.TRAY_MENU_FONT_TYPE, Font.BOLD, Constants.TRAY_MENU_FONT_SIZE + 1);
        jMenuItem.setFont(f);
        setMenuItemStyle(menuLabel, jMenuItem, menuItemText);
        jMenuItem.setBorder(new EmptyBorder(5, 13, 5, 11));
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
        menu.setUI(new BasicMenuUI() {
            @Override
            protected void paintBackground(Graphics g, JMenuItem item, Color background) {
                paintMenuSelection(g, item, css.get(Constants.CSS_TRAY_SELECTIONBACKGROUND));
            }
        });
        menu.setOpaque(false);
        Enums.AspectRatio aspectRatio = LocalizedEnum.fromStr(Enums.AspectRatio.class, menuLabel);
        String menuItemText = aspectRatio != null ? aspectRatio.getBaseI18n() : menu.getText();
        Font f = new Font(Constants.TRAY_MENU_FONT_TYPE, Font.BOLD, Constants.TRAY_MENU_FONT_SIZE + 1);
        menu.setFont(f);
        setMenuItemStyle(menuLabel, menu, menuItemText);
        menu.setBorder(new EmptyBorder(5, 13, 5, 11));
        menu.setBackground(css.get("tray_background"));
        return menu;
    }

    /**
     * Add a separator between menuitems
     */
    private void addSeparator() {
        JSeparator s = new JSeparator() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(css.get("tray_separator"));
                g.drawLine(12, getHeight() / 2, getWidth() - 13, getHeight() / 2);
            }
        };
        s.setOpaque(false);
        s.setBorder(new EmptyBorder(5, 0, 5, 0));
        GuiSingleton.getInstance().popupMenu.add(s);
    }

    /**
     * Paint the popup with the theme background and rounded corners.
     *
     * @param popupMenu menu to style
     */
    private void stylePopupMenu(JPopupMenu popupMenu) {
        popupMenu.setUI(new BasicPopupMenuUI() {
            @Override
            public void paint(Graphics g, JComponent component) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(css.get("tray_background"));
                g2.fillRoundRect(0, 0, component.getWidth(), component.getHeight(), 14, 14);
                g2.dispose();
            }
        });
        popupMenu.setOpaque(false);
        popupMenu.setBackground(css.get("tray_background"));
        popupMenu.setBorder(new EmptyBorder(6, 6, 6, 6));
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                SwingUtilities.invokeLater(() -> clipPopupWindow(popupMenu));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent event) {
            }
        });
    }

    /**
     * Clip the popup's native window so its corners do not show the window background.
     *
     * @param popupMenu visible popup whose window should be clipped
     */
    private void clipPopupWindow(JPopupMenu popupMenu) {
        Window window = SwingUtilities.getWindowAncestor(popupMenu);
        if (!(window instanceof JWindow) || !popupMenu.isShowing() || window.getWidth() < 1 || window.getHeight() < 1) {
            return;
        }
        GraphicsDevice device = window.getGraphicsConfiguration().getDevice();
        popupMenu.setOpaque(false);
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            window.setBackground(new Color(0, 0, 0, 0));
        }
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            window.setShape(new RoundRectangle2D.Float(0, 0, window.getWidth(), window.getHeight(), 14, 14));
        } else {
            // Keep the CSS background in the corners on systems without shaped windows.
            popupMenu.setOpaque(true);
        }
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
                    || (menuLabel.equals(CommonUtility.getWord(LabelKey.AUTO_DETECT_BLACK_BARS)) && MainSingleton.getInstance().config.isAutoDetectBlackBars())) {
                jMenuItem.setForeground(css.get(Constants.CSS_TRAY_ITEM_TEXT));
            }
            if (menuLabel.equals(MainSingleton.getInstance().profileArg)
                    || (menuLabel.equals(CommonUtility.getWord(LabelKey.DEFAULT))
                    && MainSingleton.getInstance().profileArg.equals(LabelKey.DEFAULT))) {
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
