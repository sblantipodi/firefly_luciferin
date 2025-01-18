package test.org.dpsoftware.grabber;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.grabber.GrabberManager;
import org.dpsoftware.grabber.ImageProcessor;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class GrabberManagerTest {

    private static GrabberManager grabberManager;
    private ImageProcessor imageProcessor;
    private ScheduledExecutorService scheduledExecutorService;

    @BeforeClass
    public static void setUp() {
        MainSingleton.getInstance().config = mock(Configuration.class);

        grabberManager = new GrabberManager();
//        imageProcessor = mock(ImageProcessor.class);
//        scheduledExecutorService = mock(ScheduledExecutorService.class);
    }

    //
//    @Test
//    public void testGetSuggestedFramerate() {
//        MainSingleton.getInstance().FPS_GW_CONSUMER = 100;
//        int suggestedFramerate = GrabberManager.getSuggestedFramerate();
//        assertEquals(90, suggestedFramerate);
//    }
//
//    @Test
//    public void testLaunchAdvancedGrabber() {
//        grabberManager.launchAdvancedGrabber(imageProcessor);
//        verify(imageProcessor, times(1)).initGStreamerLibraryPaths();
//        assertNotNull(GrabberSingleton.getInstance().pipe);
//    }
//
//    @Test
//    public void testDisposePipeline() {
//        grabberManager.bin = mock(Bin.class);
//        grabberManager.vc = mock(GStreamerGrabber.class);
//        GrabberSingleton.getInstance().pipe = mock(Pipeline.class);
//        grabberManager.disposePipeline();
//        verify(grabberManager.bin, times(1)).dispose();
//        verify(grabberManager.vc.videosink, times(1)).dispose();
//        verify(GrabberSingleton.getInstance().pipe, times(1)).dispose();
//    }
//
//    @Test
//    public void testLaunchStandardGrabber() throws AWTException {
//        grabberManager.launchStandardGrabber(scheduledExecutorService, 3);
//        verify(scheduledExecutorService, times(3)).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(25L), eq(TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void testProducerTask() throws AWTException {
//        Robot robot = mock(Robot.class);
//        grabberManager.producerTask(robot);
//        assertEquals(1, MainSingleton.getInstance().FPS_PRODUCER_COUNTER);
//    }
//
//    @Test
//    public void testGetFPS() {
//        grabberManager.getFPS();
//        assertNotEquals(0, MainSingleton.getInstance().FPS_PRODUCER);
//    }
//
    @Test
    public void testPingDevice() {
        grabberManager.pingDevice();
        // No assertion needed, just ensure no exceptions are thrown
    }

    @Test
    public void testRunBenchmark() {
        AtomicInteger framerateAlert = new AtomicInteger();
        AtomicBoolean notified = new AtomicBoolean(false);
        grabberManager.runBenchmark(framerateAlert, notified);
        assertTrue(framerateAlert.get() >= 0);
    }
}