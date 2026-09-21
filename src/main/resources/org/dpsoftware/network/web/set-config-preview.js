import {showToast} from './set-config-ui.js';

var livePreviewOn = false;
var livePreviewTimer = null;

export function wireLivePreviewButton() {
    var showBtn = document.getElementById('showLivePreview');
    if (!showBtn) {
        return;
    }
    var useWebrtc = (typeof window.webrtcPreview === 'object' && window.webrtcPreview !== null);
    showBtn.addEventListener('click', function () {
        var turningOn = !livePreviewOn;
        if (useWebrtc) {
            if (turningOn) {
                window.webrtcPreview.start().then(function (mode) {
                    setLivePreview(true, mode === 'webrtc');
                }).catch(function (err) {
                    showToast('Unable to start live preview: ' + err.message, 'bg-danger text-white');
                });
            } else {
                window.webrtcPreview.stop();
                setLivePreview(false, true);
            }
            return;
        }
        var url = turningOn ? 'screenshot/enable' : 'screenshot/enable?disable=true';
        fetch(url, {method: 'POST'}).then(function (r) {
            if (!r.ok) {
                throw new Error('HTTP ' + r.status);
            }
            return r.json();
        }).then(function () {
            setLivePreview(turningOn, false);
        }).catch(function (err) {
            showToast('Unable to toggle live preview: ' + err.message, 'bg-danger text-white');
        });
    });
}

function setLivePreview(on, useWebrtc) {
    livePreviewOn = on;
    var btn = document.getElementById('showLivePreview');
    if (btn) {
        btn.textContent = on ? 'Hide Live Preview' : 'Show Live Preview';
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
