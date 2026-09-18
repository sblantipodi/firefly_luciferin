var fieldOptions = {};
var sections = [
    {
        id: 'leds', title: 'LEDs config', fields: [
            {id: 'topLed', label: 'Top Row LEDs', type: 'number', numeric: true, min: 0},
            {id: 'leftLed', label: 'Left Col LEDs', type: 'number', numeric: true, min: 0},
            {id: 'rightLed', label: 'Right Col LEDs', type: 'number', numeric: true, min: 0},
            {id: 'bottomLeftLed', label: 'Bottom Left LEDs', type: 'number', numeric: true, min: 0},
            {id: 'bottomRightLed', label: 'Bottom Right LEDs', type: 'number', numeric: true, min: 0},
            {id: 'bottomRowLed', label: 'Bottom Row LEDs', type: 'number', numeric: true, min: 0},
            {id: 'ledStartOffset', label: 'First LED offset', type: 'number', numeric: true, min: 0},
            {id: 'orientation', label: 'Orientation', type: 'select'},
            {id: 'groupBy', label: 'LED Groups', type: 'number', numeric: true, min: 0},
            {id: 'splitBottomMargin', label: 'Bottom Gap (%)', type: 'text', numeric: false},
            {id: 'grabberAreaTopBottom', label: 'Grab Area (Top/Bottom)', type: 'number', numeric: true, min: 0},
            {id: 'grabberSide', label: 'Grab Area (Sides)', type: 'number', numeric: true, min: 0},
            {id: 'gapTypeTopBottom', label: 'Margins (Top/Bottom)', type: 'text', numeric: false},
            {id: 'gapTypeSide', label: 'Margins (Sides)', type: 'text', numeric: false}
        ]
    },
    {
        id: 'mode', title: 'Mode', fields: [
            {id: 'outputDevice', label: 'Output device (serial port)', type: 'text', numeric: false},
            {id: 'baudRate', label: 'Baud rate', type: 'select'},
            {id: 'staticGlowWormIp', label: 'Glow Worm IP', type: 'text', numeric: false},
            {id: 'fullFirmware', label: 'Full firmware', type: 'checkbox', numeric: false},
            {id: 'desiredFramerate', label: 'Capture framerate (FPS)', type: 'select'},
            {id: 'smoothingType', label: 'Smoothing type', type: 'select'},
            {
                id: 'smoothingTargetFramerate',
                label: 'Smoothing target framerate',
                type: 'number',
                numeric: true,
                min: 0,
                max: 240
            },
            {
                id: 'frameInsertionTarget',
                label: 'Frame insertion target',
                type: 'number',
                numeric: true,
                min: 0,
                max: 120
            },
            {id: 'emaAlpha', label: 'EMA alpha', type: 'number', numeric: true, step: '0.05', min: 0, max: 1},
            {id: 'numberOfCPUThreads', label: '# of CPU threads', type: 'number', numeric: true, min: 1, max: 64},
            {id: 'simdAvx', label: 'CPU extensions (AVX)', type: 'select'},
            {id: 'resamplingFactor', label: 'Resampling factor (scaling/quality)', type: 'select'},
            {id: 'captureMethod', label: 'Capture method', type: 'text', numeric: false},
            {id: 'monitorNumber', label: 'Bind to display (monitor #)', type: 'number', numeric: true, min: 1, max: 8},
            {id: 'screenResX', label: 'Screen resolution X', type: 'number', numeric: true, min: 0},
            {id: 'screenResY', label: 'Screen resolution Y', type: 'number', numeric: true, min: 0},
            {id: 'osScaling', label: 'OS scaling (%)', type: 'number', numeric: true, min: 100, max: 500},
            {id: 'defaultLedMatrix', label: 'Aspect ratio (LED matrix)', type: 'select'},
            {id: 'autoDetectBlackBars', label: 'Auto detect black bars', type: 'checkbox', numeric: false},
            {id: 'algo', label: 'Algorithm', type: 'select'},
            {id: 'theme', label: 'Theme', type: 'select'},
            {id: 'language', label: 'Language', type: 'select'}
        ],
        subAccordions: [
            {
                id: 'display', title: 'Display', fields: [
                    {id: 'cubeLut', label: '3D LUT (color tone map)', type: 'select'}
                ]
            }
        ]
    },
    {
        id: 'network', title: 'Network (MQTT)', fields: [
            {id: 'mqttEnable', label: 'Enable MQTT', type: 'checkbox', numeric: false},
            {id: 'wirelessStream', label: 'Wireless stream', type: 'checkbox', numeric: false},
            {id: 'streamType', label: 'Stream type', type: 'select'},
            {id: 'mqttServer', label: 'MQTT server (protocol://host:port)', type: 'text', numeric: false},
            {id: 'mqttTopic', label: 'MQTT topic', type: 'text', numeric: false},
            {id: 'mqttDiscoveryTopic', label: 'MQTT discovery topic', type: 'text', numeric: false},
            {id: 'mqttUsername', label: 'MQTT username', type: 'text', numeric: false},
            {id: 'mqttPwd', label: 'MQTT password', type: 'text', numeric: false}
        ]
    },
    {
        id: 'misc', title: 'Misc', fields: [
            {id: 'effect', label: 'Effect', type: 'select'},
            {id: 'colorMode', label: 'Color Mode', type: 'select'},
            {id: 'gamma', label: 'Gamma', type: 'number', numeric: true, step: '0.1', min: 0, max: 4},
            {id: 'whiteTemperature', label: 'Color temperature', type: 'number', numeric: true, min: 0, max: 30000},
            {id: 'brightness', label: 'Brightness', type: 'number', numeric: true, min: 0, max: 100},
            {id: 'nightModeFrom', label: 'Night mode from (HH:mm)', type: 'text', numeric: false},
            {id: 'nightModeTo', label: 'Night mode to (HH:mm)', type: 'text', numeric: false},
            {id: 'nightModeBrightness', label: 'Night mode brightness', type: 'text', numeric: false},
            {id: 'toggleLed', label: 'Toggle LEDs', type: 'checkbox', numeric: false},
            {id: 'startWithSystem', label: 'Run at login', type: 'checkbox', numeric: false},
            {id: 'runtimeLogLevel', label: 'Runtime log level', type: 'text', numeric: false}
        ],
        subAccordions: [
            {
                id: 'colorCorr', title: 'Color correction', fields: [
                    {
                        id: 'ccInfo',
                        label: 'Hue/Saturation/Lightness per color channel',
                        type: 'note',
                        note: 'Exposed via the hueMap field, currently not available from the web API (excluded by the server). Manage it from the JavaFX interface.'
                    }
                ]
            },
            {
                id: 'eyeCare', title: 'Eye care', fields: [
                    {id: 'nightLight', label: 'Night light', type: 'select'},
                    {id: 'nightLightLvl', label: 'Night light level', type: 'number', numeric: true, min: 1, max: 100},
                    {id: 'luminosityThreshold', label: 'Luminosity threshold', type: 'number', numeric: true, min: 0},
                    {id: 'brightnessLimiter', label: 'Brightness limiter (%)', type: 'select'}
                ]
            },
            {
                id: 'gamma', title: 'Gamma', fields: [
                    {id: 'enableAutomaticGamma', label: 'Enable automatic gamma', type: 'checkbox', numeric: false},
                    {id: 'gammaLevel', label: 'Gamma level', type: 'select'}
                ]
            },
            {
                id: 'profile', title: 'Profile', fields: [
                    {id: 'checkFullScreen', label: 'Enable full screen detection', type: 'checkbox', numeric: false},
                    {id: 'gpuThreshold', label: 'GPU load threshold', type: 'number', numeric: true, min: 0, max: 100},
                    {id: 'cpuThreshold', label: 'CPU load threshold', type: 'number', numeric: true, min: 0, max: 100},
                    {
                        id: 'profileProcess1',
                        label: 'Process 1',
                        type: 'text',
                        numeric: false,
                        list: 'profileProcesses',
                        index: 0
                    },
                    {
                        id: 'profileProcess2',
                        label: 'Process 2',
                        type: 'text',
                        numeric: false,
                        list: 'profileProcesses',
                        index: 1
                    },
                    {
                        id: 'profileProcess3',
                        label: 'Process 3',
                        type: 'text',
                        numeric: false,
                        list: 'profileProcesses',
                        index: 2
                    }
                ]
            },
            {
                id: 'smoothing', title: 'Smoothing', fields: [
                    {
                        id: 'smoothingTargetFramerate',
                        label: 'Smoothing target framerate',
                        type: 'number',
                        numeric: true,
                        min: 0,
                        max: 240
                    },
                    {
                        id: 'smoothingNote',
                        label: '',
                        type: 'note',
                        note: 'EMA alpha, frame insertion and smoothing type are derived from the target and managed by the app.'
                    }
                ]
            }
        ]
    },
    {
        id: 'devices', title: 'Devices', fields: [
            {id: 'powerSaving', label: 'Power saving', type: 'select'},
            {id: 'multiMonitor', label: 'Multi monitor', type: 'select'},
            {id: 'multiScreenSingleDevice', label: 'Single device (multi screen)', type: 'checkbox', numeric: false},
            {id: 'checkForUpdates', label: 'Check for updates', type: 'checkbox', numeric: false},
            {id: 'syncCheck', label: 'Sync check', type: 'checkbox', numeric: false}
        ],
        subAccordions: [
            {
                id: 'connectedDevices', title: 'Connected devices', fields: [
                    {id: 'devicesToolbar', label: '', type: 'note', note: ''},
                    {id: 'devicesContent', label: '', type: 'note', note: 'Loading devices…'}
                ]
            },
            {
                id: 'satellites', title: 'Satellites', fields: [
                    {
                        id: 'satInfo',
                        label: 'Satellite devices',
                        type: 'note',
                        note: 'Managed via the satellites map, currently not available from the web API (excluded by the server). Manage it from the JavaFX interface.'
                    }
                ]
            }
        ]
    },
    {
        id: 'ldr', title: 'LDR', fields: [
            {id: 'enableLDR', label: 'Enable LDR', type: 'checkbox', numeric: false},
            {id: 'ldrInterval', label: 'LDR interval', type: 'number', numeric: true, min: 0},
            {id: 'ldrMin', label: 'LDR minimum brightness', type: 'number', numeric: true, min: 0},
            {id: 'ldrTurnOff', label: 'Turn off on LDR', type: 'checkbox', numeric: false}
        ]
    }
];

