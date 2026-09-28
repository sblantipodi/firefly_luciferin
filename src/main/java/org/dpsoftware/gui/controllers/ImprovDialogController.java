/*
  ImprovDialogController.java

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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.controllers.options.ImprovOptions;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Dialog controller for the IMPROV WiFi protocol management
 */
@Slf4j
public class ImprovDialogController {

    @FXML
    public ComboBox<String> ssid;
    @FXML
    public PasswordField wifiPwd;
    @FXML
    public ComboBox<String> baudrate;
    @FXML
    public ComboBox<String> comPort;
    @FXML
    public Button okButton;
    @FXML
    public Button cancelButton;
    @FXML
    public ComboBox<String> ethCombo;
    @FXML
    public ComboBox<String> ethSelCombo;
    @FXML
    public TextField mi;
    @FXML
    public TextField mo;
    @FXML
    public TextField sck;
    @FXML
    public TextField cs;
    @FXML
    public TextField deviceName;
    @FXML
    private SettingsController settingsController;
    @FXML
    private HBox spiBox;

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
        Platform.runLater(() -> {
            if (NativeExecutor.isWindows()) {
                initSsid();
            }
            baudrate.getItems().addAll(ImprovOptions.baudRates());
            comPort.getItems().addAll(ImprovOptions.serialPorts());
            if (comPort != null && !comPort.getItems().isEmpty()) {
                comPort.setValue(comPort.getItems().getFirst());
            }
            deviceName.setText(MainSingleton.getInstance().config.getOutputDevice());
            baudrate.setValue(Enums.BaudRate.BAUD_RATE_115200.getBaudRate());
            initDefaultValues();
            ethCombo.valueProperty().addListener((_, _, newVal) -> {
                log.debug(newVal);
                if (newVal.equals(Enums.EthernetOptions.ETH_NO_ETH.getI18n())) {
                    ethSelCombo.setVisible(true);
                    ethSelCombo.setDisable(true);
                    spiBox.setVisible(false);
                } else if (newVal.equals(Enums.EthernetOptions.ETH_CUSTOM_SPI.getI18n())) {
                    ethSelCombo.setVisible(false);
                    spiBox.setVisible(true);
                } else if (newVal.equals(Enums.EthernetOptions.ETH_PREBUILT.getI18n())) {
                    ethSelCombo.setVisible(true);
                    ethSelCombo.setDisable(false);
                    spiBox.setVisible(false);
                }
            });
            setNumericTextField();
        });
    }

    /**
     * Set tooltips
     */
    private void setTooltips() {
        GuiManager.createTooltip(LabelKey.TOOLTIP_IMPROV_SSID, ssid);
        GuiManager.createTooltip(LabelKey.TOOLTIP_IMPROV_PWD, wifiPwd);
        GuiManager.createTooltip(LabelKey.TOOLTIP_IMPROV_COM, comPort);
        GuiManager.createTooltip(LabelKey.TOOLTIP_IMPROV_BAUD, baudrate);
        GuiManager.createTooltip(LabelKey.TOOLTIP_DEV_NAME, deviceName);
        GuiManager.createTooltip(LabelKey.TOOLTIP_ETHERNET, ethCombo);
        GuiManager.createTooltip(LabelKey.TOOLTIP_ETHERNET, ethSelCombo);
        GuiManager.createTooltip(LabelKey.TOOLTIP_ETHERNET, ethSelCombo);
        GuiManager.createTooltip(LabelKey.TOOLTIP_ETHERNET, mi);
        GuiManager.createTooltip(LabelKey.TOOLTIP_ETHERNET, mo);
        GuiManager.createTooltip(LabelKey.TOOLTIP_ETHERNET, sck);
        GuiManager.createTooltip(LabelKey.TOOLTIP_ETHERNET, cs);
    }

    /**
     * Init default values
     */
    public void initDefaultValues() {
        for (Enums.EthernetOptions ethOption : Enums.EthernetOptions.values()) {
            ethCombo.getItems().add(ethOption.getI18n());
        }
        ethCombo.setValue(Enums.EthernetOptions.ETH_NO_ETH.getI18n());
        for (Enums.EthernetBoards ethBoard : Enums.EthernetBoards.values()) {
            ethSelCombo.getItems().add(ethBoard.getValue());
        }
        ethSelCombo.setValue(Enums.EthernetBoards.ETH_BOARD_GLEDOPTO.getI18n());
    }

    /**
     * Init form values by reading existing config file
     */
    public void initValuesFromSettingsFile() {
        setTooltips();
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
     * Save and close dialog
     *
     * @param e event
     */
    @FXML
    public void saveAndClose(InputEvent e) {
        ssid.commitValue();
        wifiPwd.commitValue();
        baudrate.commitValue();
        comPort.commitValue();
        if (isFormValid()) {
            manageImprov();
            CommonUtility.closeCurrentStage(e);
        }
    }

    /**
     * Check if form is valid
     *
     * @return true if form is valid
     */
    private boolean isFormValid() {
        String errorKey = ImprovOptions.validationError(provisionRequest());
        if (errorKey != null) {
            MainSingleton.getInstance().guiManager.showLocalizedNotification(
                    CommonUtility.getWord(LabelKey.FIRMWARE_PROVISION_NOTIFY),
                    CommonUtility.getWord(errorKey),
                    Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.ERROR);
            return false;
        }
        return true;
    }

    /**
     * Captures the values currently entered in the JavaFX provisioning dialog.
     *
     * @return values to validate and send to the device
     */
    private ImprovOptions.ProvisionRequest provisionRequest() {
        String mode = Enums.EthernetOptions.ETH_NO_ETH.name();
        if (ethCombo.getValue().equals(Enums.EthernetOptions.ETH_CUSTOM_SPI.getI18n())) {
            mode = Enums.EthernetOptions.ETH_CUSTOM_SPI.name();
        } else if (ethCombo.getValue().equals(Enums.EthernetOptions.ETH_PREBUILT.getI18n())) {
            mode = Enums.EthernetOptions.ETH_PREBUILT.name();
        }
        return new ImprovOptions.ProvisionRequest(
                ssid.getValue(), wifiPwd.getText(), deviceName.getText(), comPort.getValue(), baudrate.getValue(),
                mode, ethSelCombo.getValue(), mi.getText(), mo.getText(), sck.getText(), cs.getText(),
                MainSingleton.getInstance().config.isMqttEnable(),
                settingsController.networkTabController.mqttHost.getText(),
                settingsController.networkTabController.mqttPort.getText(),
                settingsController.networkTabController.mqttUser.getText(),
                settingsController.networkTabController.mqttPwd.getText());
    }

    /**
     * Starts the shared serial provisioning operation and reports a failed attempt.
     */
    private void manageImprov() {
        ImprovOptions.provision(provisionRequest()).thenAccept(sent -> {
            if (!sent) {
                MainSingleton.getInstance().guiManager.showLocalizedNotification(
                        CommonUtility.getWord(LabelKey.FIRMWARE_PROVISION_NOTIFY),
                        CommonUtility.getWord(LabelKey.FIRMWARE_PROVISION_NOTIFY_HEADER),
                        Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.ERROR);
            }
        });
    }

    /**
     * @deprecated
     * Send improv wifi msg, this is the classic improv wifi protocol
     *
     * @return error
     * @throws IOException can't open port
     */
    @SuppressWarnings("unused")
    @Deprecated
    private boolean sendImprov() throws IOException {
        byte version = 0x01;
        byte rpcPacketType = 0x03;
        byte rpcCommandType = 0x01;
        byte[] ssidBytes = ssid.getValue().getBytes(StandardCharsets.UTF_8);
        byte[] passBytes = wifiPwd.getText().getBytes(StandardCharsets.UTF_8);
        int dataLen = 1 + ssidBytes.length + 1 + passBytes.length;
        int packetLen = Constants.IMPROV_HEADER.length + 1 + 1 + 1 + 1 + 1 + dataLen + 1;
        byte[] packet = new byte[packetLen];
        int idx = 0;
        // Header
        for (byte b : Constants.IMPROV_HEADER) packet[idx++] = b;
        packet[idx++] = version;
        packet[idx++] = rpcPacketType;
        packet[idx++] = (byte) (dataLen + 2);
        packet[idx++] = rpcCommandType;
        // Data
        packet[idx++] = (byte) (ssidBytes.length + passBytes.length);
        packet[idx++] = (byte) ssidBytes.length;
        System.arraycopy(ssidBytes, 0, packet, idx, ssidBytes.length);
        idx += ssidBytes.length;
        packet[idx++] = (byte) passBytes.length;
        System.arraycopy(passBytes, 0, packet, idx, passBytes.length);
        idx += passBytes.length;
        // Checksum
        int checksum = 0;
        for (int i = 0; i < idx; i++) {
            checksum += packet[i] & 0xFF;
        }
        byte checksumByte = (byte) (checksum & 0xFF);
        packet[idx] = checksumByte;
        if (MainSingleton.getInstance().output != null) {
            log.debug("Improv WiFi packet sent");
            MainSingleton.getInstance().improvActive = deviceName.getText();
            MainSingleton.getInstance().output.write(packet);
            return true;
        }
        return false;
    }

    /**
     * Send improv wifi msg with a custom DPsoftware protocol
     *
     * @return error
     * @throws IOException can't open port
     */
    /**
     * Save button from main controller
     *
     */
    @FXML
    @SuppressWarnings("Duplicates")
    public void save() {
    }

    /**
     * Initialize the SSID combo box with WLAN SSID
     */
    private void initSsid() {
        ssid.getItems().setAll(ImprovOptions.wifiSsids());
    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {
        SettingsController.addTextFieldListener(mi, true);
        SettingsController.addTextFieldListener(mo, true);
        SettingsController.addTextFieldListener(sck, true);
        SettingsController.addTextFieldListener(cs, true);
    }

}
