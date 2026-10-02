// Live preview toggle for the settings page: starts/stops the preview using WebRTC when available, falling back to polled image screenshots otherwise.
import {showToast} from './set-config-ui.js';
import {state} from './set-config-state.js';

var livePreviewOn = false;
var livePreviewTimer = null;
var previewChangeQueue = Promise.resolve();

function startLivePreview() {
    var start = window.webrtcPreview
        ? window.webrtcPreview.start()
        : fetch('screenshot/enable', {method: 'POST'}).then(function (response) {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return 'image';
        });
    return Promise.resolve(start).then(function (mode) {
        setLivePreview(true, mode === 'webrtc');
    });
}

function stopLivePreview() {
    setLivePreview(false, true);
    if (window.webrtcPreview) return window.webrtcPreview.stop();
    return fetch('screenshot/enable?disable=true', {method: 'POST'}).then(function (response) {
        if (!response.ok) throw new Error('HTTP ' + response.status);
    });
}

// Serialize quality changes so rapid selections cannot overlap preview stop/start requests.
export function withLivePreviewRestart(change) {
    var update = previewChangeQueue.then(async function () {
        if (!livePreviewOn) return change();
        var button = document.getElementById('showLivePreview');
        if (button) button.disabled = true;
        try {
            await stopLivePreview();
            try {
                return await change();
            } finally {
                // The server schedules scaling after 200ms, then restarts capture after 1s.
                await new Promise(function (resolve) {
                    setTimeout(resolve, 1500);
                });
                await startLivePreview();
            }
        } finally {
            if (button) button.disabled = false;
        }
    });
    previewChangeQueue = update.catch(function () {
    });
    return update;
}

// Wires the "Show Live Preview" button: uses WebRTC when available, otherwise enables/disables the server-side screenshot stream via the
// screenshot endpoint.
export function wireLivePreviewButton() {
    var showBtn = document.getElementById('showLivePreview');
    if (!showBtn) {
        return;
    }
    showBtn.addEventListener('click', function () {
        showBtn.disabled = true;
        Promise.resolve(livePreviewOn ? stopLivePreview() : startLivePreview()).catch(function (err) {
            showToast('Unable to toggle live preview: ' + err.message, 'bg-danger text-white');
        }).finally(function () {
            showBtn.disabled = false;
        });
    });
}

// Toggles the preview UI: button label/state, screenshot vs WebRTC video visibility, fallback notice, and the screenshot polling timer
// (image mode).
function setLivePreview(on, useWebrtc) {
    livePreviewOn = on;
    var btn = document.getElementById('showLivePreview');
    if (btn) {
        btn.textContent = on ? (state.fieldLabels['web.hidePreview'] || 'Hide Live Preview') : (state.fieldLabels['web.showPreview'] || 'Show Live Preview');
        btn.classList.toggle('active', on);
    }
    var img = document.getElementById('screenshot');
    if (img) {
        img.classList.toggle('show', on && !useWebrtc);
        if (!on || useWebrtc) {
            img.onload = null;
            img.onerror = null;
            img.src = '';
        }
    }
    if (useWebrtc && window.webrtcPreview) {
        window.webrtcPreview.showVideo(on);
    }
    var fallbackNotice = document.getElementById('livePreviewFallbackNotice');
    if (fallbackNotice) {
        fallbackNotice.classList.toggle('show', on && !useWebrtc);
    }
    if (livePreviewTimer) {
        clearInterval(livePreviewTimer);
        livePreviewTimer = null;
    }
    if (on && !useWebrtc) {
        pollScreenshot();
        livePreviewTimer = setInterval(pollScreenshot, 500);
    }
}

// Refreshes the screenshot <img> from the server (cache-busting query), used while the image-mode live preview is active.
function pollScreenshot() {
    if (!livePreviewOn) {
        return;
    }
    var img = document.getElementById('screenshot');
    if (!img) {
        return;
    }
    img.src = 'screenshot?t=' + Date.now();
    img.onload = function () {
        if (livePreviewOn) {
            img.classList.add('show');
        }
    };
    img.onerror = function () {
        img.classList.remove('show');
    };
}
