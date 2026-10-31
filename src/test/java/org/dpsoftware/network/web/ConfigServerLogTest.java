/*
  ConfigServerLogTest.java

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
package org.dpsoftware.network.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigServerLogTest {

    @TempDir
    Path tempDir;

    @Test
    void tailLogReturnsOnlyTheLastThousandLines() throws IOException {
        Path log = tempDir.resolve("FireflyLuciferin.log");
        Files.write(log, IntStream.rangeClosed(1, 1001).mapToObj(i -> "line " + i).toList());

        String[] lines = ConfigServer.tailLog(log).split("\n");

        assertEquals(1000, lines.length);
        assertEquals("line 2", lines[0]);
        assertEquals("line 1001", lines[999]);
    }

    @Test
    void tailLogKeepsShortFilesIntact() throws IOException {
        Path log = tempDir.resolve("FireflyLuciferin.log");
        Files.writeString(log, "first\nsecond\n");

        assertEquals("first\nsecond", ConfigServer.tailLog(log));
    }
}
