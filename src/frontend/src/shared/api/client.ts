/** Shared HTTP client: every feature calls the backend through `api` (REST under /api/v1). */

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

/** Fetches `url` (same-origin path) and returns the parsed JSON body; non-2xx responses throw {@link ApiError}. */
export async function fetchJson<T>(url: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json, application/problem+json');
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

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
};
