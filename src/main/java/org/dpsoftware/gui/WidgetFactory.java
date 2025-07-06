/*
  WidgetFactory.java

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
package org.dpsoftware.gui;

import javafx.scene.control.SpinnerValueFactory;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;

import java.time.LocalTime;

/**
 * Add some percks to Java FX widgets
 */
@Slf4j
public class WidgetFactory {

    /**
     * Time spinner value factory
     *
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
            }

            @Override
            public void increment(int steps) {
                LocalTime value = getValue();
                setValue(value.plusMinutes(30));
                FireflyLuciferin.checkForNightMode();
            }
        };
    }

    /**
     * Night mode value factory
     *
     * @return returns a factory that adds and subtracts 10%
     */
    public SpinnerValueFactory<String> spinnerNightModeValueFactory() {
        return new SpinnerValueFactory<>() {
            {
                setValue(MainSingleton.getInstance().config != null ? MainSingleton.getInstance().config.getNightModeBrightness() : Constants.PERCENTAGE_OFF);
            }

            @Override
            public void decrement(int steps) {
                if (getValue().length() > 2) {
                    String value = getValue().substring(0, 2);
                    setValue(Integer.parseInt(value) - 10 + "%");
                }
                FireflyLuciferin.checkForNightMode();
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
            }
        };
    }

    /**
     * Luminosity threshold value factory
     *
     * @return returns a factory that adds and subtracts 1%
     */
    public SpinnerValueFactory<String> luminosityThresholdValueFactory() {
        return new SpinnerValueFactory<>() {
            {
                setValue(MainSingleton.getInstance().config != null ? MainSingleton.getInstance().config.getLuminosityThreshold() + Constants.PERCENT : Constants.PERCENTAGE_OFF);
            }

            @Override
            public void decrement(int steps) {
                String value;
                if (getValue().length() > 2) {
                    value = getValue().substring(0, 2);
                    setValue(Integer.parseInt(value) - 1 + "%");
                    MainSingleton.getInstance().config.setLuminosityThreshold(Integer.parseInt(value) - 1);
                } else {
                    value = getValue().substring(0, 1);
                    if (Integer.parseInt(value) > 0) {
                        setValue(Integer.parseInt(value) - 1 + "%");
                        MainSingleton.getInstance().config.setLuminosityThreshold(Integer.parseInt(value) - 1);
                    }
                }
            }

            @Override
            public void increment(int steps) {
                String value;
                if (getValue().length() > 2) {
                    value = getValue().substring(0, 2);
                } else {
                    value = getValue().substring(0, 1);
                }
                if (Integer.parseInt(value) < 50) {
                    setValue(Integer.parseInt(value) + 1 + "%");
                    MainSingleton.getInstance().config.setLuminosityThreshold(Integer.parseInt(value) + 1);
                }
            }
        };
    }

    /**
     * nightLightLvl value factory
     *
     * @return returns a factory that adds and subtracts 1
     */
    public SpinnerValueFactory<Integer> nightLightLvlValueFactory() {
        return new SpinnerValueFactory<>() {
            {
                setValue(MainSingleton.getInstance().config != null ? MainSingleton.getInstance().config.getNightLightLvl() : 1);
            }

            @Override
            public void decrement(int steps) {
                if (getValue() > 1) {
                    setValue(getValue() - 1);
                    MainSingleton.getInstance().config.setNightLightLvl(getValue());
                }
            }

            @Override
            public void increment(int steps) {
                if (getValue() < 10) {
                    setValue(getValue() + 1);
                    MainSingleton.getInstance().config.setNightLightLvl(getValue());
                }
            }
        };
    }

}
