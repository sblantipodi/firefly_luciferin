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
        const classes = new Set();
        this.classList = {
            toggle(name, enabled = !classes.has(name)) {
                if (enabled) classes.add(name);
                else classes.delete(name);
                return enabled;
            },
            contains: name => classes.has(name)
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
    style = {
        removeProperty(name) {
            delete this[name];
        },
        setProperty(name, value) {
            this[name] = value;
        }
    };

    get hidden() {
        return this.attributes.has('hidden');
    }

    set hidden(value) {
        if (value) this.setAttribute('hidden', '');
        else this.removeAttribute('hidden');
    }
}

test('shows the preview container and layout button only while the preview is open', async () => {
    const elements = Object.fromEntries(['showLivePreview', 'showLedLayout', 'screenshot',
        'livePreviewContainer', 'livePreviewFrame', 'pinLivePreview', 'livePreviewFallbackNotice'].map(id => [id, new HtmlElement()]));
    elements.livePreviewContainer.hidden = true;
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
    assert.equal(elements.livePreviewContainer.hidden, true);
    assert.equal(elements.showLedLayout.hidden, true);
    assert.equal(elements.showLedLayout.disabled, true);
    vm.runInContext('setLivePreview(true, true)', context);
    await new Promise(setImmediate);
    assert.equal(elements.livePreviewContainer.hidden, false);
    assert.equal(elements.showLedLayout.hidden, false);
    assert.equal(elements.showLedLayout.textContent, 'Show LED layout');
    assert.equal(elements.ledLayout.attributes.has('hidden'), true);
    assert.equal(timers.size, 0);
    elements.pinLivePreview.click();
    assert.equal(elements.livePreviewFrame.classList.contains('pinned'), true);
    assert.equal(elements.pinLivePreview.attributes.get('aria-pressed'), 'true');
    assert.equal(elements.pinLivePreview.title, 'Unpin live preview');
    const frame = elements.livePreviewFrame;
    elements.pinLivePreview.contains = target => target === elements.pinLivePreview;
    frame.getBoundingClientRect = () => ({top: 150});
    frame.setPointerCapture = id => assert.equal(id, 1);
    const pointer = {
        pointerId: 1, isPrimary: true, button: 0, clientY: 180, target: frame, preventDefault() {
        }
    };
    frame.pointerdown({...pointer, target: elements.pinLivePreview});
    frame.pointermove({...pointer, clientY: 200});
    assert.equal(frame.style['--preview-top'], undefined);
    frame.pointerdown(pointer);
    frame.pointermove({...pointer, clientY: 260});
    assert.equal(frame.style['--preview-top'], '230px');
    frame.lostpointercapture(pointer);
    frame.pointermove(pointer);
    assert.equal(frame.style['--preview-top'], '230px');
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
    assert.equal(elements.livePreviewContainer.hidden, true);
    assert.equal(elements.showLedLayout.hidden, true);
    assert.equal(elements.showLedLayout.disabled, true);
    assert.equal(elements.ledLayout.attributes.has('hidden'), true);
    assert.equal(timers.size, 0);
    vm.runInContext('setLivePreview(true, false)', context);
    await new Promise(setImmediate);
    assert.equal(elements.livePreviewContainer.hidden, false);
    assert.equal(elements.ledLayout.attributes.has('hidden'), false);
    assert.equal(timers.size, 2);
    assert.equal(elements.livePreviewFrame.classList.contains('pinned'), true);
    elements.pinLivePreview.click();
    assert.equal(elements.livePreviewFrame.classList.contains('pinned'), false);
    assert.equal(elements.pinLivePreview.attributes.get('aria-pressed'), 'false');
    assert.equal(elements.pinLivePreview.title, 'Pin live preview');
    assert.equal(frame.style['--preview-top'], undefined);
    vm.runInContext('setLivePreview(false, true)', context);
    assert.equal(elements.livePreviewContainer.hidden, true);
    assert.equal(timers.size, 0);
});
