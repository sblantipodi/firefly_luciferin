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

import java.util.Map;


/**
 * Configuration used in the FastScreenCapture.yaml file
 */
public class Configuration {

    // Number of CPU Threads to use, this app is heavy multithreaded,
    // high cpu cores equals to higher framerate but big CPU usage
    // 4 Threads are enough for 24FPS on an Intel i7 5930K@4.2GHz
    private int numberOfCPUThreads = 4;

    // Arduino/Microcontroller config
    private int dataRate = 500000;

    // used for Serial connection timeout
    private int timeout = 2000;

    // Screen resolution
    private int screenResX = 3840;
    private int screenResY = 2160;

    // OS Scaling factor example: 150%
    private int osScaling = 150;
    private int ledOffset = 30;

    // LED Matrix Map
    private Map<Integer, LEDCoordinate> ledMatrix;

    public Configuration() {
    }

    public Configuration(Map ledMatrix) {
        this.ledMatrix = ledMatrix;
    }

    public int getNumberOfCPUThreads() {
        return numberOfCPUThreads;
    }

    public int getDataRate() {
        return dataRate;
    }

    public Map getLedMatrix() {
        return ledMatrix;
    }

    public int getTimeout() {
        return timeout;
    }

    // Screen resolution (with OS scaling - es: 3840x2160 with 150% OS scaling equals to 2560x1440)
    public int getScreenResX() {
        return screenResX;
    }

    // Screen resolution (with OS scaling - es: 3840x2160 with 150% OS scaling equals to 2560x1440)
    public int getScreenResY() {
        return screenResY;
    }

    public int getOsScaling() {
        return osScaling;
    }

    public int getLedOffset() {
        return ledOffset;
    }

    public void setLedMatrix(Map<Integer, LEDCoordinate> ledMatrix) {
        this.ledMatrix = ledMatrix;
    }

    public void setNumberOfCPUThreads(int numberOfCPUThreads) {
        this.numberOfCPUThreads = numberOfCPUThreads;
    }

    public void setDataRate(int dataRate) {
        this.dataRate = dataRate;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setScreenResX(int screenResX) {
        this.screenResX = screenResX;
    }

    public void setScreenResY(int screenResY) {
        this.screenResY = screenResY;
    }

    public void setOsScaling(int osScaling) {
        this.osScaling = osScaling;
    }

    public void setLedOffset(int ledOffset) {
        this.ledOffset = ledOffset;
    }

}
