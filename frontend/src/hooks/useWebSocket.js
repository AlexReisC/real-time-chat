import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';

/**
 * useWebSocket
 *
 * Gerencia a conexão STOMP sobre WebSocket com o Chat Service via API Gateway.
 *
 * Responsabilidades:
 *  - Conectar com o JWT no header STOMP (não em query param — evita exposição nos logs do servidor)
 *  - Subscrever mensagens privadas:   /user/queue/private
 *  - Subscrever erros do servidor:    /user/queue/errors
 *  - Expor joinRoom, leaveRoom, sendPublic, sendPrivate, subscribeToRoom
 *
 * Destinos STOMP (prefixo /app configurado no WebSocketConfig do backend):
 *  - /app/chat.addUser      → ChatController#addUser
 *  - /app/chat.removeUser   → ChatController#removeUser
 *  - /app/chat.sendPublic   → ChatController#sendPublicMessage
 *  - /app/chat.sendPrivate  → ChatController#sendPrivateMessage
 *
 * Tópicos de subscrição:
 *  - /topic/rooms.{roomId}  → broadcast da sala (ResponseMessageDTO | UserNotificationResponseDTO)
 *  - /user/queue/private    → mensagens privadas recebidas (ResponseMessageDTO)
 *  - /user/queue/errors     → erros do @MessageExceptionHandler (ErrorResponse)
 */
export function useWebSocket({ token, onMessage, onNotification, onError }) {
  const clientRef           = useRef(null);
  const roomSubscriptionRef = useRef(null);

  // ── Conexão ──────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!token) return;

    const WS_URL = import.meta.env.VITE_WS_URL ?? '';

    const client = new Client({
      brokerURL: WS_URL + '?token=${token}',

      // connectHeaders: {
      //   Authorization: `Bearer ${token}`,
      // },

      reconnectDelay: 5000,

      onConnect: () => {
        console.log('[WS] conectado');

        // Mensagens privadas — subscrição permanente durante a sessão
        client.subscribe('/user/queue/private', (frame) => {
          try {
            const msg = JSON.parse(frame.body);
            onMessage(normalizeMessage(msg));
          } catch (e) {
            console.error('[WS] falha ao parsear mensagem privada', e);
          }
        });

        // Erros retornados pelo @MessageExceptionHandler do backend
        client.subscribe('/user/queue/errors', (frame) => {
          try {
            const err = JSON.parse(frame.body);
            console.error('[WS] erro do servidor:', err);
            onError?.(err);
          } catch (e) {
            console.error('[WS] falha ao parsear erro WS', e);
          }
        });
      },

      onStompError:    (frame) => console.error('[WS] STOMP error:', frame.headers?.message),
      onDisconnect:    ()      => console.log('[WS] desconectado'),
      onWebSocketError:(event) => console.error('[WS] WebSocket error:', event),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      roomSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = null;
      client.deactivate();
    };
  // Callbacks (onMessage etc.) são estabilizados com useCallback em ChatPage —
  // não incluí-los aqui evita reconexões desnecessárias a cada render.
  }, [token]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Subscrição de sala ────────────────────────────────────────────────────
  /**
   * Inscreve no tópico de broadcast de uma sala.
   * Cancela automaticamente a subscrição da sala anterior.
   * Deve ser chamado APÓS o joinRoom ser enviado.
   *
   * Recebe dois shapes diferentes no mesmo tópico:
   *  - ResponseMessageDTO:          type = 'ROOM'
   *  - UserNotificationResponseDTO: type = 'JOIN' | 'LEAVE'
   */
  const subscribeToRoom = useCallback((roomId) => {
    const client = clientRef.current;
    if (!client?.connected) return;

    roomSubscriptionRef.current?.unsubscribe();

    roomSubscriptionRef.current = client.subscribe(
      `/topic/rooms.${roomId}`,
      (frame) => {
        try {
          const payload = JSON.parse(frame.body);

          if (payload.type === 'JOIN' || payload.type === 'LEAVE') {
            onNotification?.(payload);
          } else {
            onMessage(normalizeMessage(payload));
          }
        } catch (e) {
          console.error('[WS] falha ao parsear payload de sala', e);
        }
      }
    );
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const unsubscribeFromRoom = useCallback(() => {
    roomSubscriptionRef.current?.unsubscribe();
    roomSubscriptionRef.current = null;
  }, []);

  // ── Publicação ────────────────────────────────────────────────────────────

  /** UserNotificationDTO { room_id, type } */
  const joinRoom = useCallback((roomId) => {
    clientRef.current?.publish({
      destination: '/app/chat.addUser',
      body: JSON.stringify({ room_id: roomId, type: 'JOIN' }),
    });
  }, []);

  /** UserNotificationDTO { room_id, type } */
  const leaveRoom = useCallback((roomId) => {
    clientRef.current?.publish({
      destination: '/app/chat.removeUser',
      body: JSON.stringify({ room_id: roomId, type: 'LEAVE' }),
    });
  }, []);

  /** PublicMessageDTO { room_id, content } */
  const sendPublic = useCallback((roomId, content) => {
    clientRef.current?.publish({
      destination: '/app/chat.sendPublic',
      body: JSON.stringify({ room_id: roomId, content }),
    });
  }, []);

  /** PrivateMessageDTO { recipient_id, content } */
  const sendPrivate = useCallback((recipientId, content) => {
    clientRef.current?.publish({
      destination: '/app/chat.sendPrivate',
      body: JSON.stringify({ recipient_id: recipientId, content }),
    });
  }, []);

  return { joinRoom, leaveRoom, sendPublic, sendPrivate, subscribeToRoom, unsubscribeFromRoom };
}

// ── Normalização ─────────────────────────────────────────────────────────────
/**
 * Converte ResponseMessageDTO do backend para o shape interno do frontend.
 *
 * Backend (ResponseMessageDTO):
 *   { id, type, roomId, senderId, senderName, recipientId, content, timestamp }
 *
 * ATENÇÃO: o campo é `senderName` no record Java — não `senderUsername`.
 *
 * _chatKey: chave usada para indexar mensagens no estado do ChatPage:
 *   - mensagens de sala:    'r{roomId}'
 *   - mensagens privadas:   'd{senderId}' (identifica a conversa pelo remetente ao receber)
 */
function normalizeMessage(msg) {
  return {
    id:          msg.id,
    type:        msg.type,
    senderName:  msg.senderName,
    senderId:    msg.senderId,
    recipientId: msg.recipientId,
    roomId:      msg.roomId,
    content:     msg.content,
    timestamp:   msg.timestamp,
    _chatKey: msg.type === 'ROOM'
      ? `r${msg.roomId}`
      : `d${msg.senderId}`,
  };
}