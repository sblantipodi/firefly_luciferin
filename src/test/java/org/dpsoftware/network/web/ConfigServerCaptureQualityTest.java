package org.dpsoftware.network.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.utilities.CaptureDeviceUtilities;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConfigServerCaptureQualityTest {
    @Test
    void liveQualityChangeRecalculatesCaptureResolutionBeforeRestart() throws Exception {
        var device = new CaptureDeviceUtilities.CaptureDevice("/dev/video0", "USB capture", 0);
        var format = new CaptureDeviceUtilities.PixelFormat("MJPG");
        var small = new CaptureDeviceUtilities.Resolution(640, 480);
        var balanced = new CaptureDeviceUtilities.Resolution(1920, 1080);
        var source = new CaptureDeviceUtilities.Resolution(3840, 2160);
        small.getFps().add(60);
        balanced.getFps().add(60);
        source.getFps().add(30);
        format.getResolutions().addAll(List.of(small, balanced, source));
        device.getFormats().add(format);

        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.config.setScreenResX(3840);
        main.config.setScreenResY(2160);
        main.config.setOsScaling(100);
        main.config.setResamplingFactor(8);
        main.config.setCaptureDevice(new CaptureDeviceUtilities.BestCaptureFormat(device, format, small));

        var server = mock(ConfigServer.class, CALLS_REAL_METHODS);
        var change = ConfigServer.class.getDeclaredMethod("applyComboChange", String.class, JsonNode.class);
        change.setAccessible(true);
        try (var singleton = mockStatic(MainSingleton.class);
             var capture = mockStatic(CaptureDeviceUtilities.class, CALLS_REAL_METHODS);
             var utility = mockStatic(CommonUtility.class, CALLS_REAL_METHODS);
             var pipeline = mockStatic(PipelineManager.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            capture.when(CaptureDeviceUtilities::discover).thenReturn(List.of(device));
            utility.when(() -> CommonUtility.delayMilliseconds(any(Runnable.class), eq(200L)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(0).run();
                        return null;
                    });
            pipeline.when(() -> PipelineManager.restartCapture(any(Runnable.class), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });

            change.invoke(server, WebFieldNames.RESAMPLING_FACTOR, IntNode.valueOf(4));
            assertEquals(4, main.config.getResamplingFactor());
            assertEquals(1920, main.config.getCaptureDevice().getSuggestedWidth());
            assertEquals(1080, main.config.getCaptureDevice().getSuggestedHeight());
            assertEquals(60, main.config.getCaptureDevice().getMaxFps());

            change.invoke(server, WebFieldNames.RESAMPLING_FACTOR, IntNode.valueOf(1));
            assertEquals(1, main.config.getResamplingFactor());
            assertEquals(3840, main.config.getCaptureDevice().getSuggestedWidth());
            assertEquals(2160, main.config.getCaptureDevice().getSuggestedHeight());
            assertEquals(30, main.config.getCaptureDevice().getMaxFps());

            main.config.setCaptureDevice(null);
            change.invoke(server, WebFieldNames.RESAMPLING_FACTOR, IntNode.valueOf(4));
            assertEquals(4, main.config.getResamplingFactor());
            assertFalse(main.config.hasCaptureDevice());
        }
    }
}
