// js/api/messages.js

import { request } from './http.js';

// ── Cache (últimas 50 mensagens, retorna List diretamente) ────────────────────

/**
 * Busca as últimas 50 mensagens de uma sala via cache Redis.
 * Retorna List<ResponseMessageDTO> — sem wrapper de paginação.
 */
export async function getRoomMessages(roomId) {
  const res = await request(`/messages/room/${roomId}`);
  if (!res?.ok) throw new Error('Erro ao carregar mensagens');
  return res.json(); // List<ResponseMessageDTO>
}

/**
 * Busca as últimas 50 mensagens de uma conversa privada via cache Redis.
 * Retorna List<ResponseMessageDTO> — sem wrapper de paginação.
 */
export async function getPrivateMessages(targetUserId) {
  const res = await request(`/messages/private/${targetUserId}`);
  if (!res?.ok) throw new Error('Erro ao carregar conversa');
  return res.json(); // List<ResponseMessageDTO>
}

// ── Histórico paginado (MongoDB) ──────────────────────────────────────────────

/**
 * Busca o histórico paginado de uma sala.
 * Retorna PageResponseDTO<ResponseMessageDTO>.
 */
export async function getRoomHistory(roomId, page = 1, size = 50) {
  const res = await request(`/messages/room/${roomId}/history?page=${page}&size=${size}`);
  if (!res?.ok) throw new Error('Erro ao carregar histórico');
  return res.json(); // PageResponseDTO<ResponseMessageDTO>
}

/**
 * Busca o histórico paginado de uma conversa privada.
 * Retorna PageResponseDTO<ResponseMessageDTO>.
 */
export async function getPrivateHistory(targetUserId, page = 1, size = 50) {
  const res = await request(`/messages/private/${targetUserId}/history?page=${page}&size=${size}`);
  if (!res?.ok) throw new Error('Erro ao carregar histórico');
  return res.json(); // PageResponseDTO<ResponseMessageDTO>
}

// ── Conversas privadas ────────────────────────────────────────────────────────

/**
 * Lista as conversas privadas existentes do usuário autenticado.
 * Retorna List<ConversationSummaryDTO> com { contactId, contactUsername, content, timestamp }.
 */
export async function getPrivateConversations() {
  const res = await request('/messages/private/conversations');
  if (!res?.ok) throw new Error('Erro ao carregar conversas');
  return res.json(); // List<ConversationSummaryDTO>
}
