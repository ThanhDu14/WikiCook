import { DAYS, MEALS, type Day, type Meal, type PlanItem, type WeekPlan } from '../types';
import { dateOf } from '../lib/week';

export const WEEK = '2026-10-19';

export function item(overrides: Partial<PlanItem> = {}): PlanItem {
  return {
    id: 1,
    version: 0,
    recipeId: 42,
    recipeName: 'Canh chua cá lóc',
    coverImageUrl: 'https://img.test/canh-chua.jpg',
    totalTimeMinutes: 40,
    servings: 4,
    source: 'MANUAL',
    available: true,
    dietType: 'NONE',
    ...overrides,
  };
}

/** A week of the openapi shape; `items` places dishes by "DAY/MEAL". */
export function week(items: Partial<Record<`${Day}/${Meal}`, PlanItem>> = {}, today: string | null = '2026-10-21'): WeekPlan {
  const placed = Object.values(items);
  return {
    weekStart: WEEK,
    weekEnd: dateOf(WEEK, 6),
    today,
    empty: placed.length === 0,
    availableItemCount: placed.filter((i) => i?.available).length,
    days: DAYS.map((day, index) => ({
      date: dateOf(WEEK, index),
      day,
      slots: MEALS.map((meal) => ({ meal, item: items[`${day}/${meal}`] ?? null })),
      nutrition: null,
    })),
  };
}
