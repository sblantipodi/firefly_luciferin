// Run with: node --test src/test/js/color-picker.test.cjs
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function setup(effect = 'Bias light') {
    const styleProps = {};
    const classes = new Set();
    const elements = {
        picker: {
            classList: {
                add(c) {
                    classes.add(c);
                },
                remove(c) {
                    classes.delete(c);
                },
                contains(c) {
                    return classes.has(c);
                },
                toggle(c, force) {
                    if (force === undefined) force = !classes.has(c);
                    if (force) classes.add(c); else classes.delete(c);
                    return force;
                }
            },
            style: {
                setProperty(name, value) {
                    styleProps[name] = value;
                }
            }
        },
        settingsLogo: {
            listeners: {}, attributes: {},
            addEventListener(name, callback) { this.listeners[name] = callback; },
            setAttribute(name, value) { this.attributes[name] = value; },
            removeAttribute(name) { delete this.attributes[name]; },
            click() { return this.listeners.click(); }
        },
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
    context.wireSettingsLogo();
    return {elements, handlers, requests, messages, context, finishEffect, styleProps, classes};
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

test('the logo toggles Bias light and Solid, ignoring duplicate clicks and stale prefs', async () => {
    const {elements, requests, context, finishEffect} = setup();
    const logo = elements.settingsLogo;
    const pending = logo.click();
    await logo.click();
    assert.deepEqual(requests, [{url: 'comboChange', body: {comboName: 'effectSelect', value: 'Solid'}}]);
    assert.equal(logo.attributes['aria-disabled'], 'true');
    finishEffect({ok: true});
    await pending;
    assert.equal(elements.effectSelect.value, 'Solid');
    assert.equal(logo.attributes['aria-disabled'], undefined);
    context.applyPrefs({toggle: '1', cp: '255,0,0', effect: 'Illuminazione Bias'});
    assert.equal(elements.effectSelect.value, 'Solid');

    await logo.click();
    assert.equal(elements.effectSelect.value, 'Bias light');
    assert.deepEqual(requests.filter(request => request.url === 'comboChange').map(request => request.body.value),
        ['Solid', 'Bias light']);
    assert.equal(requests.some(request => request.url.startsWith('deviceState')), false);
});

test('logo errors preserve the selected effect and re-enable clicking; other effects stay unchanged', async () => {
    const {elements, requests, messages, finishEffect} = setup('Solid');
    const pending = elements.settingsLogo.click();
    finishEffect({ok: false, text: async () => 'Effect unavailable'});
    await pending;
    assert.equal(elements.effectSelect.value, 'Solid');
    assert.equal(elements.settingsLogo.attributes['aria-disabled'], undefined);
    assert.deepEqual(messages, ['Unable to change effect: Effect unavailable']);
    elements.effectSelect.value = 'Rainbow';
    await elements.settingsLogo.click();
    assert.equal(elements.effectSelect.value, 'Rainbow');
    assert.equal(requests.length, 1);
});

test('Enter and Space activate the logo without scrolling or submitting the form', () => {
    const {elements} = setup();
    let clicks = 0;
    let prevented = 0;
    elements.settingsLogo.click = () => { clicks++; };
    for (const key of ['Enter', ' ', 'Escape']) {
        elements.settingsLogo.listeners.keydown({key, preventDefault() { prevented++; }});
    }
    assert.equal(clicks, 2);
    assert.equal(prevented, 2);
});

test('color picker updates the glow custom properties on init, input change and prefs sync', () => {
    const {handlers, context, styleProps} = setup();
    assert.equal(styleProps['--picker-glow-rgb'], '255, 38, 0');
    assert.equal(styleProps['--picker-glow-color'], 'rgb(255, 38, 0)');

    handlers['input:change']({rgb: {r: 0, g: 145, b: 255}});
    assert.equal(styleProps['--picker-glow-rgb'], '0, 145, 255');
    assert.equal(styleProps['--picker-glow-color'], 'rgb(0, 145, 255)');

    context.applyPrefs({toggle: '1', cp: '120,250,50', effect: 'Solid'});
    assert.equal(styleProps['--picker-glow-rgb'], '120, 250, 50');
    assert.equal(styleProps['--picker-glow-color'], 'rgb(120, 250, 50)');
});

test('glow fades away by adding lights-off class when LEDs are turned off and restores it when on', () => {
    const {context, classes} = setup();
    assert.equal(classes.has('lights-off'), false);

    context.applyPrefs({toggle: '0', cp: '120,250,50', effect: 'Solid'});
    assert.equal(classes.has('lights-off'), true);

    context.applyPrefs({toggle: '1', cp: '120,250,50', effect: 'Solid'});
    assert.equal(classes.has('lights-off'), false);
});
