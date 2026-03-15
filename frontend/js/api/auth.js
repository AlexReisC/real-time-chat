// js/api/auth.js

import { request, BASE_URL } from '../api/http.js';
import { setTokens } from '../state/store.js';

export async function login(email, password) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Erro ao fazer login');

  setTokens(data.accessToken, data.refreshToken);
  return data;
}

export async function register(displayName, email, password) {
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    // Backend espera o campo como "username"
    body: JSON.stringify({ username: displayName, email, password }),
  });

  const data = await res.json();
  if (!res.ok) {
    const msg = data.errors
      ? Object.values(data.errors).join(', ')
      : (data.message || 'Erro ao criar conta');
    throw new Error(msg);
  }

  setTokens(data.accessToken, data.refreshToken);
  return data;
}

export async function getMe() {
  const res = await request('/users/me');
  if (!res?.ok) throw new Error('Não autenticado');
  return res.json();
}

export async function updateUsername(displayName) {
  const res = await request('/users/me', {
    method: 'PATCH',
    body: JSON.stringify({ username: displayName }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Erro ao atualizar');
  return data;
}

export async function updatePassword(currentPassword, newPassword) {
  const res = await request('/users/me/password', {
    method: 'PUT',
    body: JSON.stringify({ currentPassword, newPassword }),
  });
  if (!res.ok) {
    const data = await res.json();
    throw new Error(data.message || 'Erro ao atualizar senha');
  }
}
