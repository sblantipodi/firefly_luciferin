/*
  AudioLoopback.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.network.NetworkSingleton;
import org.dpsoftware.utilities.CommonUtility;

import java.awt.*;

/**
 * Manage Audio loopback and retrieve peaks and RMS values
 */
@Slf4j
public class AudioLoopback {

    private float maxPeak, maxRms = 0;
    private float maxPeakLeft, maxRmsLeft = 0;
    private float maxPeakRight, maxRmsRight = 0;
    private float rainbowHue = 0;

    /**
     * To right rotate arr[] by offset
     *
     * @param arr    array to rotate
     * @param offset rotate by offset
     */
    void rightRotate(Color[] arr, int offset) {
        int length = arr.length;
        // If arr is rotated length times then you get the same array
        while (offset > length) {
            offset = offset - length;
        }
        // Creating a temporary array of size offset
        Color[] temp = new Color[length - offset];
        // Now copying first length-offset element in array temp
        if (length - offset >= 0) {
            System.arraycopy(arr, 0, temp, 0, length - offset);
        }
        // Moving the rest element to index zero to offset
        if (length - (length - offset) >= 0) {
            System.arraycopy(arr, length - offset, arr, -offset + offset, length - (length - offset));
        }
        // Copying the temp array element in original array
        if (length - offset >= 0) {
            System.arraycopy(temp, 0, arr, offset, length - offset);
        }
    }

    /**
     * To left rotate arr[] by offset
     *
     * @param arr    array to rotate
     * @param offset rotate by offset
     */
    @SuppressWarnings("unused")
    void leftRotate(Color[] arr, int offset) {
        int length = arr.length;
        // Creating temp array of size offset
        Color[] temp = new Color[offset];
        // Copying first offset element in array temp
        System.arraycopy(arr, 0, temp, 0, offset);
        // Moving the rest element to index zero to length-offset
        if (length - offset >= 0) {
            System.arraycopy(arr, offset, arr, 0, length - offset);
        }
        // Copying the temp array element in original array
        System.arraycopy(temp, 0, arr, length - offset, offset);
    }

