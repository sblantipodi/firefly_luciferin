/*
  NativeExecutorRestartLockTest.java

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
package org.dpsoftware;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeExecutorRestartLockTest {

    @Test
    void killsVerifiedOwnerThenAcquiresLock(@TempDir Path directory) throws Exception {
        Path lockPath = lockPath(directory);
        Path readyPath = directory.resolve("ready");
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        Process owner = new ProcessBuilder(javaExecutable, "-cp", System.getProperty("java.class.path"),
                LockHolder.class.getName(), lockPath.toString(), readyPath.toString()).start();
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (!Files.exists(readyPath) && owner.isAlive() && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertTrue(Files.exists(readyPath), "lock holder failed to start");
            String argument = NativeExecutor.restartLockArgument(lockPath, owner.toHandle());
            NativeExecutor.waitForRestartLock(argument, directory, Duration.ofMillis(150));
            assertTrue(owner.waitFor(5, TimeUnit.SECONDS));
            assertFalse(Files.exists(lockPath));
        } finally {
            owner.destroyForcibly();
            owner.waitFor(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void startsAfterNormalLockRelease(@TempDir Path directory) throws Exception {
        Path lockPath = Files.createFile(lockPath(directory));
        String argument = NativeExecutor.restartLockArgument(lockPath, ProcessHandle.current());
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE)) {
            FileLock lock = channel.lock();
            Thread release = new Thread(() -> {
                try {
                    Thread.sleep(100);
                    lock.release();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
            release.start();
            try {
                NativeExecutor.waitForRestartLock(argument, directory, Duration.ofSeconds(2));
            } finally {
                release.join(2_000);
                if (lock.isValid()) lock.release();
            }
        }
        assertFalse(Files.exists(lockPath));
    }

    @Test
    void refusesToKillAnUnverifiedProcess(@TempDir Path directory) throws Exception {
        Path lockPath = Files.createFile(lockPath(directory));
        ProcessHandle current = ProcessHandle.current();
        long wrongStartTime = current.info().startInstant().orElseThrow().toEpochMilli() + 1;
        String argument = "RESTART_LOCK=" + lockPath.getFileName() + ":" + current.pid() + ":" + wrongStartTime;
        try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            assertThrows(IllegalStateException.class,
                    () -> NativeExecutor.waitForRestartLock(argument, directory, Duration.ofMillis(100)));
            assertTrue(Files.exists(lockPath));
        }
    }

    private static Path lockPath(Path directory) {
        return directory.resolve(".restart-" + UUID.randomUUID() + ".lock");
    }

    public static final class LockHolder {

        private LockHolder() {
        }

        public static void main(String[] args) throws Exception {
            try (FileChannel channel = FileChannel.open(Path.of(args[0]),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                Files.createFile(Path.of(args[1]));
                Thread.sleep(30_000);
            }
        }
    }
}
