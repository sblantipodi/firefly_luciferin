/*
  SatellitesDialogController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

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
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.controllers.options.SatellitesOptions;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.util.Map;

/**
 * Satellite manager dialog controller
 */
@Slf4j
public class SatellitesDialogController {

    @FXML
    public Button okButton;
    @FXML
    public Button applyButton;
    @FXML
    public Button cancelButton;
    @FXML
    public Button addButton;
    @FXML
    public ComboBox<String> zone;
    @FXML
    public ComboBox<String> orientation;
    @FXML
    public TextField ledNum;
    @FXML
    public ComboBox<String> deviceIp;
    @FXML
    public ComboBox<String> algo;
    boolean changeInternally;
    @FXML
    private TableView<Satellite> satelliteTable;
    @FXML
    private TableColumn<Satellite, String> zoneColumn;
    @FXML
    private TableColumn<Satellite, String> orientationColumn;
    @FXML
    private TableColumn<Satellite, String> ledNumColumn;
    @FXML
    private TableColumn<Satellite, Hyperlink> deviceIpColumn;
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
                        btn.setOnAction((ActionEvent _) -> {
                            Satellite data = getTableView().getItems().get(getIndex());
                            populateFields(data);
                            GuiSingleton.getInstance().satellitesTableData.remove(data);
                            satelliteTable.refresh();
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
        deviceIp.valueProperty().addListener((ov, _, _) -> Platform.runLater(() -> {
            try {
                String str = ov.getValue().substring(ov.getValue().indexOf("(") + 1, ov.getValue().indexOf(")"));
                deviceIp.setValue(str);
            } catch (StringIndexOutOfBoundsException e) {
                deviceIp.setValue(ov.getValue());
            }
        }));
        initCombos();
        zone.setValue(Enums.PossibleZones.TOP.getI18n());
        orientation.setValue(Enums.Direction.NORMAL.getI18n());
        algo.setValue(Enums.Algo.AVG_COLOR.getI18n());
        ledNum.setText("1");
        Platform.runLater(() -> {
            SatellitesOptions.availableDevices(MainSingleton.getInstance().config, GuiSingleton.getInstance().deviceTableData)
                    .forEach(choice -> deviceIp.getItems().add(choice.label()));
            initTable();
            deviceIp.requestFocus();
        });
    }

    /**
     * Init satellite table
     */
    private void initTable() {
        TableColumn<Satellite, Void> colBtn = getSatelliteVoidTableColumn();
        satelliteTable.getColumns().addFirst(colBtn);
        zoneColumn.setCellValueFactory(cellData -> cellData.getValue().zoneProperty());
        orientationColumn.setCellValueFactory(cellData -> cellData.getValue().orientationProperty());
        ledNumColumn.setCellValueFactory(cellData -> cellData.getValue().ledNumProperty());
        deviceIpColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Hyperlink item, boolean empty) {
                super.updateItem(item, empty);
                final Hyperlink link;
                if (!empty) {
                    Satellite glowWormDevice = getTableRow().getItem();
                    if (glowWormDevice != null) {
                        link = new Hyperlink(item != null ? item.getText() : glowWormDevice.getDeviceIp());
                        link.setOnAction(_ -> MainSingleton.getInstance().guiManager.surfToURL(Constants.HTTP + getTableRow().getItem().getDeviceIp()));
                        setGraphic(link);
                    }
                }
            }
        });
        algoColumn.setCellValueFactory(cellData -> cellData.getValue().algoProperty());
        satelliteTable.setItems(getSatellitesTableData());
        GuiSingleton.getInstance().satellitesTableData.clear();
        for (Map.Entry<String, Satellite> storedSat : MainSingleton.getInstance().config.getSatellites().entrySet()) {
            Satellite sat = new Satellite();
            if (CommonUtility.isCommonZone(storedSat.getValue().getZone())) {
                sat.setZone(LocalizedEnum.fromBaseStr(Enums.PossibleZones.class, storedSat.getValue().getZone()).getI18n());
            } else {
                sat.setZone(storedSat.getValue().getZone());
            }
            sat.setOrientation(LocalizedEnum.fromBaseStr(Enums.Direction.class, storedSat.getValue().getOrientation()).getI18n());
            sat.setAlgo(LocalizedEnum.fromBaseStr(Enums.Algo.class, storedSat.getValue().getAlgo()).getI18n());
            sat.setLedNum(storedSat.getValue().getLedNum());
            sat.setDeviceIp(storedSat.getValue().getDeviceIp());
            GuiSingleton.getInstance().satellitesTableData.add(sat);
        }
        satelliteTable.refresh();
    }

    /**
     * Init combos
     */
    private void initCombos() {
        orientation.getItems().setAll(SatellitesOptions.directions().stream().map(SatellitesOptions.Choice::label).toList());
        algo.getItems().setAll(SatellitesOptions.algorithms().stream().map(SatellitesOptions.Choice::label).toList());
        zone.getItems().setAll(SatellitesOptions.zones(MainSingleton.getInstance().config).stream()
                .map(SatellitesOptions.Choice::label).toList());
    }

    /**
     * Return the observable satellites list
     *
     * @return satellites list
     */
    public ObservableList<Satellite> getSatellitesTableData() {
        return GuiSingleton.getInstance().satellitesTableData;
    }

    /**
     * Set tooltips
     */
    public void setTooltips() {
        GuiManager.createTooltip(LabelKey.TOOLTIP_SAT_IP, deviceIp);
        GuiManager.createTooltip(LabelKey.TOOLTIP_SAT_ZONE, zone);
        GuiManager.createTooltip(LabelKey.TOOLTIP_SAT_ORIENT, orientation);
        GuiManager.createTooltip(LabelKey.TOOLTIP_SAT_NUM, ledNum);
        GuiManager.createTooltip(LabelKey.TOOLTIP_SAT_ALGO, algo);
        GuiManager.createTooltip(LabelKey.TOOLTIP_SAT_ADD, addButton);
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
        if (changeInternally) {
            MainSingleton.getInstance().guiManager.stopCapturingThreads(MainSingleton.getInstance().RUNNING);
            CommonUtility.delaySeconds(() -> MainSingleton.getInstance().guiManager.startCapturingThreads(), 4);
            SatellitesOptions.apply(config, GuiSingleton.getInstance().satellitesTableData,
                    GuiSingleton.getInstance().deviceTableData);
            MainSingleton.getInstance().config.getSatellites().clear();
            MainSingleton.getInstance().config.getSatellites().putAll(config.getSatellites());
        } else {
            config.getSatellites().clear();
            config.getSatellites().putAll(MainSingleton.getInstance().config.getSatellites());
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
        changeInternally = true;
        settingsController.injectSatellitesController(this);
        settingsController.save(e);
        changeInternally = false;
    }

    /**
     * Save and close dialog
     *
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        changeInternally = true;
        settingsController.injectSatellitesController(this);
        settingsController.save(e);
        CommonUtility.closeCurrentStage(e);
        changeInternally = false;
    }

    /**
     * Save button from main controller
     */
    @FXML
    public void addSatellite() {
        ledNum.setText(SatellitesOptions.ledCount(ledNum.getText()));
        deviceIp.commitValue();
        String selectedIp = SatellitesOptions.deviceIp(deviceIp.getValue());
        if (NetworkManager.isValidIp(selectedIp)) {
            deviceIp.getItems().removeIf(s -> s.contains("(" + selectedIp + ")"));
            GuiSingleton.getInstance().satellitesTableData.removeIf(producer -> producer.getDeviceIp().equals(selectedIp));
            GuiSingleton.getInstance().satellitesTableData.add(new Satellite(zone.getValue(), orientation.getValue(),
                    ledNum.getText(), selectedIp, "", algo.getValue()));
        } else {
            MainSingleton.getInstance().guiManager.showLocalizedNotification(LabelKey.SAT_ALERT_IP_HEADER,
                    LabelKey.SAT_ALERT_IP_CONTENT, LabelKey.SAT_ALERT_IP_TITLE, TrayIcon.MessageType.ERROR);
        }
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        SettingsController.addTextFieldListener(ledNum, false);
    }

}
