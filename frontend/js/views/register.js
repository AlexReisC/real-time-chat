// js/views/register.js

import { register } from '../api/auth.js';
import { getMe } from '../api/auth.js';
import { state } from '../state/store.js';

/**
 * Renderiza a tela de criação de conta.
 * @param {HTMLElement} root
 * @param {function} navigate
 */
export function renderRegister(root, navigate) {
  root.innerHTML = `
    <div class="auth-page">
      <div class="auth-card">
        <div class="auth-card__logo">
          <div class="auth-card__logo-mark">ch</div>
          <span class="auth-card__logo-name">chat.app</span>
        </div>

        <h1 class="auth-card__title">Criar conta</h1>
        <p class="auth-card__subtitle">Preencha os dados para começar</p>

        <div class="auth-error" id="auth-error"></div>

        <form class="auth-form" id="register-form" novalidate>
          <div class="form-field">
            <label class="form-field__label" for="displayName">Nome de exibição</label>
            <input
              class="input"
              id="displayName"
              type="text"
              placeholder="Como quer ser chamado"
              autocomplete="username"
              required
            />
            <span class="form-field__error" id="name-error"></span>
          </div>

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
              placeholder="Mínimo 8 caracteres"
              autocomplete="new-password"
              required
            />
            <span class="form-field__error" id="password-error"></span>
          </div>

          <button class="btn btn--primary" type="submit" id="btn-submit">
            Criar conta
          </button>
        </form>

        <p class="auth-card__footer">
          Já tem conta?
          <a id="link-login">Entrar</a>
        </p>
      </div>
    </div>
  `;

  const form        = root.querySelector('#register-form');
  const nameInput   = root.querySelector('#displayName');
  const emailInput  = root.querySelector('#email');
  const passInput   = root.querySelector('#password');
  const nameError   = root.querySelector('#name-error');
  const emailError  = root.querySelector('#email-error');
  const passError   = root.querySelector('#password-error');
  const authError   = root.querySelector('#auth-error');
  const btnSubmit   = root.querySelector('#btn-submit');

  root.querySelector('#link-login').addEventListener('click', () => navigate('/login'));

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    nameError.textContent  = '';
    emailError.textContent = '';
    passError.textContent  = '';
    authError.textContent  = '';
    authError.classList.remove('visible');

    const displayName = nameInput.value.trim();
    const email       = emailInput.value.trim();
    const password    = passInput.value;

    let valid = true;
    if (!displayName) { nameError.textContent = 'Informe um nome'; valid = false; }
    if (!email)       { emailError.textContent = 'Informe o e-mail'; valid = false; }
    if (!password)    { passError.textContent = 'Informe uma senha'; valid = false; }
    else if (password.length < 8) { passError.textContent = 'Mínimo 8 caracteres'; valid = false; }
    if (!valid) return;

    btnSubmit.disabled = true;
    btnSubmit.textContent = 'Criando…';

    try {
      await register(displayName, email, password);
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
      btnSubmit.textContent = 'Criar conta';
    }
  });
}
