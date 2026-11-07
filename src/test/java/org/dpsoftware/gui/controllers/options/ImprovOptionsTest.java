package org.dpsoftware.gui.controllers.options;

import org.dpsoftware.utilities.CommonUtility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImprovOptionsTest {

    /**
     * Creates provisioning values for validation tests.
     *
     * @param mode     Ethernet mode
     * @param ssid     Wi-Fi network name
     * @param password Wi-Fi password
     * @param mi       MISO pin
     * @param mo       MOSI pin
     * @param sck      clock pin
     * @param cs       chip select pin
     * @return request to validate
     */
    private static ImprovOptions.ProvisionRequest request(String mode, String ssid, String password,
                                                          String mi, String mo, String sck, String cs) {
        return new ImprovOptions.ProvisionRequest(ssid, password, "Glow Worm", "COM1", "115200",
                mode, "Gledopto-Ethernet", mi, mo, sck, cs, false, "", "", "", "");
    }

    /**
     * Checks how Ethernet mode selects the board or SPI controls.
     */
    @Test
    void ethernetModeControlsTheRequiredFields() {
        assertTrue(ImprovOptions.needsSpiPins("ETH_CUSTOM_SPI"));
        assertFalse(ImprovOptions.needsSpiPins("ETH_PREBUILT"));
        assertTrue(ImprovOptions.needsEthernetBoard("ETH_PREBUILT"));
    }

    /**
     * Checks the two validation rules shared by the GUI and web form.
     */
    @Test
    void wifiAndCustomSpiValidationMatchTheDialog() {
        ImprovOptions.ProvisionRequest noWifi = request("ETH_NO_ETH", "", "", "", "", "", "");
        assertEquals("fxml.mqtttab.improv.wifi.error", ImprovOptions.validationError(noWifi));

        ImprovOptions.ProvisionRequest missingPin = request("ETH_CUSTOM_SPI", "", "", "3", "10", "9", "");
        assertEquals("fxml.mqtttab.improv.eth.field.error", ImprovOptions.validationError(missingPin));
    }

    /**
     * Checks the request format accepted by the web provisioning endpoint.
     */
    @Test
    void provisioningRequestDeserializesFromJson() throws Exception {
        ImprovOptions.ProvisionRequest original = request("ETH_PREBUILT", "wifi", "password", "", "", "", "");
        byte[] json = CommonUtility.JSON_MAPPER.writeValueAsBytes(original);
        ImprovOptions.ProvisionRequest parsed = CommonUtility.JSON_MAPPER.readValue(json, ImprovOptions.ProvisionRequest.class);
        assertEquals(original, parsed);
    }

    /**
     * Checks the exact field names sent by the browser's provisioning button.
     */
    @Test
    void readsBrowserProvisioningPayload() throws Exception {
        String json = """
                {"ssid":"home","wifiPassword":"secret","deviceName":"Glow Worm","comPort":"COM4",
                 "baudRate":"115200","ethernetMode":"ETH_NO_ETH","ethernetBoard":"Gledopto-Ethernet",
                 "mi":"","mo":"","sck":"","cs":"","mqttEnabled":true,
                 "mqttHost":"broker","mqttPort":"1883","mqttUser":"user","mqttPassword":"mqtt-secret"}
                """;
        ImprovOptions.ProvisionRequest request = ImprovOptions.fromJson(CommonUtility.JSON_MAPPER.readTree(json));
        assertEquals("COM4", request.comPort());
        assertEquals("broker", request.mqttHost());
        assertTrue(request.mqttEnabled());
    }
}
