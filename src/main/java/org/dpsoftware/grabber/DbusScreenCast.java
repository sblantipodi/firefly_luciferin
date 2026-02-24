/*
  DbusScreenCast.java

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
package org.dpsoftware.grabber;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

/**
 * Auto-generated class.
 */
@DBusProperty(name = "AvailableSourceTypes", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "AvailableCursorModes", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "version", type = UInt32.class, access = Access.READ)
@DBusInterfaceName(value = "org.freedesktop.portal.ScreenCast")
public interface DbusScreenCast extends DBusInterface {

    void CreateSession(Map<String, Variant<?>> options);

    void SelectSources(DBusPath sessionHandle, Map<String, Variant<?>> options);

    DBusPath Start(DBusPath sessionHandle, String parentWindow, Map<String, Variant<?>> options);

    FileDescriptor OpenPipeWireRemote(DBusPath sessionHandle, Map<String, Variant<?>> options);

}
