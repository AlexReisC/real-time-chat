import { useRef, useState } from 'react';
import { useAutoResize } from '../../hooks/useAutoResize.js';

export function MessageInput({ onSend, disabled }) {
  const [value, setValue] = useState('');
  const textareaRef = useRef(null);
  useAutoResize(textareaRef, value);

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  }

  function submit() {
    const content = value.trim();
    if (!content || disabled) return;
    onSend(content);
    setValue('');
  }

  return (
    <div className="message-input">
      <textarea
        ref={textareaRef}
        className="message-input__textarea"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Escreva uma mensagem..."
        rows={1}
        maxLength={2000}
        disabled={disabled}
      />
      <button
        className="message-input__send"
        onClick={submit}
        disabled={!value.trim() || disabled}
        title="Enviar"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M14 2L2 7l5 2 2 5 5-12z" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
    </div>
  );
}
