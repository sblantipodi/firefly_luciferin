/*
  Constants.java

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
package org.dpsoftware.config;

/**
 * Constants and Strings
 */
public class Constants {

	// Enums
	public enum OsKind {
		WINDOWS,
		LINUX,
		MAC,
		OTHER
	}
	public enum PlayerStatus {
		PLAY,
		PLAY_WAITING,
		STOP,
		GREY
	}
	public enum FirmwareType {
		LIGHT,
		FULL
	}
	public enum Orientation implements LocalizedEnum {
		CLOCKWISE	 	("enum.orientation.clockwise"),
		ANTICLOCKWISE	("enum.orientation.anticlockwise");
		private final String orientation;
		Orientation(String orientation) {
			this.orientation = orientation;
		}
		public String getValue(){
			return orientation;
		}
	}
	public enum AspectRatio implements LocalizedEnum {
		FULLSCREEN	("enum.aspect.ratio.fullscreen"),
		LETTERBOX	("enum.aspect.ratio.letterbox"),
		PILLARBOX 	("enum.aspect.ratio.pillarbox");
		private final String aspectRatio;
		AspectRatio(String aspectRatio) {
			this.aspectRatio = aspectRatio;
		}
		public String getValue(){
			return aspectRatio;
		}
	}
	public enum LedOffset implements LocalizedEnum {
		BOTTOM_LEFT	  ("enum.led.offset.bottom.left"),
		BOTTOM_CENTER ("enum.led.offset.bottom.center"),
		BOTTOM_RIGHT  ("enum.led.offset.bottom.right"),
		UPPER_RIGHT   ("enum.led.offset.upper.right"),
		UPPER_LEFT    ("enum.led.offset.upper.left");
		private final String ledOffset;
		LedOffset(String ledOffset) {
			this.ledOffset = ledOffset;
		}
		public String getValue(){
			return ledOffset;
		}
	}
	public enum Effect implements LocalizedEnum {
		BIAS_LIGHT				 ("enum.effect.bias.light"),
		MUSIC_MODE_VU_METER 	 ("enum.effect.mm.vumeter"),
		MUSIC_MODE_VU_METER_DUAL ("enum.effect.mm.dual.vumeter"),
		MUSIC_MODE_BRIGHT		 ("enum.effect.mm.screencapture"),
		MUSIC_MODE_RAINBOW		 ("enum.effect.mm.rainbow"),
		SOLID 					 ("enum.effect.solid"),
		FIRE 					 ("enum.effect.fire"),
		TWINKLE 				 ("enum.effect.twinkle"),
		BPM 					 ("enum.effect.bpm"),
		RAINBOW					 ("enum.effect.rainbox"),
		MIXED_RAINBOW 			 ("enum.effect.mixedrainbox"),
		CHASE_RAINBOW			 ("enum.effect.chaserainbox"),
		SOLID_RAINBOW   		 ("enum.effect.solidrainbow");
		private final String effect;
		Effect(String effect) {
			this.effect = effect;
		}
		public String getValue(){
			return effect;
		}
	}
	public enum BaudRate {
		BAUD_RATE_230400	("230400"),
		BAUD_RATE_460800	("460800"),
		BAUD_RATE_500000	("500000"),
		BAUD_RATE_921600	("921600"),
		BAUD_RATE_1000000	("1000000"),
		BAUD_RATE_1500000	("1500000"),
		BAUD_RATE_2000000	("2000000");
		private final String baudRate;
		BaudRate(String baudRate) {
			this.baudRate = baudRate;
		}
		public String getBaudRate(){
			return baudRate;
		}
	}
	public enum Framerate implements LocalizedEnum {
		FPS_5  		("enum.framerate.5.fps"),
		FPS_10 		("enum.framerate.10.fps"),
		FPS_15 		("enum.framerate.15.fps"),
		FPS_20 		("enum.framerate.20.fps"),
		FPS_25 		("enum.framerate.25.fps"),
		FPS_30 		("enum.framerate.30.fps"),
		FPS_40 		("enum.framerate.40.fps"),
		FPS_50 		("enum.framerate.50.fps"),
		FPS_60 		("enum.framerate.60.fps"),
		FPS_90 		("enum.framerate.90.fps"),
		FPS_120  	("enum.framerate.120.fps"),
		FPS_144  	("enum.framerate.144.fps"),
		UNLOCKED  	("enum.framerate.unlocked");
		private final String framerate;
		Framerate(String framerate) {
			this.framerate = framerate;
		}
		public String getValue(){
			return framerate;
		}
	}
	public enum ScalingRatio {
		RATIO_100 	("100%"),
		RATIO_125 	("125%"),
		RATIO_150 	("150%"),
		RATIO_175 	("175%"),
		RATIO_200 	("200%"),
		RATIO_225 	("225%"),
		RATIO_250 	("250%"),
		RATIO_300 	("300%"),
		RATIO_350 	("350%");
		private final String scalingRatio;
		ScalingRatio(String scalingRatio) {
			this.scalingRatio = scalingRatio;
		}
		public String getScalingRatio(){
			return scalingRatio;
		}
	}
	public enum Gamma {
		GAMMA_10 	("1.0"),
		GAMMA_18 	("1.8"),
		GAMMA_20 	("2.0"),
		GAMMA_22 	("2.2"),
		GAMMA_24 	("2.4"),
		GAMMA_40 	("4.0"),
		GAMMA_50 	("5.0"),
		GAMMA_60 	("6.0"),
		GAMMA_80 	("8.0"),
		GAMMA_100 	("10.0");
		private final String gamma;
		Gamma(String gamma) {
			this.gamma = gamma;
		}
		public String getGamma(){
			return gamma;
		}
	}
	public enum AudioChannels implements LocalizedEnum {
		AUDIO_CHANNEL_1	("enum.audio.1.channel"),
		AUDIO_CHANNEL_2	("enum.audio.2.channel"),
		AUDIO_CHANNEL_3	("enum.audio.3.channel"),
		AUDIO_CHANNEL_4	("enum.audio.4.channel"),
		AUDIO_CHANNEL_5	("enum.audio.5.channel"),
		AUDIO_CHANNEL_6	("enum.audio.6.channel"),
		AUDIO_CHANNEL_7	("enum.audio.7.channel"),
		AUDIO_CHANNEL_8	("enum.audio.8.channel"),
		AUDIO_CHANNEL_9	("enum.audio.9.channel");
		private final String audioChannel;
		AudioChannels(String audioChannel) {
			this.audioChannel = audioChannel;
		}
		public String getValue(){
			return audioChannel;
		}
	}
	public enum WhiteTemperature implements LocalizedEnum {
		UNCORRECTEDTEMPERATURE  ("enum.white.temp.uncorrectedtemp"),
		KELVIN_1900				("enum.white.temp.1900k"),
		KELVIN_2600  			("enum.white.temp.2600k"),
		KELVIN_2850 			("enum.white.temp.2850k"),
		KELVIN_3200 			("enum.white.temp.3200k"),
		KELVIN_5200  			("enum.white.temp.5200k"),
		KELVIN_5400  			("enum.white.temp.5400k"),
		KELVIN_6000  			("enum.white.temp.6000k"),
		KELVIN_7000  			("enum.white.temp.7000k"),
		KELVIN_20000  			("enum.white.temp.20000k"),
		WARMFLUORESCENT			("enum.white.temp.warmfluorescent"),
		STANDARDFLUORESCENT  	("enum.white.temp.standard.fluorescent"),
		COOLWHITEFLUORESCENT 	("enum.white.temp.cool.white.fluorescent"),
		FULLSPECTRUMFLUORESCENT ("enum.white.temp.full.spectrum.fluorescent"),
		GROWLIGHTFLUORESCENT  	("enum.white.temp.grow.light.fluorescent"),
		BLACKLIGHTFLUORESCENT  	("enum.white.temp.black.light.fluorescent"),
		MERCURYVAPOR  			("enum.white.temp.mercury.vapor"),
		SODIUMVAPOR  			("enum.white.temp.sodium.sapor"),
		METALHALIDE  			("enum.white.temp.metal.halide"),
		HIGHPRESSURESODIUM  	("enum.white.temp.high.pressure.sodium");
		private final String whiteTemperature;
		WhiteTemperature(String whiteTemperature) {
			this.whiteTemperature = whiteTemperature;
		}
		public String getValue(){
			return whiteTemperature;
		}
	}
	public enum PowerSaving implements LocalizedEnum {
		DISABLED 	("enum.power.saving.disabled"),
		MINUTES_5 	("enum.power.saving.5.minutes"),
		MINUTES_10 	("enum.power.saving.10.minutes"),
		MINUTES_15 	("enum.power.saving.15.minutes"),
		MINUTES_20 	("enum.power.saving.20.minutes"),
		MINUTES_25 	("enum.power.saving.25.minutes"),
		MINUTES_30 	("enum.power.saving.30.minutes"),
		MINUTES_35 	("enum.power.saving.35.minutes"),
		MINUTES_40 	("enum.power.saving.40.minutes"),
		MINUTES_45 	("enum.power.saving.45.minutes"),
		MINUTES_50 	("enum.power.saving.50.minutes"),
		MINUTES_55 	("enum.power.saving.55.minutes"),
		MINUTES_60 	("enum.power.saving.60.minutes");
		private final String powerSaving;
		PowerSaving(String powerSaving) {
			this.powerSaving = powerSaving;
		}
		public String getValue(){
			return powerSaving;
		}
	}
	public enum Theme implements LocalizedEnum {
		DEFAULT 	("enum.theme.classic"),
		DARK_THEME	("enum.theme.dark");
		private final String theme;
		Theme(String theme) {
			this.theme = theme;
		}
		public String getValue(){
			return theme;
		}
	}
	public enum Language implements LocalizedEnum {
		EN 	("enum.language.en"),
		DE	("enum.language.de"),
		ES	("enum.language.es"),
		FR	("enum.language.fr"),
		IT	("enum.language.it"),
		RU	("enum.language.ru");
		private final String language;
		Language(String language) {
			this.language = language;
		}
		public String getValue(){
			return language;
		}
	}
	public enum StreamType {
		UDP		("UDP stream"),
		MQTT	("MQTT stream");
		private final String streamType;
		StreamType(String streamType) {
			this.streamType = streamType;
		}
		public String getStreamType(){
			return streamType;
		}
	}
	public enum Audio implements LocalizedEnum {
		DEFAULT_AUDIO_OUTPUT 		("enum.default.audio.output"),
		DEFAULT_AUDIO_OUTPUT_WASAPI ("enum.default.audio.output.wasapi"),
		DEFAULT_AUDIO_OUTPUT_NATIVE ("enum.default.audio.output.native");
		private final String defaultAudio;
		Audio(String defaultAudio) {
			this.defaultAudio = defaultAudio;
		}
		public String getValue(){
			return defaultAudio;
		}
	}

