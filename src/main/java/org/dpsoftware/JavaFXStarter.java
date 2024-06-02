/*
  JavaFXStarter.java

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

import org.dpsoftware.config.Constants;
import org.dpsoftware.config.InstanceConfigurer;

import java.util.Objects;

/**
 * Where everything begins
 * The reason for this is that the Main app extends Application and has a main method.
 * If that is the case, the LauncherHelper will check for the javafx.graphics module to be present as a named module.
 * If that module is not present, the launch is aborted.
 * This error comes from sun.launcher.LauncherHelper in the java.base module.
 */
public class JavaFXStarter {

    /**
     * Let's play!
     *
     * @param args args[0] contains the child number [1,2,3] to spawn, args[1] contains the profile to use
     */
    public static void main(String... args) {
        // Don't log in this class, log is not initialized yet.
        System.setProperty(Constants.LUCIFERIN_PLACEHOLDER, InstanceConfigurer.getConfigPath());
        FireflyLuciferin.main(Objects.requireNonNull(args));
    }

}