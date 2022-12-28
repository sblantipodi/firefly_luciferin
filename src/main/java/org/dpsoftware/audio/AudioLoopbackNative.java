/*
  AudioLoopbackNative.java

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
package org.dpsoftware.audio;

import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.dto.AudioDevice;
import org.dpsoftware.managers.dto.AudioVuMeter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manage Native Audio loopback and retrieve peaks and RMS values
 */
@Slf4j
public class AudioLoopbackNative extends AudioLoopback implements AudioUtility {

    AudioFormat fmt = new AudioFormat(Constants.DEFAULT_SAMPLE_RATE_NATIVE, 16, Integer.parseInt(FireflyLuciferin.config.getAudioChannels().substring(0, 1)), true, true);
    final int bufferByteSize = 2048;
    TargetDataLine line;

    /**
     * Start Native capturing audio levels, requires a native audio loopback in the OS, calculate
     * RMS and Peaks from the stream and send it to the strip
     */
    public void startVolumeLevelMeter() {
        RUNNING_AUDIO = true;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledExecutorService.schedule(() -> {
            try {
                line = AudioSystem.getTargetDataLine(fmt);
                line.open(fmt, bufferByteSize);

            } catch (LineUnavailableException | IllegalArgumentException e) {
                log.error(e.getMessage());
                RUNNING_AUDIO = false;
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
            }
            byte[] buf = new byte[bufferByteSize];
            float[] samples = new float[bufferByteSize / 2];
            line.start();
            while (((line.read(buf, 0, buf.length)) > -1) && RUNNING_AUDIO) {
                AudioVuMeter audioVuMeterLeft;
                AudioVuMeter audioVuMeterRight;
                if (Constants.Effect.MUSIC_MODE_VU_METER_DUAL.equals(LocalizedEnum.fromBaseStr(Constants.Effect.class, FireflyLuciferin.config.getEffect()))) {
                    audioVuMeterLeft = calculatePeakAndRMS(buf, samples, 0);
                    audioVuMeterRight = calculatePeakAndRMS(buf, samples, 1);
                    driveLedStrip(audioVuMeterLeft.getPeak(), audioVuMeterLeft.getRms(), audioVuMeterRight.getPeak(),
                            audioVuMeterRight.getRms(), audioVuMeterRight.getTolerance());
                } else {
                    audioVuMeterLeft = calculatePeakAndRMS(buf, samples, 0);
                    // Send RMS and Peaks value to the LED strip
                    driveLedStrip(audioVuMeterLeft.getPeak(), audioVuMeterLeft.getRms(), audioVuMeterLeft.getTolerance());
                }
            }
            line.stop();
            line.flush();
            line.close();
            scheduledExecutorService.shutdown();
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * Calculate peak and RMS audio value
     * @param buf     audio buffer
     * @param samples audio samples
     * @param channel audio channels 0 = Left, 1 = Right
     * @return peaks, rms and tolerance
     */
    private static AudioVuMeter calculatePeakAndRMS(byte[] buf, float[] samples, int channel) {
        float lastPeak = 0f;
        for (int i = 0, s = 0; i < buf.length; i+=4) {
            int sample;
            // left = 0; right 1
            int off = channel * 2;
            sample = ( buf[ i + off ] << 8 ) | ( buf[ i + off + 1 ] & 0xFF );
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
        float tolerance = 1.3f + ((FireflyLuciferin.config.getAudioLoopbackGain() * 0.1f) * 2);
        if (lastPeak > tolerance) lastPeak = tolerance;
        if (rms > tolerance) rms = tolerance;

        return new AudioVuMeter(rms, lastPeak, tolerance);
    }

    /**
     * Return the default audio loopback if present
     * @return audio loopback
     */
    @Override
    public Map<String, AudioDevice> getLoopbackDevices() {
        Map<String, AudioDevice> audioDevices = new HashMap<>();
        try {
            line = AudioSystem.getTargetDataLine(fmt);
            log.debug("Line info: {}", line.getLineInfo());
            line.open(fmt, bufferByteSize);
            line.stop();
            line.flush();
            line.close();
            audioDevices.put("", new AudioDevice(Constants.Audio.DEFAULT_AUDIO_OUTPUT.getBaseI18n(),
                    (int) line.getFormat().getSampleRate()));
        } catch (IllegalArgumentException | LineUnavailableException e) {
            log.error(e.getMessage());
        }
        return audioDevices;
    }

}