function fetchJson(url, options) {
    return fetch(url, options).then(function (r) {
        if (!r.ok) {
            return r.text().then(function (t) {
                throw new Error(t || r.statusText);
            });
        }
        return r.json();
    });
}

function optionsFor(f) {
    var src = (fieldOptions && fieldOptions[f.id]) ? fieldOptions[f.id].options : f.options;
    if (!src) {
        return [];
    }
    return src.map(function (o) {
        if (typeof o === 'object' && o !== null) {
            return {value: String(o.value), label: o.label != null ? String(o.label) : String(o.value)};
        }
        return {value: String(o), label: String(o)};
    });
}

function selectType(f) {
    return (fieldOptions && fieldOptions[f.id]) ? fieldOptions[f.id].type : (f.numeric ? 'number' : 'string');
}

function buildFieldHtml(f) {
    if (f.id === 'devicesToolbar') {
        return '<div class="d-flex justify-content-between align-items-center mb-2"><span class="text-muted small">Currently connected devices (read-only)</span> <button type="button" class="btn btn-sm btn-outline-secondary" onclick="refreshDevices()">Refresh</button></div>';
    }
    if (f.id === 'devicesContent') {
        return '<div id="devicesTable" class="table-responsive"></div>';
    }
    if (f.type === 'note') {
        return '<div class="form-text">' + f.note + '</div>';
    }
    if (f.type === 'checkbox') {
        return '<div class="form-check"><input type="checkbox" class="form-check-input" id="' + f.id + '"><label class="form-check-label" for="' + f.id + '">' + f.label + '</label></div>';
    }
    if (f.type === 'select') {
        var opts = optionsFor(f).map(function (o) {
            return '<option value="' + escapeHtml(o.value) + '">' + escapeHtml(o.label) + '</option>';
        }).join('');
        return '<div class="form-group"><label for="' + f.id + '">' + f.label + '</label> <select class="form-select" id="' + f.id + '">' + opts + '</select></div>';
    }
    var inputType = f.type === 'number' ? 'text' : f.type;
    return '<div class="form-group"><label for="' + f.id + '">' + f.label + '</label> <input type="' + inputType + '" class="form-control" id="' + f.id + '"' + (f.numeric ? ' inputmode="numeric"' : '') + '></div>';
}

