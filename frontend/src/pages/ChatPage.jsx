import { useState, useCallback, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import { Sidebar } from '../components/Sidebar';
import { ChatView } from '../components/ChatView';
import styles from './ChatPage.module.css';

// ── Mock seed data — remove when backend is integrated ────────────────────────
const SEED_ROOMS = [
  { id: 1, name: 'geral',    members: [{ id: '1', username: 'alex', online: true }, { id: '2', username: 'maria', online: true }, { id: '3', username: 'joao', online: false }] },
  { id: 2, name: 'frontend', members: [{ id: '1', username: 'alex', online: true }, { id: '2', username: 'maria', online: true }] },
  { id: 3, name: 'backend',  members: [{ id: '1', username: 'alex', online: true }, { id: '3', username: 'joao', online: false }, { id: '4', username: 'pedro', online: true }] },
];

const SEED_DMS = [
  { id: '2', username: 'maria', online: true },
  { id: '3', username: 'joao',  online: false },
  { id: '4', username: 'pedro', online: true },
];

const SEED_MESSAGES = {
  'r1': [
    { id: 'm1', fromUsername: 'joao',  content: 'Bom dia pessoal!',                      createdAt: new Date().toISOString() },
    { id: 'm2', fromUsername: 'maria', content: 'Bom dia! Alguém olhou os novos requisitos?', createdAt: new Date().toISOString() },
    { id: 'm3', fromUsername: 'alex',  content: 'Vi sim. Vou começar pelo service layer hoje.', createdAt: new Date().toISOString() },
  ],
  'd2': [
    { id: 'm4', fromUsername: 'maria', content: 'Alex, consegue revisar meu PR?', createdAt: new Date().toISOString() },
    { id: 'm5', fromUsername: 'alex',  content: 'Claro! Manda o link.',           createdAt: new Date().toISOString() },
  ],
};
// ─────────────────────────────────────────────────────────────────────────────

export function ChatPage() {
  const { user, token } = useAuth();

  const [rooms, setRooms]       = useState(SEED_ROOMS);
  const [dms, setDms]           = useState(SEED_DMS);
  const [messages, setMessages] = useState(SEED_MESSAGES);
  const [activeChat, setActiveChat] = useState(null);

  // Resolved metadata for the currently open chat
  const activeChatMeta = activeChat
    ? activeChat.startsWith('r')
      ? { ...rooms.find(r => `r${r.id}` === activeChat), type: 'room', name: rooms.find(r => `r${r.id}` === activeChat)?.name }
      : { ...dms.find(d => `d${d.id}` === activeChat), type: 'dm' }
    : null;

  const activeMembers = activeChat?.startsWith('r')
    ? rooms.find(r => `r${r.id}` === activeChat)?.members ?? []
    : [];

  // ── WebSocket ─────────────────────────────────────────────────────────────
  const handleIncomingMessage = useCallback((msg) => {
    // msg shape from backend: { chatKey, id, fromUsername, content, createdAt }
    // chatKey = 'r{roomId}' | 'd{userId}'
    setMessages(prev => ({
      ...prev,
      [msg.chatKey]: [...(prev[msg.chatKey] ?? []), msg],
    }));
  }, []);

  const { send } = useWebSocket({ token, onMessage: handleIncomingMessage });

  // ── TODO: carregar salas e histórico ao montar ───────────────────────────────
  // useEffect(() => {
  //   roomsApi.list().then(setRooms);
  // }, []);

  // ── Actions ───────────────────────────────────────────────────────────────
  function handleSelectChat(key) {
    setActiveChat(key);
    // TODO: se mensagens não estiverem em cache, buscar histórico do Redis:
    // const fetch = key.startsWith('r')
    //   ? messagesApi.roomHistory(key.slice(1))        // GET /api/v1/messages/room/{roomId}
    //   : messagesApi.privateHistory(key.slice(1));    // GET /api/v1/messages/private/{targetUserId}
    // fetch.then(msgs => setMessages(prev => ({ ...prev, [key]: msgs })));
  }

  function handleCreateRoom(name) {
    // TODO: roomsApi.create(name).then(room => { setRooms(prev => [...prev, room]); ... });
    const newRoom = { id: Date.now(), name, members: [{ id: user.id, username: user.username, online: true }] };
    setRooms(prev => [...prev, newRoom]);
    setActiveChat(`r${newRoom.id}`);
  }

  function handleDeleteRoom(roomId) {
    // TODO: roomsApi.delete(roomId).then(() => { ... });
    setRooms(prev => prev.filter(r => r.id !== roomId));
    if (activeChat === `r${roomId}`) setActiveChat(null);
  }

  function handleSend(content) {
    if (!activeChat) return;

    const isRoom = activeChat.startsWith('r');
    const destinationId = activeChat.slice(1);

    // Optimistic update
    const optimisticMsg = {
      id: `opt-${Date.now()}`,
      fromUsername: user.username,
      content,
      createdAt: new Date().toISOString(),
    };
    setMessages(prev => ({
      ...prev,
      [activeChat]: [...(prev[activeChat] ?? []), optimisticMsg],
    }));

    // Send via WebSocket
    send(isRoom ? 'room' : 'private', destinationId, content);
  }

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <div className={styles.layout}>
      <Sidebar
        rooms={rooms}
        dms={dms}
        activeChat={activeChat}
        onSelectChat={handleSelectChat}
        onCreateRoom={handleCreateRoom}
        onDeleteRoom={handleDeleteRoom}
      />

      <main className={styles.main}>
        {!activeChat ? (
          <div className={styles.welcome}>
            <div className={styles.welcomeIcon}>◈</div>
            <div className={styles.welcomeTitle}>Selecione uma conversa</div>
            <div className={styles.welcomeSub}>
              Escolha uma sala ou mensagem direta na barra lateral para começar.
            </div>
          </div>
        ) : (
          <ChatView
            chat={activeChatMeta}
            messages={messages[activeChat] ?? []}
            members={activeMembers}
            onSend={handleSend}
            onBack={() => setActiveChat(null)}
          />
        )}
      </main>
    </div>
  );
}
