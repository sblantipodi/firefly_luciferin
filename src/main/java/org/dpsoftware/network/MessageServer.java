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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private ScheduledExecutorService serverWatchdog;

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
        if (serverWatchdog != null && !serverWatchdog.isShutdown()) {
            return;
        }
        serverWatchdog = Executors.newSingleThreadScheduledExecutor();
        serverWatchdog.scheduleWithFixedDelay(() -> {
            if (isRunning()) {
                return;
            }
            try {
                start(Constants.MSG_SERVER_PORT);
            } catch (IOException e) {
                log.error("Unable to start message server: {}", e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * Init totalNumLed based on all instances
     */
    public synchronized void initNumLed() {
        StorageManager sm = new StorageManager();
        // Server starts if there are 2 or more monitors
        Configuration newMonitorConfig1 = sm.readConfigFile(Constants.CONFIG_FILENAME);
        Configuration newMonitorConfig2 = sm.readConfigFile(Constants.CONFIG_FILENAME_2);
        Integer newFirstDisplayLedNum = getFullscreenLedNum(newMonitorConfig1);
        Integer newSecondDisplayLedNum = getFullscreenLedNum(newMonitorConfig2);
        if (newFirstDisplayLedNum == null || newSecondDisplayLedNum == null) {
            log.warn("Unable to refresh LED number, keeping previous monitor configuration");
            return;
        }
        monitorConfig1 = newMonitorConfig1;
        monitorConfig2 = newMonitorConfig2;
        firstDisplayLedNum = newFirstDisplayLedNum;
        secondDisplayLedNum = newSecondDisplayLedNum;
        if (MainSingleton.getInstance().config.getMultiMonitor() == 3) {
            Configuration newMonitorConfig3 = sm.readConfigFile(Constants.CONFIG_FILENAME_3);
            Integer thirdDisplayLedNum = getFullscreenLedNum(newMonitorConfig3);
            if (thirdDisplayLedNum == null) {
                log.warn("Unable to refresh LED number, keeping previous monitor configuration");
                return;
            }
            monitorConfig3 = newMonitorConfig3;
            NetworkSingleton.getInstance().totalLedNum = firstDisplayLedNum + secondDisplayLedNum + thirdDisplayLedNum;
        } else {
            NetworkSingleton.getInstance().totalLedNum = firstDisplayLedNum + secondDisplayLedNum;
        }
        if (leds == null || leds.length != NetworkSingleton.getInstance().totalLedNum) {
            leds = new Color[NetworkSingleton.getInstance().totalLedNum];
            resetDisplayReceived();
        }
    }

    /**
     * Get fullscreen LED count from a monitor configuration.
     *
     * @param config monitor configuration
     * @return LED count, null when config is temporarily unavailable or incomplete
     */
    private Integer getFullscreenLedNum(Configuration config) {
        if (config == null || config.getLedMatrix() == null) {
            return null;
        }
        LinkedHashMap<Integer, org.dpsoftware.LEDCoordinate> fullscreenLedMatrix = config.getLedMatrix().get(Enums.AspectRatio.FULLSCREEN.getBaseI18n());
        return fullscreenLedMatrix == null ? null : fullscreenLedMatrix.size();
    }

    /**
     * Reset received-frame flags when LED buffers are rebuilt.
     */
    private void resetDisplayReceived() {
        firstDisplayReceived = false;
        secondDisplayReceived = false;
        thirdDisplayReceived = false;
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
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(Constants.MSG_SERVER_HOST));
        log.info("Message server listening on {}:{}", Constants.MSG_SERVER_HOST, port);
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
        if (serverWatchdog != null) {
            serverWatchdog.shutdownNow();
        }
    }

    /**
     * Check if message server is listening.
     *
     * @return true if server socket is open
     */
    public boolean isRunning() {
        return serverSocket != null && !serverSocket.isClosed();
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
    private void collectAndSendData(String inputLine, PrintWriter out) {
        try {
            initNumLed();
            String[] ledsString = inputLine.split(",");
            int instanceNumber = Integer.parseInt(ledsString[0]);
            int receivedLedNum = ledsString.length - 1;
            int expectedLedNum = getExpectedLedNum(instanceNumber);
            if (expectedLedNum == 0 || receivedLedNum != expectedLedNum) {
                log.debug("Led number has changed");
                resetDisplayReceived();
                initNumLed();
                out.println(inputLine);
                return;
            }
            int startIndex;
            if (instanceNumber == 1) {
                firstDisplayReceived = true;
                startIndex = -1;
            } else if (instanceNumber == 2) {
                secondDisplayReceived = true;
                startIndex = firstDisplayLedNum - 1;
            } else if (instanceNumber == 3) {
                thirdDisplayReceived = true;
                startIndex = (firstDisplayLedNum + secondDisplayLedNum) - 1;
            } else {
                out.println(inputLine);
                return;
            }
            for (int i = 1; i <= ledsString.length - 1; i++) {
                leds[startIndex + i] = new Color(Integer.parseInt(ledsString[i]));
            }
            if (MainSingleton.getInstance().config.getMultiMonitor() == 2 && firstDisplayReceived && secondDisplayReceived) {
                firstDisplayReceived = false;
                secondDisplayReceived = false;
                offerCompleteFrame();
            } else if (MainSingleton.getInstance().config.getMultiMonitor() == 3 && firstDisplayReceived && secondDisplayReceived && thirdDisplayReceived) {
                firstDisplayReceived = false;
                secondDisplayReceived = false;
                thirdDisplayReceived = false;
                offerCompleteFrame();
            }
            out.println(inputLine);
        } catch (ArrayIndexOutOfBoundsException e) {
            log.debug("Led number has changed");
            initNumLed();
            out.println(inputLine);
        }
    }

    /**
     * Get expected LED count for a monitor instance.
     *
     * @param instanceNumber monitor instance number
     * @return expected LED count
     */
    private int getExpectedLedNum(int instanceNumber) {
        return switch (instanceNumber) {
            case 1 -> firstDisplayLedNum;
            case 2 -> secondDisplayLedNum;
            case 3 ->
                    MainSingleton.getInstance().config.getMultiMonitor() == 3 && monitorConfig3 != null ? getFullscreenLedNum(monitorConfig3) : 0;
            default -> 0;
        };
    }

    /**
     * Offer a frame only when all LEDs have been filled with the current matrix size.
     */
    private void offerCompleteFrame() {
        for (Color led : leds) {
            if (led == null) {
                resetDisplayReceived();
                return;
            }
        }
        MainSingleton.getInstance().sharedQueue.offer(leds);
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
