/*
  NativeExecutor.java

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

}
