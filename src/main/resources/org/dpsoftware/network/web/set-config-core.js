// Core form engine of the settings page: builds the accordion form from the sections/fields defined in set-config-schema.js, fills it with
// the current configuration, and collects the edited values into the JSON payload.
import {sections} from './set-config-schema.js';
import {state} from './set-config-state.js';
import {escapeHtml} from './set-config-ui.js';
import {buildSatellitesHtml, collectSatellites, fillPickerControls} from './set-config-device.js';

// Resolves the option list for a select field, preferring the server-provided options over the static schema defaults.
function optionsFor(f) {
    var src = (state.fieldOptions && state.fieldOptions[f.id]) ? state.fieldOptions[f.id].options : f.options;
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

// Returns the value type ('number' or 'string') of a select field, used to cast values when collecting the payload.
function selectType(f) {
    return (state.fieldOptions && state.fieldOptions[f.id]) ? state.fieldOptions[f.id].type : (f.numeric ? 'number' : 'string');
}

// Returns the display label for a field, preferring the server-provided label.
function fieldLabel(f) {
    return (state.fieldLabels[f.id] != null) ? state.fieldLabels[f.id] : f.label;
}

// Returns the display title of a section, falling back to its id.
function sectionTitle(id) {
    return (state.sectionTitles[id] != null) ? state.sectionTitles[id] : id;
}

// Builds the HTML markup for a single field (note, checkbox, select, number, text; special-casing the devices table and output device select).
function buildFieldHtml(f) {
    var lbl = fieldLabel(f);
    if (f.id === 'devicesContent') {
        return '<div id="devicesTable" class="table-responsive"></div>';
    }
    if (f.id === 'satelliteManager') {
        return buildSatellitesHtml();
    }
    if (f.id === 'profilesControl') {
        return '<div class="form-group"><label class="d-block">' + escapeHtml(lbl) + '</label><div id="miscProfilesHost"></div></div>';
    }
    if (f.type === 'note') {
        return '<div class="form-text">' + escapeHtml(f.noteKey ? fieldLabel(f) : f.note) + '</div>';
    }
    if (f.id === 'softwareVersion') {
        return '<div class="form-group"><label class="d-block">' + escapeHtml(lbl) + '</label><a class="orange-link" href="https://github.com/sblantipodi/firefly_luciferin/releases" target="_blank" rel="noopener">'
            + escapeHtml(state.fieldLabels.softwareVersionValue || '') + '</a></div>';
    }
    if (f.type === 'readonly') {
        return '<div class="form-group"><label for="' + f.id + '">' + escapeHtml(lbl) + '</label><input class="form-control" id="' + f.id + '" readonly></div>';
    }
    if (f.type === 'action' && f.id === 'improvAction') {
        return '<button type="button" id="improvProvisionButton" class="btn btn-primary">' + escapeHtml(lbl) + '</button>';
    }
    if (f.type === 'combo') {
        var choices = optionsFor(f).map(function (option) {
            return '<option value="' + escapeHtml(option.value) + '"></option>';
        }).join('');
        return '<div class="form-group"><label for="' + f.id + '">' + escapeHtml(lbl) + '</label><input type="text" class="form-control" id="' + f.id + '" list="' + f.id + 'List"><datalist id="' + f.id + 'List">' + choices + '</datalist></div>';
    }
    if (f.type === 'actions' && f.id === 'mqttDiscoveryActions') {
        var addLabel = escapeHtml(state.fieldLabels.mqttDiscoveryAdd || 'Add');
        var removeLabel = escapeHtml(state.fieldLabels.mqttDiscoveryRemove || 'Remove');
        return '<div class="form-group"><label class="d-block">' + escapeHtml(lbl) + '</label><div class="d-flex gap-2"><button type="button" id="addButton" class="btn btn-outline-success" title="' + addLabel + '" aria-label="' + addLabel + '">✔</button><button type="button" id="removeButton" class="btn btn-outline-danger" title="' + removeLabel + '" aria-label="' + removeLabel + '">✖</button></div></div>';
    }
    if (f.type === 'checkbox') {
        return '<div class="form-check d-flex flex-column align-items-start ps-0"><label class="form-check-label mb-1" for="' + f.id + '">' + lbl + '</label><input type="checkbox" class="form-check-input mt-0 ms-0" id="' + f.id + '"></div>';
    }
    if (f.type === 'toggleButton') {
        return '<div class="form-group"><label class="d-block" for="' + f.id + '">' + escapeHtml(lbl) + '</label><button type="button" class="btn btn-outline-primary" id="' + f.id + '" aria-pressed="false"></button></div>';
    }
    if (f.type === 'select') {
        var opts = optionsFor(f).map(function (o) {
            return '<option value="' + escapeHtml(o.value) + '">' + escapeHtml(o.label) + '</option>';
        }).join('');
        if (f.direction) {
            var before = f.direction === 'vertical' ? 'up' : 'left';
            var after = f.direction === 'vertical' ? 'down' : 'right';
            return '<div class="form-group directional-field"><label for="' + f.id + '"><i class="fa-solid fa-arrow-' + before + '" aria-hidden="true"></i><span>' + escapeHtml(lbl) + '</span><i class="fa-solid fa-arrow-' + after + '" aria-hidden="true"></i></label><select class="form-select" id="' + f.id + '">' + opts + '</select></div>';
        }
        return '<div class="form-group"><label for="' + f.id + '">' + lbl + '</label> <select class="form-select" id="' + f.id + '">' + opts + '</select></div>';
    }
    var inputType = f.digitsOnly ? 'text' : f.type;
    var inputHtml;
    if (f.id === 'outputDevice') {
        inputHtml = '<select class="form-select" id="' + f.id + '"><option value="">--</option></select>';
    } else {
        inputHtml = '<input type="' + inputType + '" class="form-control" id="' + f.id + '"' + (f.numeric ? ' inputmode="numeric"' + (f.digitsOnly ? ' pattern="[0-9]+"' : ' step="' + (f.step || '1') + '"') + (f.min != null ? ' min="' + f.min + '"' : '') + (f.max != null ? ' max="' + f.max + '"' : '') : '') + (f.type === 'time' && f.step ? ' step="' + f.step + '"' : '') + '>';
    }
    if (f.type === 'range') {
        inputHtml += '<output class="d-block" id="' + f.id + 'Value" for="' + f.id + '"></output>';
    }
    return '<div class="form-group"><label for="' + f.id + '">' + lbl + '</label> ' + inputHtml + '</div>';
}

// Wraps a list of field HTML blocks into a responsive Bootstrap grid row.
function buildFieldsGrid(fields) {
    var html = '<div class="row g-3">';
    fields.forEach(function (f) {
        var colClass = (f.id === 'devicesContent' || f.id === 'improvContext' || f.id === 'improvAction'
            || f.id === 'profilesControl' || f.id === 'satelliteManager')
            ? 'col-12' : 'col-12 col-md-6 col-lg-3';
        html += '<div class="' + colClass + '" id="field-' + f.id + '">' + buildFieldHtml(f) + '</div>';
    });
    html += '</div>';
    return html;
}

// Clones the accordion template into a configured accordion item bound to the given id/parent; nested items use an h3 heading instead of h2.
function buildAccordion(id, title, parentId, nested = false) {
    var item = document.getElementById('accordionTemplate').content.firstElementChild.cloneNode(true);
    var heading = item.querySelector('.accordion-header');
    if (nested) {
        var subheading = document.createElement('h3');
        subheading.className = heading.className;
        subheading.append(...heading.childNodes);
        heading.replaceWith(subheading);
    }
    var button = item.querySelector('.accordion-button');
    button.setAttribute('data-bs-target', '#' + id);
    button.setAttribute('aria-controls', id);
    button.innerHTML = title;
    var collapse = item.querySelector('.accordion-collapse');
    collapse.id = id;
    collapse.setAttribute('data-bs-parent', '#' + parentId);
    return item;
}

// Builds the nested accordion container for a section's sub-accordions.
function buildSubAccordions(section) {
    var accordion = document.createElement('div');
    accordion.className = 'accordion mt-3';
    accordion.id = 'subAccordion-' + section.id;
    section.subAccordions.forEach(function (sub) {
        var item = buildAccordion('sub-' + section.id + '-' + sub.id, sectionTitle(sub.id), accordion.id, true);
        item.querySelector('.accordion-body').innerHTML = buildFieldsGrid(sub.fields);
        accordion.appendChild(item);
    });
    return accordion;
}

// Builds and inserts the settings accordions, their nested sections, the profiles controls, and the picker.
export function buildForm() {
    var page = document.getElementById('settingsPageTemplate').content.cloneNode(true);
    var accordion = page.querySelector('#settingsAccordion');
    sections.forEach(function (section) {
        var item = buildAccordion('section-' + section.id, sectionTitle(section.id), accordion.id);
        var body = item.querySelector('.accordion-body');
        body.innerHTML = section.fields.length === 0
            ? '<span class="text-muted">Coming soon</span>'
            : buildFieldsGrid(section.fields);
        if (section.subAccordions && section.subAccordions.length > 0) {
            body.appendChild(buildSubAccordions(section));
        }
        accordion.appendChild(item);
    });
    page.querySelector('#miscProfilesHost').appendChild(document.getElementById('profilesTemplate').content.cloneNode(true));
    var logs = buildAccordion('section-log', 'LOG', accordion.id);
    logs.querySelector('.accordion-body').appendChild(document.getElementById('logTemplate').content.cloneNode(true));
    accordion.appendChild(logs);
    document.getElementById('settingsContainer').replaceChildren(page);
    fillPickerControls();
    for (var id of [...ledCountIds, 'bottomRowLed', 'screenResX', 'screenResY', 'mqttPort',
        'improvMi', 'improvMo', 'improvSck', 'improvCs']) {
        document.getElementById(id).addEventListener('input', function (event) {
            var clean = event.target.value.replace(/[^0-9]/g, '');
            if (event.target.value !== clean) {
                event.target.value = clean;
            }
            if (event.target.id !== 'mqttPort' && !event.target.id.startsWith('improv')) {
                updateGroupByOptions();
            }
        });
    }
    ['brightness', 'whiteTemperature', 'audioLoopbackGain'].forEach(function (id) {
        var control = document.getElementById(id);
        control.addEventListener('input', function () {
            document.getElementById(id + 'Value').textContent = control.value + (id === 'brightness' ? '%' : id === 'whiteTemperature' ? ' K' : '');
        });
    });
    document.getElementById('monitorNumber').addEventListener('change', function () {
        updateCaptureMethodOptions(true);
    });
}

const ledCountIds = ['topLed', 'leftLed', 'rightLed', 'bottomLeftLed', 'bottomRightLed'];

// Rebuilds capture method choices for the selected monitor or external device.
function updateCaptureMethodOptions(resetSelection = false) {
    var monitor = document.getElementById('monitorNumber').value;
    var select = document.getElementById('captureMethod');
    var previous = select.value;
    var key = monitor.startsWith('device:') ? 'captureMethodExternal' : 'captureMethod';
    var available = (state.fieldOptions[key] || {}).options || [];
    select.replaceChildren();
    available.forEach(function (entry) {
        select.add(new Option(entry.label, entry.value));
    });
    select.value = !resetSelection && available.some(function (entry) {
        return entry.value === previous;
    }) ? previous : 'AUTO';
}

// Keeps the LED grouping choices between 1 and the smallest configured row count.
function updateGroupByOptions() {
    var select = document.getElementById('groupBy');
    var previous = select.value;
    var counts = ledCountIds.map(function (id) {
        return Number(document.getElementById(id).value);
    });
    var minimum = counts.every(function (n) {
        return Number.isSafeInteger(n) && n > 0;
    }) ? Math.min(...counts) : 0;
    select.replaceChildren();
    for (var i = 1; i <= minimum; i++) {
        select.add(new Option(String(i), String(i)));
    }
    select.value = Number(previous) <= minimum && Number(previous) >= 1 ? previous : (minimum ? '1' : '');
}

// Sets a single form control from the configuration value (handles checkboxes, selects with missing options, list fields and the staticGlowWormIp 'Auto' alias).
function fillField(f, cfg) {
    if (f.type === 'note' || f.type === 'info' || f.type === 'action' || f.type === 'satelliteManager' || f.type === 'readonly' || f.type === 'actions' || f.type === 'profiles' || f.provisioning) {
        return;
    }
    if (f.list) {
        var arr = cfg[f.list];
        var v = (arr != null && arr[f.index] != null) ? String(arr[f.index]) : '';
        document.getElementById(f.id).value = v;
        return;
    }
    var value = cfg[f.id];
    if (f.id === 'serialPort') {
        value = cfg.staticGlowWormIp && cfg.staticGlowWormIp !== '-'
            ? cfg.staticGlowWormIp : cfg.outputDevice;
    }
    if (f.id === 'brightness') {
        value = Math.round(Number(cfg.brightness || 0) / 255 * 100);
    } else if (f.id === 'whiteTemperature') {
        value = Number(cfg.whiteTemperature || 0) * 100;
    } else if (f.id === 'desiredFramerate') {
        var rates = optionsFor(f);
        value = /^\d+$/.test(String(cfg.desiredFramerate || ''))
            ? cfg.desiredFramerate + ' FPS' : (rates.length ? rates[rates.length - 1].label : cfg.desiredFramerate);
    }
    if (f.id === 'mqttHost') {
        var mqttAddress = String(cfg.mqttServer || '').replace(/^tcp:\/\//, '');
        value = mqttAddress.substring(0, mqttAddress.lastIndexOf(':')) || mqttAddress;
    } else if (f.id === 'mqttPort') {
        value = String(cfg.mqttServer || '').split(':').pop() || '1883';
    } else if (f.id === 'mqttUser') {
        value = cfg.mqttUsername;
    }
    if (f.id === 'monitorNumber' && cfg.captureDevice && cfg.captureDevice.friendlyName) {
        value = 'device:' + cfg.captureDevice.friendlyName;
    }
    if (f.id === 'defaultLedMatrix' && cfg.autoDetectBlackBars) {
        value = 'AUTO';
    }
    if (value == null) {
        return;
    }
    if (f.type === 'toggleButton') {
        el = document.getElementById(f.id);
        el.setAttribute('aria-pressed', String(!!value));
        el.textContent = value ? state.fieldLabels.turnLedOff : state.fieldLabels.turnLedOn;
        el.classList.toggle('btn-primary', !!value);
        el.classList.toggle('btn-outline-primary', !value);
    } else if (f.type === 'checkbox') {
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
            if (!present && !['groupBy', 'splitBottomMargin', 'grabberAreaTopBottom', 'grabberSide', 'gapTypeTopBottom', 'gapTypeSide'].includes(f.id)) {
                var opt = document.createElement('option');
                opt.value = val;
                opt.textContent = val;
                el.appendChild(opt);
            }
            el.value = present || !['splitBottomMargin', 'grabberAreaTopBottom', 'grabberSide', 'gapTypeTopBottom', 'gapTypeSide'].includes(f.id) ? val : '0%';
        } else {
            el.value = value;
        }
        if (f.type === 'range') {
            document.getElementById(f.id + 'Value').textContent = el.value
                + (f.id === 'brightness' ? '%' : f.id === 'whiteTemperature' ? ' K' : '');
        }
    }
}

// Fills every field (including sub-accordion fields) of the form with the given configuration object.
export function fillForm(cfg) {
    sections.forEach(function (s) {
        s.fields.forEach(function (f) {
            fillField(f, cfg);
            if (f.id === 'monitorNumber') {
                updateCaptureMethodOptions();
            }
        });
        (s.subAccordions || []).forEach(function (sub) {
            sub.fields.forEach(function (f) {
                fillField(f, cfg);
            });
        });
    });
    updateGroupByOptions();
    if (Number.isInteger(cfg.groupBy) && cfg.groupBy >= 1 && cfg.groupBy <= Number(document.getElementById('groupBy').lastElementChild?.value)) {
        document.getElementById('groupBy').value = String(cfg.groupBy);
    }
}

// Reads a single form control back into the payload object, casting to the field type and applying field-specific conversions (e.g. 'Auto' to '-').
function collectField(f, payload) {
    if (f.type === 'note' || f.type === 'info' || f.type === 'action' || f.type === 'satelliteManager' || f.type === 'readonly' || f.type === 'actions' || f.type === 'profiles' || f.provisioning) {
        return;
    }
    var el = document.getElementById(f.id);
    if (!el) {
        return;
    }
    if (f.id === 'mqttHost' || f.id === 'mqttPort') {
        return;
    }
    if (f.id === 'serialPort') {
        payload.serialPort = el.value.trim();
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
    if (f.type === 'toggleButton') {
        payload[f.id] = el.getAttribute('aria-pressed') === 'true';
    } else if (f.type === 'checkbox') {
        payload[f.id] = el.checked;
    } else if (f.type === 'select') {
        var sel = el.value;
        if (sel === '') {
            return;
        }
        if (f.id === 'monitorNumber') {
            if (sel.startsWith('device:')) {
                payload.monitorNumber = 0;
                payload.captureDeviceName = sel.substring('device:'.length);
            } else {
                payload.monitorNumber = Number(sel);
            }
            return;
        }
        if (f.id === 'defaultLedMatrix') {
            payload.autoDetectBlackBars = sel === 'AUTO';
            if (sel !== 'AUTO') {
                payload.defaultLedMatrix = sel;
            }
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
        if (f.id === 'mqttUser') {
            payload.mqttUsername = txt;
            return;
        }
        if (['mqttTopic', 'mqttPwd', 'mqttDiscoveryTopic'].includes(f.id)) {
            payload[f.id] = txt;
            return;
        }
        if (txt !== '') {
            // When staticGlowWormIp shows "Auto" (stored as "-"), send "-" to the server.
            payload[f.id] = (f.id === 'staticGlowWormIp' && txt === 'Auto') ? '-' : txt;
        }
    }
}

// Collects all edited form values into the JSON payload sent to the server: normalizes arrays, applies the outputDevice/staticGlowWormIp
// rule and appends the current color picker color.
export function collectPayload() {
    var mqttHost = document.getElementById('mqttHost').value.trim();
    var mqttPort = document.getElementById('mqttPort').value.trim();
    if (!/^[^\s:\/]+$/.test(mqttHost)) {
        throw new Error(fieldLabel({id: 'mqttHost'}) + ': invalid host');
    }
    if (!/^\d{1,5}$/.test(mqttPort) || Number(mqttPort) < 1 || Number(mqttPort) > 65535) {
        throw new Error(fieldLabel({id: 'mqttPort'}) + ': 1–65535');
    }
    for (var dimension of ['screenResX', 'screenResY']) {
        var dimensionValue = document.getElementById(dimension).value;
        if (!/^\d+$/.test(dimensionValue) || !Number.isSafeInteger(Number(dimensionValue))) {
            throw new Error(fieldLabel({id: dimension}) + ': ' + (state.fieldLabels['web.wholeNumber'] || 'Enter a whole number'));
        }
    }
    var counts = ledCountIds.map(function (id) {
        var value = document.getElementById(id).value;
        if (!/^\d+$/.test(value) || !Number.isSafeInteger(Number(value))) {
            throw new Error(fieldLabel({id: id}) + ': ' + (state.fieldLabels['web.wholeNumber'] || 'Enter a whole number'));
        }
        return Number(value);
    });
    var bottom = document.getElementById('bottomRowLed').value;
    if (!/^\d+$/.test(bottom) || !Number.isSafeInteger(Number(bottom))) {
        throw new Error(fieldLabel({id: 'bottomRowLed'}) + ': ' + (state.fieldLabels['web.wholeNumber'] || 'Enter a whole number'));
    }
    var group = Number(document.getElementById('groupBy').value);
    if (!Number.isInteger(group) || group < 1 || group > Math.min(...counts)) {
        throw new Error(fieldLabel({id: 'groupBy'}) + ': ' + (state.fieldLabels['web.validGroup'] || 'Select a valid value'));
    }
    var payload = {};
    payload.mqttServer = 'tcp://' + mqttHost + ':' + mqttPort;
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
    payload.satellites = collectSatellites();
    return payload;
}
