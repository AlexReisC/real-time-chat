import { useEffect } from 'react';
import { useAuth } from '../context/AuthContext.jsx';
import { useChat } from '../context/ChatContext.jsx';
import { useStomp } from '../hooks/useStomp.js';
import { roomsApi } from '../api/rooms.js';
import { messagesApi } from '../api/messages.js';
import { LeftSidebar } from '../components/sidebar/LeftSidebar.jsx';
import { ChatPanel } from '../components/chat/ChatPanel.jsx';
import { EmptyState } from '../components/chat/EmptyState.jsx';

export function ChatPage() {
  const { token } = useAuth();
  const {
    activeChat,
    setActiveChat,
    setRooms,
    setPrivates,
  } = useChat();

  const stomp = useStomp(token);

  // Load rooms and private conversations on mount
  useEffect(() => {
    roomsApi.list()
      .then((data) => setRooms(data?.content ?? data ?? []))
      .catch(console.error);

    messagesApi.listPrivateConversations()
      .then((data) => setPrivates(data ?? []))
      .catch(console.error);
  }, [setRooms, setPrivates]);

  // Subscribe to error queue
  useEffect(() => {
    if (!stomp) return;
    const unsub = stomp.subscribe('/user/queue/errors', (frame) => {
      try {
        const err = JSON.parse(frame.body);
        console.error('Server error:', err.message);
      } catch {}
    });
    return unsub;
  }, [stomp]);

  function handleJoinRoom(room) {
    setActiveChat({ type: 'room', data: room });
  }

  function handleOpenPrivate(conversation) {
    setActiveChat({ type: 'private', data: conversation });
  }

  function handleBack() {
    setActiveChat(null);
  }

  return (
    <div className="chat-layout">
      <LeftSidebar
        onJoinRoom={handleJoinRoom}
        onOpenPrivate={handleOpenPrivate}
      />

      <main className="chat-main">
        {activeChat ? (
          <ChatPanel stomp={stomp} onBack={handleBack} />
        ) : (
          <EmptyState />
        )}
      </main>
    </div>
  );
}
