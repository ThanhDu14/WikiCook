import { ApiError } from '@/shared/api/client';
import { FIELD_ERROR_MESSAGES, strings } from '../strings.vi';
import type { FieldError } from '../types';

/** The `code` of a problem (spec 010 contract), or `undefined` for anything that is not an API problem. */
export function problemCode(error: unknown): string | undefined {
  return error instanceof ApiError ? (error.problem.code as string | undefined) : undefined;
}

/** `errors[]` of a `VALIDATION_ERROR` as `{ field: message }`; the first error of a field wins. */
export function fieldMessages(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError) || !Array.isArray(error.problem.errors)) {
    return {};
  }
  const messages: Record<string, string> = {};
  for (const entry of error.problem.errors as FieldError[]) {
    messages[entry.field] ??= FIELD_ERROR_MESSAGES[entry.code] ?? strings.errors.invalidFormat;
  }
  return messages;
}

/** Whole minutes of a `retryAfterSeconds` for "thử lại sau X phút" (at least 1). */
export function retryMinutes(error: unknown): number {
  const seconds = error instanceof ApiError ? Number(error.problem.retryAfterSeconds) : NaN;
  return Number.isFinite(seconds) && seconds > 0 ? Math.max(1, Math.ceil(seconds / 60)) : 1;
}

/** A failure that is the network, not the server's answer. */
export const isNetworkError = (error: unknown) => !(error instanceof ApiError);
