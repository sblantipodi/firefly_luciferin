/*
  FieldOptions.java

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
package org.dpsoftware.network.web;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.lut.CubeLutToneMap;
import org.dpsoftware.utilities.CommonUtility;

import java.util.*;

/**
 * Selectable values for a configuration field.
 *
 * @param options the selectable values for the field
 * @param type    the value type, so the client can cast it correctly (e.g. "string", "number")
 */
public record FieldOptions(List<Option> options, String type) {

    /**
     * Builds enum backed field options.
     *
     * @return map of configuration field name to its possible values
     */
    public static Map<String, FieldOptions> getFieldOptions() {
        Map<String, FieldOptions> options = new LinkedHashMap<>();
        options.put(WebFieldNames.ORIENTATION, localized(Enums.Orientation.class));
        options.put(WebFieldNames.DEFAULT_LED_MATRIX, localized(Enums.AspectRatio.class));
        options.put(WebFieldNames.BAUD_RATE, new FieldOptions(Arrays.stream(Enums.BaudRate.values())
                .map(b -> new FieldOptions.Option(b.getBaudRate(), b.getBaudRate())).toList(), "string"));
        options.put(WebFieldNames.DESIRED_FRAMERATE, new FieldOptions(Arrays.stream(Enums.Framerate.values())
                .map(f -> new FieldOptions.Option(f.getBaseI18n(), f.getBaseI18n())).toList(), "string"));
        options.put(WebFieldNames.SIMD_AVX, new FieldOptions(Arrays.stream(Enums.SimdAvxOption.values())
                .map(s -> new FieldOptions.Option(String.valueOf(s.getSimdOptionNumeric()), s.getBaseI18n())).toList(), "number"));
        options.put(WebFieldNames.RESAMPLING_FACTOR, new FieldOptions(Arrays.stream(Enums.ResamplingFactor.values())
                .map(r -> new FieldOptions.Option(String.valueOf(r.getResamplingFactorValue()), r.getBaseI18n())).toList(), "number"));
        options.put(WebFieldNames.ALGO, localized(Enums.Algo.class));
        options.put(WebFieldNames.THEME, localized(Enums.Theme.class));
        options.put(WebFieldNames.LANGUAGE, localized(Enums.Language.class));
        options.put(WebFieldNames.SMOOTHING_TYPE, localized(Enums.Smoothing.class));
        options.put(WebFieldNames.STREAM_TYPE, new FieldOptions(Arrays.stream(Enums.StreamType.values())
                .map(s -> new FieldOptions.Option(s.getStreamType(), s.getStreamType())).toList(), "string"));
        options.put(WebFieldNames.EFFECT, effectOptions());
        options.put(WebFieldNames.COLOR_MODE, new FieldOptions(Arrays.stream(Enums.ColorMode.values())
                .map(c -> new FieldOptions.Option(String.valueOf(c.ordinal() + 1), c.getBaseI18n())).toList(), "number"));
        options.put(WebFieldNames.GAMMA_LEVEL, localized(Enums.GammaLevel.class));
        options.put(WebFieldNames.NIGHT_LIGHT, localized(Enums.NightLight.class));
        options.put(WebFieldNames.BRIGHTNESS_LIMITER, new FieldOptions(Arrays.stream(Enums.BrightnessLimiter.values())
                .map(b -> new FieldOptions.Option(String.valueOf(b.getBrightnessLimitFloat()), b.getBaseI18n())).toList(), "number"));
        options.put(WebFieldNames.POWER_SAVING, localized(Enums.PowerSaving.class));
        options.put(WebFieldNames.MULTI_MONITOR, new FieldOptions(List.of(
                new FieldOptions.Option("1", "Disabled"),
                new FieldOptions.Option("2", "Dual display"),
                new FieldOptions.Option("3", "Triple display")), "number"));
        // 3D LUT (color tone map) options, the available .cube LUTs (classpath + config dir) with "Disabled" pinned at the top
        options.put(WebFieldNames.CUBE_LUT, new FieldOptions(CubeLutToneMap.listAvailableLuts().stream()
                .map(name -> new FieldOptions.Option(name, name)).toList(), "string"));
        return options;
    }

    /**
     * Returns effects with Solid and Bias light first.
     *
     * @return the field options for the effect field
     */
    private static FieldOptions effectOptions() {
        Set<String> pinned = Set.of(Enums.Effect.SOLID.getValue(), Enums.Effect.BIAS_LIGHT.getValue());
        List<Enums.Effect> rest = Arrays.stream(Enums.Effect.values())
                .filter(e -> !pinned.contains(e.getValue()))
                .sorted(Comparator.comparing(LocalizedEnum::getI18n))
                .toList();
        List<FieldOptions.Option> opts = new ArrayList<>();
        pinned.stream()
                .sorted(Comparator.comparing(CommonUtility::getWord))
                .forEach(key -> opts.add(effectOption(key)));
        rest.forEach(e -> opts.add(effectOption(e.getValue())));
        return new FieldOptions(opts, "string");
    }

