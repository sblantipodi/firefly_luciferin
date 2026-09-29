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
    var payload;
    try {
        payload = collectPayload();
    } catch (e) {
        showToast((state.fieldLabels['web.collectError'] || 'Invalid value:') + ' ' + e.message, 'bg-danger text-white');
        console.error('collectPayload failed', e);
        return;
    }
    if (!confirm(state.fieldLabels['web.restartConfirm'] || 'Luciferin needs to restart to apply these settings. Proceed?')) {
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
            showToast(state.fieldLabels['web.settingsSaved'] || 'Settings saved', 'bg-success text-white');
        });
    }).catch(function (err) {
        showToast('Error: ' + err.message, 'bg-danger text-white');
        console.error('saveForm failed', err);
    });
}

// Sends the add or remove action for MQTT discovery entities and reports the result.
function runMqttDiscovery(action) {
    var button = document.getElementById(action === 'add' ? 'addButton' : 'removeButton');
    button.disabled = true;
    fetch('mqttDiscovery', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({action: action})
    }).then(function (response) {
        return response.text().then(function (body) {
            if (!response.ok) {
                throw new Error(body || response.statusText);
            }
            showToast(state.fieldLabels[action === 'add' ? 'mqttDiscoveryAdd' : 'mqttDiscoveryRemove'], 'bg-success text-white');
        });
    }).catch(function (error) {
        showToast('Error: ' + error.message, 'bg-danger text-white');
    }).finally(function () {
        button.disabled = false;
    });
}

// Enables MQTT discovery actions only while the saved broker connection is active.
function syncMqttDiscoveryButtons() {
    var enabled = document.getElementById('mqttEnable').checked && !!state.lastConfig.mqttEnable;
    document.getElementById('addButton').disabled = !enabled;
    document.getElementById('removeButton').disabled = !enabled;
}

// Shows the prebuilt board or custom SPI pins required by the selected Ethernet mode.
function syncImprovEthernetFields() {
    var mode = document.getElementById('improvEthernetMode').value;
    document.getElementById('field-improvEthernetBoard').hidden = mode !== 'ETH_PREBUILT';
    ['improvMi', 'improvMo', 'improvSck', 'improvCs'].forEach(function (id) {
        document.getElementById('field-' + id).hidden = mode !== 'ETH_CUSTOM_SPI';
    });
}

// Replaces an editable combo box's suggestions while preserving a manually entered value.
function updateImprovSuggestions(id, values) {
    var input = document.getElementById(id);
    var list = document.getElementById(id + 'List');
    list.replaceChildren();
    (values || []).forEach(function (value) {
        var option = document.createElement('option');
        option.value = value;
        list.appendChild(option);
    });
    if (!input.value && values && values.length && id === 'improvComPort') {
        input.value = values[0];
    }
}

// Refreshes Wi-Fi and serial port suggestions when the provisioning accordion opens.
function refreshProvisioningOptions() {
    fetchJson('provisioningOptions').then(function (options) {
        updateImprovSuggestions('improvSsid', options.ssids);
        updateImprovSuggestions('improvComPort', options.ports);
    }).catch(function (error) {
        showToast('Error: ' + error.message, 'bg-danger text-white');
    });
}

// Sends only provisioning values to the serial endpoint; these fields are not saved as configuration.
function runProvisioning() {
    var button = document.getElementById('improvProvisionButton');
    var value = function (id) {
        return document.getElementById(id).value;
    };
    var request = {
        ssid: value('improvSsid'), wifiPassword: value('improvWifiPwd'),
        deviceName: value('improvDeviceName'), comPort: value('improvComPort'),
        baudRate: value('improvBaudrate'), ethernetMode: value('improvEthernetMode'),
        ethernetBoard: value('improvEthernetBoard'), mi: value('improvMi'),
        mo: value('improvMo'), sck: value('improvSck'), cs: value('improvCs'),
        mqttEnabled: document.getElementById('mqttEnable').checked,
        mqttHost: value('mqttHost'), mqttPort: value('mqttPort'),
        mqttUser: value('mqttUser'), mqttPassword: value('mqttPwd')
    };
    button.disabled = true;
    fetch('provisionDevice', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(request)
    }).then(function (response) {
        return response.text().then(function (body) {
            if (!response.ok) {
                throw new Error(body || response.statusText);
            }
            showToast(state.fieldLabels.improvAction, 'bg-success text-white');
        });
    }).catch(function (error) {
        showToast('Error: ' + error.message, 'bg-danger text-white');
    }).finally(function () {
        button.disabled = false;
    });
}

