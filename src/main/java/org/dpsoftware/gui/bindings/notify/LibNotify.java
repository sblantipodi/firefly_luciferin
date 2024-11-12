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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Luciferin creates a binding to libnotify to display a notification under Linux.
 * This class loads the libraries extracted with jextract.
 * HOW TO:
 * - Download <a href="https://jdk.java.net/jextract/">jextract</a>
 * - export PATH=$HOME/Downloads/jextract/bin:$PATH
 * - chmod 0755 $HOME/Downloads/jextract/bin/*
 * - apt install libnotify-dev
 * jextract -l notify \
 * -t org.dpsoftware.gui.bindings.notify \
 * -I /usr/include/gtk-3.0/ \
 * -I /usr/include/glib-2.0/ \
 * -I /usr/include/cairo/ \
 * -I /usr/include/gdk-pixbuf-2.0/ \
 * --include-function notify_notification_new \
 * --include-function notify_notification_show \
 * --include-function notify_init \
 * --include-function notify_uninit \
 * -I /usr/lib/x86_64-linux-gnu/glib-2.0/include/ \
 * /usr/include/libnotify/notify.h
 * -
 * Copy the jextracted file in org\dpsoftware\gui\notify
 */
@Slf4j
public class LibNotify {

    private static boolean isLoaded = false;
    private static final String LIB_NOTIFY_VERSION = "libnotify.so";
    private static final String LD_CONFIG = "/etc/ld.so.conf.d/";
    private static final List<String> allPath = new LinkedList<>();

    static {
        try (Stream<Path> paths = Files.list(Path.of(LD_CONFIG))) {
            paths.forEach((file) -> {
                try (Stream<String> lines = Files.lines(file)) {
                    List<String> collection = lines.filter(line -> line.startsWith("/")).toList();
                    allPath.addAll(collection);
                } catch (IOException e) {
                    log.error("File '{}' could not be loaded", file);
                }
            });
        } catch (IOException e) {
            log.error("Directory '{}' does not exist", LD_CONFIG);
        }
        // for systems, that don't implement multiarch
        allPath.add("/usr/lib");
        // for flatpak and libraries in the flatpak sandbox
        allPath.add("/app/lib");
        // for Fedora-like distributions
        allPath.add("/usr/lib64");
        for (String path : allPath) {
            try {
                System.load(path + File.separator + LIB_NOTIFY_VERSION);
                isLoaded = true;
                break;
            } catch (UnsatisfiedLinkError ignored) { }
        }

        // When loading via System.load wasn't successful, try to load via System.loadLibrary.
        // System.loadLibrary builds the libname by prepending the prefix JNI_LIB_PREFIX
        // and appending the suffix JNI_LIB_SUFFIX. This usually does not work for library files
        // with an ending like '3.so.1'.
        if (!isLoaded) {
            try {
                System.loadLibrary(LIB_NOTIFY_VERSION);
                isLoaded = true;
            } catch (UnsatisfiedLinkError ignored) { }
        }
        log.info(isLoaded ? "Native code library " + LIB_NOTIFY_VERSION + " successfully loaded" : "Native code library " + LIB_NOTIFY_VERSION + " failed to load");
    }

    /**
     * Check if a library has been loaded
     *
     * @return true if a usable lib is found
     */
    public static boolean isSupported() {
        return isLoaded;
    }


}
