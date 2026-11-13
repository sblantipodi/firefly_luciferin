package org.dpsoftware.gui.trayicon;

import javafx.application.Platform;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.bindings.appindicator.GCallback;
import org.dpsoftware.gui.bindings.appindicator.GtkMainLoop;
import org.dpsoftware.gui.bindings.appindicator.app_indicator_h;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.foreign.MemorySegment;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TrayIconAppIndicatorTest {

    private final Queue<Runnable> gtkTasks = new ArrayDeque<>();
    private final AtomicReference<Runnable> connected = new AtomicReference<>();
    private final List<String> labels = new ArrayList<>();
    private Configuration previousConfig;
    private boolean previousRunning;
    private boolean previousError;
    private boolean previousStarting;
    private MockedStatic<NativeExecutor> nativeExecutor;
    private MockedStatic<GtkMainLoop> mainLoop;
    private MockedStatic<GCallback> callbacks;
    private MockedStatic<app_indicator_h> gtk;
    private MockedConstruction<StorageManager> storage;
    private TrayIconAppIndicator tray;

    @BeforeEach
    void setUp() {
        previousConfig = MainSingleton.getInstance().config;
        previousRunning = MainSingleton.getInstance().RUNNING;
        previousError = MainSingleton.getInstance().communicationError;
        previousStarting = ManagerSingleton.getInstance().pipelineStarting;
        MainSingleton.getInstance().config = new Configuration();
        MainSingleton.getInstance().config.setToggleLed(true);
        MainSingleton.getInstance().RUNNING = false;
        MainSingleton.getInstance().communicationError = false;
        ManagerSingleton.getInstance().pipelineStarting = false;
        nativeExecutor = mockStatic(NativeExecutor.class);
        nativeExecutor.when(NativeExecutor::isSystemTraySupported).thenReturn(true);
        mainLoop = mockStatic(GtkMainLoop.class);
        mainLoop.when(() -> GtkMainLoop.runLater(any())).thenAnswer(call -> {
            gtkTasks.add(call.getArgument(0));
            return null;
        });
        mainLoop.when(() -> GtkMainLoop.onIndicatorConnected(any(), any(), any())).thenAnswer(call -> {
            connected.set(call.getArgument(2));
            return null;
        });
        // Initialize GCallback's method handle before mocking the generated bindings.
        callbacks = mockStatic(GCallback.class);
        gtk = mockStatic(app_indicator_h.class);
        gtk.when(() -> app_indicator_h.app_indicator_new(any(), any(), anyInt()))
                .thenReturn(MemorySegment.ofAddress(1));
        gtk.when(app_indicator_h::gtk_menu_new).thenReturn(MemorySegment.ofAddress(2));
        gtk.when(app_indicator_h::gtk_menu_item_new).thenReturn(MemorySegment.ofAddress(3));
        gtk.when(() -> app_indicator_h.gtk_menu_item_set_label(any(), any())).thenAnswer(call -> {
            labels.add(((MemorySegment) call.getArgument(1)).getString(0));
            return null;
        });
        storage = mockConstruction(StorageManager.class,
                (mock, context) -> when(mock.listProfilesForThisInstance()).thenReturn(Set.of()));
        tray = new TrayIconAppIndicator();
    }

    @AfterEach
    void tearDown() {
        storage.close();
        gtk.close();
        callbacks.close();
        mainLoop.close();
        nativeExecutor.close();
        TrayIconState.update(Enums.PlayerStatus.STOP);
        MainSingleton.getInstance().config = previousConfig;
        MainSingleton.getInstance().RUNNING = previousRunning;
        MainSingleton.getInstance().communicationError = previousError;
        ManagerSingleton.getInstance().pipelineStarting = previousStarting;
    }

    @Test
    void initializationAndUpdatesWaitForGtkMainLoop() {
        if (!NativeExecutor.isLinux()) return;
        tray.initTray();
        assertNull(tray.indicator);
        gtk.verifyNoInteractions();

        drainGtk();
        assertNotNull(tray.indicator);
        gtk.verify(() -> app_indicator_h.app_indicator_set_menu(any(), any()), times(1));

        tray.updateTray();
        gtk.verify(() -> app_indicator_h.app_indicator_set_menu(any(), any()), times(1));
        drainGtk();
        gtk.verify(() -> app_indicator_h.app_indicator_set_menu(any(), any()), times(2));
    }

    @Test
    void lateDesktopConnectionAndReconnectionReexportLabelsAndKeepPlayingState() {
        if (!NativeExecutor.isLinux()) return;
        tray.initTray();
        drainGtk();
        assertNotNull(connected.get());
        MainSingleton.getInstance().RUNNING = true;
        labels.clear();

        connected.get().run();
        drainGtk();
        connected.get().run();
        drainGtk();

        gtk.verify(() -> app_indicator_h.app_indicator_set_menu(any(), any()), times(3));
        assertTrue(labels.contains(CommonUtility.getWord(LabelKey.STOP)));
        assertTrue(labels.contains(CommonUtility.getWord(LabelKey.SETTINGS)));
        assertTrue(labels.stream().noneMatch(String::isBlank));
        assertEquals(GuiManager.computeImageToUse(Enums.PlayerStatus.PLAY), TrayIconState.currentImage());
    }

    @Test
    void playerStateIsSharedImmediatelyWhileNativeIconUpdateIsQueued() {
        if (!NativeExecutor.isLinux()) return;
        tray.initTray();
        drainGtk();
        gtk.clearInvocations();

        tray.setTrayIconImage(Enums.PlayerStatus.GREY);
        gtk.verifyNoInteractions();
        assertEquals(GuiManager.computeImageToUse(Enums.PlayerStatus.GREY), TrayIconState.currentImage());
        drainGtk();
        gtk.verify(() -> app_indicator_h.app_indicator_set_icon_full(any(), any(), any()));
    }

    @Test
    void menuCallbacksRunOnJavaFxApplicationThread() {
        try (MockedStatic<Platform> platform = mockStatic(Platform.class)) {
            TrayIconAppIndicator.Action action = mock(TrayIconAppIndicator.Action.class);
            tray.methodExecutor(action);
            verifyNoInteractions(action);
            platform.verify(() -> Platform.runLater(any()));
        }
    }

    private void drainGtk() {
        while (!gtkTasks.isEmpty()) {
            gtkTasks.remove().run();
        }
    }
}
