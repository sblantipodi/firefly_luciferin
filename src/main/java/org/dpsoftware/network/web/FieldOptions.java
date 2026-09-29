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
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.gui.LabelKey;
import org.dpsoftware.gui.controllers.options.ImprovOptions;
import org.dpsoftware.gui.controllers.options.MiscTabOptions;
import org.dpsoftware.gui.controllers.options.ModeTabOptions;
import org.dpsoftware.lut.CubeLutToneMap;
import org.dpsoftware.managers.DisplayManager;
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
        List<Option> aspectRatios = new ArrayList<>(localized(Enums.AspectRatio.class).options());
        aspectRatios.add(new Option("AUTO", CommonUtility.getWord(LabelKey.AUTO_DETECT_BLACK_BARS)));
        options.put(WebFieldNames.DEFAULT_LED_MATRIX, new FieldOptions(aspectRatios, "string"));
        options.put(WebFieldNames.OS_SCALING, new FieldOptions(ModeTabOptions.scalingRatios().stream()
                .map(value -> new Option(value.replace("%", ""), value)).toList(), "number"));
        options.put(WebFieldNames.MONITOR_NUMBER, monitorOptions());
        options.put(WebFieldNames.CAPTURE_METHOD, captureMethods(false));
        options.put("captureMethodExternal", captureMethods(true));
        options.put(WebFieldNames.BAUD_RATE, new FieldOptions(Arrays.stream(Enums.BaudRate.values())
                .map(b -> new FieldOptions.Option(b.getBaudRate(), b.getBaudRate())).toList(), "string"));
        options.put(WebFieldNames.DESIRED_FRAMERATE, new FieldOptions(MiscTabOptions.captureFramerates().stream()
                .map(choice -> new Option(choice.label(), choice.label())).toList(), "string"));
        options.put(WebFieldNames.SIMD_AVX, new FieldOptions(Arrays.stream(Enums.SimdAvxOption.values())
                .filter(s -> ModeTabOptions.simdOptions(MainSingleton.getInstance().getSupportedSpeciesLengthSimd()).contains(s))
                .map(s -> new FieldOptions.Option(String.valueOf(s.getSimdOptionNumeric()), s.getI18n())).toList(), "number"));
        options.put(WebFieldNames.RESAMPLING_FACTOR, new FieldOptions(Arrays.stream(Enums.ResamplingFactor.values())
                .map(r -> new FieldOptions.Option(String.valueOf(r.getResamplingFactorValue()), r.getI18n())).toList(), "number"));
        options.put(WebFieldNames.ALGO, localized(Enums.Algo.class));
        options.put(WebFieldNames.THEME, localized(Enums.Theme.class));
        options.put(WebFieldNames.LANGUAGE, new FieldOptions(Arrays.stream(Enums.Language.values())
                .map(language -> new Option(language.getI18n(), language.getI18n())).toList(), "string"));
        options.put(WebFieldNames.SMOOTHING_TYPE, localized(Enums.Smoothing.class));
        options.put(WebFieldNames.EMA_ALPHA, new FieldOptions(Arrays.stream(Enums.Ema.values())
                .map(ema -> new Option(String.valueOf(ema.getEmaAlpha()), ema.getI18n())).toList(), "number"));
        options.put(WebFieldNames.FRAME_INSERTION_TARGET, new FieldOptions(Arrays.stream(Enums.FrameGeneration.values())
                .map(frame -> new Option(String.valueOf(frame.getFrameGenerationTarget()), frame.getI18n())).toList(), "number"));
        options.put(WebFieldNames.SMOOTHING_TARGET_FRAMERATE, new FieldOptions(Arrays.stream(Enums.SmoothingTarget.values())
                .map(target -> new Option(String.valueOf(target.getSmoothingTargetValue()), target.getSmoothingTarget())).toList(), "number"));
        options.put(WebFieldNames.STREAM_TYPE, new FieldOptions(Arrays.stream(Enums.StreamType.values())
                .map(s -> new FieldOptions.Option(s.getStreamType(), s.getStreamType())).toList(), "string"));
        // TODO check here don't remove this todo
        options.put("improvSsid", new FieldOptions(ImprovOptions.wifiSsids().stream()
                .map(ssid -> new Option(ssid, ssid)).toList(), "string"));
        options.put("improvComPort", new FieldOptions(ImprovOptions.serialPorts().stream()
                .map(port -> new Option(port, port)).toList(), "string"));
        options.put("improvBaudrate", new FieldOptions(ImprovOptions.baudRates().stream()
                .map(baud -> new Option(baud, baud)).toList(), "string"));
        options.put("improvEthernetMode", new FieldOptions(Arrays.stream(Enums.EthernetOptions.values())
                .map(mode -> new Option(mode.name(), mode.getI18n())).toList(), "string"));
        options.put("improvEthernetBoard", new FieldOptions(Arrays.stream(Enums.EthernetBoards.values())
                .map(board -> new Option(board.getValue(), board.getValue())).toList(), "string"));
        options.put(WebFieldNames.EFFECT, effectOptions());
        options.put("miscAudioEffects", new FieldOptions(Arrays.stream(Enums.Effect.values())
                .filter(effect -> MiscTabOptions.isAudioEffect(effect.getBaseI18n()))
                .map(effect -> new Option(effect.getBaseI18n(), effect.getI18n())).toList(), "string"));
        options.put(WebFieldNames.COLOR_MODE, new FieldOptions(Arrays.stream(Enums.ColorMode.values())
                .map(c -> new FieldOptions.Option(String.valueOf(c.ordinal() + 1), c.getI18n())).toList(), "number"));
        options.put(WebFieldNames.GAMMA, new FieldOptions(MiscTabOptions.gammaValues().stream()
                .map(value -> new Option(value, value)).toList(), "number"));
        options.put("audioChannels", localized(Enums.AudioChannels.class));
        options.put("audioDevice", new FieldOptions(MiscTabOptions.audioDeviceNames().stream()
                .map(name -> {
                    Enums.Audio known = LocalizedEnum.fromStr(Enums.Audio.class, name);
                    return new Option(known == null ? name : known.getBaseI18n(), name);
                }).toList(), "string"));
        options.put(WebFieldNames.GAMMA_LEVEL, localized(Enums.GammaLevel.class));
        options.put(WebFieldNames.NIGHT_LIGHT, localized(Enums.NightLight.class));
        options.put(WebFieldNames.BRIGHTNESS_LIMITER, new FieldOptions(Arrays.stream(Enums.BrightnessLimiter.values())
                .map(b -> new FieldOptions.Option(String.valueOf(b.getBrightnessLimitFloat()), b.getBaseI18n())).toList(), "number"));
        options.put(WebFieldNames.POWER_SAVING, localized(Enums.PowerSaving.class));
        options.put(WebFieldNames.MULTI_MONITOR, new FieldOptions(List.of(
                new FieldOptions.Option("1", CommonUtility.getWord("multimonitor.disabled")),
                new FieldOptions.Option("2", CommonUtility.getWord("multimonitor.dual")),
                new FieldOptions.Option("3", CommonUtility.getWord("multimonitor.triple"))), "number"));
        // 3D LUT (color tone map) options, the available .cube LUTs (classpath + config dir) with "Disabled" pinned at the top
        options.put(WebFieldNames.CUBE_LUT, new FieldOptions(CubeLutToneMap.listAvailableLuts().stream()
                .map(name -> new FieldOptions.Option(name, name)).toList(), "string"));
        return options;
    }

    /**
     * Converts the shared capture method choices into web select options.
     *
     * @param external whether the selected source is an external video device
     * @return capture method options for the web form
     */
    private static FieldOptions captureMethods(boolean external) {
        return new FieldOptions(ModeTabOptions.captureMethods(external).stream()
                .map(method -> new Option(method.name(), method.getCaptureMethod())).toList(), "string");
    }

    /**
     * Builds web monitor choices from the shared display and device lists.
     *
     * @return monitor options with display indices or external device names as values
     */
    private static FieldOptions monitorOptions() {
        List<Option> monitors = new ArrayList<>();
        DisplayManager displayManager = new DisplayManager();
        try {
            List<String> names = ModeTabOptions.displayNames(displayManager);
            for (int i = 0; i < names.size(); i++) {
                monitors.add(new Option(String.valueOf(i), names.get(i)));
            }
        } catch (RuntimeException | LinkageError ignored) {
            // The JavaFX display list can be unavailable during headless startup.
        }
        try {
            ModeTabOptions.externalCaptureDeviceNames().forEach(name ->
                    monitors.add(new Option("device:" + name, name)));
        } catch (RuntimeException | LinkageError ignored) {
            // Native capture discovery can be unavailable during headless startup.
        }
        return new FieldOptions(monitors, "string");
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
     * @return the stored effect value and its localized label
     */
    private static FieldOptions.Option effectOption(String i18nKey) {
        Enums.Effect effect = Arrays.stream(Enums.Effect.values())
                .filter(candidate -> candidate.getValue().equals(i18nKey)).findFirst().orElseThrow();
        return new FieldOptions.Option(effect.getBaseI18n(), effect.getI18n());
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
                .map(e -> new FieldOptions.Option(e.getBaseI18n(), e.getI18n()))
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
        for (String key : List.of("saveSettings", "showPreview", "hidePreview", "newProfileName",
                "addProfile", "profileHelp", "openLog", "restartConfirm", "settingsSaved",
                "wholeNumber", "validGroup", "profilePrefix", "collectError")) {
            labels.put("web." + key, CommonUtility.getWord("web." + key));
        }
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
        labels.put(WebFieldNames.SMOOTHING_TYPE, CommonUtility.getWord("web.misc.smoothing"));
        labels.put(WebFieldNames.SMOOTHING_TARGET_FRAMERATE, CommonUtility.getWord("fxml.dialog.smoothing.target.framerate"));
        labels.put(WebFieldNames.FRAME_INSERTION_TARGET, CommonUtility.getWord("fxml.dialog.smoothing.fg"));
        labels.put(WebFieldNames.EMA_ALPHA, CommonUtility.getWord("fxml.dialog.smoothing.ema"));
        labels.put("smoothingCaptureFramerate", CommonUtility.getWord("fxml.dialog.smoothing.capture.framerate"));
        labels.put(WebFieldNames.SIMD_AVX, CommonUtility.getWord("fxml.modetab.simdavx"));
        labels.put(WebFieldNames.RESAMPLING_FACTOR, CommonUtility.getWord("fxml.modetab.scaling"));
        labels.put(WebFieldNames.CAPTURE_METHOD, CommonUtility.getWord("fxml.modetab.capturemethod"));
        labels.put(WebFieldNames.MONITOR_NUMBER, CommonUtility.getWord("fxml.modetab.binddisplay"));
        labels.put(WebFieldNames.SCREEN_RES_X, CommonUtility.getWord("web.mode.screenWidth"));
        labels.put(WebFieldNames.SCREEN_RES_Y, CommonUtility.getWord("web.mode.screenHeight"));
        labels.put(WebFieldNames.OS_SCALING, CommonUtility.getWord("web.mode.scaling"));
        labels.put(WebFieldNames.DEFAULT_LED_MATRIX, CommonUtility.getWord("fxml.modetab.aspectratio"));
        labels.put(WebFieldNames.AUTO_DETECT_BLACK_BARS, CommonUtility.getWord("fxml.modetab.autodetect"));
        labels.put(WebFieldNames.ALGO, CommonUtility.getWord("web.mode.algo"));
        labels.put(WebFieldNames.LANGUAGE, CommonUtility.getWord("web.mode.language"));
        labels.put(WebFieldNames.WEB_MCP_SERVER_ENABLED, CommonUtility.getWord("web.mode.webMcpServer"));
        labels.put(WebFieldNames.MQTT_ENABLE, CommonUtility.getWord("fxml.mqtttab.enablemqtt"));
        labels.put(WebFieldNames.WIRELESS_STREAM, CommonUtility.getWord("fxml.mqtttab.wirelessstream"));
        labels.put(WebFieldNames.STREAM_TYPE, CommonUtility.getWord("fxml.mqtttab.streamtype"));
        labels.put(WebFieldNames.MQTT_SERVER, CommonUtility.getWord("fxml.mqtttab.mqttserverhost"));
        // TODO check here don't remove this todo
        labels.put("mqttHost", CommonUtility.getWord("fxml.mqtttab.mqttserverhost"));
        labels.put("mqttPort", CommonUtility.getWord("fxml.mqtttab.mqttport"));
        labels.put(WebFieldNames.MQTT_TOPIC, CommonUtility.getWord("fxml.mqtttab.mqttbasetopic"));
        labels.put(WebFieldNames.MQTT_DISCOVERY_TOPIC, CommonUtility.getWord("fxml.mqtttab.mqttdiscoverytopic"));
        labels.put(WebFieldNames.MQTT_USERNAME, CommonUtility.getWord("fxml.mqtttab.mqttusername"));
        labels.put("mqttUser", CommonUtility.getWord("fxml.mqtttab.mqttusername"));
        labels.put(WebFieldNames.MQTT_PWD, CommonUtility.getWord("fxml.mqtttab.mqttpwd"));
        labels.put("mqttDiscoveryAdd", CommonUtility.getWord("tooltip.mqttdiscoverytopic.add"));
        labels.put("mqttDiscoveryRemove", CommonUtility.getWord("tooltip.mqttdiscoverytopic.remove"));
        labels.put("mqttDiscoveryActions", CommonUtility.getWord("web.addToSmartSystems"));
        labels.put("improvContext", CommonUtility.getWord("fxml.mqtttab.improv.context"));
        labels.put("improvSsid", CommonUtility.getWord("fxml.improv.wifi"));
        labels.put("improvWifiPwd", CommonUtility.getWord("fxml.improv.pwd"));
        labels.put("improvDeviceName", CommonUtility.getWord("fxml.devicestab.device.name"));
        labels.put("improvEthernetMode", CommonUtility.getWord("web.provisioning.ethernet"));
        labels.put("improvEthernetBoard", CommonUtility.getWord("fxml.mqtttab.improv.prebuilt.eth"));
        labels.put("improvMi", "MI");
        labels.put("improvMo", "MO");
        labels.put("improvSck", "SCK");
        labels.put("improvCs", "CS");
        labels.put("improvComPort", CommonUtility.getWord("fxml.improv.comport"));
        labels.put("improvBaudrate", CommonUtility.getWord("fxml.improv.baudrate"));
        labels.put("improvAction", CommonUtility.getWord("fxml.mqtttab.provision.device"));
        labels.put(WebFieldNames.EFFECT, CommonUtility.getWord("web.misc.effect"));
        labels.put("audioDevice", CommonUtility.getWord("context.menu.audio.device"));
        labels.put("audioChannels", CommonUtility.getWord("web.misc.audioChannels"));
        labels.put("audioLoopbackGain", CommonUtility.getWord("context.menu.audio.gain"));
        labels.put("profilesControl", CommonUtility.getWord("fxml.misctab.profiles"));
        labels.put(WebFieldNames.COLOR_MODE, CommonUtility.getWord("fxml.devicestab.colormode"));
        labels.put(WebFieldNames.GAMMA, CommonUtility.getWord("fxml.misctab.gamma"));
        labels.put(WebFieldNames.WHITE_TEMPERATURE, CommonUtility.getWord("fxml.misctab.whitetemp"));
        labels.put(WebFieldNames.BRIGHTNESS, CommonUtility.getWord("fxml.misctab.brightness"));
        labels.put(WebFieldNames.NIGHT_MODE_FROM, CommonUtility.getWord("web.misc.nightFrom"));
        labels.put(WebFieldNames.NIGHT_MODE_TO, CommonUtility.getWord("web.misc.nightTo"));
        labels.put(WebFieldNames.NIGHT_MODE_BRIGHTNESS, CommonUtility.getWord("web.misc.nightBrightness"));
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
     * Adds the localized LED button captions for both toggle states.
     *
     * @param labels the labels map to update in place
     */
    public static void applyToggleLedLabels(Map<String, String> labels) {
        labels.put(WebFieldNames.TURN_LED_ON, CommonUtility.getWord(LabelKey.TURN_LED_ON));
        labels.put(WebFieldNames.TURN_LED_OFF, CommonUtility.getWord(LabelKey.TURN_LED_OFF));
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
        titles.put("provisioning", CommonUtility.getWord("fxml.mqtttab.provision.device"));
        titles.put(WebFieldNames.SECTION_MISC, CommonUtility.getWord("fxml.setting.misc"));
        titles.put("profiles", CommonUtility.getWord("fxml.misctab.profiles"));
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
