/*
  WebRtcSignalingServer.java

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.config.Constants;
import org.dpsoftware.grabber.WebRtcStreamer;
import org.glassfish.tyrus.server.Server;


/**
 * WebRTC signaling server for one live-preview viewer.
 */
@Slf4j
public class WebRtcSignalingServer {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WebRtcStreamer streamer;
    private volatile Session activeSession;
    private Server server;
    private volatile boolean running = false;

    /**
     * Creates the signaling server for the given GStreamer WebRTC streamer.
     *
     * @param streamer the GStreamer WebRTC streamer
     */
    public WebRtcSignalingServer(WebRtcStreamer streamer) {
        this.streamer = streamer;
    }

    /**
     * Starts the server when it is not already running.
     *
     * @param port the port to listen on (the dedicated WebRTC signaling port)
     */
    public synchronized void start(int port) {
        if (!streamer.isSupported()) {
            return;
        }
        if (running) {
            return;
        }
        try {
            WebRtcSignalingEndpoint.setServer(this);
            // Keep the endpoint path at /webrtc.
            server = new Server("0.0.0.0", port, "", null, WebRtcSignalingEndpoint.class);
            server.start();
            running = true;
            log.info("WebRTC signaling server listening on ws://<host>:{}{}", port, Constants.WEBRTC_SIGNALING_ENDPOINT);
        } catch (Exception e) {
            log.warn("Unable to start WebRTC signaling server on port {}", port, e);
        }
    }

    /**
     * Returns whether the required GStreamer WebRTC factories are available.
     *
     * @return true when browser WebRTC preview can be used
     */
    public boolean isWebRtcAvailable() {
        return streamer.isSupported();
    }

    /**
     * Stop the WebSocket server and tear down any active WebRTC session.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        close(activeSession);
        activeSession = null;
        streamer.stop();
        if (server != null) {
            server.stop();
            server = null;
        }
        running = false;
        log.info("WebRTC signaling server stopped");
    }

    /**
     * Starts streaming for a newly opened browser session.
     *
     * @param session the newly opened session
     */
    public void onClientConnected(Session session) {
        // Replace any previous viewer.
        Session previous = activeSession;
        activeSession = session;
        if (previous != null && !previous.getId().equals(session.getId())) {
            close(previous);
        }
        streamer.startSession(this, session);
    }

    /**
     * Relay a JSON frame received from the browser to the GStreamer pipeline.
     *
     * @param session the session that sent the frame
     * @param message the JSON payload
     */
    public void onClientMessage(Session session, String message) {
        if (!isActive(session)) {
            log.debug("Ignoring signaling message from inactive session {}", session.getId());
            return;
        }
        log.debug("WebRTC signaling message from {}: {}", session.getId(), message);
        try {
            JsonNode node = mapper.readTree(message);
            String type = node.has("type") ? node.get("type").asText() : "";
            if ("answer".equals(type)) {
                String sdp = node.get("sdp").asText();
                streamer.onAnswer(session.getId(), sdp);
            } else if ("ice".equals(type)) {
                String candidate = node.get("candidate").asText();
                int mLineIndex = node.has("sdpMLineIndex") ? node.get("sdpMLineIndex").asInt() : 0;
                streamer.onIceCandidate(session.getId(), mLineIndex, candidate);
            }
        } catch (Exception e) {
            log.warn("Failed to parse WebRTC signaling message: {}", e.getMessage());
        }
    }

    /**
     * A browser closed the /webrtc socket. The streamer stops its pipeline.
     *
     * @param session the closed session
     */
    public void onClientDisconnected(Session session) {
        if (isActive(session)) {
            activeSession = null;
        }
        if (activeSession == null) {
            streamer.stop();
        }
    }

    /**
     * Send a JSON frame to the connected browser. No-op when no session is open.
     *
     * @param json the JSON payload to send
     */
    public void sendToClient(String json) {
        Session session = activeSession;
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(json);
            } catch (Exception e) {
                log.warn("Failed to send WebRTC signaling message: {}", e.getMessage());
            }
        }
    }

    /**
     * Returns whether the given session is the currently active viewer.
     *
     * @param session the session to check
     * @return true when the session is the active one
     */
    private boolean isActive(Session session) {
        Session current = activeSession;
        return current != null && current.getId().equals(session.getId());
    }

    /**
     * Closes the session if it is open, ignoring close errors.
     *
     * @param session the session to close
     */
    private void close(Session session) {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Build the SDP offer JSON frame.
     *
     * @param sdp the SDP offer text
     * @return the JSON string
     */
    public String buildOfferMessage(String sdp) {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "offer");
        root.put("sdp", sdp);
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"type\":\"offer\",\"sdp\":\"\"}";
        }
    }

    /**
     * Build an ICE candidate JSON frame.
     *
     * @param candidate  the ICE candidate text
     * @param mLineIndex the SDP m-line index
     * @return the JSON string
     */
    public String buildIceMessage(String candidate, int mLineIndex) {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "ice");
        root.put("candidate", candidate);
        root.put("sdpMLineIndex", mLineIndex);
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"type\":\"ice\",\"candidate\":\"\",\"sdpMLineIndex\":0}";
        }
    }
}
