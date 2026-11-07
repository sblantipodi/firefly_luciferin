// Live preview toggle for the settings page: starts/stops the preview using WebRTC when available, falling back to polled image screenshots otherwise.
import {showToast} from './set-config-ui.js';
import {state} from './set-config-state.js';

var livePreviewOn = false;
var livePreviewTimer = null;
var previewChangeQueue = Promise.resolve();
var ledLayoutOn = false;
var ledLayoutTimer = null;
var ledLayoutRequest = null;
var lastLedLayout = '';

// Fetch only while visible. An aborted request cannot redraw an overlay after preview shutdown.
async function refreshLedLayout() {
    if (!livePreviewOn || !ledLayoutOn || ledLayoutRequest) return;
    var request = new AbortController();
    ledLayoutRequest = request;
    try {
        var response = await fetch('screenshot/leds', {signal: request.signal, cache: 'no-store'});
        if (!response.ok) throw new Error('HTTP ' + response.status);
        var layout = await response.json();
        if (request.signal.aborted || !livePreviewOn || !ledLayoutOn) return;
        var serialized = JSON.stringify(layout);
        if (serialized !== lastLedLayout) {
            drawLedLayout(layout);
            lastLedLayout = serialized;
        }
        document.getElementById('ledLayout').removeAttribute('hidden');
    } catch (error) {
        if (!request.signal.aborted) {
            document.getElementById('ledLayout').setAttribute('hidden', '');
            console.warn('Unable to load LED layout:', error);
        }
    } finally {
        if (ledLayoutRequest === request) ledLayoutRequest = null;
    }
}

function drawLedLayout(layout) {
    var svg = document.getElementById('ledLayout');
    svg.setAttribute('viewBox', '0 0 ' + layout.width + ' ' + layout.height);
    var fragment = document.createDocumentFragment();

    function element(name, attributes, text) {
        var node = document.createElementNS('http://www.w3.org/2000/svg', name);
        Object.keys(attributes).forEach(function (key) {
            node.setAttribute(key, attributes[key]);
        });
        if (text !== undefined) node.textContent = text;
        return node;
    }

    layout.leds.forEach(function (led) {
        var group = element('g', {});
        var color = led.active ? 'hsl(' + ((105 + led.number / layout.leds.length * 300) % 360) + ',100%,50%)' : '#fff';
        var gap = Math.min(layout.width / 600, led.width / 8, led.height / 8);
        group.appendChild(element('rect', {
            x: led.x + gap, y: led.y + gap,
            width: Math.max(0, led.width - gap * 2), height: Math.max(0, led.height - gap * 2),
            fill: led.active ? color : '#000', 'fill-opacity': led.active ? 0.22 : 0.65,
            stroke: color, 'stroke-width': 1, 'vector-effect': 'non-scaling-stroke'
        }));
        var label = '#' + led.number;
        var fontSize = Math.min(layout.width / 90, led.height * 0.45, led.width / (label.length * 0.65));
        group.appendChild(element('text', {
            x: led.x + led.width / 2, y: led.y + led.height / 2,
            'text-anchor': 'middle', 'dominant-baseline': 'central',
            fill: '#fff', stroke: '#000', 'stroke-width': fontSize / 7,
            'paint-order': 'stroke', 'font-size': fontSize, 'font-weight': 'bold', 'font-family': 'sans-serif'
        }, label));
        fragment.appendChild(group);
    });
    svg.replaceChildren(fragment);
}

function updateLedLayout() {
    var button = document.getElementById('showLedLayout');
    button.hidden = !livePreviewOn;
    button.disabled = !livePreviewOn;
    button.textContent = ledLayoutOn
        ? (state.fieldLabels['web.hideLedLayout'] || 'Hide LED layout')
        : (state.fieldLabels['web.showLedLayout'] || 'Show LED layout');
    button.setAttribute('aria-pressed', String(ledLayoutOn));
    button.classList.toggle('active', ledLayoutOn);
    if (ledLayoutTimer) clearInterval(ledLayoutTimer);
    ledLayoutTimer = null;
    if (livePreviewOn && ledLayoutOn) {
        refreshLedLayout();
        ledLayoutTimer = setInterval(refreshLedLayout, 1000);
    } else {
        if (ledLayoutRequest) ledLayoutRequest.abort();
        ledLayoutRequest = null;
        document.getElementById('ledLayout').setAttribute('hidden', '');
    }
}

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
    document.getElementById('showLedLayout').addEventListener('click', function () {
        ledLayoutOn = !ledLayoutOn;
        updateLedLayout();
    });
    updateLedLayout();
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
    updateLedLayout();
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
