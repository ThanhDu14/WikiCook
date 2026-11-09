import type { NotEnoughRecipes, RelaxSuggestion } from '../types';

const RELAX: readonly RelaxSuggestion[] = ['INCREASE_MAX_COOK_TIME', 'REMOVE_PREFERRED_CUISINES', 'SELECT_FEWER_SLOTS'];

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
