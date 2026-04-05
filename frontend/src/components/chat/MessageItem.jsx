import { useAuth } from '../../context/AuthContext.jsx';
import { Avatar } from '../Avatar.jsx';

function formatTime(timestamp) {
  if (!timestamp) return '';
  const d = new Date(timestamp);
  return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

export function MessageItem({ message }) {
  const { user } = useAuth();
  const isOwn = message.senderId === user?.id;

  return (
    <li className={`message ${isOwn ? 'message--own' : ''}`}>
      {!isOwn && <Avatar name={message.senderUsername} size="sm" />}
      <div className="message__body">
        {!isOwn && (
          <span className="message__sender">{message.senderUsername}</span>
        )}
        <div className="message__bubble">
          <p className="message__text">{message.content}</p>
          <span className="message__time">{formatTime(message.timestamp)}</span>
        </div>
      </div>
      {isOwn && <Avatar name={message.senderUsername} size="sm" />}
    </li>
  );
}
