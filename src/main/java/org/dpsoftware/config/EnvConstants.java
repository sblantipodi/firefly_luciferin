/*
  EnvConstants.java

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

/**
 * Environment variable names and values.
 */
public final class EnvConstants {

    // Environment variable names
    public static final String LUCIFERIN_LOG_LEVEL = "LUCIFERIN_LOG_LEVEL";
    public static final String LUCIFERIN_LOSSLESS_COMPRESSION_LOG = "LUCIFERIN_LOSSLESS_COMPRESSION_LOG";
    public static final String LUCIFERIN_SIMD_STRATEGY_OVERRIDE = "LUCIFERIN_SIMD_STRATEGY";
    public static final String LUCIFERIN_LIVE_PREVIEW_IMAGE = "LUCIFERIN_LIVE_PREVIEW_IMAGE";
    public static final String XDG_HOME = "XDG_CONFIG_HOME";
    public static final String PATH = "path";
    public static final String GST_GL_WINDOW = "GST_GL_WINDOW";
    public static final String GST_GL_PLATFORM = "GST_GL_PLATFORM";
    public static final String DISPLAY_MANAGER_CHK = "XDG_SESSION_TYPE";
    public static final String DISPLAY_MANAGER_HYPRLAND_CHK = "HYPRLAND_INSTANCE_SIGNATURE";
    public static final String FLATPAK_ID = "FLATPAK_ID";
    public static final String SNAP_NAME = "SNAP_NAME";
    public static final String PROCESSOR_IDENTIFIER = "PROCESSOR_IDENTIFIER";
    // GStreamer System Env Overrides
    public static final String CUSTOM_GSTREAMER_PIPELINE = System.getenv("CUSTOM_GSTREAMER_PIPELINE");
    public static final String CUSTOM_GSTREAMER_CAPS = System.getenv("CUSTOM_GSTREAMER_CAPS");
    public static final String CUSTOM_GSTREAMER_BO = System.getenv("CUSTOM_GSTREAMER_BO");

    private EnvConstants() {
    }
}
