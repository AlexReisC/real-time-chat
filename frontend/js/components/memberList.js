// js/components/memberList.js
// Sidebar direita com membros da sala.

import { getRoomMembers } from '../api/rooms.js';

/**
 * Monta o painel de membros e retorna um objeto com métodos de controle.
 * @param {HTMLElement} container
 * @param {string} roomId
 */
export function mountMemberList(container, roomId) {
  container.innerHTML = `
    <div class="members-panel__header">Membros</div>
    <ul class="members-list" id="members-list"></ul>
  `;

  const list = container.querySelector('#members-list');

  // Conjunto de membros atuais: userId → username
  const members = new Map();

  function renderMember(userId, username) {
    const li = document.createElement('li');
    li.className = 'member-item';
    li.dataset.userId = userId;
    li.innerHTML = `
      <div class="member-item__avatar">${username.charAt(0)}</div>
      <span class="member-item__name">${escapeHtml(username)}</span>
    `;
    list.appendChild(li);
  }

  async function loadMembers() {
    list.innerHTML = '<li style="padding:8px;font-size:12px;color:var(--text-muted);font-family:var(--font-mono)">carregando…</li>';
    try {
      const data = await getRoomMembers(roomId);
      list.innerHTML = '';
      members.clear();

      // O endpoint retorna PageResponse — content é lista de strings (userIds) ou objetos
      // Conforme a documentação, retorna lista de membros paginada
      const items = data.content ?? data ?? [];
      items.forEach((member) => {
        // O backend retorna strings (IDs) ou objetos — trata ambos
        const userId   = typeof member === 'string' ? member : member.id;
        const username = typeof member === 'string' ? member : (member.username ?? member.displayName ?? userId);
        members.set(userId, username);
        renderMember(userId, username);
      });

      if (members.size === 0) {
        list.innerHTML = '<li style="padding:8px;font-size:12px;color:var(--text-muted)">nenhum membro</li>';
      }
    } catch {
      list.innerHTML = '<li style="padding:8px;font-size:12px;color:var(--danger)">erro ao carregar</li>';
    }
  }

  /**
   * Processa notificações JOIN/LEAVE vindas do WebSocket.
   */
  function handlePresence(notification) {
    const { type, userId, username } = notification;

    if (type === 'JOIN') {
      if (!members.has(userId)) {
        members.set(userId, username);
        // Remove placeholder se existir
        const placeholder = list.querySelector('[data-placeholder]');
        if (placeholder) placeholder.remove();
        renderMember(userId, username);
      }
    } else if (type === 'LEAVE') {
      members.delete(userId);
      list.querySelector(`[data-user-id="${userId}"]`)?.remove();
      if (members.size === 0) {
        list.innerHTML = '<li style="padding:8px;font-size:12px;color:var(--text-muted)">nenhum membro</li>';
      }
    }
  }

  loadMembers();

  return { handlePresence };
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[c]);
}
