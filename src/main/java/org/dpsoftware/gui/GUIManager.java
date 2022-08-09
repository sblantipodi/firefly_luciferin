/*
  GUIManager.java

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

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.controllers.ColorCorrectionDialogController;
import org.dpsoftware.gui.controllers.EyeCareDialogController;
import org.dpsoftware.gui.controllers.SettingsController;
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
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;


/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
@Slf4j
@NoArgsConstructor
public class GUIManager extends JFrame {

    private Stage stage;

    // Label and framerate dialog
    @Getter JEditorPane jep = new JEditorPane();
    @Getter JFrame jFrame = new JFrame(Constants.FIREFLY_LUCIFERIN);
    public PipelineManager pipelineManager;
    public TrayIconManager trayIconManager;

    /**
     * Constructor
     * @param stage JavaFX stage
     * @throws HeadlessException GUI exception
     */
    public GUIManager(Stage stage) throws HeadlessException, UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        this.stage = stage;
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        pipelineManager = new PipelineManager();
        trayIconManager = new TrayIconManager();
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
     * Show notification. This uses the OS notification system via AWT tray icon.
     * @param title     dialog title
     * @param content   dialog msg
     * @param notificationType notification type
     */
    public void showNotification(String title, String content, TrayIcon.MessageType notificationType) {
        FireflyLuciferin.guiManager.trayIconManager.getTrayIcon().displayMessage(title, content, notificationType);
    }

    /**
     * Show localized notification. This uses the OS notification system via AWT tray icon.
     * @param title     dialog title
     * @param content   dialog msg
     * @param notificationType notification type
     */
    public void showLocalizedNotification(String title, String content, TrayIcon.MessageType notificationType) {
        FireflyLuciferin.guiManager.trayIconManager.getTrayIcon().displayMessage(CommonUtility.getWord(title),
                CommonUtility.getWord(content), notificationType);
    }

    /**
     * Set alert theme
     * @param alert in use
     */
    private void setAlertTheme(Alert alert) {
        setStylesheet(alert.getDialogPane().getStylesheets(), null);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
    }

    /**
     * Set style sheets
     * main.css is injected via fxml
     * @param stylesheets list containing style sheet file name
     * @param scene where to apply the style
     */
    private void setStylesheet(ObservableList<String> stylesheets, Scene scene) {
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
        if (NativeExecutor.isLinux() && scene != null) {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(Constants.CSS_LINUX)).toExternalForm());
        }
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
     * Show color correction dialog
     * @param settingsController we need to manually inject dialog controller in the main controller
     * @param event input event
     */
    public void showColorCorrectionDialog(SettingsController settingsController, InputEvent event) {
        Platform.runLater(() -> {
            Scene scene;
            try {
                TestCanvas testCanvas = new TestCanvas();
                testCanvas.buildAndShowTestImage(event);
                FXMLLoader fxmlLoader = new FXMLLoader(GUIManager.class.getResource(Constants.FXML_COLOR_CORRECTION_DIALOG + Constants.FXML), FireflyLuciferin.bundle);
                Parent root = fxmlLoader.load();
                ColorCorrectionDialogController controller = fxmlLoader.getController();
                controller.injectSettingsController(settingsController);
                controller.injectTestCanvas(testCanvas);
                controller.initValuesFromSettingsFile(FireflyLuciferin.config);
                scene = new Scene(root);
                setStylesheet(scene.getStylesheets(), scene);
                scene.setFill(Color.TRANSPARENT);
                Stage stage = new Stage();
                stage.initStyle(StageStyle.UNDECORATED);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(scene);
                Platform.runLater(() -> TestCanvas.setDialogMargin(stage));
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setAlwaysOnTop(true);
                stage.showAndWait();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Show eye care dialog
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showEyeCareDialog(SettingsController settingsController) {
        Platform.runLater(() -> {
            Scene scene;
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GUIManager.class.getResource(Constants.FXML_EYE_CARE_DIALOG + Constants.FXML), FireflyLuciferin.bundle);
                Parent root = fxmlLoader.load();
                EyeCareDialogController controller = fxmlLoader.getController();
                controller.injectSettingsController(settingsController);
                controller.initValuesFromSettingsFile(FireflyLuciferin.config);
                scene = new Scene(root);
                setStylesheet(scene.getStylesheets(), scene);
                scene.setFill(Color.TRANSPARENT);
                Stage stage = new Stage();
                stage.initStyle(StageStyle.UNDECORATED);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(scene);
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setAlwaysOnTop(true);
                Platform.runLater(() -> {
                    Stage parentStage = this.stage;
                    stage.setX(parentStage.getX() + (parentStage.getWidth() / 2) - (stage.getWidth() / 2));
                    stage.setY(parentStage.getY() + (parentStage.getHeight() / 2) - (stage.getHeight() / 2));
                });
                stage.showAndWait();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
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
                setStylesheet(scene.getStylesheets(), scene);
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
     * @param publishToTopic send info to the microcontroller via MQTT or via HTTP GET
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
            if (trayIconManager.trayIcon != null) {
                TrayIconManager.popupMenu.remove(0);
                TrayIconManager.popupMenu.add(trayIconManager.createMenuItem(CommonUtility.getWord(Constants.STOP)), 0);
                if (!FireflyLuciferin.RUNNING) {
                    trayIconManager.setTrayIconImage(Constants.PlayerStatus.PLAY_WAITING);
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
     * Open web browser on the specific URL
     * @param url address to surf on
     */
    public void surfToURL(String url) {
        try {
            FireflyLuciferin.hostServices.showDocument(url);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    /**
     *  Show settings dialog if using Linux and check for upgrade
     */
    public void showSettingsAndCheckForUpgrade() {
        if (!NativeExecutor.isWindows() && !NativeExecutor.isMac()) {
            showSettingsDialog();
        }
        UpgradeManager upgradeManager = new UpgradeManager();
        upgradeManager.checkForUpdates(stage);
    }

}