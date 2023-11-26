/*
  TcpClient.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.network.tcpUdp;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.Satellite;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.utilities.CommonUtility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utility class for TCP communication
 */
@Slf4j
public class TcpClient {

    /**
     * Send an HTTP GET to a specific IP address
     *
     * @param msg   msg to use in the payload param
     * @param topic http get path
     * @return response
     */
    @SuppressWarnings("UnusedReturnValue")
    public static TcpResponse httpGet(String msg, String topic, String destinationIP) {
        TcpResponse tcpResponse = new TcpResponse();
        try {
            HttpURLConnection con;
            String request = Constants.HTTP_URL
                    .replace("{0}", destinationIP)
                    .replace("{1}", topic)
                    .replace("{2}", URLEncoder.encode(msg, StandardCharsets.UTF_8));
            log.trace("HTTP GET=" + request);
            URL url = new URI(request).toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoOutput(true);
            con.setConnectTimeout(Constants.HTTP_TIMEOUT);
            con.setReadTimeout(Constants.HTTP_TIMEOUT);
            con.setRequestProperty(Constants.UPGRADE_CONTENT_TYPE, Constants.HTTP_RESPONSE);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            int status = con.getResponseCode();
            in.close();
            con.disconnect();
            tcpResponse.setResponse(response.toString());
            tcpResponse.setErrorCode(status);
            log.trace(CommonUtility.toJsonStringPrettyPrinted(tcpResponse));
            return tcpResponse;
        } catch (IOException | URISyntaxException e) {
            log.error(e.getMessage());
        }
        return tcpResponse;
    }

    /**
     * Send an HTTP GET to default device
     *
     * @param msg   msg to use in the payload param
     * @param topic http get path
     * @return response
     */
    public static TcpResponse httpGet(String msg, String topic) {
        TcpResponse tcpResponse = new TcpResponse();
        if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getDeviceIP() != null) {
            tcpResponse = httpGet(msg, topic, CommonUtility.getDeviceToUse().getDeviceIP());
            if (MainSingleton.getInstance().config.getSatellites() != null) {
                for (Map.Entry<String, Satellite> sat : MainSingleton.getInstance().config.getSatellites().entrySet()) {
                    if (CommonUtility.getDeviceToUse() != null && CommonUtility.getDeviceToUse().getDeviceIP() != null) {
                        String swappedMsg = NetworkManager.swapMac(msg, sat.getValue());
                        if (!Constants.HTTP_TOPIC_TO_SKIP_FOR_SATELLITES.contains(topic)) {
                            httpGet(swappedMsg, topic, sat.getValue().getDeviceIp());
                        }
                    }
                }
            }
        }
        return tcpResponse;
    }
}