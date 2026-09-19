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
 * In-process WebSocket signaling server for the WebRTC live preview.
 * <p>
 * It binds a Tyrus {@link Server} on the dedicated WebRTC signaling port
 * ({@link org.dpsoftware.config.Constants#WEBRTC_SIGNALING_DEFAULT_PORT}, separate from the
 * {@link ConfigServer} port because two sockets cannot share a port) and exposes a single
 * {@code /webrtc} endpoint ({@link WebRtcSignalingEndpoint}). When a browser connects, the
 * server asks the {@link WebRtcStreamer} to (re)build its GStreamer {@code webrtcbin} pipeline
 * and relays the SDP/ICE messages between the browser and the pipeline.
 * <p>
 * Only one viewer is supported at a time: a new connection tears down the previous one, which
 * matches the "Show Live Preview" button being a single consumer.
 */
@Slf4j
public class WebRtcSignalingServer {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WebRtcStreamer streamer;
    private volatile Session activeSession;
    private Server server;
    private volatile boolean running = false;

    public WebRtcSignalingServer(WebRtcStreamer streamer) {
        this.streamer = streamer;
    }

    /**
     * Start the WebSocket signaling server on {@code port}. Safe to call multiple times; it is a
     * no-op when the server is already running. The server binds the wildcard address so the
     * preview is reachable from any local interface.
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
            // Tyrus treats the third argument as the context path that prefixes the endpoint
            // declaration path. Passing an empty context path keeps the endpoint reachable at
            // ws://<host>:<port>/webrtc exactly as the browser expects (otherwise it would land
            // on /webrtc/webrtc and the handshake would fail with 404).
            server = new Server("0.0.0.0", port, "", null, WebRtcSignalingEndpoint.class);
            server.start();
            running = true;
            log.info("WebRTC signaling server listening on ws://<host>:{}{}", port, Constants.WEBRTC_SIGNALING_ENDPOINT);
        } catch (Exception e) {
            log.warn("Unable to start WebRTC signaling server on port {}", port, e);
        }
    }

    /**
     * Return whether the local GStreamer installation has the WebRTC and NICE factories needed
     * for the low-latency preview.
     *
     * @return {@code true} when browser WebRTC preview can be used
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
     * A browser opened the {@code /webrtc} socket. The streamer (re)starts its pipeline and will
     * push the SDP offer back through this channel via {@link #sendToClient}.
     *
     * @param session the newly opened session
     */
    public void onClientConnected(Session session) {
        // A new viewer supersedes any previous one: tear it down before registering the new one.
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
     * A browser closed the {@code /webrtc} socket. The streamer stops its pipeline.
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

    private boolean isActive(Session session) {
        Session current = activeSession;
        return current != null && current.getId().equals(session.getId());
    }

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