	// Misc
	public static final String BAUD_RATE_PLACEHOLDER = "BAUD_RATE_";
	public static final String FIREFLY_LUCIFERIN = "Firefly Luciferin";
	public static final String DEFAULT_BAUD_RATE = BaudRate.BAUD_RATE_500000.getBaudRate();
	public static final String SPAWNING_ROBOTS = "log.spawning.robots";
	public static final String SERIAL_PORT_IN_USE = "log.serial.port";
	public static final String TURN_LED_ON = "controller.turn.led.on";
	public static final String TURN_LED_OFF = "controller.turn.led.off";
	public static final String DEFAULT_COLOR_CHOOSER = "255,255,255,255";
	public static final String CLEAN_EXIT = "CLEAN EXIT";
	public static final int SERIAL_CHUNK_SIZE = 250;
	public static final String DATE_FORMAT = "EEEE, MMM dd, yyyy HH:mm:ss a";
	public static final String SETTING_LED_SERIAL = "Setting LEDs";
	public static final int NUMBER_OF_BENCHMARK_ITERATION = 15;
	public static final int BENCHMARK_ERROR_MARGIN = 3;
	public static final String MULTIMONITOR_1 = "multimonitor.disabled";
	public static final String MULTIMONITOR_2 = "multimonitor.dual";
	public static final String MULTIMONITOR_3 = "multimonitor.triple";
	public static final String LEFT_DISPLAY = "fxml.ledsconfigtab.leftdisplay";
	public static final String CENTER_DISPLAY = "fxml.ledsconfigtab.centerdisplay";
	public static final String RIGHT_DISPLAY = "fxml.ledsconfigtab.rightdisplay";
	public static final String MAIN_DISPLAY = "fxml.ledsconfigtab.maindisplay";
	public static final String CSS_CLASS_BOLD = "bold";
	public static final String CSS_CLASS_RED = "red";
	public static final String AUTO_DETECT_BLACK_BARS = "autodetect.black.bars";
	public static final int DEEP_BLACK_CHANNEL_TOLERANCE = 6;
	public static final String CONTEXT_MENU_COLOR = "context.menu.color";
	public static final String CONTEXT_MENU_GAMMA = "context.menu.gamma";
	public static final String CONTEXT_MENU_AUDIO_DEVICE = "context.menu.audio.device";
	public static final String CONTEXT_MENU_AUDIO_GAIN = "context.menu.audio.gain";
	public static final String NUMBER_FORMAT = "########.##";
	public static final String NIGHT_MODE_OFF = "0%";

