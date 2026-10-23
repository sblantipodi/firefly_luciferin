/*
  GuiManager.java

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
package org.dpsoftware.gui;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.input.InputEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.GrabberSingleton;
import org.dpsoftware.grabber.SimdBenchmark;
import org.dpsoftware.gui.controllers.*;
import org.dpsoftware.gui.trayicon.TrayIconAppIndicator;
import org.dpsoftware.gui.trayicon.TrayIconAwt;
import org.dpsoftware.gui.trayicon.TrayIconManager;
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
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * GUI Manager for tray icon menu and framerate counter-dialog
 */
@Slf4j
@NoArgsConstructor
public class GuiManager {

    public PipelineManager pipelineManager;
    public TrayIconManager trayIconManager;
    private DialogManager dialogManager;
    public Stage stage;
    private Stage stageInfo;
    private Scene mainScene;
    private Scene mainSceneInfo;
    private double xOffset = 0;
    private double xOffsetInfo = 0;
    private double yOffset = 0;
    private double yOffsetInfo = 0;
    private final AtomicBoolean upgradeCheckInProgress = new AtomicBoolean();

    /**
     * Constructor
     *
     * @param initTray true if traybar needs to be added, false at first startup
     * @throws HeadlessException GUI exception
     */
    public GuiManager(boolean initTray) throws HeadlessException, UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        this.stage = new Stage();
        this.stageInfo = new Stage();
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        pipelineManager = new PipelineManager();
        if (initTray) {
            // Windows uses AWT tray, Linux uses libappindicator3 and libayatana-appindicator3, see LibAppIndicator.java for more infos
            if (NativeExecutor.isWindows() || MainSingleton.getInstance().config.getTrayPreference().equals(Enums.TRAY_PREFERENCE.FORCE_AWT)) {
                trayIconManager = new TrayIconAwt();
            } else {
                trayIconManager = new TrayIconAppIndicator();
            }
        }
        // TODO Set this property to prevent a JavaFX exception when instantiating a WebView.
        // System.setProperty("javafx.sg.warn", "true");
        dialogManager = new DialogManager(this);
    }

    private DialogManager dialogManager() {
        if (dialogManager == null) {
            dialogManager = new DialogManager(this);
        }
        return dialogManager;
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
        Parent root = fxmlLoader.load();
        if (fxmlLoader.getController() instanceof SettingsController controller) {
            root.getProperties().put(SettingsController.class, controller);
        }
        return root;
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
        if (MainSingleton.getInstance().config != null) {
            switch (MainSingleton.getInstance().whoAmI) {
                case 1 -> {
                    if ((MainSingleton.getInstance().config.getMultiMonitor() != 1)) {
                        title += " (" + CommonUtility.getWord(LabelKey.RIGHT_DISPLAY) + ")";
                    }
                }
                case 2 -> {
                    if ((MainSingleton.getInstance().config.getMultiMonitor() == 2)) {
                        title += " (" + CommonUtility.getWord(LabelKey.LEFT_DISPLAY) + ")";
                    } else {
                        title += " (" + CommonUtility.getWord(LabelKey.CENTER_DISPLAY) + ")";
                    }
                }
                case 3 -> title += " (" + CommonUtility.getWord(LabelKey.LEFT_DISPLAY) + ")";
            }
        }
        if (!CommonUtility.getWord(LabelKey.DEFAULT).equals(MainSingleton.getInstance().profileArg)
                && !LabelKey.DEFAULT.equals(MainSingleton.getInstance().profileArg)) {
            title += " [" + MainSingleton.getInstance().profileArg + "]";
        }
        return title;
    }

    /**
     * Set state dto
     *
     * @return state dto
     */
    private static StateDto getStateDto() {
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
        return stateDto;
    }

    /**
     * Show firmware type dialog
     *
     * @param configPresent show only if config is not preset.
     */
    private static void showFirmwareTypeDialog(boolean configPresent) {
        if (!configPresent) {
            ButtonType fullBtn = new ButtonType(CommonUtility.getWord(LabelKey.FULL_FIRM));
            ButtonType lightBtn = new ButtonType(CommonUtility.getWord(LabelKey.LIGHT_FIRM));
            Optional<ButtonType> result = MainSingleton.getInstance().guiManager.showLocalizedAlert(LabelKey.INITIAL_TITLE,
                    LabelKey.INITIAL_HEADER, LabelKey.INITIAL_CONTEXT, Alert.AlertType.CONFIRMATION, fullBtn, lightBtn);
            if (result.isPresent() && result.get().getText().equals(CommonUtility.getWord(LabelKey.FULL_FIRM))) {
                GuiSingleton.getInstance().setFirmTypeFull(true);
            }
            if (result.isPresent() && result.get().getText().equals(CommonUtility.getWord(LabelKey.LIGHT_FIRM))) {
                GuiSingleton.getInstance().setFirmTypeFull(false);
            }
        }
    }

    /**
     * Replace last occurrence
     *
     * @param input       string
     * @param target      to replace
     * @param replacement string
     * @return new string with target replace with replacement
     */
    public static String replaceLastOccurrence(String input, String target, String replacement) {
        int lastIndex = input.lastIndexOf(target);
        if (lastIndex == -1) {
            return input;
        }
        String prefix = input.substring(0, lastIndex);
        String suffix = input.substring(lastIndex + target.length());
        return prefix + replacement + suffix;
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
    public static String setImage(String imagePlay, String imagePlayRight, String imagePlayLeft, String imagePlayCenter) {
        String img = "";
        if (GuiSingleton.getInstance().isUpgrade()) {
            imagePlay = imagePlay.replace(Constants.IMG_PATH, Constants.IMG_PATH_UPDATE);
            imagePlayRight = imagePlayRight.replace(Constants.IMG_PATH, Constants.IMG_PATH_UPDATE);
            imagePlayLeft = imagePlayLeft.replace(Constants.IMG_PATH, Constants.IMG_PATH_UPDATE);
            imagePlayCenter = imagePlayCenter.replace(Constants.IMG_PATH, Constants.IMG_PATH_UPDATE);
            // Flatpak does not accept standard path for libappindicator image
            if (NativeExecutor.isFlatpak()) {
                final String TARGET = "update/";
                final String REPLACEMENT = "update_";
                imagePlay = replaceLastOccurrence(imagePlay, TARGET, REPLACEMENT);
                imagePlayRight = replaceLastOccurrence(imagePlayRight, TARGET, REPLACEMENT);
                imagePlayLeft = replaceLastOccurrence(imagePlayLeft, TARGET, REPLACEMENT);
                imagePlayCenter = replaceLastOccurrence(imagePlayCenter, TARGET, REPLACEMENT);
            }
        }
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

    /**
     * Useful logic to choose a tray icon
     *
     * @param playerStatus player status
     * @return image path
     */
    public static String computeImageToUse(Enums.PlayerStatus playerStatus) {
        String imagePlayRight = Constants.IMAGE_CONTROL_PLAY_RIGHT;
        String imagePlayWaitingRight = Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT;
        String imageStopRight = Constants.IMAGE_CONTROL_LOGO_RIGHT;
        String imageStopRightOff = Constants.IMAGE_CONTROL_LOGO_RIGHT_OFF;
        String imageGreyStopRight = Constants.IMAGE_CONTROL_GREY_RIGHT;
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            imagePlayRight = Constants.IMAGE_CONTROL_PLAY_RIGHT_GOLD;
            imagePlayWaitingRight = Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT_GOLD;
            imageStopRight = Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD;
            imageStopRightOff = Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD_OFF;
            imageGreyStopRight = Constants.IMAGE_CONTROL_GREY_RIGHT_GOLD;
        }
        return switch (playerStatus) {
            case PLAY ->
                    setImage(Constants.IMAGE_CONTROL_PLAY, imagePlayRight, Constants.IMAGE_CONTROL_PLAY_LEFT, Constants.IMAGE_CONTROL_PLAY_CENTER);
            case PLAY_WAITING ->
                    setImage(Constants.IMAGE_CONTROL_PLAY_WAITING, imagePlayWaitingRight, Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT, Constants.IMAGE_CONTROL_PLAY_WAITING_CENTER);
            case STOP ->
                    setImage(Constants.IMAGE_TRAY_STOP, imageStopRight, Constants.IMAGE_CONTROL_LOGO_LEFT, Constants.IMAGE_CONTROL_LOGO_CENTER);
            case GREY ->
                    setImage(Constants.IMAGE_CONTROL_GREY, imageGreyStopRight, Constants.IMAGE_CONTROL_GREY_LEFT, Constants.IMAGE_CONTROL_GREY_CENTER);
            case OFF ->
                    setImage(Constants.IMAGE_CONTROL_LOGO_OFF, imageStopRightOff, Constants.IMAGE_CONTROL_LOGO_LEFT_OFF, Constants.IMAGE_CONTROL_LOGO_CENTER_OFF);
        };
    }

    /**
     * Set tooltip properties width default delays
     *
     * @param text tooltip string
     * @param node node to set the tooltip
     * @return tooltip
     */
    public static Tooltip createTooltip(String text, Node node) {
        return createTooltip(text, Constants.TOOLTIP_DELAY, node);
    }

    /**
     * Set tooltip properties width delays
     *
     * @param text      tooltip string
     * @param showDelay delay used to show the tooltip
     * @param node      node to set the tooltip
     * @return tooltip
     */
    public static Tooltip createTooltip(String text, int showDelay, Node node) {
        Tooltip tooltip = new Tooltip(CommonUtility.getWord(text));
        tooltip.setShowDelay(Duration.millis(showDelay));
        tooltip.setMaxWidth(Constants.TOOLTIP_MAX_WIDTH);
        tooltip.setWrapText(true);
        tooltip.setAutoHide(false);
        setupTooltip(node, tooltip);
        return tooltip;
    }

    /**
     * Set tooltip properties
     *
     * @param node    node to set the tooltip
     * @param tooltip tooltip to set
     */
    public static void setupTooltip(Node node, Tooltip tooltip) {
        Tooltip.install(node, tooltip);
        node.setOnMouseEntered(event -> {
            if (!(node instanceof ComboBox<?> && ((ComboBox<?>) node).isEditable()) && !(node instanceof Spinner<?>)) {
                if (!tooltip.isActivated() && !tooltip.isShowing()) {
                    tooltip.show(node, event.getScreenX(), event.getScreenY());
                }
            }
        });
        node.setOnMouseExited(_ -> tooltip.hide());
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
        return dialogManager().showAlert(title, header, content, alertType);
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
        return dialogManager().showLocalizedAlert(title, header, content, alertType);
    }


    /**
     * Show notification. This uses the OS notification system via AWT tray icon on Windows,
     * LibNotify on Linux with a fallback to a user dialog.
     *
     * @param highlight        dialog title
     * @param content          dialog msg
     * @param title            optional
     * @param notificationType notification type
     */
    public void showNotification(String highlight, String content, String title, TrayIcon.MessageType notificationType) {
        dialogManager().showNotification(highlight, content, title, notificationType);
    }

    /**
     * Show localized notification. This uses the OS notification system via AWT tray icon.
     *
     * @param highlight        dialog title
     * @param content          dialog msg
     * @param title            optional
     * @param notificationType notification type
     */
    public void showLocalizedNotification(String highlight, String content, String title, TrayIcon.MessageType notificationType) {
        dialogManager().showLocalizedNotification(highlight, content, title, notificationType);
    }

    /**
     * Set dialog theme
     *
     * @param dialog in use
     */
    public void setDialogTheme(Dialog<String> dialog) {
        dialogManager().setDialogTheme(dialog);
    }

    /**
     * Set style sheets
     * main.css is injected via fxml
     *
     * @param stylesheets list containing style sheet file name
     * @param scene       where to apply the style
     */
    public void setStylesheet(ObservableList<String> stylesheets, Scene scene) {
        Enums.Theme theme;
        if (MainSingleton.getInstance().config != null && MainSingleton.getInstance().config.getTheme() != null) {
            theme = LocalizedEnum.fromBaseStr(Enums.Theme.class, MainSingleton.getInstance().config.getTheme());
        } else {
            theme = NativeExecutor.isDarkTheme() ? Enums.Theme.DARK_THEME_ORANGE : Enums.Theme.CLASSIC;
        }
        if (theme.name().contains(Constants.CSS_DARK) || theme.name().contains(Constants.CSS_LIGHT)) {
            stylesheets.add(Objects.requireNonNull(getClass().getResource(Constants.BASE_CSS)).toExternalForm());
            stylesheets.add(Objects.requireNonNull(getClass().getResource(theme.getCssPath())).toExternalForm());
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
        return dialogManager().showWebAlert(title, header, webUrl, alertType);
    }


    /**
     * Refresh a preloaded settings window after automatic capture selection.
     */
    public void refreshAutomaticCaptureMethod() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::refreshAutomaticCaptureMethod);
            return;
        }
        if (mainScene != null && mainScene.getRoot().getProperties().get(SettingsController.class)
                instanceof SettingsController controller) {
            controller.refreshAutomaticCaptureMethod();
        }
    }

    /**
     * Show a dialog with all the settings
     *
     * @param preloadFxml if true, it preload the fxml without showing it
     */
    public void showSettingsDialog(boolean preloadFxml) {
        if (!MainSingleton.getInstance().isHeadlessMode()) {
            showStage(Constants.FXML_SETTINGS, preloadFxml, true);
        }
    }

    /**
     * Show a dialog with a framerate counter
     */
    public void showFramerateDialog() {
        showStage(Constants.FXML_INFO, false, true);
    }

    /**
     * Show color correction dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     * @param event              input event
     */
    public void showColorCorrectionDialog(SettingsController settingsController, InputEvent event) {
        dialogManager().showColorCorrectionDialog(settingsController, event);
    }


    /**
     * Show satellites dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showSatellitesDialog(SettingsController settingsController) {
        dialogManager().showSatellitesDialog(settingsController);
    }

    /**
     * Show improv dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showImprovDialog(SettingsController settingsController) {
        dialogManager().showImprovDialog(settingsController);
    }

    /**
     * Show eye care dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showEyeCareDialog(SettingsController settingsController) {
        dialogManager().showEyeCareDialog(settingsController);
    }

    /**
     * Show profile dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showProfileDialog(SettingsController settingsController) {
        dialogManager().showProfileDialog(settingsController);
    }

    /**
     * Show smoothing dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showSmoothingDialog(SettingsController settingsController) {
        dialogManager().showSmoothingDialog(settingsController);
    }

    /**
     * Show gamma dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showGammaDialog(SettingsController settingsController) {
        dialogManager().showGammaDialog(settingsController);
    }

    /**
     * Show display dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showDisplayDialog(SettingsController settingsController) {
        dialogManager().showDisplayDialog(settingsController);
    }

    /**
     * Show a stage
     *
     * @param stageName     stage to show
     * @param preloadFxml   if true, it preload the fxml without showing it
     * @param configPresent true if config file is present
     */
    public void showStage(String stageName, boolean preloadFxml, boolean configPresent) {
        if (configPresent) {
            Platform.runLater(() -> showAndMakeVisible(stageName, preloadFxml, true));
        } else {
            showAndMakeVisible(stageName, preloadFxml, false);
        }
    }

    /**
     * Show alert in a JavaFX dialog
     *
     * @param title     dialog title
     * @param header    dialog header
     * @param content   dialog msg
     * @param alertType alert type
     * @param buttons   buttons to use
     * @return an Object when we can listen for commands
     */
    public Optional<ButtonType> showLocalizedAlert(String title, String header, String content, Alert.AlertType alertType, ButtonType... buttons) {
        return dialogManager().showLocalizedAlert(title, header, content, alertType, buttons);
    }

    /**
     * @param stageName     stage to show
     * @param preloadFxml   if true, it preload the fxml without showing it
     * @param configPresent true if config file is present
     */
    private void showAndMakeVisible(String stageName, boolean preloadFxml, boolean configPresent) {
        try {
            showFirmwareTypeDialog(configPresent);
            boolean isClassicTheme;
            if (MainSingleton.getInstance().config != null) {
                isClassicTheme = LocalizedEnum.fromBaseStr(Enums.Theme.class, MainSingleton.getInstance().config.getTheme()).equals(Enums.Theme.CLASSIC);
            } else {
                isClassicTheme = !NativeExecutor.isDarkTheme();
            }
            boolean isMainStage = stageName.equals(Constants.FXML_SETTINGS) || stageName.equals(Constants.FXML_SETTINGS_CUSTOM_BAR);
            if (!NativeExecutor.isSystemTraySupported() && stageName.equals(Constants.FXML_INFO)) {
                setStage(new Stage(), stageName);
                if (!(NativeExecutor.isWindows() && !isClassicTheme)) {
                    getStage(stageName).initStyle(StageStyle.DECORATED);
                }
            }
            if (getStage(stageName) == null) {
                setStage(new Stage(), stageName);
            }
            getStage(stageName).resizableProperty().setValue(Boolean.FALSE);
            setScene(stageName, isMainStage, isClassicTheme);
            if (isMainStage) {
                refreshAutomaticCaptureMethod();
            }
            String title = createWindowTitle();
            getStage(stageName).setTitle(title);
            setStageIcon(getStage(stageName));
            if (NativeExecutor.isWindows() && !isClassicTheme) {
                manageNativeWindow(getStage(stageName).getScene(), title, preloadFxml, configPresent, stageName);
            } else {
                showWithPreload(preloadFxml, configPresent, stageName);
            }
            if (isMainStage && configPresent && !NativeExecutor.isHyprland() && !NativeExecutor.isSystemTraySupported()) {
                getStage(stageName).setIconified(true);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Setting scene into main stage, main scene is preloaded and stored in memory
     *
     * @param stageName      stage name to load
     * @param isMainStage    true if settings.fxml is passed as parameter
     * @param isClassicTheme true if using classic theme
     * @throws IOException error
     */
    public void setScene(String stageName, boolean isMainStage, boolean isClassicTheme) throws IOException {
        setScene(getStage(stageName), stageName, isMainStage, isClassicTheme);
    }

    /**
     * Setting scene into main stage, main scene is preloaded and stored in memory
     *
     * @param stage          to use for the show
     * @param stageName      stage name
     * @param isMainStage    true if settings stage
     * @param isClassicTheme true if classic theme is in use
     * @throws IOException can't load FXML
     */
    public void setScene(Stage stage, String stageName, boolean isMainStage, boolean isClassicTheme) throws IOException {
        if (isMainStage && getMainScene(stageName) != null) {
            stage.getScene().setRoot(getMainScene(stageName).getRoot());
        } else {
            log.debug("Loading FXML");
            Parent root;
            if (NativeExecutor.isWindows() && !isClassicTheme) {
                if (stageName.equals(Constants.FXML_SETTINGS)) {
                    root = loadFXML(Constants.FXML_SETTINGS_CUSTOM_BAR);
                    root.setStyle(Constants.FXML_TRANSPARENT);
                } else if (stageName.equals(Constants.FXML_INFO)) {
                    root = loadFXML(Constants.FXML_INFO_CUSTOM_BAR);
                    root.setStyle(Constants.FXML_TRANSPARENT);
                } else {
                    root = loadFXML(stageName);
                }
                manageWindowDragging(root, stageName);
            } else {
                root = loadFXML(stageName);
            }
            Scene scene = new Scene(root);
            setStylesheet(scene.getStylesheets(), scene);
            stage.setScene(scene);
            if (isMainStage) {
                setMainScene(scene, stageName);
            } else {
                setMainScene(null, stageName);
            }
            log.debug("FXML loaded");
        }
    }

    /**
     * Add Windows animations (minimize/maximize) for the undecorated window using JNA
     *
     * @param scene         in use
     * @param finalTitle    window title to target
     * @param preloadFxml   if true, it preload the fxml without showing it
     * @param configPresent true if config file is present
     */
    private void manageNativeWindow(Scene scene, String finalTitle, boolean preloadFxml, boolean configPresent, String stageName) {
        if (!getStage(stageName).isShowing() && !getStage(stageName).getStyle().name().equals(Constants.TRANSPARENT)) {
            getStage(stageName).initStyle(StageStyle.TRANSPARENT);
        }
        scene.setFill(Color.TRANSPARENT);
        showWithPreload(preloadFxml, configPresent, stageName);
        var user32 = User32.INSTANCE;
        var hWnd = user32.FindWindow(null, finalTitle);
        var oldStyle = user32.GetWindowLong(hWnd, WinUser.GWL_STYLE);
        getStage(stageName).iconifiedProperty().addListener((_, _, t1) -> {
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
     * @param preloadFxml   true if the main stage has been preloaded
     * @param configPresent true if config file is present
     */
    private void showWithPreload(boolean preloadFxml, boolean configPresent, String stageName) {
        if (preloadFxml) {
            log.debug("Preloading stage");
            getStage(stageName).setOpacity(0);
            if (configPresent) getStage(stageName).show();
            else getStage(stageName).showAndWait();
            getStage(stageName).close();
            getStage(stageName).setOpacity(1);
        } else {
            if (configPresent) {
                getStage(stageName).show();
                if (NativeExecutor.isSystemTraySupported()
                        && (stageName.equals(Constants.FXML_SETTINGS) || stageName.equals(Constants.FXML_INFO))) {
                    getStage(stageName).setIconified(false);
                    getStage(stageName).toFront();
                }
            } else {
                getStage(stageName).showAndWait();
            }
        }
    }

    /**
     * Manage window dragging
     *
     * @param root parent
     */
    private void manageWindowDragging(Parent root, String stageName) {
        root.setOnMousePressed(event -> {
            setxOffset(event.getSceneX(), stageName);
            setyOffset(event.getSceneY(), stageName);
        });
        root.setOnMouseDragged(event -> {
            if (getyOffset(stageName) < Constants.TITLE_BAR_HEIGHT) {
                getStage(stageName).setX(event.getScreenX() - getxOffset(stageName));
                getStage(stageName).setY(event.getScreenY() - getyOffset(stageName));
            }
        });
    }

    /**
     * Stop capturing threads
     *
     * @param publishToTopic send info to the microcontroller via MQTT or via HTTP GET
     */
    public void stopCapturingThreads(boolean publishToTopic) {
        if (GrabberSingleton.getInstance() != null) GrabberSingleton.getInstance().resetFlowRamp();
        if (((ManagerSingleton.getInstance().client != null) || MainSingleton.getInstance().config.isFullFirmware()) && publishToTopic) {
            StateDto stateDto = getStateDto();
            if (NativeExecutor.isLinux()) {
                CommonUtility.delayMilliseconds(() -> {
                    NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT), CommonUtility.toJsonString(stateDto));
                }, 300);
            } else {
                CommonUtility.sleepMilliseconds(300);
                NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT), CommonUtility.toJsonString(stateDto));
            }
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
        trayIconManager.updateTray();
    }

    /**
     * Stop capturing threads without sending the signal to the firmware
     */
    public void stopPipeline() {
        pipelineManager.stopCapturePipeline();
    }

    /**
     * Start capturing threads
     */
    public void startCapturingThreads() {
        SimdBenchmark.resetSimdBenchmark();
        if (!MainSingleton.getInstance().communicationError) {
            if (!MainSingleton.getInstance().RUNNING) {
                trayIconManager.setTrayIconImage(Enums.PlayerStatus.PLAY_WAITING);
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
            trayIconManager.updateTray();
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
     * Show settings when needed and check for updates. In headless mode, only Glow Worm firmware is checked.
     *
     * @param showChangelog show changelog
     */
    public void showSettingsAndCheckForUpgrade(boolean showChangelog) {
        boolean headless = MainSingleton.getInstance().isHeadlessMode();
        if (!upgradeCheckInProgress.compareAndSet(false, true)) {
            log.info("Update already in progress");
            if (!headless) {
                showLocalizedNotification(LabelKey.CHECK_UPDATE, LabelKey.UPDATE_ALREADY_IN_PROGRESS,
                        Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.INFO);
            }
            return;
        }
        AtomicBoolean released = new AtomicBoolean();
        Runnable releaseCheck = () -> {
            if (released.compareAndSet(false, true)) {
                upgradeCheckInProgress.set(false);
            }
        };
        try {
            if (!headless && !NativeExecutor.isSystemTraySupported()) {
                showSettingsDialog(false);
            }
            UpgradeManager upgradeManager = new UpgradeManager();
            upgradeManager.checkForUpdates(showChangelog && !headless, releaseCheck);
        } catch (RuntimeException | Error e) {
            releaseCheck.run();
            throw e;
        }
    }

    public Stage getStage(String stageName) {
        return stageName.equals(Constants.FXML_INFO) ? stageInfo : stage;
    }

    void setStage(Stage stagepassed, String stageName) {
        if (stageName.equals(Constants.FXML_INFO)) {
            stageInfo = stagepassed;
        } else {
            stage = stagepassed;
        }
    }

    public Scene getMainScene(String stageName) {
        if (stageName.equals(Constants.FXML_INFO)) {
            return mainSceneInfo;
        } else {
            return mainScene;
        }
    }

    public void setMainScene(Scene mainScene, String stageName) {
        if (stageName.equals(Constants.FXML_INFO)) {
            this.mainSceneInfo = mainScene;
        } else {
            this.mainScene = mainScene;
        }

    }

    public double getxOffset(String stageName) {
        if (stageName.equals(Constants.FXML_INFO)) {
            return xOffsetInfo;
        } else {
            return xOffset;
        }

    }

    public void setxOffset(double xOffset, String stageName) {
        if (stageName.equals(Constants.FXML_INFO)) {
            this.xOffsetInfo = xOffset;
        } else {
            this.xOffset = xOffset;
        }
    }

    public double getyOffset(String stageName) {
        if (stageName.equals(Constants.FXML_INFO)) {
            return yOffsetInfo;
        } else {
            return yOffset;
        }
    }

    public void setyOffset(double yOffset, String stageName) {
        if (stageName.equals(Constants.FXML_INFO)) {
            this.yOffsetInfo = yOffset;
        } else {
            this.yOffset = yOffset;
        }
    }

}
