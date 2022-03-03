/*
  MessageServer.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.controllers.DevicesTabController;
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
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Message server using Java Sockets, used for single instance multi monitor
 */
@Slf4j
public class MessageServer {

    private ServerSocket serverSocket;
    private static Color[] leds;
    public static boolean closeServer = false;
    private static boolean firstDisplayReceived = false;
    private static boolean secondDisplayReceived = false;
    private static boolean thirdDisplayReceived = false;
    private static int firstDisplayLedNum = 0;
    private static int secondDisplayLedNum = 0;
    public static int totalLedNum = FireflyLuciferin.ledNumber;
    public static MessageServer messageServer;
    private static Configuration otherConfig2;
    private static Configuration otherConfig3;

    /**
     * Start the message server, accepts multiple connections
     * @param port used for the message server
     * @throws IOException socket error
     */
    public void start(int port) throws IOException {

        log.debug("Starting message server");
        leds = new Color[totalLedNum];
        serverSocket = new ServerSocket(port);
        while (!closeServer) {
            if (!serverSocket.isClosed()) {
                new ClientHandler(serverSocket.accept()).start();
            }
        }

    }

    /**
     * Stop the message server
     * @throws IOException socket error
     */
    public void stop() throws IOException {

        log.debug("Stopping message server");
        if (serverSocket != null) {
            serverSocket.close();
        }

    }

    /**
     * Client handler, it waits for all the monitors and then sends the message to the queue
     */
    private class ClientHandler extends Thread {

        private final Socket clientSocket;
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        @SneakyThrows
        public void run() {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            try {
                while ((inputLine = in.readLine()) != null) {
                    // Send status to clients
                    if (inputLine.equals(Constants.MSG_SERVER_STATUS)) {
                        StateStatusDto stateStatusDto = new StateStatusDto();
                        stateStatusDto.setEffect(FireflyLuciferin.config.getEffect());
                        stateStatusDto.setRunning(FireflyLuciferin.RUNNING);
                        stateStatusDto.setDeviceTableData(DevicesTabController.deviceTableData);
                        stateStatusDto.setFpsgwconsumer(FireflyLuciferin.FPS_GW_CONSUMER);
                        stateStatusDto.setExit(StateStatusDto.closeOtherInstaces);
                        out.println(CommonUtility.toJsonString(stateStatusDto));
                    } else if (inputLine.contains(Constants.CLIENT_ACTION)) {
                        startStopCapture(inputLine);
                        out.println(Constants.OK);
                    } else if (Constants.EXIT.equals(inputLine)) {
                        out.println("bye");
                        FireflyLuciferin.exit();
                        break;
                    } else { // Collect data from clients and send it to the strip
                        collectAndSendData(inputLine, out);
                    }
                }
                in.close();
                out.close();
                clientSocket.close();
            } catch (SocketException e) {
                log.error(e.getMessage());
            }
        }

    }

    /**
     * Start stop capture based on other instances input
     * @param msg client input
     */
    void startStopCapture(String msg) {

        JsonNode stateStatusDto = CommonUtility.fromJsonToObject(msg);
        assert stateStatusDto != null;
        boolean otherInstanceRunning = stateStatusDto.get(Constants.RUNNING).asBoolean();
        if (FireflyLuciferin.RUNNING != otherInstanceRunning) {
            if (otherInstanceRunning) {
                FireflyLuciferin.guiManager.startCapturingThreads();
            } else {
                FireflyLuciferin.guiManager.stopCapturingThreads(false);
            }
        }

    }

