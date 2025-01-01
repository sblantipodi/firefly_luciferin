/*
  MainSingleton.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.HostServices;
import jdk.incubator.vector.VectorSpecies;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.gui.GuiManager;

import java.awt.*;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;

/**
 * Main singleton used to share common data
 */
@Getter
@Setter
@NoArgsConstructor
public class MainSingleton {

    @Getter
    private final static MainSingleton instance;

    static {
        instance = new MainSingleton();
    }

    // Who am I supposed to be? Used to manage multiple instances of Luciferin running at the same time
    public int whoAmI = 1;
    public boolean spawnInstances = true; // set this to false to avoid spawning new instances on multi monitor setup
    // Calculate Screen Capture Framerate and how fast your microcontroller can consume it
    public float FPS_CONSUMER_COUNTER;
    public float FPS_PRODUCER_COUNTER;
    public float FPS_CONSUMER = 0;
    public float FPS_PRODUCER = 0;
    public float FPS_GW_CONSUMER = 0;
    public SimpleDateFormat formatter;
    public SerialPort serial;
    public OutputStream output;
    public boolean serialConnected = false;
    public int baudRate = 0;
    // LED strip, monitor and microcontroller config
    public Configuration config;
    // Start and Stop threads
    public boolean RUNNING = false;
    // This queue orders elements FIFO. Producer offers some data, consumer throws data to the Serial port
    public BlockingQueue<Color[]> sharedQueue;
    public Color[] lastLedColor;
    // Number of LEDs on the strip
    public int ledNumber;
    public int ledNumHighLowCount;
    public int ledNumHighLowCountSecondPart;
    public GuiManager guiManager;
    public boolean communicationError = false;
    public Color colorInUse;
    public int gpio = 0; // 0 means not set, firmware discards this value
    public int colorOrder = 0; // 1 means GRB, 2 RGB, 3 BGR
    public int ldrAction = 0; // 1 no action, 2 calibrate, 3 reset, 4 save
    public int fireflyEffect = 0;
    public int relayPin = -1;
    public int sbPin = -1;
    public int ldrPin = -1;
    public int gpioClockPin = 0;
    public boolean nightMode = false;
    public String version = "";
    public ResourceBundle bundle;
    public String profileArgs;
    public HostServices hostServices;
    public boolean closeOtherInstaces = false;
    public int wifiStrength = 0;
    public int ldrStrength = 0;
    public boolean restartOnly = false;
    public boolean exitTriggered = false;
    public int supportedSpeciesLengthSimd = 0;
    public VectorSpecies<Integer> SPECIES;
    public boolean initialized = false;
    public boolean cpuLatencyBenchRunning = false;
    public int cpuLatencyBench = 0;

}

