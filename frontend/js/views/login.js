// js/views/login.js

import { login } from '../api/auth.js';
import { getMe } from '../api/auth.js';
import { state } from '../state/store.js';

/**
 * Renderiza a tela de login.
 * @param {HTMLElement} root
 * @param {function} navigate
 */
export function renderLogin(root, navigate) {
  root.innerHTML = `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-card__logo">
          <div class="auth-card__logo-mark">ch</div>
          <span class="auth-card__logo-name">chat.app</span>
        </div>

        <h1 class="auth-card__title">Bem-vindo de volta</h1>
        <p class="auth-card__subtitle">Entre com sua conta para continuar</p>

        <div class="auth-error" id="auth-error"></div>

        <form class="auth-form" id="login-form" novalidate>
          <div class="form-field">
            <label class="form-field__label" for="email">E-mail</label>
            <input
              class="input"
              id="email"
              type="email"
              placeholder="seu@email.com"
              autocomplete="email"
              required
            />
            <span class="form-field__error" id="email-error"></span>
          </div>

          <div class="form-field">
            <label class="form-field__label" for="password">Senha</label>
            <input
              class="input"
              id="password"
              type="password"
              placeholder="••••••••"
              autocomplete="current-password"
              required
            />
            <span class="form-field__error" id="password-error"></span>
          </div>

          <button class="btn btn--primary" type="submit" id="btn-submit">
            Entrar
          </button>
        </form>

        <p class="auth-card__footer">
          Não tem conta?
          <a id="link-register">Criar conta</a>
        </p>
      </div>
    </div>
  `;

  const form        = root.querySelector('#login-form');
  const emailInput  = root.querySelector('#email');
  const passInput   = root.querySelector('#password');
  const emailError  = root.querySelector('#email-error');
  const passError   = root.querySelector('#password-error');
  const authError   = root.querySelector('#auth-error');
  const btnSubmit   = root.querySelector('#btn-submit');

  root.querySelector('#link-register').addEventListener('click', () => navigate('/register'));

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Reset erros
    emailError.textContent = '';
    passError.textContent  = '';
    authError.textContent  = '';
    authError.classList.remove('visible');

    const email    = emailInput.value.trim();
    const password = passInput.value;

    let valid = true;
    if (!email) { emailError.textContent = 'Informe o e-mail'; valid = false; }
    if (!password) { passError.textContent = 'Informe a senha'; valid = false; }
    if (!valid) return;

    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Entrando…';

    try {
      await login(email, password);
      // Carrega dados do usuário logado
      const me = await getMe();
      state.currentUser = {
        id:          me.id,
        email:       me.email,
        displayName: me.username ?? me.displayName,
      };
      navigate('/chat');
    } catch (err) {
      authError.textContent = err.message;
      authError.classList.add('visible');
      btnSubmit.disabled = false;
      btnSubmit.textContent = 'Entrar';
    }
  });
}
