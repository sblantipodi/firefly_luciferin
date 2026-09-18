/*
  ConfigServer.java

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.*;
import org.dpsoftware.grabber.CubeLutToneMap;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.gui.controllers.DisplayDialogController;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.DeviceDto;
import org.dpsoftware.utilities.CommonUtility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * Minimal HTTP server exposing the saved {@link Configuration} as JSON and providing a small web page to edit it.
 * Backed by the JDK HTTP server.
 */
@Slf4j
public class ConfigServer {

    /**
     * Configuration fields to strip from the JSON payload, they are huge and not useful to a client.
     */
    private static final List<String> EXCLUDED_FIELDS = List.of("hueMap", "ledMatrix");
    private static final String JSON_OK = "{\"status\":\"OK\"}";
    /**
     * Settings web page resource, co-located in this package ({@code org.dpsoftware.network.web}).
     * <p>
     * The package is {@code opens} in the module descriptor so the class loader can read the
     * resource in JPMS module mode (a non opened package would be invisible to it).
     */
    private static final String SET_CONFIG_PAGE_RESOURCE = "setConfig.html";
    /**
     * Settings page JavaScript resource, co-located in this package.
     */
    private static final String SET_CONFIG_PAGE_JS_RESOURCE = "setConfig.js";
    private static final String SET_CONFIG_CORE_JS_RESOURCE = "setConfig-core.js";
    private static final String SET_CONFIG_DEVICE_JS_RESOURCE = "setConfig-device.js";
    private static final String SET_CONFIG_UI_JS_RESOURCE = "setConfig-ui.js";
    private final StorageManager storageManager = new StorageManager();
    private final List<HttpServer> httpServers = new java.util.ArrayList<>();
    private final Predicate<String> GET_METHOD = method -> method.equalsIgnoreCase("GET");
    private final Predicate<String> POST_METHOD = method -> method.equalsIgnoreCase("POST");
    /**
     * Idle timeout, in milliseconds, after which the live preview is automatically turned off.
     */
    private static final int LIVE_PREVIEW_IDLE_MILLIS = 30000;
    /**
     * Wall-clock milliseconds of the most recent {@code GET /screenshot}; refreshed by
     * {@link #handleGetScreenshot}. The live preview watchdog reads it to detect an idle client.
     */
    private volatile long lastScreenshotGetMillis = 0L;
    /**
     * Watchdog thread that turns off the live capture flag when no {@code GET /screenshot} has
     * arrived for more than {@value #LIVE_PREVIEW_IDLE_MILLIS} ms.
     */
    private Thread livePreviewWatchdog;

