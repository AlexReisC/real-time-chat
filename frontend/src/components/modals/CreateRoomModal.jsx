import { useState } from 'react';
import { Modal } from './Modal.jsx';
import { roomsApi } from '../../api/rooms.js';

export function CreateRoomModal({ onCreated, onClose }) {
  const [title, setTitle] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (!title.trim()) {
      setError('O título é obrigatório.');
      return;
    }

    setLoading(true);
    try {
      const room = await roomsApi.create(title.trim());
      onCreated(room);
      onClose();
    } catch (err) {
      setError(err.message ?? 'Erro ao criar sala.');
      setLoading(false);
    }
  }

  return (
    <Modal title="Nova sala" onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor="room-title">Título da sala</label>
          <input
            id="room-title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="ex: geral, dev-team, random"
            autoFocus
            disabled={loading}
          />
        </div>
        {error && <p className="form-error">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn-ghost" onClick={onClose} disabled={loading}>
            Cancelar
          </button>
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? <span className="btn-dots" /> : 'Criar'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
