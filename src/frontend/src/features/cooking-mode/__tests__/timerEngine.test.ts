import { describe, it, expect } from 'vitest';
import {
  createTimer,
  pauseTimer,
  resumeTimer,
  extendTimer,
  tickTimers,
  acknowledgeTimer,
  dismissTimer,
  calculateRemainingSeconds,
  formatTimerDisplay,
  shouldPlayAlarmAudio,
} from '../domain/timerEngine';
import type { KitchenTimer } from '../domain/sessionModel';

describe('timerEngine (T005)', () => {
  const SESSION_ID = 'test-session-1';
  const BASE_NOW = 1731300000000; // Fixed epoch ms

  it('creates a running timer with correct targetEndTimestamp based on Date.now()', () => {
    const timer = createTimer({
      sessionId: SESSION_ID,
      label: 'Luộc trứng',
      durationSeconds: 300,
      stepNumber: 1,
      now: BASE_NOW,
    });

    expect(timer.status).toBe('RUNNING');
    expect(timer.label).toBe('Luộc trứng');
    expect(timer.totalDurationSeconds).toBe(300);
    expect(timer.stepNumber).toBe(1);
    if (timer.status === 'RUNNING') {
      expect(timer.targetEndTimestamp).toBe(BASE_NOW + 300 * 1000);
    }
  });

  it('calculates remaining seconds with ceiling so 299.1s shows as 300s', () => {
    const timer: KitchenTimer = {
      timerId: 't1',
      sessionId: SESSION_ID,
      label: 'Timer 1',
      totalDurationSeconds: 300,
      stepNumber: 1,
      createdAt: BASE_NOW,
      status: 'RUNNING',
      targetEndTimestamp: BASE_NOW + 300 * 1000,
    };

    // 100ms elapsed -> 299.9s remaining -> ceiling is 300
    expect(calculateRemainingSeconds(timer, BASE_NOW + 100)).toBe(300);

    // 1000ms elapsed -> 299s remaining
    expect(calculateRemainingSeconds(timer, BASE_NOW + 1000)).toBe(299);

    // Exact deadline -> 0
    expect(calculateRemainingSeconds(timer, BASE_NOW + 300 * 1000)).toBe(0);

    // Past deadline -> 0
    expect(calculateRemainingSeconds(timer, BASE_NOW + 305 * 1000)).toBe(0);
  });

  it('formats timer display as MM:SS and HH:MM:SS properly', () => {
    expect(formatTimerDisplay(0)).toBe('00:00');
    expect(formatTimerDisplay(45)).toBe('00:45');
    expect(formatTimerDisplay(300)).toBe('05:00');
    expect(formatTimerDisplay(3600)).toBe('01:00:00');
    expect(formatTimerDisplay(5400)).toBe('01:30:00');
  });

  it('pauses a running timer and preserves exact remaining ms', () => {
    const runningTimer = createTimer({
      sessionId: SESSION_ID,
      label: 'Ninh nước dùng',
      durationSeconds: 600,
      now: BASE_NOW,
    });

    // Advance 150 seconds (150,000 ms)
    const pauseNow = BASE_NOW + 150 * 1000;
    const paused = pauseTimer(runningTimer, pauseNow);

    expect(paused.status).toBe('PAUSED');
    if (paused.status === 'PAUSED') {
      expect(paused.remainingMs).toBe(450 * 1000);
    }
    expect(calculateRemainingSeconds(paused, pauseNow + 50000)).toBe(450);
  });

  it('resumes a paused timer by generating a new targetEndTimestamp anchored to resume now', () => {
    const pausedTimer: KitchenTimer = {
      timerId: 't2',
      sessionId: SESSION_ID,
      label: 'Hầm xương',
      totalDurationSeconds: 600,
      stepNumber: null,
      createdAt: BASE_NOW,
      status: 'PAUSED',
      remainingMs: 450 * 1000,
    };

    const resumeNow = BASE_NOW + 1000 * 1000; // Resumed much later
    const resumed = resumeTimer(pausedTimer, resumeNow);

    expect(resumed.status).toBe('RUNNING');
    if (resumed.status === 'RUNNING') {
      expect(resumed.targetEndTimestamp).toBe(resumeNow + 450 * 1000);
    }
  });

  it('extends a RUNNING timer (+60s or +300s) per A04 policy', () => {
    const timer = createTimer({
      sessionId: SESSION_ID,
      label: 'Xào rau',
      durationSeconds: 120,
      now: BASE_NOW,
    });

    const extended = extendTimer(timer, 60);
    expect(extended.totalDurationSeconds).toBe(180);
    if (extended.status === 'RUNNING') {
      expect(extended.targetEndTimestamp).toBe(BASE_NOW + 180 * 1000);
    }
  });

  it('rejects extension (+1m/+5m) on PAUSED or EXPIRED timer per A04 policy', () => {
    const pausedTimer: KitchenTimer = {
      timerId: 't3',
      sessionId: SESSION_ID,
      label: 'Timer',
      totalDurationSeconds: 100,
      stepNumber: null,
      createdAt: BASE_NOW,
      status: 'PAUSED',
      remainingMs: 50 * 1000,
    };

    expect(() => extendTimer(pausedTimer, 60)).toThrow(/RUNNING/);

    const expiredTimer: KitchenTimer = {
      timerId: 't4',
      sessionId: SESSION_ID,
      label: 'Timer',
      totalDurationSeconds: 100,
      stepNumber: null,
      createdAt: BASE_NOW,
      status: 'EXPIRED',
      expiredAt: BASE_NOW - 1000,
      acknowledgedAt: null,
    };

    expect(() => extendTimer(expiredTimer, 60)).toThrow(/RUNNING/);
  });

  it('ticks timers: transitions expired timers to EXPIRED state without drift when tab was hidden', () => {
    const timer1 = createTimer({
      sessionId: SESSION_ID,
      label: 'Timer 1',
      durationSeconds: 10,
      now: BASE_NOW,
    });
    const timer2 = createTimer({
      sessionId: SESSION_ID,
      label: 'Timer 2',
      durationSeconds: 60,
      now: BASE_NOW,
    });

    // Simulate tab being suspended and waking up 20 seconds later
    const wakeUpNow = BASE_NOW + 20 * 1000;
    const updated = tickTimers([timer1, timer2], wakeUpNow);

    expect(updated[0].status).toBe('EXPIRED');
    if (updated[0].status === 'EXPIRED') {
      expect(updated[0].expiredAt).toBe(BASE_NOW + 10 * 1000);
      expect(updated[0].acknowledgedAt).toBeNull();
    }

    expect(updated[1].status).toBe('RUNNING');
    expect(calculateRemainingSeconds(updated[1], wakeUpNow)).toBe(40);
  });

  it('enforces 60-second audio window policy (A05)', () => {
    const expiredTimer: KitchenTimer = {
      timerId: 't-exp',
      sessionId: SESSION_ID,
      label: 'Baking',
      totalDurationSeconds: 60,
      stepNumber: null,
      createdAt: BASE_NOW,
      status: 'EXPIRED',
      expiredAt: BASE_NOW,
      acknowledgedAt: null,
    };

    // At deadline (now = expiredAt) -> alarm should play
    expect(shouldPlayAlarmAudio(expiredTimer, BASE_NOW)).toBe(true);

    // 30 seconds after deadline -> still within 60s window
    expect(shouldPlayAlarmAudio(expiredTimer, BASE_NOW + 30 * 1000)).toBe(true);

    // 61 seconds after deadline -> outside window, no audio
    expect(shouldPlayAlarmAudio(expiredTimer, BASE_NOW + 61 * 1000)).toBe(false);

    // If acknowledged -> no audio even within 60s
    const acked = acknowledgeTimer(expiredTimer, BASE_NOW + 10 * 1000);
    expect(shouldPlayAlarmAudio(acked, BASE_NOW + 15 * 1000)).toBe(false);
  });

  it('dismisses timer properly', () => {
    const expiredTimer: KitchenTimer = {
      timerId: 't-exp',
      sessionId: SESSION_ID,
      label: 'Baking',
      totalDurationSeconds: 60,
      stepNumber: null,
      createdAt: BASE_NOW,
      status: 'EXPIRED',
      expiredAt: BASE_NOW,
      acknowledgedAt: null,
    };

    const dismissed = dismissTimer(expiredTimer, BASE_NOW + 1000);
    expect(dismissed.status).toBe('DISMISSED');
  });
});
