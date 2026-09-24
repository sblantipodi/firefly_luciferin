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
import org.dpsoftware.config.EnvConstants;
import org.dpsoftware.config.InstanceConfigurer;
import org.dpsoftware.grabber.GStreamerGrabber;
import org.dpsoftware.gui.GuiSingleton;
import org.dpsoftware.managers.PipelineManager;
import org.dpsoftware.utilities.CommonUtility;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;

/**
 * Handles the live preview endpoints: toggling capture and its WebRTC signaling server, with an
 * idle watchdog that automatically turns the preview off when the web client becomes inactive.
 */
@Slf4j
public class LivePreviewWebHandler {

    private static final boolean FORCE_IMAGE_LIVE_PREVIEW = Boolean.parseBoolean( // Forces image preview.
            System.getenv(EnvConstants.LUCIFERIN_LIVE_PREVIEW_IMAGE));
    private static final String JSON_IMAGE_PREVIEW = "{\"status\":\"OK\",\"livePreviewMode\":\"image\"}";
    private static final String JSON_WEBRTC_PREVIEW = "{\"status\":\"OK\",\"livePreviewMode\":\"webrtc\"}";
    private static final int LIVE_PREVIEW_IDLE_MILLIS = 30000; // Preview idle timeout in milliseconds.
    private volatile long lastScreenshotGetMillis = 0L;
    private Thread livePreviewWatchdog; // Disables capture after the idle timeout.
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
     * Returns whether this request only refreshes the live preview idle timer.
     */
    private static boolean isKeepAliveRequest(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        return query != null && query.contains("keepalive=true");
    }

    /**
     * Serves the latest captured preview frame.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleGetScreenshot(HttpExchange exchange) throws IOException {
        // Keep the preview active while frames are requested.
        lastScreenshotGetMillis = System.currentTimeMillis();
        File bmp = new File(InstanceConfigurer.getConfigPath(), Constants.GSTREAMER_SCREENSHOT);
        if (!bmp.exists() || !bmp.isFile()) {
            HttpResponses.sendText(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Screenshot not available");
            return;
        }
        byte[] imageBytes = Files.readAllBytes(bmp.toPath());
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        HttpResponses.sendBytes(exchange, HttpURLConnection.HTTP_OK, "image/bmp", imageBytes);
    }

    /**
     * Enables or disables runtime preview capture.
     *
     * @param exchange the HTTP exchange containing the request and response
     * @throws IOException when the response cannot be written
     */
    public void handleEnableScreenshot(HttpExchange exchange) throws IOException {
        if (isKeepAliveRequest(exchange)) {
            lastScreenshotGetMillis = System.currentTimeMillis();
            HttpResponses.sendOk(exchange);
            return;
        }
        boolean disable = "true".equalsIgnoreCase(queryDisableParam(exchange));
        boolean on = !disable;
        boolean webRtcAvailable = !FORCE_IMAGE_LIVE_PREVIEW && webRtcSignalingServer.isWebRtcAvailable();
        GStreamerGrabber.imageLivePreviewFallback = on && !webRtcAvailable;
        GuiSingleton.getInstance().setShowLiveCapture(on);
        if (on) {
            if (webRtcAvailable) {
                webRtcSignalingServer.start(Constants.CONFIG_SERVER_DEFAULT_PORT + 1);
            } else {
                // Close WebRTC before serving image frames.
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
        HttpResponses.sendRawJson(exchange, HttpURLConnection.HTTP_OK, on && !webRtcAvailable ? JSON_IMAGE_PREVIEW : JSON_WEBRTC_PREVIEW);
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

    /** Starts or restarts the preview idle watchdog. */
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
}
