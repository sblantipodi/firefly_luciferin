/*
  GUIManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.UpgradeManager;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.managers.dto.StateStatusDto;
import org.dpsoftware.network.MessageClient;
import org.dpsoftware.utilities.CommonUtility;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;


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
    // hidden dialog displayed behing the system tray to auto hide the popup menu when clicking somewhere else on the screen
    final JDialog hiddenDialog = new JDialog ();
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
    public GUIManager(Stage stage) throws HeadlessException {

        this.stage = stage;
        pipelineManager = new PipelineManager();
        popupMenu = new JPopupMenu() {
            @Override
            public void paintComponent(final Graphics g) {
                if (FireflyLuciferin.config.getTheme().equals(Constants.Theme.DEFAULT.getTheme())) {
                    g.setColor(new Color(244, 244, 244));
                } else {
                    g.setColor(new Color(80, 89, 96));
                }
                g.fillRect(0,0,getWidth(), getHeight());
            }
        };
        initMenuListener();

    }

    /**
     * Init menu listener
     */
    private void initMenuListener() {

        //Action listener to get click on top menu items
        menuListener = e -> {
            JMenuItem jMenuItem = (JMenuItem) e.getSource();
            switch (jMenuItem.getText()) {
                case Constants.STOP -> stopCapturingThreads(true);
                case Constants.START -> startCapturingThreads();
                case Constants.SETTINGS -> showSettingsDialog();
                case Constants.INFO -> showFramerateDialog();
                default -> {
                    if (Constants.AspectRatio.FULLSCREEN.getAspectRatio().equals(jMenuItem.getText())
                            || Constants.AspectRatio.LETTERBOX.getAspectRatio().equals(jMenuItem.getText())
                            || Constants.AspectRatio.PILLARBOX.getAspectRatio().equals(jMenuItem.getText())) {
                        setAspetRatio(jMenuItem);
                        setAspetRatioMenuColor();
                    } else if (Constants.AUTO_DETECT_BLACK_BARS.equals(jMenuItem.getText())) {
                        log.info(Constants.CAPTURE_MODE_CHANGED + Constants.AUTO_DETECT_BLACK_BARS);
                        FireflyLuciferin.config.setAutoDetectBlackBars(true);
                        if (FireflyLuciferin.config.isMqttEnable()) {
                            MQTTManager.publishToTopic(Constants.ASPECT_RATIO_TOPIC, Constants.AUTO_DETECT_BLACK_BARS);
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
            }
        };

    }

    /**
     * Set aspect ratio
     * @param jMenuItem menu item
     */
    private void setAspetRatio(JMenuItem jMenuItem) {

        FireflyLuciferin.config.setDefaultLedMatrix(jMenuItem.getText());
        log.info(Constants.CAPTURE_MODE_CHANGED + jMenuItem.getText());
        GStreamerGrabber.ledMatrix = FireflyLuciferin.config.getLedMatrixInUse(jMenuItem.getText());
        FireflyLuciferin.config.setAutoDetectBlackBars(false);
        if (FireflyLuciferin.config.isMqttEnable()) {
            MQTTManager.publishToTopic(Constants.ASPECT_RATIO_TOPIC, jMenuItem.getText());
        }

    }

    /**
     * Set aspect ratio menu color
     */
    private void setAspetRatioMenuColor() {

        popupMenu.remove(2);
        addItemToPopupMenu(Constants.AspectRatio.FULLSCREEN.getAspectRatio(), 2);
        popupMenu.remove(3);
        addItemToPopupMenu(Constants.AspectRatio.LETTERBOX.getAspectRatio(), 3);
        popupMenu.remove(4);
        addItemToPopupMenu(Constants.AspectRatio.PILLARBOX.getAspectRatio(), 4);
        popupMenu.remove(5);
        addItemToPopupMenu(Constants.AUTO_DETECT_BLACK_BARS, 5);

    }

    /**
     * Load FXML files
     * @param fxml GUI file
     * @return fxmlloader
     * @throws IOException file exception
     */
    public static Parent loadFXML(String fxml) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(GUIManager.class.getResource( fxml + Constants.FXML));
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
            addItemToPopupMenu(Constants.START, 0);
            addItemToPopupMenu(Constants.AspectRatio.FULLSCREEN.getAspectRatio(), 2);
            addItemToPopupMenu(Constants.AspectRatio.LETTERBOX.getAspectRatio(), 3);
            addItemToPopupMenu(Constants.AspectRatio.PILLARBOX.getAspectRatio(), 4);
            addItemToPopupMenu(Constants.AUTO_DETECT_BLACK_BARS, 5);
            addItemToPopupMenu(Constants.SETTINGS, 7);
            addItemToPopupMenu(Constants.INFO, 8);
            addItemToPopupMenu(Constants.EXIT, 10);
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
                    int mainScreenOsScaling = (int) (Screen.getScreens().get(0).getOutputScaleX()*100);
                    // the dialog is also displayed at this position but it is behind the system tray
                    popupMenu.setLocation(CommonUtility.scaleResolution(e.getX(), mainScreenOsScaling),
                            CommonUtility.scaleResolution(e.getY(), mainScreenOsScaling));
                    hiddenDialog.setLocation(CommonUtility.scaleResolution(e.getX(), mainScreenOsScaling),
                            CommonUtility.scaleResolution(e.getY() + 30, mainScreenOsScaling));
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
     * @param position position of the item in the popup menu
     */
    public void addItemToPopupMenu(String menuLabel, int position) {

        final JMenuItem jMenuItem = new JMenuItem(menuLabel);
        if (FireflyLuciferin.config.getTheme().equals(Constants.Theme.DEFAULT.getTheme())) {
            jMenuItem.setForeground(new Color(50, 50, 50));
        } else {
            jMenuItem.setForeground(new Color(211, 211, 211));
        }
        if ((menuLabel.equals(FireflyLuciferin.config.getDefaultLedMatrix()) && !FireflyLuciferin.config.isAutoDetectBlackBars())
                || (menuLabel.equals(Constants.AUTO_DETECT_BLACK_BARS) && FireflyLuciferin.config.isAutoDetectBlackBars())) {
            jMenuItem.setForeground(new Color(0, 153, 255));
        }
        Font f = new Font("verdana", Font.BOLD, 10);
        jMenuItem.setMargin(new Insets(-2,-14,-2,7));
        jMenuItem.setFont(f);
        if (popupMenu.getComponentCount() < MENU_ITEMS_NUMBER) {
            switch (position) {
                case 0, 5, 8 -> popupMenu.addSeparator();
            }
        }
        popupMenu.add(jMenuItem, position);
        jMenuItem.addActionListener(menuListener);

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
        if (FireflyLuciferin.config.getTheme().equals(Constants.Theme.DARK_THEME.getTheme())) {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("css/dark-theme.css")).toExternalForm());
            dialogPane.getStyleClass().add("dialog-pane");
        }
        return alert.showAndWait();

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
        if (FireflyLuciferin.config.getTheme().equals(Constants.Theme.DARK_THEME.getTheme())) {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("css/dark-theme.css")).toExternalForm());
            dialogPane.getStyleClass().add("dialog-pane");
        }
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
    void showFramerateDialog() {

        showStage(Constants.FXML_INFO);

    }

    /**
     * Show a stage
     * @param stageName stage to show
     */
    void showStage(String stageName) {

        Platform.runLater(() -> {
            try {
                Scene scene = new Scene(loadFXML(stageName));
                if (FireflyLuciferin.config.getTheme().equals(Constants.Theme.DARK_THEME.getTheme())) {
                    scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("css/dark-theme.css")).toExternalForm());
                }
                if(stage == null) {
                    stage = new Stage();
                }
                stage.resizableProperty().setValue(Boolean.FALSE);
                stage.setScene(scene);
                String title = "  " + Constants.FIREFLY_LUCIFERIN;
                switch (JavaFXStarter.whoAmI) {
                    case 1:
                        if ((FireflyLuciferin.config.getMultiMonitor() != 1)) {
                            title += " (" + Constants.RIGHT_DISPLAY + ")";
                        }
                        break;
                    case 2:
                        if ((FireflyLuciferin.config.getMultiMonitor() == 2)) {
                            title += " (" + Constants.LEFT_DISPLAY + ")";
                        } else {
                            title += " (" + Constants.CENTER_DISPLAY + ")";
                        }
                        break;
                    case 3: title += " (" + Constants.LEFT_DISPLAY + ")"; break;
                }
                stage.setTitle(title);
                setStageIcon(stage);
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

        if (MQTTManager.client != null && publishToTopic) {
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
            stateDto.setStartStopInstances(Constants.PlayerStatus.STOP.name());
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
                addItemToPopupMenu(Constants.STOP, 0);
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
            case 1:
                if ((FireflyLuciferin.config.getMultiMonitor() == 1)) {
                    img = imagePlay;
                } else {
                    img = imagePlayRight;
                }
                break;
            case 2:
                if ((FireflyLuciferin.config.getMultiMonitor() == 2)) {
                    img = imagePlayLeft;
                } else {
                    img = imagePlayCenter;
                }
                break;
            case 3: img = imagePlayLeft; break;
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