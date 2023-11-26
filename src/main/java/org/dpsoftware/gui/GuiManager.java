/*
  GuiManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;
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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.controllers.ColorCorrectionDialogController;
import org.dpsoftware.gui.controllers.EyeCareDialogController;
import org.dpsoftware.gui.controllers.SatellitesDialogController;
import org.dpsoftware.gui.controllers.SettingsController;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.UpgradeManager;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.managers.dto.StateStatusDto;
import org.dpsoftware.network.NetworkSingleton;
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
public class GuiManager {

    public PipelineManager pipelineManager;
    public TrayIconManager trayIconManager;
    // Label and framerate dialog
    @Getter
    JEditorPane jep = new JEditorPane();
    private Stage stage;
    private Scene mainScene;
    private double xOffset = 0;
    private double yOffset = 0;

    /**
     * Constructor
     *
     * @param stage JavaFX stage
     * @throws HeadlessException GUI exception
     */
    public GuiManager(Stage stage) throws HeadlessException, UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        this.stage = stage;
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        pipelineManager = new PipelineManager();
        trayIconManager = new TrayIconManager();
    }

    /**
     * Load FXML files
     *
     * @param fxml GUI file
     * @return fxmlloader
     * @throws IOException file exception
     */
    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(fxml + Constants.FXML), MainSingleton.getInstance().bundle);
        return fxmlLoader.load();
    }

    /**
     * Set icon for every stage
     *
     * @param stage in use
     */
    public static void setStageIcon(Stage stage) {
        stage.getIcons().add(new javafx.scene.image.Image(String.valueOf(GuiManager.class.getResource(Constants.IMAGE_TRAY_STOP))));
    }

    /**
     * Create window title
     *
     * @return title
     */
    public static String createWindowTitle() {
        String title = "  " + Constants.FIREFLY_LUCIFERIN;
        switch (MainSingleton.getInstance().whoAmI) {
            case 1 -> {
                if ((MainSingleton.getInstance().config.getMultiMonitor() != 1)) {
                    title += " (" + CommonUtility.getWord(Constants.RIGHT_DISPLAY) + ")";
                }
            }
            case 2 -> {
                if ((MainSingleton.getInstance().config.getMultiMonitor() == 2)) {
                    title += " (" + CommonUtility.getWord(Constants.LEFT_DISPLAY) + ")";
                } else {
                    title += " (" + CommonUtility.getWord(Constants.CENTER_DISPLAY) + ")";
                }
            }
            case 3 -> title += " (" + CommonUtility.getWord(Constants.LEFT_DISPLAY) + ")";
        }
        return title;
    }

    /**
     * Show alert in a JavaFX dialog
     *
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
     *
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
     *
     * @param title            dialog title
     * @param content          dialog msg
     * @param notificationType notification type
     */
    public void showNotification(String title, String content, TrayIcon.MessageType notificationType) {
        MainSingleton.getInstance().guiManager.trayIconManager.getTrayIcon().displayMessage(title, content, notificationType);
    }

    /**
     * Show localized notification. This uses the OS notification system via AWT tray icon.
     *
     * @param title            dialog title
     * @param content          dialog msg
     * @param notificationType notification type
     */
    public void showLocalizedNotification(String title, String content, TrayIcon.MessageType notificationType) {
        MainSingleton.getInstance().guiManager.trayIconManager.getTrayIcon().displayMessage(CommonUtility.getWord(title),
                CommonUtility.getWord(content), notificationType);
    }

    /**
     * Set alert theme
     *
     * @param alert in use
     */
    private void setAlertTheme(Alert alert) {
        setStylesheet(alert.getDialogPane().getStylesheets(), null);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
    }

    /**
     * Set style sheets
     * main.css is injected via fxml
     *
     * @param stylesheets list containing style sheet file name
     * @param scene       where to apply the style
     */
    private void setStylesheet(ObservableList<String> stylesheets, Scene scene) {
        var theme = LocalizedEnum.fromBaseStr(Enums.Theme.class, MainSingleton.getInstance().config.getTheme());
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
     *
     * @param title     dialog title
     * @param header    dialog header
     * @param webUrl    URL to load inside the web view
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
     *
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
     *
     * @param preloadFxml if true, it preload the fxml without showing it
     */
    public void showSettingsDialog(boolean preloadFxml) {
        String fxml;
        fxml = Constants.FXML_SETTINGS;
        showStage(fxml, preloadFxml);
    }

    /**
     * Show a dialog with a framerate counter
     */
    public void showFramerateDialog() {
        showStage(Constants.FXML_INFO, false);
    }

    /**
     * Show color correction dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     * @param event              input event
     */
    public void showColorCorrectionDialog(SettingsController settingsController, InputEvent event) {
        Platform.runLater(() -> {
            try {
                TestCanvas testCanvas = new TestCanvas();
                testCanvas.buildAndShowTestImage(event);
                FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(Constants.FXML_COLOR_CORRECTION_DIALOG + Constants.FXML), MainSingleton.getInstance().bundle);
                Parent root = fxmlLoader.load();
                ColorCorrectionDialogController controller = fxmlLoader.getController();
                controller.injectSettingsController(settingsController);
                controller.injectTestCanvas(testCanvas);
                controller.initValuesFromSettingsFile(MainSingleton.getInstance().config);
                Stage stage = initStage(root);
                Platform.runLater(() -> new TestCanvas().setDialogMargin(stage));
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setAlwaysOnTop(true);
                stage.showAndWait();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Initialize stage
     * @param root parent root
     * @return initialized stage
     */
    private Stage initStage(Parent root) {
        Scene scene;
        scene = new Scene(root);
        setStylesheet(scene.getStylesheets(), scene);
        scene.setFill(Color.TRANSPARENT);
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        return stage;
    }

    /**
     * Show a secondary stage dialog
     *
     * @param settingsController controller
     * @param fxmlLoader         fxml loader
     * @throws IOException error
     */
    private void showSecondaryStage(Class<?> classForCast, SettingsController settingsController, FXMLLoader fxmlLoader) throws IOException {
        Parent root = fxmlLoader.load();
        Object controller;
        controller = fxmlLoader.getController();
        if (classForCast == EyeCareDialogController.class) {
            ((EyeCareDialogController) controller).injectSettingsController(settingsController);
            ((EyeCareDialogController) controller).initValuesFromSettingsFile(MainSingleton.getInstance().config);
        } else if (classForCast == SatellitesDialogController.class) {
            ((SatellitesDialogController) controller).injectSettingsController(settingsController);
            ((SatellitesDialogController) controller).setTooltips();
        }
        Stage stage = initStage(root);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        Platform.runLater(() -> {
            Stage parentStage = this.stage;
            stage.setX(parentStage.getX() + (parentStage.getWidth() / 2) - (stage.getWidth() / 2));
            stage.setY(parentStage.getY() + (parentStage.getHeight() / 2) - (stage.getHeight() / 2));
        });
        stage.showAndWait();
    }

    /**
     * Show satellites dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showSatellitesDialog(SettingsController settingsController) {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(Constants.FXML_SATELLITES_DIALOG + Constants.FXML), MainSingleton.getInstance().bundle);
                showSecondaryStage(SatellitesDialogController.class, settingsController, fxmlLoader);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Show eye care dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showEyeCareDialog(SettingsController settingsController) {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(Constants.FXML_EYE_CARE_DIALOG + Constants.FXML), MainSingleton.getInstance().bundle);
                showSecondaryStage(EyeCareDialogController.class, settingsController, fxmlLoader);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Show a stage
     *
     * @param stageName   stage to show
     * @param preloadFxml if true, it preload the fxml without showing it
     */
    void showStage(String stageName, boolean preloadFxml) {
        Platform.runLater(() -> {
            try {
                boolean isDefaultTheme = LocalizedEnum.fromBaseStr(Enums.Theme.class, MainSingleton.getInstance().config.getTheme()).equals(Enums.Theme.DEFAULT);
                boolean isMainStage = stageName.equals(Constants.FXML_SETTINGS) || stageName.equals(Constants.FXML_SETTINGS_CUSTOM_BAR);
                if (NativeExecutor.isLinux() && stageName.equals(Constants.FXML_INFO)) {
                    stage = new Stage();
                    if (!(NativeExecutor.isWindows() && !isDefaultTheme)) {
                        stage.initStyle(StageStyle.DECORATED);
                    }
                }
                if (stage == null) {
                    stage = new Stage();
                }
                stage.resizableProperty().setValue(Boolean.FALSE);
                setScene(stageName, isMainStage, isDefaultTheme);
                String title = createWindowTitle();
                stage.setTitle(title);
                setStageIcon(stage);
                if (isMainStage && NativeExecutor.isLinux()) {
                    stage.setIconified(true);
                }
                if (NativeExecutor.isWindows() && !isDefaultTheme) {
                    manageNativeWindow(stage.getScene(), title, preloadFxml);
                } else {
                    showWithPreload(preloadFxml);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Setting scene into main stage, main scene is preloaded and stored in memory
     *
     * @param stageName      stage name to load
     * @param isMainStage    true if settings.fxml is passed as parameter
     * @param isDefaultTheme true if using classic theme
     * @throws IOException error
     */
    private void setScene(String stageName, boolean isMainStage, boolean isDefaultTheme) throws IOException {
        if (isMainStage && mainScene != null) {
            stage.getScene().setRoot(mainScene.getRoot());
        } else {
            log.debug("Loading FXML");
            Parent root;
            if (NativeExecutor.isWindows() && !isDefaultTheme) {
                if (stageName.equals(Constants.FXML_SETTINGS)) {
                    root = loadFXML(Constants.FXML_SETTINGS_CUSTOM_BAR);
                    root.setStyle(Constants.FXML_TRANSPARENT);
                } else if (stageName.equals(Constants.FXML_INFO)) {
                    root = loadFXML(Constants.FXML_INFO_CUSTOM_BAR);
                    root.setStyle(Constants.FXML_TRANSPARENT);
                } else {
                    root = loadFXML(stageName);
                }
                manageWindowDragging(root);
            } else {
                root = loadFXML(stageName);
            }
            Scene scene = new Scene(root);
            setStylesheet(scene.getStylesheets(), scene);
            stage.setScene(scene);
            if (isMainStage) {
                mainScene = scene;
            } else {
                mainScene = null;
            }
            log.debug("FXML loaded");
        }
    }

    /**
     * Add Windows animations (minimize/maximize) for the undecorated window using JNA
     *
     * @param scene       in use
     * @param finalTitle  window title to target
     * @param preloadFxml if true, it preload the fxml without showing it
     */
    private void manageNativeWindow(Scene scene, String finalTitle, boolean preloadFxml) {
        if (!stage.isShowing() && !stage.getStyle().name().equals(Constants.TRANSPARENT)) {
            stage.initStyle(StageStyle.TRANSPARENT);
        }
        scene.setFill(Color.TRANSPARENT);
        showWithPreload(preloadFxml);
        var user32 = User32.INSTANCE;
        var hWnd = user32.FindWindow(null, finalTitle);
        var oldStyle = user32.GetWindowLong(hWnd, WinUser.GWL_STYLE);
        stage.iconifiedProperty().addListener((ov, t, t1) -> {
            if (t1) {
                int newStyle = oldStyle | 0x00020000 | 0x00C00000;
                user32.SetWindowLong(hWnd, WinUser.GWL_STYLE, newStyle);
            } else {
                user32.SetWindowLong(hWnd, WinUser.GWL_STYLE, oldStyle);
            }
        });
    }

    /**
     * Show a stage considering the main stage has been preloaded
     *
     * @param preloadFxml true if the main stage has been preloaded
     */
    private void showWithPreload(boolean preloadFxml) {
        if (preloadFxml) {
            log.debug("Preloading stage");
            stage.setOpacity(0);
            stage.show();
            stage.close();
            stage.setOpacity(1);
        } else {
            stage.show();
        }
    }

    /**
     * Manage window dragging
     *
     * @param root parent
     */
    private void manageWindowDragging(Parent root) {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            if (yOffset < Constants.TITLE_BAR_HEIGHT) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    /**
     * Stop capturing threads
     *
     * @param publishToTopic send info to the microcontroller via MQTT or via HTTP GET
     */
    public void stopCapturingThreads(boolean publishToTopic) {
        if (((ManagerSingleton.getInstance().client != null) || MainSingleton.getInstance().config.isFullFirmware()) && publishToTopic) {
            StateDto stateDto = new StateDto();
            stateDto.setEffect(Constants.SOLID);
            stateDto.setState(MainSingleton.getInstance().config.isToggleLed() ? Constants.ON : Constants.OFF);
            ColorDto colorDto = new ColorDto();
            String[] color = MainSingleton.getInstance().config.getColorChooser().split(",");
            colorDto.setR(Integer.parseInt(color[0]));
            colorDto.setG(Integer.parseInt(color[1]));
            colorDto.setB(Integer.parseInt(color[2]));
            stateDto.setColor(colorDto);
            stateDto.setBrightness(CommonUtility.getNightBrightness());
            stateDto.setWhitetemp(MainSingleton.getInstance().config.getWhiteTemperature());
            if (CommonUtility.getDeviceToUse() != null) {
                stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
            }
            stateDto.setStartStopInstances(Enums.PlayerStatus.STOP.name());
            CommonUtility.sleepMilliseconds(300);
            NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT), CommonUtility.toJsonString(stateDto));
        }
        if (!MainSingleton.getInstance().exitTriggered) {
            pipelineManager.stopCapturePipeline();
        }
        if (CommonUtility.isSingleDeviceOtherInstance()) {
            StateStatusDto stateStatusDto = new StateStatusDto();
            stateStatusDto.setAction(Constants.CLIENT_ACTION);
            stateStatusDto.setRunning(false);
            NetworkSingleton.getInstance().msgClient.sendMessage(CommonUtility.toJsonString(stateStatusDto));
        }
    }

    /**
     * Start capturing threads
     */
    public void startCapturingThreads() {
        if (!MainSingleton.getInstance().communicationError) {
            if (trayIconManager.trayIcon != null) {
                GuiSingleton.getInstance().popupMenu.remove(0);
                GuiSingleton.getInstance().popupMenu.add(trayIconManager.createMenuItem(CommonUtility.getWord(Constants.STOP)), 0);
                if (!MainSingleton.getInstance().RUNNING) {
                    trayIconManager.setTrayIconImage(Enums.PlayerStatus.PLAY_WAITING);
                }
            }
            if (!ManagerSingleton.getInstance().pipelineStarting) {
                pipelineManager.startCapturePipeline();
            }
            if (CommonUtility.isSingleDeviceOtherInstance()) {
                StateStatusDto stateStatusDto = new StateStatusDto();
                stateStatusDto.setAction(Constants.CLIENT_ACTION);
                stateStatusDto.setRunning(true);
                NetworkSingleton.getInstance().msgClient.sendMessage(CommonUtility.toJsonString(stateStatusDto));
            }
        }
    }

    /**
     * Open web browser on the specific URL
     *
     * @param url address to surf on
     */
    public void surfToURL(String url) {
        try {
            MainSingleton.getInstance().hostServices.showDocument(url);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    /**
     * Show settings dialog if using Linux and check for upgrade
     */
    public void showSettingsAndCheckForUpgrade() {
        if (!NativeExecutor.isWindows() && !NativeExecutor.isMac()) {
            showSettingsDialog(false);
        }
        UpgradeManager upgradeManager = new UpgradeManager();
        upgradeManager.checkForUpdates(stage);
    }

}