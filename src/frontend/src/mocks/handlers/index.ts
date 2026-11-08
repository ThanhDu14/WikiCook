import { http, HttpResponse, type RequestHandler } from 'msw';
import { searchHandlers } from './search';

export const handlers: RequestHandler[] = [
  http.get('/actuator/health', () => HttpResponse.json({ status: 'UP' })),
  ...searchHandlers,
];
