/**
 * Browser capabilities adapter for Cooking Mode:
 * - Pure procedural Web Audio API synthesizer (alarms & earcon chimes)
 * - Screen Wake Lock API
 * - Fullscreen API
 * - Vibration API
 */

export interface AudioSynthController {
  unlock(): Promise<boolean>;
  playAlarmPulse(): void;
  playEarcon(): void;
  stopAlarm(): void;
  isUnlocked(): boolean;
  close(): void;
}

class WebAudioSynth implements AudioSynthController {
  private ctx: AudioContext | null = null;
  private alarmIntervalId: number | null = null;
  private unlocked = false;

  private getContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) return null;
    if (!this.ctx) {
      try {
        this.ctx = new AudioCtx();
      } catch {
        return null;
      }
    }
    return this.ctx;
  }

  async unlock(): Promise<boolean> {
    const ctx = this.getContext();
    if (!ctx) return false;
    try {
      if (ctx.state === 'suspended') {
        await ctx.resume();
      }
      this.unlocked = ctx.state === 'running';
      return this.unlocked;
    } catch {
      this.unlocked = false;
      return false;
    }
  }

  isUnlocked(): boolean {
    const ctx = this.getContext();
    return !!ctx && ctx.state === 'running';
  }

  playEarcon(): void {
    const ctx = this.getContext();
    if (!ctx || ctx.state !== 'running') return;
    try {
      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(587.33, now); // D5
      osc.frequency.exponentialRampToValueAtTime(880, now + 0.15); // A5

      gain.gain.setValueAtTime(0.15, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.3);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start(now);
      osc.stop(now + 0.35);
    } catch {
      // Audio playback failed silently
    }
  }

  playAlarmPulse(): void {
    const ctx = this.getContext();
    if (!ctx || ctx.state !== 'running') return;
    try {
      const now = ctx.currentTime;
      // Dual-tone urgent kitchen alarm pulse: 880Hz + 1760Hz
      const osc1 = ctx.createOscillator();
      const osc2 = ctx.createOscillator();
      const gain = ctx.createGain();

      osc1.type = 'triangle';
      osc2.type = 'sine';
      osc1.frequency.setValueAtTime(880, now);
      osc2.frequency.setValueAtTime(1760, now);

      gain.gain.setValueAtTime(0.25, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.25);

      osc1.connect(gain);
      osc2.connect(gain);
      gain.connect(ctx.destination);

      osc1.start(now);
      osc2.start(now);
      osc1.stop(now + 0.28);
      osc2.stop(now + 0.28);
    } catch {
      // Ignore audio error
    }
  }

  stopAlarm(): void {
    if (this.alarmIntervalId !== null) {
      clearInterval(this.alarmIntervalId);
      this.alarmIntervalId = null;
    }
  }

  close(): void {
    this.stopAlarm();
    if (this.ctx) {
      try {
        this.ctx.close();
      } catch {
        // ignore
      }
      this.ctx = null;
      this.unlocked = false;
    }
  }
}

let sharedAudioSynth: AudioSynthController | null = null;

export function getAudioSynth(): AudioSynthController {
  if (!sharedAudioSynth) {
    sharedAudioSynth = new WebAudioSynth();
  }
  return sharedAudioSynth;
}

export function resetAudioSynthForTesting(mock?: AudioSynthController): void {
  if (sharedAudioSynth) {
    sharedAudioSynth.close();
  }
  sharedAudioSynth = mock ?? null;
}

// ----------------- Screen Wake Lock Adapter -----------------
export interface WakeLockSentinelLike {
  released: boolean;
  release(): Promise<void>;
  addEventListener(type: 'release', listener: () => void): void;
  removeEventListener(type: 'release', listener: () => void): void;
}

export interface WakeLockAdapter {
  isSupported(): boolean;
  request(): Promise<WakeLockSentinelLike | null>;
  release(): Promise<void>;
}

class BrowserWakeLock implements WakeLockAdapter {
  private sentinel: WakeLockSentinelLike | null = null;

  isSupported(): boolean {
    return typeof navigator !== 'undefined' && 'wakeLock' in navigator;
  }

  async request(): Promise<WakeLockSentinelLike | null> {
    if (!this.isSupported()) return null;
    try {
      const lock = await (navigator as unknown as {
        wakeLock: { request(type: 'screen'): Promise<WakeLockSentinelLike> };
      }).wakeLock.request('screen');

      this.sentinel = lock;
      lock.addEventListener('release', () => {
        if (this.sentinel === lock) {
          this.sentinel = null;
        }
      });
      return lock;
    } catch {
      this.sentinel = null;
      return null;
    }
  }

  async release(): Promise<void> {
    if (this.sentinel && !this.sentinel.released) {
      try {
        await this.sentinel.release();
      } catch {
        // ignore
      }
      this.sentinel = null;
    }
  }
}

export const wakeLockAdapter: WakeLockAdapter = new BrowserWakeLock();

// ----------------- Fullscreen Adapter -----------------
export interface FullscreenAdapter {
  isSupported(): boolean;
  isFullscreen(): boolean;
  request(element?: HTMLElement): Promise<boolean>;
  exit(): Promise<boolean>;
}

class BrowserFullscreen implements FullscreenAdapter {
  isSupported(): boolean {
    return typeof document !== 'undefined' && (
      'fullscreenEnabled' in document ||
      'webkitFullscreenEnabled' in document
    );
  }

  isFullscreen(): boolean {
    if (typeof document === 'undefined') return false;
    return !!(
      document.fullscreenElement ||
      (document as unknown as { webkitFullscreenElement: Element }).webkitFullscreenElement
    );
  }

  async request(element?: HTMLElement): Promise<boolean> {
    if (typeof document === 'undefined') return false;
    const target = element ?? document.documentElement;
    try {
      if (target.requestFullscreen) {
        await target.requestFullscreen();
        return true;
      }
      const webkitTarget = target as unknown as { webkitRequestFullscreen?: () => Promise<void> };
      if (webkitTarget.webkitRequestFullscreen) {
        await webkitTarget.webkitRequestFullscreen();
        return true;
      }
      return false;
    } catch {
      return false;
    }
  }

  async exit(): Promise<boolean> {
    if (typeof document === 'undefined' || !this.isFullscreen()) return false;
    try {
      if (document.exitFullscreen) {
        await document.exitFullscreen();
        return true;
      }
      const webkitDoc = document as unknown as { webkitExitFullscreen?: () => Promise<void> };
      if (webkitDoc.webkitExitFullscreen) {
        await webkitDoc.webkitExitFullscreen();
        return true;
      }
      return false;
    } catch {
      return false;
    }
  }
}

export const fullscreenAdapter: FullscreenAdapter = new BrowserFullscreen();

// ----------------- Vibration Adapter -----------------
export interface VibrationAdapter {
  isSupported(): boolean;
  vibrate(pattern: number | number[]): boolean;
  cancel(): boolean;
}

class BrowserVibration implements VibrationAdapter {
  isSupported(): boolean {
    return typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function';
  }

  vibrate(pattern: number | number[]): boolean {
    if (!this.isSupported()) return false;
    try {
      return navigator.vibrate(pattern);
    } catch {
      return false;
    }
  }

  cancel(): boolean {
    return this.vibrate(0);
  }
}

export const vibrationAdapter: VibrationAdapter = new BrowserVibration();
