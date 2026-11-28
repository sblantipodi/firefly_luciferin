const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const webRoot = path.join(__dirname, '../../main/resources/org/dpsoftware/network/web');

function readModule(filename) {
    return fs.readFileSync(path.join(webRoot, filename), 'utf8')
        .replace(/^import[\s\S]*?from '[^']+';\r?\n/gm, '')
        .replace(/^export /gm, '');
}

function controlFromMarkup(markup, tag) {
    const [, id] = markup.match(new RegExp('<' + tag + '\\b[^>]*\\bid="([^"]+)"'));
    const listeners = new Map();
    return {
        id,
        addEventListener(type, listener) {
            listeners.set(type, listener);
        },
        click() {
            return listeners.get('click')?.({currentTarget: this});
        }
    };
}

test('the Devices button sends the update request independently of the automatic update checkbox', async () => {
    const controls = [];
    const requests = [];
    const context = vm.createContext({
        $() {
        },
        state: {fieldLabels: {checkForUpdates: 'Aggiornamenti', 'web.checkForUpdates': 'Aggiornamento'}},
        escapeHtml: value => String(value ?? ''),
        document: {getElementById: id => controls.find(control => control.id === id)},
        fetch: async (url, options) => {
            requests.push({url, method: options.method, body: options.body});
            return {ok: true};
        },
        showToast() {
        }
    });
    const sections = vm.runInContext(readModule('set-config-schema.js') + '\nsections', context);
    vm.runInContext(readModule('set-config-core.js'), context);
    const mode = sections.find(section => section.id === 'mode');
    const devices = sections.find(section => section.id === 'devices');
    const updateSetting = mode.fields.find(field => field.id === 'checkForUpdates');
    const checkbox = controlFromMarkup(context.buildFieldHtml(updateSetting), 'input');
    const updateAction = devices.fields.at(-1);
    assert.equal(updateAction.id, 'checkForUpdatesButton');
    assert.equal(devices.fields.at(-2).id, 'baudRate');
    const updateMarkup = context.buildFieldsGrid([updateAction]);
    const button = controlFromMarkup(updateMarkup, 'button');
    assert.match(updateMarkup, /class="col-12 col-md-6 col-lg-3" id="field-checkForUpdatesButton"/);
    assert.match(updateMarkup, /<label[^>]*for="checkForUpdatesButton">Aggiornamenti<\/label><button\b/);
    assert.match(updateMarkup, /class="btn btn-outline-primary\b[^"]*">Aggiornamento<\/button>/);
    const html = fs.readFileSync(path.join(webRoot, 'set-config.html'), 'utf8');
    const infoMarkup = html.match(/<template id="infoTemplate">([\s\S]*?)<\/template>/)[1];
    assert.doesNotMatch(infoMarkup, /id="checkForUpdatesButton"/);
    assert.notEqual(button.id, checkbox.id);
    controls.push(checkbox, button);

    vm.runInContext(readModule('set-config-api.js'), context);
    vm.runInContext(readModule('set-config-app.js'), context);
    context.wireUpdateButton();

    await checkbox.click();
    assert.deepEqual(requests, []);
    await button.click();
    assert.deepEqual(requests, [{url: 'checkForUpdates', method: 'POST', body: '{}'}]);
    assert.equal(button.disabled, false);
});
