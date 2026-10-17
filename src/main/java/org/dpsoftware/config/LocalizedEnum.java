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
     * Get a generic localized enum starting from the enum value String.
     * The comparison is case insensitive; if the String does not match any enum localized value, null is returned.
     *
     * @param enumClass       generic enum class
     * @param enumValueString enum String
     * @param baseValue       if true check the Locale.ENGLISH string, if false get the locale in use
     * @param <E>             enum class type
     * @return specific enum, or null if no match is found
     */
    static <E extends Enum<E> & LocalizedEnum> E fromStr(Class<E> enumClass, String enumValueString, boolean baseValue) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(genericEnum -> enumValueString.equalsIgnoreCase(baseValue ? genericEnum.getBaseI18n() : genericEnum.getI18n()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a generic localized enum starting from the base (English) enum value String.
     * Example: "Solid" -> SOLID
     *
     * @param enumClass       generic enum class
     * @param enumValueString base (English) enum String
     * @param <E>             enum class type
     * @return specific enum, or null if no match is found
     */
    static <E extends Enum<E> & LocalizedEnum> E fromBaseStr(Class<E> enumClass, String enumValueString) {
        return fromStr(enumClass, enumValueString, true);
    }

    /**
     * Get a generic localized enum starting from the enum value String in the locale in use.
     * The search is performed only against the resource bundle of the locale currently active in the application,
     * not against all available locales: if the locale in use is Italian, "Solido" matches SOLID, but if the
     * locale in use is German the same String returns null even though it exists in the Italian bundle.
     * Example: "Solido" -> SOLID
     *
     * @param enumClass       generic enum class
     * @param enumValueString localized enum String in the locale in use
     * @param <E>             enum class type
     * @return specific enum, or null if no match is found
     */
    static <E extends Enum<E> & LocalizedEnum> E fromStr(Class<E> enumClass, String enumValueString) {
        return fromStr(enumClass, enumValueString, false);
    }

    /**
     * Get the resource bundle key used to localize this enum constant.
     * Example: SOLID -> "enum.effect.solid"
     *
     * @return enum value String (resource bundle key)
     */
    String getValue();

    /**
     * Get the localized enum value in the locale in use.
     * Example: SOLID -> "Solido"
     *
     * @return enum localized String in the locale in use
     */
    default String getI18n() {
        return CommonUtility.getWord(getValue());
    }

    /**
     * Get the localized enum value in the base (English) locale.
     * Example: SOLID -> "Solid"
     *
     * @return enum localized String using Locale.ENGLISH
     */
    default String getBaseI18n() {
        return CommonUtility.getWord(getValue(), Locale.ENGLISH);
    }

    /**
     * Convert a localized enum text to the base (English) localized text.
     * The input text may be either the localized value in the current locale
     * or the base English value; in both cases the result is the base
     * (English) localized value.
     * Example: "Solido" -> "Solid" or "Solid" -> "Solid"
     *
     * @param enumClass generic enum class
     * @param text      localized text in the current locale or base English text
     * @param <E>       enum class type
     * @return base (English) localized text, or the input text if no enum match is found
     */
    static <E extends Enum<E> & LocalizedEnum> String fromTextToBase(Class<E> enumClass, String text) {
        E localizedEnum = fromStr(enumClass, text, false);
        if (localizedEnum == null) localizedEnum = fromStr(enumClass, text, true);
        return localizedEnum != null ? localizedEnum.getBaseI18n() : text;
    }

}
