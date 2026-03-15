// js/api/http.js
// Cliente HTTP centralizado com refresh silencioso de token.

import { getTokens, setTokens, clearTokens } from '../state/store.js';

export const BASE_URL = 'http://localhost:8080/api/v1';

// Previne múltiplos refreshes simultâneos
let _refreshPromise = null;

async function doRefresh() {
  const { refreshToken } = getTokens();
  if (!refreshToken) throw new Error('No refresh token');

  const res = await fetch(`${BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) throw new Error('Refresh failed');

  const data = await res.json();
  setTokens(data.accessToken, data.refreshToken);
  return data.accessToken;
}

async function refreshAccessToken() {
  // Se já há um refresh em andamento, aguarda o mesmo
  if (!_refreshPromise) {
    _refreshPromise = doRefresh().finally(() => { _refreshPromise = null; });
  }
  return _refreshPromise;
}

function buildHeaders(token, extra = {}) {
  return {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...extra,
  };
}

export async function request(path, options = {}) {
  const { accessToken } = getTokens();

  let res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: buildHeaders(accessToken, options.headers),
  });

  if (res.status === 401) {
    try {
      const newToken = await refreshAccessToken();
      res = await fetch(`${BASE_URL}${path}`, {
        ...options,
        headers: buildHeaders(newToken, options.headers),
      });
    } catch {
      clearTokens();
      // Avisa o app sem criar importação circular
      window.dispatchEvent(new CustomEvent('auth:logout'));
      return null;
    }
  }

  return res;
}
