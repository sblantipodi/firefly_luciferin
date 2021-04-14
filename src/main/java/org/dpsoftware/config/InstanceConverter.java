/*
  InstanceConverter.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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
package org.dpsoftware.config;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.dpsoftware.JavaFXStarter;

/**
 * Simple converter used to log the instance number.
 */
public class InstanceConverter extends ClassicConverter {

    /* (non-Javadoc)
     * @see ch.qos.logback.core.pattern.Converter#convert(java.lang.Object)
     */
    @Override
    public String convert(ILoggingEvent event) {
        return "[Instance #" + JavaFXStarter.whoAmI + "]";
    }

}