/*
  FireflyLuciferin.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020  Davide Perini

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

import com.sun.jna.Platform;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.Getter;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.GUIManager;
import org.dpsoftware.gui.SettingsController;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Firefly Luciferin for PC Ambilight
 * (https://github.com/sblantipodi/pc_ambilight)
 */
@Getter
public class FireflyLuciferin extends Application {

    private static final Logger logger = LoggerFactory.getLogger(FireflyLuciferin.class);

    // Number of CPU Threads to use, this app is heavy multithreaded,
    // high cpu cores equals to higher framerate but big CPU usage
    // 4 Threads are enough for 24FPS on an Intel i7 5930K@4.2GHz
    // 3 thread is enough for 30FPS with GPU Hardware Acceleration and uses nearly no CPU
    private int threadPoolNumber;
    private int executorNumber;
    // Calculate Screen Capture Framerate and how fast your microcontroller can consume it
    public static float FPS_CONSUMER_COUNTER;
    public static float FPS_PRODUCER_COUNTER;
    public static float FPS_CONSUMER;
    public static float FPS_PRODUCER;
    // Serial output stream
    private SerialPort serial;
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
    // GStreamer Rendering pipeline
    public static Pipeline pipe;
    public static GUIManager guiManager;
    public static boolean communicationError = false;
    private static Color colorInUse;
    public static int usbBrightness = 255;
    // MQTT
    MQTTManager mqttManager = null;
    public static String version = "";