// Installs the provisioning controls and their default values from the JavaFX dialog.
function wireProvisioning() {
    document.getElementById('improvEthernetMode').value = 'ETH_NO_ETH';
    document.getElementById('improvBaudrate').value = '115200';
    document.getElementById('improvEthernetBoard').selectedIndex = 0;
    updateImprovSuggestions('improvComPort', (state.fieldOptions.improvComPort || {}).options?.map(function (option) {
        return option.value;
    }) || []);
    document.getElementById('improvEthernetMode').addEventListener('change', syncImprovEthernetFields);
    document.getElementById('improvProvisionButton').addEventListener('click', runProvisioning);
    document.getElementById('sub-network-provisioning').addEventListener('shown.bs.collapse', refreshProvisioningOptions);
    syncImprovEthernetFields();
}

// Applies server-provided translations to fixed controls outside the generated form.
function localizeSettingsPage() {
    var fields = [
        ['saveSettings', 'saveSettings', 'textContent'],
        ['showLivePreview', 'showPreview', 'textContent'],
        ['newProfileName', 'newProfileName', 'placeholder'],
        ['addProfile', 'addProfile', 'textContent'],
        ['appLog', 'openLog', 'textContent']
    ];
    fields.forEach(function (entry) {
        var label = state.fieldLabels['web.' + entry[1]];
        if (label) {
            document.getElementById(entry[0])[entry[2]] = label;
        }
    });
    var profileHelp = document.querySelector('#section-profiles .form-text');
    if (profileHelp && state.fieldLabels['web.profileHelp']) {
        profileHelp.textContent = state.fieldLabels['web.profileHelp'];
    }
}

// Notifies the server (comboChange endpoint) whenever a select control or the LED toggle changes, so dependent field options can be refreshed.
function wireSelectChangeListeners() {
    document.querySelectorAll('select').forEach(function (el) {
        el.addEventListener('change', function () {
            if (!el.id.startsWith('improv')) {
                notifyComboChange(el.id, el.value);
            }
        });
    });
    var toggleLed = document.getElementById('toggleLed');
    if (toggleLed) {
        toggleLed.addEventListener('change', function () {
            notifyComboChange('toggleLed', toggleLed.checked);
        });
    }
}

function revealSettingsPage() {
    document.getElementById('settingsContainer').classList.add('page-ready');
}

// Poll only while the log accordion is open; each response contains at most 1000 lines.
function wireLogAccordion() {
    var panel = document.getElementById('section-log');
    var output = document.getElementById('appLog');
    var timer;
    var loading = false;

    function refreshLog() {
        if (loading) {
            return;
        }
        loading = true;
        fetch('log', {cache: 'no-store'}).then(function (response) {
            if (response.status === 404) {
                return 'Log file not found.';
            }
            if (!response.ok) {
                throw new Error(response.statusText);
            }
            return response.text();
        }).then(function (contents) {
            if (panel.classList.contains('show')) {
                output.textContent = contents || 'Log file is empty.';
                output.scrollTop = output.scrollHeight;
            }
        }).catch(function (error) {
            if (panel.classList.contains('show')) {
                output.textContent = 'Unable to load log: ' + error.message;
            }
        }).finally(function () {
            loading = false;
        });
    }

    panel.addEventListener('shown.bs.collapse', function () {
        refreshLog();
        timer = setInterval(refreshLog, 3000);
    });
    panel.addEventListener('hidden.bs.collapse', function () {
        clearInterval(timer);
    });
}

