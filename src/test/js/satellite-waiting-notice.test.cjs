const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

test('status polling shows satellite wait alongside update notices and clears it on recovery', async () => {
    const elements = Object.fromEntries(['satelliteWaitingNotice', 'fireflyUpdateNotice',
        'glowWormUpdateAvailableNotice', 'glowWormUpdateNotice'].map(id => [id, {hidden: true}]));
    let status = {
        waitingForSatellites: true, fireflyUpdateAvailable: true,
        glowWormUpdateAvailable: true, glowWormUpdateInProgress: false
    };
    const context = vm.createContext({
        AbortController, setTimeout: () => 1, clearTimeout() {
        },
        document: {getElementById: id => elements[id]},
        fetchJson: async () => status, showToast() {
        }
    });
    const source = fs.readFileSync(path.join(__dirname,
        '../../main/resources/org/dpsoftware/network/web/set-config-status.js'), 'utf8')
        .replace(/^import .*;\r?\n/gm, '').replace(/^export /gm, '');
    vm.runInContext(source, context);
    // A newly opened page must pick up an already active wait on its first poll.
    await context.pollServerStatus();
    assert.equal(elements.satelliteWaitingNotice.hidden, false);
    assert.equal(elements.fireflyUpdateNotice.hidden, false);
    assert.equal(elements.glowWormUpdateAvailableNotice.hidden, false);
    assert.equal(elements.glowWormUpdateNotice.hidden, true);

    status = {...status, waitingForSatellites: false};
    await context.pollServerStatus();
    assert.equal(elements.satelliteWaitingNotice.hidden, true);
    assert.equal(elements.fireflyUpdateNotice.hidden, false);

    // Older servers without the new field must keep the notice hidden.
    delete status.waitingForSatellites;
    await context.pollServerStatus();
    assert.equal(elements.satelliteWaitingNotice.hidden, true);
});
