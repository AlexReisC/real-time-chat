import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import styles from './ChatView.module.css';

function initials(name) {
  return name.slice(0, 2).toUpperCase();
}

function formatTime(iso) {
  const d = new Date(iso);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

export function ChatView({ chat, messages, members, onSend, onBack }) {
  const { user } = useAuth();
  const [text, setText] = useState('');
  const bodyRef = useRef(null);
  const textareaRef = useRef(null);

  // Scroll to bottom whenever messages change
  useEffect(() => {
    if (bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
    }
  }, [messages]);

  function handleSend() {
    const trimmed = text.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setText('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  function handleInput(e) {
    const el = e.target;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
    setText(el.value);
  }

  const isRoom = chat?.type === 'room';

  return (
    <div className={styles.wrapper}>
      {/* Header */}
      <div className={styles.header}>
        <button className={styles.backBtn} onClick={onBack} title="Voltar">←</button>
        <div className={`${styles.headerIcon} ${isRoom ? '' : styles.dmHeaderIcon}`}>
          {isRoom ? '#' : initials(chat?.name ?? '?')}
        </div>
        <div className={styles.headerInfo}>
          <div className={styles.headerName}>{chat?.name}</div>
          <div className={styles.headerMeta}>
            {isRoom
              ? `${members.length} membro${members.length !== 1 ? 's' : ''}`
              : chat?.online ? 'online' : 'offline'}
          </div>
        </div>
      </div>

      <div className={styles.body}>
        {/* Messages */}
        <div className={styles.messageArea} ref={bodyRef}>
          {messages.length === 0 ? (
            <div className={styles.empty}>
              Nenhuma mensagem ainda. Seja o primeiro a escrever!
            </div>
          ) : (
            <>
              <div className={styles.dateSep}>Hoje</div>
              {messages.map((msg, i) => {
                const senderName = msg.senderName;
                const isOwn      = msg.senderId === user?.id;
                const prevMsg    = messages[i - 1];
                const showMeta   = !prevMsg || prevMsg.senderId !== msg.senderId;

                return (
                  <div key={msg.id ?? i} className={`${styles.msgRow} ${isOwn ? styles.own : ''}`}>
                    <div className={`${styles.msgAvatar} ${isOwn ? styles.ownAvatar : ''}`}>
                      {initials(senderName ?? '?')}
                    </div>
                    <div className={styles.msgContent}>
                      {showMeta && (
                        <div className={styles.msgMeta}>
                          {senderName} · {formatTime(msg.timestamp ?? msg.createdAt)}
                        </div>
                      )}
                      <div className={`${styles.bubble} ${isOwn ? styles.ownBubble : ''}`}>
                        {msg.content}
                      </div>
                    </div>
                  </div>
                );
              })}
            </>
          )}
        </div>

        {/* Members panel (rooms only) */}
        {isRoom && (
          <div className={styles.membersPanel}>
            <div className={styles.membersHeader}>Membros</div>
            <div className={styles.membersList}>
              {members.map((m) => (
                <div key={m.id ?? m.username} className={styles.memberItem}>
                  <div className={styles.memberAvatar}>{initials(m.username)}</div>
                  <div className={styles.memberName}>{m.username}</div>
                  {m.online && <div className={styles.memberOnline} />}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Input */}
      <div className={styles.inputArea}>
        <div className={styles.inputRow}>
          <textarea
            ref={textareaRef}
            className={styles.textarea}
            value={text}
            onChange={handleInput}
            onKeyDown={handleKeyDown}
            placeholder={`Mensagem ${isRoom ? '#' + chat?.name : chat?.name}...`}
            rows={1}
          />
          <button className={styles.sendBtn} onClick={handleSend}>➤</button>
        </div>
      </div>
    </div>
  );
}