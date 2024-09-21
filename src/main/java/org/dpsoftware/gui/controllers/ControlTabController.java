/*
  ControlTabController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.utilities.CommonUtility;

import java.util.Objects;

/**
 * Control Tab controller
 */
public class ControlTabController {

    @FXML
    private final StringProperty producerValue = new SimpleStringProperty("");
    @FXML
    private final StringProperty consumerValue = new SimpleStringProperty("");
    @FXML
    public Button showInfo;
    public AnimationTimer animationTimer;
    Image controlImage;
    ImageView imageView;
    Image imagePlay, imagePlayCenter, imagePlayLeft, imagePlayRight, imagePlayWaiting, imagePlayWaitingCenter, imagePlayWaitingLeft, imagePlayWaitingRight;
    Image imageStop, imageStopOff, imageStopCenter, imageStopCenterOff, imageStopLeft, imageStopLeftOff, imageStopRight, imageStopRightOff;
    Image imageGreyStop, imageGreyStopCenter, imageGreyStopLeft, imageGreyStopRight, imageGreyStopRightOff;
    // Inject main controller
    @FXML
    private SettingsController settingsController;
    // FXML binding
    @FXML
    private Label version;
    @FXML
    private Label producerLabel;
    @FXML
    private Label consumerLabel;
    @FXML
    private Button playButton;

    /**
     * Inject main controller containing the TabPane
     *
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
            if (MainSingleton.getInstance().communicationError) {
                controlImage = setImage(Enums.PlayerStatus.GREY);
            } else if (MainSingleton.getInstance().RUNNING) {
                controlImage = setImage(Enums.PlayerStatus.PLAY_WAITING);
            } else {
                controlImage = setImage(Enums.PlayerStatus.STOP);
            }
            version.setText("by Davide Perini (VERSION)".replaceAll("VERSION", MainSingleton.getInstance().version));
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
        imageStopOff = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_OFF)).toString(), true);
        imageStopCenter = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_CENTER)).toString(), true);
        imageStopCenterOff = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_CENTER_OFF)).toString(), true);
        imageStopLeft = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_LEFT)).toString(), true);
        imageStopLeftOff = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_LEFT_OFF)).toString(), true);
        imageStopRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_RIGHT)).toString(), true);
        imageStopRightOff = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_RIGHT_OFF)).toString(), true);
        imageGreyStop = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY)).toString(), true);
        imageGreyStopCenter = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_CENTER)).toString(), true);
        imageGreyStopLeft = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_LEFT)).toString(), true);
        imageGreyStopRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_RIGHT)).toString(), true);
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            imagePlayRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_RIGHT_GOLD)).toString(), true);
            imagePlayWaitingRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT_GOLD)).toString(), true);
            imageStopRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD)).toString(), true);
            imageGreyStopRight = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_RIGHT_GOLD)).toString(), true);
            imageGreyStopRightOff = new Image(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_RIGHT_GOLD_OFF)).toString(), true);
        }
    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile() {
        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
        if (!NativeExecutor.isWindows() && MainSingleton.getInstance().config.isToggleLed()) {
            switch (effectInUse) {
                case BIAS_LIGHT, MUSIC_MODE_VU_METER, MUSIC_MODE_VU_METER_DUAL, MUSIC_MODE_BRIGHT, MUSIC_MODE_RAINBOW -> {
                    controlImage = setImage(Enums.PlayerStatus.PLAY_WAITING);
                    setButtonImage();
                }
            }
        }
    }

    /**
     * Start and stop capturing
     *
     * @param e InputEvent
     */
    @FXML
    @SuppressWarnings("unused")
    public void onMouseClickedPlay(InputEvent e) {
        controlImage = setImage(Enums.PlayerStatus.GREY);
        if (!MainSingleton.getInstance().communicationError) {
            if (MainSingleton.getInstance().RUNNING) {
                controlImage = setImage(Enums.PlayerStatus.STOP);
            } else {
                controlImage = setImage(Enums.PlayerStatus.PLAY_WAITING);
            }
            setButtonImage();
            if (MainSingleton.getInstance().RUNNING) {
                MainSingleton.getInstance().guiManager.stopCapturingThreads(true);
            } else {
                MainSingleton.getInstance().guiManager.startCapturingThreads();
            }
        }
    }

    /**
     * Show info popup on Linux
     *
     * @param e InputEvent
     */
    @FXML
    @SuppressWarnings("unused")
    public void onMouseClickedShowInfo(InputEvent e) {
        MainSingleton.getInstance().guiManager.showFramerateDialog();
    }

    /**
     * Set and return LED tab image
     *
     * @param playerStatus PLAY, STOP, GREY
     * @return tray icon
     */
    @SuppressWarnings("Duplicates")
    public Image setImage(Enums.PlayerStatus playerStatus) {
        Image imgControl;
        if (MainSingleton.getInstance().config == null) {
            imgControl = imageGreyStop;
        } else {
            imgControl = switch (playerStatus) {
                case PLAY -> setImage(imagePlay, imagePlayRight, imagePlayLeft, imagePlayCenter);
                case PLAY_WAITING ->
                        setImage(imagePlayWaiting, imagePlayWaitingRight, imagePlayWaitingLeft, imagePlayWaitingCenter);
                case STOP -> setImage(imageStop, imageStopRight, imageStopLeft, imageStopCenter);
                case GREY -> setImage(imageGreyStop, imageGreyStopRight, imageGreyStopLeft, imageGreyStopCenter);
                case OFF -> setImage(imageStopOff, imageStopRightOff, imageStopLeftOff, imageStopCenterOff);
            };
        }
        return imgControl;
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
    private Image setImage(Image imagePlay, Image imagePlayRight, Image imagePlayLeft, Image imagePlayCenter) {
        Image img = null;
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
     * Manage animation timer to update the UI every seconds
     */
    public void startAnimationTimer() {
        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                now = now / 1_000_000_000;
                if (now - lastUpdate >= 1) {
                    lastUpdate = now;
                    if (NativeExecutor.isWindows()) {
                        settingsController.manageDeviceList();
                    } else {
                        settingsController.manageDeviceList();
                        setProducerValue(CommonUtility.getWord("fxml.controltab.producer") + " @ " + MainSingleton.getInstance().FPS_PRODUCER + " FPS");
                        setConsumerValue(CommonUtility.getWord("fxml.controltab.consumer") + " @ " + MainSingleton.getInstance().FPS_GW_CONSUMER + " FPS");
                        if (MainSingleton.getInstance().RUNNING && controlImage != null && controlImage.getUrl().contains("waiting")) {
                            controlImage = setImage(Enums.PlayerStatus.PLAY);
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
     *
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        settingsController.save(e);
    }

    /**
     * Set form tooltips
     *
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        if (currentConfig == null) {
            if (!NativeExecutor.isWindows()) {
                playButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_PLAYBUTTON_NULL, 50));
            }
        } else {
            if (!NativeExecutor.isWindows()) {
                playButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_PLAYBUTTON, 200));
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
     *
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
