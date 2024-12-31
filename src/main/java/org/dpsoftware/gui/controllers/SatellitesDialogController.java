/*
  SatellitesDialogController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.util.Map;
import java.util.Objects;

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
        zone.setValue(Enums.SatelliteZone.TOP.getI18n());
        orientation.setValue(Enums.Direction.NORMAL.getI18n());
        algo.setValue(Enums.Algo.AVG_COLOR.getI18n());
        ledNum.setText("1");
        Platform.runLater(() -> {
            for (GlowWormDevice gwd : GuiSingleton.getInstance().deviceTableData) {
                if (!gwd.getDeviceIP().equals(MainSingleton.getInstance().config.getStaticGlowWormIp())
                        && !gwd.getDeviceName().equals(MainSingleton.getInstance().config.getOutputDevice())
                        && MainSingleton.getInstance().config.getSatellites().values().stream().noneMatch(s -> s.getDeviceIp().equals(gwd.getDeviceIP()))) {
                    deviceIp.getItems().add(gwd.getDeviceName() + " (" + gwd.getDeviceIP() + ")");
                }
            }
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
            sat.setZone(LocalizedEnum.fromBaseStr(Enums.SatelliteZone.class, storedSat.getValue().getZone()).getI18n());
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
        for (Enums.Direction or : Enums.Direction.values()) {
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
        return GuiSingleton.getInstance().satellitesTableData;
    }

    /**
     * Set tooltips
     */
    public void setTooltips() {
        deviceIp.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAT_IP));
        zone.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAT_ZONE));
        orientation.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAT_ORIENT));
        ledNum.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAT_NUM));
        algo.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAT_ALGO));
        addButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAT_ADD));
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
            config.getSatellites().clear();
            for (Satellite sat : GuiSingleton.getInstance().satellitesTableData) {
                Satellite updatedSat = new Satellite();
                updatedSat.setLedNum(sat.getLedNum());
                updatedSat.setDeviceIp(sat.getDeviceIp());
                updatedSat.setZone(LocalizedEnum.fromStr(Enums.SatelliteZone.class, sat.getZone()).getBaseI18n());
                updatedSat.setOrientation(LocalizedEnum.fromStr(Enums.Direction.class, sat.getOrientation()).getBaseI18n());
                updatedSat.setAlgo(LocalizedEnum.fromStr(Enums.Algo.class, sat.getAlgo()).getBaseI18n());
                String deviceName = Objects.requireNonNull(GuiSingleton.getInstance().deviceTableData.stream()
                        .filter(s -> s.getDeviceIP().equals(sat.getDeviceIp()))
                        .findFirst()
                        .orElse(null)).getDeviceName();
                updatedSat.setDeviceName(deviceName);
                config.getSatellites().put(updatedSat.getDeviceIp(), updatedSat);
            }
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
        if (Integer.parseInt(ledNum.getText()) <= 0) {
            ledNum.setText("1");
        }
        if (NetworkManager.isValidIp(deviceIp.getValue())) {
            deviceIp.getItems().removeIf(s -> s.contains("(" + deviceIp.getValue() + ")"));
            GuiSingleton.getInstance().satellitesTableData.removeIf(producer -> producer.getDeviceIp().equals(deviceIp.getValue()));
            GuiSingleton.getInstance().satellitesTableData.add(new Satellite(zone.getValue(), orientation.getValue(),
                    ledNum.getText(), deviceIp.getValue(), "", algo.getValue()));
        } else {
            MainSingleton.getInstance().guiManager.showLocalizedNotification(Constants.SAT_ALERT_IP_HEADER,
                    Constants.SAT_ALERT_IP_CONTENT, Constants.SAT_ALERT_IP_TITLE, TrayIcon.MessageType.ERROR);
        }
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        SettingsController.addTextFieldListener(ledNum);
    }

}
