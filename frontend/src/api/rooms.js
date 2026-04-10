import { requestJson } from './client.js';

export const roomsApi = {
  list: (page = 0, size = 50) =>
    requestJson(`/rooms?page=${page}&size=${size}`),

  create: (title) =>
    requestJson('/rooms', {
      method: 'POST',
      body: JSON.stringify({ title }),
    }),

  delete: (roomId) =>
    requestJson(`/rooms/${roomId}`, { method: 'DELETE' }),

  getMembers: (roomId, page = 0, size = 50) =>
    requestJson(`/rooms/${roomId}/members?page=${page}&size=${size}`),

  addMember: (roomId) =>
    requestJson(`/rooms/${roomId}/members`, { method: 'POST' }),

  removeMember: (roomId) =>
    requestJson(`/rooms/${roomId}/members`, { method: 'DELETE' }),
};
