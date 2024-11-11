package org.dpsoftware.gui;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.appindicator.AppIndicator;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;
import org.dpsoftware.gui.appindicator.GCallback;

import javax.swing.*;
import java.awt.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.UUID;

import static org.dpsoftware.gui.appindicator.app_indicator_h.*;

@Slf4j
public class TrayIconAppIndicator extends TrayIconBase implements TrayIconManager {

    Arena arena;
    public java.lang.foreign.MemorySegment indicator;

    @Override
    public void manageAspectRatioListener(String menuItemText, boolean sendSetCmd) {

    }

    @Override
    public void manageProfileListener(String menuItemText) {

    }

    @Override
    public void updateTray() {

    }

    @Override
    public void initTray() {

        if (AppIndicator.isLoaded()) {
            try (var arenaGlobal = Arena.ofConfined()) {

                arena = Arena.ofAuto();
                indicator = app_indicator_new(arenaGlobal.allocateFrom(String.valueOf(UUID.randomUUID())), arenaGlobal.allocateFrom(Constants.FIREFLY_LUCIFERIN), 0);
                // init tray images
                initializeImages();
                populateTrayWithItems();
                populateProfiles();




            }
        }


    }

    /**
     * Populate tray with icons
     * @param arena
     */
    private void populateTrayWithItems() {

        var gtkMenu = gtk_menu_new();
        // Start stop menu item
        var startStopItem = gtk_menu_item_new();
        if (MainSingleton.getInstance().RUNNING || ManagerSingleton.getInstance().pipelineStarting) {
            gtk_menu_item_set_label(startStopItem, arena.allocateFrom(CommonUtility.getWord(Constants.STOP)));
        } else {
            gtk_menu_item_set_label(startStopItem, arena.allocateFrom(CommonUtility.getWord(Constants.START)));
        }
        gtk_menu_shell_append(gtkMenu, startStopItem);

        // Turn On/Off menu item
        var turnOnOffItem = gtk_menu_item_new();
        if (MainSingleton.getInstance().config.isToggleLed()) {
            gtk_menu_item_set_label(turnOnOffItem, arena.allocateFrom(CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_OFF).toLowerCase())));
        } else {
            gtk_menu_item_set_label(turnOnOffItem, arena.allocateFrom(CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_ON).toLowerCase())));
        }
        gtk_menu_shell_append(gtkMenu, turnOnOffItem);

        // Aspect Ratio menu item
        populateAspectRatio(arena, gtkMenu);

        // Exit menu item
        var exitItem = gtk_menu_item_new();
        gtk_menu_item_set_label(exitItem, arena.allocateFrom("Exit"));
        g_signal_connect_object(exitItem, arena.allocateFrom("activate"), GCallback.allocate(() -> {
            log.info("Exit");
//            AppIndicator.setIcon(indicator, Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_CENTER)).getPath());
            NativeExecutor.exit();
        }, arena), gtkMenu, 0);

        gtk_menu_shell_append(gtkMenu, exitItem);



        gtk_widget_show_all(gtkMenu);




        app_indicator_set_icon(indicator, arena.allocateFrom(Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_CENTER)).getPath()));
        app_indicator_set_menu(indicator, gtkMenu);
        app_indicator_set_title(indicator, arena.allocateFrom("Title"));
        app_indicator_set_attention_icon(indicator, arena.allocateFrom("indicator-messages-new"));
        app_indicator_set_status(indicator, 1);


    }

    /**
     * Populate aspect ratio sub menu
     *
     * @param arena
     * @param gtkMenu
     */
    private void populateAspectRatio(Arena arena, MemorySegment gtkMenu) {
        var gtkAspectRatioSubmenu = gtk_menu_new();
        var aspectRatioSubmenuItem = gtk_menu_item_new();
        gtk_menu_item_set_label(aspectRatioSubmenuItem, arena.allocateFrom(CommonUtility.getWord(Constants.ASPECT_RATIO)));
        var fullScreenItem = gtk_menu_item_new();
        gtk_menu_item_set_label(fullScreenItem, arena.allocateFrom(Enums.AspectRatio.FULLSCREEN.getI18n()));
        gtk_menu_shell_append(gtkAspectRatioSubmenu, fullScreenItem);
        var letterboxItem = gtk_menu_item_new();
        gtk_menu_item_set_label(letterboxItem, arena.allocateFrom(Enums.AspectRatio.LETTERBOX.getI18n()));
        gtk_menu_shell_append(gtkAspectRatioSubmenu, letterboxItem);
        var pillarboxItem = gtk_menu_item_new();
        gtk_menu_item_set_label(pillarboxItem, arena.allocateFrom(Enums.AspectRatio.PILLARBOX.getI18n()));
        gtk_menu_shell_append(gtkAspectRatioSubmenu, pillarboxItem);
        var autoItem = gtk_menu_item_new();
        gtk_menu_item_set_label(autoItem, arena.allocateFrom(CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS)));
        gtk_menu_shell_append(gtkAspectRatioSubmenu, autoItem);
        gtk_menu_item_set_submenu(aspectRatioSubmenuItem, gtkAspectRatioSubmenu);
        gtk_menu_shell_append(gtkMenu, aspectRatioSubmenuItem);
    }

    /**
     * Populate profiles submenu
     */
    @Override
    public void populateProfiles() {
//        StorageManager sm = new StorageManager();
//        int index = 0;
//        for (String profile : sm.listProfilesForThisInstance()) {
//            profilesSubMenu.add(createMenuItem(profile), index++);
//        }
//        profilesSubMenu.add(createMenuItem(CommonUtility.getWord(Constants.DEFAULT)));
    }

    @Override
    public void resetTray() {
        TrayIconManager.super.resetTray();
    }

    @Override
    public Image setTrayIconImage(Enums.PlayerStatus playerStatus) {
        return null;
    }

}
