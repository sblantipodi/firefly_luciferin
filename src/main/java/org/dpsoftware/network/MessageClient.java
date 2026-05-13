/*
  MessageClient.java

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
package org.dpsoftware.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.GuiSingleton;
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

    public Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private long lastConnectionAttempt;

    /**
     * Get the main instance status when in multi screen single device
     */
    public static void getSingleInstanceMultiScreenStatus() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // Create a task that runs every 2 seconds
        Runnable framerateTask = () -> {
            try {
                if (NetworkSingleton.getInstance().msgClient == null) {
                    NetworkSingleton.getInstance().msgClient = new MessageClient();
                }
                if (!NetworkSingleton.getInstance().msgClient.startConnection(Constants.MSG_SERVER_HOST, Constants.MSG_SERVER_PORT)) {
                    return;
                }
                String response = NetworkSingleton.getInstance().msgClient.sendMessage(Constants.MSG_SERVER_STATUS);
                if (response.isEmpty()) {
                    return;
                }
                JsonNode stateStatusDto = CommonUtility.fromJsonToObject(response);
                assert stateStatusDto != null;
                MainSingleton.getInstance().config.setEffect(stateStatusDto.get(Constants.EFFECT).asText());
                boolean mainInstanceRunning = stateStatusDto.get(Constants.RUNNING).asText().equals(Constants.TRUE);
                // Close instance if server is closed.
                boolean exit = stateStatusDto.get(Constants.EXIT.toLowerCase()).asBoolean();
                if (!CommonUtility.isSingleDeviceMainInstance() && exit) {
                    NativeExecutor.exit();
                }
                MainSingleton.getInstance().FPS_GW_CONSUMER = Float.parseFloat(stateStatusDto.get(Constants.FPS_GW_CONSUMER).asText());
                // Update device table data
                GuiSingleton.getInstance().deviceTableData.remove(0, GuiSingleton.getInstance().deviceTableData.size());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode arrayNode = stateStatusDto.get(Constants.DEVICE_TABLE_DATA);
                if (arrayNode.isArray()) {
                    ObjectReader reader = mapper.readerFor(new TypeReference<List<GlowWormDevice>>() {
                    });
                    List<GlowWormDevice> list = reader.readValue(arrayNode);
                    GuiSingleton.getInstance().deviceTableData.addAll(list);
                }
                // Set other instances Running
                if (MainSingleton.getInstance().RUNNING != mainInstanceRunning) {
                    if (mainInstanceRunning) {
                        MainSingleton.getInstance().guiManager.startCapturingThreads();
                    } else {
                        MainSingleton.getInstance().guiManager.stopCapturingThreads(false);
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
    public boolean startConnection(String ip, int port) {
        if (isConnected()) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - lastConnectionAttempt < Constants.UDP_RECONNECT_DELAY_MS) {
            return false;
        }
        lastConnectionAttempt = now;
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            return true;
        } catch (IOException e) {
            closeConnection();
            log.info("Message server not ready: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if the client is connected to the message server.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed() && out != null && in != null;
    }

    /**
     * Send msg to the message server
     *
     * @param msg message to send
     * @return server response
     */
    public String sendMessage(String msg) {
        try {
            if (isConnected()) {
                out.println(msg);
                return in.readLine();
            }
        } catch (IOException e) {
            closeConnection();
            log.warn("Message server connection lost: {}", e.getMessage());
        }
        return "";
    }

    private void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            log.debug(e.getMessage());
        } finally {
            in = null;
            out = null;
            clientSocket = null;
        }
    }

    /**
     * Close connection to the msg server
     *
     */
    @SuppressWarnings("unused")
    public void stopConnection() {
        log.info("Stopping message client");
        closeConnection();
    }
}
