/** Maps the RFC 7807 errors of the 070 contract to the Vietnamese messages of strings.vi.ts. */
import { ApiError } from '@/shared/api/client';
import { strings } from '../strings.vi';

const BY_TYPE: Record<string, string> = {
  'validation-error': strings.errors.invalid,
  'self-review-forbidden': strings.errors.selfReview,
  'review-already-exists': strings.errors.alreadyReviewed,
  'review-under-moderation': strings.errors.underModeration,
  'rate-limit-exceeded': strings.errors.rateLimited,
  'report-already-exists': strings.errors.alreadyReported,
  'self-report-forbidden': strings.errors.selfReport,
};

/** Last path segment of `ProblemDetail.type`, e.g. "report-already-exists". */
export function errorSlug(error: unknown): string | null {
  if (!(error instanceof ApiError) || typeof error.problem.type !== 'string') return null;
  return error.problem.type.split('/').pop() ?? null;
}

export const isUnauthenticated = (error: unknown) => error instanceof ApiError && error.status === 401;

/** A fetch that never reached the server (offline, DNS) rejects with a TypeError, not an ApiError. */
export const isNetworkError = (error: unknown) => !(error instanceof ApiError);

export function errorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return strings.errors.network;
  const slug = errorSlug(error);
  if (slug && BY_TYPE[slug]) return BY_TYPE[slug];
  switch (error.status) {
    case 400:
      return strings.errors.invalid;
    case 401:
      return strings.errors.signInRequired;
    case 403:
      return strings.errors.forbidden;
    case 404:
      return strings.errors.notFound;
    case 422:
      return strings.errors.recipeUnavailable;
    case 429:
      return strings.errors.rateLimited;
    default:
      return strings.errors.server;
  }
}

/** `invalidParams` of a 400 as `{ field: reason }`. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError) || error.status !== 400) return {};
  const params = error.problem.invalidParams;
  if (!Array.isArray(params)) return {};
  const result: Record<string, string> = {};
  for (const param of params as { name?: unknown; reason?: unknown }[]) {
    if (typeof param.name === 'string' && typeof param.reason === 'string') result[param.name] = param.reason;
  }
  return result;
}
