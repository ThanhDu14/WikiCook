import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { api, ApiError, AUTH_EVENTS, refreshSession } from './client';
import { getAccessToken, setAccessToken } from './session';
import { installMemoryStorage } from './memoryStorage.testutil';

beforeAll(() => {
  installMemoryStorage();
});

const problem = (status: number, code: string, extra: Record<string, unknown> = {}) =>
  HttpResponse.json({ type: 'about:blank', title: code, status, code, ...extra }, { status, headers: { 'Content-Type': 'application/problem+json' } });

const account = { id: 1, displayName: 'Thanh Du', email: 'du@example.com', role: 'MEMBER', emailVerified: true };
const auth = (token: string) => HttpResponse.json({ accessToken: token, expiresIn: 900, account });

const events: Array<{ name: string; detail: unknown }> = [];
const listeners = Object.values(AUTH_EVENTS).map((name) => {
  const handler = (event: Event) => events.push({ name, detail: (event as CustomEvent).detail });
  return { name, handler };
});

beforeEach(() => {
  events.length = 0;
  for (const { name, handler } of listeners) window.addEventListener(name, handler);
});

afterEach(() => {
  for (const { name, handler } of listeners) window.removeEventListener(name, handler);
  setAccessToken(null);
});

describe('api client bearer and /auth/* headers (spec 010 FR-010, R12)', () => {
  it('sends the bearer on /auth/me but on no public /auth/* route, and the CSRF header on all of them', async () => {
    setAccessToken('jwt-1');
    const seen: Record<string, { authorization: string | null; client: string | null }> = {};
    const record = ({ request }: { request: Request }) => {
      seen[new URL(request.url).pathname] = {
        authorization: request.headers.get('authorization'),
        client: request.headers.get('x-wikicook-client'),
      };
      return HttpResponse.json({});
    };
    server.use(
      http.get('/api/v1/auth/me', record),
      http.post('/api/v1/auth/login', record),
      http.post('/api/v1/auth/register', record),
      http.post('/api/v1/auth/logout', () => new HttpResponse(null, { status: 204 })),
      http.get('/api/v1/me/profile', record),
    );

    await api.get('/auth/me');
    await api.post('/auth/login', {});
    await api.post('/auth/register', {});
    await api.get('/me/profile');

    expect(seen['/api/v1/auth/me']).toEqual({ authorization: 'Bearer jwt-1', client: 'web' });
    expect(seen['/api/v1/auth/login']).toEqual({ authorization: null, client: 'web' });
    expect(seen['/api/v1/auth/register']).toEqual({ authorization: null, client: 'web' });
    expect(seen['/api/v1/me/profile']).toEqual({ authorization: 'Bearer jwt-1', client: null });
  });
});

