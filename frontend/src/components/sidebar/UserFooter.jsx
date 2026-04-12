import { useState } from 'react';
import { useAuth } from '../../context/AuthContext.jsx';
import { Avatar } from '../Avatar.jsx';
import { EditProfileModal } from '../modals/EditProfileModal.jsx';

export function UserFooter() {
  const { user, logout } = useAuth();
  const [showEdit, setShowEdit] = useState(false);

  return (
    <>
      <div className="user-footer">
        <div className="user-footer__info">
          <Avatar name={user?.username} size="sm" />
          <div className="user-footer__text">
            <span className="user-footer__name">{user?.displayName}</span>
            <span className="user-footer__email">{user?.email}</span>
          </div>
        </div>
        <div className="user-footer__actions">
          <button
            className="icon-btn"
            onClick={() => setShowEdit(true)}
            title="Editar perfil"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M9.5 2.5l2 2L4 12H2v-2l7.5-7.5z" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
          <button className="icon-btn icon-btn--danger" onClick={logout} title="Sair">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M5 2H2v10h3M9 10l3-3-3-3M12 7H5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
        </div>
      </div>

      {showEdit && <EditProfileModal onClose={() => setShowEdit(false)} />}
    </>
  );
}