	// Upgrade
	public static final String LIGHT_FIRMWARE_DUMMY_VERSION = "1.0.0";
	public static final String MINIMUM_FIRMWARE_FOR_AUTO_UPGRADE = "4.0.3";
	public static final String MIN_FIRMWARE_NOT_MATCH ="min.firmware.not.match";
	public static final String GITHUB_POM_URL = "https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/pom.xml";
	public static final String GITHUB_GLOW_WORM_URL = "https://raw.githubusercontent.com/sblantipodi/glow_worm_luciferin/master/version";
	public static final String POM_PRJ_VERSION = "<project.version>";
	public static final String POM_PRJ_VERSION_CLOSE = "</project.version>";
	public static final String DOWNLOADING = "update.downloading";
	public static final String SETUP_FILENAME_WINDOWS = "FireflyLuciferinSetup.exe";
	public static final String SETUP_FILENAME_MAC = "FireflyLuciferinMac.dmg";
	public static final String SETUP_FILENAME_LINUX_DEB = "FireflyLuciferinLinux.deb";
	public static final String SETUP_FILENAME_LINUX_RPM = "FireflyLuciferinLinux.rpm";
	public static final String GITHUB_RELEASES = "https://github.com/sblantipodi/firefly_luciferin/releases/download/v";
	public static final String GITHUB_RELEASES_FIRMWARE = "https://github.com/sblantipodi/glow_worm_luciferin/releases/download/v";
	public static final String LINUX_DESKTOP_FILE = "/usr/share/applications/fireflyluciferin-FireflyLuciferin.desktop";
	public static final String LINUX_DESKTOP_FILE_LOCAL = "/.local/share/applications/fireflyluciferin-FireflyLuciferin.desktop";
	public static final String STARTUP_WMCLASS = "StartupWMClass=org.dpsoftware.FireflyLuciferin";
	public static final String HOME_PATH = "user.home";
	public static final String DOCUMENTS_FOLDER = "Documents";
	public static final String LUCIFERIN_PLACEHOLDER = "FireflyLuciferin";
	public static final String LUCIFERIN_FOLDER = "FireflyLuciferin";
	public static final String EXPECTED_SIZE = "update.expected.size";
	public static final String DOWNLOAD_PROGRESS_BAR = "download.progress.bar";
	public static final String DOWNLOAD_COMPLETE = "download.complete";
	public static final String UPGRADE_CONTENT_TYPE = "Content-Type";
	public static final String HTTP_RESPONSE = "application/json";
	public static final String TCP_CLIENT = "TcpClient";
	public static final String HTTP_URL = "http://{0}/{1}?payload={2}";
	public static final String UPGRADE_MULTIPART = "multipart/form-data;boundary=";
	public static final String UPGRADE_URL = "http://{0}/update";
	public static final String MULTIPART_1  = "--{0}\r\nContent-Disposition: form-data; name=";
	public static final String MULTIPART_2  = "\"file\"; filename=\"{0}\"\r\nContent-Type: " + "application/octet-stream" + "\r\n\r\n";
	public static final String MULTIPART_4  = ("\r\n");
	public static final String MULTIPART_5  = (("--{0}--"));
	public static final String PROP_MINIMUM_FIRMWARE_VERSION = "minimum.firmware.version";

	// Properties
	public static final String PROPERTIES_FILENAME = "project.properties";
	public static final String PROP_VERSION = "version";
	public static final String MSG_BUNDLE = "messagebundle";

