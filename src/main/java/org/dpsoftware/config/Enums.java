/*
  Enums.java

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
package org.dpsoftware.config;

import lombok.Getter;
import org.dpsoftware.MainSingleton;

import java.util.Arrays;

/**
 * Enums
 */
public class Enums {

    public enum OsKind {
        WINDOWS,
        LINUX,
        MAC,
        OTHER
    }

    public enum SupportedDevice {
        ESP8266,
        ESP32,
        ESP32_C3_CDC,
        ESP32_C6,
        ESP32_S2,
        ESP32_S3,
        ESP32_S3_CDC
    }

    public enum PlayerStatus {
        PLAY,
        PLAY_WAITING,
        STOP,
        GREY,
        OFF
    }

    public enum FirmwareType {
        LIGHT,
        FULL
    }

    public enum HSL {
        H,
        S,
        L
    }

    public enum TRAY_PREFERENCE {
        AUTO,
        DISABLED,
        FORCE_AWT
    }

    // Color correction, Hue-Saturation-Lightness (using HSV 360° wheel)
    @Getter
    public enum ColorEnum {
        RED(330.0F, 0.0F, 30.0F),
        YELLOW(30.0F, 60.0F, 90.0F),
        GREEN(90.0F, 120.0F, 150.0F),
        CYAN(150.0F, 180.0F, 210.0F),
        BLUE(210.0F, 240.0F, 270.0F),
        MAGENTA(270.0F, 300.0F, 330.0F),
        GREY(0.0F, 0.0F, 0.0F),
        MASTER(0.0F, 0.0F, 0.0F);

        private static final ColorEnum[] vals = values();
        private final float min;
        private final float val;
        private final float max;

        ColorEnum(float min, float val, float max) {
            this.min = min;
            this.val = val;
            this.max = max;
        }

        public ColorEnum next() {
            return (this == MASTER || this == MAGENTA) ? RED : vals[this.ordinal() + 1];
        }

        public ColorEnum prev() {
            return (this == MASTER || this == RED) ? MAGENTA : vals[this.ordinal() - 1];
        }
    }

    public enum MonitorAspectRatio {
        AR_43,
        AR_169,
        AR_219,
        AR_329
    }

    public enum Orientation implements LocalizedEnum {
        CLOCKWISE("enum.orientation.clockwise"),
        ANTICLOCKWISE("enum.orientation.anticlockwise");
        private final String orientation;

        Orientation(String orientation) {
            this.orientation = orientation;
        }

        public String getValue() {
            return orientation;
        }
    }

    public enum Direction implements LocalizedEnum {
        NORMAL("enum.direction.normal"),
        INVERSE("enum.direction.inverse");
        private final String direction;

        Direction(String direction) {
            this.direction = direction;
        }

        public String getValue() {
            return direction;
        }
    }

    public enum AspectRatio implements LocalizedEnum {
        FULLSCREEN("enum.aspect.ratio.fullscreen"),
        LETTERBOX("enum.aspect.ratio.letterbox"),
        PILLARBOX("enum.aspect.ratio.pillarbox");
        private final String aspectRatio;

        AspectRatio(String aspectRatio) {
            this.aspectRatio = aspectRatio;
        }

        public String getValue() {
            return aspectRatio;
        }
    }

    public enum LedOffset implements LocalizedEnum {
        BOTTOM_LEFT("enum.led.offset.bottom.left"),
        BOTTOM_CENTER("enum.led.offset.bottom.center"),
        BOTTOM_RIGHT("enum.led.offset.bottom.right"),
        UPPER_RIGHT("enum.led.offset.upper.right"),
        UPPER_LEFT("enum.led.offset.upper.left");
        private final String ledOffset;

        LedOffset(String ledOffset) {
            this.ledOffset = ledOffset;
        }

        public String getValue() {
            return ledOffset;
        }
    }

