package org.dpsoftware;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;

import java.awt.*;
import java.net.URI;

public class InfoController {

    public Button closeButton;

//    @FXML
//    Label myLabel;
//
//    @FXML
//    void onAction(ActionEvent event) {
//        myLabel.setText("dsdasdasdas");
//    }

    @FXML public void onMouseClickedCloseBtn(InputEvent e) {
        Platform.setImplicitExit(false);
        final Node source = (Node) e.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();
    }

    @FXML public void onMouseClickedGitHubLink(ActionEvent link) {
        Desktop desktop = Desktop.getDesktop();
        try {
            String myUrl = "https://github.com/sblantipodi/JavaFastScreenCapture";
            URI github = new URI(myUrl);
            desktop.browse(github);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
