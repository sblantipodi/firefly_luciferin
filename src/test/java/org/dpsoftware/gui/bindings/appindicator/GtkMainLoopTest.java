package org.dpsoftware.gui.bindings.appindicator;

import org.dpsoftware.gui.bindings.CommonBinding;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Exercises the real native GLib callback ABI without starting a desktop or JavaFX. */
@EnabledOnOs(OS.LINUX)
class GtkMainLoopTest {

    private static MethodHandle iterate;

    @BeforeAll
    static void loadGlib() {
        try {
            System.loadLibrary("glib-2.0");
        } catch (UnsatisfiedLinkError e) {
            Path library = java.util.stream.Stream.concat(
                            java.util.Arrays.stream(System.getProperty("java.library.path").split(File.pathSeparator)),
                            CommonBinding.getLdConfigPaths().stream())
                    .map(directory -> Path.of(directory, "libglib-2.0.so.0"))
                    .filter(Files::isRegularFile).findFirst().orElse(null);
            assumeTrue(library != null, "GLib is not installed: " + e.getMessage());
            System.load(library.toAbsolutePath().toString());
        }
        iterate = Linker.nativeLinker().downcallHandle(
                SymbolLookup.loaderLookup().find("g_main_context_iteration").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    }

    @Test
    void workerSubmissionsRunOnceOnTheThreadIteratingTheMainContext() throws Throwable {
        List<Integer> results = new ArrayList<>();
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        Thread worker = Thread.ofPlatform().start(() -> {
            for (int i = 0; i < 20; i++) {
                int value = i;
                GtkMainLoop.runLater(() -> {
                    callbackThread.set(Thread.currentThread());
                    results.add(value);
                });
            }
        });
        worker.join();
        assertTrue(results.isEmpty());

        drain();
        assertEquals(java.util.stream.IntStream.range(0, 20).boxed().toList(), results);
        assertSame(Thread.currentThread(), callbackThread.get());
        drain();
        assertEquals(20, results.size());
    }

    @Test
    void repeatedConnectedSignalsDoNotCauseAMenuRegistrationLoop() throws Throwable {
        AtomicBoolean connected = new AtomicBoolean();
        AtomicInteger refreshed = new AtomicInteger();
        Runnable refresh = refreshed::incrementAndGet;
        GtkMainLoop.connectionChanged(connected, refresh, MemorySegment.NULL, 1, MemorySegment.NULL);
        GtkMainLoop.connectionChanged(connected, refresh, MemorySegment.NULL, 1, MemorySegment.NULL);
        drain();
        assertEquals(1, refreshed.get());

        GtkMainLoop.connectionChanged(connected, refresh, MemorySegment.NULL, 0, MemorySegment.NULL);
        drain();
        assertEquals(1, refreshed.get());
        GtkMainLoop.connectionChanged(connected, refresh, MemorySegment.NULL, 1, MemorySegment.NULL);
        drain();
        assertEquals(2, refreshed.get());
    }

    @Test
    void failedActionDoesNotUnwindIntoNativeCodeOrPreventTheNextAction() throws Throwable {
        AtomicInteger completed = new AtomicInteger();
        GtkMainLoop.runLater(() -> { throw new IllegalStateException("Expected test failure"); });
        GtkMainLoop.runLater(completed::incrementAndGet);

        drain();
        assertEquals(1, completed.get());
    }

    private static void drain() throws Throwable {
        for (int i = 0; i < 100; i++) {
            int dispatched = (int) iterate.invokeExact(MemorySegment.NULL, 0);
            if (dispatched == 0) {
                return;
            }
        }
        fail("GLib main context did not become idle");
    }
}
