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
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.utilities.CommonUtility;

/**
 * Satellite manager dialog controller
 */
@Slf4j
public class SatellitesDialogController {

    public static ObservableList<Satellite> satellitesTableData = FXCollections.observableArrayList();
    @FXML
    public Button okButton;
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
    private static TableColumn<Satellite, Void> getSatelliteVoidTableColumn() {
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
        deviceIp.valueProperty().addListener((ov, t, t1) -> Platform.runLater(() -> {
            try {
                String str = ov.getValue().substring(ov.getValue().indexOf("(") + 1, ov.getValue().indexOf(")"));
                deviceIp.setValue(str);
            } catch (StringIndexOutOfBoundsException e) {
                deviceIp.setValue(ov.getValue());
            }
        }));
        initCombos();
        Platform.runLater(() -> {
            for (GlowWormDevice gwd : DevicesTabController.deviceTableData) {
                if (!gwd.getDeviceIP().equals(FireflyLuciferin.config.getStaticGlowWormIp())
                        && !gwd.getDeviceName().equals(FireflyLuciferin.config.getOutputDevice())) {
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
            satellitesTableData.addAll(FireflyLuciferin.config.getSatellites().values().stream().toList());
            for (int i = 0; i < satellitesTableData.size(); i++) {
                Satellite sat = FXCollections.observableArrayList(satellitesTableData).get(i);
                sat.setZone(LocalizedEnum.fromBaseStr(Enums.SatelliteZone.class, sat.getZone()).getI18n());
                sat.setOrientation(LocalizedEnum.fromBaseStr(Enums.Orientation.class, sat.getOrientation()).getI18n());
                sat.setAlgo(LocalizedEnum.fromBaseStr(Enums.Algo.class, sat.getAlgo()).getI18n());
            }
            satelliteTable.refresh();
        });
        setTooltips();
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
        config.getSatellites().clear();
        for (int i = 0; i < satellitesTableData.size(); i++) {
            Satellite sat = FXCollections.observableArrayList(satellitesTableData).get(i);
            sat.setZone(LocalizedEnum.fromStr(Enums.SatelliteZone.class, sat.getZone()).getBaseI18n());
            sat.setOrientation(LocalizedEnum.fromStr(Enums.Orientation.class, sat.getOrientation()).getBaseI18n());
            sat.setAlgo(LocalizedEnum.fromStr(Enums.Algo.class, sat.getAlgo()).getBaseI18n());
            config.getSatellites().put(i, sat);
        }
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
     * Save button from main controller
     */
    @FXML
    public void addSatellite() {
        satellitesTableData.add(new Satellite(zone.getValue(), zoneStart.getText(), zoneEnd.getText(), orientation.getValue(),
                ledNum.getText(), deviceIp.getValue(), algo.getValue()));
    }

}
