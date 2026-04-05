import { useState } from 'react';
import { Modal } from './Modal.jsx';
import { authApi } from '../../api/auth.js';
import { useAuth } from '../../context/AuthContext.jsx';

export function EditProfileModal({ onClose }) {
  const { updateUser } = useAuth();
  const [tab, setTab] = useState('username'); // 'username' | 'password'

  return (
    <Modal title="Editar perfil" onClose={onClose}>
      <div className="tabs">
        <button
          className={`tab ${tab === 'username' ? 'tab--active' : ''}`}
          onClick={() => setTab('username')}
        >
          Nome de usuário
        </button>
        <button
          className={`tab ${tab === 'password' ? 'tab--active' : ''}`}
          onClick={() => setTab('password')}
        >
          Senha
        </button>
      </div>

      {tab === 'username' ? (
        <UsernameForm updateUser={updateUser} onClose={onClose} />
      ) : (
        <PasswordForm onClose={onClose} />
      )}
    </Modal>
  );
}

function UsernameForm({ updateUser, onClose }) {
  const { user } = useAuth();
  const [username, setUsername] = useState(user?.displayName ?? '');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (!username.trim()) { setError('Campo obrigatório.'); return; }

    setLoading(true);
    try {
      await authApi.updateProfile(username.trim());
      updateUser({ displayName: username.trim() });
      setSuccess(true);
      setTimeout(onClose, 800);
    } catch (err) {
      setError(err.message ?? 'Erro ao atualizar.');
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="field">
        <label htmlFor="new-username">Novo nome de usuário</label>
        <input
          id="new-username"
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          disabled={loading || success}
          autoFocus
        />
      </div>
      {error && <p className="form-error">{error}</p>}
      {success && <p className="form-success">Atualizado!</p>}
      <div className="modal-actions">
        <button type="button" className="btn-ghost" onClick={onClose} disabled={loading}>
          Cancelar
        </button>
        <button type="submit" className="btn-primary" disabled={loading || success}>
          {loading ? <span className="btn-dots" /> : 'Salvar'}
        </button>
      </div>
    </form>
  );
}

function PasswordForm({ onClose }) {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (!current.trim() || !next.trim()) { setError('Preencha todos os campos.'); return; }
    if (next.length < 8) { setError('Mínimo de 8 caracteres.'); return; }

    setLoading(true);
    try {
      await authApi.changePassword(current, next);
      setSuccess(true);
      setTimeout(onClose, 800);
    } catch (err) {
      setError(err.message ?? 'Erro ao alterar senha.');
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div className="field">
        <label htmlFor="current-pw">Senha atual</label>
        <input
          id="current-pw"
          type="password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
          disabled={loading || success}
          autoFocus
        />
      </div>
      <div className="field">
        <label htmlFor="new-pw">Nova senha</label>
        <input
          id="new-pw"
          type="password"
          value={next}
          onChange={(e) => setNext(e.target.value)}
          placeholder="mínimo 8 caracteres"
          disabled={loading || success}
        />
      </div>
      {error && <p className="form-error">{error}</p>}
      {success && <p className="form-success">Senha alterada!</p>}
      <div className="modal-actions">
        <button type="button" className="btn-ghost" onClick={onClose} disabled={loading}>
          Cancelar
        </button>
        <button type="submit" className="btn-primary" disabled={loading || success}>
          {loading ? <span className="btn-dots" /> : 'Alterar senha'}
        </button>
      </div>
    </form>
  );
}