    public enum Effect implements LocalizedEnum {
        BIAS_LIGHT("enum.effect.bias.light"),
        SOLID("enum.effect.solid"),
        MUSIC_MODE_VU_METER("enum.effect.mm.vumeter"),
        MUSIC_MODE_VU_METER_DUAL("enum.effect.mm.dual.vumeter"),
        MUSIC_MODE_BRIGHT("enum.effect.mm.screencapture"),
        MUSIC_MODE_RAINBOW("enum.effect.mm.rainbow"),
        FIRE("enum.effect.fire"),
        TWINKLE("enum.effect.twinkle"),
        BPM("enum.effect.bpm"),
        RAINBOW("enum.effect.rainbox"),
        SUPER_SLOW_RAINBOW("enum.effect.slow.rainbow"),
        CHASE_RAINBOW("enum.effect.chaserainbox"),
        SOLID_RAINBOW("enum.effect.solidrainbow"),
        RANDOM_COLORS("enum.effect.random.colors"),
        RAINBOW_COLORS("enum.effect.rainbow.colors"),
        METEOR("enum.effect.meteor"),
        COLOR_WATERFALL("enum.effect.waterfall"),
        RANDOM_MARQUEE("enum.effect.random.marquee"),
        RAINBOW_MARQUEE("enum.effect.rainbow.marquee"),
        RAIN1("enum.effect.rain"),
        CHRISTMAS("enum.effect.christmas");
        private final String effect;

        Effect(String effect) {
            this.effect = effect;
        }

        public String getValue() {
            return effect;
        }
    }

    @Getter
    public enum BaudRate {
        BAUD_RATE_115200("115200", 8),
        BAUD_RATE_230400("230400", 1),
        BAUD_RATE_460800("460800", 2),
        BAUD_RATE_500000("500000", 3),
        BAUD_RATE_921600("921600", 4),
        BAUD_RATE_1000000("1000000", 5),
        BAUD_RATE_1500000("1500000", 6),
        BAUD_RATE_2000000("2000000", 7),
        BAUD_RATE_4000000("4000000", 9),
        BAUD_RATE_6000000("6000000", 10);
        private final String baudRate;
        private final int baudRateValue;

        BaudRate(String baudRate, int baudRateValue) {
            this.baudRate = baudRate;
            this.baudRateValue = baudRateValue;
        }

        public static BaudRate findByValue(final int baudRateValToSearch) {
            return Arrays.stream(values()).filter(value -> value.getBaudRateValue() == baudRateValToSearch).findFirst().orElse(null);
        }

        public static BaudRate findByExtendedVal(final String baudRateToSearch) {
            return Arrays.stream(values()).filter(value -> value.getBaudRate().equals(baudRateToSearch)).findFirst().orElse(null);
        }
    }

    public enum Framerate implements LocalizedEnum {
        FPS_5("enum.framerate.5.fps"),
        FPS_10("enum.framerate.10.fps"),
        FPS_15("enum.framerate.15.fps"),
        FPS_20("enum.framerate.20.fps"),
        FPS_25("enum.framerate.25.fps"),
        FPS_30("enum.framerate.30.fps"),
        FPS_40("enum.framerate.40.fps"),
        FPS_50("enum.framerate.50.fps"),
        FPS_60("enum.framerate.60.fps"),
        FPS_90("enum.framerate.90.fps"),
        FPS_120("enum.framerate.120.fps"),
        FPS_144("enum.framerate.144.fps"),
        UNLOCKED("enum.framerate.unlocked");
        private final String framerate;

        Framerate(String framerate) {
            this.framerate = framerate;
        }

        public String getValue() {
            return framerate;
        }
    }

    @Getter
    public enum SmoothingTarget {
        TARGET_30_FPS("30 FPS", 30),
        TARGET_60_FPS("60 FPS", 60),
        TARGET_120_FPS("120 FPS", 120);
        private final String smoothingTarget;
        private final int smoothingTargetValue;

        SmoothingTarget(String smoothingTarget, int smoothingTargetValue) {
            this.smoothingTarget = smoothingTarget;
            this.smoothingTargetValue = smoothingTargetValue;
        }

