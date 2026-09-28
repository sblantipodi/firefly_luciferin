/*
  MiscTabOptions.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.gui.controllers.options;

import com.fasterxml.jackson.databind.JsonNode;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.audio.AudioLoopbackSoftware;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.dto.AudioDevice;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.utilities.CommonUtility;

import java.util.*;

/**
 * Values and conversions shared by the JavaFX Misc tab and web settings page.
 */
public final class MiscTabOptions {

    /**
     * Prevents instantiation.
     */
    private MiscTabOptions() {
    }

    /**
     * Returns the gamma choices used by the Misc tab.
     *
     * @return gamma values
     */
    public static List<String> gammaValues() {
        return Arrays.stream(Enums.Gamma.values()).map(Enums.Gamma::getGamma).toList();
    }

    /**
     * Returns the editable capture framerate choices.
     *
     * @return stored values and displayed labels
     */
    public static List<Choice> captureFramerates() {
        return Arrays.stream(Enums.Framerate.values()).map(rate -> {
            boolean unlocked = rate == Enums.Framerate.UNLOCKED;
            return new Choice(unlocked ? rate.getBaseI18n() : rate.getI18n(),
                    unlocked ? rate.getI18n() : rate.getI18n() + Constants.FPS_VAL);
        }).toList();
    }

