import type {
  CookingSession,
  RecipeSnapshot,
  CookingPreferences,
  SessionPhase,
  IngredientRowId,
  EpochMs,
} from './sessionModel';
import {
  createTimer,
  pauseTimer,
  resumeTimer,
  extendTimer,
  acknowledgeTimer,
  tickTimers,
} from './timerEngine';

export type SessionAction =
  | { type: 'SET_STEP'; stepIndex: number; now?: EpochMs }
  | { type: 'NEXT_STEP'; now?: EpochMs }
  | { type: 'PREV_STEP'; now?: EpochMs }
  | { type: 'TOGGLE_INGREDIENT'; rowId: IngredientRowId; now?: EpochMs }
  | { type: 'SET_THEME'; theme: CookingPreferences['theme']; now?: EpochMs }
  | { type: 'SET_FONT_SCALE'; fontScale: number; now?: EpochMs }
  | {
      type: 'ADD_TIMER';
      label: string;
      durationSeconds: number;
      stepNumber?: number | null;
      now?: EpochMs;
    }
  | { type: 'PAUSE_TIMER'; timerId: string; now?: EpochMs }
  | { type: 'RESUME_TIMER'; timerId: string; now?: EpochMs }
  | { type: 'EXTEND_TIMER'; timerId: string; seconds: number; now?: EpochMs }
  | { type: 'ACKNOWLEDGE_TIMER'; timerId: string; now?: EpochMs }
  | { type: 'DELETE_TIMER'; timerId: string; now?: EpochMs }
  | { type: 'TICK_TIMERS'; now: EpochMs }
  | { type: 'SET_PHASE'; phase: SessionPhase; now?: EpochMs }
  | { type: 'COMPLETE_SESSION'; now?: EpochMs }
  | { type: 'SET_COMPLETION_PROMPT'; promptState: CookingSession['completionPrompt']; now?: EpochMs };

export function createInitialSession(
  snapshot: RecipeSnapshot,
  sessionId?: string,
  now: EpochMs = Date.now(),
): CookingSession {
  const actualSessionId = sessionId || `session_${snapshot.recipeId}_${now}`;

  return {
    sessionId: actualSessionId,
    recipeId: snapshot.recipeId,
    currentStepIndex: 0,
    startedAt: now,
    lastActiveAt: now,
    expiresAt: now + 4 * 3600 * 1000, // Policy A08: fixed 4-hour TTL from startedAt
    phase: 'COOKING',
    isCompleted: false,
    snapshot,
    timers: [],
    checkedIngredientRowIds: [],
    preferences: {
      theme: 'LIGHT',
      fontScale: 1.0,
    },
    completionPrompt: 'NOT_SHOWN',
  };
}

export function sessionReducer(state: CookingSession, action: SessionAction): CookingSession {
  const now = action.now ?? Date.now();
  const totalSteps = state.snapshot.steps.length;

  switch (action.type) {
    case 'SET_STEP': {
      const clamped = Math.max(0, Math.min(totalSteps - 1, action.stepIndex));
      return {
        ...state,
        currentStepIndex: clamped,
        lastActiveAt: now,
      };
    }

    case 'NEXT_STEP': {
      if (state.currentStepIndex >= totalSteps - 1) return state;
      return {
        ...state,
        currentStepIndex: state.currentStepIndex + 1,
        lastActiveAt: now,
      };
    }

    case 'PREV_STEP': {
      if (state.currentStepIndex <= 0) return state;
      return {
        ...state,
        currentStepIndex: state.currentStepIndex - 1,
        lastActiveAt: now,
      };
    }

    case 'TOGGLE_INGREDIENT': {
      const exists = state.checkedIngredientRowIds.includes(action.rowId);
      const updated = exists
        ? state.checkedIngredientRowIds.filter((id) => id !== action.rowId)
        : [...state.checkedIngredientRowIds, action.rowId];

      return {
        ...state,
        checkedIngredientRowIds: updated,
        lastActiveAt: now,
      };
    }

    case 'SET_THEME': {
      return {
        ...state,
        preferences: {
          ...state.preferences,
          theme: action.theme,
        },
        lastActiveAt: now,
      };
    }

    case 'SET_FONT_SCALE': {
      return {
        ...state,
        preferences: {
          ...state.preferences,
          fontScale: action.fontScale,
        },
        lastActiveAt: now,
      };
    }

    case 'ADD_TIMER': {
      const newTimer = createTimer({
        sessionId: state.sessionId,
        label: action.label,
        durationSeconds: action.durationSeconds,
        stepNumber: action.stepNumber,
        now,
      });

      return {
        ...state,
        timers: [...state.timers, newTimer],
        lastActiveAt: now,
      };
    }

    case 'PAUSE_TIMER': {
      return {
        ...state,
        timers: state.timers.map((t) => (t.timerId === action.timerId ? pauseTimer(t, now) : t)),
        lastActiveAt: now,
      };
    }

    case 'RESUME_TIMER': {
      return {
        ...state,
        timers: state.timers.map((t) => (t.timerId === action.timerId ? resumeTimer(t, now) : t)),
        lastActiveAt: now,
      };
    }

    case 'EXTEND_TIMER': {
      return {
        ...state,
        timers: state.timers.map((t) =>
          t.timerId === action.timerId ? extendTimer(t, action.seconds, now) : t,
        ),
        lastActiveAt: now,
      };
    }

    case 'ACKNOWLEDGE_TIMER': {
      return {
        ...state,
        timers: state.timers.map((t) =>
          t.timerId === action.timerId ? acknowledgeTimer(t, now) : t,
        ),
        lastActiveAt: now,
      };
    }

    case 'DELETE_TIMER': {
      return {
        ...state,
        timers: state.timers.filter((t) => t.timerId !== action.timerId),
        lastActiveAt: now,
      };
    }

    case 'TICK_TIMERS': {
      const updatedTimers = tickTimers(state.timers, action.now);
      return {
        ...state,
        timers: updatedTimers,
      };
    }

    case 'SET_PHASE': {
      return {
        ...state,
        phase: action.phase,
        lastActiveAt: now,
      };
    }

    case 'COMPLETE_SESSION': {
      return {
        ...state,
        phase: 'COMPLETED',
        isCompleted: true,
        lastActiveAt: now,
      };
    }

    case 'SET_COMPLETION_PROMPT': {
      return {
        ...state,
        completionPrompt: action.promptState,
        lastActiveAt: now,
      };
    }

    default:
      return state;
  }
}
