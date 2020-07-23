/*
  OptionsController.java

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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class OptionsController {

    @FXML TextField screenWidth;
    @FXML TextField screenHeight;
    @FXML ComboBox scaling;
    @FXML ComboBox gamma;
    @FXML ComboBox captureMethod;
    @FXML TextField numberOfThreads;
    @FXML public Button saveButton;
    @FXML public Button cancelButton;
    @FXML ComboBox serialPort;
    @FXML ComboBox aspectRatio;

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {

        // Get OS scaling using JNA
        GraphicsConfiguration screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        AffineTransform screenInfo = screen.getDefaultTransform();
        double scaleX = screenInfo.getScaleX();
        double scaleY = screenInfo.getScaleY();
        scaling.getItems().addAll("100%", "125%", "150%", "175%", "200%", "225%", "250%", "300%", "350%");
        scaling.setValue(((int) (screenInfo.getScaleX() * 100)) + "%");
        // Get screen resolution
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth.setText(String.valueOf((int) (screenSize.width * scaleX)));
        addTextFieldListener(screenWidth);
        screenHeight.setText(String.valueOf((int) (screenSize.height * scaleY)));
        addTextFieldListener(screenHeight);
        // Init gamma
        gamma.getItems().addAll("1.8", "2.0", "2.2", "2.4");
        gamma.setValue("2.2");
        // Init capture methods
        captureMethod.getItems().addAll("DDUPL", "WinAPI", "CPU");
        captureMethod.setValue("DDUPL");
        // Init threads
        numberOfThreads.setText("1");
        addTextFieldListener(numberOfThreads);
        // Serial ports, most OSs cap at 256
        serialPort.getItems().add("AUTO");
        for (int i=0; i<=256; i++) {
            serialPort.getItems().add("COM" + i);
        }
        serialPort.setValue("AUTO");
        // Aspect ratio
        aspectRatio.getItems().addAll("FullScreen", "LetterBox");
        aspectRatio.setValue("FullScreen");
        // Form tooltips
        setTooltips();
        // Set focus on the save button
        Platform.runLater(() -> saveButton.requestFocus());

    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {

        Platform.setImplicitExit(false);
        System.out.println("DA"+scaling.getValue());

    }

    /**
     * Cancel button event
     * @param e event
     */
    @FXML
    public void cancel(InputEvent e) {

        Platform.setImplicitExit(false);
        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();

    }

    /**
     * Force TextField to be numeric
     * @param textField numeric fields
     */
    void addTextFieldListener(TextField textField) {

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("\\d*")) return;
            textField.setText(newValue.replaceAll("[^\\d]", ""));
        });

    }

    /**
     * Set form tooltips
     */
    void setTooltips() {

        Tooltip screenWidthTooltip = new Tooltip("Monitor resolution");
        screenWidth.setTooltip(screenWidthTooltip);
        Tooltip screenHeightTooltip = new Tooltip("Monitor resolution");
        screenHeight.setTooltip(screenHeightTooltip);
        Tooltip scalingTooltip = new Tooltip("OS scaling feature, you should not change this setting");
        scaling.setTooltip(scalingTooltip);
        Tooltip gammaTooltip = new Tooltip("Smaller values results in brighter LEDs but less accurate colors");
        gamma.setTooltip(gammaTooltip);
        Tooltip captureMethodTooltip = new Tooltip("If you have a GPU, Desktop Duplication API (DDUPL) is faster than other methods");
        captureMethod.setTooltip(captureMethodTooltip);
        Tooltip numberOfThreadsTooltip = new Tooltip("1 thread is enough when using DDUPL, 3 or more threads are recommended for other capture methods");
        numberOfThreads.setTooltip(numberOfThreadsTooltip);
        Tooltip serialPortTooltip = new Tooltip("AUTO detects first serial port available, change it if you have more than one serial port available");
        serialPort.setTooltip(serialPortTooltip);
        Tooltip aspectRatioTooltip = new Tooltip("LetterBox is recommended for films, you can change this option later");
        aspectRatio.setTooltip(aspectRatioTooltip);
        Tooltip saveButtonTooltip = new Tooltip("You can change this options later");
        saveButton.setTooltip(saveButtonTooltip);

    }

}