    /**
     * Collect data received from the client and send it to the strip
     * @param inputLine message received from the client
     * @param out       response sent to the client
     */
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
        // Two screen
        if (FireflyLuciferin.config.getMultiMonitor() == 2 && instanceNumber == 2) {
            int j = 1;
            for (int i = CommonUtility.getBottomLed(otherConfig2) + otherConfig2.getRightLed() + 1; i <= ledsString.length - 1; i++) {
                leds[startIndex + j] = new Color(Integer.parseInt(ledsString[i]));
                j++;
            }
            startIndex += (j - 1);
            for (int i = 1; i <= (CommonUtility.getBottomLed(otherConfig2) + otherConfig2.getRightLed()); i++) {
                leds[startIndex + i] = new Color(Integer.parseInt(ledsString[i]));
            }
        }
        // Three screen
        if (FireflyLuciferin.config.getMultiMonitor() == 3 && instanceNumber == 2) {
            int j = 1;
            for (int i = CommonUtility.getBottomLed(otherConfig2) + otherConfig2.getRightLed() + 1; i <= ledsString.length - 1; i++) {
                leds[startIndex + j] = new Color(Integer.parseInt(ledsString[i]));
                j++;
            }
            startIndex += (j - 1) + otherConfig3.getTopLed() + otherConfig3.getLeftLed() + CommonUtility.getBottomLed(otherConfig3);
            for (int i = 1; i <= (CommonUtility.getBottomLed(otherConfig2) + otherConfig2.getRightLed()); i++) {
                leds[startIndex + i] = new Color(Integer.parseInt(ledsString[i]));
            }
        } else if (FireflyLuciferin.config.getMultiMonitor() == 3 && instanceNumber == 3) {
            int j = 1;
            startIndex -= (CommonUtility.getBottomLed(otherConfig2) + otherConfig2.getLeftLed() + otherConfig2.getRightLed());
            for (int i = CommonUtility.getBottomLed(otherConfig3) + otherConfig3.getRightLed() + 1; i <= ledsString.length - 1; i++) {
                leds[startIndex + j] = new Color(Integer.parseInt(ledsString[i]));
                leds[startIndex + j] = new Color(Integer.parseInt(ledsString[i]));
                j++;
            }
            startIndex += (j - 1) - (otherConfig2.getRightLed() + otherConfig2.getLeftLed() );
            for (int i = 1; i <= (CommonUtility.getBottomLed(otherConfig3) + otherConfig3.getRightLed()); i++) {
                leds[startIndex + i] = new Color(Integer.parseInt(ledsString[i]));
            }
        }
        // Main instance
        if (instanceNumber == 1) {
            for (int i = 1; i <= ledsString.length - 1; i++) {
                leds[startIndex + i] = new Color(Integer.parseInt(ledsString[i]));
            }
        }
        if (FireflyLuciferin.config.getMultiMonitor() == 2 && firstDisplayReceived && secondDisplayReceived) {
            firstDisplayReceived = false; secondDisplayReceived = false;
            FireflyLuciferin.sharedQueue.offer(leds);
        } else if (FireflyLuciferin.config.getMultiMonitor() == 3 && firstDisplayReceived && secondDisplayReceived && thirdDisplayReceived) {
            firstDisplayReceived = false; secondDisplayReceived = false; thirdDisplayReceived = false;
            FireflyLuciferin.sharedQueue.offer(leds);
        }
        out.println(inputLine);

    }

    /**
     * Start message server for multi screen, single instance
     */
    public static void startMessageServer() {

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                messageServer.start(Constants.MSG_SERVER_PORT);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }, 0, TimeUnit.SECONDS);

    }

    /**
     * Init totalNumLed based on all instnces
     */
    public static void initNumLed() {

        StorageManager sm = new StorageManager();
        // Server starts if there are 2 or more monitors
        Configuration otherConfig1 = sm.readConfig(Constants.CONFIG_FILENAME);
        firstDisplayLedNum = otherConfig1.getLedMatrix().get(Constants.AspectRatio.FULLSCREEN.getBaseI18n()).size();
        otherConfig2 = sm.readConfig(Constants.CONFIG_FILENAME_2);
        secondDisplayLedNum = otherConfig2.getLedMatrix().get(Constants.AspectRatio.FULLSCREEN.getBaseI18n()).size();
        if (FireflyLuciferin.config.getMultiMonitor() == 3) {
            otherConfig3 = sm.readConfig(Constants.CONFIG_FILENAME_3);
            int thirdDisplayLedNum = otherConfig3.getLedMatrix().get(Constants.AspectRatio.FULLSCREEN.getBaseI18n()).size();
            totalLedNum = firstDisplayLedNum + secondDisplayLedNum + thirdDisplayLedNum;
        } else {
            totalLedNum = firstDisplayLedNum + secondDisplayLedNum;
        }

    }

}