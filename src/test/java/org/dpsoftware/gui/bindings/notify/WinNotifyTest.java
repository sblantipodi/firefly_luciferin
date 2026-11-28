/*
  WinNotifyTest.java

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
package org.dpsoftware.gui.bindings.notify;

import org.dpsoftware.NativeExecutor;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WinNotifyTest {

    @Test
    void isSupportedMatchesWindowsPlatform() {
        assertEquals(NativeExecutor.isWindows(), WinNotify.isSupported());
    }

    @Test
    void buildToastXmlEscapesSpecialCharacters() {
        String xml = WinNotify.buildToastXml("Title & <Test>", "Body \"with\" 'quotes'", "C:\\temp\\a&b.png", TrayIcon.MessageType.INFO);
        assertTrue(xml.contains("<text>Title &amp; &lt;Test&gt;</text>"));
        assertTrue(xml.contains("<text>Body &quot;with&quot; &apos;quotes&apos;</text>"));
        assertTrue(xml.contains("src=\"C:\\temp\\a&amp;b.png\""));
        assertTrue(xml.contains("<audio src=\"ms-winsoundevent:Notification.Default\"/>"));
    }

    @Test
    void buildToastXmlMapsMessageTypeToAudio() {
        assertTrue(WinNotify.buildToastXml("T", "B", "", TrayIcon.MessageType.NONE).contains("<audio silent=\"true\"/>"));
        assertTrue(WinNotify.buildToastXml("T", "B", "", TrayIcon.MessageType.ERROR).contains("Notification.Reminder"));
        assertTrue(WinNotify.buildToastXml("T", "B", "", TrayIcon.MessageType.WARNING).contains("Notification.IM"));
        assertTrue(WinNotify.buildToastXml("T", "B", "", TrayIcon.MessageType.INFO).contains("Notification.Default"));
    }

    @Test
    void allocGuidUsesWindowsLittleEndianLayout() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment guid = WinNotify.allocGuid(arena, "50ac103f-d235-4598-bbef-98fe4d1a3ad4");
            assertEquals(0x50ac103f, guid.get(ValueLayout.JAVA_INT, 0));
            assertEquals((short) 0xd235, guid.get(ValueLayout.JAVA_SHORT, 4));
            assertEquals((short) 0x4598, guid.get(ValueLayout.JAVA_SHORT, 6));
            assertEquals((byte) 0xbb, guid.get(ValueLayout.JAVA_BYTE, 8));
            assertEquals((byte) 0xef, guid.get(ValueLayout.JAVA_BYTE, 9));
            assertEquals((byte) 0x98, guid.get(ValueLayout.JAVA_BYTE, 10));
            assertEquals((byte) 0xd4, guid.get(ValueLayout.JAVA_BYTE, 15));
        }
    }
}
