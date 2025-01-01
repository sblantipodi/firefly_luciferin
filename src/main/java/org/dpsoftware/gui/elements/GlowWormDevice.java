/*
  GlowWormDevice.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.gui.elements;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Hyperlink;
import lombok.Getter;

/**
 * A class that map a device running Glow Worm Luciferin firmware
 */
@SuppressWarnings("unused")
public class GlowWormDevice {

    private final SimpleStringProperty deviceName = new SimpleStringProperty("");
    private final Hyperlink deviceIP = new Hyperlink("");
    private final SimpleStringProperty wifi = new SimpleStringProperty("");
    private final SimpleStringProperty deviceVersion = new SimpleStringProperty("");
    private final SimpleStringProperty deviceBoard = new SimpleStringProperty("");
    private final SimpleStringProperty mac = new SimpleStringProperty("");
    private final SimpleStringProperty gpio = new SimpleStringProperty("");
    private final SimpleStringProperty numberOfLEDSconnected = new SimpleStringProperty("");
    private final SimpleStringProperty lastSeen = new SimpleStringProperty("");
    private final SimpleStringProperty firmwareType = new SimpleStringProperty("");
    private final SimpleStringProperty baudRate = new SimpleStringProperty("");
    private final SimpleStringProperty mqttTopic = new SimpleStringProperty("");
    private final SimpleStringProperty colorMode = new SimpleStringProperty("");
    private final SimpleStringProperty colorOrder = new SimpleStringProperty("");
    private final SimpleStringProperty ldrValue = new SimpleStringProperty("");
    private final SimpleStringProperty ldrPin = new SimpleStringProperty("");
    private final SimpleStringProperty relayPin = new SimpleStringProperty("");
    private final SimpleStringProperty sbPin = new SimpleStringProperty("");
    private final SimpleStringProperty gpioClock = new SimpleStringProperty("");
    @Getter
    private boolean dhcpInUse;

