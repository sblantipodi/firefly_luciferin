// Device controls of the settings page: color picker, output device/serial selection, LED toggle, live device table and per-device status polling.
import {state} from './set-config-state.js';
import {fetchJson} from './set-config-api.js';
import {escapeHtml, showToast} from './set-config-ui.js';

var colorPicker;
var deviceIp = null;
var deviceState = {on: true, whitetemp: 65};
export var lastColor = {r: 255, g: 38, b: 0};
var pollTimer = null;
var outputDeviceTouched = false;

var DEVICE_COLUMNS = [
    {key: 'deviceName', label: 'Name'},
    {key: 'deviceIP', label: 'IP/SERIAL'},
    {key: 'deviceBoard', label: 'Board'},
    {key: 'deviceVersion', label: 'Ver'},
    {key: 'mac', label: 'MAC'},
    {key: 'gpio', label: 'LED GPIO'},
    {key: 'gpioClock', label: 'CLK GPIO'},
    {key: 'ledBuiltin', label: 'Builtin GPIO'},
    {key: 'numberOfLEDSconnected', label: 'LEDs #'},
    {key: 'firmwareType', label: 'Type'},
    {key: 'baudRate', label: 'Baud'},
    {key: 'mqttTopic', label: 'MQTT Topic'},
    {key: 'colorMode', label: 'Color'},
    {key: 'colorOrder', label: 'Order'},
    {key: 'ldrValue', label: 'LDR'},
    {key: 'ldrPin', label: 'LDR GPIO'},
    {key: 'relayPin', label: 'Relay GPIO'},
    {key: 'sbPin', label: 'Button GPIO'}
];

// Resolves the IP of the Glow Worm device to target, based on the configured output device (AUTO / static IP / device name) and the connected device list.
function resolveDeviceIp() {
    if (deviceIp) {
        return deviceIp;
    }
    var cfg = state.lastConfig || {};
    var out = cfg.outputDevice || null;
    var staticIp = cfg.staticGlowWormIp || null;
    var devices = state.devices || [];
    var match = null;
    var auto = out && String(out).toUpperCase() === 'AUTO';
    if (auto) {
        match = devices[0];
    } else if (staticIp && staticIp !== '-') {
        var ip = String(staticIp);
        match = devices.find(function (d) {
            return d.deviceIP && String(d.deviceIP) === ip;
        });
    } else if (out) {
        match = devices.find(function (d) {
            return (d.deviceName && d.deviceName === out) || (d.deviceIP && d.deviceIP === out);
        });
    }
    if (match && match.deviceIP) {
        deviceIp = match.deviceIP;
        console.log('resolveDeviceIp: resolved to', deviceIp, 'via', auto ? 'AUTO' : (staticIp && staticIp !== '-' ? 'staticGlowWormIp' : 'outputDevice'));
        return deviceIp;
    }
    console.log('resolveDeviceIp: no match. out=', out, 'staticGlowWormIp=', staticIp, 'devices=', devices.map(function (d) {
        return d.deviceName + '/' + d.deviceIP;
    }));
    return null;
}

// Populates the effect dropdown from the server-provided options and renders the initial LED toggle button state.
export function fillPickerControls() {
    var effectOpts = (state.fieldOptions && state.fieldOptions.effect) ? state.fieldOptions.effect.options : [];
    document.getElementById('effectSelect').innerHTML = effectOpts.map(function (o) {
        return '<option value="' + escapeHtml(o.value) + '">' + escapeHtml(o.label) + '</option>';
    }).join('');
    var toggle = document.getElementById('toggleLED');
    toggle.textContent = deviceState.on ? (state.fieldLabels.turnLedOff || 'Turn OFF') : (state.fieldLabels.turnLedOn || 'Turn ON');
    toggle.className = 'btn ' + (deviceState.on ? 'btn-primary' : 'btn-outline-primary') + ' w-100';
}

