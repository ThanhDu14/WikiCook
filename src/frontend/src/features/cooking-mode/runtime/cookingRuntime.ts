import type {
  CookingSession,
  RecipeSnapshot,
  StorageEnvelopeV1,
  StorageLoadResult,
  RecipeId,
  EpochMs,
  CookingPreferences,
} from '../domain/sessionModel';
import { createInitialSession, sessionReducer } from '../domain/sessionReducer';
import { shouldPlayAlarmAudio, tickTimers } from '../domain/timerEngine';
import { getAudioSynth, vibrationAdapter } from '../adapters/browserCapabilities';
import { browserClock } from '../adapters/browserClock';
import {
  loadSessionFromStorage,
  saveSessionToStorage,
  clearSessionFromStorage,
} from '../adapters/sessionPersistence';

export type CookingRuntimeListener = (session: CookingSession | null) => void;

export class CookingRuntimeManager {
  private sessions: Map<RecipeId, CookingSession> = new Map();
  private activeRecipeId: RecipeId | null = null;
  private writerId: string;
  private revision: number = 0;
  private tickIntervalId: number | null = null;
  private listeners: Set<CookingRuntimeListener> = new Set();
  private onStorageQuotaExceeded?: () => void;
  private onTabConflict?: () => void;
  private onClockJump?: () => void;