    /**
     * Returns audio outputs offered by the Misc tab, including system devices.
     *
     * @return distinct audio device names
     */
    public static List<String> audioDeviceNames() {
        Set<String> names = new LinkedHashSet<>();
        if (NativeExecutor.isWindows()) {
            names.add(Enums.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getI18n());
        }
        names.add(Enums.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getI18n());
        try {
            if (!AudioSingleton.getInstance().audioDevices.isEmpty()) {
                AudioSingleton.getInstance().audioDevices.values().stream()
                        .map(AudioDevice::getDeviceName).forEach(names::add);
            } else {
                new AudioLoopbackSoftware().getLoopbackDevices().values().stream()
                        .map(AudioDevice::getDeviceName)
                        .filter(name -> name.contains(Constants.LOOPBACK) || name.contains(Constants.SHARED))
                        .forEach(names::add);
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Native audio discovery can be unavailable during headless startup.
        }
        return new ArrayList<>(names);
    }

    /**
     * Checks whether an effect uses audio input controls instead of color controls.
     *
     * @param effectValue stored or localized effect value
     * @return true for a music effect
     */
    public static boolean isAudioEffect(String effectValue) {
        if (effectValue == null) {
            return false;
        }
        Enums.Effect effect = LocalizedEnum.fromBaseStr(Enums.Effect.class, effectValue);
        if (effect == null) {
            effect = LocalizedEnum.fromStr(Enums.Effect.class, effectValue);
        }
        return effect == Enums.Effect.MUSIC_MODE_VU_METER
                || effect == Enums.Effect.MUSIC_MODE_VU_METER_DUAL
                || effect == Enums.Effect.MUSIC_MODE_BRIGHT
                || effect == Enums.Effect.MUSIC_MODE_RAINBOW;
    }

    /**
     * Converts the GUI brightness percentage into the stored 0–255 value.
     *
     * @param percent brightness percentage
     * @return stored brightness
     */
    public static int storedBrightness(double percent) {
        return (int) (percent / 100 * 255);
    }

    /**
     * Converts stored brightness into the GUI percentage.
     *
     * @param brightness stored 0–255 brightness
     * @return GUI percentage
     */
    public static int brightnessPercent(int brightness) {
        return (int) Math.round(brightness / 255.0 * 100);
    }

    /**
     * Converts a kelvin control value into the stored hundreds of kelvin.
     *
     * @param kelvin selected color temperature
     * @return stored color temperature
     */
    public static int storedWhiteTemperature(double kelvin) {
        return (int) (kelvin / 100);
    }

    /**
     * Converts the stored color temperature to the GUI kelvin value.
     *
     * @param stored stored hundreds of kelvin
     * @return kelvin value
     */
    public static int whiteTemperatureKelvin(int stored) {
        return stored * 100;
    }

    /**
     * Formats RGBA channels for the stored color picker value.
     *
     * @param red   red channel
     * @param green green channel
     * @param blue  blue channel
     * @param alpha alpha channel
     * @return stored comma-separated color
     */
    public static String colorChooser(int red, int green, int blue, int alpha) {
        return red + "," + green + "," + blue + "," + alpha;
    }

    /**
     * Converts an HTML color to the stored color while keeping the existing opacity.
     *
     * @param hex      HTML six-digit color
     * @param previous stored color that provides the opacity
     * @return stored comma-separated color
     */
    public static String colorChooserFromHex(String hex, String previous) {
        if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Invalid color");
        }
        String[] channels = previous == null ? new String[0] : previous.split(",");
        int alpha = channels.length >= 4 ? Integer.parseInt(channels[3]) : 255;
        return colorChooser(Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16), alpha);
    }

    /**
     * Converts a GUI framerate label or editable number to its stored value.
     *
     * @param value GUI framerate value
     * @return stored framerate
     */
    public static String storedFramerate(String value) {
        if (value == null || value.isBlank()) {
            return Constants.DEFAULT_FRAMERATE;
        }
        if (value.equals(Enums.Framerate.UNLOCKED.getI18n())
                || value.equals(Enums.Framerate.UNLOCKED.getBaseI18n())) {
            return Enums.Framerate.UNLOCKED.getBaseI18n();
        }
        String number = value.replace(Constants.FPS_VAL, "").trim();
        if (!number.matches("[0-9]+") || Integer.parseInt(number) < 1) {
            throw new IllegalArgumentException("Invalid capture framerate");
        }
        return number;
    }

    /**
     * Applies the built-in smoothing level's dependent frame insertion values.
     *
     * @param config         configuration to update
     * @param smoothingValue stored or localized smoothing value
     */
    public static void applySmoothing(Configuration config, String smoothingValue) {
        SmoothingOptions.applyPreset(config, smoothingValue);
    }

    /**
     * Applies a live change from the web Misc tab to the running application and device.
     *
     * @param config running configuration
     * @param field  edited field name
     * @param value  new field value
     * @return true when this field belongs to the Misc tab
     */
    public static boolean applyWebChange(Configuration config, String field, JsonNode value) {
        if (!Set.of("toggleLed", "effect", "colorMode", "gamma", "brightness", "whiteTemperature",
                "desiredFramerate", "smoothingType", "audioDevice", "audioLoopbackGain",
                "nightModeFrom", "nightModeTo", "nightModeBrightness", "audioChannels").contains(field)) {
            return false;
        }
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing value for " + field);
        }
        String text = value.asText();
        switch (field) {
            case "toggleLed" -> {
                if (!value.isBoolean()) throw new IllegalArgumentException("toggleLed must be boolean");
                config.setToggleLed(value.asBoolean());
                if (value.asBoolean()) CommonUtility.turnOnLEDs();
                else CommonUtility.turnOffLEDs(config);
                MainSingleton.getInstance().guiManager.trayIconManager.updateTray();
            }
            case "effect" -> {
                Enums.Effect effect = LocalizedEnum.fromBaseStr(Enums.Effect.class, text);
                if (effect == null) effect = LocalizedEnum.fromStr(Enums.Effect.class, text);
                if (effect == null) throw new IllegalArgumentException("Invalid effect");
                config.setToggleLed(true);
                NetworkManager.setEffect(effect.getBaseI18n());
            }
            case "colorMode" -> {
                int mode = Integer.parseInt(text);
                if (mode < 1 || mode > Enums.ColorMode.values().length)
                    throw new IllegalArgumentException("Invalid color mode");
                config.setColorMode(mode);
                if (config.isFullFirmware() && CommonUtility.getDeviceToUse() != null) {
                    FirmwareConfigDto dto = new FirmwareConfigDto();
                    dto.setColorMode(text);
                    dto.setMAC(CommonUtility.getDeviceToUse().getMac());
                    NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_GLOW_WORM_FIRM_CONFIG), CommonUtility.toJsonString(dto));
                }
                if (MainSingleton.getInstance().RUNNING) {
                    PipelineManager.restartCapture(CommonUtility::run);
                } else {
                    CommonUtility.turnOnLEDs();
                }
            }
            case "gamma" -> {
                if (!gammaValues().contains(text)) throw new IllegalArgumentException("Invalid gamma");
                config.setGamma(Double.parseDouble(text));
            }
            case "brightness" -> {
                double percent = Double.parseDouble(text);
                if (percent < 0 || percent > 100) throw new IllegalArgumentException("Invalid brightness");
                config.setBrightness(storedBrightness(percent));
                publishLedState(config, true);
            }
            case "whiteTemperature" -> {
                double kelvin = Double.parseDouble(text);
                if (kelvin < 2000 || kelvin > 11000) throw new IllegalArgumentException("Invalid color temperature");
                config.setWhiteTemperature(storedWhiteTemperature(kelvin));
                publishLedState(config, false);
            }
            case "desiredFramerate" -> {
                config.setDesiredFramerate(storedFramerate(text));
                PipelineManager.restartCapture(CommonUtility::run);
            }
            case "smoothingType" -> {
                SmoothingOptions.applyPreset(config, text);
                PipelineManager.restartCapture(CommonUtility::run);
            }
            case "audioDevice" -> {
                config.setAudioDevice(text);
                PipelineManager.restartCapture(CommonUtility::run);
            }
            case "audioLoopbackGain" -> {
                float gain = Float.parseFloat(text);
                if (gain < -5 || gain > 5) throw new IllegalArgumentException("Invalid audio gain");
                config.setAudioLoopbackGain(gain);
            }
            case "nightModeFrom" -> config.setNightModeFrom(java.time.LocalTime.parse(text).toString());
            case "nightModeTo" -> config.setNightModeTo(java.time.LocalTime.parse(text).toString());
            case "nightModeBrightness" -> {
                if (!text.matches("(?:[0-9]|[1-8][0-9]|90)%"))
                    throw new IllegalArgumentException("Invalid night brightness");
                config.setNightModeBrightness(text);
            }
            case "audioChannels" -> config.setAudioChannels(text);
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Sends the current brightness and color temperature to the connected LED device.
     *
     * @param config           running configuration
     * @param brightnessChange whether the brightness slider changed
     */
    private static void publishLedState(Configuration config, boolean brightnessChange) {
        if (!config.isToggleLed()) return;
        if (!config.isFullFirmware()) {
            CommonUtility.turnOnLEDs();
            return;
        }
        StateDto dto = new StateDto();
        dto.setState(Constants.ON);
        dto.setEffect(MainSingleton.getInstance().RUNNING
                ? (config.isWirelessStream() ? Constants.STATE_ON_GLOWWORMWIFI : Constants.STATE_ON_GLOWWORM)
                : config.getEffect());
        String[] channels = config.getColorChooser().split(",");
        ColorDto color = new ColorDto();
        boolean streamingBrightness = brightnessChange && MainSingleton.getInstance().RUNNING;
        color.setR(streamingBrightness ? 255 : Integer.parseInt(channels[0]));
        color.setG(streamingBrightness ? 255 : Integer.parseInt(channels[1]));
        color.setB(streamingBrightness ? 255 : Integer.parseInt(channels[2]));
        dto.setColor(color);
        dto.setBrightness(CommonUtility.getNightBrightness());
        dto.setWhitetemp(config.getWhiteTemperature());
        if (CommonUtility.getDeviceToUse() != null) dto.setMAC(CommonUtility.getDeviceToUse().getMac());
        NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT), CommonUtility.toJsonString(dto));
    }

    /**
     * A stored select value and its localized label.
     */
    public record Choice(String value, String label) {
    }
}
