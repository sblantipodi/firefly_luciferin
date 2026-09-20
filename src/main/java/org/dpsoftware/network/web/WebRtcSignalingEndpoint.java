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

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket endpoint that carries the WebRTC signaling (SDP offer/answer and ICE candidates)
 * between the browser live-preview page and the GStreamer {@code webrtcbin} pipeline.
 * <p>
 * The browser is the answerer: it creates an {@code answer} to the {@code offer} produced by
 * {@code webrtcbin} and sends it back over this channel, together with the remote ICE candidates.
 * The server sends its own ICE candidates as soon as they are gathered.
 * <p>
 * The in/out JSON shapes mirror the browser payload:
 * <ul>
 *     <li>offer (server→client): {"type":"offer","sdp":"..."}</li>
 *     <li>answer (client→server): {"type":"answer","sdp":"..."}</li>
 *     <li>ice (both ways): {"type":"ice","candidate":"...","sdpMLineIndex":0}</li>
 * </ul>
 */
@Slf4j
@ServerEndpoint("/webrtc")
public class WebRtcSignalingEndpoint {

    /**
     * The signaling server owning the WebRTC pipeline. Set by {@link WebRtcSignalingServer} at
     * startup; the Tyrus container instantiates this endpoint with a no-arg constructor, so the
     * server is shared through this static field (there is a single signaling server per process).
     */
    private static volatile WebRtcSignalingServer server;

    /**
     * Wire the signaling server to this endpoint.
     *
     * @param server the signaling server
     */
    public static void setServer(WebRtcSignalingServer server) {
        WebRtcSignalingEndpoint.server = server;
    }

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
    @OnMessage
    public void onMessage(String message, Session session) {
        if (server != null) {
            server.onClientMessage(session, message);
        }
    }

    @OnClose
    public void onClose(Session session) {
        log.info("WebRTC signaling client disconnected: {}", session.getId());
        if (server != null) {
            server.onClientDisconnected(session);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.warn("WebRTC signaling error on session {}: {}", session.getId(), throwable.getMessage());
    }
}