	// Native executor
	public static final String CANT_RUN_CMD = "cant.run.cmd";
	public static final String NO_OUTPUT = "no.output";
	public static final String INTERRUPTED_WHEN_READING = "interrupted.when.reading";
	public static final String DPKG_CHECK_CMD = "dpkg --version";

	// Resources
	public static final String IMAGE_TRAY_PLAY = "/org/dpsoftware/gui/img/tray_play.png";
	public static final String IMAGE_TRAY_PLAY_CENTER = "/org/dpsoftware/gui/img/tray_play_center.png";
	public static final String IMAGE_TRAY_PLAY_LEFT = "/org/dpsoftware/gui/img/tray_play_left.png";
	public static final String IMAGE_TRAY_PLAY_RIGHT = "/org/dpsoftware/gui/img/tray_play_right.png";
	public static final String IMAGE_TRAY_PLAY_RIGHT_GOLD = "/org/dpsoftware/gui/img/tray_play_right_gold.png";
	public static final String IMAGE_TRAY_PLAY_WAITING = "/org/dpsoftware/gui/img/tray_play_waiting.png";
	public static final String IMAGE_TRAY_PLAY_WAITING_CENTER = "/org/dpsoftware/gui/img/tray_play_waiting_center.png";
	public static final String IMAGE_TRAY_PLAY_WAITING_LEFT = "/org/dpsoftware/gui/img/tray_play_waiting_left.png";
	public static final String IMAGE_TRAY_PLAY_WAITING_RIGHT = "/org/dpsoftware/gui/img/tray_play_waiting_right.png";
	public static final String IMAGE_TRAY_PLAY_WAITING_RIGHT_GOLD = "/org/dpsoftware/gui/img/tray_play_waiting_right_gold.png";
	public static final String IMAGE_TRAY_STOP = "/org/dpsoftware/gui/img/tray_stop.png";
	public static final String IMAGE_TRAY_STOP_CENTER = "/org/dpsoftware/gui/img/tray_stop_center.png";
	public static final String IMAGE_TRAY_STOP_LEFT = "/org/dpsoftware/gui/img/tray_stop_left.png";
	public static final String IMAGE_TRAY_STOP_RIGHT = "/org/dpsoftware/gui/img/tray_stop_right.png";
	public static final String IMAGE_TRAY_STOP_RIGHT_GOLD = "/org/dpsoftware/gui/img/tray_stop_right_gold.png";
	public static final String IMAGE_TRAY_GREY = "/org/dpsoftware/gui/img/tray_stop_grey.png";
	public static final String IMAGE_TRAY_GREY_CENTER = "/org/dpsoftware/gui/img/tray_stop_grey_center.png";
	public static final String IMAGE_TRAY_GREY_LEFT = "/org/dpsoftware/gui/img/tray_stop_grey_left.png";
	public static final String IMAGE_TRAY_GREY_RIGHT = "/org/dpsoftware/gui/img/tray_stop_grey_right.png";
	public static final String IMAGE_TRAY_GREY_RIGHT_GOLD = "/org/dpsoftware/gui/img/tray_stop_grey_right_gold.png";
	public static final String IMAGE_CONTROL_GREY = "/org/dpsoftware/gui/img/luciferin_logo_grey.png";
	public static final String IMAGE_CONTROL_GREY_CENTER = "/org/dpsoftware/gui/img/luciferin_logo_grey_center.png";
	public static final String IMAGE_CONTROL_GREY_LEFT = "/org/dpsoftware/gui/img/luciferin_logo_grey_left.png";
	public static final String IMAGE_CONTROL_GREY_RIGHT = "/org/dpsoftware/gui/img/luciferin_logo_grey_right.png";
	public static final String IMAGE_CONTROL_GREY_RIGHT_GOLD = "/org/dpsoftware/gui/img/luciferin_logo_grey_right_gold.png";
	public static final String IMAGE_CONTROL_PLAY = "/org/dpsoftware/gui/img/luciferin_logo_play.png";
	public static final String IMAGE_CONTROL_PLAY_CENTER = "/org/dpsoftware/gui/img/luciferin_logo_play_center.png";
	public static final String IMAGE_CONTROL_PLAY_LEFT = "/org/dpsoftware/gui/img/luciferin_logo_play_left.png";
	public static final String IMAGE_CONTROL_PLAY_RIGHT = "/org/dpsoftware/gui/img/luciferin_logo_play_right.png";
	public static final String IMAGE_CONTROL_PLAY_RIGHT_GOLD = "/org/dpsoftware/gui/img/luciferin_logo_play_right_gold.png";
	public static final String IMAGE_CONTROL_PLAY_WAITING = "/org/dpsoftware/gui/img/luciferin_logo_play_waiting.png";
	public static final String IMAGE_CONTROL_PLAY_WAITING_CENTER = "/org/dpsoftware/gui/img/luciferin_logo_play_waiting_center.png";
	public static final String IMAGE_CONTROL_PLAY_WAITING_LEFT = "/org/dpsoftware/gui/img/luciferin_logo_play_waiting_left.png";
	public static final String IMAGE_CONTROL_PLAY_WAITING_RIGHT = "/org/dpsoftware/gui/img/luciferin_logo_play_waiting_right.png";
	public static final String IMAGE_CONTROL_PLAY_WAITING_RIGHT_GOLD = "/org/dpsoftware/gui/img/luciferin_logo_play_waiting_right_gold.png";
	public static final String IMAGE_CONTROL_LOGO = "/org/dpsoftware/gui/img/luciferin_logo.png";
	public static final String IMAGE_CONTROL_LOGO_CENTER = "/org/dpsoftware/gui/img/luciferin_logo_center.png";
	public static final String IMAGE_CONTROL_LOGO_LEFT = "/org/dpsoftware/gui/img/luciferin_logo_left.png";
	public static final String IMAGE_CONTROL_LOGO_RIGHT = "/org/dpsoftware/gui/img/luciferin_logo_right.png";
	public static final String IMAGE_CONTROL_LOGO_RIGHT_GOLD = "/org/dpsoftware/gui/img/luciferin_logo_right.png";
	public static final String FXML = ".fxml";
	public static final String FXML_SETTINGS = "settings";
	public static final String FXML_INFO = "info";
	public static final String CONFIG_FILENAME = "FireflyLuciferin.yaml";
	public static final String CONFIG_FILENAME_2 = "FireflyLuciferin_2.yaml";
	public static final String CONFIG_FILENAME_3 = "FireflyLuciferin_3.yaml";
	public static final String ALREADY_EXIST = "already.exist";
	public static final String WAS_CREATED = "was.created";
	public static final String WAS_NOT_CREATED = "was.not.created";
	public static final String CLEANING_OLD_CONFIG = "cleaning.old.config";
	public static final String FAILED_TO_CLEAN_CONFIG = "failed.to.clean.old.config";
	public static final String CONFIG_OK = "config.ok";
	public static final String OK = "OK";
	public static final String KO = "KO";
	public static final String FIRMWARE_UPGRADE_RES = "firmware.upgrade.res";
	public static final String ERROR_READING_CONFIG = "error.reading.config";