    public GlowWormDevice() {
        this("", "", false, "", "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "");
    }

    public GlowWormDevice(String deviceName, String deviceIP, boolean dhcpInUse, String wifi, String deviceVersion, String deviceBoard,
                          String mac, String gpio, String numberOfLEDSconnected, String lastSeen, String firmwareType,
                          String baudRate, String mqttTopic, String colorMode, String colorOrder, String ldrValue, String relayPin,
                          String sbPin, String ldrPin, String gpioClock) {
        setDeviceName(deviceName);
        setDeviceIP(deviceIP);
        this.dhcpInUse = dhcpInUse;
        setWifi(wifi);
        setDeviceVersion(deviceVersion);
        setDeviceBoard(deviceBoard);
        setMac(mac);
        setGpio(gpio);
        setNumberOfLEDSconnected(numberOfLEDSconnected);
        setLastSeen(lastSeen);
        setFirmwareType(firmwareType);
        setBaudRate(baudRate);
        setMqttTopic(mqttTopic);
        setColorMode(colorMode);
        setColorOrder(colorOrder);
        setLdrValue(ldrValue);
        setRelayPin(relayPin);
        setSbPin(sbPin);
        setLdrPin(ldrPin);
        setGpioClock(gpioClock);
    }

    public String getDeviceName() {
        return deviceName.get();
    }

    public void setDeviceName(String deviceNameStr) {
        deviceName.set(deviceNameStr);
    }

    public StringProperty deviceNameProperty() {
        return deviceName;
    }

    public String getDeviceIP() {
        return deviceIP.getText();
    }

    public void setDeviceIP(String deviceIPStr) {
        deviceIP.setText(deviceIPStr);
    }

    public String deviceIPProperty() {
        return deviceIP.getText();
    }

    public void setDhcpInUse(boolean dhcp) {
        dhcpInUse = dhcp;
    }

    public String getWifi() {
        return wifi.get();
    }

    public void setWifi(String wifiStr) {
        wifi.set(wifiStr);
    }

    public StringProperty wifiProperty() {
        return wifi;
    }

    public String getDeviceVersion() {
        return deviceVersion.get();
    }

    public void setDeviceVersion(String deviceVersionStr) {
        deviceVersion.set(deviceVersionStr);
    }

    public StringProperty deviceVersionProperty() {
        return deviceVersion;
    }

    public String getDeviceBoard() {
        return deviceBoard.get();
    }

    public void setDeviceBoard(String deviceBoardStr) {
        deviceBoard.set(deviceBoardStr);
    }

    public StringProperty deviceBoardProperty() {
        return deviceBoard;
    }

    public String getMac() {
        return mac.get();
    }

    public void setMac(String macStr) {
        mac.set(macStr);
    }

    public StringProperty macProperty() {
        return mac;
    }

    public String getGpio() {
        return gpio.get();
    }

    public void setGpio(String gpioStr) {
        gpio.set(gpioStr);
    }

    public StringProperty gpioProperty() {
        return gpio;
    }

    public String getNumberOfLEDSconnected() {
        return numberOfLEDSconnected.get();
    }

    public void setNumberOfLEDSconnected(String numberOfLEDSconnectedStr) {
        numberOfLEDSconnected.set(numberOfLEDSconnectedStr);
    }

    public StringProperty numberOfLEDSconnectedProperty() {
        return numberOfLEDSconnected;
    }

    public String getLastSeen() {
        return lastSeen.get();
    }

    public void setLastSeen(String lastSeenStr) {
        lastSeen.set(lastSeenStr);
    }

    public StringProperty lastSeenProperty() {
        return lastSeen;
    }

    public String getFirmwareType() {
        return firmwareType.get();
    }

    public void setFirmwareType(String firmwareTypeStr) {
        firmwareType.set(firmwareTypeStr);
    }

    public StringProperty firmwareTypeProperty() {
        return firmwareType;
    }

    public String getBaudRate() {
        return baudRate.get();
    }

    public void setBaudRate(String baudRateStr) {
        baudRate.set(baudRateStr);
    }

    public StringProperty baudRateProperty() {
        return baudRate;
    }

    public String getMqttTopic() {
        return mqttTopic.get();
    }

    public void setMqttTopic(String mqttTopicStr) {
        mqttTopic.set(mqttTopicStr);
    }

    public StringProperty mqttTopicProperty() {
        return mqttTopic;
    }

    public String getColorMode() {
        return colorMode.get();
    }

    public void setColorMode(String colorModeStr) {
        colorMode.set(colorModeStr);
    }

    public String getColorOrder() {
        return colorOrder.get();
    }

    public void setColorOrder(String colorOrderStr) {
        colorOrder.set(colorOrderStr);
    }

    public StringProperty colorModeProperty() {
        return colorMode;
    }

    public StringProperty colorOrderProperty() {
        return colorOrder;
    }

    public String getLdrValue() {
        return ldrValue.get();
    }

    public void setLdrValue(String LdrValue) {
        ldrValue.set(LdrValue);
    }

    public StringProperty ldrValueProperty() {
        return ldrValue;
    }

    public String getRelayPin() {
        return relayPin.get();
    }

    public void setRelayPin(String relayPinStr) {
        relayPin.set(relayPinStr);
    }

    public StringProperty relayPinProperty() {
        return relayPin;
    }

    public String getSbPin() {
        return sbPin.get();
    }

    public void setSbPin(String sbPinStr) {
        sbPin.set(sbPinStr);
    }

    public StringProperty sbPinProperty() {
        return sbPin;
    }

    public String getLdrPin() {
        return ldrPin.get();
    }

    public void setLdrPin(String ldrPinStr) {
        ldrPin.set(ldrPinStr);
    }

    public StringProperty ldrPinProperty() {
        return ldrPin;
    }

    public String getGpioClock() {
        return gpioClock.get();
    }

    public void setGpioClock(String gpioClockStr) {
        gpioClock.set(gpioClockStr);
    }

    public StringProperty gpioClockProperty() {
        return gpioClock;
    }

}
