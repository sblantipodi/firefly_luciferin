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

import java.awt.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.UUID;

import static org.dpsoftware.gui.appindicator.app_indicator_h.*;

@Slf4j
public class TrayIconAppIndicator extends TrayIconBase implements TrayIconManager {

    public java.lang.foreign.MemorySegment indicator;
    Arena arena;
    MemorySegment gtkMenu;

    @FunctionalInterface
    public interface Action {
        void execute();
    }

    /**
     *
     * @param action
     */
    public void methodExecutor(Action action) {
        action.execute();
    }

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


            }
        }


    }

    /**
     * Populate tray with icons
     *
     * @param arena
     */
    private void populateTrayWithItems() {
        gtkMenu = gtk_menu_new();
        // Start stop menu item
        var startStopItem = gtk_menu_item_new();
        if (MainSingleton.getInstance().RUNNING || ManagerSingleton.getInstance().pipelineStarting) {
            addMenuItem(gtkMenu, CommonUtility.getWord(Constants.STOP));
        } else {
            addMenuItem(gtkMenu, CommonUtility.getWord(Constants.START));
        }
        // Turn On/Off menu item
        if (MainSingleton.getInstance().config.isToggleLed()) {
            addMenuItem(gtkMenu, CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_OFF).toLowerCase()));
        } else {
            addMenuItem(gtkMenu, CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_ON).toLowerCase()));
        }
        // Aspect Ratio menu item
        populateAspectRatio();
        populateProfiles();
        // Settings menu item
        addMenuItem(gtkMenu, CommonUtility.getWord(Constants.SETTINGS));
        // Info menu item
        addMenuItem(gtkMenu, CommonUtility.getWord(Constants.INFO));
        // Exit menu item
        addMenuItem(gtkMenu, CommonUtility.getWord(Constants.TRAY_EXIT), this::exitAction);


        gtk_widget_show_all(gtkMenu);


//
//        if (MainSingleton.getInstance().communicationError) {
//            trayIcon = new TrayIcon(setTrayIconImage(Enums.PlayerStatus.GREY), tooltipStr);
//        } else if (MainSingleton.getInstance().config.isToggleLed()) {
//            trayIcon = new TrayIcon(setTrayIconImage(Enums.PlayerStatus.STOP), tooltipStr);
//        } else {
//            trayIcon = new TrayIcon(setTrayIconImage(Enums.PlayerStatus.OFF), tooltipStr);
//        }



        app_indicator_set_menu(indicator, gtkMenu);
        app_indicator_set_title(indicator, arena.allocateFrom(getTooltip()));
        app_indicator_set_attention_icon(indicator, arena.allocateFrom("indicator-messages-new"));
        app_indicator_set_status(indicator, 1);

    }

    /**
     * Populate aspect ratio sub menu
     */
    private void populateAspectRatio() {
        var gtkAspectRatioSubmenu = gtk_menu_new();
        var aspectRatioSubmenuItem = gtk_menu_item_new();
        gtk_menu_item_set_label(aspectRatioSubmenuItem, arena.allocateFrom(CommonUtility.getWord(Constants.ASPECT_RATIO)));
        addMenuItem(gtkAspectRatioSubmenu, Enums.AspectRatio.FULLSCREEN.getI18n());
        addMenuItem(gtkAspectRatioSubmenu, Enums.AspectRatio.LETTERBOX.getI18n());
        addMenuItem(gtkAspectRatioSubmenu, Enums.AspectRatio.PILLARBOX.getI18n());
        addMenuItem(gtkAspectRatioSubmenu, CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
        gtk_menu_item_set_submenu(aspectRatioSubmenuItem, gtkAspectRatioSubmenu);
        gtk_menu_shell_append(gtkMenu, aspectRatioSubmenuItem);
    }

    /**
     * Populate profiles submenu
     */
    @Override
    public void populateProfiles() {
        var gtkProfilesSubmenu = gtk_menu_new();
        var profilesSubmenuItem = gtk_menu_item_new();
        gtk_menu_item_set_label(profilesSubmenuItem, arena.allocateFrom(CommonUtility.getWord(Constants.PROFILES)));
        StorageManager sm = new StorageManager();
        for (String profile : sm.listProfilesForThisInstance()) {
            addMenuItem(gtkProfilesSubmenu, profile);
        }
        addMenuItem(gtkProfilesSubmenu, CommonUtility.getWord(Constants.DEFAULT));
        gtk_menu_item_set_submenu(profilesSubmenuItem, gtkProfilesSubmenu);
        gtk_menu_shell_append(gtkMenu, profilesSubmenuItem);
    }

    @Override
    public void resetTray() {
        TrayIconManager.super.resetTray();
    }

    @Override
    public Image setTrayIconImage(Enums.PlayerStatus playerStatus) {
        return null;
    }

    /**
     * @param menu
     * @param label
     */
    private void addMenuItem(MemorySegment menu, String label) {
        var item = gtk_menu_item_new();
        gtk_menu_item_set_label(item, arena.allocateFrom(label));
        gtk_menu_shell_append(menu, item);
    }


    /**
     * @param menu
     * @param label
     * @param action
     */
    private void addMenuItem(MemorySegment menu, String label, Action action) {
        var item = gtk_menu_item_new();
        g_signal_connect_object(item, arena.allocateFrom("activate"), GCallback.allocate(() -> this.methodExecutor(action), arena), menu, 0);
        gtk_menu_item_set_label(item, arena.allocateFrom(label));
        gtk_menu_shell_append(menu, item);
    }

    /**
     * Exit action
     */
    public void exitAction() {
        NativeExecutor.exit();
    }

}