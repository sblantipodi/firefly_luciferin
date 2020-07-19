/*
  JavaFXStarter.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
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

        FastScreenCapture.main(args);

    }

}