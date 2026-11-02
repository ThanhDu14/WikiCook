import { useCallback, useEffect, useReducer, useRef } from 'react';
import { mealPlanApi, MealPlanApiError } from '../api/mealPlanApi';
import { vi } from '../strings.vi';
import { DAYS, MEALS, type Day, type ErrorCode, type ItemSource, type Meal, type PlanItem, type WeekPlan } from '../types';

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

/** The week with one slot replaced; `empty` and `availableItemCount` follow (FR-022, FR-025). */
export function withSlot(week: WeekPlan, day: Day, meal: Meal, item: PlanItem | null): WeekPlan {
  const days = week.days.map((d) =>
    d.day !== day ? d : { ...d, slots: d.slots.map((s) => (s.meal === meal ? { ...s, item } : s)) },
  );
  const items = days.flatMap((d) => d.slots.map((s) => s.item)).filter((i): i is PlanItem => i !== null);
  return { ...week, days, empty: items.length === 0, availableItemCount: items.filter((i) => i.available).length };
}

export function itemAt(week: WeekPlan | null, day: Day, meal: Meal): PlanItem | null {
  return week?.days[DAYS.indexOf(day)]?.slots[MEALS.indexOf(meal)]?.item ?? null;
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
        } else if (!silent.includes(code)) {
          dispatch({ type: 'notice', notice: vi.notSaved });
        }
        return code;
      }
    },
    [reload],
  );

  /**
   * Puts a recipe in a slot: adds it when the slot is empty, replaces the dish otherwise (servings kept unless
   * given, US3-4). 409 SLOT_OCCUPIED and 422 are left to the caller (dialog, picker).
   */
  const placeDish = useCallback(
    (day: Day, meal: Meal, recipe: ChosenRecipe, opts: { servings?: number; source?: ItemSource; replace?: PlanItem | null } = {}) => {
      const current = opts.replace === undefined ? itemAt(latest.current, day, meal) : opts.replace;
      const optimisticItem: PlanItem = {
        id: current?.id ?? temporaryId--,
        version: current?.version ?? 0,
        recipeId: recipe.recipeId,
        recipeName: recipe.title,
        coverImageUrl: recipe.coverImageUrl ?? null,
        totalTimeMinutes: recipe.totalTimeMinutes ?? null,
        servings: opts.servings ?? current?.servings ?? 1,
        source: opts.source ?? 'MANUAL',
        available: true,
      };
      return apply(
        (week) => withSlot(week, day, meal, optimisticItem),
        async () => {
          const saved = await mealPlanApi.putSlot(weekStart, day, meal, {
            recipeId: recipe.recipeId,
            servings: opts.servings ?? null,
            ...(opts.source ? { source: opts.source } : {}),
            expected: current ? { itemId: current.id, version: current.version } : null,
          });
          return (week) => withSlot(week, day, meal, saved);
        },
        ['SLOT_OCCUPIED', 'RECIPE_NOT_AVAILABLE'],
      );
    },
    [apply, weekStart],
  );

  const addDish = useCallback(
    (day: Day, meal: Meal, recipe: ChosenRecipe, opts: { servings?: number; source?: ItemSource } = {}) =>
      placeDish(day, meal, recipe, { ...opts, replace: null }),
    [placeDish],
  );

  const changeServings = useCallback(
    (day: Day, meal: Meal, servings: number) => {
      const current = itemAt(latest.current, day, meal);
      if (!current) return Promise.resolve('SLOT_CHANGED' as ErrorCode);
      return apply(
        (week) => withSlot(week, day, meal, { ...current, servings }),
        async () => {
          const saved = await mealPlanApi.patchServings(weekStart, day, meal, {
            servings,
            expected: { itemId: current.id, version: current.version },
          });
          return (week) => withSlot(week, day, meal, saved);
        },
      );
    },
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
    placeDish,
    changeServings,
    dismissNotice,
  };
}
