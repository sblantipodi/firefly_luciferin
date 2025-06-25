/*
  FireflyLuciferin.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.audio.AudioSingleton;
import org.dpsoftware.config.*;
import org.dpsoftware.grabber.GrabberManager;
import org.dpsoftware.grabber.GrabberSingleton;
import org.dpsoftware.grabber.ImageProcessor;
import org.dpsoftware.gui.GuiManager;
import org.dpsoftware.managers.*;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.network.MessageClient;
import org.dpsoftware.network.MessageServer;
import org.dpsoftware.network.NetworkSingleton;
import org.dpsoftware.network.tcpUdp.UdpServer;
import org.dpsoftware.utilities.CommonUtility;
import org.dpsoftware.utilities.PropertiesLoader;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Luciferin is a generic term for the light-emitting compound found in organisms that generate bioluminescence like Fireflies and Glow Worms.
 * Firefly Luciferin is a Java Fast Screen Capture PC software designed for the Glow Worm Luciferin firmware, the combination of this
 * software creates the perfect Bias Lighting and Ambient Light system for PC.
 * Written in Java with a native flavour for Windows and Linux.
 */
@Slf4j
@Getter
public class FireflyLuciferin extends Application {

    private final ImageProcessor imageProcessor;
    private final GrabberManager grabberManager;
    private final PowerSavingManager powerSavingManager;
    // Serial output stream
    SerialManager serialManager;
    // MQTT
    NetworkManager networkManager = null;
    // Number of CPU Threads to use, this app is heavy multithreaded,
    // high cpu cores equals to higher framerate but big CPU usage
    // 4 Threads are enough for 24FPS on an Intel i7 5930K@4.2GHz
    // 3 thread is enough for 30FPS with GPU Hardware Acceleration and uses nearly no CPU
    private int threadPoolNumber;
    private int executorNumber;

