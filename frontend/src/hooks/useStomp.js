import { useRef, useEffect, useCallback } from 'react';
import { Client } from '@stomp/stompjs';

const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws/chat';

export function useStomp(token) {
  const clientRef = useRef(null);
  const subscriptionsRef = useRef(new Map()); // destination → StompSubscription
  const connectedRef = useRef(false);
  const pendingRef = useRef([]); // callbacks queued before connection

  useEffect(() => {
    if (!token) return;

    const client = new Client({
      brokerURL: `${WS_URL}?token=${token}`,
      reconnectDelay: 5000,
      onConnect: () => {
        connectedRef.current = true;
        // flush any subscriptions requested before connection
        pendingRef.current.forEach(({ destination, callback }) => {
          const sub = client.subscribe(destination, callback);
          subscriptionsRef.current.set(destination, sub);
        });
        pendingRef.current = [];
      },
      onDisconnect: () => {
        connectedRef.current = false;
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      connectedRef.current = false;
      subscriptionsRef.current.clear();
      client.deactivate();
    };
  }, [token]);

  const subscribe = useCallback((destination, callback) => {
    // Unsubscribe if already subscribed to this destination
    if (subscriptionsRef.current.has(destination)) {
      subscriptionsRef.current.get(destination).unsubscribe();
      subscriptionsRef.current.delete(destination);
    }

    if (connectedRef.current && clientRef.current) {
      const sub = clientRef.current.subscribe(destination, callback);
      subscriptionsRef.current.set(destination, sub);
    } else {
      // Queue for when connection is ready
      pendingRef.current.push({ destination, callback });
    }

    return () => {
      if (subscriptionsRef.current.has(destination)) {
        subscriptionsRef.current.get(destination).unsubscribe();
        subscriptionsRef.current.delete(destination);
      }
    };
  }, []);

  const unsubscribe = useCallback((destination) => {
    if (subscriptionsRef.current.has(destination)) {
      subscriptionsRef.current.get(destination).unsubscribe();
      subscriptionsRef.current.delete(destination);
    }
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

  return { subscribe, unsubscribe, publish };
}
