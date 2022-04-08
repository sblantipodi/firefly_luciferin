/*
  ControlTabController.java

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
package org.dpsoftware.gui.controllers;

import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.utilities.CommonUtility;

import java.util.Objects;

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
    @FXML public Button showInfo;
    Image controlImage;
    ImageView imageView;
    public AnimationTimer animationTimer;
    Image imagePlay, imagePlayCenter, imagePlayLeft, imagePlayRight, imagePlayWaiting, imagePlayWaitingCenter, imagePlayWaitingLeft, imagePlayWaitingRight;
    Image imageStop, imageStopCenter, imageStopLeft, imageStopRight;
    Image imageGreyStop, imageGreyStopCenter, imageGreyStopLeft, imageGreyStopRight;


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
            initImages();
        }
    }

    /**
     * Initialize tab Control images
     */
    public void initImages() {
        imagePlay = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY)).toString(), true);
        imagePlayCenter = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_CENTER)).toString(), true);
        imagePlayLeft = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_LEFT)).toString(), true);
        imagePlayRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_RIGHT)).toString(), true);
        imagePlayWaiting = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_WAITING)).toString(), true);
        imagePlayWaitingCenter = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_WAITING_CENTER)).toString(), true);
        imagePlayWaitingLeft = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT)).toString(), true);
        imagePlayWaitingRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT)).toString(), true);
        imageStop = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO)).toString(), true);
        imageStopCenter = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_CENTER)).toString(), true);
        imageStopLeft = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_LEFT)).toString(), true);
        imageStopRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_RIGHT)).toString(), true);
        imageGreyStop = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY)).toString(), true);
        imageGreyStopCenter = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_CENTER)).toString(), true);
        imageGreyStopLeft = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_LEFT)).toString(), true);
        imageGreyStopRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_RIGHT)).toString(), true);
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            imagePlayRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_RIGHT_GOLD)).toString(), true);
            imagePlayWaitingRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT_GOLD)).toString(), true);
            imageStopRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD)).toString(), true);
            imageGreyStopRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_RIGHT_GOLD)).toString(), true);
        }
    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile() {
        if (!NativeExecutor.isWindows() && FireflyLuciferin.config.isToggleLed() && (Constants.Effect.BIAS_LIGHT.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                || Constants.Effect.MUSIC_MODE_VU_METER.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                || Constants.Effect.MUSIC_MODE_VU_METER_DUAL.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                || Constants.Effect.MUSIC_MODE_BRIGHT.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))
                || Constants.Effect.MUSIC_MODE_RAINBOW.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect())))) {
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
     * Show info popup on Linux
     * @param e InputEvent
     */
    @FXML
    @SuppressWarnings("unused")
    public void onMouseClickedShowInfo(InputEvent e) {
        FireflyLuciferin.guiManager.showFramerateDialog();
    }

    /**
     * Set and return LED tab image
     * @param playerStatus PLAY, STOP, GREY
     * @return tray icon
     */
    @SuppressWarnings("Duplicates")
    public Image setImage(Constants.PlayerStatus playerStatus) {
        Image imgControl;
        if (FireflyLuciferin.config == null) {
            imgControl = imageGreyStop;
        } else {
            imgControl = switch (playerStatus) {
                case PLAY -> setImage(imagePlay, imagePlayRight, imagePlayLeft, imagePlayCenter);
                case PLAY_WAITING -> setImage(imagePlayWaiting, imagePlayWaitingRight, imagePlayWaitingLeft, imagePlayWaitingCenter);
                case STOP -> setImage(imageStop, imageStopRight, imageStopLeft, imageStopCenter);
                case GREY -> setImage(imageGreyStop, imageGreyStopRight, imageGreyStopLeft, imageGreyStopCenter);
            };
        }
        return imgControl;
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
                        setProducerValue(CommonUtility.getWord("fxml.controltab.producer") + " @ " + FireflyLuciferin.FPS_PRODUCER + " FPS");
                        setConsumerValue(CommonUtility.getWord("fxml.controltab.consumer") + " @ " + FireflyLuciferin.FPS_GW_CONSUMER + " FPS");
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
