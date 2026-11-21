/*
  GtkMainLoop.java

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
package org.dpsoftware.gui.bindings.appindicator;

import lombok.extern.slf4j.Slf4j;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Dispatches tray operations to the GLib main loop already owned by JavaFX's GTK backend.
 * Calls from capture and network threads must be marshalled here before touching GTK widgets.
 */
@Slf4j
public final class GtkMainLoop {

    private static final ConcurrentHashMap<Long, Runnable> pending = new ConcurrentHashMap<>();
    private static final AtomicLong nextId = new AtomicLong();

    private GtkMainLoop() {
    }

    public static void runLater(Runnable action) {
        // Resolve the native symbols before retaining the action if GTK is unavailable.
        MethodHandle idleAdd = Native.idleAdd;
        long id = nextId.incrementAndGet();
        pending.put(id, action);
        try {
            int sourceId = (int) idleAdd.invokeExact(Native.dispatch, MemorySegment.ofAddress(id));
            if (sourceId == 0) {
                throw new IllegalStateException("Could not schedule GTK tray operation");
            }
        } catch (Throwable e) {
            pending.remove(id);
            throw new IllegalStateException("Could not schedule GTK tray operation", e);
        }
    }

    /** Re-export the menu when the desktop's StatusNotifierWatcher becomes available or restarts. */
    public static void onIndicatorConnected(MemorySegment indicator, Arena arena, Runnable action) {
        try {
            var descriptor = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS);
            var callback = MethodHandles.lookup().findStatic(GtkMainLoop.class, "connectionChanged",
                    MethodType.methodType(void.class, AtomicBoolean.class, Runnable.class, MemorySegment.class, int.class, MemorySegment.class));
            callback = MethodHandles.insertArguments(callback, 0, new AtomicBoolean(), action);
            var stub = Linker.nativeLinker().upcallStub(callback, descriptor, arena);
            app_indicator_h.g_signal_connect_object(indicator, arena.allocateFrom("connection-changed"), stub, indicator, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not register AppIndicator connection callback", e);
        }
    }

    static void connectionChanged(AtomicBoolean wasConnected, Runnable action, MemorySegment indicator, int connected, MemorySegment data) {
        if (connected == 0) {
            wasConnected.set(false);
        } else if (wasConnected.compareAndSet(false, true)) {
            try {
                // set_menu can register again and emit another TRUE; refresh only once per connection.
                runLater(action);
            } catch (Throwable e) {
                log.error("Could not refresh tray after connecting to the desktop", e);
            }
        }
    }

    private static int dispatch(MemorySegment data) {
        Runnable action = pending.remove(data.address());
        try {
            if (action != null) {
                action.run();
            }
        } catch (Throwable e) {
            // Exceptions must never unwind through a native upcall.
            log.error("GTK tray operation failed", e);
        }
        return 0; // G_SOURCE_REMOVE
    }

    // Lazy loading keeps headless mode and non-Linux platforms independent of GLib.
    private static final class Native {
        private static final Arena arena = Arena.ofAuto();
        private static final MethodHandle idleAdd = Linker.nativeLinker().downcallHandle(
                app_indicator_h.findOrThrow("g_idle_add"), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        private static final MemorySegment dispatch;

        static {
            try {
                var callback = MethodHandles.lookup().findStatic(GtkMainLoop.class, "dispatch",
                        MethodType.methodType(int.class, MemorySegment.class));
                dispatch = Linker.nativeLinker().upcallStub(callback, FunctionDescriptor.of(JAVA_INT, ADDRESS), arena);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }
}
