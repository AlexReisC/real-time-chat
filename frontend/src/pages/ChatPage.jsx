import { useState, useCallback, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import { roomsApi, messagesApi } from '../services/api';
import { Sidebar } from '../components/Sidebar';
import { ChatView } from '../components/ChatView';
import styles from './ChatPage.module.css';

export function ChatPage() {
  const { user, token } = useAuth();

  const [rooms, setRooms]           = useState([]);
  const [dms, setDms]               = useState([]);
  const [messages, setMessages]     = useState({});
  const [activeChat, setActiveChat] = useState(null);
  const [loadingRooms, setLoadingRooms] = useState(true);

  // Ref para acessar activeChat dentro dos callbacks do WS sem re-registrar
  const activeChatRef = useRef(activeChat);
  useEffect(() => { activeChatRef.current = activeChat; }, [activeChat]);

  // ── Carregar salas ao montar ──────────────────────────────────────────────
  useEffect(() => {
    roomsApi.list()
      .then(data => {
        // PageResponseDTO: { content, pageNumber, totalPages, totalElements, size }
        setRooms(data.content ?? data);
      })
      .catch(err => console.error('Erro ao carregar salas:', err))
      .finally(() => setLoadingRooms(false));
  }, []);

  // ── Callbacks estabilizados para o hook de WS ─────────────────────────────

  /**
   * Recebe ResponseMessageDTO normalizado pelo hook.
   * Adiciona ao estado de mensagens usando _chatKey como índice.
   * Se a mensagem chegou em uma conversa que não está aberta, poderia
   * futuramente mostrar um badge de notificação — ponto de extensão natural.
   */
  const handleIncomingMessage = useCallback((msg) => {
    setMessages(prev => ({
      ...prev,
      [msg._chatKey]: [...(prev[msg._chatKey] ?? []), msg],
    }));
  }, []);

  /**
   * Recebe UserNotificationResponseDTO (JOIN/LEAVE).
   * Atualiza a lista de membros da sala em tempo real.
   *
   * Backend envia: { type, userId, username, room_id, content, timestamp }
   * Nota: o campo é `room_id` (snake_case) por causa do @JsonProperty no DTO Java.
   */
  const handleNotification = useCallback((notification) => {
    const roomId = notification.room_id;

    setRooms(prev => prev.map(room => {
      if (String(room.id) !== String(roomId)) return room;

      const currentMembers = room.members ?? [];

      if (notification.type === 'JOIN') {
        // Evita duplicatas caso o evento chegue mais de uma vez
        const alreadyMember = currentMembers.some(m => m.id === notification.userId);
        if (alreadyMember) return room;
        return {
          ...room,
          members: [...currentMembers, { id: notification.userId, username: notification.username }],
        };
      }

      if (notification.type === 'LEAVE') {
        return {
          ...room,
          members: currentMembers.filter(m => m.id !== notification.userId),
        };
      }

      return room;
    }));
  }, []);

  const handleWsError = useCallback((err) => {
    console.error('[ChatPage] erro WS recebido:', err.message);
    // Ponto de extensão: mostrar toast/snackbar com err.message
  }, []);

  // ── WebSocket ─────────────────────────────────────────────────────────────
  const { joinRoom, leaveRoom, sendPublic, sendPrivate, subscribeToRoom, unsubscribeFromRoom } =
    useWebSocket({ token, onMessage: handleIncomingMessage, onNotification: handleNotification, onError: handleWsError });

  // ── Navegação entre chats ─────────────────────────────────────────────────
  async function handleSelectChat(key) {
    const prevChat = activeChatRef.current;

    // Sair da sala anterior via STOMP se era uma sala
    if (prevChat?.startsWith('r')) {
      const prevRoomId = prevChat.slice(1);
      leaveRoom(prevRoomId);
      unsubscribeFromRoom();
    }

    setActiveChat(key);

    const isRoom = key.startsWith('r');
    const id     = key.slice(1);

    // Carregar histórico do Redis (endpoint GET do MessageController)
    if (!messages[key]) {
      try {
        const history = isRoom
          ? await messagesApi.roomHistory(id)         // GET /api/v1/messages/room/{roomId}
          : await messagesApi.privateHistory(id);     // GET /api/v1/messages/private/{targetUserId}

        // O backend retorna List<ResponseMessageDTO> diretamente (não paginado nos endpoints de cache)
        const list = Array.isArray(history) ? history : (history.content ?? []);

        // Normaliza para o shape interno (garante senderName, _chatKey, etc.)
        const normalized = list.map(msg => ({
          ...msg,
          _chatKey: key,
        }));

        setMessages(prev => ({ ...prev, [key]: normalized }));
      } catch (err) {
        console.error('Erro ao carregar histórico:', err);
      }
    }

    // Entrar na nova sala via STOMP e subscrever ao tópico
    if (isRoom) {
      joinRoom(id);
      subscribeToRoom(id);

      // Carregar membros da sala se ainda não tiver
      const room = rooms.find(r => String(r.id) === id);
      if (room && !room.members) {
        try {
          const data = await roomsApi.members(id);
          // GET /api/v1/rooms/{roomId}/members retorna PageResponseDTO<String> (IDs)
          // O RoomService retorna os IDs dos membros — não há endpoint de User por ID no Chat Service.
          // Mapeia para o shape { id, username } com username provisório até integração com Auth Service.
          const memberIds = data.content ?? data;
          setRooms(prev => prev.map(r =>
            String(r.id) === id
              ? { ...r, members: memberIds.map(uid => ({ id: uid, username: uid })) }
              : r
          ));
        } catch (err) {
          console.error('Erro ao carregar membros:', err);
        }
      }
    }
  }

  // Sair da sala ao fechar o chat
  function handleBack() {
    const prev = activeChatRef.current;
    if (prev?.startsWith('r')) {
      leaveRoom(prev.slice(1));
      unsubscribeFromRoom();
    }
    setActiveChat(null);
  }

  // ── CRUD de salas ─────────────────────────────────────────────────────────
  async function handleCreateRoom(title) {
    try {
      // CreateRoomDTO: { title } — campo é `title`, não `name`
      const room = await roomsApi.create(title);
      setRooms(prev => [...prev, { ...room, members: [] }]);
      handleSelectChat(`r${room.id}`);
    } catch (err) {
      console.error('Erro ao criar sala:', err);
    }
  }

  function handleDeleteRoom(roomId) {
    // O backend não expõe DELETE de sala na spec fornecida —
    // operação apenas local por ora (remoção da UI sem chamar API)
    setRooms(prev => prev.filter(r => String(r.id) !== String(roomId)));
    if (activeChat === `r${roomId}`) handleBack();
  }

  // ── Envio de mensagens ────────────────────────────────────────────────────
  function handleSend(content) {
    if (!activeChat) return;

    const isRoom = activeChat.startsWith('r');
    const id     = activeChat.slice(1);

    // Update otimista — usa senderName (campo correto do ResponseMessageDTO)
    const optimistic = {
      id:         `opt-${Date.now()}`,
      type:       isRoom ? 'ROOM' : 'PRIVATE',
      senderName: user.username,
      senderId:   user.id,
      recipientId: isRoom ? null : id,
      roomId:     isRoom ? id : null,
      content,
      timestamp:  new Date().toISOString(),
      _chatKey:   activeChat,
      _optimistic: true,
    };

    setMessages(prev => ({
      ...prev,
      [activeChat]: [...(prev[activeChat] ?? []), optimistic],
    }));

    if (isRoom) {
      sendPublic(id, content);
    } else {
      sendPrivate(id, content);
    }
  }

  // ── Dados do chat ativo ───────────────────────────────────────────────────
  const activeChatMeta = (() => {
    if (!activeChat) return null;
    if (activeChat.startsWith('r')) {
      const room = rooms.find(r => `r${r.id}` === activeChat);
      return room ? { ...room, type: 'room', name: room.title ?? room.name } : null;
    }
    const dm = dms.find(d => `d${d.id}` === activeChat);
    return dm ? { ...dm, type: 'dm' } : null;
  })();

  const activeMembers = activeChat?.startsWith('r')
    ? rooms.find(r => `r${r.id}` === activeChat)?.members ?? []
    : [];

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
        loading={loadingRooms}
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
            onBack={handleBack}
          />
        )}
      </main>
    </div>
  );
}