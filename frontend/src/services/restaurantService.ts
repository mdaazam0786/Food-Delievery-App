/**
 * restaurantService.ts
 *
 * Communicates with the Restaurant Service via the API Gateway.
 *
 * Endpoints used:
 *   GET  /api/geocode                             — reverse geocoding (public)
 *   GET  /api/admin/restaurants/mine              — owner's restaurant profile
 *   POST /api/admin/restaurants                   — provision / complete onboarding
 *   POST /api/admin/restaurants/{id}/image        — banner upload
 *   POST /api/admin/restaurants/{restaurantId}/menu        — add menu item
 *   PATCH /api/admin/restaurants/{restaurantId}/menu/{itemId} — update menu item
 */

import { getAccessToken } from './authService';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ApiResponse<T> {
  success:   boolean;
  message?:  string;
  data:      T;
  timestamp: string;
}

/** Structured address breakdown from Nominatim */
export interface GeoAddressDetail {
  suburb?:      string;
  cityDistrict?: string;
  city?:        string;
  state?:       string;
  postcode?:    string;
  country?:     string;
  countryCode?: string;
}

/**
 * Full geocoding response from GET /api/geocode.
 * displayName is the pre-formatted address string shown in the address text field.
 * latitude/longitude are echoed back so the frontend can keep them in sync.
 */
export interface GeocodeResponse {
  placeId?:     number;
  osmType?:     string;
  osmId?:       number;
  latitude:     number;
  longitude:    number;
  category?:    string;
  type?:        string;
  displayName:  string;
  address?:     GeoAddressDetail;
  boundingBox?: string[];
}

export interface CreateRestaurantPayload {
  name:        string;
  description: string;
  addressText: string;
  imageUrl:    string;
  latitude:    number;
  longitude:   number;
  gstNo:       string;
  fssaiNo:     string;
}

export interface RestaurantResponse {
  id:          string;
  ownerEmail:  string;
  name:        string;
  description: string;
  addressText: string;
  latitude:    number;
  longitude:   number;
  imageUrl:    string;
  imagePublicId?: string;
  status:      string;
  gstNo?:      string;
  fssaiNo?:    string;
  rating?:     number;
  totalRatings?: number;
  discount?:   string;
  createdAt:   string;
  updatedAt:   string;
}

export interface UpdateRestaurantStatusPayload {
  status: 'OPEN' | 'CLOSED';
}

export interface MediaUploadResult {
  secureUrl: string;
  publicId:  string;
}

export interface AddMenuItemPayload {
  name: string;
  description: string;
  price: number;
  category: string;
  imageUrl?: string;
  isVeg: boolean;
}

export interface MenuItemResponse {
  id: string;
  restaurantId: string;
  name: string;
  description: string;
  price: number;
  category: string;
  imageUrl?: string;
  imagePublicId?: string;
  available: boolean;
  isVeg: boolean;
  rating: number;
  totalRatings: number;
  updatedAt?: string;
}

// ─── API ──────────────────────────────────────────────────────────────────────

/**
 * GET /api/geocode?lat={lat}&lng={lng}
 *
 * Reverse-geocodes coordinates via our backend (which proxies to Nominatim).
 * No auth token required — this is a public endpoint.
 * Use the returned `displayName` as the address text field value.
 */
export async function reverseGeocodeBackend(lat: number, lng: number): Promise<GeocodeResponse> {
  const res = await fetch(`${BASE_URL}/api/geocode?lat=${lat}&lng=${lng}`);
  if (!res.ok) throw new Error(`Geocode request failed with status ${res.status}`);
  return res.json() as Promise<GeocodeResponse>;
}

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

/**
 * GET /api/admin/restaurants/mine
 * Returns null when the owner has no restaurant yet (404).
 */
