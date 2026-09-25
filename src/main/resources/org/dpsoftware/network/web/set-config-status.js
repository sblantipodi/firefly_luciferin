// Server status polling for the settings page: periodically fetches the FPS endpoint to show the live framerate and detect when the server is offline.
import {fetchJson} from './set-config-api.js';
import {showToast} from './set-config-ui.js';

var serverOnline = true;
var serverPollController = null;

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
        var el = document.getElementById('fpsCounter');
        if (el) {
            el.textContent = 'Firefly ' + Number(fps.producing).toFixed(0) + ' FPS / GlowWorm ' + Number(fps.consuming).toFixed(0) + ' FPS';
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
        var el = document.getElementById('fpsCounter');
        if (el) {
            el.textContent = '';
        }
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
