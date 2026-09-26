// Thin HTTP helpers for the ConfigServer endpoints (fetchJson, POST wrapper, comboChange notification) used by the other set-config modules.
export function fetchJson(url, options) {
    return fetch(url, options).then(function (r) {
        if (!r.ok) {
            return r.text().then(function (t) {
                throw new Error(t || r.statusText);
            });
        }
        return r.json();
    });
}

export function postResponse(url, body) {
    var options = {method: 'POST'};
    if (body !== undefined) {
        options.headers = {'Content-Type': 'application/json'};
        options.body = body;
    }
    return fetch(url, options).then(function (response) {
        if (!response.ok) {
            return response.text().then(function (text) {
                throw new Error(text || response.statusText);
            });
        }
        return response;
    });
}

export function notifyComboChange(name, value) {
    fetch('comboChange', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({comboName: name, value: value})
    }).catch(function () {
    });
}
