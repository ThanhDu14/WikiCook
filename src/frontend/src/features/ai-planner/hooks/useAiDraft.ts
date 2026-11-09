import { useCallback, useMemo, useSyncExternalStore } from 'react';
import type { DraftItem } from '@/features/meal-planner/types';
import type { DraftResponse } from '../types';

/**
 * The unsaved drafts, one per week, in memory only (research R13): never in localStorage, sessionStorage or the
 * server; a reload loses them. Shared so the "Tạo bằng AI" button (in the week header) and the calendar of the page
 * see the same draft.
 */
const drafts = new Map<string, DraftResponse>();
const listeners = new Set<() => void>();

function emit() {
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Drops every draft (tests, sign-out). */
export function clearAllAiDrafts() {
  drafts.clear();
  emit();
}

/** The draft as dishes for `WeekGrid` / `DayView` of 030: dishes labelled AI or Auto, then the "Chọn món" meals. */
export function toDraftItems(draft: DraftResponse): DraftItem[] {
  const dishes = draft.items.map<DraftItem>((item, index) => ({
    key: `${draft.requestId}:${index}`,
    day: item.day,
    meal: item.meal,
    recipeId: item.recipeId,
    recipeName: item.recipeName,
    coverImageUrl: item.coverImageUrl ?? null,
    totalTimeMinutes: item.totalTimeMinutes ?? null,
    servings: item.servings ?? null,
    source: item.source,
  }));
  const choose = draft.chooseSlots.map<DraftItem>((slot, index) => ({
    key: `${draft.requestId}:choose:${index}`,
    day: slot.day,
    meal: slot.meal,
    recipeId: null,
    source: draft.mode,
  }));
  return [...dishes, ...choose];
}

const NONE: DraftItem[] = [];

/** The draft of `weekStart` (T044): `setDraft` after a generation, `clear` to drop it. */
export function useAiDraft(weekStart: string) {
  const draft = useSyncExternalStore(subscribe, () => drafts.get(weekStart) ?? null);
  const draftItems = useMemo(() => (draft ? toDraftItems(draft) : NONE), [draft]);
  const setDraft = useCallback((next: DraftResponse) => {
    drafts.set(weekStart, next);
    emit();
  }, [weekStart]);
  const clear = useCallback(() => {
    if (drafts.delete(weekStart)) emit();
  }, [weekStart]);
  return { draft, draftItems, setDraft, clear };
}
