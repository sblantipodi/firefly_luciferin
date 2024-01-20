/*
  AudioSingleton.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dpsoftware.managers.dto.AudioDevice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Audio singleton used to share common data
 */
@Getter
@Setter
@NoArgsConstructor
public class AudioSingleton {

    @Getter
    private final static AudioSingleton instance;

    static {
        instance = new AudioSingleton();
    }

    public volatile boolean RUNNING_AUDIO = false;
    public int AUDIO_BRIGHTNESS = 255;
    public Map<String, AudioDevice> audioDevices = new LinkedHashMap<>();

}

