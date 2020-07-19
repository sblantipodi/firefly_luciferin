package org.dpsoftware;

import javafx.fxml.FXML;
import org.dpsoftware.FastScreenCapture;

import java.io.IOException;

public class SecondaryController {

    @FXML
    private void switchToPrimary() throws IOException {
        FastScreenCapture.setRoot("info");
    }
}
