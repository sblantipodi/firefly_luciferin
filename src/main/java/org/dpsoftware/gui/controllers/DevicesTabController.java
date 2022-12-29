/*
  DevicesTabController.java

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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.InputEvent;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

/**
 * Devices Tab controller
 */
@Slf4j
public class DevicesTabController {

    // Inject main controller
    @FXML private SettingsController settingsController;
    // FXML binding
    @FXML public CheckBox checkForUpdates;
    @FXML public CheckBox multiScreenSingleDevice;
    @FXML public Button saveDeviceButton;
    @FXML private TableView<GlowWormDevice> deviceTable;
    @FXML private TableColumn<GlowWormDevice, String> deviceNameColumn;
    @FXML private TableColumn<GlowWormDevice, String> deviceBoardColumn;
    @FXML private TableColumn<GlowWormDevice, Hyperlink> deviceIPColumn;
    @FXML private TableColumn<GlowWormDevice, String> deviceVersionColumn;
    @FXML private TableColumn<GlowWormDevice, String> wifiColumn;
    @FXML private TableColumn<GlowWormDevice, String> macColumn;
    @FXML private TableColumn<GlowWormDevice, String> gpioColumn;
    @FXML private TableColumn<GlowWormDevice, String> firmwareColumn;
    @FXML private TableColumn<GlowWormDevice, String> baudrateColumn;
    @FXML private TableColumn<GlowWormDevice, String> mqttTopicColumn;
    @FXML private TableColumn<GlowWormDevice, String> numberOfLEDSconnectedColumn;
    @FXML private TableColumn<GlowWormDevice, String> colorModeColumn;
    @FXML private TableColumn<GlowWormDevice, String> ldrColumn;
    @FXML private Label versionLabel;
    @FXML public ComboBox<String> powerSaving;
    @FXML public ComboBox<String> multiMonitor;
    @FXML public CheckBox syncCheck;
    public static ObservableList<GlowWormDevice> deviceTableData = FXCollections.observableArrayList();
    public static ObservableList<GlowWormDevice> deviceTableDataTemp = FXCollections.observableArrayList();
    boolean cellEdit = false;
    public static boolean oldFirmwareDevice = false;

    /**
     * Inject main controller containing the TabPane
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
        // Device table
        deviceNameColumn.setCellValueFactory(cellData -> cellData.getValue().deviceNameProperty());
        deviceBoardColumn.setCellValueFactory(cellData -> cellData.getValue().deviceBoardProperty());
        deviceIPColumn.setCellFactory(e -> new TableCell<>() {
            @Override
            protected void updateItem(Hyperlink item, boolean empty) {
                super.updateItem(item, empty);
                final Hyperlink link;
                if (!empty) {
                    GlowWormDevice glowWormDevice = getTableRow().getItem();
                    if (glowWormDevice != null) {
                        link = new Hyperlink(item != null ? item.getText() : glowWormDevice.getDeviceIP());
                        if (glowWormDevice.getWifi().contains(Constants.DASH)) {
                            link.setStyle(Constants.CSS_NO_UNDERLINE + Constants.TC_NO_BOLD_TEXT);
                        } else {
                            link.setOnAction(evt -> FireflyLuciferin.guiManager.surfToURL(Constants.HTTP + getTableRow().getItem().getDeviceIP()));
                        }
                        setGraphic(link);
                    }
                }
            }
        });
        deviceVersionColumn.setCellValueFactory(cellData -> cellData.getValue().deviceVersionProperty());
        wifiColumn.setCellValueFactory(cellData -> cellData.getValue().wifiProperty());
        macColumn.setCellValueFactory(cellData -> cellData.getValue().macProperty());
        gpioColumn.setCellValueFactory(cellData -> cellData.getValue().gpioProperty());
        gpioColumn.setStyle(Constants.TC_BOLD_TEXT + Constants.CSS_UNDERLINE);
        firmwareColumn.setCellValueFactory(cellData -> cellData.getValue().firmwareTypeProperty());
        baudrateColumn.setCellValueFactory(cellData -> cellData.getValue().baudRateProperty());
        mqttTopicColumn.setCellValueFactory(cellData -> cellData.getValue().mqttTopicProperty());
        colorModeColumn.setCellValueFactory(cellData -> cellData.getValue().colorModeProperty());
        ldrColumn.setCellValueFactory(cellData -> cellData.getValue().ldrValueProperty());
        numberOfLEDSconnectedColumn.setCellValueFactory(cellData -> cellData.getValue().numberOfLEDSconnectedProperty());
        deviceTable.setEditable(true);
        deviceTable.setItems(getDeviceTableData());
    }

    /**
     * Init form values
     */
    void initDefaultValues() {
        versionLabel.setText(Constants.FIREFLY_LUCIFERIN + " (v" + FireflyLuciferin.version + ")");
        powerSaving.setValue(Constants.PowerSaving.DISABLED.getI18n());
        multiMonitor.setValue(CommonUtility.getWord(Constants.MULTIMONITOR_1));
        checkForUpdates.setSelected(true);
        syncCheck.setSelected(true);
        multiScreenSingleDevice.setSelected(false);
        DisplayManager displayManager = new DisplayManager();
        multiScreenSingleDevice.setDisable(displayManager.displayNumber() <= 1);
        deviceTable.setPlaceholder(new Label(CommonUtility.getWord(Constants.NO_DEVICE_FOUND)));
    }

