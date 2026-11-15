// Run with: node --test src/test/js/color-picker.test.cjs
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function setup(effect = 'Bias light') {
    const elements = {
        picker: {},
        effectSelect: {
            value: effect,
            options: [
                {value: 'Bias light', label: 'Illuminazione Bias'},
                {value: 'Solid', label: 'Solido'},
                {value: 'Rainbow', label: 'Arcobaleno'}
            ]
        }
    };
    const handlers = {};
    const requests = [];
    const messages = [];
    let finishEffect;
    const effectResponse = new Promise(resolve => { finishEffect = resolve; });
    const context = vm.createContext({
        console: {log() {}}, navigator: {language: 'it-IT'},
        state: {fieldLabels: {}, lastConfig: {outputDevice: 'AUTO'}, devices: [{deviceIP: '192.168.1.20'}]},
        document: {getElementById: id => elements[id]},
        iro: {ColorPicker: function () {
            this.color = {};
            this.on = (event, callback) => { handlers[String(event)] = callback; };
        }},
        showToast: message => messages.push(message),
        setInterval() {}, clearInterval() {},
        fetch: async (url, options) => {
            requests.push({url, body: options?.body && JSON.parse(options.body)});
            if (url === 'comboChange') return effectResponse;
            return {ok: true, json: async () => ({})};
        }
    });
    for (const file of ['set-config-api.js', 'set-config-device.js']) {
        const source = fs.readFileSync(path.join(__dirname,
            '../../main/resources/org/dpsoftware/network/web', file), 'utf8')
            .replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '');
        vm.runInContext(source, context);
    }
    context.initColorPicker();
    return {elements, handlers, requests, messages, context, finishEffect};
}

const color = {rgb: {r: 10, g: 20, b: 30}};

test('Bias light switches to Solid successfully before sending the chosen color', async () => {
    const {elements, handlers, requests, finishEffect} = setup();
    handlers['input:start']();
    handlers['input:change'](color);
    const pending = handlers['input:end'](color);
    assert.deepEqual(requests, [{url: 'comboChange', body: {comboName: 'effectSelect', value: 'Solid'}}]);
    finishEffect({ok: true});
    await pending;
    assert.equal(elements.effectSelect.value, 'Solid');
    assert.deepEqual(requests[1], {
        url: 'deviceState?ip=192.168.1.20',
        body: {state: 'ON', color: color.rgb, whitetemp: 65}
    });
});

test('other selected effects send only the color and keep their selection', async () => {
    for (const effect of ['Solid', 'Rainbow']) {
        const {elements, handlers, requests} = setup(effect);
        await handlers['input:end'](color);
        assert.equal(requests[0].url, 'deviceState?ip=192.168.1.20');
        assert.equal(requests.filter(request => request.url === 'comboChange').length, 0);
        assert.equal(elements.effectSelect.value, effect);
    }
});

test('a failed effect change does not send the color and allows retrying from Bias light', async () => {
    const {elements, handlers, requests, messages, finishEffect} = setup();
    const pending = handlers['input:end'](color);
    finishEffect({ok: false, text: async () => 'Effect unavailable'});
    await pending;
    assert.equal(requests.length, 1);
    assert.equal(elements.effectSelect.value, 'Bias light');
    assert.deepEqual(messages, ['Unable to change effect: Effect unavailable']);
});

test('localized prefs select the base effect and stale polling cannot undo the Solid selection', async () => {
    const {elements, handlers, requests, context, finishEffect} = setup('Solid');
    context.applyPrefs({toggle: '1', cp: '255,0,0', effect: 'Illuminazione Bias'});
    assert.equal(elements.effectSelect.value, 'Bias light');
    assert.equal(requests.length, 0);
    const pending = handlers['input:end'](color);
    finishEffect({ok: true});
    await pending;
    context.applyPrefs({toggle: '1', cp: '255,0,0', effect: 'Illuminazione Bias'});
    assert.equal(elements.effectSelect.value, 'Solid');
    assert.deepEqual(JSON.parse(JSON.stringify(context.lastColor)), color.rgb);
});
