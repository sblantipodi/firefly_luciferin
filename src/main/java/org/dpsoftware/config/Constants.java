/*
  Constants.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2021  Davide Perini

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

	// Misc
	public static enum OsKind {
		WINDOWS,
		LINUX,
		MAC,
		OTHER
	}
	public static enum PlayerStatus {
		PLAY,
		STOP,
		GREY
	}
	public static final String FIREFLY_LUCIFERIN = "Firefly Luciferin";
	public static final int DEFAULT_BAUD_RATE = 1000000;
	public static final String FULLSCREEN = "FullScreen";
	public static final String LETTERBOX = "Letterbox";
	public static final String SPAWNING_ROBOTS = "Spawning new robot for capture";
	public static final String SERIAL_PORT_IN_USE = "Serial Port in use: ";
	public static final String TURN_LED_ON = "Turn LEDs ON";
	public static final String TURN_LED_OFF = "Turn LEDs OFF";
	public static final String RED_COLOR = "RED_COLOR";
	public static final String GREEN_COLOR = "GREEN_COLOR";
	public static final String BLU_COLOR = "BLU_COLOR";
	public static final String BRIGHTNESS = "BRIGHTNESS";
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
	public static final String DISPLAY = "Display";
	public static final String LEFT_DISPLAY = "Left display";
	public static final String CENTER_DISPLAY = "Center display";
	public static final String RIGHT_DISPLAY = "Right display";
	public static final String MAIN_DISPLAY = "Main display";

	// Upgrade
	public static final String LIGHT_FIRMWARE_DUMMY_VERSION = "1.0.0";
	public static final String MINIMUM_FIRMWARE_FOR_AUTO_UPGRADE = "4.0.3";
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
	public static final String UPGRADE_FILE = "file";
	public static final String UPGRADE_CONTENT_TYPE = "Content-Type";
	public static final String UPGRADE_MULTIPART = "multipart/form-data;boundary=";
	public static final String UPGRADE_URL = "http://-/update";

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
	public static final String IMAGE_TRAY_STOP = "/org/dpsoftware/gui/img/tray_stop.png";
	public static final String IMAGE_TRAY_STOP_CENTER = "/org/dpsoftware/gui/img/tray_stop_center.png";
	public static final String IMAGE_TRAY_STOP_LEFT = "/org/dpsoftware/gui/img/tray_stop_left.png";
	public static final String IMAGE_TRAY_STOP_RIGHT = "/org/dpsoftware/gui/img/tray_stop_right.png";
	public static final String IMAGE_TRAY_GREY = "/org/dpsoftware/gui/img/tray_stop_grey.png";
	public static final String IMAGE_TRAY_GREY_CENTER = "/org/dpsoftware/gui/img/tray_stop_grey_center.png";
	public static final String IMAGE_TRAY_GREY_LEFT = "/org/dpsoftware/gui/img/tray_stop_grey_left.png";
	public static final String IMAGE_TRAY_GREY_RIGHT = "/org/dpsoftware/gui/img/tray_stop_grey_right.png";
	public static final String IMAGE_CONTROL_GREY = "/org/dpsoftware/gui/img/luciferin_logo_grey.png";
	public static final String IMAGE_CONTROL_GREY_CENTER = "/org/dpsoftware/gui/img/luciferin_logo_grey_center.png";
	public static final String IMAGE_CONTROL_GREY_LEFT = "/org/dpsoftware/gui/img/luciferin_logo_grey_left.png";
	public static final String IMAGE_CONTROL_GREY_RIGHT = "/org/dpsoftware/gui/img/luciferin_logo_grey_right.png";
	public static final String IMAGE_CONTROL_PLAY = "/org/dpsoftware/gui/img/luciferin_logo_play.png";
	public static final String IMAGE_CONTROL_PLAY_CENTER = "/org/dpsoftware/gui/img/luciferin_logo_play_center.png";
	public static final String IMAGE_CONTROL_PLAY_LEFT = "/org/dpsoftware/gui/img/luciferin_logo_play_left.png";
	public static final String IMAGE_CONTROL_PLAY_RIGHT = "/org/dpsoftware/gui/img/luciferin_logo_play_right.png";
	public static final String IMAGE_CONTROL_LOGO = "/org/dpsoftware/gui/img/luciferin_logo.png";
	public static final String IMAGE_CONTROL_LOGO_CENTER = "/org/dpsoftware/gui/img/luciferin_logo_center.png";
	public static final String IMAGE_CONTROL_LOGO_LEFT = "/org/dpsoftware/gui/img/luciferin_logo_left.png";
	public static final String IMAGE_CONTROL_LOGO_RIGHT = "/org/dpsoftware/gui/img/luciferin_logo_right.png";

	public static final String FXML = ".fxml";
	public static final String FXML_SETTINGS = "settings";
	public static final String FXML_SETTINGS_LINUX = "linuxSettings";
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
	public static final String ERROR_READING_CONFIG = "Error reading config file, writing a default one.";

	// MQTT
	public static final String STATE_OFF_SOLID = "{\"state\": \"OFF\", \"effect\": \"solid\"}";
	public static final String STATE_ON_SOLID_COLOR = "{\"state\": \"ON\", \"effect\": \"solid\", \"color\": {\"r\": RED_COLOR, \"g\": GREEN_COLOR, \"b\": BLU_COLOR}, \"brightness\": BRIGHTNESS}";
	public static final String STATE_ON_GLOWWORM = "{\"state\": \"ON\", \"effect\": \"GlowWorm\"}";
	public static final String STATE_ON_GLOWWORMWIFI = "{\"state\": \"ON\", \"effect\": \"GlowWormWifi\"}";
    public static final String START_WEB_SERVER_MSG = "{\"update\":true}";
	public static final String DEFAULT_MQTT_HOST = "tcp://192.168.1.3";
	public static final String DEFAULT_MQTT_PORT = "1883";
	public static final String DEFAULT_MQTT_TOPIC = "lights/glowwormluciferin/set";
	public static final String DEFAULT_MQTT_STATE_TOPIC = "lights/glowwormluciferin";
	public static final String UPDATE_MQTT_TOPIC = "lights/glowwormluciferin/update";
	public static final String FPS_TOPIC = "lights/glowwormluciferin/fps";
	public static final String UPDATE_RESULT_MQTT_TOPIC = "lights/glowwormluciferin/update/result";
	public static final String FIREFLY_LUCIFERIN_FRAMERATE = "lights/firelyluciferin/framerate";
	public static final String FIREFLY_LUCIFERIN_GAMMA = "lights/firelyluciferin/gamma";
	public static final String GLOW_WORM_GPIO_TOPIC = "lights/glowwormluciferin/gpio";
	public static final String WHOAMI = "Whoami";
	public static final String STATE_IP = "IP";
	public static final String DEVICE_VER = "ver";
	public static final String DEVICE_BOARD = "board";
	public static final String NUMBER_OF_LEDS = "lednum";
	public static final String MAC = "MAC";
	public static final String GPIO = "gpio";
	public static final String STATE = "state";
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
	public static final int FIRST_CHUNK = 190;
	public static final int SECOND_CHUNK = 380;
	public static final String LED_NUM = "\"lednum\":";
	public static final String STREAM = "\"stream\":[";
	public static final String MQTT_FRAMERATE = "{\"producing\":PROD,\"consuming\":CONS}";
	public static final String PROD_PLACEHOLDER = "PROD";
	public static final String CONS_PLACEHOLDER = "CONS";
	public static final String MQTT_GAMMA = "gamma";
	public static final String MQTT_GPIO = "gpio";

	// GUI
	public static final String SAVE = "Save";
	public static final String SAVE_AND_CLOSE = "Save and close";
	public static final String FRAMERATE_TITLE = "Framerate error";
	public static final String FRAMERATE_HEADER = "Firefly Luciferin is out of sync";
	public static final String FRAMERATE_CONTEXT = "Your computer is capturing the screen too fast and Glow Worm Luciferin firmware can't keep up.\nThis can cause synchronization issues, do you want to lower the framerate to {0} FPS?";
	public static final String GPIO_TITLE = "GPIO error";
	public static final String GPIO_HEADER = "Unsupported GPIO";
	public static final String GPIO_CONTEXT = "Luciferin supports GPIO2, GPIO5 and GPIO16";
	public static final String GPIO_OK_TITLE = "GPIO";
	public static final String GPIO_OK_HEADER = "GPIO has been changed";
	public static final String GPIO_OK_CONTEXT = "Please click OK to reboot your microcontroller\n\n";
	public static final String SERIAL_PORT = "Serial port";
	public static final String OUTPUT_DEVICE = "Output device";
	public static final String SERIAL_ERROR_TITLE = "Serial Port Error";
	public static final String SERIAL_ERROR_HEADER = "No serial port available";
	public static final String SERIAL_ERROR_OPEN_HEADER = "Can't open SERIAL PORT";
	public static final String SERIAL_PORT_AMBIGUOUS = "Serial port is ambiguous";
	public static final String SERIAL_ERROR_CONTEXT = "Serial port is in use or there is no microcontroller available. Please connect a microcontroller or go to settings and choose MQTT Stream. Luciferin restart is required.";
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
	public static final String DEVICEUPGRADE_SUCCESS = " has been successfully upgraded";
	public static final String NEW_FIRMWARE_AVAILABLE = "New firmware available!";
	public static final String CANT_UPGRADE_TOO_OLD = "Can't upgrade Glow Worm Luciferin device";
	public static final String MANUAL_UPGRADE = "Your device is running an old firmware that doesn't support automatic updates, please update it manually.";
	public static final String DEVICES_UPDATED = "These devices will be updated:\n";
	public static final String DEVICE_UPDATED = "This device will be updated:\n";
	public static final String DEVICE_UPDATED_LIGHT = "This device needs to be updated:\n";
	public static final String UPDATE_BACKGROUND = "update process runs in background, you'll be notified when it's finished.";
	public static final String UPDATE_NEEDED = "please download the new Glow Worm Luciferin firmware and update it manually.";
	public static final String STOPPING_THREADS = "Stopping Threads...";
	public static final String CAPTURE_MODE_CHANGED = "Capture mode changed to ";
	public static final String GITHUB_URL = "https://github.com/sblantipodi/firefly_luciferin";
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
	public static final String SERIAL_VERSION = "ver:";
	public static final String SERIAL_LED_NUM = "lednum:";
	public static final String SERIAL_BOARD = "board:";
	public static final String SERIAL_FRAMERATE = "framerate:";
	public static final String SERIAL_MAC = "MAC:";
	public static final String SERIAL_GPIO = "gpio:";


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
    public static final String TOOLTIP_GAMMA = "Smaller values results in brighter LEDs but less accurate colors. 2.2 is generally good for SDR contents, 6.0 is generally good for HDR contents.";
    public static final String TOOLTIP_CAPTUREMETHOD = "If you have a GPU, Desktop Duplication API (DDUPL) is faster than other methods";
	public static final String TOOLTIP_LINUXCAPTUREMETHOD = "Capture method";
	public static final String TOOLTIP_MACCAPTUREMETHOD = "Capture method";
    public static final String TOOLTIP_NUMBEROFTHREADS = "1 thread is enough when using DDUPL, 3 or more threads are recommended for other capture methods";
    public static final String TOOLTIP_SERIALPORT = "AUTO detects first serial port available, change it if you have more than one serial port available";
    public static final String TOOLTIP_ASPECTRATIO = "LetterBox is recommended for films, you can change this option later";
	public static final String TOOLTIP_FRAMERATE = "30 FPS IS THE RECOMMENDED FRAMERATE, use at your own risk.";
	public static final String TOOLTIP_MQTTHOST = "OPTIONAL: MQTT protocol://host";
    public static final String TOOLTIP_MQTTPORT = "OPTIONAL: MQTT port";
    public static final String TOOLTIP_MQTTTOPIC = "OPTIONAL: MQTT topic, used to start/stop capturing. Don't change it if you want to use Glow Worm Luciferin Firmware.";
    public static final String TOOLTIP_MQTTUSER = "OPTIONAL: MQTT username";
    public static final String TOOLTIP_MQTTPWD = "OPTIONAL: MQTT password";
	public static final String TOOLTIP_MQTTENABLE = "FULL firmware requires MQTT";
	public static final String TOOLTIP_EYE_CARE = "If enabled LEDs will never turn off in black scenes, a soft and gentle light is used instead.";
	public static final String TOOLTIP_AUTOSTART = "Start capture on Firefly Luciferin startup";
	public static final String TOOLTIP_MQTTSTREAM = "Prefer wireless stream over serial port (USB cable). Enable this option if you don't have the possibility to use a USB cable.";
	public static final String TOOLTIP_START_WITH_SYSTEM = "Launch Firefly Luciferin when system starts";
	public static final String TOOLTIP_CHECK_UPDATES = "Set and forget it to update Firefly Luciferin and Glow Worm Luciferin when updates are available. Automatic firmware upgrade is available on FULL version only";
	public static final String TOOLTIP_PLAYBUTTON_NULL = "Please configure and save before capturing";
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

	// Grabber
	public static final String INTERNAL_SCALING_X = "INTERNAL_SCALING_X";
	public static final String INTERNAL_SCALING_Y = "INTERNAL_SCALING_Y";
	public static final int RESAMPLING_FACTOR = 2;
	public static final String EMIT_SIGNALS = "emit-signals";
	public static final String GSTREAMER_PIPELINE_DDUPL = "video/x-raw(memory:D3D11Memory),width=INTERNAL_SCALING_X,height=INTERNAL_SCALING_Y,pixel-aspect-ratio=1/1,use-damage=0,sync=false,";
	public static final String GSTREAMER_PIPELINE = "video/x-raw,pixel-aspect-ratio=1/1,framerate=30/1,use-damage=0,sync=false,";
	public static final String BYTE_ORDER_BGR = "format=BGRx";
	public static final String BYTE_ORDER_RGB = "format=xRGB";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String GSTREAMER_PATH = "/gstreamer/1.0/mingw_x86_64/bin";
	public static final String PATH = "path";
	public static final String JNA_LIB_PATH = "jna.library.path";
	public static final String SCREEN_GRABBER = "ScreenGrabber";
	public static final String GSTREAMER_PIPELINE_WINDOWS = "d3d11desktopdupsrc monitor-index={0} ! videoscale method=0 ! d3d11convert";
	public static final String GSTREAMER_PIPELINE_LINUX = "ximagesrc xid={0} ! videoscale ! videoconvert";
	public static final String GSTREAMER_PIPELINE_MAC = "avfvideosrc capture-screen=true ! videoscale ! videoconvert";
	public static final String FRAMERATE_PLACEHOLDER = "framerate=FRAMERATE_PLACEHOLDER/1,";
	public static final String UNLOCKED = "UNLOCKED";

	// Exceptions
	public static final String WIN32_EXCEPTION = "Win32 Exception.";
	public static final String SELECT_OBJ_EXCEPTION = "SelectObject Exception.";
	public static final String DELETE_OBJ_EXCEPTION = "DeleteObject Exception.";
	public static final String DELETE_DC_EXCEPTION = "Delete DC Exception.";
	public static final String DEVICE_CONTEXT_RELEASE_EXCEPTION = "GlowWormDevice context did not release properly.";
	public static final String WINDOWS_EXCEPTION = "Window width and/or height were 0 even though GetWindowRect did not appear to fail.";
	public static final String CANT_FIND_GSTREAMER = "Cant' find GStreamer";
	public static final String SOMETHING_WENT_WRONG = "Something went wrong.";

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