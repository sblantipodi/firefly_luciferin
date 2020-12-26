/*
  InfoController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020  Davide Perini

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
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;

/**
 * FXML Info Controller
 */
@Slf4j
public class InfoController {

    @FXML private SplitPane splitPane;
    @FXML private Label producerLabel;
    @FXML private Label consumerLabel;
    @FXML private Label version;
    @FXML private final StringProperty producerValue = new SimpleStringProperty("");
    @FXML private final StringProperty consumerValue = new SimpleStringProperty("");
    AnimationTimer animationTimer;

    @FXML
    protected void initialize() {

        Platform.setImplicitExit(false);

        producerLabel.textProperty().bind(producerValueProperty());
        consumerLabel.textProperty().bind(consumerValueProperty());
        UpgradeManager vm = new UpgradeManager();
        version.setText("by Davide Perini (VERSION)".replaceAll("VERSION", FireflyLuciferin.version));
        runLater();
        startAnimationTimer();

    }

    /**
     * Run Later after GUI Init
     */
    private void runLater() {

        Platform.runLater(() -> {
            Stage stage = (Stage) splitPane.getScene().getWindow();
            if (stage != null) {
                stage.setOnCloseRequest(evt -> {
                    animationTimer.stop();
                });
            }
        });

    }

    /**
     * Manage animation timer to update the UI every seconds
     */
    private void startAnimationTimer() {

        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0 ;
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 1_000_000_000) {
                    setProducerValue("Producing @ " + FireflyLuciferin.FPS_PRODUCER + " FPS");
                    setConsumerValue("Consuming @ " + (FireflyLuciferin.config.isMqttEnable() ? FireflyLuciferin.FPS_GW_CONSUMER : FireflyLuciferin.FPS_CONSUMER) + " FPS");
                }
            }
        };
        animationTimer.start();

    }

    @FXML
    public void onMouseClickedCloseBtn(InputEvent e) {

        animationTimer.stop();
        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();
        animationTimer.stop();

    }

    @FXML
    public void onMouseClickedGitHubLink(ActionEvent link) {

        FireflyLuciferin.guiManager.surfToGitHub();

    }

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
