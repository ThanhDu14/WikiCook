import { useCallback, useEffect, useReducer, useRef } from 'react';
import { mealPlanApi, MealPlanApiError } from '../api/mealPlanApi';
import { vi } from '../strings.vi';
import { DAYS, MAX_DISHES, MEALS, type Day, type ErrorCode, type ItemSource, type Meal, type PlacedItem, type PlanItem, type WeekPlan } from '../types';

/** A slot of the week. */
export interface SlotRef {
  day: Day;
  meal: Meal;
}

/** What the picker hands over when a recipe is chosen. */
export interface ChosenRecipe {
  recipeId: number;
  title: string;
  coverImageUrl?: string | null;
  totalTimeMinutes?: number | null;
}

type Status = 'loading' | 'ready' | 'error';

interface State {
  status: Status;
  week: WeekPlan | null;
  error: ErrorCode | null;
  notice: string | null;
}

type Action =
  | { type: 'loading' }
  | { type: 'loaded'; week: WeekPlan }
  | { type: 'failed'; error: ErrorCode }
  | { type: 'week'; week: WeekPlan }
  | { type: 'notice'; notice: string | null };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'loading':
      return { ...state, status: state.week ? state.status : 'loading', error: null };
    case 'loaded':
      return { ...state, status: 'ready', week: action.week, error: null };
    case 'failed':
      return { ...state, status: 'error', error: action.error };
    case 'week':
      return { ...state, week: action.week };
    case 'notice':
      return { ...state, notice: action.notice };
  }
}

/** Where a dish goes: a meal, and a place in it (none = its end). */
export interface MoveTarget extends SlotRef {
  position?: number;
}

/** The week with the dishes of one meal replaced, positioned in list order; `empty` and `availableItemCount` follow (FR-022, FR-025). */
export function withMeal(week: WeekPlan, day: Day, meal: Meal, items: PlanItem[]): WeekPlan {
  const placed = items.map((item, position) => (item.position === position ? item : { ...item, position }));
  const days = week.days.map((d) =>
    d.day !== day ? d : { ...d, slots: d.slots.map((s) => (s.meal === meal ? { ...s, items: placed } : s)) },
  );
  const all = days.flatMap((d) => d.slots.flatMap((s) => s.items));
  return { ...week, days, empty: all.length === 0, availableItemCount: all.filter((i) => i.available).length };
}

/** The dishes of a meal, in order. */
export function mealAt(week: WeekPlan | null, day: Day, meal: Meal): PlanItem[] {
  return week?.days[DAYS.indexOf(day)]?.slots[MEALS.indexOf(meal)]?.items ?? [];
}

/** A dish of the week with its meal and place. */
export function findItem(week: WeekPlan | null, itemId: number): (SlotRef & { index: number; item: PlanItem }) | null {
  for (const d of week?.days ?? []) {
    for (const s of d.slots) {
      const index = s.items.findIndex((i) => i.id === itemId);
      if (index >= 0) return { day: d.day, meal: s.meal, index, item: s.items[index] };
    }
  }
  return null;
}

/** The week with one dish swapped for what the server saved, in the meal the server says. */
function withPlaced(week: WeekPlan, placed: PlacedItem, replacing = placed.item.id): WeekPlan {
  const items = mealAt(week, placed.day, placed.meal);
  return withMeal(week, placed.day, placed.meal, items.map((i) => (i.id === replacing ? placed.item : i)));
}

const codeOf = (error: unknown): ErrorCode => (error instanceof MealPlanApiError ? error.code : 'UNKNOWN');

let temporaryId = -1;

/**
 * State of one week (T044, research R12). Every change shows at once, is saved straight away (no separate save
 * step, US1-9), and is put back with a notice when saving fails (FR-019).
 */
