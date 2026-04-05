import { useState } from 'react';
import { useChat } from '../../context/ChatContext.jsx';
import { RoomItem } from './RoomItem.jsx';
import { PrivateItem } from './PrivateItem.jsx';
import { UserFooter } from './UserFooter.jsx';
import { CreateRoomModal } from '../modals/CreateRoomModal.jsx';

export function LeftSidebar({ onJoinRoom, onOpenPrivate }) {
  const { rooms, privates, addRoom } = useChat();
  const [showCreate, setShowCreate] = useState(false);

  function handleRoomCreated(room) {
    addRoom(room);
  }

  return (
    <>
      <aside className="left-sidebar">
        <div className="sidebar-header">
          <span className="sidebar-logo">
            <span className="sidebar-logo__glyph">◈</span>
            Relay
          </span>
          <button
            className="icon-btn"
            onClick={() => setShowCreate(true)}
            title="Nova sala"
          >
            <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
              <path d="M7.5 2v11M2 7.5h11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        <div className="sidebar-scroll">
          <section className="sidebar-section">
            <span className="sidebar-section__label">Salas</span>
            {rooms.length === 0 ? (
              <p className="sidebar-empty">Nenhuma sala ainda</p>
            ) : (
              <ul className="nav-list">
                {rooms.map((room) => (
                  <RoomItem key={room.id} room={room} onJoin={onJoinRoom} />
                ))}
              </ul>
            )}
          </section>

          <section className="sidebar-section">
            <span className="sidebar-section__label">Mensagens privadas</span>
            {privates.length === 0 ? (
              <p className="sidebar-empty">Nenhuma conversa ainda</p>
            ) : (
              <ul className="nav-list">
                {privates.map((conv) => (
                  <PrivateItem
                    key={conv.userId}
                    conversation={conv}
                    onClick={onOpenPrivate}
                  />
                ))}
              </ul>
            )}
          </section>
        </div>

        <UserFooter />
      </aside>

      {showCreate && (
        <CreateRoomModal
          onCreated={handleRoomCreated}
          onClose={() => setShowCreate(false)}
        />
      )}
    </>
  );
}
