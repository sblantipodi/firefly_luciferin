var fieldOptions = {};
var fieldLabels = {};
var sectionTitles = {};
var sections = [
    {
        id: 'leds', fields: [
            {id: 'topLed', type: 'number', numeric: true, min: 0},
            {id: 'leftLed', type: 'number', numeric: true, min: 0},
            {id: 'rightLed', type: 'number', numeric: true, min: 0},
            {id: 'bottomLeftLed', type: 'number', numeric: true, min: 0},
            {id: 'bottomRightLed', type: 'number', numeric: true, min: 0},
            {id: 'bottomRowLed', type: 'number', numeric: true, min: 0},
            {id: 'ledStartOffset', type: 'number', numeric: true, min: 0},
            {id: 'orientation', type: 'select'},
            {id: 'groupBy', type: 'number', numeric: true, min: 0},
            {id: 'splitBottomMargin', type: 'text', numeric: false},
            {id: 'grabberAreaTopBottom', type: 'number', numeric: true, min: 0},
            {id: 'grabberSide', type: 'number', numeric: true, min: 0},
            {id: 'gapTypeTopBottom', type: 'text', numeric: false},
            {id: 'gapTypeSide', type: 'text', numeric: false}
        ]
    },
    {
        id: 'mode', fields: [
            {id: 'outputDevice', type: 'text', numeric: false},
            {id: 'baudRate', type: 'select'},
            {id: 'staticGlowWormIp', type: 'text', numeric: false},
            {id: 'desiredFramerate', type: 'select'},
            {id: 'smoothingType', type: 'select'},
            {id: 'smoothingTargetFramerate', type: 'number', numeric: true, min: 0, max: 240},
            {id: 'frameInsertionTarget', type: 'number', numeric: true, min: 0, max: 120},
            {id: 'emaAlpha', type: 'number', numeric: true, step: '0.05', min: 0, max: 1},
            {id: 'simdAvx', type: 'select'},
            {id: 'resamplingFactor', type: 'select'},
            {id: 'captureMethod', type: 'text', numeric: false},
            {id: 'monitorNumber', type: 'number', numeric: true, min: 1, max: 8},
            {id: 'screenResX', type: 'number', numeric: true, min: 0},
            {id: 'screenResY', type: 'number', numeric: true, min: 0},
            {id: 'osScaling', type: 'number', numeric: true, min: 100, max: 500},
            {id: 'defaultLedMatrix', type: 'select'},
            {id: 'autoDetectBlackBars', type: 'checkbox', numeric: false},
            {id: 'algo', type: 'select'},
            {id: 'language', type: 'select'}
        ],
        subAccordions: [
            {
                id: 'display', fields: [
                    {id: 'cubeLut', type: 'select'}
                ]
            }
        ]
    },
    {
        id: 'network', fields: [
            {id: 'mqttEnable', type: 'checkbox', numeric: false},
            {id: 'wirelessStream', type: 'checkbox', numeric: false},
            {id: 'streamType', type: 'select'},
            {id: 'mqttServer', type: 'text', numeric: false},
            {id: 'mqttTopic', type: 'text', numeric: false},
            {id: 'mqttDiscoveryTopic', type: 'text', numeric: false},
            {id: 'mqttUsername', type: 'text', numeric: false},
            {id: 'mqttPwd', type: 'text', numeric: false}
        ]
    },
    {
        id: 'misc', fields: [
            {id: 'effect', type: 'select'},
            {id: 'colorMode', type: 'select'},
            {id: 'gamma', type: 'number', numeric: true, step: '0.1', min: 0, max: 4},
            {id: 'whiteTemperature', type: 'number', numeric: true, min: 0, max: 30000},
            {id: 'brightness', type: 'number', numeric: true, min: 0, max: 100},
            {id: 'nightModeFrom', type: 'text', numeric: false},
            {id: 'nightModeTo', type: 'text', numeric: false},
            {id: 'nightModeBrightness', type: 'text', numeric: false},
            {id: 'toggleLed', type: 'checkbox', numeric: false},
            {id: 'startWithSystem', type: 'checkbox', numeric: false},
            {id: 'runtimeLogLevel', type: 'text', numeric: false}
        ],
        subAccordions: [
            {
                id: 'colorCorr', fields: [
                    {
                        id: 'ccInfo',
                        type: 'note',
                        note: 'Exposed via the hueMap field, currently not available from the web API (excluded by the server). Manage it from the JavaFX interface.'
                    }
                ]
            },
            {
                id: 'eyeCare', fields: [
                    {id: 'nightLight', type: 'select'},
                    {id: 'nightLightLvl', type: 'number', numeric: true, min: 1, max: 100},
                    {id: 'luminosityThreshold', type: 'number', numeric: true, min: 0},
                    {id: 'brightnessLimiter', type: 'select'}
                ]
            },
            {
                id: 'gamma', fields: [
                    {id: 'enableAutomaticGamma', type: 'checkbox', numeric: false},
                    {id: 'gammaLevel', type: 'select'}
                ]
            },
            {
                id: 'profile', fields: [
                    {id: 'checkFullScreen', type: 'checkbox', numeric: false},
                    {id: 'gpuThreshold', type: 'number', numeric: true, min: 0, max: 100},
                    {id: 'cpuThreshold', type: 'number', numeric: true, min: 0, max: 100},
                    {id: 'profileProcess1', type: 'text', numeric: false, list: 'profileProcesses', index: 0},
                    {id: 'profileProcess2', type: 'text', numeric: false, list: 'profileProcesses', index: 1},
                    {id: 'profileProcess3', type: 'text', numeric: false, list: 'profileProcesses', index: 2}
                ]
            },
            {
                id: 'smoothing', fields: [
                    {id: 'smoothingTargetFramerate', type: 'number', numeric: true, min: 0, max: 240},
                    {
                        id: 'smoothingNote',
                        type: 'note',
                        note: 'EMA alpha, frame insertion and smoothing type are derived from the target and managed by the app.'
                    }
                ]
            }
        ]
    },
    {
        id: 'devices', fields: [
            {id: 'powerSaving', type: 'select'},
            {id: 'multiMonitor', type: 'select'},
            {id: 'multiScreenSingleDevice', type: 'checkbox', numeric: false},
            {id: 'checkForUpdates', type: 'checkbox', numeric: false},
            {id: 'syncCheck', type: 'checkbox', numeric: false}
        ],
        subAccordions: [
            {
                id: 'connectedDevices', fields: [
                    {id: 'devicesContent', type: 'note', note: 'Loading devices…'}
                ]
            },
            {
                id: 'satellites', fields: [
                    {
                        id: 'satInfo',
                        type: 'note',
                        note: 'Managed via the satellites map, currently not available from the web API (excluded by the server). Manage it from the JavaFX interface.'
                    }
                ]
            }
        ]
    },
    {
        id: 'ldr', fields: [
            {id: 'enableLDR', type: 'checkbox', numeric: false},
            {id: 'ldrInterval', type: 'number', numeric: true, min: 0},
            {id: 'ldrMin', type: 'number', numeric: true, min: 0},
            {id: 'ldrTurnOff', type: 'checkbox', numeric: false}
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

function fieldLabel(f) {
    return (fieldLabels[f.id] != null) ? fieldLabels[f.id] : f.label;
}

function sectionTitle(id) {
    return (sectionTitles[id] != null) ? sectionTitles[id] : id;
}

function buildFieldHtml(f) {
    var lbl = fieldLabel(f);
    if (f.id === 'devicesContent') {
        return '<div id="devicesTable" class="table-responsive"></div>';
    }
    if (f.type === 'note') {
        return '<div class="form-text">' + f.note + '</div>';
    }
    if (f.type === 'checkbox') {
        return '<div class="form-check d-flex flex-column align-items-start ps-0"><label class="form-check-label mb-1" for="' + f.id + '">' + lbl + '</label><input type="checkbox" class="form-check-input mt-0 ms-0" id="' + f.id + '"></div>';
    }
    if (f.type === 'select') {
        var opts = optionsFor(f).map(function (o) {
            return '<option value="' + escapeHtml(o.value) + '">' + escapeHtml(o.label) + '</option>';
        }).join('');
        return '<div class="form-group"><label for="' + f.id + '">' + lbl + '</label> <select class="form-select" id="' + f.id + '">' + opts + '</select></div>';
    }
    var inputType = f.type === 'number' ? 'text' : f.type;
    var inputHtml;
    if (f.id === 'outputDevice') {
        inputHtml = '<select class="form-select" id="' + f.id + '"><option value="">--</option></select>';
    } else {
        inputHtml = '<input type="' + inputType + '" class="form-control" id="' + f.id + '"' + (f.numeric ? ' inputmode="numeric"' : '') + '>';
    }
    return '<div class="form-group"><label for="' + f.id + '">' + lbl + '</label> ' + inputHtml + '</div>';
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
        html += '<h3 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#sub-' + section.id + '-' + sub.id + '" aria-expanded="false" aria-controls="sub-' + section.id + '-' + sub.id + '">' + sectionTitle(sub.id) + '</button></h3>';
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
    html += '<h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#section-profiles" aria-expanded="false" aria-controls="section-profiles">' + sectionTitle('profile') + '</button></h2>';
    html += '<div id="section-profiles" class="accordion-collapse collapse" data-bs-parent="#settingsAccordion">';
    html += '<div class="accordion-body">';
    html += '<div id="profilesList" class="list-group mb-2"></div>';
    html += '<div class="input-group input-group-sm mb-2"><input type="text" class="form-control" id="newProfileName" placeholder="New profile name"><button type="button" class="btn btn-orange btn-sm" onclick="addProfile()">Add</button></div>';
    html += '<div class="form-text text-muted">Click a profile name to activate it. Firefly will restart with the selected profile. Add a new profile to copy the current configuration under a new name.</div>';
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
        html += '<h2 class="accordion-header"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#section-' + s.id + '" aria-expanded="false" aria-controls="section-' + s.id + '">' + sectionTitle(s.id) + '</button></h2>';
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
    html += '<div class="text-center py-2"><button type="button" id="showLivePreview" class="btn btn-sm">Show Live Preview</button></div>';
    html += '<div class="text-center py-2"><img id="screenshot" alt="Captured frame (TRACE)"></div>';
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
    if (payload.outputDevice && payload.staticGlowWormIp && payload.staticGlowWormIp !== 'Auto' && payload.staticGlowWormIp !== '-') {
        payload.outputDevice = '-';
    }
    payload.colorChooser = lastColor.r + ',' + lastColor.g + ',' + lastColor.b + ',255';
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
