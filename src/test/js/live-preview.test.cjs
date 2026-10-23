// Regression check: SVG hidden is an attribute, not HTMLElement.hidden.
// Run with: node --test src/test/js/live-preview.test.cjs
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

class SvgElement {
    constructor() {
        this.attributes = new Map();
        this.children = [];
        this.classList = {
            toggle() {
            }
        };
    }

    setAttribute(name, value) {
        this.attributes.set(name, String(value));
    }

    removeAttribute(name) {
        this.attributes.delete(name);
    }

    appendChild(child) {
        this.children.push(child);
    }

    replaceChildren(fragment) {
        this.children = fragment.children;
    }

    addEventListener(name, callback) {
        this[name] = callback;
    }
}

class HtmlElement extends SvgElement {
    get hidden() {
        return this.attributes.has('hidden');
    }

    set hidden(value) {
        if (value) this.setAttribute('hidden', '');
        else this.removeAttribute('hidden');
    }
}

test('shows the button only with preview open and shows the SVG only after clicking', async () => {
    const elements = Object.fromEntries(['showLivePreview', 'showLedLayout', 'screenshot',
        'livePreviewFallbackNotice'].map(id => [id, new HtmlElement()]));
    elements.ledLayout = new SvgElement();
    elements.ledLayout.setAttribute('hidden', '');
    const timers = new Map();
    let timer = 0;
    const context = vm.createContext({
        console, AbortController, Promise, state: {fieldLabels: {}}, showToast() {
        },
        document: {
            getElementById: id => elements[id],
            createDocumentFragment: () => new SvgElement(),
            createElementNS: () => new SvgElement()
        },
        window: {
            webrtcPreview: {
                showVideo() {
                }
            }
        },
        clearInterval: id => timers.delete(id),
        setInterval: callback => {
            timers.set(++timer, callback);
            return timer;
        },
        fetch: async () => ({
            ok: true, json: async () => ({
                width: 1920, height: 1080,
                leds: [{number: 7, x: 0, y: 0, width: 120, height: 100, active: true}]
            })
        })
    });
    const source = fs.readFileSync(path.join(__dirname,
        '../../main/resources/org/dpsoftware/network/web/set-config-preview.js'), 'utf8')
        .replace(/^import .*;\r?\n/gm, '').replaceAll('export function', 'function');
    vm.runInContext(source, context);
    vm.runInContext('wireLivePreviewButton()', context);
    assert.equal(elements.showLedLayout.hidden, true);
    assert.equal(elements.showLedLayout.disabled, true);
    vm.runInContext('setLivePreview(true, true)', context);
    await new Promise(setImmediate);
    assert.equal(elements.showLedLayout.hidden, false);
    assert.equal(elements.showLedLayout.textContent, 'Show LED layout');
    assert.equal(elements.ledLayout.attributes.has('hidden'), true);
    assert.equal(timers.size, 0);
    elements.showLedLayout.click();
    await new Promise(setImmediate);
    assert.equal(elements.ledLayout.attributes.has('hidden'), false);
    assert.equal(elements.showLedLayout.textContent, 'Hide LED layout');
    assert.equal(elements.showLedLayout.disabled, false);
    assert.equal(elements.ledLayout.children[0].children[1].textContent, '#7');
    elements.showLedLayout.click();
    assert.equal(elements.ledLayout.attributes.has('hidden'), true);
    assert.equal(timers.size, 0);
    elements.showLedLayout.click();
    await new Promise(setImmediate);
    assert.equal(elements.ledLayout.attributes.has('hidden'), false);
    vm.runInContext('setLivePreview(false, true)', context);
    assert.equal(elements.showLedLayout.hidden, true);
    assert.equal(elements.showLedLayout.disabled, true);
    assert.equal(elements.ledLayout.attributes.has('hidden'), true);
    assert.equal(timers.size, 0);
    vm.runInContext('setLivePreview(true, false)', context);
    await new Promise(setImmediate);
    assert.equal(elements.ledLayout.attributes.has('hidden'), false);
    assert.equal(timers.size, 2);
    vm.runInContext('setLivePreview(false, true)', context);
    assert.equal(timers.size, 0);
});
