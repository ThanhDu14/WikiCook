import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { createTimer } from '../domain/timerEngine';
import {
  saveSessionToStorage,
  loadSessionFromStorage,
  clearSessionFromStorage,
  hasResumableSession,
} from '../adapters/sessionPersistence';
import { createInitialSession } from '../domain/sessionReducer';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';
import type { CookingSession } from '../domain/sessionModel';

describe('sessionPersistence (T041 - US5)', () => {
  const BASE_NOW = 1731300000000;
  let testSession: CookingSession;

  beforeEach(() => {
    localStorage.clear();
    testSession = createInitialSession(mockPhoBoSnapshot, 'sess-persist-1', BASE_NOW);
  });

  afterEach(() => vi.restoreAllMocks());

  it('returns UNAVAILABLE when reading storage throws', () => {
    vi.spyOn(localStorage, 'getItem').mockImplementation(() => { throw new Error('blocked'); });
    expect(loadSessionFromStorage(testSession.recipeId, BASE_NOW).kind).toBe('UNAVAILABLE');
    expect(hasResumableSession(testSession.recipeId, BASE_NOW)).toBe(false);
  });

  it('degrades safely when the localStorage getter throws', () => {
    vi.spyOn(globalThis, 'localStorage', 'get').mockImplementation(() => { throw new Error('blocked'); });
    expect(loadSessionFromStorage(testSession.recipeId, BASE_NOW).kind).toBe('UNAVAILABLE');
    expect(() => clearSessionFromStorage(testSession.recipeId)).not.toThrow();
  });

  it.each([
    { status: 'IDLE', durationMs: 60000 },
    { status: 'RUNNING', targetEndTimestamp: BASE_NOW + 60000 },
    { status: 'PAUSED', remainingMs: 30000 },
    { status: 'EXPIRED', expiredAt: BASE_NOW - 60000, acknowledgedAt: null },
    { status: 'EXPIRED', expiredAt: BASE_NOW - 60000, acknowledgedAt: BASE_NOW },
    { status: 'DISMISSED', dismissedAt: BASE_NOW },
  ])('restores valid timer state %j', (patch) => {
    const timer = createTimer({ sessionId: testSession.sessionId, label: 'Boil', durationSeconds: 60, now: BASE_NOW });
    localStorage.setItem(`cooking_session_${testSession.recipeId}`, JSON.stringify({
      schemaVersion: 1, writerId: 'writer', revision: 0, savedAt: BASE_NOW,
      session: { ...testSession, timers: [{ ...timer, ...patch }] },
    }));
    const result = loadSessionFromStorage(testSession.recipeId, BASE_NOW);
    expect(result.kind).toBe('RESUMABLE');
    if (result.kind === 'RESUMABLE') expect(result.value.session.timers[0]).toMatchObject(patch);
  });

  it('does not offer completed sessions for resume', () => {
    saveSessionToStorage({ ...testSession, isCompleted: true, phase: 'COMPLETED' }, 'writer', 1, BASE_NOW);
    expect(loadSessionFromStorage(testSession.recipeId, BASE_NOW).kind).toBe('NONE');
  });

  it('does not crash when clearing storage throws and reports failed expiry purge', () => {
    saveSessionToStorage(testSession, 'writer', 1, BASE_NOW);
    vi.spyOn(localStorage, 'removeItem').mockImplementation(() => { throw new Error('blocked'); });
    expect(() => clearSessionFromStorage(testSession.recipeId)).not.toThrow();
    expect(loadSessionFromStorage(testSession.recipeId, testSession.expiresAt).kind).toBe('UNAVAILABLE');
  });

  const invalidFields: [string, unknown][] = [
    ['writerId', null], ['revision', -1], ['revision', 0.5], ['savedAt', null],
    ['session.sessionId', null], ['session.recipeId', 'wrong-recipe'],
    ['session.startedAt', null], ['session.lastActiveAt', null], ['session.expiresAt', BASE_NOW + 99999999],
    ['session.currentStepIndex', -1], ['session.currentStepIndex', 999], ['session.currentStepIndex', 0.5],
    ['session.phase', 'UNKNOWN'], ['session.isCompleted', 'false'], ['session.completionPrompt', null],
    ['session.snapshot', null], ['session.snapshot.recipeId', 'wrong-recipe'],
    ['session.snapshot.statusAtLoad', 'DRAFT'], ['session.snapshot.title', null],
    ['session.snapshot.loadedAt', null], ['session.snapshot.baseServings', 'four'],
    ['session.snapshot.ingredients', {}], ['session.snapshot.ingredients.0.name', null],
    ['session.snapshot.ingredients.0.quantity', 'many'], ['session.snapshot.ingredients.0.isOptional', null],
    ['session.snapshot.steps', []], ['session.snapshot.steps.0.instructionText', null],
    ['session.snapshot.steps.0.images.0.url', null], ['session.snapshot.steps.0.timerSeconds', '300'],
    ['session.snapshot.steps.0.ingredientRowIds', [1]],
    ['session.preferences', null], ['session.preferences.theme', 'UNKNOWN'],
    ['session.preferences.fontScale', 0], ['session.checkedIngredientRowIds', [1]],
    ['session.timers', {}], ['session.timers', [null]],
  ];

  it.each(invalidFields)('rejects malformed %s', (path, value) => {
    saveSessionToStorage(testSession, 'writer', 1, BASE_NOW);
    const key = `cooking_session_${testSession.recipeId}`;
    const envelope = JSON.parse(localStorage.getItem(key)!);
    const parts = path.split('.');
    let parent = envelope;
    for (const part of parts.slice(0, -1)) parent = parent[part];
    parent[parts.at(-1)!] = value;
    localStorage.setItem(key, JSON.stringify(envelope));
    expect(loadSessionFromStorage(testSession.recipeId, BASE_NOW).kind).toBe('INVALID');
  });

  it('rejects non-finite JSON timestamps', () => {
    saveSessionToStorage(testSession, 'writer', 1, BASE_NOW);
    const key = `cooking_session_${testSession.recipeId}`;
    localStorage.setItem(key, localStorage.getItem(key)!.replace(`"savedAt":${BASE_NOW}`, '"savedAt":1e400'));
    expect(loadSessionFromStorage(testSession.recipeId, BASE_NOW).kind).toBe('INVALID');
  });

  it.each([
    { status: 'IDLE', durationMs: null },
    { status: 'RUNNING', targetEndTimestamp: null },
    { status: 'PAUSED', remainingMs: -1 },
    { status: 'EXPIRED', expiredAt: BASE_NOW, acknowledgedAt: 'bad' },
    { status: 'DISMISSED', dismissedAt: null },
    { status: 'UNKNOWN' },
    { sessionId: 'wrong-session' },
    { totalDurationSeconds: null },
    { createdAt: null },
    { stepNumber: '1' },
  ])('rejects malformed timer %j', (patch) => {
    const timer = { ...createTimer({ sessionId: testSession.sessionId, label: 'Boil', durationSeconds: 60, stepNumber: 1, now: BASE_NOW }), ...patch };
    localStorage.setItem(`cooking_session_${testSession.recipeId}`, JSON.stringify({
      schemaVersion: 1, writerId: 'writer', revision: 1, savedAt: BASE_NOW,
      session: { ...testSession, timers: [timer] },
    }));
    expect(loadSessionFromStorage(testSession.recipeId, BASE_NOW).kind).toBe('INVALID');
  });

  it('preserves deadlines and reconciles overdue timers without restarting their alarm window', () => {
    testSession.timers = [createTimer({ sessionId: testSession.sessionId, label: 'Boil', durationSeconds: 60, stepNumber: 1, now: BASE_NOW })];
    saveSessionToStorage(testSession, 'writer', 1, BASE_NOW);
    const loaded = loadSessionFromStorage(testSession.recipeId, BASE_NOW + 120000);
    expect(loaded.kind).toBe('RESUMABLE');
    if (loaded.kind === 'RESUMABLE') {
      expect(loaded.value.session.timers[0]).toMatchObject({ status: 'EXPIRED', expiredAt: BASE_NOW + 60000 });
    }
  });

  it('saves session wrapped in StorageEnvelopeV1 and loads it back accurately', () => {
    saveSessionToStorage(testSession, 'tab-writer-1', 1, BASE_NOW);

    const loaded = loadSessionFromStorage(mockPhoBoSnapshot.recipeId, BASE_NOW);
    expect(loaded.kind).toBe('RESUMABLE');
    if (loaded.kind === 'RESUMABLE') {
      expect(loaded.value.schemaVersion).toBe(1);
      expect(loaded.value.writerId).toBe('tab-writer-1');
      expect(loaded.value.revision).toBe(1);
      expect(loaded.value.session.sessionId).toBe('sess-persist-1');
      expect(loaded.value.session.recipeId).toBe(mockPhoBoSnapshot.recipeId);
      expect(loaded.value.session.expiresAt).toBe(BASE_NOW + 4 * 3600 * 1000);
    }
  });

  it('returns NONE when no session is stored for given recipeId', () => {
    const loaded = loadSessionFromStorage('recipe-non-existent', BASE_NOW);
    expect(loaded.kind).toBe('NONE');
    expect(hasResumableSession('recipe-non-existent', BASE_NOW)).toBe(false);
  });

  it('purges session and returns EXPIRED when session exceeds 4-hour TTL (A08)', () => {
    saveSessionToStorage(testSession, 'tab-writer-1', 1, BASE_NOW);

    // 4 hours + 1 ms later
    const expiredNow = BASE_NOW + 4 * 3600 * 1000 + 1;
    const loaded = loadSessionFromStorage(mockPhoBoSnapshot.recipeId, expiredNow);

    expect(loaded.kind).toBe('EXPIRED');
    expect(localStorage.getItem(`cooking_session_${mockPhoBoSnapshot.recipeId}`)).toBeNull();
    expect(hasResumableSession(mockPhoBoSnapshot.recipeId, expiredNow)).toBe(false);
  });

  it('handles corrupted JSON gracefully and returns INVALID', () => {
    localStorage.setItem(`cooking_session_${mockPhoBoSnapshot.recipeId}`, '{ corrupted json ...');

    const loaded = loadSessionFromStorage(mockPhoBoSnapshot.recipeId, BASE_NOW);
    expect(loaded.kind).toBe('INVALID');
    expect(hasResumableSession(mockPhoBoSnapshot.recipeId, BASE_NOW)).toBe(false);
  });

  it('returns UNSUPPORTED_VERSION if schemaVersion is not 1', () => {
    const v2Envelope = {
      schemaVersion: 2,
      writerId: 'tab-future',
      revision: 1,
      savedAt: BASE_NOW,
      session: testSession,
    };
    localStorage.setItem(
      `cooking_session_${mockPhoBoSnapshot.recipeId}`,
      JSON.stringify(v2Envelope),
    );

    const loaded = loadSessionFromStorage(mockPhoBoSnapshot.recipeId, BASE_NOW);
    expect(loaded.kind).toBe('UNSUPPORTED_VERSION');
    expect(hasResumableSession(mockPhoBoSnapshot.recipeId, BASE_NOW)).toBe(false);
  });

  it('isolates sessions between different recipes without cross-over', () => {
    const recipeA = 'recipe-pho-bo';
    const recipeB = 'recipe-bun-cha';

    const sessionA: CookingSession = {
      ...testSession,
      recipeId: recipeA,
      snapshot: { ...testSession.snapshot, recipeId: recipeA },
      sessionId: 'sess-A',
    };
    const sessionB: CookingSession = {
      ...testSession,
      recipeId: recipeB,
      snapshot: { ...testSession.snapshot, recipeId: recipeB },
      sessionId: 'sess-B',
    };

    saveSessionToStorage(sessionA, 'tab-1', 1, BASE_NOW);
    saveSessionToStorage(sessionB, 'tab-1', 1, BASE_NOW);

    const loadedA = loadSessionFromStorage(recipeA, BASE_NOW);
    const loadedB = loadSessionFromStorage(recipeB, BASE_NOW);

    expect(loadedA.kind).toBe('RESUMABLE');
    expect(loadedB.kind).toBe('RESUMABLE');
    if (loadedA.kind === 'RESUMABLE' && loadedB.kind === 'RESUMABLE') {
      expect(loadedA.value.session.sessionId).toBe('sess-A');
      expect(loadedB.value.session.sessionId).toBe('sess-B');
    }

    clearSessionFromStorage(recipeA);
    expect(loadSessionFromStorage(recipeA, BASE_NOW).kind).toBe('NONE');
    expect(loadSessionFromStorage(recipeB, BASE_NOW).kind).toBe('RESUMABLE');
  });
});
