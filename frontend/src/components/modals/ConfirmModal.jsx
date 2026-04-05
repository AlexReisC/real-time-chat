import { useState } from 'react';
import { Modal } from './Modal.jsx';

export function ConfirmModal({ title, message, confirmLabel = 'Confirmar', danger = false, onConfirm, onClose }) {
  const [loading, setLoading] = useState(false);

  async function handleConfirm() {
    setLoading(true);
    try {
      await onConfirm();
      onClose();
    } catch {
      setLoading(false);
    }
  }

  return (
    <Modal title={title} onClose={onClose}>
      <p className="confirm-message">{message}</p>
      <div className="modal-actions">
        <button className="btn-ghost" onClick={onClose} disabled={loading}>
          Cancelar
        </button>
        <button
          className={danger ? 'btn-danger' : 'btn-primary'}
          onClick={handleConfirm}
          disabled={loading}
        >
          {loading ? <span className="btn-dots" /> : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