        public static SmoothingTarget findByValue(final int targetValToSearch) {
            return Arrays.stream(values()).filter(value -> value.getSmoothingTargetValue() == targetValToSearch).findFirst().orElse(null);
        }

        public static SmoothingTarget findByExtendedVal(final String targetToSearch) {
            return Arrays.stream(values()).filter(value -> value.getSmoothingTarget().equals(targetToSearch)).findFirst().orElse(null);
        }
    }

    public enum Smoothing implements LocalizedEnum {
        DISABLED("no.smoothing", 0, 0.0F),
        SMOOTHING_LVL_1("enum.smoothing.lvl.1", 0, 0.35F),
        SMOOTHING_LVL_2("enum.smoothing.lvl.2", 30, 0.30F),
        SMOOTHING_LVL_3("enum.smoothing.lvl.3", 30, 0.20F),
        SMOOTHING_LVL_4("enum.smoothing.lvl.4", 15, 0.20F),
        SMOOTHING_LVL_5("enum.smoothing.lvl.5", 10, 0.15F),
        SMOOTHING_LVL_6("enum.smoothing.lvl.6", 5, 0.10F),
        CUSTOM("enum.smoothing.custom", -1, -1);
        private final String frameInsertionStr;
        @Getter
        private final int frameInsertionFramerate;
        @Getter
        private final float emaAlpha;

        Smoothing(String frameInsertionStr, int frameInsertionFramerate, float emaAlpha) {
            this.frameInsertionStr = frameInsertionStr;
            this.frameInsertionFramerate = frameInsertionFramerate;
            this.emaAlpha = emaAlpha;
        }

        public static Smoothing findByFramerateAndAlpha(int frameInsertionFramerate, float emaAlpha) {
            for (Smoothing smoothing : values()) {
                if (smoothing.getFrameInsertionFramerate() == frameInsertionFramerate
                        && smoothing.getEmaAlpha() == emaAlpha
                        && MainSingleton.getInstance().config.getSmoothingTargetFramerate() == Constants.DEFAULT_SMOOTHING_TARGET) {
                    return smoothing;
                }
            }
            return CUSTOM;
        }

        public String getValue() {
            return frameInsertionStr;
        }
    }

    public enum FrameGeneration implements LocalizedEnum {
        DISABLED("enum.disabled", 0),
        FI_2X("2x", 30),
        FI_3X("3x", 20),
        FI_4X("4x", 15),
        FI_6X("6x", 10),
        FI_12X("12x", 5),
        FI_30X("30x", 2);
        private final String frameGenerationStr;
        @Getter
        private final int frameGenerationTarget;

        FrameGeneration(String frameGenerationStr, int frameInsertionTarget) {
            this.frameGenerationStr = frameGenerationStr;
            this.frameGenerationTarget = frameInsertionTarget;
        }

        public static FrameGeneration findByValue(final int valToSearch) {
            return Arrays.stream(values()).filter(value -> value.getFrameGenerationTarget() == valToSearch).findFirst().orElse(null);
        }

        public String getValue() {
            return frameGenerationStr;
        }

    }

    public enum Ema implements LocalizedEnum {
        DISABLED("enum.disabled", 0.0F),
        SMOOTHING_EMA_1("enum.ema.lvl.1", 0.35F), // Very fast
        SMOOTHING_EMA_2("enum.ema.lvl.2", 0.30F), // Fast
        SMOOTHING_EMA_3("enum.ema.lvl.3", 0.25F), // Rapid
        SMOOTHING_EMA_4("enum.ema.lvl.4", 0.20F), // Moderate
        SMOOTHING_EMA_5("enum.ema.lvl.5", 0.15F), // Slow
        SMOOTHING_EMA_6("enum.ema.lvl.6", 0.10F); // Very slow
        private final String emaStr;
        @Getter
        private final float emaAlpha;

        Ema(String emaStr, float emaAlpha) {
            this.emaStr = emaStr;
            this.emaAlpha = emaAlpha;
        }

        public static Ema findByValue(final float valToSearch) {
            return Arrays.stream(values()).filter(value -> value.getEmaAlpha() == valToSearch).findFirst().orElse(null);
        }

