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
import org.dpsoftware.grabber.CubeLutToneMap;
import org.dpsoftware.utilities.CommonUtility;

import java.util.*;

/**
 * Possible values for a single configuration field, exposed to the web settings page.
 * The {@code value} is the exact value to persist (English i18n string for localized enums,
 * or the numeric value for numeric enums) and the {@code label} is the human readable English text.
 *
 * @param options the selectable values for the field
 * @param type    the value type, so the client can cast it correctly (e.g. "string", "number")
 */
public record FieldOptions(List<Option> options, String type) {

    /**
     * Build the map of possible values for every configuration field that is backed by an enum,
     * using the enums as the single source of truth (no value list is duplicated in the client).
     * Each entry exposes the value to persist and the English display label, plus the value type
     * so the client can cast it correctly.
     *
     * @return map of configuration field name to its possible values
     */
    public static Map<String, FieldOptions> getFieldOptions() {
        Map<String, FieldOptions> options = new LinkedHashMap<>();
        options.put("orientation", localized(Enums.Orientation.class));
        options.put("defaultLedMatrix", localized(Enums.AspectRatio.class));
        options.put("baudRate", new FieldOptions(Arrays.stream(Enums.BaudRate.values())
                .map(b -> new FieldOptions.Option(b.getBaudRate(), b.getBaudRate())).toList(), "string"));
        options.put("desiredFramerate", new FieldOptions(Arrays.stream(Enums.Framerate.values())
                .map(f -> new FieldOptions.Option(f.getBaseI18n(), f.getBaseI18n())).toList(), "string"));
        options.put("simdAvx", new FieldOptions(Arrays.stream(Enums.SimdAvxOption.values())
                .map(s -> new FieldOptions.Option(String.valueOf(s.getSimdOptionNumeric()), s.getBaseI18n())).toList(), "number"));
        options.put("resamplingFactor", new FieldOptions(Arrays.stream(Enums.ResamplingFactor.values())
                .map(r -> new FieldOptions.Option(String.valueOf(r.getResamplingFactorValue()), r.getBaseI18n())).toList(), "number"));
        options.put("algo", localized(Enums.Algo.class));
        options.put("theme", localized(Enums.Theme.class));
        options.put("language", localized(Enums.Language.class));
        options.put("smoothingType", localized(Enums.Smoothing.class));
        options.put("streamType", new FieldOptions(Arrays.stream(Enums.StreamType.values())
                .map(s -> new FieldOptions.Option(s.getStreamType(), s.getStreamType())).toList(), "string"));
        options.put("effect", effectOptions());
        options.put("colorMode", new FieldOptions(Arrays.stream(Enums.ColorMode.values())
                .map(c -> new FieldOptions.Option(String.valueOf(c.ordinal() + 1), c.getBaseI18n())).toList(), "number"));
        options.put("gammaLevel", localized(Enums.GammaLevel.class));
        options.put("nightLight", localized(Enums.NightLight.class));
        options.put("brightnessLimiter", new FieldOptions(Arrays.stream(Enums.BrightnessLimiter.values())
                .map(b -> new FieldOptions.Option(String.valueOf(b.getBrightnessLimitFloat()), b.getBaseI18n())).toList(), "number"));
        options.put("powerSaving", localized(Enums.PowerSaving.class));
        options.put("multiMonitor", new FieldOptions(List.of(
                new FieldOptions.Option("1", "Disabled"),
                new FieldOptions.Option("2", "Dual display"),
                new FieldOptions.Option("3", "Triple display")), "number"));
        // 3D LUT (color tone map) options, the available .cube LUTs (classpath + config dir) with
        // "Disabled" pinned at the top, the same list the JavaFX combo box is populated with.
        options.put("cubeLut", new FieldOptions(CubeLutToneMap.listAvailableLuts().stream()
                .map(name -> new FieldOptions.Option(name, name)).toList(), "string"));
        return options;
    }

