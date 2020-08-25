/*
  Constants.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/
package org.dpsoftware.config;

public class Constants {

	public static final String FIREFLY_LUCIFERIN_VERSION = "1.2.5";

	// Misc
	public static final String FIREFLY_LUCIFERIN = "Firefly Luciferin";
	public static final String FULLSCREEN = "FullScreen";
	public static final String LETTERBOX = "Letterbox";
	public static final String SPAWNING_ROBOTS = "Spawning new robot for capture";
	public static final String SERIAL_PORT_IN_USE = "Serial Port in use: ";

	// Upgrade
	public static final String GITHUB_POM_URL = "https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/pom.xml";
	public static final String POM_PRJ_VERSION = "<project.version>";
	public static final String POM_PRJ_VERSION_CLOSE = "</project.version>";
	public static final String DOWNLOADING = "Downloading";
	public static final String SETUP_FILENAME_WINDOWS = "FireflyLuciferinSetup.exe";
	public static final String SETUP_FILENAME_LINUX_DEB = "FireflyLuciferinLinux.deb";
	public static final String SETUP_FILENAME_LINUX_RPM = "FireflyLuciferinLinux.rpm";
	public static final String GITHUB_RELEASES = "https://github.com/sblantipodi/firefly_luciferin/releases/download/v";
	public static final String HOME_PATH = "user.home";
	public static final String DOCUMENTS_FOLDER = "Documents";
	public static final String LUCIFERIN_PLACEHOLDER = "FireflyLuciferin";
	public static final String LUCIFERIN_FOLDER = "FireflyLuciferin";
	public static final String EXPECTED_SIZE = "Expected size: ";
	public static final String DOWNLOAD_PROGRESS_BAR = "Downloading : ";
	public static final String DOWNLOAD_COMPLETE = " download completed";

	// Native executor
	public static final String CANT_RUN_CMD = "Couldn't run command {} : {}";
	public static final String NO_OUTPUT = "Problem reading output from {}: {}";
	public static final String INTERRUPTED_WHEN_READING = "Interrupted while reading output from {}: {}";
	public static final String DPKG_CHECK_CMD = "dpkg --version";

	// Resources
	public static final String IMAGE_TRAY_PLAY = "/org/dpsoftware/gui/img/tray_play.png";
	public static final String IMAGE_TRAY_STOP = "/org/dpsoftware/gui/img/tray_stop.png";
	public static final String IMAGE_TRAY_GREY = "/org/dpsoftware/gui/img/tray_stop_grey.png";
	public static final String IMAGE_CONTROL_GREY = "/org/dpsoftware/gui/img/java_fast_screen_capture_logo_grey.png";
	public static final String IMAGE_CONTROL_PLAY = "/org/dpsoftware/gui/img/java_fast_screen_capture_logo_play.png";
	public static final String IMAGE_CONTROL_LOGO = "/org/dpsoftware/gui/img/java_fast_screen_capture_logo.png";
	public static final String FXML = ".fxml";
	public static final String FXML_SETTINGS = "settings";
	public static final String FXML_SETTINGS_LINUX = "linuxSettings";
	public static final String FXML_INFO = "info";
	public static final String CONFIG_FILENAME = "FireflyLuciferin.yaml";
	public static final String ALREADY_EXIST = "already exists";
	public static final String WAS_CREATED = "was created";
	public static final String WAS_NOT_CREATED = "was not created";
	public static final String CLEANING_OLD_CONFIG = "Cleaning old config";
	public static final String FAILED_TO_CLEAN_CONFIG = "Failed to clean old config";
	public static final String CONFIG_OK = "Configuration OK.";
	public static final String ERROR_READING_CONFIG = "Error reading config file, writing a default one.";

	// MQTT
	public static final String STATE_ON_SOLID = "{\"state\": \"ON\", \"effect\": \"solid\"}";
	public static final String STATE_ON_GLOWWORM = "{\"state\": \"ON\", \"effect\": \"GlowWormWifi\"}";
	public static final String STATE_ON_GLOWWORMWIFI = "{\"state\": \"ON\", \"effect\": \"GlowWorm\"}";
	public static final String DEFAULT_MQTT_HOST = "tcp://192.168.1.3";
	public static final String DEFAULT_MQTT_PORT = "1883";
	public static final String DEFAULT_MQTT_TOPIC = "lights/glowwormluciferin/set";
	public static final String MQTT_DISABLED = "MQTT disabled.";
	public static final String MQTT_DEVICE_NAME_WIN = "FireflyLuciferin";
	public static final String MQTT_DEVICE_NAME_LIN = "FireflyLuciferinLinux";
	public static final String MQTT_CONNECTED = "Connected to MQTT Server";
	public static final String MQTT_CANT_SEND = "Cant't send MQTT msg";
	public static final String MQTT_STREAM_TOPIC = "/stream";
	public static final String MQTT_RECONNECTED = "Reconnected";
	public static final String MQTT_DISCONNECTED = "Disconnected";
	public static final String MQTT_START = "START";
	public static final String MQTT_STOP = "STOP";

	// GUI
	public static final String SAVE = "Save";
	public static final String SAVE_AND_CLOSE = "Save and close";
	public static final String SERIAL_ERROR_TITLE = "Serial Port Error";
	public static final String SERIAL_ERROR_HEADER = "No serial port available";
	public static final String SERIAL_ERROR_OPEN_HEADER = "Can't open SERIAL PORT";
	public static final String SERIAL_ERROR_CONTEXT = "Serial port is in use or there is no microcontroller available. Please connect a microcontroller or go to settings and choose MQTT Stream. Luciferin restart is required.";
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
	public static final String STOPPING_THREADS = "Stopping Threads...";
	public static final String CAPTURE_MODE_CHANGED = "Capture mode changed to ";
	public static final String GITHUB_URL = "https://github.com/sblantipodi/firefly_luciferin";
	public static final String SERIAL_PORT_AUTO = "AUTO";
	public static final String SERIAL_PORT_COM = "COM";
	public static final String SERIAL_PORT_TTY = "/dev/ttyUSB";
	public static final String CLOCKWISE = "Clockwise";
	public static final String ANTICLOCKWISE = "Anticlockwise";
	public static final String VERSION = "VERSION";
	public static final String BY_DAVIDE = "by Davide Perini (VERSION)";
	public static final String PRODUCING = "Producing @ ";
	public static final String CONSUMING = "Consuming @ ";
	public static final String FPS = "FPS";
	public static final String PERCENT = "%";
	public static final String GAMMA_DEFAULT = "2.2";

	// Tooltips
	public static final String TOOLTIP_TOPLED = "# of LEDs in the top row";
	public static final String TOOLTIP_LEFTLED = "# of LEDs in the left column";
	public static final String TOOLTIP_RIGHTLED = "# of LEDs in the right column";
	public static final String TOOLTIP_BOTTOMLEFTLED = "# of LEDs in bottom left row";
    public static final String TOOLTIP_BOTTOMRIGHTLED = "# of LEDs in the bottom right row";
    public static final String TOOLTIP_ORIENTATION = "Orientation of your LED strip";
    public static final String TOOLTIP_SCREENWIDTH = "Monitor resolution";
    public static final String TOOLTIP_SCREENHEIGHT = "Monitor resolution";
    public static final String TOOLTIP_SCALING = "OS scaling feature, you should not change this setting";
    public static final String TOOLTIP_GAMMA = "Smaller values results in brighter LEDs but less accurate colors. 2.2 is generally good for SDR contents, 6.0 is generally good for HDR contents.";
    public static final String TOOLTIP_CAPTUREMETHOD = "If you have a GPU, Desktop Duplication API (DDUPL) is faster than other methods";
    public static final String TOOLTIP_LINUXCAPTUREMETHOD = "If you have a GPU, Desktop Duplication API (DDUPL) is faster than other methods";
    public static final String TOOLTIP_NUMBEROFTHREADS = "1 thread is enough when using DDUPL, 3 or more threads are recommended for other capture methods";
    public static final String TOOLTIP_SERIALPORT = "AUTO detects first serial port available, change it if you have more than one serial port available";
    public static final String TOOLTIP_ASPECTRATIO = "LetterBox is recommended for films, you can change this option later";
    public static final String TOOLTIP_MQTTHOST = "OPTIONAL: MQTT protocol://host";
    public static final String TOOLTIP_MQTTPORT = "OPTIONAL: MQTT port";
    public static final String TOOLTIP_MQTTTOPIC = "OPTIONAL: MQTT topic, used to start/stop capturing. Don't change it if you want to use Glow Worm Luciferin Firmware.";
    public static final String TOOLTIP_MQTTUSER = "OPTIONAL: MQTT username";
    public static final String TOOLTIP_MQTTPWD = "OPTIONAL: MQTT password";
    public static final String TOOLTIP_MQTTENABLE = "MQTT is Optional";
    public static final String TOOLTIP_MQTTSTREAM = "Prefer wireless stream over serial port (USB cable). This option is ignored if MQTT is disabled. Enable this option if you don't have the possibility to use a USB cable.";
	public static final String TOOLTIP_PLAYBUTTON_NULL = "Please configure and save before capturing";
    public static final String TOOLTIP_SAVELEDBUTTON_NULL = "You can change this options later";
    public static final String TOOLTIP_SAVEMQTTBUTTON_NULL = "You can change this options later";
    public static final String TOOLTIP_SAVESETTINGSBUTTON_NULL = "You can change this options later";
	public static final String TOOLTIP_PLAYBUTTON = "START/STOP capturing";
	public static final String TOOLTIP_SAVELEDBUTTON = "Changes will take effect the next time you launch the app";
	public static final String TOOLTIP_SAVEMQTTBUTTON = "Changes will take effect the next time you launch the app";
	public static final String TOOLTIP_SAVESETTINGSBUTTON = "Changes will take effect the next time you launch the app";
	public static final String TOOLTIP_SHOWTESTIMAGEBUTTON = "Show a test image, useful to check for LED alignment behind the monitor";

	// Grabber
	public static final String EMIT_SIGNALS = "emit-signals";
	public static final String GSTREAMER_PIPELINE = "video/x-raw,pixel-aspect-ratio=1/1,framerate=30/1,use-damage=0,sync=false,";
	public static final String BYTE_ORDER_BGR = "format=BGRx";
	public static final String BYTE_ORDER_RGB = "format=xRGB";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String GSTREAMER_PATH = "/gstreamer/1.0/x86_64/bin";
	public static final String PATH = "path";
	public static final String JNA_LIB_PATH = "jna.library.path";
	public static final String SCREEN_GRABBER = "ScreenGrabber";
	public static final String GSTREAMER_PIPELINE_WINDOWS = "dxgiscreencapsrc ! videoscale ! videoconvert";
	public static final String GSTREAMER_PIPELINE_LINUX = "ximagesrc ! videoscale ! videoconvert";

	// Exceptions
	public static final String WIN32_EXCEPTION = "Win32 Exception.";
	public static final String SELECT_OBJ_EXCEPTION = "SelectObject Exception.";
	public static final String DELETE_OBJ_EXCEPTION = "DeleteObject Exception.";
	public static final String DELETE_DC_EXCEPTION = "Delete DC Exception.";
	public static final String DEVICE_CONTEXT_RELEASE_EXCEPTION = "Device context did not release properly.";
	public static final String WINDOWS_EXCEPTION = "Window width and/or height were 0 even though GetWindowRect did not appear to fail.";
	public static final String CANT_FIND_GSTREAMER = "Cant' find GStreamer";
	public static final String SOMETHING_WENT_WRONG = "Something went wrong.";

	// Image processor
	public static final String FAT_JAR_NAME = "FireflyLuciferin-jar-with-dependencies.jar";
	public static final String CLASSES = "classes";
	public static final String TARGET = "target";
	public static final String MAIN_RES = "src/main/resources";
	public static final String GSTREAMER_PATH_IN_USE = "GStreamer path in use=";

}