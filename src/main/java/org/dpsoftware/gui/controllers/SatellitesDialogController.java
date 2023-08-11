/*
  SatellitesDialogController.java

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.TestCanvas;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;

/**
 * Satellite manager dialog controller
 */
@Slf4j
public class SatellitesDialogController {

    public static ObservableList<Satellite> satellitesTableData = FXCollections.observableArrayList();
    @FXML
    public Button okButton;
    @FXML
    public Button applyButton;
    @FXML
    public Button cancelButton;
    @FXML
    public ComboBox<String> zone;
    @FXML
    public TextField zoneStart;
    @FXML
    public TextField zoneEnd;
    @FXML
    public ComboBox<String> orientation;
    @FXML
    public TextField ledNum;
    @FXML
    public ComboBox<String> deviceIp;
    @FXML
    public ComboBox<String> algo;
    @FXML
    private TableView<Satellite> satelliteTable;
    @FXML
    private TableColumn<Satellite, String> zoneColumn;
    @FXML
    private TableColumn<Satellite, String> zoneStartColumn;
    @FXML
    private TableColumn<Satellite, String> zoneEndColumn;
    @FXML
    private TableColumn<Satellite, String> orientationColumn;
    @FXML
    private TableColumn<Satellite, String> ledNumColumn;
    @FXML
    private TableColumn<Satellite, String> deviceIpColumn;
    @FXML
    private TableColumn<Satellite, String> algoColumn;
    // Inject main controller
    @FXML
    private SettingsController settingsController;

    /**
     * Add remove button from table view
     *
     * @return button
     */
    private TableColumn<Satellite, Void> getSatelliteVoidTableColumn() {
        TableColumn<Satellite, Void> colBtn = new TableColumn<>("");
        colBtn.setMaxWidth(Constants.REMOVE_BTN_TABLE);
        Callback<TableColumn<Satellite, Void>, TableCell<Satellite, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Satellite, Void> call(final TableColumn<Satellite, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button(Constants.UNICODE_X);

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            Satellite data = getTableView().getItems().get(getIndex());
                            populateFields(data);
                            satellitesTableData.remove(data);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };
        colBtn.setCellFactory(cellFactory);
        return colBtn;
    }

    /**
     * Populate fields for editing once removing a satellite
     *
     * @param sat satellite
     */
    private void populateFields(Satellite sat) {
        deviceIp.getItems().add("IP (" + sat.getDeviceIp() + ")");
        deviceIp.setValue(sat.getDeviceIp());
        algo.setValue(sat.getAlgo());
        zoneStart.setText(sat.getZoneStart());
        zoneEnd.setText(sat.getZoneEnd());
        ledNum.setText(sat.getLedNum());
        zone.setValue(sat.getZone());
        orientation.setValue(sat.getOrientation());
    }

