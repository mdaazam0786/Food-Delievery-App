/**
 * orderService.ts
 *
 * Communicates with the Order Service via the API Gateway.
 *
 * Endpoints used:
 *   PUT  /api/orders/{orderId}/status          — update order status
 *   GET  /api/orders/restaurant/{restaurantId} — paginated order list
 */

import { getAccessToken } from './authService';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

// ─── Shared helpers ───────────────────────────────────────────────────────────

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

async function handleResponse<T>(res: Response): Promise<T> {
  const json: ApiResponse<T> = await res.json().catch(() => ({
    success:   false,
    message:   `HTTP ${res.status}`,
    data:      null as unknown as T,
    timestamp: new Date().toISOString(),
  }));
  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Request failed with status ${res.status}`);
  }
  return json.data;
}

// ─── Types ────────────────────────────────────────────────────────────────────

/** Backend status enum values sent in the request body */
export type OrderStatusPayload =
  | 'ACCEPTED'
  | 'READY_FOR_PICKUP'
  | 'OUT_FOR_DELIVERY';

/** Maps UI ActiveStatus keys → backend enum strings */
export const STATUS_MAP: Record<string, OrderStatusPayload> = {
  preparing: 'ACCEPTED',
  ready:     'READY_FOR_PICKUP',
  delivery:  'OUT_FOR_DELIVERY',
};

export interface OrderItem {
  menuItemId:     string;
  itemName:       string;
  quantity:       number;
  price:          number;
}

export interface OrderItemRequest {
  menuItemId: string;
  itemName: string;
  quantity: number;
  price: number;
}

export interface CreateOrderRequest {
  restaurantId: string;
  deliveryAddressId: string;
  items: OrderItemRequest[];
  notes?: string;
}

export interface CreateOrderResponse {
  id: string;
  orderId: string;
  restaurantId: string;
  userEmail: string;
  status: string;
  totalAmount: number;
  items: OrderItem[];
  createdAt: string;
}

/** A single order row returned by GET /api/orders/restaurant/{restaurantId} */
export interface RestaurantOrder {
  id:            string;
  userEmail:     string;
  restaurantId:  string;
  status:        string;
  totalAmount:   number;
  currency?:     string;
  items:         OrderItem[];
  createdAt:     string;
}

/** @deprecated Use RestaurantOrder — kept for dashboard compatibility */
export type OrderSummary = RestaurantOrder;

/** Spring-style Page wrapper */
export interface Page<T> {
  content:       T[];
  totalPages:    number;
  totalElements: number;
  number:        number;  // current page (0-indexed)
  size:          number;
}

// ─── API calls ────────────────────────────────────────────────────────────────

/**
 * PUT /api/orders/{orderId}/status
 *
 * Updates the status of a single order.
 * orderId should be the raw ID string (without the '#ORD-' prefix).
 */
export async function updateOrderStatus(
  orderId: string,
  status:  OrderStatusPayload,
): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/orders/${orderId}/status`, {
    method:  'PATCH',
    headers: authHeaders(),
    body:    JSON.stringify({ status }),
  });
  await handleResponse<unknown>(res);
}

/**
 * GET /api/orders/restaurant/{restaurantId}?page=0&size=5
 *
 * Returns a paginated list of orders for the given restaurant.
 */
export async function fetchRestaurantOrders(
  restaurantId: number | string,
  page = 0,
  size = 5,
): Promise<Page<RestaurantOrder>> {
  const url = `${BASE_URL}/api/orders/restaurant/${restaurantId}?page=${page}&size=${size}`;
  const res = await fetch(url, {
    method:  'GET',
    headers: authHeaders(),
  });
  return handleResponse<Page<RestaurantOrder>>(res);
}

/**
 * POST /api/orders
 *
 * Creates a new order with the given items and delivery address.
 * This is Step 1 of the payment pipeline.
 *
 * @param orderRequest Order creation payload with restaurant ID, delivery address, and items
 * @returns The created order with its ID
 */
export async function createOrder(orderRequest: CreateOrderRequest): Promise<CreateOrderResponse> {
  const res = await fetch(`${BASE_URL}/api/orders`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(orderRequest),
  });
  return handleResponse<CreateOrderResponse>(res);
}

/**
 * GET /api/orders/user/history?page=0&size=10
 *
 * Returns a paginated list of orders for the current user (completed/cancelled).
 * Used for displaying order history in user profile.
 *
 * @deprecated Use fetchUserOrders instead
 */
export async function fetchUserOrderHistory(
  page = 0,
  size = 10,
): Promise<Page<RestaurantOrder>> {
  const url = `${BASE_URL}/api/orders/user/history?page=${page}&size=${size}`;
  const res = await fetch(url, {
    method: 'GET',
    headers: authHeaders(),
  });
  return handleResponse<Page<RestaurantOrder>>(res);
}

/**
 * GET /api/orders/me?page=0&size=10
 *
 * Returns a paginated list of all orders for the current authenticated user.
 * Used for displaying user's order history in profile section.
 *
 * @param page Page number (0-indexed)
 * @param size Number of items per page
 * @returns Paginated list of user's orders with restaurant information
 */
export async function fetchUserOrders(
  page = 0,
  size = 10,
): Promise<Page<RestaurantOrder>> {
  const url = `${BASE_URL}/api/orders/me?page=${page}&size=${size}`;
  const res = await fetch(url, {
    method: 'GET',
    headers: authHeaders(),
  });
  return handleResponse<Page<RestaurantOrder>>(res);
}
