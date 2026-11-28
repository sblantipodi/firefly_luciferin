/*
  CommonBinding.java

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
package org.dpsoftware.gui.bindings;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.EnvConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Common methods for jextracted bindings
 */
@Slf4j
public class CommonBinding {

    private static final String LD_CONFIG = "/etc/ld.so.conf.d/";

    /**
     * Get absolute path of an image, used for native access
     *
     * @param imgStr relative path
     * @return absolute path
     */
    public static String getIconPath(String imgStr) {
        if (NativeExecutor.isSystemTraySupported()) {
            String imgAbsolutePath = Objects.requireNonNull(CommonBinding.class.getResource(imgStr)).getPath()
                    .replace(Constants.JAVA_PREFIX, "").replace(Constants.FILE_PREFIX, "")
                    .split(Constants.FAT_JAR_NAME)[0] + Constants.CLASSES + imgStr;
            if (Files.exists(Paths.get(imgAbsolutePath))) {
                imgStr = imgAbsolutePath;
            } else {
                imgStr = Objects.requireNonNull(CommonBinding.class.getResource(imgStr)).getPath();
            }
        }
        return imgStr;
    }

    /**
     * Find library paths from the runtime environment and ld-config.
     *
     * @return ld config paths
     */
    public static List<String> getLdConfigPaths() {
        List<String> allPath = new LinkedList<>();
        // Content runtimes such as Snap expose versioned libraries through LD_LIBRARY_PATH.
        String runtimePaths = System.getenv(EnvConstants.LD_LIBRARY_PATH);
        if (runtimePaths != null) {
            Stream.of(runtimePaths.split(File.pathSeparator))
                    .filter(path -> path.startsWith("/"))
                    .forEach(allPath::add);
        }
        try (Stream<Path> paths = Files.list(Path.of(LD_CONFIG))) {
            paths.forEach((file) -> {
                try (Stream<String> lines = Files.lines(file)) {
                    List<String> collection = lines.filter(line -> line.startsWith("/")).toList();
                    allPath.addAll(collection);
                } catch (IOException e) {
                    log.warn("File '{}' could not be loaded", file);
                }
            });
        } catch (IOException e) {
            log.warn("Directory '{}' does not exist", LD_CONFIG);
        }
        return allPath;
    }

}
