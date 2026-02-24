/*
  MessageServer.java

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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.StateStatusDto;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Message server using Java Sockets, used for single instance multi monitor
 */
@Slf4j
@Getter
@Setter
public class MessageServer {

    private Color[] leds;
    private boolean firstDisplayReceived = false;
    private boolean secondDisplayReceived = false;
    private boolean thirdDisplayReceived = false;
    private int firstDisplayLedNum = 0;
    private int secondDisplayLedNum = 0;
    private ServerSocket serverSocket;
    private Configuration monitorConfig1, monitorConfig2, monitorConfig3;

    private static StateStatusDto getStateStatusDto() {
        StateStatusDto stateStatusDto = new StateStatusDto();
        stateStatusDto.setEffect(MainSingleton.getInstance().config.getEffect());
        stateStatusDto.setRunning(MainSingleton.getInstance().RUNNING);
        stateStatusDto.setDeviceTableData(GuiSingleton.getInstance().deviceTableData);
        stateStatusDto.setFpsgwconsumer(MainSingleton.getInstance().FPS_GW_CONSUMER);
        stateStatusDto.setExit(MainSingleton.getInstance().closeOtherInstaces);
        return stateStatusDto;
    }

    /**
     * Start message server for multi screen, single instance
     */
    public void startMessageServer() {
        CommonUtility.delaySeconds(() -> {
            try {
                NetworkSingleton.getInstance().messageServer.start(Constants.MSG_SERVER_PORT);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }, 0);
    }

    /**
     * Init totalNumLed based on all instances
     */
    public void initNumLed() {
        StorageManager sm = new StorageManager();
        // Server starts if there are 2 or more monitors
        monitorConfig1 = sm.readConfigFile(Constants.CONFIG_FILENAME);
        firstDisplayLedNum = monitorConfig1.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()).size();
        monitorConfig2 = sm.readConfigFile(Constants.CONFIG_FILENAME_2);
        secondDisplayLedNum = monitorConfig2.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()).size();
        if (MainSingleton.getInstance().config.getMultiMonitor() == 3) {
            monitorConfig3 = sm.readConfigFile(Constants.CONFIG_FILENAME_3);
            int thirdDisplayLedNum = monitorConfig3.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n()).size();
            NetworkSingleton.getInstance().totalLedNum = firstDisplayLedNum + secondDisplayLedNum + thirdDisplayLedNum;
        } else {
            NetworkSingleton.getInstance().totalLedNum = firstDisplayLedNum + secondDisplayLedNum;
        }
    }

    /**
     * Start the message server, accepts multiple connections
     *
     * @param port used for the message server
     * @throws IOException socket error
     */
    public void start(int port) throws IOException {
        log.info("Starting message server");
        leds = new Color[NetworkSingleton.getInstance().totalLedNum];
        serverSocket = new ServerSocket(port);
        while (!NetworkSingleton.getInstance().closeServer) {
            if (!serverSocket.isClosed()) {
                new ClientHandler(serverSocket.accept()).start();
            }
        }
    }

    /**
     * Stop the message server
     *
     * @throws IOException socket error
     */
    public void stop() throws IOException {
        log.info("Stopping message server");
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    /**
     * Start stop capture based on other instances input
     *
     * @param msg client input
     */
    void startStopCapture(String msg) {
        JsonNode stateStatusDto = CommonUtility.fromJsonToObject(msg);
        assert stateStatusDto != null;
        boolean otherInstanceRunning = stateStatusDto.get(Constants.RUNNING).asBoolean();
        if (MainSingleton.getInstance().RUNNING != otherInstanceRunning) {
            if (otherInstanceRunning) {
                MainSingleton.getInstance().guiManager.startCapturingThreads();
            } else {
                MainSingleton.getInstance().guiManager.stopCapturingThreads(false);
            }
        }
    }

    /**
     * Collect data received from the client and send it to the strip
     *
     * @param inputLine message received from the client
     * @param out       response sent to the client
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void collectAndSendData(String inputLine, PrintWriter out) {
        String[] ledsString = inputLine.split(",");
        int instanceNumber = Integer.parseInt(ledsString[0]);
        int startIndex = 0;
        if (instanceNumber == 1) {
            firstDisplayReceived = true;
            startIndex = -1;
        } else if (instanceNumber == 2) {
            secondDisplayReceived = true;
            startIndex = firstDisplayLedNum - 1;
        } else if (instanceNumber == 3) {
            thirdDisplayReceived = true;
            startIndex = (firstDisplayLedNum + secondDisplayLedNum) - 1;
        }
        for (int i = 1; i <= ledsString.length - 1; i++) {
            leds[startIndex + i] = new Color(Integer.parseInt(ledsString[i]));
        }
        if (MainSingleton.getInstance().config.getMultiMonitor() == 2 && firstDisplayReceived && secondDisplayReceived) {
            firstDisplayReceived = false;
            secondDisplayReceived = false;
            MainSingleton.getInstance().sharedQueue.offer(leds);
        } else if (MainSingleton.getInstance().config.getMultiMonitor() == 3 && firstDisplayReceived && secondDisplayReceived && thirdDisplayReceived) {
            firstDisplayReceived = false;
            secondDisplayReceived = false;
            thirdDisplayReceived = false;
            MainSingleton.getInstance().sharedQueue.offer(leds);
        }
        out.println(inputLine);
    }

    /**
     * Client handler, it waits for all the monitors and then sends the message to the queue
     */
    private class ClientHandler extends Thread {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    // Send status to clients
                    if (inputLine.equals(Constants.MSG_SERVER_STATUS)) {
                        StateStatusDto stateStatusDto = getStateStatusDto();
                        out.println(CommonUtility.toJsonString(stateStatusDto));
                    } else if (inputLine.contains(Constants.CLIENT_ACTION)) {
                        startStopCapture(inputLine);
                        out.println(Constants.OK);
                    } else if (Constants.EXIT.equals(inputLine)) {
                        out.println("bye");
                        NativeExecutor.exit();
                        break;
                    } else { // Collect data from clients and send it to the strip
                        collectAndSendData(inputLine, out);
                    }
                }
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
}