        public String getValue() {
            return emaStr;
        }
    }

    @Getter
    public enum ScalingRatio {
        RATIO_100("100%"),
        RATIO_125("125%"),
        RATIO_150("150%"),
        RATIO_175("175%"),
        RATIO_200("200%"),
        RATIO_225("225%"),
        RATIO_250("250%"),
        RATIO_300("300%"),
        RATIO_350("350%");
        private final String scalingRatio;

        ScalingRatio(String scalingRatio) {
            this.scalingRatio = scalingRatio;
        }
    }

    @Getter
    public enum Gamma {
        GAMMA_10("1.0"),
        GAMMA_12("1.2"),
        GAMMA_14("1.4"),
        GAMMA_16("1.6"),
        GAMMA_18("1.8"),
        GAMMA_20("2.0"),
        GAMMA_22("2.2"),
        GAMMA_24("2.4"),
        GAMMA_26("2.6"),
        GAMMA_28("2.8"),
        GAMMA_30("3.0"),
        GAMMA_40("4.0"),
        GAMMA_50("5.0"),
        GAMMA_60("6.0"),
        GAMMA_80("8.0"),
        GAMMA_100("10.0");
        private final String gamma;

        Gamma(String gamma) {
            this.gamma = gamma;
        }
    }

    public enum AudioChannels implements LocalizedEnum {
        AUDIO_CHANNEL_1("enum.audio.1.channel"),
        AUDIO_CHANNEL_2("enum.audio.2.channel"),
        AUDIO_CHANNEL_3("enum.audio.3.channel"),
        AUDIO_CHANNEL_4("enum.audio.4.channel"),
        AUDIO_CHANNEL_5("enum.audio.5.channel"),
        AUDIO_CHANNEL_6("enum.audio.6.channel"),
        AUDIO_CHANNEL_7("enum.audio.7.channel"),
        AUDIO_CHANNEL_8("enum.audio.8.channel"),
        AUDIO_CHANNEL_9("enum.audio.9.channel");
        private final String audioChannel;

        AudioChannels(String audioChannel) {
            this.audioChannel = audioChannel;
        }

        public String getValue() {
            return audioChannel;
        }
    }

    public enum LdrInterval implements LocalizedEnum {
        CONTINUOUS("ldr.reading.continuous", 0),
        MINUTES_10("enum.power.saving.10.minutes", 10),
        MINUTES_20("enum.power.saving.20.minutes", 20),
        MINUTES_30("enum.power.saving.30.minutes", 30),
        MINUTES_40("enum.power.saving.40.minutes", 40),
        MINUTES_50("enum.power.saving.50.minutes", 50),
        MINUTES_60("enum.power.saving.60.minutes", 60),
        MINUTES_120("enum.power.saving.120.minutes", 120);
        private final String ldrInterval;
        private final int ldrIntervalValue;

        LdrInterval(String ldrInterval, int ldrIntervalValue) {
            this.ldrInterval = ldrInterval;
            this.ldrIntervalValue = ldrIntervalValue;
        }

        public static LdrInterval findByValue(final int ldrContReadingValue) {
            return Arrays.stream(values()).filter(value -> value.getLdrIntervalInteger() == ldrContReadingValue).findFirst().orElse(null);
        }

        public String getValue() {
            return ldrInterval;
        }

        public int getLdrIntervalInteger() {
            return ldrIntervalValue;
        }
    }

    public enum ResamplingFactor implements LocalizedEnum {
        POOR("enum.resampling.factor.poor", 12),
        FAIR("enum.resampling.factor.fair", 8),
        BALANCED("enum.resampling.factor.balanced", 4),
        VERY_GOOD("enum.resampling.factor.verygood", 2),
        NATIVE("enum.resampling.factor.native", 1);
        private final String quality;
        private final int resamplingFactor;

        ResamplingFactor(String quality, int resamplingFactor) {
            this.quality = quality;
            this.resamplingFactor = resamplingFactor;
        }

