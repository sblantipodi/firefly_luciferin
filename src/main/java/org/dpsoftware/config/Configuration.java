/*
  Configuration.java

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
package org.dpsoftware.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.managers.dto.HSLColor;

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

    // Color correction, Hue-Saturation (using HSV 360° wheel)
    public Map<Enums.ColorEnum, HSLColor> hueMap;
    // Numbers of LEDs
    int topLed;
    int leftLed;
    int rightLed;
    int bottomLeftLed;
    int bottomRightLed;
    int bottomRowLed;
    // LED strip orientation
    String orientation;
    // Number of CPU Threads to use, this app is heavy multithreaded,
    // high cpu cores equals to higher framerate but big CPU usage
    // 4 Threads are enough for 24FPS on an Intel i7 5930K@4.2GHz
    // 3 thread is enough for 30FPS with GPU Hardware Acceleration and uses nearly no CPU
    private int numberOfCPUThreads;
    // Windows Desktop Duplication API
    private String captureMethod;
    // Serial port to use, use AUTO for automatic port search
    // NOTE: for full firmware this contains the deviceName of the MQTT device where to stream
    @JsonProperty("serialPort")
    private String outputDevice;
    // Arduino/Microcontroller config
    private String baudRate = Constants.DEFAULT_BAUD_RATE;
    // Default led matrix to use
    private String defaultLedMatrix;
    private boolean autoDetectBlackBars = true;
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
    @JsonProperty("wifiEnable")
    private boolean fullFirmware = false; // old name for compatibility with previous version
    private String mqttServer = "";
    private String mqttTopic = "dpsoftwaremqtt";
    private String mqttUsername = "123StellaStella";
    private String mqttPwd = "";
    private String mqttDiscoveryTopic = "homeassistant";
    private boolean mqttEnable = false;
    @JsonProperty("mqttStream")
    private boolean wirelessStream = false; // this refers to wireless stream (MQTT or UDP), old name for compatibility with previous version
    private String streamType = Enums.StreamType.UDP.getStreamType();
    private boolean checkForUpdates = true;
    // Misc Tab
    private boolean eyeCare = false;
    private String nightModeFrom = LocalTime.now().withHour(22).withMinute(0).truncatedTo(ChronoUnit.MINUTES).toString();
    private String nightModeTo = LocalTime.now().withHour(8).withMinute(0).truncatedTo(ChronoUnit.MINUTES).toString();
    private String nightModeBrightness = "0%";
    private boolean toggleLed = true;
    private String desiredFramerate = Constants.DEFAULT_FRAMERATE;
    private String frameInsertion = Enums.FrameInsertion.NO_SMOOTHING.getBaseI18n();
    private String colorChooser = Constants.DEFAULT_COLOR_CHOOSER;
    private int brightness;
    private int ledStartOffset = 0;
    private String splitBottomMargin = Constants.SPLIT_BOTTOM_MARGIN_DEFAULT;
    private String grabberAreaTopBottom = Constants.GRABBER_AREA_TOP_BOTTOM_DEFAULT;
    private String grabberSide = Constants.GRABBER_AREA_SIDE_DEFAULT;
    private String gapTypeTopBottom = Constants.GAP_TYPE_DEFAULT_TOP_BOTTOM;
    private String gapTypeSide = Constants.GAP_TYPE_DEFAULT_SIDE;
    private boolean startWithSystem = true;
    private int multiMonitor = 1;
    private int monitorNumber = 1;
    private boolean syncCheck = true;
    private String effect = Enums.Effect.BIAS_LIGHT.getBaseI18n();
    private float audioLoopbackGain = 0.0f;
    private String audioDevice = NativeExecutor.isWindows() ? Enums.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getBaseI18n()
            : Enums.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n();
    private String audioChannels = Enums.AudioChannels.AUDIO_CHANNEL_2.getBaseI18n();
    private boolean multiScreenSingleDevice = false;
    private String powerSaving = "";
    private String theme = Enums.Theme.DEFAULT.getBaseI18n();
    private String language;
    private int groupBy = Constants.GROUP_BY_LEDS;
    // This is just a temporary variable to know what is the current profile once the user select a profile via tray icon
    private String defaultProfile = Constants.DEFAULT;
    // LDR
    private boolean enableLDR;
    private boolean ldrTurnOff;
    private int ldrInterval;
    private int ldrMin;
    // Brightness limiter
    private Float brightnessLimiter = Enums.BrightnessLimiter.BRIGHTNESS_LIMIT_DISABLED.getBrightnessLimitFloat();
    private int sampleRate = 0;
    // LED Matrix Map
    private Map<String, LinkedHashMap<Integer, LEDCoordinate>> ledMatrix;
    // Deprecated values
    private boolean splitBottomRow = true;
    private boolean extendedLog = false;
    private String configVersion = "";

    /**
     * Constructor
     *
     * @param fullScreenLedMatrix config matrix for LED strip
     * @param letterboxLedMatrix  letterbox config matrix for LED strip
     * @param fitScreenLedMatrix  pillarbox config matrix for LED strip
     * @param hueMap              used for color correction
     */
    public Configuration(LinkedHashMap<Integer, LEDCoordinate> fullScreenLedMatrix, LinkedHashMap<Integer, LEDCoordinate> letterboxLedMatrix,
                         LinkedHashMap<Integer, LEDCoordinate> fitScreenLedMatrix, Map<Enums.ColorEnum, HSLColor> hueMap) {
        this.ledMatrix = new LinkedHashMap<>();
        ledMatrix.put(Enums.AspectRatio.FULLSCREEN.getBaseI18n(), fullScreenLedMatrix);
        ledMatrix.put(Enums.AspectRatio.LETTERBOX.getBaseI18n(), letterboxLedMatrix);
        ledMatrix.put(Enums.AspectRatio.PILLARBOX.getBaseI18n(), fitScreenLedMatrix);
        this.hueMap = hueMap;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Get the LED Matrix in use from the available list
     *
     * @param ledMatrixInUse config matrix for LED strip
     * @return return led matrix in use
     */
    public LinkedHashMap<Integer, LEDCoordinate> getLedMatrixInUse(String ledMatrixInUse) {
        return ledMatrix.get(ledMatrixInUse);
    }

    // WinAPI and DDUPL enables GPU Hardware Acceleration, CPU uses CPU brute force only,
    // DDUPL (Desktop Duplication API) is recommended in Win8/Win10/Win11
    public enum CaptureMethod {
        CPU,
        WinAPI,
        DDUPL,
        XIMAGESRC,
        AVFVIDEOSRC
    }

}