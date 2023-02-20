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
import org.dpsoftware.audio.AudioLoopback;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.dto.StateStatusDto;
import org.dpsoftware.managers.dto.mqttdiscovery.SensorProducingDiscovery;
import org.dpsoftware.network.MessageClient;
import org.dpsoftware.network.tcpUdp.UdpServer;
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

/**
 * A utility class for running native commands and get the results
 */
@Slf4j
@NoArgsConstructor
public final class NativeExecutor {

    public static boolean restartOnly = false;
    public static boolean exitTriggered = false;

    /**
     * This is the real runner that return command output line by line.
     * It waits for the output, don't use it if you don't need the result output.
     *
     * @param cmdToRunUsingArgs Command to run and args, in an array
     * @return A list of string containing the output, empty list if command does not exist
     */
    public static List<String> runNativeWaitForOutput(String[] cmdToRunUsingArgs) {
        return runNative(cmdToRunUsingArgs, true);
    }

    /**
     * This is the real runner that execute commands without waiting for the output.
     * It doesn't wait for the output, use it if you don't need the result output.
     *
     * @param cmdToRunUsingArgs Command to run and args, in an array
     */
    public static void runNativeNoWaitForOutput(String[] cmdToRunUsingArgs) {
        runNative(cmdToRunUsingArgs, false);
    }

    /**
     * This is the real runner that executes command.
     * Don't use this method directly and prefer the runNativeWaitForOutput() or runNativeNoWaitForOutput() shortcut.
     *
     * @param cmdToRunUsingArgs Command to run and args, in an array
     * @param waitForOutput     Example: If you need to exit the app you don't need to wait for the output or the app will not exit
     * @return A list of string containing the output, empty list if command does not exist
     */
    private static List<String> runNative(String[] cmdToRunUsingArgs, boolean waitForOutput) {
        Process process;
        ArrayList<String> cmdOutput = new ArrayList<>();
        try {
            if (cmdToRunUsingArgs.length > 1) {
                process = Runtime.getRuntime().exec(cmdToRunUsingArgs);
            } else {
                process = Runtime.getRuntime().exec(cmdToRunUsingArgs[0]);
            }
        } catch (SecurityException | IOException e) {
            log.debug(CommonUtility.getWord(Constants.CANT_RUN_CMD), Arrays.toString(cmdToRunUsingArgs), e.getMessage());
            return new ArrayList<>(0);
        }
        if (waitForOutput) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    cmdOutput.add(line);
                }
                process.waitFor();
            } catch (IOException e) {
                log.debug(CommonUtility.getWord(Constants.NO_OUTPUT), Arrays.toString(cmdToRunUsingArgs), e.getMessage());
                return new ArrayList<>(0);
            } catch (InterruptedException ie) {
                log.debug(CommonUtility.getWord(Constants.INTERRUPTED_WHEN_READING), Arrays.toString(cmdToRunUsingArgs), ie.getMessage());
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
        String strToRun;
        log.debug("Installation path from spawn={}", getInstallationPath());
        if (NativeExecutor.isWindows()) {
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
            strToRun = Constants.CMD_RUN + command + " " + whoAmISupposedToBe;
        } else {
            strToRun = getInstallationPath() + " " + whoAmISupposedToBe;
        }
        runNativeNoWaitForOutput(new String[]{strToRun});
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
            log.debug("Installation path from restart={}", getInstallationPath());
            List<String> execCommand = new ArrayList<>();
            execCommand.add(getInstallationPath());
            execCommand.add(String.valueOf(JavaFXStarter.whoAmI));
            if (profileToUse != null) {
                execCommand.add("\"" + profileToUse + "\"");
            }
            runNativeNoWaitForOutput(execCommand.toArray(String[]::new));
            if (CommonUtility.isSingleDeviceMultiScreen()) {
                restartOnly = true;
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
                log.debug(e.getMessage());
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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!exitTriggered) {
                log.debug("Exit hook triggered.");
                exitTriggered = true;
                lastWill();
//                CommonUtility.sleepMilliseconds(100);
//                Runtime.getRuntime().halt(0);
            }
        }));
    }

    /**
     * This is the last will before exiting the app. This method is called when manually exiting the app or
     * when the OS entered the shutdown/reboot phase.
     */
    private static void lastWill() {
        if (!Enums.PowerSaving.DISABLED.equals(LocalizedEnum.fromBaseStr(Enums.PowerSaving.class,
                FireflyLuciferin.config.getPowerSaving()))) {
            CommonUtility.turnOffLEDs(FireflyLuciferin.config, 1, true);
        }
        if (FireflyLuciferin.config.isMqttEnable()) {
            SensorProducingDiscovery sensorProducingDiscovery = new SensorProducingDiscovery();
            sensorProducingDiscovery.setZeroValue();
        }
    }

    /**
     * Gracefully exit the app, this method is called manually.
     */
    public static void exit() {
        if (FireflyLuciferin.RUNNING) {
            FireflyLuciferin.guiManager.stopCapturingThreads(true);
        }
        exitTriggered = true;
        log.debug(Constants.CLEAN_EXIT);
        UdpServer.udpBroadcastReceiverRunning = false;
        exitOtherInstances();
        if (FireflyLuciferin.serial != null) {
            FireflyLuciferin.serial.removeEventListener();
            FireflyLuciferin.serial.close();
        }
        AudioLoopback.RUNNING_AUDIO = false;
        lastWill();
        CommonUtility.sleepSeconds(2);
        System.exit(0);
    }

    /**
     * Exit single device instances
     */
    static void exitOtherInstances() {
        if (!NativeExecutor.restartOnly) {
            if (CommonUtility.isSingleDeviceMainInstance()) {
                StateStatusDto.closeOtherInstaces = true;
                CommonUtility.sleepSeconds(6);
            } else if (CommonUtility.isSingleDeviceOtherInstance()) {
                MessageClient.msgClient.sendMessage(Constants.EXIT);
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
        List<String> scrProcess = runNativeWaitForOutput(scrCmd);
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

}
