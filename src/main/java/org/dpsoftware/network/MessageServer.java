/*
  MessageServer.java

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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.managers.StorageManager;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

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

    /**
     * Start the message server, accepts multiple connections
     * @param port used for the message server
     * @throws IOException socket error
     */
    public void start(int port) throws IOException {

        log.debug("Starting message server");
        StorageManager sm = new StorageManager();
        // Server starts if there are 2 or more monitors
        Configuration otherConfig1 = sm.readConfig(Constants.CONFIG_FILENAME);
        firstDisplayLedNum = otherConfig1.getLedMatrix().get(Constants.AspectRatio.FULLSCREEN.getAspectRatio()).size();
        Configuration otherConfig2 = sm.readConfig(Constants.CONFIG_FILENAME_2);
        secondDisplayLedNum = otherConfig2.getLedMatrix().get(Constants.AspectRatio.FULLSCREEN.getAspectRatio()).size();
        if (FireflyLuciferin.config.getMultiMonitor() == 3) {
            Configuration otherConfig3 = sm.readConfig(Constants.CONFIG_FILENAME_3);
            int thirdDisplayLedNum = otherConfig3.getLedMatrix().get(Constants.AspectRatio.FULLSCREEN.getAspectRatio()).size();
            totalLedNum = firstDisplayLedNum + secondDisplayLedNum + thirdDisplayLedNum;
        } else {
            totalLedNum = firstDisplayLedNum + secondDisplayLedNum;
        }
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
    private static class ClientHandler extends Thread {

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
                        secondDisplayReceived = true;
                        startIndex = (firstDisplayLedNum + secondDisplayLedNum) - 1;
                    }
                    for (int i = 1; i <= ledsString.length - 1; i++) {
                        leds[startIndex + i] = new Color(Integer.parseInt(ledsString[i]));
                    }
                    if (FireflyLuciferin.config.getMultiMonitor() == 2 && firstDisplayReceived && secondDisplayReceived) {
                        firstDisplayReceived = false; secondDisplayReceived = false;
                        FireflyLuciferin.sharedQueue.offer(leds);
                    } else if (FireflyLuciferin.config.getMultiMonitor() == 3 && firstDisplayReceived && secondDisplayReceived && thirdDisplayReceived) {
                        firstDisplayReceived = false; secondDisplayReceived = false; thirdDisplayReceived = false;
                        FireflyLuciferin.sharedQueue.offer(leds);
                    }
                    if (".".equals(inputLine)) {
                        out.println("bye");
                        break;
                    }
                    out.println(inputLine);
                }
                in.close();
                out.close();
                clientSocket.close();
            } catch (SocketException e) {
                log.error(e.getMessage());
            }
        }

    }

}