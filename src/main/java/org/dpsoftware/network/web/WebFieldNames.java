/*
  WebFieldNames.java

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

/**
 * Names of the configuration fields, section keys and request parameters exchanged between the
 * web settings page and the {@link ConfigServer} endpoints. The values are the exact JSON keys the
 * client reads and writes, so they must stay stable; the constants only remove the string literals
 * scattered across the server side.
 */
public final class WebFieldNames {

    // --- comboChange request payload keys ---
    public static final String COMBO_NAME = "comboName";
    public static final String VALUE = "value";
    // --- LED configuration section fields ---
    public static final String TOP_LED = "topLed";
    public static final String LEFT_LED = "leftLed";
    public static final String RIGHT_LED = "rightLed";
    public static final String BOTTOM_LEFT_LED = "bottomLeftLed";
    public static final String BOTTOM_RIGHT_LED = "bottomRightLed";
    public static final String BOTTOM_ROW_LED = "bottomRowLed";
    public static final String LED_START_OFFSET = "ledStartOffset";
    public static final String ORIENTATION = "orientation";
    public static final String GROUP_BY = "groupBy";
    public static final String SPLIT_BOTTOM_MARGIN = "splitBottomMargin";
    public static final String GRABBER_AREA_TOP_BOTTOM = "grabberAreaTopBottom";
    public static final String GRABBER_SIDE = "grabberSide";
    public static final String GAP_TYPE_TOP_BOTTOM = "gapTypeTopBottom";
    public static final String GAP_TYPE_SIDE = "gapTypeSide";
    public static final String DEFAULT_LED_MATRIX = "defaultLedMatrix";
    // --- mode / capture section fields ---
    public static final String OUTPUT_DEVICE = "outputDevice";
    public static final String BAUD_RATE = "baudRate";
    public static final String STATIC_GLOW_WORM_IP = "staticGlowWormIp";
    public static final String DESIRED_FRAMERATE = "desiredFramerate";
    public static final String SMOOTHING_TYPE = "smoothingType";
    public static final String SMOOTHING_TARGET_FRAMERATE = "smoothingTargetFramerate";
    public static final String FRAME_INSERTION_TARGET = "frameInsertionTarget";
    public static final String EMA_ALPHA = "emaAlpha";
    public static final String SIMD_AVX = "simdAvx";
    public static final String RESAMPLING_FACTOR = "resamplingFactor";
    public static final String CAPTURE_METHOD = "captureMethod";
    public static final String MONITOR_NUMBER = "monitorNumber";
    public static final String SCREEN_RES_X = "screenResX";
    public static final String SCREEN_RES_Y = "screenResY";
    public static final String OS_SCALING = "osScaling";
    public static final String AUTO_DETECT_BLACK_BARS = "autoDetectBlackBars";
    public static final String ALGO = "algo";
    public static final String LANGUAGE = "language";
    // --- MQTT section fields ---
    public static final String MQTT_ENABLE = "mqttEnable";
    public static final String WIRELESS_STREAM = "wirelessStream";
    public static final String STREAM_TYPE = "streamType";
    public static final String MQTT_SERVER = "mqttServer";
    public static final String MQTT_TOPIC = "mqttTopic";
    public static final String MQTT_DISCOVERY_TOPIC = "mqttDiscoveryTopic";
    public static final String MQTT_USERNAME = "mqttUsername";
    public static final String MQTT_PWD = "mqttPwd";
    // --- misc section fields ---
    public static final String EFFECT = "effect";
    public static final String COLOR_MODE = "colorMode";
    public static final String GAMMA = "gamma";
    public static final String WHITE_TEMPERATURE = "whiteTemperature";
    public static final String BRIGHTNESS = "brightness";
    public static final String NIGHT_MODE_FROM = "nightModeFrom";
    public static final String NIGHT_MODE_TO = "nightModeTo";
    public static final String NIGHT_MODE_BRIGHTNESS = "nightModeBrightness";
    public static final String TOGGLE_LED = "toggleLed";
    public static final String START_WITH_SYSTEM = "startWithSystem";
    public static final String RUNTIME_LOG_LEVEL = "runtimeLogLevel";
    public static final String CUBE_LUT = "cubeLut";
    public static final String THEME = "theme";
    // --- eye-care section fields ---
    public static final String NIGHT_LIGHT = "nightLight";
    public static final String NIGHT_LIGHT_LVL = "nightLightLvl";
    public static final String LUMINOSITY_THRESHOLD = "luminosityThreshold";
    public static final String BRIGHTNESS_LIMITER = "brightnessLimiter";
    public static final String ENABLE_AUTOMATIC_GAMMA = "enableAutomaticGamma";
    public static final String GAMMA_LEVEL = "gammaLevel";
    public static final String ENABLE_LDR = "enableLDR";
    public static final String LDR_INTERVAL = "ldrInterval";
    public static final String LDR_MIN = "ldrMin";
    public static final String LDR_TURN_OFF = "ldrTurnOff";
    // --- device section fields ---
    public static final String POWER_SAVING = "powerSaving";
    public static final String MULTI_MONITOR = "multiMonitor";
    public static final String MULTI_SCREEN_SINGLE_DEVICE = "multiScreenSingleDevice";
    public static final String CHECK_FOR_UPDATES = "checkForUpdates";
    public static final String SYNC_CHECK = "syncCheck";
    // --- profile section fields ---
    public static final String CHECK_FULL_SCREEN = "checkFullScreen";
    public static final String GPU_THRESHOLD = "gpuThreshold";
    public static final String CPU_THRESHOLD = "cpuThreshold";
    public static final String PROFILE_PROCESS_1 = "profileProcess1";
    public static final String PROFILE_PROCESS_2 = "profileProcess2";
    public static final String PROFILE_PROCESS_3 = "profileProcess3";
    // --- section / sub-accordion titles ---
    public static final String SECTION_LEDS = "leds";
    public static final String SECTION_MODE = "mode";
    public static final String SECTION_NETWORK = "network";
    public static final String SECTION_MISC = "misc";
    public static final String SECTION_DEVICES = "devices";
    public static final String SECTION_LDR = "ldr";
    public static final String SECTION_DISPLAY = "display";
    public static final String SECTION_COLOR_CORR = "colorCorr";
    public static final String SECTION_EYE_CARE = "eyeCare";
    public static final String SECTION_GAMMA = "gamma";
    public static final String SECTION_PROFILE = "profile";
    public static final String SECTION_SMOOTHING = "smoothing";
    public static final String SECTION_CONNECTED_DEVICES = "connectedDevices";
    public static final String SECTION_SATELLITES = "satellites";
    // --- client widget ids ---
    public static final String EFFECT_SELECT = "effectSelect";
    public static final String TURN_LED_ON = "turnLedOn";
    public static final String TURN_LED_OFF = "turnLedOff";
    // --- device /prefs JSON keys ---
    public static final String PREFS_EFFECT = "effect";
    public static final String PREFS_FF_EFFECT = "ffeffect";
    private WebFieldNames() {
    }
}
