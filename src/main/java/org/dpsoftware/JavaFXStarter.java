/*
  JavaFXStarter.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2021  Davide Perini

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

import lombok.extern.slf4j.Slf4j;

/**
 * Where everything begins
 * The reason for this is that the Main app extends Application and has a main method.
 * If that is the case, the LauncherHelper will check for the javafx.graphics module to be present as a named module.
 * If that module is not present, the launch is aborted.
 * This error comes from sun.launcher.LauncherHelper in the java.base module.
 */
@Slf4j
public class JavaFXStarter {

    // Who am I supposed to be? Used to manage multiple instances of Luciferin running at the same time
    public static int whoAmI = 1;
    public static boolean spawnInstances = true;

    /**
     * Let's play!
     * @param args an array containing the child number [1,2,3] to spawn
     */
    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            log.debug("Starting instance #: " + args[0]);
            whoAmI = Integer.parseInt(args[0]);
            spawnInstances = false;
        } else {
            log.debug("Starting default instance");
        }
        FireflyLuciferin.main(args);
    }

}