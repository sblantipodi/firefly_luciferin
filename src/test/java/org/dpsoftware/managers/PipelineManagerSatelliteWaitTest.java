/*
  PipelineManagerSatelliteWaitTest.java

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
package org.dpsoftware.managers;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.LabelKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PipelineManagerSatelliteWaitTest {
    private final PipelineManager.SatelliteWaitNotice notice = new PipelineManager.SatelliteWaitNotice();
    private GuiManager previousGui;
    private GuiManager gui;
    private boolean previousWaiting;

    @BeforeEach
    void setup() {
        previousGui = MainSingleton.getInstance().guiManager;
        previousWaiting = GuiSingleton.getInstance().waitingForSatellites;
        GuiSingleton.getInstance().waitingForSatellites = false;
        gui = mock(GuiManager.class);
        MainSingleton.getInstance().guiManager = gui;
    }

    @AfterEach
    void restore() {
        MainSingleton.getInstance().guiManager = previousGui;
        GuiSingleton.getInstance().waitingForSatellites = previousWaiting;
    }

    private void tick(boolean waiting, long seconds) {
        notice.update(waiting, TimeUnit.SECONDS.toNanos(seconds));
    }

    private void verifyNotice(int count) {
        verify(gui, times(count)).showLocalizedNotification(LabelKey.SAT_WAITING_TITLE, LabelKey.SAT_WAITING,
                Constants.FIREFLY_LUCIFERIN, TrayIcon.MessageType.WARNING);
    }

    @Test
    void warnsAfterFifteenSecondsOnceDespiteRepeatedStartupRetries() {
        tick(true, 1);
        tick(true, 15);
        assertFalse(GuiSingleton.getInstance().waitingForSatellites);
        verifyNoInteractions(gui);
        tick(true, 16);
        assertTrue(GuiSingleton.getInstance().waitingForSatellites);
        tick(true, 17);
        tick(true, 120);
        verifyNotice(1);
        verifyNoMoreInteractions(gui);
    }

    @Test
    void findingSatellitesBeforeTimeoutCancelsWarning() {
        tick(true, 1);
        tick(true, 14);
        tick(false, 15);
        tick(false, 120);
        assertFalse(GuiSingleton.getInstance().waitingForSatellites);
        verifyNoInteractions(gui);
    }

    @Test
    void newWaitMustLastAnotherFifteenSeconds() {
        tick(true, 1);
        tick(true, 16);
        tick(false, 20);
        assertFalse(GuiSingleton.getInstance().waitingForSatellites);
        tick(true, 100);
        tick(true, 114);
        verifyNotice(1);
        tick(true, 115);
        verifyNotice(2);
    }

    @Test
    void waitingForMainDeviceAloneDoesNotWarnAboutSatellites() {
        tick(false, 1);
        tick(false, 100);
        verifyNoInteractions(gui);
    }

    @Test
    void separateStartupAttemptDoesNotInheritPreviousWait() {
        tick(true, 1);
        tick(true, 14);
        var nextAttempt = new PipelineManager.SatelliteWaitNotice();
        nextAttempt.update(true, TimeUnit.SECONDS.toNanos(100));
        nextAttempt.update(true, TimeUnit.SECONDS.toNanos(114));
        verifyNoInteractions(gui);
        nextAttempt.update(true, TimeUnit.SECONDS.toNanos(115));
        verifyNotice(1);
    }

    @Test
    void waitingMessagesAreAvailableInEverySupportedLanguage() {
        for (String language : new String[]{"en", "it", "de", "es", "fr", "hu", "pl", "ru"}) {
            var bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, Locale.forLanguageTag(language));
            assertFalse(bundle.getString(LabelKey.SAT_WAITING_TITLE).isBlank());
            assertFalse(bundle.getString(LabelKey.SAT_WAITING).isBlank());
        }
        assertEquals("Waiting for the configured satellites.",
                ResourceBundle.getBundle(Constants.MSG_BUNDLE, Locale.ENGLISH).getString(LabelKey.SAT_WAITING));
    }
}
