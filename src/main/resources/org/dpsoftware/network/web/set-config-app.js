// Entry point of the LUCIFERIN web configuration page: wires together the form, device picker, profiles, live preview and status polling
// modules, and handles loading/saving the configuration against the ConfigServer HTTP endpoints.
import {state} from './set-config-state.js';
import {fetchJson, notifyComboChange} from './set-config-api.js';
import {buildForm, collectPayload, fillForm} from './set-config-core.js';
import {applyAutoOutputDevice, initColorPicker, refreshDevices, syncDeviceFromPrefs} from './set-config-device.js';
import {addProfile, renderProfiles} from './set-config-profiles.js';
import {wireLivePreviewButton} from './set-config-preview.js';
import {pollServerStatus} from './set-config-status.js';
import {showToast} from './set-config-ui.js';

// Collects the form payload and POSTs it to 'setConfig'; on success the server restarts to apply the settings.
function saveForm() {
    if (!confirm('Luciferin needs to restart to apply these settings. Proceed?')) {
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
    console.log('saveForm POST setConfig, body length', body.length);
    fetch('setConfig', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: body
    }).then(function (r) {
        return r.text().then(function (t) {
            if (!r.ok) {
                throw new Error(t || r.statusText);
            }
            showToast('Settings saved', 'bg-success text-white');
        });
    }).catch(function (err) {
        showToast('Error: ' + err.message, 'bg-danger text-white');
        console.error('saveForm failed', err);
    });
}

// Notifies the server (comboChange endpoint) whenever a select control or the LED toggle changes, so dependent field options can be refreshed.
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

$(function () {
    var br = '<br class="d-sm-none">';
    $('#subtitle').html('Bias Lighting and Ambient Light software' + br + ' designed for ' + br + 'Glow Worm Luciferin firmware');
    fetchJson('sectionTitles').then(function (titles) {
        state.sectionTitles = titles || {};
    }).catch(function () {
    }).then(function () {
        return fetchJson('getFieldOptions');
    }).then(function (data) {
        state.fieldOptions = (data && data.options) || {};
        state.fieldLabels = (data && data.labels) || {};
        buildForm();
        document.getElementById('saveSettings').addEventListener('click', saveForm);
        document.getElementById('addProfile').addEventListener('click', addProfile);
        initColorPicker();
        wireLivePreviewButton();
        wireSelectChangeListeners();
        return fetchJson('getConfig');
    }).then(function (cfg) {
        state.lastConfig = cfg || {};
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
    pollServerStatus();
    setInterval(pollServerStatus, 1000);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) {
            // Replace any request left pending while the mobile tab was suspended.
            pollServerStatus(true);
        }
    });
});
