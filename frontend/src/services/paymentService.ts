/**
 * paymentService.ts
 *
 * Handles payment processing workflows including payment initiation via gateway.
 *
 * Endpoints:
 *   POST /api/payments/initiate — Initialize payment session
 */

import { getAccessToken } from './authService';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

// ─── Types ────────────────────────────────────────────────────────────────────

export interface InitiatePaymentResponse {
  orderId: string;
  razorpayOrderId: string;
  merchantKey: string;
  amount: number;
  currency: string;
  userEmail: string;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

// ─── Helper ───────────────────────────────────────────────────────────────────

function authHeaders(): Record<string, string> {
  const token = getAccessToken();
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function handleResponse<T>(res: Response): Promise<T> {
  const json: ApiResponse<T> = await res.json().catch(() => ({
    success: false,
    message: `HTTP ${res.status}`,
    data: null as unknown as T,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Request failed with status ${res.status}`);
  }

  return json.data;
}

// ─── API calls ────────────────────────────────────────────────────────────────

/**
 * POST /api/payments/initiate
 *
 * Initiates a payment session for an order. Returns payment gateway details
 * (Razorpay Order ID, merchant key, etc.) to launch the payment modal.
 *
 * @param orderId The newly created order ID from Step 1
 * @returns Payment gateway initialization response with Razorpay details
 */
export async function initiatePayment(orderId: string): Promise<InitiatePaymentResponse> {
  const res = await fetch(`${BASE_URL}/api/payments/initiate`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ orderId }),
  });

  return handleResponse<InitiatePaymentResponse>(res);
}
