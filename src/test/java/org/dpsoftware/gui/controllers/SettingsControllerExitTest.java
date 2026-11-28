package org.dpsoftware.gui.controllers;

import javafx.application.Platform;
import javafx.scene.input.InputEvent;
import org.dpsoftware.NativeExecutor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SettingsControllerExitTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void linuxAndWindowsHideBeforeDeferringRestartOffTheUiThread(boolean linux) {
        SettingsController controller = spy(new SettingsController());
        InputEvent event = mock(InputEvent.class);
        boolean[] hidden = {false};
        doAnswer(call -> { hidden[0] = true; return null; }).when(controller).cancel(event);

        try (MockedStatic<NativeExecutor> nativeExecutor = mockStatic(NativeExecutor.class);
             MockedStatic<Platform> platform = mockStatic(Platform.class);
             MockedStatic<CompletableFuture> futures = mockStatic(CompletableFuture.class)) {
            nativeExecutor.when(NativeExecutor::isLinux).thenReturn(linux);
            nativeExecutor.when(NativeExecutor::isWindows).thenReturn(!linux);
            CompletableFuture<Void> completed = new CompletableFuture<>();
            completed.complete(null);
            futures.when(() -> CompletableFuture.runAsync(any(Runnable.class))).thenReturn(completed);
            platform.when(() -> Platform.runLater(any())).thenAnswer(call -> {
                assertTrue(hidden[0], "The window must be hidden before deferring the restart");
                return null;
            });

            controller.exit(event);

            nativeExecutor.verify(NativeExecutor::restartNativeInstanceWithCurrentProfile, never());
            futures.verifyNoInteractions();
            ArgumentCaptor<Runnable> uiContinuation = ArgumentCaptor.forClass(Runnable.class);
            platform.verify(() -> Platform.runLater(uiContinuation.capture()));
            uiContinuation.getValue().run();

            // The UI continuation submits work; it must not execute the blocking restart itself.
            nativeExecutor.verify(NativeExecutor::restartNativeInstanceWithCurrentProfile, never());
            ArgumentCaptor<Runnable> restart = ArgumentCaptor.forClass(Runnable.class);
            futures.verify(() -> CompletableFuture.runAsync(restart.capture()));
            restart.getValue().run();
            nativeExecutor.verify(NativeExecutor::restartNativeInstanceWithCurrentProfile);
        }
    }

}
