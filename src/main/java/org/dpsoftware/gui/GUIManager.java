/*
  GUIManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2021  Davide Perini

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
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.UpgradeManager;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.managers.dto.UnsubscribeInstanceDto;
import org.dpsoftware.utility.JsonUtility;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
@Slf4j
@NoArgsConstructor
public class GUIManager extends JFrame {

    private Stage stage;
    // Tray icon
    TrayIcon trayIcon = null;
    // create a popup menu
    PopupMenu popup = new PopupMenu();
    // Label and framerate dialog
    @Getter JEditorPane jep = new JEditorPane();
    @Getter JFrame jFrame = new JFrame(Constants.FIREFLY_LUCIFERIN);
    // Menu items for start and stop
    MenuItem stopItem;
    MenuItem startItem;
    // Tray icons
    Image imagePlay, imagePlayCenter, imagePlayLeft, imagePlayRight, imagePlayWaiting, imagePlayWaitingCenter, imagePlayWaitingLeft, imagePlayWaitingRight;
    Image imageStop, imageStopCenter, imageStopLeft, imageStopRight;
    Image imageGreyStop, imageGreyStopCenter, imageGreyStopLeft, imageGreyStopRight;
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * Constructor
     * @param stage JavaFX stage
     * @throws HeadlessException GUI exception
     */
    public GUIManager(Stage stage) throws HeadlessException {

        this.stage = stage;

    }

    /**
     *
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

            // create menu item for the default action
            stopItem = new MenuItem(Constants.STOP);
            startItem = new MenuItem(Constants.START);
            MenuItem settingsItem = new MenuItem(Constants.SETTINGS);
            MenuItem infoItem = new MenuItem(Constants.INFO);
            MenuItem exitItem = new MenuItem(Constants.EXIT);

            // create a action listener to listen for default action executed on the tray icon
            ActionListener listener = initPopupMenuListener();

            stopItem.addActionListener(listener);
            startItem.addActionListener(listener);
            exitItem.addActionListener(listener);
            settingsItem.addActionListener(listener);
            infoItem.addActionListener(listener);
            popup.add(startItem);
            popup.addSeparator();

            initGrabMode();

            popup.addSeparator();
            popup.add(settingsItem);
            popup.add(infoItem);
            popup.addSeparator();
            popup.add(exitItem);
            // construct a TrayIcon
            if (FireflyLuciferin.communicationError) {
                trayIcon = new TrayIcon(setTrayIconImage(Constants.PlayerStatus.GREY), Constants.FIREFLY_LUCIFERIN, popup);
            } else {
                trayIcon = new TrayIcon(setTrayIconImage(Constants.PlayerStatus.STOP), Constants.FIREFLY_LUCIFERIN, popup);
            }
            // set the TrayIcon properties
            trayIcon.addActionListener(listener);
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error(String.valueOf(e));
            }
        }

        if (!NativeExecutor.isWindows() && !NativeExecutor.isMac()) {
            showSettingsDialog();
        }

        checkForUpdates();

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
     *  Check for updates
     */
    void checkForUpdates() {

        UpgradeManager vm = new UpgradeManager();
        // Check Firefly updates
        boolean fireflyUpdate = false;
        if (JavaFXStarter.whoAmI == 1) {
            fireflyUpdate = vm.checkFireflyUpdates(stage, this);
        }
        // If Firefly Luciferin is up to date, check for the Glow Worm Luciferin firmware
        vm.checkGlowWormUpdates(this, fireflyUpdate);

    }

    /**
     * Init popup menu
     * @return tray icon listener
     */
    ActionListener initPopupMenuListener() {

        return actionEvent -> {
            if (actionEvent.getActionCommand() == null) {
                if (FireflyLuciferin.RUNNING) {
                    stopCapturingThreads(true);
                } else {
                    startCapturingThreads();
                }
            } else {
                switch (actionEvent.getActionCommand()) {
                    case Constants.STOP -> stopCapturingThreads(true);
                    case Constants.START -> startCapturingThreads();
                    case Constants.SETTINGS -> showSettingsDialog();
                    case Constants.INFO -> showFramerateDialog();
                    default -> {
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
     * Add params in the tray icon menu for every ledMatrix found in the FireflyLuciferin.yaml
     */
    void initGrabMode() {

        FireflyLuciferin.config.getLedMatrix().forEach((ledMatrixKey, ledMatrix) -> {

            CheckboxMenuItem checkboxMenuItem = new CheckboxMenuItem(ledMatrixKey,
                    ledMatrixKey.equals(FireflyLuciferin.config.getDefaultLedMatrix()));
            checkboxMenuItem.addItemListener(itemListener -> {
                log.info(Constants.STOPPING_THREADS);
                stopCapturingThreads(true);
                for (int i=0; i < popup.getItemCount(); i++) {
                    if (popup.getItem(i) instanceof CheckboxMenuItem) {
                        if (!popup.getItem(i).getLabel().equals(checkboxMenuItem.getLabel())) {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(false);
                        } else {
                            ((CheckboxMenuItem) popup.getItem(i)).setState(true);
                            FireflyLuciferin.config.setDefaultLedMatrix(checkboxMenuItem.getLabel());
                            log.info(Constants.CAPTURE_MODE_CHANGED + checkboxMenuItem.getLabel());
                            try {
                                TimeUnit.SECONDS.sleep(5);
                            } catch (InterruptedException e) {
                                log.error(e.getMessage());
                            }
                            startCapturingThreads();
                        }
                    }
                }
            });
            popup.add(checkboxMenuItem);

        });

    }

    /**
     * Show alert in a JavaFX dialog
     * @param title dialog title
     * @param header dialog header
     * @param content dialog msg
     * @param alertType alert type
     * @return an Object when we can listen for commands
     */
    public Optional<ButtonType> showAlert(String title, String header, String content, Alert.AlertType alertType) {

        Platform.setImplicitExit(false);
        Alert alert = new Alert(alertType);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        setStageIcon(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setContentText(content);
        return alert.showAndWait();

    }

    /**
     * Show a dialog with all the settings
     */
    void showSettingsDialog() {

        String fxml;
        if (NativeExecutor.isWindows() || NativeExecutor.isMac()) {
            fxml = Constants.FXML_SETTINGS;
        } else {
            fxml = Constants.FXML_SETTINGS_LINUX;
        }
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
            stateDto.setBrightness(null);
            ColorDto colorDto = new ColorDto();
            String[] color = FireflyLuciferin.config.getColorChooser().split(",");
            colorDto.setR(Integer.parseInt(color[0]));
            colorDto.setG(Integer.parseInt(color[1]));
            colorDto.setB(Integer.parseInt(color[2]));
            stateDto.setColor(colorDto);
            stateDto.setBrightness(Integer.parseInt(color[3]));
            stateDto.setStartStopInstances(Constants.PlayerStatus.STOP.name());
            MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), JsonUtility.writeValueAsString(stateDto));
        }
        if (FireflyLuciferin.config.getMultiMonitor() == 1 || MQTTManager.client == null) {
            stopPipelines();
        }

    }


    /**
     * Stop capturing threads
     */
    public void stopPipelines() {

        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
        if (trayIcon != null) {
            setTrayIconImage(Constants.PlayerStatus.STOP);
            popup.remove(0);
            popup.insert(startItem, 0);
        }
        if (FireflyLuciferin.pipe != null && ((FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name()))
                || (FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name()))
                || (FireflyLuciferin.config.getCaptureMethod().equals(Configuration.CaptureMethod.AVFVIDEOSRC.name())))) {
            FireflyLuciferin.pipe.stop();
        }
        FireflyLuciferin.FPS_PRODUCER_COUNTER = 0;
        FireflyLuciferin.FPS_CONSUMER_COUNTER = 0;
        FireflyLuciferin.FPS_CONSUMER = 0;
        FireflyLuciferin.FPS_PRODUCER = 0;
        FireflyLuciferin.RUNNING = false;

    }

    /**
     * Start capturing threads
     */
    public void startCapturingThreads() {

        if (!FireflyLuciferin.communicationError) {
            popup.remove(0);
            popup.insert(stopItem, 0);
            if (!FireflyLuciferin.config.isMqttEnable()) {
                FireflyLuciferin.RUNNING = true;
                if (trayIcon != null) {
                    setTrayIconImage(Constants.PlayerStatus.PLAY);
                }
            } else {
                if (trayIcon != null) {
                    setTrayIconImage(Constants.PlayerStatus.PLAY_WAITING);
                }
            }
            if (MQTTManager.client != null) {
                scheduledExecutorService = Executors.newScheduledThreadPool(1);
                AtomicInteger retryNumber = new AtomicInteger();
                Runnable framerateTask = () -> {
                    GlowWormDevice glowWormDeviceToUse = null;
                    // Waiting MQTT Device
                    if (FireflyLuciferin.config.isMqttStream()) {
                        if (!FireflyLuciferin.config.getSerialPort().equals(Constants.SERIAL_PORT_AUTO) || FireflyLuciferin.config.getMultiMonitor() > 1) {
                            glowWormDeviceToUse = SettingsController.deviceTableData.stream()
                                    .filter(glowWormDevice -> glowWormDevice.getDeviceName().equals(FireflyLuciferin.config.getSerialPort()))
                                    .findAny().orElse(null);
                        } else if (SettingsController.deviceTableData != null && SettingsController.deviceTableData.size() > 0) {
                            glowWormDeviceToUse = SettingsController.deviceTableData.get(0);
                        }
                    } else {
                        // Waiting both MQTT and serial device
                        GlowWormDevice glowWormDeviceSerial = SettingsController.deviceTableData.stream()
                                .filter(glowWormDevice -> glowWormDevice.getDeviceIP().equals(FireflyLuciferin.config.getSerialPort()))
                                .findAny().orElse(null);
                        if (glowWormDeviceSerial != null && glowWormDeviceSerial.getMac() != null) {
                            glowWormDeviceToUse = SettingsController.deviceTableData.stream()
                                    .filter(glowWormDevice -> glowWormDevice.getMac().equals(glowWormDeviceSerial.getMac()))
                                    .filter(glowWormDevice -> !glowWormDevice.getDeviceName().equals(Constants.USB_DEVICE))
                                    .findAny().orElse(null);
                        }
                    }
                    if (glowWormDeviceToUse != null) {
                        FireflyLuciferin.RUNNING = true;
                        if (trayIcon != null) {
                            setTrayIconImage(Constants.PlayerStatus.PLAY);
                        }
                        try {
                            StateDto stateDto = new StateDto();
                            stateDto.setState(Constants.ON);
                            stateDto.setBrightness(null);
                            stateDto.setMAC(glowWormDeviceToUse.getMac());
                            if ((FireflyLuciferin.config.isMqttEnable() && FireflyLuciferin.config.isMqttStream())) {
                                // If multi display change stream topic
                                if (retryNumber.getAndIncrement() < 5 && FireflyLuciferin.config.getMultiMonitor() > 1) {
                                    MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_UNSUBSCRIBE),
                                            JsonUtility.writeValueAsString(new UnsubscribeInstanceDto(String.valueOf(JavaFXStarter.whoAmI), FireflyLuciferin.config.getSerialPort())));
                                    TimeUnit.SECONDS.sleep(1);
                                } else {
                                    retryNumber.set(0);
                                    stateDto.setEffect(Constants.STATE_ON_GLOWWORMWIFI);
                                    MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), JsonUtility.writeValueAsString(stateDto));
                                }
                            } else {
                                stateDto.setEffect(Constants.STATE_ON_GLOWWORM);
                                MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), JsonUtility.writeValueAsString(stateDto));
                            }
                        } catch (InterruptedException e) {
                            log.error(e.getMessage());
                        }
                        if (FireflyLuciferin.FPS_GW_CONSUMER > 0 || !FireflyLuciferin.RUNNING) {
                            scheduledExecutorService.shutdown();
                        }
                    } else {
                        log.debug("Waiting the device for my instance #" + JavaFXStarter.whoAmI + "...");
                    }
                };
                scheduledExecutorService.scheduleAtFixedRate(framerateTask, 1, 1, TimeUnit.SECONDS);
            }
        }

    }

    /**
     * Set and return tray icon image
     * @param playerStatus status
     * @return tray icon
     */
    Image setTrayIconImage(Constants.PlayerStatus playerStatus) {

        Image img = null;
        switch (playerStatus) {
            case PLAY:
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
                break;
            case PLAY_WAITING:
                switch (JavaFXStarter.whoAmI) {
                    case 1:
                        if ((FireflyLuciferin.config.getMultiMonitor() == 1)) {
                            img = imagePlayWaiting;
                        } else {
                            img = imagePlayWaitingRight;
                        }
                        break;
                    case 2:
                        if ((FireflyLuciferin.config.getMultiMonitor() == 2)) {
                            img = imagePlayWaitingLeft;
                        } else {
                            img = imagePlayWaitingCenter;
                        }
                        break;
                    case 3: img = imagePlayWaitingLeft; break;
                }
                break;
            case STOP:
                switch (JavaFXStarter.whoAmI) {
                    case 1:
                        if ((FireflyLuciferin.config.getMultiMonitor() == 1)) {
                            img = imageStop;
                        } else {
                            img = imageStopRight;
                        }
                        break;
                    case 2:
                        if ((FireflyLuciferin.config.getMultiMonitor() == 2)) {
                            img = imageStopLeft;
                        } else {
                            img = imageStopCenter;
                        }
                        break;
                    case 3: img = imageStopLeft; break;
                }
                break;
            case GREY:
                switch (JavaFXStarter.whoAmI) {
                    case 1:
                        if ((FireflyLuciferin.config.getMultiMonitor() == 1)) {
                            img = imageGreyStop;
                        } else {
                            img = imageGreyStopRight;
                        }
                        break;
                    case 2:
                        if ((FireflyLuciferin.config.getMultiMonitor() == 2)) {
                            img = imageGreyStopLeft;
                        } else {
                            img = imageGreyStopCenter;
                        }
                        break;
                    case 3: img = imageGreyStopLeft; break;
                }
                break;
        }
        if (trayIcon != null) {
            trayIcon.setImage(img);
        }
        return img;

    }

    /**
     * Surf to the project GitHub page
     */
    public void surfToGitHub() {

        if(Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                URI github = new URI(Constants.GITHUB_URL);
                desktop.browse(github);
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }

    }

}