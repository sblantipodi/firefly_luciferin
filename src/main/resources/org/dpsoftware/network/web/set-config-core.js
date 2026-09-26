// Core form engine of the settings page: builds the accordion form from the sections/fields defined in set-config-schema.js, fills it with
// the current configuration, and collects the edited values into the JSON payload.
import {sections} from './set-config-schema.js';
import {state} from './set-config-state.js';
import {escapeHtml} from './set-config-ui.js';
import {fillPickerControls, lastColor} from './set-config-device.js';

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

// Wraps a list of field HTML blocks into a responsive Bootstrap grid row.
function buildFieldsGrid(fields) {
    var html = '<div class="row g-3">';
    fields.forEach(function (f) {
        var colClass = (f.id === 'devicesContent') ? 'col-12' : 'col-12 col-md-6 col-lg-3';
        html += '<div class="' + colClass + '">' + buildFieldHtml(f) + '</div>';
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

// Builds and inserts the whole settings page: one accordion section per schema section (with sub-accordions), a profiles section, then initializes the picker.
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
    var profiles = buildAccordion('section-profiles', sectionTitle('profile'), accordion.id);
    profiles.querySelector('.accordion-body').appendChild(document.getElementById('profilesTemplate').content.cloneNode(true));
    accordion.appendChild(profiles);
    document.querySelector('.container-fluid + .container').replaceChildren(page);
    fillPickerControls();
}

// Sets a single form control from the configuration value (handles checkboxes, selects with missing options, list fields and the staticGlowWormIp 'Auto' alias).
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

// Fills every field (including sub-accordion fields) of the form with the given configuration object.
export function fillForm(cfg) {
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

// Reads a single form control back into the payload object, casting to the field type and applying field-specific conversions (e.g. 'Auto' to '-').
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

// Collects all edited form values into the JSON payload sent to the server: normalizes arrays, applies the outputDevice/staticGlowWormIp
// rule and appends the current color picker color.
export function collectPayload() {
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