function buildSubAccordionsHtml(section, subIdx) {
    if (!section.subAccordions || section.subAccordions.length === 0) {
        return '';
    }
    var html = '<div class="accordion mt-3" id="subAccordion-' + section.id + '">';
    section.subAccordions.forEach(function (sub) {
        html += '<div class="accordion-item">';
        html += '<h3 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#sub-' + section.id + '-' + sub.id + '" aria-expanded="false" aria-controls="sub-' + section.id + '-' + sub.id + '">' + sub.title + '</button></h3>';
        html += '<div id="sub-' + section.id + '-' + sub.id + '" class="accordion-collapse collapse" data-bs-parent="#subAccordion-' + section.id + '">';
        html += '<div class="accordion-body">';
        sub.fields.forEach(function (f) {
            html += buildFieldHtml(f);
        });
        html += '</div></div></div>';
    });
    html += '</div>';
    return html;
}

var deviceIp = null;
var deviceState = {on: true, effect: 'Solid', whitetemp: 65, brightness: 255};
var lastColor = {r: 255, g: 38, b: 0};

function resolveDeviceIp() {
    if (deviceIp) {
        return deviceIp;
    }
    var out = null;
    try {
        out = (window.__lastConfig && window.__lastConfig.outputDevice) || null;
    } catch (e) {
        out = null;
    }
    if (!out || !window.__devices || !window.__devices.length) {
        return null;
    }
    var match = window.__devices.find(function (d) {
        return (d.deviceName && out && d.deviceName === out) || (d.deviceIP && out && d.deviceIP === out);
    });
    if (match && match.deviceIP) {
        deviceIp = match.deviceIP;
        return deviceIp;
    }
    return null;
}

