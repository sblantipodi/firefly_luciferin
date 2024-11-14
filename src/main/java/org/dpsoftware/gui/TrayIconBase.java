/*
  TrayIconBase.java

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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.gui.bindings.CommonBinding;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;

import javax.swing.*;
import java.util.Locale;

/**
 * Common methods for tray icons
 */
@Slf4j
public abstract class TrayIconBase extends CommonBinding {

    public Timer timer;

    /**
     * Generate tooltip string
     *
     * @return tooltip string
     */
    public String getTooltip() {
        String tooltipStr;
        if (MainSingleton.getInstance().config.getMultiMonitor() > 1) {
            if (Constants.SERIAL_PORT_AUTO.equals(MainSingleton.getInstance().config.getOutputDevice()) && NetworkManager.isValidIp(MainSingleton.getInstance().config.getStaticGlowWormIp())) {
                tooltipStr = MainSingleton.getInstance().config.getStaticGlowWormIp();
            } else {
                tooltipStr = MainSingleton.getInstance().config.getOutputDevice();
            }
        } else {
            tooltipStr = Constants.FIREFLY_LUCIFERIN;
        }
        return tooltipStr;
    }

    /**
     * Update LEDs state based on profiles
     */
    public void updateLEDs() {
        CommonUtility.turnOnLEDs();
        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
        boolean requirePipeline = Enums.Effect.BIAS_LIGHT.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_VU_METER.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_VU_METER_DUAL.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_BRIGHT.equals(effectInUse)
                || Enums.Effect.MUSIC_MODE_RAINBOW.equals(effectInUse);
        if (!MainSingleton.getInstance().RUNNING && requirePipeline) {
            MainSingleton.getInstance().guiManager.startCapturingThreads();
        } else if (MainSingleton.getInstance().RUNNING) {
            MainSingleton.getInstance().guiManager.stopCapturingThreads(true);
            if (requirePipeline) {
                CommonUtility.delaySeconds(() -> MainSingleton.getInstance().guiManager.startCapturingThreads(), 4);
            }
        }
    }

    /**
     * Set profiles and restart if needed
     *
     * @param menuItemText text of the menu clicked
     */
    public void setProfileAndRestart(String menuItemText) {
        StorageManager sm = new StorageManager();
        MainSingleton.getInstance().config = sm.readProfileAndCheckDifference(menuItemText, sm);
        if (sm.restartNeeded) {
            if (menuItemText.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                NativeExecutor.restartNativeInstance(null);
            } else {
                NativeExecutor.restartNativeInstance(menuItemText);
            }
        }
    }

    /**
     * Toggle LEDs
     */
    public void manageOnOff() {
        MainSingleton.getInstance().config.setEffect(Enums.Effect.SOLID.getBaseI18n());
        if (MainSingleton.getInstance().config.isToggleLed()) {
            MainSingleton.getInstance().config.setToggleLed(false);
            CommonUtility.turnOffLEDs(MainSingleton.getInstance().config);
            MainSingleton.getInstance().config.setToggleLed(false);
        } else {
            MainSingleton.getInstance().config.setToggleLed(true);
            CommonUtility.turnOnLEDs();
            MainSingleton.getInstance().config.setToggleLed(true);
        }
        MainSingleton.getInstance().guiManager.trayIconManager.updateTray();
    }

    /**
     * Set aspect ratio
     *
     * @param selectedAspectRatio menu item
     * @param sendSetCmd          send mqtt msg back
     */
    public void setAspectRatio(String selectedAspectRatio, boolean sendSetCmd) {
        MainSingleton.getInstance().config.setDefaultLedMatrix(selectedAspectRatio);
        log.info("{}{}", CommonUtility.getWord(Constants.CAPTURE_MODE_CHANGED), selectedAspectRatio);
        GStreamerGrabber.ledMatrix = MainSingleton.getInstance().config.getLedMatrixInUse(selectedAspectRatio);
        MainSingleton.getInstance().config.setAutoDetectBlackBars(false);
        if (MainSingleton.getInstance().config.isMqttEnable()) {
            CommonUtility.delaySeconds(() -> NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_ASPECT_RATIO), selectedAspectRatio), 1);
            if (sendSetCmd) {
                CommonUtility.delaySeconds(() -> NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_SET_ASPECT_RATIO), selectedAspectRatio), 1);
            }
        }
    }

    /**
     * Manage aspect ratio listener actions
     *
     * @param menuItemText item text
     * @param sendSetCmd   send mqtt msg back
     */
    public void manageAspectRatioListener(String menuItemText, boolean sendSetCmd) {
        if (MainSingleton.getInstance().config != null && (!menuItemText.equals(MainSingleton.getInstance().config.getDefaultLedMatrix())
                || (MainSingleton.getInstance().config.isAutoDetectBlackBars() && !CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS).equals(menuItemText)))) {
            if (Enums.AspectRatio.FULLSCREEN.getBaseI18n().equals(menuItemText)
                    || Enums.AspectRatio.LETTERBOX.getBaseI18n().equals(menuItemText)
                    || Enums.AspectRatio.PILLARBOX.getBaseI18n().equals(menuItemText)) {
                setAspectRatio(menuItemText, sendSetCmd);
            } else if (CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS).equals(menuItemText) ||
                    CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS, Locale.ENGLISH).equals(menuItemText)) {
                log.info("{}{}", CommonUtility.getWord(Constants.CAPTURE_MODE_CHANGED), CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS));
                MainSingleton.getInstance().config.setAutoDetectBlackBars(true);
                if (MainSingleton.getInstance().config.isMqttEnable()) {
                    CommonUtility.delaySeconds(() -> NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_ASPECT_RATIO), CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS, Locale.ENGLISH)), 1);
                    if (sendSetCmd) {
                        CommonUtility.delaySeconds(() -> NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_SET_ASPECT_RATIO), CommonUtility.getWord(Constants.AUTO_DETECT_BLACK_BARS, Locale.ENGLISH)), 1);
                    }
                }
            }
        }
    }

    /**
     * Manage profile listener action
     *
     * @param selectedProfile from the tray icon
     */
    public void profileAction(String selectedProfile) {
        StorageManager sm = new StorageManager();
        if (sm.listProfilesForThisInstance().stream().anyMatch(profile -> profile.equals(selectedProfile))
                || selectedProfile.equals(CommonUtility.getWord(Constants.DEFAULT))) {
            if (selectedProfile.equals(CommonUtility.getWord(Constants.DEFAULT))) {
                NativeExecutor.restartNativeInstance(null);
            } else {
                NativeExecutor.restartNativeInstance(selectedProfile);
            }
        }
        if (CommonUtility.getWord(Constants.TRAY_EXIT).equals(selectedProfile)) {
            NativeExecutor.exit();
        }
    }

    /**
     * Stop action
     */
    public void stopAction() {
        MainSingleton.getInstance().guiManager.stopCapturingThreads(true);
    }

    /**
     * Start action
     */
    public void startAction() {
        MainSingleton.getInstance().guiManager.startCapturingThreads();
    }

    /**
     * Turn Off action
     */
    public void turnOffAction() {
        manageOnOff();
    }

    /**
     * Turn On action
     */
    public void turnOnAction() {
        manageOnOff();
    }

    /**
     * Settings action
     */
    public void settingsAction() {
        MainSingleton.getInstance().guiManager.showSettingsDialog(false);
    }

    /**
     * Info action
     */
    public void infoAction() {
        MainSingleton.getInstance().guiManager.showFramerateDialog();
    }

    /**
     * Exit action
     */
    public void exitAction() {
        NativeExecutor.exit();
    }

}
