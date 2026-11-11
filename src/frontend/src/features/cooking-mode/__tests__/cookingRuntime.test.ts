import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { CookingRuntimeManager } from '../runtime/cookingRuntime';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';

describe('cookingRuntime (T017)', () => {
  let runtime: CookingRuntimeManager;
  const BASE_NOW = 1731300000000;

  beforeEach(() => {
    localStorage.clear();
    runtime = new CookingRuntimeManager();
  });

  afterEach(() => {
    runtime.cleanup();
    localStorage.clear();
  });

  it('starts a new cooking session and saves to localStorage envelope v1', () => {
    const session = runtime.startSession(mockPhoBoSnapshot, BASE_NOW);

    expect(session.recipeId).toBe(mockPhoBoSnapshot.recipeId);
    expect(session.phase).toBe('COOKING');
    expect(session.currentStepIndex).toBe(0);

    const storedRaw = localStorage.getItem(`cooking_session_${mockPhoBoSnapshot.recipeId}`);
    expect(storedRaw).not.toBeNull();
    const envelope = JSON.parse(storedRaw!);
    expect(envelope.schemaVersion).toBe(1);
    expect(envelope.session.recipeId).toBe(mockPhoBoSnapshot.recipeId);
    expect(envelope.session.expiresAt).toBe(BASE_NOW + 4 * 3600 * 1000);
  });

  it('purges expired sessions on start/load per US5-AC5 & A08', () => {
    const expiredEnvelope = {
      schemaVersion: 1,
      writerId: 'tab-1',
      revision: 1,
      savedAt: BASE_NOW - 5 * 3600 * 1000,
      session: {
        sessionId: 'old-sess',
        recipeId: mockPhoBoSnapshot.recipeId,
        currentStepIndex: 2,
        startedAt: BASE_NOW - 5 * 3600 * 1000,
        expiresAt: BASE_NOW - 1 * 3600 * 1000, // expired 1h ago
        phase: 'COOKING',
        isCompleted: false,
        snapshot: mockPhoBoSnapshot,
        timers: [],
        checkedIngredientRowIds: [],
        preferences: { theme: 'LIGHT', fontScale: 1.0 },
        completionPrompt: 'NOT_SHOWN',
      },
    };

    localStorage.setItem(`cooking_session_${mockPhoBoSnapshot.recipeId}`, JSON.stringify(expiredEnvelope));

    const check = runtime.checkResumableSession(mockPhoBoSnapshot.recipeId, BASE_NOW);
    expect(check.kind).toBe('EXPIRED');
    expect(localStorage.getItem(`cooking_session_${mockPhoBoSnapshot.recipeId}`)).toBeNull();
  });

  it('supports background mode when user keeps timers on exit (A11)', () => {
    runtime.startSession(mockPhoBoSnapshot, BASE_NOW);
    runtime.addTimer('Hầm', 600, 1, BASE_NOW);

    // User chooses to keep timers on exit
    runtime.exitSession(true, BASE_NOW);

    const active = runtime.getActiveSession();
    expect(active?.phase).toBe('BACKGROUND');
    expect(active?.timers).toHaveLength(1);
  });

  it('discards session completely when user cancels and exits (A11)', () => {
    runtime.startSession(mockPhoBoSnapshot, BASE_NOW);
    runtime.addTimer('Hầm', 600, 1, BASE_NOW);

    // User chooses to discard all
    runtime.exitSession(false, BASE_NOW);

    expect(runtime.getActiveSession()).toBeNull();
    expect(localStorage.getItem(`cooking_session_${mockPhoBoSnapshot.recipeId}`)).toBeNull();
  });
});
