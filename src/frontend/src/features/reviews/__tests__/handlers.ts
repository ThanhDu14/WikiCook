/** MSW handlers of the 070 endpoints for tests only (the app always calls the real API). */
import { http, HttpResponse } from 'msw';
import type { ReviewListResponse } from '../types';
import { makeList } from './fixtures';

export const REVIEWS_URL = '/api/v1/recipes/:recipeId/reviews';
export const REVIEW_URL = '/api/v1/reviews/:reviewId';
export const HELPFUL_URL = '/api/v1/reviews/:reviewId/helpful';
export const REVIEW_REPORT_URL = '/api/v1/reviews/:reviewId/reports';
export const COOKSNAP_REPORT_URL = '/api/v1/cooksnaps/:cooksnapId/reports';

/** GET list handler returning `body` and recording each request URL into `calls`. */
export function listHandler(body: ReviewListResponse | (() => ReviewListResponse) = makeList(), calls: URL[] = []) {
  return http.get(REVIEWS_URL, ({ request }) => {
    calls.push(new URL(request.url));
    return HttpResponse.json(typeof body === 'function' ? body() : body);
  });
}
