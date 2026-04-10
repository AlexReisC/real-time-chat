import { useChat } from '../../context/ChatContext.jsx';
import { Avatar } from '../Avatar.jsx';

export function MembersBar() {
  const { members } = useChat();

  return (
    <aside className="members-bar">
      <span className="members-bar__label">Membros — {members.length}</span>
      <ul className="members-list">
        {members.map((member) => (
          <li key={member.userId} className="member-item">
            <Avatar name={member.username} size="xs" />
            <span className="member-item__name">{member.username}</span>
          </li>
        ))}
      </ul>
    </aside>
  );
}
