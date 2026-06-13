/**
 * restaurantAcceptanceService.ts
 *
 * Communicates with the Restaurant Acceptance Service via the API Gateway.
 *
 * Endpoints:
 *   POST /api/restaurants/orders/{orderId}/accept
 *   POST /api/restaurants/orders/{orderId}/decline
 *   GET  /api/restaurants/{restaurantId}/orders/stream  (SSE)
 */

import { getAccessToken } from './authService';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

interface ApiResponse<T> {
  success:   boolean;
  message?:  string;
  data:      T;
  timestamp: string;
}

function authHeaders(): Record<string, string> {
  const token = getAccessToken();
  return {
    'Content-Type':  'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function handleResponse(res: Response): Promise<void> {
  const json: ApiResponse<unknown> = await res.json().catch(() => ({
    success:   false,
    message:   `HTTP ${res.status}`,
    data:      null,
    timestamp: new Date().toISOString(),
  }));
  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Request failed with status ${res.status}`);
  }
}

/** Payload pushed over SSE when a new paid order arrives */
export interface PendingOrderPayload {
  orderId:      string;
  restaurantId: string;
  userEmail:    string;
  receivedAt:   string;
  status?:      string;
}

const DEFAULT_PREP_MINUTES = 25;
const DEFAULT_DECLINE_REASON = 'ITEM_UNAVAILABLE';

/**
 * POST /api/restaurants/orders/{orderId}/accept
 */
export async function acceptRestaurantOrder(
  orderId: string,
  estimatedPrepTimeMinutes = DEFAULT_PREP_MINUTES,
): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/restaurants/orders/${orderId}/accept`, {
    method:  'POST',
    headers: authHeaders(),
    body:    JSON.stringify({ estimatedPrepTimeMinutes }),
  });
  await handleResponse(res);
}

/**
 * POST /api/restaurants/orders/{orderId}/decline
 */
export async function declineRestaurantOrder(
  orderId: string,
  reason = DEFAULT_DECLINE_REASON,
): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/restaurants/orders/${orderId}/decline`, {
    method:  'POST',
    headers: authHeaders(),
    body:    JSON.stringify({ reason }),
  });
  await handleResponse(res);
}

/**
 * Opens an SSE connection for live incoming-order notifications.
 * Uses ?token= because EventSource cannot send Authorization headers.
 */
export function openRestaurantOrderStream(
  restaurantId: string,
  handlers: {
    onNewOrder?:    (order: PendingOrderPayload) => void;
    onOrderExpired?: (orderId: string) => void;
    onConnected?:   () => void;
    onError?:       () => void;
  },
): EventSource | null {
  const token = getAccessToken();
  if (!token) return null;

  const url =
    `${BASE_URL}/api/restaurants/${encodeURIComponent(restaurantId)}` +
    `/orders/stream?token=${encodeURIComponent(token)}`;

  const es = new EventSource(url);

  es.addEventListener('connected', () => handlers.onConnected?.());

  es.addEventListener('new_order', (e: MessageEvent) => {
    try {
      const payload = JSON.parse(e.data as string) as PendingOrderPayload;
      handlers.onNewOrder?.(payload);
    } catch {
      /* ignore malformed payloads */
    }
  });

  es.addEventListener('order_expired', (e: MessageEvent) => {
    try {
      const payload = JSON.parse(e.data as string) as { orderId?: string };
      if (payload.orderId) handlers.onOrderExpired?.(payload.orderId);
    } catch {
      /* ignore malformed payloads */
    }
  });

  es.onerror = () => handlers.onError?.();

  return es;
}
