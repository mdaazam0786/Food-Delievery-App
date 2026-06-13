/**
 * userService.ts — User profile API integration
 */

import { getAccessToken } from './authService';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

// ─── Types ────────────────────────────────────────────────────────────────────

export interface UserAddress {
  id: string;
  label: string;
  formattedAddress: string;
  latitude: number;
  longitude: number;
  isDefault?: boolean;
}

export interface UserProfile {
  id: number;
  email: string;
  fullName?: string;
  phoneNumber?: string;
  bio?: string;
  avatarUrl?: string;
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
 * GET /api/v1/address
 * Fetch all saved addresses for the current user
 */
export async function getUserAddresses(): Promise<UserAddress[]> {
  const res = await authFetch(`${BASE_URL}/api/v1/address`);

  const json: ApiResponse<UserAddress[]> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as UserAddress[],
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to fetch addresses with status ${res.status}`);
  }

  return json.data || [];
}

/**
 * GET /api/v1/users
 * Fetch current user's profile
 */
export async function fetchCurrentUser(): Promise<UserProfile> {
  const res = await authFetch(`${BASE_URL}/api/v1/users`);

  const json: ApiResponse<UserProfile> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as UserProfile,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to fetch profile with status ${res.status}`);
  }

  return json.data;
}

/**
 * POST /api/v1/address
 * Save a new address for the current user
 */
export async function saveUserAddress(payload: {
  formattedAddress: string;
  street: string;
  apartment: string;
  postCode?: string;
  label: string;
  latitude: number;
  longitude: number;
}): Promise<UserAddress> {
  const res = await authFetch(`${BASE_URL}/api/v1/address`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const json: ApiResponse<UserAddress> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as UserAddress,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to save address with status ${res.status}`);
  }

  return json.data;
}

/**
 * PUT /api/v1/users
 * Update the current user's profile
 */
export async function updateUserProfile(payload: {
  fullName?: string;
  email?: string;
  phoneNumber?: string;
}): Promise<UserProfile> {
  const res = await authFetch(`${BASE_URL}/api/v1/users`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const json: ApiResponse<UserProfile> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as UserProfile,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Failed to update profile with status ${res.status}`);
  }

  return json.data;
}