export async function fetchMyRestaurant(): Promise<RestaurantResponse | null> {
  const res = await authFetch(`${BASE_URL}/api/admin/restaurants/mine`);

  if (res.status === 404) return null;

  const json: ApiResponse<RestaurantResponse> = await res.json().catch(() => ({
    success:   false,
    message:   `HTTP ${res.status}`,
    data:      null as unknown as RestaurantResponse,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    if (res.status === 404) return null;
    throw new Error(json.message ?? `Request failed with status ${res.status}`);
  }

  return json.data;
}

/**
 * POST /api/admin/restaurants
 * Provisions a new restaurant or completes an auto-provisioned stub.
 */
export async function createRestaurant(
  payload: CreateRestaurantPayload,
): Promise<RestaurantResponse> {
  const res = await authFetch(`${BASE_URL}/api/admin/restaurants`, {
    method:  'POST',
    headers: { 'Content-Type': 'application/json' },
    body:    JSON.stringify(payload),
  });

  const json: ApiResponse<RestaurantResponse> = await res.json().catch(() => ({
    success:   false,
    message:   `HTTP ${res.status}`,
    data:      null as unknown as RestaurantResponse,
    timestamp: new Date().toISOString(),
  }));

  if (!(res.ok || res.status === 201) || !json.success) {
    throw new Error(json.message ?? `Request failed with status ${res.status}`);
  }

  return json.data;
}

/**
 * POST /api/admin/restaurants/upload-image
 * Uploads a restaurant image immediately and returns the URL.
 * Used by the provisioning form for two-step image upload.
 */
export async function uploadRestaurantImageImmediate(file: File): Promise<string> {
  const form = new FormData();
  form.append('file', file);

  const token = getAccessToken();
  if (!token) throw new Error('No access token — user is not authenticated.');

  const res = await fetch(`${BASE_URL}/api/admin/restaurants/upload-image`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: form,
  });

  if (!res.ok) {
    const errorText = await res.text().catch(() => `HTTP ${res.status}`);
    throw new Error(`Image upload failed: ${errorText}`);
  }

  const json = await res.json() as { imageUrl?: string; error?: string };
  if (!json.imageUrl) {
    throw new Error(json.error ?? 'No URL returned from image upload');
  }

  return json.imageUrl;
}

/**
 * POST /api/admin/restaurants/{restaurantId}/image
 * Legacy endpoint for uploading images after restaurant creation.
 * Kept for backwards compatibility.
 */
export async function uploadRestaurantImage(
  restaurantId: string,
  file: File,
): Promise<MediaUploadResult> {
  const form = new FormData();
  form.append('image', file);

  const res = await authFetch(`${BASE_URL}/api/admin/restaurants/${restaurantId}/image`, {
    method: 'POST',
    body:   form,
  });

  const json: ApiResponse<MediaUploadResult> = await res.json().catch(() => ({
    success:   false,
    message:   `HTTP ${res.status}`,
    data:      null as unknown as MediaUploadResult,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Image upload failed with status ${res.status}`);
  }

  return json.data;
}

/**
 * GET /api/restaurants/{restaurantId}
 * Gets restaurant details (public endpoint, no auth required).
 */
export async function getRestaurantDetails(restaurantId: string): Promise<RestaurantResponse> {
  const res = await fetch(`${BASE_URL}/api/restaurants/${restaurantId}`);

  const json: ApiResponse<RestaurantResponse> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as RestaurantResponse,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to fetch restaurant with status ${res.status}`);
  }

  return json.data;
}

/**
 * GET /api/restaurants/{restaurantId}/menu
 * Gets menu items for a restaurant (public endpoint - no auth required).
 * 
 * Returns available menu items only. Used by customers browsing restaurants.
 */
export async function getMenuItems(restaurantId: string): Promise<MenuItemResponse[]> {
  const res = await fetch(`${BASE_URL}/api/restaurants/${restaurantId}/menu`);

  const json: ApiResponse<MenuItemResponse[]> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as MenuItemResponse[],
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to fetch menu items with status ${res.status}`);
  }

  return json.data || [];
}

/**
 * GET /api/restaurants/{restaurantId}/menu
 * Gets all available menu items for a restaurant (public endpoint, no auth required).
 * Used by customers viewing the restaurant menu.
 */
export async function getPublicMenuItems(restaurantId: string): Promise<MenuItemResponse[]> {
  const res = await fetch(`${BASE_URL}/api/restaurants/${restaurantId}/menu`);

  const json: ApiResponse<MenuItemResponse[]> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as MenuItemResponse[],
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to fetch menu items with status ${res.status}`);
  }

  return json.data || [];
}

