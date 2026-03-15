// js/app.js
// Roteador SPA mínimo e ponto de entrada da aplicação.

import { renderLogin }    from './views/login.js';
import { renderRegister } from './views/register.js';
import { renderChat }     from './views/chat.js';
import { getTokens, clearTokens } from './state/store.js';
import { getMe } from './api/auth.js';
import { state } from './state/store.js';

const routes = {
  '/login':    renderLogin,
  '/register': renderRegister,
  '/chat':     renderChat,
};

const root = document.getElementById('app');

export function navigate(path) {
  window.history.pushState({}, '', path);
  render(path);
}

function render(path) {
  const view = routes[path];
  if (!view) {
    navigate('/login');
    return;
  }
  root.innerHTML = '';
  view(root, navigate);
}

// Botão voltar/avançar do browser
window.addEventListener('popstate', () => render(window.location.pathname));

// Logout disparado pelo http.js ao falhar o refresh
window.addEventListener('auth:logout', () => {
  clearTokens();
  navigate('/login');
});

// ── Bootstrap ────────────────────────────────────────────
// Se há token mas o estado em memória foi perdido (ex: reload da página),
// recarrega os dados do usuário antes de renderizar o chat.
async function bootstrap() {
  const { accessToken } = getTokens();

  if (!accessToken) {
    navigate('/login');
    return;
  }

  // Token existe mas currentUser foi perdido — busca no backend
  if (!state.currentUser) {
    try {
      const me = await getMe();
      state.currentUser = {
        id:          me.id,
        email:       me.email,
        displayName: me.username ?? me.displayName,
      };
    } catch {
      // Token inválido ou expirado — limpa e manda para login
      clearTokens();
      navigate('/login');
      return;
    }
  }

  const path = window.location.pathname;
  render(path === '/' ? '/chat' : path);
}

bootstrap();