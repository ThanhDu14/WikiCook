import type { EpochMs } from '../domain/sessionModel';

export class BrowserClock {
  private lastWallMs: EpochMs = Date.now();
  private lastMonoMs: number = typeof performance !== 'undefined' ? performance.now() : 0;

  now(): EpochMs {
    return Date.now();
  }

  monotonicNow(): number {
    return typeof performance !== 'undefined' ? performance.now() : Date.now();
  }

  recordSample(wallMs: EpochMs = this.now(), monoMs: number = this.monotonicNow()): void {
    this.lastWallMs = wallMs;
    this.lastMonoMs = monoMs;
  }

  /**
   * Detects clock jumps per A03 policy:
   * Compares wall-clock delta against monotonic delta.
   * If discrepancy exceeds threshold (default 2000ms), returns true.
   */
  detectDiscrepancy(
    currentWallMs: EpochMs = this.now(),
    currentMonoMs: number = this.monotonicNow(),
    thresholdMs: number = 2000,
  ): boolean {
    const wallDelta = currentWallMs - this.lastWallMs;
    const monoDelta = currentMonoMs - this.lastMonoMs;
    const discrepancy = Math.abs(wallDelta - monoDelta);

    return discrepancy > thresholdMs;
  }
}

export const browserClock = new BrowserClock();