        public static ResamplingFactor findByValue(final int resamplingFactorValue) {
            return Arrays.stream(values()).filter(value -> value.getResamplingFactorValue() == resamplingFactorValue).findFirst().orElse(null);
        }

        public String getValue() {
            return quality;
        }

        public int getResamplingFactorValue() {
            return resamplingFactor;
        }
    }

    public enum BrightnessLimiter implements LocalizedEnum {
        BRIGHTNESS_LIMIT_DISABLED("enum.disabled", 1.0F),
        BRIGHTNESS_LIMIT_90("90%", 0.9F),
        BRIGHTNESS_LIMIT_80("80%", 0.8F),
        BRIGHTNESS_LIMIT_70("70%", 0.7F),
        BRIGHTNESS_LIMIT_60("60%", 0.6F),
        BRIGHTNESS_LIMIT_50("50%", 0.5F),
        BRIGHTNESS_LIMIT_40("40%", 0.4F),
        BRIGHTNESS_LIMIT_30("30%", 0.3F);
        private final String brightnessLimit;
        @Getter
        private final float brightnessLimitFloat;

        BrightnessLimiter(String brightnessLimit, float brightnessLimitFloat) {
            this.brightnessLimit = brightnessLimit;
            this.brightnessLimitFloat = brightnessLimitFloat;
        }

        public static BrightnessLimiter findByValue(final float brightnessLimitValToSearch) {
            return Arrays.stream(values()).filter(value -> value.getBrightnessLimitFloat() == brightnessLimitValToSearch).findFirst().orElse(null);
        }

        public String getValue() {
            return brightnessLimit;
        }
    }

    public enum PowerSaving implements LocalizedEnum {
        DISABLED("enum.disabled"),
        MINUTES_2("enum.power.saving.2.minutes"),
        MINUTES_5("enum.power.saving.5.minutes"),
        MINUTES_10("enum.power.saving.10.minutes"),
        MINUTES_15("enum.power.saving.15.minutes"),
        MINUTES_20("enum.power.saving.20.minutes"),
        MINUTES_25("enum.power.saving.25.minutes"),
        MINUTES_30("enum.power.saving.30.minutes"),
        MINUTES_35("enum.power.saving.35.minutes"),
        MINUTES_40("enum.power.saving.40.minutes"),
        MINUTES_45("enum.power.saving.45.minutes"),
        MINUTES_50("enum.power.saving.50.minutes"),
        MINUTES_55("enum.power.saving.55.minutes"),
        MINUTES_60("enum.power.saving.60.minutes");
        private final String powerSaving;

        PowerSaving(String powerSaving) {
            this.powerSaving = powerSaving;
        }

        public String getValue() {
            return powerSaving;
        }
    }

    public enum Theme implements LocalizedEnum {
        CLASSIC("enum.theme.classic", "css/main.css"),
        LIGHT_THEME_SILVER("enum.theme.light.silver", "css/theme-light-silver.css"),
        LIGHT_THEME_CYAN("enum.theme.light.cyan", "css/theme-light-cyan.css"),
        DARK_THEME_ORANGE("enum.theme.dark.orange", "css/theme-dark-orange.css"),
        DARK_THEME_CYAN("enum.theme.dark.cyan", "css/theme-dark-cyan.css"),
        DARK_BLUE_THEME("enum.theme.blue.dark", "css/theme-dark-blue.css"),
        DARK_THEME_ARTIC("enum.theme.dark.artic", "css/theme-dark-artic.css"),
        DARK_THEME_PURPLE("enum.theme.purple", "css/theme-dark-purple.css"),
        DARK_THEME_AMETHYST("enum.theme.dark.amethyst", "css/theme-dark-amethyst.css"),
        DARK_THEME_BRONZE("enum.theme.dark.bronze", "css/theme-dark-bronze-gold.css"),
        DARK_THEME_EMERALD("enum.theme.dark.emerald", "css/theme-dark-emerald.css"),
        DARK_THEME_RUBY("enum.theme.dark.ruby", "css/theme-dark-ruby-red.css");
        private final String theme;
        @Getter
        private final String cssPath;

