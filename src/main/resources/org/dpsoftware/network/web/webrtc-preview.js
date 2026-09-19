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
// sibling port (CONFIG_SERVER_DEFAULT_PORT + 1) bound to the same interface.

(function () {
    var CONFIG_SERVER_DEFAULT_PORT = 33556;
    var SIGNALING_PORT_OFFSET = 1;
    var videoEl = null;
    var socket = null;
    var peer = null;
    var reconnectTimer = null;
    var keepAliveTimer = null;
    var pendingIceCandidates = [];

    function ensureVideoElement() {
        if (!videoEl) {
            videoEl = document.getElementById('webrtcPreview');
        }
        return videoEl;
    }

    function signalingUrl() {
        var host = location.hostname || '127.0.0.1';
        var port = Number((location.port && location.port !== '') ? location.port : CONFIG_SERVER_DEFAULT_PORT);
        return 'ws://' + host + ':' + (port + SIGNALING_PORT_OFFSET) + '/webrtc';
    }

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

    function addIceCandidate(p, candidate) {
        p.addIceCandidate(new RTCIceCandidate(candidate)).catch(function (err) {
            console.warn('WebRTC addIceCandidate failed:', err);
        });
    }

    function flushPendingIceCandidates(p) {
        while (pendingIceCandidates.length) {
            addIceCandidate(p, pendingIceCandidates.shift());
        }
    }

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

    function startKeepAlive() {
        if (keepAliveTimer) {
            return;
        }
        keepAliveTimer = setInterval(function () {
            fetch('screenshot/enable?keepalive=true', {method: 'POST'}).catch(function () {});
        }, 10000);
    }

    function stopKeepAlive() {
        if (keepAliveTimer) {
            clearInterval(keepAliveTimer);
            keepAliveTimer = null;
        }
    }

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
