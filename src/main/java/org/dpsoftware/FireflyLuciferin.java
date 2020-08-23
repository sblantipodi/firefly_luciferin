/*
  FireflyLuciferin.java

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

package org.dpsoftware;

import com.sun.jna.Platform;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.GUIManager;
import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.*;


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
    private OutputStream output;
    // LED strip, monitor and microcontroller config
    public static Configuration config;
    // Start and Stop threads
    public static boolean RUNNING = false;
    // This queue orders elements FIFO. Producer offers some data, consumer throws data to the Serial port
    public static BlockingQueue<Color[]> sharedQueue;
    // Image processing
    ImageProcessor imageProcessor;
    // Number of LEDs on the strip
    private final int ledNumber;
    // GStreamer Rendering pipeline
    public static Pipeline pipe;
    public static GUIManager guiManager;
    public static boolean communicationError = false;
    // MQTT
    MQTTManager mqttManager = null;
    // JavaFX scene
    public static final String VERSION = "1.2.0";


    /**
     * Constructor
     */
    public FireflyLuciferin() {

        loadConfigurationYaml();
        String ledMatrixInUse = config.getDefaultLedMatrix();
        sharedQueue = new LinkedBlockingQueue<>(config.getLedMatrixInUse(ledMatrixInUse).size() * 30);
        imageProcessor = new ImageProcessor();
        ledNumber = config.getLedMatrixInUse(ledMatrixInUse).size();
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

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error("Can't set system look n feel");
        }
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(threadPoolNumber);

        // Desktop Duplication API producers
        if ((config.getCaptureMethod().equals(Configuration.WindowsCaptureMethod.DDUPL.name())) || (config.getCaptureMethod().equals(Configuration.LinuxCaptureMethod.XIMAGESRC.name()))) {
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
            return "Something went wrong.";
        }, scheduledExecutorService).thenAcceptAsync(logger::info).exceptionally(e -> {
            clean();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
            return null;
        });

        if (config.isMqttEnable()) {
            mqttManager = new MQTTManager();
        } else {
            logger.debug("MQTT disabled.");
        }
        // Manage tray icon and framerate dialog
        guiManager = new GUIManager(mqttManager, stage);
        guiManager.initTray();
        getFPS();

    }

    /**
     * Windows 8/10 Desktop Duplication API screen grabber (GStreamer)
     * @param scheduledExecutorService executor service used to restart grabbing if it fails
     */
    void launchDDUPLGrabber(ScheduledExecutorService scheduledExecutorService) {

        imageProcessor.initGStreamerLibraryPaths();
        Gst.init("ScreenGrabber", "");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (RUNNING && FPS_PRODUCER_COUNTER == 0) {
                GStreamerGrabber vc = new GStreamerGrabber();
                Bin bin;
                if (Platform.isWindows()) {
                    bin = Gst.parseBinFromDescription("dxgiscreencapsrc ! videoscale ! videoconvert",true);
                } else {
                    bin = Gst.parseBinFromDescription("ximagesrc ! videoscale ! videoconvert",true);
                }
                pipe = new Pipeline();
                pipe.addMany(bin, vc.getElement());
                Pipeline.linkMany(bin, vc.getElement());
                JFrame f = new JFrame("ScreenGrabber");
                f.add(vc);
                vc.setPreferredSize(new Dimension((int)screenSize.getWidth(), (int)screenSize.getHeight()));
                f.pack();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                pipe.play();
                f.setVisible(false);
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
            if (!(config.getCaptureMethod().equals(Configuration.WindowsCaptureMethod.WinAPI.name())) && i%3 == 0) {
                robot = new Robot();
                logger.info("Spawning new robot for capture");
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
                if (Platform.isWindows()) {
                    fxml = "settings";
                } else {
                    fxml = "linuxSettings";
                }
                Scene scene = new Scene(GUIManager.loadFXML(fxml));
                Stage stage = new Stage();
                stage.setTitle("  Settings");
                stage.setScene(scene);
                if (!SystemTray.isSupported() || com.sun.jna.Platform.isLinux()) {
                    stage.setOnCloseRequest(evt -> System.exit(0));
                }
                GUIManager.setStageIcon(stage);
                stage.showAndWait();
                config = sm.readConfig();
            } catch (IOException stageError) {
                logger.error(stageError.toString());
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
                //logger.debug(" --* Producing @ " + FPS_PRODUCER + " FPS *-- "
                //    + " --* Consuming @ " + FPS_CONSUMER + " FPS *-- ");
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
                if (config.getSerialPort().equals(serialPortAvailable.getName()) || config.getSerialPort().equals("AUTO")) {
                    serialPortId = serialPortAvailable;
                }
            }
            try {
                if (serialPortId != null) {
                    logger.info("Serial Port in use: " + serialPortId.getName());
                    serial = serialPortId.open(this.getClass().getName(), config.getTimeout());
                    serial.setSerialPortParams(config.getDataRate(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                }
            } catch (PortInUseException | UnsupportedCommOperationException | NullPointerException e) {
                communicationError = true;
                GUIManager guiManager = new GUIManager();
                guiManager.showErrorAlert(guiManager.getSERIAL_ERROR_TITLE(),
                        guiManager.getSERIAL_ERROR_OPEN_HEADER(),
                        guiManager.getSERIAL_ERROR_CONTEXT());
                logger.error(guiManager.getSERIAL_ERROR_OPEN_HEADER());
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
            if (!(config.getCaptureMethod().equals(Configuration.WindowsCaptureMethod.CPU.name()))) {
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
                guiManager.showErrorAlert(guiManager.getSERIAL_ERROR_TITLE(),
                        guiManager.getSERIAL_ERROR_HEADER(),
                        guiManager.getSERIAL_ERROR_CONTEXT());
                logger.error(e.toString());
                logger.error(guiManager.getSERIAL_ERROR_HEADER());
            }
        }

    }

    /**
     * Write Serial Stream to the Serial Output
     * using Adalight Checksum
     * @param leds array of LEDs containing the average color to display on the LED
     */
    private void sendColors(Color[] leds) throws IOException {

        if ("Clockwise".equals(config.getOrientation())) {
            Collections.reverse(Arrays.asList(leds));
        }

        int i = 0, j = -1;
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

            byte[] ledsArray = new byte[(ledNumber * 3) + 6];

            // Adalight checksum
            int ledsCountHi = ((ledNumber - 1) >> 8) & 0xff;
            int ledsCountLo = (ledNumber - 1) & 0xff;

            ledsArray[++j] = (byte) ('A');
            ledsArray[++j] = (byte) ('d');
            ledsArray[++j] = (byte) ('a');
            ledsArray[++j] = (byte) (ledsCountHi);
            ledsArray[++j] = (byte) (ledsCountLo);
            ledsArray[++j] = (byte) ((ledsCountHi ^ ledsCountLo ^ 0x55));

            while (i < ledNumber) {
                ledsArray[++j] = (byte) leds[i].getRed();
                ledsArray[++j] = (byte) leds[i].getGreen();
                ledsArray[++j] = (byte) leds[i].getBlue();
                i++;
            }
            output.write(ledsArray);

        }

        FPS_CONSUMER_COUNTER++;

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

}
