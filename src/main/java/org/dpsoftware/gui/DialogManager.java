/*
  DialogManager.java

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

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.bindings.notify.LibNotify;
import org.dpsoftware.gui.controllers.*;
import org.dpsoftware.gui.tc.RleVisualMapHandler;
import org.dpsoftware.gui.tc.TcInteractionHandler;
import org.dpsoftware.gui.trayicon.TrayIconAwt;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;


/**
 * Alerts, notifications, web content, and secondary dialogs.
 */
@Slf4j
class DialogManager {

    private final GuiManager guiManager;
    private final WebView wv;

    /**
     * Create the dialog manager for the main GUI.
     *
     * @param guiManager owner of the main settings stage and theme
     */
    DialogManager(GuiManager guiManager) {
        this.guiManager = guiManager;
        this.wv = new WebView();
    }

    /**
     * Convert alert type
     *
     * @param notificationType error, info, warning
     * @return converted alert type
     */
    private static Alert.AlertType convertAlertType(TrayIcon.MessageType notificationType) {
        return switch (notificationType) {
            case ERROR -> Alert.AlertType.ERROR;
            case WARNING -> Alert.AlertType.WARNING;
            case NONE -> Alert.AlertType.NONE;
            default -> Alert.AlertType.INFORMATION;
        };
    }

    /**
     * Show alert in a JavaFX dialog
     *
     * @param title     dialog title
     * @param header    dialog header
     * @param content   dialog msg
     * @param alertType alert type
     * @return the selected button, or an empty value if the dialog is dismissed
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
     * @return the selected button, or an empty value if the dialog is dismissed
     */
    public Optional<ButtonType> showLocalizedAlert(String title, String header, String content, Alert.AlertType alertType) {
        title = CommonUtility.getWord(title);
        header = CommonUtility.getWord(header);
        content = CommonUtility.getWord(content);
        return showAlert(title, header, content, alertType);
    }

    /**
     * Show alert in a JavaFX dialog
     *
     * @param title     dialog title
     * @param header    dialog header
     * @param content   dialog msg
     * @param alertType alert type
     * @param buttons   buttons to display
     * @return the selected button, or an empty value if the dialog is dismissed
     */
    private Optional<ButtonType> showAlert(String title, String header, String content, Alert.AlertType alertType, ButtonType... buttons) {
        Alert alert = createAlert(title, header, alertType);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(buttons);
        setAlertTheme(alert);
        return alert.showAndWait();
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
        if (NativeExecutor.isWindows()) {
            ((TrayIconAwt) MainSingleton.getInstance().guiManager.trayIconManager).trayIcon.displayMessage(highlight, content, notificationType);
        } else {
            if (LibNotify.isSupported()) {
                LibNotify.showLinuxNotification(highlight, content, notificationType);
            } else {
                showAlert(title, highlight, content, convertAlertType(notificationType));
            }
        }
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
        if (NativeExecutor.isWindows()) {
            ((TrayIconAwt) MainSingleton.getInstance().guiManager.trayIconManager).trayIcon
                    .displayMessage(CommonUtility.getWord(highlight), CommonUtility.getWord(content), notificationType);
        } else {
            if (LibNotify.isSupported()) {
                LibNotify.showLocalizedLinuxNotification(highlight, content, notificationType);
            } else {
                showLocalizedAlert(title, highlight, content, convertAlertType(notificationType));
            }
        }
    }

    /**
     * Set alert theme
     *
     * @param alert in use
     */
    private void setAlertTheme(Alert alert) {
        guiManager.setStylesheet(alert.getDialogPane().getStylesheets(), null);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
    }