// Match the firmware's seasonal snow: December 14 through January 6.
function showChristmasSnow() {
    var now = new Date();
    var month = now.getMonth();
    var day = now.getDate();
    if (!((month === 11 && day >= 14) || (month === 0 && day <= 6)) ||
        window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        return;
    }

    var snow = document.createElement('div');
    snow.id = 'snow';
    snow.setAttribute('aria-hidden', 'true');
    document.body.appendChild(snow);

    var flakesCount = window.innerWidth >= 1200 ? 40 :
        window.innerWidth >= 992 ? 30 :
            window.innerWidth >= 768 ? 24 : 16;
    var svgNamespace = 'http://www.w3.org/2000/svg';
    var arms = [
        'M12 12V2 M12 5L9.5 3 M12 5L14.5 3',
        'M12 12V2 M12 8L9.5 6 M12 8L14.5 6 M12 5L10 3.5 M12 5L14 3.5',
        'M12 12V2 M12 6L9 3.5 M12 6L15 3.5 M12 3.5L10.5 2.5 M12 3.5L13.5 2.5'
    ];
    for (var i = 0; i < flakesCount; i++) {
        var flake = document.createElement('div');
        var depth = Math.random();
        var layer = depth < 0.45 ? 'far' : depth < 0.85 ? 'middle' : 'near';
        var duration = layer === 'far' ? 18 + Math.random() * 9 :
            layer === 'middle' ? 12 + Math.random() * 7 : 8 + Math.random() * 5;
        var size = layer === 'far' ? 9 + Math.random() * 7 :
            layer === 'middle' ? 15 + Math.random() * 8 : 23 + Math.random() * 9;
        flake.className = 'snowflake snowflake--' + layer;
        flake.style.left = Math.random() * 100 + 'vw';
        flake.style.opacity = (layer === 'far' ? 0.3 : layer === 'middle' ? 0.55 : 0.75) + Math.random() * 0.2;
        flake.style.setProperty('--drift', Math.random() * 160 - 80 + 'px');
        flake.style.setProperty('--turn', Math.random() * 120 - 60 + 'deg');
        flake.style.animationDuration = duration + 's';
        flake.style.animationDelay = -Math.random() * duration + 's';

        var shape = document.createElementNS(svgNamespace, 'svg');
        shape.setAttribute('class', 'snowflake-shape');
        shape.setAttribute('viewBox', '0 0 24 24');
        shape.style.width = size + 'px';
        shape.style.height = size + 'px';
        shape.style.setProperty('--sway', 5 + Math.random() * 13 + 'px');
        shape.style.animationDuration = 2 + Math.random() * 3 + 's';
        shape.style.animationDelay = -Math.random() * 5 + 's';
        var arm = arms[Math.floor(Math.random() * arms.length)];
        for (var spoke = 0; spoke < 6; spoke++) {
            var path = document.createElementNS(svgNamespace, 'path');
            path.setAttribute('d', arm);
            path.setAttribute('transform', 'rotate(' + spoke * 60 + ' 12 12)');
            shape.appendChild(path);
        }
        flake.appendChild(shape);
        snow.appendChild(flake);
    }
}

$(function () {
    showChristmasSnow();
    fetchJson('sectionTitles').then(function (titles) {
        state.sectionTitles = titles || {};
    }).catch(function () {
    }).then(function () {
        return fetchJson('getFieldOptions');
    }).then(function (data) {
        state.fieldOptions = (data && data.options) || {};
        state.fieldLabels = (data && data.labels) || {};
        buildForm();
        localizeSettingsPage();
        wireLogAccordion();
        document.getElementById('saveSettings').addEventListener('click', saveForm);
        document.getElementById('addButton').addEventListener('click', function () {
            runMqttDiscovery('add');
        });
        document.getElementById('removeButton').addEventListener('click', function () {
            runMqttDiscovery('remove');
        });
        document.getElementById('mqttEnable').addEventListener('change', syncMqttDiscoveryButtons);
        syncMqttDiscoveryButtons();
        wireProvisioning();
        document.getElementById('addProfile').addEventListener('click', addProfile);
        initColorPicker();
        wireLivePreviewButton();
        wireSelectChangeListeners();
        revealSettingsPage();
        return fetchJson('getConfig');
    }).then(function (cfg) {
        state.lastConfig = cfg || {};
        fillForm(cfg);
        document.getElementById('improvDeviceName').value = cfg.outputDevice || '';
        syncMqttDiscoveryButtons();
        applyAutoOutputDevice();
        var profile = cfg && cfg.activeProfile;
        var profileEl = document.getElementById('activeProfile');
        if (profileEl) {
            profileEl.textContent = profile ? ((state.fieldLabels['web.profilePrefix'] || 'Profile:') + ' ' + profile) : '';
        }
        syncDeviceFromPrefs();
        return fetchJson('listProfiles');
    }).then(function (profilesData) {
        renderProfiles(profilesData);
    }).catch(function (err) {
        revealSettingsPage();
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