        Theme(String theme, String cssPath) {
            this.theme = theme;
            this.cssPath = cssPath;
        }

        public String getValue() {
            return theme;
        }
    }

    public enum Language implements LocalizedEnum {
        DE("enum.language.de"),
        EN("enum.language.en"),
        ES("enum.language.es"),
        FR("enum.language.fr"),
        HU("enum.language.hu"),
        IT("enum.language.it"),
        RU("enum.language.ru"),
        PL("enum.language.pl");
        private final String language;

        Language(String language) {
            this.language = language;
        }

        public String getValue() {
            return language;
        }
    }

    public enum NightLight implements LocalizedEnum {
        DISABLED("enum.nightlight.disabled"),
        AUTO("enum.nightlight.auto"),
        ENABLED("enum.nightlight.enabled");
        private final String nightLight;

        NightLight(String nightLight) {
            this.nightLight = nightLight;
        }

        public static NightLight findByValue(final String valToSearch) {
            return Arrays.stream(values()).filter(value -> value.getBaseI18n().equals(valToSearch)).findFirst().orElse(null);
        }

        public String getValue() {
            return nightLight;
        }
    }

    @Getter
    public enum StreamType {
        UDP("UDP stream"),
        MQTT("MQTT stream");
        private final String streamType;

        StreamType(String streamType) {
            this.streamType = streamType;
        }

    }

    public enum Audio implements LocalizedEnum {
        DEFAULT_AUDIO_OUTPUT_WASAPI("enum.default.audio.output.wasapi"),
        DEFAULT_AUDIO_OUTPUT_NATIVE("enum.default.audio.output.native");
        private final String defaultAudio;

        Audio(String defaultAudio) {
            this.defaultAudio = defaultAudio;
        }

        public String getValue() {
            return defaultAudio;
        }
    }

    @Getter
    public enum InterfaceToExclude {
        VIRTUAL("virtual"),
        HYPER("hyper-v"),
        VMWARE("vmware"),
        VBOX("vbox"),
        DOCKER("docker"),
        TUN("tun"),
        TAP("tap");
        private final String interfaceToExclude;

        InterfaceToExclude(String interfaceToExclude) {
            this.interfaceToExclude = interfaceToExclude;
        }