	// MQTT (topic are used even when using WiFi only)
	public static final boolean JSON_STREAM = false;
	public static final String STATE_ON_GLOWWORM = "GlowWorm";
	public static final String STATE_ON_GLOWWORMWIFI = "GlowWormWifi";
	public static final String DEFAULT_MQTT_HOST = "tcp://192.168.1.3";
	public static final String DEFAULT_MQTT_PORT = "1883";
	public static final String DEFAULT_MQTT_TOPIC = "lights/glowwormluciferin/set";
	public static final String DEFAULT_MQTT_STATE_TOPIC = "lights/glowwormluciferin";
	public static final String UPDATE_MQTT_TOPIC = "lights/glowwormluciferin/update";
	public static final String FPS_TOPIC = "lights/glowwormluciferin/fps";
	public static final String UPDATE_RESULT_MQTT_TOPIC = "lights/glowwormluciferin/update/result";
	public static final String FIREFLY_LUCIFERIN_FRAMERATE = "lights/firelyluciferin/framerate";
	public static final String FIREFLY_LUCIFERIN_GAMMA = "lights/firelyluciferin/gamma";
	public static final String GLOW_WORM_FIRM_CONFIG_TOPIC = "lights/glowwormluciferin/firmwareconfig";
	public static final String UNSUBSCRIBE_STREAM_TOPIC = "lights/glowwormluciferin/unsubscribe";
	public static final String ASPECT_RATIO_TOPIC = "lights/firelyluciferin/aspectratio";
	public static final String STATE_IP = "IP";
	public static final String WIFI = "wifi";
	public static final String DEVICE_VER = "ver";
	public static final String DEVICE_BOARD = "board";
	public static final String MQTT_TOPIC = "mqttopic";
	public static final String NUMBER_OF_LEDS = "lednum";
	public static final String BAUD_RATE = "baudrate";
	public static final String MAC = "MAC";
	public static final String GPIO = "gpio";
	public static final String STATE = "state";
	public static final String RUNNING = "running";
	public static final String DEVICE_TABLE_DATA = "deviceTableData";
	public static final String FPS_GW_CONSUMER = "fpsgwconsumer";
	public static final String TRUE = "true";
	public static final String ON = "ON";
	public static final String OFF = "OFF";
	public static final String EFFECT = "effect";
	public static final String SOLID = "solid";
	public static final String COLOR = "color";
	public static final String MQTT_BRIGHTNESS = "brightness";
	public static final String MQTT_DISABLED = "MQTT disabled.";
	public static final String MQTT_DEVICE_NAME_WIN = "FireflyLuciferin";
	public static final String MQTT_DEVICE_NAME_LIN = "FireflyLuciferinLinux";
	public static final String MQTT_DEVICE_NAME_MAC = "FireflyLuciferinMac";
	public static final String MQTT_CONNECTED = "Connected to MQTT Server";
	public static final String MQTT_CANT_SEND = "Cant't send MQTT msg";
	public static final String MQTT_STREAM_TOPIC = "/stream";
	public static final String MQTT_RECONNECTED = "Reconnected";
	public static final String MQTT_DISCONNECTED = "Disconnected";
	public static final String MQTT_START = "START";
	public static final String MQTT_STOP = "STOP";
	public static final String MQTT_TOPIC_FRAMERATE = "framerate";
	public static final String MQTT_DEVICE_NAME = "deviceName";
	public static final int FIRST_CHUNK = 170;
	public static final int SECOND_CHUNK = 340;
	public static final int THIRD_CHUNK = 510;
	public static final int MAX_CHUNK = 510;
	public static final String LED_NUM = "\"lednum\":";
	public static final String STREAM = "\"stream\":[";
	public static final String MQTT_GAMMA = "gamma";
	public static final String MQTT_SET = "set";
	public static final String MQTT_EMPTY = "empty";
	public static final String MQTT_UPDATE = "update";
	public static final String MQTT_FPS = "fps";
	public static final String MQTT_UPDATE_RES = "update/result";
	public static final String MQTT_FRAMERATE = "framerate";
	public static final String MQTT_FIRMWARE_CONFIG = "firmwareconfig";
	public static final String MQTT_UNSUBSCRIBE = "unsubscribe";
	public static final String MQTT_BASE_TOPIC = "glowwormluciferin";
	public static final String MQTT_FIREFLY_BASE_TOPIC = "firelyluciferin";
	public static final String START_STOP_INSTANCES = "startStopInstances";

