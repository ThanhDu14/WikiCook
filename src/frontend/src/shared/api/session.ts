/**
 * The access token (JWT of spec 010, 15 minutes) lives in memory only (research R2): it is never written to
 * `localStorage`, so an XSS cannot read it, and a reload gets a new one from the HttpOnly refresh cookie
 * (`POST /auth/refresh`). A signed-out browser holds no token and sends no identity at all (search then shows
 * everything, spec 020 AS 3.4).
 */

/** Where earlier builds kept the token and the dev identity; removed on first use so nothing stays in `localStorage` (R2). */
const LEGACY_KEYS = ['wk.accessToken', 'wk.devUserId'];

let accessToken: string | null = null;

function storage(): Storage | null {
  try {
    return window.localStorage;
  } catch {
    return null; // private mode or storage disabled
  }
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
  const local = storage();
  for (const key of LEGACY_KEYS) local?.removeItem(key);
}
