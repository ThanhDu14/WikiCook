import type { DurationCandidate } from './sessionModel';

export interface ResolvedStepDurations {
  primary?: DurationCandidate;
  alternatives: DurationCandidate[];
}

/**
 * Parses Vietnamese duration candidates from recipe step text.
 * Enforces policies A01 and A06:
 * - Compound "1 giờ 30 phút" -> single 5400s candidate
 * - Range "3-5 phút" or "3 đến 5 phút" -> 300s primary upper, 180s secondary lower
 * - Ignores 0, negative (-5 phút), reversed ranges (5-3 phút), or overflow (> 86400s)
 * - Independent parsed chips without auto-sum
 */
export function parseDurationCandidates(text: string): DurationCandidate[] {
  if (!text || typeof text !== 'string') return [];

  const candidates: DurationCandidate[] = [];
  const visitedSpans: Array<[number, number]> = [];

  const overlapsWithVisited = (start: number, end: number) => {
    return visitedSpans.some(([s, e]) => Math.max(s, start) < Math.min(e, end));
  };

  // 1. Compound: "X giờ/tiếng Y phút"
  const compoundRegex = /(?<![-–—\d])(\d+)\s*(?:giờ|tiếng)\s*(\d+)\s*phút/gi;
  let match: RegExpExecArray | null;

  while ((match = compoundRegex.exec(text)) !== null) {
    const hours = parseInt(match[1], 10);
    const minutes = parseInt(match[2], 10);
    if (hours > 0 && minutes >= 0 && hours <= 24) {
      const totalSeconds = hours * 3600 + minutes * 60;
      if (totalSeconds > 0 && totalSeconds <= 86400) {
        const start = match.index;
        const end = start + match[0].length;
        candidates.push({
          seconds: totalSeconds,
          source: 'TEXT',
          label: match[0].trim(),
          textSpan: { start, end },
        });
        visitedSpans.push([start, end]);
      }
    }
  }

  // 2. Ranges: "X - Y phút/giây/giờ/tiếng" or "X đến Y phút/giây/giờ/tiếng"
  const rangeRegex = /(?<![-–—\d])(\d+)\s*(?:-|–|—|đến)\s*(\d+)\s*(phút|giây|giờ|tiếng)/gi;

  while ((match = rangeRegex.exec(text)) !== null) {
    const start = match.index;
    const end = start + match[0].length;
    if (overlapsWithVisited(start, end)) continue;

    const lowerVal = parseInt(match[1], 10);
    const upperVal = parseInt(match[2], 10);
    const unit = match[3].toLowerCase();

    // A06: Reversed ranges (e.g. 5-3 phút) are invalid
    if (lowerVal > 0 && upperVal > lowerVal) {
      let multiplier = 60;
      if (unit === 'giây') multiplier = 1;
      else if (unit === 'giờ' || unit === 'tiếng') multiplier = 3600;

      const lowerSec = lowerVal * multiplier;
      const upperSec = upperVal * multiplier;

      if (upperSec <= 86400) {
        candidates.push({
          seconds: upperSec,
          lowerSeconds: lowerSec,
          source: 'TEXT',
          label: match[0].trim(),
          textSpan: { start, end },
        });
        visitedSpans.push([start, end]);
      }
    }
  }

  // 3. Simple standalone: "X phút", "X giây", "X giờ/tiếng"
  const simpleRegex = /(?<![-–—\d])(\d+)\s*(phút|giây|giờ|tiếng)/gi;

  while ((match = simpleRegex.exec(text)) !== null) {
    const start = match.index;
    const end = start + match[0].length;
    if (overlapsWithVisited(start, end)) continue;

    const val = parseInt(match[1], 10);
    const unit = match[2].toLowerCase();

    if (val > 0) {
      let multiplier = 60;
      if (unit === 'giây') multiplier = 1;
      else if (unit === 'giờ' || unit === 'tiếng') multiplier = 3600;

      const totalSeconds = val * multiplier;
      if (totalSeconds <= 86400) {
        candidates.push({
          seconds: totalSeconds,
          source: 'TEXT',
          label: match[0].trim(),
          textSpan: { start, end },
        });
        visitedSpans.push([start, end]);
      }
    }
  }

  // Sort candidates by appearance order in text
  candidates.sort((a, b) => (a.textSpan?.start ?? 0) - (b.textSpan?.start ?? 0));
  return candidates;
}

/**
 * Resolves step durations coordinating author timer and parsed text candidates per A01.
 */
export function resolveStepDurations(
  authorTimerSeconds: number | null,
  instructionText: string,
): ResolvedStepDurations {
  const textCandidates = parseDurationCandidates(instructionText);

  if (authorTimerSeconds && authorTimerSeconds > 0) {
    const primary: DurationCandidate = {
      seconds: authorTimerSeconds,
      source: 'AUTHOR',
      label: 'Theo công thức',
    };

    // Filter out text candidates that are identical in duration to avoid duplicate chips
    const alternatives = textCandidates.filter((cand) => cand.seconds !== authorTimerSeconds);

    return {
      primary,
      alternatives,
    };
  }

  if (textCandidates.length > 0) {
    const [primary, ...alternatives] = textCandidates;
    return {
      primary,
      alternatives,
    };
  }

  return {
    alternatives: [],
  };
}
