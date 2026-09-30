// Device controls of the settings page: color picker, output device/serial selection, LED toggle, live device table and per-device status polling.
import {state} from './set-config-state.js';
import {fetchJson} from './set-config-api.js';
import {escapeHtml, showToast} from './set-config-ui.js';

var colorPicker;
var deviceIp = null;
var checkedDeviceIp = null;
var deviceState = {on: true, whitetemp: 65};
export var lastColor = {r: 255, g: 38, b: 0};
var pollTimer = null;
var outputDeviceTouched = false;
var colorInputActive = false;
var colorPendingUntil = 0;
var satelliteDraft = null;

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

// Shows whether the selected IP answered devicePrefs and makes the dot link to that IP.
function updateDeviceReachability(ip, reachable) {
    var indicator = document.getElementById('deviceReachability');
    if (!indicator) {
        return;
    }
    var validIp = typeof ip === 'string' && /^(\d{1,3}\.){3}\d{1,3}$/.test(ip)
        && ip.split('.').every(function (part) {
            return Number(part) <= 255;
        });
    var address = validIp ? ip : null;
    var status = reachable ? (state.fieldLabels['web.device.reachable'] || 'Device reachable')
        : (state.fieldLabels['web.device.unreachable'] || 'Device unreachable');
    var description = address ? status + ': ' + address : status;
    indicator.classList.toggle('is-reachable', !!address && reachable);
    indicator.setAttribute('aria-label', description);
    indicator.title = description;
    if (address) {
        indicator.href = 'http://' + address + '/';
        indicator.removeAttribute('aria-disabled');
        indicator.removeAttribute('tabindex');
    } else {
        indicator.removeAttribute('href');
        indicator.setAttribute('aria-disabled', 'true');
        indicator.tabIndex = -1;
    }
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
            // A prefs request can return the previous color while the new one is still reaching the device.
            if (!colorInputActive && Date.now() >= colorPendingUntil) {
                lastColor = {r: Number(parts[0]), g: Number(parts[1]), b: Number(parts[2])};
                var picker = document.getElementById('picker');
                if (picker && colorPicker) {
                    colorPicker.color.rgb = lastColor;
                }
            }
            document.getElementById('effectSelect').value = prefs.effect;
        }
    }
}