    /**
     * Effect options with "Solid" and "Bias light" pinned at the top, the remaining effects sorted alphabetically.
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

    private static FieldOptions.Option effectOption(String i18nKey) {
        return new FieldOptions.Option(CommonUtility.getWord(i18nKey), CommonUtility.getWord(i18nKey));
    }

    /**
     * Build {@link FieldOptions} for a localized enum, using its base (English) i18n value to persist
     * and its English i18n text as label.
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
        labels.put("topLed", CommonUtility.getWord("fxml.ledsconfigtab.toprow"));
        labels.put("leftLed", CommonUtility.getWord("fxml.ledsconfigtab.leftcol"));
        labels.put("rightLed", CommonUtility.getWord("fxml.ledsconfigtab.rightcol"));
        labels.put("bottomLeftLed", CommonUtility.getWord("fxml.ledsconfigtab.bottomleft"));
        labels.put("bottomRightLed", CommonUtility.getWord("fxml.ledsconfigtab.bottomright"));
        labels.put("bottomRowLed", CommonUtility.getWord("fxml.ledsconfigtab.bottomrow"));
        labels.put("ledStartOffset", CommonUtility.getWord("fxml.ledsconfigtab.firstled"));
        labels.put("orientation", CommonUtility.getWord("fxml.ledsconfigtab.orientation"));
        labels.put("groupBy", CommonUtility.getWord("fxml.ledsconfigtab.group.by"));
        labels.put("splitBottomMargin", CommonUtility.getWord("fxml.ledsconfigtab.splitbottomrow"));
        labels.put("grabberAreaTopBottom", CommonUtility.getWord("fxml.ledsconfigtab.grabberArea"));
        labels.put("grabberSide", CommonUtility.getWord("fxml.ledsconfigtab.grabberArea"));
        labels.put("gapTypeTopBottom", CommonUtility.getWord("fxml.ledsconfigtab.gapType"));
        labels.put("gapTypeSide", CommonUtility.getWord("fxml.ledsconfigtab.gapType"));
        labels.put("outputDevice", CommonUtility.getWord("fxml.modetab.outputdevice"));
        labels.put("baudRate", CommonUtility.getWord("fxml.modetab.baudrate"));
        labels.put("staticGlowWormIp", CommonUtility.getWord("fxml.modetab.serialport"));
        labels.put("desiredFramerate", CommonUtility.getWord("fxml.misctab.captureframerate"));
        labels.put("smoothingType", CommonUtility.getWord("fxml.dialog.smoothing.type"));
        labels.put("smoothingTargetFramerate", CommonUtility.getWord("fxml.dialog.smoothing.target.framerate"));
        labels.put("frameInsertionTarget", CommonUtility.getWord("fxml.dialog.smoothing.frameinsertion"));
        labels.put("emaAlpha", CommonUtility.getWord("fxml.dialog.smoothing.emaalpha"));
        labels.put("simdAvx", CommonUtility.getWord("fxml.modetab.simdavx"));
        labels.put("resamplingFactor", CommonUtility.getWord("fxml.modetab.scaling"));
        labels.put("captureMethod", CommonUtility.getWord("fxml.modetab.capturemethod"));
        labels.put("monitorNumber", CommonUtility.getWord("fxml.modetab.binddisplay"));
        labels.put("screenResX", CommonUtility.getWord("fxml.modetab.screenresolution"));
        labels.put("screenResY", CommonUtility.getWord("fxml.modetab.screenresolution"));
        labels.put("osScaling", CommonUtility.getWord("fxml.modetab.os.scaling"));
        labels.put("defaultLedMatrix", CommonUtility.getWord("fxml.modetab.aspectratio"));
        labels.put("autoDetectBlackBars", CommonUtility.getWord("fxml.modetab.autodetect"));
        labels.put("algo", CommonUtility.getWord("fxml.modetab.algo"));
        labels.put("language", CommonUtility.getWord("fxml.misctab.language"));
        labels.put("mqttEnable", CommonUtility.getWord("fxml.mqtttab.enablemqtt"));
        labels.put("wirelessStream", CommonUtility.getWord("fxml.mqtttab.wirelessstream"));
        labels.put("streamType", CommonUtility.getWord("fxml.mqtttab.streamtype"));
        labels.put("mqttServer", CommonUtility.getWord("fxml.mqtttab.mqttserverhost"));
        labels.put("mqttTopic", CommonUtility.getWord("fxml.mqtttab.mqttbasetopic"));
        labels.put("mqttDiscoveryTopic", CommonUtility.getWord("fxml.mqtttab.mqttdiscoverytopic"));
        labels.put("mqttUsername", CommonUtility.getWord("fxml.mqtttab.mqttusername"));
        labels.put("mqttPwd", CommonUtility.getWord("fxml.mqtttab.mqttpwd"));
        labels.put("effect", CommonUtility.getWord("fxml.misctab.effect"));
        labels.put("colorMode", CommonUtility.getWord("fxml.devicestab.colormode"));
        labels.put("gamma", CommonUtility.getWord("fxml.misctab.gamma"));
        labels.put("whiteTemperature", CommonUtility.getWord("fxml.misctab.whitetemp"));
        labels.put("brightness", CommonUtility.getWord("fxml.misctab.brightness"));
        labels.put("nightModeFrom", CommonUtility.getWord("fxml.misctab.nightmode.from"));
        labels.put("nightModeTo", CommonUtility.getWord("fxml.misctab.nightmode.to"));
        labels.put("nightModeBrightness", CommonUtility.getWord("fxml.misctab.nightmode.brightness"));
        labels.put("toggleLed", CommonUtility.getWord("fxml.misctab.ledcontrol"));
        labels.put("startWithSystem", CommonUtility.getWord("fxml.misctab.runlogin"));
        labels.put("runtimeLogLevel", CommonUtility.getWord("fxml.misctab.runtimelog"));
        labels.put("cubeLut", CommonUtility.getWord("fxml.misctab.cubeLut"));
        labels.put("nightLight", CommonUtility.getWord("fxml.eyecare.night.light"));
        labels.put("nightLightLvl", CommonUtility.getWord("fxml.eyecare.nightlight.level"));
        labels.put("luminosityThreshold", CommonUtility.getWord("fxml.eyecare.luminosity.threshold"));
        labels.put("brightnessLimiter", CommonUtility.getWord("fxml.eyecare.brightness.limiter"));
        labels.put("enableAutomaticGamma", CommonUtility.getWord("fxml.gamma.enable.automatic"));
        labels.put("gammaLevel", CommonUtility.getWord("fxml.gamma.level"));
        labels.put("checkFullScreen", CommonUtility.getWord("fxml.profile.fullscreen.cb"));
        labels.put("gpuThreshold", CommonUtility.getWord("fxml.profile.gpu"));
        labels.put("cpuThreshold", CommonUtility.getWord("fxml.profile.cpu"));
        labels.put("profileProcess1", CommonUtility.getWord("fxml.profile.process1"));
        labels.put("profileProcess2", CommonUtility.getWord("fxml.profile.process2"));
        labels.put("profileProcess3", CommonUtility.getWord("fxml.profile.process3"));
        labels.put("powerSaving", CommonUtility.getWord("fxml.devicestab.power.saving"));
        labels.put("multiMonitor", CommonUtility.getWord("fxml.devicestab.multi.monitor"));
        labels.put("multiScreenSingleDevice", CommonUtility.getWord("fxml.devicestab.single.device"));
        labels.put("checkForUpdates", CommonUtility.getWord("fxml.devicestab.check.updates"));
        labels.put("syncCheck", CommonUtility.getWord("fxml.devicestab.sync.check"));
        labels.put("enableLDR", CommonUtility.getWord("fxml.eyecare.enableldr"));
        labels.put("ldrInterval", CommonUtility.getWord("fxml.eyecare.ldr.interval"));
        labels.put("ldrMin", CommonUtility.getWord("fxml.eyecare.ldr.min.bright"));
        labels.put("ldrTurnOff", CommonUtility.getWord("fxml.eyecare.ldr.turnoff"));
        return labels;
    }

    /**
     * Enrich the field labels with the current LED toggle state, so the client can show the
     * right "turn on/off" wording.
     *
     * @param labels the labels map to update in place
     */
    public static void applyToggleLedLabels(Map<String, String> labels) {
        labels.put("toggleLed", CommonUtility.getWord(
                MainSingleton.getInstance().config.isToggleLed()
                        ? Constants.TURN_LED_OFF : Constants.TURN_LED_ON));
        labels.put("turnLedOn", CommonUtility.getWord(Constants.TURN_LED_ON));
        labels.put("turnLedOff", CommonUtility.getWord(Constants.TURN_LED_OFF));
    }

