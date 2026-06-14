/**
 * authService.ts
 *
 * Wired to the real Foodzie backend via the API Gateway at localhost:8080.
 *
 * All auth endpoints are public (no JWT needed):
 *   POST /api/v1/auth/register
 *   POST /api/v1/auth/login
 *   POST /api/v1/auth/mfa/verify
 *   POST /api/v1/auth/refresh
 *   POST /api/v1/auth/password-reset
 *   POST /api/v1/auth/password-reset/confirm
 *
 * Protected endpoints (require Bearer token):
 *   POST /api/v1/auth/logout
 *   POST /api/v1/auth/logout-all
 */

// ─── Config ───────────────────────────────────────────────────────────────────

/**
 * In development, Vite proxies /api → http://localhost:8080 (see vite.config.ts).
 * In production, set VITE_API_BASE_URL to your deployed gateway URL.
 */
const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

const KEYS = {
  ACCESS_TOKEN:  'foodzie_access_token',
  REFRESH_TOKEN: 'foodzie_refresh_token',
  USER:          'foodzie_user',
} as const;

// ─── Types ────────────────────────────────────────────────────────────────────

/** Wrapper every backend response is nested inside */
interface ApiResponse<T> {
  success:   boolean;
  message?:  string;
  data:      T;
  timestamp: string;
}

/** Full auth payload returned on login / register / refresh */
export interface AuthData {
  accessToken:       string;
  refreshToken:      string;
  tokenType:         string;
  expiresIn:         number;   // seconds (900 = 15 min)
  userId:            number;
  email:             string;
  username:          string;
  roles:             string[];
  mfaRequired:       boolean;
  mfaChallengeToken?: string;  // only present when mfaRequired === true
}

/** Minimal user info cached in localStorage */
export interface StoredUser {
  userId:   number;
  email:    string;
  username: string;
  roles:    string[];
}

// ─── Token storage helpers ────────────────────────────────────────────────────

export function getAccessToken(): string | null {
  return localStorage.getItem(KEYS.ACCESS_TOKEN);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(KEYS.REFRESH_TOKEN);
}

/** Alias used by SplashScreen to decide whether the user is logged in */
export function getToken(): string | null {
  return getAccessToken();
}

export function getStoredUser(): StoredUser | null {
  const raw = localStorage.getItem(KEYS.USER);
  if (!raw) return null;
  try { return JSON.parse(raw) as StoredUser; } catch { return null; }
}

function persistSession(data: AuthData): void {
  localStorage.setItem(KEYS.ACCESS_TOKEN,  data.accessToken);
  localStorage.setItem(KEYS.REFRESH_TOKEN, data.refreshToken);
  localStorage.setItem(KEYS.USER, JSON.stringify({
    userId:   data.userId,
    email:    data.email,
    username: data.username,
    roles:    data.roles,
  } satisfies StoredUser));
}

export function clearSession(): void {
  localStorage.removeItem(KEYS.ACCESS_TOKEN);
  localStorage.removeItem(KEYS.REFRESH_TOKEN);
  localStorage.removeItem(KEYS.USER);
}

/**
 * Returns the route to navigate to immediately after a successful login/register.
 * Customers pass through the location screen first; admin/driver go straight to their panel.
 */
export function getPostLoginRoute(): string {
  const user = getStoredUser();
  if (user?.roles.includes('ROLE_RESTAURANT')) return '/admin';
  if (user?.roles.includes('ROLE_DRIVER'))     return '/driver';
  return '/location';
}

/**
 * Returns the correct post-login destination route based on the user's roles.
 *
 * Role priority (highest first):
 *   ROLE_RESTAURANT → /admin   (restaurant owner / admin panel)
 *   ROLE_DRIVER     → /driver  (driver dashboard — future)
 *   ROLE_USER       → /app     (customer home feed)
 *
 * Falls back to /app if no stored user is found.
 */
export function getDestinationByRole(): string {
  const user = getStoredUser();
  if (!user) return '/app';
  if (user.roles.includes('ROLE_RESTAURANT')) return '/admin';
  if (user.roles.includes('ROLE_DRIVER'))     return '/driver';
  return '/app';
}

/** @deprecated use clearSession() */
export const clearToken = clearSession;

// ─── HTTP helpers ─────────────────────────────────────────────────────────────

async function post<T>(
  path: string,
  body: unknown,
  authenticated = false,
): Promise<ApiResponse<T>> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (authenticated) {
    const token = getAccessToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method:  'POST',
    headers,
    body:    JSON.stringify(body),
  });

  // Parse body regardless of status so we can surface the server's message
  const json: ApiResponse<T> = await res.json().catch(() => ({
    success:   false,
    message:   `HTTP ${res.status}`,
    data:      null as unknown as T,
    timestamp: new Date().toISOString(),
  }));

  if (!res.ok || !json.success) {
    throw new Error(json.message ?? `Request failed with status ${res.status}`);
  }

  return json;
}

// ─── Auth API ─────────────────────────────────────────────────────────────────

/**
 * POST /api/v1/auth/register
 *
 * The backend requires a unique `username` field (separate from `fullName`).
 * We derive a username from the full name: lowercase, spaces → underscores,
 * then append a short random suffix to reduce collision probability.
 */
