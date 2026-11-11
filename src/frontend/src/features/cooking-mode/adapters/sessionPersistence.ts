import type {
  CookingSession,
  StorageEnvelopeV1,
  StorageLoadResult,
  RecipeId,
  EpochMs,
} from '../domain/sessionModel';
import { tickTimers } from '../domain/timerEngine';

const STORAGE_PREFIX = 'cooking_session_';

export function getSessionStorageKey(recipeId: RecipeId): string {
  return `${STORAGE_PREFIX}${recipeId}`;
}

export function saveSessionToStorage(
  session: CookingSession,
  writerId: string,
  revision: number,
  now: EpochMs = Date.now(),
): void {
  if (typeof localStorage === 'undefined') return;

  const envelope: StorageEnvelopeV1 = {
    schemaVersion: 1,
    writerId,
    revision,
    savedAt: now,
    session,
  };

  const key = getSessionStorageKey(session.recipeId);
  localStorage.setItem(key, JSON.stringify(envelope));
}

export function loadSessionFromStorage(
  recipeId: RecipeId,
  now: EpochMs = Date.now(),
): StorageLoadResult {
  if (typeof localStorage === 'undefined') {
    return { kind: 'UNAVAILABLE' };
  }

  const key = getSessionStorageKey(recipeId);
  const raw = localStorage.getItem(key);
  if (!raw) {
    return { kind: 'NONE' };
  }

  try {
    const parsed = JSON.parse(raw);

    // Validate envelope structure
    if (!parsed || typeof parsed !== 'object') {
      return { kind: 'INVALID' };
    }

    if (parsed.schemaVersion !== 1) {
      return { kind: 'UNSUPPORTED_VERSION' };
    }

    const session = parsed.session;
    if (
      !session ||
      typeof session !== 'object' ||
      typeof session.sessionId !== 'string' ||
      typeof session.recipeId !== 'string' ||
      typeof session.startedAt !== 'number' ||
      typeof session.expiresAt !== 'number'
    ) {
      return { kind: 'INVALID' };
    }

    // Check fixed 4-hour TTL (A08)
    if (now >= session.expiresAt) {
      clearSessionFromStorage(recipeId);
      return { kind: 'EXPIRED' };
    }

    // Do not prompt resume for already completed sessions
    if (session.isCompleted || session.phase === 'COMPLETED') {
      return { kind: 'NONE' };
    }

    // Reconcile timer deadlines immediately against current wall clock (FR-018, US5-AC3, US5-AC4)
    const reconciledTimers = Array.isArray(session.timers)
      ? tickTimers(session.timers, now)
      : [];

    const reconciledEnvelope: StorageEnvelopeV1 = {
      ...(parsed as StorageEnvelopeV1),
      session: {
        ...(session as CookingSession),
        timers: reconciledTimers,
      },
    };

    return {
      kind: 'RESUMABLE',
      value: reconciledEnvelope,
    };
  } catch {
    return { kind: 'INVALID' };
  }
}

export function clearSessionFromStorage(recipeId: RecipeId): void {
  if (typeof localStorage === 'undefined') return;
  const key = getSessionStorageKey(recipeId);
  localStorage.removeItem(key);
}

export function hasResumableSession(recipeId: RecipeId, now: EpochMs = Date.now()): boolean {
  const result = loadSessionFromStorage(recipeId, now);
  return result.kind === 'RESUMABLE';
}
