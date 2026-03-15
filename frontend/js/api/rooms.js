// js/api/rooms.js

import { request } from './http.js';

export async function listRooms(page = 0, size = 50) {
  const res = await request(`/rooms?page=${page}&size=${size}`);
  if (!res?.ok) throw new Error('Erro ao listar salas');
  return res.json(); // PageResponseDTO<Room>
}

export async function createRoom(title) {
  const res = await request('/rooms', {
    method: 'POST',
    body: JSON.stringify({ title }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || 'Erro ao criar sala');
  return data;
}

export async function getRoomMembers(roomId, page = 0, size = 100) {
  const res = await request(`/rooms/${roomId}/members?page=${page}&size=${size}`);
  if (!res?.ok) throw new Error('Erro ao listar membros');
  return res.json(); // PageResponseDTO<String>
}

/**
 * Exclui a sala e todas as suas mensagens (o backend limpa MongoDB + cache Redis).
 */
export async function deleteRoom(roomId) {
  const res = await request(`/rooms/${roomId}`, { method: 'DELETE' });
  if (!res?.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || 'Erro ao excluir sala');
  }
}

/**
 * Remove o usuário autenticado da sala permanentemente.
 * O backend extrai userId/username do JWT e faz broadcast do LEAVE via WebSocket.
 */
export async function leaveRoom(roomId) {
  const res = await request(`/rooms/${roomId}/members`, { method: 'DELETE' });
  if (!res?.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || 'Erro ao sair da sala');
  }
}
