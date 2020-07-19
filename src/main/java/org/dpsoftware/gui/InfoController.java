/*
  GStreamerGrabber.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/

package org.dpsoftware.gui;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;
import org.dpsoftware.FastScreenCapture;

import java.awt.*;
import java.net.URI;

public class InfoController {

    @FXML
    public Label producerLabel;
    @FXML
    public Label consumerLabel;
    @FXML
    public Label version;
    private final StringProperty producerValue = new SimpleStringProperty("");
    private final StringProperty consumerValue = new SimpleStringProperty("");
    public Button closeButton;

    @FXML
    protected void initialize() {

        producerLabel.textProperty().bind(producerValueProperty());
        consumerLabel.textProperty().bind(consumerValueProperty());
        version.setText("by Davide Perini (VERSION)".replaceAll("VERSION", FastScreenCapture.VERSION));
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                setProducerValue("Producing @ " + FastScreenCapture.FPS_PRODUCER + " FPS");
                setConsumerValue("Consuming @ " + FastScreenCapture.FPS_CONSUMER + " FPS");
            }
        }.start();

    }

    @FXML
    public void onMouseClickedCloseBtn(InputEvent e) {

        Platform.setImplicitExit(false);
        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();

    }

    @FXML
    public void onMouseClickedGitHubLink(ActionEvent link) {

        Desktop desktop = Desktop.getDesktop();
        try {
            String myUrl = "https://github.com/sblantipodi/JavaFastScreenCapture";
            URI github = new URI(myUrl);
            desktop.browse(github);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

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