    /**
     * Constructor
     */
    public FireflyLuciferin() {

        getCIComputedVersion();
        String ledMatrixInUse = "";
        try {
            loadConfigurationYaml();
            ledMatrixInUse = config.getDefaultLedMatrix();
        } catch (NullPointerException e) {
            logger.error("Please configure the app.");
            System.exit(0);
        }
        sharedQueue = new LinkedBlockingQueue<>(config.getLedMatrixInUse(ledMatrixInUse).size() * 30);
        imageProcessor = new ImageProcessor();
        ledNumber = config.getLedMatrixInUse(ledMatrixInUse).size();
        usbBrightness = config.getBrightness();
        initSerial();
        initOutputStream();
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
        if (!Platform.isLinux()) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(threadPoolNumber);

        // Desktop Duplication API producers
        if ((config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL.name())) || (config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name())) || (config.getCaptureMethod().equals(Configuration.CaptureMethod.AVFVIDEOSRC.name()))) {
            launchDDUPLGrabber(scheduledExecutorService);
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
        }, scheduledExecutorService).thenAcceptAsync(logger::info).exceptionally(e -> {
            clean();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
            return null;
        });

        if (config.isMqttEnable()) {
            mqttManager = new MQTTManager();
        } else {
            logger.debug(Constants.MQTT_DISABLED);
        }
        // Manage tray icon and framerate dialog
        guiManager = new GUIManager(mqttManager, stage);
        guiManager.initTray();
        getFPS();

        if (config.isAutoStartCapture()) {
            guiManager.startCapturingThreads();
        }
        if (!config.isMqttEnable()) {
            manageSolidLed();
        }

    }

    /**
     * Extract project version computed from Continuous Integration
     */
    private void getCIComputedVersion() {

        final Properties properties = new Properties();
        try {
            properties.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
            version = properties.getProperty("version");
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    /**
     * Windows 8/10 Desktop Duplication API screen grabber (GStreamer)
     * @param scheduledExecutorService executor service used to restart grabbing if it fails
     */
    void launchDDUPLGrabber(ScheduledExecutorService scheduledExecutorService) {

        imageProcessor.initGStreamerLibraryPaths();
        Gst.init(Constants.SCREEN_GRABBER, "");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        AtomicInteger pipelineRetry = new AtomicInteger();

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (RUNNING && FPS_PRODUCER_COUNTER == 0) {
                pipelineRetry.getAndIncrement();
                if (pipe == null || !pipe.isPlaying() || pipelineRetry.get() >= 2) {
                    if (pipe != null) {
                        logger.debug("Restarting pipeline");
                        pipe.stop();
                    } else {
                        logger.debug("Starting a new pipeline");
                    }
                    GStreamerGrabber vc = new GStreamerGrabber();
                    Bin bin;
                    if (Platform.isWindows()) {
                        bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_WINDOWS,true);
                    } else if (Platform.isLinux()) {
                        bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_LINUX,true);
                    } else {
                        bin = Gst.parseBinFromDescription(Constants.GSTREAMER_PIPELINE_MAC,true);
                    }
                    pipe = new Pipeline();
                    pipe.addMany(bin, vc.getElement());
                    Pipeline.linkMany(bin, vc.getElement());
                    JFrame f = new JFrame(Constants.SCREEN_GRABBER);
                    f.add(vc);
                    vc.setPreferredSize(new Dimension((int)screenSize.getWidth(), (int)screenSize.getHeight()));
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
                logger.info(Constants.SPAWNING_ROBOTS);
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
        config = sm.readConfig();
        if (config == null) {
            try {
                String fxml;
                if (Platform.isWindows() || Platform.isMac()) {
                    fxml = Constants.FXML_SETTINGS;
                } else {
                    fxml = Constants.FXML_SETTINGS_LINUX;
                }
                Scene scene = new Scene(GUIManager.loadFXML(fxml));
                Stage stage = new Stage();
                stage.setTitle("  " + Constants.SETTINGS);
                stage.setScene(scene);
                if (!SystemTray.isSupported() || com.sun.jna.Platform.isLinux()) {
                    stage.setOnCloseRequest(evt -> System.exit(0));
                }
                GUIManager.setStageIcon(stage);
                stage.showAndWait();
                config = sm.readConfig();
            } catch (IOException stageError) {
                logger.error(stageError.getMessage());
            }
        }

    }

    /**
     * Calculate Screen Capture Framerate and how fast your microcontroller can consume it
     */
    void getFPS() {

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // Create a task that runs every 5 seconds
        Runnable framerateTask = () -> {
            if (FPS_PRODUCER_COUNTER > 0 || FPS_CONSUMER_COUNTER > 0) {
                FPS_PRODUCER = FPS_PRODUCER_COUNTER / 5;
                FPS_CONSUMER = FPS_CONSUMER_COUNTER / 5;
                if (config.isExtendedLog()) {
                    logger.debug(" --* Producing @ " + FPS_PRODUCER + " FPS *-- "
                            + " --* Consuming @ " + FPS_CONSUMER + " FPS *-- ");
                }
                FPS_CONSUMER_COUNTER = FPS_PRODUCER_COUNTER = 0;
            } else {
                FPS_PRODUCER = FPS_CONSUMER = 0;
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(framerateTask, 0, 5, TimeUnit.SECONDS);

    }

    /**
     * Initialize Serial communication
     */
    private void initSerial() {

        CommPortIdentifier serialPortId = null;
        if (!(config.isMqttEnable() && config.isMqttStream())) {
            var enumComm = CommPortIdentifier.getPortIdentifiers();
            while (enumComm.hasMoreElements() && serialPortId == null) {
                CommPortIdentifier serialPortAvailable = (CommPortIdentifier) enumComm.nextElement();
                if (config.getSerialPort().equals(serialPortAvailable.getName()) || config.getSerialPort().equals(Constants.SERIAL_PORT_AUTO)) {
                    serialPortId = serialPortAvailable;
                }
            }
            try {
                if (serialPortId != null) {
                    logger.info(Constants.SERIAL_PORT_IN_USE + serialPortId.getName());
                    serial = serialPortId.open(this.getClass().getName(), config.getTimeout());
                    serial.setSerialPortParams(config.getDataRate(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    SettingsController.deviceTableData.add(new GlowWormDevice(Constants.USB_DEVICE, serialPortId.getName(), Constants.DASH, Constants.DASH));
                }
            } catch (PortInUseException | UnsupportedCommOperationException | NullPointerException e) {
                communicationError = true;
                GUIManager guiManager = new GUIManager();
                guiManager.showAlert(Constants.SERIAL_ERROR_TITLE,
                        Constants.SERIAL_ERROR_OPEN_HEADER,
                        Constants.SERIAL_ERROR_CONTEXT, Alert.AlertType.ERROR);
                logger.error(Constants.SERIAL_ERROR_OPEN_HEADER);
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
                GUIManager guiManager = new GUIManager();
                guiManager.showAlert(Constants.SERIAL_ERROR_TITLE,
                        Constants.SERIAL_ERROR_HEADER,
                        Constants.SERIAL_ERROR_CONTEXT, Alert.AlertType.ERROR);
                logger.error(e.getMessage());
                logger.error(Constants.SERIAL_ERROR_HEADER);
            }
        }

    }

    /**
     * Write Serial Stream to the Serial Output
     * using DPsoftware Checksum
     * @param leds array of LEDs containing the average color to display on the LED
     */
    private void sendColors(Color[] leds) throws IOException {

        if ("Clockwise".equals(config.getOrientation())) {
            Collections.reverse(Arrays.asList(leds));
        }
        if (config.getLedStartOffset() > 0) {
            java.util.List<Color> tempList = new ArrayList<>();
            java.util.List<Color> tempListHead = Arrays.asList(leds).subList(config.getLedStartOffset(), leds.length);
            List<Color> tempListTail = Arrays.asList(leds).subList(0, config.getLedStartOffset());
            tempList.addAll(tempListHead);
            tempList.addAll(tempListTail);
            leds = tempList.toArray(leds);
        }

        int i = 0;
        if (config.isMqttEnable() && config.isMqttStream()) {
            StringBuilder ledString = new StringBuilder("{" + "\"lednum\":" + ledNumber + ",\"stream\":[");
            while (i < ledNumber) {
                ledString.append(leds[i].getRGB());
                ledString.append(",");
                i++;
            }
            ledString.append(".");
            mqttManager.stream(ledString.toString().replace(",.","") + "]}");
        } else {
            sendColorsViaUSB(leds, usbBrightness);
        }
        FPS_CONSUMER_COUNTER++;

    }

    /**
     * Send color info via USB Serial
     * @param leds array with colors
     * @throws IOException can't write to serial
     */
    public static void sendColorsViaUSB(Color[] leds, int brightness) throws IOException {

        int i = 0, j = -1;

        byte[] ledsArray = new byte[(ledNumber * 3) + 8];

        // Adalight checksum
        int ledsCountHi = ((ledNumber - 1) >> 8) & 0xff;
        int ledsCountLo = (ledNumber - 1) & 0xff;
        int brightnessToSend = (brightness) & 0xff;

        ledsArray[++j] = (byte) ('D');
        ledsArray[++j] = (byte) ('P');
        ledsArray[++j] = (byte) ('s');
        ledsArray[++j] = (byte) ('o');
        ledsArray[++j] = (byte) (ledsCountHi);
        ledsArray[++j] = (byte) (ledsCountLo);
        ledsArray[++j] = (byte) (brightnessToSend);
        ledsArray[++j] = (byte) ((ledsCountHi ^ ledsCountLo ^ brightnessToSend ^ 0x55));

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

    /**
     * Write Serial Stream to the Serial Output
     *
     * @param robot an AWT Robot instance for screen capture.
     *              One instance every three threads seems to be the hot spot for performance.
     */
    private void producerTask(Robot robot) {

        sharedQueue.offer(ImageProcessor.getColors(robot, null));
        FPS_PRODUCER_COUNTER++;
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
                if (num.length == ledNumber) {
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
                e.printStackTrace();
            }
        }
        if(serial != null) {
            serial.close();
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
                        usbBrightness = Integer.parseInt(color[3]);
                    } else {
                        colorToUse[0] = colorInUse;
                    }
                    try {
                        sendColorsViaUSB(colorToUse, usbBrightness);
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
        }, 2, 2, TimeUnit.SECONDS);

    }

}
