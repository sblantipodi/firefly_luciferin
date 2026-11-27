/*
  WinNotify.java

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

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Windows Toast Notification sender using native WinRT COM APIs via Java FFM (combase.dll)
 * so notifications persist in the Windows Action Center / Notification Center without spawning external processes.
 */
@Slf4j
public class WinNotify {

    static final String APP_USER_MODEL_ID = "DPsoftware.FireflyLuciferin";
    private static final String AUMID_REGISTRY_KEY = "Software\\Classes\\AppUserModelId\\" + APP_USER_MODEL_ID;
    private static final String ICON_FILE_NAME = "firefly_luciferin_logo.png";

    private static final int RO_INIT_MULTITHREADED = 1;
    private static final int S_OK = 0;
    private static final int S_FALSE = 1;
    private static final int RPC_E_CHANGED_MODE = 0x80010106;

    // WinRT activatable class IDs
    private static final String CLASS_XML_DOCUMENT = "Windows.Data.Xml.Dom.XmlDocument";
    private static final String CLASS_TOAST_NOTIFICATION_MANAGER = "Windows.UI.Notifications.ToastNotificationManager";
    private static final String CLASS_TOAST_NOTIFICATION = "Windows.UI.Notifications.ToastNotification";

    // WinRT interface IIDs
    private static final String IID_IXML_DOCUMENT = "f7f3a506-1e87-42d6-bcfb-b8c809fa5494";
    private static final String IID_IXML_DOCUMENT_IO = "6cd0e74e-ee65-4489-9ebf-ca43e87ba637";
    private static final String IID_ITOAST_NOTIFICATION_MANAGER_STATICS = "50ac103f-d235-4598-bbef-98fe4d1a3ad4";
    private static final String IID_ITOAST_NOTIFICATION_FACTORY = "04124b20-82c6-4229-b109-fd9ed4662b53";
    private static final String IID_ITOAST_NOTIFIER = "75927b93-03f3-41ec-91d3-6e5bac1b38e7";

    // COM vtable slot indices (0-2: IUnknown, 3-5: IInspectable, 6+: interface methods)
    private static final int SLOT_QUERY_INTERFACE = 0;
    private static final int SLOT_RELEASE = 2;
    private static final int SLOT_IXML_DOCUMENT_IO_LOAD_XML = 6;
    private static final int SLOT_TOAST_MANAGER_CREATE_NOTIFIER_WITH_ID = 7;
    private static final int SLOT_TOAST_FACTORY_CREATE_NOTIFICATION = 6;
    private static final int SLOT_TOAST_NOTIFIER_SHOW = 6;

    private static final FunctionDescriptor DESC_QI = FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
    private static final FunctionDescriptor DESC_RELEASE = FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
    private static final FunctionDescriptor DESC_LOAD_XML = FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
    private static final FunctionDescriptor DESC_CREATE_NOTIFIER_WITH_ID = FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
    private static final FunctionDescriptor DESC_CREATE_TOAST = FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
    private static final FunctionDescriptor DESC_SHOW_TOAST = FunctionDescriptor.of(
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS);

