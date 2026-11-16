/** Shared HTTP client: every feature calls the backend through `api` (REST under /api/v1). */

import { GUEST, getAccessToken, getDevUserId, setAccessToken } from './session';

export const API_BASE = '/api/v1';

/** RFC 7807 body returned by the backend (Spring ProblemDetail). */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  [extension: string]: unknown;
}

export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`);
    this.name = 'ApiError';
    this.status = problem.status;
    this.problem = problem;
  }
}

/** Events the client raises on `window` so `AuthProvider` (spec 010) learns what happened without importing it. */
export const AUTH_EVENTS = {
  /** A refresh succeeded; `detail` is the `AuthResponse` (`accessToken`, `expiresIn`, `account`). */
  refreshed: 'auth:refreshed',
  /** A request got 401 and the refresh cookie could not renew the session. */
  expired: 'auth:expired',
  /** 403 `ACCOUNT_SUSPENDED` / `ACCOUNT_BANNED` (FR-013, FR-024); `detail` is the problem. The token is already gone. */
  restricted: 'auth:restricted',
} as const;

function emit(name: string, detail?: unknown) {
  window.dispatchEvent(new CustomEvent(name, { detail }));
}

function isJson(response: Response) {
  const type = response.headers.get('content-type') ?? '';
  return type.includes('application/json') || type.includes('+json');
}

async function toProblem(response: Response): Promise<ProblemDetail> {
  if (isJson(response)) {
    try {
      const body = (await response.json()) as Partial<ProblemDetail>;
      return { ...body, status: body.status ?? response.status };
    } catch {
      // fall through to a synthetic problem
    }
  }
  // e.g. an HTML 502 page from the proxy while the backend is down
  return { status: response.status, title: response.statusText || 'Request failed' };
}

const AUTH_PREFIX = `${API_BASE}/auth/`;
/** The only `/auth/*` routes that need the bearer: every other one is public and ignores or rejects it (R10, R12). */
const BEARER_AUTH_ROUTES = new Set([`${API_BASE}/auth/me`, `${API_BASE}/auth/role-notice/ack`, `${API_BASE}/auth/reauthenticate`]);
const REFRESH_URL = `${API_BASE}/auth/refresh`;
const LOGIN_URL = `${API_BASE}/auth/login`;
const RESTRICTED_CODES = new Set(['ACCOUNT_SUSPENDED', 'ACCOUNT_BANNED']);

function pathOf(url: string) {
  return new URL(url, window.location.origin).pathname;
}

const isAuthRoute = (path: string) => path.startsWith(AUTH_PREFIX);
const sendsBearer = (path: string) => !isAuthRoute(path) || BEARER_AUTH_ROUTES.has(path);

/**
 * Identity headers of every request (ACT-01): `X-User-Id` (shopping list) and `X-Dev-User-Id` (dev stubs) from the
 * dev session, none for a guest; `Authorization: Bearer` while a token is held, except on the public `/auth/*`
 * routes. `/auth/*` also gets `X-WikiCook-Client: web`, the CSRF guard of the cookie-reading endpoints (R2).
 * Headers set by the caller win.
 */
