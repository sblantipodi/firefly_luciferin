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


    public DBusPath CreateSession(Map<String, Variant<?>> options);
    public DBusPath SelectSources(DBusPath sessionHandle, Map<String, Variant<?>> options);
    public DBusPath Start(DBusPath sessionHandle, String parentWindow, Map<String, Variant<?>> options);
    public FileDescriptor OpenPipeWireRemote(DBusPath sessionHandle, Map<String, Variant<?>> options);

}
