/*
  LibAppIndicator.java

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
package org.dpsoftware.gui.bindings.appindicator;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.gui.bindings.CommonBinding;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;


/**
 * Luciferin creates a binding to libappindicator3 and libayatana-appindicator3 to display a tray icon under Linux.
 * AWT tray icon uses the old XEMBED APIs and even if some distros bundles xembed-sni-proxy it doesn't work well on all distros.
 * For this reason Luciferin uses jextract to create a binding for the native API.
 * This class loads the libraries extracted with jextract with the static block.
 * Typically, this step is unnecessary, as jextract automatically loads the libraries in the generated binding when using the --library option.
 * However, for libappindicator, this method is ineffective because the same header file can be bound to multiple .so files,
 * and the 3.so.1 file does not function well with the automatic binding.
 * HOW TO:
 * - Download <a href="https://jdk.java.net/jextract/">jextract</a>
 * - export PATH=$HOME/Downloads/jextract/bin:$PATH
 * - chmod 0755 $HOME/Downloads/jextract/bin/*
 * - git clone <a href="https://github.com/AyatanaIndicators/libayatana-appindicator">libayatana-appindicator</a>
 * - or
 * - apt install libayatana*dev
 * jextract \
 * -t org.dpsoftware.gui.bindings.appindicator \
 * -I /usr/include/gtk-3.0/ \
 * -I /usr/include/glib-2.0/ \
 * -I /usr/include/cairo/ \
 * -I /usr/include/gdk-pixbuf-2.0/ \
 * -I /usr/include/pango-1.0/ \
 * -I /usr/include/atk-1.0/ \
 * -I /usr/include/harfbuzz/ \
 * -I /usr/lib/x86_64-linux-gnu/glib-2.0/include/ \
 * --include-function app_indicator_build_menu_from_desktop \
 * --include-function app_indicator_get_category \
 * --include-function app_indicator_get_attention_icon \
 * --include-function app_indicator_get_id \
 * --include-function app_indicator_get_icon \
 * --include-function app_indicator_get_label \
 * --include-function app_indicator_get_menu \
 * --include-function app_indicator_get_ordering_index \
 * --include-function app_indicator_get_status \
 * --include-function app_indicator_get_title \
 * --include-function app_indicator_new \
 * --include-function app_indicator_set_attention_icon \
 * --include-function app_indicator_set_icon \
 * --include-function app_indicator_set_label \
 * --include-function app_indicator_set_menu \
 * --include-function app_indicator_set_ordering_index \
 * --include-function app_indicator_set_status \
 * --include-function app_indicator_set_title \
 * --include-function app_indicator_set_secondary_activate_target \
 * --include-function g_error_free \
 * --include-function g_object_set_data_full \
 * --include-function g_signal_connect_object \
 * --include-function gtk_action_get_name \
 * --include-function gtk_action_group_new \
 * --include-function gtk_action_group_add_action \
 * --include-function gtk_action_group_add_actions \
 * --include-function gtk_container_add \
 * --include-function gtk_init \
 * --include-function gtk_main \
 * --include-function gtk_menu_item_new \
 * --include-function gtk_menu_item_set_label \
 * --include-function gtk_menu_item_get_label \
 * --include-function gtk_menu_item_set_submenu \
 * --include-function gtk_menu_new \
 * --include-function gtk_menu_shell_append \
 * --include-function gtk_message_dialog_new \
 * --include-function gtk_scrolled_window_new \
 * --include-function gtk_scrolled_window_set_policy \
 * --include-function gtk_scrolled_window_set_shadow_type \
 * --include-function gtk_statusbar_new \
 * --include-function gtk_table_new \
 * --include-function gtk_table_attach \
 * --include-function gtk_text_view_new \
 * --include-function gtk_widget_destroy \
 * --include-function gtk_widget_destroyed \
 * --include-function gtk_widget_grab_focus \
 * --include-function gtk_widget_show \
 * --include-function gtk_widget_show_all \
 * --include-function gtk_window_add_accel_group \
 * --include-function gtk_window_new \
 * --include-function gtk_window_set_default_size \
 * --include-function gtk_window_set_icon \
 * --include-function gtk_window_set_icon_name \
 * --include-function gtk_window_set_title \
 * --include-function gtk_separator_menu_item_new \
 * --include-function gtk_ui_manager_add_ui \
 * --include-function gtk_ui_manager_add_ui_from_string \
 * --include-function gtk_ui_manager_get_accel_group \
 * --include-function gtk_ui_manager_get_widget \
 * --include-function gtk_ui_manager_insert_action_group \
 * --include-function gtk_ui_manager_new \
 * --include-function gtk_status_icon_new_from_icon_name \
 * --include-function gtk_menu_item_new_with_label \
 * --include-function gdk_event_get \
 * --include-typedef GCallback \
 * /usr/include/libayatana-appindicator3-0.1/libayatana-appindicator/app-indicator.h
 * -
 * Copy the jextracted file in org\dpsoftware\gui\appindicator
 */
@Slf4j
public class LibAppIndicator extends CommonBinding {

    private static boolean isLoaded = false;
    private static final String APPINDICATOR_VERSION = "libappindicator3.so.1";
    private static final String FLATPAK_APPINDICATOR_VERSION = "libappindicator3.so";
    private static final String LD_CONFIG = "/etc/ld.so.conf.d/";
    private static final String AYATANA_APPINDICATOR_VERSION = "libayatana-appindicator3.so.1";
    private static final String AYATANA_APPINDICATOR_LIBNAME_VERSION = "ayatana-appindicator3";
    private static final String APPINDICATOR_LIBNAME_VERSION = "appindicator3";
    private static boolean ayatana = false;
    private static boolean appindicator = false;
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
                System.load(path + File.separator + AYATANA_APPINDICATOR_VERSION);
                isLoaded = true;
                ayatana = true;
                break;
            } catch (UnsatisfiedLinkError e) {
                try {
                    if (!path.equals("/app/lib")) {
                        System.load(path + File.separator + APPINDICATOR_VERSION);
                    } else {
                        // flatpak has an own, self-compiled version
                        System.load(path + File.separator + FLATPAK_APPINDICATOR_VERSION);
                    }
                    isLoaded = true;
                    appindicator = true;
                    break;
                } catch (UnsatisfiedLinkError ignored) { }
            }
        }

        // When loading via System.load wasn't successful, try to load via System.loadLibrary.
        // System.loadLibrary builds the libname by prepending the prefix JNI_LIB_PREFIX
        // and appending the suffix JNI_LIB_SUFFIX. This usually does not work for library files
        // with an ending like '3.so.1'.
        if (!isLoaded) {
            try {
                System.loadLibrary(AYATANA_APPINDICATOR_LIBNAME_VERSION);
                isLoaded = true;
                ayatana = true;
            } catch (UnsatisfiedLinkError e) {
                try {
                    System.loadLibrary(APPINDICATOR_LIBNAME_VERSION);
                    isLoaded = true;
                    appindicator = true;
                } catch (UnsatisfiedLinkError ignored) {
                }
            }
        }
        log.info(ayatana ? "Native code library " + AYATANA_APPINDICATOR_VERSION + " successfully loaded" : "Native code library " + AYATANA_APPINDICATOR_VERSION + " failed to load");
        log.info(appindicator ? "Native code library " + APPINDICATOR_VERSION + " successfully loaded" : "Native code library " + APPINDICATOR_VERSION + " failed to load");
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
