import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

/** Default handlers every test gets; override per test with `server.use(...)`. */
export const server = setupServer(http.get('/actuator/health', () => HttpResponse.json({ status: 'UP' })));