    /**
     * Constructor
     */
    public FireflyLuciferin() {
        PropertiesLoader propertiesLoader = new PropertiesLoader();
        MainSingleton.getInstance().formatter = new SimpleDateFormat(Constants.DATE_FORMAT);
        // Extract project version computed from Continuous Integration (GitHub Actions)
        MainSingleton.getInstance().version = propertiesLoader.retrieveProperties(Constants.PROP_VERSION);
        Locale currentLocale = Locale.getDefault();
        MainSingleton.getInstance().bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, currentLocale);
        if (MainSingleton.getInstance().bundle.getLocale().toString().isEmpty()) {
            MainSingleton.getInstance().bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, Locale.ENGLISH);
        }
        String ledMatrixInUse = "";
        try {
            StorageManager storageManager = new StorageManager();
            MainSingleton.getInstance().config = storageManager.loadConfigurationYaml();
            ledMatrixInUse = MainSingleton.getInstance().config.getDefaultLedMatrix();
        } catch (NullPointerException e) {
            log.error("Please configure the app.");
            NativeExecutor.exit();
        }
        manageLocale();
        // Queue is configured to hold a single frame, don't use `put` on it, but `offer` to prevent to block the writing thread.
        MainSingleton.getInstance().sharedQueue = new LinkedBlockingQueue<>(1);
        imageProcessor = new ImageProcessor(true);
        serialManager = new SerialManager();
        grabberManager = new GrabberManager();
        if (CommonUtility.isSingleDeviceMainInstance()) {
            NetworkSingleton.getInstance().messageServer = new MessageServer();
            NetworkSingleton.getInstance().messageServer.initNumLed();
        }
        setLedNumber(ledMatrixInUse);
        MainSingleton.getInstance().baudRate = Enums.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + MainSingleton.getInstance().config.getBaudRate()).getBaudRateValue();
        // Check if I'm the main program, if yes and multi monitor, spawn other guys
        NativeExecutor.spawnNewInstances();
        initThreadPool();
        MainSingleton.getInstance().hostServices = this.getHostServices();
        powerSavingManager = new PowerSavingManager();
        powerSavingManager.setLastFrameTime(LocalDateTime.now());
        NativeExecutor.setHighPriorityThreads(MainSingleton.getInstance().config.getThreadPriority());
    }

    /**
     * Set brightness
     *
     * @param tempNightMode previous value
     */
    private static void setNightBrightness(boolean tempNightMode) {
        if (tempNightMode != MainSingleton.getInstance().nightMode) {
            log.info("Night Mode: {}", MainSingleton.getInstance().nightMode);
            if (MainSingleton.getInstance().config != null && MainSingleton.getInstance().config.isFullFirmware()) {
                StateDto stateDto = new StateDto();
                stateDto.setState(Constants.ON);
                stateDto.setBrightness(CommonUtility.getNightBrightness());
                log.info(String.valueOf(stateDto.getBrightness()));
                if (CommonUtility.getDeviceToUse() != null) {
                    stateDto.setMAC(CommonUtility.getDeviceToUse().getMac());
                }
                stateDto.setWhitetemp(MainSingleton.getInstance().config.getWhiteTemperature());
                NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.TOPIC_DEFAULT_MQTT), CommonUtility.toJsonString(stateDto));
            }
        }
    }

    /**
     * Set LED number, this can be changed on the fly.
     * We are transferring byte via Serial, the maximum decimal number that can be represented with 1 byte is 255.
     * Use a multiplier to set a much bigger number using only 2 bytes.
     *
     * @param ledMatrixInUse led matrix in use
     */
    public static void setLedNumber(String ledMatrixInUse) {
        MainSingleton.getInstance().ledNumber = CommonUtility.isSingleDeviceMultiScreen() ? NetworkSingleton.getInstance().totalLedNum : MainSingleton.getInstance().config.getLedMatrixInUse(ledMatrixInUse).size();
        int multiplier = (int) Math.floor((double) MainSingleton.getInstance().ledNumber / Constants.SERIAL_CHUNK_SIZE);
        int lastPart = MainSingleton.getInstance().ledNumber - (Constants.SERIAL_CHUNK_SIZE * multiplier);
        if (lastPart < 1) {
            multiplier--;
            MainSingleton.getInstance().ledNumHighLowCount = Constants.SERIAL_CHUNK_SIZE - 1;
        } else {
            MainSingleton.getInstance().ledNumHighLowCount = MainSingleton.getInstance().ledNumber > Constants.SERIAL_CHUNK_SIZE ? lastPart - 1 : MainSingleton.getInstance().ledNumber - 1;
        }
        MainSingleton.getInstance().ledNumHighLowCountSecondPart = MainSingleton.getInstance().ledNumber > Constants.SERIAL_CHUNK_SIZE ? multiplier : 0;
    }

    /**
     * Startup JavaFX context
     *
     * @param args startup args
     */
    public static void main(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && args[0].equals(Constants.RESTART_DELAY)) {
            String[] newArray = new String[args.length - 1];
            System.arraycopy(args, 1, newArray, 0, newArray.length);
            args = newArray;
            CommonUtility.sleepSeconds(Constants.RESTART_DELAY_SECONDS);
        }
        moveToStandardDocsFolder();
        if (args != null && args.length > 0) {
            MainSingleton.getInstance().whoAmI = Integer.parseInt(args[0]);
            MainSingleton.getInstance().spawnInstances = false;
            CommonUtility.sleepMilliseconds(Constants.SPAWN_INSTANCE_WAIT_START_DELAY);
        }
        MainSingleton.getInstance().profileArgs = Constants.DEFAULT;
        if (args != null && args.length > 1) {
            MainSingleton.getInstance().profileArgs = args[1];
        }
        NativeExecutor.createStartWMClass();
        StorageManager sm = new StorageManager();
        sm.deleteTempFiles();
        launch(args);
    }

    /**
     * Move config files to a standard docs folder
     */
    static void moveToStandardDocsFolder() {
        String path = InstanceConfigurer.getConfigPath();
        String oldDocPath = InstanceConfigurer.getOldConfigPath();
        File newDirWithConfigFile = new File(path + File.separator + Constants.CONFIG_FILENAME);
        File oldDir = new File(oldDocPath);
        if (newDirWithConfigFile.exists() && oldDir.exists() && !path.equals(oldDocPath)) {
            if (oldDir.exists()) StorageManager.deleteDirectory(oldDir);
            log.info("Deleting old config file");
        }
        if (oldDir.exists() && !newDirWithConfigFile.exists() && !path.equals(oldDocPath)) {
            StorageManager.copyDir(oldDocPath, path);
            NativeExecutor.restartNativeInstance();
        }
    }

    /**
     * Activate/deactivate night mode
     */
    public static void checkForNightMode() {
        var tempNightMode = MainSingleton.getInstance().nightMode;
        if (!(MainSingleton.getInstance().config.getNightModeBrightness().equals(Constants.PERCENTAGE_OFF)) && MainSingleton.getInstance().config.isToggleLed()) {
            LocalTime from = LocalTime.now();
            LocalTime to = LocalTime.now();
            from = from.withHour(LocalTime.parse(MainSingleton.getInstance().config.getNightModeFrom()).getHour());
            from = from.withMinute(LocalTime.parse(MainSingleton.getInstance().config.getNightModeFrom()).getMinute());
            to = to.withHour(LocalTime.parse(MainSingleton.getInstance().config.getNightModeTo()).getHour());
            to = to.withMinute(LocalTime.parse(MainSingleton.getInstance().config.getNightModeTo()).getMinute());
            MainSingleton.getInstance().nightMode = (LocalTime.now().isAfter(from) || LocalTime.now().isBefore(to));
            setNightBrightness(tempNightMode);
        } else {
            MainSingleton.getInstance().nightMode = false;
        }
    }

    /**
     * Set log level at runtime based on user preferences
     */
    private void setRuntimeLogLevel() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        log.debug("** Log level -> {} **", MainSingleton.getInstance().config.getRuntimeLogLevel());
        loggerContext.getLogger(Constants.LOG_LEVEL_ROOT).setLevel(Level.toLevel(MainSingleton.getInstance().config.getRuntimeLogLevel()));
        if (JavaFXStarter.startupArgs != null && JavaFXStarter.startupArgs.length > 0) {
            log.info("Starting instance #: {}", JavaFXStarter.startupArgs[0]);
            if (JavaFXStarter.startupArgs.length > 1) {
                log.info("Profile to use: {}", JavaFXStarter.startupArgs[1]);
            }
        } else {
            log.info("Starting default instance");
        }
        logEnvironment();
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
        scheduleCheckForNightMode();
        StorageManager storageManager = new StorageManager();
        storageManager.updateConfigFile(MainSingleton.getInstance().config);
        setRuntimeLogLevel();
        // Manage tray icon and framerate dialog
        MainSingleton.getInstance().guiManager = new GuiManager(true);
        MainSingleton.getInstance().guiManager.trayIconManager.initTray();
        MainSingleton.getInstance().guiManager.showSettingsAndCheckForUpgrade(!NativeExecutor.isSystemTraySupported());
        if (CommonUtility.isSingleDeviceMainInstance() || !CommonUtility.isSingleDeviceMultiScreen()) {
            serialManager.initSerial();
        }
        if (MainSingleton.getInstance().config.isMqttEnable()) {
            connectToMqttServer();
        } else {
            log.info(Constants.MQTT_DISABLED);
            if (MainSingleton.getInstance().config.isFullFirmware()) {
                UdpServer udpServer = new UdpServer();
                NetworkSingleton.getInstance().udpBroadcastReceiverRunning = true;
                udpServer.receiveBroadcastUDPPacket();
            }
        }
        grabberManager.getFPS();
        grabberManager.pingDevice();
        imageProcessor.calculateBorders();
        // If this instance spawns new instances, don't launch grabbers here.
        if (!(MainSingleton.getInstance().spawnInstances && MainSingleton.getInstance().config.getMultiMonitor() > 1)) {
            launchGrabberAndConsumers();
        }
        // If multi monitor, first instance, single instance, start message server
        if (CommonUtility.isSingleDeviceMainInstance()) {
            MessageServer messageServer = new MessageServer();
            messageServer.startMessageServer();
        }
        if (CommonUtility.isSingleDeviceOtherInstance()) {
            MessageClient.getSingleInstanceMultiScreenStatus();
        }
        Enums.Effect effectInUse = LocalizedEnum.fromBaseStr(Enums.Effect.class, MainSingleton.getInstance().config.getEffect());
        if (MainSingleton.getInstance().config.isToggleLed()) {
            switch (effectInUse) {
                case BIAS_LIGHT, MUSIC_MODE_VU_METER, MUSIC_MODE_VU_METER_DUAL, MUSIC_MODE_BRIGHT, MUSIC_MODE_RAINBOW ->
                        manageAutoStart();
            }
        }
        if (!MainSingleton.getInstance().config.isMqttEnable() && !MainSingleton.getInstance().config.isFullFirmware()) {
            serialManager.manageSolidLed();
        }
        scheduleBackgroundTasks(stage);
        // Preload main dialog that requires 1.8s to laod the FXML (more or less on a 13900K CPU)
        if (NativeExecutor.isSystemTraySupported()) {
            MainSingleton.getInstance().guiManager.showSettingsDialog(true);
        }
        NativeExecutor.setSimdAvxInstructions();
    }

    /**
     * Launch grabber and consumers
     *
     * @throws AWTException GUI exception
     */
    private void launchGrabberAndConsumers() throws AWTException {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(threadPoolNumber);
        // Desktop Duplication API producers
        if ((MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX11.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.DDUPL_DX12.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.XIMAGESRC_NVIDIA.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.PIPEWIREXDG.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.PIPEWIREXDG_NVIDIA.name()))
                || (MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.AVFVIDEOSRC.name()))) {
            grabberManager.launchAdvancedGrabber(imageProcessor);
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
        }, scheduledExecutorService).thenAcceptAsync(log::info).exceptionally(_ -> {
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
        networkManager = new NetworkManager(false, retryCounter);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (!networkManager.connected) {
                log.info("MQTT retry");
                retryCounter.getAndIncrement();
                networkManager = new NetworkManager(true, retryCounter);
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
    @SuppressWarnings("all")
    private void scheduleBackgroundTasks(Stage stage) {
        // Wayland only, create a task that pings Glow Worm device every 2 seconds, this is needed because wayland stops sending
        // updates to the device when the image on the screen is still.
        if (NativeExecutor.isWayland()) {
            ScheduledExecutorService waylandScheduledExecutorService = Executors.newScheduledThreadPool(1);
            Runnable waylandTask = () -> {
                if (MainSingleton.getInstance().RUNNING && MainSingleton.getInstance().FPS_PRODUCER == 0
                        && MainSingleton.getInstance().lastLedColor != null && MainSingleton.getInstance().lastLedColor.length > 0) {
                    Collections.reverse(Arrays.asList(MainSingleton.getInstance().lastLedColor));
                    MainSingleton.getInstance().sharedQueue.offer(MainSingleton.getInstance().lastLedColor);
                }
            };
            waylandScheduledExecutorService.scheduleAtFixedRate(waylandTask, 0, 200, TimeUnit.MILLISECONDS);
        }
        NativeExecutor.addShutdownHook();
        if (!MainSingleton.getInstance().config.isMultiScreenSingleDevice() || CommonUtility.isSingleDeviceMainInstance()) {
            powerSavingManager.addPowerSavingTask();
        }
        if (MainSingleton.getInstance().config.getNightLight() != null && MainSingleton.getInstance().config.getNightLight().equals(Enums.NightLight.AUTO.getBaseI18n())) {
            GrabberSingleton.getInstance().getNightLightExecutor().scheduleAtFixedRate(GrabberSingleton.getInstance().getNightLightTask(), 0, 5, TimeUnit.SECONDS);
        }
        PipelineManager.manageGamingProfile();
    }

    /**
     * Delay autostart when on multi monitor, first instance must start capturing for first.
     */
    void manageAutoStart() {
        int timeToWait = 0;
        if ((MainSingleton.getInstance().config.getMultiMonitor() == 2 && MainSingleton.getInstance().whoAmI == 2)
                || (MainSingleton.getInstance().config.getMultiMonitor() == 3 && MainSingleton.getInstance().whoAmI == 3)) {
            timeToWait = 15;
        }
        CommonUtility.delaySeconds(() -> MainSingleton.getInstance().guiManager.startCapturingThreads(), timeToWait);
    }

    /**
     * Manage localization
     */
    private void manageLocale() {
        Locale currentLocale;
        currentLocale = Locale.getDefault();
        if (MainSingleton.getInstance().config != null && MainSingleton.getInstance().config.getLanguage() != null) {
            currentLocale = Locale.forLanguageTag(LocalizedEnum.fromBaseStr(Enums.Language.class, MainSingleton.getInstance().config.getLanguage()).name().toLowerCase());
        }
        Locale.setDefault(currentLocale);
        MainSingleton.getInstance().bundle = ResourceBundle.getBundle(Constants.MSG_BUNDLE, currentLocale);
    }

    /**
     * Check if it's time to activate the night mode
     */
    private void scheduleCheckForNightMode() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // Create a task that runs every 1 minutes
        Runnable nightModeTask = FireflyLuciferin::checkForNightMode;
        scheduledExecutorService.scheduleAtFixedRate(nightModeTask, 10, 60, TimeUnit.SECONDS);
    }

    /**
     * Initialize how many Threads to use in the ThreadPool and how many Executor to use
     */
    private void initThreadPool() {
        int numberOfCPUThreads = MainSingleton.getInstance().config.getNumberOfCPUThreads();
        threadPoolNumber = numberOfCPUThreads * 2;
        if (numberOfCPUThreads > 1) {
            if (!(MainSingleton.getInstance().config.getCaptureMethod().equals(Configuration.CaptureMethod.CPU.name()))) {
                executorNumber = numberOfCPUThreads;
            } else {
                executorNumber = numberOfCPUThreads * 3;
            }
        } else {
            executorNumber = numberOfCPUThreads;
        }
    }

    /**
     * Send color stream to the microcontroller
     * using DPsoftware Checksum
     *
     * @param leds array of LEDs containing the average color to display on the LED
     */
    private void sendColors(Color[] leds) throws IOException {
        if (!Enums.PowerSaving.DISABLED.equals(LocalizedEnum.fromBaseStr(Enums.PowerSaving.class, MainSingleton.getInstance().config.getPowerSaving()))) {
            if (powerSavingManager.isUnlockCheckLedDuplication()) {
                powerSavingManager.setUnlockCheckLedDuplication(false);
                powerSavingManager.checkForLedDuplication(leds);
            }
            if (powerSavingManager.isShutDownLedStrip() || powerSavingManager.isScreenSaverRunning()) {
                Arrays.fill(leds, new Color(0, 0, 0));
            }
        }
        if (Enums.Orientation.CLOCKWISE.equals((LocalizedEnum.fromBaseStr(Enums.Orientation.class, MainSingleton.getInstance().config.getOrientation())))) {
            Collections.reverse(Arrays.asList(leds));
        }
        if (MainSingleton.getInstance().config.getLedStartOffset() > 0) {
            List<Color> tempList = new ArrayList<>();
            List<Color> tempListHead = Arrays.asList(leds).subList(MainSingleton.getInstance().config.getLedStartOffset(), leds.length);
            List<Color> tempListTail = Arrays.asList(leds).subList(0, MainSingleton.getInstance().config.getLedStartOffset());
            tempList.addAll(tempListHead);
            tempList.addAll(tempListTail);
            leds = tempList.toArray(leds);
        }
        int i = 0;
        if (leds != null && leds[0] != null) {
            if (MainSingleton.getInstance().config.isFullFirmware() && MainSingleton.getInstance().config.isWirelessStream()) {
                // Single part stream
                if (MainSingleton.getInstance().ledNumber < Constants.FIRST_CHUNK || !Constants.JSON_STREAM) {
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
                    if (i >= Constants.THIRD_CHUNK && i < MainSingleton.getInstance().ledNumber) {
                        sendChunck(i, leds, 4);
                    }
                }
            } else {
                serialManager.sendColorsViaUSB(leds);
            }
        }
        MainSingleton.getInstance().FPS_CONSUMER_COUNTER++;
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
            ledStr.append((AudioSingleton.getInstance().AUDIO_BRIGHTNESS == 255 ? CommonUtility.getNightBrightness() : AudioSingleton.getInstance().AUDIO_BRIGHTNESS)).append(",");
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
            NetworkManager.stream(ledStr.toString().replace(",.", "") + "]}");
        } else {
            NetworkManager.streamColors(leds, ledStr);
        }
        return i;
    }

    /**
     * Fast consumer
     */
    @SuppressWarnings("InfiniteLoopStatement")
    void consume() throws InterruptedException, IOException {
        boolean isWayland = NativeExecutor.isWayland();
        while (true) {
            Color[] colorArray = MainSingleton.getInstance().sharedQueue.take();
            if (isWayland) MainSingleton.getInstance().lastLedColor = colorArray;
            if (MainSingleton.getInstance().RUNNING) {
                if (CommonUtility.isSingleDeviceMultiScreen()) {
                    if (colorArray.length == NetworkSingleton.getInstance().totalLedNum) {
                        sendColors(colorArray);
                    }
                } else if (colorArray.length == MainSingleton.getInstance().ledNumber) {
                    sendColors(colorArray);
                }
            }
        }
    }

    /**
     * Clean and Close Serial Output Stream
     */
    private void clean() {
        if (MainSingleton.getInstance().output != null) {
            try {
                MainSingleton.getInstance().output.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        if (MainSingleton.getInstance().serial != null) {
            MainSingleton.getInstance().serial.closePort();
        }
    }

    /**
     * Log the environment in use
     */
    private void logEnvironment() {
        if (NativeExecutor.isLinux()) {
            if (NativeExecutor.isFlatpak()) {
                log.debug("Running on Linux using Flatpak sandbox");
            } else if (NativeExecutor.isSnap()) {
                log.debug("Running on Linux using Snap sandbox");
            } else {
                log.debug("Running on Linux");
            }
        } else if (NativeExecutor.isWindows()) {
            log.debug("Running on Windows");
        }
        log.info("Traffic Class for the UDP socket: 0x{}", Integer.toHexString(MainSingleton.getInstance().config.getUdpTrafficClass()).toUpperCase());
    }

}
