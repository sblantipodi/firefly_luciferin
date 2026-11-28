/*
  StorageManagerRestartLockTest.java

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

  You should have received a copy of the GNU Lesser General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware.managers;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.InstanceConfigurer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class StorageManagerRestartLockTest {

    @Test
    void removesOnlyOldUnlockedRestartLocks(@TempDir Path configDirectory) throws Exception {
        Path stale = createLockFile(configDirectory);
        Path active = createLockFile(configDirectory);
        Path recent = createLockFile(configDirectory);
        Path unrelated = configDirectory.resolve("settings.lock");
        Files.createFile(unrelated);
        FileTime old = FileTime.from(Instant.now().minus(10, ChronoUnit.MINUTES));
        Files.setLastModifiedTime(stale, old);
        Files.setLastModifiedTime(active, old);

        MainSingleton main = new MainSingleton();
        main.whoAmI = 1;
        try (FileChannel channel = FileChannel.open(active, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock();
             MockedStatic<InstanceConfigurer> configurer = mockStatic(InstanceConfigurer.class);
             MockedStatic<MainSingleton> mainSingleton = mockStatic(MainSingleton.class)) {
            configurer.when(InstanceConfigurer::getConfigPath).thenReturn(configDirectory.toString());
            mainSingleton.when(MainSingleton::getInstance).thenReturn(main);
            new StorageManager().deleteTempFiles();
        }

        assertFalse(Files.exists(stale));
        assertTrue(Files.exists(active));
        assertTrue(Files.exists(recent));
        assertTrue(Files.exists(unrelated));
    }

    @Test
    void otherInstancesLeaveRestartLockCleanupToInstanceOne(@TempDir Path configDirectory) throws Exception {
        Path stale = createLockFile(configDirectory);
        Files.setLastModifiedTime(stale, FileTime.from(Instant.now().minus(10, ChronoUnit.MINUTES)));

        MainSingleton main = new MainSingleton();
        main.whoAmI = 2;
        try (MockedStatic<InstanceConfigurer> configurer = mockStatic(InstanceConfigurer.class);
             MockedStatic<MainSingleton> mainSingleton = mockStatic(MainSingleton.class)) {
            configurer.when(InstanceConfigurer::getConfigPath).thenReturn(configDirectory.toString());
            mainSingleton.when(MainSingleton::getInstance).thenReturn(main);
            new StorageManager().deleteTempFiles();
        }

        assertTrue(Files.exists(stale));
    }

    private static Path createLockFile(Path directory) throws Exception {
        return Files.createFile(directory.resolve(".restart-" + UUID.randomUUID() + ".lock"));
    }
}
