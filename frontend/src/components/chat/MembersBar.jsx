import { useChat } from '../../context/ChatContext.jsx';
import { Avatar } from '../Avatar.jsx';

export function MembersBar() {
  const { members } = useChat();

  return (
    <aside className="members-bar">
      <span className="members-bar__label">Membros — {members.length}</span>
      <ul className="members-list">
        {members.map((member) => (
          <li key={member.id ?? member} className="member-item">
            <Avatar name={member.username ?? member} size="xs" />
            <span className="member-item__name">{member.username ?? member}</span>
          </li>
        ))}
      </ul>
    </aside>
  );
}
