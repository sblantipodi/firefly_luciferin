/*
  NativeExecutor.java

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
package org.dpsoftware;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.dto.mqttdiscovery.SensorProducingDiscovery;
import org.dpsoftware.network.NetworkSingleton;
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
import java.util.concurrent.TimeUnit;

/**
 * A utility class for running native commands and get the results
 */
@Slf4j
@NoArgsConstructor
public final class NativeExecutor {

    /**
     * This is the real runner that executes command.
     * Don't use this method directly and prefer the runNativeWaitForOutput() or runNativeNoWaitForOutput() shortcut.
     *
     * @param cmdToRunUsingArgs Command to run and args, in an array
     * @param waitForOutput     Example: If you need to exit the app you don't need to wait for the output or the app will not exit
     * @return A list of string containing the output, empty list if command does not exist
     */
    public static List<String> runNative(String[] cmdToRunUsingArgs, int waitForOutput) {
        Process process;
        ArrayList<String> cmdOutput = new ArrayList<>();
        try {
            process = Runtime.getRuntime().exec(cmdToRunUsingArgs);
        } catch (SecurityException | IOException e) {
            log.info(CommonUtility.getWord(Constants.CANT_RUN_CMD), Arrays.toString(cmdToRunUsingArgs), e.getMessage());
            return new ArrayList<>(0);
        }
        if (waitForOutput > 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while (process.waitFor(waitForOutput, TimeUnit.MILLISECONDS) && (line = reader.readLine()) != null) {
                    cmdOutput.add(line);
                }
            } catch (IOException e) {
                log.info(CommonUtility.getWord(Constants.NO_OUTPUT), Arrays.toString(cmdToRunUsingArgs), e.getMessage());
                return new ArrayList<>(0);
            } catch (InterruptedException ie) {
                log.info(CommonUtility.getWord(Constants.INTERRUPTED_WHEN_READING), Arrays.toString(cmdToRunUsingArgs), ie.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        return cmdOutput;
    }

    /**
     * Spawn new Luciferin Native instance
     *
     * @param whoAmISupposedToBe instance #
     */
    public static void spawnNewInstance(int whoAmISupposedToBe) {
        log.info("Installation path from spawn={}", getInstallationPath());
        List<String> execCommand = new ArrayList<>();
        if (NativeExecutor.isWindows()) {
            execCommand.add(Constants.CMD_SHELL_FOR_CMD_EXECUTION);
            execCommand.add(Constants.CMD_PARAM_FOR_CMD_EXECUTION);
        }
        execCommand.add(getInstallationPath());
        execCommand.add(String.valueOf(whoAmISupposedToBe));
        runNative(execCommand.toArray(String[]::new), Constants.SPAWN_INSTANCE_WAIT_DELAY);
    }

    /**
     * Check if I'm the main program, if yes and multi monitor, spawn other guys
     */
    public static void spawnNewInstances() {
        if (MainSingleton.getInstance().spawnInstances && MainSingleton.getInstance().config.getMultiMonitor() > 1) {
            if (MainSingleton.getInstance().config.getMultiMonitor() == 3) {
                NativeExecutor.spawnNewInstance(1);
                NativeExecutor.spawnNewInstance(2);
                NativeExecutor.spawnNewInstance(3);
            } else {
                NativeExecutor.spawnNewInstance(1);
                if (MainSingleton.getInstance().config.getMultiMonitor() == 2) {
                    NativeExecutor.spawnNewInstance(2);
                }
            }
            NativeExecutor.exit();
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
     *
     * @param profileToUse restart with active profile if any
     */
    public static void restartNativeInstance(String profileToUse) {
        if (NativeExecutor.isWindows() || NativeExecutor.isLinux()) {
            log.info("Installation path from restart={}", getInstallationPath());
            List<String> execCommand = new ArrayList<>();
            execCommand.add(getInstallationPath());
            execCommand.add(String.valueOf(MainSingleton.getInstance().whoAmI));
            if (profileToUse != null) {
                execCommand.add(profileToUse);
            }
            runNative(execCommand.toArray(String[]::new), 0);
            if (CommonUtility.isSingleDeviceMultiScreen()) {
                MainSingleton.getInstance().restartOnly = true;
            }
            NativeExecutor.exit();
        }
    }

    /**
     * Get the installation path
     *
     * @return path
     */
    public static String getInstallationPath() {
        String luciferinClassPath = FireflyLuciferin.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        log.info("Installation path={}", luciferinClassPath);
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
     * If you have a dual Monitor setup, and you start Firefly it opens two instances.
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
                log.info(e.getMessage());
            }
        }
    }

    /**
     * Single point to fake the OS if needed
     *
     * @return if the OS match
     */
    public static boolean isLinux() {
        return com.sun.jna.Platform.isLinux();
    }

    /**
     * Check if Wayland
     *
     * @return if it's Wayland
     */
    public static boolean isWayland() {
        return isLinux() && System.getenv(Constants.DISPLAY_MANAGER_CHK).equalsIgnoreCase(Constants.WAYLAND);
    }

    /**
     * Single point to fake the OS if needed
     *
     * @return if the OS match
     */
    public static boolean isWindows() {
        return com.sun.jna.Platform.isWindows();
    }

    /**
     * Single point to fake the OS if needed
     *
     * @return if the OS match
     */
    public static boolean isMac() {
        return com.sun.jna.Platform.isMac();
    }

    /**
     * Single point to fake for system tray support if needed
     *
     * @return if the OS supports system tray
     */
    public static boolean isSystemTraySupported() {
        return SystemTray.isSupported();
    }

    /**
     * Add a hook that is triggered when the OS is shutting down or during reboot.
     */
    public static void addShutdownHook() {
        Thread hook = new Thread(() -> {
            if (!MainSingleton.getInstance().exitTriggered) {
                log.info("Exit hook triggered.");
                MainSingleton.getInstance().exitTriggered = true;
                lastWill();
            }
        });
        hook.setPriority(Thread.MAX_PRIORITY);
        Runtime.getRuntime().addShutdownHook(hook);
    }

    /**
     * This is the last will before exiting the app. This method is called when manually exiting the app or
     * when the OS entered the shutdown/reboot phase.
     */
    private static void lastWill() {
        if (MainSingleton.getInstance().config.getSatellites().isEmpty() || PipelineManager.isSatellitesEngaged()) {
            if (!Enums.PowerSaving.DISABLED.equals(LocalizedEnum.fromBaseStr(Enums.PowerSaving.class,
                    MainSingleton.getInstance().config.getPowerSaving()))) {
                CommonUtility.turnOffLEDs(MainSingleton.getInstance().config, 1);
            }
            if (MainSingleton.getInstance().config.isMqttEnable()) {
                SensorProducingDiscovery sensorProducingDiscovery = new SensorProducingDiscovery();
                sensorProducingDiscovery.setZeroValue();
            }
        }
    }

    /**
     * Gracefully exit the app, this method is called manually.
     */
    public static void exit() {
        if (MainSingleton.getInstance().RUNNING) {
            MainSingleton.getInstance().guiManager.stopCapturingThreads(true);
        }
        MainSingleton.getInstance().exitTriggered = true;
        log.info(Constants.CLEAN_EXIT);
        NetworkSingleton.getInstance().udpBroadcastReceiverRunning = false;
        exitOtherInstances();
        if (MainSingleton.getInstance().serial != null) {
            MainSingleton.getInstance().serial.removeEventListener();
            MainSingleton.getInstance().serial.close();
        }
        AudioSingleton.getInstance().RUNNING_AUDIO = false;
        CommonUtility.delaySeconds(() -> {
            lastWill();
            System.exit(0);
        }, 2);
    }

    /**
     * Exit single device instances
     */
    static void exitOtherInstances() {
        if (!MainSingleton.getInstance().restartOnly) {
            if (CommonUtility.isSingleDeviceMainInstance()) {
                MainSingleton.getInstance().closeOtherInstaces = true;
                CommonUtility.sleepSeconds(6);
            } else if (CommonUtility.isSingleDeviceOtherInstance()) {
                NetworkSingleton.getInstance().msgClient.sendMessage(Constants.EXIT);
                CommonUtility.sleepSeconds(6);
            }
        }
    }

    /**
     * Detect if a screensaver is running via running processes (Windows only)
     * Linux uses various types of screensavers, and it's not really possible to detect if a screensaver is running.
     *
     * @return boolean if screen saver running
     */
    public static boolean isScreensaverRunning() {
        String[] scrCmd = {Constants.CMD_SHELL_FOR_CMD_EXECUTION, Constants.CMD_PARAM_FOR_CMD_EXECUTION, Constants.CMD_LIST_RUNNING_PROCESS};
        List<String> scrProcess = runNative(scrCmd, Constants.CMD_WAIT_DELAY);
        return scrProcess.stream().filter(s -> s.contains(Constants.SCREENSAVER_EXTENSION)).findAny().orElse(null) != null;
    }

    /**
     * Check is screen saver is enabled via Windows Registry (Windows only)
     * Linux uses various types of screensavers, and it's not really possible to detect if a screensaver is enabled.
     *
     * @return boolean if the screen saver is enabled or not
     */
    public static boolean isScreenSaverEnabled() {
        return Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, Constants.REGISTRY_KEY_PATH_SCREEN_SAVER,
                Constants.REGISTRY_KEY_NAME_SCREEN_SAVER);
    }

    /**
     * Change thread priority to high = 128
     * JNA 5.14.0 added the possibility to do this: Kernel32Util.setCurrentProcessPriority(Kernel32.HIGH_PRIORITY_CLASS);
     * consider using JNA instead of native cmd via powershell.
     */
    public static void setHighPriorityThreads(String priority) {
        if (isWindows()) {
            CommonUtility.delaySeconds(() -> {
                log.info("Changing thread priority to -> " + priority);
                String[] cmd = {Constants.CMD_POWERSHELL, Constants.CMD_SET_PRIORITY
                        .replace("{0}", String.valueOf(Enums.ThreadPriority.valueOf(priority).getValue()))};
                NativeExecutor.runNative(cmd, 0);
            }, 1);
        }
    }

    /**
     * Write Windows registry key to Launch Firefly Luciferin when system starts
     */
    public void writeRegistryKey() {
        String installationPath = getInstallationPath();
        if (!installationPath.isEmpty()) {
            log.info("Writing Windows Registry key");
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, Constants.REGISTRY_KEY_PATH,
                    Constants.REGISTRY_KEY_NAME, installationPath);
        }
        log.info(Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER,
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

}
