import { useState } from 'react';
import { useChat } from '../../context/ChatContext.jsx';
import { JoinRoomModal } from '../modals/JoinRoomModal.jsx';
import { ConfirmModal } from '../modals/ConfirmModal.jsx';
import { roomsApi } from '../../api/rooms.js';

export function RoomItem({ room, onJoin }) {
  const { activeChat, removeRoom } = useChat();
  const isActive = activeChat?.type === 'room' && activeChat.data.id === room.id;

  const [showJoin, setShowJoin] = useState(false);
  const [showDelete, setShowDelete] = useState(false);

  async function handleDelete() {
    await roomsApi.delete(room.id);
    removeRoom(room.id);
  }

  return (
    <>
      <li className={`nav-item ${isActive ? 'nav-item--active' : ''}`}>
        <button
          className="nav-item__label"
          onClick={() => setShowJoin(true)}
          title={room.title}
        >
          <span className="nav-item__hash">#</span>
          <span className="nav-item__name">{room.title}</span>
        </button>
        <div className="nav-item__actions">
          <button
            className="nav-item__action"
            title="Excluir sala"
            onClick={(e) => { e.stopPropagation(); setShowDelete(true); }}
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M1 1l10 10M11 1L1 11" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
            </svg>
          </button>
        </div>
      </li>

      {showJoin && (
        <JoinRoomModal
          room={room}
          onJoin={(r) => onJoin(r)}
          onClose={() => setShowJoin(false)}
        />
      )}

      {showDelete && (
        <ConfirmModal
          title="Excluir sala"
          message={`Excluir "${room.title}" permanentemente?`}
          confirmLabel="Excluir"
          danger
          onConfirm={handleDelete}
          onClose={() => setShowDelete(false)}
        />
      )}
    </>
  );
}
