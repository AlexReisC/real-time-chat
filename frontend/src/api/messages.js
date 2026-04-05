import { requestJson } from './client.js';

export const messagesApi = {
  loadRoomCache: (roomId) =>
    requestJson(`/messages/room/${roomId}`),

  loadRoomHistory: (roomId, page = 0, size = 50) =>
    requestJson(`/messages/room/${roomId}/history?page=${page}&size=${size}`),

  loadPrivateCache: (targetUserId) =>
    requestJson(`/messages/private/${targetUserId}`),

  loadPrivateHistory: (targetUserId, page = 0, size = 50) =>
    requestJson(`/messages/private/${targetUserId}/history?page=${page}&size=${size}`),

  listPrivateConversations: () =>
    requestJson('/private/conversations'),
};
