/*
  LocalizedEnum.java

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
package org.dpsoftware.config;

import org.dpsoftware.utilities.CommonUtility;

import java.util.Arrays;
import java.util.Locale;

/**
 * Interface used for enum localization
 */
public interface LocalizedEnum {

    /**
     * Get a generic localized enum starting from the enum value String
     *
     * @param enumClass       generic enum class
     * @param enumValueString enum String
     * @param baseValue       if true check the Locale.English string, if false get the locale in use
     * @param <E>             enum class type
     * @return specific enum
     */
    static <E extends Enum<E> & LocalizedEnum> E fromStr(Class<E> enumClass, String enumValueString, boolean baseValue) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(genericEnum -> enumValueString.equalsIgnoreCase(baseValue ? genericEnum.getBaseI18n() : genericEnum.getI18n()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a generic localized enum starting from the enum value String
     *
     * @param enumClass       generic enum class
     * @param enumValueString enum String
     * @param <E>             enum class type
     * @return specific enum
     */
    static <E extends Enum<E> & LocalizedEnum> E fromBaseStr(Class<E> enumClass, String enumValueString) {
        return fromStr(enumClass, enumValueString, true);
    }

    /**
     * Get a generic localized enum starting from the enum value String
     *
     * @param enumClass       generic enum class
     * @param enumValueString enum String
     * @param <E>             enum class type
     * @return specifi enum
     */
    static <E extends Enum<E> & LocalizedEnum> E fromStr(Class<E> enumClass, String enumValueString) {
        return fromStr(enumClass, enumValueString, false);
    }

    /**
     * Get a generic enum value
     *
     * @return enum value String
     */
    String getValue();

    /**
     * Get a generic localized enum value
     *
     * @return enum localized String
     */
    default String getI18n() {
        return CommonUtility.getWord(getValue());
    }

    /**
     * Get a generic localized enum value
     *
     * @return enum localized String using Locale.ENGLISH
     */
    default String getBaseI18n() {
        return CommonUtility.getWord(getValue(), Locale.ENGLISH);
    }

}
