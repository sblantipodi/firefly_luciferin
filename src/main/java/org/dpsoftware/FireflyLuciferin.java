/*
  FireflyLuciferin.java

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
package org.dpsoftware;

import gnu.io.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.audio.AudioLoopback;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.GUIManager;
import org.dpsoftware.gui.controllers.DevicesTabController;
import org.dpsoftware.gui.controllers.SettingsController;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.UpgradeManager;
import org.dpsoftware.managers.dto.MqttFramerateDto;
import org.dpsoftware.managers.dto.StateStatusDto;
import org.dpsoftware.network.MessageClient;
import org.dpsoftware.network.MessageServer;
import org.dpsoftware.utilities.CommonUtility;
import org.dpsoftware.utilities.PropertiesLoader;
import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Firefly Luciferin for PC Ambilight
 * (https://github.com/sblantipodi/pc_ambilight)
 */
@Slf4j
@Getter
public class FireflyLuciferin extends Application implements SerialPortEventListener {

    // Number of CPU Threads to use, this app is heavy multithreaded,
    // high cpu cores equals to higher framerate but big CPU usage
    // 4 Threads are enough for 24FPS on an Intel i7 5930K@4.2GHz
    // 3 thread is enough for 30FPS with GPU Hardware Acceleration and uses nearly no CPU
    private int threadPoolNumber;
    private int executorNumber;
    // Calculate Screen Capture Framerate and how fast your microcontroller can consume it
    public static float FPS_CONSUMER_COUNTER;
    public static float FPS_PRODUCER_COUNTER;
    public static float FPS_CONSUMER = 0;
    public static float FPS_PRODUCER = 0;
    public static float FPS_GW_CONSUMER = 0;
    public static SimpleDateFormat formatter;
    // Serial output stream
    public static SerialPort serial;
    private BufferedReader input;
    public static OutputStream output;
    // LED strip, monitor and microcontroller config
    public static Configuration config;
    // Start and Stop threads
    public static boolean RUNNING = false;
    // This queue orders elements FIFO. Producer offers some data, consumer throws data to the Serial port
    public static BlockingQueue<Color[]> sharedQueue;
    // Image processing
    ImageProcessor imageProcessor;
    // Number of LEDs on the strip
    public static int ledNumber;
    public static int ledNumHighLowCount;
    public static int ledNumHighLowCountSecondPart;
    // GStreamer Rendering pipeline
    public static Pipeline pipe;
    public static GUIManager guiManager;
    public static boolean communicationError = false;
    public static boolean serialConnected = false;
    private static Color colorInUse;
    public static int gpio = 0; // 0 means not set, firmware discards this value
    public static int baudRate = 0;
    public static int whiteTemperature = 0;
    public static int fireflyEffect = 0;
    public static boolean nightMode = false;
    // MQTT
    MQTTManager mqttManager = null;
    public static String version = "";
    public static String minimumFirmwareVersion = "";

