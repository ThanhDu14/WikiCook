/**
 * Who the browser acts as (Sprint 3 retro ACT-01). Until spec 010 adds login, a dev user id stands in for the
 * signed-in user; GUEST sends no identity at all (search then shows everything, spec 020 AS 3.4). The access token
 * slot is where spec 010 will store its JWT, so every feature already sends `Authorization`.
 */

export const GUEST = 'guest' as const;
export type DevUser = number | typeof GUEST;

const DEV_USER_KEY = 'wk.devUserId';
const TOKEN_KEY = 'wk.accessToken';
const DEFAULT_DEV_USER = 1;

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
  return storage()?.getItem(TOKEN_KEY) ?? null;
}

export function setAccessToken(token: string | null): void {
  if (token === null) {
    storage()?.removeItem(TOKEN_KEY);
  } else {
    storage()?.setItem(TOKEN_KEY, token);
  }
}