/**
 * POST /api/admin/restaurants/{restaurantId}/menu
 * Adds a new menu item to the restaurant.
 */
export async function addMenuItem(
  restaurantId: string,
  payload: AddMenuItemPayload,
): Promise<MenuItemResponse> {
  const token = getAccessToken();
  if (!token) throw new Error('No access token — user is not authenticated.');

  console.log('addMenuItem payload:', payload);

  const res = await fetch(`${BASE_URL}/api/admin/restaurants/${restaurantId}/menu`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const json: ApiResponse<MenuItemResponse> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as MenuItemResponse,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to add menu item with status ${res.status}`);
  }

  return json.data;
}

/**
 * PATCH /api/admin/restaurants/{restaurantId}/menu/{itemId}
 * Updates an existing menu item.
 */
export async function updateMenuItem(
  restaurantId: string,
  itemId: string,
  payload: Partial<AddMenuItemPayload>,
): Promise<MenuItemResponse> {
  const token = getAccessToken();
  if (!token) throw new Error('No access token — user is not authenticated.');

  const res = await fetch(`${BASE_URL}/api/admin/restaurants/${restaurantId}/menu/${itemId}`, {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const json: ApiResponse<MenuItemResponse> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as MenuItemResponse,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to update menu item with status ${res.status}`);
  }

  return json.data;
}

/**
 * POST /api/admin/menu-items/{itemId}/upload-image
 * Uploads a menu item image immediately and returns the URL.
 * Used by the menu item form for immediate image upload on file selection.
 */
export async function uploadMenuItemImageImmediate(file: File): Promise<string> {
  // For form phase, we upload without a specific itemId (provisional upload)
  // If an itemId is available, the caller should provide it
  const form = new FormData();
  form.append('file', file);

  const token = getAccessToken();
  if (!token) throw new Error('No access token — user is not authenticated.');

  // Use the same provisioning endpoint for now - returns just the imageUrl
  const res = await fetch(`${BASE_URL}/api/admin/restaurants/upload-image`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: form,
  });

  if (!res.ok) {
    const errorText = await res.text().catch(() => `HTTP ${res.status}`);
    throw new Error(`Image upload failed: ${errorText}`);
  }

  const json = await res.json() as { imageUrl?: string; error?: string };
  if (!json.imageUrl) {
    throw new Error(json.error ?? 'No URL returned from image upload');
  }

  return json.imageUrl;
}

/**
 * POST /api/admin/restaurants/{restaurantId}/menu-items/{itemId}/upload-image
 * Uploads a menu item image with persistence to MongoDB.
 * Used when updating an existing menu item with an image.
 */
export async function uploadMenuItemImageWithPersist(
  itemId: string,
  file: File,
): Promise<string> {
  const form = new FormData();
  form.append('file', file);

  const token = getAccessToken();
  if (!token) throw new Error('No access token — user is not authenticated.');

  const res = await fetch(`${BASE_URL}/api/admin/restaurants/menu-items/${itemId}/upload-image`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: form,
  });

  if (!res.ok) {
    const errorText = await res.text().catch(() => `HTTP ${res.status}`);
    throw new Error(`Image upload failed: ${errorText}`);
  }

  const json = await res.json() as { imageUrl?: string; error?: string };
  if (!json.imageUrl) {
    throw new Error(json.error ?? 'No URL returned from image upload');
  }

  return json.imageUrl;
}

/**
 * PUT /api/admin/restaurants/{restaurantId}/status
 * Updates the restaurant's operational status (OPEN or CLOSED).
 */
export async function updateRestaurantStatus(
  restaurantId: string,
  status: 'OPEN' | 'CLOSED',
): Promise<RestaurantResponse> {
  const token = getAccessToken();
  if (!token) throw new Error('No access token — user is not authenticated.');

  const res = await fetch(`${BASE_URL}/api/admin/restaurants/${restaurantId}/status`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ status }),
  });

  const json: ApiResponse<RestaurantResponse> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as RestaurantResponse,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to update restaurant status with status ${res.status}`);
  }

  return json.data;
}
