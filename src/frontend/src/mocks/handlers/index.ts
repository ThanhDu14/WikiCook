import type { RequestHandler } from 'msw';
import { searchHandlers } from './search';

export const handlers: RequestHandler[] = [
  ...searchHandlers,
];