    /**
     * Build a single effect option from a localized i18n key.
     *
     * @param i18nKey the i18n key of the effect
     * @return the option with value and label both set to the localized word
     */
    private static FieldOptions.Option effectOption(String i18nKey) {
        return new FieldOptions.Option(CommonUtility.getWord(i18nKey), CommonUtility.getWord(i18nKey));
    }

    /**
     * Builds options from a localized enum.
     *
     * @param enumClass the localized enum class
     * @param <E>       the localized enum type
     * @return the field options
     */
    private static <E extends Enum<E> & LocalizedEnum> FieldOptions localized(Class<E> enumClass) {
        List<FieldOptions.Option> opts = Arrays.stream(enumClass.getEnumConstants())
                .map(e -> new FieldOptions.Option(e.getBaseI18n(), e.getBaseI18n()))
                .toList();
        return new FieldOptions(opts, "string");
    }

    /**
     * Build the map of localized labels for every configuration field, using the current application locale.
     *
     * @return map of configuration field name to its localized label
     */
    public static Map<String, String> getFieldLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(WebFieldNames.TOP_LED, CommonUtility.getWord("fxml.ledsconfigtab.toprow"));
        labels.put(WebFieldNames.LEFT_LED, CommonUtility.getWord("fxml.ledsconfigtab.leftcol"));
        labels.put(WebFieldNames.RIGHT_LED, CommonUtility.getWord("fxml.ledsconfigtab.rightcol"));
        labels.put(WebFieldNames.BOTTOM_LEFT_LED, CommonUtility.getWord("fxml.ledsconfigtab.bottomleft"));
        labels.put(WebFieldNames.BOTTOM_RIGHT_LED, CommonUtility.getWord("fxml.ledsconfigtab.bottomright"));
        labels.put(WebFieldNames.BOTTOM_ROW_LED, CommonUtility.getWord("fxml.ledsconfigtab.bottomrow"));
        labels.put(WebFieldNames.LED_START_OFFSET, CommonUtility.getWord("fxml.ledsconfigtab.firstled"));
        labels.put(WebFieldNames.ORIENTATION, CommonUtility.getWord("fxml.ledsconfigtab.orientation"));
        labels.put(WebFieldNames.GROUP_BY, CommonUtility.getWord("fxml.ledsconfigtab.group.by"));
        labels.put(WebFieldNames.SPLIT_BOTTOM_MARGIN, CommonUtility.getWord("fxml.ledsconfigtab.splitbottomrow"));
        labels.put(WebFieldNames.GRABBER_AREA_TOP_BOTTOM, CommonUtility.getWord("fxml.ledsconfigtab.grabberArea"));
        labels.put(WebFieldNames.GRABBER_SIDE, CommonUtility.getWord("fxml.ledsconfigtab.grabberArea"));
        labels.put(WebFieldNames.GAP_TYPE_TOP_BOTTOM, CommonUtility.getWord("fxml.ledsconfigtab.gapType"));
        labels.put(WebFieldNames.GAP_TYPE_SIDE, CommonUtility.getWord("fxml.ledsconfigtab.gapType"));
        labels.put(WebFieldNames.OUTPUT_DEVICE, CommonUtility.getWord("fxml.modetab.outputdevice"));
        labels.put(WebFieldNames.BAUD_RATE, CommonUtility.getWord("fxml.modetab.baudrate"));
        labels.put(WebFieldNames.STATIC_GLOW_WORM_IP, CommonUtility.getWord("fxml.modetab.serialport"));
        labels.put(WebFieldNames.DESIRED_FRAMERATE, CommonUtility.getWord("fxml.misctab.captureframerate"));
        labels.put(WebFieldNames.SMOOTHING_TYPE, CommonUtility.getWord("fxml.dialog.smoothing.type"));
        labels.put(WebFieldNames.SMOOTHING_TARGET_FRAMERATE, CommonUtility.getWord("fxml.dialog.smoothing.target.framerate"));
        labels.put(WebFieldNames.FRAME_INSERTION_TARGET, CommonUtility.getWord("fxml.dialog.smoothing.frameinsertion"));
        labels.put(WebFieldNames.EMA_ALPHA, CommonUtility.getWord("fxml.dialog.smoothing.emaalpha"));
        labels.put(WebFieldNames.SIMD_AVX, CommonUtility.getWord("fxml.modetab.simdavx"));
        labels.put(WebFieldNames.RESAMPLING_FACTOR, CommonUtility.getWord("fxml.modetab.scaling"));
        labels.put(WebFieldNames.CAPTURE_METHOD, CommonUtility.getWord("fxml.modetab.capturemethod"));
        labels.put(WebFieldNames.MONITOR_NUMBER, CommonUtility.getWord("fxml.modetab.binddisplay"));
        labels.put(WebFieldNames.SCREEN_RES_X, CommonUtility.getWord("fxml.modetab.screenresolution"));
        labels.put(WebFieldNames.SCREEN_RES_Y, CommonUtility.getWord("fxml.modetab.screenresolution"));
        labels.put(WebFieldNames.OS_SCALING, CommonUtility.getWord("fxml.modetab.os.scaling"));
        labels.put(WebFieldNames.DEFAULT_LED_MATRIX, CommonUtility.getWord("fxml.modetab.aspectratio"));
        labels.put(WebFieldNames.AUTO_DETECT_BLACK_BARS, CommonUtility.getWord("fxml.modetab.autodetect"));
        labels.put(WebFieldNames.ALGO, CommonUtility.getWord("fxml.modetab.algo"));
        labels.put(WebFieldNames.LANGUAGE, CommonUtility.getWord("fxml.misctab.language"));
        labels.put(WebFieldNames.MQTT_ENABLE, CommonUtility.getWord("fxml.mqtttab.enablemqtt"));
        labels.put(WebFieldNames.WIRELESS_STREAM, CommonUtility.getWord("fxml.mqtttab.wirelessstream"));
        labels.put(WebFieldNames.STREAM_TYPE, CommonUtility.getWord("fxml.mqtttab.streamtype"));
        labels.put(WebFieldNames.MQTT_SERVER, CommonUtility.getWord("fxml.mqtttab.mqttserverhost"));
        labels.put(WebFieldNames.MQTT_TOPIC, CommonUtility.getWord("fxml.mqtttab.mqttbasetopic"));
        labels.put(WebFieldNames.MQTT_DISCOVERY_TOPIC, CommonUtility.getWord("fxml.mqtttab.mqttdiscoverytopic"));
        labels.put(WebFieldNames.MQTT_USERNAME, CommonUtility.getWord("fxml.mqtttab.mqttusername"));
        labels.put(WebFieldNames.MQTT_PWD, CommonUtility.getWord("fxml.mqtttab.mqttpwd"));
        labels.put(WebFieldNames.EFFECT, CommonUtility.getWord("fxml.misctab.effect"));
        labels.put(WebFieldNames.COLOR_MODE, CommonUtility.getWord("fxml.devicestab.colormode"));
        labels.put(WebFieldNames.GAMMA, CommonUtility.getWord("fxml.misctab.gamma"));
        labels.put(WebFieldNames.WHITE_TEMPERATURE, CommonUtility.getWord("fxml.misctab.whitetemp"));
        labels.put(WebFieldNames.BRIGHTNESS, CommonUtility.getWord("fxml.misctab.brightness"));
        labels.put(WebFieldNames.NIGHT_MODE_FROM, CommonUtility.getWord("fxml.misctab.nightmode.from"));
        labels.put(WebFieldNames.NIGHT_MODE_TO, CommonUtility.getWord("fxml.misctab.nightmode.to"));
        labels.put(WebFieldNames.NIGHT_MODE_BRIGHTNESS, CommonUtility.getWord("fxml.misctab.nightmode.brightness"));
        labels.put(WebFieldNames.TOGGLE_LED, CommonUtility.getWord("fxml.misctab.ledcontrol"));
        labels.put(WebFieldNames.START_WITH_SYSTEM, CommonUtility.getWord("fxml.misctab.runlogin"));
        labels.put(WebFieldNames.RUNTIME_LOG_LEVEL, CommonUtility.getWord("fxml.misctab.runtimelog"));
        labels.put(WebFieldNames.CUBE_LUT, CommonUtility.getWord("fxml.misctab.cubeLut"));
        labels.put(WebFieldNames.NIGHT_LIGHT, CommonUtility.getWord("fxml.eyecare.night.light"));
        labels.put(WebFieldNames.NIGHT_LIGHT_LVL, CommonUtility.getWord("fxml.eyecare.nightlight.level"));
        labels.put(WebFieldNames.LUMINOSITY_THRESHOLD, CommonUtility.getWord("fxml.eyecare.luminosity.threshold"));
        labels.put(WebFieldNames.BRIGHTNESS_LIMITER, CommonUtility.getWord("fxml.eyecare.brightness.limiter"));
        labels.put(WebFieldNames.ENABLE_AUTOMATIC_GAMMA, CommonUtility.getWord("fxml.gamma.enable.automatic"));
        labels.put(WebFieldNames.GAMMA_LEVEL, CommonUtility.getWord("fxml.gamma.level"));
        labels.put(WebFieldNames.CHECK_FULL_SCREEN, CommonUtility.getWord("fxml.profile.fullscreen.cb"));
        labels.put(WebFieldNames.GPU_THRESHOLD, CommonUtility.getWord("fxml.profile.gpu"));
        labels.put(WebFieldNames.CPU_THRESHOLD, CommonUtility.getWord("fxml.profile.cpu"));
        labels.put(WebFieldNames.PROFILE_PROCESS_1, CommonUtility.getWord("fxml.profile.process1"));
        labels.put(WebFieldNames.PROFILE_PROCESS_2, CommonUtility.getWord("fxml.profile.process2"));
        labels.put(WebFieldNames.PROFILE_PROCESS_3, CommonUtility.getWord("fxml.profile.process3"));
        labels.put(WebFieldNames.POWER_SAVING, CommonUtility.getWord("fxml.devicestab.power.saving"));
        labels.put(WebFieldNames.MULTI_MONITOR, CommonUtility.getWord("fxml.devicestab.multi.monitor"));
        labels.put(WebFieldNames.MULTI_SCREEN_SINGLE_DEVICE, CommonUtility.getWord("fxml.devicestab.single.device"));
        labels.put(WebFieldNames.CHECK_FOR_UPDATES, CommonUtility.getWord("fxml.devicestab.check.updates"));
        labels.put(WebFieldNames.SYNC_CHECK, CommonUtility.getWord("fxml.devicestab.sync.check"));
        labels.put(WebFieldNames.ENABLE_LDR, CommonUtility.getWord("fxml.eyecare.enableldr"));
        labels.put(WebFieldNames.LDR_INTERVAL, CommonUtility.getWord("fxml.eyecare.ldr.interval"));
        labels.put(WebFieldNames.LDR_MIN, CommonUtility.getWord("fxml.eyecare.ldr.min.bright"));
        labels.put(WebFieldNames.LDR_TURN_OFF, CommonUtility.getWord("fxml.eyecare.ldr.turnoff"));
        return labels;
    }

    /**
     * Enrich the field labels with the current LED toggle state, so the client can show the
     * right "turn on/off" wording.
     *
     * @param labels the labels map to update in place
     */
    public static void applyToggleLedLabels(Map<String, String> labels) {
        labels.put(WebFieldNames.TOGGLE_LED, CommonUtility.getWord(
                MainSingleton.getInstance().config.isToggleLed()
                        ? Constants.TURN_LED_OFF : Constants.TURN_LED_ON));
        labels.put(WebFieldNames.TURN_LED_ON, CommonUtility.getWord(Constants.TURN_LED_ON));
        labels.put(WebFieldNames.TURN_LED_OFF, CommonUtility.getWord(Constants.TURN_LED_OFF));
    }

    /**
     * Build the map of localized titles for every section and sub-accordion of the settings page.
     *
     * @return map of section key to its localized title
     */
    public static Map<String, String> getSectionTitles() {
        Map<String, String> titles = new LinkedHashMap<>();
        titles.put(WebFieldNames.SECTION_LEDS, CommonUtility.getWord("fxml.setting.ledsconfig"));
        titles.put(WebFieldNames.SECTION_MODE, CommonUtility.getWord("fxml.setting.mode"));
        titles.put(WebFieldNames.SECTION_NETWORK, CommonUtility.getWord("fxml.setting.wifimqtt"));
        titles.put(WebFieldNames.SECTION_MISC, CommonUtility.getWord("fxml.setting.misc"));
        titles.put(WebFieldNames.SECTION_DEVICES, CommonUtility.getWord("fxml.setting.devices"));
        titles.put(WebFieldNames.SECTION_LDR, CommonUtility.getWord("fxml.setting.ldr"));
        titles.put(WebFieldNames.SECTION_DISPLAY, CommonUtility.getWord("fxml.ledsconfigtab.display"));
        titles.put(WebFieldNames.SECTION_COLOR_CORR, CommonUtility.getWord("fxml.misctab.colorcorrection"));
        titles.put(WebFieldNames.SECTION_EYE_CARE, CommonUtility.getWord("fxml.misctab.eyecare"));
        titles.put(WebFieldNames.SECTION_GAMMA, CommonUtility.getWord("fxml.misctab.gamma"));
        titles.put(WebFieldNames.SECTION_PROFILE, CommonUtility.getWord("fxml.misctab.profiles"));
        titles.put(WebFieldNames.SECTION_SMOOTHING, CommonUtility.getWord("fxml.dialog.smoothing.title"));
        titles.put(WebFieldNames.SECTION_CONNECTED_DEVICES, CommonUtility.getWord("fxml.devicestab.connected.devices"));
        titles.put(WebFieldNames.SECTION_SATELLITES, CommonUtility.getWord("fxml.devicestab.satellites"));
        return titles;
    }

    /**
     * A single selectable value.
     *
     * @param value the exact value to persist
     * @param label the human readable English text
     */
    public record Option(String value, String label) {
    }
}
