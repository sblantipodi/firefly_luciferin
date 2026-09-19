var deviceIp = null;
var deviceState = {on: true, whitetemp: 65, brightness: 255};
var lastColor = {r: 255, g: 38, b: 0};
var deviceReachable = false;
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

function resolveDeviceIp() {
    if (deviceIp) {
        return deviceIp;
    }
    var cfg = window.__lastConfig || {};
    var out = cfg.outputDevice || null;
    var staticIp = cfg.staticGlowWormIp || null;
    var devices = window.__devices || [];
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

function buildPickerHtml() {
    var effectOpts = (fieldOptions && fieldOptions.effect) ? fieldOptions.effect.options : [];
    var opts = effectOpts.map(function (o) {
        return '<option value="' + escapeHtml(o.value) + '">' + escapeHtml(o.label) + '</option>';
    }).join('');
    var toggleLabel = deviceState.on ? (fieldLabels.turnLedOff || 'Turn OFF') : (fieldLabels.turnLedOn || 'Turn ON');
    var toggleClass = deviceState.on ? 'btn-primary' : 'btn-outline-primary';
    return '<div class="row mb-3 align-items-start justify-content-center"><div class="col-12 col-sm-8 col-md-6 col-lg-4 text-center"><div id="picker"></div>' +
        '<div class="form-group mt-2"><select id="effectSelect" class="form-select w-100">' + opts + '</select></div>' +
        '<div class="form-group mt-2"><button id="toggleLED" type="button" class="btn ' + toggleClass + ' w-100">' + toggleLabel + '</button></div>' +
        '<div id="activeProfile" class="text-center text-muted small mt-1"></div>' +
        '</div></div></div>';
}

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
            if (picker && window.__colorPicker) {
                window.__colorPicker.color.rgb = lastColor;
            }
            document.getElementById('effectSelect').value = prefs.effect;
        }
    }
}

function syncDeviceFromPrefs() {
    var ip = resolveDeviceIp();
    if (!ip) {
        console.log('syncDeviceFromPrefs: no IP resolved, scheduling poll');
        deviceReachable = false;
        schedulePoll();
        return;
    }
    console.log('syncDeviceFromPrefs: fetching devicePrefs for IP', ip);
    fetchJson('devicePrefs?ip=' + encodeURIComponent(ip)).then(function (prefs) {
        deviceReachable = true;
        if (prefs && prefs.error) {
            deviceReachable = false;
        } else {
            applyPrefs(prefs);
        }
        schedulePoll();
    }).catch(function () {
        deviceReachable = false;
        schedulePoll();
    });
}

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

function setToggleUi(on) {
    deviceState.on = on;
    var toggle = document.getElementById('toggleLED');
    if (toggle) {
        toggle.textContent = on ? (fieldLabels.turnLedOff || 'Turn OFF') : (fieldLabels.turnLedOn || 'Turn ON');
        toggle.classList.toggle('btn-primary', on);
        toggle.classList.toggle('btn-outline-primary', !on);
        toggle.classList.toggle('active', on);
    }
    var formCheckbox = document.getElementById('toggleLed');
    if (formCheckbox) {
        formCheckbox.checked = on;
    }
}

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

function buildPayload() {
    return {
        state: deviceState.on ? 'ON' : 'OFF',
        color: lastColor,
        whitetemp: deviceState.whitetemp
    };
}

function initColorPicker() {
    var pickerEl = document.getElementById('picker');
    if (!pickerEl || typeof iro === 'undefined') {
        return;
    }
    var colorPicker = new iro.ColorPicker('#picker', {
        width: 288,
        color: '#0091ff'
    });
    window.__colorPicker = colorPicker;
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

function renderOutputDeviceSuggestions() {
    var el = document.getElementById('outputDevice');
    if (!el || el.tagName !== 'SELECT') {
        return;
    }
    var devices = window.__devices || [];
    var names = devices.map(function (d) {
        return d.deviceName;
    }).filter(Boolean);
    var preserved = outputDeviceTouched ? el.value : null;
    var current = preserved || (window.__lastConfig && window.__lastConfig.outputDevice) || el.value;
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

function refreshDevices() {
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

function renderDevices(devices) {
    window.__devices = Array.isArray(devices) ? devices : [];
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

function applyAutoOutputDevice() {
    if (outputDeviceTouched) {
        deviceIp = null;
        resolveDeviceIp();
        return;
    }
    var cfg = window.__lastConfig;
    if (!cfg) {
        return;
    }
    var devices = window.__devices || [];
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
