/** Shared HTTP client: every feature calls the backend through `api` (REST under /api/v1). */

import { GUEST, getAccessToken, getDevUserId } from './session';

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

/**
 * Identity headers of every request (ACT-01): `X-User-Id` (shopping list) and `X-Dev-User-Id` (dev stubs) from the
 * dev session, none for a guest; `Authorization: Bearer` once spec 010 stores a token. Headers set by the caller win.
 */
function addIdentity(headers: Headers) {
  const user = getDevUserId();
  if (user !== GUEST) {
    if (!headers.has('X-User-Id')) headers.set('X-User-Id', String(user));
    if (!headers.has('X-Dev-User-Id')) headers.set('X-Dev-User-Id', String(user));
  }
  const token = getAccessToken();
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }
}

/** Fetches `url` (same-origin path) and returns the parsed JSON body; non-2xx responses throw {@link ApiError}. */
export async function fetchJson<T>(url: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json, application/problem+json');
  // FormData sets its own multipart Content-Type with the boundary.
  if (init.body !== undefined && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  addIdentity(headers);

  const response = await fetch(new URL(url, window.location.origin), { ...init, headers });
  if (!response.ok) {
    throw new ApiError(await toProblem(response));
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
  /** Multipart POST (e.g. a recipe cover, spec 060) with the same identity headers. */
  upload: <T>(path: string, form: FormData) => fetchJson<T>(API_BASE + path, { method: 'POST', body: form }),
};
