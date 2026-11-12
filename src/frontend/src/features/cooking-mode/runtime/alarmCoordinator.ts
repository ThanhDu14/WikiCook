import type { KitchenTimer, EpochMs } from '../domain/sessionModel';
import { shouldPlayAlarmAudio } from '../domain/timerEngine';
import { getAudioSynth, vibrationAdapter } from '../adapters/browserCapabilities';

export class AlarmCoordinator {
  private isAlarmActive: boolean = false;
  private intervalId: number | null = null;
  private timers: KitchenTimer[] = [];
  private evaluatedAt: EpochMs = 0;
  private sampledAt: EpochMs = 0;
  private listening = false;

  private listen(): void {
    if (!this.listening && typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', this.handleVisibilityChange);
      this.listening = true;
    }
  }

  private handleVisibilityChange = (): void => {
    this.updateAlarm(this.evaluatedAt + Date.now() - this.sampledAt);
  };

  public evaluateAlarms(timers: KitchenTimer[], now: EpochMs = Date.now()): {
    shouldSound: boolean;
    unacknowledgedExpiredCount: number;
  } {
    this.listen();
    this.timers = timers;
    this.evaluatedAt = now;
    this.sampledAt = Date.now();
    const shouldSound = this.updateAlarm(now);
    return {
      shouldSound,
      unacknowledgedExpiredCount: timers.filter(
        (t) => t.status === 'EXPIRED' && t.acknowledgedAt === null,
      ).length,
    };
  }

  private updateAlarm(now: EpochMs): boolean {
    const shouldSound = typeof document !== 'undefined'
      && document.visibilityState === 'visible'
      && this.timers.some((timer) => shouldPlayAlarmAudio(timer, now)
        && timer.status === 'EXPIRED' && now >= timer.expiredAt && now < timer.expiredAt + 60_000);
    if (shouldSound && !this.isAlarmActive) {
      this.isAlarmActive = true;
      this.pulse();
      if (typeof window !== 'undefined') {
        this.intervalId = window.setInterval(() => {
          const current = this.evaluatedAt + Date.now() - this.sampledAt;
          if (this.updateAlarm(current)) this.pulse();
        }, 1000);
      }
    } else if (!shouldSound && this.isAlarmActive) {
      this.silence();
    }
    return shouldSound;
  }

  private pulse(): void {
    getAudioSynth().playAlarmPulse();
    vibrationAdapter.vibrate([300, 150, 300]);
  }

  private silence(): void {
    this.isAlarmActive = false;
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    getAudioSynth().stopAlarm();
    vibrationAdapter.cancel();
  }

  public stopAlarmLoop(): void {
    this.timers = [];
    this.silence();
  }

  public cleanup(): void {
    this.stopAlarmLoop();
    if (this.listening) {
      document.removeEventListener('visibilitychange', this.handleVisibilityChange);
      this.listening = false;
    }
  }
}

export const sharedAlarmCoordinator = new AlarmCoordinator();
