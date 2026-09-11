import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import type { Execution } from '../types';

/**
 * Subscribes to /topic/executions over STOMP-over-WebSocket and keeps a
 * rolling feed of the most recent live execution events. Falls back
 * gracefully (silently retries) if the backend isn't reachable yet.
 */
export function useExecutionStream(maxItems = 30) {
  const [events, setEvents] = useState<Execution[]>([]);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const client = new Client({
      brokerURL: `${import.meta.env.VITE_WS_URL}/ws`,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/executions', (message) => {
          try {
            const execution: Execution = JSON.parse(message.body);
            setEvents((prev) => [execution, ...prev].slice(0, maxItems));
          } catch {
            // ignore malformed frames
          }
        });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false)
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [maxItems]);

  return { events, connected };
}
