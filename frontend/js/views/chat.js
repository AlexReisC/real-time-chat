// js/views/chat.js
// Orquestra o layout do chat: sidebar + área principal + WebSocket.

import { connect, disconnect, subscribe, publish } from '../ws/stomp.js';
import { mountSidebar } from '../components/sidebar.js';
import { mountRoomChat } from '../components/roomChat.js';
import { mountPrivateChat } from '../components/privateChat.js';
import { state } from '../state/store.js';
import { showToast } from '../components/modals.js';

/**
 * @param {HTMLElement} root
 * @param {function} navigate
 */
export function renderChat(root, navigate) {
  if (!state.currentUser) {
    navigate('/login');
    return;
  }

  root.innerHTML = `
    <div class="chat-layout">
      <aside class="sidebar" id="sidebar"></aside>
      <main class="chat-main" id="chat-main">
        <div class="chat-empty" id="chat-empty">
          <div class="chat-empty__icon">💬</div>
          <span class="chat-empty__text">selecione uma sala ou conversa</span>
        </div>
      </main>
    </div>
  `;

  const sidebarEl  = root.querySelector('#sidebar');
  const mainEl     = root.querySelector('#chat-main');

  // Função de cleanup do painel ativo (chat de sala ou privado)
  let unmountCurrent = null;

  function showEmpty() {
    mainEl.innerHTML = `
      <div class="chat-empty" id="chat-empty">
        <div class="chat-empty__icon">💬</div>
        <span class="chat-empty__text">selecione uma sala ou conversa</span>
      </div>
    `;
  }

  function clearMain() {
    if (typeof unmountCurrent === 'function') {
      unmountCurrent();
      unmountCurrent = null;
    }
    mainEl.innerHTML = '';
  }

  // ── Handlers da sidebar ──────────────────────────────

  async function handleRoomSelect(room) {
    // Modal de confirmação para entrar na sala
    const { confirmModal } = await import('../components/modals.js');
    const ok = await confirmModal(
      `Entrar em #${room.title}`,
      `Deseja entrar na sala <strong>#${room.title}</strong>?`
    );
    if (!ok) return;

    clearMain();
    state.activeRoom    = room;
    state.activePrivate = null;

    // Notifica o backend via WebSocket
    publish('/app/chat.addUser', { roomId: room.id, type: 'JOIN' });

    unmountCurrent = mountRoomChat(mainEl, room, () => {
      clearMain();
      showEmpty();
      state.activeRoom = null;
      // Notifica saída
      publish('/app/chat.removeUser', { roomId: room.id, type: 'LEAVE' });
      sidebarControl.setActiveRoom(null);
    });

    sidebarControl.setActiveRoom(room.id);
  }

  function handlePrivateSelect(contact) {
    clearMain();
    state.activePrivate = contact;
    state.activeRoom    = null;

    unmountCurrent = mountPrivateChat(mainEl, contact, () => {
      clearMain();
      showEmpty();
      state.activePrivate = null;
      sidebarControl.setActivePrivate(null);
    });

    sidebarControl.setActivePrivate(contact.userId);
  }

  // ── WebSocket ────────────────────────────────────────

  let sidebarControl = {};

  // Erros que representam estado já correto no servidor — não exibem toast.
  const SILENT_ERRORS = [
    'usuário já é membro da sala',
    'já é membro',
  ];

  function isSilentError(message = '') {
    return SILENT_ERRORS.some((s) => message.toLowerCase().includes(s));
  }

  function onStompConnected() {
    showToast('Conectado', 'success', 2000);

    // Subscreve mensagens privadas globalmente para detectar novos contatos.
    // A subscrição fica ativa durante toda a sessão — privateChat filtra por remetente.
    subscribe('/user/queue/private', (msg) => {
      const senderId   = msg.senderId;
      const senderName = msg.senderUsername ?? senderId;

      // Se a mensagem veio de outro usuário e ainda não está na lista de DMs, adiciona
      if (senderId !== state.currentUser?.id) {
        sidebarControl.addPrivateItem?.({ userId: senderId, username: senderName });
      }
    });

    // Subscreve erros globais do WebSocket.
    // Erros de "já é membro" são absorvidos silenciosamente — ocorrem quando o
    // usuário re-entra numa sala que já participava (addNewUser lança EntityAlreadyExistsException).
    subscribe('/user/queue/errors', (err) => {
      if (isSilentError(err.message)) return;
      showToast(err.message || 'Erro inesperado', 'error');
    });
  }

  function onStompError() {
    showToast('Falha na conexão WebSocket — reconectando…', 'error');
  }

  connect(onStompConnected, onStompError);

  // Monta sidebar (async) e guarda referência de controle
  mountSidebar(sidebarEl, {
    onRoomSelect:    handleRoomSelect,
    onPrivateSelect: handlePrivateSelect,
    onRoomDeleted:   () => { clearMain(); showEmpty(); },
    onPrivateDeleted: () => { clearMain(); showEmpty(); },
  }).then((ctrl) => {
    sidebarControl = ctrl;
  }).catch((err) => {
    console.error('[chat] mountSidebar falhou:', err);
    showToast('Erro ao carregar sidebar', 'error');
  });

  // Cleanup ao fazer logout: desconecta STOMP e limpa a área principal.
  // A navegação para /login já é feita pelo listener global em app.js.
  window.addEventListener('auth:logout', () => {
    clearMain();
    disconnect();
  }, { once: true });
}