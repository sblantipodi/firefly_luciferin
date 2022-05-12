/*
  GUIManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.UpgradeManager;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.Preset;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.managers.dto.StateStatusDto;
import org.dpsoftware.network.MessageClient;
import org.dpsoftware.utilities.CommonUtility;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import static org.dpsoftware.utilities.CommonUtility.scaleDownResolution;


/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
@Slf4j
@NoArgsConstructor
public class GUIManager extends JFrame {

    private Stage stage;
    // Tray icon
    @Getter @Setter
    TrayIcon trayIcon = null;
    // Label and framerate dialog
    @Getter JEditorPane jep = new JEditorPane();
    @Getter JFrame jFrame = new JFrame(Constants.FIREFLY_LUCIFERIN);
    public static JPopupMenu popupMenu;
    JMenu aspectRatioSubMenu;
    JMenu presetsMenu;

    // hidden dialog displayed behing the system tray to auto hide the popup menu when clicking somewhere else on the screen
    final JDialog hiddenDialog = new JDialog();
    private static final int MENU_ITEMS_NUMBER = 10;
    ActionListener menuListener;
    // Tray icons
    Image imagePlay, imagePlayCenter, imagePlayLeft, imagePlayRight, imagePlayWaiting, imagePlayWaitingCenter, imagePlayWaitingLeft, imagePlayWaitingRight;
    Image imageStop, imageStopCenter, imageStopLeft, imageStopRight;
    Image imageGreyStop, imageGreyStopCenter, imageGreyStopLeft, imageGreyStopRight;
    public PipelineManager pipelineManager;

    /**
     * Constructor
     * @param stage JavaFX stage
     * @throws HeadlessException GUI exception
     */
    public GUIManager(Stage stage) throws HeadlessException, UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        this.stage = stage;
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        setMenuItemStyle(null, null, null);
        pipelineManager = new PipelineManager();
        popupMenu = new JPopupMenu();
        popupMenu.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(160, 160, 160)));
        aspectRatioSubMenu = createSubMenuItem("Aspect Ratio ");
        presetsMenu = createSubMenuItem("Favourites ");
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
                stopCapturingThreads(true);
            } else if (CommonUtility.getWord(Constants.START).equals(menuItemText)) {
                startCapturingThreads();
            } else if (CommonUtility.getWord(Constants.SETTINGS).equals(menuItemText)) {
                showSettingsDialog();
            } else if (CommonUtility.getWord(Constants.INFO).equals(menuItemText)) {
                showFramerateDialog();
            } else {
                if (Constants.AspectRatio.FULLSCREEN.getBaseI18n().equals(menuItemText)
                        || Constants.AspectRatio.LETTERBOX.getBaseI18n().equals(menuItemText)
                        || Constants.AspectRatio.PILLARBOX.getBaseI18n().equals(menuItemText)) {
                    setAspetRatio(jMenuItem);
                    setAspetRatioMenuColor();
                } else if (CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS).equals(menuItemText)) {
                    log.info(CommonUtility.getWord(Constants.CAPTURE_MODE_CHANGED) + CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
                    FireflyLuciferin.config.setAutoDetectBlackBars(true);
                    if (FireflyLuciferin.config.isMqttEnable()) {
                        MQTTManager.publishToTopic(Constants.ASPECT_RATIO_TOPIC, CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
                    }
                    setAspetRatioMenuColor();
                } else {
                    if (FireflyLuciferin.RUNNING) {
                        stopCapturingThreads(true);
                    }
                    log.debug(Constants.CLEAN_EXIT);
                    FireflyLuciferin.exit();
                }
            }
        };
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
     * Set aspect ratio menu color
     */
    private void setAspetRatioMenuColor() {
        aspectRatioSubMenu.removeAll();
        aspectRatioSubMenu.add(createMenuItem(Constants.AspectRatio.FULLSCREEN.getI18n()), 0);
        aspectRatioSubMenu.add(createMenuItem(Constants.AspectRatio.LETTERBOX.getI18n()), 1);
        aspectRatioSubMenu.add(createMenuItem(Constants.AspectRatio.PILLARBOX.getI18n()), 2);
        aspectRatioSubMenu.add(createMenuItem(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)), 3);
    }

    /**
     * Load FXML files
     * @param fxml GUI file
     * @return fxmlloader
     * @throws IOException file exception
     */
    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GUIManager.class.getResource( fxml + Constants.FXML), FireflyLuciferin.bundle);
        return fxmlLoader.load();
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
            aspectRatioSubMenu.add(createMenuItem(Constants.AspectRatio.FULLSCREEN.getI18n()), 0);
            aspectRatioSubMenu.add(createMenuItem(Constants.AspectRatio.LETTERBOX.getI18n()), 1);
            aspectRatioSubMenu.add(createMenuItem(Constants.AspectRatio.PILLARBOX.getI18n()), 2);
            aspectRatioSubMenu.add(createMenuItem(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)), 3);
            aspectRatioSubMenu.getPopupMenu().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(160, 160, 160)));
            int index = 0;
            for (Preset preset : FireflyLuciferin.config.getPresets()) {
                presetsMenu.add(createMenuItem(preset.getPresetName()), index++);
            }
            presetsMenu.getPopupMenu().setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(160, 160, 160)));
            popupMenu.add(aspectRatioSubMenu);
            popupMenu.add(presetsMenu);
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
        if (!NativeExecutor.isWindows() && !NativeExecutor.isMac()) {
            showSettingsDialog();
        }
        UpgradeManager upgradeManager = new UpgradeManager();
        upgradeManager.checkForUpdates(stage);
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
                        stopCapturingThreads(true);
                    } else {
                        startCapturingThreads();
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
        jMenuItem.setBorder(BorderFactory.createMatteBorder(3, 10, 3, 10, Color.GRAY));
        jMenuItem.setBorderPainted(false);
        jMenuItem.addActionListener(menuListener);
        jMenuItem.setBackground(getBackgroundColor());
        return jMenuItem;
    }

    public JMenu createSubMenuItem(String menuLabel) {
        final JMenu menu = new JMenu(menuLabel);
        menu.setOpaque(true);
        Constants.AspectRatio aspectRatio = LocalizedEnum.fromStr(Constants.AspectRatio.class, menuLabel);
        String menuItemText = aspectRatio != null ? aspectRatio.getBaseI18n() : menu.getText();
        Font f = new Font("verdana", Font.BOLD, 10);
        menu.setFont(f);
        setMenuItemStyle(menuLabel, menu, menuItemText);
        menu.setBorder(BorderFactory.createMatteBorder(3, 10, 3, 10, Color.GRAY));
        menu.setBorderPainted(false);
        menu.setBackground(getBackgroundColor());
        return menu;
    }

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

    private void addSeparator() {
        if (popupMenu.getComponentCount() < MENU_ITEMS_NUMBER) {
            JSeparator s = new JSeparator();
            s.setOrientation(JSeparator.HORIZONTAL);
            s.setBackground(new Color(215, 215, 215));
            s.setForeground(new Color(215, 215, 215));
            s.setBorder(new EmptyBorder(0, 0, 0, 0)); // 20 px on left and right
            popupMenu.add(s);
        }
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
        }
        if (menuLabel != null && menuItemText != null && jMenuItem != null) {
            if (menuLabel.equals(FireflyLuciferin.config.getDefaultPreset())) {
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
     * Show alert in a JavaFX dialog
     * @param title     dialog title
     * @param header    dialog header
     * @param content   dialog msg
     * @param alertType alert type
     * @return an Object when we can listen for commands
     */
    public Optional<ButtonType> showAlert(String title, String header, String content, Alert.AlertType alertType) {
        Alert alert = createAlert(title, header, alertType);
        alert.setContentText(content);
        setAlertTheme(alert);
        return alert.showAndWait();
    }

    /**
     * Set alert theme
     * @param alert in use
     */
    private void setAlertTheme(Alert alert) {
        setStylesheet(alert.getDialogPane().getStylesheets());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
    }

    /**
     * Set style sheets
     * main.css is injected via fxml
     * @param stylesheets list containing style sheet file name
     */
    private void setStylesheet(ObservableList<String> stylesheets) {
        var theme = LocalizedEnum.fromBaseStr(Constants.Theme.class, FireflyLuciferin.config.getTheme());
        switch (theme) {
            case DARK_THEME_CYAN -> {
                stylesheets.add(Objects.requireNonNull(getClass().getResource(Constants.CSS_THEME_DARK)).toExternalForm());
                stylesheets.add(Objects.requireNonNull(getClass().getResource(Constants.CSS_THEME_DARK_CYAN)).toExternalForm());
            }
            case DARK_BLUE_THEME -> {
                stylesheets.add(Objects.requireNonNull(getClass().getResource(Constants.CSS_THEME_DARK)).toExternalForm());
                stylesheets.add(Objects.requireNonNull(getClass().getResource(Constants.CSS_THEME_DARK_BLUE)).toExternalForm());
            }
            case DARK_THEME_ORANGE -> {
                stylesheets.add(Objects.requireNonNull(getClass().getResource(Constants.CSS_THEME_DARK)).toExternalForm());
                stylesheets.add(Objects.requireNonNull(getClass().getResource(Constants.CSS_THEME_DARK_ORANGE)).toExternalForm());
            }
            case DARK_THEME_PURPLE -> {
                stylesheets.add(Objects.requireNonNull(getClass().getResource(Constants.CSS_THEME_DARK)).toExternalForm());
                stylesheets.add(Objects.requireNonNull(getClass().getResource(Constants.CSS_THEME_DARK_PURPLE)).toExternalForm());
            }
        }
    }

    /**
     * Show alert in a JavaFX dialog
     * @param title     dialog title
     * @param header    dialog header
     * @param content   dialog msg
     * @param alertType alert type
     * @return an Object when we can listen for commands
     */
    public Optional<ButtonType> showLocalizedAlert(String title, String header, String content, Alert.AlertType alertType) {
        title = CommonUtility.getWord(title);
        header = CommonUtility.getWord(header);
        content = CommonUtility.getWord(content);
        return showAlert(title, header, content, alertType);
    }

    /**
     * Show an alert that contains a Web View in a JavaFX dialog
     * @param title     dialog title
     * @param header    dialog header
     * @param webUrl URL to load inside the web view
     * @param alertType alert type
     * @return an Object when we can listen for commands
     */
    public Optional<ButtonType> showWebAlert(String title, String header, String webUrl, Alert.AlertType alertType) {
        final WebView wv = new WebView();
        wv.getEngine().load(webUrl);
        wv.setPrefWidth(450);
        wv.setPrefHeight(200);
        Alert alert = createAlert(title, header, alertType);
        alert.getDialogPane().setContent(wv);
        setAlertTheme(alert);
        return alert.showAndWait();
    }

    /**
     * Create a generic alert
     * @param title     dialog title
     * @param header    dialog header
     * @param alertType alert type
     * @return generic alert
     */
    private Alert createAlert(String title, String header, Alert.AlertType alertType) {
        Platform.setImplicitExit(false);
        Alert alert = new Alert(alertType);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        setStageIcon(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert;
    }

    /**
     * Show a dialog with all the settings
     */
    void showSettingsDialog() {
        String fxml;
        fxml = Constants.FXML_SETTINGS;
        showStage(fxml);
    }

    /**
     * Show a dialog with a framerate counter
     */
    public void showFramerateDialog() {
        showStage(Constants.FXML_INFO);
    }

    /**
     * Show a stage
     * @param stageName stage to show
     */
    void showStage(String stageName) {
        Platform.runLater(() -> {
            try {
                if (NativeExecutor.isLinux() && stageName.equals(Constants.FXML_INFO)) {
                    stage = new Stage();
                }
                Scene scene = new Scene(loadFXML(stageName));
                setStylesheet(scene.getStylesheets());
                if (NativeExecutor.isLinux()) {
                    scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(Constants.CSS_LINUX)).toExternalForm());
                }
                if(stage == null) {
                    stage = new Stage();
                }
                stage.resizableProperty().setValue(Boolean.FALSE);
                stage.setScene(scene);
                String title = "  " + Constants.FIREFLY_LUCIFERIN;
                switch (JavaFXStarter.whoAmI) {
                    case 1 -> {
                        if ((FireflyLuciferin.config.getMultiMonitor() != 1)) {
                            title += " (" + CommonUtility.getWord(Constants.RIGHT_DISPLAY) + ")";
                        }
                    }
                    case 2 -> {
                        if ((FireflyLuciferin.config.getMultiMonitor() == 2)) {
                            title += " (" + CommonUtility.getWord(Constants.LEFT_DISPLAY) + ")";
                        } else {
                            title += " (" + CommonUtility.getWord(Constants.CENTER_DISPLAY) + ")";
                        }
                    }
                    case 3 -> title += " (" + CommonUtility.getWord(Constants.LEFT_DISPLAY) + ")";
                }
                stage.setTitle(title);
                setStageIcon(stage);
                if (stageName.equals(Constants.FXML_SETTINGS) && NativeExecutor.isLinux()) {
                    stage.setIconified(true);
                }
                stage.show();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Set icon for every stage
     * @param stage in use
     */
    public static void setStageIcon(Stage stage) {
        stage.getIcons().add(new javafx.scene.image.Image(String.valueOf(GUIManager.class.getResource(Constants.IMAGE_TRAY_STOP))));
    }

    /**
     * Stop capturing threads
     */
    public void stopCapturingThreads(boolean publishToTopic) {
        if (((MQTTManager.client != null) || FireflyLuciferin.config.isWifiEnable()) && publishToTopic) {
            StateDto stateDto = new StateDto();
            stateDto.setEffect(Constants.SOLID);
            stateDto.setState(FireflyLuciferin.config.isToggleLed() ? Constants.ON : Constants.OFF);
            ColorDto colorDto = new ColorDto();
            String[] color = FireflyLuciferin.config.getColorChooser().split(",");
            colorDto.setR(Integer.parseInt(color[0]));
            colorDto.setG(Integer.parseInt(color[1]));
            colorDto.setB(Integer.parseInt(color[2]));
            stateDto.setColor(colorDto);
            stateDto.setBrightness(CommonUtility.getNightBrightness());
            stateDto.setWhitetemp(FireflyLuciferin.config.getWhiteTemperature());
            if (CommonUtility.getDeviceToUse() != null) {
                stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
            }
            stateDto.setStartStopInstances(Constants.PlayerStatus.STOP.name());
            CommonUtility.sleepMilliseconds(300);
            MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.toJsonString(stateDto));
        }
        if (FireflyLuciferin.config.getMultiMonitor() == 1 || MQTTManager.client == null || CommonUtility.isSingleDeviceMultiScreen()) {
            pipelineManager.stopCapturePipeline();
        }
        if (CommonUtility.isSingleDeviceOtherInstance()) {
            StateStatusDto stateStatusDto = new StateStatusDto();
            stateStatusDto.setAction(Constants.CLIENT_ACTION);
            stateStatusDto.setRunning(false);
            MessageClient.msgClient.sendMessage(CommonUtility.toJsonString(stateStatusDto));
        }
    }

    /**
     * Start capturing threads
     */
    public void startCapturingThreads() {
        if (!FireflyLuciferin.communicationError) {
            if (trayIcon != null) {
                popupMenu.remove(0);
                popupMenu.add(createMenuItem(CommonUtility.getWord(Constants.STOP)), 0);
                if (!FireflyLuciferin.RUNNING) {
                    setTrayIconImage(Constants.PlayerStatus.PLAY_WAITING);
                }
            }
            if (!PipelineManager.pipelineStarting) {
                pipelineManager.startCapturePipeline();
            }
            if (CommonUtility.isSingleDeviceOtherInstance()) {
                StateStatusDto stateStatusDto = new StateStatusDto();
                stateStatusDto.setAction(Constants.CLIENT_ACTION);
                stateStatusDto.setRunning(true);
                MessageClient.msgClient.sendMessage(CommonUtility.toJsonString(stateStatusDto));
            }
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

    /**
     * Open web browser on the specific URL
     * @param url address to surf on
     */
    public void surfToURL(String url) {
        if(Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                URI github = new URI(url);
                desktop.browse(github);
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
    }
}