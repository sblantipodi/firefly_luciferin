/*
  Satellite.java

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
package org.dpsoftware.gui.elements;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A class that map a Glow Worm satellite
 */
@SuppressWarnings("unused")
public class Satellite {

    private final SimpleStringProperty zone = new SimpleStringProperty("");
    private final SimpleStringProperty orientation = new SimpleStringProperty("");
    private final SimpleStringProperty ledNum = new SimpleStringProperty("");
    private final SimpleStringProperty deviceIp = new SimpleStringProperty("");
    private final SimpleStringProperty algo = new SimpleStringProperty("");

    public Satellite() {
        this("", "", "", "", "");
    }

    public Satellite(String zone, String orientation, String ledNum, String deviceIp, String algo) {
        setZone(zone);
        setOrientation(orientation);
        setLedNum(ledNum);
        setDeviceIp(deviceIp);
        setAlgo(algo);
    }

    public String getZone() {
        return zone.get();
    }

    public void setZone(String zoneStr) {
        zone.set(zoneStr);
    }

    public StringProperty zoneProperty() {
        return zone;
    }

    public String getOrientation() {
        return orientation.get();
    }

    public void setOrientation(String orientationStr) {
        orientation.set(orientationStr);
    }

    public StringProperty orientationProperty() {
        return orientation;
    }

    public String getLedNum() {
        return ledNum.get();
    }

    public void setLedNum(String ledNumStr) {
        ledNum.set(ledNumStr);
    }

    public StringProperty ledNumProperty() {
        return ledNum;
    }

    public String getDeviceIp() {
        return deviceIp.get();
    }

    public void setDeviceIp(String deviceIpStr) {
        deviceIp.set(deviceIpStr);
    }

    public StringProperty deviceIpProperty() {
        return deviceIp;
    }

    public String getAlgo() {
        return algo.get();
    }

    public void setAlgo(String algoStr) {
        algo.set(algoStr);
    }

    public StringProperty algoProperty() {
        return algo;
    }

}
