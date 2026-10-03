// Small UI utilities shared by the settings page modules: HTML escaping and Bootstrap toast notifications.
export function escapeHtml(v) {
    return String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

export function showToast(message, contextClass) {
    if (!document.getElementById('toastContainer')) {
        document.body.insertAdjacentHTML('beforeend', '<div id="toastContainer"><div></div></div>');
    }
    var container = document.getElementById('toastContainer').children[0];
    container.insertAdjacentHTML('beforeend', '<div class="toast ' + contextClass + '" role="alert" aria-live="assertive" aria-atomic="true"><div class="toast-body">' + message + '</div></div>');
    $($(container.lastElementChild)).toast('show');
}
