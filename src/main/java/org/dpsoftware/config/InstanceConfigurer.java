/*
  InstanceConfigurer.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.config;

import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;

import java.io.File;

/**
 * Class used to discover a possible path for the config/logs files.
 * Don't log in this class, log is not initialized yet.
 */
public class InstanceConfigurer {

    /**
     * Search for a path for the config/logs files.
     * This patch may change in Windows because of the non-standard Documents folder path. ex: ~/OneDrive/Documents
     * NOTE: No one must log anything before this class or two log files will be created.
     *
     * @return path
     */
    public static String getConfigPath() {
        if (com.sun.jna.Platform.isWindows()) {
            return Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL) + File.separator + Constants.LUCIFERIN_FOLDER;
        } else {
            String xdgConfigHome = System.getenv(Constants.XDG_HOME);
            if (xdgConfigHome == null) {
                // If XDG_CONFIG_HOME is not set, use ~/.config as the default
                xdgConfigHome = System.getProperty(Constants.HOME_PATH) + File.separator + Constants.LINUX_CONFIG_PATH;
            }
            return xdgConfigHome + File.separator + Constants.LUCIFERIN_FOLDER;
        }
    }

    /**
     * Search for a path for the config/logs files.
     *
     * @return path
     */
    public static String getOldConfigPath() {
        return System.getProperty(Constants.HOME_PATH) + File.separator + Constants.DOCUMENTS_FOLDER + File.separator + Constants.LUCIFERIN_FOLDER;
    }

    /**
     * Return a standard path for the JavaFX cache folder.
     *
     * @return standard path
     */
    public static String getOpenJfxCachePath() {
        return System.getProperty(Constants.HOME_PATH) + File.separator + Constants.OPENJFX_PATH;
    }

    /**
     * Get the installation path for jpackage app
     *
     * @return path
     */
    public static String getJpackageInstallationPath() {
        return System.getProperty(Constants.JPACKAGE_APP_PATH);
    }

    /**
     * Get the installation path
     *
     * @return path
     */
    public static String getInstallationPath() {
        String luciferinClassPath = FireflyLuciferin.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (luciferinClassPath.contains(".jar")) {
            if (NativeExecutor.isWindows()) {
                return luciferinClassPath.replace("/", "\\")
                        .substring(1, luciferinClassPath.length() - Constants.REGISTRY_JARNAME_WINDOWS.length())
                        .replace("%20", " ") + Constants.REGISTRY_KEY_VALUE_WINDOWS;
            } else {
                return "/" + luciferinClassPath
                        .substring(1, luciferinClassPath.length() - Constants.REGISTRY_JARNAME_LINUX.length())
                        .replace("%20", " ") + Constants.REGISTRY_KEY_VALUE_LINUX;
            }
        }
        return System.getProperty(Constants.HOME_PATH) + Constants.REGISTRY_DEFAULT_KEY_VALUE;
    }

}