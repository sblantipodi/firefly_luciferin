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
        screenWidth.setText(String.valueOf(screenSize.width * scaleX));
        screenHeight.setText(String.valueOf(screenSize.height * scaleY));
        // Init gamma
        gamma.getItems().addAll("1.8", "2.0", "2.2", "2.4");
        gamma.setValue("2.2");
        // Init capture methods
        captureMethod.getItems().addAll("DDUPL", "WinAPI", "CPU");
        captureMethod.setValue("DDUPL");
        // Init threads
        numberOfThreads.setText("3");




    }

    @FXML
    public void save(InputEvent e) {

        Platform.setImplicitExit(false);

        System.out.println("DA"+scaling.getValue());

    }

    @FXML
    public void cancel(InputEvent e) {

        Platform.setImplicitExit(false);

        System.out.println("DA"+scaling.getValue());

    }

}
