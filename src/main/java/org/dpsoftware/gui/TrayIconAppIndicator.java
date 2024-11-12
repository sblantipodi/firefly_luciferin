/*
  TrayIconAppIndicator.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.gui;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.bindings.appindicator.GCallback;
import org.dpsoftware.gui.bindings.notify.LibNotify;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

import static org.dpsoftware.gui.bindings.appindicator.app_indicator_h.*;
import static org.dpsoftware.gui.bindings.notify.notify_h.*;

/**
 * This class manages the LibAppIndicator tray icon features
 * using libappindicator e libayatana-appindicator.
 * Bindings are generated with jextract. See LibAppIndicator.java for more infos on it.
 */
@Slf4j
public class TrayIconAppIndicator extends TrayIconBase implements TrayIconManager {

    public java.lang.foreign.MemorySegment indicator;
    Arena arena;
    MemorySegment gtkMenu;

    /**
     * Execute callback with no input param
     *
     * @param action callback
     */
    public void methodExecutor(Action action) {
        action.execute();
    }

    /**
     * Execute callback with input param
     *
     * @param action callback
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

    /**
     * Udpate tray icon with new profiles
     */
    @Override
    public void updateTray() {
        populateTrayWithItems();
    }

    /**
     * Create and initialize tray icon menu
     */
    @Override
    public void initTray() {


        if (LibNotify.isSupported()) {
            try (var arenaGlobal = Arena.ofConfined()) {
                notify_init(arenaGlobal.allocateFrom(Constants.FIREFLY_LUCIFERIN));
                // Crea una nuova notifica
                MemorySegment notification = notify_notification_new(arenaGlobal.allocateFrom("Title c"),
                        arenaGlobal.allocateFrom("this is a long bogu this is a long bogu this is a long bogu this is a long bogu \nsdas\nthis is a long bogu"), arenaGlobal.allocateFrom(getIconPath(Constants.IMAGE_TRAY_STOP)));
                // Mostra la notifica
                notify_notification_show(notification, MemorySegment.NULL);
                notify_uninit();
            }
        }

        if (NativeExecutor.isSystemTraySupported()) {
            try (var arenaGlobal = Arena.ofConfined()) {
                arena = Arena.ofAuto();
                indicator = app_indicator_new(arenaGlobal.allocateFrom(String.valueOf(UUID.randomUUID())), arenaGlobal.allocateFrom(Constants.FIREFLY_LUCIFERIN), 0);
                populateTrayWithItems();
            }
        }
    }

    /**
     * Populate tray with icons
     */
    private void populateTrayWithItems() {
        if (NativeExecutor.isSystemTraySupported()) {
            gtkMenu = gtk_menu_new();
            // Start stop menu item
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
    }

    /**
     * Populate aspect ratio sub menu
     */
    private void populateAspectRatio() {
        if (NativeExecutor.isSystemTraySupported()) {
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
    }

    /**
     * Populate profiles submenu
     */
    @Override
    public void populateProfiles() {
        if (NativeExecutor.isSystemTraySupported()) {
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
    }

    /**
     * Reset tray
     */
    @Override
    public void resetTray() {
        if (NativeExecutor.isSystemTraySupported()) {
            TrayIconManager.super.resetTray();
        }
    }

    /**
     * Set a new icon image on tray
     *
     * @param playerStatus status of the player
     * @return img icon
     */
    @Override
    public String setTrayIconImage(Enums.PlayerStatus playerStatus) {
        String imgStr = computeImageToUse(playerStatus);
        imgStr = getIconPath(imgStr);
        app_indicator_set_icon(indicator, arena.allocateFrom(imgStr));
        return imgStr;
    }

    /**
     * Get absolute path of an image, used for native access
     *
     * @param imgStr relative path
     * @return absolute path
     */
    private String getIconPath(String imgStr) {
        if (NativeExecutor.isSystemTraySupported()) {
            String imgAbsolutePath = Objects.requireNonNull(this.getClass().getResource(imgStr)).getPath()
                    .replace(Constants.JAVA_PREFIX, "").replace(Constants.FILE_PREFIX, "")
                    .split(Constants.FAT_JAR_NAME)[0] + Constants.CLASSES + imgStr;
            if (Files.exists(Paths.get(imgAbsolutePath))) {
                imgStr = imgAbsolutePath;
            } else {
                imgStr = Objects.requireNonNull(this.getClass().getResource(imgStr)).getPath();
            }
        }
        return imgStr;
    }

    /**
     * Create a menu item
     *
     * @param menu where to add the item
     * @param label to use for the item
     * @param action callback to execute once the item is clicked
     */
    private void addMenuItem(MemorySegment menu, String label, Action action) {
        var item = gtk_menu_item_new();
        g_signal_connect_object(item, arena.allocateFrom(Constants.ACTIVATE_EVENT), GCallback.allocate(() -> this.methodExecutor(action), arena), menu, 0);
        gtk_menu_item_set_label(item, arena.allocateFrom(label));
        gtk_menu_shell_append(menu, item);
    }

    /**
     * Create a menu item
     *
     * @param menu where to add the item
     * @param label to use for the item
     * @param action callback to execute once the item is clicked
     */
    private void addMenuItem(MemorySegment menu, String label, ActionInput action) {
        var item = gtk_menu_item_new();
        g_signal_connect_object(item, arena.allocateFrom(Constants.ACTIVATE_EVENT), GCallback.allocate(() -> this.methodExecutorInput(action, label), arena), menu, 0);
        gtk_menu_item_set_label(item, arena.allocateFrom(label));
        gtk_menu_shell_append(menu, item);
    }

    /**
     * Profile action
     *
     * @param selectedProfile from tray
     */
    public void profileAction(String selectedProfile) {
        super.profileAction(selectedProfile);
        manageAspectRatioListener(selectedProfile, true);
    }

    /**
     * Aspect ratio action
     *
     * @param selectedAspectRatio from tray
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

    @FunctionalInterface
    public interface Action {
        void execute();
    }

    @FunctionalInterface
    public interface ActionInput {
        void execute(String input);
    }


}