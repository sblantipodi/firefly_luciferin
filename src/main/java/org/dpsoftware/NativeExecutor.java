/*
  NativeExecutor.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.controllers.SettingsController;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An utility class for running native commands and get the results
 */
@Slf4j
@NoArgsConstructor
public final class NativeExecutor {

    public static boolean restartOnly = false;
    enum PowerSavingScreenSaver {
        NOT_TRIGGERED,
        TRIGGERED_RUNNING,
        TRIGGERED_NOT_RUNNING
    }
    static PowerSavingScreenSaver powerSavingScreenSaver = PowerSavingScreenSaver.NOT_TRIGGERED;

    /**
     * Run native commands
     * @param cmdToRun Command to run
     * @return A list of string containing the output, empty list if command does not exist
     */
    public static List<String> runNative(String cmdToRun) {
        String[] cmd = cmdToRun.split(" ");
        return runNative(cmd);
    }

    /**
     * This is the real runner that return command output line by line
     * @param cmdToRunUsingArgs Command to run and args, in an array
     * @return A list of string containing the output, empty list if command does not exist
     */
    public static List<String> runNative(String[] cmdToRunUsingArgs) {
        Process process;
        try {
            process = Runtime.getRuntime().exec(cmdToRunUsingArgs);
        } catch (SecurityException | IOException e) {
            log.debug(CommonUtility.getWord(Constants.CANT_RUN_CMD), Arrays.toString(cmdToRunUsingArgs), e.getMessage());
            return new ArrayList<>(0);
        }
        ArrayList<String> sa = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sa.add(line);
            }
            process.waitFor();
        } catch (IOException e) {
            log.debug(CommonUtility.getWord(Constants.NO_OUTPUT), Arrays.toString(cmdToRunUsingArgs), e.getMessage());
            return new ArrayList<>(0);
        } catch (InterruptedException ie) {
            log.debug(CommonUtility.getWord(Constants.INTERRUPTED_WHEN_READING), Arrays.toString(cmdToRunUsingArgs), ie.getMessage());
            Thread.currentThread().interrupt();
        }
        return sa;
    }

    /**
     * Spawn new Luciferin Native instance
     * @param whoAmISupposedToBe instance #
     */
    public static void spawnNewInstance(int whoAmISupposedToBe) {
        if (!NativeExecutor.isWindows()) {
            String[] cmdToRun = getInstallationPath().split("\\\\");
            StringBuilder command = new StringBuilder();
            for (String str : cmdToRun) {
                if (str.contains(" ")) {
                    command.append("\\" + "\"").append(str).append("\"");
                } else {
                    command.append("\\").append(str);
                }
            }
            command = new StringBuilder(command.substring(1));
            runNative(Constants.CMD_START_APP + command + " " + whoAmISupposedToBe);
        } else {
            log.debug("Installation path from spawn={}", getInstallationPath());
            runNative(new String[]{getInstallationPath() + " " + whoAmISupposedToBe});
        }
    }

    /**
     * Check if I'm the main program, if yes and multi monitor, spawn other guys
     */
    public static void spawnNewInstances() {
        if (JavaFXStarter.spawnInstances && FireflyLuciferin.config.getMultiMonitor() > 1) {
            if (FireflyLuciferin.config.getMultiMonitor() == 3) {
                NativeExecutor.spawnNewInstance(3);
                CommonUtility.sleepSeconds(5);
                NativeExecutor.spawnNewInstance(1);
                if ((FireflyLuciferin.config.getMultiMonitor() == 2) || (FireflyLuciferin.config.getMultiMonitor() == 3)) {
                    CommonUtility.sleepSeconds(5);
                    NativeExecutor.spawnNewInstance(2);
                }
            } else {
                if (FireflyLuciferin.config.getMultiMonitor() == 2) {
                    NativeExecutor.spawnNewInstance(2);
                }
                CommonUtility.sleepSeconds(5);
                NativeExecutor.spawnNewInstance(1);
            }
            FireflyLuciferin.exit();
        }
    }

    /**
     * Restart a native instance of Luciferin
     */
    public static void restartNativeInstance() {
        restartNativeInstance(null);
    }

    /**
     * Restart a native instance of Luciferin
     * @param profileToUse restart with active profile if any
     */
    public static void restartNativeInstance(String profileToUse) {
        log.debug(Constants.CLEAN_EXIT);
        if (NativeExecutor.isWindows() || NativeExecutor.isLinux()) {
            log.debug("Installation path from restart={}", getInstallationPath());
            String execCommand = getInstallationPath() + " " + JavaFXStarter.whoAmI;
            if (profileToUse != null) {
                execCommand += " " + "\"" + profileToUse + "\"";
            }
            runNative(new String[]{execCommand});
        }
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            restartOnly = true;
        }
        FireflyLuciferin.exit();
    }

    /**
     * Write Windows registry key to Launch Firefly Luciferin when system starts
     */
    public void writeRegistryKey() {
        String installationPath = getInstallationPath();
        if (!installationPath.isEmpty()) {
            log.debug("Writing Windows Registry key");
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, Constants.REGISTRY_KEY_PATH,
                    Constants.REGISTRY_KEY_NAME, installationPath);
        }
        log.debug(Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER,
                Constants.REGISTRY_KEY_PATH, Constants.REGISTRY_KEY_NAME));
    }

    /**
     * Remove Windows registry key used to Launch Firefly Luciferin when system starts
     */
    public void deleteRegistryKey() {
        if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, Constants.REGISTRY_KEY_PATH,
                Constants.REGISTRY_KEY_NAME)) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, Constants.REGISTRY_KEY_PATH,
                    Constants.REGISTRY_KEY_NAME);
        }
    }

    /**
     * Get the installation path
     * @return path
     */
    public static String getInstallationPath() {
        String luciferinClassPath = FireflyLuciferin.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        log.debug("Installation path={}", luciferinClassPath);
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
        return Constants.REGISTRY_DEFAULT_KEY_VALUE;
    }

    /**
     * Create a .desktop file inside the user folder and append a StatupWMClass on it.
     * If you have a dual Monitor setup and you start Firefly it opens two instances.
     * So you have two Firefly icons in the tray.
     * If you add the StartupWMClass parameter to launcher file, gnome will merge these
     * two icons into one and open a preview of the open windows if you click on it
     */
    public static void createStartWMClass() {
        if (isLinux()) {
            Path originalPath = Paths.get(Constants.LINUX_DESKTOP_FILE);
            Path copied = Paths.get(System.getProperty(Constants.HOME_PATH) + Constants.LINUX_DESKTOP_FILE_LOCAL);
            try {
                Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
                if (Files.exists(originalPath) && !Files.exists(copied)) {
                    Files.write(copied, Constants.STARTUP_WMCLASS.getBytes(), StandardOpenOption.APPEND);
                }
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
        }
    }

    /**
     * Single point to fake the OS if needed
     * @return if the OS match
     */
    public static boolean isLinux() {
        return com.sun.jna.Platform.isLinux();
    }

    /**
     * Single point to fake the OS if needed
     * @return if the OS match
     */
    public static boolean isWindows() {
        return com.sun.jna.Platform.isWindows();
    }

    /**
     * Single point to fake the OS if needed
     * @return if the OS match
     */
    public static boolean isMac() {
        return com.sun.jna.Platform.isMac();
    }

    /**
     * Single point to fake for system tray support if needed
     * @return if the OS supports system tray
     */
    public static boolean isSystemTraySupported() {
        return SystemTray.isSupported();
    }

    /**
     * Add a Hook that is triggered when the OS is shutting down or during reboot.
     */
    public static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Exit hook triggered.");
            if (!FireflyLuciferin.RUNNING && (!Constants.PowerSaving.DISABLED.equals(LocalizedEnum.fromBaseStr(Constants.PowerSaving.class,
                    FireflyLuciferin.config.getPowerSaving())))) {
                SettingsController settingsController = new SettingsController();
                settingsController.turnOffLEDs(FireflyLuciferin.config);
            }
        }));
    }

    /**
     * Execute a task that checks if screensaver is active
     */
    public static void addScreenSaverTask() {
        if (isWindows() && (!Constants.PowerSaving.DISABLED.equals(LocalizedEnum.fromBaseStr(Constants.PowerSaving.class,
                FireflyLuciferin.config.getPowerSaving())))) {
            int minutesToShutdown = Integer.parseInt(FireflyLuciferin.config.getPowerSaving().split(" ")[0]);
            ScheduledExecutorService scheduledExecutorServiceSS = Executors.newScheduledThreadPool(1);
            scheduledExecutorServiceSS.scheduleAtFixedRate(() -> {
                if (isScreensaverRunning()) {
                    CommonUtility.conditionedLog(NativeExecutor.class.getName(), "Screen saver active, power saving on.");
                    SettingsController settingsController = new SettingsController();
                    settingsController.turnOffLEDs(FireflyLuciferin.config);
                    powerSavingScreenSaver = PowerSavingScreenSaver.TRIGGERED_RUNNING;
                } else {
                    if (powerSavingScreenSaver == PowerSavingScreenSaver.TRIGGERED_RUNNING) {
                        FireflyLuciferin.guiManager.startCapturingThreads();
                    } else if (powerSavingScreenSaver == PowerSavingScreenSaver.TRIGGERED_NOT_RUNNING) {
                        CommonUtility.turnOnLEDs();
                    }
                    powerSavingScreenSaver = PowerSavingScreenSaver.NOT_TRIGGERED;
                }
                // TODO
            }, 5, minutesToShutdown, TimeUnit.SECONDS);
        }
    }

    /**
     * Detect if a screensaver is running (Windows only)
     * Linux uses various type of screensavers, and it's not really possible to detect if a screensaver is running.
     * @return boolean if screen saver running
     */
    public static boolean isScreensaverRunning() {
        String[] scrCmd = {Constants.CMD_SHELL_FOR_CMD_EXECUTION, Constants.CMD_PARAM_FOR_CMD_EXECUTION, Constants.CMD_LIST_RUNNING_PROCESS};
        List<String> scrProcess = runNative(scrCmd);
        return scrProcess.stream().filter(s -> s.contains(Constants.SCREENSAVER_EXTENSION)).findAny().orElse(null) != null;
    }

}
