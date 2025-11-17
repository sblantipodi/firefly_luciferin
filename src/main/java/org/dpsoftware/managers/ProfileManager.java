/*
  ProfileManager.java

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
package org.dpsoftware.managers;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.management.OperatingSystemMXBean;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Setter
@Slf4j
public class ProfileManager {

    @Getter
    private final static ProfileManager instance;

    static {
        instance = new ProfileManager();
    }

    String profileName;
    Configuration configuration;
    ScheduledExecutorService profileService;
    ScheduledExecutorService cpuService;
    ScheduledExecutorService gpuService;
    ScheduledExecutorService windowService;
    Double gpuLoad = null;
    boolean gpuLoadThreadRunning = false;
    Double cpuLoad = null;
    boolean cpuLoadThreadRunning = false;
    boolean profileThreadRunning = false;
    boolean isFullscreen = false;
    int threadDelay = Constants.PROFILE_THREAD_DELAY;

    /**
     * Manage profiles that requires a profile switch.
     */
    public void manageExecProfiles() {
        boolean activateProfileSerice = false;
        List<ProfileManager> profileConfigs = new ArrayList<>();
        StorageManager sm = new StorageManager();
        Set<String> profileNames = sm.listProfilesForThisInstance();
        for (String profileName : profileNames) {
            Configuration baseProfileConfig = sm.readProfileConfig(profileName);
            ProfileManager profileConfig = new ProfileManager();
            profileConfig.setProfileName(profileName);
            profileConfig.setConfiguration(baseProfileConfig);
            profileConfigs.add(profileConfig);
            if (profileConfig.getConfiguration().isCheckFullScreen()) {
                log.debug("Activating window fullscreen check for profiles");
                activateProfileSerice = true;
                updateFullScreenWindow();
            }
            if (profileConfig.getConfiguration().getCpuThreshold() > 0) {
                log.debug("Activating CPU threshold management for profiles.");
                activateProfileSerice = true;
                updateCpuUsage();
            }
            if (profileConfig.getConfiguration().getGpuThreshold() > 0) {
                log.debug("Activating GPU threshold management for profiles.");
                activateProfileSerice = true;
                updateGpuUsage();
            }
            if (profileConfig.getConfiguration().getProfileProcesses() != null && !profileConfig.getConfiguration().getProfileProcesses().isEmpty()) {
                log.debug("Activating process management for profiles");
                activateProfileSerice = true;
            }
        }
        if (activateProfileSerice && !profileThreadRunning) {
            profileService = Executors.newScheduledThreadPool(1);
            Runnable profileTask = getProfileTask(profileConfigs);
            profileService.scheduleAtFixedRate(profileTask, threadDelay, Constants.CMD_WAIT_DELAY, TimeUnit.MILLISECONDS);
            profileThreadRunning = true;
        }
    }

    /**
     * Run a task to check for profile switches based on CPU and GPU load, processes, and fullscreen status.
     *
     * @return profile task runnable
     */
    private Runnable getProfileTask(List<ProfileManager> profileConfigs) {
        return () -> {
            AtomicReference<String> profileNameToUse = new AtomicReference<>("");
            boolean profileInUseStillActive = false;
            for (ProfileManager profile : profileConfigs) {
                if (profile.getConfiguration().isCheckFullScreen() && isFullscreen) {
                    log.trace("Full screen windows detected, profile: {}", profile.getProfileName());
                    profileInUseStillActive = isProfileInUseStillActive(profile, profileNameToUse, profileInUseStillActive);
                } else if ((profile.getConfiguration().getGpuThreshold() > 0) && (getGpuLoad() != null) && (getGpuLoad() > profile.getConfiguration().getGpuThreshold())) {
                    log.trace("High GPU usage detected ({}%), profile: {}", getGpuLoad(), profile.getProfileName());
                    profileInUseStillActive = isProfileInUseStillActive(profile, profileNameToUse, profileInUseStillActive);
                } else if ((profile.getConfiguration().getCpuThreshold() > 0) && (getCpuLoad() != null) && (getCpuLoad() > profile.getConfiguration().getCpuThreshold())) {
                    log.trace("High CPU usage detected ({}%), profile: {}", getCpuLoad(), profile.getProfileName());
                    profileInUseStillActive = isProfileInUseStillActive(profile, profileNameToUse, profileInUseStillActive);
                } else {
                    for (String process : profile.getConfiguration().getProfileProcesses()) {
                        boolean isRunning = ProcessHandle.allProcesses()
                                .map(ProcessHandle::info)
                                .map(info -> info.command().orElse("").toLowerCase())
                                .anyMatch(cmd -> cmd.contains(process.toLowerCase()));
                        if (isRunning) {
                            log.trace("Process \"{}\" detected, profile: {}", process, profile.getProfileName());
                            profileInUseStillActive = isProfileInUseStillActive(profile, profileNameToUse, profileInUseStillActive);
                        }
                    }
                }
            }
            if (!profileNameToUse.get().isEmpty() && !profileInUseStillActive) {
                log.debug("Profile switch triggered");
                if (!MainSingleton.getInstance().getGuiManager().getStage(Constants.FXML_SETTINGS).isShowing()) {
                    log.debug("Switch to: {}.", profileNameToUse.get());
                    NativeExecutor.restartNativeInstance(profileNameToUse.get());
                }
            }
            if (profileNameToUse.get().isEmpty() && !MainSingleton.getInstance().profileArg.equals(Constants.DEFAULT)) {
                log.debug("Profile switch triggered");
                if (!MainSingleton.getInstance().getGuiManager().getStage(Constants.FXML_SETTINGS).isShowing()) {
                    log.debug("Switch to default profile.");
                    NativeExecutor.restartNativeInstance();
                }
            }
        };
    }

    /**
     * Check if the current profile is still active.
     * If the profile is in use, it sets the profile name to use and returns true.
     *
     * @param profile                 the profile to check
     * @param profileNameToUse        the reference to set the profile name
     * @param profileInUseStillActive the current status of the profile
     * @return true if the profile is still active, false otherwise
     */
    private static boolean isProfileInUseStillActive(ProfileManager profile, AtomicReference<String> profileNameToUse, boolean profileInUseStillActive) {
        profileNameToUse.set(profile.getProfileName());
        if (MainSingleton.getInstance().profileArg.equals(profile.getProfileName())) {
            profileInUseStillActive = true;
        }
        return profileInUseStillActive;
    }

    /**
     * Get CPU usage via OperatingSystemMXBean (Java 8+)
     */
    void updateCpuUsage() {
        if (NativeExecutor.isWindows() && !cpuLoadThreadRunning) {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            cpuService = Executors.newScheduledThreadPool(1);
            Runnable cpuTask = () -> cpuLoad = osBean.getCpuLoad() * 100;
            cpuService.scheduleAtFixedRate(cpuTask, 0, Constants.CMD_WAIT_DELAY, TimeUnit.MILLISECONDS);
            cpuLoadThreadRunning = true;
        }
    }

    /**
     * Get GPU usage via PowerShell command (Windows only)
     * This method is used to get the GPU usage in percentage.
     * It runs a PowerShell command and parses the output.
     */
    void updateGpuUsage() {
        if (NativeExecutor.isWindows() && !gpuLoadThreadRunning) {
            gpuService = Executors.newScheduledThreadPool(1);
            Runnable gpuTask = () -> {
                String[] cmd = {Constants.CMD_SHELL_FOR_CMD_EXECUTION, Constants.CMD_PARAM_FOR_CMD_EXECUTION, Constants.CMD_GPU_USAGE};
                List<String> commandOutput = NativeExecutor.runNative(cmd, Constants.CMD_WAIT_DELAY);
                for (String s : commandOutput) {
                    String line = s.trim();
                    if (!line.isEmpty()) {
                        try {
                            gpuLoad = Double.parseDouble(line);
                            break;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

            };
            gpuService.scheduleAtFixedRate(gpuTask, 0, Constants.CMD_WAIT_DELAY, TimeUnit.MILLISECONDS);
            gpuLoadThreadRunning = true;
        }
    }

    /**
     * Update the fullscreen window status.
     * This method checks if the current foreground window is in fullscreen mode.
     * It uses the User32 library to get the window title and dimensions, and compares them with the screen resolution.
     */
    void updateFullScreenWindow() {
        User32 user32 = User32.INSTANCE;
        windowService = Executors.newScheduledThreadPool(1);
        Runnable gpuTask = () -> {
            WinDef.HWND hwnd = user32.GetForegroundWindow();
            char[] buffer = new char[1024];
            user32.GetWindowText(hwnd, buffer, 1024);
            String windowTitle = Native.toString(buffer);
            log.trace("Window title: {}", windowTitle);
            WinDef.RECT rect = new WinDef.RECT();
            user32.GetWindowRect(hwnd, rect);
            int windowWidth = rect.right - rect.left;
            int windowHeight = rect.bottom - rect.top;
            log.trace("Window dimension: {}x{}", windowWidth, windowHeight);
            int screenWidth = user32.GetSystemMetrics(WinUser.SM_CXSCREEN);
            int screenHeight = user32.GetSystemMetrics(WinUser.SM_CYSCREEN);
            log.trace("Screen resolution: {}x{}", screenWidth, screenHeight);
            isFullscreen = ((windowWidth == screenWidth) && (windowHeight == screenHeight) && !(windowTitle.equalsIgnoreCase(Constants.PROGRAM_MANAGER)));
            log.trace("Current window is Fullscreen: {}", isFullscreen);
        };
        windowService.scheduleAtFixedRate(gpuTask, 0, Constants.CMD_WAIT_DELAY, TimeUnit.MILLISECONDS);
        gpuLoadThreadRunning = true;
    }

    /**
     * Reset all values and stop all threads.
     * This method is used to reset the profile manager to its initial state.
     */
    public void resetValues() {
        profileName = null;
        configuration = null;
        if (profileService != null) {
            profileService.shutdownNow();
            profileService = null;
        }
        if (cpuService != null) {
            cpuService.shutdownNow();
            cpuService = null;
        }
        if (gpuService != null) {
            gpuService.shutdownNow();
            gpuService = null;
        }
        if (windowService != null) {
            windowService.shutdownNow();
            windowService = null;
        }
        gpuLoad = 0.0;
        gpuLoadThreadRunning = false;
        cpuLoad = 0.0;
        cpuLoadThreadRunning = false;
        profileThreadRunning = false;
        isFullscreen = false;
        threadDelay = Constants.PROFILE_THREAD_DELAY;
    }

}