    /**
     * Inject main controller containing the TabPane
     *
     * @param settingsController TabPane controller
     */
    public void injectSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {
        setNumericTextField();
        deviceIp.valueProperty().addListener((ov, t, t1) -> Platform.runLater(() -> {
            try {
                String str = ov.getValue().substring(ov.getValue().indexOf("(") + 1, ov.getValue().indexOf(")"));
                deviceIp.setValue(str);
            } catch (StringIndexOutOfBoundsException e) {
                deviceIp.setValue(ov.getValue());
            }
        }));
        zone.valueProperty().addListener((ov, t, t1) -> Platform.runLater(() -> {
            getStartEndLeds startEndLeds = getGetStartEndLeds();
            zoneStart.setText(String.valueOf(startEndLeds.start));
            zoneEnd.setText(String.valueOf(startEndLeds.end));
        }));
        initCombos();
        Platform.runLater(() -> {
            for (GlowWormDevice gwd : DevicesTabController.deviceTableData) {
                if (!gwd.getDeviceIP().equals(FireflyLuciferin.config.getStaticGlowWormIp())
                        && !gwd.getDeviceName().equals(FireflyLuciferin.config.getOutputDevice())
                        && FireflyLuciferin.config.getSatellites().values().stream().noneMatch(s -> s.getDeviceIp().equals(gwd.getDeviceIP()))) {
                    deviceIp.getItems().add(gwd.getDeviceName() + " (" + gwd.getDeviceIP() + ")");
                }
            }
            TableColumn<Satellite, Void> colBtn = getSatelliteVoidTableColumn();
            satelliteTable.getColumns().add(0, colBtn);
            zoneColumn.setCellValueFactory(cellData -> cellData.getValue().zoneProperty());
            zoneStartColumn.setCellValueFactory(cellData -> cellData.getValue().zoneStartProperty());
            zoneEndColumn.setCellValueFactory(cellData -> cellData.getValue().zoneEndProperty());
            orientationColumn.setCellValueFactory(cellData -> cellData.getValue().orientationProperty());
            ledNumColumn.setCellValueFactory(cellData -> cellData.getValue().ledNumProperty());
            deviceIpColumn.setCellValueFactory(cellData -> cellData.getValue().deviceIpProperty());
            algoColumn.setCellValueFactory(cellData -> cellData.getValue().algoProperty());
            satelliteTable.setItems(getSatellitesTableData());
            satellitesTableData.clear();
            if (!FireflyLuciferin.config.getSatellites().isEmpty()) {
                satellitesTableData.addAll(FireflyLuciferin.config.getSatellites().values().stream().toList());
            }
            for (int i = 0; i < satellitesTableData.size(); i++) {
                Satellite sat = FXCollections.observableArrayList(satellitesTableData).get(i);
                sat.setZone(LocalizedEnum.fromBaseStr(Enums.SatelliteZone.class, sat.getZone()).getI18n());
                sat.setOrientation(LocalizedEnum.fromBaseStr(Enums.Orientation.class, sat.getOrientation()).getI18n());
                sat.setAlgo(LocalizedEnum.fromBaseStr(Enums.Algo.class, sat.getAlgo()).getI18n());
            }
            satelliteTable.refresh();
        });
        setTooltips();
        zone.setValue(Enums.SatelliteZone.TOP.getI18n());
        orientation.setValue(Enums.Orientation.CLOCKWISE.getI18n());
        algo.setValue(Enums.Algo.AVG_COLOR.getI18n());
        ledNum.setText("0");
        zoneStart.setText("0");
        zoneEnd.setText("0");
    }

    /**
     * Init combos
     */
    private void initCombos() {
        for (Enums.Orientation or : Enums.Orientation.values()) {
            orientation.getItems().add(or.getI18n());
        }
        for (Enums.Algo al : Enums.Algo.values()) {
            algo.getItems().add(al.getI18n());
        }
        for (Enums.SatelliteZone zo : Enums.SatelliteZone.values()) {
            zone.getItems().add(zo.getI18n());
        }
    }

    /**
     * Return the observable satellites list
     *
     * @return satellites list
     */
    public ObservableList<Satellite> getSatellitesTableData() {
        return satellitesTableData;
    }

    /**
     * Set tooltips
     */
    private void setTooltips() {

    }