function buildPickerHtml() {
    var effects = ['Solid', 'Fire', 'Twinkle', 'Bpm', 'Rainbow', 'Slow rainbow', 'Chase rainbow', 'Solid rainbow', 'Random colors', 'Rainbow colors', 'Meteor', 'Color waterfall', 'Random marquee', 'Rainbow marquee', 'Pulsing rainbow', 'Christmas', 'Bias light', 'Music mode (VU Meter)', 'Music mode (Stereo VU Meter)', 'Music mode (Screen capture)', 'Music mode (Rainbow music)'];
    var opts = effects.map(function (e) {
        return '<option value="' + e + '"' + (deviceState.effect === e ? ' selected' : '') + '>' + e + '</option>';
    }).join('');
    var toggleLabel = deviceState.on ? 'Turn OFF' : 'Turn ON';
    var toggleClass = deviceState.on ? 'btn-primary' : 'btn-outline-primary';
    return '<div class="row mb-3 align-items-start justify-content-center"><div class="col-12 col-sm-8 col-md-6 col-lg-4 text-center"><div style="max-width: 288px; margin: 0 auto;"><div id="picker"></div></div>' +
        '<div class="form-group mt-2"><select id="effectSelect" class="form-select w-100">' + opts + '</select></div>' +
        '<div class="form-group mt-2"><button id="toggleLED" type="button" class="btn ' + toggleClass + ' w-100" style="color:#fff">' + toggleLabel + '</button></div>' +
        '<div id="activeProfile" class="text-center text-muted small mt-1"></div>' +
        '</div></div></div>';
}

var deviceReachable = false;
var pollTimer = null;

function applyPrefs(prefs) {
    if (!prefs) {
        return;
    }
    setToggleUi(prefs.toggle === '1');
    if (prefs.effect) {
        // Normalize the device technical state "GlowWormWifi" to the real effect value "Bias light".
        var effect = PICKER_EFFECT_TO_CONFIG[prefs.effect] || prefs.effect;
        deviceState.effect = effect;
        var sel = document.getElementById('effectSelect');
        if (sel) {
            var existing = Array.prototype.find.call(sel.options, function (o) {
                return o.value === effect;
            });
            if (!existing) {
                var opt = document.createElement('option');
                opt.value = effect;
                opt.textContent = effect;
                sel.appendChild(opt);
            }
            sel.value = effect;
        }
    }
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
        }
    }
}

function syncDeviceFromPrefs() {
    var ip = resolveDeviceIp();
    if (!ip) {
        deviceReachable = false;
        schedulePoll();
        return;
    }
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
    pollTimer = setInterval(syncDeviceFromPrefs, 5000);
}

