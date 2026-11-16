import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

/** Default handlers every test gets; override per test with `server.use(...)`. */
export const server = setupServer(
  http.get('/actuator/health', () => HttpResponse.json({ status: 'UP' })),
  // A guest: the app asks for a session on start-up and after every 401 (spec 010 T048). Tests that need a signed-in
  // user override it with `signedInHandlers` from `@/features/auth/__tests__/handlers`.
  http.post('/api/v1/auth/refresh', () =>
    HttpResponse.json(
      { type: 'about:blank', title: 'Unauthorized', status: 401, code: 'UNAUTHENTICATED' },
      { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
    ),
  ),
);
