/*
  Configuration.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/
package org.dpsoftware;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;


/**
 * Configuration used in the FastScreenCapture.yaml file
 * All defaults can be manually overridden in the yaml file
 */
@NoArgsConstructor
@Getter
@Setter
public class Configuration {

    // Number of CPU Threads to use, this app is heavy multithreaded,
    // high cpu cores equals to higher framerate but big CPU usage
    // 4 Threads are enough for 24FPS on an Intel i7 5930K@4.2GHz
    // 3 thread is enough for 30FPS with GPU Hardware Acceleration and uses nearly no CPU
    private int numberOfCPUThreads;

    // WinAPI and DDUPL enables GPU Hardware Acceleration, CPU uses CPU brute force only,
    // DDUPL (Desktop Duplication API) is recommended in Win8/Win10
    public enum CaptureMethod {
        CPU,
        WinAPI,
        DDUPL
    }

    // Windows Desktop Duplication API
    private CaptureMethod captureMethod;

    // Serial port to use, use AUTO for automatic port search
    private String serialPort;

    // Arduino/Microcontroller config
    private int dataRate = 500000;

    // Default led matrix to use
    private String defaultLedMatrix;

    // used for Serial connection timeout
    private int timeout = 2000;

    // Screen resolution
    private int screenResX;
    private int screenResY;

    // OS Scaling factor example: 150%
    private int osScaling;

    // Gamma correction of 2.2 is recommended for LEDs like WS2812B or similar
    private double gamma;

    // MQTT Config params
    private String mqttServer = "";
    private String mqttTopic = "";
    private String mqttUsername = "";
    private String mqttPwd = "";

    // LED Matrix Map
    private Map<String, Map<Integer, LEDCoordinate>> ledMatrix;

    /**
     * Constructor
     * @param fullScreenLedMatrix config matrix for LED strip
     * @param letterboxLedMatrix letterbox config matrix for LED strip
     */
    public Configuration(Map<Integer, LEDCoordinate> fullScreenLedMatrix, Map<Integer, LEDCoordinate> letterboxLedMatrix) {

        this.ledMatrix = new HashMap<>();
        ledMatrix.put("FullScreen", fullScreenLedMatrix);
        ledMatrix.put("Letterbox", letterboxLedMatrix);

    }

    /**
     * Get the LED Matrix in use from the available list
     * @param ledMatrixInUse config matrix for LED strip
     * @return return led matrix in use
     */
    public Map<Integer, LEDCoordinate> getLedMatrixInUse(String ledMatrixInUse) {

        return ledMatrix.get(ledMatrixInUse);

    }

}