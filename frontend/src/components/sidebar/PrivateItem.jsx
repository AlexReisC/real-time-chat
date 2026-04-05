import { useState } from 'react';
import { useChat } from '../../context/ChatContext.jsx';
import { ConfirmModal } from '../modals/ConfirmModal.jsx';
import { Avatar } from '../Avatar.jsx';

export function PrivateItem({ conversation, onClick }) {
  const { activeChat, removePrivate } = useChat();
  const isActive =
    activeChat?.type === 'private' && activeChat.data.userId === conversation.userId;

  const [showDelete, setShowDelete] = useState(false);

  async function handleDelete() {
    removePrivate(conversation.userId);
  }

  return (
    <>
      <li className={`nav-item ${isActive ? 'nav-item--active' : ''}`}>
        <button
          className="nav-item__label"
          onClick={() => onClick(conversation)}
          title={conversation.username}
        >
          <Avatar name={conversation.username} size="xs" />
          <span className="nav-item__name">{conversation.username}</span>
        </button>
        <div className="nav-item__actions">
          <button
            className="nav-item__action"
            title="Excluir conversa"
            onClick={(e) => { e.stopPropagation(); setShowDelete(true); }}
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M1 1l10 10M11 1L1 11" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
            </svg>
          </button>
        </div>
      </li>

      {showDelete && (
        <ConfirmModal
          title="Excluir conversa"
          message={`Excluir conversa com "${conversation.username}"?`}
          confirmLabel="Excluir"
          danger
          onConfirm={handleDelete}
          onClose={() => setShowDelete(false)}
        />
      )}
    </>
  );
}
