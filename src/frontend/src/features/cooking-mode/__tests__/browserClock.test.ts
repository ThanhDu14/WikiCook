import { describe, it, expect, beforeEach } from 'vitest';
import { BrowserClock } from '../adapters/browserClock';

describe('browserClock (T013 & A03)', () => {
  let clock: BrowserClock;

  beforeEach(() => {
    clock = new BrowserClock();
  });

  it('provides current epoch ms via now()', () => {
    const before = Date.now();
    const current = clock.now();
    const after = Date.now();
    expect(current).toBeGreaterThanOrEqual(before);
    expect(current).toBeLessThanOrEqual(after);
  });

  it('detects wall-clock jumps (A03 policy) when Date.now() differs significantly from monotonic performance.now()', () => {
    // Normal passage of time: 1000ms in wall clock, 1000ms in monotonic
    clock.recordSample(100000, 1000);
    const noJump = clock.detectDiscrepancy(101000, 2000, 2000);
    expect(noJump).toBe(false);

    // Clock jumped forward by 60 seconds while monotonic only advanced 1 second
    const jumpForward = clock.detectDiscrepancy(161000, 2000, 2000);
    expect(jumpForward).toBe(true);

    // Clock jumped backward by 10 seconds
    const jumpBackward = clock.detectDiscrepancy(91000, 2000, 2000);
    expect(jumpBackward).toBe(true);
  });
});
