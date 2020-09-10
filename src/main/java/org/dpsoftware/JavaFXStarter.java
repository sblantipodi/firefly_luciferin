/*
  JavaFXStarter.java

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

/**
 * This error comes from sun.launcher.LauncherHelper in the java.base module.
 * The reason for this is that the Main app extends Application and has a main method.
 * If that is the case, the LauncherHelper will check for the javafx.graphics module to be present as a named module.
 * If that module is not present, the launch is aborted.
 */
public class JavaFXStarter {

    public static void main(String[] args) {

        FireflyLuciferin.main(args);

    }

}