function setToggleUi(on) {
    deviceState.on = on;
    var toggle = document.getElementById('toggleLED');
    if (!toggle) {
        return;
    }
    toggle.textContent = on ? 'Turn OFF' : 'Turn ON';
    toggle.classList.toggle('btn-primary', on);
    toggle.classList.toggle('btn-outline-primary', !on);
    if (on) {
        toggle.style.backgroundColor = 'orange';
        toggle.style.color = '#fff';
    } else {
        toggle.style.backgroundColor = 'lightgrey';
        // "Turn ON" label in white.
        toggle.style.color = '#fff';
    }
}

function sendToDevice(payload, successMsg) {
    var ip = resolveDeviceIp();
    if (!ip) {
        showToast('No connected device matches the output device', 'bg-warning text-dark');
        return;
    }
    var url = 'http://' + ip + '/lights/glowwormluciferin/set?payload=' + encodeURIComponent(JSON.stringify(payload));
    // no-cors: the firmware does not send CORS headers, a regular fetch would fail
    // even when the device processed the request. no-cors fires the request and
    // the device still applies the change; the response is opaque (unreadable).
    fetch(url, {mode: 'no-cors'}).then(function () {
        showToast(successMsg || ('Sent to ' + ip), 'bg-success text-white');
    }).catch(function (err) {
        showToast('Unable to reach device ' + ip + ': ' + err.message, 'bg-danger text-white');
    });
}

function buildPayload() {
    return {
        state: deviceState.on ? 'ON' : 'OFF',
        effect: deviceState.effect,
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
        deviceState.effect = 'Solid';
        var sel = document.getElementById('effectSelect');
        if (sel) {
            sel.value = 'Solid';
        }
        sendToDevice(buildPayload(), 'Color sent to device');
        syncDeviceFromPrefs();
    });
    var toggle = document.getElementById('toggleLED');
    if (toggle) {
        toggle.onclick = function () {
            // Optimistic flip for immediate feedback, the device's /prefs (synced right after)
            // corrects the UI to the real state.
            deviceState.on = !deviceState.on;
            setToggleUi(deviceState.on);
            sendToDevice(buildPayload(), 'State sent to device');
            syncDeviceFromPrefs();
        };
    }
    var effect = document.getElementById('effectSelect');
    if (effect) {
        effect.onchange = function () {
            deviceState.effect = effect.value;
            sendToDevice(buildPayload(), 'Effect sent to device');
            syncDeviceFromPrefs();
        };
    }
}

function buildForm() {
    var html = '<div class="row"><div class="col margin-2">';
    html += buildPickerHtml();
    html += '<form id="settingsForm" novalidate>';
    html += '<div class="accordion" id="settingsAccordion">';
    sections.forEach(function (s, idx) {
        html += '<div class="accordion-item">';
        html += '<h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#section-' + s.id + '" aria-expanded="false" aria-controls="section-' + s.id + '">' + s.title + '</button></h2>';
        html += '<div id="section-' + s.id + '" class="accordion-collapse collapse" data-bs-parent="#settingsAccordion">';
        html += '<div class="accordion-body">';
        if (s.fields.length === 0) {
            html += '<span class="text-muted">Coming soon</span>';
        } else {
            s.fields.forEach(function (f) {
                html += buildFieldHtml(f);
            });
        }
        html += buildSubAccordionsHtml(s, idx);
        html += '</div></div></div>';
    });
    html += '</div>';
    html += '<div class="text-center py-2"><button type="button" id="showLivePreview" class="btn btn-sm" style="background-color:lightgrey;border:0;color:#fff;font-weight:bold">Show Live Preview</button></div>';
    html += '<div class="text-center py-2"><img id="screenshot" alt="Captured frame (TRACE)" style="max-width:100%;border:1px solid #ccc;display:none"></div>';
    html += '<div class="mt-3"><button type="button" class="btn btn-orange w-100" onclick="saveForm()"> SAVE SETTINGS</button></div>';
    html += '<div class="text-center text-muted py-3"><span id="fpsCounter"></span></div></form></div></div>';
    $('.container-fluid + .container').html(html);
}

