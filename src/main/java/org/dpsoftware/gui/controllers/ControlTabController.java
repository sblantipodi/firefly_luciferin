/*
  ControlTabController.java

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
import org.dpsoftware.gui.GuiManager;
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
        if (!NativeExecutor.isSystemTraySupported()) {
            producerLabel.textProperty().bind(producerValueProperty());
            consumerLabel.textProperty().bind(consumerValueProperty());
            if (MainSingleton.getInstance().communicationError) {
                controlImage = getImage(Enums.PlayerStatus.GREY);
            } else if (MainSingleton.getInstance().RUNNING) {
                controlImage = getImage(Enums.PlayerStatus.PLAY_WAITING);
            } else {
                controlImage = getImage(Enums.PlayerStatus.STOP);
            }
            version.setText("by Davide Perini (VERSION)".replaceAll("VERSION", MainSingleton.getInstance().version));
            setButtonImage();
        }
    }

    /**
     * Transform string to image
     *
     * @param status player status
     * @return image
     */
    Image getImage(Enums.PlayerStatus status) {
        try {
            return new Image(Objects.requireNonNull(this.getClass().getResource(GuiManager.computeImageToUse(status))).toString(), true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile() {
        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
        if (!NativeExecutor.isSystemTraySupported() && MainSingleton.getInstance().config.isToggleLed()) {
            switch (effectInUse) {
                case BIAS_LIGHT, MUSIC_MODE_VU_METER, MUSIC_MODE_VU_METER_DUAL, MUSIC_MODE_BRIGHT, MUSIC_MODE_RAINBOW ->
                        controlImage = getImage(Enums.PlayerStatus.PLAY_WAITING);
                default -> controlImage = getImage(Enums.PlayerStatus.STOP);
            }
        } else {
            controlImage = getImage(Enums.PlayerStatus.STOP);
        }
        setButtonImage();
    }

    /**
     * Start and stop capturing
     *
     * @param e InputEvent
     */
    @FXML
    @SuppressWarnings("unused")
    public void onMouseClickedPlay(InputEvent e) {
        controlImage = getImage(Enums.PlayerStatus.GREY);
        if (!MainSingleton.getInstance().communicationError) {
            if (MainSingleton.getInstance().RUNNING) {
                controlImage = getImage(Enums.PlayerStatus.STOP);
            } else {
                controlImage = getImage(Enums.PlayerStatus.PLAY_WAITING);
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
                    if (NativeExecutor.isSystemTraySupported()) {
                        settingsController.manageDeviceList();
                    } else {
                        settingsController.manageDeviceList();
                        setProducerValue(CommonUtility.getWord("fxml.controltab.producer") + " @ " + MainSingleton.getInstance().FPS_PRODUCER + " FPS");
                        setConsumerValue(CommonUtility.getWord("fxml.controltab.consumer") + " @ " + MainSingleton.getInstance().FPS_GW_CONSUMER + " FPS");
                        if (MainSingleton.getInstance().RUNNING && controlImage != null && controlImage.getUrl().contains("waiting")) {
                            controlImage = getImage(Enums.PlayerStatus.PLAY);
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
                GuiManager.createTooltip(Constants.TOOLTIP_PLAYBUTTON_NULL, 50, playButton);
            }
        } else {
            if (!NativeExecutor.isWindows()) {
                GuiManager.createTooltip(Constants.TOOLTIP_PLAYBUTTON, 200, playButton);
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
