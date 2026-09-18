var fields = [
    {id: 'baudRate', label: 'Baud Rate', type: 'text', numeric: false},
    {id: 'desiredFramerate', label: 'Framerate', type: 'text', numeric: false},
    {id: 'colorMode', label: 'Color Mode (1=RGB, 2=RGBW)', type: 'number', numeric: true, min: 1, max: 2},
    {id: 'gamma', label: 'Gamma', type: 'number', numeric: true, step: '0.1', min: 0, max: 4},
    {id: 'whiteTemperature', label: 'White Temperature (Kelvin)', type: 'number', numeric: true, min: 0, max: 30000},
    {id: 'topLed', label: 'Top LEDs', type: 'number', numeric: true, min: 0},
    {id: 'leftLed', label: 'Left LEDs', type: 'number', numeric: true, min: 0},
    {id: 'rightLed', label: 'Right LEDs', type: 'number', numeric: true, min: 0},
    {id: 'bottomLeftLed', label: 'Bottom-Left LED', type: 'number', numeric: true, min: 0},
    {id: 'bottomRightLed', label: 'Bottom-Right LED', type: 'number', numeric: true, min: 0},
    {id: 'bottomRowLed', label: 'Bottom-Row LEDs', type: 'number', numeric: true, min: 0},
    {id: 'ledStartOffset', label: 'LED Start Offset', type: 'number', numeric: true, min: 0},
    {id: 'enableLDR', label: 'Enable LDR', type: 'checkbox', numeric: false},
    {id: 'ldrInterval', label: 'LDR Interval', type: 'number', numeric: true, min: 0},
    {id: 'ldrMin', label: 'LDR Minimum Brightness', type: 'number', numeric: true, min: 0},
    {id: 'ldrTurnOff', label: 'Turn off on LDR', type: 'checkbox', numeric: false}
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

function buildForm() {
    var html = '<div class="row"><div class="col margin-2">';
    html += '<form onsubmit="event.preventDefault(); saveForm();">';
    fields.forEach(function (f) {
        if (f.type === 'checkbox') {
            html += '<div class="form-check"><input type="checkbox" class="form-check-input" id="' + f.id + '"><label class="form-check-label" for="' + f.id + '">' + f.label + '</label></div><br>';
        } else {
            html += '<div class="form-group"><label for="' + f.id + '">' + f.label + '</label> <input type="' + f.type + '" class="form-control" id="' + f.id + '"' + (f.numeric ? ' inputmode="numeric" required' : '') + (f.min != null ? ' min="' + f.min + '"' : '') + (f.max != null ? ' max="' + f.max + '"' : '') + (f.step ? ' step="' + f.step + '"' : '') + '></div>';
        }
    });
    html += '<button type="submit" class="btn btn-orange"> SAVE SETTINGS</button><br><br></form></div></div>';
    $('.container').html(html);
}

function fillForm(cfg) {
    fields.forEach(function (f) {
        var value = cfg[f.id];
        if (value == null) {
            return;
        }
        if (f.type === 'checkbox') {
            document.getElementById(f.id).checked = !!value;
        } else {
            document.getElementById(f.id).value = value;
        }
    });
}

function collectPayload() {
    var payload = {};
    fields.forEach(function (f) {
        if (f.type === 'checkbox') {
            payload[f.id] = document.getElementById(f.id).checked;
        } else if (f.numeric) {
            var v = document.getElementById(f.id).value;
            if (v !== '') {
                payload[f.id] = Number(v);
            }
        } else {
            var v2 = document.getElementById(f.id).value;
            if (v2 !== '') {
                payload[f.id] = v2;
            }
        }
    });
    return payload;
}

function saveForm() {
    fetchJson('setConfig', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(collectPayload())
    }).then(function () {
        showToast('Settings saved', 'bg-success text-white');
    }).catch(function (err) {
        showToast('Error: ' + err.message, 'bg-danger text-white');
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
    $('#subtitle').text('Firefly Luciferin settings');
    buildForm();
    fetchJson('getConfig').then(fillForm).catch(function (err) {
        showToast('Unable to load configuration: ' + err.message, 'bg-danger text-white');
    });
});
