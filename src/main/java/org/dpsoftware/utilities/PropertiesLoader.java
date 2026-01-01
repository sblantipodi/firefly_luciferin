/*
  PropertiesLoader.java

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
package org.dpsoftware.utilities;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * GUI Manager for tray icon menu and framerate counter dialog
 */
@Slf4j
@NoArgsConstructor
public class PropertiesLoader {

    /**
     * Extract project version computed from Continuous Integration
     *
     * @return properties value
     */
    public String retrieveProperties(String prop) {
        final Properties properties = new Properties();
        try (final InputStream fis = this.getClass().getClassLoader().getResourceAsStream(Constants.PROPERTIES_FILENAME)) {
            properties.load(fis);
            return properties.getProperty(prop);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return prop;
    }

}