function fillField(f, cfg) {
    if (f.type === 'note') {
        return;
    }
    if (f.list) {
        var arr = cfg[f.list];
        var v = (arr != null && arr[f.index] != null) ? String(arr[f.index]) : '';
        document.getElementById(f.id).value = v;
        return;
    }
    var value = cfg[f.id];
    if (value == null) {
        return;
    }
    if (f.type === 'checkbox') {
        document.getElementById(f.id).checked = !!value;
    } else {
        var el = document.getElementById(f.id);
        if (el.tagName === 'SELECT') {
            var val = String(value);
            // Normalize the device technical state "GlowWormWifi" to the real effect value "Bias light".
            val = PICKER_EFFECT_TO_CONFIG[val] || val;
            var present = Array.prototype.some.call(el.options, function (o) {
                return o.value === val;
            });
            if (!present) {
                var opt = document.createElement('option');
                opt.value = val;
                opt.textContent = val;
                el.appendChild(opt);
            }
            el.value = val;
        } else {
            el.value = value;
        }
    }
}

function fillForm(cfg) {
    sections.forEach(function (s) {
        s.fields.forEach(function (f) {
            fillField(f, cfg);
        });
        (s.subAccordions || []).forEach(function (sub) {
            sub.fields.forEach(function (f) {
                fillField(f, cfg);
            });
        });
    });
}

function collectField(f, payload) {
    if (f.type === 'note') {
        return;
    }
    var el = document.getElementById(f.id);
    if (!el) {
        return;
    }
    if (f.list) {
        var v = el.value;
        if (!payload[f.list]) {
            payload[f.list] = [];
        }
        payload[f.list][f.index] = v;
        return;
    }
    if (f.type === 'checkbox') {
        payload[f.id] = el.checked;
    } else if (f.type === 'select') {
        var sel = el.value;
        if (sel === '') {
            return;
        }
        payload[f.id] = (selectType(f) === 'number') ? Number(sel) : sel;
    } else if (f.numeric) {
        var num = el.value;
        if (num !== '') {
            payload[f.id] = Number(num);
        }
    } else {
        var txt = el.value;
        if (txt !== '') {
            payload[f.id] = txt;
        }
    }
}

function collectPayload() {
    var payload = {};
    sections.forEach(function (s) {
        s.fields.forEach(function (f) {
            collectField(f, payload);
        });
        (s.subAccordions || []).forEach(function (sub) {
            sub.fields.forEach(function (f) {
                collectField(f, payload);
            });
        });
    });
    // Drop empty list slots (only present when a list field was added)
    Object.keys(payload).forEach(function (k) {
        if (Array.isArray(payload[k])) {
            payload[k] = payload[k].map(function (x) {
                return (x === undefined || x === null || x === '') ? null : x;
            });
        }
    });
    return payload;
}

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
    {key: 'sbPin', label: 'Button GPIO'},
    {key: 'lastSeen', label: 'Last seen'}
];

