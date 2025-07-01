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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;

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

    String profileName;
    Configuration configuration;

    /**
     * Manage profiles that requires a profile switch.
     */
    public static void manageExecProfiles() {
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
                NativeExecutor.updateCpuUsage();
            }
            if (profileConfig.getConfiguration().getGpuThreshold() > 0) {
                log.debug("Activating GPU threshold management for profiles.");
                activateProfileSerice = true;
                NativeExecutor.updateGpuUsage();
            }
            if (profileConfig.getConfiguration().getProfileProcesses() != null && !profileConfig.getConfiguration().getProfileProcesses().isEmpty()) {
                log.debug("Activating process management for profiles");
                activateProfileSerice = true;
            }
        }
        if (activateProfileSerice && !ManagerSingleton.getInstance().profileThreadRunning) {
            ScheduledExecutorService profileService = Executors.newScheduledThreadPool(1);
            Runnable profileTask = () -> {
                AtomicReference<String> profileNameToUse = new AtomicReference<>("");
                boolean profileInUseStillActive = false;
                for (ProfileManager profile : profileConfigs) {
                    if ((profile.getConfiguration().getGpuThreshold() > 0) && (ManagerSingleton.getInstance().getGpuLoad() != null) && (ManagerSingleton.getInstance().getGpuLoad() > profile.getConfiguration().getGpuThreshold())) {
                        log.trace("High GPU usage detected ({}%), profile: {}", ManagerSingleton.getInstance().getGpuLoad(), profile.getProfileName());
                        profileNameToUse.set(profile.getProfileName());
                        if (MainSingleton.getInstance().profileArg.equals(profile.getProfileName())) {
                            profileInUseStillActive = true;
                        }
                    }
                    if ((profile.getConfiguration().getCpuThreshold() > 0) && (ManagerSingleton.getInstance().getCpuLoad() != null) && (ManagerSingleton.getInstance().getCpuLoad() > profile.getConfiguration().getCpuThreshold())) {
                        log.trace("High CPU usage detected ({}%), profile: {}", ManagerSingleton.getInstance().getCpuLoad(), profile.getProfileName());
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
            ManagerSingleton.getInstance().profileThreadRunning = true;
        }
    }

}
