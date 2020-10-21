/*
  NativeExecutor.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020  Davide Perini

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import lombok.NoArgsConstructor;
import org.dpsoftware.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An utility class for running native commands and get the results
 */
@NoArgsConstructor
public final class NativeExecutor {

    private static final Logger logger = LoggerFactory.getLogger(NativeExecutor.class);

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
            logger.debug(Constants.CANT_RUN_CMD, Arrays.toString(cmdToRunUsingArgs), e.getMessage());
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
            logger.debug(Constants.NO_OUTPUT, Arrays.toString(cmdToRunUsingArgs), e.getMessage());
            return new ArrayList<>(0);
        } catch (InterruptedException ie) {
            logger.debug(Constants.INTERRUPTED_WHEN_READING, Arrays.toString(cmdToRunUsingArgs), ie.getMessage());
            Thread.currentThread().interrupt();
        }

        return sa;

    }

    /**
     * Write Windows registry key to Launch Firefly Luciferin when system starts
     */
    public void writeRegistryKey() {

        String installationPath = getInstallationPath();
        if (!installationPath.isEmpty()) {
            logger.debug("Writing Windows Registry key");
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, Constants.REGISTRY_KEY_PATH,
                    Constants.REGISTRY_KEY_NAME, installationPath);
        }
        logger.debug(Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER,
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
    String getInstallationPath() {

        String luciferinClassPath = FireflyLuciferin.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        logger.debug("Installation path={}", luciferinClassPath);
        if (luciferinClassPath.contains(".jar")) {
            return luciferinClassPath.replace("/", "\\")
                    .substring(1, luciferinClassPath.length() - Constants.REGISTRY_JARNAME.length())
                    .replace("%20", " ") + Constants.REGISTRY_KEY_VALUE;
        }
        return Constants.REGISTRY_DEFAULT_KEY_VALUE;

    }

}
