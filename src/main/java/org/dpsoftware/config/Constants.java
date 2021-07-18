/*
  Constants.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

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
	public enum AspectRatio {
		FULLSCREEN	("FullScreen"),
		LETTERBOX	("Letterbox"),
		PILLARBOX 	("Pillarbox");
		private final String aspectRatio;
		AspectRatio(String aspectRatio) {
			this.aspectRatio = aspectRatio;
		}
		public String getAspectRatio(){
			return aspectRatio;
		}
	}
	public enum Effect {
		BIAS_LIGHT			("Bias light"),
		MUSIC_MODE_VU_METER ("Music mode (VU Meter)"),
		MUSIC_MODE_BRIGHT	("Music mode (Screen capture)"),
		MUSIC_MODE_RAINBOW	("Music mode (Rainbow music)"),
		SOLID 				("Solid"),
		BPM 				("Bpm"),
        MIXED_RAINBOW 		("Mixed rainbow"),
        RAINBOW				("Rainbow"),
        SOLID_RAINBOW   	("Solid rainbow");
		private final String effect;
		Effect(String effect) {
			this.effect = effect;
		}
		public String getEffect(){
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
	public enum Framerate {
		FPS_5  		("5 FPS"),
		FPS_10 		("10 FPS"),
		FPS_15 		("15 FPS"),
		FPS_20 		("20 FPS"),
		FPS_25 		("25 FPS"),
		FPS_30 		("30 FPS"),
		FPS_40 		("40 FPS"),
		FPS_50 		("50 FPS"),
		FPS_60 		("60 FPS"),
		FPS_90 		("90 FPS"),
		FPS_120  	("120 FPS"),
		UNLOCKED  	("UNLOCKED");
		private final String framerate;
		Framerate(String framerate) {
			this.framerate = framerate;
		}
		public String getFramerate(){
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
	public enum AudioChannels {
		AUDIO_CHANNEL_1	("1 channel"),
		AUDIO_CHANNEL_2	("2 channels"),
		AUDIO_CHANNEL_3	("3 channels"),
		AUDIO_CHANNEL_4	("4 channels"),
		AUDIO_CHANNEL_5	("5 channels"),
		AUDIO_CHANNEL_6	("6 channels"),
		AUDIO_CHANNEL_7	("7 channels"),
		AUDIO_CHANNEL_8	("8 channels"),
		AUDIO_CHANNEL_9	("9 channels");
		private final String audioChannel;
		AudioChannels(String audioChannel) {
			this.audioChannel = audioChannel;
		}
		public String getAudioChannels(){
			return audioChannel;
		}
	}
	public enum WhiteTemperature {
		UNCORRECTEDTEMPERATURE  ("Uncorrected temperature"),
		KELVIN_1900				("1900 Kelvin"),
		KELVIN_2600  			("2600 Kelvin"),
		KELVIN_2850 			("2850 Kelvin"),
		KELVIN_3200 			("3200 Kelvin"),
		KELVIN_5200  			("5200 Kelvin"),
		KELVIN_5400  			("5400 Kelvin"),
		KELVIN_6000  			("6000 Kelvin"),
		KELVIN_7000  			("7000 Kelvin"),
		KELVIN_20000  			("20000 Kelvin"),
		WARMFLUORESCENT			("Warm Fluorescent"),
		STANDARDFLUORESCENT  	("Standard Fluorescent"),
		COOLWHITEFLUORESCENT 	("Cool White Fluorescent"),
		FULLSPECTRUMFLUORESCENT ("Full Spectrum Fluorescent"),
		GROWLIGHTFLUORESCENT  	("Grow Light Fluorescent"),
		BLACKLIGHTFLUORESCENT  	("Black Light Fluorescent"),
		MERCURYVAPOR  			("Mercury Vapor"),
		SODIUMVAPOR  			("Sodium Vapor"),
		METALHALIDE  			("Metal Halide"),
		HIGHPRESSURESODIUM  	("High Pressure Sodium");
		private final String whiteTemperature;
		WhiteTemperature(String whiteTemperature) {
			this.whiteTemperature = whiteTemperature;
		}
		public String getWhiteTemperature(){
			return whiteTemperature;
		}
	}
	public enum PowerSaving {
		DISABLED 	("Disabled"),
		MINUTES_5 	("5 minutes"),
		MINUTES_10 	("10 minutes"),
		MINUTES_15 	("15 minutes"),
		MINUTES_20 	("20 minutes"),
		MINUTES_25 	("25 minutes"),
		MINUTES_30 	("30 minutes"),
		MINUTES_35 	("35 minutes"),
		MINUTES_40 	("40 minutes"),
		MINUTES_45 	("45 minutes"),
		MINUTES_50 	("50 minutes"),
		MINUTES_55 	("55 minutes"),
		MINUTES_60 	("60 minutes");
		private final String powerSaving;
		PowerSaving(String powerSaving) {
			this.powerSaving = powerSaving;
		}
		public String getPowerSaving(){
			return powerSaving;
		}
	}

	// Misc
	public static final String BAUD_RATE_PLACEHOLDER = "BAUD_RATE_";
	public static final String FIREFLY_LUCIFERIN = "Firefly Luciferin";
	public static final String DEFAULT_BAUD_RATE = BaudRate.BAUD_RATE_500000.getBaudRate();
	public static final String SPAWNING_ROBOTS = "Spawning new robot for capture";
	public static final String SERIAL_PORT_IN_USE = "Serial Port in use: ";
	public static final String TURN_LED_ON = "Turn LEDs ON";
	public static final String TURN_LED_OFF = "Turn LEDs OFF";
	public static final String DEFAULT_COLOR_CHOOSER = "255,255,255,255";
	public static final String CLEAN_EXIT = "CLEAN EXIT";
	public static final int SERIAL_CHUNK_SIZE = 250;
	public static final String DATE_FORMAT = "EEEE, MMM dd, yyyy HH:mm:ss a";
	public static final String SETTING_LED_SERIAL = "Setting LEDs";
	public static final int NUMBER_OF_BENCHMARK_ITERATION = 10;
	public static final int BENCHMARK_ERROR_MARGIN = 2;
	public static final String MULTIMONITOR_1 = "Disabled";
	public static final String MULTIMONITOR_2 = "Dual display";
	public static final String MULTIMONITOR_3 = "Triple display";
	public static final String LEFT_DISPLAY = "Left display";
	public static final String CENTER_DISPLAY = "Center display";
	public static final String RIGHT_DISPLAY = "Right display";
	public static final String MAIN_DISPLAY = "Main display";
	public static final String CSS_CLASS_BOLD = "bold";
	public static final String CSS_CLASS_RED = "red";
	public static final String AUTO_DETECT_BLACK_BARS = "Auto";
	public static final int DEEP_BLACK_CHANNEL_TOLERANCE = 4;
	public static final String CONTEXT_MENU_COLOR = "Choose color";
	public static final String CONTEXT_MENU_GAMMA = "Gamma";
	public static final String CONTEXT_MENU_AUDIO_DEVICE = "Audio device";
	public static final String CONTEXT_MENU_AUDIO_GAIN = "Audio gain";
	public static final String NUMBER_FORMAT = "########.##";
	public static final String NIGHT_MODE_OFF = "0%";

	// Upgrade
	public static final String LIGHT_FIRMWARE_DUMMY_VERSION = "1.0.0";
	public static final String MINIMUM_FIRMWARE_FOR_AUTO_UPGRADE = "4.0.3";
	public static final String MIN_FIRMWARE_NOT_MATCH ="[{}, ver={}] Connected device does not match the minimum firmware version requirement.";
	public static final String GITHUB_POM_URL = "https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/pom.xml";
	public static final String GITHUB_GLOW_WORM_URL = "https://raw.githubusercontent.com/sblantipodi/glow_worm_luciferin/master/version";
	public static final String POM_PRJ_VERSION = "<project.version>";
	public static final String POM_PRJ_VERSION_CLOSE = "</project.version>";
	public static final String DOWNLOADING = "Downloading";
	public static final String SETUP_FILENAME_WINDOWS = "FireflyLuciferinSetup.exe";
	public static final String SETUP_FILENAME_MAC = "FireflyLuciferinMac.dmg";
	public static final String SETUP_FILENAME_LINUX_DEB = "FireflyLuciferinLinux.deb";
	public static final String SETUP_FILENAME_LINUX_RPM = "FireflyLuciferinLinux.rpm";
	public static final String GITHUB_RELEASES = "https://github.com/sblantipodi/firefly_luciferin/releases/download/v";
	public static final String GITHUB_RELEASES_FIRMWARE = "https://github.com/sblantipodi/glow_worm_luciferin/releases/download/v";
	public static final String HOME_PATH = "user.home";
	public static final String DOCUMENTS_FOLDER = "Documents";
	public static final String LUCIFERIN_PLACEHOLDER = "FireflyLuciferin";
	public static final String LUCIFERIN_FOLDER = "FireflyLuciferin";
	public static final String EXPECTED_SIZE = "Expected size: ";
	public static final String DOWNLOAD_PROGRESS_BAR = "Downloading : ";
	public static final String DOWNLOAD_COMPLETE = " download completed";
	public static final String UPGRADE_CONTENT_TYPE = "Content-Type";
	public static final String UPGRADE_MULTIPART = "multipart/form-data;boundary=";
	public static final String UPGRADE_URL = "http://-/update";
	public static final String MULTIPART_1  = "--{0}\r\nContent-Disposition: form-data; name=";
	public static final String MULTIPART_2  = "\"file\"; filename=\"{0}\"\r\nContent-Type: " + "application/octet-stream" + "\r\n\r\n";
	public static final String MULTIPART_4  = ("\r\n");
	public static final String MULTIPART_5  = (("--{0}--"));
	public static final String PROP_MINIMUM_FIRMWARE_VERSION = "minimum.firmware.version";

	// Properties
	public static final String PROPERTIES_FILENAME = "project.properties";
	public static final String PROP_VERSION = "version";

	// Native executor
	public static final String CANT_RUN_CMD = "Couldn't run command {} : {}";
	public static final String NO_OUTPUT = "Problem reading output from {}: {}";
	public static final String INTERRUPTED_WHEN_READING = "Interrupted while reading output from {}: {}";
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
	public static final String ALREADY_EXIST = "already exists";
	public static final String WAS_CREATED = "was created";
	public static final String WAS_NOT_CREATED = "was not created";
	public static final String CLEANING_OLD_CONFIG = "Cleaning old config";
	public static final String FAILED_TO_CLEAN_CONFIG = "Failed to clean old config";
	public static final String CONFIG_OK = "Configuration OK.";
	public static final String OK = "OK";
	public static final String KO = "KO";
	public static final String FIRMWARE_UPGRADE_RES = "[{}] Firmware upgrade {}";
	public static final String ERROR_READING_CONFIG = "Error reading config file, writing a default one.";

	// MQTT
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
	public static final String SAVE = "Save";
	public static final String SAVE_AND_CLOSE = "Save and close";
	public static final String FRAMERATE_TITLE = "Framerate error";
	public static final String FRAMERATE_HEADER = "Firefly Luciferin is out of sync";
	public static final String FRAMERATE_CONTEXT = "Your computer is capturing the screen too fast and Glow Worm Luciferin firmware can't keep up.\nThis can cause synchronization issues, do you want to lower the framerate to {0} FPS?";
	public static final String GPIO_TITLE = "GPIO error";
	public static final String GPIO_HEADER = "Unsupported GPIO";
	public static final String GPIO_CONTEXT = "Luciferin supports GPIO2, GPIO5 and GPIO16";
	public static final String BAUDRATE_TITLE = "Warning";
	public static final String BAUDRATE_HEADER = "Are you sure you want to change the baud rate?";
	public static final String BAUDRATE_CONTEXT = """
			If you are experiencing flickering and your hardware connections are properly made, lowering the baud rate may reduce the flicker.
						
			If you want to increase the maximum framerate, increasing the baud rate can help.
			If you increase the baud rate and your microcontroller doesn't support that speed, you will not be able to change it again until you manually reflash the firmware.

			By pressing OK you accept to change the baud rate and to reboot your microcontroller, please wait some moments until it reconnects.
						
			""";
	public static final String GPIO_OK_TITLE = "GPIO";
	public static final String GPIO_OK_HEADER = "GPIO has been changed";
	public static final String GPIO_OK_CONTEXT = "Please click OK to reboot your microcontroller\n\n";
	public static final String SERIAL_PORT = "Serial port";
	public static final String OUTPUT_DEVICE = "Output device";
	public static final String SERIAL_ERROR_TITLE = "Serial Port Error";
	public static final String SERIAL_ERROR_HEADER = "No serial port available";
	public static final String SERIAL_ERROR_OPEN_HEADER = "Can't open SERIAL PORT";
	public static final String SERIAL_PORT_AMBIGUOUS = "Serial port is ambiguous";
	public static final String SERIAL_PORT_AMBIGUOUS_CONTEXT = "There is more than one device connected to your serial ports. Please go to \"Settings\", \"Mode\" and select the serial port you want to use.";
	public static final String MQTT_ERROR_TITLE = "MQTT Connection Error";
	public static final String MQTT_ERROR_HEADER = "Unable to connect to the MQTT server";
	public static final String MQTT_ERROR_CONTEXT = "Luciferin is unable to connect to the MQTT server, please correct your settings and retry.";
	public static final String START = "Start";
	public static final String STOP = "Stop";
	public static final String INFO = "Info";
	public static final String SETTINGS = "Settings";
	public static final String EXIT = "Exit";
	public static final String CLICK_OK_DOWNLOAD = "Click Ok to download and install the new version.";
	public static final String CLICK_OK_DOWNLOAD_LINUX = "Click Ok to download new version in your ~/Documents/FireflyLuciferin folder. ";
	public static final String ONCE_DOWNLOAD_FINISHED = "Once the download is finished, please go to that folder and install it manually.";
	public static final String NEW_VERSION_AVAILABLE = "New version available!";
	public static final String UPGRADE_SUCCESS = "Upgrade success";
	public static final String DEVICEUPGRADE_SUCCESS = " firmware upgrade successful.\nScreen capture will begin as soon as the microcontroller reboot is complete.";
	public static final String NEW_FIRMWARE_AVAILABLE = "New firmware available!";
	public static final String CANT_UPGRADE_TOO_OLD = "Can't upgrade Glow Worm Luciferin device";
	public static final String MANUAL_UPGRADE = "Your device is running an old firmware that doesn't support automatic updates, please update it manually.";
	public static final String DEVICES_UPDATED = "These devices will be updated:\n";
	public static final String DEVICE_UPDATED = "This device will be updated:\n";
	public static final String DEVICE_UPDATED_LIGHT = "This device needs to be updated:\n";
	public static final String UPDATE_BACKGROUND = "update process runs in background, you'll be notified when it's finished.";
	public static final String UPDATE_NEEDED = "click OK to open the Web Browser installer.\nUpgrade must be done manually.";
	public static final String UPDATE_NEEDED_LINUX = "click OK to download the new firmware inside the ~/Documents/FireflyLuciferin folder\nUpgrade must be done manually.";
	public static final String CAPTURE_MODE_CHANGED = "Capture mode changed to ";
	public static final String GITHUB_URL = "https://github.com/sblantipodi/firefly_luciferin";
	public static final String WEB_INSTALLER_URL = "https://sblantipodi.github.io/glow_worm_luciferin";
	public static final String SERIAL_PORT_AUTO = "AUTO";
	public static final String SERIAL_PORT_COM = "COM";
	public static final String SERIAL_PORT_TTY = "/dev/ttyUSB";
	public static final String CLOCKWISE = "Clockwise";
	public static final String ANTICLOCKWISE = "Anticlockwise";
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
	public static final String NO_DEVICE_FOUND = "No devices found";

	// Tooltips
	public static final String TOOLTIP_TOPLED = "# of LEDs in the top row";
	public static final String TOOLTIP_LEFTLED = "# of LEDs in the left column";
	public static final String TOOLTIP_RIGHTLED = "# of LEDs in the right column";
	public static final String TOOLTIP_BOTTOMLEFTLED = "# of LEDs in bottom left row";
	public static final String TOOLTIP_BOTTOMRIGHTLED = "# of LEDs in the bottom right row";
	public static final String TOOLTIP_BOTTOMROWLED = "# of LEDs in the bottom row";
    public static final String TOOLTIP_ORIENTATION = "Orientation of your LED strip";
    public static final String TOOLTIP_SCREENWIDTH = "Monitor resolution";
	public static final String TOOLTIP_SCREENHEIGHT = "Monitor resolution";
	public static final String TOOLTIP_LEDSTARTOFFSET = "First LED offset";
	public static final String TOOLTIP_SCALING = "OS scaling feature, you should not change this setting";
	public static final String TOOLTIP_WHITE_TEMP = "Color correction can be made by adjusting white temperature";
    public static final String TOOLTIP_GAMMA = "Smaller values results in brighter LEDs but less accurate colors. 2.2 is generally good for SDR contents, 6.0 is generally good for HDR contents.";
    public static final String TOOLTIP_CAPTUREMETHOD = "If you have a GPU, Desktop Duplication API (DDUPL) is faster than other methods";
	public static final String TOOLTIP_LINUXCAPTUREMETHOD = "Capture method";
	public static final String TOOLTIP_MACCAPTUREMETHOD = "Capture method";
    public static final String TOOLTIP_NUMBEROFTHREADS = "1 thread is enough when using DDUPL, 3 or more threads are recommended for other capture methods";
    public static final String TOOLTIP_SERIALPORT = "Output device to use for this display";
    public static final String TOOLTIP_ASPECTRATIO = "Select letterbox if your video content presents horizontal black bars or pillarbox if you see vertical black bars.";
	public static final String TOOLTIP_FRAMERATE = "30 FPS is the recommended framerate. This setting is specific to the bias light function.";
	public static final String TOOLTIP_MQTTHOST = "OPTIONAL: MQTT protocol://host";
	public static final String TOOLTIP_POWER_SAVING = "Turn off LEDs if there is no activity for the configured number of minutes";
	public static final String TOOLTIP_MULTIMONITOR = "One microcontroller per monitor is required";
	public static final String TOOLTIP_MONITORNUMBER = "Display number for this instance";
    public static final String TOOLTIP_MQTTPORT = "OPTIONAL: MQTT port";
    public static final String TOOLTIP_MQTTTOPIC = "OPTIONAL: MQTT topic, change it if you want to run Luciferin on more than one computer over MQTT.";
    public static final String TOOLTIP_MQTTUSER = "OPTIONAL: MQTT username";
    public static final String TOOLTIP_MQTTPWD = "OPTIONAL: MQTT password";
	public static final String TOOLTIP_MQTTENABLE = "FULL firmware requires MQTT";
	public static final String TOOLTIP_EYE_CARE = "If enabled LEDs will never turn off in black scenes, a soft and gentle light is used instead.";
	public static final String TOOLTIP_MQTTSTREAM = "Prefer wireless stream over serial port (USB cable). Enable this option if you don't have the possibility to use a USB cable.";
	public static final String TOOLTIP_START_WITH_SYSTEM = "Launch Firefly Luciferin when system starts";
	public static final String TOOLTIP_CHECK_UPDATES = "Set and forget it to update Firefly Luciferin and Glow Worm Luciferin when updates are available. Automatic firmware upgrade is available on FULL version only";
	public static final String TOOLTIP_PLAYBUTTON_NULL = "Please configure and save before capturing";
	public static final String TOOLTIP_SYNC_CHECK = "Check if Firefly Luciferin is in sync with the Glow Worm Luciferin firmware";
	public static final String TOOLTIP_BRIGHTNESS = "Set the brightness of the LED strip";
	public static final String TOOLTIP_SPLIT_BOTTOM_ROW = "Split/Merge bottom LEDs row";
    public static final String TOOLTIP_SAVELEDBUTTON_NULL = "You can change this options later";
    public static final String TOOLTIP_SAVEMQTTBUTTON_NULL = "You can change this options later";
	public static final String TOOLTIP_SAVESETTINGSBUTTON_NULL = "You can change this options later";
	public static final String TOOLTIP_SAVEDEVICEBUTTON_NULL = "You can change this options later";
	public static final String TOOLTIP_PLAYBUTTON = "START/STOP capturing";
	public static final String TOOLTIP_SAVELEDBUTTON = "Changes will take effect the next time you launch the app";
	public static final String TOOLTIP_SAVEMQTTBUTTON = "Changes will take effect the next time you launch the app";
	public static final String TOOLTIP_SAVESETTINGSBUTTON = "Changes will take effect the next time you launch the app";
	public static final String TOOLTIP_SAVEDEVICEBUTTON = "Changes will take effect the next time you launch the app";
	public static final String TOOLTIP_SHOWTESTIMAGEBUTTON = "Show a test image, first and last LEDs are shown in orange. Unsaved settings will not be displayed here.";
	public static final String TOOLTIP_BAUD_RATE = "Change it wisely";
	public static final String TOOLTIP_AUDIO_CHANNELS = "Numbers of supported audio channels";
	public static final String TOOLTIP_AUDIO_GAIN = "Audio gain";
	public static final String TOOLTIP_AUDIO_DEVICE = "Audio device to capture";
	public static final String TOOLTIP_EFFECT = "Ambient light effect, music mode, colors";
	public static final String TOOLTIP_COLORS = "Color to use when using the Solid effect";
	public static final String TOOLTIP_NIGHT_MODE_FROM = "Night mode will be enabled at this time";
	public static final String TOOLTIP_NIGHT_MODE_TO = "Night mode will end at this time";
	public static final String TOOLTIP_NIGHT_MODE_BRIGHT = "Reduce brightness when in night mode, 0% disables night mode";

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
	public static final String GSTREAMER_PIPELINE_WINDOWS = "d3d11desktopdupsrc monitor-index={0} ! d3d11convert ! d3d11download";
	public static final String GSTREAMER_PIPELINE_LINUX = "ximagesrc startx={0} endx={1} starty={2} endy={3} ! videoscale ! videoconvert";
	public static final String GSTREAMER_PIPELINE_MAC = "avfvideosrc capture-screen=true ! videoscale ! videoconvert";
	public static final String FRAMERATE_PLACEHOLDER = "framerate=FRAMERATE_PLACEHOLDER/1,";
	public static final String UNLOCKED = "UNLOCKED";
	public static final int NUMBER_OF_AREA_TO_CHECK = 50;

	// Message server
	public static final String MSG_SERVER_HOST = "127.0.0.1";
	public static final int MSG_SERVER_PORT = 5555;
	public static final String MSG_SERVER_STATUS = "MSG_SERVER_STATUS";

	// Exceptions
	public static final String WIN32_EXCEPTION = "Win32 Exception.";
	public static final String SELECT_OBJ_EXCEPTION = "SelectObject Exception.";
	public static final String DELETE_OBJ_EXCEPTION = "DeleteObject Exception.";
	public static final String DELETE_DC_EXCEPTION = "Delete DC Exception.";
	public static final String DEVICE_CONTEXT_RELEASE_EXCEPTION = "GlowWormDevice context did not release properly.";
	public static final String WINDOWS_EXCEPTION = "Window width and/or height were 0 even though GetWindowRect did not appear to fail.";
	public static final String CANT_FIND_GSTREAMER = "Cant' find GStreamer";
	public static final String SOMETHING_WENT_WRONG = "Something went wrong.";

	// Network
	public static final String ACTION = "action";
	public static final String CLIENT_ACTION = "clientActionSetState";

	// Audio
	public static final String DEFAULT_AUDIO_OUTPUT = "Default audio output";
	public static final String WASAPI = "WASAPI";
	public static final String LOOPBACK = "Loopback";
	public static final String MUSIC_MODE = "Music mode";

	// Image processor
	public static final String FAT_JAR_NAME = "FireflyLuciferin-jar-with-dependencies.jar";
	public static final String CLASSES = "classes";
	public static final String TARGET = "target";
	public static final String MAIN_RES = "src/main/resources";
	public static final String GSTREAMER_PATH_IN_USE = "GStreamer path in use=";

	// Windows Registry
	public static final String REGISTRY_KEY_PATH = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run\\";
	public static final String REGISTRY_KEY_NAME = "FireflyLuciferin";
	public static final String REGISTRY_KEY_VALUE_WINDOWS = "Firefly Luciferin.exe";
	public static final String REGISTRY_KEY_VALUE_LINUX = "bin/FireflyLuciferin";
	public static final String REGISTRY_DEFAULT_KEY_VALUE = "C:\\Users\\sblantipodi\\AppData\\Local\\Firefly Luciferin\\Firefly Luciferin.exe";
	public static final String REGISTRY_JARNAME_WINDOWS = "app\\FireflyLuciferin-jar-with-dependencies.jar";
	public static final String REGISTRY_JARNAME_LINUX = "lib/app/FireflyLuciferin-jar-with-dependencies.jar";

}