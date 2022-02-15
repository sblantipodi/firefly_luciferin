/*
  LocalizedEnum.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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
package org.dpsoftware.config;

import org.dpsoftware.utilities.CommonUtility;

import java.util.Arrays;
import java.util.Locale;

/**
 * Interface used for enum localization
 */
public interface LocalizedEnum {

    /**
     * Get a generic enum value
     * @return enum value String
     */
    String getValue();

    /**
     * Get a generic localized enum value
     * @return enum localized String
     */
    default String getLocalizedValue() {
        return CommonUtility.getWord(getValue());
    }

    /**
     * Get a generic localized enum value
     * @return enum localized String using Locale.ENGLISH
     */
    default String getLocalizedBaseValue() {
        return CommonUtility.getWord(getValue(), Locale.ENGLISH);
    }

    /**
     * Get a generic localized enum starting from the enum value String
     * @param enumClass
     * @param enumValueString
     * @param baseValue
     * @param <E>
     * @return
     */
    static <E extends Enum<E> & LocalizedEnum> E fromString(Class<E> enumClass, String enumValueString, boolean baseValue) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(framerate -> enumValueString.equalsIgnoreCase(baseValue ? framerate.getLocalizedBaseValue() : framerate.getLocalizedValue()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a generic localized enum starting from the enum value String
     * @param enumClass
     * @param enumValueString
     * @param <E>
     * @return
     */
    static <E extends Enum<E> & LocalizedEnum> E fromBaseString(Class<E> enumClass, String enumValueString) {
        return fromString(enumClass, enumValueString, true);
    }

    /**
     * Get a generic localized enum starting from the enum value String
     * @param enumClass
     * @param enumValueString
     * @param <E>
     * @return
     */
    static <E extends Enum<E> & LocalizedEnum> E fromString(Class<E> enumClass, String enumValueString) {
        return fromString(enumClass, enumValueString, false);
    }

}
