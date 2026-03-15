// js/components/privateChat.js
// Painel de conversa privada.

import { subscribe, unsubscribe, publish } from '../ws/stomp.js';
import { getPrivateMessages } from '../api/messages.js';
import { state } from '../state/store.js';
import { showToast } from './modals.js';

/**
 * Monta o chat privado e retorna função de cleanup.
 * @param {HTMLElement} container
 * @param {{ userId: string, username: string }} contact
 * @param {function} onBack
 */
export function mountPrivateChat(container, contact, onBack) {
  // Mensagens privadas chegam nessa fila para o usuário logado
  const privateDestination = `/user/queue/private`;

  container.innerHTML = `
    <div class="chat-panel" style="border-right:none">
      <div class="chat-panel__header">
        <button class="chat-panel__back" id="btn-back" title="Voltar">←</button>
        <div>
          <div class="chat-panel__title">@ ${escapeHtml(contact.username)}</div>
          <div class="chat-panel__subtitle">mensagem direta</div>
        </div>
      </div>
      <ul class="message-list" id="message-list" aria-live="polite"></ul>
      <div class="message-form" id="message-form">
        <textarea
          class="message-form__input"
          id="message-input"
          placeholder="Mensagem para @${escapeHtml(contact.username)}"
          rows="1"
          autocomplete="off"
        ></textarea>
        <button class="message-form__send" id="btn-send" title="Enviar">➤</button>
      </div>
    </div>
  `;

  const list  = container.querySelector('#message-list');
  const input = container.querySelector('#message-input');
  const btnSend = container.querySelector('#btn-send');

  const renderedIds = new Set();

  // ── Renderização ─────────────────────────────────────
  function appendMessage(msg) {
    if (msg.id && renderedIds.has(msg.id)) return;
    if (msg.id) renderedIds.add(msg.id);

    const isMine = msg.senderId === state.currentUser?.id;

    const li = document.createElement('li');
    li.className = `message ${isMine ? 'message--mine' : ''}`;
    li.innerHTML = `
      <span style="font-size:11px;font-weight:600;color:${isMine ? 'var(--accent)' : 'var(--text-secondary)'};margin-right:8px">
        ${escapeHtml(msg.senderUsername ?? (isMine ? state.currentUser.displayName : contact.username))}
      </span>
      <span class="message__content">${escapeHtml(msg.content)}</span>
      <span style="font-size:10px;color:var(--text-muted);margin-left:8px;font-family:var(--font-mono)">
        ${formatTime(msg.timestamp)}
      </span>
    `;
    list.appendChild(li);
    list.scrollTop = list.scrollHeight;
  }

  // ── Histórico ────────────────────────────────────────
  // Usa GET /messages/private/{targetUserId} — retorna List<ResponseMessageDTO> direto.
  // O backend serve via cache Redis (últimas 50), já em ordem crescente de timestamp.
  async function loadHistory() {
    list.innerHTML = '<li class="messages-loading"><div class="spinner"></div>carregando…</li>';
    try {
      const messages = await getPrivateMessages(contact.userId);
      list.innerHTML = '';
      messages.forEach(appendMessage);
    } catch {
      list.innerHTML = '<li class="messages-loading" style="color:var(--danger)">falha ao carregar histórico</li>';
    }
  }

  // ── WebSocket ────────────────────────────────────────
  // A subscrição da fila privada é compartilhada — filtra pelo remetente correto
  subscribe(privateDestination, (msg) => {
    const isFromContact = msg.senderId === contact.userId;
    const isFromMe      = msg.senderId === state.currentUser?.id && msg.recipientId === contact.userId;

    if (isFromContact || isFromMe) {
      appendMessage(msg);
    }
  });

  subscribe('/user/queue/errors', (err) => {
    showToast(err.message || 'Erro no servidor', 'error');
  });

  // ── Envio ────────────────────────────────────────────
  function sendMessage() {
    const content = input.value.trim();
    if (!content) return;

    publish('/app/chat.sendPrivate', {
      recipientId: contact.userId,
      content,
    });

    input.value = '';
    input.style.height = 'auto';
    input.focus();
  }

  btnSend.addEventListener('click', sendMessage);

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

  input.addEventListener('input', () => {
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 120) + 'px';
  });

  container.querySelector('#btn-back').addEventListener('click', onBack);

  loadHistory();
  input.focus();

  return function unmount() {
    unsubscribe(privateDestination);
  };
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[c]);
}

function formatTime(ts) {
  if (!ts) return '';
  return new Date(ts).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}