// Fetches the device preferences for the resolved IP and syncs the UI, (re)scheduling the periodic poll afterwards.
export function syncDeviceFromPrefs() {
    var ip = resolveDeviceIp();
    if (ip !== checkedDeviceIp) {
        checkedDeviceIp = ip;
        updateDeviceReachability(ip, false);
    }
    if (!ip) {
        updateDeviceReachability(null, false);
        console.log('syncDeviceFromPrefs: no IP resolved, scheduling poll');
        schedulePoll();
        return;
    }
    console.log('syncDeviceFromPrefs: fetching devicePrefs for IP', ip);
    fetchJson('devicePrefs?ip=' + encodeURIComponent(ip), {cache: 'no-store'}).then(function (prefs) {
        if (checkedDeviceIp === ip) {
            updateDeviceReachability(ip, !!prefs && !prefs.error);
        }
        if (checkedDeviceIp === ip && !(prefs && prefs.error)) {
            applyPrefs(prefs);
        }
        schedulePoll();
    }).catch(function () {
        if (checkedDeviceIp === ip) {
            updateDeviceReachability(ip, false);
        }
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

// Updates the device picker and Misc LED buttons to reflect the same on/off state.
function setToggleUi(on) {
    deviceState.on = on;
    var toggle = document.getElementById('toggleLED');
    if (toggle) {
        toggle.textContent = on ? (state.fieldLabels.turnLedOff || 'Turn OFF') : (state.fieldLabels.turnLedOn || 'Turn ON');
        toggle.classList.toggle('btn-primary', on);
        toggle.classList.toggle('btn-outline-primary', !on);
        toggle.classList.toggle('active', on);
    }
    var formButton = document.getElementById('toggleLed');
    if (formButton) {
        formButton.setAttribute('aria-pressed', String(on));
        formButton.textContent = on ? state.fieldLabels.turnLedOff : state.fieldLabels.turnLedOn;
        formButton.classList.toggle('btn-primary', on);
        formButton.classList.toggle('btn-outline-primary', !on);
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
    colorPicker.on('input:start', function () {
        colorInputActive = true;
    });
    colorPicker.on('input:change', function (color) {
        lastColor = color.rgb;
    });
    colorPicker.on(['input:end'], function (color) {
        colorInputActive = false;
        lastColor = color.rgb;
        colorPendingUntil = Date.now() + 7000;
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
            colorPendingUntil = 0;
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

// Refreshes the editable device selector from serial ports or discovered wireless devices.
export function refreshSerialPortSuggestions() {
    var input = document.getElementById('serialPort');
    if (!input) {
        return;
    }
    var wireless = document.getElementById('wirelessStream').checked;
    document.querySelector('label[for="serialPort"]').textContent = wireless
        ? state.fieldLabels.serialPortWirelessLabel : state.fieldLabels.serialPort;
    var key = wireless ? 'serialPortWireless' : 'serialPortSerial';
    var configured = ((state.fieldOptions[key] || {}).options || []).map(function (option) {
        return String(option.value);
    });
    var connected = wireless ? (state.devices || []).map(function (device) {
        return device.deviceName;
    }).filter(Boolean) : [];
    var multiMonitor = Number(document.getElementById('multiMonitor').value);
    var choices = Array.from(new Set(configured.concat(connected))).filter(function (choice) {
        return multiMonitor === 1 || choice !== 'AUTO';
    });
    document.getElementById('serialPortList').innerHTML = choices.map(function (choice) {
        return '<option value="' + escapeHtml(choice) + '"></option>';
    }).join('');
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
    refreshSerialPortSuggestions();
    refreshSatelliteDeviceSuggestions();
    refreshLdrLabel();
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
                text = '<a class="orange-link" href="http://' + ip + '" target="_blank" rel="noopener">' + ip + '</a>';
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

// Shows the current room brightness reported by the configured device in the LDR readout.
export function refreshLdrLabel() {
    var label = document.getElementById('ldrLabel');
    if (!label) {
        return;
    }
    var config = state.lastConfig || {};
    var devices = state.devices || [];
    var selected = devices.find(function (device) {
        return device.deviceIP === config.staticGlowWormIp || device.deviceIP === config.outputDevice
            || device.deviceName === config.outputDevice;
    }) || (config.outputDevice === 'AUTO' ? devices[0] : null);
    label.textContent = selected && selected.ldrValue ? selected.ldrValue : '-';
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

// Builds the satellite table and editable controls; the page's Save settings button persists them.
export function buildSatellitesHtml() {
    var labels = state.fieldLabels;
    var options = function (id) {
        return ((state.fieldOptions[id] || {}).options || []).map(function (choice) {
            return '<option value="' + escapeHtml(choice.value) + '">' + escapeHtml(choice.label) + '</option>';
        }).join('');
    };
    return '<div class="form-group"><div id="satelliteTable" class="table-responsive mb-3"></div>'
        + '<div class="row g-3">'
        + '<div class="col-12 col-md-6"><label for="satelliteDeviceIp">' + escapeHtml(labels.satelliteDeviceIp) + '</label><input class="form-control" id="satelliteDeviceIp" list="satelliteDeviceIpList"><datalist id="satelliteDeviceIpList">' + options('satelliteDeviceIp') + '</datalist></div>'
        + '<div class="col-12 col-md-6"><label for="satelliteZone">' + escapeHtml(labels.satelliteZone) + '</label><select class="form-select" id="satelliteZone">' + options('satelliteZone') + '</select></div>'
        + '<div class="col-12 col-md-6"><label for="satelliteOrientation">' + escapeHtml(labels.satelliteOrientation) + '</label><select class="form-select" id="satelliteOrientation">' + options('satelliteOrientation') + '</select></div>'
        + '<div class="col-12 col-md-6"><label for="satelliteLedNum">' + escapeHtml(labels.satelliteLedNum) + '</label><input class="form-control" id="satelliteLedNum" type="text" inputmode="numeric" pattern="[0-9]+" value="1"></div>'
        + '<div class="col-12 col-md-6"><label for="satelliteAlgo">' + escapeHtml(labels.satelliteAlgo) + '</label><select class="form-select" id="satelliteAlgo">' + options('satelliteAlgo') + '</select></div>'
        + '<div class="col-12 col-md-6"><label class="d-block" for="satelliteAdd">' + escapeHtml(labels.satelliteAdd) + '</label><button type="button" class="btn btn-success" id="satelliteAdd" title="' + escapeHtml(labels.satelliteAddTooltip) + '" aria-label="' + escapeHtml(labels.satelliteAdd) + '">➕</button></div>'
        + '</div></div>';
}

// Returns the localized caption for a stored satellite zone, direction or algorithm.
function satelliteChoiceLabel(id, value) {
    var match = ((state.fieldOptions[id] || {}).options || []).find(function (choice) {
        return choice.value === value;
    });
    return match ? match.label : value;
}

// Renders the editable satellite rows, including the remove button that selects a row for editing.
function renderSatelliteTable() {
    var table = document.getElementById('satelliteTable');
    if (!table || satelliteDraft === null) {
        return;
    }
    var labels = state.fieldLabels;
    var rows = Object.values(satelliteDraft);
    var html = '<table class="table table-sm table-striped align-middle"><thead><tr><th></th><th>'
        + escapeHtml(labels.satelliteTableIp) + '</th><th>' + escapeHtml(labels.satelliteTableZone)
        + '</th><th>' + escapeHtml(labels.satelliteLedNum) + '</th><th>'
        + escapeHtml(labels.satelliteTableAlgo) + '</th><th>'
        + escapeHtml(labels.satelliteTableOrientation) + '</th></tr></thead><tbody>';
    rows.forEach(function (row) {
        var ip = escapeHtml(row.deviceIp);
        html += '<tr><td><button type="button" class="btn btn-sm btn-outline-danger satellite-remove" data-ip="' + ip
            + '" title="' + escapeHtml(labels.satelliteRemove) + '" aria-label="' + escapeHtml(labels.satelliteRemove) + '"'
            + (state.lastConfig && state.lastConfig.fullFirmware ? '' : ' disabled') + '>×</button></td>'
            + '<td><a href="http://' + ip + '" target="_blank" rel="noopener">' + ip + '</a></td>'
            + '<td>' + escapeHtml(satelliteChoiceLabel('satelliteZone', row.zone)) + '</td><td>' + escapeHtml(row.ledNum)
            + '</td><td>' + escapeHtml(satelliteChoiceLabel('satelliteAlgo', row.algo)) + '</td><td>'
            + escapeHtml(satelliteChoiceLabel('satelliteOrientation', row.orientation)) + '</td></tr>';
    });
    table.innerHTML = html + '</tbody></table>';
}

// Refreshes discovered satellite IP suggestions while excluding the main device and assigned rows.
export function refreshSatelliteDeviceSuggestions() {
    var list = document.getElementById('satelliteDeviceIpList');
    if (!list || satelliteDraft === null) {
        return;
    }
    var config = state.lastConfig || {};
    var available = ((state.fieldOptions.satelliteDeviceIp || {}).options || []).concat(
        (state.devices || []).map(function (device) {
            return {
                value: device.deviceIP,
                label: device.deviceName + ' (' + device.deviceIP + ')',
                deviceName: device.deviceName
            };
        }));
    var seen = new Set();
    list.innerHTML = available.filter(function (choice) {
        var ip = String(choice.value || '');
        if (!ip || seen.has(ip) || satelliteDraft[ip] || ip === config.staticGlowWormIp
            || choice.deviceName === config.outputDevice) {
            return false;
        }
        seen.add(ip);
        return true;
    }).map(function (choice) {
        return '<option value="' + escapeHtml(choice.label) + '"></option>';
    }).join('');
}

// Loads saved satellites into the editable draft and restores the dialog's default field values.
export function initializeSatellites(config) {
    satelliteDraft = {};
    Object.values((config && config.satellites) || {}).forEach(function (satellite) {
        satelliteDraft[satellite.deviceIp] = {...satellite};
    });
    document.getElementById('satelliteDeviceIp').value = '';
    document.getElementById('satelliteZone').value = state.fieldLabels.satelliteDefaultZone;
    document.getElementById('satelliteOrientation').selectedIndex = 0;
    document.getElementById('satelliteAlgo').selectedIndex = 0;
    document.getElementById('satelliteLedNum').value = '1';
    ['satelliteDeviceIp', 'satelliteZone', 'satelliteOrientation', 'satelliteLedNum', 'satelliteAlgo',
        'satelliteAdd'].forEach(function (id) {
        document.getElementById(id).disabled = !config.fullFirmware;
    });
    renderSatelliteTable();
    refreshSatelliteDeviceSuggestions();
}

// Returns the current satellite draft for the common Save settings request.
export function collectSatellites() {
    if (satelliteDraft === null) {
        throw new Error('Satellite settings are not loaded');
    }
    return {...satelliteDraft};
}

// Wires add and remove actions of the satellite manager.
export function wireSatellites() {
    var input = document.getElementById('satelliteDeviceIp');
    var count = document.getElementById('satelliteLedNum');
    count.addEventListener('input', function () {
        count.value = count.value.replace(/[^0-9]/g, '');
    });
    document.getElementById('satelliteAdd').addEventListener('click', function () {
        var selected = input.value.trim();
        var ip = selected.includes('(') ? selected.slice(selected.lastIndexOf('(') + 1, selected.lastIndexOf(')')).trim() : selected;
        var octets = ip.split('.');
        if (octets.length !== 4 || octets.some(function (octet) {
            return !/^\d{1,3}$/.test(octet) || Number(octet) > 255;
        })) {
            showToast(state.fieldLabels.satelliteIpError, 'bg-danger text-white');
            return;
        }
        if (!/^\d+$/.test(count.value)) {
            showToast(state.fieldLabels['web.wholeNumber'], 'bg-danger text-white');
            return;
        }
        var prior = satelliteDraft[ip] || {};
        satelliteDraft[ip] = {
            zone: document.getElementById('satelliteZone').value,
            orientation: document.getElementById('satelliteOrientation').value,
            ledNum: String(Math.max(1, Number(count.value))),
            deviceIp: ip,
            deviceName: prior.deviceName || '',
            algo: document.getElementById('satelliteAlgo').value
        };
        input.value = '';
        renderSatelliteTable();
        refreshSatelliteDeviceSuggestions();
    });
    document.getElementById('satelliteTable').addEventListener('click', function (event) {
        var button = event.target.closest('.satellite-remove');
        if (!button) {
            return;
        }
        var row = satelliteDraft[button.dataset.ip];
        delete satelliteDraft[button.dataset.ip];
        input.value = row.deviceIp;
        ['Zone', 'Orientation', 'Algo'].forEach(function (part) {
            document.getElementById('satellite' + part).value = row[part.toLowerCase()];
        });
        count.value = row.ledNum;
        renderSatelliteTable();
        refreshSatelliteDeviceSuggestions();
    });
}
