/**
 * Who the browser acts as. The access token (JWT of spec 010, 15 minutes) lives in memory only (research R2): it is
 * never written to `localStorage`, so an XSS cannot read it, and a reload gets a new one from the HttpOnly refresh
 * cookie (`POST /auth/refresh`). Until the backend of spec 010 is merged, a dev user id still stands in for the
 * signed-in user (Sprint 3 retro ACT-01); GUEST sends no identity at all (search then shows everything, spec 020
 * AS 3.4).
 */

export const GUEST = 'guest' as const;
export type DevUser = number | typeof GUEST;

const DEV_USER_KEY = 'wk.devUserId';
/** Where an earlier build kept the token; removed on first use so no token stays in `localStorage` (R2). */
const LEGACY_TOKEN_KEY = 'wk.accessToken';
const DEFAULT_DEV_USER = 1;

let accessToken: string | null = null;

function storage(): Storage | null {
  try {
    return window.localStorage;
  } catch {
    return null; // private mode or storage disabled
  }
}

export function getDevUserId(): DevUser {
  const value = storage()?.getItem(DEV_USER_KEY);
  if (value === GUEST) {
    return GUEST;
  }
  const id = Number(value);
  return value !== null && value !== undefined && Number.isInteger(id) && id > 0 ? id : DEFAULT_DEV_USER;
}

export function setDevUserId(user: DevUser): void {
  storage()?.setItem(DEV_USER_KEY, String(user));
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
  storage()?.removeItem(LEGACY_TOKEN_KEY);
}
