import styles from './FormInput.module.css';

export function FormInput({
  label,
  type = 'text',
  value,
  onChange,
  placeholder,
  error,
  autoFocus,
}) {
  return (
    <div className={styles.group}>
      {label && <label className={styles.label}>{label}</label>}
      <input
        type={type}
        className={`${styles.input} ${error ? styles.hasError : ''}`}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoFocus={autoFocus}
        autoComplete={type === 'password' ? 'current-password' : 'off'}
      />
      {error && <span className={styles.error}>{error}</span>}
    </div>
  );
}
