// Browser-side WebRTC live preview.
//
// The Firefly server (GStreamer webrtcbin) is the offerer: it creates the SDP offer and
// pushes the captured frames into it. This module is the answerer. When the user enables
// the preview it:
//   1. POSTs screenshot/enable so the grabber keeps producing live frames.
//   2. opens a WebSocket to the dedicated signaling port.
//   3. answers the incoming offer and relays ICE candidates both ways.
//
// The WebSocket URL is derived from the page origin: the signaling server runs on the
// sibling port (config server default port + 1) bound to the same interface. The port is
// injected at serve time from the Java-side Constants.CONFIG_SERVER_DEFAULT_PORT.PORT_PLACEHOLDER

(function () {
    // __CONFIG_SERVER_DEFAULT_PORT__ is replaced at serve time with the Java-side
    // Constants.CONFIG_SERVER_DEFAULT_PORT. The || fallback covers the case where
    // the resource is served statically without the substitution (e.g. from the IDE).
    var CONFIG_SERVER_DEFAULT_PORT = __CONFIG_SERVER_DEFAULT_PORT__ || 8091;
    var SIGNALING_PORT_OFFSET = 1;
    var videoEl = null;
    var socket = null;
    var peer = null;
    var reconnectTimer = null;
    var keepAliveTimer = null;
    var pendingIceCandidates = [];

    // Lazily fetches the live preview <video> element.
    function ensureVideoElement() {
        if (!videoEl) {
            videoEl = document.getElementById('webrtcPreview');
        }
        return videoEl;
    }

    // Builds the WebSocket signaling URL from the page origin (config server port + 1, same host).
    function signalingUrl() {
        var host = location.hostname || '127.0.0.1';
        var port = Number((location.port && location.port !== '') ? location.port : CONFIG_SERVER_DEFAULT_PORT);
        return 'ws://' + host + ':' + (port + SIGNALING_PORT_OFFSET) + '/webrtc';
    }

    // Creates (or reuses) the RTCPeerConnection with its ICE/track/connection handlers; remote tracks are attached to the video element.
    function createPeer() {
        if (peer) {
            return peer;
        }
        peer = new RTCPeerConnection({
            iceServers: [{urls: 'stun:stun.l.google.com:19302'}]
        });
        peer.onicecandidate = function (event) {
            if (event.candidate && socket && socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({
                    type: 'ice',
                    candidate: event.candidate.candidate,
                    sdpMLineIndex: event.candidate.sdpMLineIndex
                }));
            }
        };
        peer.ontrack = function (event) {
            if (videoEl && event.streams && event.streams[0]) {
                videoEl.srcObject = event.streams[0];
                videoEl.play().catch(function () {});
            }
        };
        peer.onconnectionstatechange = function () {
            console.debug('WebRTC connection state:', peer.connectionState);
            if (peer.connectionState === 'failed') {
                stop();
            }
        };
        return peer;
    }

    // Adds a single ICE candidate to the peer connection (errors are logged).
    function addIceCandidate(p, candidate) {
        p.addIceCandidate(new RTCIceCandidate(candidate)).catch(function (err) {
            console.warn('WebRTC addIceCandidate failed:', err);
        });
    }

    // Adds any ICE candidates that arrived before the remote description was set (they are queued in pendingIceCandidates in the meantime).
    function flushPendingIceCandidates(p) {
        while (pendingIceCandidates.length) {
            addIceCandidate(p, pendingIceCandidates.shift());
        }
    }

    // Handles signaling messages from the server: 'offer' is answered via the normal SDP flow and the answer sent back; 'ice' candidates are
    // applied (or queued until the remote description exists).
    function handleMessage(raw) {
        var msg;
        try {
            msg = JSON.parse(raw);
        } catch (e) {
            return;
        }
        if (!msg || !msg.type) {
            return;
        }
        var p = createPeer();
        if (msg.type === 'offer') {
            p.setRemoteDescription({type: 'offer', sdp: msg.sdp})
                .then(function () {
                    flushPendingIceCandidates(p);
                    return p.createAnswer();
                })
                .then(function (answer) {
                    return p.setLocalDescription(answer);
                })
                .then(function () {
                    if (socket && socket.readyState === WebSocket.OPEN) {
                        socket.send(JSON.stringify({type: 'answer', sdp: p.localDescription.sdp}));
                    }
                })
                .catch(function (err) {
                    console.warn('WebRTC offer/answer failed:', err);
                });
        } else if (msg.type === 'ice') {
            var candidate = {sdpMLineIndex: msg.sdpMLineIndex, candidate: msg.candidate};
            if (p.remoteDescription) {
                addIceCandidate(p, candidate);
            } else {
                pendingIceCandidates.push(candidate);
            }
        }
    }

    // Starts the preview: enables the grabber (screenshot/enable), keeps it alive, then either reports 'image' mode (no WebRTC) or opens the
    // signaling socket to start the 'webrtc' mode.
    function start() {
        if (socket && socket.readyState <= WebSocket.OPEN) {
            return;
        }
        var video = ensureVideoElement();
        if (video) {
            video.classList.add('show');
        }
        return fetch('screenshot/enable', {method: 'POST'}).then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        }).then(function (result) {
            startKeepAlive();
            if (result.livePreviewMode === 'image') {
                if (video) {
                    video.classList.remove('show');
                }
                return 'image';
            }
            openSocket();
            return 'webrtc';
        });
    }

    // Starts the 10s keep-alive poll that keeps the server-side grabber alive while the preview is on.
    function startKeepAlive() {
        if (keepAliveTimer) {
            return;
        }
        keepAliveTimer = setInterval(function () {
            fetch('screenshot/enable?keepalive=true', {method: 'POST'}).catch(function () {});
        }, 10000);
    }

    // Stops the keep-alive poll.
    function stopKeepAlive() {
        if (keepAliveTimer) {
            clearInterval(keepAliveTimer);
            keepAliveTimer = null;
        }
    }

    // Opens the signaling WebSocket; on close it retries after 1.5s while the preview button still indicates the preview should be on.
    function openSocket() {
        socket = new WebSocket(signalingUrl());
        socket.onopen = function () {
            if (reconnectTimer) {
                clearTimeout(reconnectTimer);
                reconnectTimer = null;
            }
        };
        socket.onmessage = function (event) {
            handleMessage(event.data);
        };
        socket.onclose = function () {
            if (videoEl) {
                videoEl.classList.remove('show');
            }
            // Reconnect while the preview is still considered on by the button state.
            if (document.getElementById('showLivePreview') &&
                document.getElementById('showLivePreview').classList.contains('active')) {
                reconnectTimer = setTimeout(openSocket, 1500);
            }
        };
        socket.onerror = function () {
            // onclose fires after onerror.
        };
    }

    // Stops the preview: clears timers, closes the socket and peer connection, hides the video and disables the grabber on the server.
    function stop() {
        stopKeepAlive();
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }
        if (socket) {
            socket.close();
            socket = null;
        }
        if (peer) {
            peer.close();
            peer = null;
        }
        pendingIceCandidates = [];
        if (videoEl) {
            videoEl.classList.remove('show');
            videoEl.srcObject = null;
        }
        fetch('screenshot/enable?disable=true', {method: 'POST'}).catch(function () {
        });
    }

    // Shows or hides the WebRTC <video> element (toggled from the page controller).
    function showVideo(on) {
        var video = ensureVideoElement();
        if (video) {
            video.classList.toggle('show', on);
        }
    }

    // Exposed to the page controller (set-config-ui.js).
    window.webrtcPreview = {
        start: start,
        stop: stop,
        showVideo: showVideo
    };
})();
