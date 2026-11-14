package org.dpsoftware.gui.trayicon;

import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TrayIconAwtTest {

    @ParameterizedTest
    @EnumSource(Enums.Theme.class)
    void allThemesContainRequiredTrayColors(Enums.Theme theme) {
        Map<String, Color> colors = TrayIconAwt.loadColors(theme.getCssPath());
        assertFalse(colors.isEmpty(), "Colors should not be empty for theme: " + theme);
        assertNotNull(colors.get("tray_background"), "Missing tray_background for theme: " + theme);
        assertNotNull(colors.get("tray_separator"), "Missing tray_separator for theme: " + theme);
        assertNotNull(colors.get(Constants.CSS_TRAY_ITEM_FOREGROUND), "Missing item foreground for theme: " + theme);
        assertNotNull(colors.get(Constants.CSS_TRAY_ITEM_SELECTIONBACKGROUND), "Missing item selection background for theme: " + theme);
        assertNotNull(colors.get(Constants.CSS_TRAY_ITEM_SELECTIONFOREGROUND), "Missing item selection foreground for theme: " + theme);
        assertNotNull(colors.get(Constants.CSS_TRAY_FOREGROUND), "Missing menu foreground for theme: " + theme);
        assertNotNull(colors.get(Constants.CSS_TRAY_SELECTIONBACKGROUND), "Missing menu selection background for theme: " + theme);
        assertNotNull(colors.get(Constants.CSS_TRAY_SELECTIONFOREGROUND), "Missing menu selection foreground for theme: " + theme);
        assertNotNull(colors.get(Constants.CSS_TRAY_ITEM_TEXT), "Missing item text color for theme: " + theme);
        assertNotNull(colors.get("tray_menu_border"), "Missing menu border color for theme: " + theme);
        assertNotNull(colors.get("tray_menu_item_border"), "Missing item border color for theme: " + theme);
    }

    @Test
    void loadColorsHandlesMissingFileGracefully() {
        Map<String, Color> colors = TrayIconAwt.loadColors("non-existent.css");
        assertNotNull(colors);
        assertTrue(colors.isEmpty());
    }
}
