import { describe, it, expect } from 'vitest';
import { sessionReducer, createInitialSession } from '../domain/sessionReducer';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';
import type { CookingSession } from '../domain/sessionModel';

describe('sessionReducer (T010)', () => {
  const BASE_NOW = 1731300000000;

  const getInitialState = (): CookingSession => {
    return createInitialSession(mockPhoBoSnapshot, 'sess-123', BASE_NOW);
  };

  it('initializes session with step 0, startedAt, 4-hour fixed TTL (A08)', () => {
    const session = getInitialState();
    expect(session.sessionId).toBe('sess-123');
    expect(session.currentStepIndex).toBe(0);
    expect(session.phase).toBe('COOKING');
    expect(session.isCompleted).toBe(false);
    expect(session.startedAt).toBe(BASE_NOW);
    expect(session.expiresAt).toBe(BASE_NOW + 4 * 3600 * 1000); // exactly 4 hours
    expect(session.checkedIngredientRowIds).toEqual([]);
    expect(session.preferences.theme).toBe('LIGHT');
  });

  it('navigates next and prev steps within bounds', () => {
    let state = getInitialState();

    state = sessionReducer(state, { type: 'NEXT_STEP', now: BASE_NOW + 1000 });
    expect(state.currentStepIndex).toBe(1);

    state = sessionReducer(state, { type: 'NEXT_STEP', now: BASE_NOW + 2000 });
    expect(state.currentStepIndex).toBe(2);

    state = sessionReducer(state, { type: 'PREV_STEP', now: BASE_NOW + 3000 });
    expect(state.currentStepIndex).toBe(1);

    // Jump directly to step
    state = sessionReducer(state, { type: 'SET_STEP', stepIndex: 4, now: BASE_NOW + 4000 });
    expect(state.currentStepIndex).toBe(4);

    // Clamp beyond max steps (total 5 steps -> max index 4)
    state = sessionReducer(state, { type: 'SET_STEP', stepIndex: 10, now: BASE_NOW + 5000 });
    expect(state.currentStepIndex).toBe(4);

    // Clamp below 0
    state = sessionReducer(state, { type: 'SET_STEP', stepIndex: -5, now: BASE_NOW + 6000 });
    expect(state.currentStepIndex).toBe(0);
  });

  it('toggles ingredient checked state', () => {
    let state = getInitialState();

    state = sessionReducer(state, { type: 'TOGGLE_INGREDIENT', rowId: 'ing-1', now: BASE_NOW });
    expect(state.checkedIngredientRowIds).toEqual(['ing-1']);

    state = sessionReducer(state, { type: 'TOGGLE_INGREDIENT', rowId: 'ing-2', now: BASE_NOW });
    expect(state.checkedIngredientRowIds).toEqual(['ing-1', 'ing-2']);

    // Toggle off ing-1
    state = sessionReducer(state, { type: 'TOGGLE_INGREDIENT', rowId: 'ing-1', now: BASE_NOW });
    expect(state.checkedIngredientRowIds).toEqual(['ing-2']);
  });

  it('toggles theme and font scale preferences', () => {
    let state = getInitialState();

    state = sessionReducer(state, { type: 'SET_THEME', theme: 'HIGH_CONTRAST_DARK', now: BASE_NOW });
    expect(state.preferences.theme).toBe('HIGH_CONTRAST_DARK');

    state = sessionReducer(state, { type: 'SET_FONT_SCALE', fontScale: 1.25, now: BASE_NOW });
    expect(state.preferences.fontScale).toBe(1.25);
  });

  it('handles timer mutations (add, pause, resume, extend, delete, tick)', () => {
    let state = getInitialState();

    state = sessionReducer(state, {
      type: 'ADD_TIMER',
      label: 'Luộc xương',
      durationSeconds: 300,
      stepNumber: 1,
      now: BASE_NOW,
    });

    expect(state.timers).toHaveLength(1);
    const timerId = state.timers[0].timerId;
    expect(state.timers[0].status).toBe('RUNNING');

    // Pause timer
    state = sessionReducer(state, { type: 'PAUSE_TIMER', timerId, now: BASE_NOW + 100 * 1000 });
    expect(state.timers[0].status).toBe('PAUSED');

    // Resume timer
    state = sessionReducer(state, { type: 'RESUME_TIMER', timerId, now: BASE_NOW + 150 * 1000 });
    expect(state.timers[0].status).toBe('RUNNING');

    // Extend timer (+60s)
    state = sessionReducer(state, { type: 'EXTEND_TIMER', timerId, seconds: 60, now: BASE_NOW + 160 * 1000 });
    expect(state.timers[0].totalDurationSeconds).toBe(360);

    // Delete timer
    state = sessionReducer(state, { type: 'DELETE_TIMER', timerId, now: BASE_NOW + 170 * 1000 });
    expect(state.timers).toHaveLength(0);
  });

  it('transitions session phase to COMPLETED and marks isCompleted', () => {
    let state = getInitialState();
    expect(state.isCompleted).toBe(false);

    state = sessionReducer(state, { type: 'COMPLETE_SESSION', now: BASE_NOW + 5000 });
    expect(state.phase).toBe('COMPLETED');
    expect(state.isCompleted).toBe(true);
  });
});
