import { requestJson } from './client.js';

export const authApi = {
  register: (username, email, password) =>
    requestJson('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, email, password }),
    }),

  login: (email, password) =>
    requestJson('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  refresh: (refreshToken) =>
    requestJson('/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),

  getMe: () => requestJson('/users/me'),

  updateProfile: (username) =>
    requestJson('/users/me', {
      method: 'PATCH',
      body: JSON.stringify({ username }),
    }),

  changePassword: (currentPassword, newPassword) =>
    requestJson('/users/me/password', {
      method: 'PUT',
      body: JSON.stringify({ currentPassword, newPassword }),
    }),
};
