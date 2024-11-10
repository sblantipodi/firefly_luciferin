package org.dpsoftware.gui;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.utilities.CommonUtility;
import org.purejava.appindicator.AppIndicator;
import org.purejava.appindicator.GCallback;
import org.purejava.appindicator.GObject;
import org.purejava.appindicator.Gtk;

import javax.swing.*;
import java.awt.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.UUID;

import static org.purejava.appindicator.app_indicator_h_1.APP_INDICATOR_CATEGORY_APPLICATION_STATUS;
import static org.purejava.appindicator.app_indicator_h_1.APP_INDICATOR_STATUS_ACTIVE;

@Slf4j
public class TrayIconManagerLinux extends TrayIconBase implements TrayIconManager {

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

        try (var _ = Arena.ofConfined()) {
            var arenaAuto = Arena.ofAuto();
            indicator = AppIndicator.newIndicator(String.valueOf(UUID.randomUUID()), Constants.FIREFLY_LUCIFERIN, APP_INDICATOR_CATEGORY_APPLICATION_STATUS());
            // init tray images
            initializeImages();
            populateTrayWithItems(arenaAuto);








        }


    }

    /**
     * Populate tray with icons
     * @param arenaAuto
     */
    private void populateTrayWithItems(Arena arenaAuto) {

        var gtkMenu = Gtk.newMenu();
        // Start stop menu item
        var startStopItem = Gtk.newMenuItem();
        if (MainSingleton.getInstance().RUNNING || ManagerSingleton.getInstance().pipelineStarting) {
            Gtk.menuItemSetLabel(startStopItem, CommonUtility.getWord(Constants.STOP));
        } else {
            Gtk.menuItemSetLabel(startStopItem, CommonUtility.getWord(Constants.START));
        }
        Gtk.menuShellAppend(gtkMenu, startStopItem);

        // Turn On/Off menu item
        var turnOnOffItem = Gtk.newMenuItem();
        if (MainSingleton.getInstance().config.isToggleLed()) {
            Gtk.menuItemSetLabel(turnOnOffItem, CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_OFF).toLowerCase()));
        } else {
            Gtk.menuItemSetLabel(turnOnOffItem, CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_ON).toLowerCase()));
        }
        Gtk.menuShellAppend(gtkMenu, turnOnOffItem);

        // Aspect Ratio menu item
        populateAspectRatio(gtkMenu);

        // Exit menu item
        var exitItem = Gtk.newMenuItem();
        Gtk.menuItemSetLabel(exitItem, "Exit");
        GObject.signalConnectObject(exitItem, "activate", GCallback.allocate(() -> {
            log.info("Exit");
//            AppIndicator.setIcon(indicator, Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_GREY_CENTER)).getPath());
            NativeExecutor.exit();
        }, arenaAuto), gtkMenu, 0);

        Gtk.menuShellAppend(gtkMenu, exitItem);



        Gtk.widgetShowAll(gtkMenu);




        AppIndicator.setIcon(indicator, Objects.requireNonNull(this.getClass().getResource(Constants.IMAGE_CONTROL_PLAY_CENTER)).getPath());
        AppIndicator.setMenu(indicator, gtkMenu);
        AppIndicator.setTitle(indicator, "Title");
        AppIndicator.setAttentionIcon(indicator, "indicator-messages-new");
        AppIndicator.setStatus(indicator, APP_INDICATOR_STATUS_ACTIVE());



    }

    /**
     * Populate aspect ratio sub menu
     * @param gtkMenu
     */
    private void populateAspectRatio(MemorySegment gtkMenu) {
        var gtkAspectRatioSubmenu = Gtk.newMenu();
        var aspectRatioSubmenuItem = Gtk.newMenuItem();
        Gtk.menuItemSetLabel(aspectRatioSubmenuItem, CommonUtility.getWord(Constants.ASPECT_RATIO));
        var fullScreenItem = Gtk.newMenuItem();
        Gtk.menuItemSetLabel(fullScreenItem, Enums.AspectRatio.FULLSCREEN.getI18n());
        Gtk.menuShellAppend(gtkAspectRatioSubmenu, fullScreenItem);
        var letterboxItem = Gtk.newMenuItem();
        Gtk.menuItemSetLabel(letterboxItem, Enums.AspectRatio.LETTERBOX.getI18n());
        Gtk.menuShellAppend(gtkAspectRatioSubmenu, letterboxItem);
        var pillarboxItem = Gtk.newMenuItem();
        Gtk.menuItemSetLabel(pillarboxItem, Enums.AspectRatio.PILLARBOX.getI18n());
        Gtk.menuShellAppend(gtkAspectRatioSubmenu, pillarboxItem);
        var autoItem = Gtk.newMenuItem();
        Gtk.menuItemSetLabel(autoItem, CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
        Gtk.menuShellAppend(gtkAspectRatioSubmenu, autoItem);
        Gtk.menuItemSetSubmenu(aspectRatioSubmenuItem, gtkAspectRatioSubmenu);
        Gtk.menuShellAppend(gtkMenu, aspectRatioSubmenuItem);
    }

    @Override
    public void populateProfiles() {

    }

    @Override
    public void resetTray() {
        TrayIconManager.super.resetTray();
    }

    @Override
    public Image setTrayIconImage(Enums.PlayerStatus playerStatus) {
        return null;
    }

    @Override
    public TrayIcon getTrayIcon() {
        return trayIcon;
    }

    @Override
    public JMenu getProfilesSubMenu() {
        return profilesSubMenu;
    }

    @Override
    public MemorySegment getIndicator() {
        return indicator;
    }

}
