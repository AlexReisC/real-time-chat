import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { FormInput } from '../components/FormInput';
import styles from './LoginPage.module.css';

export function LoginPage() {
  const { login, register } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [loading, setLoading] = useState(false);

  const [username, setUsername] = useState('');
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm]   = useState('');
  const [errors, setErrors]     = useState({});

  function validate() {
    const e = {};
    if (isRegister && username.trim().length < 3)
      e.username = 'Mínimo 3 caracteres';
    if (!email.includes('@'))
      e.email = 'Email inválido';
    if (password.length < 6)
      e.password = 'Mínimo 6 caracteres';
    if (isRegister && confirm !== password)
      e.confirm = 'Senhas não coincidem';
    return e;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }

    setLoading(true);
    try {
      if (isRegister) {
        await register(username.trim(), email.trim(), password);
      } else {
        await login(email.trim(), password);
      }
    } catch (err) {
      setErrors({ global: err.message });
    } finally {
      setLoading(false);
    }
  }

  function toggle() {
    setIsRegister(v => !v);
    setErrors({});
    setUsername(''); setEmail(''); setPassword(''); setConfirm('');
  }

  return (
    <div className={styles.screen}>
      <div className={styles.bg} />
      <div className={styles.grid} />

      <form className={styles.card} onSubmit={handleSubmit} noValidate>
        <div className={styles.logo}>RELAY //</div>
        <h1 className={styles.title}>
          {isRegister ? 'Criar conta' : 'Entrar na conta'}
        </h1>
        <p className={styles.sub}>
          {isRegister ? 'Junte-se ao seu workspace' : 'Acesse seu workspace de chat'}
        </p>

        {errors.global && (
          <div className={styles.globalError}>{errors.global}</div>
        )}

        {isRegister && (
          <FormInput
            label="Nome de usuário"
            value={username}
            onChange={setUsername}
            placeholder="ex: alex_dev"
            error={errors.username}
            autoFocus
          />
        )}

        <FormInput
          label="Email"
          type="email"
          value={email}
          onChange={setEmail}
          placeholder="voce@email.com"
          error={errors.email}
          autoFocus={!isRegister}
        />

        <FormInput
          label="Senha"
          type="password"
          value={password}
          onChange={setPassword}
          placeholder="••••••••"
          error={errors.password}
        />

        {isRegister && (
          <FormInput
            label="Confirmar senha"
            type="password"
            value={confirm}
            onChange={setConfirm}
            placeholder="••••••••"
            error={errors.confirm}
          />
        )}

        <button className={styles.submit} type="submit" disabled={loading}>
          {loading ? 'Aguarde...' : isRegister ? 'Criar conta' : 'Entrar'}
        </button>

        <p className={styles.toggleRow}>
          {isRegister ? 'Já tem conta?' : 'Não tem conta?'}
          <button type="button" className={styles.toggleBtn} onClick={toggle}>
            {isRegister ? ' Entrar' : ' Criar conta'}
          </button>
        </p>
      </form>
    </div>
  );
}
