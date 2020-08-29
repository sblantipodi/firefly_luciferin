/*
  GlowWormDevice.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/
package org.dpsoftware.gui.elements;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GlowWormDevice {
    private final SimpleStringProperty deviceName = new SimpleStringProperty("");
    private final SimpleStringProperty deviceIP = new SimpleStringProperty("");
    private final SimpleStringProperty deviceVersion = new SimpleStringProperty("");

    public GlowWormDevice() {
        this("", "", "");
    }

    public GlowWormDevice(String deviceName, String deviceIP, String deviceVersion) {
        setDeviceName(deviceName);
        setDeviceIP(deviceIP);
        setDeviceVersion(deviceVersion);
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
        return deviceIP.get();
    }

    public void setDeviceIP(String deviceIPStr) {
        deviceIP.set(deviceIPStr);
    }

    public StringProperty deviceIPProperty() {
        return deviceIP;
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

}
