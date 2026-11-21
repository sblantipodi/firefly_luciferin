// Run with: node --test src/test/js/cube-lut.test.cjs
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

function setup(selected = 'custom.cube') {
    const elements = Object.fromEntries(['uploadCubeLut', 'removeCubeLut', 'cubeLut', 'cubeLutFile'].map(id => [id, {
        value: '', disabled: false, hidden: false, listeners: {}, options: [],
        addEventListener(name, callback) { this.listeners[name] = callback; },
        dispatchEvent(event) { this.listeners[event.type]?.(event); },
        replaceChildren() { this.options = []; },
        add(option) { this.options.push(option); },
        click() { this.listeners.click?.(); }
    }]));
    elements.cubeLut.value = selected;
    const errors = [];
    const removed = [];
    const context = vm.createContext({
        state: {fieldOptions: {userCubeLuts: {options: [{value: 'custom.cube'}]}}},
        document: {getElementById: id => elements[id]},
        $() {}, Event, Option: function (label, value) { this.label = label; this.value = value; },
        escapeHtml: value => value, showToast: error => errors.push(error),
        removeCubeLut: async filename => {
            removed.push(filename);
            return {filename: 'Disabled', luts: ['Disabled', 'default.cube'], userLuts: []};
        },
        uploadCubeLut: async () => ({
            filename: 'custom.cube', luts: ['Disabled', 'default.cube', 'custom.cube'], userLuts: ['custom.cube']
        })
    });
    const source = fs.readFileSync(path.join(__dirname,
        '../../main/resources/org/dpsoftware/network/web/set-config-app.js'), 'utf8')
        .replace(/^import[\s\S]*?from '[^']+';\r?\n/gm, '');
    vm.runInContext(source, context);
    vm.runInContext('wireLutUpload()', context);
    return {elements, errors, removed, context};
}

test('Remove appears only for a selected user LUT and removal selects Disabled', async () => {
    const {elements, removed, errors} = setup();
    assert.equal(elements.removeCubeLut.hidden, false);
    for (const value of ['default.cube', 'Disabled', 'missing.cube']) {
        elements.cubeLut.value = value;
        elements.cubeLut.dispatchEvent(new Event('change'));
        assert.equal(elements.removeCubeLut.hidden, true);
    }
    elements.cubeLut.value = 'custom.cube';
    elements.cubeLut.dispatchEvent(new Event('change'));
    assert.equal(elements.removeCubeLut.hidden, false);
    elements.removeCubeLut.click();
    assert.equal(elements.uploadCubeLut.disabled, true);
    await new Promise(setImmediate);
    assert.deepEqual(removed, ['custom.cube']);
    assert.equal(elements.cubeLut.value, 'Disabled');
    assert.equal(elements.removeCubeLut.hidden, true);
    assert.equal(elements.uploadCubeLut.disabled, false);
    assert.deepEqual(elements.cubeLut.options.map(option => option.value), ['Disabled', 'default.cube']);
    assert.deepEqual(errors, []);
});

test('upload selects the new user LUT and exposes Remove immediately', async () => {
    const {elements} = setup('Disabled');
    elements.cubeLutFile.files = [{name: 'custom.cube'}];
    elements.cubeLutFile.dispatchEvent(new Event('change'));
    await new Promise(setImmediate);
    assert.equal(elements.cubeLut.value, 'custom.cube');
    assert.equal(elements.removeCubeLut.hidden, false);
    assert.equal(elements.cubeLut.disabled, false);
    assert.equal(elements.cubeLutFile.value, '');
});

test('a removal error retains the selected LUT and re-enables controls', async () => {
    const {elements, errors, context} = setup();
    context.removeCubeLut = async () => { throw new Error('Unable to remove LUT file'); };
    elements.removeCubeLut.click();
    await new Promise(setImmediate);
    assert.equal(elements.cubeLut.value, 'custom.cube');
    assert.equal(elements.removeCubeLut.hidden, false);
    assert.equal(elements.removeCubeLut.disabled, false);
    assert.equal(elements.uploadCubeLut.disabled, false);
    assert.equal(elements.cubeLut.disabled, false);
    assert.deepEqual(errors, ['Error: Unable to remove LUT file']);
});
