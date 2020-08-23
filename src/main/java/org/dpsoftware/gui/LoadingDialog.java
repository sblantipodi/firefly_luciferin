package org.dpsoftware.gui;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoadingDialog {

    private final Stage dialogStage;
    private final ProgressBar progressBar = new ProgressBar();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    public LoadingDialog() {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("LOADING");
        final Label label = new Label();
        label.setText("Please wait...");
        //progressBar.setProgress(-1F);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        //progressIndicator.setProgress(-1F);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        final HBox hb = new HBox();
        hb.setSpacing(5);
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(progressBar, progressIndicator);
        Scene scene = new Scene(hb);
        dialogStage.setScene(scene);
    }

    public void activateProgressBar(final Task task) throws InterruptedException {
        progressBar.progressProperty().bind(task.progressProperty());
        progressIndicator.progressProperty().bind(task.progressProperty());
        dialogStage.show();
    }

    public Stage getDialogStage() {
        return dialogStage;
    }
}