function addIdentity(headers: Headers, path: string) {
  const user = getDevUserId();
  if (user !== GUEST) {
    if (!headers.has('X-User-Id')) headers.set('X-User-Id', String(user));
    if (!headers.has('X-Dev-User-Id')) headers.set('X-Dev-User-Id', String(user));
  }
  const token = getAccessToken();
  if (token && sendsBearer(path) && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  if (isAuthRoute(path)) {
    headers.set('X-WikiCook-Client', 'web');
  }
}

/** What a refresh came to; `unavailable` (network or 5xx) is not a 401, the session may still be fine (spec 010 T051). */
export type RefreshResult<Account = unknown> =
  | { status: 'ok'; auth: { accessToken: string; expiresIn: number; account: Account } }
  | { status: 'unauthenticated' }
  | { status: 'restricted'; problem: ProblemDetail }
  | { status: 'unavailable'; error: unknown };

let refreshing: Promise<RefreshResult> | null = null;

async function doRefresh(): Promise<RefreshResult> {
  let response: Response;
  try {
    const headers = new Headers({ Accept: 'application/json, application/problem+json' });
    addIdentity(headers, REFRESH_URL);
    response = await fetch(new URL(REFRESH_URL, window.location.origin), { method: 'POST', headers, credentials: 'same-origin' });
  } catch (error) {
    return { status: 'unavailable', error };
  }
  if (response.ok) {
    try {
      const auth = (await response.json()) as { accessToken: string; expiresIn: number; account: unknown };
      setAccessToken(auth.accessToken);
      emit(AUTH_EVENTS.refreshed, auth);
      return { status: 'ok', auth };
    } catch (error) {
      return { status: 'unavailable', error };
    }
  }
  const problem = await toProblem(response);
  if (response.status >= 500) {
    return { status: 'unavailable', error: new ApiError(problem) };
  }
  if (RESTRICTED_CODES.has(String(problem.code))) {
    setAccessToken(null);
    emit(AUTH_EVENTS.restricted, problem);
    return { status: 'restricted', problem };
  }
  setAccessToken(null);
  return { status: 'unauthenticated' };
}

/**
 * Exchanges the HttpOnly refresh cookie for a new access token (`POST /auth/refresh`). One request at a time:
 * callers that arrive while one is in flight share its result, so five 401s in a row cause one refresh.
 */
export function refreshSession<Account = unknown>(): Promise<RefreshResult<Account>> {
  refreshing ??= doRefresh().finally(() => {
    refreshing = null;
  });
  return refreshing as Promise<RefreshResult<Account>>;
}

async function send(url: string, init: RequestInit, path: string): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json, application/problem+json');
  // FormData sets its own multipart Content-Type with the boundary.
  if (init.body !== undefined && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  addIdentity(headers, path);
  return fetch(new URL(url, window.location.origin), { ...init, headers, credentials: 'same-origin' });
}

/**
 * Fetches `url` (same-origin path) and returns the parsed JSON body; non-2xx responses throw {@link ApiError}.
 * A 401 outside `/auth/*` triggers one refresh and one resend of the request; if the session cannot be renewed the
 * original 401 is thrown (code `UNAUTHENTICATED`) and `auth:expired` is raised. Features never touch tokens.
 */
export async function fetchJson<T>(url: string, init: RequestInit = {}): Promise<T> {
  const path = pathOf(url);
  let response = await send(url, init, path);

  if (response.status === 401 && !isAuthRoute(path)) {
    const result = await refreshSession();
    if (result.status === 'ok') {
      response = await send(url, init, path);
    } else if (result.status === 'unavailable') {
      throw result.error;
    } else if (result.status === 'unauthenticated') {
      emit(AUTH_EVENTS.expired);
    }
  }

  if (!response.ok) {
    const problem = await toProblem(response);
    if (response.status === 403 && RESTRICTED_CODES.has(String(problem.code)) && path !== LOGIN_URL) {
      setAccessToken(null);
      emit(AUTH_EVENTS.restricted, problem);
    }
    throw new ApiError(problem);
  }
  if (response.status === 204 || !isJson(response)) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

const withBody = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) });

export const api = {
  get: <T>(path: string) => fetchJson<T>(API_BASE + path),
  post: <T>(path: string, body: unknown) => fetchJson<T>(API_BASE + path, withBody('POST', body)),
  put: <T>(path: string, body: unknown) => fetchJson<T>(API_BASE + path, withBody('PUT', body)),
  patch: <T>(path: string, body: unknown) => fetchJson<T>(API_BASE + path, withBody('PATCH', body)),
  delete: <T = void>(path: string) => fetchJson<T>(API_BASE + path, { method: 'DELETE' }),
  /** Multipart POST (e.g. a recipe cover, spec 060; the profile with its avatar, spec 011) with the same identity headers. */
  upload: <T>(path: string, form: FormData) => fetchJson<T>(API_BASE + path, { method: 'POST', body: form }),
  /** Same as {@link api.upload}: the name the spec 011 tasks use for a multipart POST. */
  postForm: <T>(path: string, form: FormData) => fetchJson<T>(API_BASE + path, { method: 'POST', body: form }),
};
