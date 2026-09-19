/*
  LivePreviewWebHandler.java

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

import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.InstanceConfigurer;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.utilities.CommonUtility;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Handles the live preview endpoints: toggling capture and its WebRTC signaling server, with an
 * idle watchdog that automatically turns the preview off when the web client becomes inactive.
 */
@Slf4j
public class LivePreviewWebHandler {

    // Forces the compatible image-based preview even when the local WebRTC/NICE plugins are installed.
    private static final boolean FORCE_IMAGE_LIVE_PREVIEW = Boolean.parseBoolean(
            System.getenv("LUCIFERIN_LIVE_PREVIEW_IMAGE"));
    private static final String JSON_OK = "{\"status\":\"OK\"}";
    private static final String JSON_IMAGE_PREVIEW = "{\"status\":\"OK\",\"livePreviewMode\":\"image\"}";
    private static final String JSON_WEBRTC_PREVIEW = "{\"status\":\"OK\",\"livePreviewMode\":\"webrtc\"}";
    // Idle timeout, in milliseconds, after which the live preview is automatically turned off.
    private static final int LIVE_PREVIEW_IDLE_MILLIS = 30000;
    private volatile long lastScreenshotGetMillis = 0L;
    // Watchdog thread that turns off the live capture flag when no {@code GET /screenshot} has arrived for more than #LIVE_PREVIEW_IDLE_MILLIS} ms.
    private Thread livePreviewWatchdog;
    private final WebRtcSignalingServer webRtcSignalingServer;

    public LivePreviewWebHandler(WebRtcSignalingServer webRtcSignalingServer) {
        this.webRtcSignalingServer = webRtcSignalingServer;
    }

    /**
     * Read the {@code disable} query parameter from the request, or {@code null} when absent.
     *
     * @param exchange the HTTP exchange containing the request
     * @return the disable value or {@code null}
     */
    private static String queryDisableParam(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals("disable")) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    /**
     * Returns whether this request only refreshes the live-preview idle timer.
     */
    private static boolean isKeepAliveRequest(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        return query != null && query.contains("keepalive=true");
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
    public void handleGetScreenshot(HttpExchange exchange) throws IOException {
        // A client is actively pulling frames: refresh the idle timestamp so the watchdog does
        // not turn off the live preview while the page is polling.
        lastScreenshotGetMillis = System.currentTimeMillis();
        File bmp = new File(InstanceConfigurer.getConfigPath(), Constants.GSTREAMER_SCREENSHOT);
        if (!bmp.exists() || !bmp.isFile()) {
            sendNotAvailableError(exchange);
            return;
        }
        byte[] imageBytes = Files.readAllBytes(bmp.toPath());
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
    public void handleEnableScreenshot(HttpExchange exchange) throws IOException {
        if (isKeepAliveRequest(exchange)) {
            lastScreenshotGetMillis = System.currentTimeMillis();
            sendOkJson(exchange);
            return;
        }
        boolean disable = "true".equalsIgnoreCase(queryDisableParam(exchange));
        boolean on = !disable;
        boolean webRtcAvailable = !FORCE_IMAGE_LIVE_PREVIEW && webRtcSignalingServer.isWebRtcAvailable();
        GStreamerGrabber.imageLivePreviewFallback = on && !webRtcAvailable;
        GuiSingleton.getInstance().setShowLiveCapture(on);
        if (on) {
            if (webRtcAvailable) {
                webRtcSignalingServer.start(Constants.WEBRTC_SIGNALING_DEFAULT_PORT);
            } else {
                // Covers a live environment change only after restart and also tears down a
                // previously open WebRTC session before serving image frames.
                webRtcSignalingServer.stop();
            }
            if (!MainSingleton.getInstance().RUNNING) {
                PipelineManager.restartCapture(CommonUtility::run);
            }
            startLivePreviewWatchdog();
        } else {
            stopLivePreviewWatchdog();
            webRtcSignalingServer.stop();
        }
        log.info("Live preview toggled: showLiveCapture set to {}, mode={}{}", on,
                webRtcAvailable ? "webrtc" : "image",
                FORCE_IMAGE_LIVE_PREVIEW ? " (forced by LUCIFERIN_LIVE_PREVIEW_IMAGE)" : "");
        sendJson(exchange, on && !webRtcAvailable ? JSON_IMAGE_PREVIEW : JSON_WEBRTC_PREVIEW);
    }

    /**
     * Stop the live preview watchdog, if any is running.
     */
    public synchronized void stopLivePreviewWatchdog() {
        if (livePreviewWatchdog != null) {
            livePreviewWatchdog.interrupt();
            livePreviewWatchdog = null;
        }
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
                    webRtcSignalingServer.stop();
                    return;
                }
            }
        }, "live-preview-watchdog");
        livePreviewWatchdog.setDaemon(true);
        livePreviewWatchdog.start();
    }

    /**
     * Send a JSON {@code OK} response.
     *
     * @param exchange the HTTP exchange to send the response on
     * @throws IOException when the response cannot be written
     */
    private void sendOkJson(HttpExchange exchange) throws IOException {
        sendJson(exchange, JSON_OK);
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }

    /**
     * Send a 404 plain text error response.
     *
     * @param exchange the HTTP exchange to reply on
     * @throws IOException when the response cannot be written
     */
    private void sendNotAvailableError(HttpExchange exchange) throws IOException {
        byte[] responseBytes = "Screenshot not available".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, responseBytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(responseBytes);
        }
    }
}