describe('api client refresh on 401 (spec 010 T023)', () => {
  it('refreshes once, stores the new token, raises auth:refreshed and resends the request', async () => {
    setAccessToken('expired');
    const sent: Array<string | null> = [];
    server.use(
      http.get('/api/v1/me/profile', ({ request }) => {
        sent.push(request.headers.get('authorization'));
        return request.headers.get('authorization') === 'Bearer fresh' ? HttpResponse.json({ ok: true }) : problem(401, 'UNAUTHENTICATED');
      }),
      http.post('/api/v1/auth/refresh', () => auth('fresh')),
    );

    await expect(api.get('/me/profile')).resolves.toEqual({ ok: true });

    expect(sent).toEqual(['Bearer expired', 'Bearer fresh']);
    expect(getAccessToken()).toBe('fresh');
    expect(events.map((event) => event.name)).toEqual([AUTH_EVENTS.refreshed]);
    expect(events[0].detail).toMatchObject({ accessToken: 'fresh', account: { displayName: 'Thanh Du' } });
  });

  it('throws the original 401 and raises auth:expired when the refresh fails', async () => {
    setAccessToken('expired');
    server.use(http.get('/api/v1/me/profile', () => problem(401, 'UNAUTHENTICATED')));

    const error = await api.get('/me/profile').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 401, problem: { code: 'UNAUTHENTICATED' } });
    expect(getAccessToken()).toBeNull();
    expect(events.map((event) => event.name)).toEqual([AUTH_EVENTS.expired]);
  });

  it('resends only once: a second 401 after a good refresh is the answer', async () => {
    let calls = 0;
    server.use(
      http.get('/api/v1/me/profile', () => {
        calls += 1;
        return problem(401, 'UNAUTHENTICATED');
      }),
      http.post('/api/v1/auth/refresh', () => auth('fresh')),
    );

    await expect(api.get('/me/profile')).rejects.toMatchObject({ status: 401 });

    expect(calls).toBe(2);
  });

  it('five concurrent 401s cause one refresh', async () => {
    let refreshes = 0;
    server.use(
      http.get('/api/v1/me/profile', ({ request }) =>
        request.headers.get('authorization') === 'Bearer fresh' ? HttpResponse.json({ ok: true }) : problem(401, 'UNAUTHENTICATED'),
      ),
      http.post('/api/v1/auth/refresh', async () => {
        refreshes += 1;
        await new Promise((resolve) => setTimeout(resolve, 20));
        return auth('fresh');
      }),
    );

    const results = await Promise.all(Array.from({ length: 5 }, () => api.get('/me/profile')));

    expect(results).toHaveLength(5);
    expect(refreshes).toBe(1);
  });

  it('does not refresh for a 401 of /auth/* itself, so a wrong password is not retried', async () => {
    let refreshes = 0;
    server.use(
      http.post('/api/v1/auth/login', () => problem(401, 'INVALID_CREDENTIALS')),
      http.post('/api/v1/auth/refresh', () => {
        refreshes += 1;
        return auth('x');
      }),
    );

    await expect(api.post('/auth/login', {})).rejects.toMatchObject({ status: 401, problem: { code: 'INVALID_CREDENTIALS' } });

    expect(refreshes).toBe(0);
  });

  it('does not turn a network or 5xx failure of the refresh into a 401', async () => {
    server.use(
      http.get('/api/v1/me/profile', () => problem(401, 'UNAUTHENTICATED')),
      http.post('/api/v1/auth/refresh', () => problem(503, 'SERVICE_UNAVAILABLE')),
    );

    await expect(api.get('/me/profile')).rejects.toMatchObject({ status: 503 });
    expect(events.map((event) => event.name)).not.toContain(AUTH_EVENTS.expired);
  });

  it('a suspended or banned account loses the token and raises auth:restricted (FR-013, FR-024)', async () => {
    setAccessToken('jwt');
    server.use(http.get('/api/v1/me/profile', () => problem(403, 'ACCOUNT_SUSPENDED', { reason: 'Spam', endsAt: '2026-12-01T00:00:00Z' })));

    await expect(api.get('/me/profile')).rejects.toMatchObject({ status: 403, problem: { code: 'ACCOUNT_SUSPENDED' } });

    expect(getAccessToken()).toBeNull();
    expect(events).toEqual([{ name: AUTH_EVENTS.restricted, detail: expect.objectContaining({ code: 'ACCOUNT_SUSPENDED', reason: 'Spam' }) }]);
  });

  it('refreshSession reports the kind of failure', async () => {
    server.use(http.post('/api/v1/auth/refresh', () => problem(403, 'ACCOUNT_BANNED', { reason: 'Vi phạm' })));

    await expect(refreshSession()).resolves.toMatchObject({ status: 'restricted', problem: { code: 'ACCOUNT_BANNED' } });
  });

  it('a 403 that is not a suspension keeps the token (a missing role is not a sign-out)', async () => {
    setAccessToken('jwt');
    server.use(http.get('/api/v1/admin/audit-logs', () => problem(403, 'ACCESS_DENIED')));

    await expect(api.get('/admin/audit-logs')).rejects.toMatchObject({ status: 403 });

    expect(getAccessToken()).toBe('jwt');
  });
});
