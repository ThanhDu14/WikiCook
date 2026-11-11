import type { KitchenTimer, EpochMs } from '../domain/sessionModel';
import { shouldPlayAlarmAudio } from '../domain/timerEngine';
import { getAudioSynth, vibrationAdapter } from '../adapters/browserCapabilities';

export class AlarmCoordinator {
  private isAlarmActive: boolean = false;
  private intervalId: number | null = null;

  public evaluateAlarms(timers: KitchenTimer[], now: EpochMs = Date.now()): {
    shouldSound: boolean;
    unacknowledgedExpiredCount: number;
  } {
    const soundingTimers = timers.filter((t) => shouldPlayAlarmAudio(t, now));
    const shouldSound = soundingTimers.length > 0;

    const unacknowledgedExpiredCount = timers.filter(
      (t) => t.status === 'EXPIRED' && t.acknowledgedAt === null,
    ).length;

    if (shouldSound && !this.isAlarmActive) {
      this.startAlarmLoop();
    } else if (!shouldSound && this.isAlarmActive) {
      this.stopAlarmLoop();
    }

    return {
      shouldSound,
      unacknowledgedExpiredCount,
    };
  }

  private startAlarmLoop(): void {
    this.isAlarmActive = true;
    const synth = getAudioSynth();
    synth.playAlarmPulse();
    vibrationAdapter.vibrate([300, 150, 300]);

    if (typeof window !== 'undefined' && this.intervalId === null) {
      this.intervalId = window.setInterval(() => {
        synth.playAlarmPulse();
        vibrationAdapter.vibrate([300, 150, 300]);
      }, 1000);
    }
  }

  public stopAlarmLoop(): void {
    this.isAlarmActive = false;
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    getAudioSynth().stopAlarm();
    vibrationAdapter.cancel();
  }

  public cleanup(): void {
    this.stopAlarmLoop();
  }
}

export const sharedAlarmCoordinator = new AlarmCoordinator();
