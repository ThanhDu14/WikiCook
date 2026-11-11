import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { CookingRuntimeManager } from '../runtime/cookingRuntime';
import { sharedAlarmCoordinator } from '../runtime/alarmCoordinator';
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

  const foreignWrite = (recipeId: string) => {
    window.dispatchEvent(new StorageEvent('storage', {
      key: `cooking_session_${recipeId}`,
      newValue: JSON.stringify({ writerId: 'foreign-tab' }),
    }));
  };

  it('ignores foreign writes for recipes this runtime does not own', () => {
    const conflict = vi.fn();
    runtime.setCallbacks({ onTabConflict: conflict });
    runtime.startSession(mockPhoBoSnapshot, BASE_NOW);
    foreignWrite('another-recipe');
    expect(conflict).not.toHaveBeenCalled();
    runtime.nextStep(BASE_NOW);
    expect(runtime.getActiveSession()?.currentStepIndex).toBe(1);
  });

  it('freezes only the losing recipe, including public persistence and restart paths', () => {
    const conflict = vi.fn();
    runtime.setCallbacks({ onTabConflict: conflict });
    const original = runtime.startSession(mockPhoBoSnapshot, BASE_NOW);
    runtime.addTimer('timer', 1, null, BASE_NOW);
    const frozen = runtime.getActiveSession();
    const stored = localStorage.getItem(`cooking_session_${original.recipeId}`);
    foreignWrite(original.recipeId);
    foreignWrite(original.recipeId);
    runtime.nextStep(BASE_NOW);
    runtime.tick(BASE_NOW + 2000);
    runtime.saveSession(original, BASE_NOW);
    runtime.startSession(mockPhoBoSnapshot, BASE_NOW);
    runtime.resumeSession(JSON.parse(stored!), BASE_NOW + 2000);
    expect(runtime.completeSession(undefined, BASE_NOW)).toBe(false);
    runtime.exitSession(false, BASE_NOW);
    expect(runtime.getActiveSession()).toBe(frozen);
    expect(localStorage.getItem(`cooking_session_${original.recipeId}`)).toBe(stored);
    expect(conflict).toHaveBeenCalledTimes(1);
    expect(runtime.getSessions()).toEqual([]);

    runtime.startSession({ ...mockPhoBoSnapshot, recipeId: 'second' }, BASE_NOW);
    runtime.nextStep(BASE_NOW);
    expect(runtime.getActiveSession()?.currentStepIndex).toBe(1);
    expect(runtime.getSessions().map((s) => s.recipeId)).toEqual(['second']);
  });

  it('removes losing recipe alarms from the coordinated batch while retaining other recipes', () => {
    const evaluate = vi.spyOn(sharedAlarmCoordinator, 'evaluateAlarms');
    const first = runtime.startSession(mockPhoBoSnapshot, BASE_NOW);
    runtime.addTimer('first', 1, null, BASE_NOW);
    runtime.startSession({ ...mockPhoBoSnapshot, recipeId: 'second' }, BASE_NOW);
    runtime.addTimer('second', 1, null, BASE_NOW);
    foreignWrite(first.recipeId);
    expect(evaluate).toHaveBeenLastCalledWith(runtime.getSessions().flatMap((s) => s.timers));
    expect(evaluate.mock.calls.at(-1)?.[0].map((t) => t.label)).toEqual(['second']);
    evaluate.mockRestore();
    sharedAlarmCoordinator.cleanup();
  });

  it('selects existing recipes and deactivates without dropping background sessions', () => {
    const first = runtime.startSession(mockPhoBoSnapshot, BASE_NOW);
    runtime.startSession({ ...mockPhoBoSnapshot, recipeId: 'second' }, BASE_NOW);
    expect(runtime.selectSession(first.recipeId)).toBe(true);
    expect(runtime.getActiveSession()?.recipeId).toBe(first.recipeId);
    expect(runtime.selectSession('missing')).toBe(false);
    runtime.deactivateSession();
    expect(runtime.getActiveSession()).toBeNull();
    expect(runtime.getSessions()).toHaveLength(2);
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
        lastActiveAt: BASE_NOW - 5 * 3600 * 1000,
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