    /**
     * Choose what to send to the LED strip
     *
     * @param lastPeak  last peak on the audio line
     * @param rms       RMS value on the sine wave
     * @param tolerance lower the gain, we don't want to set volume to 100% to use all the strip
     */
    public void driveLedStrip(float lastPeak, float rms, float tolerance) {
        if (Enums.Effect.MUSIC_MODE_VU_METER.equals(LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect()))) {
            sendAudioInfoToStrip(lastPeak, rms, tolerance);
        } else if (Enums.Effect.MUSIC_MODE_RAINBOW.equals(LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect()))) {
            sendAudioInfoToStrip(lastPeak, rms, tolerance);
            setAudioBrightness(lastPeak);
        } else {
            setAudioBrightness(lastPeak);
        }
    }

    /**
     * Choose what to send to the LED strip
     *
     * @param lastPeakLeft  last peak on the audio line
     * @param lastPeakRight last peak on the audio line
     * @param rmsLeft       RMS value on the sine wave
     * @param rmsRight      RMS value on the sine wave
     * @param tolerance     lower the gain, we don't want to set volume to 100% to use all the strip
     */
    public void driveLedStrip(float lastPeakLeft, float rmsLeft, float lastPeakRight, float rmsRight, float tolerance) {
        sendAudioInfoToStrip(lastPeakLeft, rmsLeft, lastPeakRight, rmsRight, tolerance);
    }

    /**
     * Send audio information to the LED Strip
     *
     * @param lastPeak  last peak on the audio line
     * @param rms       RMS value on the sine wave
     * @param tolerance lower the gain, we don't want to set volume to 100% to use all the strip
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void sendAudioInfoToStrip(float lastPeak, float rms, float tolerance) {
        maxRms = Math.max(rms, maxRms);
        maxPeak = Math.max(lastPeak, maxPeak);
        // log.info("Peak: {} RMS: {} - MaxPeak: {} MaxRMS: {}", lastPeak, rms, maxPeak, maxRms);
        Color[] leds = new Color[NetworkSingleton.getInstance().totalLedNum];

        if (Enums.Effect.MUSIC_MODE_VU_METER.equals(LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect()))) {
            calculateVuMeterEffect(leds, lastPeak, rms, tolerance);
        } else if (Enums.Effect.MUSIC_MODE_RAINBOW.equals(LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect()))) {
            calculateRainbowEffect(leds);
        }

        MainSingleton.getInstance().FPS_PRODUCER_COUNTER++;
        if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
            MainSingleton.getInstance().sharedQueue.offer(leds);
        }
    }

    /**
     * Send audio information to the LED Strip
     *
     * @param lastPeakLeft  last peak on the audio line
     * @param lastPeakRight last peak on the audio line
     * @param rmsLeft       RMS value on the sine wave
     * @param rmsRight      RMS value on the sine wave
     * @param tolerance     lower the gain, we don't want to set volume to 100% to use all the strip
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void sendAudioInfoToStrip(float lastPeakLeft, float rmsLeft, float lastPeakRight, float rmsRight, float tolerance) {
        maxRmsLeft = Math.max(rmsLeft, maxRmsLeft);
        maxPeakLeft = Math.max(lastPeakLeft, maxPeakLeft);
        maxRmsRight = Math.max(rmsLeft, maxRmsRight);
        maxPeakRight = Math.max(lastPeakLeft, maxPeakRight);
        // log.info("Peak: {} RMS: {} - MaxPeak: {} MaxRMS: {}", lastPeak, rms, maxPeak, maxRms);
        Color[] leds = new Color[NetworkSingleton.getInstance().totalLedNum];
        calculateVuMeterEffectDual(leds, lastPeakLeft, rmsLeft, lastPeakRight, rmsRight, tolerance);
        MainSingleton.getInstance().FPS_PRODUCER_COUNTER++;
        if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
            MainSingleton.getInstance().sharedQueue.offer(leds);
        }
    }

    /**
     * Set audio brightness
     *
     * @param lastPeak lastPeak during audio recording
     */
    public void setAudioBrightness(float lastPeak) {
        int brigthness = (int) (254f * lastPeak);
        AudioSingleton.getInstance().AUDIO_BRIGHTNESS = Math.min(brigthness, 254);
    }

    /**
     * Create a VU Meter, (Red and Yellow for the Peaks, Green for RMS)
     *
     * @param leds      LEDs array to send to the strip
     * @param lastPeak  last peak on the audio line
     * @param rms       RMS value on the sine wave
     * @param tolerance lower the gain, we don't want to set volume to 100% to use all the strip
     */
    private void calculateVuMeterEffect(Color[] leds, float lastPeak, float rms, float tolerance) {
        for (int i = 0; i < NetworkSingleton.getInstance().totalLedNum; i++) {
            leds[i] = new Color(0, 0, 255);
        }
        int peakLeds = (int) ((NetworkSingleton.getInstance().totalLedNum * lastPeak) * tolerance);
        int peakYellowLeds = ((peakLeds * 30) / 100);
        int rmsLeds = (int) ((NetworkSingleton.getInstance().totalLedNum * rms) * tolerance);
        if (peakLeds > NetworkSingleton.getInstance().totalLedNum) {
            peakLeds = NetworkSingleton.getInstance().totalLedNum;
        }
        if (rmsLeds > NetworkSingleton.getInstance().totalLedNum) {
            rmsLeds = NetworkSingleton.getInstance().totalLedNum;
        }
        setLedsColor(leds, peakLeds, peakYellowLeds, rmsLeds);
    }

    /**
     * Create a VU Meter, (Red and Yellow for the Peaks, Green for RMS)
     *
     * @param leds          LEDs array to send to the strip
     * @param lastPeakLeft  last peak on the audio line
     * @param lastPeakRight last peak on the audio line
     * @param rmsLeft       RMS value on the sine wave
     * @param rmsRight      RMS value on the sine wave
     * @param tolerance     lower the gain, we don't want to set volume to 100% to use all the strip
     */
    private void calculateVuMeterEffectDual(Color[] leds, float lastPeakLeft, float rmsLeft, float lastPeakRight, float rmsRight, float tolerance) {
        int ledNumDual = ((NetworkSingleton.getInstance().totalLedNum % 2) == 0) ? (NetworkSingleton.getInstance().totalLedNum / 2) : ((NetworkSingleton.getInstance().totalLedNum / 2) + 1);
        for (int i = 0; i < NetworkSingleton.getInstance().totalLedNum; i++) {
            leds[i] = new Color(0, 0, 255);
        }
        int peakLeds = (int) ((ledNumDual * lastPeakRight) * tolerance);
        int peakYellowLeds = ((peakLeds * 30) / 100);
        int rmsLeds = (int) ((ledNumDual * rmsRight) * tolerance);
        if (peakLeds > ledNumDual) {
            peakLeds = ledNumDual;
        }
        if (rmsLeds > ledNumDual) {
            rmsLeds = ledNumDual;
        }
        setLedsColor(leds, peakLeds, peakYellowLeds, rmsLeds);
        ledNumDual = NetworkSingleton.getInstance().totalLedNum / 2;
        peakLeds = (int) ((ledNumDual * lastPeakLeft) * tolerance);
        peakYellowLeds = ((peakLeds * 30) / 100);
        rmsLeds = (int) ((ledNumDual * rmsLeft) * tolerance);
        if (peakLeds > ledNumDual) {
            peakLeds = ledNumDual;
        }
        if (rmsLeds > ledNumDual) {
            rmsLeds = ledNumDual;
        }
        for (int i = 1; i <= peakLeds; i++) {
            if (i <= (peakLeds - peakYellowLeds)) {
                leds[NetworkSingleton.getInstance().totalLedNum - i] = new Color(255, 255, 0);
            } else {
                leds[NetworkSingleton.getInstance().totalLedNum - i] = new Color(255, 0, 0);
            }
        }
        for (int i = 1; i <= rmsLeds; i++) {
            leds[NetworkSingleton.getInstance().totalLedNum - i] = new Color(0, 255, 0);
        }
        if (!CommonUtility.isSplitBottomRow(MainSingleton.getInstance().config.getSplitBottomMargin())) {
            rightRotate(leds, MainSingleton.getInstance().config.getBottomRowLed() / 2);
        }
    }

    /**
     * Set LEDs color based on peaks and rms
     *
     * @param leds           leds arrat
     * @param peakLeds       audio peaks
     * @param peakYellowLeds yellow audio peaks
     * @param rmsLeds        rms audio
     */
    private void setLedsColor(Color[] leds, int peakLeds, int peakYellowLeds, int rmsLeds) {
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
    }

    /**
     * Create an audio rainbow effect
     *
     * @param leds LEDs array to send to the strip
     */
    private void calculateRainbowEffect(Color[] leds) {
        for (int i = 0; i < NetworkSingleton.getInstance().totalLedNum; i++) {
            leds[i] = Color.getHSBColor(rainbowHue, 1.0f, 1.0f);
        }
        if (rainbowHue >= 1) rainbowHue = 0;
        rainbowHue += 0.002f;
    }

    /**
     * Stop capturing audio levels
     */
    public void stopVolumeLevelMeter() {
        AudioSingleton.getInstance().RUNNING_AUDIO = false;
    }

}