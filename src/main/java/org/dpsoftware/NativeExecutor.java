/*
  NativeExecutor.java

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
package org.dpsoftware;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import jdk.incubator.vector.IntVector;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.*;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.bindings.appindicator.LibAppIndicator;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.SerialManager;
import org.dpsoftware.managers.StorageManager;
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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A utility class for running native commands and get the results
 */
@Slf4j
@NoArgsConstructor
public final class NativeExecutor {

    private static final String RESTART_LOCK_PREFIX = "RESTART_LOCK=";
    private static final Duration RESTART_LOCK_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RESTART_KILL_TIMEOUT = Duration.ofSeconds(5);
    private static final int RESTART_EXIT_TIMEOUT_SECONDS = 5;
    private enum ShutdownState { RUNNING, RESTARTING, EXITING }
    private static final AtomicReference<ShutdownState> shutdownState = new AtomicReference<>(ShutdownState.RUNNING);
    private static FileChannel restartLockChannel;
    private static FileLock restartLock;

    /**
     * This is the real runner that executes command. Non blocking method.
     *
     * @param cmdToRunUsingArgs Command to run and args, in an array
     * @return A list of string containing the output, empty list if command does not exist
     */
    public static List<String> runNative(String[] cmdToRunUsingArgs) {
        ArrayList<String> cmdOutput = new ArrayList<>();
        try {
            log.trace("Executing cmd={}", Arrays.toString(cmdToRunUsingArgs));
            ProcessBuilder processBuilder = new ProcessBuilder(cmdToRunUsingArgs);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                long start = System.currentTimeMillis();
                while ((line = reader.readLine()) != null) {
                    log.trace(line);
                    cmdOutput.add(line);
                    if (System.currentTimeMillis() - start > Constants.CMD_WAIT_DELAY) {
                        log.error("Timeout reading output of {}", Arrays.toString(cmdToRunUsingArgs));
                        process.destroy();
                        break;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            log.error("Error executing command: {}", e.getMessage(), e);
        }
        return cmdOutput;
    }

    /**
     * This is the real runner that executes command. Blocking method.
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
     * Start a native command without waiting and report whether the process was launched.
     * The other runNative overloads keep their existing output and error handling behavior.
     *
     * @param cmdToRunUsingArgs command to run and its arguments
     * @param inheritIO whether the child process inherits this process's standard input, output, and error streams
     * @return true if the process was started, false otherwise
     */
    static boolean runNative(String[] cmdToRunUsingArgs, boolean inheritIO) {
        try {
            log.trace("Executing cmd={}", Arrays.toString(cmdToRunUsingArgs));
            ProcessBuilder processBuilder = new ProcessBuilder(cmdToRunUsingArgs);
            if (inheritIO) {
                processBuilder.inheritIO();
            }
            processBuilder.start();
            return true;
        } catch (IOException | RuntimeException e) {
            log.error("Could not start command: {}", Arrays.toString(cmdToRunUsingArgs), e);
            return false;
        }
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
        execCommand.add(MainSingleton.getInstance().profileArg);
        if (MainSingleton.getInstance().isHeadlessMode()) {
            execCommand.add(Constants.HEADLESS_ARG);
        }
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
            // We don't use NativeExecutor.exit() here because we need to avoid race conditions
            System.exit(0);
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
        MainSingleton main = MainSingleton.getInstance();
        if ((NativeExecutor.isWindows() || NativeExecutor.isLinux())
                && shutdownState.compareAndSet(ShutdownState.RUNNING, ShutdownState.RESTARTING)) {
            List<String> execCommand = new ArrayList<>();
            restartCmd(execCommand);
            int lockArgumentIndex = execCommand.size();
            execCommand.add(null);
            execCommand.add(String.valueOf(main.whoAmI));
            String effectiveProfile = profileToUse != null ? profileToUse : main.profileArg;
            execCommand.add(effectiveProfile);
            if (main.isHeadlessMode()) {
                writeProfileFile(effectiveProfile);
            }
            if (main.isHeadlessMode()) {
                execCommand.add(Constants.HEADLESS_ARG);
            }
            Path lockPath = null;
            boolean launched = false;
            try {
                lockPath = createRestartLock();
                log.info("Restart lock created at {}", lockPath);
                execCommand.set(lockArgumentIndex, restartLockArgument(lockPath, ProcessHandle.current()));
                log.info("Restarting instance");
                log.debug("Restart command: {}", execCommand);
                launched = runNative(execCommand.toArray(String[]::new), true);
            } catch (IOException | RuntimeException e) {
                log.error("Could not prepare replacement instance", e);
            }
            if (!launched) {
                releaseRestartLock();
                if (lockPath != null) {
                    try {
                        Files.deleteIfExists(lockPath);
                    } catch (IOException cleanupError) {
                        log.warn("Could not remove restart lock {}", lockPath, cleanupError);
                    }
                }
                shutdownState.compareAndSet(ShutdownState.RESTARTING, ShutdownState.RUNNING);
                return;
            }
            // The replacement waits on our file lock, so exit without blocking on capture cleanup.
            main.restartOnly = true;
            main.exitTriggered = true;
            startRestartExitWatchdog();
            log.info("Replacement process launched; exiting previous instance");
            System.exit(0);
        }
    }

    /**
     * Restart a native instance of Luciferin
     */
    public static void restartNativeInstanceWithCurrentProfile() {
        NativeExecutor.restartNativeInstance(MainSingleton.getInstance().profileArg);
    }

    /**
     * Keep an OS file lock until this process exits. This works across launcher and PID namespaces.
     *
     * @return path of the newly acquired restart lock
     * @throws IOException if the lock file cannot be created or acquired
     */
    private static Path createRestartLock() throws IOException {
        Path lockDirectory = Paths.get(InstanceConfigurer.getConfigPath());
        Files.createDirectories(lockDirectory);
        Path lockPath = lockDirectory.resolve(".restart-" + UUID.randomUUID() + ".lock");
        try {
            restartLockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            restartLock = restartLockChannel.lock();
            return lockPath;
        } catch (IOException | RuntimeException e) {
            releaseRestartLock();
            Files.deleteIfExists(lockPath);
            throw e;
        }
    }

    /**
     * Release the restart lock after a failed attempt to launch the replacement process.
     */
    private static void releaseRestartLock() {
        try {
            if (restartLock != null) restartLock.close();
        } catch (IOException e) {
            log.warn("Could not release restart lock", e);
        }
        try {
            if (restartLockChannel != null) restartLockChannel.close();
        } catch (IOException e) {
            log.warn("Could not close restart lock channel", e);
        }
        restartLock = null;
        restartLockChannel = null;
    }

    /**
     * Include the previous process identity so the replacement cannot kill a reused PID.
     */
    static String restartLockArgument(Path lockPath, ProcessHandle owner) {
        Instant startedAt = owner.info().startInstant()
                .orElseThrow(() -> new IllegalStateException("Cannot identify the restarting process"));
        return RESTART_LOCK_PREFIX + lockPath.getFileName() + ":" + owner.pid() + ":" + startedAt.toEpochMilli();
    }

    /**
     * Wait for the previous process to release its lock. After ten seconds, kill only
     * the process that created this restart request, then acquire the lock before starting.
     */
    static void waitForRestartLock(String restartArgument) {
        waitForRestartLock(restartArgument, Paths.get(InstanceConfigurer.getConfigPath()), RESTART_LOCK_TIMEOUT);
    }

    static void waitForRestartLock(String restartArgument, Path lockDirectory, Duration timeout) {
        String[] parts = restartArgument.substring(RESTART_LOCK_PREFIX.length()).split(":", -1);
        if (parts.length != 3 || !parts[0].matches("\\.restart-[0-9a-fA-F-]{36}\\.lock")) {
            throw new IllegalArgumentException("Invalid restart lock argument");
        }
        long ownerPid;
        long ownerStartedAt;
        try {
            ownerPid = Long.parseLong(parts[1]);
            ownerStartedAt = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid restart process identity", e);
        }
        if (ownerPid <= 0 || ownerStartedAt <= 0) {
            throw new IllegalArgumentException("Invalid restart process identity");
        }
        Path lockPath = lockDirectory.resolve(parts[0]);
        log.info("Waiting for previous instance to release restart lock {}", lockPath);
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE)) {
            FileLock acquired = tryRestartLockUntil(channel, timeout);
            if (acquired == null) {
                log.warn("Restart lock still held after {} seconds; terminating previous instance PID {}",
                        timeout.toSeconds(), ownerPid);
                terminateRestartOwner(ownerPid, ownerStartedAt);
                acquired = tryRestartLockUntil(channel, RESTART_KILL_TIMEOUT);
            }
            if (acquired == null) {
                throw new IllegalStateException("Previous instance did not release restart lock " + lockPath);
            }
            try (FileLock ignored = acquired) {
                log.info("Previous instance released restart lock {}", lockPath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not wait for previous instance", e);
        }
        try {
            Files.deleteIfExists(lockPath);
        } catch (IOException e) {
            log.warn("Could not remove restart lock {}", lockPath, e);
        }
    }

    private static FileLock tryRestartLockUntil(FileChannel channel, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            try {
                FileLock acquired = channel.tryLock();
                if (acquired != null) {
                    return acquired;
                }
            } catch (OverlappingFileLockException e) {
                // The previous instance may be another lock holder in this JVM during tests.
            } catch (IOException e) {
                throw new IllegalStateException("Could not acquire restart lock", e);
            }
            if (System.nanoTime() >= deadline) {
                return null;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for restart lock", e);
            }
        }
    }

    private static void terminateRestartOwner(long pid, long startedAtMillis) {
        ProcessHandle owner = ProcessHandle.of(pid)
                .orElseThrow(() -> new IllegalStateException("Previous instance PID " + pid + " is not visible"));
        long actualStartedAt = owner.info().startInstant()
                .orElseThrow(() -> new IllegalStateException("Cannot verify previous instance PID " + pid))
                .toEpochMilli();
        if (pid == ProcessHandle.current().pid() || actualStartedAt != startedAtMillis) {
            throw new IllegalStateException("Restart process identity changed; refusing to kill PID " + pid);
        }
        if (owner.isAlive() && !owner.destroyForcibly()) {
            throw new IllegalStateException("Could not terminate previous instance PID " + pid);
        }
    }

    /**
     * Check whether a startup argument identifies a restart lock.
     *
     * @param argument startup argument to inspect
     * @return true if the argument contains a restart lock file name
     */
    static boolean isRestartLockArgument(String argument) {
        return argument.startsWith(RESTART_LOCK_PREFIX);
    }

    /**
     * Writes the active profile name to a file if certain conditions are met.
     *
     * @param profileToUse write profilename to file, useful for systemctl restart
     */
    private static void writeProfileFile(String profileToUse) {
        if (profileToUse != null && !profileToUse.isEmpty() && !LabelKey.DEFAULT.equals(profileToUse) && !CommonUtility.getWord(LabelKey.DEFAULT).equals(profileToUse)) {
            new StorageManager().writeStartProfileFile(profileToUse);
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
        return isLinux() && System.getenv(EnvConstants.DISPLAY_MANAGER_CHK) != null && System.getenv(EnvConstants.DISPLAY_MANAGER_CHK).equalsIgnoreCase(Constants.WAYLAND);
    }

    /**
     * Check if Hyprland
     *
     * @return if it's Hyprland
     */
    public static boolean isHyprland() {
        return isLinux() && System.getenv(EnvConstants.DISPLAY_MANAGER_HYPRLAND_CHK) != null;
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
        return System.getenv(EnvConstants.FLATPAK_ID) != null;
    }

    /**
     * Check if Snap
     *
     * @return if it's Snap
     */
    public static boolean isSnap() {
        return System.getenv(EnvConstants.SNAP_NAME) != null && System.getenv(EnvConstants.SNAP_NAME).equals("fireflyluciferin");
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
        if (MainSingleton.getInstance() != null) {
            if (MainSingleton.getInstance().isHeadlessMode()) {
                return false;
            }
            if (MainSingleton.getInstance().config != null && MainSingleton.getInstance().config.getTrayPreference() != null) {
                trayPreference = MainSingleton.getInstance().config.getTrayPreference();
            }
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
        if (!shutdownState.compareAndSet(ShutdownState.RUNNING, ShutdownState.EXITING)
                && !shutdownState.compareAndSet(ShutdownState.RESTARTING, ShutdownState.EXITING)) {
            return;
        }
        try {
            if (MainSingleton.getInstance().RUNNING) {
                MainSingleton.getInstance().guiManager.stopCapturingThreads(true);
            }
            if (MainSingleton.getInstance().serial != null) {
                SerialManager sm = new SerialManager();
                sm.closeSerial();
            }
            log.info(Constants.CLEAN_EXIT);
            NetworkSingleton.getInstance().udpBroadcastReceiverRunning = false;
            exitOtherInstances();
            AudioSingleton.getInstance().RUNNING_AUDIO = false;
        } catch (RuntimeException e) {
            log.error("Error during shutdown", e);
        } finally {
            MainSingleton.getInstance().exitTriggered = true;
            CommonUtility.delaySeconds(() -> {
                try {
                    if (!MainSingleton.getInstance().restartOnly) {
                        lastWill();
                    }
                } catch (RuntimeException | Error e) {
                    log.error("Error during shutdown cleanup", e);
                }
                System.exit(0);
            }, 2);
        }
    }

    /**
     * Native capture or network cleanup can block indefinitely. Only the restarting JVM is halted.
     */
    private static void startRestartExitWatchdog() {
        Thread watchdog = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(RESTART_EXIT_TIMEOUT_SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            log.error("Restart shutdown timed out; forcing this instance to exit");
            Runtime.getRuntime().halt(0);
        }, "luciferin-restart-exit-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
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
     * Check if HDR is active
     */
    public static boolean isHdrActive() {
        if (isWindows()) {
            try {
                String baseKey = Constants.REGISTRY_HDR_KEY_PATH;
                String[] subKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, baseKey);
                for (String monitorKey : subKeys) {
                    String fullKey = baseKey + "\\" + monitorKey;
                    if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullKey, Constants.REGISTRY_HDR_VAL)) {
                        int hdrEnabled = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, Constants.REGISTRY_HDR_VAL);
                        if (hdrEnabled == 1) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("HDR registry check failed", e);
            }
        }
        return false;
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
            try (DBusConnection connection = DBusConnectionBuilder.forSessionBus().build()) {
                try {
                    Properties propsKde = connection.getRemoteObject(Constants.BUSNAME_KDE_NIGHTLIGHT, Constants.OBJPATH_KDE_NIGHTLIGHT, Properties.class);
                    if (propsKde.Get(Constants.BUSNAME_KDE_NIGHTLIGHT, Constants.PROP_KDE_NIGHTLIGHT)) {
                        nightLightEnabled = true;
                    }
                } catch (Exception e) {
                    log.debug("KDE nightlight DBus check failed", e);
                }
                if (!nightLightEnabled) {
                    try {
                        Properties propsGnome = connection.getRemoteObject(Constants.BUSNAME_GNOME_NIGHTLIGHT, Constants.OBJPATH_GNOME_NIGHTLIGHT, Properties.class);
                        if (propsGnome.Get(Constants.BUSNAME_GNOME_NIGHTLIGHT, Constants.PROP_GNOME_NIGHTLIGHT)) {
                            nightLightEnabled = true;
                        }
                    } catch (Exception e) {
                        log.debug("GNOME nightlight DBus check failed", e);
                    }
                }
            } catch (Exception e) {
                log.debug("DBus session bus connection failed", e);
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
