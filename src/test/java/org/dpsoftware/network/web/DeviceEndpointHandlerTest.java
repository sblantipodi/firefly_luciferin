package org.dpsoftware.network.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.network.tcpUdp.TcpClient;
import org.dpsoftware.utilities.CommonUtility;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression tests for picker commands sent through the device state endpoint.
 */
class DeviceEndpointHandlerTest {
    private static final String IP = "192.168.1.20";
    private static final String PAYLOAD = "{\"state\":\"ON\",\"color\":{\"r\":10,\"g\":20,\"b\":30},\"whitetemp\":65}";
    private Configuration previousConfig;
    private MqttClient previousClient;
    private Configuration config;
    private GlowWormDevice device;

    /**
     * Saves the shared runtime state and prepares an output device with a previous color.
     */
    @BeforeEach
    void setUp() {
        previousConfig = MainSingleton.getInstance().config;
        previousClient = ManagerSingleton.getInstance().client;
        config = new Configuration();
        config.setColorChooser("255,82,0,123");
        config.setEffect(Constants.SOLID);
        MainSingleton.getInstance().config = config;
        device = mock(GlowWormDevice.class);
        when(device.getDeviceIP()).thenReturn(IP);
        when(device.getMac()).thenReturn("AA:BB:CC:DD:EE:FF");
    }

    /**
     * Restores the shared configuration and MQTT client after each test.
     */
    @AfterEach
    void tearDown() {
        MainSingleton.getInstance().config = previousConfig;
        ManagerSingleton.getInstance().client = previousClient;
    }

    /**
     * Creates a mocked device state request with writable response headers and body.
     *
     * @param ip   the requested output device address
     * @param body the JSON command to read from the request
     * @return the mocked HTTP exchange
     */
    private HttpExchange exchange(String ip, String body) {
        HttpExchange exchange = mock(HttpExchange.class);
        when(exchange.getRequestURI()).thenReturn(URI.create("/deviceState?ip=" + ip));
        when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        return exchange;
    }

    /**
     * Verifies that MQTT receives the new runtime color without invoking the HTTP feedback path.
     *
     * @throws Exception when handling the request or parsing the published command fails
     */
    @Test
    void mqttPickerUpdatesRuntimeBeforePublishingWithoutHttpEffectFeedback() throws Exception {
        config.setMqttEnable(true);
        MqttClient client = mock(MqttClient.class);
        when(client.isConnected()).thenReturn(true);
        ManagerSingleton.getInstance().client = client;
        HttpExchange exchange = exchange(IP, PAYLOAD);
        try (MockedStatic<CommonUtility> utility = mockStatic(CommonUtility.class, CALLS_REAL_METHODS);
             MockedStatic<NetworkManager> network = mockStatic(NetworkManager.class, CALLS_REAL_METHODS);
             MockedStatic<TcpClient> http = mockStatic(TcpClient.class)) {
            utility.when(CommonUtility::getDeviceToUse).thenReturn(device);
            network.when(() -> NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT)).thenReturn("lights/custom/set");
            network.when(() -> NetworkManager.publishToTopic(eq("lights/custom/set"), anyString())).thenAnswer(invocation -> {
                assertEquals("10,20,30,123", config.getColorChooser());
                assertTrue(config.isToggleLed());
                assertEquals(65, config.getWhiteTemperature());
                JsonNode command = CommonUtility.JSON_MAPPER.readTree(invocation.getArgument(1, String.class));
                assertEquals("AA:BB:CC:DD:EE:FF", command.path(Constants.MAC).asText());
                assertEquals(10, command.path(Constants.COLOR).path("r").asInt());
                assertFalse(command.has(Constants.EFFECT));
                return null;
            });

            new DeviceEndpointHandler().handleDeviceState(exchange);

            network.verify(() -> NetworkManager.publishToTopic(eq("lights/custom/set"), anyString()));
            http.verifyNoInteractions();
            verify(exchange).sendResponseHeaders(eq(200), anyLong());
        }
    }

    /**
     * Verifies that HTTP commands preserve the current effect and update the runtime color.
     *
     * @throws Exception when handling the request or parsing the HTTP command fails
     */
    @Test
    void withoutMqttUsesHttpAndPreservesEffect() throws Exception {
        config.setMqttEnable(false);
        HttpExchange exchange = exchange(IP, PAYLOAD.replace("ON", "OFF"));
        try (MockedStatic<CommonUtility> utility = mockStatic(CommonUtility.class, CALLS_REAL_METHODS);
             MockedStatic<TcpClient> http = mockStatic(TcpClient.class)) {
            utility.when(CommonUtility::getDeviceToUse).thenReturn(device);
            http.when(() -> TcpClient.httpGet(anyString(), eq(Constants.TOPIC_DEFAULT_MQTT), eq(IP))).thenAnswer(invocation -> {
                assertEquals("10,20,30,123", config.getColorChooser());
                assertFalse(config.isToggleLed());
                JsonNode command = CommonUtility.JSON_MAPPER.readTree(invocation.getArgument(0, String.class));
                assertEquals(Constants.SOLID, command.path(Constants.EFFECT).asText());
                return new TcpResponse(200, "OK");
            });
            new DeviceEndpointHandler().handleDeviceState(exchange);
            http.verify(() -> TcpClient.httpGet(anyString(), eq(Constants.TOPIC_DEFAULT_MQTT), eq(IP)));
            verify(exchange).sendResponseHeaders(eq(200), anyLong());
        }
    }

    /**
     * Verifies that a disconnected broker leaves the runtime state untouched and returns an error.
     *
     * @throws Exception when handling the request fails
     */
    @Test
    void disconnectedMqttDoesNotFallBackToHttpOrChangeRuntime() throws Exception {
        config.setMqttEnable(true);
        ManagerSingleton.getInstance().client = null;
        HttpExchange exchange = exchange(IP, PAYLOAD);
        try (MockedStatic<CommonUtility> utility = mockStatic(CommonUtility.class, CALLS_REAL_METHODS);
             MockedStatic<TcpClient> http = mockStatic(TcpClient.class)) {
            utility.when(CommonUtility::getDeviceToUse).thenReturn(device);
            new DeviceEndpointHandler().handleDeviceState(exchange);
            assertEquals("255,82,0,123", config.getColorChooser());
            http.verifyNoInteractions();
            verify(exchange).sendResponseHeaders(eq(503), anyLong());
        }
    }

    /**
     * Verifies that an unexpected device or invalid RGB channel is rejected before sending a command.
     *
     * @throws Exception when handling either request fails
     */
    @Test
    void rejectsDifferentDeviceAndInvalidChannels() throws Exception {
        try (MockedStatic<CommonUtility> utility = mockStatic(CommonUtility.class, CALLS_REAL_METHODS);
             MockedStatic<TcpClient> http = mockStatic(TcpClient.class)) {
            utility.when(CommonUtility::getDeviceToUse).thenReturn(device);
            HttpExchange otherDevice = exchange("192.168.1.21", PAYLOAD);
            new DeviceEndpointHandler().handleDeviceState(otherDevice);
            verify(otherDevice).sendResponseHeaders(eq(400), anyLong());
            HttpExchange invalidColor = exchange(IP, PAYLOAD.replace("10", "256"));
            new DeviceEndpointHandler().handleDeviceState(invalidColor);
            verify(invalidColor).sendResponseHeaders(eq(400), anyLong());
            assertEquals("255,82,0,123", config.getColorChooser());
            http.verifyNoInteractions();
        }
    }
}
