package org.dpsoftware.gui;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.grabber.GrabberSingleton;
import org.dpsoftware.gui.trayicon.TrayIconManager;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GuiManagerStopCaptureTest {

    @Test
    void delayedLinuxStopUsesTheNewPickerColorInsteadOfRestoringThePreviousColor() throws Exception {
        MainSingleton main = new MainSingleton();
        main.config = new Configuration();
        main.config.setFullFirmware(true);
        main.config.setToggleLed(true);
        main.config.setColorChooser("255,0,0,255");
        GuiManager gui = new GuiManager();
        gui.pipelineManager = mock(PipelineManager.class);
        gui.trayIconManager = mock(TrayIconManager.class);

        try (MockedStatic<MainSingleton> singleton = mockStatic(MainSingleton.class);
             MockedStatic<GrabberSingleton> grabber = mockStatic(GrabberSingleton.class);
             MockedStatic<NativeExecutor> os = mockStatic(NativeExecutor.class);
             MockedStatic<CommonUtility> utility = mockStatic(CommonUtility.class);
             MockedStatic<NetworkManager> network = mockStatic(NetworkManager.class)) {
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            os.when(NativeExecutor::isLinux).thenReturn(true);
            utility.when(CommonUtility::getNightBrightness).thenReturn(255);
            utility.when(() -> CommonUtility.toJsonString(any(StateDto.class))).thenCallRealMethod();
            network.when(() -> NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT)).thenReturn("lights/set");

            gui.stopCapturingThreads(true);

            ArgumentCaptor<Runnable> stop = ArgumentCaptor.forClass(Runnable.class);
            utility.verify(() -> CommonUtility.delayMilliseconds(stop.capture(), eq(300L)));
            network.verify(() -> NetworkManager.publishToTopic(anyString(), anyString()), never());
            verify(gui.pipelineManager).stopCapturePipeline();

            // The web picker sends blue after stopping capture, before the delayed STOP is sent.
            main.config.setColorChooser("0,0,255,255");
            stop.getValue().run();

            ArgumentCaptor<String> published = ArgumentCaptor.forClass(String.class);
            network.verify(() -> NetworkManager.publishToTopic(eq("lights/set"), published.capture()));
            var command = CommonUtility.JSON_MAPPER.readTree(published.getValue());
            assertEquals(0, command.path("color").path("r").asInt());
            assertEquals(0, command.path("color").path("g").asInt());
            assertEquals(255, command.path("color").path("b").asInt());
            assertEquals(Constants.SOLID, command.path("effect").asText());
            assertEquals(Constants.ON, command.path("state").asText());
            assertEquals("STOP", command.path("startStopInstances").asText());
        }
    }
}