    /**
     * Init form values by reading existing config file
     * @param currentConfig stored config
     */
    public void initValuesFromSettingsFile(Configuration currentConfig) {
        versionLabel.setText(Constants.FIREFLY_LUCIFERIN + " (v" + FireflyLuciferin.version + ")");
        if (!currentConfig.getPowerSaving().isEmpty()) {
            powerSaving.setValue(LocalizedEnum.fromBaseStr(Constants.PowerSaving.class, currentConfig.getPowerSaving()).getI18n());
        } else {
            powerSaving.setValue(LocalizedEnum.fromBaseStr(Constants.PowerSaving.class, Constants.PowerSaving.DISABLED.getBaseI18n()).getI18n());
        }
        multiScreenSingleDevice.setDisable(false);
        switch (currentConfig.getMultiMonitor()) {
            case 2 -> multiMonitor.setValue(CommonUtility.getWord(Constants.MULTIMONITOR_2));
            case 3 -> multiMonitor.setValue(CommonUtility.getWord(Constants.MULTIMONITOR_3));
            default -> multiMonitor.setValue(CommonUtility.getWord(Constants.MULTIMONITOR_1));
        }
        DisplayManager displayManager = new DisplayManager();
        multiScreenSingleDevice.setDisable(displayManager.displayNumber() <= 1);
        checkForUpdates.setSelected(currentConfig.isCheckForUpdates());
        multiScreenSingleDevice.setSelected(CommonUtility.isSingleDeviceMultiScreen());
        syncCheck.setSelected(currentConfig.isSyncCheck());
    }

    /**
     * Init combo boxes
     */
    void initComboBox() {
        for (Constants.PowerSaving pwr : Constants.PowerSaving.values()) {
            powerSaving.getItems().add(pwr.getI18n());
        }
    }