export async function register(
  fullName: string,
  email:    string,
  password: string,
): Promise<AuthData> {
  // Build a username from the full name
  const base     = fullName.trim().toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_.]/g, '');
  const suffix   = Math.floor(Math.random() * 9000 + 1000); // 4-digit suffix
  const username = `${base || 'user'}_${suffix}`;

  const { data } = await post<AuthData>('/api/v1/auth/register', {
    email,
    username,
    password,
    fullName,
  });

  // MFA is not expected on fresh registration, but guard anyway
  if (!data.mfaRequired) {
    persistSession(data);
  }

  return data;
}

/**
 * POST /api/v1/auth/login
 *
 * The backend accepts either email or username in the `emailOrUsername` field.
 * Returns either full tokens or an MFA challenge.
 */
export async function login(
  emailOrUsername: string,
  password:        string,
): Promise<AuthData> {
  const { data } = await post<AuthData>('/api/v1/auth/login', {
    emailOrUsername,
    password,
  });

  if (!data.mfaRequired) {
    persistSession(data);
  }

  return data;
}

/**
 * POST /api/v1/auth/mfa/verify
 *
 * Complete the MFA step after login returned mfaRequired=true.
 */
export async function verifyMfa(
  challengeToken: string,
  otpCode:        string,
): Promise<AuthData> {
  const { data } = await post<AuthData>('/api/v1/auth/mfa/verify', {
    challengeToken,
    otpCode,
  });

  persistSession(data);
  return data;
}

/**
 * POST /api/v1/auth/refresh
 *
 * Silently rotate the access token using the stored refresh token.
 * Called automatically by the token interceptor when a 401 is received.
 */
export async function refreshAccessToken(): Promise<AuthData> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error('No refresh token available');

  const { data } = await post<AuthData>('/api/v1/auth/refresh', { refreshToken });
  persistSession(data);
  return data;
}

/**
 * POST /api/v1/auth/password-reset
 *
 * Initiate a password reset. Always returns success (prevents email enumeration).
 */
export async function forgotPassword(email: string): Promise<void> {
  await post<null>('/api/v1/auth/password-reset', { email });
}

/**
 * POST /api/v1/auth/password-reset/confirm
 *
 * Complete the password reset with the token from the reset email.
 */
export async function confirmPasswordReset(
  token:       string,
  newPassword: string,
): Promise<void> {
  await post<null>('/api/v1/auth/password-reset/confirm', { token, newPassword });
}

/**
 * POST /api/v1/auth/logout  (authenticated)
 *
 * Revokes the current refresh token on the server, then clears local storage.
 */
export async function logout(): Promise<void> {
  const refreshToken = getRefreshToken();
  if (refreshToken) {
    // Best-effort — don't block the UI if the server call fails
    await post<null>('/api/v1/auth/logout', { refreshToken }, true).catch(() => {});
  }
  clearSession();
}

/**
 * POST /api/v1/auth/logout-all  (authenticated)
 *
 * Revokes ALL sessions for this user, then clears local storage.
 */
export async function logoutAll(): Promise<void> {
  await post<null>('/api/v1/auth/logout-all', {}, true).catch(() => {});
  clearSession();
}

// ─── OAuth2 social login ──────────────────────────────────────────────────────

export type OAuthProvider = 'google' | 'github';

/** JWT payload claims issued by auth-service after OAuth login */
interface OAuthJwtClaims {
  sub:     string;
  userId?: number;
  email?:  string;
  roles?:  string[];
}

function decodeJwtPayload(token: string): OAuthJwtClaims {
  const parts = token.split('.');
  if (parts.length !== 3) throw new Error('Invalid access token');
  const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  const json   = atob(base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '='));
  return JSON.parse(json) as OAuthJwtClaims;
}

/**
 * Stores tokens returned by the OAuth redirect handler.
 * User profile fields are extracted from the JWT claims.
 */
export function persistOAuthSession(accessToken: string, refreshToken: string): void {
  const claims = decodeJwtPayload(accessToken);
  const roles  = Array.isArray(claims.roles) ? claims.roles : [];

  persistSession({
    accessToken,
    refreshToken,
    tokenType:   'Bearer',
    expiresIn:   900,
    userId:      Number(claims.userId ?? 0),
    email:       claims.email ?? '',
    username:    claims.sub ?? '',
    roles,
    mfaRequired: false,
  });
}

/**
 * Redirects the browser to the Spring Security OAuth2 authorization endpoint.
 *
 * For production, OAuth flow must go directly to auth-service (not API Gateway)
 * because OAuth redirects need proper handling of session state.
 *
 * Flow:
 *   1. GET /oauth2/authorize/{provider}  → provider consent screen
 *   2. GET /oauth2/callback/{provider}   → auth-service issues JWT
 *   3. Redirect to /auth/callback?token=…&refreshToken=…
 */
export function startOAuthLogin(provider: OAuthProvider): void {
  // In production, OAuth must go directly to auth-service
  const authServiceUrl = import.meta.env.VITE_AUTH_SERVICE_URL || `${BASE_URL}/oauth2/authorize/${provider}`;
  const oauthUrl = `${authServiceUrl}/oauth2/authorize/${provider}`;
  window.location.href = oauthUrl;
}
