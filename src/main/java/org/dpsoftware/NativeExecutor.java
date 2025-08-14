/*
  NativeExecutor.java

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
package org.dpsoftware;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import jdk.incubator.vector.IntVector;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.InstanceConfigurer;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.bindings.appindicator.LibAppIndicator;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.dto.mqttdiscovery.SensorProducingDiscovery;
import org.dpsoftware.network.NetworkSingleton;
import org.dpsoftware.utilities.CommonUtility;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.Properties;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
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
     * @param waitForOutput     Example: If you need to exit the app you don't need to wait for the output or the app will not exit (millis)
     * @return A list of string containing the output, empty list if command does not exist
     */
    public static List<String> runNative(String[] cmdToRunUsingArgs, int waitForOutput) {
        ArrayList<String> cmdOutput = new ArrayList<>();
        try {
            log.trace("Executing cmd={}", Arrays.stream(cmdToRunUsingArgs).toList());
            ProcessBuilder processBuilder = new ProcessBuilder(cmdToRunUsingArgs);
            Process process = processBuilder.start();
            if (waitForOutput > 0) {
                if (process.waitFor(waitForOutput, TimeUnit.MILLISECONDS)) {
                    int exitCode = process.exitValue();
                    log.trace("Exit code: {}", exitCode);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.trace(line);
                        cmdOutput.add(line);
                    }
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    while ((line = errorReader.readLine()) != null) {
                        log.trace(line);
                        cmdOutput.add(line);
                    }
                } else {
                    log.error("The command {} has exceeded the time limit and has been terminated.", Arrays.toString(cmdToRunUsingArgs));
                    process.destroy();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return cmdOutput;
    }

    /**
     * Spawn new Luciferin Native instance
     *
     * @param whoAmISupposedToBe instance #
     */
    public static void spawnNewInstance(int whoAmISupposedToBe) {
        List<String> execCommand = new ArrayList<>();
        if (NativeExecutor.isWindows()) {
            execCommand.add(Constants.CMD_SHELL_FOR_CMD_EXECUTION);
            execCommand.add(Constants.CMD_PARAM_FOR_CMD_EXECUTION);
        }
        restartCmd(execCommand);
        execCommand.add(String.valueOf(whoAmISupposedToBe));
        log.info("Spawning new instance");
        runNative(execCommand.toArray(String[]::new), 0);
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
            List<String> execCommand = new ArrayList<>();
            restartCmd(execCommand);
            execCommand.add(String.valueOf(MainSingleton.getInstance().whoAmI));
            if (profileToUse != null) {
                execCommand.add(profileToUse);
            }
            log.info("Restarting instance");
            log.debug("Restart command: {}", execCommand);
            runNative(execCommand.toArray(String[]::new), 0);
            if (CommonUtility.isSingleDeviceMultiScreen()) {
                MainSingleton.getInstance().restartOnly = true;
            }
            NativeExecutor.exit();
        }
    }

    /**
     * Restart a native instance of Luciferin
     */
    public static void restartNativeInstanceWithCurrentProfile() {
        if (MainSingleton.getInstance().profileArg.equals(Constants.DEFAULT)) {
            NativeExecutor.restartNativeInstance();
        } else {
            NativeExecutor.restartNativeInstance(MainSingleton.getInstance().profileArg);
        }
    }

    /**
     * Restart CMDs
     *
     * @param execCommand commands to execute
     */
    private static void restartCmd(List<String> execCommand) {
        if (NativeExecutor.isFlatpak()) {
            execCommand.addAll(Arrays.stream(Constants.FLATPAK_RUN).toList());
        } else if (NativeExecutor.isSnap()) {
            execCommand.addAll(Arrays.stream(Constants.SNAP_RUN).toList());
        } else if (InstanceConfigurer.getJpackageInstallationPath() != null) {
            execCommand.add(InstanceConfigurer.getJpackageInstallationPath());
        } else {
            execCommand.add(System.getProperty(Constants.JAVA_HOME) + Constants.JAVA_BIN);
            execCommand.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            execCommand.add(Constants.JAR_PARAM);
            execCommand.add(System.getProperty(Constants.JAVA_COMMAND).split("\\s+")[0]);
        }
        if (NativeExecutor.isRunningOnSandbox()) {
            execCommand.add(Constants.RESTART_DELAY);
        }
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
                if (Files.exists(copied)) {
                    Files.delete(copied);
                }
                if (Files.exists(originalPath)) {
                    Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
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
     * Check if Hyprland
     *
     * @return if it's Hyprland
     */
    public static boolean isHyprland() {
        return isLinux() && System.getenv(Constants.DISPLAY_MANAGER_HYPRLAND_CHK) != null;
    }

    /**
     * Check if is running on a sandbox
     *
     * @return true if running on a sandbox
     */
    public static boolean isRunningOnSandbox() {
        return isFlatpak() || isSnap();
    }

    /**
     * Check if Flatpak
     *
     * @return if it's Flatpak
     */
    public static boolean isFlatpak() {
        return System.getenv(Constants.FLATPAK_ID) != null;
    }

    /**
     * Check if Snap
     *
     * @return if it's Snap
     */
    public static boolean isSnap() {
        return System.getenv(Constants.SNAP_NAME) != null;
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
     * Tray support in Linux is minimal and must be enabled using an env variable FIREFLY_FORCE_TRAY
     *
     * @return if the OS supports system tray
     */
    public static boolean isSystemTraySupported() {
        boolean supported = false;
        Enums.TRAY_PREFERENCE trayPreference = Enums.TRAY_PREFERENCE.AUTO;
        if (MainSingleton.getInstance() != null
                && MainSingleton.getInstance().config != null
                && MainSingleton.getInstance().config.getTrayPreference() != null) {
            trayPreference = MainSingleton.getInstance().config.getTrayPreference();
        }
        switch (trayPreference) {
            case AUTO ->
                    supported = ((isWindows() && SystemTray.isSupported()) || (isLinux() && LibAppIndicator.isSupported()));
            case FORCE_AWT -> supported = SystemTray.isSupported();
        }
        return supported;
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
            MainSingleton.getInstance().serial.closePort();
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
        return scrProcess.stream().anyMatch(s -> s.contains(Constants.SCREENSAVER_EXTENSION));
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
                log.info("Changing thread priority to -> {}", priority);
                String[] cmd = {Constants.CMD_POWERSHELL, Constants.CMD_SET_PRIORITY
                        .replace("{0}", String.valueOf(Enums.ThreadPriority.valueOf(priority).getValue()))};
                NativeExecutor.runNative(cmd, 0);
            }, 1);
        }
    }

    /**
     * Detect if user is running a dark theme
     *
     * @return true if dark theme is in use
     */
    public static boolean isDarkTheme() {
        boolean isDark = false;
        if (isWindows()) {
            isDark = Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, Constants.REGISTRY_THEME_PATH, Constants.REGISTRY_THEME_KEY) &&
                    Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, Constants.REGISTRY_THEME_PATH, Constants.REGISTRY_THEME_KEY) == 0;
        } else if (isLinux()) {
            List<String> scrProcess = runNative(Constants.CMD_DARK_THEME_LINUX, Constants.CMD_WAIT_DELAY);
            return scrProcess.stream().filter(s -> s.contains(Constants.CMD_DARK_THEME_LINUX_OUTPUT)).findAny().orElse(null) != null;
        }
        return isDark;
    }

    /**
     * Single Instruction Multiple Data - Advanced Vector Extensions
     * Check if CPU supports SIMD Instructions (AVX, AVX256 or AVX512)
     */
    public static void setSimdAvxInstructions() {
        switch (IntVector.SPECIES_PREFERRED.length()) {
            case 16:
                log.info("CPU SIMD AVX512 Instructions supported");
                break;
            case 8:
                log.info("CPU SIMD AVX256 Instructions supported");
                break;
            case 4:
                log.info("CPU SIMD AVX Instructions supported");
                break;
        }
        MainSingleton.getInstance().setSupportedSpeciesLengthSimd(IntVector.SPECIES_PREFERRED.length());
        switch (Enums.SimdAvxOption.findByValue(MainSingleton.getInstance().config.getSimdAvx())) {
            case AUTO -> MainSingleton.getInstance().setSPECIES(IntVector.SPECIES_PREFERRED);
            case AVX512 -> MainSingleton.getInstance().setSPECIES(IntVector.SPECIES_512);
            case AVX256 -> MainSingleton.getInstance().setSPECIES(IntVector.SPECIES_256);
            case AVX -> MainSingleton.getInstance().setSPECIES(IntVector.SPECIES_128);
            case DISABLED -> MainSingleton.getInstance().setSPECIES(null);
        }
        log.info("SIMD CPU Instructions: {}", Enums.SimdAvxOption.findByValue(MainSingleton.getInstance().config.getSimdAvx()).getBaseI18n());
    }

    /**
     * Check if Night Light is enabled on both Windows and KDE/GNOME
     *
     * @return if Night Light is enabled
     */
    public static boolean isNightLight() {
        boolean nightLightEnabled = false;
        if (NativeExecutor.isWindows()) {
            byte[] data = Advapi32Util.registryGetBinaryValue(WinReg.HKEY_CURRENT_USER, Constants.NIGHT_LIGHT_KEY_PATH, Constants.NIGHT_LIGHT_VALUE_NAME);
            if (data != null && data.length > 41) {
                nightLightEnabled = true;
            }
        } else if (NativeExecutor.isLinux()) {
            try {
                DBusConnection connection = DBusConnectionBuilder.forSessionBus().build();
                Properties propsKde = connection.getRemoteObject(Constants.BUSNAME_KDE_NIGHTLIGHT, Constants.OBJPATH_KDE_NIGHTLIGHT, Properties.class);
                if (propsKde.Get(Constants.BUSNAME_KDE_NIGHTLIGHT, Constants.PROP_KDE_NIGHTLIGHT)) {
                    nightLightEnabled = true;
                }
                connection.close();
            } catch (Exception ignored) {
            }
            try {
                DBusConnection connection = DBusConnectionBuilder.forSessionBus().build();
                Properties propsGnome = connection.getRemoteObject(Constants.BUSNAME_GNOME_NIGHTLIGHT, Constants.OBJPATH_GNOME_NIGHTLIGHT, Properties.class);
                if (propsGnome.Get(Constants.BUSNAME_GNOME_NIGHTLIGHT, Constants.PROP_GNOME_NIGHTLIGHT)) {
                    nightLightEnabled = true;
                }
                connection.close();
            } catch (Exception ignored) {
            }
        }
        return nightLightEnabled;
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
     * Write Windows registry key to Launch Firefly Luciferin when system starts
     */
    public void writeRegistryKey() {
        String installationPath = InstanceConfigurer.getJpackageInstallationPath();
        if (installationPath == null) installationPath = InstanceConfigurer.getInstallationPath();
        if (!installationPath.isEmpty()) {
            log.debug("Writing Windows Registry key");
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, Constants.REGISTRY_KEY_PATH,
                    Constants.REGISTRY_KEY_NAME, installationPath);
            log.debug("Registry key: {}", Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER,
                    Constants.REGISTRY_KEY_PATH, Constants.REGISTRY_KEY_NAME));
        }
    }

}