    /**
     * Collect the addresses to bind: loopback (127.0.0.1) plus every non-link-local IPv4 interface address.
     * Link-local (169.254.x.x) addresses are excluded to avoid exposing the endpoint on ad-hoc Wi-Fi or
     * Bluetooth networks; multicast and IPv6 addresses are skipped as well.
     *
     * @return the set of addresses to bind, ordered (loopback first)
     */
    private static Set<InetAddress> localBindAddresses() {
        Set<InetAddress> addresses = new LinkedHashSet<>();
        try {
            addresses.add(InetAddress.getLoopbackAddress());
        } catch (Exception ignored) {
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addressesEnum = networkInterface.getInetAddresses();
                while (addressesEnum.hasMoreElements()) {
                    InetAddress address = addressesEnum.nextElement();
                    if (address.isLoopbackAddress()) {
                        continue;
                    }
                    String host = address.getHostAddress();
                    // Skip IPv6, link-local (169.254.x.x) and any multicast
                    if (host.contains(":") || host.startsWith("169.254.") || address.isMulticastAddress()) {
                        continue;
                    }
                    addresses.add(address);
                }
            }
        } catch (SocketException e) {
            log.warn("Unable to enumerate network interfaces: {}", e.getMessage());
        }
        return addresses;
    }

    /**
     * Build the map of possible values for every configuration field that is backed by an enum,
     * using the enums as the single source of truth (no value list is duplicated in the client).
     * Each entry exposes the value to persist and the English display label, plus the value type
     * so the client can cast it correctly.
     *
     * @return map of configuration field name to its possible values
     */
    private static Map<String, FieldOptions> getFieldOptions() {
        Map<String, FieldOptions> options = new LinkedHashMap<>();
        options.put("orientation", localized(Enums.Orientation.class));
        options.put("defaultLedMatrix", localized(Enums.AspectRatio.class));
        options.put("baudRate", new FieldOptions(Arrays.stream(Enums.BaudRate.values())
                .map(b -> new FieldOptions.Option(b.getBaudRate(), b.getBaudRate())).toList(), "string"));
        options.put("desiredFramerate", new FieldOptions(Arrays.stream(Enums.Framerate.values())
                .map(f -> new FieldOptions.Option(f.getBaseI18n(), f.getBaseI18n())).toList(), "string"));
        options.put("simdAvx", new FieldOptions(Arrays.stream(Enums.SimdAvxOption.values())
                .map(s -> new FieldOptions.Option(String.valueOf(s.getSimdOptionNumeric()), s.getBaseI18n())).toList(), "number"));
        options.put("resamplingFactor", new FieldOptions(Arrays.stream(Enums.ResamplingFactor.values())
                .map(r -> new FieldOptions.Option(String.valueOf(r.getResamplingFactorValue()), r.getBaseI18n())).toList(), "number"));
        options.put("algo", localized(Enums.Algo.class));
        options.put("theme", localized(Enums.Theme.class));
        options.put("language", localized(Enums.Language.class));
        options.put("smoothingType", localized(Enums.Smoothing.class));
        options.put("streamType", new FieldOptions(Arrays.stream(Enums.StreamType.values())
                .map(s -> new FieldOptions.Option(s.getStreamType(), s.getStreamType())).toList(), "string"));
        options.put("effect", effectOptions());
        options.put("colorMode", new FieldOptions(Arrays.stream(Enums.ColorMode.values())
                .map(c -> new FieldOptions.Option(String.valueOf(c.ordinal() + 1), c.getBaseI18n())).toList(), "number"));
        options.put("gammaLevel", localized(Enums.GammaLevel.class));
        options.put("nightLight", localized(Enums.NightLight.class));
        options.put("brightnessLimiter", new FieldOptions(Arrays.stream(Enums.BrightnessLimiter.values())
                .map(b -> new FieldOptions.Option(String.valueOf(b.getBrightnessLimitFloat()), b.getBaseI18n())).toList(), "number"));
        options.put("powerSaving", localized(Enums.PowerSaving.class));
        options.put("multiMonitor", new FieldOptions(List.of(
                new FieldOptions.Option("1", "Disabled"),
                new FieldOptions.Option("2", "Dual display"),
                new FieldOptions.Option("3", "Triple display")), "number"));
        // 3D LUT (color tone map) options, the available .cube LUTs (classpath + config dir) with
        // "Disabled" pinned at the top, the same list the JavaFX combo box is populated with.
        options.put("cubeLut", new FieldOptions(CubeLutToneMap.listAvailableLuts().stream()
                .map(name -> new FieldOptions.Option(name, name)).toList(), "string"));
        return options;
    }

    /**
     * Effect options with "Solid" and "Bias light" pinned at the top, the remaining effects sorted alphabetically.
     *
     * @return the field options for the effect field
     */
    private static FieldOptions effectOptions() {
        Set<String> pinned = Set.of(Enums.Effect.SOLID.getValue(), Enums.Effect.BIAS_LIGHT.getValue());
        List<Enums.Effect> rest = Arrays.stream(Enums.Effect.values())
                .filter(e -> !pinned.contains(e.getValue()))
                .sorted(Comparator.comparing(LocalizedEnum::getI18n))
                .toList();
        List<FieldOptions.Option> opts = new ArrayList<>();
        pinned.stream()
                .sorted(Comparator.comparing(CommonUtility::getWord))
                .forEach(key -> opts.add(effectOption(key)));
        rest.forEach(e -> opts.add(effectOption(e.getValue())));
        return new FieldOptions(opts, "string");
    }

    private static FieldOptions.Option effectOption(String i18nKey) {
        return new FieldOptions.Option(CommonUtility.getWord(i18nKey), CommonUtility.getWord(i18nKey));
    }

    /**
     * Handle GET /fps, exposing the current producing and consuming framerate.
     * Read-only, the values are the live counters kept in {@link MainSingleton}.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetFps(HttpExchange exchange) throws IOException {
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(new FpsDto(
                MainSingleton.getInstance().FPS_PRODUCER,
                MainSingleton.getInstance().FPS_GW_CONSUMER));
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle GET /screenshot, serving the last captured frame BMP saved by the grabber
     * (written by {@code intBufferRgbToImage} when the runtime log level is TRACE).
     * The file is read from the config path; a 404 is returned when it does not exist yet
     * so the client can keep retrying until a frame is captured.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetScreenshot(HttpExchange exchange) throws IOException {
        // A client is actively pulling frames: refresh the idle timestamp so the watchdog does
        // not turn off the live preview while the page is polling.
        lastScreenshotGetMillis = System.currentTimeMillis();
        java.io.File bmp = new java.io.File(InstanceConfigurer.getConfigPath(), Constants.GSTREAMER_SCREENSHOT);
        if (!bmp.exists() || !bmp.isFile()) {
            sendError(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Screenshot not available");
            return;
        }
        byte[] imageBytes = java.nio.file.Files.readAllBytes(bmp.toPath());
        exchange.getResponseHeaders().set("Content-Type", "image/bmp");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, imageBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(imageBytes);
        }
    }

    /**
     * Handle POST /screenshot/enable, toggling the live capture flag so the grabber starts (or
     * stops) writing the live preview BMP (see {@code GStreamerGrabber.rgbFrame}, which writes the
     * frame when either the log level is TRACE or {@code showLiveCapture} is true). The flag is a
     * runtime-only toggle on {@link GuiSingleton}; it does not change the persisted configuration
     * nor the runtime log level. The {@code disable} query parameter, when set to true, turns the
     * preview off (showLiveCapture=false); otherwise it is turned on (showLiveCapture=true).
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleEnableScreenshot(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        boolean disable = query != null && "true".equalsIgnoreCase(queryParam(query, "disable"));
        boolean on = !disable;
        GuiSingleton.getInstance().setShowLiveCapture(on);
        if (on) {
            if (!MainSingleton.getInstance().RUNNING) {
                PipelineManager.restartCapture(CommonUtility::run);
            }
            startLivePreviewWatchdog();
        } else {
            stopLivePreviewWatchdog();
        }
        log.info("Live preview toggled: showLiveCapture set to {}", on);
        sendJson(exchange, HttpURLConnection.HTTP_OK, JSON_OK);
    }

    /**
     * Start the idle watchdog that automatically turns off the live capture flag when no
     * {@code GET /screenshot} has arrived for more than {@value #LIVE_PREVIEW_IDLE_MILLIS} ms.
     * Any previously running watchdog is stopped first, so the method is safe to call again while
     * one is already running (it restarts the idle timer).
     */
    private synchronized void startLivePreviewWatchdog() {
        stopLivePreviewWatchdog();
        lastScreenshotGetMillis = System.currentTimeMillis();
        livePreviewWatchdog = new Thread(() -> {
            while (true) {
                CommonUtility.sleepMilliseconds(LIVE_PREVIEW_IDLE_MILLIS);
                if (System.currentTimeMillis() - lastScreenshotGetMillis > LIVE_PREVIEW_IDLE_MILLIS) {
                    log.info("Live preview idle for more than {} ms, turning showLiveCapture off", LIVE_PREVIEW_IDLE_MILLIS);
                    GuiSingleton.getInstance().setShowLiveCapture(false);
                    return;
                }
            }
        }, "live-preview-watchdog");
        livePreviewWatchdog.setDaemon(true);
        livePreviewWatchdog.start();
    }

    /**
     * Extract a query parameter value from a URL query string.
     *
     * @param query the query string (without the leading '?')
     * @param name  the parameter name
     * @return the value or {@code null} when absent
     */
    private static String queryParam(String query, String name) {
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    /**
     * Handle GET /listProfiles, exposing the list of profile names available for this instance
     * (read from the config directory via {@link StorageManager#listProfilesForThisInstance()}).
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleListProfiles(HttpExchange exchange) throws IOException {
        List<String> profiles = new ArrayList<>(new LinkedHashSet<>(storageManager.listProfilesForThisInstance()));
        profiles.removeIf(p -> CommonUtility.getWord(Constants.DEFAULT, Locale.ENGLISH).equals(p));
        profiles.addFirst(CommonUtility.getWord(Constants.DEFAULT, Locale.ENGLISH));
        String profileArg = MainSingleton.getInstance().profileArg;
        String activeProfile;
        if (profileArg == null || profileArg.isEmpty()
                || Constants.DEFAULT.equals(profileArg)
                || CommonUtility.getWord(Constants.DEFAULT).equals(profileArg)) {
            activeProfile = CommonUtility.getWord(Constants.DEFAULT, Locale.ENGLISH);
        } else {
            activeProfile = profileArg;
        }
        ObjectNode node = CommonUtility.JSON_MAPPER.createObjectNode();
        node.putPOJO("profiles", profiles);
        node.put("activeProfile", activeProfile);
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(node);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle POST /activateProfile?name=<profile>, activating a profile by restarting the native
     * instance with it. When {@code name} is absent or empty the current (default) profile is used.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleActivateProfile(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String name = query != null ? queryParam(query, "name") : null;
        if (name == null || name.isEmpty()) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Missing or empty name parameter");
            return;
        }
        // When "Default" is selected, restart without a profile (null) so the main config is used.
        sendJson(exchange, HttpURLConnection.HTTP_OK, JSON_OK);
        if (name.equals(CommonUtility.getWord(Constants.DEFAULT))) {
            NativeExecutor.restartNativeInstance("\"" + Constants.DEFAULT + "\"");
        } else {
            NativeExecutor.restartNativeInstance("\"" + name + "\"");
        }
    }

    /**
     * Stop the live preview watchdog, if any is running.
     */
    private synchronized void stopLivePreviewWatchdog() {
        if (livePreviewWatchdog != null) {
            livePreviewWatchdog.interrupt();
            livePreviewWatchdog = null;
        }
    }

    /**
     * Handle GET /getConfig, serializing the saved configuration to JSON.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetConfig(HttpExchange exchange) throws IOException {
        // Expose the config of the profile in use (or the main config when no profile is set).
        Configuration config = storageManager.readProfileInUseConfig();
        if (config == null) {
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Configuration not found");
            return;
        }
        sendConfiguration(exchange, config);
    }

    /**
     * Serialize the configuration to JSON, strip the heavy/not interesting fields, and write it as the HTTP response body.
     *
     * @param exchange the HTTP exchange to send the response on
     * @param config   the configuration to serialize
     * @throws IOException when the response cannot be written
     */
    private void sendConfiguration(HttpExchange exchange, Configuration config) throws IOException {
        ObjectNode configNode = CommonUtility.JSON_MAPPER.valueToTree(config);
        EXCLUDED_FIELDS.forEach(configNode::remove);
        // Expose the active profile (when a non-default profile is in use) so the client can show it.
        String profileArg = MainSingleton.getInstance().profileArg;
        if (profileArg != null && !profileArg.isEmpty()
                && !Constants.DEFAULT.equals(profileArg)
                && !CommonUtility.getWord(Constants.DEFAULT).equals(profileArg)) {
            configNode.put("activeProfile", profileArg);
        }
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(configNode);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Build {@link FieldOptions} for a localized enum, using its base (English) i18n value to persist
     * and its English i18n text as label.
     *
     * @param enumClass the localized enum class
     * @param <E>       the localized enum type
     * @return the field options
     */
    private static <E extends Enum<E> & LocalizedEnum> FieldOptions localized(Class<E> enumClass) {
        List<FieldOptions.Option> opts = Arrays.stream(enumClass.getEnumConstants())
                .map(e -> new FieldOptions.Option(e.getBaseI18n(), e.getBaseI18n()))
                .toList();
        return new FieldOptions(opts, "string");
    }

    /**
     * Build the map of localized labels for every configuration field, using the current application locale.
     *
     * @return map of configuration field name to its localized label
     */
    private static Map<String, String> getFieldLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("topLed", CommonUtility.getWord("fxml.ledsconfigtab.toprow"));
        labels.put("leftLed", CommonUtility.getWord("fxml.ledsconfigtab.leftcol"));
        labels.put("rightLed", CommonUtility.getWord("fxml.ledsconfigtab.rightcol"));
        labels.put("bottomLeftLed", CommonUtility.getWord("fxml.ledsconfigtab.bottomleft"));
        labels.put("bottomRightLed", CommonUtility.getWord("fxml.ledsconfigtab.bottomright"));
        labels.put("bottomRowLed", CommonUtility.getWord("fxml.ledsconfigtab.bottomrow"));
        labels.put("ledStartOffset", CommonUtility.getWord("fxml.ledsconfigtab.firstled"));
        labels.put("orientation", CommonUtility.getWord("fxml.ledsconfigtab.orientation"));
        labels.put("groupBy", CommonUtility.getWord("fxml.ledsconfigtab.group.by"));
        labels.put("splitBottomMargin", CommonUtility.getWord("fxml.ledsconfigtab.splitbottomrow"));
        labels.put("grabberAreaTopBottom", CommonUtility.getWord("fxml.ledsconfigtab.grabberArea"));
        labels.put("grabberSide", CommonUtility.getWord("fxml.ledsconfigtab.grabberArea"));
        labels.put("gapTypeTopBottom", CommonUtility.getWord("fxml.ledsconfigtab.gapType"));
        labels.put("gapTypeSide", CommonUtility.getWord("fxml.ledsconfigtab.gapType"));
        labels.put("outputDevice", CommonUtility.getWord("fxml.modetab.outputdevice"));
        labels.put("baudRate", CommonUtility.getWord("fxml.modetab.baudrate"));
        labels.put("staticGlowWormIp", CommonUtility.getWord("fxml.modetab.serialport"));
        labels.put("desiredFramerate", CommonUtility.getWord("fxml.misctab.captureframerate"));
        labels.put("smoothingType", CommonUtility.getWord("fxml.dialog.smoothing.type"));
        labels.put("smoothingTargetFramerate", CommonUtility.getWord("fxml.dialog.smoothing.target.framerate"));
        labels.put("frameInsertionTarget", CommonUtility.getWord("fxml.dialog.smoothing.frameinsertion"));
        labels.put("emaAlpha", CommonUtility.getWord("fxml.dialog.smoothing.emaalpha"));
        labels.put("simdAvx", CommonUtility.getWord("fxml.modetab.simdavx"));
        labels.put("resamplingFactor", CommonUtility.getWord("fxml.modetab.scaling"));
        labels.put("captureMethod", CommonUtility.getWord("fxml.modetab.capturemethod"));
        labels.put("monitorNumber", CommonUtility.getWord("fxml.modetab.binddisplay"));
        labels.put("screenResX", CommonUtility.getWord("fxml.modetab.screenresolution"));
        labels.put("screenResY", CommonUtility.getWord("fxml.modetab.screenresolution"));
        labels.put("osScaling", CommonUtility.getWord("fxml.modetab.os.scaling"));
        labels.put("defaultLedMatrix", CommonUtility.getWord("fxml.modetab.aspectratio"));
        labels.put("autoDetectBlackBars", CommonUtility.getWord("fxml.modetab.autodetect"));
        labels.put("algo", CommonUtility.getWord("fxml.modetab.algo"));
        labels.put("language", CommonUtility.getWord("fxml.misctab.language"));
        labels.put("mqttEnable", CommonUtility.getWord("fxml.mqtttab.enablemqtt"));
        labels.put("wirelessStream", CommonUtility.getWord("fxml.mqtttab.wirelessstream"));
        labels.put("streamType", CommonUtility.getWord("fxml.mqtttab.streamtype"));
        labels.put("mqttServer", CommonUtility.getWord("fxml.mqtttab.mqttserverhost"));
        labels.put("mqttTopic", CommonUtility.getWord("fxml.mqtttab.mqttbasetopic"));
        labels.put("mqttDiscoveryTopic", CommonUtility.getWord("fxml.mqtttab.mqttdiscoverytopic"));
        labels.put("mqttUsername", CommonUtility.getWord("fxml.mqtttab.mqttusername"));
        labels.put("mqttPwd", CommonUtility.getWord("fxml.mqtttab.mqttpwd"));
        labels.put("effect", CommonUtility.getWord("fxml.misctab.effect"));
        labels.put("colorMode", CommonUtility.getWord("fxml.devicestab.colormode"));
        labels.put("gamma", CommonUtility.getWord("fxml.misctab.gamma"));
        labels.put("whiteTemperature", CommonUtility.getWord("fxml.misctab.whitetemp"));
        labels.put("brightness", CommonUtility.getWord("fxml.misctab.brightness"));
        labels.put("nightModeFrom", CommonUtility.getWord("fxml.misctab.nightmode.from"));
        labels.put("nightModeTo", CommonUtility.getWord("fxml.misctab.nightmode.to"));
        labels.put("nightModeBrightness", CommonUtility.getWord("fxml.misctab.nightmode.brightness"));
        labels.put("toggleLed", CommonUtility.getWord("fxml.misctab.ledcontrol"));
        labels.put("startWithSystem", CommonUtility.getWord("fxml.misctab.runlogin"));
        labels.put("runtimeLogLevel", CommonUtility.getWord("fxml.misctab.runtimelog"));
        labels.put("cubeLut", CommonUtility.getWord("fxml.misctab.cubeLut"));
        labels.put("nightLight", CommonUtility.getWord("fxml.eyecare.night.light"));
        labels.put("nightLightLvl", CommonUtility.getWord("fxml.eyecare.nightlight.level"));
        labels.put("luminosityThreshold", CommonUtility.getWord("fxml.eyecare.luminosity.threshold"));
        labels.put("brightnessLimiter", CommonUtility.getWord("fxml.eyecare.brightness.limiter"));
        labels.put("enableAutomaticGamma", CommonUtility.getWord("fxml.gamma.enable.automatic"));
        labels.put("gammaLevel", CommonUtility.getWord("fxml.gamma.level"));
        labels.put("checkFullScreen", CommonUtility.getWord("fxml.profile.fullscreen.cb"));
        labels.put("gpuThreshold", CommonUtility.getWord("fxml.profile.gpu"));
        labels.put("cpuThreshold", CommonUtility.getWord("fxml.profile.cpu"));
        labels.put("profileProcess1", CommonUtility.getWord("fxml.profile.process1"));
        labels.put("profileProcess2", CommonUtility.getWord("fxml.profile.process2"));
        labels.put("profileProcess3", CommonUtility.getWord("fxml.profile.process3"));
        labels.put("powerSaving", CommonUtility.getWord("fxml.devicestab.power.saving"));
        labels.put("multiMonitor", CommonUtility.getWord("fxml.devicestab.multi.monitor"));
        labels.put("multiScreenSingleDevice", CommonUtility.getWord("fxml.devicestab.single.device"));
        labels.put("checkForUpdates", CommonUtility.getWord("fxml.devicestab.check.updates"));
        labels.put("syncCheck", CommonUtility.getWord("fxml.devicestab.sync.check"));
        labels.put("enableLDR", CommonUtility.getWord("fxml.eyecare.enableldr"));
        labels.put("ldrInterval", CommonUtility.getWord("fxml.eyecare.ldr.interval"));
        labels.put("ldrMin", CommonUtility.getWord("fxml.eyecare.ldr.min.bright"));
        labels.put("ldrTurnOff", CommonUtility.getWord("fxml.eyecare.ldr.turnoff"));
        return labels;
    }

    /**
     * Handle POST /setConfig, taking a JSON payload with some configuration parameters,
     * merging it into the saved configuration (the excluded fields are preserved) and persisting it.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleSetConfig(HttpExchange exchange) throws IOException {
        JsonNode payload;
        try (InputStream requestBody = exchange.getRequestBody()) {
            payload = CommonUtility.JSON_MAPPER.readTree(requestBody);
        } catch (IOException e) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON payload");
            return;
        }
        if (payload == null || payload.isNull() || !payload.isObject()) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Payload must be a JSON object");
            return;
        }
        log.info("setConfig payload received: {}", CommonUtility.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
        // Read the config of the profile in use (or the main config when no profile is set).
        Configuration config = storageManager.readProfileInUseConfig();
        if (config == null) {
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Configuration not found");
            return;
        }
        ObjectNode configTree = CommonUtility.JSON_MAPPER.valueToTree(config);
        mergePayload(payload, configTree);
        Configuration updatedConfig = CommonUtility.JSON_MAPPER.treeToValue(configTree, Configuration.class);
        try {
            // Persist into the profile in use (or the main config when no profile is set); null lets
            // writeConfig pick the right file based on profileArg and whoAmI.
            updatedConfig.setEffect(LocalizedEnum.fromStr(Enums.Effect.class, updatedConfig.getEffect()).getBaseI18n());
            storageManager.writeConfig(updatedConfig, null);
        } catch (IOException e) {
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Unable to save configuration: " + e.getMessage());
            return;
        }
        log.info("Configuration updated via setConfig endpoint");
        sendJson(exchange, HttpURLConnection.HTTP_OK, JSON_OK);
        // Restart Firefly with the profile in use (if any) and preserving headless mode.
        NativeExecutor.restartNativeInstanceWithCurrentProfile();
    }

    /**
     * Stop the config endpoint on every bound interface.
     */
    public void stop() {
        httpServers.forEach(server -> {
            try {
                server.stop(0);
            } catch (Exception ignored) {
            }
        });
        httpServers.clear();
    }

    /**
     * Handle GET /getDevices, exposing the currently connected devices (in-memory device table) as JSON.
     * Read-only, the connected devices are a runtime state and cannot be persisted.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetDevices(HttpExchange exchange) throws IOException {
        List<DeviceDto> devices = DeviceDto.fromDevices(GuiSingleton.getInstance().getDeviceTableData());
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(devices);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Start the config HTTP endpoint on the loopback address and every non-link-local local IPv4 interface.
     * A separate {@link HttpServer} is created per interface because the JDK {@code HttpServer} can only
     * bind to a single {@link InetSocketAddress} at a time. Link-local (169.254.x.x) and site-local
     * multicast addresses are skipped so the endpoint is not exposed on Wi-Fi/Bluetooth ad-hoc networks.
     */
    @SuppressWarnings("all")
    public void start() {
        if (!httpServers.isEmpty()) {
            return;
        }
        try {
            for (InetAddress address : localBindAddresses()) {
                HttpServer server = HttpServer.create(new InetSocketAddress(address, Constants.CONFIG_SERVER_DEFAULT_PORT), 0);
                server.createContext(Constants.CONFIG_ENDPOINT, withGuard(this::handleGetConfig, GET_METHOD));
                server.createContext(Constants.GET_DEVICES_ENDPOINT, withGuard(this::handleGetDevices, GET_METHOD));
                server.createContext(Constants.FIELD_OPTIONS_ENDPOINT, withGuard(this::handleGetFieldOptions, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_PAGE_ENDPOINT, withGuard(this::handleSetConfigPage, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_PAGE_JS_ENDPOINT, withGuard(this::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_CORE_JS_ENDPOINT, withGuard(this::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_DEVICE_JS_ENDPOINT, withGuard(this::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_UI_JS_ENDPOINT, withGuard(this::handleSetConfigPageJs, GET_METHOD));
                server.createContext(Constants.SET_CONFIG_ENDPOINT, withGuard(this::handleSetConfig, POST_METHOD));
                server.createContext(Constants.DEVICE_PREFS_ENDPOINT, withGuard(this::handleDevicePrefs, GET_METHOD));
                server.createContext(Constants.FPS_ENDPOINT, withGuard(this::handleGetFps, GET_METHOD));
                server.createContext(Constants.SCREENSHOT_ENDPOINT, withGuard(this::handleGetScreenshot, GET_METHOD));
                server.createContext(Constants.SCREENSHOT_ENABLE_ENDPOINT, withGuard(this::handleEnableScreenshot, POST_METHOD));
                server.createContext(Constants.LIST_PROFILES_ENDPOINT, withGuard(this::handleListProfiles, GET_METHOD));
                server.createContext(Constants.ACTIVATE_PROFILE_ENDPOINT, withGuard(this::handleActivateProfile, POST_METHOD));
                server.createContext(Constants.COMBO_CHANGE_ENDPOINT, withGuard(this::handleComboChange, POST_METHOD));
                server.createContext(Constants.SECTION_TITLES_ENDPOINT, withGuard(this::handleGetSectionTitles, GET_METHOD));
                server.createContext("/", withGuard(this::handleRoot, GET_METHOD));
                server.setExecutor(Executors.newCachedThreadPool(runnable -> {
                    Thread thread = new Thread(runnable, "firefly-config-server");
                    thread.setDaemon(true);
                    return thread;
                }));
                server.start();
                httpServers.add(server);
                log.info("Config server listening on http://{}:{}", address.getHostAddress(), Constants.CONFIG_SERVER_DEFAULT_PORT);
            }
            if (httpServers.isEmpty()) {
                log.warn("No local interface found, config server not started");
            }
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "firefly-config-shutdown"));
        } catch (IOException e) {
            stop();
            log.warn("Unable to start config server: {}", e.getMessage());
        }
    }

    /**
     * Handle GET /getFieldOptions, exposing the possible values for every configuration field backed by an enum.
     * Read-only, the options are derived from the enums (single source of truth, no client-side duplication).
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetFieldOptions(HttpExchange exchange) throws IOException {
        ObjectNode response = CommonUtility.JSON_MAPPER.createObjectNode();
        response.set("options", CommonUtility.JSON_MAPPER.valueToTree(getFieldOptions()));
        Map<String, String> labels = getFieldLabels();
        labels.put("toggleLed", CommonUtility.getWord(
                MainSingleton.getInstance().config.isToggleLed()
                        ? Constants.TURN_LED_OFF : Constants.TURN_LED_ON));
        labels.put("turnLedOn", CommonUtility.getWord(Constants.TURN_LED_ON));
        labels.put("turnLedOff", CommonUtility.getWord(Constants.TURN_LED_OFF));
        response.set("labels", CommonUtility.JSON_MAPPER.valueToTree(labels));
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle GET /sectionTitles, returning the localized titles for every section and sub-accordion.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleGetSectionTitles(HttpExchange exchange) throws IOException {
        Map<String, String> titles = new LinkedHashMap<>();
        titles.put("leds", CommonUtility.getWord("fxml.setting.ledsconfig"));
        titles.put("mode", CommonUtility.getWord("fxml.setting.mode"));
        titles.put("network", CommonUtility.getWord("fxml.setting.wifimqtt"));
        titles.put("misc", CommonUtility.getWord("fxml.setting.misc"));
        titles.put("devices", CommonUtility.getWord("fxml.setting.devices"));
        titles.put("ldr", CommonUtility.getWord("fxml.setting.ldr"));
        titles.put("display", CommonUtility.getWord("fxml.ledsconfigtab.display"));
        titles.put("colorCorr", CommonUtility.getWord("fxml.misctab.colorcorrection"));
        titles.put("eyeCare", CommonUtility.getWord("fxml.misctab.eyecare"));
        titles.put("gamma", CommonUtility.getWord("fxml.misctab.gamma"));
        titles.put("profile", CommonUtility.getWord("fxml.misctab.profiles"));
        titles.put("smoothing", CommonUtility.getWord("fxml.dialog.smoothing.title"));
        titles.put("connectedDevices", CommonUtility.getWord("fxml.devicestab.connected.devices"));
        titles.put("satellites", CommonUtility.getWord("fxml.devicestab.satellites"));
        ObjectNode response = CommonUtility.JSON_MAPPER.valueToTree(titles);
        byte[] responseBytes = CommonUtility.JSON_MAPPER.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle POST /comboChange, applying the web page select change to the running configuration.
     * Expected JSON body: {"comboName": "cubeLut", "value": "HDR2SDR_tonemap_LUT_1.cube"}.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleComboChange(HttpExchange exchange) throws IOException {
        JsonNode payload;
        try (InputStream requestBody = exchange.getRequestBody()) {
            payload = CommonUtility.JSON_MAPPER.readTree(requestBody);
        } catch (IOException e) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Invalid JSON payload");
            return;
        }
        String comboName = payload.has("comboName") ? payload.get("comboName").asText() : "";
        JsonNode valueNode = payload.has("value") ? payload.get("value") : null;
        log.info("Web combo change: {} = {}", comboName, valueNode);
        applyComboChange(comboName, valueNode);
        sendJson(exchange, HttpURLConnection.HTTP_OK, JSON_OK);
    }

    /**
     * Apply a combo change to the running configuration on the JavaFX thread.
     *
     * @param comboName the name of the combo box that changed
     * @param value     the newly selected value, as a JSON node (string or boolean)
     */
    private void applyComboChange(String comboName, JsonNode value) {
        String valueText = value != null ? value.asText() : "";
        if (valueText == null || valueText.isEmpty()) {
            return;
        }
        switch (comboName) {
            case "cubeLut" -> CommonUtility.delayMilliseconds(() -> {
                DisplayDialogController.handleCubeLutCombo(valueText);
            }, 200);
            case "desiredFramerate" -> CommonUtility.delayMilliseconds(() -> {
                MainSingleton.getInstance().config.setDesiredFramerate(valueText);
                PipelineManager.restartCapture(CommonUtility::run);
            }, 200);
            case "resamplingFactor" -> CommonUtility.delayMilliseconds(() -> {
                Enums.ResamplingFactor rf = Enums.ResamplingFactor.findByValue(Integer.parseInt(valueText));
                if (rf != null) {
                    PipelineManager.restartCapture(CommonUtility::run, () ->
                            MainSingleton.getInstance().config.setResamplingFactor(rf.getResamplingFactorValue()));
                }
            }, 200);
            case "effectSelect" -> {
                String finalNewVal = LocalizedEnum.fromStr(Enums.Effect.class, valueText).getBaseI18n();
                NetworkManager.setEffect(finalNewVal);
            }
            case "toggleLed" -> {
                boolean on;
                if (value.isBoolean()) {
                    on = value.asBoolean();
                } else {
                    on = Boolean.parseBoolean(valueText);
                }
                CommonUtility.delayMilliseconds(() -> {
                    MainSingleton.getInstance().config.setToggleLed(on);
                    if (on) {
                        CommonUtility.turnOnLEDs();
                    } else {
                        CommonUtility.turnOffLEDs(MainSingleton.getInstance().config);
                    }
                }, 200);
            }
            default -> {
                // Not yet wired
            }
        }
    }

    /**
     * Handle GET /setConfigPage, serving the minimal HTML page that hosts the settings form.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleSetConfigPage(HttpExchange exchange) throws IOException {
        sendResource(exchange, SET_CONFIG_PAGE_RESOURCE, "text/html; charset=utf-8");
    }

    /**
     * Handle GET requests for the settings page JavaScript files.
     * Serves setConfig.js, setConfig-core.js, setConfig-device.js, setConfig-ui.js.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleSetConfigPageJs(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String resource;
        if (path.endsWith("setConfig-core.js")) {
            resource = SET_CONFIG_CORE_JS_RESOURCE;
        } else if (path.endsWith("setConfig-device.js")) {
            resource = SET_CONFIG_DEVICE_JS_RESOURCE;
        } else if (path.endsWith("setConfig-ui.js")) {
            resource = SET_CONFIG_UI_JS_RESOURCE;
        } else {
            resource = SET_CONFIG_PAGE_JS_RESOURCE;
        }
        sendResource(exchange, resource, "application/javascript; charset=utf-8");
    }

    /**
     * Read a class relative resource and send it as the HTTP response body.
     *
     * @param exchange the HTTP exchange to send the response on
     * @param resource the class relative resource name
     * @param mimeType the response MIME type
     * @throws IOException when the resource is missing or the response cannot be written
     */
    private void sendResource(HttpExchange exchange, String resource, String mimeType) throws IOException {
        try (InputStream resourceStream = getClass().getResourceAsStream(resource)) {
            if (resourceStream == null) {
                sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Resource not found: " + resource);
                return;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            resourceStream.transferTo(buffer);
            byte[] responseBytes = buffer.toByteArray();
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(responseBytes);
            }
        }
    }

    /**
     * Merge the payload fields into the configuration tree, excluding the fields not exposed to a client.
     *
     * @param payload    the JSON object received from the client
     * @param configTree the configuration tree to update in place
     */
    private void mergePayload(JsonNode payload, ObjectNode configTree) {
        payload.fields().forEachRemaining(entry -> {
            if (!EXCLUDED_FIELDS.contains(entry.getKey()) && !entry.getValue().isNull()) {
                configTree.set(entry.getKey(), entry.getValue());
            }
        });
    }

    /**
     * Wrap an HTTP handler enforcing the CORS preflight handling and a single allowed method.
     * OPTIONS requests are answered with no content, disallowed methods are rejected with a 405.
     *
     * @param handler       the handler to guard
     * @param methodAllowed the predicate deciding which method the handler accepts
     * @return the guarded handler
     */
    private HttpHandler withGuard(HttpHandler handler, Predicate<String> methodAllowed) {
        return exchange -> {
            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_NO_CONTENT, -1);
                    exchange.close();
                    return;
                }
                if (!methodAllowed.test(exchange.getRequestMethod())) {
                    byte[] responseBytes = ("Only the " + methodAllowedDescription(methodAllowed) + " method is supported").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(405, responseBytes.length);
                    try (OutputStream responseBody = exchange.getResponseBody()) {
                        responseBody.write(responseBytes);
                    }
                    return;
                }
                handler.handle(exchange);
            } catch (IOException e) {
                log.error("Config server request failed: {}", e.getMessage());
            }
        };
    }

    /**
         * Plain data holder for the current framerate counters, JSON friendly.
         */
        private record FpsDto(float producing, float consuming) {
    }

    /**
     * Send a JSON string as the HTTP response body.
     *
     * @param exchange   the HTTP exchange to send the response on
     * @param statusCode the HTTP status code to return
     * @param json       the JSON content to send
     * @throws IOException when the response cannot be written
     */
    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Handle GET /devicePrefs?ip=<addr>, proxying the device {@code /prefs} endpoint server-side.
     * The device firmware does not send CORS headers, so the browser cannot read {@code /prefs} directly;
     * this endpoint fetches it from the JVM (no CORS) and returns the JSON to the client.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleDevicePrefs(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String ip = query == null ? null : queryParam(query, "ip");
        if (ip == null || !ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Missing or invalid ip parameter");
            return;
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + ip + "/prefs")).timeout(Duration.ofSeconds(2)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            try {
                JsonNode prefsNode = CommonUtility.JSON_MAPPER.readTree(body);
                if (prefsNode.isObject()) {
                    ((ObjectNode) prefsNode).put("effect", LocalizedEnum.fromBaseStr(Enums.Effect.class, prefsNode.get("effect").asText()).getI18n());
                    ((ObjectNode) prefsNode).put("ffeffect", LocalizedEnum.fromBaseStr(Enums.Effect.class, prefsNode.get("ffeffect").asText()).getI18n());
                    body = CommonUtility.JSON_MAPPER.writeValueAsString(prefsNode);
                }
            } catch (Exception e) {
                log.warn("Device prefs response is not valid JSON: {}", e.getMessage());
            }
            byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(response.statusCode(), responseBytes.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(responseBytes);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendError(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "Interrupted");
        } catch (Exception e) {
            sendError(exchange, HttpURLConnection.HTTP_BAD_GATEWAY, "Device unreachable: " + e.getMessage());
        }
    }

    /**
     * Handle GET / (and any unknown path), serving the settings page.
     * The other endpoints are matched by their specific contexts before falling back to this root context.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    private void handleRoot(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isEmpty()) {
            handleSetConfigPage(exchange);
        } else if (path.endsWith(".js")) {
            handleSetConfigPageJs(exchange);
        } else {
            byte[] responseBytes = ("Not found: " + path).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, responseBytes.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(responseBytes);
            }
        }
    }

    /**
     * Human readable description of the method allowed by a guard predicate.
     *
     * @param methodAllowed the predicate deciding which method the handler accepts
     * @return the method name the guard allows
     */
    private String methodAllowedDescription(Predicate<String> methodAllowed) {
        for (String candidate : List.of("GET", "POST")) {
            if (methodAllowed.test(candidate)) {
                return candidate;
            }
        }
        return "GET";
    }

    /**
         * Possible values for a single configuration field, exposed to the web settings page.
         * The {@code value} is the exact value to persist (English i18n string for localized enums,
         * or the numeric value for numeric enums) and the {@code label} is the human readable English text.
         */
        private record FieldOptions(List<Option> options, String type) {

        /**
         * A single selectable value.
         */
        private record Option(String value, String label) {
            }
        }

    /**
     * Send a plain text error response and close the exchange.
     *
     * @param exchange   the HTTP exchange to reply on
     * @param statusCode the HTTP status code to return
     * @param message    the human-readable error description
     * @throws IOException when the response cannot be written
     */
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

}