// Applies the device's live preferences (LED state, white temperature, color, effect) to the UI controls and local state.
function applyPrefs(prefs) {
    if (!prefs) {
        return;
    }
    setToggleUi(prefs.toggle === '1');
    if (prefs.whiteTemp != null && prefs.whiteTemp !== '') {
        deviceState.whitetemp = Number(prefs.whiteTemp);
    }
    if (prefs.cp && prefs.cp.length > 0) {
        var parts = prefs.cp.split(',');
        if (parts.length === 3) {
            lastColor = {r: Number(parts[0]), g: Number(parts[1]), b: Number(parts[2])};
            var picker = document.getElementById('picker');
            if (picker && colorPicker) {
                colorPicker.color.rgb = lastColor;
            }
            document.getElementById('effectSelect').value = prefs.effect;
        }
    }
}

// Fetches the device preferences for the resolved IP and syncs the UI, (re)scheduling the periodic poll afterwards.
export function syncDeviceFromPrefs() {
    var ip = resolveDeviceIp();
    if (!ip) {
        console.log('syncDeviceFromPrefs: no IP resolved, scheduling poll');
        schedulePoll();
        return;
    }
    console.log('syncDeviceFromPrefs: fetching devicePrefs for IP', ip);
    fetchJson('devicePrefs?ip=' + encodeURIComponent(ip)).then(function (prefs) {
        if (!(prefs && prefs.error)) {
            applyPrefs(prefs);
        }
        schedulePoll();
    }).catch(function () {
        schedulePoll();
    });
}

// (Re)starts the 5s interval that re-syncs device prefs and refreshes devices.
function schedulePoll() {
    if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
    }
    pollTimer = setInterval(function () {
        syncDeviceFromPrefs();
        refreshDevices();
    }, 5000);
}

// Updates the LED toggle button and the form checkbox to reflect the on/off state, keeping both in sync.
function setToggleUi(on) {
    deviceState.on = on;
    var toggle = document.getElementById('toggleLED');
    if (toggle) {
        toggle.textContent = on ? (state.fieldLabels.turnLedOff || 'Turn OFF') : (state.fieldLabels.turnLedOn || 'Turn ON');
        toggle.classList.toggle('btn-primary', on);
        toggle.classList.toggle('btn-outline-primary', !on);
        toggle.classList.toggle('active', on);
    }
    var formCheckbox = document.getElementById('toggleLed');
    if (formCheckbox) {
        formCheckbox.checked = on;
    }
}

// Sends a payload (state/color/whitetemp) directly to the Glow Worm device over its HTTP API (no-cors fetch, so the response is not readable).
function sendToDevice(payload, successMsg) {
    var ip = resolveDeviceIp();
    if (!ip) {
        showToast('No connected device matches the output device', 'bg-warning text-dark');
        return;
    }
    var url = 'http://' + ip + '/lights/glowwormluciferin/set?payload=' + encodeURIComponent(JSON.stringify(payload));
    fetch(url, {mode: 'no-cors'}).then(function () {
        showToast(successMsg || ('Sent to ' + ip), 'bg-success text-white');
    }).catch(function (err) {
        showToast('Unable to reach device ' + ip + ': ' + err.message, 'bg-danger text-white');
    });
}

// Builds the device payload from the current UI state (LED on/off, color, white temp).
function buildPayload() {
    return {
        state: deviceState.on ? 'ON' : 'OFF',
        color: lastColor,
        whitetemp: deviceState.whitetemp
    };
}

// Initializes the iro color picker, the LED toggle button and the output device select; color changes are pushed to the device immediately.
export function initColorPicker() {
    var pickerEl = document.getElementById('picker');
    if (!pickerEl || typeof iro === 'undefined') {
        return;
    }
    colorPicker = new iro.ColorPicker('#picker', {
        width: 288,
        color: '#0091ff'
    });
    colorPicker.on(['input:end'], function (color) {
        lastColor = color.rgb;
        sendToDevice(buildPayload(), 'Color sent to device');
        syncDeviceFromPrefs();
    });
    var toggle = document.getElementById('toggleLED');
    if (toggle) {
        toggle.onclick = function () {
            deviceState.on = !deviceState.on;
            setToggleUi(deviceState.on);
            sendToDevice(buildPayload(), 'State sent to device');
            syncDeviceFromPrefs();
        };
    }
    var outputDeviceEl = document.getElementById('outputDevice');
    if (outputDeviceEl && outputDeviceEl.tagName === 'SELECT') {
        outputDeviceEl.addEventListener('change', function () {
            outputDeviceTouched = true;
            deviceIp = null;
            resolveDeviceIp();
            syncDeviceFromPrefs();
        });
    }
}

