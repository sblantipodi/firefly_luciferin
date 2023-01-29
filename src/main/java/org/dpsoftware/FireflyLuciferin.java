/*
  FireflyLuciferin.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2023  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.audio.AudioLoopback;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.LocalizedEnum;
import org.dpsoftware.grabber.GrabberManager;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.GUIManager;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.PowerSavingManager;
import org.dpsoftware.managers.SerialManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.managers.dto.StateStatusDto;
import org.dpsoftware.network.MessageClient;
import org.dpsoftware.network.MessageServer;
import org.dpsoftware.network.tcpUdp.UdpClient;
import org.dpsoftware.network.tcpUdp.UdpServer;
import org.dpsoftware.utilities.CommonUtility;
import org.dpsoftware.utilities.PropertiesLoader;
import org.freedesktop.gstreamer.Pipeline;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Luciferin is a generic term for the light-emitting compound found in organisms that generate bioluminescence like Fireflies and Glow Worms.
 * Firefly Luciferin is a Java Fast Screen Capture PC software designed for the Glow Worm Luciferin firmware, the combination of these
 * software create the perfect Bias Lighting and Ambient Light system for PC.
 * Written in Java with a native flavour for Windows and Linux.
 */
@Slf4j
@Getter
public class FireflyLuciferin extends Application implements SerialPortEventListener {

    // Calculate Screen Capture Framerate and how fast your microcontroller can consume it
    public static float FPS_CONSUMER_COUNTER;
    public static float FPS_PRODUCER_COUNTER;
    public static float FPS_CONSUMER = 0;
    public static float FPS_PRODUCER = 0;
    public static float FPS_GW_CONSUMER = 0;
    public static SimpleDateFormat formatter;
    public static SerialPort serial;
    public static OutputStream output;
    public static boolean serialConnected = false;
    public static int baudRate = 0;
    // LED strip, monitor and microcontroller config
    public static Configuration config;
    // Start and Stop threads
    public static boolean RUNNING = false;
    // This queue orders elements FIFO. Producer offers some data, consumer throws data to the Serial port
    public static BlockingQueue<Color[]> sharedQueue;
    // Number of LEDs on the strip
    public static int ledNumber;
    public static int ledNumHighLowCount;
    public static int ledNumHighLowCountSecondPart;
    // GStreamer Rendering pipeline
    public static Pipeline pipe;
    public static GUIManager guiManager;
    public static boolean communicationError = false;
    public static Color colorInUse;
    public static int gpio = 0; // 0 means not set, firmware discards this value
    public static int ldrAction = 0; // 1 no action, 2 calibrate, 3 reset, 4 save
    public static int fireflyEffect = 0;
    public static boolean nightMode = false;
    public static String version = "";
    public static String minimumFirmwareVersion = "";
    public static ResourceBundle bundle;
    public static String profileArgs;
    public static HostServices hostServices;
    // Image processing
    private final ImageProcessor imageProcessor;
    private final GrabberManager grabberManager;
    // Serial output stream
    SerialManager serialManager;
    // MQTT
    MQTTManager mqttManager = null;
    // Number of CPU Threads to use, this app is heavy multithreaded,
    // high cpu cores equals to higher framerate but big CPU usage
    // 4 Threads are enough for 24FPS on an Intel i7 5930K@4.2GHz
    // 3 thread is enough for 30FPS with GPU Hardware Acceleration and uses nearly no CPU
    private int threadPoolNumber;
    private int executorNumber;
    // UDP
    private UdpClient udpClient;
    private final PowerSavingManager powerSavingManager;