    private static final AtomicBoolean aumidRegistered = new AtomicBoolean(false);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WinNotify-Thread");
        t.setDaemon(true);
        return t;
    });
    private static final ThreadLocal<Boolean> threadComInitialized = ThreadLocal.withInitial(() -> false);
    private static volatile String cachedIconPath;
    private static volatile boolean nativeReady;
    private static MethodHandle roInitialize;
    private static MethodHandle windowsCreateString;
    private static MethodHandle windowsDeleteString;
    private static MethodHandle roGetActivationFactory;
    private static MethodHandle roActivateInstance;

    static {
        if (NativeExecutor.isWindows()) {
            try {
                Linker linker = Linker.nativeLinker();
                SymbolLookup combase = SymbolLookup.libraryLookup("combase.dll", Arena.global());
                roInitialize = linker.downcallHandle(
                        combase.findOrThrow("RoInitialize"),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
                windowsCreateString = linker.downcallHandle(
                        combase.findOrThrow("WindowsCreateString"),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                windowsDeleteString = linker.downcallHandle(
                        combase.findOrThrow("WindowsDeleteString"),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                roGetActivationFactory = linker.downcallHandle(
                        combase.findOrThrow("RoGetActivationFactory"),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                roActivateInstance = linker.downcallHandle(
                        combase.findOrThrow("RoActivateInstance"),
                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                nativeReady = true;
            } catch (Throwable t) {
                nativeReady = false;
                log.warn("Native WinRT combase.dll initialization failed: {}", t.getMessage());
            }
        }
    }

    /**
     * Check if Windows toast notifications are supported on the current OS.
     *
     * @return true on Windows when combase.dll WinRT bindings are loaded
     */
    public static boolean isSupported() {
        return NativeExecutor.isWindows() && nativeReady;
    }

    /**
     * Show a Windows toast notification asynchronously, invoking the fallback if dispatch fails.
     *
     * @param title            notification title
     * @param content          notification message
     * @param notificationType notification severity
     * @param fallback         optional fallback runnable (e.g. AWT TrayIcon.displayMessage)
     */
    public static void showWindowsNotification(String title, String content, TrayIcon.MessageType notificationType, Runnable fallback) {
        if (!isSupported()) {
            if (fallback != null) {
                fallback.run();
            }
            return;
        }
        EXECUTOR.execute(() -> {
            if (!sendToast(title, content, notificationType) && fallback != null) {
                fallback.run();
            }
        });
    }

    /**
     * Dispatch a Windows toast notification using native WinRT COM APIs.
     */
    static boolean sendToast(String title, String content, TrayIcon.MessageType notificationType) {
        if (!nativeReady) {
            return false;
        }
        try (Arena arena = Arena.ofConfined()) {
            ensureRoInitialized();
            String iconPath = ensureIconExtracted();
            ensureAppUserModelIdRegistered(iconPath);

            String toastXml = buildToastXml(title, content, iconPath, notificationType);
            MemorySegment hXmlDocClass = createHString(arena, CLASS_XML_DOCUMENT);
            MemorySegment hXml = createHString(arena, toastXml);
            MemorySegment hManagerClass = createHString(arena, CLASS_TOAST_NOTIFICATION_MANAGER);
            MemorySegment hFactoryClass = createHString(arena, CLASS_TOAST_NOTIFICATION);
            MemorySegment hAumid = createHString(arena, APP_USER_MODEL_ID);

            MemorySegment xmlDoc = MemorySegment.NULL;
            MemorySegment xmlDocIo = MemorySegment.NULL;
            MemorySegment toastManagerStatics = MemorySegment.NULL;
            MemorySegment toastNotifier = MemorySegment.NULL;
            MemorySegment toastFactory = MemorySegment.NULL;
            MemorySegment toastNotification = MemorySegment.NULL;

            try {
                // 1. Activate XmlDocument and QueryInterface for IXmlDocumentIO & IXmlDocument
                MemorySegment ppOut = arena.allocate(ValueLayout.ADDRESS);
                int hr = (int) roActivateInstance.invokeExact(hXmlDocClass, ppOut);
                checkHr(hr, "RoActivateInstance(XmlDocument)");
                xmlDoc = ppOut.get(ValueLayout.ADDRESS, 0);

                MemorySegment iidXmlDocIo = allocGuid(arena, IID_IXML_DOCUMENT_IO);
                hr = (int) comCall(xmlDoc, SLOT_QUERY_INTERFACE, DESC_QI, iidXmlDocIo, ppOut);
                checkHr(hr, "QueryInterface(IXmlDocumentIO)");
                xmlDocIo = ppOut.get(ValueLayout.ADDRESS, 0);

                hr = (int) comCall(xmlDocIo, SLOT_IXML_DOCUMENT_IO_LOAD_XML, DESC_LOAD_XML, hXml);
                checkHr(hr, "IXmlDocumentIO::LoadXml");

                // 2. Get ToastNotificationManager statics and create ToastNotifier(aumid)
                MemorySegment iidToastManager = allocGuid(arena, IID_ITOAST_NOTIFICATION_MANAGER_STATICS);
                hr = (int) roGetActivationFactory.invokeExact(hManagerClass, iidToastManager, ppOut);
                checkHr(hr, "RoGetActivationFactory(ToastNotificationManager)");
                toastManagerStatics = ppOut.get(ValueLayout.ADDRESS, 0);

                hr = (int) comCall(toastManagerStatics, SLOT_TOAST_MANAGER_CREATE_NOTIFIER_WITH_ID,
                        DESC_CREATE_NOTIFIER_WITH_ID, hAumid, ppOut);
                checkHr(hr, "IToastNotificationManagerStatics::CreateToastNotifierWithId");
                toastNotifier = ppOut.get(ValueLayout.ADDRESS, 0);

                // 3. Get ToastNotification factory and create ToastNotification(xmlDoc)
                MemorySegment iidToastFactory = allocGuid(arena, IID_ITOAST_NOTIFICATION_FACTORY);
                hr = (int) roGetActivationFactory.invokeExact(hFactoryClass, iidToastFactory, ppOut);
                checkHr(hr, "RoGetActivationFactory(ToastNotification)");
                toastFactory = ppOut.get(ValueLayout.ADDRESS, 0);

                MemorySegment iidXmlDoc = allocGuid(arena, IID_IXML_DOCUMENT);
                hr = (int) comCall(xmlDoc, SLOT_QUERY_INTERFACE, DESC_QI, iidXmlDoc, ppOut);
                checkHr(hr, "QueryInterface(IXmlDocument)");
                MemorySegment iXmlDoc = ppOut.get(ValueLayout.ADDRESS, 0);
                try {
                    hr = (int) comCall(toastFactory, SLOT_TOAST_FACTORY_CREATE_NOTIFICATION,
                            DESC_CREATE_TOAST, iXmlDoc, ppOut);
                    checkHr(hr, "IToastNotificationFactory::CreateToastNotification");
                    toastNotification = ppOut.get(ValueLayout.ADDRESS, 0);
                } finally {
                    releaseCom(iXmlDoc);
                }

                // 4. Show toast
                hr = (int) comCall(toastNotifier, SLOT_TOAST_NOTIFIER_SHOW, DESC_SHOW_TOAST, toastNotification);
                checkHr(hr, "IToastNotifier::Show");
                return true;
            } finally {
                releaseCom(toastNotification);
                releaseCom(toastFactory);
                releaseCom(toastNotifier);
                releaseCom(toastManagerStatics);
                releaseCom(xmlDocIo);
                releaseCom(xmlDoc);
                deleteHString(hAumid);
                deleteHString(hFactoryClass);
                deleteHString(hManagerClass);
                deleteHString(hXml);
                deleteHString(hXmlDocClass);
            }
        } catch (Throwable t) {
            log.warn("Failed to send native Windows toast notification: {}", t.getMessage());
            return false;
        }
    }

    static String buildToastXml(String title, String content, String iconPath, TrayIcon.MessageType notificationType) {
        String escapedTitle = escapeXml(title);
        String escapedBody = escapeXml(content);
        String logoXml = (iconPath != null && !iconPath.isEmpty())
                ? "<image placement=\"appLogoOverride\" src=\"" + escapeXml(iconPath) + "\"/>"
                : "";
        String audioXml = buildAudioXml(notificationType);
        return "<toast><visual><binding template=\"ToastGeneric\"><text>"
                + escapedTitle + "</text><text>" + escapedBody + "</text>" + logoXml
                + "</binding></visual>" + audioXml + "</toast>";
    }

    static String buildAudioXml(TrayIcon.MessageType notificationType) {
        if (notificationType == null) {
            return "<audio src=\"ms-winsoundevent:Notification.Default\"/>";
        }
        return switch (notificationType) {
            case NONE -> "<audio silent=\"true\"/>";
            case ERROR -> "<audio src=\"ms-winsoundevent:Notification.Reminder\"/>";
            case WARNING -> "<audio src=\"ms-winsoundevent:Notification.IM\"/>";
            case INFO -> "<audio src=\"ms-winsoundevent:Notification.Default\"/>";
        };
    }

    static String escapeXml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    static MemorySegment allocGuid(Arena arena, String guidStr) {
        String[] parts = guidStr.split("-");
        MemorySegment guid = arena.allocate(16, 4);
        guid.set(ValueLayout.JAVA_INT, 0, (int) Long.parseLong(parts[0], 16));
        guid.set(ValueLayout.JAVA_SHORT, 4, (short) Integer.parseInt(parts[1], 16));
        guid.set(ValueLayout.JAVA_SHORT, 6, (short) Integer.parseInt(parts[2], 16));
        byte[] tail = hexToBytes(parts[3] + parts[4]);
        MemorySegment.copy(tail, 0, guid, ValueLayout.JAVA_BYTE, 8, 8);
        return guid;
    }

    private static byte[] hexToBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static void ensureRoInitialized() throws Throwable {
        if (!Boolean.TRUE.equals(threadComInitialized.get())) {
            int hr = (int) roInitialize.invokeExact(RO_INIT_MULTITHREADED);
            if (hr != S_OK && hr != S_FALSE && hr != RPC_E_CHANGED_MODE) {
                checkHr(hr, "RoInitialize");
            }
            threadComInitialized.set(true);
        }
    }

    private static MemorySegment createHString(Arena arena, String value) throws Throwable {
        String safe = value != null ? value : "";
        char[] chars = new char[safe.length() + 1];
        safe.getChars(0, safe.length(), chars, 0);
        MemorySegment buf = arena.allocateFrom(ValueLayout.JAVA_CHAR, chars);
        MemorySegment outHString = arena.allocate(ValueLayout.ADDRESS);
        int hr = (int) windowsCreateString.invokeExact(buf, safe.length(), outHString);
        checkHr(hr, "WindowsCreateString");
        return outHString.get(ValueLayout.ADDRESS, 0);
    }

    private static void deleteHString(MemorySegment hString) {
        if (hString != null && !hString.equals(MemorySegment.NULL)) {
            try {
                int ignored = (int) windowsDeleteString.invokeExact(hString);
            } catch (Throwable ignored) {
            }
        }
    }

    private static Object comCall(MemorySegment comPtr, int slot, FunctionDescriptor descriptor, Object... args) throws Throwable {
        MemorySegment vtbl = comPtr.reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0)
                .reinterpret((long) (slot + 1) * ValueLayout.ADDRESS.byteSize());
        MemorySegment fnPtr = vtbl.getAtIndex(ValueLayout.ADDRESS, slot);
        MethodHandle mh = Linker.nativeLinker().downcallHandle(fnPtr, descriptor);
        Object[] fullArgs = new Object[args.length + 1];
        fullArgs[0] = comPtr;
        System.arraycopy(args, 0, fullArgs, 1, args.length);
        return mh.invokeWithArguments(fullArgs);
    }

    private static void releaseCom(MemorySegment comPtr) {
        if (comPtr != null && !comPtr.equals(MemorySegment.NULL)) {
            try {
                comCall(comPtr, SLOT_RELEASE, DESC_RELEASE);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void checkHr(int hr, String step) {
        if (hr < 0) {
            throw new IllegalStateException(step + " failed with HRESULT 0x" + Integer.toHexString(hr));
        }
    }

    private static void ensureAppUserModelIdRegistered(String iconPath) {
        if (aumidRegistered.compareAndSet(false, true)) {
            try {
                Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, AUMID_REGISTRY_KEY);
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, AUMID_REGISTRY_KEY,
                        "DisplayName", Constants.FIREFLY_LUCIFERIN);
                if (iconPath != null && !iconPath.isEmpty()) {
                    Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, AUMID_REGISTRY_KEY,
                            "IconUri", iconPath);
                }
            } catch (Throwable t) {
                aumidRegistered.set(false);
                log.debug("Unable to register AUMID in registry: {}", t.getMessage());
            }
        }
    }

    private static String ensureIconExtracted() {
        if (cachedIconPath != null && Files.exists(Path.of(cachedIconPath))) {
            return cachedIconPath;
        }
        Path target = Path.of(System.getProperty("java.io.tmpdir"), ICON_FILE_NAME);
        if (!Files.exists(target)) {
            try (InputStream is = WinNotify.class.getResourceAsStream(Constants.IMAGE_TRAY_STOP)) {
                if (is != null) {
                    Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log.debug("Unable to extract notification icon: {}", e.getMessage());
                return "";
            }
        }
        if (Files.exists(target)) {
            cachedIconPath = target.toAbsolutePath().toString();
            return cachedIconPath;
        }
        return "";
    }
}
