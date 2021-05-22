/*
  AudioLoopback.java

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
package org.dpsoftware.audio;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;

import java.awt.*;

/**
 * Manage Audio loopback and retrieve peaks and RMS values
 */
@Slf4j
public class AudioLoopback {

    public static volatile boolean RUNNING_AUDIO = false;
    static float maxPeak, maxRms = 0;

    /**
     * Send audio information to the LED Strip (Red and Yellow manages the Peaks, Green manages RMS)
     *
     * @param lastPeak  last peak on the audio line
     * @param rms       RMS value on the sine wave
     * @param tolerance lower the gain, we don't want to set volume to 100% to use all the strip
     */
     public static void sendAudioInfoToStrip(float lastPeak, float rms, float tolerance) {

        maxRms = Math.max(rms, maxRms);
        maxPeak = Math.max(lastPeak, maxPeak);
        if (FireflyLuciferin.config.isExtendedLog()) {
            log.debug("Peak: {} RMS: {} - MaxPeak: {} MaxRMS: {}", lastPeak, rms, maxPeak, maxRms);
        }
        Color[] leds = new Color[FireflyLuciferin.ledNumber];
        for (int i = 0; i < FireflyLuciferin.ledNumber; i++) {
            leds[i] = new Color(0, 0, 255);
        }
        int peakLeds = (int) ((FireflyLuciferin.ledNumber * lastPeak) / tolerance);
        int peakYellowLeds = ((peakLeds * 30) / 100);
        int rmsLeds = (int) ((FireflyLuciferin.ledNumber * rms) / tolerance);
        if (peakLeds > FireflyLuciferin.ledNumber) {
            peakLeds = FireflyLuciferin.ledNumber;
        }
        if (rmsLeds > FireflyLuciferin.ledNumber) {
            rmsLeds = FireflyLuciferin.ledNumber;
        }
        for (int i = 0; i < peakLeds; i++) {
            if (i < (peakLeds - peakYellowLeds)) {
                leds[i] = new Color(255, 255, 0);
            } else {
                leds[i] = new Color(255, 0, 0);
            }
        }
        for (int i = 0; i < rmsLeds; i++) {
            leds[i] = new Color(0, 255, 0);
        }

        FireflyLuciferin.FPS_PRODUCER_COUNTER++;
        FireflyLuciferin.sharedQueue.offer(leds);

    }

    /**
     * Stop capturing audio levels
     */
    public void stopVolumeLevelMeter() {

        RUNNING_AUDIO = false;

    }

}