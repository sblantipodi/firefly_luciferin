/*
  UdpServer.java

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
package org.dpsoftware.network.tcpUdp;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Constants;
import org.dpsoftware.managers.UpgradeManager;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * UDP server class
 */
@Slf4j
public class UdpServer {

    public static boolean udpBroadcastReceiverRunning = false;

    /**
     * Receive UDP broadcast from Glow Worm Luciferin device
     */
    @SuppressWarnings("Duplicates")
    public void receiveBroadcastUDPPacket() {

        CompletableFuture.supplyAsync(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(Constants.UDP_BROADCAST_PORT);
                socket.setBroadcast(true);
                System.out.println("UDP broadcast listen on " + socket.getLocalAddress());
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                while (udpBroadcastReceiverRunning) {
                    CommonUtility.conditionedLog(this.getClass().getTypeName(), "Waiting for UDP broadcast");
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    JsonNode responseJson = CommonUtility.fromJsonToObject(received);
                    if (responseJson != null && responseJson.get(Constants.STATE) != null) {
                        CommonUtility.updateDeviceTable(Objects.requireNonNull(responseJson));
                        CommonUtility.updateFpsWithDeviceTopic(Objects.requireNonNull(responseJson));
                    } else if (responseJson != null && responseJson.get(Constants.MQTT_FRAMERATE) != null) {
                        CommonUtility.updateFpsWithFpsTopic(Objects.requireNonNull(responseJson));
                    } else if (UpgradeManager.deviceNameForSerialDevice.equals(received)) {
                        log.debug("Update successfull=" + received);
                        if (!CommonUtility.isSingleDeviceMultiScreen() || CommonUtility.isSingleDeviceMainInstance()) {
                            javafx.application.Platform.runLater(() -> FireflyLuciferin.guiManager.showAlert(Constants.FIREFLY_LUCIFERIN,
                                    Constants.UPGRADE_SUCCESS, received + Constants.DEVICEUPGRADE_SUCCESS,
                                    Alert.AlertType.INFORMATION));
                        }
                        CommonUtility.sleepSeconds(60);
                        FireflyLuciferin.guiManager.startCapturingThreads();
                    }
                    CommonUtility.conditionedLog(this.getClass().getTypeName(), "UDP broadcast received");
                    CommonUtility.conditionedLog(this.getClass().getTypeName(), received);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            return null;
        });

    }

}
