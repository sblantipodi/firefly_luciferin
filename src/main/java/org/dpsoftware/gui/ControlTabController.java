/*
  ControlTabController.java

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

import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;

/**
 * Control Tab controller
 */
public class ControlTabController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    // FXML binding
    @FXML private Label version;
    @FXML private Label producerLabel;
    @FXML private Label consumerLabel;
    @FXML private final StringProperty producerValue = new SimpleStringProperty("");
    @FXML private final StringProperty consumerValue = new SimpleStringProperty("");
    @FXML private Button playButton;
    Image controlImage;
    ImageView imageView;
    public AnimationTimer animationTimer;


    /**
     * Inject main controller containing the TabPane
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {

        if (NativeExecutor.isLinux()) {
            producerLabel.textProperty().bind(producerValueProperty());
            consumerLabel.textProperty().bind(consumerValueProperty());
            if (FireflyLuciferin.communicationError) {
                controlImage = setImage(Constants.PlayerStatus.GREY);
            } else if (FireflyLuciferin.RUNNING) {
                controlImage = setImage(Constants.PlayerStatus.PLAY_WAITING);
            } else {
                controlImage = setImage(Constants.PlayerStatus.STOP);
            }
            version.setText("by Davide Perini (VERSION)".replaceAll("VERSION", FireflyLuciferin.version));
            setButtonImage();
        }

    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile() {

        if (!NativeExecutor.isWindows() && FireflyLuciferin.config.isToggleLed() && (Constants.Effect.BIAS_LIGHT.getEffect().equals(FireflyLuciferin.config.getEffect())
                || Constants.Effect.MUSIC_MODE_VU_METER.getEffect().equals(FireflyLuciferin.config.getEffect())
                || Constants.Effect.MUSIC_MODE_BRIGHT.getEffect().equals(FireflyLuciferin.config.getEffect())
                || Constants.Effect.MUSIC_MODE_RAINBOW.getEffect().equals(FireflyLuciferin.config.getEffect()))) {
            controlImage = setImage(Constants.PlayerStatus.PLAY_WAITING);
            setButtonImage();
        }

    }

    /**
     * Start and stop capturing
     * @param e InputEvent
     */
    @FXML
    @SuppressWarnings("unused")
    public void onMouseClickedPlay(InputEvent e) {

        controlImage = setImage(Constants.PlayerStatus.GREY);
        if (!FireflyLuciferin.communicationError) {
            if (FireflyLuciferin.RUNNING) {
                controlImage = setImage(Constants.PlayerStatus.STOP);
            } else {
                controlImage = setImage(Constants.PlayerStatus.PLAY_WAITING);
            }
            setButtonImage();
            if (FireflyLuciferin.RUNNING) {
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
            } else {
                FireflyLuciferin.guiManager.startCapturingThreads();
            }
        }

    }

    /**
     * Set and return LED tab image
     * @param playerStatus PLAY, STOP, GREY
     * @return tray icon
     */
    @SuppressWarnings("ConstantConditions")
    public Image setImage(Constants.PlayerStatus playerStatus) {

        String imgPath = "";
        if (settingsController.currentConfig == null) {
            imgPath = Constants.IMAGE_CONTROL_PLAY;
        } else {
            switch (playerStatus) {
                case PLAY:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((settingsController.currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_RIGHT;
                            }
                            break;
                        case 2:
                            if ((settingsController.currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_PLAY_LEFT;
                            break;
                    }
                    break;
                case PLAY_WAITING:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((settingsController.currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT;
                            }
                            break;
                        case 2:
                            if ((settingsController.currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT;
                            break;
                    }
                    break;
                case STOP:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((settingsController.currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_LOGO;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_LOGO_RIGHT;
                            }
                            break;
                        case 2:
                            if ((settingsController.currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_LOGO_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_LOGO_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_LOGO_LEFT;
                            break;
                    }
                    break;
                case GREY:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((settingsController.currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_GREY;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_GREY_RIGHT;
                            }
                            break;
                        case 2:
                            if ((settingsController.currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_GREY_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_GREY_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_GREY_LEFT;
                            break;
                    }
                    break;
            }
        }
        return new Image(this.getClass().getResource(imgPath).toString(), true);

    }

    /**
     * Manage animation timer to update the UI every seconds
     */
    public void startAnimationTimer() {

        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0 ;
            @Override
            public void handle(long now) {
                now = now / 1_000_000_000;
                if (now - lastUpdate >= 1) {
                    lastUpdate = now;
                    if (NativeExecutor.isWindows()) {
                        settingsController.manageDeviceList();
                    } else {
                        settingsController.manageDeviceList();
                        setProducerValue("Producing @ " + FireflyLuciferin.FPS_PRODUCER + " FPS");
                        setConsumerValue("Consuming @ " + FireflyLuciferin.FPS_GW_CONSUMER + " FPS");
                        if (FireflyLuciferin.RUNNING && controlImage != null && controlImage.getUrl().contains("waiting")) {
                            controlImage = setImage(Constants.PlayerStatus.PLAY);
                            setButtonImage();
                        }
                    }
                }
            }
        };
        animationTimer.start();

    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {

        settingsController.save(e);

    }

    /**
     * Set form tooltips
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {

        if (currentConfig == null) {
            if (!NativeExecutor.isWindows()) {
                playButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_PLAYBUTTON_NULL, 50, 6000));
            }
        } else {
            if (!NativeExecutor.isWindows()) {
                playButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_PLAYBUTTON, 200, 6000));
            }
        }

    }

    /**
     * Set button image
     */
    private void setButtonImage() {

        imageView = new ImageView(controlImage);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);
        playButton.setGraphic(imageView);

    }

    /**
     * Return the observable devices list
     * @return devices list
     */
    public StringProperty producerValueProperty() {
        return producerValue;
    }

    public void setProducerValue(String producerValue) {
        this.producerValue.set(producerValue);
    }

    public StringProperty consumerValueProperty() {
        return consumerValue;
    }

    public void setConsumerValue(String consumerValue) {
        this.consumerValue.set(consumerValue);
    }

}
