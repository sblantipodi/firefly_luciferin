/*
  WebRtcSignalingEndpoint.java

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

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket endpoint for browser and GStreamer WebRTC signaling.
 */
@Slf4j
@ServerEndpoint("/webrtc")
public class WebRtcSignalingEndpoint {

    private static volatile WebRtcSignalingServer server; // Shared server for Tyrus endpoint instances.

    /**
     * Wire the signaling server to this endpoint.
     *
     * @param server the signaling server
     */
    public static void setServer(WebRtcSignalingServer server) {
        WebRtcSignalingEndpoint.server = server;
    }

    /**
     * Fired when a browser opens the /webrtc socket; starts streaming for the session.
     *
     * @param session the newly opened WebSocket session
     */
    @OnOpen
    public void onOpen(Session session) {
        log.info("WebRTC signaling client connected: {}", session.getId());
        if (server != null) {
            server.onClientConnected(session);
        }
    }

    /**
     * Receive a JSON frame from the browser.
     *
     * @param message the JSON payload (answer or ICE candidate)
     * @param session the open WebSocket session
     */
    @SuppressWarnings("unused")
    @OnMessage
    public void onMessage(String message, Session session) {
        if (server != null) {
            server.onClientMessage(session, message);
        }
    }

    /**
     * Fired when a browser closes the /webrtc socket; stops the stream when no viewer is left.
     *
     * @param session the closed WebSocket session
     */
    @SuppressWarnings("unused")
    @OnClose
    public void onClose(Session session) {
        log.info("WebRTC signaling client disconnected: {}", session.getId());
        if (server != null) {
            server.onClientDisconnected(session);
        }
    }

    /**
     * Fired on a WebSocket error; logs it.
     *
     * @param session   the affected WebSocket session
     * @param throwable the error that occurred
     */
    @SuppressWarnings("unused")
    @OnError
    public void onError(Session session, Throwable throwable) {
        log.warn("WebRTC signaling error on session {}: {}", session.getId(), throwable.getMessage());
    }
}
