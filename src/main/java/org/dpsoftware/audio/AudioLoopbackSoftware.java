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

import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.config.Constants;
import xt.audio.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manage Software audio loopback and retrieve peaks and RMS values
 */
@Slf4j
public class AudioLoopbackSoftware extends AudioLoopback implements AudioUtility {

    static int runNumber = 0;
    static float lastRmsRun = 0;
    static float lastPeackRun = 0;
    static String defaultOutputDevice;

    /**
     * Start software capturing audio levels, does not require a native audio loopback in the OS
     */
    public void startVolumeLevelMeter() {

        RUNNING_AUDIO = true;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledExecutorService.schedule(() -> {

            try (XtPlatform platform = XtAudio.init("demo", Pointer.NULL)) {
                XtService service = platform.getService(Enums.XtSystem.WASAPI);
                try (XtDeviceList list = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.INPUT))) {
                    for (int count = 0; count < list.getCount(); count++) {
                        String id = list.getId(count);
                        String devi = list.getName(id);
                        log.debug(devi);
                        EnumSet<Enums.XtDeviceCaps> caps = list.getCapabilities(id);
                        if (caps.contains(Enums.XtDeviceCaps.LOOPBACK) && (devi.substring(0, devi.lastIndexOf("(")).equals(defaultOutputDevice))) {
                            try (XtDevice device = service.openDevice(id)) {
                                Structs.XtStreamParams streamParams = new Structs.XtStreamParams(true, AudioLoopbackSoftware::onBuffer, null, null);
                                Structs.XtFormat format = new Structs.XtFormat(new Structs.XtMix(48000, Enums.XtSample.FLOAT32), new Structs.XtChannels(2, 0, 0, 0));
                                Structs.XtBufferSize buffer = device.getBufferSize(format);
                                Structs.XtDeviceStreamParams deviceParams = new Structs.XtDeviceStreamParams(streamParams, format, buffer.current);
                                try (XtStream stream = device.openStream(deviceParams, null);
                                XtSafeBuffer ignored = XtSafeBuffer.register(stream, true)) {
                                    log.debug("Using: {}", devi);
                                    stream.start();
                                    while (RUNNING_AUDIO) {
                                        Thread.onSpinWait();
                                    }
                                    stream.stop();
                                    scheduledExecutorService.shutdown();
                                }
                            }
                        }
                    }
                }
            } catch (XtException e) {
                log.debug(e.getMessage());
            }
            scheduledExecutorService.shutdown();
        }, 0, TimeUnit.SECONDS);

    }

    /**
     * Callback called ever 10ms containing the audio stream, calculate RMS and Peaks from the stream
     * and send it to the strip
     * @param stream    audio stream
     * @param buffer    audio buffer
     * @param audioData audio data
     * @return non significative one
     */
    static int onBuffer(XtStream stream, Structs.XtBuffer buffer, Object audioData) {

        XtSafeBuffer safe = XtSafeBuffer.get(stream);
        if (safe != null) {
            safe.lock(buffer);
            float[] samples = (float[]) safe.getInput();
            float lastPeak = 0f;
            float rms = 0f;
            float peak = 0f;
            for (int i = 0; i < buffer.frames; i++) {
                float sample = (samples[i * 4] + samples[i * 4 + 1]);
                float abs = Math.abs(sample);
                if (abs > peak) {
                    peak = abs;
                }
                rms += ((sample * sample) + (sample * sample));
            }
            rms = (float) Math.sqrt(rms / (samples.length / 4.0));
            if (lastPeak > peak) {
                peak = lastPeak * 0.875f;
            }
            lastPeak = peak;
            float tolerance = FireflyLuciferin.config.getAudioLoopbackGain();
            // WASAPI runs every 10ms giving 100FPS, average reading and reduce it by 5 for 20FPS
            if (runNumber < 5) {
                if (lastPeak > lastPeackRun) {
                    lastPeackRun = lastPeak * 0.875f;
                }
                if (rms > lastRmsRun) {
                    lastRmsRun = rms * 0.875f;
                }
            } else {
                runNumber = 0;
                lastRmsRun = 0f;
                lastPeackRun = 0f;
                if (Constants.Effect.MUSIC_MODE_VU_METER.getEffect().equals(FireflyLuciferin.config.getEffect())) {
                    sendAudioInfoToStrip(lastPeak, rms, tolerance);
                } else {
                    setAudioBrightness(lastPeak);
                }
            }
            runNumber++;
            safe.unlock(buffer);
        }
        return 0;

    }

    /**
     * Print all loopback devices
     * @return a map containing the audio default loopback
     */
    @Override
    public Map<String, String> getLoopbackDevices() {

        Map<String, String> audioDevices = new HashMap<>();
        XtAudio.setOnError(AudioLoopbackSoftware::onError);
        try (XtPlatform platform = XtAudio.init("Sample", null)) {
            Enums.XtSystem pro = platform.setupToSystem(Enums.XtSetup.PRO_AUDIO);
            log.debug("Pro Audio: " + pro + " (" + (platform.getService(pro) != null) + ")");
            Enums.XtSystem system = platform.setupToSystem(Enums.XtSetup.SYSTEM_AUDIO);
            log.debug("System Audio: " + system + " (" + (platform.getService(system) != null) + ")");
            Enums.XtSystem consumer = platform.setupToSystem(Enums.XtSetup.CONSUMER_AUDIO);
            log.debug("Consumer Audio: " + consumer + " (" + (platform.getService(consumer) != null) + ")");
            for (Enums.XtSystem systemName : platform.getSystems()) {
                XtService service = platform.getService(systemName);
                log.debug("System " + systemName + ":");
                log.debug("  Capabilities: " + service.getCapabilities());
                try (XtDeviceList all = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.ALL))) {
                    String defaultInputId = service.getDefaultDeviceId(false);
                    if (defaultInputId != null) {
                        String name = all.getName(defaultInputId);
                        log.debug("  Default input: " + name + " (" + defaultInputId + ")");
                    }
                    String defaultOutputId = service.getDefaultDeviceId(true);
                    if (defaultOutputId != null) {
                        String name = all.getName(defaultOutputId);
                        log.debug("  Default output: " + name + " (" + defaultOutputId + ")");
                        if (systemName.name().equals(Constants.WASAPI)) {
                            audioDevices.put(defaultOutputId, name);
                            defaultOutputDevice = name.substring(0, name.lastIndexOf("("));
                        }
                    }
                }
                try (XtDeviceList inputs = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.INPUT))) {
                    log.debug("  Input device count: " + inputs.getCount());
                    printDevices(service, inputs);
                }
                try (XtDeviceList outputs = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.OUTPUT))) {
                    log.debug("  Output device count: " + outputs.getCount());
                    printDevices(service, outputs);
                }
            }
        } catch (XtException e) {
            log.error(String.valueOf(XtAudio.getErrorInfo(e.getError())));
        } catch (Throwable t) {
            log.error(t.getMessage());
        }
        return audioDevices;

    }

    /**
     * Print all the audio devices available
     * @param service service containing
     * @param list    device list
     */
    static void printDevices(XtService service, XtDeviceList list) {

        for (int d = 0; d < list.getCount(); d++) {
            String id = list.getId(d);
            try (XtDevice device = service.openDevice(id)) {
                Optional<Structs.XtMix> mix = device.getMix();
                log.debug("    Device " + id + ":");
                log.debug("      Name: " + list.getName(id));
                log.debug("      Capabilities: " + list.getCapabilities(id));
                log.debug("      Input channels: " + device.getChannelCount(false));
                log.debug("      Output channels: " + device.getChannelCount(true));
                log.debug("      Interleaved access: " + device.supportsAccess(true));
                log.debug("      Non-interleaved access: " + device.supportsAccess(false));
                mix.ifPresent(xtMix -> log.debug("      Current mix: " + xtMix.rate + " " + xtMix.sample));
            } catch (XtException e) {
                log.error(String.valueOf(XtAudio.getErrorInfo(e.getError())));
            }
        }

    }

    /**
     * Log the error message
     * @param message error msg
     */
    static void onError(String message) {
        log.error(message);
    }

}