import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { TOKEN_KEY } from '../api/client.js';

const WS_URL = import.meta.env.VITE_WS_URL || '/api/v1/ws';

/**
 * Creates and activates a STOMP client over SockJS. The JWT is forwarded as a
 * `Authorization: Bearer <token>` header on the CONNECT frame so the backend
 * channel interceptor can attach the user principal.
 */
export function createStompClient({ onConnect, onError } = {}) {
  const token = localStorage.getItem(TOKEN_KEY);

  const client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: (frame) => onConnect && onConnect(client, frame),
    onStompError: (frame) => onError && onError(frame),
  });

  client.activate();
  return client;
}
