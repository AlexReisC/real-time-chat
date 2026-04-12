import { useChat } from '../../context/ChatContext.jsx';
import { useAuth } from '../../context/AuthContext.jsx';
import { Avatar } from '../Avatar.jsx';

export function MembersBar() {
  const { members = [], setActiveChat } = useChat();
  const { user } = useAuth();

  const handleMemberClick = (member) => {
    if (member.userId === user?.id) return;
    setActiveChat({ type: 'private', data: member });
  };

  return (
    <aside className="members-bar">
      <span className="members-bar__label">Membros — {members.length}</span>
      <ul className="members-list">
        {members.map((member) => (
          <li
            key={member.userId}
            className={`member-item ${member.userId !== user?.id ? 'member-item--clickable' : ''}`}
            onClick={() => handleMemberClick(member)}
          >
            <Avatar name={member.username} size="xs" />
            <span className="member-item__name">{member.username}</span>
          </li>
        ))}
      </ul>
    </aside>
  );
}
