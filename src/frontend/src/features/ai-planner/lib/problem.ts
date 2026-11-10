import type { NotEnoughRecipes, RelaxSuggestion } from '../types';

const RELAX: readonly RelaxSuggestion[] = ['INCREASE_MAX_COOK_TIME', 'REMOVE_PREFERRED_CUISINES', 'SELECT_FEWER_SLOTS'];
const VIETNAM = 'Asia/Ho_Chi_Minh';
const clock = new Intl.DateTimeFormat('vi-VN', { timeZone: VIETNAM, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' });

/** The details of a 422 `NOT_ENOUGH_RECIPES`; missing ones read as nothing found and "chọn ít ô hơn" (US4-2). */
export function notEnoughOf(problem: Record<string, unknown>): NotEnoughRecipes {
  const number = (value: unknown) => (typeof value === 'number' && Number.isFinite(value) ? value : 0);
  const suggestions = Array.isArray(problem.suggestions)
    ? RELAX.filter((s) => (problem.suggestions as unknown[]).includes(s))
    : [];
  return {
    eligibleCount: number(problem.eligibleCount),
    requestedSlots: number(problem.requestedSlots),
    suggestions: suggestions.length > 0 ? suggestions : ['SELECT_FEWER_SLOTS'],
  };
}

/** `nextAvailableAt` of a 429 `RATE_LIMITED` (or of the options), when it is a valid time. */
export function nextAvailableAtOf(problem: Record<string, unknown>): string | null {
  const at = problem.nextAvailableAt;
  return typeof at === 'string' && !Number.isNaN(Date.parse(at)) ? at : null;
}

/** "15:20": the hour of an instant in Vietnam, where the planner's calendar lives (research R2). */
export function vietnamTime(instant: string): string {
  return clock.format(new Date(instant));
}