        public static boolean contains(String interfaceToSearch) {
            for (InterfaceToExclude i : InterfaceToExclude.values()) {
                if (interfaceToSearch.contains(i.interfaceToExclude)) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum ColorMode implements LocalizedEnum {
        RGB_MODE("enum.color.mode.rgb"),
        RGBW_MODE_ACCURATE("enum.color.mode.rgbw.accurate"),
        RGBW_MODE_BRIGHTER("enum.color.mode.rgbw.brighter"),
        RGBW_RGB("enum.color.mode.rgbw.rgb"),
        DOTSTAR("enum.color.mode.dotstar");
        private final String colorMode;

        ColorMode(String colorMode) {
            this.colorMode = colorMode;
        }

        public String getValue() {
            return colorMode;
        }
    }

    public enum ColorOrder {
        GRB_GRBW(1),
        RGB_RGBW(2),
        BGR_BGRW(3),
        BRG_BRGW(4),
        RBG_RBGW(5),
        GBR_GBRW(6),
        GRWB(7),
        GWBR(8),
        WRBG(9),
        RGWB(10),
        RWBG(11),
        WGBR(12),
        BGWR(13),
        BWRG(14),
        WGRB(15),
        BRWG(16),
        BWGR(17),
        WRGB(18),
        RBWG(19),
        RWGB(20),
        WBGR(21),
        GBWR(22),
        GWRB(23),
        WBRG(24);
        private final int colorOrder;

        ColorOrder(int colorOrder) {
            this.colorOrder = colorOrder;
        }

        public static ColorOrder findByValue(final int colorOrderValToSearch) {
            return Arrays.stream(values()).filter(value -> value.getValue() == colorOrderValToSearch).findFirst().orElse(null);
        }

        public int getValue() {
            return colorOrder;
        }
    }

    public enum ThreadPriority {
        REALTIME(256),
        HIGH(128),
        ABOVE_NORMAL(32768),
        NORMAL(32),
        BELOW_NORMAL(16384),
        LOW(16384);
        private final int threadPriority;

        ThreadPriority(int threadPriority) {
            this.threadPriority = threadPriority;
        }

        public int getValue() {
            return threadPriority;
        }
    }

    public enum Algo implements LocalizedEnum {
        AVG_COLOR("enum.color.algo.avg"),
        AVG_ALL_COLOR("enum.color.algo.avg.all");
        private final String algo;

        Algo(String algo) {
            this.algo = algo;
        }

        public String getValue() {
            return algo;
        }
    }

    /**
     * NOTE: This enum is used to define the zones on multiscreen setups, order of its values is important.
     */
    public enum PossibleZones implements LocalizedEnum {
        BOTTOM_LEFT("enum.satellite.zone.bottom.left"),
        BOTTOM("enum.satellite.zone.bottom"),
        BOTTOM_RIGHT("enum.satellite.zone.bottom.right"),
        RIGHT("enum.satellite.zone.right"),
        TOP_RIGHT("enum.satellite.zone.top.right"),
        TOP("enum.satellite.zone.top"),
        TOP_LEFT("enum.satellite.zone.top.left"),
        LEFT("enum.satellite.zone.left"),
        ENTIRE_SCREEN("enum.satellite.zone.entire.screen");
        private final String zone;

        PossibleZones(String zone) {
            this.zone = zone;
        }

        public String getValue() {
            return zone;
        }
    }

    public enum SimdAvxOption implements LocalizedEnum {
        AUTO("enum.simd.auto", 0),
        AVX512("enum.simd.avx512", 1),
        AVX256("enum.simd.avx256", 2),
        AVX("enum.simd.avx", 3),
        DISABLED("enum.simd.disabled", 4);
        private final String simdOption;
        @Getter
        private final int simdOptionNumeric;

        SimdAvxOption(String simdOption, int simdOptionNumeric) {
            this.simdOption = simdOption;
            this.simdOptionNumeric = simdOptionNumeric;
        }

        public static SimdAvxOption findByValue(final int valToSearch) {
            return Arrays.stream(values()).filter(value -> value.getSimdOptionNumeric() == valToSearch).findFirst().orElse(null);
        }

        public String getValue() {
            return simdOption;
        }
    }

    public enum CpuGpuLoadThreshold implements LocalizedEnum {
        CPU_GPU_THRESHOLD_DISABLED("enum.disabled", 0),
        CPU_GPU_THRESHOLD_100("100%", 100),
        CPU_GPU_THRESHOLD_90("90%", 90),
        CPU_GPU_THRESHOLD_80("80%", 80),
        CPU_GPU_THRESHOLD_70("70%", 70),
        CPU_GPU_THRESHOLD_60("60%", 60),
        CPU_GPU_THRESHOLD_50("50%", 50),
        CPU_GPU_THRESHOLD_40("40%", 40),
        CPU_GPU_THRESHOLD_30("30%", 30),
        CPU_GPU_THRESHOLD_20("20%", 20),
        CPU_GPU_THRESHOLD_10("10%", 10);
        private final String cpuGpuLoadThreshold;
        @Getter
        private final int cpuGpuLoadThresholdVal;

        CpuGpuLoadThreshold(String cpuGpuLoadThreshold, int cpuGpuLoadThresholdVal) {
            this.cpuGpuLoadThreshold = cpuGpuLoadThreshold;
            this.cpuGpuLoadThresholdVal = cpuGpuLoadThresholdVal;
        }

        public static CpuGpuLoadThreshold findByValue(final int cpuGpuLoadThresholdValToSearch) {
            return Arrays.stream(values()).filter(value -> value.getCpuGpuLoadThresholdVal() == cpuGpuLoadThresholdValToSearch).findFirst().orElse(null);
        }

        public String getValue() {
            return cpuGpuLoadThreshold;
        }
    }

}
