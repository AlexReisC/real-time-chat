// js/components/modals.js
// Utilitários para modais e toasts.

// ── Toast ────────────────────────────────────────────────

function getToastContainer() {
  let el = document.getElementById('toast-container');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast-container';
    document.body.appendChild(el);
  }
  return el;
}

/**
 * Exibe um toast temporário.
 * @param {string} message
 * @param {'error'|'success'|'info'} type
 * @param {number} duration — ms
 */
export function showToast(message, type = 'info', duration = 3500) {
  const container = getToastContainer();
  const toast = document.createElement('div');
  toast.className = `toast toast--${type}`;
  toast.textContent = message;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.animation = 'fadeOut 200ms ease forwards';
    setTimeout(() => toast.remove(), 200);
  }, duration);
}

// ── Modal genérico ───────────────────────────────────────

/**
 * Cria e exibe um modal.
 * Retorna uma Promise que resolve com true (confirmado) ou false (cancelado/fechado).
 *
 * @param {object} options
 * @param {string} options.title
 * @param {string} options.bodyHtml  — HTML interno do corpo
 * @param {string} options.confirmLabel
 * @param {string} options.confirmClass — classe CSS do botão de confirmação
 * @param {boolean} options.showCancel
 */
export function openModal({
  title,
  bodyHtml,
  confirmLabel = 'Confirmar',
  confirmClass = 'btn--primary',
  showCancel = true,
}) {
  return new Promise((resolve) => {
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop';

    backdrop.innerHTML = `
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <h2 class="modal__title" id="modal-title">${title}</h2>
        <div class="modal__body">${bodyHtml}</div>
        <div class="modal__actions">
          ${showCancel ? '<button class="btn btn--ghost" id="modal-cancel">Cancelar</button>' : ''}
          <button class="btn ${confirmClass}" id="modal-confirm">${confirmLabel}</button>
        </div>
      </div>
    `;

    document.body.appendChild(backdrop);

    function close(result) {
      backdrop.remove();
      resolve(result);
    }

    backdrop.querySelector('#modal-confirm').addEventListener('click', () => close(true));
    backdrop.querySelector('#modal-cancel')?.addEventListener('click', () => close(false));

    // Fecha ao clicar fora
    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) close(false);
    });

    // Fecha com Escape
    function onKey(e) {
      if (e.key === 'Escape') { close(false); document.removeEventListener('keydown', onKey); }
    }
    document.addEventListener('keydown', onKey);
  });
}

/**
 * Modal de confirmação simples.
 */
export function confirmModal(title, message, { danger = false } = {}) {
  return openModal({
    title,
    bodyHtml: `<p class="modal__confirm-text">${message}</p>`,
    confirmLabel: 'Confirmar',
    confirmClass: danger ? 'btn--danger' : 'btn--primary',
  });
}

/**
 * Modal com um campo de texto.
 * Retorna a string digitada ou null se cancelado.
 */
export async function promptModal(title, label, placeholder = '') {
  const bodyHtml = `
    <div class="modal-form">
      <div class="form-field">
        <label class="form-field__label" for="modal-input">${label}</label>
        <input class="input" id="modal-input" type="text" placeholder="${placeholder}" autocomplete="off" />
      </div>
    </div>
  `;

  return new Promise((resolve) => {
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop';

    backdrop.innerHTML = `
      <div class="modal" role="dialog" aria-modal="true">
        <h2 class="modal__title">${title}</h2>
        <div class="modal__body">${bodyHtml}</div>
        <div class="modal__actions">
          <button class="btn btn--ghost" id="modal-cancel">Cancelar</button>
          <button class="btn btn--primary" id="modal-confirm">Confirmar</button>
        </div>
      </div>
    `;

    document.body.appendChild(backdrop);

    const input = backdrop.querySelector('#modal-input');
    // Foca automaticamente
    requestAnimationFrame(() => input.focus());

    function close(result) {
      backdrop.remove();
      resolve(result);
    }

    backdrop.querySelector('#modal-confirm').addEventListener('click', () => {
      const value = input.value.trim();
      if (!value) { input.focus(); return; }
      close(value);
    });

    backdrop.querySelector('#modal-cancel').addEventListener('click', () => close(null));
    backdrop.addEventListener('click', (e) => { if (e.target === backdrop) close(null); });

    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const value = input.value.trim();
        if (value) close(value);
      }
      if (e.key === 'Escape') close(null);
    });
  });
}

/**
 * Modal de edição de perfil (username + senha).
 */
export function openProfileModal(currentDisplayName, onSave) {
  const backdrop = document.createElement('div');
  backdrop.className = 'modal-backdrop';

  backdrop.innerHTML = `
    <div class="modal" role="dialog" aria-modal="true">
      <h2 class="modal__title">Editar perfil</h2>
      <div class="modal__body">
        <div class="modal-form" id="profile-form">

          <p style="font-size:12px;color:var(--text-muted);margin-bottom:4px;font-family:var(--font-mono)">NOME DE EXIBIÇÃO</p>
          <input class="input" id="profile-name" type="text" value="${escapeAttr(currentDisplayName)}" />
          <span id="profile-name-error" style="font-size:12px;color:var(--danger);min-height:16px;display:block"></span>

          <hr style="border:none;border-top:1px solid var(--border);margin:8px 0" />

          <p style="font-size:12px;color:var(--text-muted);margin-bottom:4px;font-family:var(--font-mono)">ALTERAR SENHA <span style="font-size:11px;font-style:italic">(opcional)</span></p>
          <input class="input" id="profile-cur-pass"  type="password" placeholder="Senha atual" />
          <input class="input" id="profile-new-pass"  type="password" placeholder="Nova senha (mín. 8 caracteres)" style="margin-top:8px" />
          <span id="profile-pass-error" style="font-size:12px;color:var(--danger);min-height:16px;display:block"></span>
        </div>
      </div>
      <div class="modal__actions">
        <button class="btn btn--ghost" id="modal-cancel">Cancelar</button>
        <button class="btn btn--primary" id="modal-confirm">Salvar</button>
      </div>
    </div>
  `;

  document.body.appendChild(backdrop);

  function close() { backdrop.remove(); }

  backdrop.querySelector('#modal-cancel').addEventListener('click', close);
  backdrop.addEventListener('click', (e) => { if (e.target === backdrop) close(); });

  backdrop.querySelector('#modal-confirm').addEventListener('click', async () => {
    const nameInput   = backdrop.querySelector('#profile-name');
    const curPass     = backdrop.querySelector('#profile-cur-pass').value;
    const newPass     = backdrop.querySelector('#profile-new-pass').value;
    const nameError   = backdrop.querySelector('#profile-name-error');
    const passError   = backdrop.querySelector('#profile-pass-error');

    nameError.textContent = '';
    passError.textContent = '';

    const name = nameInput.value.trim();
    if (!name) { nameError.textContent = 'Nome não pode ser vazio'; return; }

    if ((curPass || newPass) && !(curPass && newPass)) {
      passError.textContent = 'Preencha a senha atual e a nova senha';
      return;
    }

    if (newPass && newPass.length < 8) {
      passError.textContent = 'Nova senha deve ter ao menos 8 caracteres';
      return;
    }

    const btn = backdrop.querySelector('#modal-confirm');
    btn.disabled = true;
    btn.textContent = 'Salvando…';

    try {
      await onSave({
        displayName: name,
        currentPassword: curPass || null,
        newPassword: newPass || null,
      });
      close();
    } catch (err) {
      passError.textContent = err.message;
      btn.disabled = false;
      btn.textContent = 'Salvar';
    }
  });
}

function escapeAttr(str) {
  return String(str).replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
