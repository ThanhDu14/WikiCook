import { describe, it, expect } from 'vitest';
import { parseDurationCandidates, resolveStepDurations } from '../domain/durationParser';

describe('durationParser (T006)', () => {
  it('parses single simple minute and second durations', () => {
    const text = 'Đun sôi nước trong 15 phút, sau đó tắt bếp.';
    const candidates = parseDurationCandidates(text);

    expect(candidates).toHaveLength(1);
    expect(candidates[0].seconds).toBe(900);
    expect(candidates[0].source).toBe('TEXT');
    expect(candidates[0].label).toContain('15 phút');
  });

  it('parses compound durations like "1 giờ 30 phút" into single 5400s candidate (US3-AC1)', () => {
    const text = 'Ninh nhỏ lửa trong 1 giờ 30 phút cho xương nhừ.';
    const candidates = parseDurationCandidates(text);

    expect(candidates).toHaveLength(1);
    expect(candidates[0].seconds).toBe(5400); // 1h30m = 3600 + 1800
    expect(candidates[0].source).toBe('TEXT');
    expect(candidates[0].label).toContain('1 giờ 30 phút');
  });

  it('parses "tiếng" as hours: "2 tiếng 15 phút" -> 8100s', () => {
    const text = 'Hầm chân giò trong 2 tiếng 15 phút.';
    const candidates = parseDurationCandidates(text);

    expect(candidates).toHaveLength(1);
    expect(candidates[0].seconds).toBe(8100); // 2*3600 + 15*60 = 7200 + 900
  });

  it('parses range durations "3-5 phút" with upper primary (300s) and lower secondary (180s) (US3-AC2)', () => {
    const text = 'Nướng hành tây trên chảo trong 3-5 phút cho xém cạnh.';
    const candidates = parseDurationCandidates(text);

    expect(candidates).toHaveLength(1);
    expect(candidates[0].seconds).toBe(300); // 5 min primary
    expect(candidates[0].lowerSeconds).toBe(180); // 3 min secondary
    expect(candidates[0].label).toContain('3-5 phút');
  });

  it('parses range with "đến": "10 đến 15 phút" -> 900s upper, 600s lower', () => {
    const text = 'Chiên cá từ 10 đến 15 phút cho vàng giòn cả hai mặt.';
    const candidates = parseDurationCandidates(text);

    expect(candidates).toHaveLength(1);
    expect(candidates[0].seconds).toBe(900);
    expect(candidates[0].lowerSeconds).toBe(600);
  });

  it('extracts multiple independent durations without auto-summing (A06)', () => {
    const text = 'Luộc gà 15 phút, sau đó ngâm trong nồi thêm 10 phút.';
    const candidates = parseDurationCandidates(text);

    expect(candidates).toHaveLength(2);
    expect(candidates[0].seconds).toBe(900);
    expect(candidates[1].seconds).toBe(600);
  });

  it('ignores invalid, reversed ranges, negatives, and 0s per A06 policy', () => {
    // Reversed range "5-3 phút" -> invalid
    expect(parseDurationCandidates('Rang trong 5-3 phút.')).toEqual([]);

    // 0 minutes -> invalid
    expect(parseDurationCandidates('Nấu trong 0 phút.')).toEqual([]);

    // Negative "-5 phút" -> invalid
    expect(parseDurationCandidates('Nấu trong -5 phút.')).toEqual([]);
  });

  it('deduplicates equivalent durations and coordinates with author timer (A01)', () => {
    const authorTimerSeconds = 300; // 5 min
    const text = 'Trần sơ xương bò trong 5 phút rồi vớt ra.';

    const resolved = resolveStepDurations(authorTimerSeconds, text);

    // Primary should be AUTHOR timer
    expect(resolved.primary).toBeDefined();
    expect(resolved.primary?.source).toBe('AUTHOR');
    expect(resolved.primary?.seconds).toBe(300);

    // Text candidate is 300s, which is identical to author timer -> deduplicated from alternatives
    expect(resolved.alternatives).toHaveLength(0);
  });

  it('keeps distinct parsed durations as separate choices when differing from author timer (A01)', () => {
    const authorTimerSeconds = 600; // 10 min
    const text = 'Đun sôi trong 3 phút rồi hạ lửa 10 phút.';

    const resolved = resolveStepDurations(authorTimerSeconds, text);

    expect(resolved.primary?.source).toBe('AUTHOR');
    expect(resolved.primary?.seconds).toBe(600);

    // 3 minutes (180s) is distinct from 600s author timer, so it appears in alternatives
    expect(resolved.alternatives).toHaveLength(1);
    expect(resolved.alternatives[0].seconds).toBe(180);
    expect(resolved.alternatives[0].source).toBe('TEXT');
  });
});
