/*
  Copyright © 2020 - 2026 Davide Perini (https://github.com/sblantipodi)
  Licensed under the GNU General Public License, version 3 or later.
*/
package org.dpsoftware.gui.controllers.options;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.elements.Satellite;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

class SatelliteSplitZonesTest {

    private static final String SPLIT = "Split main strip";

    private static Configuration config() {
        Configuration config = new Configuration();
        config.setScreenResX(1920);
        config.setScreenResY(1080);
        config.setTopLed(40);
        config.setLeftLed(20);
        config.setRightLed(20);
        config.setBottomLeftLed(10);
        config.setBottomRightLed(10);
        config.setBottomRowLed(20);
        config.setGroupBy(1);
        config.setDefaultLedMatrix(Enums.AspectRatio.FULLSCREEN.getBaseI18n());
        config.regenerateLedMatrix();
        assertEquals(100, config.getLedMatrixInUse(config.getDefaultLedMatrix()).size());
        return config;
    }

    private static void split(Configuration config, int count) {
        SatellitesOptions.apply(config, List.of(new Satellite(SPLIT, Enums.Direction.NORMAL.getBaseI18n(),
                String.valueOf(count), "192.168.1.20", "satellite", Enums.Algo.AVG_COLOR.getBaseI18n())), List.of());
        SatellitesOptions.updateSplitZones(config);
    }

    @Test
    void marksFirst64LedsInEveryAspectRatioAndPreservesTheirOtherFields() {
        Configuration config = config();
        var original = config.getLedMatrix();
        original.values().forEach(matrix -> {
            matrix.get(1).setX(1033);
            matrix.get(1).setActive(false);
            matrix.get(1).setGroupedLed(true);
        });
        split(config, 64);
        config.getLedMatrix().forEach((ratio, matrix) -> {
            assertEquals(64, matrix.values().stream().filter(led -> SPLIT.equals(led.getZone())).count());
            for (int i = 1; i <= 64; i++) assertEquals(SPLIT, matrix.get(i).getZone());
            assertEquals(original.get(ratio).get(65).getZone(), matrix.get(65).getZone());
            assertEquals(1033, matrix.get(1).getX());
            assertFalse(matrix.get(1).isActive());
            assertTrue(matrix.get(1).isGroupedLed());
            assertNotEquals(SPLIT, original.get(ratio).get(1).getZone());
        });
    }

    @Test
    void changingLedCountRebuildsTheDefaultLayoutAndMarksTheNewRange() {
        Configuration config = config();
        Configuration defaults = config();
        split(config, 64);
        config.getLedMatrix().values().forEach(matrix -> matrix.get(30).setX(-123));
        split(config, 20);
        config.getLedMatrix().forEach((ratio, matrix) -> {
            for (int i = 1; i <= 20; i++) assertEquals(SPLIT, matrix.get(i).getZone());
            for (int i = 21; i <= 100; i++) {
                assertEquals(defaults.getLedMatrix().get(ratio).get(i).getZone(), matrix.get(i).getZone());
            }
            assertEquals(defaults.getLedMatrix().get(ratio).get(30).getX(), matrix.get(30).getX());
        });
        split(config, 80);
        config.getLedMatrix().forEach((ratio, matrix) -> {
            for (int i = 1; i <= 80; i++) assertEquals(SPLIT, matrix.get(i).getZone());
            for (int i = 81; i <= 100; i++) {
                assertEquals(defaults.getLedMatrix().get(ratio).get(i).getZone(), matrix.get(i).getZone());
            }
        });
    }

    @Test
    void removalAfterYamlReloadRestoresTheWholeDefaultLedMatrix() throws Exception {
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        Configuration config = config();
        split(config, 64);
        String saved = yaml.writeValueAsString(config);
        assertTrue(saved.contains("Split main strip"));
        assertFalse(saved.contains("originalZone"));
        Configuration restored = yaml.readValue(saved, Configuration.class);
        SatellitesOptions.apply(restored, List.of(), List.of());
        SatellitesOptions.updateSplitZones(restored);
        var tree = yaml.readTree(yaml.writeValueAsString(restored));
        var expected = yaml.readTree(yaml.writeValueAsString(config()));
        assertEquals(expected.path("ledMatrix"), tree.path("ledMatrix"));
    }

    @Test
    void selectingAnotherZoneRemovesSplitMarkers() {
        Configuration config = config();
        Configuration defaults = config();
        split(config, 64);
        config.getSatellites().values().iterator().next().setZone(Enums.PossibleZones.TOP.getBaseI18n());
        SatellitesOptions.updateSplitZones(config);
        config.getLedMatrix().forEach((ratio, matrix) -> {
            for (int i = 1; i <= 100; i++) {
                assertEquals(defaults.getLedMatrix().get(ratio).get(i).getZone(), matrix.get(i).getZone());
            }
        });
    }

    @Test
    void regeneratingTheLayoutRetainsTheConfiguredSplit() {
        Configuration config = config();
        split(config, 64);
        config.setScreenResX(2560);
        config.regenerateLedMatrix();
        config.getLedMatrix().values().forEach(matrix -> {
            assertEquals(SPLIT, matrix.get(64).getZone());
            assertNotEquals(SPLIT, matrix.get(65).getZone());
        });
    }

    @Test
    void localizedChoiceStillStoresTheExactEnglishMarkerWithoutDuplicateOptions() {
        Configuration config = config();
        split(config, 64);
        var main = MainSingleton.getInstance();
        var previous = main.bundle;
        try {
            main.bundle = ResourceBundle.getBundle("messagebundle", Locale.ITALIAN);
            var choices = SatellitesOptions.zones(config).stream().filter(choice -> SPLIT.equals(choice.value())).toList();
            assertEquals(1, choices.size());
            assertEquals("Dividi strip principale", choices.getFirst().label());
            var satellite = config.getSatellites().values().iterator().next();
            satellite.setZone(choices.getFirst().label());
            SatellitesOptions.apply(config, List.of(satellite), List.of());
            assertEquals(SPLIT, config.getSatellites().values().iterator().next().getZone());
        } finally {
            main.bundle = previous;
        }
    }

    @Test
    void rejectsMoreLedsThanTheMatrixContainsWithoutChangingZones() {
        Configuration config = config();
        var original = config.getLedMatrix();
        assertThrows(IllegalArgumentException.class, () -> split(config, 101));
        assertSame(original, config.getLedMatrix());
    }
}