    /**
     * Devices Table edit manager
     */
    public void setTableEdit() {
        gpioColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        gpioColumn.setOnEditStart(t -> cellEdit = true);
        gpioColumn.setOnEditCommit(t -> {
            cellEdit = false;
            GlowWormDevice device = t.getTableView().getItems().get(t.getTablePosition().getRow());
            if (t.getNewValue().equals(String.valueOf(2)) || t.getNewValue().equals(String.valueOf(3)) || t.getNewValue().equals(String.valueOf(5))
                    || t.getNewValue().equals(String.valueOf(16))) {
                Optional<ButtonType> result = FireflyLuciferin.guiManager.showLocalizedAlert(Constants.GPIO_OK_TITLE, Constants.GPIO_OK_HEADER,
                        Constants.GPIO_OK_CONTEXT, Alert.AlertType.CONFIRMATION);
                ButtonType button = result.orElse(ButtonType.OK);
                if (button == ButtonType.OK) {
                    log.debug("Setting GPIO" + t.getNewValue() + " on " + device.getDeviceName());
                    device.setGpio(t.getNewValue());
                    if (FireflyLuciferin.guiManager != null) {
                        FireflyLuciferin.guiManager.stopCapturingThreads(true);
                    }
                    if (FireflyLuciferin.config != null && FireflyLuciferin.config.isWifiEnable()) {
                        FirmwareConfigDto gpioDto = new FirmwareConfigDto();
                        gpioDto.setGpio(Integer.parseInt(t.getNewValue()));
                        gpioDto.setMAC(device.getMac());
                        MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FIRMWARE_CONFIG),
                                CommonUtility.toJsonString(gpioDto));
                    } else if (FireflyLuciferin.config != null) {
                        FireflyLuciferin.gpio = Integer.parseInt(t.getNewValue());
                        settingsController.sendSerialParams();
                    }
                }
            } else {
                log.debug("Unsupported GPIO");
                if (NativeExecutor.isWindows()) {
                    FireflyLuciferin.guiManager.showLocalizedNotification(Constants.GPIO_HEADER, Constants.GPIO_CONTEXT, TrayIcon.MessageType.ERROR);
                } else {
                    FireflyLuciferin.guiManager.showLocalizedAlert(Constants.GPIO_TITLE, Constants.GPIO_HEADER, Constants.GPIO_CONTEXT, Alert.AlertType.ERROR);
                }
            }
        });
    }

    /**
     * Manage the device list tab update
     */
    public void manageDeviceList() {
        if (!cellEdit) {
            Calendar calendar = Calendar.getInstance();
            Calendar calendarTemp = Calendar.getInstance();
            ObservableList<GlowWormDevice> deviceTableDataToRemove = FXCollections.observableArrayList();
            deviceTableData.forEach(glowWormDevice -> {
                calendar.setTime(new Date());
                calendarTemp.setTime(new Date());
                calendar.add(Calendar.SECOND, - 20);
                calendarTemp.add(Calendar.SECOND, - 60);
                try {
                    if (calendar.getTime().after(FireflyLuciferin.formatter.parse(glowWormDevice.getLastSeen()))
                            && FireflyLuciferin.formatter.parse(glowWormDevice.getLastSeen()).after(calendarTemp.getTime())) {
                        if (!(Constants.SERIAL_PORT_AUTO.equals(FireflyLuciferin.config.getSerialPort())
                                && FireflyLuciferin.config.getMultiMonitor() > 1) && !oldFirmwareDevice) {
                            deviceTableDataToRemove.add(glowWormDevice);
                        }
                    }
                } catch (ParseException e) {
                    log.error(e.getMessage());
                }
            });
            // Temp list contains the removed devices, they will be readded if a microcontroller restart occurs, and if the capture is runnning.
            deviceTableDataTemp.addAll(deviceTableDataToRemove);
            deviceTableData.removeAll(deviceTableDataToRemove);
            deviceTable.refresh();
        }
    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {
        settingsController.save(e);
    }

    /**
     * Save button from main controller
     * @param config stored config
     */
    @FXML
    public void save(Configuration config) {
        config.setPowerSaving(LocalizedEnum.fromStr(Constants.PowerSaving.class, powerSaving.getValue()).getBaseI18n());
        config.setMultiMonitor(multiMonitor.getSelectionModel().getSelectedIndex() + 1);
        config.setCheckForUpdates(checkForUpdates.isSelected());
        config.setMultiScreenSingleDevice(multiScreenSingleDevice.isSelected());
        config.setSyncCheck(syncCheck.isSelected());
    }

    /**
     * Set red button if a param requires Firefly restart
     */
    @FXML
    public void saveButtonHover() {
        settingsController.checkProfileDifferences();
    }

    /**
     * Set form tooltips
     * @param currentConfig stored config
     */
    void setTooltips(Configuration currentConfig) {
        powerSaving.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_POWER_SAVING));
        multiMonitor.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_MULTIMONITOR));
        checkForUpdates.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_CHECK_UPDATES));
        syncCheck.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SYNC_CHECK));
        if (currentConfig == null) {
            saveDeviceButton.setTooltip(settingsController.createTooltip(Constants.TOOLTIP_SAVEDEVICEBUTTON_NULL));
        }
    }

    /**
     * Return the observable devices list
     * @return devices list
     */
    public ObservableList<GlowWormDevice> getDeviceTableData() {
        return deviceTableData;
    }

    /**
     * Open browser to the GitHub project page
     */
    @FXML
    public void onMouseClickedGitHubLink() {
        FireflyLuciferin.guiManager.surfToURL(Constants.GITHUB_URL);
    }

}