function escapeHtml(v) {
    return String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function refreshDevices() {
    fetchJson('getDevices').then(renderDevices).catch(function (err) {
        document.getElementById('devicesTable').innerHTML = '<span class="text-danger">Unable to load devices: ' + escapeHtml(err.message) + '</span>';
    });
}

function renderDevices(devices) {
    window.__devices = Array.isArray(devices) ? devices : [];
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
    devices.forEach(function (d) {
        html += '<tr>';
        DEVICE_COLUMNS.forEach(function (c) {
            var v = d[c.key];
            var isBool = (typeof v === 'boolean');
            var text = isBool ? (v ? '✔' : '') : (v == null || v === '' ? '—' : escapeHtml(v));
            html += '<td>' + text + '</td>';
        });
        html += '</tr>';
    });
    html += '</tbody></table>';
    el.innerHTML = html;
}

var PICKER_EFFECT_TO_CONFIG = {
    'Solid': 'Solid',
    'Fire': 'Fire',
    'Twinkle': 'Twinkle',
    'Bpm': 'Bpm',
    'Rainbow': 'Rainbow',
    'Slow rainbow': 'Slow rainbow',
    'Chase rainbow': 'Chase rainbow',
    'Solid rainbow': 'Solid rainbow',
    'Random colors': 'Random colors',
    'Rainbow colors': 'Rainbow colors',
    'Meteor': 'Meteor',
    'Color waterfall': 'Color waterfall',
    'Random marquee': 'Random marquee',
    'Rainbow marquee': 'Rainbow marquee',
    'Pulsing rainbow': 'Pulsing rainbow',
    'Christmas': 'Christmas',
    'Bias light': 'Bias light',
    // "GlowWormWifi" is the device-side technical state for the Bias Light effect; map it back so the
    // select shows Bias light and the config stores the real effect value.
    'GlowWormWifi': 'Bias light',
    'Music mode (VU Meter)': 'Music mode (VU Meter)',
    'Music mode (Stereo VU Meter)': 'Music mode (Stereo VU Meter)',
    'Music mode (Screen capture)': 'Music mode (Screen capture)',
    'Music mode (Rainbow music)': 'Music mode (Rainbow music)'
};

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
    // Persist the state of the top device picker (effectSelect) into the config effect field.
    // Its labels are the English effect i18n values, the same format the config effect field stores.
    var effectSelect = document.getElementById('effectSelect');
    if (effectSelect && effectSelect.value) {
        payload.effect = PICKER_EFFECT_TO_CONFIG[effectSelect.value] || effectSelect.value;
    }
    // Persist the color picker selection into the config colorChooser field (format "255,g,b,255").
    payload.colorChooser = '255,' + lastColor.g + ',' + lastColor.b + ',255';
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

function showToast(message, contextClass) {
    if (!document.getElementById('toastContainer')) {
        document.body.insertAdjacentHTML('beforeend', '<div id="toastContainer" style="position:relative;"><div style="position:absolute;top:0;right:0;"></div></div>');
    }
    var container = document.getElementById('toastContainer').children[0];
    container.insertAdjacentHTML('beforeend', '<div class="toast ' + contextClass + '" role="alert" aria-live="assertive" aria-atomic="true" style="position:fixed;bottom:10px;right:10px;z-index:1051;"><div class="toast-body">' + message + '</div></div>');
    $($(container.lastElementChild)).toast('show');
}

$(function () {
    var br = '<br class="d-sm-none">';
    $('#subtitle').html('Bias Lighting and Ambient Light software' + br + 'designed for' + br + 'Glow Worm Luciferin firmware');
    fetchJson('getFieldOptions').then(function (opts) {
        fieldOptions = opts || {};
        buildForm();
        initColorPicker();
        wireLivePreviewButton();
        return fetchJson('getConfig');
    }).then(function (cfg) {
        window.__lastConfig = cfg || {};
        fillForm(cfg);
        var profile = cfg && cfg.activeProfile;
        var profileEl = document.getElementById('activeProfile');
        if (profileEl) {
            profileEl.textContent = profile ? ('Profile: ' + profile) : '';
        }
        syncDeviceFromPrefs();
    }).catch(function (err) {
        showToast('Unable to load settings: ' + err.message, 'bg-danger text-white');
    });
    refreshDevices();
    pollFps();
    setInterval(pollFps, 1000);
});

function wireLivePreviewButton() {
    var showBtn = document.getElementById('showLivePreview');
    if (!showBtn) {
        return;
    }
    showBtn.addEventListener('click', function () {
        // Toggle: when the preview is on, ask the server to turn it off; when off, turn it on.
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

var livePreviewOn = false;
var livePreviewTimer = null;

function setLivePreview(on) {
    livePreviewOn = on;
    var btn = document.getElementById('showLivePreview');
    if (btn) {
        if (on) {
            // "Hide Live Preview": fully green (filled), no outline, dark text for contrast.
            btn.textContent = 'Hide Live Preview';
            btn.classList.remove('btn-outline-success');
            btn.style.backgroundColor = '#28a745';
            btn.style.border = '0';
            btn.style.color = '#fff';
            btn.style.fontWeight = 'bold';
        } else {
            // "Show Live Preview": light grey (like "Turn ON"), no coloured border, white bold text.
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
            // Clear the source to abort any in-flight image load so a delayed onload cannot
            // re-show the frame after the preview has been switched off.
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
    // Ignore stray ticks: if the preview has been switched off, a delayed poll (or the load
    // callback of an in-flight request) must not re-show the image.
    if (!livePreviewOn) {
        return;
    }
    var img = document.getElementById('screenshot');
    if (!img) {
        return;
    }
    // Cache-busting so the browser re-fetches the BMP (the grabber rewrites the same file path).
    img.src = 'screenshot?t=' + Date.now();
    img.onload = function () {
        // Re-check: the preview may have been turned off while this request was in flight.
        if (!livePreviewOn) {
            return;
        }
        img.style.display = 'inline-block';
    };
    img.onerror = function () {
        // 404 while no frame is captured yet; the button stays ON and polling continues.
        if (!livePreviewOn) {
            return;
        }
        img.style.display = 'none';
    };
}
