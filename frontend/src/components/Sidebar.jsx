import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Modal } from './Modal';
import { FormInput } from './FormInput';
import modalStyles from './Modal.module.css';
import styles from './Sidebar.module.css';

function initials(name) {
  return name.slice(0, 2).toUpperCase();
}

export function Sidebar({
  rooms,
  dms,
  activeChat,
  onSelectChat,
  onCreateRoom,
  onDeleteRoom,
}) {
  const { user, updateProfile } = useAuth();

  const [newRoomOpen, setNewRoomOpen]       = useState(false);
  const [deleteOpen, setDeleteOpen]         = useState(false);
  const [profileOpen, setProfileOpen]       = useState(false);
  const [pendingRoom, setPendingRoom]       = useState(null);

  const [roomName, setRoomName]             = useState('');
  const [roomError, setRoomError]           = useState('');

  const [editUsername, setEditUsername]       = useState('');
  const [editCurrentPass, setEditCurrentPass] = useState('');
  const [editPassword, setEditPassword]       = useState('');
  const [editConfirm, setEditConfirm]         = useState('');
  const [editError, setEditError]             = useState('');

  // ── New room ───────────────────────────────────────────────────────────
  function openNewRoom() {
    setRoomName('');
    setRoomError('');
    setNewRoomOpen(true);
  }

  function handleCreateRoom() {
    const name = roomName.trim().toLowerCase().replace(/\s+/g, '-');
    if (name.length < 2) { setRoomError('Mínimo 2 caracteres'); return; }
    onCreateRoom(name);
    setNewRoomOpen(false);
  }

  // ── Delete room ────────────────────────────────────────────────────────
  function openDelete(room, e) {
    e.stopPropagation();
    setPendingRoom(room);
    setDeleteOpen(true);
  }

  function handleConfirmDelete() {
    if (pendingRoom) onDeleteRoom(pendingRoom.id);
    setDeleteOpen(false);
    setPendingRoom(null);
  }

  // ── Profile ────────────────────────────────────────────────────────────
  function openProfile() {
    setEditUsername(user?.username ?? '');
    setEditCurrentPass('');
    setEditPassword('');
    setEditConfirm('');
    setEditError('');
    setProfileOpen(true);
  }

  async function handleSaveProfile() {
    if (editPassword && !editCurrentPass) {
      setEditError('Informe a senha atual para alterá-la');
      return;
    }
    if (editPassword && editPassword !== editConfirm) {
      setEditError('Senhas não coincidem');
      return;
    }
    await updateProfile(
      editUsername,
      editCurrentPass || null,
      editPassword   || null,
    );
    setProfileOpen(false);
  }

  // ── Render ─────────────────────────────────────────────────────────────
  return (
    <>
      <aside className={styles.sidebar}>
        {/* Header */}
        <div className={styles.header}>
          <span className={styles.brand}>RELAY</span>
          <button className={styles.iconBtn} onClick={openNewRoom} title="Nova sala">+</button>
        </div>

        {/* Scrollable lists */}
        <div className={styles.lists}>
          <div className={styles.sectionLabel}>Salas</div>
          {rooms.map(room => (
            <div
              key={room.id}
              className={`${styles.item} ${activeChat === `r${room.id}` ? styles.active : ''}`}
              onClick={() => onSelectChat(`r${room.id}`)}
            >
              <div className={styles.itemIcon}>#</div>
              <div className={styles.itemInfo}>
                <div className={styles.itemName}>{room.name}</div>
                <div className={styles.itemMeta}>{room.members?.length ?? 0} membros</div>
              </div>
              <button
                className={styles.deleteBtn}
                onClick={(e) => openDelete(room, e)}
                title="Deletar sala"
              >×</button>
            </div>
          ))}

          <div className={styles.divider} />

          <div className={styles.sectionLabel}>Mensagens diretas</div>
          {dms.map(dm => (
            <div
              key={dm.id}
              className={`${styles.item} ${activeChat === `d${dm.id}` ? styles.active : ''}`}
              onClick={() => onSelectChat(`d${dm.id}`)}
            >
              <div className={`${styles.itemIcon} ${styles.dmIcon}`}>{initials(dm.username)}</div>
              <div className={styles.itemInfo}>
                <div className={styles.itemName}>{dm.username}</div>
                <div className={styles.itemMeta}>{dm.online ? 'online' : 'offline'}</div>
              </div>
              {dm.online && <div className={styles.presenceDot} />}
            </div>
          ))}
        </div>

        {/* Footer / current user */}
        <div className={styles.footer}>
          <div className={styles.userAvatar}>{initials(user?.username ?? 'ME')}</div>
          <div className={styles.userInfo}>
            <div className={styles.userName}>{user?.username}</div>
            <div className={styles.userStatus}>
              <div className={styles.onlineDot} /> online
            </div>
          </div>
          <button className={styles.editBtn} onClick={openProfile} title="Editar perfil">✎</button>
        </div>
      </aside>

      {/* ── Modal: Nova sala ──────────────────────────────────────────── */}
      <Modal open={newRoomOpen} onClose={() => setNewRoomOpen(false)}>
        <div className={modalStyles.title}>Nova sala</div>
        <div className={modalStyles.sub}>Crie um espaço para sua equipe colaborar</div>
        <FormInput
          label="Nome da sala"
          value={roomName}
          onChange={(v) => { setRoomName(v); setRoomError(''); }}
          placeholder="ex: frontend, devops, design"
          error={roomError}
          autoFocus
        />
        <div className={modalStyles.footer}>
          <button className={modalStyles.btnSecondary} onClick={() => setNewRoomOpen(false)}>
            Cancelar
          </button>
          <button className={modalStyles.btnAccent} onClick={handleCreateRoom}>
            Criar sala
          </button>
        </div>
      </Modal>

      {/* ── Modal: Deletar sala ───────────────────────────────────────── */}
      <Modal open={deleteOpen} onClose={() => setDeleteOpen(false)}>
        <div className={modalStyles.title}>Deletar sala</div>
        <div className={modalStyles.sub}>
          Esta ação é irreversível. Todas as mensagens serão perdidas.
        </div>
        {pendingRoom && (
          <div className={styles.deleteWarning}>
            Sala: <strong>#{pendingRoom.name}</strong>
          </div>
        )}
        <div className={modalStyles.footer}>
          <button className={modalStyles.btnSecondary} onClick={() => setDeleteOpen(false)}>
            Cancelar
          </button>
          <button className={modalStyles.btnDanger} onClick={handleConfirmDelete}>
            Deletar
          </button>
        </div>
      </Modal>

      {/* ── Modal: Editar perfil ──────────────────────────────────────── */}
      <Modal open={profileOpen} onClose={() => setProfileOpen(false)}>
        <div className={modalStyles.title}>Editar perfil</div>
        <div className={modalStyles.sub}>Atualize suas informações de conta</div>
        <FormInput
          label="Nome de usuário"
          value={editUsername}
          onChange={setEditUsername}
          placeholder="seu_username"
        />
        <FormInput
          label="Senha atual"
          type="password"
          value={editCurrentPass}
          onChange={setEditCurrentPass}
          placeholder="Obrigatório para trocar a senha"
        />
        <FormInput
          label="Nova senha"
          type="password"
          value={editPassword}
          onChange={setEditPassword}
          placeholder="Deixe vazio para não alterar"
        />
        <FormInput
          label="Confirmar nova senha"
          type="password"
          value={editConfirm}
          onChange={setEditConfirm}
          placeholder="••••••••"
          error={editError}
        />
        <div className={modalStyles.footer}>
          <button className={modalStyles.btnSecondary} onClick={() => setProfileOpen(false)}>
            Cancelar
          </button>
          <button className={modalStyles.btnAccent} onClick={handleSaveProfile}>
            Salvar
          </button>
        </div>
      </Modal>
    </>
  );
}
