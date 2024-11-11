package org.dpsoftware.gui;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.appindicator.AppIndicator;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;
import org.dpsoftware.gui.appindicator.GCallback;

import java.awt.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Locale;
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

    @FunctionalInterface
    public interface ActionInput {
        void execute(String input);
    }

    /**
     *
     * @param action
     */
    public void methodExecutor(Action action) {
        action.execute();
    }

    /**
     *
     * @param action
     */
    public void methodExecutorInput(ActionInput action, String input) {
        action.execute(input);
    }

    /**
     * Manage aspect ratio listener actions
     *
     * @param menuItemText item text
     * @param sendSetCmd   send mqtt msg back
     */
    @Override
    public void manageAspectRatioListener(String menuItemText, boolean sendSetCmd) {
        super.manageAspectRatioListener(menuItemText, sendSetCmd);
        populateTrayWithItems();
    }

    /**
     * Manage Profiles Listener
     *
     * @param menuItemText item text
     */
    @Override
    public void manageProfileListener(String menuItemText) {
        MainSingleton.getInstance().profileArgs = menuItemText;
        setProfileAndRestart(menuItemText);
        MainSingleton.getInstance().profileArgs = menuItemText;
        updateLEDs();
        populateTrayWithItems();
        FireflyLuciferin.setLedNumber(MainSingleton.getInstance().config.getDefaultLedMatrix());
    }

    @Override
    public void updateTray() {
        populateTrayWithItems();
    }

    @Override
    public void initTray() {

        if (AppIndicator.isLoaded()) {
            try (var arenaGlobal = Arena.ofConfined()) {

                arena = Arena.ofAuto();
                indicator = app_indicator_new(arenaGlobal.allocateFrom(String.valueOf(UUID.randomUUID())), arenaGlobal.allocateFrom(Constants.FIREFLY_LUCIFERIN), 0);
                // init tray images
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
            addMenuItem(gtkMenu, CommonUtility.getWord(Constants.STOP), this::stopAction);
        } else {
            addMenuItem(gtkMenu, CommonUtility.getWord(Constants.START), this::startAction);
        }
        // Turn On/Off menu item
        if (MainSingleton.getInstance().config.isToggleLed()) {
            addMenuItem(gtkMenu, CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_OFF).toLowerCase()), this::turnOffAction);
        } else {
            addMenuItem(gtkMenu, CommonUtility.capitalize(CommonUtility.getWord(Constants.TURN_LED_ON).toLowerCase()), this::turnOnAction);
        }
        // Aspect Ratio menu item
        populateAspectRatio();
        populateProfiles();
        // Settings menu item
        addMenuItem(gtkMenu, CommonUtility.getWord(Constants.SETTINGS), this::settingsAction);
        // Info menu item
        addMenuItem(gtkMenu, CommonUtility.getWord(Constants.INFO), this::infoAction);
        // Exit menu item
        addMenuItem(gtkMenu, CommonUtility.getWord(Constants.TRAY_EXIT), this::exitAction);


        gtk_widget_show_all(gtkMenu);


        if (MainSingleton.getInstance().communicationError) {
            setTrayIconImage(Enums.PlayerStatus.GREY);
        } else if (MainSingleton.getInstance().config.isToggleLed()) {
            setTrayIconImage(Enums.PlayerStatus.STOP);
        } else {
            setTrayIconImage(Enums.PlayerStatus.OFF);
        }







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
        addMenuItem(gtkAspectRatioSubmenu, Enums.AspectRatio.FULLSCREEN.getI18n(), this::aspectRatioAction);
        addMenuItem(gtkAspectRatioSubmenu, Enums.AspectRatio.LETTERBOX.getI18n(), this::aspectRatioAction);
        addMenuItem(gtkAspectRatioSubmenu, Enums.AspectRatio.PILLARBOX.getI18n(), this::aspectRatioAction);
        addMenuItem(gtkAspectRatioSubmenu, CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS), this::aspectRatioAction);
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
            addMenuItem(gtkProfilesSubmenu, profile, this::profileAction);
        }
        addMenuItem(gtkProfilesSubmenu, CommonUtility.getWord(Constants.DEFAULT), this::profileAction);
        gtk_menu_item_set_submenu(profilesSubmenuItem, gtkProfilesSubmenu);
        gtk_menu_shell_append(gtkMenu, profilesSubmenuItem);
    }

    @Override
    public void resetTray() {
        TrayIconManager.super.resetTray();
    }

    @Override
    public String setTrayIconImage(Enums.PlayerStatus playerStatus) {
        String imgStr = computeImageToUse(playerStatus);
        app_indicator_set_icon(indicator, arena.allocateFrom(Objects.requireNonNull(this.getClass().getResource(imgStr)).getPath()));
        return imgStr;
    }

    /**
     * @param menu
     * @param label
     * @param action
     */
    private void addMenuItem(MemorySegment menu, String label, Action action) {
        var item = gtk_menu_item_new();
        g_signal_connect_object(item, arena.allocateFrom(Constants.ACTIVATE_EVENT), GCallback.allocate(() -> this.methodExecutor(action), arena), menu, 0);
        gtk_menu_item_set_label(item, arena.allocateFrom(label));
        gtk_menu_shell_append(menu, item);
    }

    /**
     * @param menu
     * @param label
     * @param action
     */
    private void addMenuItem(MemorySegment menu, String label, ActionInput action) {
        var item = gtk_menu_item_new();
        g_signal_connect_object(item, arena.allocateFrom(Constants.ACTIVATE_EVENT), GCallback.allocate(() -> this.methodExecutorInput(action, label), arena), menu, 0);
        gtk_menu_item_set_label(item, arena.allocateFrom(label));
        gtk_menu_shell_append(menu, item);
    }

    /**
     *
     * @param selectedProfile
     */
    public void profileAction(String selectedProfile) {
        super.profileAction(selectedProfile);
        manageAspectRatioListener(selectedProfile, true);
    }

    /**
     *
     * @param selectedAspectRatio
     */
    public void aspectRatioAction(String selectedAspectRatio) {
        manageAspectRatioListener(selectedAspectRatio, true);
    }

    /**
     * Stop action
     */
    public void stopAction() {
        super.stopAction();
        populateTrayWithItems();
    }

    /**
     * Stop action
     */
    public void startAction() {
        super.startAction();
        populateTrayWithItems();
    }

    /**
     * Turn Off action
     */
    public void turnOffAction() {
        super.turnOffAction();
        populateTrayWithItems();
    }

    /**
     * Turn On action
     */
    public void turnOnAction() {
        super.turnOnAction();
        populateTrayWithItems();
    }


}