	// GUI
	public static final String SAVE = "fxml.save";
	public static final String SAVE_AND_CLOSE = "fxml.save.and.close";
	public static final String FRAMERATE_TITLE = "framerate.title";
	public static final String FRAMERATE_HEADER = "framerate.header";
	public static final String FRAMERATE_CONTEXT = "framerate.context";
	public static final String GPIO_TITLE = "gpio.title";
	public static final String GPIO_HEADER = "gpio.header";
	public static final String GPIO_CONTEXT = "gpio.context";
	public static final String BAUDRATE_TITLE = "baudrate.title";
	public static final String BAUDRATE_HEADER = "baudrate.header";
	public static final String BAUDRATE_CONTEXT = "baudrate.context";
	public static final String GPIO_OK_TITLE = "gpio.ok.title";
	public static final String GPIO_OK_HEADER = "gpio.ok.header";
	public static final String GPIO_OK_CONTEXT = "gpio.ok.context";
	public static final String SERIAL_PORT = "serial.port";
	public static final String OUTPUT_DEVICE = "fxml.modetab.outputdevice";
	public static final String SERIAL_ERROR_TITLE = "serial.port.title";
	public static final String SERIAL_ERROR_HEADER = "serial.error.header";
	public static final String SERIAL_ERROR_OPEN_HEADER = "serial.port.open.header";
	public static final String SERIAL_PORT_AMBIGUOUS = "serial.port.ambiguos";
	public static final String SERIAL_PORT_AMBIGUOUS_CONTEXT = "serial.port.ambiguos.context";
	public static final String MQTT_ERROR_TITLE = "mqtt.error.title";
	public static final String MQTT_ERROR_HEADER = "mqtt.error.header";
	public static final String MQTT_ERROR_CONTEXT = "mqtt.error.context";
	public static final String START = "tray.icon.start";
	public static final String STOP = "tray.icon.stop";
	public static final String INFO = "tray.icon.info";
	public static final String SETTINGS = "tray.icon.settings";
	public static final String EXIT = "exit";
	public static final String TRAY_EXIT = "tray.icon.exit";
	public static final String CLICK_OK_DOWNLOAD = "click.ok.download";
	public static final String CLICK_OK_DOWNLOAD_LINUX = "click.ok.download.linux";
	public static final String ONCE_DOWNLOAD_FINISHED = "once.download.finished";
	public static final String NEW_VERSION_AVAILABLE = "new.version.available";
	public static final String GITHUB_CHANGELOG = "https://sblantipodi.github.io/firefly_luciferin";
	public static final String UPGRADE_SUCCESS = "upgrade.success";
	public static final String DEVICEUPGRADE_SUCCESS = "device.upgrade.success";
	public static final String NEW_FIRMWARE_AVAILABLE = "new.firmware.available";
	public static final String CANT_UPGRADE_TOO_OLD = "cant.upgrade.too.old";
	public static final String MANUAL_UPGRADE = "manual.upgrade";
	public static final String DEVICES_UPDATED = "devices.updated";
	public static final String DEVICE_UPDATED = "device.updated";
	public static final String DEVICE_UPDATED_LIGHT = "device.updated.light";
	public static final String UPDATE_BACKGROUND = "update.background";
	public static final String UPDATE_NEEDED = "update.needed";
	public static final String UPDATE_NEEDED_LINUX = "update.needed.linux";
	public static final String CAPTURE_MODE_CHANGED = "capture.mode.changed";
	public static final String GITHUB_URL = "https://github.com/sblantipodi/firefly_luciferin";
	public static final String WEB_INSTALLER_URL = "https://sblantipodi.github.io/glow_worm_luciferin";
	public static final String SERIAL_PORT_AUTO = "AUTO";
	public static final String SERIAL_PORT_COM = "COM";
	public static final String SERIAL_PORT_TTY = "/dev/ttyUSB";
	public static final String PERCENT = "%";
	public static final String GAMMA_DEFAULT = "2.2";
	public static final String USB_DEVICE = "USB device";
	public static final String ESP8266 = "ESP8266";
	public static final String ESP32 = "ESP32";
	public static final String DASH = "-";
	public static final String UPDATE_FILENAME = "GlowWormLuciferinFULL_board_firmware.bin";
	public static final String UPDATE_FILENAME_LIGHT = "GlowWormLuciferinLIGHT_board_firmware.bin";
	public static final String SERIAL_VERSION = "ver:";
	public static final String SERIAL_LED_NUM = "lednum:";
	public static final String SERIAL_BOARD = "board:";
	public static final String SERIAL_FRAMERATE = "framerate:";
	public static final String SERIAL_FIRMWARE = "firmware:";
	public static final String SERIAL_BAUDRATE = "baudrate:";
	public static final String SERIAL_MQTTTOPIC = "mqttopic:";
	public static final String SERIAL_MAC = "MAC:";
	public static final String SERIAL_GPIO = "gpio:";
	public static final String NO_DEVICE_FOUND = "no.device.found";
	public static final int FAKE_GUI_TRAY_ICON = -100;
	public static final String SCREEN_MAIN = "main.screen";
	public static final String SCREEN_LEFT = "left.screen";
	public static final String SCREEN_RIGHT = "right.screen";
	public static final String SCREEN_CENTER = "center.screen";

