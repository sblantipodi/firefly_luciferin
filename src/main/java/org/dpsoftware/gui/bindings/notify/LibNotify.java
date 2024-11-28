/*
  LibNotify.java

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
package org.dpsoftware.gui.bindings.notify;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.bindings.CommonBinding;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.LinkedList;
import java.util.List;

import static org.dpsoftware.gui.bindings.notify.notify_h.*;

/**
 * Luciferin creates a binding to libnotify to display a notification under Linux.
 * This class loads the libraries extracted with jextract.
 * HOW TO:
 * - Download <a href="https://jdk.java.net/jextract/">jextract</a>
 * - export PATH=$HOME/Downloads/jextract/bin:$PATH
 * - chmod 0755 $HOME/Downloads/jextract/bin/*
 * - apt install libnotify-dev
 * jextract \
 * -t org.dpsoftware.gui.bindings.notify \
 * -I /usr/include/gtk-3.0/ \
 * -I /usr/include/glib-2.0/ \
 * -I /usr/include/cairo/ \
 * -I /usr/include/gdk-pixbuf-2.0/ \
 * --include-function notify_notification_new \
 * --include-function notify_notification_show \
 * --include-function notify_init \
 * --include-function notify_uninit \
 * --include-function gdk_pixbuf_new_from_file \
 * --include-function notify_notification_set_image_from_pixbuf \
 * --include-function notify_notification_set_urgency \
 * --include-function notify_notification_set_timeout \
 * --include-function notify_notification_show \
 * --include-function g_object_unref \
 * -I /usr/lib/x86_64-linux-gnu/glib-2.0/include/ \
 * /usr/include/libnotify/notify.h
 * -
 * Copy the jextracted file in org\dpsoftware\gui\notify
 * Note: passing "jextract -l notify" would avoid the static loading but it doesn't work on sanbox.
 */
@Slf4j
public class LibNotify extends CommonBinding {

    private static boolean isLoaded = false;
    private static final String NOTIFY_SO = "libnotify.so";
    private static final String NOTIFY_SO_VERSION = "libnotify.so.4";
    private static final String NOTIFY = "notify";
    private static final String LIBNOTIFY = "libnotify";
    private static final List<String> allPath = new LinkedList<>();

    static {
        // Enrich with LD paths
        List<String> ldPaths = getLdConfigPaths();
        if (!ldPaths.isEmpty()) {
            allPath.addAll(getLdConfigPaths());
        }
        // for systems, that don't implement multiarch
        allPath.add("/usr/lib");
        // for flatpak and libraries in the flatpak sandbox
        allPath.add("/app/lib");
        // for Fedora-like distributions
        allPath.add("/usr/lib64");
        // for org.freedesktop.sdk (flatpak run org.freedesktop.Sdk//24.08)
        allPath.add("/lib");
        allPath.add("/lib/x86_64-linux-gnu");
        for (String path : allPath) {
            try {
                System.load(path + File.separator + NOTIFY_SO);
                isLoaded = true;
                break;
            } catch (UnsatisfiedLinkError e) {
                try {
                    System.load(path + File.separator + NOTIFY_SO_VERSION);
                    isLoaded = true;
                    break;
                } catch (UnsatisfiedLinkError ignored) { }
            }
        }
        if (!isLoaded) {
            try {
                System.loadLibrary(NOTIFY);
                isLoaded = true;
            } catch (UnsatisfiedLinkError e) {
                try {
                    System.loadLibrary(LIBNOTIFY);
                    isLoaded = true;
                } catch (UnsatisfiedLinkError ignored) {
                }
            }
        }
        log.info(isLoaded ? "Native code library " + NOTIFY_SO + " successfully loaded" : "Native code library " + NOTIFY_SO + " failed to load");
    }

    /**
     * Check if a library has been loaded
     *
     * @return true if a usable lib is found
     */
    public static boolean isSupported() {
        return isLoaded;
    }

    /**
     * Show notification. This uses the OS notification system via AWT tray icon.
     *
     * @param title            dialog title
     * @param content          dialog msg
     * @param notificationType notification type
     */
    public static void showLinuxNotification(String title, String content, TrayIcon.MessageType notificationType) {
        useNotificationBinding(title, content, notificationType);
    }

    /**
     * Show localized notification. This uses the OS notification system via AWT tray icon.
     *
     * @param title            dialog title
     * @param content          dialog msg
     * @param notificationType notification type
     */
    public static void showLocalizedLinuxNotification(String title, String content, TrayIcon.MessageType notificationType) {
        useNotificationBinding(CommonUtility.getWord(title), CommonUtility.getWord(content), notificationType);
    }

    /**
     * Show the notification using the binding
     *
     * @param title            dialog title
     * @param content          dialog msg
     * @param notificationType notification type
     */
    private static void useNotificationBinding(String title, String content, TrayIcon.MessageType notificationType) {
        if (LibNotify.isSupported()) {
            try (var arenaGlobal = Arena.ofConfined()) {
                notify_init(arenaGlobal.allocateFrom(Constants.FIREFLY_LUCIFERIN));
                MemorySegment notification = notify_notification_new(arenaGlobal.allocateFrom(title),
                        arenaGlobal.allocateFrom(content), arenaGlobal.allocateFrom(getIconPath(Constants.IMAGE_TRAY_STOP)));
                var image = gdk_pixbuf_new_from_file(arenaGlobal.allocateFrom(getIconPath(Constants.IMAGE_TRAY_STOP)), MemorySegment.NULL);
                notify_notification_set_image_from_pixbuf(notification, image);
                if (notificationType.equals(TrayIcon.MessageType.ERROR)) {
                    // 0 low, 1 normal, 2 critical
                    notify_notification_set_urgency(notification, 2);
                }
                notify_notification_show(notification, MemorySegment.NULL);
                // g_object_unref(image);
                //  notify_uninit();
            }
        }
    }

}
