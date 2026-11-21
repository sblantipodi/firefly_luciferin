/*
  WebRtcFrameIntegrationTest.java

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

import jakarta.websocket.Session;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.lut.CubeLutToneMap;
import org.dpsoftware.network.web.WebRtcSignalingServer;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSrc;
import org.freedesktop.gstreamer.glib.GObject;
import org.freedesktop.gstreamer.webrtc.WebRTCBin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Uses real GstBuffers/caps; opt in on hosts with GStreamer installed. No network connection is opened. */
@EnabledIfSystemProperty(named = "luciferin.gstreamerTests", matches = "true")
class WebRtcFrameIntegrationTest {
    private Configuration previousConfig;
    private WebRtcStreamer streamer;
    private Session session;
    private WebRtcSignalingServer server;

    @BeforeAll
    static void initializeGst() { Gst.init(); }

    @BeforeEach
    void setUp() {
        previousConfig = MainSingleton.getInstance().config;
        var config = new Configuration();
        config.setCaptureMethod(Configuration.CaptureMethod.USB_VIDEO.name());
        config.setCubeLut("HDR to SDR standard.cube");
        MainSingleton.getInstance().config = config;
        CubeLutToneMap.refresh();
        streamer = spy(new WebRtcStreamer());
        doReturn(true).when(streamer).isSupported();
        session = mock(Session.class);
        when(session.getId()).thenReturn("preview-test");
        when(session.isOpen()).thenReturn(true);
        server = mock(WebRtcSignalingServer.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        Object dispatcher = field(streamer, "frameDispatcher");
        streamer.stop();
        awaitTermination(dispatcher);
        MainSingleton.getInstance().config = previousConfig;
        CubeLutToneMap.refresh();
    }

    private record Fixture(Pipeline pipeline, AppSrc source) {}

    private static Fixture fixture() {
        Pipeline pipeline = mock(Pipeline.class);
        AppSrc source = mock(AppSrc.class);
        WebRTCBin bin = mock(WebRTCBin.class);
        when(pipeline.getElementByName("webrtcsrc")).thenReturn(source);
        when(pipeline.getElementByName("webrtcpreviewcaps")).thenReturn(mock(Element.class));
        when(pipeline.getElementByName("webrtcbin")).thenReturn(bin);
        when(pipeline.getBus()).thenReturn(mock(Bus.class));
        when(bin.get("ice-agent")).thenReturn(mock(GObject.class));
        when(source.pushBuffer(any(Buffer.class))).thenReturn(FlowReturn.OK);
        return new Fixture(pipeline, source);
    }

    @Test
    void workerUsesOwnedPixelsAndCaptureTimeLutSnapshot() throws Exception {
        Fixture fixture = fixture();
        doReturn(fixture.pipeline()).when(streamer).createPipeline();
        byte[] pixels = new byte[]{(byte) 200, (byte) 150, 100, 73};
        ByteBuffer source = ByteBuffer.wrap(pixels);
        int[] expected = CubeLutToneMap.lookup(100, 150, 200);
        CountDownLatch pushed = new CountDownLatch(1);
        AtomicReference<byte[]> actual = new AtomicReference<>();
        AtomicReference<Thread> processingThread = new AtomicReference<>();
        when(fixture.source().pushBuffer(any(Buffer.class))).thenAnswer(invocation -> {
            Buffer buffer = invocation.getArgument(0);
            ByteBuffer mapped = buffer.map(false);
            try {
                byte[] bytes = new byte[4];
                mapped.get(bytes);
                actual.set(bytes);
                processingThread.set(Thread.currentThread());
            } finally { buffer.unmap(); }
            pushed.countDown();
            return FlowReturn.OK;
        });
        streamer.startSession(server, session);
        synchronized (streamer) {
            streamer.pushFrame(source, 1, 1);
            java.util.Arrays.fill(pixels, (byte) 0);
            MainSingleton.getInstance().config.setCubeLut(Constants.DISABLED);
            CubeLutToneMap.refresh();
        }
        assertTrue(pushed.await(5, TimeUnit.SECONDS));
        assertNotSame(Thread.currentThread(), processingThread.get());
        assertArrayEquals(new byte[]{(byte) expected[2], (byte) expected[1], (byte) expected[0], 73}, actual.get());
        assertEquals(0, source.position());
    }

    @Test
    void stoppedSessionCannotPushQueuedFrameIntoItsReplacement() throws Exception {
        Fixture first = fixture(), second = fixture();
        doReturn(first.pipeline(), second.pipeline()).when(streamer).createPipeline();
        streamer.startSession(server, session);
        Object oldDispatcher = field(streamer, "frameDispatcher");
        synchronized (streamer) {
            streamer.pushFrame(ByteBuffer.allocate(16), 2, 2);
            streamer.startSession(server, session);
        }
        awaitTermination(oldDispatcher);
        verify(first.source(), never()).pushBuffer(any(Buffer.class));
        CountDownLatch pushed = new CountDownLatch(1);
        when(second.source().pushBuffer(any(Buffer.class))).thenAnswer(_ -> { pushed.countDown(); return FlowReturn.OK; });
        pumpUntil(pushed, 2, 2);
        verify(first.pipeline()).setState(State.NULL);
    }

    @Test
    void resolutionChangeRebuildsSessionAndOnlyNewFramesReachNewSource() throws Exception {
        Fixture first = fixture(), second = fixture();
        doReturn(first.pipeline(), second.pipeline()).when(streamer).createPipeline();
        CountDownLatch firstPush = new CountDownLatch(1), secondPush = new CountDownLatch(1);
        when(first.source().pushBuffer(any(Buffer.class))).thenAnswer(_ -> { firstPush.countDown(); return FlowReturn.OK; });
        when(second.source().pushBuffer(any(Buffer.class))).thenAnswer(invocation -> {
            Buffer buffer = invocation.getArgument(0);
            ByteBuffer mapped = buffer.map(false);
            try { assertEquals(4 * 2 * 4, mapped.remaining()); } finally { buffer.unmap(); }
            secondPush.countDown();
            return FlowReturn.OK;
        });
        streamer.startSession(server, session);
        pumpUntil(firstPush, 2, 2);
        Object oldDispatcher = field(streamer, "frameDispatcher");
        pumpUntil(secondPush, 4, 2);
        awaitTermination(oldDispatcher);
        verify(first.source(), times(1)).pushBuffer(any(Buffer.class));
        verify(first.pipeline()).setState(State.NULL);
        assertEquals(4, field(streamer, "captureWidth"));
        assertEquals(2, field(streamer, "captureHeight"));
    }

    @Test
    void boundedAppsrcAcceptsTransferredBuffersAndReleasesWrappersSafely() {
        try (Pipeline pipeline = (Pipeline) Gst.parseLaunch(
                "appsrc name=src is-live=true format=time block=false max-buffers=1 max-bytes=0 max-time=0 leaky-type=downstream ! fakesink sync=false")) {
            AppSrc source = (AppSrc) pipeline.getElementByName("src");
            source.set("caps", Caps.fromString("video/x-raw,format=BGRx,width=2,height=2,framerate=30/1"));
            pipeline.play();
            try {
                for (int i = 0; i < 20; i++) {
                    Buffer buffer = new Buffer(16);
                    assertEquals(FlowReturn.OK, source.pushBuffer(buffer));
                    // Same cleanup as PreviewFrame.close, after native ownership transfer.
                    assertDoesNotThrow(buffer::dispose);
                }
                assertTrue(((Number) source.get("current-level-buffers")).longValue() <= 1);
            } finally { pipeline.setState(State.NULL); }
        }
    }

    private void pumpUntil(CountDownLatch pushed, int width, int height) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (pushed.getCount() != 0 && System.nanoTime() < deadline) {
            streamer.pushFrame(ByteBuffer.allocate(width * height * 4), width, height);
            if (pushed.await(10, TimeUnit.MILLISECONDS)) return;
        }
        assertEquals(0, pushed.getCount(), "Preview did not produce a frame");
    }

    private static Object field(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void awaitTermination(Object dispatcher) throws Exception {
        if (dispatcher != null) {
            ExecutorService executor = (ExecutorService) field(dispatcher, "executor");
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
