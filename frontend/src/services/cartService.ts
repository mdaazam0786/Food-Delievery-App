/**
 * cartService.ts — Cart API service layer
 *
 * Provides type-safe methods for interacting with the backend cart service.
 * All methods handle authentication headers and response parsing.
 */

import { getAccessToken } from './authService';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

// ─── Types ────────────────────────────────────────────────────────────────────

export interface CartItemDTO {
  itemId: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface CartDTO {
  id: string;
  userEmail: string;
  restaurantId: string | null;
  items: CartItemDTO[];
  totalAmount: number;
  totalItems: number;
  createdAt: string;
  updatedAt: string;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

// ─── Helper ───────────────────────────────────────────────────────────────────

async function authFetch(url: string, init?: RequestInit): Promise<Response> {
  const token = getAccessToken();
  if (!token) throw new Error('No access token — user is not authenticated.');
  return fetch(url, {
    ...init,
    headers: {
      ...(init?.headers ?? {}),
      Authorization: `Bearer ${token}`,
    },
  });
}

// ─── Endpoints ────────────────────────────────────────────────────────────────

/**
 * GET /api/cart
 * Fetch the current user's cart. Creates an empty one if none exists.
 */
export async function getCart(): Promise<CartDTO> {
  const res = await authFetch(`${BASE_URL}/api/cart`);

  const json: ApiResponse<CartDTO> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as CartDTO,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to fetch cart with status ${res.status}`);
  }

  return json.data;
}

/**
 * POST /api/cart/items
 * Add an item to the cart with a specific quantity.
 */
export async function addItemToCart(payload: {
  restaurantId: string;
  itemId: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
}): Promise<CartDTO> {
  const res = await authFetch(`${BASE_URL}/api/cart/items`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  const json: ApiResponse<CartDTO> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as CartDTO,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to add item with status ${res.status}`);
  }

  return json.data;
}

/**
 * PATCH /api/cart/items/{itemId}
 * Update the quantity of a specific item.
 * Quantity 0 removes the item.
 */
export async function updateCartItemQuantity(
  itemId: string,
  quantity: number
): Promise<CartDTO> {
  const res = await authFetch(`${BASE_URL}/api/cart/items/${itemId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quantity }),
  });

  const json: ApiResponse<CartDTO> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as CartDTO,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(
      json.message ?? `Failed to update quantity with status ${res.status}`
    );
  }

  return json.data;
}

/**
 * DELETE /api/cart/items/{itemId}
 * Remove a specific item from the cart.
 */
export async function removeCartItem(itemId: string): Promise<CartDTO> {
  const res = await authFetch(`${BASE_URL}/api/cart/items/${itemId}`, {
    method: 'DELETE',
  });

  const json: ApiResponse<CartDTO> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as CartDTO,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to remove item with status ${res.status}`);
  }

  return json.data;
}

/**
 * DELETE /api/cart
 * Clear the entire cart (usually called after checkout).
 */
export async function clearCart(): Promise<void> {
  const res = await authFetch(`${BASE_URL}/api/cart`, {
    method: 'DELETE',
  });

  const json: ApiResponse<null> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as null,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to clear cart with status ${res.status}`);
  }
}
