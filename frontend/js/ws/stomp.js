// js/ws/stomp.js
// Gerencia a conexão STOMP única e todas as subscrições.

import { getTokens } from '../state/store.js';

let client = null;

// destino (string) → objeto de subscrição STOMP
const subscriptions = new Map();

// Callback a ser chamado após (re)conexão — usado para restaurar subscrições
let _onConnectedCallback = null;

export function connect(onConnected, onError) {
  _onConnectedCallback = onConnected;

  const { accessToken } = getTokens();

  // Usando SockJS + STOMP (versão legada compatível com Spring WebSocket)
  client = new StompJs.Client({
    brokerURL: `ws://localhost:8080/ws/chat?token=${accessToken}`,
    reconnectDelay: 5000,
    onConnect: onConnected,
    onStompError: onError,
  });
  client.activate();

  // Silencia os logs do STOMP no console em produção
  client.debug = null;

  client.connect(
    {},
    () => {
      if (typeof _onConnectedCallback === 'function') {
        _onConnectedCallback();
      }
    },
    (err) => {
      console.error('[STOMP] Erro de conexão:', err);
      if (typeof onError === 'function') onError(err);
    }
  );
}

export function disconnect() {
  subscriptions.forEach((sub) => {
    try { sub.unsubscribe(); } catch { /* ignora */ }
  });
  subscriptions.clear();

  if (client?.connected) {
    client.disconnect();
  }
  client = null;
}

export function isConnected() {
  return client?.connected === true;
}

/**
 * Subscreve a um destino STOMP.
 * Idempotente: ignora se já há subscrição ativa para o destino.
 */
export function subscribe(destination, callback) {
  if (subscriptions.has(destination)) return;

  const sub = client.subscribe(destination, (frame) => {
    try {
      const body = JSON.parse(frame.body);
      callback(body);
    } catch (e) {
      console.warn('[STOMP] Falha ao parsear frame:', frame.body, e);
    }
  });

  subscriptions.set(destination, sub);
}

export function unsubscribe(destination) {
  const sub = subscriptions.get(destination);
  if (sub) {
    try { sub.unsubscribe(); } catch { /* ignora */ }
    subscriptions.delete(destination);
  }
}

export function publish(destination, body) {
  if (!client?.connected) {
    console.warn('[STOMP] Tentativa de publicar sem conexão ativa');
    return;
  }
  client.send(destination, {}, JSON.stringify(body));
}
