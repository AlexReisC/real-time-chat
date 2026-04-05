import { useEffect, useRef, useState } from 'react';
import { useChat } from '../../context/ChatContext.jsx';
import { messagesApi } from '../../api/messages.js';
import { MessageItem } from './MessageItem.jsx';

export function MessageList({ activeChat }) {
  const { messages, pagination, prependMessages } = useChat();
  const bottomRef = useRef(null);
  const listRef = useRef(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const userScrolledUp = useRef(false);

  // Scroll to bottom when new messages arrive (unless user scrolled up)
  useEffect(() => {
    if (!userScrolledUp.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  // Reset scroll flag when activeChat changes, scroll to bottom immediately
  useEffect(() => {
    userScrolledUp.current = false;
    setTimeout(() => {
      bottomRef.current?.scrollIntoView({ behavior: 'instant' });
    }, 50);
  }, [activeChat]);

  function handleScroll() {
    const el = listRef.current;
    if (!el) return;
    const distFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    userScrolledUp.current = distFromBottom > 100;
  }

  async function loadMore() {
    if (loadingMore) return;
    const nextPage = (pagination.page ?? 0) + 1;
    if (nextPage >= pagination.totalPages) return;

    setLoadingMore(true);
    const prevScrollHeight = listRef.current?.scrollHeight ?? 0;

    try {
      let data;
      if (activeChat.type === 'room') {
        data = await messagesApi.loadRoomHistory(activeChat.data.id, nextPage);
      } else {
        data = await messagesApi.loadPrivateHistory(activeChat.data.userId, nextPage);
      }

      const older = [...(data.content ?? [])].reverse();
      prependMessages(older, { page: nextPage, totalPages: data.totalPages });

      // Maintain scroll position after prepend
      requestAnimationFrame(() => {
        if (listRef.current) {
          listRef.current.scrollTop = listRef.current.scrollHeight - prevScrollHeight;
        }
      });
    } finally {
      setLoadingMore(false);
    }
  }

  const hasMore = (pagination.page ?? 0) + 1 < (pagination.totalPages ?? 1);

  return (
    <div className="message-list" ref={listRef} onScroll={handleScroll}>
      {hasMore && (
        <div className="load-more">
          <button className="load-more__btn" onClick={loadMore} disabled={loadingMore}>
            {loadingMore ? 'Carregando...' : 'Carregar mensagens anteriores'}
          </button>
        </div>
      )}

      <ul className="messages">
        {messages.map((msg) => (
          <MessageItem key={msg.id} message={msg} />
        ))}
      </ul>

      <div ref={bottomRef} />
    </div>
  );
}
