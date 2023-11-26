/*
  AudioLoopback.java

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

import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.dto.AudioDevice;
import xt.audio.*;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manage Software audio loopback and retrieve peaks and RMS values
 */
@Slf4j
public class AudioLoopbackSoftware extends AudioLoopback implements AudioUtility {

    int runNumber = 0;
    float lastRmsRun = 0, lastRmsRunLeft = 0, lastRmsRunRight = 0;
    float lastPeackRun = 0, lastPeackRunLeft = 0, lastPeackRunRight = 0;

    /**
     * Log the error message
     *
     * @param message error msg
     */
    static void onError(String message) {
        log.trace(message);
    }

    /**
     * Callback called ever 10ms containing the audio stream, calculate RMS and Peaks from the stream
     * and send it to the strip
     *
     * @param stream    audio stream
     * @param buffer    audio buffer
     * @param audioData audio data
     * @return non significative one
     */
    int onBuffer(XtStream stream, Structs.XtBuffer buffer, Object audioData) {
        XtSafeBuffer safe = XtSafeBuffer.get(stream);
        if (safe != null) {
            safe.lock(buffer);
            float[] samples = (float[]) safe.getInput();
            buildStereoPeaks(buffer, samples);
            runNumber++;
            safe.unlock(buffer);
        }
        return 0;
    }

    /**
     * Generate peaks and RMS from audio buffer (mono and stereo)
     *
     * @param buffer  audio buffer
     * @param samples audio samples
     */
    void buildStereoPeaks(Structs.XtBuffer buffer, float[] samples) {
        float lastPeak = 0f;
        float rms = 0f;
        float peak = 0f;
        float lastPeakLeft = 0f;
        float rmsLeft = 0f;
        float peakLeft = 0f;
        float lastPeakRight = 0f;
        float rmsRight = 0f;
        float peakRight = 0f;
        for (int i = 0; i < buffer.frames; i++) {
            float sample = (samples[i * 4] + samples[i * 4 + 1]);
            float sampleLeft = (samples[i * 4]);
            float sampleRight = (samples[i * 4 + 3]);
            float abs = Math.abs(sample);
            float absLeft = Math.abs(sampleLeft);
            float absRight = Math.abs(sampleRight);
            if (abs > peak) {
                peak = abs;
            }
            if (absLeft > peakLeft) {
                peakLeft = absLeft;
            }
            if (absRight > peakRight) {
                peakRight = absRight;
            }
            rms += ((sample * sample) + (sample * sample));
            rmsLeft += (sampleLeft * sampleLeft);
            rmsRight += (sampleRight * sampleRight);
        }
        rms = (float) Math.sqrt(rms / (samples.length / 4.0));
        rmsLeft = (float) Math.sqrt(rmsLeft / (samples.length / 2.0));
        rmsRight = (float) Math.sqrt(rmsRight / (samples.length / 2.0));
        if (lastPeak > peak) {
            peak = lastPeak * 0.875f;
        }
        if (lastPeakLeft > peakLeft) {
            peakLeft = lastPeakLeft * 0.875f;
        }
        if (lastPeakRight > peakRight) {
            peakRight = lastPeakRight * 0.875f;
        }
        lastPeak = peak;
        lastPeakLeft = peakLeft;
        lastPeakRight = peakRight;
        float tolerance = 1.0f + ((MainSingleton.getInstance().config.getAudioLoopbackGain() * 0.1f) * 2);
        // WASAPI runs every 10ms giving 100FPS, average reading and reduce it by 5 for 20FPS
        if (runNumber < 5) {
            if (lastPeak > lastPeackRun) {
                lastPeackRun = lastPeak * 0.875f;
            }
            if (lastPeakLeft > lastPeackRunLeft) {
                lastPeackRunLeft = lastPeakLeft * 0.875f;
            }
            if (lastPeakRight > lastPeackRunRight) {
                lastPeackRunRight = lastPeakRight * 0.875f;
            }
            if (rms > lastRmsRun) {
                lastRmsRun = rms * 0.875f;
            }
            if (rmsLeft > lastRmsRunLeft) {
                lastRmsRunLeft = rmsLeft * 0.875f;
            }
            if (rmsRight > lastRmsRunRight) {
                lastRmsRunRight = rmsRight * 0.875f;
            }
        } else {
            runNumber = 0;
            lastRmsRun = 0f;
            lastPeackRun = 0f;
            lastRmsRunLeft = 0f;
            lastPeackRunLeft = 0f;
            lastRmsRunRight = 0f;
            lastPeackRunRight = 0f;
            // Send RMS and Peaks value to the LED strip
            if (org.dpsoftware.config.Enums.Effect.MUSIC_MODE_VU_METER_DUAL.equals(LocalizedEnum.fromBaseStr(org.dpsoftware.config.Enums.Effect.class, MainSingleton.getInstance().config.getEffect()))) {
                driveLedStrip(lastPeakLeft, rmsLeft, lastPeakRight, rmsRight, tolerance);
            } else {
                driveLedStrip(lastPeak, rms, tolerance);
            }
        }
    }

