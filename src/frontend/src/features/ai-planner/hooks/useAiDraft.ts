import { useCallback, useMemo, useSyncExternalStore } from 'react';
import type { Day, DraftChange, DraftItem, Meal, PickerResult } from '@/features/meal-planner/types';
import type { ConfirmRequest, DraftResponse, DraftResponseItem, GenerateRequest, SlotKey } from '../types';

/** A draft as edited so far, and the request it came from ("Tạo lại", servings of a picked dish). */
interface StoredDraft {
  draft: DraftResponse;
  request: GenerateRequest | null;
}

/**
 * The unsaved drafts, one per week, in memory only (research R13): never in localStorage, sessionStorage or the
 * server; a reload loses them. Shared so the "Tạo bằng AI" button (in the week header) and the calendar of the page
 * see the same draft.
 */
const drafts = new Map<string, StoredDraft>();
const listeners = new Set<() => void>();

function emit() {
  listeners.forEach((listener) => listener());
}

/** Calls `listener` on every change of any draft. */
export function subscribeAiDrafts(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** Drops every draft (tests, sign-out). */
export function clearAllAiDrafts() {
  drafts.clear();
  emit();
}

/** Whether `weekStart` has a draft now (read outside React, e.g. by the leave guard while navigating). */
export function hasAiDraft(weekStart: string): boolean {
  return drafts.has(weekStart);
}

/** Drops the draft of `weekStart`. */
export function clearAiDraft(weekStart: string) {
  if (drafts.delete(weekStart)) emit();
}

/**
 * A draft holds at most one entry per meal (a dish or a "Chọn món" meal, US1), so the meal is the key: it stays the
 * same when other entries move or go. Spec 2026-11-02 (several dishes per meal) changes this with T107.
 */
const keyOf = (slot: SlotKey) => `${slot.day}:${slot.meal}`;
const sameMeal = (a: SlotKey, b: SlotKey) => a.day === b.day && a.meal === b.meal;

/** The meal a draft key stands for. */
export function draftSlot(key: string): SlotKey {
  const [day, meal] = key.split(':');
  return { day: day as Day, meal: meal as Meal };
}

/** The draft as dishes for `WeekGrid` / `DayView` of 030: dishes labelled AI, Auto or picked, then the "Chọn món" meals. */
export function toDraftItems(draft: DraftResponse): DraftItem[] {
  const dishes = draft.items.map<DraftItem>((item) => ({
    key: keyOf(item),
    day: item.day,
    meal: item.meal,
    recipeId: item.recipeId,
    recipeName: item.recipeName,
    coverImageUrl: item.coverImageUrl ?? null,
    totalTimeMinutes: item.totalTimeMinutes ?? null,
    servings: item.servings ?? null,
    source: item.source,
  }));
  const choose = draft.chooseSlots.map<DraftItem>((slot) => ({
    key: keyOf(slot),
    day: slot.day,
    meal: slot.meal,
    recipeId: null,
    source: draft.mode,
  }));
  return [...dishes, ...choose];
}

/** The body of `POST …/confirm`: the dishes as they are now; "Chọn món" meals left empty are not sent (FR-018). */
export function toConfirmRequest(draft: DraftResponse): ConfirmRequest {
  return {
    requestId: draft.requestId,
    items: draft.items.map((item) => ({ day: item.day, meal: item.meal, recipeId: item.recipeId, servings: item.servings ?? null, source: item.source })),
  };
}

/** Moves the entry at `from` to `to`; an entry already at `to` (dish or "Chọn món") takes the place of the first. */
function move(draft: DraftResponse, from: SlotKey, to: SlotKey): DraftResponse {
  if (sameMeal(from, to)) return draft;
  const swap = (slot: SlotKey): SlotKey => (sameMeal(slot, from) ? to : sameMeal(slot, to) ? from : slot);
  return {
    ...draft,
    items: draft.items.map((item) => ({ ...item, ...swap(item) })),
    chooseSlots: draft.chooseSlots.map(swap),
  };
}

function remove(draft: DraftResponse, at: SlotKey): DraftResponse {
  return {
    ...draft,
    items: draft.items.filter((item) => !sameMeal(item, at)),
    chooseSlots: draft.chooseSlots.filter((slot) => !sameMeal(slot, at)),
  };
}

/** Puts a dish of the picker at `at` (replacing its dish or filling its "Chọn món"), with the draft's servings (FR-040). */
function place(stored: StoredDraft, at: SlotKey, recipe: PickerResult): DraftResponse {
  const { draft } = stored;
  const dish: DraftResponseItem = {
    day: at.day,
    meal: at.meal,
    recipeId: recipe.recipeId,
    recipeName: recipe.title,
    coverImageUrl: recipe.coverImageUrl ?? null,
    totalTimeMinutes: recipe.totalTimeMinutes ?? null,
    servings: stored.request ? stored.request.constraints.servings : (draft.items.find((i) => sameMeal(i, at))?.servings ?? null),
    source: 'MANUAL',
  };
  const index = draft.items.findIndex((item) => sameMeal(item, at));
  return {
    ...draft,
    items: index >= 0 ? draft.items.map((item, i) => (i === index ? dish : item)) : [...draft.items, dish],
    chooseSlots: draft.chooseSlots.filter((slot) => !sameMeal(slot, at)),
  };
}

const NONE: DraftItem[] = [];

/**
 * The draft of `weekStart` (T044, T053): `setDraft` after a generation (a new one replaces the old, US2-5), `change`
 * for a move or removal from the calendar, `place` for a dish chosen in the picker, `clear` for "Bỏ nháp". Nothing
 * here calls the server (FR-017).
 */
export function useAiDraft(weekStart: string) {
  const stored = useSyncExternalStore(subscribeAiDrafts, () => drafts.get(weekStart) ?? null);
  const draft = stored?.draft ?? null;
  const draftItems = useMemo(() => (draft ? toDraftItems(draft) : NONE), [draft]);

  const update = useCallback(
    (edit: (current: StoredDraft) => DraftResponse) => {
      const current = drafts.get(weekStart);
      if (!current) return;
      drafts.set(weekStart, { ...current, draft: edit(current) });
      emit();
    },
    [weekStart],
  );

  const setDraft = useCallback(
    (next: DraftResponse, request: GenerateRequest | null = null) => {
      drafts.set(weekStart, { draft: next, request });
      emit();
    },
    [weekStart],
  );
  const clear = useCallback(() => clearAiDraft(weekStart), [weekStart]);

  /** Move and remove; "replace" and "choose" open the picker, which then calls `place`. */
  const change = useCallback(
    (c: DraftChange) => {
      if (c.type === 'move') update(({ draft: d }) => move(d, draftSlot(c.key), c.to));
      else if (c.type === 'remove') update(({ draft: d }) => remove(d, draftSlot(c.key)));
    },
    [update],
  );
  const placeDish = useCallback((key: string, recipe: PickerResult) => update((s) => place(s, draftSlot(key), recipe)), [update]);

  return { draft, request: stored?.request ?? null, draftItems, setDraft, clear, change, place: placeDish };
}
