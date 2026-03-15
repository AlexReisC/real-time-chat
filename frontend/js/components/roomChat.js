// js/components/roomChat.js
// Painel de chat de sala pública.

import { subscribe, unsubscribe, publish } from '../ws/stomp.js';
import { getRoomMessages } from '../api/messages.js';
import { state } from '../state/store.js';
import { mountMemberList } from './memberList.js';
import { showToast } from './modals.js';

/**
 * Monta o chat de uma sala e retorna função de cleanup.
 * @param {HTMLElement} container
 * @param {{ id: string, title: string }} room
 * @param {function} onBack — callback para voltar à tela inicial
 */
export function mountRoomChat(container, room, onBack) {
  const msgDestination = `/topic/rooms.${room.id}`;

  container.innerHTML = `
    <div class="chat-panel" id="chat-panel">
      <div class="chat-panel__header">
        <button class="chat-panel__back" id="btn-back" title="Voltar">←</button>
        <div>
          <div class="chat-panel__title"># ${escapeHtml(room.title)}</div>
        </div>
      </div>
      <ul class="message-list" id="message-list" aria-live="polite" aria-label="Mensagens da sala"></ul>
      <div class="message-form" id="message-form">
        <textarea
          class="message-form__input"
          id="message-input"
          placeholder="Mensagem em #${escapeHtml(room.title)}"
          rows="1"
          autocomplete="off"
        ></textarea>
        <button class="message-form__send" id="btn-send" title="Enviar">➤</button>
      </div>
    </div>
    <div class="members-panel" id="members-panel"></div>
  `;

  // Inicializa sidebar de membros
  const membersPanel = container.querySelector('#members-panel');
  const memberList = mountMemberList(membersPanel, room.id);

  const list  = container.querySelector('#message-list');
  const input = container.querySelector('#message-input');
  const form  = container.querySelector('#message-form');
  const btnSend = container.querySelector('#btn-send');

  // IDs já renderizados — evita duplicatas ao mesclar histórico + WebSocket
  const renderedIds = new Set();

  // ── Renderização de mensagem ─────────────────────────
  function appendMessage(msg) {
    if (msg.id && renderedIds.has(msg.id)) return;
    if (msg.id) renderedIds.add(msg.id);

    const isMine = msg.senderId === state.currentUser?.id;

    const li = document.createElement('li');
    li.className = `message ${isMine ? 'message--mine' : ''}`;
    li.innerHTML = `
      <span style="font-size:11px;font-weight:600;color:${isMine ? 'var(--accent)' : 'var(--text-secondary)'};margin-right:8px">
        ${escapeHtml(msg.senderUsername ?? 'desconhecido')}
      </span>
      <span class="message__content">${escapeHtml(msg.content)}</span>
      <span style="font-size:10px;color:var(--text-muted);margin-left:8px;font-family:var(--font-mono)">
        ${formatTime(msg.timestamp)}
      </span>
    `;
    list.appendChild(li);
    scrollToBottom();
  }

  function appendNotification(text) {
    const li = document.createElement('li');
    li.className = 'message message--notification';
    li.textContent = text;
    list.appendChild(li);
    scrollToBottom();
  }

  function scrollToBottom() {
    list.scrollTop = list.scrollHeight;
  }

  // ── Histórico via REST ───────────────────────────────
  // Usa GET /messages/room/{roomId} — retorna List<ResponseMessageDTO> direto (sem PageResponse).
  // O backend serve via cache Redis (últimas 50), já em ordem crescente de timestamp.
  async function loadHistory() {
    list.innerHTML = '<li class="messages-loading"><div class="spinner"></div>carregando histórico…</li>';
    try {
      const messages = await getRoomMessages(room.id);
      list.innerHTML = '';
      messages.forEach(appendMessage);
    } catch {
      list.innerHTML = '<li class="messages-loading" style="color:var(--danger)">falha ao carregar histórico</li>';
    }
  }

  // ── WebSocket ────────────────────────────────────────
  subscribe(msgDestination, (payload) => {
    // Notificações de presença têm campo "type" com JOIN/LEAVE
    if (payload.type === 'JOIN' || payload.type === 'LEAVE') {
      memberList.handlePresence(payload);
      const action = payload.type === 'JOIN' ? 'entrou na sala' : 'saiu da sala';
      appendNotification(`${payload.username} ${action}`);
      return;
    }
    appendMessage(payload);
  });

  // Subscreve fila de erros privada
  subscribe(`/user/queue/errors`, (err) => {
    showToast(err.message || 'Erro no servidor', 'error');
  });

  // ── Envio de mensagem ────────────────────────────────
  function sendMessage() {
    const content = input.value.trim();
    if (!content) return;

    publish('/app/chat.sendPublic', { roomId: room.id, content });
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

  // Auto-resize do textarea
  input.addEventListener('input', () => {
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 120) + 'px';
  });

  container.querySelector('#btn-back').addEventListener('click', onBack);

  // Carrega histórico
  loadHistory();
  input.focus();

  // Cleanup — chamado ao trocar de sala/conversa
  return function unmount() {
    unsubscribe(msgDestination);
  };
}

// ── Utilitários ──────────────────────────────────────────
function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[c]);
}

function formatTime(ts) {
  if (!ts) return '';
  return new Date(ts).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}
