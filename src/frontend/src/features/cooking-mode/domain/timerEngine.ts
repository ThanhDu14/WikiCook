import type { KitchenTimer, EpochMs } from './sessionModel';

export interface CreateTimerOptions {
  sessionId: string;
  label: string;
  durationSeconds: number;
  stepNumber?: number | null;
  now?: EpochMs;
}

export function createTimer(options: CreateTimerOptions): KitchenTimer {
  const now = options.now ?? Date.now();
  const id = `timer_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;

  return {
    timerId: id,
    sessionId: options.sessionId,
    label: options.label,
    totalDurationSeconds: options.durationSeconds,
    stepNumber: options.stepNumber ?? null,
    createdAt: now,
    status: 'RUNNING',
    targetEndTimestamp: now + options.durationSeconds * 1000,
  };
}

export function pauseTimer(timer: KitchenTimer, now: EpochMs = Date.now()): KitchenTimer {
  if (timer.status !== 'RUNNING') return timer;
  const remainingMs = Math.max(0, timer.targetEndTimestamp - now);
  return {
    ...timer,
    status: 'PAUSED',
    remainingMs,
  };
}

export function resumeTimer(timer: KitchenTimer, now: EpochMs = Date.now()): KitchenTimer {
  if (timer.status !== 'PAUSED') return timer;
  return {
    ...timer,
    status: 'RUNNING',
    targetEndTimestamp: now + timer.remainingMs,
  };
}

export function extendTimer(
  timer: KitchenTimer,
  extendSeconds: number,
  _now: EpochMs = Date.now(),
): KitchenTimer {
  if (timer.status !== 'RUNNING') {
    throw new Error(`Extension is only permitted for RUNNING timers (current: ${timer.status})`);
  }
  const addedMs = extendSeconds * 1000;
  return {
    ...timer,
    totalDurationSeconds: timer.totalDurationSeconds + extendSeconds,
    targetEndTimestamp: timer.targetEndTimestamp + addedMs,
  };
}

export function acknowledgeTimer(timer: KitchenTimer, now: EpochMs = Date.now()): KitchenTimer {
  if (timer.status !== 'EXPIRED') return timer;
  return {
    ...timer,
    acknowledgedAt: now,
  };
}

export function dismissTimer(timer: KitchenTimer, now: EpochMs = Date.now()): KitchenTimer {
  return {
    ...timer,
    status: 'DISMISSED',
    dismissedAt: now,
  };
}

export function tickTimers(timers: KitchenTimer[], now: EpochMs = Date.now()): KitchenTimer[] {
  return timers.map((timer) => {
    if (timer.status === 'RUNNING' && timer.targetEndTimestamp <= now) {
      return {
        ...timer,
        status: 'EXPIRED',
        expiredAt: timer.targetEndTimestamp,
        acknowledgedAt: null,
      };
    }
    return timer;
  });
}

export function calculateRemainingSeconds(timer: KitchenTimer, now: EpochMs = Date.now()): number {
  switch (timer.status) {
    case 'RUNNING':
      return Math.max(0, Math.ceil((timer.targetEndTimestamp - now) / 1000));
    case 'PAUSED':
      return Math.max(0, Math.ceil(timer.remainingMs / 1000));
    case 'IDLE':
      return Math.max(0, Math.ceil(timer.durationMs / 1000));
    case 'EXPIRED':
    case 'DISMISSED':
    default:
      return 0;
  }
}

export function formatTimerDisplay(seconds: number): string {
  const safeSeconds = Math.max(0, Math.floor(seconds));
  const h = Math.floor(safeSeconds / 3600);
  const m = Math.floor((safeSeconds % 3600) / 60);
  const s = safeSeconds % 60;

  const mm = String(m).padStart(2, '0');
  const ss = String(s).padStart(2, '0');

  if (h > 0) {
    const hh = String(h).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
  }
  return `${mm}:${ss}`;
}

export function shouldPlayAlarmAudio(timer: KitchenTimer, now: EpochMs = Date.now()): boolean {
  if (timer.status !== 'EXPIRED') return false;
  if (timer.acknowledgedAt !== null) return false;
  // A05 policy: 60-second window from expiredAt
  return now - timer.expiredAt <= 60_000;
}
