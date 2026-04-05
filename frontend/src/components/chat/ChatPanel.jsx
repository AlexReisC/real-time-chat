import { useEffect, useRef, useState } from 'react';
import { useChat } from '../../context/ChatContext.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import { roomsApi } from '../../api/rooms.js';
import { messagesApi } from '../../api/messages.js';
import { MessageList } from './MessageList.jsx';
import { MessageInput } from './MessageInput.jsx';
import { MembersBar } from './MembersBar.jsx';
import { ConfirmModal } from '../modals/ConfirmModal.jsx';

export function ChatPanel({ stomp, onBack }) {
  const { user } = useAuth();
  const {
    activeChat,
    setMessages,
    appendMessage,
    setMembers,
    removeRoom,
    setActiveChat,
  } = useChat();

  const [modal, setModal] = useState(null); // 'leave' | 'delete-room' | 'delete-private'
  const prevChatRef = useRef(null);
  const unsubscribeRef = useRef(null);

  const isRoom = activeChat?.type === 'room';
  const isPrivate = activeChat?.type === 'private';

  // ── When activeChat changes: unsubscribe previous, load messages, subscribe new ──
  useEffect(() => {
    if (!activeChat || !stomp) return;

    // Unsubscribe previous subscription
    if (unsubscribeRef.current) {
      unsubscribeRef.current();
      unsubscribeRef.current = null;
    }

    async function setup() {
      try {
        if (isRoom) {
          const roomId = activeChat.data.id;

          // 1. Load cached messages (last 50)
          const cached = await messagesApi.loadRoomCache(roomId);
          const sorted = [...(cached ?? [])].sort(
            (a, b) => new Date(a.timestamp) - new Date(b.timestamp)
          );
          setMessages(sorted);

          // 2. Load members
          const membersData = await roomsApi.getMembers(roomId);
          setMembers(membersData?.content ?? membersData ?? []);

          // 3. Send STOMP addUser notification
          stomp.publish('/app/chat.addUser', {
            roomId,
            type: 'JOIN',
          });

          // 4. Subscribe to room topic
          const unsub = stomp.subscribe(
            `/topic/rooms.${roomId}`,
            (frame) => {
              try {
                const msg = JSON.parse(frame.body);
                // Notifications (JOIN/LEAVE) have a `type` field but no content as a message
                if (msg.type === 'JOIN' || msg.type === 'LEAVE') return;
                appendMessage(msg);
              } catch (e) {
                console.error('Failed to parse STOMP message', e);
              }
            }
          );
          unsubscribeRef.current = unsub;

        } else if (isPrivate) {
          const targetUserId = activeChat.data.userId;

          // 1. Load cached messages
          const cached = await messagesApi.loadPrivateCache(targetUserId);
          const sorted = [...(cached ?? [])].sort(
            (a, b) => new Date(a.timestamp) - new Date(b.timestamp)
          );
          setMessages(sorted);

          // 2. Subscribe to private queue
          const unsub = stomp.subscribe(
            '/user/queue/private',
            (frame) => {
              try {
                const msg = JSON.parse(frame.body);
                // Only show messages relevant to this conversation
                const isRelevant =
                  msg.senderId === targetUserId ||
                  msg.recipientId === targetUserId;
                if (isRelevant) appendMessage(msg);
              } catch (e) {
                console.error('Failed to parse STOMP message', e);
              }
            }
          );
          unsubscribeRef.current = unsub;
        }
      } catch (err) {
        console.error('ChatPanel setup error', err);
      }
    }

    setup();

    return () => {
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeChat?.type, activeChat?.data?.id ?? activeChat?.data?.userId]);

  function handleSend(content) {
    if (!stomp || !user) return;

    if (isRoom) {
      stomp.publish('/app/chat.sendPublic', {
        roomId: activeChat.data.id,
        content,
      });
    } else {
      stomp.publish('/app/chat.sendPrivate', {
        recipientId: activeChat.data.userId,
        content,
      });
    }
  }

  async function handleLeaveRoom() {
    const roomId = activeChat.data.id;
    await roomsApi.removeMember(roomId);
    stomp.publish('/app/chat.removeUser', {
      roomId,
      type: 'LEAVE',
    });
    setActiveChat(null);
  }

  async function handleDeleteRoom() {
    const roomId = activeChat.data.id;
    await roomsApi.delete(roomId);
    removeRoom(roomId);
    setActiveChat(null);
  }

  async function handleDeletePrivate() {
    setActiveChat(null);
  }

  const title = isRoom
    ? `# ${activeChat.data.title}`
    : activeChat?.data?.username ?? '';

  const subtitle = isRoom
    ? `${activeChat.data.membersIds?.length ?? ''} membros`
    : 'Mensagem privada';

  return (
    <div className="chat-panel">
      {/* Header */}
      <header className="chat-header">
        <div className="chat-header__left">
          <button className="icon-btn" onClick={onBack} title="Voltar">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M10 3L5 8l5 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
          <div>
            <h2 className="chat-header__title">{title}</h2>
            <span className="chat-header__sub">{subtitle}</span>
          </div>
        </div>

        <div className="chat-header__actions">
          {isRoom && (
            <>
              <button
                className="btn-ghost btn-ghost--sm"
                onClick={() => setModal('leave')}
              >
                Sair da sala
              </button>
              <button
                className="btn-danger-ghost btn-ghost--sm"
                onClick={() => setModal('delete-room')}
              >
                Excluir sala
              </button>
            </>
          )}
          {isPrivate && (
            <button
              className="btn-danger-ghost btn-ghost--sm"
              onClick={() => setModal('delete-private')}
            >
              Excluir conversa
            </button>
          )}
        </div>
      </header>

      {/* Body */}
      <div className={`chat-body ${isRoom ? 'chat-body--with-members' : ''}`}>
        <MessageList activeChat={activeChat} />
        {isRoom && <MembersBar />}
      </div>

      {/* Input */}
      <MessageInput onSend={handleSend} disabled={!stomp} />

      {/* Modals */}
      {modal === 'leave' && (
        <ConfirmModal
          title="Sair da sala"
          message={`Sair de "${activeChat.data.title}"? Você será removido da lista de membros.`}
          confirmLabel="Sair"
          danger
          onConfirm={handleLeaveRoom}
          onClose={() => setModal(null)}
        />
      )}
      {modal === 'delete-room' && (
        <ConfirmModal
          title="Excluir sala"
          message={`Excluir "${activeChat.data.title}" permanentemente?`}
          confirmLabel="Excluir"
          danger
          onConfirm={handleDeleteRoom}
          onClose={() => setModal(null)}
        />
      )}
      {modal === 'delete-private' && (
        <ConfirmModal
          title="Excluir conversa"
          message={`Excluir conversa com "${activeChat.data.username}"?`}
          confirmLabel="Excluir"
          danger
          onConfirm={handleDeletePrivate}
          onClose={() => setModal(null)}
        />
      )}
    </div>
  );
}
