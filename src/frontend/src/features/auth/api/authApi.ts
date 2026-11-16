import { api, API_BASE, fetchJson, refreshSession, type RefreshResult } from '@/shared/api/client';
import { setAccessToken } from '@/shared/api/session';
import type {
  AuthResponse,
  CurrentAccount,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  ResendVerificationRequest,
  VerificationResult,
} from '../types';

/** `POST /auth/register`: creates an unverified Member and mails the 24-hour link; no session yet (FR-001 – FR-005). */
export const register = (body: RegisterRequest) => api.post<RegisterResponse>('/auth/register', body);

/** `POST /auth/login`: on success the access token is kept in memory and the server sets the `wc_rt` cookie (FR-007, FR-009). */
export async function login(body: LoginRequest): Promise<AuthResponse> {
  const auth = await api.post<AuthResponse>('/auth/login', body);
  setAccessToken(auth.accessToken);
  return auth;
}

/** `POST /auth/logout`: revokes this device's session; the token is dropped even when the call fails (FR-010). */
export async function logout(): Promise<void> {
  try {
    await fetchJson<void>(`${API_BASE}/auth/logout`, { method: 'POST' });
  } finally {
    setAccessToken(null);
  }
}

/** `POST /auth/refresh` through the shared single-flight client; used by `AuthProvider` at start-up. */
export const refresh = (): Promise<RefreshResult<CurrentAccount>> => refreshSession<CurrentAccount>();

/** `GET /auth/me`. */
export const me = () => api.get<CurrentAccount>('/auth/me');

/** `POST /auth/verification/confirm`: the token comes from the `#token=` fragment of the e-mailed link (FR-005). */
export const confirmVerification = (token: string) => api.post<VerificationResult>('/auth/verification/confirm', { token });

/** `POST /auth/verification/resend`: always 202, even for an unknown address; 429 after 3 sends in 15 minutes (FR-017). */
export const resendVerification = (body: ResendVerificationRequest) => api.post<void>('/auth/verification/resend', body);

/** `POST /auth/password/forgot`: 202 with the same answer and timing for any address; 429 after too many requests (FR-014, FR-017). */
export const forgotPassword = (email: string) => api.post<void>('/auth/password/forgot', { email });

/** `POST /auth/password/reset/validate`: checks the link before the form shows, without using it up. */
export const validateResetToken = (token: string) => api.post<{ email: string; expiresAt: string }>('/auth/password/reset/validate', { token });

/** `POST /auth/password/reset`: sets the new password and ends every session (FR-015, FR-016). */
export const resetPassword = (body: { token: string; newPassword: string; confirmPassword: string }) =>
  api.post<void>('/auth/password/reset', body);
