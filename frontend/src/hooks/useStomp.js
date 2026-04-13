import { useRef, useEffect, useCallback, useMemo } from 'react';
import { Client } from '@stomp/stompjs';

const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws/chat';

export function useStomp(token) {
  const clientRef = useRef(null);
  const subscriptionsRef = useRef(new Map()); // id → StompSubscription
  const pendingRef = useRef(new Map()); // id → { destination, callback }
  const connectedRef = useRef(false);
  const subCounterRef = useRef(0);

  useEffect(() => {
    if (!token) return;

    const client = new Client({
      brokerURL: `${WS_URL}?token=${token}`,
      reconnectDelay: 5000,
      onConnect: () => {
        connectedRef.current = true;
        // flush any subscriptions requested before connection
        pendingRef.current.forEach(({ destination, callback }, id) => {
          const sub = client.subscribe(destination, callback);
          subscriptionsRef.current.set(id, sub);
        });
        pendingRef.current.clear();
      },
      onDisconnect: () => {
        connectedRef.current = false;
        // StompJS handles unsubscribing on disconnect, 
        // but we clear our refs to stay in sync with the new session
        subscriptionsRef.current.clear();
        pendingRef.current.clear();
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      connectedRef.current = false;
      const currentSubs = subscriptionsRef.current;
      const currentPending = pendingRef.current;
      currentSubs.clear();
      currentPending.clear();
      client.deactivate();
    };
  }, [token]);

  const subscribe = useCallback((destination, callback) => {
    const subId = ++subCounterRef.current;

    if (connectedRef.current && clientRef.current) {
      const sub = clientRef.current.subscribe(destination, callback);
      subscriptionsRef.current.set(subId, sub);
    } else {
      pendingRef.current.set(subId, { destination, callback });
    }

    return () => {
      // Remove from pending if it hasn't connected yet
      if (pendingRef.current.has(subId)) {
        pendingRef.current.delete(subId);
      }
      
      // Unsubscribe if it's already active
      if (subscriptionsRef.current.has(subId)) {
        const sub = subscriptionsRef.current.get(subId);
        // Only unsubscribe if the client is still connected/active
        // to avoid "No underlying STOMP connection" errors
        if (connectedRef.current && clientRef.current?.connected) {
          try {
            sub.unsubscribe();
          } catch (e) {
            console.warn('Failed to unsubscribe', e);
          }
        }
        subscriptionsRef.current.delete(subId);
      }
    };
  }, []);

  const publish = useCallback((destination, body) => {
    if (!clientRef.current || !connectedRef.current) {
      console.warn('STOMP: tried to publish before connection', destination);
      return;
    }
    clientRef.current.publish({
      destination,
      body: JSON.stringify(body),
    });
  }, []);

  return useMemo(() => ({ subscribe, publish }), [subscribe, publish]);
}
