import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AlarmCoordinator } from '../runtime/alarmCoordinator';
import { getAudioSynth, vibrationAdapter } from '../adapters/browserCapabilities';
import type { KitchenTimer } from '../domain/sessionModel';

vi.mock('../adapters/browserCapabilities', () => ({
  getAudioSynth: () => synth,
  vibrationAdapter: { vibrate: vi.fn(), cancel: vi.fn() },
}));
const synth = { playAlarmPulse: vi.fn(), stopAlarm: vi.fn() };
const expired = (id: string, expiredAt = Date.now()): KitchenTimer => ({
  timerId: id, sessionId: 'session', stepNumber: null, label: id,
  totalDurationSeconds: 1, createdAt: expiredAt - 1000,
  status: 'EXPIRED', expiredAt, acknowledgedAt: null,
});

describe('AlarmCoordinator', () => {
  let coordinator: AlarmCoordinator;
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(1731300000000);
    vi.clearAllMocks();
    Object.defineProperty(document, 'visibilityState', { configurable: true, value: 'visible' });
    coordinator = new AlarmCoordinator();
  });
  afterEach(() => {
    coordinator.cleanup();
    vi.useRealTimers();
  });

  it('coordinates simultaneous expirations into one loop and stops at the deadline window', () => {
    const timers = [expired('one'), expired('two')];
    expect(coordinator.evaluateAlarms(timers).unacknowledgedExpiredCount).toBe(2);
    coordinator.evaluateAlarms(timers);
    expect(getAudioSynth().playAlarmPulse).toHaveBeenCalledTimes(1);
    vi.advanceTimersByTime(61000);
    expect(getAudioSynth().playAlarmPulse).toHaveBeenCalledTimes(60);
    expect(getAudioSynth().stopAlarm).toHaveBeenCalled();
  });

  it('never sounds or vibrates while hidden and resumes only the remaining window', () => {
    const timers = [expired('one')];
    Object.defineProperty(document, 'visibilityState', { value: 'hidden' });
    expect(coordinator.evaluateAlarms(timers).shouldSound).toBe(false);
    vi.advanceTimersByTime(30000);
    expect(synth.playAlarmPulse).not.toHaveBeenCalled();
    expect(vibrationAdapter.vibrate).not.toHaveBeenCalled();
    Object.defineProperty(document, 'visibilityState', { value: 'visible' });
    document.dispatchEvent(new Event('visibilitychange'));
    expect(synth.playAlarmPulse).toHaveBeenCalledTimes(1);
    vi.advanceTimersByTime(31000);
    expect(synth.playAlarmPulse).toHaveBeenCalledTimes(30);
  });

  it('stops immediately on hiding and does not restart an overdue alarm', () => {
    coordinator.evaluateAlarms([expired('one')]);
    Object.defineProperty(document, 'visibilityState', { value: 'hidden' });
    document.dispatchEvent(new Event('visibilitychange'));
    expect(synth.stopAlarm).toHaveBeenCalled();
    vi.advanceTimersByTime(61000);
    Object.defineProperty(document, 'visibilityState', { value: 'visible' });
    document.dispatchEvent(new Event('visibilitychange'));
    expect(synth.playAlarmPulse).toHaveBeenCalledTimes(1);
  });
});
