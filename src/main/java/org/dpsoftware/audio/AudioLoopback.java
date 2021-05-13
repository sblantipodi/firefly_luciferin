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

import org.dpsoftware.FireflyLuciferin;

import javax.sound.sampled.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AudioLoopback {

    public static boolean RUNNING_AUDIO = false;
    AudioFormat fmt = new AudioFormat(44100f, 8, 1, true, false);
    final int bufferByteSize = 2048;
    TargetDataLine line;

    public void startVolumeLevelMeter() {

        RUNNING_AUDIO = true;

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.schedule(() -> {
            try {

            try {
                line = AudioSystem.getTargetDataLine(fmt);
                System.out.println(line.getLineInfo());
                line.open(fmt, bufferByteSize);

            } catch (LineUnavailableException e) {
                System.err.println(e);
                return;
            }



                byte[] buf = new byte[bufferByteSize];
            float[] samples = new float[bufferByteSize / 2];

            float lastPeak = 0f;

            line.start();
//                FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
//                System.out.println("Volume:"+control.getValue());

//                FloatControl controll = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
//                System.out.println("Volume:"+controll.getValue());
            int b;
            float maxPeak = 0;
            float maxRms = 0;
            float tolerance = 0.7f;
            while(((b = line.read(buf, 0, buf.length)) > -1) && RUNNING_AUDIO) {

                // convert bytes to samples here
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

//                rms += 0.22f;

                // 0.21 0.11
//                if (lastPeak > 0.58) lastPeak = 0.58f;
//                if (rms > 0.33) rms = 0.33f;

                if (lastPeak > tolerance) lastPeak = tolerance;
                if (rms > tolerance) rms = tolerance;

                System.out.print("peak: " + lastPeak);
                System.out.println(" rms: " + rms);

//                System.out.print("peak: " + maxPeak);
//                System.out.println(" rms: " + maxRms);


                Color[] leds = new Color[FireflyLuciferin.ledNumber];

                for (int i=0; i<FireflyLuciferin.ledNumber; i++) {
                    leds[i] = new Color(0, 0, 255);
                }



                int peakLeds = (int) ((FireflyLuciferin.ledNumber * lastPeak) / tolerance);
                int peakYellowLeds = ((peakLeds * 20) / 100);
                int rmsLeds = (int) ((FireflyLuciferin.ledNumber * rms) / tolerance);






                for (int i=0; i < peakLeds; i++) {
                    if (i < (peakLeds - peakYellowLeds)) {
                        leds[i] = new Color(255, 255, 0);
                    } else {
                        leds[i] = new Color(255, 0, 0);
                    }

                }

                for (int i=0; i < rmsLeds; i++) {
                    leds[i] = new Color(0, 255, 0);

                }


                FireflyLuciferin.sharedQueue.offer(leds);


            }
            } catch (Exception e) {
                System.out.print(e.getMessage());
            }
        }, 0, TimeUnit.SECONDS);

    }




}