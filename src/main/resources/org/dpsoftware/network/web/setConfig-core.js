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
        id: 'network', title: 'Network', fields: [
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

function escapeHtml(v) {
    return String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
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
    if (f.id === 'devicesContent') {
        return '<div id="devicesTable" class="table-responsive"></div>';
    }
    if (f.type === 'note') {
        return '<div class="form-text">' + f.note + '</div>';
    }
    if (f.type === 'checkbox') {
        return '<div class="form-check d-flex flex-column align-items-start ps-0"><label class="form-check-label mb-1" for="' + f.id + '">' + f.label + '</label><input type="checkbox" class="form-check-input mt-0 ms-0" id="' + f.id + '"></div>';
    }
    if (f.type === 'select') {
        var opts = optionsFor(f).map(function (o) {
            return '<option value="' + escapeHtml(o.value) + '">' + escapeHtml(o.label) + '</option>';
        }).join('');
        return '<div class="form-group"><label for="' + f.id + '">' + f.label + '</label> <select class="form-select" id="' + f.id + '">' + opts + '</select></div>';
    }
    var inputType = f.type === 'number' ? 'text' : f.type;
    var inputHtml;
    if (f.id === 'outputDevice') {
        inputHtml = '<select class="form-select" id="' + f.id + '"><option value="">--</option></select>';
    } else {
        inputHtml = '<input type="' + inputType + '" class="form-control" id="' + f.id + '"' + (f.numeric ? ' inputmode="numeric"' : '') + '>';
    }
    return '<div class="form-group"><label for="' + f.id + '">' + f.label + '</label> ' + inputHtml + '</div>';
}

function buildFieldsGrid(fields) {
    var html = '<div class="row g-3">';
    fields.forEach(function (f) {
        var colClass = (f.id === 'devicesContent') ? 'col-12' : 'col-12 col-md-6 col-lg-3';
        html += '<div class="' + colClass + '">' + buildFieldHtml(f) + '</div>';
    });
    html += '</div>';
    return html;
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
        html += buildFieldsGrid(sub.fields);
        html += '</div></div></div>';
    });
    html += '</div>';
    return html;
}

function buildProfilesAccordion() {
    var activeProfile = (window.__lastConfig && window.__lastConfig.activeProfile) ? window.__lastConfig.activeProfile : '';
    var html = '<div class="accordion-item">';
    html += '<h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#section-profiles" aria-expanded="false" aria-controls="section-profiles">Profiles</button></h2>';
    html += '<div id="section-profiles" class="accordion-collapse collapse" data-bs-parent="#settingsAccordion">';
    html += '<div class="accordion-body">';
    html += '<div id="profilesList" class="list-group mb-2"></div>';
    html += '<div class="form-text text-muted">Click a profile name to activate it. Firefly will restart with the selected profile.</div>';
    html += '</div></div></div>';
    return html;
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
            html += buildFieldsGrid(s.fields);
        }
        html += buildSubAccordionsHtml(s, idx);
        html += '</div></div></div>';
    });
    html += buildProfilesAccordion();
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
        // When staticGlowWormIp is "-" display "Auto" in the form.
        if (f.id === 'staticGlowWormIp' && value === '-') {
            value = 'Auto';
        }
        var el = document.getElementById(f.id);
        if (el.tagName === 'SELECT') {
            var val = String(value);
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
            // When staticGlowWormIp shows "Auto" (stored as "-"), send "-" to the server.
            payload[f.id] = (f.id === 'staticGlowWormIp' && txt === 'Auto') ? '-' : txt;
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
    Object.keys(payload).forEach(function (k) {
        if (Array.isArray(payload[k])) {
            payload[k] = payload[k].map(function (x) {
                return (x === undefined || x === null || x === '') ? null : x;
            });
        }
    });
    return payload;
}

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
    var effectSelect = document.getElementById('effectSelect');
    if (effectSelect && effectSelect.value) {
        payload.effect = PICKER_EFFECT_TO_CONFIG[effectSelect.value] || effectSelect.value;
    }
    if (payload.outputDevice && payload.staticGlowWormIp && payload.staticGlowWormIp !== 'Auto' && payload.staticGlowWormIp !== '-') {
        payload.outputDevice = '-';
    }
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
