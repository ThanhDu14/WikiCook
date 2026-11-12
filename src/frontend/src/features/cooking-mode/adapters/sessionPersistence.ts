import type {
  CookingSession,
  StorageEnvelopeV1,
  StorageLoadResult,
  RecipeId,
  EpochMs,
} from '../domain/sessionModel';
import { tickTimers } from '../domain/timerEngine';

const STORAGE_PREFIX = 'cooking_session_';
const SESSION_TTL_MS = 4 * 3600 * 1000;

const isRecord = (value: unknown): value is Record<string, unknown> =>
  value !== null && typeof value === 'object' && !Array.isArray(value);
const isFiniteNumber = (value: unknown): value is number =>
  typeof value === 'number' && Number.isFinite(value);
const isNonNegative = (value: unknown): value is number => isFiniteNumber(value) && value >= 0;
const isPositive = (value: unknown): value is number => isFiniteNumber(value) && value > 0;
const isIndex = (value: unknown): value is number => isNonNegative(value) && Number.isInteger(value);
const isString = (value: unknown): value is string => typeof value === 'string';
const isNullableString = (value: unknown): boolean => value === null || isString(value);
const isStrings = (value: unknown): value is string[] => Array.isArray(value) && value.every(isString);

function isSnapshot(value: unknown, recipeId: RecipeId): boolean {
  if (!isRecord(value) || value.recipeId !== recipeId || value.statusAtLoad !== 'PUBLISHED' ||
    !isString(value.title) || !isFiniteNumber(value.loadedAt) ||
    !(value.baseServings === null || isPositive(value.baseServings)) ||
    !Array.isArray(value.ingredients) || !Array.isArray(value.steps) || value.steps.length === 0) return false;

  return value.ingredients.every((row: unknown) => isRecord(row) &&
    isString(row.rowId) && (row.ingredientId === undefined || isString(row.ingredientId)) &&
    isString(row.name) && (row.quantity === null || isFiniteNumber(row.quantity)) &&
    isNullableString(row.amountText) && isString(row.unitName) && isNullableString(row.note) &&
    isNullableString(row.groupName) && typeof row.isOptional === 'boolean' && isFiniteNumber(row.position)) &&
    value.steps.every((step: unknown) => isRecord(step) && isString(step.stepId) &&
      isIndex(step.stepNumber) && step.stepNumber > 0 && isString(step.instructionText) &&
      (step.timerSeconds === null || isNonNegative(step.timerSeconds)) &&
      (step.ingredientRowIds === undefined || isStrings(step.ingredientRowIds)) &&
      Array.isArray(step.images) && step.images.every((image: unknown) => isRecord(image) &&
        isString(image.url) && isString(image.alt) && isFiniteNumber(image.position)));
}

function isTimer(value: unknown, sessionId: string): boolean {
  if (!isRecord(value) || !isString(value.timerId) || value.sessionId !== sessionId ||
    !isString(value.label) || !isNonNegative(value.totalDurationSeconds) ||
    !isFiniteNumber(value.createdAt) ||
    !(value.stepNumber === null || (isIndex(value.stepNumber) && value.stepNumber > 0))) return false;

  switch (value.status) {
    case 'IDLE': return isNonNegative(value.durationMs);
    case 'RUNNING': return isFiniteNumber(value.targetEndTimestamp);
    case 'PAUSED': return isNonNegative(value.remainingMs);
    case 'EXPIRED': return isFiniteNumber(value.expiredAt) &&
      (value.acknowledgedAt === null || isFiniteNumber(value.acknowledgedAt));
    case 'DISMISSED': return isFiniteNumber(value.dismissedAt);
    default: return false;
  }
}

function isEnvelope(value: Record<string, unknown>, recipeId: RecipeId): value is Record<string, unknown> & StorageEnvelopeV1 {
  const session = value.session;
  if (!isString(value.writerId) || !isIndex(value.revision) || !isFiniteNumber(value.savedAt) ||
    !isRecord(session) || !isString(session.sessionId) || session.recipeId !== recipeId ||
    !isFiniteNumber(session.startedAt) || !isFiniteNumber(session.lastActiveAt) ||
    !isFiniteNumber(session.expiresAt) || session.expiresAt !== session.startedAt + SESSION_TTL_MS ||
    !isIndex(session.currentStepIndex) || !isSnapshot(session.snapshot, recipeId) ||
    !(isString(session.phase) && ['COOKING', 'BACKGROUND', 'COMPLETED'].includes(session.phase)) ||
    typeof session.isCompleted !== 'boolean' ||
    !(isString(session.completionPrompt) && ['NOT_SHOWN', 'SHOWN', 'DISMISSED'].includes(session.completionPrompt)) ||
    !isStrings(session.checkedIngredientRowIds) || !isRecord(session.preferences) ||
    !(isString(session.preferences.theme) && ['LIGHT', 'HIGH_CONTRAST_DARK'].includes(session.preferences.theme)) ||
    !isPositive(session.preferences.fontScale) || !Array.isArray(session.timers)) return false;

  const snapshot = session.snapshot as CookingSession['snapshot'];
  return session.currentStepIndex < snapshot.steps.length &&
    session.timers.every((timer: unknown) => isTimer(timer, session.sessionId as string));
}

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
  const key = getSessionStorageKey(recipeId);
  let raw: string | null;
  try {
    if (typeof localStorage === 'undefined') return { kind: 'UNAVAILABLE' };
    raw = localStorage.getItem(key);
  } catch {
    return { kind: 'UNAVAILABLE' };
  }
  if (!raw) {
    return { kind: 'NONE' };
  }

  try {
    const parsed = JSON.parse(raw);

    // Validate envelope structure
    if (!isRecord(parsed)) {
      return { kind: 'INVALID' };
    }

    if (parsed.schemaVersion !== 1) {
      return { kind: 'UNSUPPORTED_VERSION' };
    }

    if (!isEnvelope(parsed, recipeId)) {
      return { kind: 'INVALID' };
    }
    const session = parsed.session;

    // Check fixed 4-hour TTL (A08)
    if (now >= session.expiresAt) {
      try {
        localStorage.removeItem(key);
      } catch {
        return { kind: 'UNAVAILABLE' };
      }
      return { kind: 'EXPIRED' };
    }

    // Do not prompt resume for already completed sessions
    if (session.isCompleted || session.phase === 'COMPLETED') {
      return { kind: 'NONE' };
    }

    // Reconcile timer deadlines immediately against current wall clock (FR-018, US5-AC3, US5-AC4)
    const reconciledTimers = tickTimers(session.timers, now);

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
  try {
    if (typeof localStorage === 'undefined') return;
    localStorage.removeItem(getSessionStorageKey(recipeId));
  } catch {
    // Clearing persistence must not prevent cancellation of the in-memory session (A09).
  }
}

export function hasResumableSession(recipeId: RecipeId, now: EpochMs = Date.now()): boolean {
  const result = loadSessionFromStorage(recipeId, now);
  return result.kind === 'RESUMABLE';
}