  constructor() {
    this.writerId = `writer_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;
    this.initEventListeners();
    this.startTicker();
  }

  private initEventListeners(): void {
    if (typeof window === 'undefined') return;

    window.addEventListener('storage', this.handleStorageEvent);
    document.addEventListener('visibilitychange', this.handleVisibilityChange);
  }

  private handleStorageEvent = (e: StorageEvent): void => {
    if (!e.key || !e.key.startsWith('cooking_session_')) return;
    const recipeId = e.key.replace('cooking_session_', '');

    if (!e.newValue) {
      // Key was removed by another tab
      if (this.activeRecipeId === recipeId) {
        this.sessions.delete(recipeId);
        this.notifyListeners();
      }
      return;
    }

    try {
      const envelope: StorageEnvelopeV1 = JSON.parse(e.newValue);
      if (envelope.writerId !== this.writerId) {
        // Competing writer detected per A10 policy
        if (this.onTabConflict) {
          this.onTabConflict();
        }
      }
    } catch {
      // Ignore parse failure
    }
  };

  private handleVisibilityChange = (): void => {
    if (typeof document !== 'undefined' && document.visibilityState === 'visible') {
      this.tick();
    }
  };

  private startTicker(): void {
    if (typeof window === 'undefined') return;
    this.stopTicker();

    this.tickIntervalId = window.setInterval(() => {
      this.tick();
    }, 250);
  }

  private stopTicker(): void {
    if (this.tickIntervalId !== null) {
      clearInterval(this.tickIntervalId);
      this.tickIntervalId = null;
    }
  }

  public tick(now: EpochMs = Date.now()): void {
    if (browserClock.detectDiscrepancy(now)) {
      if (this.onClockJump) {
        this.onClockJump();
      }
    }
    browserClock.recordSample(now);

    let changed = false;

    for (const [recipeId, session] of this.sessions.entries()) {
      if (session.timers.length === 0) continue;

      const previousTimers = session.timers;
      const updatedSession = sessionReducer(session, { type: 'TICK_TIMERS', now });

      if (updatedSession !== session) {
        this.sessions.set(recipeId, updatedSession);
        changed = true;

        // Check if any timer became newly expired to sound alarm & vibrate
        for (const t of updatedSession.timers) {
          const prev = previousTimers.find((p) => p.timerId === t.timerId);
          if (t.status === 'EXPIRED' && prev?.status === 'RUNNING') {
            if (shouldPlayAlarmAudio(t, now)) {
              getAudioSynth().playAlarmPulse();
              vibrationAdapter.vibrate([300, 150, 300]);
            }
          }
        }
      }
    }

    if (changed) {
      this.notifyListeners();
    }
  }

  public subscribe(listener: CookingRuntimeListener): () => void {
    this.listeners.add(listener);
    listener(this.getActiveSession());
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notifyListeners(): void {
    const active = this.getActiveSession();
    this.listeners.forEach((cb) => cb(active));
  }

  public getActiveSession(): CookingSession | null {
    if (!this.activeRecipeId) return null;
    return this.sessions.get(this.activeRecipeId) ?? null;
  }

  public checkResumableSession(recipeId: RecipeId, now: EpochMs = Date.now()): StorageLoadResult {
    return loadSessionFromStorage(recipeId, now);
  }

  public startSession(
    snapshot: RecipeSnapshot,
    now: EpochMs = Date.now(),
    existingSessionId?: string,
  ): CookingSession {
    const session = createInitialSession(snapshot, existingSessionId, now);
    this.sessions.set(snapshot.recipeId, session);
    this.activeRecipeId = snapshot.recipeId;
    this.saveSession(session, now);
    this.notifyListeners();
    return session;
  }

  public resumeSession(envelope: StorageEnvelopeV1, now: EpochMs = Date.now()): CookingSession {
    const reconciledTimers = Array.isArray(envelope.session.timers)
      ? tickTimers(envelope.session.timers, now)
      : [];

    const session: CookingSession = {
      ...envelope.session,
      timers: reconciledTimers,
      phase: 'COOKING',
      lastActiveAt: now,
    };

    this.sessions.set(session.recipeId, session);
    this.activeRecipeId = session.recipeId;
    this.saveSession(session, now);
    this.notifyListeners();
    return session;
  }

  public saveSession(session: CookingSession, now: EpochMs = Date.now()): void {
    if (typeof localStorage === 'undefined') return;

    this.revision += 1;

    try {
      saveSessionToStorage(session, this.writerId, this.revision, now);
    } catch {
      // Storage quota failure or security error per A09
      if (this.onStorageQuotaExceeded) {
        this.onStorageQuotaExceeded();
      }
    }
  }

  public exitSession(keepTimers: boolean, now: EpochMs = Date.now()): void {
    const session = this.getActiveSession();
    if (!session) return;

    if (keepTimers) {
      const updated = sessionReducer(session, { type: 'SET_PHASE', phase: 'BACKGROUND', now });
      this.sessions.set(session.recipeId, updated);
      this.saveSession(updated, now);
      this.notifyListeners();
    } else {
      this.sessions.delete(session.recipeId);
      this.activeRecipeId = null;
      clearSessionFromStorage(session.recipeId);
      this.notifyListeners();
    }
  }

  public completeSession(onCompleted?: (recipeId: string) => void, now: EpochMs = Date.now()): boolean {
    const session = this.getActiveSession();
    if (!session) return false;

    // A11: Emit completion at most once
    if (session.isCompleted || session.phase === 'COMPLETED') {
      return false;
    }

    // Check unresolved timers per A11 policy
    const running = session.timers.filter((t) => t.status === 'RUNNING').length;
    const paused = session.timers.filter((t) => t.status === 'PAUSED').length;
    const unacknowledgedExpired = session.timers.filter(
      (t) => t.status === 'EXPIRED' && t.acknowledgedAt === null,
    ).length;

    if (running > 0 || paused > 0 || unacknowledgedExpired > 0) {
      return false; // Timers must be resolved first
    }

    const completed = sessionReducer(session, { type: 'COMPLETE_SESSION', now });
    this.sessions.set(session.recipeId, completed);
    this.saveSession(completed, now);
    this.notifyListeners();

    if (onCompleted) {
      onCompleted(session.recipeId);
    }
    return true;
  }

  // Timer actions
  public addTimer(label: string, durationSeconds: number, stepNumber?: number | null, now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'ADD_TIMER', label, durationSeconds, stepNumber, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public pauseTimer(timerId: string, now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'PAUSE_TIMER', timerId, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public resumeTimer(timerId: string, now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'RESUME_TIMER', timerId, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public extendTimer(timerId: string, seconds: number, now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'EXTEND_TIMER', timerId, seconds, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public acknowledgeTimer(timerId: string, now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'ACKNOWLEDGE_TIMER', timerId, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public deleteTimer(timerId: string, now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'DELETE_TIMER', timerId, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  // Step navigation actions
  public setStep(stepIndex: number, now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'SET_STEP', stepIndex, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public nextStep(now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'NEXT_STEP', now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public prevStep(now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'PREV_STEP', now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public toggleIngredient(rowId: string, now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'TOGGLE_INGREDIENT', rowId, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public setTheme(theme: CookingPreferences['theme'], now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'SET_THEME', theme, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public setFontScale(fontScale: number, now?: EpochMs): void {
    const session = this.getActiveSession();
    if (!session) return;

    const updated = sessionReducer(session, { type: 'SET_FONT_SCALE', fontScale, now });
    this.sessions.set(session.recipeId, updated);
    this.saveSession(updated, now);
    this.notifyListeners();
  }

  public setCallbacks(callbacks: {
    onStorageQuotaExceeded?: () => void;
    onTabConflict?: () => void;
    onClockJump?: () => void;
  }): void {
    this.onStorageQuotaExceeded = callbacks.onStorageQuotaExceeded;
    this.onTabConflict = callbacks.onTabConflict;
    this.onClockJump = callbacks.onClockJump;
  }

  public cleanup(): void {
    this.stopTicker();
    if (typeof window !== 'undefined') {
      window.removeEventListener('storage', this.handleStorageEvent);
      document.removeEventListener('visibilitychange', this.handleVisibilityChange);
    }
    this.listeners.clear();
    this.sessions.clear();
    this.activeRecipeId = null;
  }
}

export const sharedCookingRuntime = new CookingRuntimeManager();
