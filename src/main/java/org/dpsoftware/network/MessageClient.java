/*
  MessageClient.java

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
package org.dpsoftware.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.controllers.DevicesTabController;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.utilities.CommonUtility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Message client for Java Sockets, used for single device multi monitor
 */
@Slf4j
public class MessageClient {

    public static MessageClient msgClient;
    public Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    /**
     * Get the main instance status when in multi screen single device
     */
    public static void getSingleInstanceMultiScreenStatus() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // Create a task that runs every 2 seconds
        Runnable framerateTask = () -> {
            try {
                if (msgClient == null || msgClient.clientSocket == null) {
                    msgClient = new MessageClient();
                    msgClient.startConnection(Constants.MSG_SERVER_HOST, Constants.MSG_SERVER_PORT);
                }
                String response = msgClient.sendMessage(Constants.MSG_SERVER_STATUS);
                JsonNode stateStatusDto = CommonUtility.fromJsonToObject(response);
                assert stateStatusDto != null;
                FireflyLuciferin.config.setEffect(stateStatusDto.get(Constants.EFFECT).asText());
                boolean mainInstanceRunning = stateStatusDto.get(Constants.RUNNING).asText().equals(Constants.TRUE);
                // Close instance if server is closed.
                boolean exit = stateStatusDto.get(Constants.EXIT.toLowerCase()).asBoolean();
                if (!CommonUtility.isSingleDeviceMainInstance() && exit) {
                    NativeExecutor.exit();
                }
                FireflyLuciferin.FPS_GW_CONSUMER = Float.parseFloat(stateStatusDto.get(Constants.FPS_GW_CONSUMER).asText());
                // Update device table data
                DevicesTabController.deviceTableData.remove(0, DevicesTabController.deviceTableData.size());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode arrayNode = stateStatusDto.get(Constants.DEVICE_TABLE_DATA);
                if (arrayNode.isArray()) {
                    ObjectReader reader = mapper.readerFor(new TypeReference<List<GlowWormDevice>>() {
                    });
                    List<GlowWormDevice> list = reader.readValue(arrayNode);
                    DevicesTabController.deviceTableData.addAll(list);
                }
                // Set other instances Running
                if (FireflyLuciferin.RUNNING != mainInstanceRunning) {
                    if (mainInstanceRunning) {
                        FireflyLuciferin.guiManager.startCapturingThreads();
                    } else {
                        FireflyLuciferin.guiManager.stopCapturingThreads(false);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 10, 2, TimeUnit.SECONDS);
    }

    /**
     * Connect to the message server
     *
     * @param ip   ip of the msg server
     * @param port port of the msg server
     */
    public void startConnection(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Send msg to the message server
     *
     * @param msg message to send
     * @return server response
     */
    public String sendMessage(String msg) {
        try {
            if (out != null) {
                out.println(msg);
                return in.readLine();
            }
        } catch (IOException e) {
            MessageClient.msgClient = null;
            log.error(e.getMessage());
        }
        return "";
    }

    /**
     * Close connection to the msg server
     *
     * @throws IOException socket error
     */
    @SuppressWarnings("unused")
    public void stopConnection() throws IOException {
        log.info("Stopping message client");
        in.close();
        out.close();
        clientSocket.close();
    }
}