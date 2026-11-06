// Small UI utilities shared by the settings page modules: HTML escaping and Bootstrap toast notifications.
export function escapeHtml(v) {
    return String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function comboOptionsHtml(options) {
    return options.map(function (option) {
        return '<li><button type="button" class="dropdown-item" data-value="' + escapeHtml(option.value)
            + '">' + escapeHtml(option.label) + '</button></li>';
    }).join('');
}

// Keeps editable dropdowns the same total width as a normal text field.
export function buildEditableComboHtml(id, label, options = []) {
    return '<label for="' + id + '">' + escapeHtml(label)
        + '</label><div class="dropdown device-selector">'
        + '<input type="text" class="form-control" id="' + id + '" autocomplete="off">'
        + '<button type="button" class="btn btn-outline-secondary dropdown-toggle" id="' + id + 'Toggle"'
        + ' data-bs-toggle="dropdown" data-bs-reference="parent" aria-expanded="false" aria-controls="' + id + 'List" aria-label="' + escapeHtml(label) + '"></button>'
        + '<ul class="dropdown-menu w-100" id="' + id + 'List" aria-labelledby="' + id + 'Toggle">'
        + comboOptionsHtml(options) + '</ul></div>';
}

// Refreshes discovered options without clearing the input or disturbing an unchanged menu.
export function updateEditableComboOptions(id, options) {
    var list = document.getElementById(id + 'List');
    if (list.children.length === options.length && options.every(function (option, index) {
        var button = list.children[index].firstElementChild;
        return button.dataset.value === String(option.value) && button.textContent === String(option.label);
    })) {
        return;
    }
    list.innerHTML = comboOptionsHtml(options);
}

export function showToast(message, contextClass) {
    if (!document.getElementById('toastContainer')) {
        document.body.insertAdjacentHTML('beforeend', '<div id="toastContainer"><div></div></div>');
    }
    var container = document.getElementById('toastContainer').children[0];
    container.insertAdjacentHTML('beforeend', '<div class="toast ' + contextClass + '" role="alert" aria-live="assertive" aria-atomic="true"><div class="toast-body">' + message + '</div></div>');
    $($(container.lastElementChild)).toast('show');
}
