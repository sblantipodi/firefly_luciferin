package org.dpsoftware.gui;

import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Enums;

import java.awt.*;

public interface TrayIconManager {

    void manageAspectRatioListener(String menuItemText, boolean sendSetCmd);

    void manageProfileListener(String menuItemText);

    void updateTray();

    void initTray();

    void populateProfiles();

    /**
     * Reset try icon after a serial reconnection
     */
    default void resetTray() {
        if (NativeExecutor.isSystemTraySupported()) {
            setTrayIconImage(Enums.PlayerStatus.STOP);
        }
    }

    @SuppressWarnings("Duplicates")
    Image setTrayIconImage(Enums.PlayerStatus playerStatus);

}
