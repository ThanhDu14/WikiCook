import { API_BASE, ApiError, api, fetchJson } from '@/shared/api/client';
import type { AiErrorCode, ConfirmRequest, ConfirmResult, DraftResponse, GenerateRequest, GenerationOptions } from '../types';

/** A failed AI planner call: HTTP status (0 = no response) and the ProblemDetail `code`; `problem` keeps the rest. */
export class AiPlannerApiError extends Error {
  readonly status: number;
  readonly code: AiErrorCode;
  readonly problem: Record<string, unknown>;

  constructor(status: number, code: AiErrorCode, message: string, problem: Record<string, unknown> = {}) {
    super(message);
    this.name = 'AiPlannerApiError';
    this.status = status;
    this.code = code;
    this.problem = problem;
  }
}

function toError(error: unknown): AiPlannerApiError {
  if (error instanceof AiPlannerApiError) return error;
  if (error instanceof ApiError) {
    const code = typeof error.problem.code === 'string' ? (error.problem.code as AiErrorCode) : 'UNKNOWN';
    return new AiPlannerApiError(error.status, code, error.message, error.problem);
  }
  if (error instanceof DOMException && error.name === 'AbortError') {
    return new AiPlannerApiError(0, 'ABORTED', 'Aborted');
  }
  // fetch rejects with a TypeError when the network is down
  return new AiPlannerApiError(0, 'NETWORK', error instanceof Error ? error.message : 'Network error');
}

async function call<T>(request: () => Promise<T>, signal?: AbortSignal): Promise<T> {
  try {
    return await request();
  } catch (error) {
    if (signal?.aborted) throw new AiPlannerApiError(0, 'ABORTED', 'Aborted');
    throw toError(error);
  }
}

const week = (weekStart: string) => `/ai-planner/weeks/${encodeURIComponent(weekStart)}`;

/** Calls of specs/031 contracts/openapi.yaml (T026); confirm and suggestions are used by later stories. */
export const aiPlannerApi = {
  getOptions: (weekStart: string) => call(() => api.get<GenerationOptions>(`${week(weekStart)}/options`)),

  /** Synchronous, up to 60 s; `signal` aborts the wait (FR-023). */
  generate: (weekStart: string, body: GenerateRequest, signal?: AbortSignal) =>
    call(
      () => fetchJson<DraftResponse>(`${API_BASE}${week(weekStart)}/generations`, { method: 'POST', body: JSON.stringify(body), signal }),
      signal,
    ),

  cancel: (requestId: string) => call(() => api.delete(`/ai-planner/generations/${encodeURIComponent(requestId)}`)),

  confirm: (weekStart: string, body: ConfirmRequest) => call(() => api.post<ConfirmResult>(`${week(weekStart)}/confirm`, body)),
};

/** A random v4 UUID for `GenerateRequest.requestId`. */
export function newRequestId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}
