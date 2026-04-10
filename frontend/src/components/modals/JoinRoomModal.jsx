import { useState } from 'react';
import { Modal } from './Modal.jsx';
import { roomsApi } from '../../api/rooms.js';
import { useChat } from '../../context/ChatContext.jsx';

export function JoinRoomModal({ room, onJoin, onClose }) {
  const { updateRoom } = useChat();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleJoin() {
    setLoading(true);
    setError('');
    try {
      // REST: add current user to room members
      const updatedRoom = await roomsApi.addMember(room.id);
      updateRoom(updatedRoom);
      onJoin(updatedRoom);
      onClose();
    } catch (err) {
      // 409 means already a member — proceed anyway
      if (err.status === 409) {
        onJoin(room);
        onClose();
      } else {
        setError(err.message ?? 'Erro ao entrar na sala.');
        setLoading(false);
      }
    }
  }

  return (
    <Modal title="Entrar na sala" onClose={onClose}>
      <p className="confirm-message">
        Deseja entrar em <strong>{room.title}</strong>?
      </p>
      {error && <p className="form-error">{error}</p>}
      <div className="modal-actions">
        <button className="btn-ghost" onClick={onClose} disabled={loading}>
          Cancelar
        </button>
        <button className="btn-primary" onClick={handleJoin} disabled={loading}>
          {loading ? <span className="btn-dots" /> : 'Entrar'}
        </button>
      </div>
    </Modal>
  );
}
