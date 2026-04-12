import { useAuth } from '../../context/AuthContext.jsx';
import { useChat } from '../../context/ChatContext.jsx';
import { Avatar } from '../Avatar.jsx';

function formatTime(timestamp) {
  if (!timestamp) return '';
  const d = new Date(timestamp);
  return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

export function MessageItem({ message }) {
  const { user } = useAuth();
  const { setActiveChat } = useChat();
  const isOwn = message.senderId === user?.id;

  const handleStartPrivate = () => {
    if (isOwn) return;
    setActiveChat({
      type: 'private',
      data: { userId: message.senderId, username: message.senderUsername }
    });
  };

  return (
    <li className={`message ${isOwn ? 'message--own' : ''}`}>
      {!isOwn && (
        <div onClick={handleStartPrivate} className="message__avatar--clickable">
          <Avatar name={message.senderUsername} size="sm" />
        </div>
      )}
      <div className="message__body">
        {!isOwn && (
          <span className="message__sender message__sender--clickable" onClick={handleStartPrivate}>
            {message.senderUsername}
          </span>
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