	// Tooltips
	public static final String TOOLTIP_TOPLED = "tooltip.topled";
	public static final String TOOLTIP_LEFTLED = "tooltip.leftled";
	public static final String TOOLTIP_RIGHTLED = "tooltip.rightled";
	public static final String TOOLTIP_BOTTOMLEFTLED = "tooltip.bottomleftled";
	public static final String TOOLTIP_BOTTOMRIGHTLED = "tooltip.bottomrightled";
	public static final String TOOLTIP_BOTTOMROWLED = "tooltip.bottomrowled";
    public static final String TOOLTIP_ORIENTATION = "tooltip.orientation";
    public static final String TOOLTIP_SCREENWIDTH = "tooltip.screenwidth";
	public static final String TOOLTIP_SCREENHEIGHT = "tooltip.screenheight";
	public static final String TOOLTIP_LEDSTARTOFFSET = "tooltip.ledstartoffset";
	public static final String TOOLTIP_SCALING = "tooltip.scaling";
	public static final String TOOLTIP_WHITE_TEMP = "tooltip.white.temp";
    public static final String TOOLTIP_GAMMA = "tooltip.gamma";
    public static final String TOOLTIP_CAPTUREMETHOD = "tooltip.capturemethod";
	public static final String TOOLTIP_LINUXCAPTUREMETHOD = "tooltip.linuxcapturemethod";
	public static final String TOOLTIP_MACCAPTUREMETHOD = "tooltip.maccapturemethod";
    public static final String TOOLTIP_NUMBEROFTHREADS = "tooltip.numberofthreads";
    public static final String TOOLTIP_SERIALPORT = "tooltip.serialport";
	public static final String TOOLTIP_ASPECTRATIO = "tooltip.aspectratio";
	public static final String TOOLTIP_LANGUAGE = "tooltip.language";
	public static final String TOOLTIP_FRAMERATE = "tooltip.framerate";
	public static final String TOOLTIP_MQTTHOST = "tooltip.mqtthost";
	public static final String TOOLTIP_POWER_SAVING = "tooltip.power.saving";
	public static final String TOOLTIP_MULTIMONITOR = "tooltip.multimonitor";
	public static final String TOOLTIP_MONITORNUMBER = "tooltip.monitornumber";
    public static final String TOOLTIP_MQTTPORT = "tooltip.mqttport";
    public static final String TOOLTIP_MQTTTOPIC = "tooltip.mqtttopic";
    public static final String TOOLTIP_MQTTUSER = "tooltip.mqttuser";
    public static final String TOOLTIP_MQTTPWD = "tooltip.mqttpwd";
	public static final String TOOLTIP_MQTTENABLE = "tooltip.mqttenable";
	public static final String TOOLTIP_WIFIENABLE = "tooltip.wifienable";
	public static final String TOOLTIP_EYE_CARE = "tooltip.eye.care";
	public static final String TOOLTIP_MQTTSTREAM = "tooltip.mqttstream";
	public static final String TOOLTIP_STREAMTYPE = "tooltip.streamtype";
	public static final String TOOLTIP_START_WITH_SYSTEM = "tooltip.start.with.system";
	public static final String TOOLTIP_CHECK_UPDATES = "tooltip.check.updates";
	public static final String TOOLTIP_PLAYBUTTON_NULL = "tooltip.playbutton.null";
	public static final String TOOLTIP_SYNC_CHECK = "tooltip.sync.check";
	public static final String TOOLTIP_BRIGHTNESS = "tooltip.brightness";
	public static final String TOOLTIP_SPLIT_BOTTOM_ROW = "tooltip.split.bottom.row";
    public static final String TOOLTIP_SAVELEDBUTTON_NULL = "tooltip.saveledbutton.null";
    public static final String TOOLTIP_SAVEMQTTBUTTON_NULL = "tooltip.savemqttbutton.null";
	public static final String TOOLTIP_SAVESETTINGSBUTTON_NULL = "tooltip.savesettingsbutton.null";
	public static final String TOOLTIP_SAVEDEVICEBUTTON_NULL = "tooltip.savedevicebutton.null";
	public static final String TOOLTIP_PLAYBUTTON = "tooltip.playbutton";
	public static final String TOOLTIP_SAVELEDBUTTON = "tooltip.saveledbutton";
	public static final String TOOLTIP_SAVEMQTTBUTTON = "tooltip.savemqttbutton";
	public static final String TOOLTIP_SAVESETTINGSBUTTON = "tooltip.savesettingsbutton";
	public static final String TOOLTIP_SAVEDEVICEBUTTON = "tooltip.savedevicebutton";
	public static final String TOOLTIP_SHOWTESTIMAGEBUTTON = "tooltip.showtestimagebutton";
	public static final String TOOLTIP_BAUD_RATE = "tooltip.baud.rate";
	public static final String TOOLTIP_THEME = "tooltip.theme";
	public static final String TOOLTIP_AUDIO_CHANNELS = "tooltip.audio.channels";
	public static final String TOOLTIP_AUDIO_GAIN = "tooltip.audio.gain";
	public static final String TOOLTIP_AUDIO_DEVICE = "tooltip.audio.device";
	public static final String TOOLTIP_EFFECT = "tooltip.effect";
	public static final String TOOLTIP_COLORS = "tooltip.colors";
	public static final String TOOLTIP_NIGHT_MODE_FROM = "tooltip.night.mode.from";
	public static final String TOOLTIP_NIGHT_MODE_TO = "tooltip.night.mode.to";
	public static final String TOOLTIP_NIGHT_MODE_BRIGHT = "tooltip.night.mode.bright";

