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
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.utilities.CommonUtility;
import xt.audio.*;

import java.util.EnumSet;
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

    /**
     * Start software capturing audio levels, does not require a native audio loopback in the OS
     */
     @SuppressWarnings("unused")
    public void startVolumeLevelMeter() {

        RUNNING_AUDIO = true;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledExecutorService.schedule(() -> {

            try (XtPlatform platform = XtAudio.init("DPsoftwareAudio", Pointer.NULL)) {
                Enums.XtSystem system = platform.setupToSystem(Enums.XtSetup.SYSTEM_AUDIO);
                XtService service = platform.getService(NativeExecutor.isWindows() ? Enums.XtSystem.WASAPI : Enums.XtSystem.PULSE_AUDIO);
                try (XtDeviceList list = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.INPUT))) {
                    for (int count = 0; count < list.getCount(); count++) {
                        String id = list.getId(count);
                        String devi = list.getName(id);
                        EnumSet<Enums.XtDeviceCaps> caps = list.getCapabilities(id);
                        String defaultDeviceStr = audioDevices.entrySet().iterator().next().getValue();
                        if (FireflyLuciferin.config.getAudioDevice().equals(Constants.DEFAULT_AUDIO_OUTPUT)) {
                            defaultDeviceStr = defaultDeviceStr.substring(0, defaultDeviceStr.lastIndexOf("("));
                        } else {
                            defaultDeviceStr = FireflyLuciferin.config.getAudioDevice().substring(0, FireflyLuciferin.config.getAudioDevice().lastIndexOf("("));
                        }
                        if (caps.contains(Enums.XtDeviceCaps.LOOPBACK) && (devi.substring(0, devi.lastIndexOf("(")).equals(defaultDeviceStr))) {
                            try (XtDevice device = service.openDevice(id)) {
                                Structs.XtStreamParams streamParams = new Structs.XtStreamParams(true, AudioLoopbackSoftware::onBuffer, null, null);
                                Structs.XtFormat format = new Structs.XtFormat(new Structs.XtMix(48000, Enums.XtSample.FLOAT32),
                                        new Structs.XtChannels(Integer.parseInt(FireflyLuciferin.config.getAudioChannels().substring(0, 1)), 0, 0, 0));
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
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), e.getMessage());
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
            float tolerance = 1.0f + ((FireflyLuciferin.config.getAudioLoopbackGain() * 0.1f) * 2);
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
                // Send RMS and Peaks value to the LED strip
                driveLedStrip(lastPeak, rms, tolerance);
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

        XtAudio.setOnError(AudioLoopbackSoftware::onError);
        try (XtPlatform platform = XtAudio.init("Sample", null)) {
            Enums.XtSystem pro = platform.setupToSystem(Enums.XtSetup.PRO_AUDIO);
            CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "Pro Audio: " + pro + " (" + (platform.getService(pro) != null) + ")");
            Enums.XtSystem system = platform.setupToSystem(Enums.XtSetup.SYSTEM_AUDIO);
            CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "System Audio: " + system + " (" + (platform.getService(system) != null) + ")");
            Enums.XtSystem consumer = platform.setupToSystem(Enums.XtSetup.CONSUMER_AUDIO);
            CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "Consumer Audio: " + consumer + " (" + (platform.getService(consumer) != null) + ")");
            for (Enums.XtSystem systemName : platform.getSystems()) {
                XtService service = platform.getService(systemName);
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "System " + systemName + ":");
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "  Capabilities: " + service.getCapabilities());
                try (XtDeviceList all = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.ALL))) {
                    String defaultInputId = service.getDefaultDeviceId(false);
                    if (defaultInputId != null) {
                        String name = all.getName(defaultInputId);
                        CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "  Default input: " + name + " (" + defaultInputId + ")");
                    }
                    String defaultOutputId = service.getDefaultDeviceId(true);
                    if (defaultOutputId != null) {
                        String name = all.getName(defaultOutputId);
                        CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "  Default output: " + name + " (" + defaultOutputId + ")");
                        if (FireflyLuciferin.config.getAudioDevice().equals(Constants.DEFAULT_AUDIO_OUTPUT)) {
                            if (NativeExecutor.isWindows() && systemName.name().equals(Constants.WASAPI)) {
                                audioDevices.put(defaultOutputId, name);
                            }
                        }
                    }
                }
                try (XtDeviceList inputs = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.INPUT))) {
                    CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "  Input device count: " + inputs.getCount());
                    printDevices(service, inputs, true);
                }
                try (XtDeviceList outputs = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.OUTPUT))) {
                    CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "  Output device count: " + outputs.getCount());
                    printDevices(service, outputs, false);
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
     * @param service   service containing
     * @param list      device list
     * @param addDevice add device to the system device list
     */
    static void printDevices(XtService service, XtDeviceList list, boolean addDevice) {

        for (int d = 0; d < list.getCount(); d++) {
            String id = list.getId(d);
            try (XtDevice device = service.openDevice(id)) {
                Optional<Structs.XtMix> mix = device.getMix();
                String deviceName = list.getName(id);
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "    Device " + id + ":");
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "      Name: " + deviceName);
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "      Capabilities: " + list.getCapabilities(id));
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "      Input channels: " + device.getChannelCount(false));
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "      Output channels: " + device.getChannelCount(true));
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "      Interleaved access: " + device.supportsAccess(true));
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "      Non-interleaved access: " + device.supportsAccess(false));
                mix.ifPresent(xtMix -> CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), "      Current mix: " + xtMix.rate + " " + xtMix.sample));
                if (addDevice && deviceName.contains(Constants.LOOPBACK)) {
                    AudioLoopback.audioDevices.put(id, deviceName);
                }
            } catch (XtException e) {
                CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), String.valueOf(XtAudio.getErrorInfo(e.getError())));
            }
        }

    }

    /**
     * Log the error message
     * @param message error msg
     */
    static void onError(String message) {
        CommonUtility.conditionedLog(AudioLoopbackNative.class.getName(), message);
    }

}