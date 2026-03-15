// js/state/store.js
// Estado global da aplicação — tokens em sessionStorage, resto em memória.

// ── Tokens ───────────────────────────────────────────────
export function getTokens() {
  return {
    accessToken:  sessionStorage.getItem('accessToken'),
    refreshToken: sessionStorage.getItem('refreshToken'),
  };
}

export function setTokens(accessToken, refreshToken) {
  sessionStorage.setItem('accessToken', accessToken);
  sessionStorage.setItem('refreshToken', refreshToken);
}

export function clearTokens() {
  sessionStorage.removeItem('accessToken');
  sessionStorage.removeItem('refreshToken');
}

// ── Estado em memória ────────────────────────────────────
// Perdido no reload — aceitável para portfólio.
let _currentUser   = null; // { id, email, displayName }
let _activeRoom    = null; // { id, title } | null
let _activePrivate = null; // { userId, username } | null

export const state = {
  get currentUser()    { return _currentUser; },
  set currentUser(v)   { _currentUser = v; },

  get activeRoom()     { return _activeRoom; },
  set activeRoom(v)    { _activeRoom = v; },

  get activePrivate()  { return _activePrivate; },
  set activePrivate(v) { _activePrivate = v; },
};
