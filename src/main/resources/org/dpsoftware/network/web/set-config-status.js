// Server status polling for the settings page: periodically fetches the FPS endpoint to show the live framerate and detect when the server is offline.
import {fetchJson} from './set-config-api.js';
import {showToast} from './set-config-ui.js';

var serverOnline = true;
var serverPollController = null;

// Separate capture and LED output counters so each has its own icon and visibility.
function updateFooterFps(status) {
    [
        {group: 'footerFpsGroup', value: 'fpsCounter', key: 'producing'},
        {group: 'footerGlowWormFpsGroup', value: 'glowWormFpsCounter', key: 'consuming'}
    ].forEach(function (counter) {
        var group = document.getElementById(counter.group);
        var value = document.getElementById(counter.value);
        if (!group || !value) {
            return;
        }
        var raw = status && status[counter.key];
        var fps = Number(raw);
        var valid = raw != null && raw !== '' && Number.isFinite(fps) && fps >= 0;
        group.hidden = !valid;
        value.textContent = valid ? Math.round(fps).toFixed(1) + 'FPS' : '';
    });
}

// Use the effective gamma and detected HDR state, matching the MQTT runtime values.
function updateFooterGamma(status) {
    var group = document.getElementById('footerGammaGroup');
    var value = document.getElementById('footerGamma');
    if (!group || !value) {
        return;
    }
    var gamma = status ? Number(status.adaptiveGamma) : NaN;
    var valid = !!status && status.adaptiveGamma != null && status.adaptiveGamma !== ''
        && Number.isFinite(gamma) && gamma > 0;
    group.hidden = !valid;
    value.textContent = valid ? gamma.toFixed(3) + (status.hdrActive === true ? ' HDR' : '') : '';
}

// Polls the 'fps' endpoint to update the FPS counter and detect server offline/online transitions (with a 3s timeout to bound pending
// requests); force=true aborts a previous in-flight poll (used on tab visibility change).
export function pollServerStatus(force) {
    if (serverPollController) {
        if (force !== true) {
            return;
        }
        serverPollController.abort();
    }
    var controller = new AbortController();
    serverPollController = controller;
    // Bound detection time even when the network leaves the request pending.
    var timeout = setTimeout(function () {
        controller.abort();
    }, 3000);
    return fetchJson('fps', {signal: controller.signal, cache: 'no-store'}).then(function (fps) {
        if (serverPollController !== controller) {
            return;
        }
        updateFooterGamma(fps);
        var logo = document.getElementById('settingsLogo');
        if (logo && fps.trayIconImage && logo.dataset.trayIconImage !== fps.trayIconImage) {
            logo.dataset.trayIconImage = fps.trayIconImage;
            logo.src = 'luciferin-logo.png?icon=' + encodeURIComponent(fps.trayIconImage);
        }
        updateFooterFps(fps);
        var updateNotice = document.getElementById('fireflyUpdateNotice');
        if (updateNotice) {
            updateNotice.hidden = !fps.fireflyUpdateAvailable;
        }
        var deviceUpdateAvailableNotice = document.getElementById('glowWormUpdateAvailableNotice');
        if (deviceUpdateAvailableNotice) {
            deviceUpdateAvailableNotice.hidden = !fps.glowWormUpdateAvailable;
        }
        var deviceUpdateNotice = document.getElementById('glowWormUpdateNotice');
        if (deviceUpdateNotice) {
            deviceUpdateNotice.hidden = !fps.glowWormUpdateInProgress;
        }
        if (!serverOnline) {
            serverOnline = true;
            document.body.classList.remove('server-down');
            hideOfflineOverlay();
            showToast('Firefly Luciferin is online again', 'bg-success text-white');
        }
    }).catch(function () {
        if (serverPollController !== controller) {
            return;
        }
        updateFooterFps(null);
        updateFooterGamma(null);
        if (serverOnline) {
            serverOnline = false;
            document.body.classList.add('server-down');
            showOfflineOverlay();
        }
    }).finally(function () {
        clearTimeout(timeout);
        if (serverPollController === controller) {
            serverPollController = null;
        }
    });
}

// Shows the full-screen "server offline/restarting" overlay (created lazily).
function showOfflineOverlay() {
    var overlay = document.getElementById('offlineOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'offlineOverlay';
        overlay.className = 'offline-overlay';
        overlay.innerHTML = '<div class="offline-overlay-content">Firefly Luciferin is offline or restarting</div>';
        document.body.appendChild(overlay);
    }
    overlay.classList.add('show');
}

// Hides the offline overlay when the server comes back online.
function hideOfflineOverlay() {
    var overlay = document.getElementById('offlineOverlay');
    if (overlay) {
        overlay.classList.remove('show');
    }
}
