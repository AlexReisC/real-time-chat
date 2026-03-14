import { useEffect, useRef, useCallback } from 'react';

/**
 * useWebSocket
 *
 * Manages a STOMP-over-WebSocket connection to the chat service.
 * Subscribes to room and private-message topics and exposes a send function.
 *
 * TODO: install @stomp/stompjs  →  npm install @stomp/stompjs
 * TODO: uncomment real implementation below and remove mock stubs.
 */
export function useWebSocket({ token, onMessage }) {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!token) return;

    // ── Real STOMP implementation ──────────────────────────────────────────
    // import { Client } from '@stomp/stompjs';
    //
    // const client = new Client({
    //   brokerURL: `ws://localhost:8080/ws`,           // via API Gateway
    //   connectHeaders: { Authorization: `Bearer ${token}` },
    //   reconnectDelay: 5000,
    //   onConnect: () => {
    //     // Subscribe to room broadcasts
    //     client.subscribe('/topic/rooms/*', (frame) => {
    //       onMessage(JSON.parse(frame.body));
    //     });
    //     // Subscribe to private messages
    //     client.subscribe('/user/queue/private', (frame) => {
    //       onMessage(JSON.parse(frame.body));
    //     });
    //   },
    //   onStompError: (frame) => console.error('STOMP error', frame),
    // });
    // client.activate();
    // clientRef.current = client;
    //
    // return () => client.deactivate();
    // ──────────────────────────────────────────────────────────────────────

    // Mock stub — remove when real WS is connected
    console.log('[WS] mock connection established');
    return () => console.log('[WS] mock connection closed');
  }, [token, onMessage]);

  /**
   * Send a message to a room or a private user.
   * @param {'room'|'private'} type
   * @param {string} destinationId  - roomId or recipientUserId
   * @param {string} content
   */
  const send = useCallback((type, destinationId, content) => {
    // Real implementation:
    // if (!clientRef.current?.connected) return;
    // const destination = type === 'room'
    //   ? `/app/chat.room.${destinationId}`
    //   : `/app/chat.private`;
    // clientRef.current.publish({
    //   destination,
    //   body: JSON.stringify({ destinationId, content }),
    // });

    // Mock stub — remove when real WS is connected
    console.log(`[WS] send → ${type}:${destinationId}`, content);
  }, []);

  return { send };
}
