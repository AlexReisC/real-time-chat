// js/components/sidebar.js
// Sidebar esquerda: lista de salas, conversas privadas e perfil do usuário.

import { listRooms, createRoom, deleteRoom, leaveRoom } from '../api/rooms.js';
import { getPrivateConversations } from '../api/messages.js';
import { updateUsername, updatePassword } from '../api/auth.js';
import { state } from '../state/store.js';
import { promptModal, confirmModal, showToast, openProfileModal } from './modals.js';

/**
 * Monta a sidebar e retorna objeto de controle.
 *
 * @param {HTMLElement} container
 * @param {object} callbacks
 * @param {function} callbacks.onRoomSelect      — (room) => void
 * @param {function} callbacks.onPrivateSelect   — (contact) => void
 * @param {function} callbacks.onRoomDeleted     — (roomId) => void
 * @param {function} callbacks.onPrivateDeleted  — (userId) => void
 */
export async function mountSidebar(container, { onRoomSelect, onPrivateSelect, onRoomDeleted, onPrivateDeleted }) {
  const user = state.currentUser;

  container.innerHTML = `
    <div class="sidebar__header">
      <div class="sidebar__logo-mark">ch</div>
      <span class="sidebar__logo-name">chat.app</span>
    </div>

    <nav class="sidebar__nav" id="sidebar-nav">
      <!-- Salas -->
      <div class="nav-section" id="rooms-section">
        <div class="nav-section__header">
          <span class="nav-section__label">Salas</span>
          <button class="btn--icon" id="btn-new-room" title="Nova sala" style="font-size:16px">+</button>
        </div>
        <ul id="rooms-list" style="list-style:none;padding:0"></ul>
      </div>

      <!-- Conversas privadas -->
      <div class="nav-section" id="privates-section">
        <div class="nav-section__header">
          <span class="nav-section__label">Mensagens diretas</span>
        </div>
        <ul id="privates-list" style="list-style:none;padding:0">
          <li style="padding:6px 8px;font-size:12px;color:var(--text-muted)" data-placeholder>nenhuma conversa</li>
        </ul>
      </div>
    </nav>

    <div class="sidebar__footer">
      <div class="user-profile" id="btn-profile" role="button" tabindex="0" title="Editar perfil">
        <div class="user-profile__avatar" id="profile-avatar">${(user?.displayName ?? 'U').charAt(0).toUpperCase()}</div>
        <div class="user-profile__info">
          <div class="user-profile__name" id="profile-display-name">${escapeHtml(user?.displayName ?? '')}</div>
          <div class="user-profile__email">${escapeHtml(user?.email ?? '')}</div>
        </div>
        <span class="user-profile__edit-icon">✎</span>
      </div>
    </div>
  `;

  const roomsList    = container.querySelector('#rooms-list');
  const privatesList = container.querySelector('#privates-list');

  let activeRoomId    = null;
  let activePrivateId = null;

  // ── Controle de destaque ativo ───────────────────────

  function setActiveRoom(id) {
    activeRoomId    = id;
    activePrivateId = null;
    roomsList.querySelectorAll('.nav-item').forEach((el) => {
      el.classList.toggle('active', el.dataset.roomId === id);
    });
    privatesList.querySelectorAll('.nav-item').forEach((el) => el.classList.remove('active'));
  }

  function setActivePrivate(userId) {
    activePrivateId = userId;
    activeRoomId    = null;
    privatesList.querySelectorAll('.nav-item').forEach((el) => {
      el.classList.toggle('active', el.dataset.userId === userId);
    });
    roomsList.querySelectorAll('.nav-item').forEach((el) => el.classList.remove('active'));
  }

  // ── Salas ─────────────────────────────────────────────

  function addRoomItem(room) {
    if (roomsList.querySelector(`[data-room-id="${room.id}"]`)) return;

    const li = document.createElement('li');
    li.className = 'nav-item';
    li.dataset.roomId = room.id;
    li.innerHTML = `
      <span class="nav-item__icon">#</span>
      <span class="nav-item__name">${escapeHtml(room.title)}</span>
      <span class="nav-item__actions">
        <button class="nav-item__action-btn" data-action="leave" title="Sair da sala">↩</button>
        <button class="nav-item__action-btn danger" data-action="delete" title="Excluir sala">✕</button>
      </span>
    `;

    li.addEventListener('click', (e) => {
      if (e.target.dataset.action) return;
      setActiveRoom(room.id);
      onRoomSelect(room);
    });

    // Sair da sala — chama DELETE /rooms/{roomId}/members
    // O backend remove o membro do MongoDB e faz broadcast do LEAVE via WebSocket.
    li.querySelector('[data-action="leave"]').addEventListener('click', async (e) => {
      e.stopPropagation();
      const ok = await confirmModal('Sair da sala', `Deseja sair de <strong>#${escapeHtml(room.title)}</strong>?`);
      if (!ok) return;

      try {
        await leaveRoom(room.id);
      } catch (err) {
        // Se a chamada falhar (ex: usuário já não era membro), apenas ignora
        // e continua com a remoção local — o estado do servidor já está correto.
        console.warn('[leaveRoom] Erro ignorado:', err.message);
      }

      li.remove();
      if (activeRoomId === room.id) onRoomDeleted(room.id);
    });

    // Excluir sala — chama DELETE /rooms/{roomId}
    // O backend remove a sala, todas as mensagens e o cache Redis.
    li.querySelector('[data-action="delete"]').addEventListener('click', async (e) => {
      e.stopPropagation();
      const ok = await confirmModal(
        'Excluir sala',
        `Tem certeza que deseja excluir <strong>#${escapeHtml(room.title)}</strong>? Esta ação não pode ser desfeita.`,
        { danger: true }
      );
      if (!ok) return;

      try {
        await deleteRoom(room.id);
        li.remove();
        if (activeRoomId === room.id) onRoomDeleted(room.id);
        showToast(`Sala "#${room.title}" excluída`, 'success');
      } catch (err) {
        showToast(err.message, 'error');
      }
    });

    roomsList.querySelector('[data-placeholder]')?.remove();
    roomsList.appendChild(li);
  }

  async function loadRooms() {
    roomsList.innerHTML = '<li style="padding:6px 8px;font-size:12px;color:var(--text-muted);font-family:var(--font-mono)" data-placeholder>carregando…</li>';
    try {
      const data = await listRooms();
      roomsList.innerHTML = '';
      const rooms = data.content ?? [];
      if (rooms.length === 0) {
        roomsList.innerHTML = '<li style="padding:6px 8px;font-size:12px;color:var(--text-muted)">nenhuma sala</li>';
      } else {
        rooms.forEach(addRoomItem);
      }
    } catch {
      roomsList.innerHTML = '<li style="padding:6px 8px;font-size:12px;color:var(--danger)">erro ao carregar</li>';
    }
  }

  container.querySelector('#btn-new-room').addEventListener('click', async () => {
    const title = await promptModal('Nova sala', 'Nome da sala', 'ex: geral');
    if (!title) return;
    try {
      const room = await createRoom(title);
      addRoomItem(room);
      showToast(`Sala "#${room.title}" criada`, 'success');
    } catch (err) {
      showToast(err.message, 'error');
    }
  });

  // ── Conversas privadas ────────────────────────────────
  // Adicionadas de duas fontes:
  //   1. Ao carregar: GET /messages/private/conversations (conversas já existentes)
  //   2. Em tempo real: quando chega mensagem privada via WebSocket (addPrivateItem)

  function addPrivateItem(contact) {
    // contact = { userId, username }
    if (privatesList.querySelector(`[data-user-id="${contact.userId}"]`)) return;

    // Remove placeholders
    privatesList.querySelectorAll('[data-placeholder]').forEach((el) => el.remove());
    privatesList.querySelector('li:not(.nav-item)')?.remove();

    const li = document.createElement('li');
    li.className = 'nav-item';
    li.dataset.userId = contact.userId;
    li.innerHTML = `
      <span class="nav-item__icon" style="font-size:11px">@</span>
      <span class="nav-item__name">${escapeHtml(contact.username)}</span>
      <span class="nav-item__actions">
        <button class="nav-item__action-btn danger" data-action="delete" title="Excluir conversa">✕</button>
      </span>
    `;

    li.addEventListener('click', (e) => {
      if (e.target.dataset.action) return;
      setActivePrivate(contact.userId);
      onPrivateSelect(contact);
    });

    // Excluir conversa — remove apenas da sidebar (não há endpoint de deleção no backend)
    li.querySelector('[data-action="delete"]').addEventListener('click', async (e) => {
      e.stopPropagation();
      const ok = await confirmModal(
        'Excluir conversa',
        `Excluir conversa com <strong>@${escapeHtml(contact.username)}</strong>?`,
        { danger: true }
      );
      if (!ok) return;
      li.remove();
      if (activePrivateId === contact.userId) onPrivateDeleted(contact.userId);
    });

    privatesList.appendChild(li);
  }

  async function loadPrivateConversations() {
    try {
      const conversations = await getPrivateConversations();
      // ConversationSummaryDTO: { contactId, contactUsername, content, timestamp }
      conversations.forEach((conv) => {
        addPrivateItem({ userId: conv.contactId, username: conv.contactUsername });
      });
    } catch {
      // Silencioso — lista de DMs é opcional na inicialização
    }
  }

  // ── Perfil ────────────────────────────────────────────
  container.querySelector('#btn-profile').addEventListener('click', () => {
    openProfileModal(state.currentUser?.displayName ?? '', async ({ displayName, currentPassword, newPassword }) => {
      let changed = false;

      if (displayName !== state.currentUser?.displayName) {
        await updateUsername(displayName);
        state.currentUser = { ...state.currentUser, displayName };
        container.querySelector('#profile-display-name').textContent = displayName;
        container.querySelector('#profile-avatar').textContent = displayName.charAt(0).toUpperCase();
        changed = true;
      }

      if (currentPassword && newPassword) {
        await updatePassword(currentPassword, newPassword);
        changed = true;
      }

      if (changed) showToast('Perfil atualizado', 'success');
    });
  });

  // Carrega salas e conversas em paralelo
  await Promise.all([loadRooms(), loadPrivateConversations()]);

  return { addPrivateItem, setActiveRoom, setActivePrivate };
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[c]);
}