    /**
     * Print all the audio devices available
     *
     * @param service         service containing
     * @param list            device list
     * @param addDevice       add device to the system device list
     * @param defaultOutputId default output to use
     */
    void printDevices(XtService service, XtDeviceList list, boolean addDevice, String defaultOutputId) {
        for (int d = 0; d < list.getCount(); d++) {
            String id = list.getId(d);
            try (XtDevice device = service.openDevice(id)) {
                Optional<Structs.XtMix> mix = device.getMix();
                String deviceName = list.getName(id);
                AtomicInteger sampleRate = new AtomicInteger(MainSingleton.getInstance().config.getSampleRate() == 0 ?
                        Constants.DEFAULT_SAMPLE_RATE : MainSingleton.getInstance().config.getSampleRate());
                log.trace("    Device " + id + ":");
                log.trace("      Name: " + deviceName);
                log.trace("      Capabilities: " + list.getCapabilities(id));
                log.trace("      Input channels: " + device.getChannelCount(false));
                log.trace("      Output channels: " + device.getChannelCount(true));
                log.trace("      Interleaved access: " + device.supportsAccess(true));
                log.trace("      Non-interleaved access: " + device.supportsAccess(false));
                mix.ifPresent(xtMix -> {
                    log.trace("      Current mix: " + xtMix.rate + " " + xtMix.sample);
                    sampleRate.set(xtMix.rate);
                });
                if (id.equals(defaultOutputId)) {
                    AudioSingleton.getInstance().audioDevices.get(id).setSampleRate(sampleRate.get());
                }
                if (addDevice && deviceName.contains(Constants.LOOPBACK)) {
                    AudioSingleton.getInstance().audioDevices.put(id, new AudioDevice(deviceName, sampleRate.get()));
                }
            } catch (XtException e) {
                log.trace(String.valueOf(XtAudio.getErrorInfo(e.getError())));
            }
        }
    }

