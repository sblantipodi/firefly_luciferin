package org.dpsoftware;

import javafx.fxml.FXML;

import java.io.IOException;

public class SecondaryController {

    @FXML
    private void switchToPrimary() throws IOException {
        FastScreenCapture.setRoot("primary");
    }
}
