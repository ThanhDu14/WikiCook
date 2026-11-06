import { mealAt, withMeal } from '../hooks/useWeekPlan';
import { vi } from '../strings.vi';
import type { DraftItem, PlanItem, WeekPlan } from '../types';

/** Ids of draft dishes on the calendar, far from the server's and the optimistic ones. */
const DRAFT_ID = -1_000_000;

/** The week with the draft dishes after the saved ones of each meal, marked with `draftKey` (T102). */
export function withDraft(week: WeekPlan, drafts: DraftItem[] | undefined): WeekPlan {
  if (!drafts?.length) return week;
  return drafts.reduce((w, draft, index) => {
    const items = mealAt(w, draft.day, draft.meal);
    const shown: PlanItem = {
      id: DRAFT_ID - index,
      version: 0,
      position: items.length,
      recipeId: draft.recipeId,
      recipeName: draft.recipeName ?? vi.chooseDish,
      coverImageUrl: draft.coverImageUrl ?? null,
      totalTimeMinutes: draft.totalTimeMinutes ?? null,
      servings: draft.servings ?? 0,
      source: draft.source,
      available: true,
      draftKey: draft.key,
    };
    // `empty` and `availableItemCount` stay those of the saved week
    return { ...withMeal(w, draft.day, draft.meal, [...items, shown]), empty: week.empty, availableItemCount: week.availableItemCount };
  }, week);
}