// Refreshes the output device select options with the connected device names, preserving the user's choice or the configured value.
function renderOutputDeviceSuggestions() {
    var el = document.getElementById('outputDevice');
    if (!el || el.tagName !== 'SELECT') {
        return;
    }
    var devices = state.devices || [];
    var names = devices.map(function (d) {
        return d.deviceName;
    }).filter(Boolean);
    var preserved = outputDeviceTouched ? el.value : null;
    var current = preserved || (state.lastConfig && state.lastConfig.outputDevice) || el.value;
    var html = '';
    names.forEach(function (name) {
        html += '<option value="' + escapeHtml(name) + '">' + escapeHtml(name) + '</option>';
    });
    if (current && names.indexOf(current) === -1) {
        html += '<option value="' + escapeHtml(current) + '">' + escapeHtml(current) + '</option>';
    }
    if (!html) {
        html = '<option value="">--</option>';
    }
    el.innerHTML = html;
    if (current) {
        el.value = current;
    }
}

// Fetches the list of connected devices from the server, renders the device table and updates the auto output device resolution.
export function refreshDevices() {
    fetchJson('getDevices').then(function (devices) {
        renderDevices(devices);
        applyAutoOutputDevice();
    }).catch(function (err) {
        var el = document.getElementById('devicesTable');
        if (el) {
            el.innerHTML = '<span class="text-danger">Unable to load devices: ' + escapeHtml(err.message) + '</span>';
        }
    });
}

// Renders the connected devices into the table (DEVICE_COLUMNS) and the output device select, storing the list in the shared state.
function renderDevices(devices) {
    state.devices = Array.isArray(devices) ? devices : [];
    renderOutputDeviceSuggestions();
    var el = document.getElementById('devicesTable');
    if (!el) {
        return;
    }
    if (!Array.isArray(devices) || devices.length === 0) {
        el.innerHTML = '<span class="text-muted">No connected devices</span>';
        return;
    }
    var html = '<table class="table table-sm table-striped align-middle"><thead><tr>';
    DEVICE_COLUMNS.forEach(function (c) {
        html += '<th>' + c.label + '</th>';
    });
    html += '</tr></thead><tbody>';
    var ipRe = /^(\d{1,3}\.){3}\d{1,3}$/;
    devices.forEach(function (d) {
        html += '<tr>';
        DEVICE_COLUMNS.forEach(function (c) {
            var v = d[c.key];
            var isBool = (typeof v === 'boolean');
            var text;
            if (isBool) {
                text = v ? '✔' : '';
            } else if (v == null || v === '') {
                text = '—';
            } else if (c.key === 'deviceIP' && ipRe.test(String(v))) {
                var ip = escapeHtml(String(v));
                text = '<a href="http://' + ip + '" target="_blank" rel="noopener">' + ip + '</a>';
            } else {
                text = escapeHtml(v);
            }
            html += '<td>' + text + '</td>';
        });
        html += '</tr>';
    });
    html += '</tbody></table>';
    el.innerHTML = html;
}

// Selects the matching device name in the output device select based on the configured AUTO/static IP, unless the user manually touched the select.
export function applyAutoOutputDevice() {
    if (outputDeviceTouched) {
        deviceIp = null;
        resolveDeviceIp();
        return;
    }
    var cfg = state.lastConfig;
    if (!cfg) {
        return;
    }
    var devices = state.devices || [];
    var match = null;
    var out = cfg.outputDevice;
    if (out && String(out).toUpperCase() === 'AUTO') {
        match = devices[0];
    } else if (cfg.staticGlowWormIp) {
        var ip = String(cfg.staticGlowWormIp);
        match = devices.find(function (d) {
            return d.deviceIP && String(d.deviceIP) === ip;
        });
    }
    if (!match || !match.deviceName) {
        return;
    }
    var el = document.getElementById('outputDevice');
    if (el) {
        el.value = match.deviceName;
    }
    deviceIp = null;
    resolveDeviceIp();
    syncDeviceFromPrefs();
}
