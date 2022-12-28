/*
  InfoController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.utilities.CommonUtility;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * FXML Info Controller
 */
@Slf4j
public class InfoController {

    @FXML private SplitPane splitPane;
    @FXML private Label producerLabel;
    @FXML private Label consumerLabel;
    @FXML private Label wifiLdrLabel;
    @FXML private Label version;
    @FXML private final StringProperty producerValue = new SimpleStringProperty("");
    @FXML private final StringProperty consumerValue = new SimpleStringProperty("");
    @FXML private final StringProperty wifiLdrValue = new SimpleStringProperty("");
    final CategoryAxis xAxisFramerate = new CategoryAxis();
    final NumberAxis yAxisFramerate = new NumberAxis();
    @FXML private LineChart<String, Number> lineChart = new LineChart<>(xAxisFramerate, yAxisFramerate);
    XYChart.Series<String, Number> producingSeries = new XYChart.Series<>();
    XYChart.Series<String, Number> consumingSeries = new XYChart.Series<>();
    final CategoryAxis xAxisWifi = new CategoryAxis();
    final NumberAxis yAxisWifi = new NumberAxis();
    @FXML private LineChart<String, Number> lineChartWifi = new LineChart<>(xAxisWifi, yAxisWifi);
    XYChart.Series<String, Number> wifiSeries = new XYChart.Series<>();
    final int WINDOW_SIZE = 20;
    ScheduledExecutorService scheduledExecutorService;

    @FXML
    protected void initialize() {
        Platform.setImplicitExit(false);
        lineChart.setTitle(CommonUtility.getWord(Constants.INFO_FRAMERATE));
        lineChartWifi.setTitle(CommonUtility.getWord(Constants.INFO_WIFI_STRENGTH));

        lineChart.getData().add(producingSeries);
        lineChart.getData().add(consumingSeries);
        lineChartWifi.getData().add(wifiSeries);

        lineChart.getXAxis().setTickLabelsVisible(false);
        lineChart.getXAxis().setTickMarkVisible(false);
        lineChartWifi.getXAxis().setTickLabelsVisible(false);
        lineChartWifi.getXAxis().setTickMarkVisible(false);

        producerLabel.textProperty().bind(producerValueProperty());
        consumerLabel.textProperty().bind(consumerValueProperty());
        wifiLdrLabel.textProperty().bind(wifiLdrValueProperty());
        version.setText(Constants.INFO_VERSION.replaceAll("VERSION", FireflyLuciferin.version));
        runLater();
        startAnimationTimer();
        if (NativeExecutor.isLinux()) {
            splitPane.setDividerPosition(0,0.4);
        }
    }

    /**
     * Run Later after GUI Init
     */
    private void runLater() {
        Platform.runLater(() -> {
            Stage stage = (Stage) splitPane.getScene().getWindow();
            if (stage != null) {
                stage.setOnCloseRequest(evt -> scheduledExecutorService.shutdownNow());
            }
        });
    }

    /**
     * Manage animation timer to update the UI every seconds
     */
    private void startAnimationTimer() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            String now = LocalDateTime.now().toString();
            producingSeries.getData().add(new XYChart.Data<>(now, FireflyLuciferin.FPS_PRODUCER));
            consumingSeries.getData().add(new XYChart.Data<>(now, FireflyLuciferin.FPS_GW_CONSUMER));
            wifiSeries.getData().add(new XYChart.Data<>(now, CommonUtility.wifiStrength));
            if (producingSeries.getData().size() > WINDOW_SIZE) {
                producingSeries.getData().remove(0);
            }
            if (consumingSeries.getData().size() > WINDOW_SIZE) {
                consumingSeries.getData().remove(0);
            }
            if (wifiSeries.getData().size() > WINDOW_SIZE) {
                wifiSeries.getData().remove(0);
            }
            setProducerValue(CommonUtility.getWord(Constants.INFO_PRODUCING) + FireflyLuciferin.FPS_PRODUCER + Constants.INFO_FPS);
            setConsumerValue(CommonUtility.getWord(Constants.INFO_CONSUMING) + FireflyLuciferin.FPS_GW_CONSUMER + Constants.INFO_FPS);
            String wifiLdr = Constants.INFO_WIFI + CommonUtility.wifiStrength + Constants.PERCENT;
            if (FireflyLuciferin.config.isEnableLDR()) {
                wifiLdr += Constants.INFO_LDR + CommonUtility.ldrStrength + Constants.PERCENT;
            }
            setWifiLdrValue(wifiLdr);
        }), 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Open browser to the GitHub project page
     */
    @FXML
    public void onMouseClickedGitHubLink() {
        FireflyLuciferin.guiManager.surfToURL(Constants.GITHUB_URL);
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

    public StringProperty wifiLdrValueProperty() {
        return wifiLdrValue;
    }

    public void setWifiLdrValue(String wifiValue) {
        this.wifiLdrValue.set(wifiValue);
    }

}
