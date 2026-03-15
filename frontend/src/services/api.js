/**
 * api.js — HTTP client for the Chat backend
 *
 * Base URL resolves to the API Gateway (port 8080 by default).
 * All requests attach the JWT from localStorage automatically.
 *
 * TODO: npm install axios
 * TODO: set VITE_API_URL in .env  →  VITE_API_URL=http://localhost:8080
 */

const BASE_URL = import.meta.env.VITE_API_URL ?? '';

function getToken() {
  return localStorage.getItem('relay_token');
}

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(error.message ?? 'Request failed');
  }

  if (res.status === 204) return null;
  return res.json();
}

// ── Auth — /api/v1/auth ───────────────────────────────────────────────────────

export const authApi = {
  /** POST /api/v1/auth/login → { token, refreshToken, ... } */
  login: (email, password) =>
    request('POST', '/api/v1/auth/login', { email, password }),

  /** POST /api/v1/auth/register → { token, refreshToken, ... } */
  register: (username, email, password) =>
    request('POST', '/api/v1/auth/register', { username, email, password }),

  /** POST /api/v1/auth/refresh → { token } */
  refresh: (refreshToken) =>
    request('POST', '/api/v1/auth/refresh', { refreshToken }),
};

// ── User — /api/v1/users ──────────────────────────────────────────────────────

export const userApi = {
  /** GET /api/v1/users/me → User */
  me: () =>
    request('GET', '/api/v1/users/me'),

  /** PATCH /api/v1/users/me → User (atualizar username) */
  updateMe: (username) =>
    request('PATCH', '/api/v1/users/me', { username }),

  /** PUT /api/v1/users/me/password → 204 */
  updatePassword: (currentPassword, newPassword) =>
    request('PUT', '/api/v1/users/me/password', { currentPassword, newPassword }),
};

// ── Rooms — /api/v1/rooms ─────────────────────────────────────────────────────

export const roomsApi = {
  /** GET /api/v1/rooms → Room[] */
  list: () =>
    request('GET', '/api/v1/rooms'),

  /** POST /api/v1/rooms → Room  —  CreateRoomDTO usa `title`, não `name` */
  create: (title) =>
    request('POST', '/api/v1/rooms', { title }),

  /** GET /api/v1/rooms/{roomId}/members → User[] */
  members: (roomId) =>
    request('GET', `/api/v1/rooms/${roomId}/members`),
};

// ── Messages — /api/v1/messages ───────────────────────────────────────────────

export const messagesApi = {
  /**
   * GET /api/v1/messages/room/{roomId}
   * Retorna List<ResponseMessageDTO> do cache Redis (não paginado).
   * Fallback automático para MongoDB se o cache estiver vazio.
   */
  roomHistory: (roomId) =>
    request('GET', `/api/v1/messages/room/${roomId}`),

  /**
   * GET /api/v1/messages/private
   * Lista as conversas privadas do usuário autenticado.
   */
  privateConversations: () =>
    request('GET', '/api/v1/messages/private'),

  /**
   * GET /api/v1/messages/private/{targetUserId}
   * Retorna List<ResponseMessageDTO> do cache Redis com o usuário alvo.
   * Fallback automático para MongoDB se o cache estiver vazio.
   */
  privateHistory: (targetUserId) =>
    request('GET', `/api/v1/messages/private/${targetUserId}`),
};