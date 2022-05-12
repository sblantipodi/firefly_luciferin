/*
  Presets.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2022  Davide Perini

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
package org.dpsoftware.managers.dto;

import lombok.Getter;
import lombok.Setter;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Preset class used for presets that stores misc tab values
 */
@Getter
@Setter
public class Preset {

    private String presetName;
    private double gamma;
    private int colorMode = 1;
    private int whiteTemperature = Constants.DEFAULT_WHITE_TEMP;
    private boolean eyeCare = false;
    private String nightModeFrom = LocalTime.now().withHour(22).withMinute(0).truncatedTo(ChronoUnit.MINUTES).toString();
    private String nightModeTo = LocalTime.now().withHour(8).withMinute(0).truncatedTo(ChronoUnit.MINUTES).toString();
    private String nightModeBrightness = "0%";
    private boolean toggleLed = true;
    private String desiredFramerate = "30";
    private String colorChooser = Constants.DEFAULT_COLOR_CHOOSER;
    private int brightness;
    private String effect = Constants.Effect.BIAS_LIGHT.getBaseI18n();
    private float audioLoopbackGain = 0.0f;
    private String audioDevice = NativeExecutor.isWindows() ? Constants.Audio.DEFAULT_AUDIO_OUTPUT_WASAPI.getBaseI18n()
            : Constants.Audio.DEFAULT_AUDIO_OUTPUT_NATIVE.getBaseI18n();
    private String audioChannels = Constants.AudioChannels.AUDIO_CHANNEL_2.getBaseI18n();

}
