/*
  Configuration.java

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
package org.dpsoftware.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Configuration used in the FireflyLuciferin.yaml file, supports deep copy
 * All defaults can be manually overridden in the yaml file
 */
@NoArgsConstructor
@Getter
@Setter
public class Configuration implements Cloneable {

    public Object clone()throws CloneNotSupportedException{
        return super.clone();
    }

    // Number of CPU Threads to use, this app is heavy multithreaded,
    // high cpu cores equals to higher framerate but big CPU usage
    // 4 Threads are enough for 24FPS on an Intel i7 5930K@4.2GHz
    // 3 thread is enough for 30FPS with GPU Hardware Acceleration and uses nearly no CPU
    private int numberOfCPUThreads;

    // WinAPI and DDUPL enables GPU Hardware Acceleration, CPU uses CPU brute force only,
    // DDUPL (Desktop Duplication API) is recommended in Win8/Win10/Win11
    public enum CaptureMethod {
        CPU,
        WinAPI,
        DDUPL,
        XIMAGESRC,
        AVFVIDEOSRC
    }

    // Windows Desktop Duplication API
    private String captureMethod;

    // Serial port to use, use AUTO for automatic port search
    // NOTE: for multi display this contain the deviceName of the MQTT device where to stream
    private String serialPort;

    // Arduino/Microcontroller config
    private String baudRate = Constants.DEFAULT_BAUD_RATE;

    // Default led matrix to use
    private String defaultLedMatrix;
    private boolean autoDetectBlackBars = true;

    // Numbers of LEDs
    int topLed;
    int leftLed;
    int rightLed;
    int bottomLeftLed;
    int bottomRightLed;
    int bottomRowLed;

    // LED strip orientation
    String orientation;

    // used for Serial connection timeout
    private int timeout = 100;

    // Screen resolution
    private int screenResX;
    private int screenResY;

    // OS Scaling factor example: 150%
    private int osScaling;

    // Gamma correction of 2.2 is recommended for LEDs like WS2812B or similar
    private double gamma;

    // White temperature for color correction (Kelvin)
    private int whiteTemperature = Constants.DEFAULT_WHITE_TEMP;

    // Used for RGB, RGBW strips (accurate, brighter)
    private int colorMode = 1;

    // MQTT WiFi Config params
    private boolean wifiEnable = false;
    private String mqttServer = "";
    private String mqttTopic = "";
    private String mqttUsername = "";
    private String mqttPwd = "";
    private boolean mqttEnable = false;
    private boolean mqttStream = false; // this refers to wireless stream, old name for compatibility with previous version
    private String streamType = Constants.StreamType.UDP.getStreamType();
    private boolean checkForUpdates = true;
    // Misc Tab
    private boolean eyeCare = false;
    private String nightModeFrom = LocalTime.now().withHour(22).withMinute(0).truncatedTo(ChronoUnit.MINUTES).toString();
    private String nightModeTo = LocalTime.now().withHour(8).withMinute(0).truncatedTo(ChronoUnit.MINUTES).toString();
    private String nightModeBrightness = "0%";
    private boolean toggleLed = true;
    private String desiredFramerate = "30";
    private String colorChooser = Constants.DEFAULT_COLOR_CHOOSER;
    private int brightness;
    private int ledStartOffset = 0;
    private String splitBottomMargin = Constants.SPLIT_BOTTOM_MARGIN_DEFAULT;
    private boolean startWithSystem = true;
    private int multiMonitor = 1;
    private int monitorNumber = 1;
    private boolean syncCheck = true;
    private String effect = Constants.Effect.BIAS_LIGHT.getBaseI18n();
    private float audioLoopbackGain = 0.0f;
    private String audioDevice = NativeExecutor.isWindows() ? Constants.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getBaseI18n()
            : Constants.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n();
    private String audioChannels = Constants.AudioChannels.AUDIO_CHANNEL_2.getBaseI18n();
    private boolean multiScreenSingleDevice = false;
    private String powerSaving = "";
    private String theme = Constants.Theme.DEFAULT.getBaseI18n();
    private String language;

    // LED Matrix Map
    private Map<String, LinkedHashMap<Integer, LEDCoordinate>> ledMatrix;
    // Deprecated values
    private boolean splitBottomRow;

    private boolean extendedLog = false;
    private String configVersion = "";


    /**
     * Constructor
     * @param fullScreenLedMatrix config matrix for LED strip
     * @param letterboxLedMatrix letterbox config matrix for LED strip
     */
    public Configuration(LinkedHashMap<Integer, LEDCoordinate> fullScreenLedMatrix, LinkedHashMap<Integer, LEDCoordinate> letterboxLedMatrix,
                         LinkedHashMap<Integer, LEDCoordinate> fitScreenLedMatrix) {
        this.ledMatrix = new LinkedHashMap<>();
        ledMatrix.put(Constants.AspectRatio.FULLSCREEN.getBaseI18n(), fullScreenLedMatrix);
        ledMatrix.put(Constants.AspectRatio.LETTERBOX.getBaseI18n(), letterboxLedMatrix);
        ledMatrix.put(Constants.AspectRatio.PILLARBOX.getBaseI18n(), fitScreenLedMatrix);
    }

    /**
     * Get the LED Matrix in use from the available list
     * @param ledMatrixInUse config matrix for LED strip
     * @return return led matrix in use
     */
    public LinkedHashMap<Integer, LEDCoordinate> getLedMatrixInUse(String ledMatrixInUse) {
        return ledMatrix.get(ledMatrixInUse);
    }

}