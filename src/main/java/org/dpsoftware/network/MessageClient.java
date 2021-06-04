/*
  MessageClient.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.utilities.CommonUtility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Message client for Java Sockets, used for single instance multi monitor
 */
@Slf4j
public class MessageClient {

    public Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    public static MessageClient msgClient;

    /**
     * Connect to the message server
     * @param ip ip of the msg server
     * @param port port of the msg server
     * @throws IOException socket error
     */
    public void startConnection(String ip, int port) throws IOException {

        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

    }

    /**
     * Send msg to the message server
     * @param msg message to send
     * @return server response
     * @throws IOException socket error
     */
    public String sendMessage(String msg) throws IOException {

        if (out != null) {
            out.println(msg);
            return in.readLine();
        }
        return "";

    }

    /**
     * Close connection to the msg server
     * @throws IOException socket error
     */
    @SuppressWarnings("unused")
    public void stopConnection() throws IOException {

        log.debug("Stopping message client");
        in.close();
        out.close();
        clientSocket.close();

    }

    /**
     * Get the main instance status when in multi screen single instance
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
                FireflyLuciferin.config.setEffect(stateStatusDto.get(Constants.EFFECT).toString());
                boolean mainInstanceRunning = stateStatusDto.get(Constants.RUNNING).toString().equals(Constants.TRUE);
                if (FireflyLuciferin.RUNNING != mainInstanceRunning) {
                    if (mainInstanceRunning) {
                        FireflyLuciferin.guiManager.startCapturingThreads();
                    } else {
                        FireflyLuciferin.guiManager.stopCapturingThreads(false);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                NativeExecutor.restartNativeInstance();
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 10, 2, TimeUnit.SECONDS);

    }

}