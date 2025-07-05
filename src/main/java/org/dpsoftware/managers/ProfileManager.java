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
    Double gpuLoad = 0.0;
    boolean gpuLoadThreadRunning = false;
    Double cpuLoad = 0.0;
    boolean cpuLoadThreadRunning = false;
    boolean profileThreadRunning = false;
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
            Runnable profileTask = () -> {
                AtomicReference<String> profileNameToUse = new AtomicReference<>("");
                boolean profileInUseStillActive = false;
                for (ProfileManager profile : profileConfigs) {
                    if ((profile.getConfiguration().getGpuThreshold() > 0) && (getGpuLoad() != null) && (getGpuLoad() > profile.getConfiguration().getGpuThreshold())) {
                        log.trace("High GPU usage detected ({}%), profile: {}", getGpuLoad(), profile.getProfileName());
                        profileNameToUse.set(profile.getProfileName());
                        if (MainSingleton.getInstance().profileArg.equals(profile.getProfileName())) {
                            profileInUseStillActive = true;
                        }
                    }
                    if ((profile.getConfiguration().getCpuThreshold() > 0) && (getCpuLoad() != null) && (getCpuLoad() > profile.getConfiguration().getCpuThreshold())) {
                        log.trace("High CPU usage detected ({}%), profile: {}", getCpuLoad(), profile.getProfileName());
                        profileNameToUse.set(profile.getProfileName());
                        if (MainSingleton.getInstance().profileArg.equals(profile.getProfileName())) {
                            profileInUseStillActive = true;
                        }
                    }
                    for (String process : profile.getConfiguration().getProfileProcesses()) {
                        boolean isRunning = ProcessHandle.allProcesses()
                                .map(ProcessHandle::info)
                                .map(info -> info.command().orElse("").toLowerCase())
                                .anyMatch(cmd -> cmd.contains(process.toLowerCase()));
                        if (isRunning) {
                            log.trace("Process \"{}\" detected, profile: {}", process, profile.getProfileName());
                            profileNameToUse.set(profile.getProfileName());
                            if (MainSingleton.getInstance().profileArg.equals(profile.getProfileName())) {
                                profileInUseStillActive = true;
                            }
                        }
                    }
                }
                if (!profileNameToUse.get().isEmpty() && !profileInUseStillActive) {
                    log.debug("Profile switch triggered, switch to: {}.", profileNameToUse.get());
                    NativeExecutor.restartNativeInstance(profileNameToUse.get());
                }
                if (profileNameToUse.get().isEmpty() && !MainSingleton.getInstance().profileArg.equals(Constants.DEFAULT)) {
                    log.debug("Profile switch triggered, switch to default profile.");
                    NativeExecutor.restartNativeInstance();
                }
            };
            profileService.scheduleAtFixedRate(profileTask, Constants.PROFILE_THREAD_DELAY, Constants.CMD_WAIT_DELAY, TimeUnit.MILLISECONDS);
            profileThreadRunning = true;
        }
    }

    /**
     * Get CPU usage via OperatingSystemMXBean (Java 8+)
     */
    void updateCpuUsage() {
        if (NativeExecutor.isWindows() && !cpuLoadThreadRunning) {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            cpuService = Executors.newScheduledThreadPool(1);
            Runnable cpuTask = () -> cpuLoad = osBean.getCpuLoad() * 100;
            cpuService.scheduleAtFixedRate(cpuTask, Constants.PROFILE_THREAD_DELAY, Constants.CMD_WAIT_DELAY, TimeUnit.MILLISECONDS);
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
            gpuService.scheduleAtFixedRate(gpuTask, Constants.PROFILE_THREAD_DELAY, Constants.CMD_WAIT_DELAY, TimeUnit.MILLISECONDS);
            gpuLoadThreadRunning = true;
        }
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
        gpuLoad = 0.0;
        gpuLoadThreadRunning = false;
        cpuLoad = 0.0;
        cpuLoadThreadRunning = false;
        profileThreadRunning = false;
        threadDelay = Constants.SPAWN_INSTANCE_WAIT_START_DELAY;
    }

}
