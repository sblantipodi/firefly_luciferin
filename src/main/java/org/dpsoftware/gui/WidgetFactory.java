/*
  WidgetFactory.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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
package org.dpsoftware.gui;

import javafx.scene.control.SpinnerValueFactory;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Constants;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.utilities.CommonUtility;

import java.time.LocalTime;

/**
 * Add some percks to Java FX widgets
 */
public class WidgetFactory {

    /**
     * Time spinner value factory
     * @return usable factory for time spinner that adds and subtract 30 minutes
     */
    public SpinnerValueFactory<LocalTime> timeSpinnerValueFactory(LocalTime storedLocalTime) {

        return new SpinnerValueFactory<>() {

            {
                setValue(storedLocalTime);
            }

            @Override
            public void decrement(int steps) {
                LocalTime value = getValue();
                setValue(value.minusMinutes(30));
                FireflyLuciferin.checkForNightMode();
                setNightBrightness();
            }

            @Override
            public void increment(int steps) {
                LocalTime value = getValue();
                setValue(value.plusMinutes(30));
                FireflyLuciferin.checkForNightMode();
                setNightBrightness();
            }

        };

    }

    /**
     * Night mode value factory
     * @return returns a factory that adds and subtracts 10%
     */
    public SpinnerValueFactory<String> spinnerNightModeValueFactory() {

        return new SpinnerValueFactory<>() {

            {
                setValue(FireflyLuciferin.config != null ? FireflyLuciferin.config.getNightModeBrightness() : Constants.NIGHT_MODE_OFF);
            }

            @Override
            public void decrement(int steps) {
                if (getValue().length() > 2) {
                    String value = getValue().substring(0, 2);
                    setValue(Integer.parseInt(value) - 10 + "%");
                }
                FireflyLuciferin.checkForNightMode();
                setNightBrightness();
            }

            @Override
            public void increment(int steps) {
                String value;
                if (getValue().length() > 2) {
                    value = getValue().substring(0, 2);
                } else {
                    value = getValue().substring(0, 1);

                }
                if (Integer.parseInt(value) < 90) {
                    setValue(Integer.parseInt(value) + 10 + "%");
                }
                FireflyLuciferin.checkForNightMode();
                setNightBrightness();
            }

        };

    }

    /**
     * Set brightness
     */
    private void setNightBrightness() {

        if (FireflyLuciferin.config != null && FireflyLuciferin.config.isWifiEnable() && FireflyLuciferin.config.isToggleLed()) {
            StateDto stateDto = new StateDto();
            stateDto.setState(Constants.ON);
            stateDto.setBrightness(CommonUtility.getNightBrightness());
            stateDto.setWhitetemp(FireflyLuciferin.config.getWhiteTemperature());
            MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.toJsonString(stateDto));
        }

    }

}