    /**
     * Constructor
     */
    public FireflyLuciferin() {
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        formatter = new SimpleDateFormat(Constants.DATE_FORMAT);
        // Extract project version computed from Continuous Integration (GitHub Actions)
        version = propertiesLoader.retrieveProperties(Constants.PROP_VERSION);
        Locale currentLocale = Locale.getDefault();
        bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, currentLocale);
        if (bundle.getLocale().toString().isEmpty()) {
            bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, Locale.ENGLISH);
        }
        String ledMatrixInUse = "";
        try {
            StorageManager storageManager = new StorageManager();
            config = storageManager.loadConfigurationYaml();
            ledMatrixInUse = config.getDefaultLedMatrix();
        } catch (NullPointerException e) {
            log.error("Please configure the app.");
            FireflyLuciferin.exit();
        }
        manageLocale();
        sharedQueue = new LinkedBlockingQueue<>(config.getLedMatrixInUse(ledMatrixInUse).size() * 30);
        imageProcessor = new ImageProcessor(true);
        serialManager = new SerialManager();
        grabberManager = new GrabberManager();
        if (CommonUtility.isSingleDeviceMainInstance()) {
            MessageServer.messageServer = new MessageServer();
            MessageServer.initNumLed();
        }
        setLedNumber(ledMatrixInUse);
        baudRate = Constants.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + config.getBaudRate()).getBaudRateValue();
        // Check if I'm the main program, if yes and multi monitor, spawn other guys
        NativeExecutor.spawnNewInstances();
        if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
            serialManager.initSerial(this);
            serialManager.initOutputStream();
        }
        initThreadPool();
        hostServices = this.getHostServices();
        powerSavingManager = new PowerSavingManager();
        powerSavingManager.setLastFrameTime(LocalDateTime.now());
    }

    /**
     * Set LED number, this can be changed on the fly.
     *
     * @param ledMatrixInUse led matrix in use
     */
    public static void setLedNumber(String ledMatrixInUse) {
        ledNumber = CommonUtility.isSingleDeviceMultiScreen() ? MessageServer.totalLedNum : config.getLedMatrixInUse(ledMatrixInUse).size();
        ledNumHighLowCount = ledNumber > Constants.SERIAL_CHUNK_SIZE ? Constants.SERIAL_CHUNK_SIZE - 1 : ledNumber - 1;
        ledNumHighLowCountSecondPart = ledNumber > Constants.SERIAL_CHUNK_SIZE ? ledNumber - Constants.SERIAL_CHUNK_SIZE : 0;
    }

    /**
     * Startup JavaFX context
     *
     * @param args startup args
     */
    public static void main(String[] args) {
        if (args.length > 1) {
            profileArgs = args[1];
        }
        NativeExecutor.createStartWMClass();
        StorageManager sm = new StorageManager();
        sm.deleteTempFiles();
        launch(args);
    }

    /**
     * Activate/deactivate night mode
     */
    public static void checkForNightMode() {
        var tempNightMode = nightMode;
        if (!(FireflyLuciferin.config.getNightModeBrightness().equals(Constants.NIGHT_MODE_OFF)) && FireflyLuciferin.config.isToggleLed()) {
            LocalTime from = LocalTime.now();
            LocalTime to = LocalTime.now();
            from = from.withHour(LocalTime.parse(FireflyLuciferin.config.getNightModeFrom()).getHour());
            from = from.withMinute(LocalTime.parse(FireflyLuciferin.config.getNightModeFrom()).getMinute());
            to = to.withHour(LocalTime.parse(FireflyLuciferin.config.getNightModeTo()).getHour());
            to = to.withMinute(LocalTime.parse(FireflyLuciferin.config.getNightModeTo()).getMinute());
            nightMode = (LocalTime.now().isAfter(from) || LocalTime.now().isBefore(to));
            setNightBrightness(tempNightMode);
        } else {
            nightMode = false;
        }
    }

    /**
     * Set brightness
     *
     * @param tempNightMode previous value
     */
    private static void setNightBrightness(boolean tempNightMode) {
        if (tempNightMode != nightMode) {
            log.debug("Night Mode: " + nightMode);
            if (FireflyLuciferin.config != null && FireflyLuciferin.config.isFullFirmware()) {
                StateDto stateDto = new StateDto();
                stateDto.setState(Constants.ON);
                stateDto.setBrightness(CommonUtility.getNightBrightness());
                log.debug(stateDto.getBrightness() + "");
                if (CommonUtility.getDeviceToUse() != null) {
                    stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
                }
                stateDto.setWhitetemp(FireflyLuciferin.config.getWhiteTemperature());
                MQTTManager.publishToTopic(MQTTManager.getTopic(Constants.DEFAULT_MQTT_TOPIC), CommonUtility.toJsonString(stateDto));
            }
        }
    }

    /**
     * Grecefully exit the app
     */
    public static void exit() {
        UdpServer.udpBroadcastReceiverRunning = false;
        exitOtherInstances();
        if (FireflyLuciferin.serial != null) {
            FireflyLuciferin.serial.removeEventListener();
            FireflyLuciferin.serial.close();
        }
        AudioLoopback.RUNNING_AUDIO = false;
        CommonUtility.sleepSeconds(2);
        System.exit(0);
    }

    /**
     * Exit single device instances
     */
    public static void exitOtherInstances() {
        if (!NativeExecutor.restartOnly) {
            if (CommonUtility.isSingleDeviceMainInstance()) {
                StateStatusDto.closeOtherInstaces = true;
                CommonUtility.sleepSeconds(6);
            } else if (CommonUtility.isSingleDeviceOtherInstance()) {
                MessageClient.msgClient.sendMessage(Constants.EXIT);
                CommonUtility.sleepSeconds(6);
            }
        }
    }

    /**
     * Create one fast consumer and many producers.
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Gnome 3 doesn't like this
        if (!NativeExecutor.isLinux()) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        launchGrabberAndConsumers();
        scheduleCheckForNightMode();
        StorageManager storageManager = new StorageManager();
        storageManager.updateConfigFile(config);
        // Manage tray icon and framerate dialog
        guiManager = new GUIManager(stage);
        guiManager.trayIconManager.initTray();
        guiManager.showSettingsAndCheckForUpgrade();
        if (config.isMqttEnable()) {
            connectToMqttServer();
        } else {
            log.debug(Constants.MQTT_DISABLED);
            if (config.isFullFirmware()) {
                UdpServer udpServer = new UdpServer();
                UdpServer.udpBroadcastReceiverRunning = true;
                udpServer.receiveBroadcastUDPPacket();
            }
        }
        grabberManager.getFPS();
        imageProcessor.calculateBorders();
        // If multi monitor, first instance, single instance, start message server
        if (CommonUtility.isSingleDeviceMainInstance()) {
            MessageServer.startMessageServer();
        }
        if (CommonUtility.isSingleDeviceOtherInstance()) {
            MessageClient.getSingleInstanceMultiScreenStatus();
        }
        Constants.Effect effectInUse = LocalizedEnum.fromBaseStr(Constants.Effect.class, config.getEffect());
        if (config.isToggleLed()) {
            switch (effectInUse) {
                case BIAS_LIGHT, MUSIC_MODE_VU_METER, MUSIC_MODE_VU_METER_DUAL, MUSIC_MODE_BRIGHT, MUSIC_MODE_RAINBOW ->
                        manageAutoStart();
            }
        }
        if (!config.isMqttEnable() && !config.isFullFirmware()) {
            serialManager.manageSolidLed();
        }
        scheduleBackgroundTasks(stage);
    }

    /**
     * Launch grabber and consumers
     *
     * @throws AWTException GUI exception
     */
    private void launchGrabberAndConsumers() throws AWTException {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(threadPoolNumber);
        // Desktop Duplication API producers
        if ((config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name()))
                || (config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name()))
                || (config.getCaptureMethod().equals(Configuration.CaptureMethod.AVFVIDEOSRC.name()))) {
            grabberManager.launchAdvancedGrabber(scheduledExecutorService, imageProcessor);
        } else { // Standard Producers
            grabberManager.launchStandardGrabber(scheduledExecutorService, executorNumber);
        }
        // Run a very fast consumer
        CompletableFuture.supplyAsync(() -> {
            try {
                consume();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
            return CommonUtility.getWord(Constants.SOMETHING_WENT_WRONG);
        }, scheduledExecutorService).thenAcceptAsync(log::info).exceptionally(e -> {
            clean();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
            return null;
        });
    }

    /**
     * During the PC startup Firefly Luciferin starts, if it starts before that the network connection is established,
     * MQTT fails to connect, retry until we get a solid connection to the MQTT server.
     */
    private void connectToMqttServer() {
        AtomicInteger retryCounter = new AtomicInteger();
        mqttManager = new MQTTManager(false, retryCounter);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (!mqttManager.connected) {
                log.debug("MQTT retry");
                retryCounter.getAndIncrement();
                mqttManager = new MQTTManager(true, retryCounter);
            } else {
                retryCounter.set(0);
                executor.shutdown();
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    /**
     * Schedule background tasks
     *
     * @param stage main stage
     */
    @SuppressWarnings("unused")
    private void scheduleBackgroundTasks(Stage stage) {
        // Create a task that runs every 5 seconds, reconnect serial devices when needed
        ScheduledExecutorService serialscheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable framerateTask = () -> {
            if (!serialConnected && !config.isWirelessStream()) {
                if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
                    serialManager.initSerial(this);
                }
            }
        };
        serialscheduledExecutorService.scheduleAtFixedRate(framerateTask, 0, 5, TimeUnit.SECONDS);
        // TODO rimetti
        NativeExecutor.addShutdownHook();
//        DisplayInfo screenInfo = new DisplayManager().getPrimaryDisplay();
//        log.debug(screenInfo.getMonitorName());
        powerSavingManager.addPowerSavingTask();
    }

    /**
     * Delay autostart when on multi monitor, first instance must start capturing for first.
     */
    void manageAutoStart() {
        int timeToWait = 0;
        if ((config.getMultiMonitor() == 2 && JavaFXStarter.whoAmI == 2)
                || (config.getMultiMonitor() == 3 && JavaFXStarter.whoAmI == 3)) {
            timeToWait = 15;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> guiManager.startCapturingThreads(), timeToWait, TimeUnit.SECONDS);
    }

    /**
     * Manage localization
     */
    private void manageLocale() {
        Locale currentLocale;
        if (config.getLanguage() != null) {
            currentLocale = Locale.forLanguageTag(LocalizedEnum.fromBaseStr(Constants.Language.class, config.getLanguage()).name().toLowerCase());
        } else {
            currentLocale = Locale.ENGLISH;
            config.setLanguage(Constants.Language.EN.getBaseI18n());
            for (Constants.Language lang : Constants.Language.values()) {
                if (lang.name().equalsIgnoreCase(Locale.getDefault().getLanguage())) {
                    currentLocale = Locale.forLanguageTag(lang.name().toLowerCase());
                    config.setLanguage(lang.getBaseI18n());
                }
            }
        }
        bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, currentLocale);
    }

    /**
     * Check if it's time to activate the night mode
     */
    private void scheduleCheckForNightMode() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // Create a task that runs every 1 minutes
        Runnable framerateTask = FireflyLuciferin::checkForNightMode;
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 10, 60, TimeUnit.SECONDS);
    }

    /**
     * Handle an event on the serial port. Read the data and print it.
     *
     * @param event input event
     */
    public synchronized void serialEvent(SerialPortEvent event) {
        serialManager.handleSerialEvent(event);
    }

    /**
     * Initialize how many Threads to use in the ThreadPool and how many Executor to use
     */
    private void initThreadPool() {
        int numberOfCPUThreads = config.getNumberOfCPUThreads();
        threadPoolNumber = numberOfCPUThreads * 2;
        if (numberOfCPUThreads > 1) {
            if (!(config.getCaptureMethod().equals(Configuration.CaptureMethod.CPU.name()))) {
                executorNumber = numberOfCPUThreads;
            } else {
                executorNumber = numberOfCPUThreads * 3;
            }
        } else {
            executorNumber = numberOfCPUThreads;
        }
    }

    /**
     * Send color stram to the microcontroller
     * using DPsoftware Checksum
     *
     * @param leds array of LEDs containing the average color to display on the LED
     */
    private void sendColors(Color[] leds) throws IOException {
        if (!Constants.PowerSaving.DISABLED.equals(LocalizedEnum.fromBaseStr(Constants.PowerSaving.class, config.getPowerSaving()))) {
            if (powerSavingManager.isUnlockCheckLedDuplication()) {
                powerSavingManager.setUnlockCheckLedDuplication(false);
                powerSavingManager.checkForLedDuplication(leds);
            }
            if (powerSavingManager.isShutDownLedStrip()) {
                Arrays.fill(leds, new Color(0, 0, 0));
            }
        }
        if (Constants.Orientation.CLOCKWISE.equals((LocalizedEnum.fromBaseStr(Constants.Orientation.class, config.getOrientation())))) {
            Collections.reverse(Arrays.asList(leds));
        }
        if (config.getLedStartOffset() > 0) {
            List<Color> tempList = new ArrayList<>();
            List<Color> tempListHead = Arrays.asList(leds).subList(config.getLedStartOffset(), leds.length);
            List<Color> tempListTail = Arrays.asList(leds).subList(0, config.getLedStartOffset());
            tempList.addAll(tempListHead);
            tempList.addAll(tempListTail);
            leds = tempList.toArray(leds);
        }
        int i = 0;
        if (leds != null && leds[0] != null) {
            if (config.isFullFirmware() && config.isWirelessStream()) {
                // Single part stream
                if (ledNumber < Constants.FIRST_CHUNK || !Constants.JSON_STREAM) {
                    sendChunck(i, leds, 1);
                } else { // Multi part stream
                    // First Chunk
                    i = sendChunck(i, leds, 1);
                    // Second Chunk
                    i = sendChunck(i, leds, 2);
                    // Third Chunk
                    if (i >= Constants.SECOND_CHUNK && i < Constants.THIRD_CHUNK) {
                        i = sendChunck(i, leds, 3);
                    }
                    // Fourth Chunk
                    if (i >= Constants.THIRD_CHUNK && i < ledNumber) {
                        sendChunck(i, leds, 4);
                    }
                }
            } else {
                serialManager.sendColorsViaUSB(leds);
            }
        }
        FPS_CONSUMER_COUNTER++;
    }

    /**
     * Send single chunk to MQTT topic
     *
     * @param i           index
     * @param leds        LEDs array to send
     * @param chunkNumber chunk number
     * @return index of the remaining leds to send
     */
    int sendChunck(int i, Color[] leds, int chunkNumber) {
        int firstChunk = Constants.FIRST_CHUNK;
        StringBuilder ledStr = new StringBuilder();
        int ledNum = leds.length;
        if (Constants.JSON_STREAM) {
            ledStr.append("{" + Constants.LED_NUM).append(ledNum).append(",");
            ledStr.append("\"part\":").append(chunkNumber).append(",");
            ledStr.append(Constants.STREAM);
        } else {
            ledStr.append(ledNum).append(",");
            ledStr.append((AudioLoopback.AUDIO_BRIGHTNESS == 255 ? CommonUtility.getNightBrightness() : AudioLoopback.AUDIO_BRIGHTNESS)).append(",");
            firstChunk = Constants.MAX_CHUNK;
        }
        switch (chunkNumber) {
            case 1 -> {
                // First chunk equals MAX_CHUNK when in byte array
                while (i < firstChunk && i < ledNum) {
                    ledStr.append(leds[i].getRGB());
                    ledStr.append(",");
                    i++;
                }
            }
            case 2 -> {
                while (i >= Constants.FIRST_CHUNK && i < Constants.SECOND_CHUNK && i < ledNum) {
                    ledStr.append(leds[i].getRGB());
                    ledStr.append(",");
                    i++;
                }
            }
            case 3 -> {
                while (i >= Constants.SECOND_CHUNK && i < Constants.THIRD_CHUNK && i < ledNum) {
                    ledStr.append(leds[i].getRGB());
                    ledStr.append(",");
                    i++;
                }
            }
            case 4 -> {
                while (i >= Constants.THIRD_CHUNK && i < ledNum) {
                    ledStr.append(leds[i].getRGB());
                    ledStr.append(",");
                    i++;
                }
            }
        }
        if (Constants.JSON_STREAM) {
            ledStr.append(".");
            MQTTManager.stream(ledStr.toString().replace(",.", "") + "]}");
        } else {
            // UDP stream or MQTT stream
            if (config.getStreamType().equals(Constants.StreamType.UDP.getStreamType())) {
                if (udpClient == null || udpClient.socket.isClosed()) {
                    try {
                        udpClient = new UdpClient(CommonUtility.getDeviceToUse().getDeviceIP());
                    } catch (SocketException | UnknownHostException e) {
                        udpClient = null;
                    }
                }
                assert udpClient != null;
                udpClient.manageStream(leds);
            } else {
                ledStr.append("0");
                MQTTManager.stream(ledStr.toString());
            }
        }
        return i;
    }

    /**
     * Fast consumer
     */
    @SuppressWarnings("InfiniteLoopStatement")
    void consume() throws InterruptedException, IOException {
        while (true) {
            Color[] num = sharedQueue.take();
            if (RUNNING) {
                if (CommonUtility.isSingleDeviceMultiScreen()) {
                    if (num.length == MessageServer.totalLedNum) {
                        sendColors(num);
                    }
                } else if (num.length == ledNumber) {
                    sendColors(num);
                }
            }
        }
    }

    /**
     * Clean and Close Serial Output Stream
     */
    private void clean() {
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        if (serial != null) {
            serial.close();
        }
    }

}