    /**
     * Close dialog
     *
     * @param e event
     */
    @FXML
    public void close(InputEvent e) {
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save button from main controller
     *
     * @param config stored config
     */
    @FXML
    @SuppressWarnings("Duplicates")
    public void save(Configuration config) {
        FireflyLuciferin.guiManager.stopCapturingThreads(FireflyLuciferin.RUNNING);
        CommonUtility.delaySeconds(() -> FireflyLuciferin.guiManager.startCapturingThreads(), 4);
        config.getSatellites().clear();
        for (int i = 0; i < satellitesTableData.size(); i++) {
            Satellite sat = FXCollections.observableArrayList(satellitesTableData).get(i);
            sat.setZone(LocalizedEnum.fromStr(Enums.SatelliteZone.class, sat.getZone()).getBaseI18n());
            sat.setOrientation(LocalizedEnum.fromStr(Enums.Orientation.class, sat.getOrientation()).getBaseI18n());
            sat.setAlgo(LocalizedEnum.fromStr(Enums.Algo.class, sat.getAlgo()).getBaseI18n());
            config.getSatellites().put(sat.getDeviceIp(), sat);
        }
        FireflyLuciferin.config.getSatellites().clear();
        FireflyLuciferin.config.getSatellites().putAll(config.getSatellites());
    }

    /**
     * Save button from main controller
     *
     * @param e event
     */
    @FXML
    @SuppressWarnings("Duplicates")
    public void save(InputEvent e) {
        settingsController.injectSatellitesController(this);
        settingsController.save(e);
    }

    /**
     * Save and close dialog
     *
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        settingsController.injectSatellitesController(this);
        settingsController.save(e);
        CommonUtility.closeCurrentStage(e);
    }

    /**
     * Save button from main controller
     */
    @FXML
    public void addSatellite() {
        boolean zeroValues = false;
        boolean endGreaterThanStart = false;
        if (Integer.parseInt(zoneStart.getText()) == 0
                || Integer.parseInt(zoneEnd.getText()) == 0
                || Integer.parseInt(zoneStart.getText()) == 0) {
            zeroValues = true;
        }
//        if (Integer.parseInt(zoneEnd.getText()) < Integer.parseInt(zoneStart.getText())) {
//            endGreaterThanStart = true;
//        }

        getStartEndLeds result = getGetStartEndLeds();


        if (!zeroValues && !endGreaterThanStart) {
            deviceIp.getItems().remove("IP (" + deviceIp.getValue() + ")");
            satellitesTableData.removeIf(producer -> producer.getDeviceIp().equals(deviceIp.getValue()));
            satellitesTableData.add(new Satellite(zone.getValue(), String.valueOf(result.start()), String.valueOf(result.end()), orientation.getValue(),
                    ledNum.getText(), deviceIp.getValue(), algo.getValue()));
        } else {
            if (NativeExecutor.isWindows()) {
                FireflyLuciferin.guiManager.showLocalizedNotification(Constants.LDR_ALERT_ENABLED,
                        Constants.TOOLTIP_EYEC_ENABLE_LDR, TrayIcon.MessageType.INFO);
            } else {
                FireflyLuciferin.guiManager.showLocalizedAlert(Constants.LDR_ALERT_TITLE, Constants.LDR_ALERT_ENABLED,
                        Constants.TOOLTIP_EYEC_ENABLE_LDR, Alert.AlertType.INFORMATION);
            }
        }
    }

    private getStartEndLeds getGetStartEndLeds() {
        int start = 0, end = 0;
        if (Enums.Orientation.CLOCKWISE.equals((LocalizedEnum.fromBaseStr(Enums.Orientation.class, orientation.getValue())))) {
            if (Enums.SatelliteZone.RIGHT.equals((LocalizedEnum.fromBaseStr(Enums.SatelliteZone.class, zone.getValue())))) {
                start = 1 + Integer.parseInt(TestCanvas.drawNumLabel(FireflyLuciferin.config, FireflyLuciferin.config.getBottomRightLed()).substring(1));
                end = Integer.parseInt(TestCanvas.drawNumLabel(FireflyLuciferin.config, start + FireflyLuciferin.config.getRightLed()).substring(1)) - 1;
            } else if (Enums.SatelliteZone.LEFT.equals((LocalizedEnum.fromBaseStr(Enums.SatelliteZone.class, zone.getValue())))) {
                int pos = FireflyLuciferin.config.getBottomRightLed() + FireflyLuciferin.config.getRightLed() + FireflyLuciferin.config.getTopLed();
                start = 1 + Integer.parseInt(TestCanvas.drawNumLabel(FireflyLuciferin.config, pos).substring(1));
                end = Integer.parseInt(TestCanvas.drawNumLabel(FireflyLuciferin.config, start + FireflyLuciferin.config.getLeftLed()).substring(1)) - 1;

            } else if (Enums.SatelliteZone.BOTTOM_LEFT.equals((LocalizedEnum.fromBaseStr(Enums.SatelliteZone.class, zone.getValue())))) {
                int pos = FireflyLuciferin.config.getBottomRightLed() + FireflyLuciferin.config.getRightLed() + FireflyLuciferin.config.getTopLed() + FireflyLuciferin.config.getLeftLed();
                start = 1 + Integer.parseInt(TestCanvas.drawNumLabel(FireflyLuciferin.config, pos).substring(1));
                end = Integer.parseInt(TestCanvas.drawNumLabel(FireflyLuciferin.config, start + FireflyLuciferin.config.getBottomLeftLed()).substring(1)) - 1;

            } else if (Enums.SatelliteZone.BOTTOM_RIGHT.equals((LocalizedEnum.fromBaseStr(Enums.SatelliteZone.class, zone.getValue())))) {
                int pos = 1;
                start = Integer.parseInt(TestCanvas.drawNumLabel(FireflyLuciferin.config, pos).substring(1));
                end = Integer.parseInt(TestCanvas.drawNumLabel(FireflyLuciferin.config, start + FireflyLuciferin.config.getBottomRightLed()).substring(1)) - 1;

            }
        }
        return new getStartEndLeds(start, end);
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        SettingsController.addTextFieldListener(zoneStart);
        SettingsController.addTextFieldListener(zoneEnd);
        SettingsController.addTextFieldListener(ledNum);
    }

    private record getStartEndLeds(int start, int end) {
    }

}
