import { useEffect } from 'react';
import styles from './Modal.module.css';

export function Modal({ open, onClose, children }) {
  useEffect(() => {
    if (!open) return;
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [open, onClose]);

  return (
    <div
      className={`${styles.overlay} ${open ? styles.open : ''}`}
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className={styles.card}>
        {children}
      </div>
    </div>
  );
}
