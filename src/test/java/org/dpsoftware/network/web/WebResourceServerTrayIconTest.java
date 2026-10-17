package org.dpsoftware.network.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.trayicon.TrayIconAwt;
import org.dpsoftware.gui.trayicon.TrayIconState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebResourceServerTrayIconTest {

    private Configuration previousConfig;
    private int previousInstance;
    private boolean previousUpgrade;
    private Configuration config;
    private TrayIconAwt tray;

    @BeforeEach
    void setUp() {
        previousConfig = MainSingleton.getInstance().config;
        previousInstance = MainSingleton.getInstance().whoAmI;
        previousUpgrade = GuiSingleton.getInstance().isUpgrade();
        config = new Configuration();
        config.setMultiMonitor(1);
        MainSingleton.getInstance().config = config;
        MainSingleton.getInstance().whoAmI = 1;
        GuiSingleton.getInstance().setUpgrade(false);
        // Exercise the real AWT image selection without constructing an OS tray.
        tray = mock(TrayIconAwt.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        TrayIconState.update(Enums.PlayerStatus.STOP);
        MainSingleton.getInstance().config = previousConfig;
        MainSingleton.getInstance().whoAmI = previousInstance;
        GuiSingleton.getInstance().setUpgrade(previousUpgrade);
    }

    @Test
    void webServesTheExactAwtImageForEveryPlayerState() throws Exception {
        for (Enums.PlayerStatus status : Enums.PlayerStatus.values()) {
            assertServedImage(tray.setTrayIconImage(status));
        }
    }

    @Test
    void webSharesMultiMonitorAndUpdateVariantsWithAwt() throws Exception {
        config.setMultiMonitor(3);
        for (boolean singleDevice : new boolean[]{false, true}) {
            config.setMultiScreenSingleDevice(singleDevice);
            for (boolean updateAvailable : new boolean[]{false, true}) {
                GuiSingleton.getInstance().setUpgrade(updateAvailable);
                for (int instance = 1; instance <= 3; instance++) {
                    MainSingleton.getInstance().whoAmI = instance;
                    for (Enums.PlayerStatus status : Enums.PlayerStatus.values()) {
                        assertServedImage(tray.setTrayIconImage(status));
                    }
                }
            }
        }
    }

    private void assertServedImage(String trayResource) throws Exception {
        assertEquals(trayResource, TrayIconState.currentImage());
        HttpExchange exchange = mock(HttpExchange.class);
        Headers headers = new Headers();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        // Query parameters must not allow callers to choose arbitrary resources.
        when(exchange.getRequestURI()).thenReturn(URI.create("/luciferin-logo.png?icon=ignored"));
        when(exchange.getResponseHeaders()).thenReturn(headers);
        when(exchange.getResponseBody()).thenReturn(body);

        new WebResourceServer().handleRoot(exchange);

        byte[] expected;
        try (var stream = getClass().getResourceAsStream(trayResource)) {
            assertNotNull(stream, trayResource);
            expected = stream.readAllBytes();
        }
        verify(exchange).sendResponseHeaders(200, expected.length);
        assertEquals("image/png", headers.getFirst("Content-Type"));
        assertEquals("no-store", headers.getFirst("Cache-Control"));
        assertArrayEquals(expected, body.toByteArray(), trayResource);
    }

    @Test
    void flatpakUpdateIconsUseTheOriginalBundledPngForWeb() throws Exception {
        GuiSingleton.getInstance().setUpgrade(true);
        try (MockedStatic<NativeExecutor> nativeExecutor = mockStatic(NativeExecutor.class)) {
            nativeExecutor.when(NativeExecutor::isFlatpak).thenReturn(true);
            for (Enums.PlayerStatus status : Enums.PlayerStatus.values()) {
                String flatpakIcon = tray.setTrayIconImage(status);
                assertTrue(flatpakIcon.contains("/update_"));
                assertServedImage(flatpakIcon.replace(Constants.IMG_PATH + "update_", Constants.IMG_PATH_UPDATE));
            }
        }
    }
}
