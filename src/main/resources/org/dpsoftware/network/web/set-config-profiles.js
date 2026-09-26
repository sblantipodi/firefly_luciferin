// Configuration profiles UI: renders the profile list, activates a profile (server restart) and adds/removes profiles via the ConfigServer endpoints.
import {fetchJson, postResponse} from './set-config-api.js';
import {collectPayload} from './set-config-core.js';
import {showToast} from './set-config-ui.js';

// Renders the profile list with activate buttons and (for non-default, non-active profiles) a remove button, marking the active profile.
export function renderProfiles(data) {
    var container = document.getElementById('profilesList');
    if (!container) {
        return;
    }
    var profiles = data.profiles || [];
    var activeProfile = data.activeProfile || '';
    if (profiles.length === 0) {
        container.innerHTML = '<div class="text-muted">No profiles available</div>';
        return;
    }
    var defaultName = 'Default';
    container.replaceChildren();
    profiles.forEach(function (name) {
        var isActive = name === activeProfile;
        var row = document.getElementById('profileRowTemplate').content.firstElementChild.cloneNode(true);
        var activateButton = row.querySelector('.list-group-item-action');
        activateButton.textContent = name;
        if (isActive) {
            activateButton.append(' ', document.getElementById('activeProfileBadgeTemplate').content.cloneNode(true));
        }
        activateButton.addEventListener('click', function () {
            activateProfile(name);
        });
        var removeButton = row.querySelector('.btn-outline-danger');
        if (name !== defaultName && !isActive) {
            removeButton.addEventListener('click', function (event) {
                event.stopPropagation();
                removeProfile(name);
            });
        } else {
            removeButton.remove();
        }
        container.appendChild(row);
    });
}

// Activates a profile via the server endpoint (with confirmation); the server restarts to apply it.
export function activateProfile(name) {
    var confirmMsg = 'Activate profile "' + name + '"? Firefly will restart.';
    if (!confirm(confirmMsg)) {
        return;
    }
    postResponse('activateProfile?name=' + encodeURIComponent(name)).then(function () {
        showToast('Activating profile: ' + name, 'bg-info text-white');
    }).catch(function (err) {
        showToast('Unable to activate profile: ' + err.message, 'bg-danger text-white');
    });
}

// Creates a new profile from the current form values (collectPayload) and refreshes the list afterwards.
export function addProfile() {
    var name = document.getElementById('newProfileName').value.trim();
    if (!name) {
        showToast('Enter a profile name', 'bg-warning text-dark');
        return;
    }
    var payload;
    try {
        payload = collectPayload();
    } catch (e) {
        showToast('Collect error: ' + e.message, 'bg-danger text-white');
        console.error('collectPayload failed', e);
        return;
    }
    var body = JSON.stringify(payload);
    postResponse('addProfile?name=' + encodeURIComponent(name), body).then(function () {
        showToast('Profile "' + name + '" added', 'bg-success text-white');
        document.getElementById('newProfileName').value = '';
        return fetchJson('listProfiles');
    }).then(function (data) {
        renderProfiles(data);
    }).catch(function (err) {
        showToast('Unable to add profile: ' + err.message, 'bg-danger text-white');
    });
}

// Removes a profile via the server endpoint (with confirmation) and refreshes the list afterwards.
export function removeProfile(name) {
    var confirmMsg = 'Remove profile "' + name + '"? This cannot be undone.';
    if (!confirm(confirmMsg)) {
        return;
    }
    postResponse('removeProfile?name=' + encodeURIComponent(name)).then(function () {
        showToast('Profile "' + name + '" removed', 'bg-success text-white');
        return fetchJson('listProfiles');
    }).then(function (data) {
        renderProfiles(data);
    }).catch(function (err) {
        showToast('Unable to remove profile: ' + err.message, 'bg-danger text-white');
    });
}
