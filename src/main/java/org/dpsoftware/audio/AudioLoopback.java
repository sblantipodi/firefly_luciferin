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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manage Audio loopback and retrieve peaks and RMS values
 */
@Slf4j
public class AudioLoopback {

    public static boolean RUNNING_AUDIO = false;
    AudioFormat fmt = new AudioFormat(44100f, 8, 1, true, false);
    final int bufferByteSize = 2048;
    TargetDataLine line;

    /**
     * Start capturing audio levels
     */
    public void startVolumeLevelMeter() {

        RUNNING_AUDIO = true;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledExecutorService.schedule(() -> {
            try {
                line = AudioSystem.getTargetDataLine(fmt);
                log.debug("Line info: {}", line.getLineInfo());
                line.open(fmt, bufferByteSize);

            } catch (LineUnavailableException e) {
                log.error(e.getMessage());
                return;
            }
            byte[] buf = new byte[bufferByteSize];
            float[] samples = new float[bufferByteSize / 2];
            float lastPeak = 0f;
            line.start();
            int b;
            float maxPeak = 0;
            float maxRms = 0;
            while (((b = line.read(buf, 0, buf.length)) > -1) && RUNNING_AUDIO) {
                for (int i = 0, s = 0; i < b; ) {
                    int sample = 0;
                    sample |= buf[i++] & 0xFF; // (reverse these two lines
                    sample |= buf[i++] << 8;   //  if the format is big endian)
                    // normalize to range of +/-1.0f
                    samples[s++] = sample / 32768f;
                }
                float rms = 0f;
                float peak = 0f;
                for (float sample : samples) {
                    float abs = Math.abs(sample);
                    if (abs > peak) {
                        peak = abs;
                    }
                    rms += sample * sample;
                }
                rms = (float) Math.sqrt(rms / samples.length);
                if (lastPeak > peak) {
                    peak = lastPeak * 0.875f;
                }
                lastPeak = peak;
                maxRms = rms > maxRms ? rms : maxRms;
                maxPeak = lastPeak > maxPeak ? lastPeak : maxPeak;
                float tolerance = FireflyLuciferin.config.getAudioLoopbackGain();
                if (lastPeak > tolerance) lastPeak = tolerance;
                if (rms > tolerance) rms = tolerance;

                sendAudioInfoToStrip(lastPeak, rms, maxPeak, rms, tolerance);

            }
        }, 0, TimeUnit.SECONDS);

    }

    /**
     * Send audio information to the LED Strip (Red and Yellow manages the Peaks, Green manages RMS)
     * @param lastPeak last peak on the audio line
     * @param rms RMS value on the sine wave
     * @param maxPeak max peak on the audio line
     * @param maxRms max RMS value on the sine wave
     * @param tolerance lower the gain, we don't want to set volume to 100% to use all the strip
     */
    private void sendAudioInfoToStrip(float lastPeak, float rms, float maxPeak, float maxRms, float tolerance) {

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

}