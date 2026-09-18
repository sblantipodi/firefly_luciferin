function showToast(message, contextClass) {
    if (!document.getElementById('toastContainer')) {
        document.body.insertAdjacentHTML('beforeend', '<div id="toastContainer" style="position:relative;"><div style="position:absolute;top:0;right:0;"></div></div>');
    }
    var container = document.getElementById('toastContainer').children[0];
    container.insertAdjacentHTML('beforeend', '<div class="toast ' + contextClass + '" role="alert" aria-live="assertive" aria-atomic="true" style="position:fixed;bottom:10px;right:10px;z-index:1051;"><div class="toast-body">' + message + '</div></div>');
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
    var html = '';
    profiles.forEach(function (p) {
        var isActive = p === activeProfile;
        var badge = isActive ? ' <span class="badge bg-success">active</span>' : '';
        html += '<button type="button" class="list-group-item list-group-item-action" onclick="activateProfile(\'' + escapeHtml(p) + '\')">' + escapeHtml(p) + badge + '</button>';
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

var livePreviewOn = false;
var livePreviewTimer = null;

function wireLivePreviewButton() {
    var showBtn = document.getElementById('showLivePreview');
    if (!showBtn) {
        return;
    }
    showBtn.addEventListener('click', function () {
        var turningOn = !livePreviewOn;
        var url = turningOn ? 'screenshot/enable' : 'screenshot/enable?disable=true';
        fetch(url, {method: 'POST'}).then(function (r) {
            if (!r.ok) {
                throw new Error('HTTP ' + r.status);
            }
            return r.json();
        }).then(function () {
            setLivePreview(turningOn);
        }).catch(function (err) {
            showToast('Unable to toggle live preview: ' + err.message, 'bg-danger text-white');
        });
    });
}

function setLivePreview(on) {
    livePreviewOn = on;
    var btn = document.getElementById('showLivePreview');
    if (btn) {
        if (on) {
            btn.textContent = 'Hide Live Preview';
            btn.classList.remove('btn-outline-success');
            btn.style.backgroundColor = '#28a745';
            btn.style.border = '0';
            btn.style.color = '#fff';
            btn.style.fontWeight = 'bold';
        } else {
            btn.textContent = 'Show Live Preview';
            btn.style.backgroundColor = 'lightgrey';
            btn.style.border = '0';
            btn.style.color = '#fff';
            btn.style.fontWeight = 'bold';
        }
    }
    var img = document.getElementById('screenshot');
    if (img) {
        img.style.display = on ? 'inline-block' : 'none';
        if (!on) {
            img.onload = null;
            img.onerror = null;
            img.src = '';
        }
    }
    if (livePreviewTimer) {
        clearInterval(livePreviewTimer);
        livePreviewTimer = null;
    }
    if (on) {
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
        if (!livePreviewOn) {
            return;
        }
        img.style.display = 'inline-block';
    };
    img.onerror = function () {
        if (!livePreviewOn) {
            return;
        }
        img.style.display = 'none';
    };
}

function pollFps() {
    fetchJson('fps').then(function (fps) {
        var el = document.getElementById('fpsCounter');
        if (el) {
            el.textContent = 'Firefly ' + Number(fps.producing).toFixed(0) + ' FPS / GlowWorm ' + Number(fps.consuming).toFixed(0) + ' FPS';
        }
    }).catch(function () {
        var el = document.getElementById('fpsCounter');
        if (el) {
            el.textContent = '';
        }
    });
}

$(function () {
    var br = '<br class="d-sm-none">';
    $('#subtitle').html('Bias Lighting and Ambient Light software' + br + ' designed for ' + br + 'Glow Worm Luciferin firmware');
    fetchJson('getFieldOptions').then(function (opts) {
        fieldOptions = opts || {};
        buildForm();
        initColorPicker();
        wireLivePreviewButton();
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
    pollFps();
    setInterval(pollFps, 1000);
});