    /**
     * Constructor
     */
    public FireflyLuciferin() {

        PropertiesLoader propertiesLoader = new PropertiesLoader();
        formatter = new SimpleDateFormat(Constants.DATE_FORMAT);
        // Extract project version computed from Continuous Integration (GitHub Actions)
        version = propertiesLoader.retrieveProperties(Constants.PROP_VERSION);
        String ledMatrixInUse = "";
        try {
            loadConfigurationYaml();
            ledMatrixInUse = config.getDefaultLedMatrix();
        } catch (NullPointerException e) {
            log.error("Please configure the app.");
            FireflyLuciferin.exit();
        }
        sharedQueue = new LinkedBlockingQueue<>(config.getLedMatrixInUse(ledMatrixInUse).size() * 30);
        imageProcessor = new ImageProcessor(true);
        imageProcessor.lastFrameTime = LocalDateTime.now();
        imageProcessor.checkForLedDuplicationTask();
        if (CommonUtility.isSingleDeviceMainInstance()) {
            MessageServer.messageServer = new MessageServer();
            MessageServer.initNumLed();
        }
        ledNumber = CommonUtility.isSingleDeviceMultiScreen() ? MessageServer.totalLedNum : config.getLedMatrixInUse(ledMatrixInUse).size();
        ledNumHighLowCount = ledNumber > Constants.SERIAL_CHUNK_SIZE ? Constants.SERIAL_CHUNK_SIZE - 1 : ledNumber - 1;
        ledNumHighLowCountSecondPart = ledNumber > Constants.SERIAL_CHUNK_SIZE ? ledNumber - Constants.SERIAL_CHUNK_SIZE : 0;
        whiteTemperature = config.getWhiteTemperature();
        baudRate = Constants.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + config.getBaudRate()).ordinal() + 1;
        // Check if I'm the main program, if yes and multi monitor, spawn other guys
        NativeExecutor.spawnNewInstances();
        if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
            initSerial();
            initOutputStream();
        }
        initThreadPool();

    }

    /**
     * Startup JavaFX context
     * @param args startup args
     */
    public static void main(String[] args) {

        launch(args);

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
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(threadPoolNumber);

        // Desktop Duplication API producers
        if ((config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name()))
                || (config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name()))
                || (config.getCaptureMethod().equals(Configuration.CaptureMethod.AVFVIDEOSRC.name()))) {
            launchAdvancedGrabber(scheduledExecutorService);
        } else { // Standard Producers
            launchStandardGrabber(scheduledExecutorService);
        }

        // Run a very fast consumer
        CompletableFuture.supplyAsync(() -> {
            try {
                consume();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
            return Constants.SOMETHING_WENT_WRONG;
        }, scheduledExecutorService).thenAcceptAsync(log::info).exceptionally(e -> {
            clean();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
            return null;
        });
        checkForNightMode();

        if (config.isMqttEnable()) {
            mqttManager = new MQTTManager();
        } else {
            log.debug(Constants.MQTT_DISABLED);
        }
        updateConfigFile();

        // Manage tray icon and framerate dialog
        guiManager = new GUIManager(stage);
        guiManager.initTray();
        getFPS();
        imageProcessor.calculateBorders();
        // If multi monitor, first instance, single instance, start message server
        if (CommonUtility.isSingleDeviceMainInstance()) {
            MessageServer.startMessageServer();
        }
        if (CommonUtility.isSingleDeviceOtherInstance()) {
            MessageClient.getSingleInstanceMultiScreenStatus();
        }
        if (config.isToggleLed() && (Constants.Effect.BIAS_LIGHT.getEffect().equals(config.getEffect())
                || Constants.Effect.MUSIC_MODE_VU_METER.getEffect().equals(config.getEffect())
                || Constants.Effect.MUSIC_MODE_BRIGHT.getEffect().equals(config.getEffect())
                || Constants.Effect.MUSIC_MODE_RAINBOW.getEffect().equals(config.getEffect()))) {
            manageAutoStart();
        }
        if (!config.isMqttEnable()) {
            manageSolidLed();
        }
        // Create a task that runs every 5 seconds, reconnect serial devices when needed
        ScheduledExecutorService serialscheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable framerateTask = () -> {
            if (!serialConnected) {
                if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
                    initSerial();
                }
            }
        };
        serialscheduledExecutorService.scheduleAtFixedRate(framerateTask, 0, 5, TimeUnit.SECONDS);

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
     * Launch Advanced screen grabber (DDUPL for Windows, ximagesrc for Linux)
     * @param scheduledExecutorService executor service used to restart grabbing if it fails
     */
    void launchAdvancedGrabber(ScheduledExecutorService scheduledExecutorService) {

        imageProcessor.initGStreamerLibraryPaths();
        //System.setProperty("gstreamer.GNative.nameFormats", "%s-0|lib%s-0|%s|lib%s");
        Gst.init(Constants.SCREEN_GRABBER, "");
        AtomicInteger pipelineRetry = new AtomicInteger();

        String linuxParams = null;
        if (NativeExecutor.isLinux()) {
            linuxParams = PipelineManager.getLinuxPipelineParams();
        }
        String finalLinuxParams = linuxParams;
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (!PipelineManager.pipelineStopping && RUNNING && FPS_PRODUCER_COUNTER == 0) {
                pipelineRetry.getAndIncrement();
                if (pipe == null || !pipe.isPlaying() || pipelineRetry.get() >= 2) {
                    if (pipe != null) {
                        log.debug("Restarting pipeline");
                        pipe.stop();
                    } else {
                        log.debug("Starting a new pipeline");
                    }
                    GStreamerGrabber vc = new GStreamerGrabber();
                    Bin bin;
                    if (NativeExecutor.isWindows()) {
                        bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_WINDOWS
                                .replace("{0}", String.valueOf(FireflyLuciferin.config.getMonitorNumber() - 1)),true);
                    } else if (NativeExecutor.isLinux()) {
                        bin = Gst.parseBinFromDescription(finalLinuxParams, true);
                    } else {
                        bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_MAC,true);
                    }
                    pipe = new Pipeline();
                    pipe.addMany(bin, vc.getElement());
                    Pipeline.linkMany(bin, vc.getElement());
                    JFrame f = new JFrame(Constants.SCREEN_GRABBER);
                    f.add(vc);
                    vc.setPreferredSize(new Dimension(config.getScreenResX(), config.getScreenResY()));
                    f.pack();
                    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    pipe.play();
                    f.setVisible(false);
                }
            } else {
                pipelineRetry.set(0);
            }
        }, 1, 2, TimeUnit.SECONDS);

    }

    /**
     * Producers for CPU and WinAPI capturing
     * @param scheduledExecutorService executor service used to restart grabbing if it fails
     * @throws AWTException GUI exception
     */
    void launchStandardGrabber(ScheduledExecutorService scheduledExecutorService) throws AWTException {

        Robot robot = null;

        for (int i = 0; i < executorNumber; i++) {
            // One AWT Robot instance every 3 threads seems to be the sweet spot for performance/memory.
            if (!(config.getCaptureMethod().equals(Configuration.CaptureMethod.WinAPI.name())) && i%3 == 0) {
                robot = new Robot();
                log.info(Constants.SPAWNING_ROBOTS);
            }
            Robot finalRobot = robot;
            // No need for completablefuture here, we wrote the queue with a producer and we forget it
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                if (RUNNING) {
                    producerTask(finalRobot);
                }
            }, 0, 25, TimeUnit.MILLISECONDS);
        }

    }

    /**
     * Load config yaml and create a default config if not present
     */
    void loadConfigurationYaml() {

        StorageManager sm = new StorageManager();
        config = sm.readConfig(false);
        if (config == null) {
            try {
                String fxml;
                fxml = Constants.FXML_SETTINGS;
                Scene scene = new Scene(GUIManager.loadFXML(fxml));
                Stage stage = new Stage();
                stage.setTitle("  " + Constants.SETTINGS);
                stage.setScene(scene);
                if (!NativeExecutor.isSystemTraySupported() || NativeExecutor.isLinux()) {
                    stage.setOnCloseRequest(evt -> FireflyLuciferin.exit());
                }
                GUIManager.setStageIcon(stage);
                stage.showAndWait();
                config = sm.readConfig(false);
            } catch (IOException stageError) {
                log.error(stageError.getMessage());
            }
        }

    }

    /**
     * Calculate Screen Capture Framerate and how fast your microcontroller can consume it
     */
    void getFPS() {

        AtomicInteger framerateAlert = new AtomicInteger();
        AtomicBoolean notified = new AtomicBoolean(false);
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        // Create a task that runs every 5 seconds
        Runnable framerateTask = () -> {

            if (FPS_PRODUCER_COUNTER > 0 || FPS_CONSUMER_COUNTER > 0) {
                if (CommonUtility.isSingleDeviceOtherInstance() && FireflyLuciferin.config.getEffect().contains(Constants.MUSIC_MODE)) {
                    FPS_PRODUCER = FPS_GW_CONSUMER;
                } else {
                    FPS_PRODUCER = FPS_PRODUCER_COUNTER / 5;
                }
                FPS_CONSUMER = FPS_CONSUMER_COUNTER / 5;
                CommonUtility.conditionedLog(this.getClass().getName(),
                        " --* Producing @ " + FPS_PRODUCER + " FPS *-- " + " --* Consuming @ " + FPS_GW_CONSUMER + " FPS *-- ");
                FPS_CONSUMER_COUNTER = FPS_PRODUCER_COUNTER = 0;
            } else {
                FPS_PRODUCER = FPS_CONSUMER = 0;
            }
            runBenchmark(framerateAlert, notified);
            if (config.isMqttEnable()) {
                MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FRAMERATE),
                        CommonUtility.toJsonString(new MqttFramerateDto(String.valueOf(FPS_PRODUCER), String.valueOf(FPS_CONSUMER))));
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 0, 5, TimeUnit.SECONDS);

    }

    /**
     * Check if it's time to activate the night mode
     */
    public static void checkForNightMode() {

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // Create a task that runs every 1 minutes
        Runnable framerateTask = () -> {
            if (!FireflyLuciferin.config.getNightModeBrightness().equals(Constants.NIGHT_MODE_OFF)) {
                LocalDateTime from = LocalDateTime.now(), to = LocalDateTime.now();
                from = from.withHour(LocalTime.parse(FireflyLuciferin.config.getNightModeFrom()).getHour());
                from = from.withMinute(LocalTime.parse(FireflyLuciferin.config.getNightModeFrom()).getMinute());
                to = to.withHour(LocalTime.parse(FireflyLuciferin.config.getNightModeTo()).getHour());
                to = to.withMinute(LocalTime.parse(FireflyLuciferin.config.getNightModeTo()).getMinute());
                if (from.isAfter(to)) {
                    to = to.plusDays(1);
                }
                nightMode = (LocalDateTime.now().isAfter(from) && LocalDateTime.now().isBefore(to));
            } else {
                nightMode = false;
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 0, 1, TimeUnit.MINUTES);

    }

    /**
     * Small benchmark to check if Glow Worm Luciferin firmware can keep up with Firefly Luciferin PC software
     * @param framerateAlert number of times Firefly was faster than Glow Worm
     * @param notified       don't alert user more than one time
     */
    private void runBenchmark(AtomicInteger framerateAlert, AtomicBoolean notified) {

        if (!notified.get()) {
            if ((FPS_PRODUCER > 0) && (framerateAlert.get() < Constants.NUMBER_OF_BENCHMARK_ITERATION)
                    && (FPS_GW_CONSUMER < FPS_PRODUCER - Constants.BENCHMARK_ERROR_MARGIN)) {
                framerateAlert.getAndIncrement();
            } else {
                framerateAlert.set(0);
            }
            if (FPS_GW_CONSUMER == 0 && framerateAlert.get() == 6 && config.isMqttEnable()) {
                log.debug("Glow Worm Luciferin is not responding, restarting...");
                NativeExecutor.restartNativeInstance();
            }
            if (framerateAlert.get() == Constants.NUMBER_OF_BENCHMARK_ITERATION && !notified.get() && FPS_GW_CONSUMER > 0) {
                notified.set(true);
                javafx.application.Platform.runLater(() -> {
                    int suggestedFramerate;
                    if (FPS_GW_CONSUMER > (60 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 60;
                    } else if (FPS_GW_CONSUMER > (50 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 50;
                    } else if (FPS_GW_CONSUMER > (40 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 40;
                    } else if (FPS_GW_CONSUMER > (30 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 30;
                    } else if (FPS_GW_CONSUMER > (25 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 25;
                    } else if (FPS_GW_CONSUMER > (20 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 20;
                    } else if (FPS_GW_CONSUMER > (15 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 15;
                    } else if (FPS_GW_CONSUMER > (10 + Constants.BENCHMARK_ERROR_MARGIN)) {
                        suggestedFramerate = 10;
                    } else {
                        suggestedFramerate = 5;
                    }
                    log.error(Constants.FRAMERATE_HEADER + ". " + Constants.FRAMERATE_CONTEXT.replace("{0}", String.valueOf(suggestedFramerate)));
                    if (config.isSyncCheck()) {
                        Optional<ButtonType> result = guiManager.showAlert(Constants.FRAMERATE_TITLE, Constants.FRAMERATE_HEADER,
                                Constants.FRAMERATE_CONTEXT.replace("{0}", String.valueOf(suggestedFramerate)), Alert.AlertType.CONFIRMATION);
                        ButtonType button = result.orElse(ButtonType.OK);
                        if (button == ButtonType.OK) {
                            try {
                                StorageManager sm = new StorageManager();
                                config.setDesiredFramerate(String.valueOf(suggestedFramerate));
                                sm.writeConfig(config, null);
                                SettingsController settingsController = new SettingsController();
                                settingsController.exit(null);
                            } catch (IOException ioException) {
                                log.error("Can't write config file.");
                            }
                        }
                    }
                });
            }
        }

    }

    /**
     * Initialize Serial communication
     */
    private void initSerial() {

        CommPortIdentifier serialPortId = null;
        if (!(config.isMqttEnable() && config.isMqttStream())) {
            int numberOfSerialDevices = 0;
            var enumComm = CommPortIdentifier.getPortIdentifiers();
            while (enumComm.hasMoreElements()) {
                numberOfSerialDevices++;
                CommPortIdentifier serialPortAvailable = (CommPortIdentifier) enumComm.nextElement();
                if (config.getSerialPort().equals(serialPortAvailable.getName()) || config.getSerialPort().equals(Constants.SERIAL_PORT_AUTO)) {
                    serialPortId = serialPortAvailable;
                }
            }
            try {
                if (serialPortId != null) {
                    log.debug(Constants.SERIAL_PORT_IN_USE + serialPortId.getName() + ", connecting...");
                    serial = serialPortId.open(this.getClass().getName(), config.getTimeout());
                    serial.setSerialPortParams(Integer.parseInt(config.getBaudRate()), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    input = new BufferedReader(new InputStreamReader(serial.getInputStream()));
                    // add event listeners
                    serial.addEventListener(this);
                    serial.notifyOnDataAvailable(true);
                    DevicesTabController.deviceTableData.add(new GlowWormDevice(Constants.USB_DEVICE, serialPortId.getName(),
                            Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH, Constants.DASH,
                            FireflyLuciferin.formatter.format(new Date()), Constants.DASH,  Constants.DASH, Constants.DASH));
                    GUIManager guiManager = new GUIManager();
                    if (numberOfSerialDevices > 1 && config.getSerialPort().equals(Constants.SERIAL_PORT_AUTO)) {
                        communicationError = true;
                        guiManager.showAlert(Constants.SERIAL_ERROR_TITLE,
                                Constants.SERIAL_PORT_AMBIGUOUS,
                                Constants.SERIAL_PORT_AMBIGUOUS_CONTEXT, Alert.AlertType.ERROR);
                        log.error(Constants.SERIAL_ERROR_OPEN_HEADER);
                    }
                    log.debug("Connected: Serial " + serialPortId.getName());
                    if (FireflyLuciferin.guiManager != null) {
                        FireflyLuciferin.guiManager.resetTray();
                    }
                    serialConnected = true;
                    communicationError = false;
                    initOutputStream();
                }
            } catch (PortInUseException | UnsupportedCommOperationException | NullPointerException | IOException | TooManyListenersException e) {
                log.error(e.getMessage());
                communicationError = true;
            }
        }

    }

    /**
     * Return the list of connected serial devices, available or not
     * @return available devices
     */
    public static Map<String, Boolean> getAvailableDevices() {

        CommPortIdentifier serialPortId = null;
        var enumComm = CommPortIdentifier.getPortIdentifiers();
        Map<String, Boolean> availableDevice = new HashMap<>();
        while (enumComm.hasMoreElements()) {
            try {
                serialPortId = (CommPortIdentifier) enumComm.nextElement();
                if (serialPortId != null) {
                    serial = serialPortId.open(FireflyLuciferin.class.getName(), config != null ? config.getTimeout() : 2000);
                    availableDevice.put(serialPortId.getName(), true);
                    serial.close();
                }
            } catch (PortInUseException | NullPointerException e) {
                if (serialPortId != null) {
                    availableDevice.put(serialPortId.getName(), false);
                }
            }
        }
        return availableDevice;

    }

    /**
     * Return the number of the connected devices
     * @return connected devices
     */
    @SuppressWarnings("unused")
    static int getConnectedDevices() {

        var enumComm = CommPortIdentifier.getPortIdentifiers();
        int numberOfSerialDevices = 0;
        while (enumComm.hasMoreElements()) {
            numberOfSerialDevices++;
        }
        return numberOfSerialDevices;

    }

    /**
     * Handle an event on the serial port. Read the data and print it.
     * @param event input event
     */
    public synchronized void serialEvent(SerialPortEvent event) {

        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                if (input.ready()) {
                    String inputLine = input.readLine();
                    CommonUtility.conditionedLog(this.getClass().getName(), inputLine);
                    DevicesTabController.deviceTableData.forEach(glowWormDevice -> {
                        if (glowWormDevice.getDeviceName().equals(Constants.USB_DEVICE)) {
                            glowWormDevice.setLastSeen(FireflyLuciferin.formatter.format(new Date()));
                            // Skipping the Setting LED loop from Glow Worm Luciferin Serial communication
                            if (!inputLine.contains(Constants.SETTING_LED_SERIAL)) {
                                if (inputLine.contains(Constants.SERIAL_VERSION)) {
                                    glowWormDevice.setDeviceVersion(inputLine.replace(Constants.SERIAL_VERSION, ""));
                                } else if (inputLine.contains(Constants.SERIAL_LED_NUM)) {
                                    glowWormDevice.setNumberOfLEDSconnected(inputLine.replace(Constants.SERIAL_LED_NUM, ""));
                                } else if (inputLine.contains(Constants.SERIAL_BOARD)) {
                                    glowWormDevice.setDeviceBoard(inputLine.replace(Constants.SERIAL_BOARD, ""));
                                } else if (inputLine.contains(Constants.SERIAL_MAC)) {
                                    glowWormDevice.setMac(inputLine.replace(Constants.SERIAL_MAC, ""));
                                } else if (inputLine.contains(Constants.SERIAL_GPIO)) {
                                    glowWormDevice.setGpio(inputLine.replace(Constants.SERIAL_GPIO, ""));
                                } else if (inputLine.contains(Constants.SERIAL_FIRMWARE)) {
                                    glowWormDevice.setFirmwareType(inputLine.replace(Constants.SERIAL_FIRMWARE, ""));
                                } else if (inputLine.contains(Constants.SERIAL_MQTTTOPIC)) {
                                    glowWormDevice.setMqttTopic(inputLine.replace(Constants.SERIAL_MQTTTOPIC, ""));
                                } else if (inputLine.contains(Constants.SERIAL_BAUDRATE)) {
                                    boolean validBaudrate = true;
                                    int receivedBaudrate = Integer.parseInt(inputLine.replace(Constants.SERIAL_BAUDRATE, ""));
                                    if (!(receivedBaudrate >= 1 && receivedBaudrate <= 7)) {
                                        validBaudrate = false;
                                    }
                                    glowWormDevice.setBaudRate(validBaudrate ? Constants.BaudRate.values()[receivedBaudrate - 1].getBaudRate() : Constants.DASH);
                                } else if (!config.isMqttEnable() && inputLine.contains(Constants.SERIAL_FRAMERATE)) {
                                    FPS_GW_CONSUMER = Float.parseFloat(inputLine.replace(Constants.SERIAL_FRAMERATE, ""));
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                // We don't care about this exception
            }
        }

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
     * Initialize OutputStream
     */
    private void initOutputStream() {

        if (!(config.isMqttEnable() && config.isMqttStream()) && !communicationError) {
            try {
                output = serial.getOutputStream();
            } catch (IOException | NullPointerException e) {
                communicationError = true;
                log.error(e.getMessage());
                log.error(Constants.SERIAL_ERROR_HEADER);
            }
        }

    }

    /**
     * Send color stram to the microcontroller
     * using DPsoftware Checksum
     * @param leds array of LEDs containing the average color to display on the LED
     */
    private void sendColors(Color[] leds) throws IOException {

        if (!config.getPowerSaving().equals(Constants.PowerSaving.DISABLED.getPowerSaving())) {
            if (imageProcessor.ledArray == null || imageProcessor.unlockCheckLedDuplication) {
                imageProcessor.checkForLedDuplication(leds);
            }
            if (imageProcessor.shutDownLedStrip) {
                Arrays.fill(leds, new Color(0,0,0));
            }
        }
        if (Constants.CLOCKWISE.equals(config.getOrientation())) {
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
        if (config.isMqttEnable() && config.isMqttStream()) {
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
            sendColorsViaUSB(leds);
        }
        FPS_CONSUMER_COUNTER++;

    }

    /**
     * Send single chunk to MQTT topic
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
            case 1:
                // First chunk equals MAX_CHUNK when in byte array
                while (i < firstChunk && i < ledNum) {
                    ledStr.append(leds[i].getRGB());
                    ledStr.append(",");
                    i++;
                }
                break;
            case 2:
                while (i >= Constants.FIRST_CHUNK && i < Constants.SECOND_CHUNK && i < ledNum) {
                    ledStr.append(leds[i].getRGB());
                    ledStr.append(",");
                    i++;
                }
                break;
            case 3:
                while (i >= Constants.SECOND_CHUNK && i < Constants.THIRD_CHUNK && i < ledNum) {
                    ledStr.append(leds[i].getRGB());
                    ledStr.append(",");
                    i++;
                }
                break;
            case 4:
                while (i >= Constants.THIRD_CHUNK && i < ledNum) {
                    ledStr.append(leds[i].getRGB());
                    ledStr.append(",");
                    i++;
                }
                break;
        }
        if (Constants.JSON_STREAM) {
            ledStr.append(".");
            MQTTManager.stream(ledStr.toString().replace(",.","") + "]}");
        } else {
            ledStr.append("0");
            MQTTManager.stream(ledStr.toString());
        }
        return i;

    }

    /**
     * Send color info via USB Serial
     * @param leds array with colors
     * @throws IOException can't write to serial
     */
    public static void sendColorsViaUSB(Color[] leds) throws IOException {

        // Effect is set via MQTT when using Full Firmware
        if (config.isMqttEnable()) {
            fireflyEffect = 100;
        } else {
            for (Constants.Effect ef : Constants.Effect.values()) {
                if(ef.getEffect().equals(FireflyLuciferin.config.getEffect())) {
                    fireflyEffect = ef.ordinal() + 1;
                }
            }
        }
        if (!UpgradeManager.serialVersionOk) {
            UpgradeManager upgradeManager = new UpgradeManager();
            // Check if the connected device match the minimum firmware version requirements for this Firefly Luciferin version
            Boolean firmwareMatchMinRequirements = upgradeManager.firmwareMatchMinimumRequirements();
            if (firmwareMatchMinRequirements != null) {
                if (firmwareMatchMinRequirements) {
                    UpgradeManager.serialVersionOk = true;
                }
            }
        } else {
            int i = 0, j = -1;
            byte[] ledsArray = new byte[(ledNumber * 3) + 15];
            // DPsoftware checksum
            int ledsCountHi = ((ledNumHighLowCount) >> 8) & 0xff;
            int ledsCountLo = (ledNumHighLowCount) & 0xff;
            int loSecondPart = (ledNumHighLowCountSecondPart) & 0xff;
            int brightnessToSend = (AudioLoopback.AUDIO_BRIGHTNESS == 255 ? CommonUtility.getNightBrightness() : AudioLoopback.AUDIO_BRIGHTNESS) & 0xff;
            int gpioToSend = (gpio) & 0xff;
            int baudRateToSend = (baudRate) & 0xff;
            int whiteTempToSend = (whiteTemperature) & 0xff;
            int fireflyEffectToSend = (fireflyEffect) & 0xff;

            ledsArray[++j] = (byte) ('D');
            ledsArray[++j] = (byte) ('P');
            ledsArray[++j] = (byte) ('s');
            ledsArray[++j] = (byte) ('o');
            ledsArray[++j] = (byte) ('f');
            ledsArray[++j] = (byte) ('t');
            ledsArray[++j] = (byte) (ledsCountHi);
            ledsArray[++j] = (byte) (ledsCountLo);
            ledsArray[++j] = (byte) (loSecondPart);
            ledsArray[++j] = (byte) (brightnessToSend);
            ledsArray[++j] = (byte) (gpioToSend);
            ledsArray[++j] = (byte) (baudRateToSend);
            ledsArray[++j] = (byte) (whiteTempToSend);
            ledsArray[++j] = (byte) (fireflyEffectToSend);
            ledsArray[++j] = (byte) ((ledsCountHi ^ ledsCountLo ^ loSecondPart ^ brightnessToSend ^ gpioToSend ^ baudRateToSend ^ whiteTempToSend ^ fireflyEffectToSend ^ 0x55));

            if (leds.length == 1) {
                colorInUse = leds[0];
                while (i < ledNumber) {
                    ledsArray[++j] = (byte) leds[0].getRed();
                    ledsArray[++j] = (byte) leds[0].getGreen();
                    ledsArray[++j] = (byte) leds[0].getBlue();
                    i++;
                }
            } else {
                while (i < ledNumber) {
                    ledsArray[++j] = (byte) leds[i].getRed();
                    ledsArray[++j] = (byte) leds[i].getGreen();
                    ledsArray[++j] = (byte) leds[i].getBlue();
                    i++;
                }
            }
            output.write(ledsArray);
        }

    }

    /**
     * Write Serial Stream to the Serial Output
     * @param robot an AWT Robot instance for screen capture.
     *              One instance every three threads seems to be the hot spot for performance.
     */
    private void producerTask(Robot robot) {

        if (!AudioLoopback.RUNNING_AUDIO || Constants.Effect.MUSIC_MODE_BRIGHT.getEffect().equals(FireflyLuciferin.config.getEffect())
                || Constants.Effect.MUSIC_MODE_RAINBOW.getEffect().equals(FireflyLuciferin.config.getEffect())) {
            PipelineManager.offerToTheQueue(ImageProcessor.getColors(robot, null));
            FPS_PRODUCER_COUNTER++;
        }
        //System.gc(); // uncomment when hammering the JVM

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

        if(output != null) {
            try {
                output.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        if(serial != null) {
            serial.close();
        }

    }

    /**
     * Grecefully exit the app
     */
    public static void exit() {

        exitOtherInstances();
        if (FireflyLuciferin.serial != null) {
            FireflyLuciferin.serial.removeEventListener();
            FireflyLuciferin.serial.close();
        }
        AudioLoopback.RUNNING_AUDIO = false;
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
     * Check SOLID LEDs config and refresh LED strip state accordingly
     * This function works with GlowWormLuciferin Light, MQTT version does not need it
     */
    void manageSolidLed() {

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (!RUNNING) {
                if (config.isToggleLed() && !config.isMqttEnable()) {
                    Color[] colorToUse = new Color[1];
                    if (colorInUse == null) {
                        String[] color = FireflyLuciferin.config.getColorChooser().split(",");
                        colorToUse[0] = new Color(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]));
                        config.setBrightness(Integer.parseInt(color[3]));
                    } else {
                        colorToUse[0] = colorInUse;
                    }
                    try {
                        if (FireflyLuciferin.config.getEffect().equals(Constants.Effect.RAINBOW.getEffect())) {
                            for (int i=0; i <= 10; i++) {
                                sendColorsViaUSB(colorToUse);
                            }
                        } else {
                            sendColorsViaUSB(colorToUse);
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        }, 2, 100, TimeUnit.MILLISECONDS);

    }

    /**
     * Check if the config file updated, if not, write a new one
     * @throws IOException can't write to config file
     */
    private void updateConfigFile() throws IOException {

        // Firefly Luciferin v1.9.4 introduced a new aspect ratio, writing it without user interactions
        // Firefly Luciferin v1.10.2 introduced a config version and a refactored LED matrix
        // Firefly Luciferin v1.11.3 introduced a white temperature and a refactored LED matrix
        if (config.getLedMatrix().size() < Constants.AspectRatio.values().length || config.getConfigVersion().isEmpty() || config.getWhiteTemperature() == 0) {
            log.debug("Config file is old, writing a new one.");
            LEDCoordinate ledCoordinate = new LEDCoordinate();
            config.getLedMatrix().put(Constants.AspectRatio.FULLSCREEN.getAspectRatio(), ledCoordinate.initFullScreenLedMatrix(config.getScreenResX(),
                    config.getScreenResY(), config.getBottomRightLed(), config.getRightLed(), config.getTopLed(), config.getLeftLed(),
                    config.getBottomLeftLed(), config.getBottomRowLed(), config.isSplitBottomRow()));
            config.getLedMatrix().put(Constants.AspectRatio.LETTERBOX.getAspectRatio(), ledCoordinate.initLetterboxLedMatrix(config.getScreenResX(),
                    config.getScreenResY(), config.getBottomRightLed(), config.getRightLed(), config.getTopLed(), config.getLeftLed(),
                    config.getBottomLeftLed(), config.getBottomRowLed(), config.isSplitBottomRow()));
            config.getLedMatrix().put(Constants.AspectRatio.PILLARBOX.getAspectRatio(), ledCoordinate.initPillarboxMatrix(config.getScreenResX(),
                    config.getScreenResY(), config.getBottomRightLed(), config.getRightLed(), config.getTopLed(), config.getLeftLed(),
                    config.getBottomLeftLed(), config.getBottomRowLed(), config.isSplitBottomRow()));
            config.setConfigVersion(FireflyLuciferin.version);
            if (config.getWhiteTemperature() == 0) {
                config.setWhiteTemperature(Constants.WhiteTemperature.UNCORRECTEDTEMPERATURE.ordinal() + 1);
            }
            StorageManager sm = new StorageManager();
            sm.writeConfig(config, null);
        }

    }

}
