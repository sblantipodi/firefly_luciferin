/*
  CommonUtilityTest.java

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
package org.dpsoftware.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import org.dpsoftware.config.Enums;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for pure (singleton-free) methods in {@link CommonUtility}.
 */
class CommonUtilityTest {

    // --- isInteger ---

    @Test
    void isInteger_validNumbers() {
        assertTrue(CommonUtility.isInteger("0"));
        assertTrue(CommonUtility.isInteger("42"));
        assertTrue(CommonUtility.isInteger("-7"));
        assertTrue(CommonUtility.isInteger("2147483647"));  // Integer.MAX_VALUE
        assertTrue(CommonUtility.isInteger("-2147483648")); // Integer.MIN_VALUE
    }

    @Test
    void isInteger_invalidInput() {
        assertFalse(CommonUtility.isInteger(null));
        assertFalse(CommonUtility.isInteger(""));
        assertFalse(CommonUtility.isInteger("abc"));
        assertFalse(CommonUtility.isInteger("12.34"));
        assertFalse(CommonUtility.isInteger(" 42 "));
        assertFalse(CommonUtility.isInteger("2147483648")); // overflow
    }

    // --- capitalize ---

    @Test
    void capitalize_normalString() {
        assertEquals("Hello", CommonUtility.capitalize("hello"));
        assertEquals("World", CommonUtility.capitalize("world"));
    }

    @Test
    void capitalize_edgeCases() {
        assertNull(CommonUtility.capitalize(null));
        assertEquals("", CommonUtility.capitalize(""));
        assertEquals("A", CommonUtility.capitalize("a"));
        assertEquals("Already", CommonUtility.capitalize("already"));
    }

    // --- removeChars ---

    @Test
    void removeChars_mixedString() {
        assertEquals("123", CommonUtility.removeChars("abc123def"));
        assertEquals("456", CommonUtility.removeChars("test-456!@#"));
    }

    @Test
    void removeChars_leadingZeros() {
        assertEquals("5", CommonUtility.removeChars("a005b"));
        assertEquals("0", CommonUtility.removeChars("000")); // (?!$) guard keeps single zero
    }

    @Test
    void removeChars_numbersOnly() {
        assertEquals("42", CommonUtility.removeChars("42"));
    }

    // --- scaleDownResolution / scaleUpResolution ---

    @Test
    void scaleDownResolution_100PercentIsIdentity() {
        assertEquals(1920, CommonUtility.scaleDownResolution(1920, 100));
        assertEquals(1080, CommonUtility.scaleDownResolution(1080, 100));
    }

    @Test
    void scaleDownResolution_200Percent() {
        assertEquals(960, CommonUtility.scaleDownResolution(1920, 200));
    }

    @Test
    void scaleUpResolution_50Percent() {
        assertEquals(960, CommonUtility.scaleUpResolution(1920, 50));
    }

    @Test
    void scaleUpResolution_100PercentIsIdentity() {
        assertEquals(1080, CommonUtility.scaleUpResolution(1080, 100));
    }

    // --- checkMonitorAspectRatio ---

    @Test
    void checkMonitorAspectRatio_4by3() {
        assertEquals(Enums.MonitorAspectRatio.AR_43,
                CommonUtility.checkMonitorAspectRatio(800, 600));
        assertEquals(Enums.MonitorAspectRatio.AR_43,
                CommonUtility.checkMonitorAspectRatio(1024, 768));
    }

    @Test
    void checkMonitorAspectRatio_16by9() {
        assertEquals(Enums.MonitorAspectRatio.AR_169,
                CommonUtility.checkMonitorAspectRatio(1920, 1080));
        assertEquals(Enums.MonitorAspectRatio.AR_169,
                CommonUtility.checkMonitorAspectRatio(1280, 720));
    }

    @Test
    void checkMonitorAspectRatio_21by9() {
        assertEquals(Enums.MonitorAspectRatio.AR_219,
                CommonUtility.checkMonitorAspectRatio(2560, 1080));
    }

    @Test
    void checkMonitorAspectRatio_unknownDefaultsTo169() {
        assertEquals(Enums.MonitorAspectRatio.AR_169,
                CommonUtility.checkMonitorAspectRatio(1000, 1000)); // 1:1 square
    }

    // --- isSplitBottomRow ---

    @Test
    void isSplitBottomRow_zeroPercentReturnsFalse() {
        assertFalse(CommonUtility.isSplitBottomRow("0%"));
    }

    @Test
    void isSplitBottomRow_positivePercentReturnsTrue() {
        assertTrue(CommonUtility.isSplitBottomRow("10%"));
        assertTrue(CommonUtility.isSplitBottomRow("50%"));
    }

    // --- toJsonString / fromJsonToObject ---

    @Test
    void toJsonString_simpleObject() {
        String json = CommonUtility.toJsonString(42);
        assertNotNull(json);
        assertEquals("42", json.trim());
    }

    @Test
    void toJsonString_nullSerializesToJsonLiteral() {
        // Jackson's writeValueAsString(null) returns the JSON literal "null" (a String)
        assertEquals("null", CommonUtility.toJsonString(null));
    }

    @Test
    void fromJsonToObject_validJson() {
        JsonNode node = CommonUtility.fromJsonToObject("{\"key\":\"value\"}");
        assertNotNull(node);
        assertEquals("value", node.get("key").asText());
    }

    @Test
    void fromJsonToObject_invalidJsonReturnsNull() {
        assertNull(CommonUtility.fromJsonToObject("not json"));
    }

    // --- deepClone ---

    @Test
    void deepClone_returnsDifferentInstance() {
        java.util.Map<String, Integer> original = new java.util.HashMap<>();
        original.put("a", 1);
        original.put("b", 2);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Integer> clone = CommonUtility.deepClone(original, java.util.Map.class);
        assertNotSame(original, clone);
        assertEquals(1, clone.get("a"));
        assertEquals(2, clone.get("b"));

        // Modifying clone should not affect original
        clone.put("c", 3);
        assertFalse(original.containsKey("c"));
    }

    // --- isCommonZone ---

    @Test
    void isCommonZone_knownZonesReturnTrue() {
        assertTrue(CommonUtility.isCommonZone("Entire screen"));
        assertTrue(CommonUtility.isCommonZone("Top"));
        assertTrue(CommonUtility.isCommonZone("Bottom"));
        assertTrue(CommonUtility.isCommonZone("Left"));
        assertTrue(CommonUtility.isCommonZone("Right"));
        assertTrue(CommonUtility.isCommonZone("Top left"));
        assertTrue(CommonUtility.isCommonZone("Top right"));
        assertTrue(CommonUtility.isCommonZone("Bottom left"));
        assertTrue(CommonUtility.isCommonZone("Bottom right"));
    }

    @Test
    void isCommonZone_unknownZoneReturnsFalse() {
        assertFalse(CommonUtility.isCommonZone("unknown"));
        assertFalse(CommonUtility.isCommonZone("center"));
        assertFalse(CommonUtility.isCommonZone(""));
    }
}
