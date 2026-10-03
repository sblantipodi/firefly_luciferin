package org.dpsoftware.gui.controllers.options;

import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.config.LocalizedEnum;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Values and transitions shared by the JavaFX smoothing dialog and web settings.
 */
public final class SmoothingOptions {

    /**
     * Prevents instantiation.
     */
    private SmoothingOptions() {
    }

    /**
     * Applies a named smoothing preset to its EMA and frame insertion settings.
     *
     * @param config configuration to update
     * @param value  stored or localized smoothing name
     */
    public static void applyPreset(Configuration config, String value) {
        Enums.Smoothing smoothing = LocalizedEnum.fromBaseStr(Enums.Smoothing.class, value);
        if (smoothing == null) smoothing = LocalizedEnum.fromStr(Enums.Smoothing.class, value);
        if (smoothing == null) throw new IllegalArgumentException("Invalid smoothing level");
        config.setSmoothingType(smoothing.getBaseI18n());
        if (smoothing == Enums.Smoothing.CUSTOM) return;
        config.setFrameInsertionTarget(smoothing.getFrameInsertionFramerate());
        config.setEmaAlpha(smoothing.getEmaAlpha());
        config.setSmoothingTargetFramerate(Constants.DEFAULT_SMOOTHING_TARGET);
    }

    /**
     * Applies the three controls in the smoothing dialog and derives the matching preset.
     *
     * @param config          configuration to update
     * @param alpha           EMA weight
     * @param frameInsertion  generated frame target
     * @param targetFramerate smoothing target in FPS
     */
    public static void applyControls(Configuration config, float alpha, int frameInsertion, int targetFramerate) {
        if (Enums.Ema.findByValue(alpha) == null || Enums.FrameGeneration.findByValue(frameInsertion) == null
                || Enums.SmoothingTarget.findByValue(targetFramerate) == null) {
            throw new IllegalArgumentException("Invalid smoothing controls");
        }
        config.setEmaAlpha(alpha);
        config.setFrameInsertionTarget(frameInsertion);
        config.setSmoothingTargetFramerate(targetFramerate);
        Enums.Smoothing match = Arrays.stream(Enums.Smoothing.values())
                .filter(level -> level != Enums.Smoothing.CUSTOM
                        && level.getEmaAlpha() == alpha
                        && level.getFrameInsertionFramerate() == frameInsertion
                        && targetFramerate == Constants.DEFAULT_SMOOTHING_TARGET)
                .findFirst().orElse(Enums.Smoothing.CUSTOM);
        config.setSmoothingType(match.getBaseI18n());
    }

    /**
     * Calculates the frame rate displayed by the JavaFX smoothing dialog.
     *
     * @param config current settings
     * @return display value, including FPS unit
     */
    public static String captureFramerateLabel(Configuration config) {
        int frames = config.getFrameInsertionTarget();
        if (frames == 0) {
            try {
                frames = Integer.parseInt(config.getDesiredFramerate());
            } catch (NumberFormatException ignored) {
                return config.getDesiredFramerate();
            }
        } else if (config.getSmoothingTargetFramerate() == 120) {
            frames *= 2;
        } else if (config.getSmoothingTargetFramerate() == 30) {
            frames /= 2;
        }
        return frames + Constants.FPS_VAL;
    }

    /**
     * Exposes preset parameters so the web controls can update together.
     *
     * @return preset name to EMA, frame insertion, and target FPS
     */
    public static Map<String, Preset> presets() {
        Map<String, Preset> presets = new LinkedHashMap<>();
        for (Enums.Smoothing level : Enums.Smoothing.values()) {
            if (level != Enums.Smoothing.CUSTOM) {
                presets.put(level.getBaseI18n(), new Preset(level.getEmaAlpha(),
                        level.getFrameInsertionFramerate(), Constants.DEFAULT_SMOOTHING_TARGET));
            }
        }
        return presets;
    }

    /**
     * Values of one named smoothing preset.
     */
    public record Preset(float emaAlpha, int frameInsertionTarget, int smoothingTargetFramerate) {
    }
}
