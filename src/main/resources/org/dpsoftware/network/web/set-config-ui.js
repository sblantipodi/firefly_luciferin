function notifyComboChange(name, value) {
    fetch('comboChange', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({comboName: name, value: value})
    }).catch(function () {
    });
}

function wireSelectChangeListeners() {
    document.querySelectorAll('select').forEach(function (el) {
        el.addEventListener('change', function () {
            notifyComboChange(el.id, el.value);
        });
    });
    var toggleLed = document.getElementById('toggleLed');
    if (toggleLed) {
        toggleLed.addEventListener('change', function () {
            notifyComboChange('toggleLed', toggleLed.checked);
        });
    }
}

function showToast(message, contextClass) {
    if (!document.getElementById('toastContainer')) {
        document.body.insertAdjacentHTML('beforeend', '<div id="toastContainer"><div></div></div>');
    }
    var container = document.getElementById('toastContainer').children[0];
    container.insertAdjacentHTML('beforeend', '<div class="toast ' + contextClass + '" role="alert" aria-live="assertive" aria-atomic="true"><div class="toast-body">' + message + '</div></div>');
    $($(container.lastElementChild)).toast('show');
}

function renderProfiles(data) {
    var container = document.getElementById('profilesList');
    if (!container) {
        return;
    }
    var profiles = data.profiles || [];
    var activeProfile = data.activeProfile || '';
    if (profiles.length === 0) {
        container.innerHTML = '<div class="text-muted">No profiles available</div>';
        return;
    }
    var defaultName = 'Default';
    var html = '';
    profiles.forEach(function (p) {
        var isActive = p === activeProfile;
        var badge = isActive ? ' <span class="badge bg-success">active</span>' : '';
        var deleteBtn = (p !== defaultName && !isActive)
            ? ' <button type="button" class="btn btn-sm btn-outline-danger float-end" title="Remove profile" onclick="event.stopPropagation();removeProfile(\'' + escapeHtml(p) + '\')">&times;</button>'
            : '';
        html += '<div class="list-group-item d-flex justify-content-between align-items-center">' +
            '<button type="button" class="list-group-item-action flex-grow-0 text-start" style="background:none;border:none;padding:0" onclick="activateProfile(\'' + escapeHtml(p) + '\')">' + escapeHtml(p) + badge + '</button>' +
            deleteBtn +
            '</div>';
    });
    container.innerHTML = html;
}

function activateProfile(name) {
    var confirmMsg = 'Activate profile "' + name + '"? Firefly will restart.';
    if (!confirm(confirmMsg)) {
        return;
    }
    fetch('activateProfile?name=' + encodeURIComponent(name), {method: 'POST'}).then(function (r) {
        if (!r.ok) {
            return r.text().then(function (t) {
                throw new Error(t || r.statusText);
            });
        }
        showToast('Activating profile: ' + name, 'bg-info text-white');
    }).catch(function (err) {
        showToast('Unable to activate profile: ' + err.message, 'bg-danger text-white');
    });
}

function addProfile() {
    var name = document.getElementById('newProfileName').value.trim();
    if (!name) {
        showToast('Enter a profile name', 'bg-warning text-dark');
        return;
    }
    var payload;
    try {
        payload = collectPayload();
    } catch (e) {
        showToast('Collect error: ' + e.message, 'bg-danger text-white');
        console.error('collectPayload failed', e);
        return;
    }
    var body = JSON.stringify(payload);
    fetch('addProfile?name=' + encodeURIComponent(name), {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: body
    }).then(function (r) {
        if (!r.ok) {
            return r.text().then(function (t) {
                throw new Error(t || r.statusText);
            });
        }
        showToast('Profile "' + name + '" added', 'bg-success text-white');
        document.getElementById('newProfileName').value = '';
        return fetchJson('listProfiles');
    }).then(function (data) {
        renderProfiles(data);
    }).catch(function (err) {
        showToast('Unable to add profile: ' + err.message, 'bg-danger text-white');
    });
}

function removeProfile(name) {
    var confirmMsg = 'Remove profile "' + name + '"? This cannot be undone.';
    if (!confirm(confirmMsg)) {
        return;
    }
    fetch('removeProfile?name=' + encodeURIComponent(name), {method: 'POST'}).then(function (r) {
        if (!r.ok) {
            return r.text().then(function (t) {
                throw new Error(t || r.statusText);
            });
        }
        showToast('Profile "' + name + '" removed', 'bg-success text-white');
        return fetchJson('listProfiles');
    }).then(function (data) {
        renderProfiles(data);
    }).catch(function (err) {
        showToast('Unable to remove profile: ' + err.message, 'bg-danger text-white');
    });
}

var livePreviewOn = false;
var livePreviewTimer = null;

function wireLivePreviewButton() {
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

var serverOnline = true;
var serverPollController = null;

function poll(force) {
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

function hideOfflineOverlay() {
    var overlay = document.getElementById('offlineOverlay');
    if (overlay) {
        overlay.classList.remove('show');
    }
}

$(function () {
    var br = '<br class="d-sm-none">';
    $('#subtitle').html('Bias Lighting and Ambient Light software' + br + ' designed for ' + br + 'Glow Worm Luciferin firmware');
    fetchJson('sectionTitles').then(function (titles) {
        sectionTitles = titles || {};
    }).catch(function () {
    }).then(function () {
        return fetchJson('getFieldOptions');
    }).then(function (data) {
        fieldOptions = (data && data.options) || {};
        fieldLabels = (data && data.labels) || {};
        buildForm();
        initColorPicker();
        wireLivePreviewButton();
        wireSelectChangeListeners();
        return fetchJson('getConfig');
    }).then(function (cfg) {
        window.__lastConfig = cfg || {};
        fillForm(cfg);
        applyAutoOutputDevice();
        var profile = cfg && cfg.activeProfile;
        var profileEl = document.getElementById('activeProfile');
        if (profileEl) {
            profileEl.textContent = profile ? ('Profile: ' + profile) : '';
        }
        syncDeviceFromPrefs();
        return fetchJson('listProfiles');
    }).then(function (profilesData) {
        renderProfiles(profilesData);
    }).catch(function (err) {
        showToast('Unable to load settings: ' + err.message, 'bg-danger text-white');
    });
    refreshDevices();
    poll();
    setInterval(poll, 1000);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) {
            // Replace any request left pending while the mobile tab was suspended.
            poll(true);
        }
    });
});