    /**
     * Build the map of localized titles for every section and sub-accordion of the settings page.
     *
     * @return map of section key to its localized title
     */
    public static Map<String, String> getSectionTitles() {
        Map<String, String> titles = new LinkedHashMap<>();
        titles.put("leds", CommonUtility.getWord("fxml.setting.ledsconfig"));
        titles.put("mode", CommonUtility.getWord("fxml.setting.mode"));
        titles.put("network", CommonUtility.getWord("fxml.setting.wifimqtt"));
        titles.put("misc", CommonUtility.getWord("fxml.setting.misc"));
        titles.put("devices", CommonUtility.getWord("fxml.setting.devices"));
        titles.put("ldr", CommonUtility.getWord("fxml.setting.ldr"));
        titles.put("display", CommonUtility.getWord("fxml.ledsconfigtab.display"));
        titles.put("colorCorr", CommonUtility.getWord("fxml.misctab.colorcorrection"));
        titles.put("eyeCare", CommonUtility.getWord("fxml.misctab.eyecare"));
        titles.put("gamma", CommonUtility.getWord("fxml.misctab.gamma"));
        titles.put("profile", CommonUtility.getWord("fxml.misctab.profiles"));
        titles.put("smoothing", CommonUtility.getWord("fxml.dialog.smoothing.title"));
        titles.put("connectedDevices", CommonUtility.getWord("fxml.devicestab.connected.devices"));
        titles.put("satellites", CommonUtility.getWord("fxml.devicestab.satellites"));
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