    /**
     * Start software capturing audio levels, does not require a native audio loopback in the OS
     */
    @SuppressWarnings("unused")
    public void startVolumeLevelMeter() {
        AtomicBoolean audioEngaged = new AtomicBoolean(false);
        AudioSingleton.getInstance().RUNNING_AUDIO = true;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        scheduledExecutorService.scheduleAtFixedRate(() -> {

            if (!audioEngaged.get()) {
                try (XtPlatform platform = XtAudio.init("DPsoftwareAudio", Pointer.NULL)) {
                    Enums.XtSystem system = platform.setupToSystem(Enums.XtSetup.SYSTEM_AUDIO);
                    XtService service = platform.getService(NativeExecutor.isWindows() ? Enums.XtSystem.WASAPI : Enums.XtSystem.PULSE_AUDIO);
                    try (XtDeviceList list = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.INPUT))) {
                        for (int count = 0; count < list.getCount(); count++) {
                            String id = list.getId(count);
                            String devi = list.getName(id);
                            EnumSet<Enums.XtDeviceCaps> caps = list.getCapabilities(id);
                            String defaultDeviceStr = AudioSingleton.getInstance().audioDevices.entrySet().iterator().next().getValue().getDeviceName();
                            int sampleRate = AudioSingleton.getInstance().audioDevices.entrySet().iterator().next().getValue().getSampleRate();
                            String idd = AudioSingleton.getInstance().audioDevices.entrySet().iterator().next().getKey();

                            if (MainSingleton.getInstance().config.getAudioDevice().equals(org.dpsoftware.config.Enums.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getBaseI18n())) {
                                defaultDeviceStr = defaultDeviceStr.substring(0, defaultDeviceStr.lastIndexOf("("));
                            } else {
                                defaultDeviceStr = MainSingleton.getInstance().config.getAudioDevice().substring(0, MainSingleton.getInstance().config.getAudioDevice().lastIndexOf("("));
                            }
                            if (caps.contains(Enums.XtDeviceCaps.LOOPBACK) && (devi.substring(0, devi.lastIndexOf("(")).equals(defaultDeviceStr))) {
                                try (XtDevice device = service.openDevice(id)) {
                                    Structs.XtStreamParams streamParams = new Structs.XtStreamParams(true, this::onBuffer, null, null);
                                    Structs.XtFormat format = new Structs.XtFormat(new Structs.XtMix(sampleRate, Enums.XtSample.FLOAT32),
                                            new Structs.XtChannels(Integer.parseInt(MainSingleton.getInstance().config.getAudioChannels().substring(0, 1)), 0, 0, 0));
                                    log.info(defaultDeviceStr);
                                    log.trace("Device Key: " + idd);
                                    if (device.supportsFormat(format)) {
                                        log.trace("Device format supported");
                                        Structs.XtBufferSize buffer = device.getBufferSize(format);
                                        Structs.XtDeviceStreamParams deviceParams = new Structs.XtDeviceStreamParams(streamParams, format, buffer.current);
                                        try (XtStream stream = device.openStream(deviceParams, null);
                                             XtSafeBuffer ignored = XtSafeBuffer.register(stream)) {
                                            log.info("Audio device engaged, using: {}", devi);
                                            stream.start();
                                            audioEngaged.set(true);
                                            while (AudioSingleton.getInstance().RUNNING_AUDIO) {
                                                Thread.onSpinWait();
                                            }
                                            log.trace(("Stopping audio recording"));
                                            stream.stop();
                                            stream.close();
                                            scheduledExecutorService.shutdown();
                                        }
                                    } else {
                                        log.trace("Audio format not supported, stopping audio recording and retry.");
                                        audioEngaged.set(false);
                                    }
                                }
                            }
                        }
                    }
                } catch (XtException e) {
                    log.trace(e.getMessage());
                }
            }
        }, 1, 5, TimeUnit.SECONDS);
    }

    /**
     * Print all loopback devices
     *
     * @return a map containing the audio default loopback
     */
    @Override
    public Map<String, AudioDevice> getLoopbackDevices() {
        XtAudio.setOnError(AudioLoopbackSoftware::onError);
        try (XtPlatform platform = XtAudio.init("Sample", null)) {
            Enums.XtSystem pro = platform.setupToSystem(Enums.XtSetup.PRO_AUDIO);
            log.trace("Pro Audio: " + pro + " (" + (platform.getService(pro) != null) + ")");
            Enums.XtSystem system = platform.setupToSystem(Enums.XtSetup.SYSTEM_AUDIO);
            log.trace("System Audio: " + system + " (" + (platform.getService(system) != null) + ")");
            Enums.XtSystem consumer = platform.setupToSystem(Enums.XtSetup.CONSUMER_AUDIO);
            log.trace("Consumer Audio: " + consumer + " (" + (platform.getService(consumer) != null) + ")");
            String defaultOutputWASAPIId = "";
            for (Enums.XtSystem systemName : platform.getSystems()) {
                XtService service = platform.getService(systemName);
                log.trace("System " + systemName + ":");
                log.trace("  Capabilities: " + service.getCapabilities());
                try (XtDeviceList all = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.ALL))) {
                    String defaultInputId = service.getDefaultDeviceId(false);
                    if (defaultInputId != null) {
                        String name = all.getName(defaultInputId);
                        log.trace("  Default input: " + name + " (" + defaultInputId + ")");
                    }
                    String defaultOutputId = service.getDefaultDeviceId(true);
                    if (defaultOutputId != null) {
                        String name = all.getName(defaultOutputId);
                        log.trace("  Default output: " + name + " (" + defaultOutputId + ")");
                        if (MainSingleton.getInstance().config.getAudioDevice().equals(org.dpsoftware.config.Enums.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getBaseI18n())) {
                            if (NativeExecutor.isWindows() && systemName.name().equals(Constants.WASAPI)) {
                                defaultOutputWASAPIId = defaultOutputId;
                                AudioSingleton.getInstance().audioDevices.put(defaultOutputId, new AudioDevice(name, MainSingleton.getInstance().config.getSampleRate() == 0 ?
                                        Constants.DEFAULT_SAMPLE_RATE : MainSingleton.getInstance().config.getSampleRate()));
                            }
                        }
                    }
                }
                try (XtDeviceList inputs = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.INPUT))) {
                    log.trace("  Input device count: " + inputs.getCount());
                    printDevices(service, inputs, true, defaultOutputWASAPIId);
                }
                try (XtDeviceList outputs = service.openDeviceList(EnumSet.of(Enums.XtEnumFlags.OUTPUT))) {
                    log.trace("  Output device count: " + outputs.getCount());
                    printDevices(service, outputs, false, defaultOutputWASAPIId);
                }
            }
        } catch (XtException e) {
            log.error(String.valueOf(XtAudio.getErrorInfo(e.getError())));
        } catch (Throwable t) {
            log.error(t.getMessage());
        }
        return AudioSingleton.getInstance().audioDevices;
    }

}