	// Grabber
	public static final String INTERNAL_SCALING_X = "INTERNAL_SCALING_X";
	public static final String INTERNAL_SCALING_Y = "INTERNAL_SCALING_Y";
	public static final int RESAMPLING_FACTOR = 8;
	public static final String EMIT_SIGNALS = "emit-signals";
	public static final String GSTREAMER_PIPELINE_DDUPL ="video/x-raw(memory:SystemMemory),width=INTERNAL_SCALING_X,height=INTERNAL_SCALING_Y,sync=false,";
	public static final String GSTREAMER_PIPELINE = "video/x-raw,width=INTERNAL_SCALING_X,height=INTERNAL_SCALING_Y,sync=false,";
	public static final String BYTE_ORDER_BGR = "format=BGRx";
	public static final String BYTE_ORDER_RGB = "format=xRGB";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String GSTREAMER_PATH = "/gstreamer/1.0/mingw_x86_64/bin";
	public static final String PATH = "path";
	public static final String JNA_LIB_PATH = "jna.library.path";
	public static final String JNA_GSTREAMER_PATH = "gstreamer.path";
	public static final String JNA_LIB_PATH_FOLDER = "/Library/Frameworks/GStreamer.framework/Libraries/";
	public static final String SCREEN_GRABBER = "ScreenGrabber";
	// ./gst-device-monitor-1.0.exe "Source/Monitor"
	// ./gst-launch-1.0 d3d11screencapturesrc monitor-handle=221948 ! d3d11convert ! d3d11download ! autovideosink
	public static final String GSTREAMER_PIPELINE_WINDOWS_HARDWARE_HANDLE = "d3d11screencapturesrc monitor-handle={0} ! d3d11convert ! d3d11download";
	public static final String GSTREAMER_PIPELINE_LINUX = "ximagesrc startx={0} endx={1} starty={2} endy={3} ! videoscale ! videoconvert";
	public static final String GSTREAMER_PIPELINE_MAC = "avfvideosrc capture-screen=true ! videoscale ! videoconvert";
	public static final String FRAMERATE_PLACEHOLDER = "framerate=FRAMERATE_PLACEHOLDER/1,";
	public static final int NUMBER_OF_AREA_TO_CHECK = 50;

	// Message server
	public static final String MSG_SERVER_HOST = "127.0.0.1";
	public static final int MSG_SERVER_PORT = 5555;
	public static final String MSG_SERVER_STATUS = "MSG_SERVER_STATUS";

	// Exceptions
	public static final String WIN32_EXCEPTION = "exceptions.win32.exception";
	public static final String SELECT_OBJ_EXCEPTION = "exceptions.select.obj";
	public static final String DELETE_OBJ_EXCEPTION = "exceptions.delete.obj";
	public static final String DELETE_DC_EXCEPTION = "exceptions.delete.dc";
	public static final String DEVICE_CONTEXT_RELEASE_EXCEPTION = "exceptions.device.context.release";
	public static final String WINDOWS_EXCEPTION = "exceptions.windows";
	public static final String CANT_FIND_GSTREAMER = "exceptions.cant.find.gsreamer";
	public static final String SOMETHING_WENT_WRONG = "exceptions.something.went.wrong";

	// Network
	public static final String ACTION = "action";
	public static final String CLIENT_ACTION = "clientActionSetState";

	// UDP
	public static final int UDP_PORT = 4210;
	public static final int UDP_BROADCAST_PORT = 5001;
	public static final int UDP_BROADCAST_PORT_2 = 5002;
	public static final int UDP_BROADCAST_PORT_3 = 5003;
	public static final int UDP_PORT_PREFERRED_OUTBOUND = 10002;
	public static final String UDP_IP_FOR_PREFERRED_OUTBOUND = "8.8.8.8";
	public static final String UDP_PING = "PING";
	public static final String UDP_PONG = "PONG";
	public static final double UDP_CHUNK_SIZE = 140;
	public static final int UDP_MAX_BUFFER_SIZE = 4096;
	public static final int UDP_MICROCONTROLLER_REST_TIME = 0;

	// Audio
	public static final String WASAPI = "WASAPI";
	public static final String LOOPBACK = "Loopback";
	public static final String MUSIC_MODE = "Music mode";
	public static final int DEFAULT_SAMPLE_RATE = 48000;
	public static final int DEFAULT_SAMPLE_RATE_NATIVE = 44100;

	// Image processor
	public static final String FAT_JAR_NAME = "FireflyLuciferin-jar-with-dependencies.jar";
	public static final String CLASSES = "classes";
	public static final String TARGET = "target";
	public static final String MAIN_RES = "src/main/resources";
	public static final String GSTREAMER_PATH_IN_USE = "GStreamer path in use=";

	// Info
	public static final String INFO_FRAMERATE = "Framerate (FPS)";
	public static final String INFO_WIFI_STRENGTH = "WiFi signal strength (%)";
	public static final String INFO_VERSION = "© Davide Perini (v.VERSION)";
	public static final String INFO_PRODUCING = "fxml.info.producing";
	public static final String INFO_CONSUMING = "fxml.info.consuming";
	public static final String INFO_WIFI = "WiFi: ";
	public static final String INFO_FPS = " FPS";

	// Windows Registry
	public static final String REGISTRY_KEY_PATH = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run\\";
	public static final String REGISTRY_KEY_NAME = "FireflyLuciferin";
	public static final String REGISTRY_KEY_VALUE_WINDOWS = "Firefly Luciferin.exe";
	public static final String REGISTRY_KEY_VALUE_LINUX = "bin/FireflyLuciferin";
	public static final String REGISTRY_DEFAULT_KEY_VALUE = "C:\\Users\\perin\\AppData\\Local\\Firefly Luciferin\\Firefly Luciferin.exe";
	public static final String REGISTRY_JARNAME_WINDOWS = "app\\FireflyLuciferin-jar-with-dependencies.jar";
	public static final String REGISTRY_JARNAME_LINUX = "lib/app/FireflyLuciferin-jar-with-dependencies.jar";

}