    /**
     * Set dialog theme
     *
     * @param dialog in use
     */
    public void setDialogTheme(Dialog<String> dialog) {
        guiManager.setStylesheet(dialog.getDialogPane().getStylesheets(), null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
    }

    /**
     * Show an alert that contains a Web View in a JavaFX dialog
     *
     * @param title     dialog title
     * @param header    dialog header
     * @param webUrl    URL to load inside the web view
     * @param alertType alert type
     * @return the selected button, or an empty value if the dialog is dismissed
     */
    public Optional<ButtonType> showWebAlert(String title, String header, String webUrl, Alert.AlertType alertType) {
        //wv.getEngine().load(Objects.requireNonNull(getClass().getResource("css/pro.html")).toExternalForm());
        wv.getEngine().load(webUrl);
        wv.getEngine().setUserStyleSheetLocation(Objects.requireNonNull(getClass().getResource(Constants.CSS_WEB_VIEW)).toExternalForm());
        int windowWidth = 1200 * CommonUtility.scaleDownResolution(MainSingleton.getInstance().config.getScreenResX(),
                MainSingleton.getInstance().config.getOsScaling()) / Constants.REFERENCE_RESOLUTION_FOR_SCALING_X;
        int windowHeight = 600 * CommonUtility.scaleDownResolution(MainSingleton.getInstance().config.getScreenResY(),
                MainSingleton.getInstance().config.getOsScaling()) / Constants.REFERENCE_RESOLUTION_FOR_SCALING_Y;
        wv.setPrefWidth(windowWidth);
        wv.setPrefHeight(windowHeight);
        Alert alert = createAlert(title, header, alertType);
        alert.getDialogPane().setContent(wv);
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL, ButtonType.PREVIOUS, ButtonType.NEXT);
        final Node btnPrev = alert.getDialogPane().lookupButton(ButtonType.PREVIOUS);
        btnPrev.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            goBack();
        });
        final Node btnNext = alert.getDialogPane().lookupButton(ButtonType.NEXT);
        btnNext.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            goNext();
        });
        setAlertTheme(alert);
        return alert.showAndWait();
    }

    /**
     * Go back in web view history
     */
    private void goBack() {
        Platform.runLater(() -> wv.getEngine().executeScript("history.back()"));
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
        GuiManager.setStageIcon(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert;
    }

    /**
     * Show color correction dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     * @param event              input event
     */
    public void showColorCorrectionDialog(SettingsController settingsController, InputEvent event) {
        Platform.runLater(() -> {
            TestCanvas previous = GuiSingleton.getInstance().testCanvas;
            TestCanvas testCanvas;
            if (!GuiSingleton.getInstance().rleVisualMapVisible && previous != null && previous.getStage() != null && previous.getCanvas() != null) {
                // Reuse the existing canvas/scene/stage to avoid repeatedly tearing down and re creating JavaFX scenes
                // (which can orphan the NGCanvas in the render graph and exhaust the Prism texture pool).
                testCanvas = previous;
                previous.refreshExisting(new TcInteractionHandler(previous), new RleVisualMapHandler(previous), MainSingleton.getInstance().config);
            } else {
                testCanvas = new TestCanvas();
                testCanvas.buildAndShowTestImage(event);
                GuiSingleton.getInstance().testCanvas = testCanvas;
            }
            Platform.runLater(() -> {
                // Reuse the existing dialog stage if it's still open, otherwise create a new one
                Stage existingStage = GuiSingleton.getInstance().colorDialog;
                if (existingStage != null && existingStage.isShowing()) {
                    // Already open: just re inject and refresh
                    ColorCorrectionDialogController existingController = (ColorCorrectionDialogController) existingStage.getProperties().get(Constants.FXML_COLOR_CORRECTION_DIALOG);
                    if (existingController != null) {
                        existingController.injectSettingsController(settingsController);
                        existingController.injectTestCanvas(testCanvas);
                        existingController.initValuesFromSettingsFile(testCanvas.getConfigHistory().getFirst());
                        existingStage.toFront();
                        return;
                    }
                }
                // Close the old stage if it exists but is not showing
                if (existingStage != null) {
                    existingStage.close();
                }
                FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(Constants.FXML_COLOR_CORRECTION_DIALOG + Constants.FXML), MainSingleton.getInstance().bundle);
                Parent root;
                try {
                    root = fxmlLoader.load();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                ColorCorrectionDialogController controller = fxmlLoader.getController();
                controller.injectSettingsController(settingsController);
                controller.injectTestCanvas(testCanvas);
                controller.initValuesFromSettingsFile(testCanvas.getConfigHistory().getFirst());
                Stage newStage = initStage(root);
                newStage.initStyle(StageStyle.TRANSPARENT);
                newStage.initModality(Modality.NONE);
                newStage.setAlwaysOnTop(true);
                // Dialog drag support
                final Stage finalStage = newStage;
                final Delta dragDelta = new Delta();
                root.setOnMousePressed(ev -> {
                    dragDelta.x = finalStage.getX() - ev.getScreenX();
                    dragDelta.y = finalStage.getY() - ev.getScreenY();
                });
                root.setOnMouseDragged(eve -> {
                    finalStage.setX(eve.getScreenX() + dragDelta.x);
                    finalStage.setY(eve.getScreenY() + dragDelta.y);
                });
                GuiSingleton.getInstance().colorDialog = finalStage;
                finalStage.getProperties().put(Constants.FXML_COLOR_CORRECTION_DIALOG, controller);
                finalStage.show();
                finalStage.toFront();
                Platform.runLater(() -> {
                    new TestCanvas().setDialogMargin(finalStage);
                    testCanvas.setDialogY((int) finalStage.getY());
                    finalStage.setAlwaysOnTop(true);
                    finalStage.toFront();
                });
            });
        });
    }

    /**
     * Go forward in web view history
     */
    private void goNext() {
        Platform.runLater(() -> wv.getEngine().executeScript("history.forward()"));
    }

    /**
     * Show a secondary stage dialog
     *
     * @param classForCast controller class used to initialize the dialog
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
            if (MainSingleton.getInstance().config != null) {
                ((EyeCareDialogController) controller).initValuesFromSettingsFile(MainSingleton.getInstance().config);
            } else {
                ((EyeCareDialogController) controller).initDefaultValues();
            }
        }
        if (classForCast == ImprovDialogController.class) {
            ((ImprovDialogController) controller).injectSettingsController(settingsController);
            if (MainSingleton.getInstance().config != null) {
                ((ImprovDialogController) controller).initValuesFromSettingsFile();
            } else {
                ((ImprovDialogController) controller).initDefaultValues();
            }
        }
        if (classForCast == ProfileDialogController.class) {
            ((ProfileDialogController) controller).injectSettingsController(settingsController);
            if (MainSingleton.getInstance().config != null) {
                ((ProfileDialogController) controller).initValuesFromSettingsFile(MainSingleton.getInstance().config);
            } else {
                ((ProfileDialogController) controller).initDefaultValues();
            }
        }
        if (classForCast == SmoothingDialogController.class) {
            ((SmoothingDialogController) controller).injectSettingsController(settingsController);
            if (MainSingleton.getInstance().config != null) {
                ((SmoothingDialogController) controller).initValuesFromSettingsFile(MainSingleton.getInstance().config);
            } else {
                ((SmoothingDialogController) controller).initDefaultValues();
            }
        }
        if (classForCast == GammaDialogController.class) {
            ((GammaDialogController) controller).injectSettingsController(settingsController);
            if (MainSingleton.getInstance().config != null) {
                ((GammaDialogController) controller).initValuesFromSettingsFile(MainSingleton.getInstance().config);
            } else {
                ((GammaDialogController) controller).initDefaultValues();
            }
        } else if (classForCast == DisplayDialogController.class) {
            ((DisplayDialogController) controller).injectSettingsController(settingsController);
            if (MainSingleton.getInstance().config != null) {
                ((DisplayDialogController) controller).initValuesFromSettingsFile(MainSingleton.getInstance().config);
            } else {
                ((DisplayDialogController) controller).initDefaultValues();
            }
        } else if (classForCast == SatellitesDialogController.class) {
            ((SatellitesDialogController) controller).injectSettingsController(settingsController);
            ((SatellitesDialogController) controller).setTooltips();
        }
        Stage stage = initStage(root);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        Platform.runLater(() -> {
            Stage parentStage = guiManager.getStage(Constants.FXML_SETTINGS);
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
     * Show improv dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showImprovDialog(SettingsController settingsController) {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(Constants.FXML_IMPROV_DIALOG + Constants.FXML), MainSingleton.getInstance().bundle);
                showSecondaryStage(ImprovDialogController.class, settingsController, fxmlLoader);
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
     * Show profile dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showProfileDialog(SettingsController settingsController) {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(Constants.FXML_PROFILE_DIALOG + Constants.FXML), MainSingleton.getInstance().bundle);
                showSecondaryStage(ProfileDialogController.class, settingsController, fxmlLoader);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Show smoothing dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showSmoothingDialog(SettingsController settingsController) {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(Constants.FXML_SMOOTHING_DIALOG + Constants.FXML), MainSingleton.getInstance().bundle);
                showSecondaryStage(SmoothingDialogController.class, settingsController, fxmlLoader);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Show gamma dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showGammaDialog(SettingsController settingsController) {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(Constants.FXML_GAMMA_DIALOG + Constants.FXML), MainSingleton.getInstance().bundle);
                showSecondaryStage(GammaDialogController.class, settingsController, fxmlLoader);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Show display dialog
     *
     * @param settingsController we need to manually inject dialog controller in the main controller
     */
    public void showDisplayDialog(SettingsController settingsController) {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(GuiManager.class.getResource(Constants.FXML_DISPLAY_DIALOG + Constants.FXML), MainSingleton.getInstance().bundle);
                showSecondaryStage(DisplayDialogController.class, settingsController, fxmlLoader);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * Show alert in a JavaFX dialog
     *
     * @param title     dialog title
     * @param header    dialog header
     * @param content   dialog msg
     * @param alertType alert type
     * @param buttons   buttons to use
     * @return the selected button, or an empty value if the dialog is dismissed
     */
    public Optional<ButtonType> showLocalizedAlert(String title, String header, String content, Alert.AlertType alertType, ButtonType... buttons) {
        title = CommonUtility.getWord(title);
        header = CommonUtility.getWord(header);
        content = CommonUtility.getWord(content);
        return showAlert(title, header, content, alertType, buttons);
    }

    /**
     * Initialize stage
     *
     * @param root parent root
     * @return initialized stage
     */
    private Stage initStage(Parent root) {
        Scene scene;
        scene = new Scene(root);
        guiManager.setStylesheet(scene.getStylesheets(), scene);
        scene.setFill(Color.TRANSPARENT);
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        return stage;
    }

    /**
     * Utility class
     */
    static class Delta {
        double x, y;
    }
}