export function useWeekPlan(weekStart: string, options: { onUnauthenticated?: () => void } = {}) {
  const [state, dispatch] = useReducer(reducer, { status: 'loading', week: null, error: null, notice: null });
  /** The week as last applied, read by callbacks without waiting for a render. */
  const latest = useRef<WeekPlan | null>(null);
  const request = useRef(0);
  const onUnauthenticated = useRef(options.onUnauthenticated);
  useEffect(() => {
    onUnauthenticated.current = options.onUnauthenticated;
  }, [options.onUnauthenticated]);

  const reload = useCallback(async () => {
    const mine = ++request.current;
    dispatch({ type: 'loading' });
    try {
      const week = await mealPlanApi.getWeek(weekStart);
      if (mine !== request.current) return;
      latest.current = week;
      dispatch({ type: 'loaded', week });
    } catch (error) {
      if (mine !== request.current) return;
      const code = codeOf(error);
      if (code === 'UNAUTHENTICATED') onUnauthenticated.current?.();
      dispatch({ type: 'failed', error: code });
    }
  }, [weekStart]);

  useEffect(() => {
    latest.current = null;
    void reload();
  }, [reload]);

  /** Applies `optimistic`, runs `save`, keeps what the server returned or restores the snapshot. */
  const apply = useCallback(
    async (optimistic: (week: WeekPlan) => WeekPlan, save: () => Promise<(week: WeekPlan) => WeekPlan>, silent: ErrorCode[] = []) => {
      const snapshot = latest.current;
      if (!snapshot) return 'UNKNOWN' as ErrorCode;
      const next = optimistic(snapshot);
      latest.current = next;
      dispatch({ type: 'week', week: next });
      try {
        const settle = await save();
        const settled = settle(latest.current ?? next);
        latest.current = settled;
        dispatch({ type: 'week', week: settled });
        return null;
      } catch (error) {
        const code = codeOf(error);
        latest.current = snapshot;
        dispatch({ type: 'week', week: snapshot });
        if (code === 'UNAUTHENTICATED') {
          onUnauthenticated.current?.();
        } else if (code === 'SLOT_CHANGED') {
          dispatch({ type: 'notice', notice: vi.planChanged });
          void reload();
        } else if (code === 'MEAL_FULL') {
          // filled up from another tab or device meanwhile
          dispatch({ type: 'notice', notice: vi.mealFull });
          void reload();
        } else if (!silent.includes(code)) {
          dispatch({ type: 'notice', notice: vi.notSaved });
        }
        return code;
      }
    },
    [reload],
  );

  /** Says the meal is full without asking the server (FR-004). */
  const full = useCallback(() => {
    dispatch({ type: 'notice', notice: vi.mealFull });
    return Promise.resolve('MEAL_FULL' as ErrorCode);
  }, []);

  /** Adds a dish at the end of a meal (US1-2, US1-11); 422 is left to the caller (picker). */
  const addDish = useCallback(
    (day: Day, meal: Meal, recipe: ChosenRecipe, opts: { servings?: number; source?: ItemSource } = {}) => {
      const count = mealAt(latest.current, day, meal).length;
      if (count >= MAX_DISHES) return full();
      const optimistic: PlanItem = {
        id: temporaryId--,
        version: 0,
        position: count,
        recipeId: recipe.recipeId,
        recipeName: recipe.title,
        coverImageUrl: recipe.coverImageUrl ?? null,
        totalTimeMinutes: recipe.totalTimeMinutes ?? null,
        servings: opts.servings ?? 1,
        source: opts.source ?? 'MANUAL',
        available: true,
      };
      return apply(
        (week) => withMeal(week, day, meal, [...mealAt(week, day, meal), optimistic]),
        async () => {
          const placed = await mealPlanApi.addItem(weekStart, day, meal, {
            recipeId: recipe.recipeId,
            servings: opts.servings ?? null,
            ...(opts.source ? { source: opts.source } : {}),
          });
          return (week) => withPlaced(week, placed, optimistic.id);
        },
        ['RECIPE_NOT_AVAILABLE'],
      );
    },
    [apply, full, weekStart],
  );

  /** Puts another recipe in the place of a dish; its servings stay unless given (US3-5). */
  const replaceDish = useCallback(
    (itemId: number, recipe: ChosenRecipe, opts: { servings?: number; source?: ItemSource } = {}) => {
      const found = findItem(latest.current, itemId);
      if (!found) return Promise.resolve('SLOT_CHANGED' as ErrorCode);
      const { day, meal, item } = found;
      const optimistic: PlanItem = {
        ...item,
        recipeId: recipe.recipeId,
        recipeName: recipe.title,
        coverImageUrl: recipe.coverImageUrl ?? null,
        totalTimeMinutes: recipe.totalTimeMinutes ?? null,
        servings: opts.servings ?? item.servings,
        source: opts.source ?? 'MANUAL',
        available: true,
      };
      return apply(
        (week) => withPlaced(week, { day, meal, item: optimistic }),
        async () => {
          const placed = await mealPlanApi.replaceItem(weekStart, itemId, {
            recipeId: recipe.recipeId,
            servings: opts.servings ?? null,
            ...(opts.source ? { source: opts.source } : {}),
            version: item.version,
          });
          return (week) => withPlaced(week, placed);
        },
        ['RECIPE_NOT_AVAILABLE'],
      );
    },
    [apply, weekStart],
  );

  const changeServings = useCallback(
    (itemId: number, servings: number) => {
      const found = findItem(latest.current, itemId);
      if (!found) return Promise.resolve('SLOT_CHANGED' as ErrorCode);
      const { day, meal, item } = found;
      return apply(
        (week) => withPlaced(week, { day, meal, item: { ...item, servings } }),
        async () => {
          const placed = await mealPlanApi.patchServings(weekStart, itemId, { servings, version: item.version });
          return (week) => withPlaced(week, placed);
        },
      );
    },
    [apply, weekStart],
  );

  /**
   * Moves a dish to another meal (its end, or `position`) or to another place in its own meal (US3-1 … US3-4).
   * A full meal takes nothing more (FR-004). The server returns the meals it changed, in order.
   */
  const moveItem = useCallback(
    (itemId: number, to: MoveTarget) => {
      const found = findItem(latest.current, itemId);
      if (!found) return Promise.resolve('SLOT_CHANGED' as ErrorCode);
      const sameMeal = found.day === to.day && found.meal === to.meal;
      const targetCount = mealAt(latest.current, to.day, to.meal).length;
      if (!sameMeal && targetCount >= MAX_DISHES) return full();
      const last = sameMeal ? targetCount - 1 : targetCount;
      const at = Math.min(to.position ?? last, last);
      if (sameMeal && at === found.index) return Promise.resolve(null);
      return apply(
        (week) => {
          const source = mealAt(week, found.day, found.meal).filter((i) => i.id !== itemId);
          const target = sameMeal ? source : [...mealAt(week, to.day, to.meal)];
          target.splice(at, 0, found.item);
          const moved = sameMeal ? week : withMeal(week, found.day, found.meal, source);
          return withMeal(moved, to.day, to.meal, target);
        },
        async () => {
          const result = await mealPlanApi.moveItem(weekStart, itemId, {
            day: to.day,
            meal: to.meal,
            position: to.position ?? null,
            version: found.item.version,
          });
          return (week) => result.meals.reduce((w, m) => withMeal(w, m.day, m.meal, m.items), week);
        },
      );
    },
    [apply, full, weekStart],
  );

  /** Removes one dish; the dishes after it move up (US3-6). */
  const removeDish = useCallback(
    (itemId: number) => {
      const found = findItem(latest.current, itemId);
      if (!found) return Promise.resolve(null);
      return apply(
        (week) => withMeal(week, found.day, found.meal, mealAt(week, found.day, found.meal).filter((i) => i.id !== itemId)),
        async () => {
          await mealPlanApi.deleteItem(weekStart, itemId, found.item.version);
          return (week) => week;
        },
      );
    },
    [apply, weekStart],
  );

  /** Empties the three meals of a day (US3-7); the caller has asked for confirmation. */
  const clearDay = useCallback(
    (day: Day) =>
      apply(
        (week) => MEALS.reduce((w, meal) => withMeal(w, day, meal, []), week),
        async () => {
          await mealPlanApi.clearDay(weekStart, day);
          return (week) => week;
        },
      ),
    [apply, weekStart],
  );

  const dismissNotice = useCallback(() => dispatch({ type: 'notice', notice: null }), []);

  return {
    status: state.status,
    week: state.week,
    error: state.error,
    notice: state.notice,
    reload,
    addDish,
    replaceDish,
    changeServings,
    moveItem,
    removeDish,
    clearDay,
    dismissNotice,
  };
}
