package org.dpsoftware;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class InfoController {

    public Button closeButton;

    @FXML
    private void switchToSecondary() throws IOException {
        FastScreenCapture.setRoot("secondary");
    }

    @FXML public void onMouseClickedCloseBtn(InputEvent e) {
        Platform.setImplicitExit(false);
        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();
    }

}